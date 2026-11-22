import React, { useState } from 'react';
import { useQuery } from '@tanstack/react-query';
import { useNavigate } from 'react-router-dom';
import { getCenterScorecard } from '@/api/providerDataQualityApi';
import { getTrainingCenters } from '@/api/providerPortalApi';
import { PageLoading } from '@/components/feedback/LoadingSpinner';

export function ProviderScorecardsPage(): React.JSX.Element {
  const navigate = useNavigate();

  // 1. Fetch Affiliated Centers for Dropdown
  const {
    data: centersData,
    isLoading: centersLoading,
    isError: centersError,
  } = useQuery({
    queryKey: ['provider-centers-scorecard-list'],
    queryFn: () => getTrainingCenters({ size: 100 }),
  });

  const centers = centersData?.content ?? [];
  const [selectedCenterId, setSelectedCenterId] = useState<number | null>(null);

  // Set default center when centers load
  const effectiveCenterId =
    selectedCenterId ?? (centers.length > 0 ? centers[0].id : null);

  // 2. Fetch Scorecard for Selected Center
  const {
    data: scorecard,
    isLoading: scorecardLoading,
    isError: scorecardError,
    refetch: refetchScorecard,
  } = useQuery({
    queryKey: ['center-scorecard', effectiveCenterId],
    queryFn: () => (effectiveCenterId ? getCenterScorecard(effectiveCenterId) : null),
    enabled: !!effectiveCenterId,
  });

  const cleanlinessPct = scorecard?.cleanlinessIndexPct;
  const completionPct = scorecard?.completionRatePct;
  const dropoutPct = scorecard?.dropoutRatePct;

  // Visual Gauge Health Category
  const getHealthLevel = (pct: number | null | undefined) => {
    if (pct === null || pct === undefined) return { label: 'No Evaluated Data', color: 'text-slate-400', stroke: 'stroke-slate-300' };
    if (pct >= 90) return { label: 'Excellent Cleanliness', color: 'text-emerald-600', stroke: 'stroke-emerald-500' };
    if (pct >= 75) return { label: 'Good Quality', color: 'text-blue-600', stroke: 'stroke-blue-500' };
    if (pct >= 50) return { label: 'Moderate Defects', color: 'text-amber-600', stroke: 'stroke-amber-500' };
    return { label: 'Needs Remediation', color: 'text-red-600', stroke: 'stroke-red-500' };
  };

  const health = getHealthLevel(cleanlinessPct);

  return (
    <div className="space-y-6">
      <div className="card p-6 border-l-4 border-l-navy bg-white shadow-sm flex flex-col sm:flex-row sm:items-center justify-between gap-4">
        <div>
          <h1 className="text-2xl font-bold text-navy flex items-center gap-2">
            <span>📊</span> Training Center Cleanliness Scorecards
          </h1>
          <p className="text-sm text-gray-600 mt-1">
            Inspect authoritative data cleanliness indices, evaluate active anomaly distributions, and analyze batch completion vs dropout rates across affiliated centers.
          </p>
        </div>
      </div>

      {/* Center Selector Bar */}
      <div className="bg-white rounded-xl shadow-sm border border-slate-200 p-5 flex flex-wrap items-center justify-between gap-4">
        <div className="flex items-center space-x-3 w-full sm:w-auto">
          <label className="text-sm font-bold text-slate-700 whitespace-nowrap">
            Select Training Center:
          </label>
          {centersLoading ? (
            <span className="text-xs text-slate-400">Loading affiliated centers...</span>
          ) : centersError ? (
            <span className="text-xs text-red-500">Failed to load centers</span>
          ) : centers.length === 0 ? (
            <span className="text-xs text-slate-500">No centers affiliated</span>
          ) : (
            <select
              value={effectiveCenterId ?? ''}
              onChange={(e) => setSelectedCenterId(Number(e.target.value))}
              className="text-sm border border-slate-300 rounded-lg px-3 py-2 bg-white focus:outline-none focus:ring-2 focus:ring-blue-500 font-medium text-slate-800"
              data-testid="select-center-scorecard"
            >
              {centers.map((c) => (
                <option key={c.id} value={c.id}>
                  {c.centerName} ({c.centerCode})
                </option>
              ))}
            </select>
          )}
        </div>

        {scorecard && (
          <div className="text-xs text-slate-500">
            Evaluated at: <span className="font-semibold text-slate-700">{new Date(scorecard.evaluatedAt).toLocaleString()}</span>
          </div>
        )}
      </div>

      {scorecardLoading ? (
        <div className="p-16 text-center bg-white rounded-xl shadow-sm border border-slate-200">
          <PageLoading message="Calculating authoritative center cleanliness metrics..." />
        </div>
      ) : scorecardError ? (
        <div className="p-10 text-center bg-white rounded-xl shadow-sm border border-red-200 text-red-600 space-y-3">
          <div className="text-base font-bold">Failed to load center scorecard</div>
          <p className="text-xs text-slate-500">Ensure the center belongs to your provider tenancy.</p>
          <button
            onClick={() => refetchScorecard()}
            className="px-4 py-2 bg-red-100 hover:bg-red-200 text-red-800 text-xs font-semibold rounded-lg"
          >
            Retry Calculation
          </button>
        </div>
      ) : scorecard ? (
        <div className="space-y-6">
          {/* Top Row: Cleanliness Index Gauge + Outcome Metrics */}
          <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
            {/* 1. Authoritative Cleanliness Index Gauge */}
            <div className="bg-white rounded-xl shadow-sm border border-slate-200 p-6 flex flex-col justify-between" data-testid="cleanliness-gauge-card">
              <div className="border-b border-slate-100 pb-3 flex items-center justify-between">
                <div>
                  <h3 className="text-base font-bold text-slate-800">Cleanliness Index</h3>
                  <p className="text-xs text-slate-500">Phase 28 Authoritative Quality Gauge</p>
                </div>
                <span className={`text-xs font-bold px-2.5 py-1 rounded-full bg-slate-100 ${health.color}`}>
                  {health.label}
                </span>
              </div>

              {/* Gauge Display */}
              <div className="my-6 flex flex-col items-center justify-center">
                {cleanlinessPct == null ? (
                  <div className="text-center py-6 space-y-2" data-testid="no-data-cleanliness">
                    <div className="text-3xl text-slate-300">📊</div>
                    <div className="text-sm font-semibold text-slate-600">Insufficient Evaluated Cohort</div>
                    <div className="text-xs text-slate-400 max-w-xs">
                      No non-deleted records have been evaluated for this center yet. Score is not misrepresented as 100%.
                    </div>
                  </div>
                ) : (
                  <div className="relative flex items-center justify-center" data-testid="cleanliness-percentage-display">
                    {/* SVG Circular Progress Gauge */}
                    <svg className="w-48 h-48 transform -rotate-90">
                      <circle
                        cx="96"
                        cy="96"
                        r="80"
                        className="stroke-slate-100"
                        strokeWidth="14"
                        fill="transparent"
                      />
                      <circle
                        cx="96"
                        cy="96"
                        r="80"
                        className={`${health.stroke} transition-all duration-1000 ease-out`}
                        strokeWidth="14"
                        strokeDasharray={2 * Math.PI * 80}
                        strokeDashoffset={2 * Math.PI * 80 * (1 - cleanlinessPct / 100)}
                        strokeLinecap="round"
                        fill="transparent"
                      />
                    </svg>

                    <div className="absolute flex flex-col items-center justify-center text-center">
                      <span className="text-4xl font-extrabold text-slate-800 font-mono">
                        {cleanlinessPct.toFixed(2)}%
                      </span>
                      <span className="text-xs font-medium text-slate-400 uppercase tracking-wider mt-1">
                        Clean Data
                      </span>
                    </div>
                  </div>
                )}
              </div>

              {/* Formula & Supporting Cohort Stats */}
              <div className="bg-slate-50 p-3 rounded-lg border border-slate-200 text-xs text-slate-600 space-y-1.5">
                <div className="flex justify-between">
                  <span className="text-slate-500">Authoritative Formula:</span>
                  <code className="font-mono text-slate-800 font-semibold">{scorecard.cleanlinessFormula}</code>
                </div>
                <div className="flex justify-between">
                  <span className="text-slate-500">Eligible Domain Records:</span>
                  <span className="font-semibold text-slate-800">{scorecard.eligiblePopulation}</span>
                </div>
                <div className="flex justify-between">
                  <span className="text-slate-500">Distinct Affected Records:</span>
                  <span className="font-semibold text-red-600">{scorecard.distinctAffectedEntities}</span>
                </div>
                <div className="flex justify-between">
                  <span className="text-slate-500">Clean Records Estimate:</span>
                  <span className="font-semibold text-emerald-600">{scorecard.cleanEntitiesEstimate}</span>
                </div>
              </div>
            </div>

            {/* 2. Completion vs Dropout Outcome Comparison Chart */}
            <div className="bg-white rounded-xl shadow-sm border border-slate-200 p-6 flex flex-col justify-between" data-testid="outcomes-chart-card">
              <div className="border-b border-slate-100 pb-3 flex items-center justify-between">
                <div>
                  <h3 className="text-base font-bold text-slate-800">Enrollment Outcomes</h3>
                  <p className="text-xs text-slate-500">Completion vs Dropout Rate Comparison</p>
                </div>
                <span className="text-xs font-semibold text-slate-500">
                  Total Enrollments: {scorecard.totalEnrollments}
                </span>
              </div>

              {/* Comparison Bars */}
              <div className="my-6 space-y-6">
                {scorecard.totalEnrollments === 0 ? (
                  <div className="text-center py-6 text-slate-400 text-xs italic">
                    No enrollments recorded for this training center yet.
                  </div>
                ) : (
                  <>
                    {/* Completion Rate Bar */}
                    <div className="space-y-2">
                      <div className="flex justify-between text-xs font-semibold">
                        <span className="text-emerald-700 flex items-center">
                          <span className="w-2.5 h-2.5 rounded-full bg-emerald-500 inline-block mr-1.5"></span>
                          Completion Rate (Status: COMPLETED)
                        </span>
                        <span className="font-mono text-slate-800">
                          {completionPct != null ? `${completionPct.toFixed(2)}%` : 'N/A'}{' '}
                          <span className="text-slate-400 font-normal">
                            ({scorecard.completedEnrollments}/{scorecard.totalEnrollments})
                          </span>
                        </span>
                      </div>
                      <div className="w-full bg-slate-100 rounded-full h-3.5 overflow-hidden">
                        <div
                          className="bg-emerald-500 h-full rounded-full transition-all duration-700"
                          style={{ width: `${Math.min(100, completionPct ?? 0)}%` }}
                        ></div>
                      </div>
                    </div>

                    {/* Dropout Rate Bar */}
                    <div className="space-y-2">
                      <div className="flex justify-between text-xs font-semibold">
                        <span className="text-red-700 flex items-center">
                          <span className="w-2.5 h-2.5 rounded-full bg-red-500 inline-block mr-1.5"></span>
                          Dropout Rate (Status: DROPPED)
                        </span>
                        <span className="font-mono text-slate-800">
                          {dropoutPct != null ? `${dropoutPct.toFixed(2)}%` : 'N/A'}{' '}
                          <span className="text-slate-400 font-normal">
                            ({scorecard.droppedEnrollments}/{scorecard.totalEnrollments})
                          </span>
                        </span>
                      </div>
                      <div className="w-full bg-slate-100 rounded-full h-3.5 overflow-hidden">
                        <div
                          className="bg-red-500 h-full rounded-full transition-all duration-700"
                          style={{ width: `${Math.min(100, dropoutPct ?? 0)}%` }}
                        ></div>
                      </div>
                    </div>

                    {/* In-Progress Cohort */}
                    <div className="flex justify-between text-xs text-slate-600 bg-slate-50 p-2.5 rounded border border-slate-200">
                      <span>Currently Active / In Progress:</span>
                      <span className="font-bold text-blue-600 font-mono">
                        {scorecard.inProgressEnrollments} trainees
                      </span>
                    </div>
                  </>
                )}
              </div>

              {/* Data Criteria Disclosure */}
              <div className="text-[11px] text-slate-400 border-t border-slate-100 pt-3">
                * Note: Evaluated directly from official enrollment records; completion requires completed status, and dropout is strictly recorded via official exit records.
              </div>
            </div>
          </div>

          {/* Bottom Row: Active Issues Breakdown & Quick Remediation Link */}
          <div className="bg-white rounded-xl shadow-sm border border-slate-200 p-6 space-y-4">
            <div className="flex items-center justify-between border-b border-slate-100 pb-3">
              <div>
                <h3 className="text-base font-bold text-slate-800">
                  Active Issues in {scorecard.centerName}
                </h3>
                <p className="text-xs text-slate-500">
                  Total Active Defect Cohort: {scorecard.totalActiveIssues} open anomalies
                </p>
              </div>

              <button
                onClick={() => navigate(`/provider/data-quality?centerId=${scorecard.centerId}`)}
                className="px-4 py-2 bg-blue-600 hover:bg-blue-700 text-white text-xs font-semibold rounded-lg shadow-sm"
                data-testid="btn-view-tracker-center"
              >
                Inspect Issues in Tracker →
              </button>
            </div>

            <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
              {/* Severity Breakdown */}
              <div className="bg-slate-50 p-4 rounded-lg border border-slate-200 space-y-2">
                <h4 className="text-xs font-bold uppercase tracking-wider text-slate-600">
                  By Severity
                </h4>
                <div className="grid grid-cols-4 gap-2 text-center">
                  <div className="bg-red-100 border border-red-200 p-2 rounded">
                    <div className="text-xs font-bold text-red-800">CRITICAL</div>
                    <div className="text-lg font-extrabold text-red-900 font-mono">
                      {scorecard.activeIssuesBySeverity?.['CRITICAL'] ?? 0}
                    </div>
                  </div>
                  <div className="bg-amber-100 border border-amber-200 p-2 rounded">
                    <div className="text-xs font-bold text-amber-800">HIGH</div>
                    <div className="text-lg font-extrabold text-amber-900 font-mono">
                      {scorecard.activeIssuesBySeverity?.['HIGH'] ?? 0}
                    </div>
                  </div>
                  <div className="bg-blue-100 border border-blue-200 p-2 rounded">
                    <div className="text-xs font-bold text-blue-800">MEDIUM</div>
                    <div className="text-lg font-extrabold text-blue-900 font-mono">
                      {scorecard.activeIssuesBySeverity?.['MEDIUM'] ?? 0}
                    </div>
                  </div>
                  <div className="bg-slate-100 border border-slate-200 p-2 rounded">
                    <div className="text-xs font-bold text-slate-700">LOW</div>
                    <div className="text-lg font-extrabold text-slate-800 font-mono">
                      {scorecard.activeIssuesBySeverity?.['LOW'] ?? 0}
                    </div>
                  </div>
                </div>
              </div>

              {/* Category Breakdown */}
              <div className="bg-slate-50 p-4 rounded-lg border border-slate-200 space-y-2">
                <h4 className="text-xs font-bold uppercase tracking-wider text-slate-600">
                  By Category
                </h4>
                <div className="grid grid-cols-4 gap-2 text-center">
                  <div className="bg-white border border-slate-200 p-2 rounded">
                    <div className="text-[11px] font-bold text-slate-700">COMPLETENESS</div>
                    <div className="text-lg font-extrabold text-slate-800 font-mono">
                      {scorecard.activeIssuesByCategory?.['COMPLETENESS'] ?? 0}
                    </div>
                  </div>
                  <div className="bg-white border border-slate-200 p-2 rounded">
                    <div className="text-[11px] font-bold text-slate-700">CONSISTENCY</div>
                    <div className="text-lg font-extrabold text-slate-800 font-mono">
                      {scorecard.activeIssuesByCategory?.['CONSISTENCY'] ?? 0}
                    </div>
                  </div>
                  <div className="bg-white border border-slate-200 p-2 rounded">
                    <div className="text-[11px] font-bold text-slate-700">VALIDITY</div>
                    <div className="text-lg font-extrabold text-slate-800 font-mono">
                      {scorecard.activeIssuesByCategory?.['VALIDITY'] ?? 0}
                    </div>
                  </div>
                  <div className="bg-white border border-slate-200 p-2 rounded">
                    <div className="text-[11px] font-bold text-slate-700">DUPLICATE</div>
                    <div className="text-lg font-extrabold text-slate-800 font-mono">
                      {scorecard.activeIssuesByCategory?.['DUPLICATE'] ?? 0}
                    </div>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </div>
      ) : null}
    </div>
  );
}
