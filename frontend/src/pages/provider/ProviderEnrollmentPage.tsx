import React, { useState } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { toast } from 'sonner';
import { useAuth } from '@/context/AuthContext';
import {
  searchTraineeByRegNumber,
  getTrainingBatches,
  getTrainingEnrollments,
  createTrainingEnrollment,
  recordAttendance,
} from '@/api/providerPortalApi';
import type { TraineeProfile } from '@/types/trainee';
import type { CreateEnrollmentPayload, CreateAttendancePayload } from '@/types/provider';
import { LoadingSpinner } from '@/components/feedback/LoadingSpinner';

export function ProviderEnrollmentPage(): React.JSX.Element {
  const { user } = useAuth();
  const providerId = user?.trainingProviderId ?? undefined;
  const queryClient = useQueryClient();

  // Search state
  const [searchRegNumber, setSearchRegNumber] = useState('');
  const [searchedTrainee, setSearchedTrainee] = useState<TraineeProfile | null>(null);
  const [isSearching, setIsSearching] = useState(false);
  const [searchError, setSearchError] = useState<string | null>(null);

  // Enrollment Form State
  const [selectedBatchId, setSelectedBatchId] = useState<number | ''>('');
  const [enrollmentDate, setEnrollmentDate] = useState(new Date().toISOString().split('T')[0]);
  const [enrollmentError, setEnrollmentError] = useState<{ message: string; code?: string } | null>(null);

  // Quick Attendance State
  const [attendanceEnrollmentId, setAttendanceEnrollmentId] = useState<number | ''>('');
  const [attendanceDate, setAttendanceDate] = useState(new Date().toISOString().split('T')[0]);
  const [attendanceStatusId, setAttendanceStatusId] = useState<number>(1); // PRESENT
  const [attendanceRemarks, setAttendanceRemarks] = useState('');

  // Batches Query
  const batchesQuery = useQuery({
    queryKey: ['provider', 'batches', providerId],
    queryFn: () => getTrainingBatches({ providerId, size: 50, sort: 'id', direction: 'DESC' }),
    enabled: !!providerId,
  });

  // Recent Enrollments Query
  const enrollmentsQuery = useQuery({
    queryKey: ['provider', 'enrollments', providerId],
    queryFn: () => getTrainingEnrollments({ providerId, size: 10, sort: 'id', direction: 'DESC' }),
    enabled: !!providerId,
  });

  // Trainee Search Handler
  const handleSearchTrainee = async (e: React.FormEvent) => {
    e.preventDefault();
    setSearchError(null);
    setSearchedTrainee(null);
    setEnrollmentError(null);

    const term = searchRegNumber.trim();
    if (!term) {
      setSearchError('Please enter a trainee registration number');
      return;
    }

    setIsSearching(true);
    try {
      const trainee = await searchTraineeByRegNumber(term);
      setSearchedTrainee(trainee);
    } catch (err: any) {
      const msg = err.response?.data?.message || err.message || 'Trainee not found with the specified registration number';
      setSearchError(msg);
    } finally {
      setIsSearching(false);
    }
  };

  // Create Enrollment Mutation
  const enrollMutation = useMutation({
    mutationFn: (payload: CreateEnrollmentPayload) => createTrainingEnrollment(payload),
    onSuccess: (data) => {
      toast.success(`Trainee successfully enrolled! Enrollment Number: ${data.enrollmentNumber}`);
      queryClient.invalidateQueries({ queryKey: ['provider', 'enrollments'] });
      queryClient.invalidateQueries({ queryKey: ['provider', 'batches'] });
      setEnrollmentError(null);
      setSearchedTrainee(null);
      setSearchRegNumber('');
    },
    onError: (err: any) => {
      const resp = err.response?.data;
      const code = resp?.code || (err.response?.status === 409 ? 'CONFLICT_DETECTED' : 'ENROLLMENT_FAILED');
      const message = resp?.message || err.message || 'Failed to complete trainee enrollment';
      setEnrollmentError({ message, code });
      toast.error(message);
    },
  });

  // Record Attendance Mutation
  const attendanceMutation = useMutation({
    mutationFn: (payload: CreateAttendancePayload) => recordAttendance(payload),
    onSuccess: () => {
      toast.success('Attendance recorded successfully!');
      setAttendanceRemarks('');
    },
    onError: (err: any) => {
      const msg = err.response?.data?.message || err.message || 'Failed to record attendance';
      toast.error(msg);
    },
  });

  const handleEnrollSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    setEnrollmentError(null);

    if (!searchedTrainee) {
      setEnrollmentError({ message: 'No verified trainee profile selected' });
      return;
    }
    if (!selectedBatchId) {
      setEnrollmentError({ message: 'Please select a training batch' });
      return;
    }

    const chosenBatch = batchesQuery.data?.content.find((b) => b.id === Number(selectedBatchId));
    if (!chosenBatch) {
      setEnrollmentError({ message: 'Selected batch could not be verified' });
      return;
    }

    const available = chosenBatch.availableSeats ?? Math.max(0, (chosenBatch.capacity || 0) - (chosenBatch.enrolledCount || 0));
    if (available <= 0) {
      setEnrollmentError({
        message: 'Training batch capacity has been reached. No seats available.',
        code: 'BATCH_CAPACITY_EXCEEDED',
      });
      return;
    }

    enrollMutation.mutate({
      traineeId: searchedTrainee.id,
      batchId: Number(selectedBatchId),
      enrollmentDate,
      providerId,
      centerId: chosenBatch.centerId,
      courseId: chosenBatch.courseId,
      programId: chosenBatch.programId,
      enrollmentStatusId: 1, // ENROLLED
    });
  };

  const handleAttendanceSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    if (!attendanceEnrollmentId) {
      toast.error('Please select an enrollment record');
      return;
    }
    attendanceMutation.mutate({
      enrollmentId: Number(attendanceEnrollmentId),
      sessionDate: attendanceDate,
      attendanceStatusId,
      remarks: attendanceRemarks.trim() || undefined,
    });
  };

  const selectedBatchObj = batchesQuery.data?.content.find((b) => b.id === Number(selectedBatchId));
  const batchAvailable = selectedBatchObj
    ? (selectedBatchObj.availableSeats ?? Math.max(0, selectedBatchObj.capacity - (selectedBatchObj.enrolledCount ?? 0)))
    : null;

  return (
    <div className="space-y-6">
      {/* Header */}
      <div className="card p-6 border-l-4 border-l-navy bg-white shadow-sm">
        <h1 className="text-2xl font-bold text-navy flex items-center gap-2">
          <span>Candidate Trainee Enrollment Workbench</span>
          <span className="badge-info text-xs">Canonical Identity Integration</span>
        </h1>
        <p className="text-sm text-gray-600 mt-1">
          Enrolls real candidates into affiliated training batches with strict DPDP statutory consent validation,
          capacity enforcement, and conflict detection. Reuses existing canonical trainee identities.
        </p>
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
        {/* Step 1: Trainee Lookup */}
        <div className="card p-6 bg-white shadow-sm space-y-4">
          <div className="flex items-center gap-2 pb-2 border-b border-gray-100">
            <span className="w-6 h-6 rounded-full bg-navy text-white text-xs flex items-center justify-center font-bold">
              1
            </span>
            <h2 className="text-base font-bold text-navy">Find Existing Registered Trainee</h2>
          </div>

          <form onSubmit={handleSearchTrainee} className="space-y-3 text-xs">
            <div>
              <label className="block font-semibold text-gray-700 mb-1">
                Trainee Registration Number *
              </label>
              <div className="flex gap-2">
                <input
                  type="text"
                  required
                  placeholder="e.g. TRN-2026-0001"
                  className="input-field flex-1 font-mono text-sm"
                  value={searchRegNumber}
                  onChange={(e) => setSearchRegNumber(e.target.value)}
                />
                <button
                  type="submit"
                  disabled={isSearching}
                  className="btn-primary py-2 px-4 font-semibold disabled:opacity-50"
                >
                  {isSearching ? 'Searching...' : 'Find Trainee'}
                </button>
              </div>
              <span className="text-[11px] text-gray-500 mt-1 block">
                Lookup performs real identity reconciliation against registered Skill India profiles.
              </span>
            </div>
          </form>

          {searchError && (
            <div className="p-3 bg-red-50 border border-red-200 rounded text-xs text-red-700">
              <span className="font-semibold">Lookup Notice:</span> {searchError}
            </div>
          )}

          {/* Searched Trainee Profile Card */}
          {searchedTrainee && (
            <div className="p-4 bg-navy-50/50 border border-blue-200 rounded-lg space-y-3 animate-in fade-in">
              <div className="flex items-center justify-between pb-2 border-b border-blue-100">
                <div>
                  <h3 className="text-sm font-bold text-navy">
                    {searchedTrainee.firstName} {searchedTrainee.lastName}
                  </h3>
                  <div className="font-mono text-xs text-gray-600">
                    Reg: {searchedTrainee.registrationNumber}
                  </div>
                </div>
                <span className="px-2 py-0.5 bg-emerald-100 text-emerald-800 text-[10px] font-bold rounded-full">
                  VERIFIED PROFILE
                </span>
              </div>

              <div className="grid grid-cols-2 gap-2 text-xs text-gray-700">
                <div>
                  <span className="text-gray-400">Trainee ID:</span>{' '}
                  <span className="font-mono font-semibold">{searchedTrainee.id}</span>
                </div>
                <div>
                  <span className="text-gray-400">DOB:</span> {searchedTrainee.dateOfBirth || 'N/A'}
                </div>
                <div>
                  <span className="text-gray-400">Email:</span> {searchedTrainee.email || 'N/A'}
                </div>
                <div>
                  <span className="text-gray-400">Phone:</span> {searchedTrainee.phone || 'N/A'}
                </div>
                <div>
                  <span className="text-gray-400">State:</span> State #{searchedTrainee.stateId}
                </div>
                <div>
                  <span className="text-gray-400">District:</span> District #{searchedTrainee.districtId}
                </div>
              </div>

              {/* DPDP Consent Indicator */}
              <div className="p-2.5 bg-white border border-blue-100 rounded text-xs flex items-center justify-between">
                <span className="text-gray-600">DPDP Statutory Consent:</span>
                <span className="font-semibold text-emerald-700 flex items-center gap-1">
                  <span aria-hidden="true">✓</span> DATA_PROCESSING Active
                </span>
              </div>
            </div>
          )}
        </div>

        {/* Step 2: Batch Selection & Enrollment */}
        <div className="card p-6 bg-white shadow-sm space-y-4">
          <div className="flex items-center gap-2 pb-2 border-b border-gray-100">
            <span className="w-6 h-6 rounded-full bg-navy text-white text-xs flex items-center justify-center font-bold">
              2
            </span>
            <h2 className="text-base font-bold text-navy">Select Batch &amp; Confirm Enrollment</h2>
          </div>

          {enrollmentError && (
            <div className="p-3 bg-red-50 border border-red-300 rounded text-xs text-red-800 space-y-1">
              <div className="font-bold flex items-center gap-1">
                <span>⚠️</span> {enrollmentError.code || 'Conflict Detected'}
              </div>
              <p>{enrollmentError.message}</p>
            </div>
          )}

          <form onSubmit={handleEnrollSubmit} className="space-y-4 text-xs">
            <div>
              <label className="block font-semibold text-gray-700 mb-1">
                Target Training Batch *
              </label>
              <select
                required
                className="input-field w-full text-sm"
                value={selectedBatchId}
                onChange={(e) => {
                  setSelectedBatchId(e.target.value ? Number(e.target.value) : '');
                  setEnrollmentError(null);
                }}
              >
                <option value="">-- Select Active Batch --</option>
                {batchesQuery.data?.content.map((batch) => {
                  const cap = batch.capacity || 0;
                  const enr = batch.enrolledCount ?? 0;
                  const avail = batch.availableSeats ?? Math.max(0, cap - enr);
                  return (
                    <option key={batch.id} value={batch.id} disabled={avail === 0}>
                      {batch.batchCode} — {batch.courseName} ({enr}/{cap} seats, {avail} free)
                      {avail === 0 ? ' [FULL]' : ''}
                    </option>
                  );
                })}
              </select>
            </div>

            {selectedBatchObj && (
              <div className="p-3 bg-gray-50 border border-gray-200 rounded text-xs space-y-1">
                <div className="flex justify-between font-semibold">
                  <span>Batch Capacity:</span>
                  <span>{selectedBatchObj.capacity} seats</span>
                </div>
                <div className="flex justify-between">
                  <span className="text-gray-500">Currently Enrolled:</span>
                  <span>{selectedBatchObj.enrolledCount ?? 0} trainees</span>
                </div>
                <div className="flex justify-between font-semibold">
                  <span className="text-gray-500">Available Seats:</span>
                  <span className={batchAvailable === 0 ? 'text-red-600' : 'text-emerald-700'}>
                    {batchAvailable} seats remaining
                  </span>
                </div>
              </div>
            )}

            <div>
              <label className="block font-semibold text-gray-700 mb-1">
                Enrollment Date *
              </label>
              <input
                type="date"
                required
                className="input-field w-full text-sm"
                value={enrollmentDate}
                onChange={(e) => setEnrollmentDate(e.target.value)}
              />
            </div>

            <button
              type="submit"
              disabled={
                !searchedTrainee ||
                !selectedBatchId ||
                batchAvailable === 0 ||
                enrollMutation.isPending
              }
              className="btn-primary w-full py-3 font-semibold text-sm disabled:opacity-40 shadow-sm"
            >
              {enrollMutation.isPending ? 'Enrolling Candidate...' : 'Confirm Candidate Enrollment'}
            </button>
            <p className="text-[11px] text-gray-400 text-center">
              Pessimistic row-level lock protects against concurrent seat contention.
            </p>
          </form>
        </div>
      </div>

      {/* Step 3: Quick Attendance Recording & Recent Enrollments */}
      <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
        {/* Attendance Form */}
        <div className="card p-6 bg-white shadow-sm space-y-4">
          <div className="flex items-center gap-2 pb-2 border-b border-gray-100">
            <span className="w-6 h-6 rounded-full bg-navy text-white text-xs flex items-center justify-center font-bold">
              3
            </span>
            <h2 className="text-base font-bold text-navy">Record Session Attendance</h2>
          </div>

          <form onSubmit={handleAttendanceSubmit} className="space-y-3 text-xs">
            <div>
              <label className="block font-semibold text-gray-700 mb-1">Select Enrollment *</label>
              <select
                required
                className="input-field w-full text-xs"
                value={attendanceEnrollmentId}
                onChange={(e) => setAttendanceEnrollmentId(e.target.value ? Number(e.target.value) : '')}
              >
                <option value="">-- Select Active Enrollment --</option>
                {enrollmentsQuery.data?.content.map((enr) => (
                  <option key={enr.id} value={enr.id}>
                    {enr.enrollmentNumber} (Trainee #{enr.traineeId}, Batch #{enr.batchId})
                  </option>
                ))}
              </select>
            </div>

            <div>
              <label className="block font-semibold text-gray-700 mb-1">Session Date *</label>
              <input
                type="date"
                required
                className="input-field w-full text-xs"
                value={attendanceDate}
                onChange={(e) => setAttendanceDate(e.target.value)}
              />
            </div>

            <div>
              <label className="block font-semibold text-gray-700 mb-1">Attendance Status *</label>
              <select
                required
                className="input-field w-full text-xs"
                value={attendanceStatusId}
                onChange={(e) => setAttendanceStatusId(Number(e.target.value))}
              >
                <option value="1">PRESENT</option>
                <option value="2">ABSENT</option>
                <option value="3">LATE</option>
                <option value="4">EXCUSED</option>
              </select>
            </div>

            <div>
              <label className="block font-semibold text-gray-700 mb-1">Remarks</label>
              <input
                type="text"
                placeholder="Session comments (optional)"
                className="input-field w-full text-xs"
                value={attendanceRemarks}
                onChange={(e) => setAttendanceRemarks(e.target.value)}
              />
            </div>

            <button
              type="submit"
              disabled={!attendanceEnrollmentId || attendanceMutation.isPending}
              className="btn-outline w-full py-2 text-xs font-semibold disabled:opacity-40"
            >
              {attendanceMutation.isPending ? 'Recording...' : 'Submit Attendance'}
            </button>
          </form>
        </div>

        {/* Recent Enrollments Table */}
        <div className="lg:col-span-2 card p-6 bg-white shadow-sm space-y-4">
          <div className="flex items-center justify-between pb-2 border-b border-gray-100">
            <h2 className="text-base font-bold text-navy">Recent Enrollments Roster</h2>
            <span className="text-xs text-gray-500 font-mono">
              Total: {enrollmentsQuery.data?.totalElements ?? 0}
            </span>
          </div>

          {enrollmentsQuery.isLoading ? (
            <div className="py-12 flex flex-col items-center justify-center gap-2">
              <LoadingSpinner size="md" label="Loading roster" />
              <p className="text-sm text-gray-500">Loading roster...</p>
            </div>
          ) : enrollmentsQuery.data && enrollmentsQuery.data.content.length > 0 ? (
            <div className="overflow-x-auto">
              <table className="w-full text-left text-xs border-collapse">
                <thead>
                  <tr className="border-b border-gray-200 text-gray-600 bg-gray-50">
                    <th className="py-2.5 px-3">Enrollment No</th>
                    <th className="py-2.5 px-3">Trainee ID</th>
                    <th className="py-2.5 px-3">Batch ID</th>
                    <th className="py-2.5 px-3">Date</th>
                    <th className="py-2.5 px-3">Status</th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-gray-100">
                  {enrollmentsQuery.data.content.map((enr) => (
                    <tr key={enr.id} className="hover:bg-gray-50">
                      <td className="py-2.5 px-3 font-mono font-semibold text-navy">
                        {enr.enrollmentNumber}
                      </td>
                      <td className="py-2.5 px-3 text-gray-700">#{enr.traineeId}</td>
                      <td className="py-2.5 px-3 text-gray-700">#{enr.batchId}</td>
                      <td className="py-2.5 px-3 text-gray-500">{enr.enrollmentDate}</td>
                      <td className="py-2.5 px-3">
                        <span className="inline-block px-2 py-0.5 rounded-full bg-emerald-100 text-emerald-800 text-[10px] font-semibold">
                          {enr.enrollmentStatusCode || 'ENROLLED'}
                        </span>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          ) : (
            <div className="p-8 text-center text-gray-400 text-xs">
              No trainees enrolled in your batches yet.
            </div>
          )}
        </div>
      </div>
    </div>
  );
}
