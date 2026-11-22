import React, { useState } from 'react';
import { useQuery } from '@tanstack/react-query';
import { useSearchParams } from 'react-router-dom';
import {
  getQuarantinedRecords,
  getImportBatchStatus,
  downloadQuarantineExport,
} from '@/api/providerPortalApi';
import type { ApiError } from '@/api/types';
import { LoadingSpinner } from '@/components/feedback/LoadingSpinner';

export function ProviderQuarantinePage(): React.JSX.Element {
  const [searchParams, setSearchParams] = useSearchParams();

  // Active batch ID from query params or manual entry
  const queryBatchId = searchParams.get('batchId') ? Number(searchParams.get('batchId')) : '';
  const [batchIdInput, setBatchIdInput] = useState<number | ''>(queryBatchId);
  const activeBatchId = typeof queryBatchId === 'number' && queryBatchId > 0 ? queryBatchId : null;

  const [page, setPage] = useState(0);
  const [size] = useState(15);
  const [includeDuplicates, setIncludeDuplicates] = useState(true);

  // Authenticated export state
  const [isDownloading, setIsDownloading] = useState(false);
  const [downloadError, setDownloadError] = useState<string | null>(null);
  const [downloadSuccess, setDownloadSuccess] = useState<string | null>(null);

  // Batch Status / Header Query
  const batchStatusQuery = useQuery({
    queryKey: ['provider', 'importStatus', activeBatchId],
    queryFn: () => (activeBatchId ? getImportBatchStatus(activeBatchId) : null),
    enabled: !!activeBatchId,
  });

  // Quarantine Records Query
  const quarantineQuery = useQuery({
    queryKey: ['provider', 'quarantine', activeBatchId, includeDuplicates, page, size],
    queryFn: () => (activeBatchId ? getQuarantinedRecords(activeBatchId, includeDuplicates, page, size) : null),
    enabled: !!activeBatchId,
  });

  const handleBatchSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    if (batchIdInput) {
      setSearchParams({ batchId: String(batchIdInput) });
      setPage(0);
      setDownloadError(null);
      setDownloadSuccess(null);
    }
  };

  const handleDownloadExport = async () => {
    if (!activeBatchId || isDownloading) return;
    setIsDownloading(true);
    setDownloadError(null);
    setDownloadSuccess(null);
    try {
      const blob = await downloadQuarantineExport(activeBatchId, includeDuplicates);
      if (!blob || blob.size === 0) {
        setDownloadError('Exported CSV is empty or no quarantine records found to download.');
        return;
      }
      const url = window.URL.createObjectURL(blob);
      const link = document.createElement('a');
      link.href = url;
      link.setAttribute('download', `import_batch_${activeBatchId}_quarantine.csv`);
      document.body.appendChild(link);
      link.click();
      document.body.removeChild(link);
      window.URL.revokeObjectURL(url);
      setDownloadSuccess('Quarantine CSV exported successfully.');
    } catch (err: unknown) {
      const apiErr = err as ApiError;
      if (apiErr?.status === 401) {
        setDownloadError('Authentication session expired. Please log in again to export.');
      } else if (apiErr?.status === 403) {
        setDownloadError('You do not have permission to export quarantine records for this batch.');
      } else if (apiErr?.status === 404) {
        setDownloadError('The requested import batch or quarantine dataset was not found.');
      } else {
        setDownloadError(apiErr?.message || 'Failed to download quarantine CSV export. Please try again.');
      }
    } finally {
      setIsDownloading(false);
    }
  };

  const totalPages = quarantineQuery.data?.totalPages ?? 0;
  const totalElements = quarantineQuery.data?.totalElements ?? 0;

  return (
    <div className="space-y-6">
      {/* Header */}
      <div className="card p-6 border-l-4 border-l-navy bg-white shadow-sm flex flex-col sm:flex-row sm:items-center justify-between gap-4">
        <div>
          <h1 className="text-2xl font-bold text-navy flex items-center gap-2">
            <span>Import Quarantine Inspection &amp; Error Export</span>
            <span className="badge-info text-xs">CWE-1236 Sanitized</span>
          </h1>
          <p className="text-sm text-gray-600 mt-1">
            Authoritative row-level validation defect tracking, statutory error code resolution, and sanitized CSV exports
          </p>
        </div>

        {activeBatchId && (
          <button
            type="button"
            onClick={handleDownloadExport}
            disabled={isDownloading}
            className="btn-primary py-2.5 px-4 text-xs font-semibold flex items-center gap-2 self-start sm:self-auto shadow-sm disabled:opacity-50"
          >
            {isDownloading ? (
              <>
                <LoadingSpinner size="sm" label="Exporting CSV" />
                <span>Exporting CSV...</span>
              </>
            ) : (
              <>
                <span aria-hidden="true">📥</span>
                <span>Export Error CSV</span>
              </>
            )}
          </button>
        )}
      </div>

      {/* Feedback Alerts */}
      {downloadError && (
        <div className="p-3 bg-red-50 border border-red-200 rounded text-xs text-red-800 flex items-center justify-between">
          <div className="flex items-center gap-2">
            <span aria-hidden="true">⚠️</span>
            <span>{downloadError}</span>
          </div>
          <button
            type="button"
            onClick={() => setDownloadError(null)}
            className="text-red-500 hover:text-red-700 font-bold ml-2 text-sm"
            aria-label="Dismiss download error"
          >
            &times;
          </button>
        </div>
      )}

      {downloadSuccess && (
        <div className="p-3 bg-emerald-50 border border-emerald-200 rounded text-xs text-emerald-800 flex items-center justify-between">
          <div className="flex items-center gap-2">
            <span aria-hidden="true">✓</span>
            <span>{downloadSuccess}</span>
          </div>
          <button
            type="button"
            onClick={() => setDownloadSuccess(null)}
            className="text-emerald-500 hover:text-emerald-700 font-bold ml-2 text-sm"
            aria-label="Dismiss download success"
          >
            &times;
          </button>
        </div>
      )}

      {/* Batch Selector Bar */}
      <div className="card p-4 bg-white shadow-sm flex flex-wrap items-center justify-between gap-4">
        <form onSubmit={handleBatchSubmit} className="flex items-center gap-3">
          <label className="text-xs font-semibold text-gray-700">Import Batch ID:</label>
          <input
            type="number"
            min="1"
            placeholder="e.g. 501"
            className="input-field text-xs py-1.5 px-3 w-32 font-mono"
            value={batchIdInput}
            onChange={(e) => setBatchIdInput(e.target.value ? Number(e.target.value) : '')}
          />
          <button type="submit" className="btn-primary text-xs py-1.5 px-3">
            Load Records
          </button>
        </form>

        <div className="flex items-center gap-4 text-xs">
          <label className="flex items-center gap-1.5 text-gray-700 font-medium cursor-pointer">
            <input
              type="checkbox"
              checked={includeDuplicates}
              onChange={(e) => {
                setIncludeDuplicates(e.target.checked);
                setPage(0);
              }}
              className="rounded text-navy focus:ring-navy"
            />
            Include Duplicate Rows
          </label>

          {activeBatchId && (
            <div className="text-gray-500 font-mono">
              Quarantined: <span className="font-bold text-red-700">{totalElements}</span> rows
            </div>
          )}
        </div>
      </div>

      {/* Batch Summary Bar */}
      {batchStatusQuery.data && (
        <div className="p-4 bg-gray-50 border border-gray-200 rounded-lg flex flex-wrap items-center justify-between gap-4 text-xs">
          <div>
            <span className="text-gray-500">Batch Code:</span>{' '}
            <span className="font-mono font-bold text-navy">{batchStatusQuery.data.batchCode}</span>
          </div>
          <div>
            <span className="text-gray-500">Entity:</span>{' '}
            <span className="font-semibold text-gray-800">{batchStatusQuery.data.entityType}</span>
          </div>
          <div>
            <span className="text-gray-500">Total Rows:</span>{' '}
            <span className="font-semibold text-gray-800">{batchStatusQuery.data.rowCount}</span>
          </div>
          <div>
            <span className="text-gray-500">Failed / Quarantined:</span>{' '}
            <span className="font-semibold text-red-600">{batchStatusQuery.data.failureCount}</span>
          </div>
          <div>
            <span className="text-gray-500">Duplicates:</span>{' '}
            <span className="font-semibold text-amber-600">{batchStatusQuery.data.duplicateCount}</span>
          </div>
          <div>
            <span className="text-gray-500">Status:</span>{' '}
            <span className="font-bold text-gray-800">{batchStatusQuery.data.status}</span>
          </div>
        </div>
      )}

      {/* Quarantine Grid */}
      <div className="card p-6 bg-white shadow-sm">
        {!activeBatchId ? (
          <div className="p-12 text-center text-gray-400 text-xs space-y-2">
            <span className="text-3xl block" aria-hidden="true">
              🔍
            </span>
            <p className="font-medium text-gray-600">No Import Batch Specified</p>
            <p>Enter an Import Batch ID above or navigate from the Bulk Ingestion workbench.</p>
          </div>
        ) : quarantineQuery.isLoading ? (
          <div className="py-16 flex flex-col items-center justify-center gap-2">
            <LoadingSpinner size="lg" label="Loading quarantine" />
            <p className="text-sm text-gray-500">Loading quarantined records...</p>
          </div>
        ) : quarantineQuery.isError ? (
          <div className="p-6 bg-red-50 border border-red-200 rounded-lg text-center text-red-800">
            <p className="font-semibold mb-2">Failed to load quarantine records</p>
            <p className="text-xs mb-4">
              {(quarantineQuery.error as Error)?.message || 'Network error communicating with the backend.'}
            </p>
            <button
              onClick={() => quarantineQuery.refetch()}
              className="btn-outline text-xs py-1.5 px-3 bg-white border-red-300 text-red-700"
            >
              Retry
            </button>
          </div>
        ) : quarantineQuery.data && quarantineQuery.data.content.length > 0 ? (
          <div className="space-y-4">
            <div className="overflow-x-auto">
              <table className="w-full text-left text-xs border-collapse">
                <thead>
                  <tr className="border-b border-gray-200 text-gray-600 bg-gray-50">
                    <th className="py-3 px-3">Row #</th>
                    <th className="py-3 px-3">Entity</th>
                    <th className="py-3 px-3">Outcome Status</th>
                    <th className="py-3 px-3">Exact Error Code</th>
                    <th className="py-3 px-3">Validation Message / Reason</th>
                    <th className="py-3 px-3">Logged At</th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-gray-100">
                  {quarantineQuery.data.content.map((rec) => (
                    <tr key={rec.id} className="hover:bg-gray-50/60 transition-colors">
                      <td className="py-3 px-3 font-mono font-bold text-gray-800">
                        {rec.sourceRowNumber}
                      </td>
                      <td className="py-3 px-3 text-gray-600 font-medium">
                        {rec.entityType || 'Trainee'}
                      </td>
                      <td className="py-3 px-3">
                        <span
                          className={`inline-block px-2 py-0.5 rounded text-[10px] font-bold ${
                            rec.importRecordStatusCode === 'DUPLICATE'
                              ? 'bg-amber-100 text-amber-800 border border-amber-200'
                              : 'bg-red-100 text-red-800 border border-red-200'
                          }`}
                        >
                          {rec.importRecordStatusCode || 'FAILED'}
                        </span>
                      </td>
                      <td className="py-3 px-3 font-mono font-semibold text-red-700">
                        {rec.errorCode || 'VALIDATION_FAILED'}
                      </td>
                      <td className="py-3 px-3 text-gray-800 max-w-md">
                        {rec.errorMessage || 'Field format constraint violation'}
                      </td>
                      <td className="py-3 px-3 text-gray-400 font-mono text-[11px]">
                        {rec.createdAt ? rec.createdAt.replace('T', ' ').slice(0, 19) : 'N/A'}
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>

            {/* Pagination Controls */}
            {totalPages > 1 && (
              <div className="flex items-center justify-between pt-4 border-t border-gray-100 text-xs">
                <span className="text-gray-500">
                  Showing page <span className="font-semibold text-gray-800">{page + 1}</span> of{' '}
                  <span className="font-semibold text-gray-800">{totalPages}</span>
                </span>
                <div className="flex gap-2">
                  <button
                    onClick={() => setPage((p) => Math.max(0, p - 1))}
                    disabled={page === 0}
                    className="btn-outline py-1 px-3 disabled:opacity-40"
                  >
                    Previous
                  </button>
                  <button
                    onClick={() => setPage((p) => Math.min(totalPages - 1, p + 1))}
                    disabled={page >= totalPages - 1}
                    className="btn-outline py-1 px-3 disabled:opacity-40"
                  >
                    Next
                  </button>
                </div>
              </div>
            )}
          </div>
        ) : (
          <div className="p-12 text-center border border-dashed border-gray-200 rounded-lg">
            <span className="text-3xl mb-2 inline-block text-emerald-600" aria-hidden="true">
              ✓
            </span>
            <p className="text-base font-semibold text-gray-800 mb-1">No Quarantine Records</p>
            <p className="text-xs text-gray-500 max-w-md mx-auto">
              This import batch has zero quarantined rows. All submitted dataset records passed validation.
            </p>
          </div>
        )}
      </div>
    </div>
  );
}
