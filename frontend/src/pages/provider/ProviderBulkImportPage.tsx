import React, { useState, useEffect, useRef } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { Link, useSearchParams } from 'react-router-dom';
import { toast } from 'sonner';
import {
  uploadBulkImportBatch,
  processImportBatch,
  getImportBatchStatus,
} from '@/api/providerPortalApi';
import type { ImportBatchItem } from '@/types/provider';
import { LoadingSpinner } from '@/components/feedback/LoadingSpinner';

const STORAGE_KEY_LAST_IMPORT = 'sih26135_last_import_batch_id';
const MAX_POLL_COUNT = 60; // Max 60 poll cycles before pausing to protect network/CPU
const MAX_CONSECUTIVE_FAILURES = 5;

export function ProviderBulkImportPage(): React.JSX.Element {
  const [searchParams, setSearchParams] = useSearchParams();
  const queryClient = useQueryClient();

  // Selected file state
  const [selectedFile, setSelectedFile] = useState<File | null>(null);
  const [isDragging, setIsDragging] = useState(false);
  const [entityType, setEntityType] = useState<'Trainee' | 'PlacementRecord'>('Trainee');
  const [uploadError, setUploadError] = useState<string | null>(null);
  const fileInputRef = useRef<HTMLInputElement>(null);

  // Active Import Batch ID
  const urlBatchId = searchParams.get('batchId') ? Number(searchParams.get('batchId')) : null;
  const savedBatchId = localStorage.getItem(STORAGE_KEY_LAST_IMPORT)
    ? Number(localStorage.getItem(STORAGE_KEY_LAST_IMPORT))
    : null;
  const [activeBatchId, setActiveBatchId] = useState<number | null>(urlBatchId || savedBatchId || null);

  // Bounded Polling & Resiliency state
  const [isPollingPaused, setIsPollingPaused] = useState<boolean>(false);
  const [isTimedOut, setIsTimedOut] = useState<boolean>(false);
  const [lastNetworkError, setLastNetworkError] = useState<string | null>(null);
  const pollCountRef = useRef<number>(0);
  const consecutiveFailuresRef = useRef<number>(0);

  // Reset polling counters whenever activeBatchId changes
  const resetPollingBudget = () => {
    pollCountRef.current = 0;
    consecutiveFailuresRef.current = 0;
    setIsPollingPaused(false);
    setIsTimedOut(false);
    setLastNetworkError(null);
  };

  // Sync activeBatchId with URL and localStorage
  useEffect(() => {
    if (activeBatchId) {
      localStorage.setItem(STORAGE_KEY_LAST_IMPORT, String(activeBatchId));
      if (searchParams.get('batchId') !== String(activeBatchId)) {
        setSearchParams({ batchId: String(activeBatchId) });
      }
    }
  }, [activeBatchId, searchParams, setSearchParams]);

  // Upload Mutation
  const uploadMutation = useMutation({
    mutationFn: (file: File) => uploadBulkImportBatch(file, entityType, 'PROVIDER_PORTAL'),
    onSuccess: (data: ImportBatchItem) => {
      toast.success(`Import batch ${data.batchCode} staged successfully!`);
      setActiveBatchId(data.id);
      resetPollingBudget();
      setSelectedFile(null);
      setUploadError(null);
      queryClient.invalidateQueries({ queryKey: ['provider', 'importStatus', data.id] });
    },
    onError: (err: any) => {
      const msg = err.response?.data?.message || err.message || 'File upload failed';
      setUploadError(msg);
      toast.error(msg);
    },
  });

  // Process Mutation
  const processMutation = useMutation({
    mutationFn: (batchId: number) => processImportBatch(batchId),
    onSuccess: () => {
      toast.success('Batch processing started');
      resetPollingBudget();
      queryClient.invalidateQueries({ queryKey: ['provider', 'importStatus', activeBatchId] });
    },
    onError: (err: any) => {
      const msg = err.response?.data?.message || err.message || 'Failed to initiate batch processing';
      toast.error(msg);
    },
  });

  // Bounded Polling Status Query with Exponential Backoff
  const statusQuery = useQuery({
    queryKey: ['provider', 'importStatus', activeBatchId],
    queryFn: async () => {
      if (!activeBatchId) return null;
      try {
        const res = await getImportBatchStatus(activeBatchId);
        consecutiveFailuresRef.current = 0;
        setLastNetworkError(null);
        return res;
      } catch (err: unknown) {
        consecutiveFailuresRef.current += 1;
        const msg = (err as any)?.message || 'Failed to poll batch status';
        setLastNetworkError(msg);
        throw err;
      }
    },
    enabled: !!activeBatchId && !isPollingPaused && !isTimedOut,
    refetchInterval: (query) => {
      if (isPollingPaused || isTimedOut) return false;
      const data = query.state.data;
      if (!data) return 1500;
      // Stop polling when batch reaches terminal state
      if (data.isTerminal || ['COMPLETED', 'FAILED', 'PARTIALLY_COMPLETED'].includes(data.status)) {
        return false;
      }

      // Check consecutive failure limit
      if (consecutiveFailuresRef.current >= MAX_CONSECUTIVE_FAILURES) {
        setIsPollingPaused(true);
        return false;
      }

      // Check poll count budget
      pollCountRef.current += 1;
      if (pollCountRef.current >= MAX_POLL_COUNT) {
        setIsTimedOut(true);
        return false;
      }

      // Exponential backoff if there are network errors
      if (consecutiveFailuresRef.current > 0) {
        return Math.min(10000, 1500 * Math.pow(2, consecutiveFailuresRef.current));
      }

      return 1500;
    },
  });

  const handleResumePolling = () => {
    resetPollingBudget();
    statusQuery.refetch();
  };

  // Drag and Drop Handlers
  const handleDragOver = (e: React.DragEvent) => {
    e.preventDefault();
    setIsDragging(true);
  };

  const handleDragLeave = (e: React.DragEvent) => {
    e.preventDefault();
    setIsDragging(false);
  };

  const handleDrop = (e: React.DragEvent) => {
    e.preventDefault();
    setIsDragging(false);
    if (e.dataTransfer.files && e.dataTransfer.files.length > 0) {
      validateAndSetFile(e.dataTransfer.files[0]);
    }
  };

  const handleFileInputChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    if (e.target.files && e.target.files.length > 0) {
      validateAndSetFile(e.target.files[0]);
    }
  };

  const validateAndSetFile = (file: File) => {
    setUploadError(null);
    const lowerName = file.name.toLowerCase();
    const isCsv = lowerName.endsWith('.csv');
    const isJson = lowerName.endsWith('.json') || lowerName.endsWith('.ndjson');
    if (!isCsv && !isJson) {
      setUploadError('Only CSV, JSON, and NDJSON files are supported for bulk ingestion.');
      return;
    }
    const maxBytes = 10 * 1024 * 1024; // 10MB
    if (file.size > maxBytes) {
      setUploadError('File exceeds maximum allowed upload size of 10MB.');
      return;
    }
    setSelectedFile(file);
  };

  const handleUploadSubmit = () => {
    if (!selectedFile) return;
    uploadMutation.mutate(selectedFile);
  };

  const handleProcessSubmit = () => {
    if (!activeBatchId) return;
    processMutation.mutate(activeBatchId);
  };

  const handleClearActiveBatch = () => {
    setActiveBatchId(null);
    localStorage.removeItem(STORAGE_KEY_LAST_IMPORT);
    setSearchParams({});
    resetPollingBudget();
  };

  const statusData = statusQuery.data;
  const totalRows = statusData?.rowCount ?? 0;
  const processedRows =
    (statusData?.successCount ?? 0) +
    (statusData?.failureCount ?? 0) +
    (statusData?.duplicateCount ?? 0) +
    (statusData?.skippedCount ?? 0);
  const progressPct = totalRows > 0 ? Math.min(100, Math.round((processedRows / totalRows) * 100)) : 0;
  const isTerminal = statusData?.isTerminal || ['COMPLETED', 'FAILED', 'PARTIALLY_COMPLETED'].includes(statusData?.status ?? '');

  return (
    <div className="space-y-6">
      {/* Header */}
      <div className="card p-6 border-l-4 border-l-navy bg-white shadow-sm flex flex-col sm:flex-row sm:items-center justify-between gap-4">
        <div>
          <h1 className="text-2xl font-bold text-navy flex items-center gap-2">
            <span>Multipart Bulk Ingestion Workbench</span>
            <span className="badge-info text-xs">Streaming Validator</span>
          </h1>
          <p className="text-sm text-gray-600 mt-1">
            Upload and process high-volume trainee roster CSV and JSON datasets with streaming validation,
            canonical identity deduplication, and automated quarantine persistence.
          </p>
        </div>
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
        {/* Upload Panel */}
        <div className="card p-6 bg-white shadow-sm space-y-4">
          <div className="flex items-center justify-between pb-2 border-b border-gray-100">
            <h2 className="text-base font-bold text-navy">1. Select &amp; Stage Dataset File (CSV / JSON)</h2>
            <div className="flex items-center gap-2">
              <label className="text-xs font-semibold text-gray-600">Entity:</label>
              <select
                value={entityType}
                onChange={(e) => setEntityType(e.target.value as any)}
                className="input-field text-xs py-1 px-2 font-medium"
              >
                <option value="Trainee">Trainee Roster</option>
                <option value="PlacementRecord">Placement Records</option>
              </select>
            </div>
          </div>

          {/* Drag & Drop Box */}
          <div
            onDragOver={handleDragOver}
            onDragLeave={handleDragLeave}
            onDrop={handleDrop}
            onClick={() => fileInputRef.current?.click()}
            className={`border-2 border-dashed rounded-lg p-8 text-center cursor-pointer transition-all ${
              isDragging
                ? 'border-navy bg-navy-50/50'
                : selectedFile
                ? 'border-emerald-400 bg-emerald-50/30'
                : 'border-gray-300 hover:border-gray-400 bg-gray-50/50'
            }`}
          >
            <input
              ref={fileInputRef}
              type="file"
              accept=".csv,.json,.ndjson"
              className="hidden"
              onChange={handleFileInputChange}
            />

            {selectedFile ? (
              <div className="space-y-2">
                <span className="text-3xl inline-block" aria-hidden="true">
                  📄
                </span>
                <p className="text-sm font-bold text-emerald-800">{selectedFile.name}</p>
                <p className="text-xs text-gray-500">
                  Size: {(selectedFile.size / 1024).toFixed(1)} KB &bull; Type:{' '}
                  {selectedFile.name.toLowerCase().endsWith('.csv') ? 'CSV' : 'JSON / NDJSON'}
                </p>
                <span className="text-xs text-navy font-semibold underline block pt-1">
                  Click to select a different file
                </span>
              </div>
            ) : (
              <div className="space-y-2">
                <span className="text-3xl inline-block text-gray-400" aria-hidden="true">
                  📥
                </span>
                <p className="text-sm font-semibold text-gray-700">
                  Drag and drop your dataset (CSV or JSON) here, or <span className="text-navy underline">browse</span>
                </p>
                <p className="text-xs text-gray-400">
                  Supported formats: UTF-8 CSV, JSON array ([&#123;...&#125;]), or NDJSON (max 10MB)
                </p>
              </div>
            )}
          </div>

          {uploadError && (
            <div className="p-3 bg-red-50 border border-red-200 rounded text-xs text-red-700">
              <span className="font-semibold">Upload Error:</span> {uploadError}
            </div>
          )}

          <button
            onClick={handleUploadSubmit}
            disabled={!selectedFile || uploadMutation.isPending}
            className="btn-primary w-full py-3 text-sm font-semibold disabled:opacity-40 shadow-sm"
          >
            {uploadMutation.isPending ? 'Uploading & Staging...' : 'Upload & Stage Import Batch'}
          </button>
        </div>

        {/* Status & Processing Panel */}
        <div className="card p-6 bg-white shadow-sm space-y-4">
          <div className="flex items-center justify-between pb-2 border-b border-gray-100">
            <h2 className="text-base font-bold text-navy">2. Processing Monitor</h2>
            {activeBatchId && (
              <button
                onClick={handleClearActiveBatch}
                className="text-xs text-gray-400 hover:text-gray-600 underline"
              >
                Clear / New Import
              </button>
            )}
          </div>

          {activeBatchId ? (
            <div className="space-y-4">
              {/* Batch Metadata Header */}
              <div className="p-3 bg-gray-50 border border-gray-200 rounded-lg flex items-center justify-between text-xs">
                <div>
                  <span className="text-gray-500">Active Batch Code:</span>{' '}
                  <span className="font-mono font-bold text-navy">
                    {statusData?.batchCode || `ID #${activeBatchId}`}
                  </span>
                </div>
                <div>
                  <span
                    className={`inline-block px-2.5 py-0.5 text-xs font-semibold rounded-full ${
                      statusData?.status === 'COMPLETED'
                        ? 'bg-emerald-100 text-emerald-800'
                        : statusData?.status === 'RUNNING'
                        ? 'bg-blue-100 text-blue-800 animate-pulse'
                        : statusData?.status === 'FAILED'
                        ? 'bg-red-100 text-red-800'
                        : statusData?.status === 'PARTIALLY_COMPLETED'
                        ? 'bg-amber-100 text-amber-800'
                        : 'bg-gray-200 text-gray-800'
                    }`}
                  >
                    {statusData?.status || 'STAGED / PENDING'}
                  </span>
                </div>
              </div>

              {/* Polling Stalled / Timed Out Alerts */}
              {isTimedOut && !isTerminal && (
                <div className="p-3 bg-amber-50 border border-amber-200 rounded text-xs text-amber-800 space-y-1">
                  <div className="font-semibold flex items-center gap-1.5">
                    <span aria-hidden="true">⏱️</span>
                    <span>Monitoring Budget Reached (Polling Paused)</span>
                  </div>
                  <p className="text-[11px] text-amber-700">
                    Background worker is processing. Polling has been paused to protect bandwidth.
                  </p>
                  <div className="pt-1 flex gap-2">
                    <button
                      type="button"
                      onClick={handleResumePolling}
                      className="btn-outline text-xs py-1 px-2.5 bg-white border-amber-300 text-amber-800 hover:bg-amber-100"
                    >
                      Resume Monitoring
                    </button>
                    <button
                      type="button"
                      onClick={() => statusQuery.refetch()}
                      className="btn-outline text-xs py-1 px-2.5 bg-white border-gray-300 text-gray-700 hover:bg-gray-100"
                    >
                      Refresh Now
                    </button>
                  </div>
                </div>
              )}

              {isPollingPaused && !isTimedOut && !isTerminal && (
                <div className="p-3 bg-red-50 border border-red-200 rounded text-xs text-red-800 space-y-1">
                  <div className="font-semibold flex items-center gap-1.5">
                    <span aria-hidden="true">⚠️</span>
                    <span>Monitoring Paused: Repeated Network Failures</span>
                  </div>
                  <p className="text-[11px] text-red-700">
                    {lastNetworkError || 'Multiple requests failed. Check connectivity or server status.'}
                  </p>
                  <div className="pt-1 flex gap-2">
                    <button
                      type="button"
                      onClick={handleResumePolling}
                      className="btn-outline text-xs py-1 px-2.5 bg-white border-red-300 text-red-800 hover:bg-red-100"
                    >
                      Retry / Resume
                    </button>
                  </div>
                </div>
              )}

              {/* Progress Bar */}
              <div className="space-y-1 text-xs">
                <div className="flex justify-between font-semibold text-gray-700">
                  <span>Reconciliation Progress:</span>
                  <span>{progressPct}% ({processedRows} / {totalRows} rows)</span>
                </div>
                <div className="w-full bg-gray-200 rounded-full h-2.5 overflow-hidden">
                  <div
                    className={`h-2.5 rounded-full transition-all duration-500 ${
                      statusData?.status === 'FAILED'
                        ? 'bg-red-500'
                        : statusData?.status === 'COMPLETED'
                        ? 'bg-emerald-500'
                        : 'bg-navy'
                    }`}
                    style={{ width: `${progressPct}%` }}
                  />
                </div>
              </div>

              {/* Real-time Metrics Grid */}
              <div className="grid grid-cols-2 sm:grid-cols-4 gap-2 text-center text-xs">
                <div className="p-2.5 bg-gray-50 border border-gray-100 rounded">
                  <span className="text-gray-400 block text-[10px]">TOTAL ROWS</span>
                  <span className="font-bold text-gray-800 text-base">{totalRows}</span>
                </div>
                <div className="p-2.5 bg-emerald-50 border border-emerald-100 rounded">
                  <span className="text-emerald-600 block text-[10px]">SUCCESSFUL</span>
                  <span className="font-bold text-emerald-800 text-base">{statusData?.successCount ?? 0}</span>
                </div>
                <div className="p-2.5 bg-red-50 border border-red-100 rounded">
                  <span className="text-red-600 block text-[10px]">QUARANTINED</span>
                  <span className="font-bold text-red-800 text-base">{statusData?.failureCount ?? 0}</span>
                </div>
                <div className="p-2.5 bg-amber-50 border border-amber-100 rounded">
                  <span className="text-amber-600 block text-[10px]">DUPLICATES</span>
                  <span className="font-bold text-amber-800 text-base">{statusData?.duplicateCount ?? 0}</span>
                </div>
              </div>

              {/* Trigger / Navigation Actions */}
              <div className="space-y-2 pt-2">
                {!isTerminal && statusData?.status !== 'RUNNING' && (
                  <button
                    onClick={handleProcessSubmit}
                    disabled={processMutation.isPending}
                    className="btn-primary w-full py-2.5 text-xs font-semibold shadow-sm"
                  >
                    {processMutation.isPending ? 'Initiating Chunked Worker...' : 'Execute Batch Processing'}
                  </button>
                )}

                {statusData?.status === 'RUNNING' && (
                  <div className="py-2 flex items-center justify-center gap-2 text-xs text-navy font-semibold">
                    <LoadingSpinner size="sm" />
                    <span>Worker actively validating &amp; committing records...</span>
                  </div>
                )}

                {(isTerminal || (statusData?.failureCount ?? 0) > 0 || (statusData?.duplicateCount ?? 0) > 0) && (
                  <Link
                    to={`/provider/quarantine?batchId=${activeBatchId}`}
                    className="btn-outline w-full py-2.5 text-xs font-semibold flex items-center justify-center gap-1.5 border-navy text-navy bg-navy-50/20 hover:bg-navy-50"
                  >
                    <span aria-hidden="true">⚠️</span> Inspect Quarantined Records ({((statusData?.failureCount ?? 0) + (statusData?.duplicateCount ?? 0))} rows)
                  </Link>
                )}
              </div>
            </div>
          ) : (
            <div className="p-12 text-center text-gray-400 text-xs space-y-2">
              <span className="text-3xl block" aria-hidden="true">
                📊
              </span>
              <p className="font-medium text-gray-600">No Active Import Selected</p>
              <p>Upload a CSV or JSON file on the left to begin staging and processing.</p>
            </div>
          )}
        </div>
      </div>
    </div>
  );
}
