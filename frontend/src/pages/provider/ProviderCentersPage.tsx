import React, { useState } from 'react';
import { useQuery } from '@tanstack/react-query';
import { useAuth } from '@/context/AuthContext';
import { getTrainingCenters } from '@/api/providerPortalApi';
import { LoadingSpinner } from '@/components/feedback/LoadingSpinner';

export function ProviderCentersPage(): React.JSX.Element {
  const { user } = useAuth();
  const providerId = user?.trainingProviderId ?? undefined;

  const [page, setPage] = useState(0);
  const [size] = useState(10);
  const [searchCode, setSearchCode] = useState('');
  const [debouncedCode, setDebouncedCode] = useState('');

  const centersQuery = useQuery({
    queryKey: ['provider', 'centers', providerId, page, size, debouncedCode],
    queryFn: () =>
      getTrainingCenters({
        providerId,
        code: debouncedCode ? debouncedCode.trim() : undefined,
        page,
        size,
        sort: 'id',
        direction: 'ASC',
      }),
    enabled: !!providerId,
  });

  const handleSearchSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    setDebouncedCode(searchCode);
    setPage(0);
  };

  const handleClearSearch = () => {
    setSearchCode('');
    setDebouncedCode('');
    setPage(0);
  };

  const totalPages = centersQuery.data?.totalPages ?? 0;
  const totalElements = centersQuery.data?.totalElements ?? 0;

  return (
    <div className="space-y-6">
      {/* Header */}
      <div className="card p-6 border-l-4 border-l-navy bg-white shadow-sm flex flex-col sm:flex-row sm:items-center justify-between gap-4">
        <div>
          <h1 className="text-2xl font-bold text-navy flex items-center gap-2">
            <span>Training Centers Directory</span>
            <span className="badge-info text-xs">Provider Scoped</span>
          </h1>
          <p className="text-sm text-gray-600 mt-1">
            Authoritative affiliated operational training locations under your organization
          </p>
        </div>
        <div className="text-xs text-gray-500 font-mono bg-gray-50 border border-gray-200 rounded p-2">
          Provider ID: {providerId ?? 'Unresolved'}
        </div>
      </div>

      {/* Filter and Search Bar */}
      <div className="card p-4 bg-white shadow-sm">
        <form onSubmit={handleSearchSubmit} className="flex flex-wrap items-center gap-3">
          <div className="flex-1 min-w-[240px]">
            <input
              type="text"
              className="input-field w-full text-sm"
              placeholder="Search by exact center code (e.g. TC-DEL-NORTH)..."
              value={searchCode}
              onChange={(e) => setSearchCode(e.target.value)}
            />
          </div>
          <button type="submit" className="btn-primary text-xs py-2 px-4">
            Search
          </button>
          {debouncedCode && (
            <button
              type="button"
              onClick={handleClearSearch}
              className="btn-outline text-xs py-2 px-3 text-gray-600"
            >
              Clear
            </button>
          )}
          <div className="ml-auto text-xs text-gray-500">
            Total Centers: <span className="font-semibold text-gray-800">{totalElements}</span>
          </div>
        </form>
      </div>

      {/* Main Table Content */}
      <div className="card p-6 bg-white shadow-sm">
        {centersQuery.isLoading ? (
          <div className="py-16 flex flex-col items-center justify-center gap-2">
            <LoadingSpinner size="lg" label="Loading centers" />
            <p className="text-sm text-gray-500">Loading affiliated training centers...</p>
          </div>
        ) : centersQuery.isError ? (
          <div className="p-6 bg-red-50 border border-red-200 rounded-lg text-center text-red-800">
            <p className="font-semibold mb-2">Failed to load training centers</p>
            <p className="text-xs mb-4">
              {(centersQuery.error as Error)?.message || 'Network error communicating with the backend.'}
            </p>
            <button
              onClick={() => centersQuery.refetch()}
              className="btn-outline text-xs py-1.5 px-3 bg-white border-red-300 text-red-700"
            >
              Retry
            </button>
          </div>
        ) : (centersQuery.data?.content ?? []).length > 0 ? (
          <div className="space-y-4">
            <div className="overflow-x-auto">
              <table className="w-full text-left text-sm border-collapse">
                <thead>
                  <tr className="border-b border-gray-200 text-xs font-semibold text-gray-600 bg-gray-50">
                    <th className="py-3 px-3">Center Code</th>
                    <th className="py-3 px-3">Center Name</th>
                    <th className="py-3 px-3">Location / Jurisdiction</th>
                    <th className="py-3 px-3">Contact</th>
                    <th className="py-3 px-3">Affiliation Status</th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-gray-100">
                  {(centersQuery.data?.content ?? []).map((center) => (
                    <tr key={center.id} className="hover:bg-gray-50/60 transition-colors">
                      <td className="py-3 px-3 font-mono font-semibold text-navy">
                        {center.centerCode}
                      </td>
                      <td className="py-3 px-3 font-medium text-gray-900">
                        {center.centerName}
                        {center.addressLine1 && (
                          <div className="text-xs text-gray-500 font-normal mt-0.5">
                            {center.addressLine1} {center.pincode ? `(${center.pincode})` : ''}
                          </div>
                        )}
                      </td>
                      <td className="py-3 px-3 text-gray-600 text-xs">
                        <div>
                          <span className="font-semibold text-gray-700">District:</span> {center.districtName || center.districtId}
                        </div>
                        <div>
                          <span className="font-semibold text-gray-700">State:</span> {center.stateName || center.stateId}
                        </div>
                      </td>
                      <td className="py-3 px-3 text-xs text-gray-600">
                        <div>{center.contactPersonName || 'Officer in Charge'}</div>
                        {center.contactEmail && <div className="text-gray-500">{center.contactEmail}</div>}
                        {center.contactPhone && <div className="text-gray-500">{center.contactPhone}</div>}
                      </td>
                      <td className="py-3 px-3">
                        <span className="inline-block px-2.5 py-0.5 text-xs font-semibold rounded-full bg-emerald-100 text-emerald-800">
                          {center.lifecycleStatusCode || 'AFFILIATED'}
                        </span>
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
            <span className="text-3xl mb-2 inline-block" aria-hidden="true">
              🏢
            </span>
            <p className="text-base font-semibold text-gray-800 mb-1">No Training Centers Found</p>
            <p className="text-xs text-gray-500 max-w-md mx-auto">
              {debouncedCode
                ? `No center matches code "${debouncedCode}". Try clearing the search.`
                : 'No affiliated training centers are registered for your provider account.'}
            </p>
          </div>
        )}
      </div>
    </div>
  );
}
