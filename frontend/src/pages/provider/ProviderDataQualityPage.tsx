import React, { useState } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import {
  getProviderDataQualityIssues,
  getDataQualityIssueById,
  getDataQualityIssueEvents,
  startIssueInvestigation,
  resolveDataQualityIssue,
  dismissDataQualityIssue,
  triggerRuleReEvaluation,
} from '@/api/providerDataQualityApi';
import { getTrainingCenters } from '@/api/providerPortalApi';
import { apiPut } from '@/api/client';
import type {
  ProviderDataQualityIssue,
  IssueFilterParams,
} from '@/types/providerDataQuality';
import { PageLoading } from '@/components/feedback/LoadingSpinner';
import { toast } from 'sonner';

export function ProviderDataQualityPage(): React.JSX.Element {
  const queryClient = useQueryClient();

  // Tab & Filters
  const [activeTab, setActiveTab] = useState<'active' | 'all'>('active');
  const [severity, setSeverity] = useState<string>('');
  const [category, setCategory] = useState<string>('');
  const [status, setStatus] = useState<string>('');
  const [entityType, setEntityType] = useState<string>('');
  const [centerId, setCenterId] = useState<string>('');
  const [page, setPage] = useState<number>(0);
  const pageSize = 10;

  // Selected Issue for Detail Drawer / Remediation Modal
  const [selectedIssueId, setSelectedIssueId] = useState<number | null>(null);
  const [detailOpen, setDetailOpen] = useState<boolean>(false);
  const [timelinePage, setTimelinePage] = useState<number>(0);

  // Remediation Modal State
  const [remediationOpen, setRemediationOpen] = useState<boolean>(false);
  const [remediatingIssue, setRemediatingIssue] = useState<ProviderDataQualityIssue | null>(null);
  const [resolutionNotes, setResolutionNotes] = useState<string>('');
  const [enrollmentStartDate, setEnrollmentStartDate] = useState<string>('');
  const [enrollmentExpCompDate, setEnrollmentExpCompDate] = useState<string>('');
  const [enrollmentActCompDate, setEnrollmentActCompDate] = useState<string>('');
  const [enrollmentStatusId, setEnrollmentStatusId] = useState<number>(1);
  const [dropoutReasonId, setDropoutReasonId] = useState<string>('');
  const [dropoutRemarks, setDropoutRemarks] = useState<string>('');

  // Dismiss Modal State
  const [dismissOpen, setDismissOpen] = useState<boolean>(false);
  const [dismissRemarks, setDismissRemarks] = useState<string>('');

  // 1. Fetch Centers for Filter Selector
  const { data: centersData } = useQuery({
    queryKey: ['provider-centers-filter'],
    queryFn: () => getTrainingCenters({ size: 100 }),
  });
  const centers = centersData?.content ?? [];

  // 2. Fetch Provider DQ Issues
  const filterParams: IssueFilterParams = {
    severity: severity || undefined,
    category: category || undefined,
    status: status || undefined,
    entityType: entityType || undefined,
    centerId: centerId ? Number(centerId) : undefined,
    activeOnly: activeTab === 'active',
    page,
    size: pageSize,
  };

  const {
    data: issuesData,
    isLoading: issuesLoading,
    isError: issuesError,
    refetch: refetchIssues,
  } = useQuery({
    queryKey: ['provider-dq-issues', filterParams],
    queryFn: () => getProviderDataQualityIssues(filterParams),
  });

  // 3. Fetch Detail for Drawer
  const { data: issueDetail, isLoading: detailLoading } = useQuery({
    queryKey: ['provider-dq-issue-detail', selectedIssueId],
    queryFn: () => (selectedIssueId ? getDataQualityIssueById(selectedIssueId) : null),
    enabled: !!selectedIssueId && detailOpen,
  });

  // 4. Fetch Timeline Events
  const { data: timelineEvents, isLoading: eventsLoading } = useQuery({
    queryKey: ['provider-dq-issue-events', selectedIssueId, timelinePage],
    queryFn: () => (selectedIssueId ? getDataQualityIssueEvents(selectedIssueId, timelinePage, 10) : []),
    enabled: !!selectedIssueId && detailOpen,
  });

  // Mutations
  const investigateMutation = useMutation({
    mutationFn: (id: number) => startIssueInvestigation(id),
    onSuccess: (updated) => {
      toast.success(`Investigation started for issue #${updated.id}`);
      queryClient.invalidateQueries({ queryKey: ['provider-dq-issues'] });
      queryClient.invalidateQueries({ queryKey: ['provider-dq-issue-detail', updated.id] });
      queryClient.invalidateQueries({ queryKey: ['center-scorecard'] });
    },
    onError: (err: any) => {
      toast.error(err.response?.data?.message ?? 'Failed to start investigation');
    },
  });

  const resolveMutation = useMutation({
    mutationFn: async ({
      issue,
      notes,
      recordUpdate,
    }: {
      issue: ProviderDataQualityIssue;
      notes: string;
      recordUpdate?: any;
    }) => {
      // If underlying record is editable (e.g. TrainingEnrollment), update it first
      if (recordUpdate && issue.entityId && issue.entityType.toUpperCase().includes('ENROLLMENT')) {
        await apiPut(`/api/v1/training-enrollments/${issue.entityId}`, recordUpdate);
      }
      // Authoritative issue resolution
      const res = await resolveDataQualityIssue(issue.id, { resolutionNotes: notes });
      // Trigger background rule re-evaluation
      try {
        await triggerRuleReEvaluation(issue.id);
      } catch {
        // Non-blocking
      }
      return res;
    },
    onSuccess: (updated) => {
      toast.success(`Issue #${updated.id} resolved successfully!`);
      setRemediationOpen(false);
      setDetailOpen(false);
      setResolutionNotes('');
      // Invalidate relevant React Query caches for immediate UI & scorecard reconciliation
      queryClient.invalidateQueries({ queryKey: ['provider-dq-issues'] });
      queryClient.invalidateQueries({ queryKey: ['provider-dq-issue-detail'] });
      queryClient.invalidateQueries({ queryKey: ['center-scorecard'] });
    },
    onError: (err: any) => {
      toast.error(err.response?.data?.message ?? err.message ?? 'Failed to resolve issue');
    },
  });

  const dismissMutation = useMutation({
    mutationFn: ({ id, remarks }: { id: number; remarks: string }) =>
      dismissDataQualityIssue(id, { remarks }),
    onSuccess: (updated) => {
      toast.success(`Issue #${updated.id} dismissed as false positive / suppressed.`);
      setDismissOpen(false);
      setDetailOpen(false);
      setDismissRemarks('');
      queryClient.invalidateQueries({ queryKey: ['provider-dq-issues'] });
      queryClient.invalidateQueries({ queryKey: ['provider-dq-issue-detail'] });
      queryClient.invalidateQueries({ queryKey: ['center-scorecard'] });
    },
    onError: (err: any) => {
      toast.error(err.response?.data?.message ?? 'Failed to dismiss issue');
    },
  });

  // Open remediation modal and prefill data
  const handleOpenRemediation = (issue: ProviderDataQualityIssue) => {
    setRemediatingIssue(issue);
    setResolutionNotes('');
    if (issue.entityType.toUpperCase().includes('ENROLLMENT') && issue.entityId) {
      // Default dates
      setEnrollmentStartDate(new Date().toISOString().split('T')[0]);
      setEnrollmentExpCompDate(new Date(Date.now() + 90 * 86400000).toISOString().split('T')[0]);
      setEnrollmentActCompDate('');
      setEnrollmentStatusId(3); // Completed or In Progress
      setDropoutReasonId('');
      setDropoutRemarks('');
    }
    setRemediationOpen(true);
  };

  const handleOpenDetail = (id: number) => {
    setSelectedIssueId(id);
    setTimelinePage(0);
    setDetailOpen(true);
  };

  const handleOpenDismiss = (issue: ProviderDataQualityIssue) => {
    setRemediatingIssue(issue);
    setDismissRemarks('');
    setDismissOpen(true);
  };

  return (
    <div className="space-y-6">
      <div className="card p-6 border-l-4 border-l-navy bg-white shadow-sm flex flex-col sm:flex-row sm:items-center justify-between gap-4">
        <div>
          <h1 className="text-2xl font-bold text-navy flex items-center gap-2">
            <span>🔍</span> Data Quality Remediation Tracker
          </h1>
          <p className="text-sm text-gray-600 mt-1">
            Inspect detected domain anomalies across centers, batches, and enrollments. Review audit timelines and perform authoritative record corrections.
          </p>
        </div>
      </div>

      {/* Top Controls: Active Queue vs Historical & Filter Bar */}
      <div className="bg-white rounded-xl shadow-sm border border-slate-200 p-5 space-y-4">
        <div className="flex flex-wrap items-center justify-between gap-4 border-b border-slate-100 pb-4">
          <div className="flex items-center space-x-2">
            <button
              onClick={() => {
                setActiveTab('active');
                setPage(0);
              }}
              className={`px-4 py-2 text-sm font-semibold rounded-lg transition-colors ${
                activeTab === 'active'
                  ? 'bg-blue-600 text-white shadow-sm'
                  : 'bg-slate-100 text-slate-600 hover:bg-slate-200'
              }`}
              data-testid="tab-active-queue"
            >
              Active Queue (Actionable)
            </button>
            <button
              onClick={() => {
                setActiveTab('all');
                setPage(0);
              }}
              className={`px-4 py-2 text-sm font-semibold rounded-lg transition-colors ${
                activeTab === 'all'
                  ? 'bg-blue-600 text-white shadow-sm'
                  : 'bg-slate-100 text-slate-600 hover:bg-slate-200'
              }`}
              data-testid="tab-all-issues"
            >
              All Issues (Historical)
            </button>
          </div>

          <div className="text-xs text-slate-500 font-medium">
            Showing server-side paginated real Phase 28 issues
          </div>
        </div>

        {/* Filter Controls */}
        <div className="grid grid-cols-1 sm:grid-cols-2 md:grid-cols-5 gap-3">
          <div>
            <label className="block text-xs font-semibold text-slate-600 mb-1">Severity</label>
            <select
              value={severity}
              onChange={(e) => {
                setSeverity(e.target.value);
                setPage(0);
              }}
              className="w-full text-sm border border-slate-300 rounded-lg px-3 py-2 bg-white focus:outline-none focus:ring-2 focus:ring-blue-500"
              data-testid="filter-severity"
            >
              <option value="">All Severities</option>
              <option value="CRITICAL">Critical</option>
              <option value="MAJOR">Major / High</option>
              <option value="MINOR">Minor / Low / Medium</option>
            </select>
          </div>

          <div>
            <label className="block text-xs font-semibold text-slate-600 mb-1">Category</label>
            <select
              value={category}
              onChange={(e) => {
                setCategory(e.target.value);
                setPage(0);
              }}
              className="w-full text-sm border border-slate-300 rounded-lg px-3 py-2 bg-white focus:outline-none focus:ring-2 focus:ring-blue-500"
              data-testid="filter-category"
            >
              <option value="">All Categories</option>
              <option value="COMPLETENESS">Completeness</option>
              <option value="CONSISTENCY">Consistency</option>
              <option value="VALIDITY">Validity</option>
              <option value="DUPLICATE">Duplicate</option>
            </select>
          </div>

          <div>
            <label className="block text-xs font-semibold text-slate-600 mb-1">Status</label>
            <select
              value={status}
              onChange={(e) => {
                setStatus(e.target.value);
                setPage(0);
              }}
              className="w-full text-sm border border-slate-300 rounded-lg px-3 py-2 bg-white focus:outline-none focus:ring-2 focus:ring-blue-500"
              data-testid="filter-status"
            >
              <option value="">All Statuses</option>
              <option value="OPEN">Open</option>
              <option value="INVESTIGATING">Investigating / In Progress</option>
              <option value="RESOLVED">Resolved</option>
              <option value="DISMISSED">Dismissed / False Positive</option>
            </select>
          </div>

          <div>
            <label className="block text-xs font-semibold text-slate-600 mb-1">Affected Entity</label>
            <select
              value={entityType}
              onChange={(e) => {
                setEntityType(e.target.value);
                setPage(0);
              }}
              className="w-full text-sm border border-slate-300 rounded-lg px-3 py-2 bg-white focus:outline-none focus:ring-2 focus:ring-blue-500"
              data-testid="filter-entity-type"
            >
              <option value="">All Entities</option>
              <option value="Trainee">Trainee</option>
              <option value="BatchEnrollment">Batch Enrollment</option>
              <option value="AttendanceRecord">Attendance Record</option>
              <option value="TrainingBatch">Training Batch</option>
            </select>
          </div>

          <div>
            <label className="block text-xs font-semibold text-slate-600 mb-1">Affiliated Center</label>
            <select
              value={centerId}
              onChange={(e) => {
                setCenterId(e.target.value);
                setPage(0);
              }}
              className="w-full text-sm border border-slate-300 rounded-lg px-3 py-2 bg-white focus:outline-none focus:ring-2 focus:ring-blue-500"
              data-testid="filter-center"
            >
              <option value="">All Centers</option>
              {centers.map((c) => (
                <option key={c.id} value={c.id}>
                  {c.centerName} ({c.centerCode})
                </option>
              ))}
            </select>
          </div>
        </div>
      </div>

      {/* Issues Table */}
      <div className="bg-white rounded-xl shadow-sm border border-slate-200 overflow-hidden">
        {issuesLoading ? (
          <div className="p-12 text-center">
            <PageLoading message="Loading data quality issues..." />
          </div>
        ) : issuesError ? (
          <div className="p-8 text-center text-red-600 space-y-2">
            <div className="text-base font-semibold">Failed to load data quality issues</div>
            <button
              onClick={() => refetchIssues()}
              className="px-4 py-2 bg-red-100 hover:bg-red-200 text-red-800 text-sm font-semibold rounded-lg"
            >
              Retry
            </button>
          </div>
        ) : issuesData?.content.length === 0 ? (
          <div className="p-12 text-center text-slate-500 space-y-3" data-testid="empty-issues-message">
            <div className="text-4xl">🎉</div>
            <div className="text-base font-semibold text-slate-700">No issues found</div>
            <div className="text-sm text-slate-500">
              {activeTab === 'active'
                ? 'No active data quality issues currently match the selected criteria.'
                : 'No historical issues found.'}
            </div>
          </div>
        ) : (
          <div className="overflow-x-auto">
            <table className="w-full text-left text-sm" data-testid="issues-table">
              <thead className="bg-slate-50 border-b border-slate-200 text-slate-600 font-semibold text-xs uppercase tracking-wider">
                <tr>
                  <th className="py-3 px-4">Issue ID</th>
                  <th className="py-3 px-4">Severity</th>
                  <th className="py-3 px-4">Category</th>
                  <th className="py-3 px-4">Entity</th>
                  <th className="py-3 px-4">Center</th>
                  <th className="py-3 px-4">Description</th>
                  <th className="py-3 px-4">Status</th>
                  <th className="py-3 px-4">Detected</th>
                  <th className="py-3 px-4 text-right">Actions</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-100">
                {issuesData?.content.map((issue) => (
                  <tr
                    key={issue.id}
                    className="hover:bg-slate-50/80 transition-colors"
                    data-testid={`issue-row-${issue.id}`}
                  >
                    <td className="py-3 px-4 font-mono font-medium text-slate-700">
                      #{issue.id}
                    </td>
                    <td className="py-3 px-4">
                      <span
                        className={`inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-semibold ${
                          issue.severityCode === 'CRITICAL'
                            ? 'bg-red-100 text-red-800 border border-red-200'
                            : issue.severityCode === 'HIGH' || issue.severityCode === 'MAJOR'
                            ? 'bg-amber-100 text-amber-800 border border-amber-200'
                            : 'bg-blue-100 text-blue-800 border border-blue-200'
                        }`}
                      >
                        {issue.severityName ?? issue.severityCode}
                      </span>
                    </td>
                    <td className="py-3 px-4">
                      <span className="inline-flex items-center px-2 py-0.5 rounded text-xs font-medium bg-slate-100 text-slate-700">
                        {issue.categoryName ?? issue.categoryCode}
                      </span>
                    </td>
                    <td className="py-3 px-4">
                      <div className="font-medium text-slate-800">{issue.entityType}</div>
                      <div className="text-xs text-slate-500 font-mono">ID: {issue.entityId ?? 'N/A'}</div>
                    </td>
                    <td className="py-3 px-4 text-slate-600 text-xs font-medium">
                      {issue.centerName ? (
                        <div>{issue.centerName}</div>
                      ) : (
                        <span className="text-slate-400 italic">Unassigned Center</span>
                      )}
                      {issue.batchCode && (
                        <div className="text-[11px] text-slate-500 font-mono">Batch: {issue.batchCode}</div>
                      )}
                    </td>
                    <td className="py-3 px-4 max-w-xs text-slate-700 truncate" title={issue.issueSummary}>
                      {issue.issueSummary}
                    </td>
                    <td className="py-3 px-4">
                      <span
                        className={`inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-semibold ${
                          issue.statusCode === 'OPEN'
                            ? 'bg-red-50 text-red-700 border border-red-200'
                            : issue.statusCode === 'IN_PROGRESS' || issue.statusCode === 'ASSIGNED'
                            ? 'bg-amber-50 text-amber-700 border border-amber-200'
                            : issue.statusCode === 'RESOLVED'
                            ? 'bg-emerald-50 text-emerald-700 border border-emerald-200'
                            : 'bg-slate-100 text-slate-600 border border-slate-200'
                        }`}
                      >
                        {issue.statusName ?? issue.statusCode}
                      </span>
                    </td>
                    <td className="py-3 px-4 text-xs text-slate-500 whitespace-nowrap">
                      {new Date(issue.detectedAt).toLocaleString()}
                    </td>
                    <td className="py-3 px-4 text-right space-x-1 whitespace-nowrap">
                      <button
                        onClick={() => handleOpenDetail(issue.id)}
                        className="px-2.5 py-1 text-xs font-semibold text-blue-600 hover:text-blue-800 bg-blue-50 hover:bg-blue-100 rounded"
                        data-testid={`btn-inspect-${issue.id}`}
                      >
                        Inspect
                      </button>

                      {issue.isOpen && (
                        <>
                          {issue.statusCode === 'OPEN' && (
                            <button
                              onClick={() => investigateMutation.mutate(issue.id)}
                              className="px-2.5 py-1 text-xs font-semibold text-amber-700 hover:text-amber-900 bg-amber-50 hover:bg-amber-100 rounded"
                              data-testid={`btn-investigate-${issue.id}`}
                            >
                              Investigate
                            </button>
                          )}

                          <button
                            onClick={() => handleOpenRemediation(issue)}
                            className="px-2.5 py-1 text-xs font-semibold text-emerald-700 hover:text-emerald-900 bg-emerald-50 hover:bg-emerald-100 rounded"
                            data-testid={`btn-remediate-${issue.id}`}
                          >
                            Remediate
                          </button>

                          <button
                            onClick={() => handleOpenDismiss(issue)}
                            className="px-2 py-1 text-xs font-semibold text-slate-500 hover:text-slate-700 bg-slate-100 hover:bg-slate-200 rounded"
                            data-testid={`btn-dismiss-${issue.id}`}
                          >
                            Dismiss
                          </button>
                        </>
                      )}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}

        {/* Pagination Bar */}
        {issuesData && issuesData.totalPages > 1 && (
          <div className="flex items-center justify-between px-5 py-3 border-t border-slate-200 bg-slate-50 text-sm">
            <div className="text-slate-500 text-xs">
              Page <span className="font-semibold text-slate-800">{issuesData.page + 1}</span> of{' '}
              <span className="font-semibold text-slate-800">{issuesData.totalPages}</span> ({issuesData.totalElements} issues)
            </div>
            <div className="flex items-center space-x-2">
              <button
                disabled={issuesData.first}
                onClick={() => setPage((p) => Math.max(0, p - 1))}
                className="px-3 py-1.5 border border-slate-300 rounded text-xs font-medium text-slate-700 bg-white hover:bg-slate-100 disabled:opacity-50"
                data-testid="btn-prev-page"
              >
                Previous
              </button>
              <button
                disabled={issuesData.last}
                onClick={() => setPage((p) => p + 1)}
                className="px-3 py-1.5 border border-slate-300 rounded text-xs font-medium text-slate-700 bg-white hover:bg-slate-100 disabled:opacity-50"
                data-testid="btn-next-page"
              >
                Next
              </button>
            </div>
          </div>
        )}
      </div>

      {/* Detail Drawer Modal */}
      {detailOpen && selectedIssueId && (
        <div
          className="fixed inset-0 z-50 flex items-center justify-center bg-black/50 p-4"
          data-testid="detail-drawer"
        >
          <div className="bg-white rounded-xl shadow-xl border border-slate-200 w-full max-w-2xl max-h-[90vh] flex flex-col overflow-hidden">
            <div className="flex items-center justify-between px-6 py-4 border-b border-slate-100">
              <h3 className="text-base font-bold text-slate-800">
                Data Quality Issue #{selectedIssueId} Detail
              </h3>
              <button
                onClick={() => setDetailOpen(false)}
                className="text-slate-400 hover:text-slate-600 text-lg"
              >
                ✕
              </button>
            </div>

            <div className="p-6 overflow-y-auto space-y-5 flex-1">
              {detailLoading ? (
                <div className="py-8 text-center">
                  <PageLoading message="Loading issue details..." />
                </div>
              ) : issueDetail ? (
                <>
                  <div className="grid grid-cols-2 gap-4 text-sm bg-slate-50 p-4 rounded-lg border border-slate-200">
                    <div>
                      <span className="text-slate-500 text-xs block">Rule Code & Name</span>
                      <span className="font-semibold text-slate-800">
                        {issueDetail.ruleCode ? `${issueDetail.ruleCode} — ${issueDetail.ruleName}` : 'Ad-hoc anomaly'}
                      </span>
                    </div>
                    <div>
                      <span className="text-slate-500 text-xs block">Severity & Category</span>
                      <span className="font-semibold text-slate-800">
                        {issueDetail.severityName ?? issueDetail.severityCode} / {issueDetail.categoryName ?? issueDetail.categoryCode}
                      </span>
                    </div>
                    <div>
                      <span className="text-slate-500 text-xs block">Target Entity</span>
                      <span className="font-semibold text-slate-800">
                        {issueDetail.entityType} (ID: {issueDetail.entityId})
                      </span>
                    </div>
                    <div>
                      <span className="text-slate-500 text-xs block">Affiliated Center</span>
                      <span className="font-semibold text-slate-800">
                        {issueDetail.centerName ?? 'Unassigned Center'}
                      </span>
                    </div>
                    <div className="col-span-2">
                      <span className="text-slate-500 text-xs block">Failure Description</span>
                      <span className="text-slate-800 font-medium">{issueDetail.issueSummary}</span>
                    </div>
                    {issueDetail.resolvedAt && (
                      <div className="col-span-2 bg-emerald-50 p-3 rounded border border-emerald-200 text-emerald-900">
                        <div className="text-xs font-bold uppercase">Resolution Details</div>
                        <div className="text-xs mt-1">Resolved at: {new Date(issueDetail.resolvedAt).toLocaleString()}</div>
                        <div className="text-xs">Notes: {issueDetail.resolutionNotes}</div>
                      </div>
                    )}
                  </div>

                  {/* Remediation Capability Note */}
                  <div className={`p-3 rounded-lg text-xs border ${
                    issueDetail.remediationPermitted
                      ? 'bg-blue-50 border-blue-200 text-blue-900'
                      : 'bg-amber-50 border-amber-200 text-amber-900'
                  }`}>
                    <span className="font-bold">Remediation Status: </span>
                    {issueDetail.remediationReason}
                  </div>

                  {/* Historical Audit Timeline */}
                  <div className="space-y-3">
                    <div className="flex items-center justify-between">
                      <h4 className="text-sm font-bold text-slate-800">Audit Event Trail</h4>
                      <span className="text-xs text-slate-500">Bounded Event History</span>
                    </div>

                    {eventsLoading ? (
                      <div className="py-4 text-center text-xs text-slate-500">Loading timeline...</div>
                    ) : timelineEvents && timelineEvents.length > 0 ? (
                      <div className="space-y-2 border-l-2 border-slate-200 pl-4 py-1">
                        {timelineEvents.map((ev) => (
                          <div key={ev.id} className="relative pb-2" data-testid={`event-item-${ev.id}`}>
                            <div className="absolute -left-[21px] top-1 w-2.5 h-2.5 rounded-full bg-blue-500 ring-4 ring-white" />
                            <div className="flex items-center space-x-2 text-xs">
                              <span className="font-bold text-slate-700">{ev.statusName ?? ev.statusCode}</span>
                              <span className="text-slate-400">•</span>
                              <span className="text-slate-500">{new Date(ev.changedAt).toLocaleString()}</span>
                            </div>
                            {ev.remarks && (
                              <div className="text-xs text-slate-600 mt-0.5">{ev.remarks}</div>
                            )}
                          </div>
                        ))}
                      </div>
                    ) : (
                      <div className="text-xs text-slate-500 italic">No historical events recorded yet.</div>
                    )}
                  </div>
                </>
              ) : null}
            </div>

            <div className="px-6 py-3 bg-slate-50 border-t border-slate-100 flex items-center justify-end space-x-2">
              {issueDetail?.isOpen && (
                <button
                  onClick={() => {
                    setDetailOpen(false);
                    handleOpenRemediation(issueDetail);
                  }}
                  className="px-4 py-2 bg-emerald-600 hover:bg-emerald-700 text-white text-xs font-semibold rounded-lg shadow-sm"
                >
                  Proceed to Remediation
                </button>
              )}
              <button
                onClick={() => setDetailOpen(false)}
                className="px-4 py-2 bg-slate-200 hover:bg-slate-300 text-slate-700 text-xs font-semibold rounded-lg"
              >
                Close
              </button>
            </div>
          </div>
        </div>
      )}

      {/* Inline Remediation Modal */}
      {remediationOpen && remediatingIssue && (
        <div
          className="fixed inset-0 z-50 flex items-center justify-center bg-black/50 p-4"
          data-testid="remediation-modal"
        >
          <div className="bg-white rounded-xl shadow-xl border border-slate-200 w-full max-w-xl max-h-[90vh] flex flex-col overflow-hidden">
            <div className="flex items-center justify-between px-6 py-4 border-b border-slate-100 bg-emerald-50/50">
              <h3 className="text-base font-bold text-slate-800">
                Remediate Issue #{remediatingIssue.id} ({remediatingIssue.entityType})
              </h3>
              <button
                onClick={() => setRemediationOpen(false)}
                className="text-slate-400 hover:text-slate-600 text-lg"
              >
                ✕
              </button>
            </div>

            <div className="p-6 overflow-y-auto space-y-4 flex-1">
              {/* Informational Guidance Banner */}
              <div className="text-xs p-3 rounded-lg bg-slate-50 border border-slate-200 text-slate-700">
                <div className="font-semibold text-slate-800 mb-1">{remediatingIssue.issueSummary}</div>
                <div>{remediatingIssue.remediationReason}</div>
              </div>

              {/* Supported Entity Correction Fields: Training Enrollment */}
              {remediatingIssue.entityType.toUpperCase().includes('ENROLLMENT') ? (
                <div className="space-y-3 border-t border-slate-200 pt-3">
                  <h4 className="text-xs font-bold uppercase tracking-wider text-slate-500">
                    Correct Underlying Training Enrollment Record
                  </h4>
                  <div className="grid grid-cols-2 gap-3">
                    <div>
                      <label className="block text-xs font-medium text-slate-600 mb-1">
                        Start Date
                      </label>
                      <input
                        type="date"
                        value={enrollmentStartDate}
                        onChange={(e) => setEnrollmentStartDate(e.target.value)}
                        className="w-full text-xs border border-slate-300 rounded px-2.5 py-1.5"
                        data-testid="input-start-date"
                      />
                    </div>
                    <div>
                      <label className="block text-xs font-medium text-slate-600 mb-1">
                        Expected Completion Date
                      </label>
                      <input
                        type="date"
                        value={enrollmentExpCompDate}
                        onChange={(e) => setEnrollmentExpCompDate(e.target.value)}
                        className="w-full text-xs border border-slate-300 rounded px-2.5 py-1.5"
                        data-testid="input-exp-comp-date"
                      />
                    </div>
                    <div>
                      <label className="block text-xs font-medium text-slate-600 mb-1">
                        Actual Completion Date (Optional)
                      </label>
                      <input
                        type="date"
                        value={enrollmentActCompDate}
                        onChange={(e) => setEnrollmentActCompDate(e.target.value)}
                        className="w-full text-xs border border-slate-300 rounded px-2.5 py-1.5"
                        data-testid="input-act-comp-date"
                      />
                    </div>
                    <div>
                      <label className="block text-xs font-medium text-slate-600 mb-1">
                        Enrollment Status
                      </label>
                      <select
                        value={enrollmentStatusId}
                        onChange={(e) => setEnrollmentStatusId(Number(e.target.value))}
                        className="w-full text-xs border border-slate-300 rounded px-2.5 py-1.5 bg-white"
                        data-testid="select-enrollment-status"
                      >
                        <option value={1}>Enrolled</option>
                        <option value={2}>In Progress</option>
                        <option value={3}>Completed</option>
                        <option value={4}>Dropped Out</option>
                      </select>
                    </div>
                  </div>
                </div>
              ) : null}

              {/* Mandatory Resolution Notes */}
              <div>
                <label className="block text-xs font-bold text-slate-700 mb-1">
                  Resolution Notes <span className="text-red-500">*</span>
                </label>
                <textarea
                  rows={3}
                  value={resolutionNotes}
                  onChange={(e) => setResolutionNotes(e.target.value)}
                  placeholder="Explain remediation actions taken to correct this anomaly (mandatory)..."
                  className="w-full text-xs border border-slate-300 rounded-lg p-2.5 focus:ring-2 focus:ring-emerald-500 focus:outline-none"
                  data-testid="input-resolution-notes"
                />
                <div className="text-[11px] text-slate-400 text-right mt-1">
                  {resolutionNotes.length}/500 characters
                </div>
              </div>
            </div>

            <div className="px-6 py-3 bg-slate-50 border-t border-slate-100 flex items-center justify-end space-x-2">
              <button
                onClick={() => setRemediationOpen(false)}
                className="px-4 py-2 bg-slate-200 hover:bg-slate-300 text-slate-700 text-xs font-semibold rounded-lg"
              >
                Cancel
              </button>
              <button
                disabled={!resolutionNotes.trim() || resolveMutation.isPending}
                onClick={() => {
                  let recordUpdate: any = undefined;
                  if (remediatingIssue.entityType.toUpperCase().includes('ENROLLMENT')) {
                    recordUpdate = {
                      startDate: enrollmentStartDate || null,
                      expectedCompletionDate: enrollmentExpCompDate || null,
                      actualCompletionDate: enrollmentActCompDate || null,
                      enrollmentStatusId: enrollmentStatusId,
                      dropoutReasonId: dropoutReasonId ? Number(dropoutReasonId) : null,
                      dropoutRemarks: dropoutRemarks || null,
                    };
                  }
                  resolveMutation.mutate({
                    issue: remediatingIssue,
                    notes: resolutionNotes,
                    recordUpdate,
                  });
                }}
                className="px-4 py-2 bg-emerald-600 hover:bg-emerald-700 text-white text-xs font-semibold rounded-lg shadow-sm disabled:opacity-50"
                data-testid="btn-submit-resolution"
              >
                {resolveMutation.isPending ? 'Resolving...' : 'Confirm & Resolve Issue'}
              </button>
            </div>
          </div>
        </div>
      )}

      {/* Dismiss (False Positive / Suppressed) Modal */}
      {dismissOpen && remediatingIssue && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/50 p-4">
          <div className="bg-white rounded-xl shadow-xl border border-slate-200 w-full max-w-md p-6 space-y-4">
            <h3 className="text-base font-bold text-slate-800">
              Dismiss Issue #{remediatingIssue.id}
            </h3>
            <p className="text-xs text-slate-600">
              Provide justification remarks explaining why this issue is a false positive or why it should be dismissed.
            </p>
            <textarea
              rows={3}
              value={dismissRemarks}
              onChange={(e) => setDismissRemarks(e.target.value)}
              placeholder="Mandatory dismissal remarks..."
              className="w-full text-xs border border-slate-300 rounded p-2.5 focus:ring-2 focus:ring-blue-500 focus:outline-none"
              data-testid="input-dismiss-remarks"
            />
            <div className="flex items-center justify-end space-x-2">
              <button
                onClick={() => setDismissOpen(false)}
                className="px-4 py-2 bg-slate-200 hover:bg-slate-300 text-slate-700 text-xs font-semibold rounded-lg"
              >
                Cancel
              </button>
              <button
                disabled={!dismissRemarks.trim() || dismissMutation.isPending}
                onClick={() =>
                  dismissMutation.mutate({
                    id: remediatingIssue.id,
                    remarks: dismissRemarks,
                  })
                }
                className="px-4 py-2 bg-red-600 hover:bg-red-700 text-white text-xs font-semibold rounded-lg disabled:opacity-50"
                data-testid="btn-confirm-dismiss"
              >
                {dismissMutation.isPending ? 'Dismissing...' : 'Confirm Dismissal'}
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
