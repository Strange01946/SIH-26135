import React, { useState } from 'react';
import { Link } from 'react-router-dom';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { toast } from 'sonner';
import { useAuth } from '@/context/AuthContext';
import { getTrainingBatches, updateTrainingBatchStatus } from '@/api/providerPortalApi';
import { LoadingSpinner } from '@/components/feedback/LoadingSpinner';

export function ProviderBatchesPage(): React.JSX.Element {
  const { user, hasAuthority } = useAuth();
  const providerId = user?.trainingProviderId ?? undefined;
  const queryClient = useQueryClient();

  const [page, setPage] = useState(0);
  const [size] = useState(10);
  const [statusFilter, setStatusFilter] = useState<number | undefined>(undefined);
  // Queries
  const batchesQuery = useQuery({
    queryKey: ['provider', 'batches', providerId, page, size, statusFilter],
    queryFn: () =>
      getTrainingBatches({
        ...(statusFilter ? { statusId: statusFilter } : { providerId }),
        page,
        size,
        sort: 'id',
        direction: 'DESC',
      }),
    enabled: !!providerId,
    refetchInterval: 15_000,
  });

  // Update Status Mutation
  const updateStatusMutation = useMutation({
    mutationFn: ({ batchId, statusId }: { batchId: number; statusId: number }) =>
      updateTrainingBatchStatus(batchId, { batchStatusId: statusId }),
    onSuccess: (data) => {
      toast.success(`Batch ${data.batchCode} status transitioned to ${data.batchStatusName || data.batchStatusCode}!`);
      queryClient.invalidateQueries({ queryKey: ['provider', 'batches'] });
    },
    onError: (err: any) => {
      const msg = err.response?.data?.message || err.message || 'Failed to update batch status';
      toast.error(msg);
    },
  });

  const totalPages = batchesQuery.data?.totalPages ?? 0;
  const totalElements = batchesQuery.data?.totalElements ?? 0;

  return (
    <div className="space-y-6">
      {/* Header */}
      <div className="card p-6 border-l-4 border-l-navy bg-white shadow-sm flex flex-col sm:flex-row sm:items-center justify-between gap-4">
        <div>
          <h1 className="text-2xl font-bold text-navy flex items-center gap-2">
            <span>Training Batches Workbench</span>
            <span className="badge-info text-xs">Capacity &amp; Lifecycle Controls</span>
          </h1>
          <p className="text-sm text-gray-600 mt-1">
            Authoritative schedule, real-time seat occupancy, and enrollment capacity management
          </p>
        </div>
        {hasAuthority('venue.booking.request') && (
          <Link to="/provider/venue-bookings?view=shared" className="btn-primary self-start">Request a training venue</Link>
        )}
      </div>

      {/* Filter Bar */}
      <div className="card p-4 bg-white shadow-sm flex flex-wrap items-center justify-between gap-3">
        <div className="flex items-center gap-2 text-xs">
          <span className="font-semibold text-gray-700">Filter Status:</span>
          <select
            value={statusFilter ?? ''}
            onChange={(e) => {
              setStatusFilter(e.target.value ? Number(e.target.value) : undefined);
              setPage(0);
            }}
            className="input-field text-xs py-1.5 px-3 min-w-[140px]"
          >
            <option value="">All Statuses</option>
            <option value="1">SCHEDULED</option>
            <option value="2">IN_PROGRESS</option>
            <option value="3">COMPLETED</option>
            <option value="4">CANCELLED</option>
          </select>
          {statusFilter !== undefined && (
            <button
              onClick={() => {
                setStatusFilter(undefined);
                setPage(0);
              }}
              className="text-xs text-navy hover:underline ml-2"
            >
              Reset
            </button>
          )}
        </div>
        <div className="text-xs text-gray-500">
          Total Batches: <span className="font-semibold text-gray-800">{totalElements}</span>
        </div>
      </div>

      {/* Batches Table */}
      <div className="card p-6 bg-white shadow-sm">
        {batchesQuery.isLoading ? (
          <div className="py-16 flex flex-col items-center justify-center gap-2">
            <LoadingSpinner size="lg" label="Loading batches" />
            <p className="text-sm text-gray-500">Loading training batches and occupancy metrics...</p>
          </div>
        ) : batchesQuery.isError ? (
          <div className="p-6 bg-red-50 border border-red-200 rounded-lg text-center text-red-800">
            <p className="font-semibold mb-2">Failed to load training batches</p>
            <p className="text-xs mb-4">
              {(batchesQuery.error as Error)?.message || 'Network error communicating with the backend.'}
            </p>
            <button
              onClick={() => batchesQuery.refetch()}
              className="btn-outline text-xs py-1.5 px-3 bg-white border-red-300 text-red-700"
            >
              Retry
            </button>
          </div>
        ) : batchesQuery.data && batchesQuery.data.content.length > 0 ? (
          <div className="space-y-4">
            <div className="overflow-x-auto">
              <table className="w-full text-left text-sm border-collapse">
                <thead>
                  <tr className="border-b border-gray-200 text-xs font-semibold text-gray-600 bg-gray-50">
                    <th className="py-3 px-3">Batch Code</th>
                    <th className="py-3 px-3">Course</th>
                    <th className="py-3 px-3">Training Center</th>
                    <th className="py-3 px-3">Schedule</th>
                    <th className="py-3 px-3">Authoritative Seat Occupancy</th>
                    <th className="py-3 px-3">Status</th>
                    <th className="py-3 px-3">Lifecycle Actions</th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-gray-100">
                  {batchesQuery.data.content.map((batch) => {
                    const capacity = batch.capacity || 0;
                    const enrolled = batch.enrolledCount ?? 0;
                    const available = batch.availableSeats ?? Math.max(0, capacity - enrolled);
                    const pct = capacity > 0 ? Math.min(100, Math.round((enrolled / capacity) * 100)) : 0;
                    const isFull = available === 0;
                    const isNearFull = !isFull && available <= Math.max(1, Math.floor(capacity * 0.2));

                    return (
                      <tr key={batch.id} className="hover:bg-gray-50/60 transition-colors">
                        <td className="py-3 px-3 font-mono font-semibold text-navy">
                          {batch.batchCode}
                        </td>
                        <td className="py-3 px-3 text-gray-800">
                          <div className="font-medium">{batch.courseName || `Course #${batch.courseId}`}</div>
                          {batch.courseCode && (
                            <span className="text-[11px] text-gray-500 font-mono">{batch.courseCode}</span>
                          )}
                        </td>
                        <td className="py-3 px-3 text-gray-600 text-xs">
                          <div className="font-medium text-gray-800">{batch.centerName || `Center #${batch.centerId}`}</div>
                          {batch.centerCode && <span className="font-mono text-gray-400">{batch.centerCode}</span>}
                        </td>
                        <td className="py-3 px-3 text-xs text-gray-600">
                          <div><span className="text-gray-400">Start:</span> {batch.startDate}</div>
                          <div><span className="text-gray-400">End:</span> {batch.endDate}</div>
                        </td>
                        <td className="py-3 px-3">
                          <div className="w-44 space-y-1">
                            <div className="flex justify-between items-center text-xs">
                              <span className="font-semibold text-gray-700">
                                {enrolled} / {capacity}
                              </span>
                              {isFull ? (
                                <span className="px-1.5 py-0.2 bg-red-100 text-red-800 text-[10px] font-bold rounded">
                                  FULL
                                </span>
                              ) : isNearFull ? (
                                <span className="px-1.5 py-0.2 bg-amber-100 text-amber-800 text-[10px] font-semibold rounded">
                                  {available} left
                                </span>
                              ) : (
                                <span className="text-gray-500 text-[11px]">{available} available</span>
                              )}
                            </div>
                            <div className="w-full bg-gray-200 rounded-full h-2 overflow-hidden">
                              <div
                                className={`h-2 rounded-full transition-all duration-300 ${
                                  isFull ? 'bg-red-500' : isNearFull ? 'bg-amber-500' : 'bg-emerald-500'
                                }`}
                                style={{ width: `${pct}%` }}
                              />
                            </div>
                            <div className="text-[10px] text-gray-400 text-right">{pct}% occupancy</div>
                          </div>
                        </td>
                        <td className="py-3 px-3">
                          <span
                            className={`inline-block px-2.5 py-0.5 text-xs font-semibold rounded-full ${
                              batch.batchStatusCode === 'COMPLETED'
                                ? 'bg-blue-100 text-blue-800'
                                : batch.batchStatusCode === 'IN_PROGRESS'
                                ? 'bg-emerald-100 text-emerald-800'
                                : batch.batchStatusCode === 'CANCELLED'
                                ? 'bg-red-100 text-red-800'
                                : 'bg-amber-100 text-amber-800'
                            }`}
                          >
                            {batch.batchStatusName || batch.batchStatusCode || 'SCHEDULED'}
                          </span>
                        </td>
                        <td className="py-3 px-3">
                          {batch.batchStatusCode === 'SCHEDULED' && (
                            <button
                              onClick={() => updateStatusMutation.mutate({ batchId: batch.id, statusId: 2 })}
                              disabled={updateStatusMutation.isPending}
                              className="btn-outline text-[11px] py-1 px-2.5 border-emerald-500 text-emerald-700 hover:bg-emerald-50"
                            >
                              Start Batch
                            </button>
                          )}
                          {batch.batchStatusCode === 'IN_PROGRESS' && (
                            <button
                              onClick={() => updateStatusMutation.mutate({ batchId: batch.id, statusId: 3 })}
                              disabled={updateStatusMutation.isPending}
                              className="btn-outline text-[11px] py-1 px-2.5 border-blue-500 text-blue-700 hover:bg-blue-50"
                            >
                              Complete
                            </button>
                          )}
                          {batch.batchStatusCode === 'COMPLETED' && (
                            <span className="text-xs text-gray-400 italic">Finalized</span>
                          )}
                        </td>
                      </tr>
                    );
                  })}
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
            <span className="text-3xl mb-2 inline-block" aria-hidden="true">
              🗓️
            </span>
            <p className="text-base font-semibold text-gray-800 mb-1">No Training Batches Found</p>
            <p className="text-xs text-gray-500 max-w-md mx-auto mb-4">
              Approved venue bookings appear here as training batches.
            </p>

          </div>
        )}
      </div>

    </div>
  );
}
