import React, { useState, useEffect, useCallback } from 'react';
import { useQueryClient } from '@tanstack/react-query';
import {
  listOnboardingApplications,
  approveOnboardingApplication,
  rejectOnboardingApplication,
  pendingOnboardingKey,
} from '@/api/adminApi';
import type { OnboardingApplicationResponse } from '@/api/registrationApi';
import { useAuth } from '@/context/AuthContext';
import { LoadingSpinner } from '@/components/feedback/LoadingSpinner';
import type { ApiError } from '@/api/types';

export function AdminOnboardingPage(): React.JSX.Element {
  const { user, hasRole, hasAuthority } = useAuth();
  const queryClient = useQueryClient();
  const canApproveOrReject =
    hasRole('ROLE_super_admin') ||
    hasRole('ROLE_government_admin') ||
    hasRole('ROLE_ADMIN') ||
    hasRole('ADMIN') ||
    hasAuthority('user.manage');

  const [applications, setApplications] = useState<OnboardingApplicationResponse[]>([]);
  const [statusFilter, setStatusFilter] = useState<string>('PENDING');
  const [loading, setLoading] = useState(true);
  const [errorMsg, setErrorMsg] = useState<string | null>(null);
  const [successMsg, setSuccessMsg] = useState<string | null>(null);

  // Rejection Modal State
  const [rejectModalApp, setRejectModalApp] = useState<OnboardingApplicationResponse | null>(null);
  const [rejectionReason, setRejectionReason] = useState('');
  const [processingId, setProcessingId] = useState<number | null>(null);

  const fetchApplications = useCallback(async () => {
    setLoading(true);
    setErrorMsg(null);
    try {
      const data = await listOnboardingApplications(statusFilter === 'ALL' ? undefined : statusFilter);
      setApplications(data);
    } catch (err) {
      const apiErr = err as ApiError;
      setErrorMsg(apiErr.message || 'Failed to load onboarding applications.');
    } finally {
      setLoading(false);
    }
  }, [statusFilter]);

  useEffect(() => {
    fetchApplications();
  }, [fetchApplications]);

  const handleApprove = async (app: OnboardingApplicationResponse) => {
    if (!window.confirm(`Are you sure you want to approve onboarding for "${app.organizationName}"? This will activate the admin user account.`)) {
      return;
    }

    setProcessingId(app.applicationId);
    setErrorMsg(null);
    setSuccessMsg(null);
    try {
      await approveOnboardingApplication(app.applicationId);
      void queryClient.invalidateQueries({ queryKey: pendingOnboardingKey });
      setSuccessMsg(`Application for "${app.organizationName}" approved successfully.`);
      await fetchApplications();
    } catch (err) {
      const apiErr = err as ApiError;
      setErrorMsg(apiErr.message || 'Failed to approve application.');
    } finally {
      setProcessingId(null);
    }
  };

  const handleRejectConfirm = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!rejectModalApp) return;

    setProcessingId(rejectModalApp.applicationId);
    setErrorMsg(null);
    setSuccessMsg(null);
    try {
      await rejectOnboardingApplication(rejectModalApp.applicationId, rejectionReason.trim());
      void queryClient.invalidateQueries({ queryKey: pendingOnboardingKey });
      setSuccessMsg(`Application for "${rejectModalApp.organizationName}" rejected.`);
      setRejectModalApp(null);
      setRejectionReason('');
      await fetchApplications();
    } catch (err) {
      const apiErr = err as ApiError;
      setErrorMsg(apiErr.message || 'Failed to reject application.');
    } finally {
      setProcessingId(null);
    }
  };

  return (
    <div className="space-y-6">
      {/* Page Header */}
      <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4 border-b pb-4">
        <div>
          <div className="flex items-center gap-2">
            <h1 className="text-2xl font-bold tracking-tight text-navy">
              Organisation Onboarding Requests
            </h1>
            {user?.stateId != null && (
              <span className="text-xs bg-indigo-50 text-indigo-700 px-2.5 py-0.5 rounded border border-indigo-200 font-medium">
                State Jurisdiction (ID: {user.stateId})
              </span>
            )}
          </div>
          <p className="text-sm text-gray-500 mt-1">
            Review and adjudicate pending training provider and employer registrations.
          </p>
        </div>

        {/* Filter Controls */}
        <div className="flex items-center gap-3">
          <label className="text-xs font-semibold text-gray-700">Filter Status:</label>
          <select
            value={statusFilter}
            onChange={(e) => setStatusFilter(e.target.value)}
            className="px-3 py-1.5 border rounded-md text-sm bg-white shadow-sm focus:ring-1 focus:ring-navy focus:outline-none"
          >
            <option value="PENDING">Pending Review</option>
            <option value="APPROVED">Approved</option>
            <option value="REJECTED">Rejected</option>
            <option value="ALL">All Applications</option>
          </select>

          <button
            type="button"
            onClick={fetchApplications}
            disabled={loading}
            className="btn-secondary text-xs px-3 py-1.5"
          >
            Refresh
          </button>
        </div>
      </div>

      {/* Notifications */}
      {successMsg && (
        <div className="p-4 rounded-md bg-emerald-50 border border-emerald-200 text-sm text-emerald-800">
          {successMsg}
        </div>
      )}
      {errorMsg && (
        <div className="p-4 rounded-md bg-red-50 border border-red-200 text-sm text-red-800">
          {errorMsg}
        </div>
      )}

      {/* Content */}
      {loading ? (
        <div className="py-12 flex flex-col items-center gap-3">
          <LoadingSpinner size="lg" />
          <span className="text-sm text-gray-500">Loading onboarding requests...</span>
        </div>
      ) : applications.length === 0 ? (
        <div className="text-center py-12 bg-white rounded-lg border border-dashed p-8">
          <p className="text-gray-500 text-sm">No applications found matching the selected filter ({statusFilter}).</p>
        </div>
      ) : (
        <div className="bg-white rounded-lg shadow overflow-hidden">
          <div className="overflow-x-auto">
            <table className="min-w-full divide-y divide-gray-200 text-sm">
              <thead className="bg-gray-50 text-gray-700 uppercase font-semibold text-xs tracking-wider">
                <tr>
                  <th scope="col" className="px-4 py-3 text-left">App Number</th>
                  <th scope="col" className="px-4 py-3 text-left">Type</th>
                  <th scope="col" className="px-4 py-3 text-left">Organisation</th>
                  <th scope="col" className="px-4 py-3 text-left">Contact Person</th>
                  <th scope="col" className="px-4 py-3 text-left">Email / Phone</th>
                  <th scope="col" className="px-4 py-3 text-left">Submitted</th>
                  <th scope="col" className="px-4 py-3 text-left">Status</th>
                  <th scope="col" className="px-4 py-3 text-right">Actions</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-gray-100 bg-white">
                {applications.map((app) => (
                  <tr key={app.applicationId} className="hover:bg-gray-50/80 transition-colors">
                    <td className="px-4 py-3.5 font-mono text-xs font-semibold text-navy">
                      {app.applicationNumber}
                    </td>
                    <td className="px-4 py-3.5">
                      <span className="inline-flex px-2 py-0.5 rounded text-xs font-medium bg-blue-50 text-blue-700 border border-blue-200">
                        {app.applicantType === 'TRAINING_PROVIDER' ? 'Provider' : 'Employer'}
                      </span>
                    </td>
                    <td className="px-4 py-3.5 font-medium text-gray-900">
                      {app.organizationName}
                    </td>
                    <td className="px-4 py-3.5 text-gray-600">
                      {app.contactPerson}
                    </td>
                    <td className="px-4 py-3.5 text-xs text-gray-600">
                      <div>{app.contactEmail}</div>
                      {app.contactPhone && <div className="text-gray-400">{app.contactPhone}</div>}
                    </td>
                    <td className="px-4 py-3.5 text-xs text-gray-500 whitespace-nowrap">
                      {new Date(app.createdAt).toLocaleDateString()}
                    </td>
                    <td className="px-4 py-3.5 whitespace-nowrap">
                      <span
                        className={`inline-flex px-2 py-0.5 rounded-full text-xs font-bold ${
                          app.status === 'APPROVED'
                            ? 'bg-emerald-100 text-emerald-800'
                            : app.status === 'REJECTED'
                            ? 'bg-red-100 text-red-800'
                            : 'bg-amber-100 text-amber-800'
                        }`}
                      >
                        {app.status}
                      </span>
                    </td>
                    <td className="px-4 py-3.5 text-right whitespace-nowrap">
                      {app.status === 'PENDING' ? (
                        canApproveOrReject ? (
                          <div className="flex items-center justify-end gap-2">
                            <button
                              type="button"
                              disabled={processingId === app.applicationId}
                              onClick={() => handleApprove(app)}
                              className="px-2.5 py-1 text-xs font-medium text-emerald-700 bg-emerald-50 hover:bg-emerald-100 rounded border border-emerald-300 transition-colors"
                            >
                              Approve
                            </button>
                            <button
                              type="button"
                              disabled={processingId === app.applicationId}
                              onClick={() => { setRejectModalApp(app); setRejectionReason(''); }}
                              className="px-2.5 py-1 text-xs font-medium text-red-700 bg-red-50 hover:bg-red-100 rounded border border-red-300 transition-colors"
                            >
                              Reject
                            </button>
                          </div>
                        ) : (
                          <span className="text-xs font-medium text-amber-700 bg-amber-50 px-2.5 py-1 rounded border border-amber-200">
                            Read-Only Review
                          </span>
                        )
                      ) : (
                        <span className="text-xs text-gray-400">
                          {app.status === 'REJECTED' && app.rejectionReason
                            ? `Reason: ${app.rejectionReason.slice(0, 25)}...`
                            : 'Decision recorded'}
                        </span>
                      )}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </div>
      )}

      {/* Reject Modal */}
      {rejectModalApp && (
        <div className="fixed inset-0 z-50 bg-black/40 flex items-center justify-center p-4">
          <div className="bg-white rounded-lg shadow-xl max-w-md w-full p-6">
            <h3 className="text-lg font-bold text-gray-900 mb-2">Reject Application</h3>
            <p className="text-xs text-gray-500 mb-4">
              Rejecting will disable the user account for <span className="font-semibold text-gray-800">{rejectModalApp.organizationName}</span>. Please provide a formal reason.
            </p>

            <form onSubmit={handleRejectConfirm} className="space-y-4">
              <div>
                <label className="block text-xs font-medium text-gray-700 mb-1">Reason for Rejection *</label>
                <textarea
                  required
                  rows={3}
                  value={rejectionReason}
                  onChange={(e) => setRejectionReason(e.target.value)}
                  placeholder="e.g. Mismatched incorporation records or invalid accreditation certificates."
                  className="w-full px-3 py-2 border rounded-md text-sm focus:ring-1 focus:ring-navy focus:outline-none"
                />
              </div>

              <div className="flex justify-end gap-3 pt-2">
                <button
                  type="button"
                  onClick={() => setRejectModalApp(null)}
                  className="btn-secondary text-sm"
                >
                  Cancel
                </button>
                <button
                  type="submit"
                  disabled={processingId === rejectModalApp.applicationId}
                  className="px-4 py-2 bg-red-600 hover:bg-red-700 text-white rounded-md text-sm font-medium transition-colors"
                >
                  {processingId === rejectModalApp.applicationId ? 'Rejecting...' : 'Confirm Rejection'}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
}
