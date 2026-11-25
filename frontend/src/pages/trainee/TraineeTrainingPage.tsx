import React, { useState } from 'react';
import { Link, useOutletContext } from 'react-router-dom';
import { useQuery } from '@tanstack/react-query';
import type {
  TraineeProfile,
  TrainingEnrollment,
  AttendanceRecord,
} from '@/types/trainee';
import { getTraineeEnrollments, getTraineeAttendance } from '@/api/traineeApi';
import { LoadingSpinner } from '@/components/feedback/LoadingSpinner';
import { EmptyState } from '@/components/feedback/EmptyState';
import { ErrorState } from '@/components/feedback/ErrorState';
import { simulationActive } from '@/lib/demoEnvironment';
import { careerDate } from '@/components/career/CareerInsights';

interface TraineeContextType {
  trainee: TraineeProfile | null;
  traineeId?: number;
  isUnlinked: boolean;
  isLoading: boolean;
}

export interface AttendanceStats {
  totalSessions: number;
  presentCount: number;
  absentCount: number;
  attendanceRate: number | null;
}

export function calculateAttendanceStats(
  records: AttendanceRecord[],
): AttendanceStats {
  if (!records || records.length === 0) {
    return {
      totalSessions: 0,
      presentCount: 0,
      absentCount: 0,
      attendanceRate: null,
    };
  }

  const totalSessions = records.length;
  const presentCount = records.filter((r) => Boolean(r.countsAsPresent)).length;
  const absentCount = totalSessions - presentCount;
  const attendanceRate =
    totalSessions > 0 ? Math.round((presentCount / totalSessions) * 100) : null;

  return {
    totalSessions,
    presentCount,
    absentCount,
    attendanceRate,
  };
}

export const TraineeTrainingPage: React.FC = () => {
  const { traineeId, isUnlinked, isLoading: isIdentityLoading } =
    useOutletContext<TraineeContextType>();

  const [selectedEnrollmentId, setSelectedEnrollmentId] = useState<number | 'ALL'>('ALL');
  const [page, setPage] = useState(0);

  // Query enrollments
  const enrollmentsQuery = useQuery({
    queryKey: ['trainee', 'enrollments', traineeId, 'training', page],
    queryFn: () => getTraineeEnrollments(traineeId!, page, 20, true),
    enabled: Boolean(traineeId),
    refetchOnMount: 'always',
    refetchInterval: simulationActive ? false : 15_000,
    retry: false,
  });

  // Query attendance
  const attendanceQuery = useQuery({
    queryKey: ['trainee', 'attendance', traineeId],
    queryFn: () => getTraineeAttendance(traineeId!),
    enabled: Boolean(traineeId),
    refetchOnMount: 'always',
    refetchInterval: simulationActive ? false : 15_000,
    retry: false,
  });

  if (isIdentityLoading) {
    return (
      <div className="bg-white rounded-lg p-12 border border-gov-neutral-200 flex justify-center">
        <LoadingSpinner size="md" />
      </div>
    );
  }

  if (isUnlinked || !traineeId) {
    return (
      <div className="bg-white rounded-lg p-8 border border-gov-neutral-200">
        <EmptyState
          title="Training Record Restricted"
          description="A linked candidate identification number is required to access enrolled training batches and attendance histories."
        />
      </div>
    );
  }

  const enrollments = enrollmentsQuery.data?.content ?? [];
  const attendanceRecords = attendanceQuery.data ?? [];

  // Filter attendance by selected enrollment if not 'ALL'
  const filteredAttendance =
    selectedEnrollmentId === 'ALL'
      ? attendanceRecords
      : attendanceRecords.filter((r) => r.enrollmentId === selectedEnrollmentId);

  // Calculate factual attendance statistics
  const { totalSessions, presentCount, absentCount, attendanceRate } =
    calculateAttendanceStats(filteredAttendance);

  return (
    <div className="space-y-6">
      <header className="flex flex-wrap items-center justify-between gap-3">
        <h1 className="page-title">My Training &amp; Attendance</h1>
        {!simulationActive && <Link className="btn-primary" to="/trainee/find-training">Find training</Link>}
      </header>
      {/* Attendance Metrics Header */}
      <section
        aria-labelledby="attendance-summary-heading"
        className="bg-white rounded-lg shadow-sm border border-gov-neutral-200 p-6"
      >
        <div className="flex flex-col sm:flex-row sm:items-center justify-between pb-4 border-b border-gov-neutral-200 gap-4">
          <div>
            <h2
              id="attendance-summary-heading"
              className="text-lg font-bold text-gov-neutral-900"
            >
              Training Attendance Records
            </h2>
            <p className="text-xs text-gov-neutral-500">
              Verified daily session records logged by authorized training center instructors.
            </p>
          </div>
          {attendanceRate !== null && (
            <span className="px-3 py-1 rounded-full text-xs font-semibold bg-gov-neutral-100 text-gov-neutral-800 border border-gov-neutral-300">
              Recorded Rate: {attendanceRate}%
            </span>
          )}
        </div>

        {/* Attendance Summary Stat Row */}
        <div className="grid grid-cols-2 sm:grid-cols-4 gap-4 pt-5">
          <div className="bg-gov-neutral-50 p-4 rounded-lg border border-gov-neutral-200">
            <span className="text-xs font-semibold uppercase text-gov-neutral-500">
              Total Logged Sessions
            </span>
            <p className="text-2xl font-bold text-gov-neutral-900 mt-1 font-mono">
              {totalSessions}
            </p>
          </div>
          <div className="bg-emerald-50 p-4 rounded-lg border border-emerald-200">
            <span className="text-xs font-semibold uppercase text-emerald-700">
              Sessions Present
            </span>
            <p className="text-2xl font-bold text-emerald-800 mt-1 font-mono">
              {presentCount}
            </p>
          </div>
          <div className="bg-amber-50 p-4 rounded-lg border border-amber-200">
            <span className="text-xs font-semibold uppercase text-amber-700">
              Sessions Absent
            </span>
            <p className="text-2xl font-bold text-amber-800 mt-1 font-mono">
              {absentCount}
            </p>
          </div>
          <div className="bg-gov-neutral-50 p-4 rounded-lg border border-gov-neutral-200">
            <span className="text-xs font-semibold uppercase text-gov-neutral-500">
              Recorded Attendance Rate
            </span>
            <p className="text-2xl font-extrabold mt-1 font-mono text-gov-neutral-900">
              {attendanceRate !== null ? `${attendanceRate}%` : '—'}
            </p>
          </div>
        </div>
      </section>

      {/* Enrollments & Batches Section */}
      <section
        aria-labelledby="enrollment-list-heading"
        className="bg-white rounded-lg shadow-sm border border-gov-neutral-200 p-6"
      >
        <div className="pb-4 border-b border-gov-neutral-200 mb-4">
          <h3
            id="enrollment-list-heading"
            className="text-base font-bold text-gov-neutral-900"
          >
            Enrolled Training Batches &amp; Schemes
          </h3>
          <p className="text-xs text-gov-neutral-500">
            Your training registrations and session timetables.
          </p>
        </div>

        {enrollmentsQuery.isLoading ? (
          <div className="py-8 flex justify-center">
            <LoadingSpinner size="md" />
          </div>
        ) : enrollmentsQuery.isError ? (
          <ErrorState error={enrollmentsQuery.error} title="Unable to load enrollments" onRetry={() => void enrollmentsQuery.refetch()} />
        ) : enrollments.length === 0 ? (
          <EmptyState
            title="No Enrollments Found"
            description="There are currently no training course enrollments recorded for this candidate."
          />
        ) : (
          <div className="space-y-4">
            <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
              {enrollments.map((enr: TrainingEnrollment) => (
                <div
                  key={enr.id}
                  className={`p-4 rounded-lg border transition-all ${
                    selectedEnrollmentId === enr.id
                      ? 'border-gov-navy bg-gov-neutral-50 shadow-sm ring-1 ring-gov-navy'
                      : 'border-gov-neutral-200 bg-white hover:border-gov-neutral-300'
                  }`}
                >
                  <div className="flex items-start justify-between">
                    <div>
                      <span className="text-xs font-mono font-bold text-gov-neutral-500 uppercase">
                        {enr.enrollmentNumber}
                      </span>
                      <h4 className="text-base font-bold text-gov-neutral-900 mt-0.5">
                        {enr.courseName || `Course #${enr.courseId}`}
                      </h4>
                      <p className="text-xs text-gov-neutral-600 mt-1">
                        Provider: {enr.providerName || 'Affiliated Provider'}
                      </p>
                      <p className="text-xs text-gov-neutral-600">
                        Center: {enr.centerName || 'Training Center'}
                      </p>
                    </div>
                    <span
                      className={`text-xs font-semibold px-2 py-0.5 rounded border ${
                        enr.isCompletedFlag
                          ? 'bg-emerald-50 text-emerald-700 border-emerald-200'
                          : 'bg-blue-50 text-blue-700 border-blue-200'
                      }`}
                    >
                      {enr.enrollmentStatusName || (enr.isCompletedFlag ? 'Completed' : 'In Training')}
                    </span>
                  </div>

                  <div className="grid grid-cols-2 gap-2 mt-4 pt-3 border-t border-gov-neutral-200 text-xs text-gov-neutral-600">
                    <div>
                      <span className="font-medium text-gov-neutral-500">Batch Code:</span>{' '}
                      <span className="font-mono">{enr.batchCode || '—'}</span>
                    </div>
                    <div>
                      <span className="font-medium text-gov-neutral-500">Enrollment Date:</span>{' '}
                      <span>{enr.enrollmentDate ? new Date(enr.enrollmentDate).toLocaleDateString() : '—'}</span>
                    </div>
                  </div>

                  <div className="mt-3 flex flex-wrap justify-end gap-3">
                    {!simulationActive && enr.batchId && <Link className="text-xs font-bold text-gov-navy underline"
                      to={`/trainee/find-training/${enr.batchId}`}>View timetable</Link>}
                    <button
                      type="button"
                      onClick={() =>
                        setSelectedEnrollmentId(
                          selectedEnrollmentId === enr.id ? 'ALL' : enr.id,
                        )
                      }
                      className="text-xs font-bold text-gov-navy hover:underline focus:outline-none"
                    >
                      {selectedEnrollmentId === enr.id
                        ? 'Showing Attendance (Click to Show All)'
                        : 'Filter Attendance for this Batch'}
                    </button>
                  </div>
                </div>
              ))}
            </div>
          </div>
        )}
        {(page > 0 || enrollmentsQuery.data?.hasNext) && (
          <nav className="mt-4 flex items-center gap-3" aria-label="Enrollment pages">
            <button className="btn-secondary" type="button" disabled={page === 0 || enrollmentsQuery.isFetching}
              onClick={() => setPage(page - 1)}>Previous</button>
            <span className="text-sm">Page {page + 1}</span>
            <button className="btn-secondary" type="button" disabled={!enrollmentsQuery.data?.hasNext || enrollmentsQuery.isFetching}
              onClick={() => setPage(page + 1)}>Next</button>
          </nav>
        )}
      </section>

      {/* Detailed Attendance Records Table */}
      <section
        aria-labelledby="attendance-records-heading"
        className="bg-white rounded-lg shadow-sm border border-gov-neutral-200 overflow-hidden"
      >
        <div className="p-6 border-b border-gov-neutral-200 flex flex-col sm:flex-row sm:items-center justify-between gap-4">
          <div>
            <h3
              id="attendance-records-heading"
              className="text-base font-bold text-gov-neutral-900"
            >
              Session-by-Session Attendance Log
            </h3>
            <p className="text-xs text-gov-neutral-500">
              Audit trail of daily classroom and practical session attendance records.
            </p>
          </div>
          {selectedEnrollmentId !== 'ALL' && (
            <button
              type="button"
              onClick={() => setSelectedEnrollmentId('ALL')}
              className="text-xs font-semibold px-2.5 py-1 rounded border border-gov-neutral-300 text-gov-neutral-700 hover:bg-gov-neutral-50"
            >
              Clear Batch Filter
            </button>
          )}
        </div>

        {attendanceQuery.isLoading ? (
          <div className="py-12 flex justify-center">
            <LoadingSpinner size="md" />
          </div>
        ) : attendanceQuery.isError ? (
          <ErrorState error={attendanceQuery.error} title="Unable to load attendance records" onRetry={() => void attendanceQuery.refetch()} />
        ) : filteredAttendance.length === 0 ? (
          <div className="p-8">
            <EmptyState
              title="No Attendance Logs Found"
              description="No individual daily attendance logs match the current selection."
            />
          </div>
        ) : (
          <div className="overflow-x-auto">
            <table className="w-full text-left border-collapse text-xs md:text-sm">
              <thead className="bg-gov-neutral-50 border-b border-gov-neutral-200 text-gov-neutral-700 uppercase tracking-wider text-xs">
                <tr>
                  <th scope="col" className="px-6 py-3 font-semibold">
                    Session Date
                  </th>
                  <th scope="col" className="px-6 py-3 font-semibold">
                    Sequence
                  </th>
                  <th scope="col" className="px-6 py-3 font-semibold">
                    Batch Code
                  </th>
                  <th scope="col" className="px-6 py-3 font-semibold">
                    Attendance Status
                  </th>
                  <th scope="col" className="px-6 py-3 font-semibold">
                    NCVET Present Credit
                  </th>
                  <th scope="col" className="px-6 py-3 font-semibold">
                    Remarks
                  </th>
                </tr>
              </thead>
              <tbody className="divide-y divide-gov-neutral-200">
                {filteredAttendance.map((record) => (
                  <tr key={record.id} className="hover:bg-gov-neutral-50 transition-colors">
                    <td className="px-6 py-3.5 font-medium text-gov-neutral-900 whitespace-nowrap">
                      {careerDate(record.sessionDate)}
                      {(record.scheduledStart || record.sessionStartTime) && <p className="mt-1 text-xs font-normal text-slate-600">
                        {record.scheduledStart?.slice(11, 16) ?? record.sessionStartTime?.slice(0, 5)}
                        {(record.scheduledEnd || record.sessionEndTime) && <>
                          {' – '}
                          {record.scheduledEnd && record.scheduledEnd.slice(0, 10) !== record.sessionDate &&
                            `${careerDate(record.scheduledEnd.slice(0, 10))}, `}
                          {record.scheduledEnd?.slice(11, 16) ?? record.sessionEndTime?.slice(0, 5)}
                        </>} IST
                      </p>}
                    </td>
                    <td className="px-6 py-3.5 text-gov-neutral-600 whitespace-nowrap font-mono">
                      #{record.sessionSequence ?? 1}
                    </td>
                    <td className="px-6 py-3.5 text-gov-neutral-600 whitespace-nowrap font-mono">
                      {record.batchCode || `Batch #${record.batchId}`}
                    </td>
                    <td className="px-6 py-3.5 whitespace-nowrap">
                      <span
                        className={`inline-flex items-center px-2 py-0.5 rounded text-xs font-semibold ${
                          record.countsAsPresent
                            ? 'bg-emerald-50 text-emerald-700'
                            : 'bg-red-50 text-red-700'
                        }`}
                      >
                        {record.attendanceStatusCode}
                      </span>
                    </td>
                    <td className="px-6 py-3.5 whitespace-nowrap">
                      {record.countsAsPresent ? (
                        <span className="text-emerald-700 font-semibold">✓ Yes (1.0)</span>
                      ) : (
                        <span className="text-red-600 font-semibold">✗ No (0.0)</span>
                      )}
                    </td>
                    <td className="px-6 py-3.5 text-gov-neutral-500 italic max-w-xs truncate">
                      {record.remarks || '—'}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </section>
    </div>
  );
};

export default TraineeTrainingPage;
