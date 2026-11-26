import React, { useEffect, useMemo, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import {
  CalendarIcon,
  TableIcon,
  SearchIcon as Search,
  CheckCircleIcon as CheckCircle2,
  ClockIcon as Clock,
  UsersIcon as Users,
  AwardIcon as Award,
  ChevronLeftIcon as ChevronLeft,
  ChevronRightIcon as ChevronRight,
  RefreshCwIcon as RefreshCw,
  ExternalLinkIcon as ExternalLink,
  AlertCircleIcon as AlertCircle,
} from '@/components/common/Icons';
import { useAuth } from '@/context/AuthContext';
import { getAssessorAssessments } from '@/api/assessorApi';
import type { AssessorAssessmentSummary } from '@/types/assessor';
import { OpenBatchAssessments } from './OpenBatchAssessments';

export function AssessorWorkbenchPage(): React.JSX.Element {
  const { user } = useAuth();
  const navigate = useNavigate();

  const [assessments, setAssessments] = useState<AssessorAssessmentSummary[]>([]);
  const [loading, setLoading] = useState<boolean>(true);
  const [error, setError] = useState<string | null>(null);

  const [viewMode, setViewMode] = useState<'table' | 'calendar'>('table');
  const [search, setSearch] = useState<string>('');
  const [statusFilter, setStatusFilter] = useState<'ALL' | 'SCHEDULED' | 'FINALIZED'>('ALL');

  // Calendar navigation state
  const [currentDate, setCurrentDate] = useState<Date>(new Date());

  const fetchAssessments = async () => {
    setLoading(true);
    setError(null);
    try {
      const data = await getAssessorAssessments({
        status: statusFilter !== 'ALL' ? statusFilter : undefined,
        search: search.trim() || undefined,
      });
      setAssessments(data);
    } catch (err: any) {
      setError(err?.message || 'Failed to load assigned assessments.');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchAssessments();
  }, [statusFilter]);

  const handleSearchSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    fetchAssessments();
  };

  // Metrics
  const metrics = useMemo(() => {
    const total = assessments.length;
    const scheduled = assessments.filter((a) => !a.isFinalized).length;
    const finalized = assessments.filter((a) => a.isFinalized).length;
    const candidates = assessments.reduce(
      (sum, a) => sum + (a.totalCandidates || a.candidateCount || 0),
      0,
    );
    return { total, scheduled, finalized, candidates };
  }, [assessments]);

  // Calendar calculations
  const calendarDays = useMemo(() => {
    const year = currentDate.getFullYear();
    const month = currentDate.getMonth();

    const firstDayIndex = new Date(year, month, 1).getDay();
    const daysInMonth = new Date(year, month + 1, 0).getDate();

    const days: { date: Date; dateStr: string; isCurrentMonth: boolean; items: AssessorAssessmentSummary[] }[] = [];

    // Previous month padding
    const prevMonthDays = new Date(year, month, 0).getDate();
    for (let i = firstDayIndex - 1; i >= 0; i--) {
      const d = new Date(year, month - 1, prevMonthDays - i);
      const dateStr = d.toISOString().split('T')[0];
      days.push({
        date: d,
        dateStr,
        isCurrentMonth: false,
        items: assessments.filter((a) => a.assessmentDate === dateStr),
      });
    }

    // Current month days
    for (let day = 1; day <= daysInMonth; day++) {
      const d = new Date(year, month, day);
      const dateStr = `${year}-${String(month + 1).padStart(2, '0')}-${String(day).padStart(2, '0')}`;
      days.push({
        date: d,
        dateStr,
        isCurrentMonth: true,
        items: assessments.filter((a) => a.assessmentDate === dateStr),
      });
    }

    // Next month padding to reach complete 35 or 42 grid
    const totalSlots = days.length > 35 ? 42 : 35;
    const remaining = totalSlots - days.length;
    for (let i = 1; i <= remaining; i++) {
      const d = new Date(year, month + 1, i);
      const dateStr = d.toISOString().split('T')[0];
      days.push({
        date: d,
        dateStr,
        isCurrentMonth: false,
        items: assessments.filter((a) => a.assessmentDate === dateStr),
      });
    }

    return days;
  }, [currentDate, assessments]);

  const monthYearLabel = currentDate.toLocaleString('default', {
    month: 'long',
    year: 'numeric',
  });

  return (
    <div className="space-y-6 max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-6">
      {/* Header Banner */}
      <div className="bg-gradient-to-r from-navy-900 via-navy-800 to-navy-700 text-white rounded-xl p-6 shadow-md border border-navy-600">
        <div className="flex flex-col md:flex-row md:items-center justify-between gap-4">
          <div>
            <div className="flex items-center gap-3">
              <h1 className="text-2xl font-bold tracking-tight">Assessor Workbench</h1>
              <span className="px-2.5 py-0.5 text-xs font-semibold rounded-full bg-blue-500/20 text-blue-200 border border-blue-400/30">
                ROLE_ASSESSOR
              </span>
            </div>
            <p className="mt-1 text-sm text-gray-200">
              Evaluator: <span className="font-semibold">{user?.fullName || user?.username}</span> &bull; Authoritative Assessment Evaluation &amp; Certification Subsystem
            </p>
          </div>
          <div className="flex items-center gap-2">
            <button
              onClick={fetchAssessments}
              disabled={loading}
              className="inline-flex items-center gap-1.5 px-3.5 py-2 text-xs font-medium rounded-lg bg-white/10 hover:bg-white/20 text-white transition border border-white/20 focus:outline-none"
              title="Refresh Assignments"
            >
              <RefreshCw className={`h-4 w-4 ${loading ? 'animate-spin' : ''}`} />
              Refresh
            </button>
          </div>
        </div>

        {/* Metrics Grid */}
        <div className="grid grid-cols-2 sm:grid-cols-4 gap-4 mt-6 pt-6 border-t border-white/10">
          <div className="bg-white/5 rounded-lg p-3 border border-white/10">
            <div className="flex items-center justify-between text-xs text-gray-300">
              <span>Assigned Assessments</span>
              <Award className="h-4 w-4 text-blue-300" />
            </div>
            <div className="mt-1 text-2xl font-bold text-white">{metrics.total}</div>
          </div>
          <div className="bg-white/5 rounded-lg p-3 border border-white/10">
            <div className="flex items-center justify-between text-xs text-amber-200">
              <span>Scheduled / Pending</span>
              <Clock className="h-4 w-4 text-amber-300" />
            </div>
            <div className="mt-1 text-2xl font-bold text-amber-300">{metrics.scheduled}</div>
          </div>
          <div className="bg-white/5 rounded-lg p-3 border border-white/10">
            <div className="flex items-center justify-between text-xs text-emerald-200">
              <span>Finalized / Certified</span>
              <CheckCircle2 className="h-4 w-4 text-emerald-300" />
            </div>
            <div className="mt-1 text-2xl font-bold text-emerald-300">{metrics.finalized}</div>
          </div>
          <div className="bg-white/5 rounded-lg p-3 border border-white/10">
            <div className="flex items-center justify-between text-xs text-blue-200">
              <span>Total Candidates</span>
              <Users className="h-4 w-4 text-blue-300" />
            </div>
            <div className="mt-1 text-2xl font-bold text-white">{metrics.candidates}</div>
          </div>
        </div>
      </div>

      {/* Control Bar: Filters & View Switcher */}
      <div className="bg-white rounded-lg shadow-sm border border-gray-200 p-4 flex flex-col md:flex-row md:items-center justify-between gap-4">
        <form onSubmit={handleSearchSubmit} className="flex-1 flex items-center gap-3 max-w-lg">
          <div className="relative flex-1">
            <Search className="absolute left-3 top-1/2 -translate-y-1/2 h-4 w-4 text-gray-400" />
            <input
              type="text"
              value={search}
              onChange={(e) => setSearch(e.target.value)}
              placeholder="Search by code, course, or batch..."
              className="w-full pl-9 pr-3 py-2 text-sm border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-blue-500"
            />
          </div>
          <button
            type="submit"
            className="px-3.5 py-2 text-xs font-semibold text-white bg-blue-600 rounded-md hover:bg-blue-700 transition"
          >
            Search
          </button>
        </form>

        <div className="flex items-center gap-3">
          {/* Status Filter */}
          <div className="flex items-center gap-1.5 text-xs">
            <span className="text-gray-500 font-medium">Status:</span>
            <select
              value={statusFilter}
              onChange={(e) => setStatusFilter(e.target.value as any)}
              className="px-2.5 py-1.5 text-xs font-medium border border-gray-300 rounded-md bg-white text-gray-700 focus:outline-none focus:ring-2 focus:ring-blue-500"
            >
              <option value="ALL">All Statuses</option>
              <option value="SCHEDULED">Scheduled / In-Progress</option>
              <option value="FINALIZED">Finalized</option>
            </select>
          </div>

          {/* View Toggle */}
          <div className="inline-flex rounded-lg border border-gray-200 p-0.5 bg-gray-50">
            <button
              onClick={() => setViewMode('table')}
              className={`inline-flex items-center gap-1.5 px-3 py-1.5 text-xs font-semibold rounded-md transition ${
                viewMode === 'table'
                  ? 'bg-white text-blue-700 shadow-sm'
                  : 'text-gray-600 hover:text-gray-900'
              }`}
            >
              <TableIcon className="h-3.5 w-3.5" />
              Table
            </button>
            <button
              onClick={() => setViewMode('calendar')}
              className={`inline-flex items-center gap-1.5 px-3 py-1.5 text-xs font-semibold rounded-md transition ${
                viewMode === 'calendar'
                  ? 'bg-white text-blue-700 shadow-sm'
                  : 'text-gray-600 hover:text-gray-900'
              }`}
            >
              <CalendarIcon className="h-3.5 w-3.5" />
              Calendar
            </button>
          </div>
        </div>
      </div>

      {/* Error Alert */}
      {error && (
        <div className="p-4 rounded-lg bg-red-50 border border-red-200 text-red-800 flex items-start gap-3">
          <AlertCircle className="h-5 w-5 text-red-600 flex-shrink-0 mt-0.5" />
          <div className="text-sm">
            <p className="font-semibold">Unable to fetch assessment records</p>
            <p>{error}</p>
          </div>
        </div>
      )}

      <OpenBatchAssessments />
      {/* Main Content Area */}
      {loading ? (
        <div className="bg-white rounded-xl shadow-sm border border-gray-200 p-12 text-center text-gray-500">
          <RefreshCw className="h-8 w-8 text-blue-600 animate-spin mx-auto mb-3" />
          <p className="text-sm font-medium">Loading assigned assessments from database...</p>
        </div>
      ) : viewMode === 'table' ? (
        /* TABLE VIEW */
        <div className="bg-white rounded-xl shadow-sm border border-gray-200 overflow-hidden">
          {assessments.length === 0 ? (
            <div className="p-12 text-center text-gray-500">
              <Award className="h-10 w-10 text-gray-300 mx-auto mb-3" />
              <h3 className="text-base font-semibold text-gray-900">No Assessments Assigned</h3>
              <p className="text-sm text-gray-500 mt-1 max-w-sm mx-auto">
                No evaluation tasks have been assigned to your evaluator account matching the selected filter.
              </p>
            </div>
          ) : (
            <div className="overflow-x-auto">
              <table className="min-w-full divide-y divide-gray-200 text-left text-sm">
                <thead className="bg-gray-50 text-gray-600 font-semibold text-xs uppercase tracking-wider">
                  <tr>
                    <th scope="col" className="px-6 py-3.5">Assessment Code / Name</th>
                    <th scope="col" className="px-6 py-3.5">Course &amp; Batch</th>
                    <th scope="col" className="px-6 py-3.5">Center</th>
                    <th scope="col" className="px-6 py-3.5">Assessment Date</th>
                    <th scope="col" className="px-6 py-3.5">Candidates</th>
                    <th scope="col" className="px-6 py-3.5">Pass Mark</th>
                    <th scope="col" className="px-6 py-3.5">Status</th>
                    <th scope="col" className="px-6 py-3.5 text-right">Action</th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-gray-200 bg-white">
                  {assessments.map((item) => {
                    const asmId = item.id || item.assessmentId;
                    const totCand = item.totalCandidates ?? item.candidateCount ?? 0;
                    const scCand = item.scoredCandidates ?? item.scoredCount ?? 0;
                    return (
                      <tr key={asmId} className="hover:bg-gray-50/80 transition">
                        <td className="px-6 py-4">
                          <div className="font-semibold text-gray-900">{item.assessmentCode}</div>
                          <div className="text-xs text-gray-500 truncate max-w-xs">{item.assessmentName}</div>
                        </td>
                        <td className="px-6 py-4">
                          <div className="font-medium text-gray-800">{item.courseName || '—'}</div>
                          <div className="text-xs text-gray-500 font-mono">Batch: {item.batchCode || '—'}</div>
                        </td>
                        <td className="px-6 py-4 text-gray-700 text-xs">
                          {item.centerName || 'Assigned Center'}
                        </td>
                        <td className="px-6 py-4 text-gray-800 text-xs whitespace-nowrap">
                          {item.assessmentDate}
                        </td>
                        <td className="px-6 py-4 whitespace-nowrap">
                          <div className="text-xs font-semibold text-gray-900">
                            {scCand} / {totCand} Scored
                          </div>
                          <div className="w-24 bg-gray-200 rounded-full h-1.5 mt-1 overflow-hidden">
                            <div
                              className={`h-1.5 rounded-full ${
                                item.isFinalized ? 'bg-emerald-500' : 'bg-blue-600'
                              }`}
                              style={{
                                width: `${totCand > 0 ? (scCand / totCand) * 100 : 0}%`,
                              }}
                            />
                          </div>
                        </td>
                        <td className="px-6 py-4 text-xs text-gray-700 whitespace-nowrap">
                          <span className="font-medium">{item.passScore}</span> / {item.maximumScore}
                        </td>
                        <td className="px-6 py-4 whitespace-nowrap">
                          {item.isFinalized ? (
                            <span className="inline-flex items-center gap-1 px-2.5 py-1 rounded-full text-xs font-semibold bg-emerald-50 text-emerald-700 border border-emerald-200">
                              <CheckCircle2 className="h-3 w-3 text-emerald-500" />
                              Finalized
                            </span>
                          ) : (
                            <span className="inline-flex items-center gap-1 px-2.5 py-1 rounded-full text-xs font-semibold bg-amber-50 text-amber-700 border border-amber-200">
                              <Clock className="h-3 w-3 text-amber-500" />
                              Scheduled
                            </span>
                          )}
                        </td>
                        <td className="px-6 py-4 text-right whitespace-nowrap">
                          <button
                            onClick={() => navigate(`/assessor/assessments/${asmId}`)}
                            className={`inline-flex items-center gap-1.5 px-3 py-1.5 rounded-md text-xs font-semibold transition shadow-sm ${
                              item.isFinalized
                                ? 'bg-gray-100 hover:bg-gray-200 text-gray-700 border border-gray-300'
                                : 'bg-blue-600 hover:bg-blue-700 text-white'
                            }`}
                          >
                            {item.isFinalized ? 'View Results' : 'Scoring Grid'}
                            <ExternalLink className="h-3 w-3" />
                          </button>
                        </td>
                      </tr>
                    );
                  })}
                </tbody>
              </table>
            </div>
          )}
        </div>
      ) : (
        /* CALENDAR VIEW */
        <div className="bg-white rounded-xl shadow-sm border border-gray-200 p-6 space-y-4">
          {/* Month Controls */}
          <div className="flex items-center justify-between">
            <h2 className="text-lg font-bold text-gray-900">{monthYearLabel}</h2>
            <div className="flex items-center gap-2">
              <button
                onClick={() => setCurrentDate(new Date(currentDate.getFullYear(), currentDate.getMonth() - 1, 1))}
                className="p-1.5 rounded-md border border-gray-300 hover:bg-gray-100 transition text-gray-700"
                title="Previous Month"
              >
                <ChevronLeft className="h-4 w-4" />
              </button>
              <button
                onClick={() => setCurrentDate(new Date())}
                className="px-3 py-1 text-xs font-semibold rounded-md border border-gray-300 hover:bg-gray-100 transition text-gray-700"
              >
                Today
              </button>
              <button
                onClick={() => setCurrentDate(new Date(currentDate.getFullYear(), currentDate.getMonth() + 1, 1))}
                className="p-1.5 rounded-md border border-gray-300 hover:bg-gray-100 transition text-gray-700"
                title="Next Month"
              >
                <ChevronRight className="h-4 w-4" />
              </button>
            </div>
          </div>

          {/* Calendar Grid */}
          <div className="grid grid-cols-7 gap-px bg-gray-200 rounded-lg overflow-hidden border border-gray-200">
            {['Sun', 'Mon', 'Tue', 'Wed', 'Thu', 'Fri', 'Sat'].map((day) => (
              <div key={day} className="bg-gray-50 py-2 text-center text-xs font-semibold text-gray-600">
                {day}
              </div>
            ))}

            {calendarDays.map((cell, idx) => {
              const isToday = cell.dateStr === new Date().toISOString().split('T')[0];
              return (
                <div
                  key={idx}
                  className={`min-h-[105px] p-2 bg-white flex flex-col justify-between transition ${
                    !cell.isCurrentMonth ? 'bg-gray-50/60 text-gray-400' : 'text-gray-800'
                  } ${isToday ? 'ring-2 ring-blue-500 ring-inset' : ''}`}
                >
                  <div className="flex items-center justify-between text-xs font-medium">
                    <span className={isToday ? 'px-1.5 py-0.5 rounded-full bg-blue-600 text-white font-bold' : ''}>
                      {cell.date.getDate()}
                    </span>
                    {cell.items.length > 0 && (
                      <span className="text-[10px] text-gray-500 font-mono">
                        {cell.items.length} {cell.items.length === 1 ? 'task' : 'tasks'}
                      </span>
                    )}
                  </div>

                  <div className="space-y-1.5 mt-1 overflow-y-auto max-h-[85px]">
                    {cell.items.map((item) => (
                      <button
                        key={item.id || item.assessmentId}
                        onClick={() => navigate(`/assessor/assessments/${item.id || item.assessmentId}`)}
                        className={`w-full text-left p-1 rounded text-[11px] truncate block font-medium transition border ${
                          item.isFinalized
                            ? 'bg-emerald-50 text-emerald-800 border-emerald-200 hover:bg-emerald-100'
                            : 'bg-blue-50 text-blue-800 border-blue-200 hover:bg-blue-100'
                        }`}
                        title={`${item.assessmentCode}: ${item.assessmentName} (${item.totalCandidates || item.candidateCount || 0} candidates)`}
                      >
                        <div className="font-bold truncate">{item.assessmentCode}</div>
                        <div className="text-[10px] opacity-80 truncate">{item.courseName}</div>
                      </button>
                    ))}
                  </div>
                </div>
              );
            })}
          </div>
        </div>
      )}
    </div>
  );
}
