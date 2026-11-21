import React, { useState } from 'react';
import { useOutletContext } from 'react-router-dom';
import { useQuery } from '@tanstack/react-query';
import type {
  TraineeProfile,
  TrainingEnrollment,
  AttendanceRecord,
} from '@/types/trainee';
import { getTraineeEnrollments, getTraineeAttendance } from '@/api/traineeApi';
import { LoadingSpinner } from '@/components/feedback/LoadingSpinner';
import { EmptyState } from '@/components/feedback/EmptyState';

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

  // Query enrollments
  const enrollmentsQuery = useQuery({
    queryKey: ['trainee', 'enrollments', traineeId],
    queryFn: () => getTraineeEnrollments(traineeId!),
    enabled: Boolean(traineeId),
    retry: false,
  });

  // Query attendance
  const attendanceQuery = useQuery({
    queryKey: ['trainee', 'attendance', traineeId],
    queryFn: () => getTraineeAttendance(traineeId!),
    enabled: Boolean(traineeId),
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
            Certified courses and centers associated with this candidate profile.
          </p>
        </div>

        {enrollmentsQuery.isLoading ? (
          <div className="py-8 flex justify-center">
            <LoadingSpinner size="md" />
          </div>
        ) : enrollmentsQuery.isError ? (
          <div
            role="alert"
            className="bg-blue-50 border border-blue-200 p-4 rounded-lg text-blue-900 text-sm flex items-start gap-3"
          >
            <svg
              className="w-5 h-5 text-blue-600 mt-0.5 shrink-0"
              fill="currentColor"
              viewBox="0 0 20 20"
            >
              <path
                fillRule="evenodd"
                d="M18 10a8 8 0 11-16 0 8 8 0 0116 0zm-7-4a1 1 0 11-2 0 1 1 0 012 0zM9 9a1 1 0 000 2v3a1 1 0 001 1h1a1 1 0 100-2v-3a1 1 0 00-1-1H9z"
                clipRule="evenodd"
              />
            </svg>
            <div>
              <p className="font-semibold">Center Synchronization Notice</p>
              <p className="text-xs text-blue-800 mt-0.5">
                Detailed batch master records are maintained by your Training Provider. Your enrollment standing is active in the central verification system.
              </p>
            </div>
          </div>
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

                  <div className="mt-3 flex justify-end">
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
          <div className="p-6 text-center text-sm text-gov-neutral-600">
            <p className="font-medium text-gov-neutral-800">
              Attendance records are restricted to verified biometric or trainer-authorized devices.
            </p>
            <p className="text-xs text-gov-neutral-500 mt-1">
              If your attendance percentage is not visible, check with your batch trainer.
            </p>
          </div>
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
                      {new Date(record.sessionDate).toLocaleDateString()}
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
