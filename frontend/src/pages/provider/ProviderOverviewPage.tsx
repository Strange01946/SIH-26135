import React from 'react';
import { useQuery } from '@tanstack/react-query';
import { Link } from 'react-router-dom';
import { useAuth } from '@/context/AuthContext';
import {
  getTrainingCenters,
  getCourses,
  getTrainingBatches,
  getTrainingEnrollments,
} from '@/api/providerPortalApi';
import { LoadingSpinner } from '@/components/feedback/LoadingSpinner';

export function ProviderOverviewPage(): React.JSX.Element {
  const { user } = useAuth();
  const providerId = user?.trainingProviderId ?? undefined;

  // 1. Fetch provider centers count
  const centersQuery = useQuery({
    queryKey: ['provider', 'centers', providerId],
    queryFn: () => getTrainingCenters({ providerId, size: 5 }),
    enabled: !!providerId,
  });

  // 2. Fetch courses count
  const coursesQuery = useQuery({
    queryKey: ['provider', 'courses'],
    queryFn: () => getCourses({ size: 5 }),
  });

  // 3. Fetch batches
  const batchesQuery = useQuery({
    queryKey: ['provider', 'batches', providerId],
    queryFn: () => getTrainingBatches({ providerId, size: 5, sort: 'id', direction: 'DESC' }),
    enabled: !!providerId,
  });

  // 4. Fetch enrollments count
  const enrollmentsQuery = useQuery({
    queryKey: ['provider', 'enrollments', providerId],
    queryFn: () => getTrainingEnrollments({ providerId, size: 5 }),
    enabled: !!providerId,
  });

  const isLoading = centersQuery.isLoading || batchesQuery.isLoading;

  return (
    <div className="space-y-6">
      {/* Provider Header Banner */}
      <div className="card p-6 border-l-4 border-l-navy bg-white shadow-sm">
        <div className="flex flex-col md:flex-row md:items-center justify-between gap-4">
          <div>
            <div className="flex items-center gap-2 mb-1">
              <h1 className="text-2xl font-bold text-navy">Training Provider Portal</h1>
              <span className="badge-info font-semibold">ROLE_TRAINING_PROVIDER</span>
            </div>
            <p className="text-sm text-gray-600">
              Operations Console for Affiliated Centers, Batch Management, Candidate Enrollment &amp; Bulk Ingestion
            </p>
          </div>

          {/* User Session Metadata Badge */}
          {user && (
            <div className="bg-gray-50 border border-gray-200 rounded-md p-3 text-xs space-y-1 text-gray-700">
              <div>
                <span className="font-semibold text-gray-900">Signed In As:</span> {user.username} (ID: {user.id})
              </div>
              {user.email && (
                <div>
                  <span className="font-semibold text-gray-900">Email:</span> {user.email}
                </div>
              )}
              <div className="pt-1 text-[11px] text-gray-500 font-mono">
                {user.trainingProviderId ? `Provider ID: ${user.trainingProviderId}` : 'No Provider Linked'}
                {user.stateId ? ` | State: ${user.stateId}` : ''}
                {user.districtId ? ` | District: ${user.districtId}` : ''}
              </div>
            </div>
          )}
        </div>
      </div>

      {/* Metric Cards Grid */}
      <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4">
        {/* Centers Metric */}
        <Link
          to="/provider/centers"
          className="card p-5 hover:border-navy hover:shadow-md transition-all group"
        >
          <div className="flex items-center justify-between mb-2">
            <span className="text-xs font-semibold uppercase tracking-wider text-gray-500">
              Affiliated Centers
            </span>
            <span className="text-xl group-hover:scale-110 transition-transform" aria-hidden="true">
              🏢
            </span>
          </div>
          <div className="text-2xl font-bold text-navy">
            {centersQuery.isLoading ? '...' : (centersQuery.data?.totalElements ?? 0)}
          </div>
          <p className="text-xs text-gray-500 mt-1">Operational training locations</p>
        </Link>

        {/* Courses Metric */}
        <Link
          to="/provider/courses"
          className="card p-5 hover:border-navy hover:shadow-md transition-all group"
        >
          <div className="flex items-center justify-between mb-2">
            <span className="text-xs font-semibold uppercase tracking-wider text-gray-500">
              Approved Courses
            </span>
            <span className="text-xl group-hover:scale-110 transition-transform" aria-hidden="true">
              📚
            </span>
          </div>
          <div className="text-2xl font-bold text-navy">
            {coursesQuery.isLoading ? '...' : (coursesQuery.data?.totalElements ?? 0)}
          </div>
          <p className="text-xs text-gray-500 mt-1">National skill curriculum catalog</p>
        </Link>

        {/* Batches Metric */}
        <Link
          to="/provider/batches"
          className="card p-5 hover:border-navy hover:shadow-md transition-all group"
        >
          <div className="flex items-center justify-between mb-2">
            <span className="text-xs font-semibold uppercase tracking-wider text-gray-500">
              Training Batches
            </span>
            <span className="text-xl group-hover:scale-110 transition-transform" aria-hidden="true">
              🗓️
            </span>
          </div>
          <div className="text-2xl font-bold text-navy">
            {batchesQuery.isLoading ? '...' : (batchesQuery.data?.totalElements ?? 0)}
          </div>
          <p className="text-xs text-gray-500 mt-1">Scheduled and active batches</p>
        </Link>

        {/* Enrollments Metric */}
        <Link
          to="/provider/enrollment"
          className="card p-5 hover:border-navy hover:shadow-md transition-all group"
        >
          <div className="flex items-center justify-between mb-2">
            <span className="text-xs font-semibold uppercase tracking-wider text-gray-500">
              Enrolled Trainees
            </span>
            <span className="text-xl group-hover:scale-110 transition-transform" aria-hidden="true">
              👥
            </span>
          </div>
          <div className="text-2xl font-bold text-navy">
            {enrollmentsQuery.isLoading ? '...' : (enrollmentsQuery.data?.totalElements ?? 0)}
          </div>
          <p className="text-xs text-gray-500 mt-1">Total active candidates</p>
        </Link>
      </div>

      {/* Quick Action Navigation Grid */}
      <div className="grid grid-cols-2 md:grid-cols-6 gap-3">
        <Link
          to="/provider/batches"
          className="btn-primary py-2.5 px-3 flex items-center justify-center gap-1.5 text-xs font-semibold rounded-md shadow-sm"
        >
          <span aria-hidden="true">➕</span> Create Batch
        </Link>
        <Link
          to="/provider/enrollment"
          className="btn-outline py-2.5 px-3 flex items-center justify-center gap-1.5 text-xs font-semibold rounded-md shadow-sm bg-white"
        >
          <span aria-hidden="true">📝</span> Enroll Trainee
        </Link>
        <Link
          to="/provider/imports"
          className="btn-outline py-2.5 px-3 flex items-center justify-center gap-1.5 text-xs font-semibold rounded-md shadow-sm bg-white"
        >
          <span aria-hidden="true">📤</span> Bulk CSV
        </Link>
        <Link
          to="/provider/quarantine"
          className="btn-outline py-2.5 px-3 flex items-center justify-center gap-1.5 text-xs font-semibold rounded-md shadow-sm bg-white"
        >
          <span aria-hidden="true">⚠️</span> Quarantine
        </Link>
        <Link
          to="/provider/data-quality"
          className="btn-outline py-2.5 px-3 flex items-center justify-center gap-1.5 text-xs font-semibold rounded-md shadow-sm bg-white border-blue-300 text-blue-700 hover:bg-blue-50"
        >
          <span aria-hidden="true">🔍</span> Data Quality
        </Link>
        <Link
          to="/provider/scorecards"
          className="btn-outline py-2.5 px-3 flex items-center justify-center gap-1.5 text-xs font-semibold rounded-md shadow-sm bg-white border-emerald-300 text-emerald-700 hover:bg-emerald-50"
        >
          <span aria-hidden="true">📊</span> Scorecards
        </Link>
      </div>

      {/* Recent Batches Workbench Preview */}
      <div className="card p-6 bg-white shadow-sm">
        <div className="flex items-center justify-between mb-4">
          <div>
            <h2 className="text-base font-bold text-navy">Recent Training Batches &amp; Occupancy</h2>
            <p className="text-xs text-gray-500">Live authoritative seat occupancy and lifecycle status</p>
          </div>
          <Link to="/provider/batches" className="text-xs font-semibold text-navy hover:underline">
            View All Batches &rarr;
          </Link>
        </div>

        {isLoading ? (
          <div className="py-12 flex flex-col items-center justify-center gap-2">
            <LoadingSpinner size="md" label="Loading batch occupancy" />
            <p className="text-sm text-gray-500">Loading batch occupancy data...</p>
          </div>
        ) : batchesQuery.data && batchesQuery.data.content.length > 0 ? (
          <div className="overflow-x-auto">
            <table className="w-full text-left text-sm border-collapse">
              <thead>
                <tr className="border-b border-gray-200 text-xs font-semibold text-gray-600 bg-gray-50">
                  <th className="py-2.5 px-3">Batch Code</th>
                  <th className="py-2.5 px-3">Course</th>
                  <th className="py-2.5 px-3">Center</th>
                  <th className="py-2.5 px-3">Dates</th>
                  <th className="py-2.5 px-3">Seat Occupancy</th>
                  <th className="py-2.5 px-3">Status</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-gray-100">
                {batchesQuery.data.content.map((batch) => {
                  const capacity = batch.capacity || 0;
                  const enrolled = batch.enrolledCount ?? 0;
                  const available = batch.availableSeats ?? Math.max(0, capacity - enrolled);
                  const pct = capacity > 0 ? Math.min(100, Math.round((enrolled / capacity) * 100)) : 0;
                  const isFull = available === 0;

                  return (
                    <tr key={batch.id} className="hover:bg-gray-50/60 transition-colors">
                      <td className="py-3 px-3 font-mono font-semibold text-navy">
                        {batch.batchCode}
                      </td>
                      <td className="py-3 px-3 text-gray-800">
                        {batch.courseName || `Course #${batch.courseId}`}
                      </td>
                      <td className="py-3 px-3 text-gray-600">
                        {batch.centerName || `Center #${batch.centerId}`}
                      </td>
                      <td className="py-3 px-3 text-xs text-gray-500">
                        {batch.startDate} to {batch.endDate}
                      </td>
                      <td className="py-3 px-3">
                        <div className="w-40 space-y-1">
                          <div className="flex justify-between text-xs">
                            <span className="font-semibold text-gray-700">
                              {enrolled} / {capacity}
                            </span>
                            <span className={isFull ? 'text-red-600 font-bold' : 'text-gray-500'}>
                              {isFull ? 'FULL' : `${available} left`}
                            </span>
                          </div>
                          <div className="w-full bg-gray-200 rounded-full h-1.5 overflow-hidden">
                            <div
                              className={`h-1.5 rounded-full ${isFull ? 'bg-red-500' : pct > 80 ? 'bg-amber-500' : 'bg-emerald-500'}`}
                              style={{ width: `${pct}%` }}
                            />
                          </div>
                        </div>
                      </td>
                      <td className="py-3 px-3">
                        <span
                          className={`inline-block px-2 py-0.5 text-xs font-semibold rounded-full ${
                            batch.batchStatusCode === 'COMPLETED'
                              ? 'bg-blue-100 text-blue-800'
                              : batch.batchStatusCode === 'IN_PROGRESS'
                              ? 'bg-emerald-100 text-emerald-800'
                              : 'bg-amber-100 text-amber-800'
                          }`}
                        >
                          {batch.batchStatusName || batch.batchStatusCode || 'SCHEDULED'}
                        </span>
                      </td>
                    </tr>
                  );
                })}
              </tbody>
            </table>
          </div>
        ) : (
          <div className="p-8 text-center border border-dashed border-gray-200 rounded-lg">
            <p className="text-sm text-gray-500 mb-3">No training batches created yet.</p>
            <Link to="/provider/batches" className="btn-primary text-xs py-2 px-4">
              Create Your First Batch
            </Link>
          </div>
        )}
      </div>
    </div>
  );
}
