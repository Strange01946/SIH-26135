import React, { useState } from 'react';
import { useQuery } from '@tanstack/react-query';
import { getCourses } from '@/api/providerPortalApi';
import { LoadingSpinner } from '@/components/feedback/LoadingSpinner';

export function ProviderCoursesPage(): React.JSX.Element {
  const [page, setPage] = useState(0);
  const [size] = useState(10);
  const [searchCode, setSearchCode] = useState('');
  const [debouncedCode, setDebouncedCode] = useState('');
  const [sectorId, setSectorId] = useState<number | undefined>(undefined);

  const coursesQuery = useQuery({
    queryKey: ['provider', 'courses', page, size, debouncedCode, sectorId],
    queryFn: () =>
      getCourses({
        code: debouncedCode ? debouncedCode.trim() : undefined,
        sectorId,
        page,
        size,
        sort: 'id',
        direction: 'ASC',
      }),
  });

  const handleSearchSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    setDebouncedCode(searchCode);
    setPage(0);
  };

  const handleClearFilters = () => {
    setSearchCode('');
    setDebouncedCode('');
    setSectorId(undefined);
    setPage(0);
  };

  const totalPages = coursesQuery.data?.totalPages ?? 0;
  const totalElements = coursesQuery.data?.totalElements ?? 0;

  return (
    <div className="space-y-6">
      {/* Header */}
      <div className="card p-6 border-l-4 border-l-navy bg-white shadow-sm flex flex-col sm:flex-row sm:items-center justify-between gap-4">
        <div>
          <h1 className="text-2xl font-bold text-navy flex items-center gap-2">
            <span>Approved Courses Directory</span>
            <span className="badge-info text-xs">National Qualification Catalog</span>
          </h1>
          <p className="text-sm text-gray-600 mt-1">
            Authoritative NSQF-aligned job roles and vocational courses available for batch scheduling
          </p>
        </div>
      </div>

      {/* Filter and Search Bar */}
      <div className="card p-4 bg-white shadow-sm">
        <form onSubmit={handleSearchSubmit} className="flex flex-wrap items-center gap-3">
          <div className="flex-1 min-w-[220px]">
            <input
              type="text"
              className="input-field w-full text-sm"
              placeholder="Filter by course code (e.g. CRS-IT-01)..."
              value={searchCode}
              onChange={(e) => setSearchCode(e.target.value)}
            />
          </div>
          <button type="submit" className="btn-primary text-xs py-2 px-4">
            Search
          </button>
          {(debouncedCode || sectorId !== undefined) && (
            <button
              type="button"
              onClick={handleClearFilters}
              className="btn-outline text-xs py-2 px-3 text-gray-600"
            >
              Clear
            </button>
          )}
          <div className="ml-auto text-xs text-gray-500">
            Total Courses: <span className="font-semibold text-gray-800">{totalElements}</span>
          </div>
        </form>
      </div>

      {/* Main Table Content */}
      <div className="card p-6 bg-white shadow-sm">
        {coursesQuery.isLoading ? (
          <div className="py-16 flex flex-col items-center justify-center gap-2">
            <LoadingSpinner size="lg" label="Loading courses" />
            <p className="text-sm text-gray-500">Loading approved courses catalog...</p>
          </div>
        ) : coursesQuery.isError ? (
          <div className="p-6 bg-red-50 border border-red-200 rounded-lg text-center text-red-800">
            <p className="font-semibold mb-2">Failed to load courses</p>
            <p className="text-xs mb-4">
              {(coursesQuery.error as Error)?.message || 'Network error communicating with the backend.'}
            </p>
            <button
              onClick={() => coursesQuery.refetch()}
              className="btn-outline text-xs py-1.5 px-3 bg-white border-red-300 text-red-700"
            >
              Retry
            </button>
          </div>
        ) : coursesQuery.data && coursesQuery.data.content.length > 0 ? (
          <div className="space-y-4">
            <div className="overflow-x-auto">
              <table className="w-full text-left text-sm border-collapse">
                <thead>
                  <tr className="border-b border-gray-200 text-xs font-semibold text-gray-600 bg-gray-50">
                    <th className="py-3 px-3">Course Code</th>
                    <th className="py-3 px-3">Course Name</th>
                    <th className="py-3 px-3">Sector</th>
                    <th className="py-3 px-3">Duration / NSQF</th>
                    <th className="py-3 px-3">Delivery Mode</th>
                    <th className="py-3 px-3">Status</th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-gray-100">
                  {coursesQuery.data.content.map((course) => (
                    <tr key={course.id} className="hover:bg-gray-50/60 transition-colors">
                      <td className="py-3 px-3 font-mono font-semibold text-navy">
                        {course.courseCode}
                      </td>
                      <td className="py-3 px-3 font-medium text-gray-900">
                        {course.courseName}
                        {course.description && (
                          <div className="text-xs text-gray-500 font-normal line-clamp-1 mt-0.5">
                            {course.description}
                          </div>
                        )}
                      </td>
                      <td className="py-3 px-3 text-gray-700 text-xs">
                        <span className="inline-block px-2 py-0.5 rounded bg-gray-100 border border-gray-200 font-medium">
                          {course.sectorName || (course.sectorId ? `Sector #${course.sectorId}` : 'General')}
                        </span>
                      </td>
                      <td className="py-3 px-3 text-xs text-gray-600">
                        <div>
                          <span className="font-semibold text-gray-700">Duration:</span>{' '}
                          {course.durationHours ? `${course.durationHours} hrs` : 'N/A'}
                        </div>
                        {course.nsqfLevel && (
                          <div>
                            <span className="font-semibold text-gray-700">NSQF Level:</span>{' '}
                            {course.nsqfLevel}
                          </div>
                        )}
                      </td>
                      <td className="py-3 px-3 text-xs text-gray-600">
                        {course.deliveryModeName || 'Classroom / Practical'}
                      </td>
                      <td className="py-3 px-3">
                        <span className="inline-block px-2.5 py-0.5 text-xs font-semibold rounded-full bg-emerald-100 text-emerald-800">
                          {course.lifecycleStatusCode || 'APPROVED'}
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
              📚
            </span>
            <p className="text-base font-semibold text-gray-800 mb-1">No Courses Found</p>
            <p className="text-xs text-gray-500 max-w-md mx-auto">
              {debouncedCode
                ? `No courses matching code "${debouncedCode}". Try clearing the search.`
                : 'No approved courses are currently active in the national catalog.'}
            </p>
          </div>
        )}
      </div>
    </div>
  );
}
