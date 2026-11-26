import React, { useEffect, useMemo, useState } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import {
  ArrowLeftIcon as ArrowLeft,
  SaveIcon as Save,
  CheckCircleIcon as CheckCircle,
  CheckCircleIcon as CheckCircle2,
  AlertTriangleIcon as AlertTriangle,
  DownloadIcon as Download,
  ClockIcon as Clock,
  XCircleIcon as XCircle,
  RefreshCwIcon as RefreshCw,
  FileTextIcon as FileText,
  AlertCircleIcon as AlertCircle,
  UsersIcon as Users,
} from '@/components/common/Icons';
import {
  getAssessmentRoster,
  saveDraftScores,
  finalizeAssessment,
  issuePendingCertificates,
  downloadCertificatePdf,
} from '@/api/assessorApi';
import type {
  AssessorRosterResponse,
  CandidateScoreInput,
} from '@/types/assessor';
import { StaffCareerSkills } from '@/components/career/CareerSkillsPanel';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { completeBatchAssessment, getPublishedBatch } from '@/api/batchAssessmentApi';
import { AssessmentDocumentButton, BatchAssessmentSummary } from '@/components/assessment/BatchAssessmentSummary';

interface CandidateRowState {
  appeared: boolean;
  theory: string;
  practical: string;
  viva: string;
  remarks: string;
}

export function AssessorScoringPage(): React.JSX.Element {
  const { assessmentId: assessmentIdParam } = useParams<{ assessmentId: string }>();
  const assessmentId = Number(assessmentIdParam);
  const navigate = useNavigate();

  const [roster, setRoster] = useState<AssessorRosterResponse | null>(null);
  const [loading, setLoading] = useState<boolean>(true);
  const [savingDraft, setSavingDraft] = useState<boolean>(false);
  const [finalizing, setFinalizing] = useState<boolean>(false);
  const [error, setError] = useState<string | null>(null);
  const [successMessage, setSuccessMessage] = useState<string | null>(null);

  // Editable form state per traineeId
  const [rowStates, setRowStates] = useState<Record<number, CandidateRowState>>({});
  const [isDirty, setIsDirty] = useState<boolean>(false);
  const [showFinalizeModal, setShowFinalizeModal] = useState<boolean>(false);
  const [downloadingCertId, setDownloadingCertId] = useState<number | null>(null);
  const [skillCandidate, setSkillCandidate] = useState('');

  // Load roster data
  const loadRoster = async () => {
    if (!assessmentId || isNaN(assessmentId)) {
      setError('Invalid Assessment ID.');
      setLoading(false);
      return;
    }

    setLoading(true);
    setError(null);
    try {
      const data = await getAssessmentRoster(assessmentId);
      setRoster(data);

      // Initialize rowStates
      const initialMap: Record<number, CandidateRowState> = {};
      data.candidates.forEach((c) => {
        initialMap[c.traineeId] = {
          appeared: c.appeared ?? c.appearedFlag ?? true,
          theory: c.theoryScore !== null ? String(c.theoryScore) : '',
          practical: c.practicalScore !== null ? String(c.practicalScore) : '',
          viva: c.vivaScore !== null ? String(c.vivaScore) : '',
          remarks: c.remarks || '',
        };
      });
      setRowStates(initialMap);
      setIsDirty(false);
    } catch (err: any) {
      setError(err?.message || 'Failed to load candidate roster.');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadRoster();
  }, [assessmentId]);

  // Window unload guard if unsaved changes exist
  useEffect(() => {
    const handleBeforeUnload = (e: BeforeUnloadEvent) => {
      if (isDirty && !roster?.assessment.isFinalized) {
        e.preventDefault();
        e.returnValue = '';
      }
    };
    window.addEventListener('beforeunload', handleBeforeUnload);
    return () => window.removeEventListener('beforeunload', handleBeforeUnload);
  }, [isDirty, roster]);

  const assessment = roster?.assessment;
  const isFinalized = !!assessment?.isFinalized ||
    assessment?.lifecycleStatusCode === 'FINALIZED' ||
    assessment?.lifecycleStatusCode === 'COMPLETED' ||
    (roster?.candidates != null && roster.candidates.length > 0 && roster.candidates.every((c) => c.isFinalized));
  const queryClient = useQueryClient();
  const publishedBatchKey = ['assessor-published-batch', assessmentId];
  const publishedBatch = useQuery({
    queryKey: publishedBatchKey, queryFn: () => getPublishedBatch(assessmentId),
    enabled: !!roster?.managedBatch,
  });
  const completion = useMutation({
    mutationFn: () => completeBatchAssessment(assessmentId),
    onSuccess: async (publication) => {
      await loadRoster();
      if (publishedBatch.data) queryClient.setQueryData(publishedBatchKey, { ...publishedBatch.data, publication });
    },
  });
  const scoringLocked = isFinalized || (!!roster?.managedBatch && (!roster.checkedIn || !publishedBatch.data?.publication.completedAt));

  const maxScore = assessment?.maximumScore ?? 100;
  const passScore = assessment?.passScore ?? maxScore / 2;
  const theoryMax = assessment?.theoryMaxScore ?? assessment?.theoryMax ?? 30;
  const practicalMax = assessment?.practicalMaxScore ?? assessment?.practicalMax ?? 50;
  const vivaMax = assessment?.vivaMaxScore ?? assessment?.vivaMax ?? 20;

  // Handle cell edits
  const handleScoreChange = (
    traineeId: number,
    field: 'theory' | 'practical' | 'viva' | 'remarks',
    val: string,
  ) => {
    if (scoringLocked) return;
    setRowStates((prev) => ({
      ...prev,
      [traineeId]: {
        ...prev[traineeId],
        [field]: val,
      },
    }));
    setIsDirty(true);
    setSuccessMessage(null);
  };

  // Keyboard navigation handler for inputs
  const handleKeyDown = (
    e: React.KeyboardEvent<HTMLInputElement>,
    rowIndex: number,
    fieldIndex: number,
  ) => {
    if (e.key === 'Enter') {
      e.preventDefault();
      // Move to next cell or next row's first input
      const nextField = fieldIndex < 2 ? fieldIndex + 1 : 0;
      const nextRow = fieldIndex < 2 ? rowIndex : rowIndex + 1;
      const nextInput = document.querySelector<HTMLInputElement>(
        `input[data-row="${nextRow}"][data-col="${nextField}"]`,
      );
      if (nextInput) {
        nextInput.focus();
        nextInput.select();
      }
    }
  };

  // Compute live row calculations and validations
  const computedCandidates = useMemo(() => {
    if (!roster) return [];

    return roster.candidates.map((c, idx) => {
      const row = rowStates[c.traineeId] || {
        appeared: true,
        theory: '',
        practical: '',
        viva: '',
        remarks: '',
      };

      const appeared = row.appeared;
      const theoryVal = row.theory.trim() !== '' ? parseFloat(row.theory) : null;
      const practicalVal = row.practical.trim() !== '' ? parseFloat(row.practical) : null;
      const vivaVal = row.viva.trim() !== '' ? parseFloat(row.viva) : null;

      const hasInvalidTheory = theoryVal !== null && (isNaN(theoryVal) || theoryVal < 0 || theoryVal > theoryMax);
      const hasInvalidPractical = practicalVal !== null && (isNaN(practicalVal) || practicalVal < 0 || practicalVal > practicalMax);
      const hasInvalidViva = vivaVal !== null && (isNaN(vivaVal) || vivaVal < 0 || vivaVal > vivaMax);

      let total: number | null = null;
      let percentage: number | null = null;
      let outcomePreview: 'UNASSESSED' | 'PENDING' | 'PASS' | 'FAIL' | 'ABSENT' = 'UNASSESSED';

      if (!appeared) {
        total = 0;
        percentage = 0;
        outcomePreview = 'ABSENT';
      } else {
        const hasAnyScore = theoryVal !== null || practicalVal !== null || vivaVal !== null;
        const hasAllScores = theoryVal !== null && practicalVal !== null && vivaVal !== null;

        if (hasAnyScore) {
          total = [theoryVal, practicalVal, vivaVal].reduce<number>((sum, score) => sum + Math.round((score ?? 0) * 100), 0) / 100;
          percentage = Math.round(((total / maxScore) * 100) * 100) / 100;

          if (isFinalized) {
            outcomePreview = (c.assessmentOutcomeCode || c.outcome || 'PENDING') as any;
          } else if (hasAllScores && !hasInvalidTheory && !hasInvalidPractical && !hasInvalidViva) {
            outcomePreview = total >= passScore ? 'PASS' : 'FAIL';
          } else {
            outcomePreview = 'PENDING';
          }
        } else {
          outcomePreview = isFinalized
            ? ((c.assessmentOutcomeCode || c.outcome || 'PENDING') as any)
            : 'UNASSESSED';
        }
      }

      return {
        ...c,
        rowIndex: idx,
        rowState: row,
        appeared,
        theoryVal,
        practicalVal,
        vivaVal,
        total,
        percentage,
        outcomePreview,
        hasErrors: hasInvalidTheory || hasInvalidPractical || hasInvalidViva,
        hasInvalidTheory,
        hasInvalidPractical,
        hasInvalidViva,
        isMissingComponent: appeared && (theoryVal === null || practicalVal === null || vivaVal === null),
      };
    });
  }, [roster, rowStates, isFinalized, theoryMax, practicalMax, vivaMax, maxScore, passScore]);

  // Overall Statistics Ribbon
  const stats = useMemo(() => {
    const total = computedCandidates.length;
    const absent = computedCandidates.filter((c) => !c.appeared).length;
    const appeared = total - absent;
    const passed = computedCandidates.filter((c) => c.outcomePreview === 'PASS').length;
    const failed = computedCandidates.filter((c) => c.outcomePreview === 'FAIL').length;
    const hasAnyErrors = computedCandidates.some((c) => c.hasErrors);
    const hasMissingForFinalize = computedCandidates.some((c) => c.isMissingComponent);
    return { total, absent, appeared, passed, failed, hasAnyErrors, hasMissingForFinalize };
  }, [computedCandidates]);

  // Save Draft Payload Assembly
  const buildPayload = (): CandidateScoreInput[] => {
    return computedCandidates.map((c) => ({
      traineeId: c.traineeId,
      enrollmentId: c.enrollmentId,
      appeared: c.rowState.appeared,
      theoryScore: c.rowState.appeared && c.rowState.theory.trim() !== '' ? parseFloat(c.rowState.theory) : null,
      practicalScore: c.rowState.appeared && c.rowState.practical.trim() !== '' ? parseFloat(c.rowState.practical) : null,
      vivaScore: c.rowState.appeared && c.rowState.viva.trim() !== '' ? parseFloat(c.rowState.viva) : null,
      remarks: c.rowState.remarks || undefined,
    }));
  };

  const handleSaveDraft = async () => {
    if (stats.hasAnyErrors) {
      setError('Please fix all invalid scores (out of bounds or negative) before saving.');
      return;
    }

    setSavingDraft(true);
    setError(null);
    setSuccessMessage(null);
    try {
      const candidates = buildPayload();
      const resp = await saveDraftScores(assessmentId, candidates);
      setSuccessMessage(
        `Draft saved successfully! ${resp.totalProcessed ?? resp.processedCandidates ?? 0} candidate records saved.`,
      );
      setIsDirty(false);
      await loadRoster();
    } catch (err: any) {
      setError(err?.message || 'Failed to save draft scores.');
    } finally {
      setSavingDraft(false);
    }
  };

  const handleFinalizeConfirm = async () => {
    if (stats.hasAnyErrors) {
      setError('Please resolve score validation errors before finalization.');
      setShowFinalizeModal(false);
      return;
    }
    if (stats.hasMissingForFinalize) {
      setError('All appeared candidates must have Theory, Practical, and Viva marks entered before finalization.');
      setShowFinalizeModal(false);
      return;
    }

    setFinalizing(true);
    setError(null);
    setSuccessMessage(null);
    try {
      const candidates = buildPayload();
      const resp = await finalizeAssessment(assessmentId, candidates);
      setSuccessMessage(
        `Assessment finalized. ${resp.passedCount} candidates passed and ${resp.certificatesIssued} certificates were issued.`,
      );
      setIsDirty(false);
      setShowFinalizeModal(false);
      await loadRoster();
    } catch (err: any) {
      setError(err?.message || 'Failed to finalize assessment.');
    } finally {
      setFinalizing(false);
    }
  };

  const handleDownloadCert = async (certId?: number) => {
    if (!certId) return;
    setDownloadingCertId(certId);
    try {
      const res = await downloadCertificatePdf(certId);
      if (res.success && res.blob) {
        const url = window.URL.createObjectURL(res.blob);
        const a = document.createElement('a');
        a.href = url;
        a.download = `Certificate_${certId}.pdf`;
        document.body.appendChild(a);
        a.click();
        document.body.removeChild(a);
        window.URL.revokeObjectURL(url);
      } else {
        setError(res.message || 'Unable to download Certificate PDF.');
      }
    } catch (err: any) {
      setError(err?.message || 'Certificate download failed.');
    } finally {
      setDownloadingCertId(null);
    }
  };

  const [issuingPending, setIssuingPending] = useState(false);
  const hasPendingCertificates = useMemo(() => {
    return computedCandidates.some((c) => c.outcomePreview === 'PASS' && !c.certificateNumber);
  }, [computedCandidates]);

  const handleIssuePending = async () => {
    setIssuingPending(true);
    setError(null);
    setSuccessMessage(null);
    try {
      const resp = await issuePendingCertificates(assessmentId);
      setSuccessMessage(
        `Pending candidate requirements evaluated! ${resp.certificatesIssued} total certificates issued.`,
      );
      await loadRoster();
    } catch (err: any) {
      setError(err?.message || 'Failed to issue pending certificates.');
    } finally {
      setIssuingPending(false);
    }
  };

  return (
    <div className="space-y-6 max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-6">
      {roster && !roster.managedBatch && roster.candidates.length > 0 && (
        <details className="card space-y-3 p-4">
          <summary className="cursor-pointer font-semibold text-navy">Candidate skills and job goals</summary>
          <label className="form-label max-w-sm">Candidate for skill observations
            <select className="form-input" value={skillCandidate} onChange={event => setSkillCandidate(event.target.value)}>
              <option value="">Select a trainee</option>
              {roster.candidates.map(candidate => <option key={candidate.traineeId} value={candidate.traineeId}>
                {candidate.fullName} · {candidate.registrationNumber}
              </option>)}
            </select>
          </label>
          {skillCandidate && <StaffCareerSkills key={skillCandidate} traineeId={Number(skillCandidate)} assessmentId={assessmentId} />}
        </details>
      )}
      {/* Top Breadcrumb & Actions Bar */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
        <div className="flex items-center gap-3">
          <button
            onClick={() => navigate('/assessor/dashboard')}
            className="inline-flex items-center gap-1.5 px-3 py-1.5 rounded-md border border-gray-300 text-xs font-semibold text-gray-700 bg-white hover:bg-gray-50 transition"
          >
            <ArrowLeft className="h-4 w-4" />
            Back to Dashboard
          </button>
          <div className="h-4 w-px bg-gray-300 hidden sm:block" />
          <span className="text-xs text-gray-500 font-mono">ID #{assessmentId}</span>
          {isDirty && !isFinalized && (
            <span className="inline-flex items-center gap-1 px-2 py-0.5 rounded text-[11px] font-medium bg-amber-100 text-amber-800">
              Unsaved Changes
            </span>
          )}
        </div>

        {/* Action Buttons */}
        {!isFinalized ? (
          <div className="flex items-center gap-2.5">
            <button
              onClick={handleSaveDraft}
              disabled={scoringLocked || savingDraft || finalizing || stats.hasAnyErrors}
              className="inline-flex items-center gap-1.5 px-3.5 py-2 rounded-lg text-xs font-semibold bg-white border border-gray-300 text-gray-700 hover:bg-gray-50 transition disabled:opacity-50"
            >
              <Save className={`h-3.5 w-3.5 ${savingDraft ? 'animate-spin' : ''}`} />
              Save Draft
            </button>
            <button
              onClick={() => setShowFinalizeModal(true)}
              disabled={scoringLocked || savingDraft || finalizing || stats.hasAnyErrors || stats.hasMissingForFinalize}
              className="inline-flex items-center gap-1.5 px-4 py-2 rounded-lg text-xs font-semibold bg-emerald-600 hover:bg-emerald-700 text-white transition shadow-sm disabled:opacity-50"
              title={
                stats.hasMissingForFinalize
                  ? 'All appeared candidates require Theory, Practical, and Viva scores'
                  : 'Finalize and declare results'
              }
            >
              <CheckCircle className={`h-3.5 w-3.5 ${finalizing ? 'animate-spin' : ''}`} />
              {roster?.managedBatch ? 'Publish batch results and certificates' : 'Finalize & Issue Certificates'}
            </button>
          </div>
        ) : hasPendingCertificates ? (
          <div className="flex items-center gap-2.5">
            <button
              onClick={handleIssuePending}
              disabled={issuingPending}
              className="inline-flex items-center gap-1.5 px-3.5 py-2 rounded-lg text-xs font-semibold bg-blue-600 hover:bg-blue-700 text-white transition shadow-sm disabled:opacity-50"
              title="Re-evaluate pending candidate requirements and issue certificates"
            >
              <RefreshCw className={`h-3.5 w-3.5 ${issuingPending ? 'animate-spin' : ''}`} />
              Issue Pending Certificates
            </button>
          </div>
        ) : null}
      </div>

      {publishedBatch.data && <section className="card p-4 space-y-2">
        <h2 className="font-semibold">Batch assessment details</h2>
        <BatchAssessmentSummary appointment={publishedBatch.data.publication} />
        {publishedBatch.data.publication.skills && <p className="text-sm">Skills taught: {publishedBatch.data.publication.skills}</p>}
      </section>}
      {roster?.managedBatch && !isFinalized && publishedBatch.data && (
        <section className="card p-4 space-y-2">
          {publishedBatch.data.publication.completedAt ? <p role="status">Assessment completed. You can now enter marks.</p> : <>
            <p>After the scheduled start, mark the assessment completed to open marks entry. The roster uses recorded training attendance.</p>
            <button type="button" className="btn-primary" disabled={completion.isPending || !publishedBatch.data.publication.canComplete}
              onClick={() => completion.mutate()}>Mark assessment completed</button>
          </>}
          {completion.isError && <p role="alert" className="text-red-800">{completion.error.message}</p>}
        </section>
      )}
      {roster?.managedBatch && publishedBatch.isError && <p role="alert" className="text-red-800">
        {publishedBatch.error.message} <button type="button" className="btn-outline" onClick={() => void publishedBatch.refetch()}>Retry assessment details</button>
      </p>}
      {/* Finalized Banner Alert */}
      {isFinalized && (
        <div className="bg-emerald-50 border border-emerald-300 rounded-xl p-4 flex items-start gap-3 shadow-sm">
          <CheckCircle2 className="h-6 w-6 text-emerald-600 flex-shrink-0 mt-0.5" />
          <div>
            <h3 className="text-sm font-bold text-emerald-900">
              Assessment Finalized &amp; Authoritatively Certified
            </h3>
            <p className="text-xs text-emerald-700 mt-0.5">
              Scores are finalized. Each candidate’s certificate status appears in the roster.
            </p>
          </div>
        </div>
      )}

      {/* Feedback Messages */}
      {error && (
        <div className="p-4 rounded-lg bg-red-50 border border-red-200 text-red-800 flex items-start gap-3">
          <AlertCircle className="h-5 w-5 text-red-600 flex-shrink-0 mt-0.5" />
          <div className="text-sm">
            <p className="font-semibold">Evaluation Operation Error</p>
            <p>{error}</p>
          </div>
        </div>
      )}

      {successMessage && (
        <div className="p-4 rounded-lg bg-emerald-50 border border-emerald-200 text-emerald-800 flex items-start gap-3">
          <CheckCircle2 className="h-5 w-5 text-emerald-600 flex-shrink-0 mt-0.5" />
          <div className="text-sm font-semibold">{successMessage}</div>
        </div>
      )}

      {/* Assessment Header Card */}
      {assessment && (
        <div className="bg-white rounded-xl shadow-sm border border-gray-200 p-6">
          <div className="flex flex-col lg:flex-row lg:items-center justify-between gap-4 pb-4 border-b border-gray-100">
            <div>
              <div className="flex items-center gap-2.5">
                <h1 className="text-xl font-bold text-gray-900">{assessment.assessmentName}</h1>
                <span className="px-2.5 py-0.5 text-xs font-semibold rounded-full bg-blue-100 text-blue-800 font-mono">
                  {assessment.assessmentCode}
                </span>
                {isFinalized ? (
                  <span className="px-2.5 py-0.5 text-xs font-semibold rounded-full bg-emerald-100 text-emerald-800 border border-emerald-200">
                    FINALIZED
                  </span>
                ) : (
                  <span className="px-2.5 py-0.5 text-xs font-semibold rounded-full bg-amber-100 text-amber-800 border border-amber-200">
                    {publishedBatch.data?.publication.completedAt ? 'EVALUATION COMPLETE' : 'SCHEDULED'}
                  </span>
                )}
              </div>
              <p className="text-xs text-gray-500 mt-1">
                Course: <span className="font-medium text-gray-800">{assessment.courseName}</span> &bull; Batch:{' '}
                <span className="font-mono text-gray-800">{assessment.batchCode}</span> &bull; Center:{' '}
                <span className="font-medium text-gray-800">{assessment.centerName}</span>
              </p>
            </div>

            {/* Component Scheme Pill */}
            <div className="bg-gray-50 border border-gray-200 rounded-lg p-3 text-xs flex items-center gap-4">
              <div>
                <span className="text-gray-500 block">Theory Max:</span>
                <span className="font-bold text-gray-800">{theoryMax}</span>
              </div>
              <div className="h-6 w-px bg-gray-200" />
              <div>
                <span className="text-gray-500 block">Practical Max:</span>
                <span className="font-bold text-gray-800">{practicalMax}</span>
              </div>
              <div className="h-6 w-px bg-gray-200" />
              <div>
                <span className="text-gray-500 block">Viva Max:</span>
                <span className="font-bold text-gray-800">{vivaMax}</span>
              </div>
              <div className="h-6 w-px bg-gray-200" />
              <div>
                <span className="text-gray-500 block">Pass Threshold:</span>
                <span className="font-bold text-emerald-700">{passScore} / {maxScore}</span>
              </div>
            </div>
          </div>

          {/* Quick Metrics Ribbon */}
          <div className="grid grid-cols-2 sm:grid-cols-5 gap-3 pt-4">
            <div className="bg-gray-50 rounded-lg p-2.5 border border-gray-100">
              <span className="text-[11px] text-gray-500">Total Roster</span>
              <div className="text-lg font-bold text-gray-900">{stats.total}</div>
            </div>
            <div className="bg-blue-50/50 rounded-lg p-2.5 border border-blue-100">
              <span className="text-[11px] text-blue-700">{roster?.managedBatch ? 'Eligible for scoring' : 'Appeared'}</span>
              <div className="text-lg font-bold text-blue-900">{roster?.managedBatch && !roster.checkedIn ? '—' : stats.appeared}</div>
            </div>
            <div className="bg-gray-50 rounded-lg p-2.5 border border-gray-100">
              <span className="text-[11px] text-gray-500">{roster?.managedBatch ? 'Not evaluated' : 'Absent'}</span>
              <div className="text-lg font-bold text-gray-600">{roster?.managedBatch && !roster.checkedIn ? '—' : stats.absent}</div>
            </div>
            <div className="bg-emerald-50/50 rounded-lg p-2.5 border border-emerald-100">
              <span className="text-[11px] text-emerald-700">Passing (Est.)</span>
              <div className="text-lg font-bold text-emerald-800">{stats.passed}</div>
            </div>
            <div className="bg-rose-50/50 rounded-lg p-2.5 border border-rose-100">
              <span className="text-[11px] text-rose-700">Failing (Est.)</span>
              <div className="text-lg font-bold text-rose-800">{stats.failed}</div>
            </div>
          </div>
        </div>
      )}

      {/* Spreadsheet Scoring Grid */}
      <div className="bg-white rounded-xl shadow-sm border border-gray-200 overflow-hidden">
        <div className="p-4 bg-gray-50/80 border-b border-gray-200 flex items-center justify-between">
          <div className="flex items-center gap-2">
            <FileText className="h-4 w-4 text-gray-600" />
            <h2 className="text-sm font-bold text-gray-900">Candidate Evaluation Roster</h2>
          </div>
          <span className="text-xs text-gray-500">
            Use <kbd className="px-1.5 py-0.5 text-[10px] bg-white border border-gray-300 rounded font-mono">Tab</kbd> or{' '}
            <kbd className="px-1.5 py-0.5 text-[10px] bg-white border border-gray-300 rounded font-mono">Enter</kbd> to
            navigate cells
          </span>
        </div>

        {loading ? (
          <div className="p-12 text-center text-gray-500">
            <RefreshCw className="h-8 w-8 text-blue-600 animate-spin mx-auto mb-3" />
            <p className="text-sm font-medium">Loading candidate scoring grid...</p>
          </div>
        ) : computedCandidates.length === 0 ? (
          <div className="p-12 text-center text-gray-500">
            <Users className="h-10 w-10 text-gray-300 mx-auto mb-3" />
            <p className="text-sm font-semibold text-gray-800">No candidates enrolled in this batch.</p>
          </div>
        ) : (
          <div className="overflow-x-auto">
            <table className="min-w-full divide-y divide-gray-200 text-left text-sm">
              <thead className="bg-gray-50 text-gray-600 font-semibold text-xs uppercase tracking-wider">
                <tr>
                  <th scope="col" className="px-4 py-3">#</th>
                  <th scope="col" className="px-4 py-3">Candidate Details</th>
                  <th scope="col" className="px-4 py-3 text-center">Attendance</th>
                  <th scope="col" className="px-4 py-3 w-32">
                    Theory <span className="text-gray-400 font-normal">(/ {theoryMax})</span>
                  </th>
                  <th scope="col" className="px-4 py-3 w-32">
                    Practical <span className="text-gray-400 font-normal">(/ {practicalMax})</span>
                  </th>
                  <th scope="col" className="px-4 py-3 w-32">
                    Viva <span className="text-gray-400 font-normal">(/ {vivaMax})</span>
                  </th>
                  <th scope="col" className="px-4 py-3 text-center">Aggregate</th>
                  <th scope="col" className="px-4 py-3 text-center">Result</th>
                  <th scope="col" className="px-4 py-3">Remarks / Certificate</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-gray-200 bg-white">
                {computedCandidates.map((c, idx) => (
                  <tr
                    key={c.traineeId}
                    className={`hover:bg-gray-50/60 transition ${
                      !c.appeared ? 'bg-gray-50/40 opacity-75' : ''
                    } ${c.hasErrors ? 'bg-red-50/20' : ''}`}
                  >
                    {/* Index */}
                    <td className="px-4 py-3.5 text-xs text-gray-400 font-mono">{idx + 1}</td>

                    {/* Candidate Identity */}
                    <td className="px-4 py-3.5">
                      <div className="font-semibold text-gray-900">{c.fullName}</div>
                      <div className="text-xs text-gray-500 font-mono">
                        Reg: {c.registrationNumber || c.traineeCode} &bull; Attempt: #{c.attemptNumber}
                      </div>
                    </td>

                    <td className="px-4 py-3.5 text-center">
                      <span className="text-xs font-semibold">
                        {roster?.managedBatch ? (roster.checkedIn ? c.appeared ? 'Eligible for scoring' : 'Not evaluated' : 'Pending evaluation') : c.appeared ? 'Present' : 'Absent'}
                        {publishedBatch.data?.candidates.filter((candidate) => candidate.traineeId === c.traineeId).map((candidate) => (
                          <span key={candidate.traineeId} className="block font-normal text-gray-600">{candidate.present} / {candidate.trainingSessions} training sessions</span>
                        ))}
                      </span>
                    </td>

                    {/* Theory Marks */}
                    <td className="px-4 py-3.5">
                      <input
                        type="number"
                        step="0.01"
                        min="0"
                        max={theoryMax}
                        disabled={!c.appeared || scoringLocked}
                        data-row={idx}
                        data-col={0}
                        value={c.rowState.theory}
                        onChange={(e) => handleScoreChange(c.traineeId, 'theory', e.target.value)}
                        onKeyDown={(e) => handleKeyDown(e, idx, 0)}
                        placeholder="0.00"
                        className={`w-24 px-2 py-1.5 text-sm font-mono border rounded-md text-right focus:outline-none focus:ring-2 ${
                          c.hasInvalidTheory
                            ? 'border-red-500 bg-red-50 text-red-900 focus:ring-red-400'
                            : 'border-gray-300 focus:ring-blue-500'
                        } disabled:bg-gray-100 disabled:text-gray-400`}
                      />
                      {c.hasInvalidTheory && (
                        <div className="text-[10px] text-red-600 mt-0.5">0 - {theoryMax} max</div>
                      )}
                    </td>

                    {/* Practical Marks */}
                    <td className="px-4 py-3.5">
                      <input
                        type="number"
                        step="0.01"
                        min="0"
                        max={practicalMax}
                        disabled={!c.appeared || scoringLocked}
                        data-row={idx}
                        data-col={1}
                        value={c.rowState.practical}
                        onChange={(e) => handleScoreChange(c.traineeId, 'practical', e.target.value)}
                        onKeyDown={(e) => handleKeyDown(e, idx, 1)}
                        placeholder="0.00"
                        className={`w-24 px-2 py-1.5 text-sm font-mono border rounded-md text-right focus:outline-none focus:ring-2 ${
                          c.hasInvalidPractical
                            ? 'border-red-500 bg-red-50 text-red-900 focus:ring-red-400'
                            : 'border-gray-300 focus:ring-blue-500'
                        } disabled:bg-gray-100 disabled:text-gray-400`}
                      />
                      {c.hasInvalidPractical && (
                        <div className="text-[10px] text-red-600 mt-0.5">0 - {practicalMax} max</div>
                      )}
                    </td>

                    {/* Viva Marks */}
                    <td className="px-4 py-3.5">
                      <input
                        type="number"
                        step="0.01"
                        min="0"
                        max={vivaMax}
                        disabled={!c.appeared || scoringLocked}
                        data-row={idx}
                        data-col={2}
                        value={c.rowState.viva}
                        onChange={(e) => handleScoreChange(c.traineeId, 'viva', e.target.value)}
                        onKeyDown={(e) => handleKeyDown(e, idx, 2)}
                        placeholder="0.00"
                        className={`w-24 px-2 py-1.5 text-sm font-mono border rounded-md text-right focus:outline-none focus:ring-2 ${
                          c.hasInvalidViva
                            ? 'border-red-500 bg-red-50 text-red-900 focus:ring-red-400'
                            : 'border-gray-300 focus:ring-blue-500'
                        } disabled:bg-gray-100 disabled:text-gray-400`}
                      />
                      {c.hasInvalidViva && (
                        <div className="text-[10px] text-red-600 mt-0.5">0 - {vivaMax} max</div>
                      )}
                    </td>

                    {/* Total & Percentage Preview */}
                    <td className="px-4 py-3.5 text-center">
                      <div className="font-mono font-bold text-gray-900">
                        {c.total !== null ? c.total.toFixed(2) : '—'}
                      </div>
                      <div className="text-[11px] text-gray-500 font-mono">
                        {c.percentage !== null ? `${c.percentage.toFixed(1)}%` : '—'}
                      </div>
                    </td>

                    {/* Outcome Badge */}
                    <td className="px-4 py-3.5 text-center">
                      {c.outcomePreview === 'PASS' ? (
                        <span className="inline-flex items-center gap-1 px-2.5 py-1 rounded-full text-xs font-bold bg-emerald-100 text-emerald-800">
                          <CheckCircle2 className="h-3 w-3 text-emerald-600" />
                          PASS
                        </span>
                      ) : c.outcomePreview === 'FAIL' ? (
                        <span className="inline-flex items-center gap-1 px-2.5 py-1 rounded-full text-xs font-bold bg-rose-100 text-rose-800">
                          <XCircle className="h-3 w-3 text-rose-600" />
                          FAIL
                        </span>
                      ) : c.outcomePreview === 'ABSENT' ? (
                        <span className="inline-flex items-center gap-1 px-2.5 py-1 rounded-full text-xs font-semibold bg-gray-100 text-gray-600">
                          ABSENT
                        </span>
                      ) : c.outcomePreview === 'PENDING' ? (
                        <span className="inline-flex items-center gap-1 px-2.5 py-1 rounded-full text-xs font-semibold bg-amber-100 text-amber-800">
                          <Clock className="h-3 w-3 text-amber-600" />
                          DRAFT
                        </span>
                      ) : (
                        <span className="inline-flex items-center gap-1 px-2.5 py-1 rounded-full text-xs font-semibold bg-slate-100 text-slate-600">
                          UNASSESSED
                        </span>
                      )}
                    </td>

                    {/* Remarks & Certificate Download */}
                    <td className="px-4 py-3.5">
                      {isFinalized && c.certificateNumber ? (
                        <div className="flex items-center gap-2">
                          <div className="text-xs">
                            <span className="font-semibold text-gray-700 block">Certificate</span>
                            <span className="font-mono text-[11px] text-emerald-700">{c.certificateNumber}</span>
                          </div>
                          {(() => {
                            const certIdToDownload = c.certificateId || c.certificationId;
                            return certIdToDownload ? (
                              <button
                                onClick={() => handleDownloadCert(certIdToDownload)}
                                disabled={downloadingCertId === certIdToDownload}
                                className="inline-flex items-center gap-1 px-2 py-1 rounded border border-gray-300 bg-white hover:bg-gray-50 text-xs font-semibold text-gray-700 transition"
                                title="Download PDF Certificate"
                              >
                                <Download className="h-3 w-3 text-blue-600" />
                                PDF
                              </button>
                            ) : null;
                          })()}
                        </div>
                      ) : isFinalized ? (
                        <div className="text-xs">
                          <span
                            className={`inline-flex items-center gap-1 px-2 py-0.5 rounded text-[11px] font-medium ${
                              c.isPassFlag
                                ? 'bg-amber-50 text-amber-700 border border-amber-200'
                                : 'bg-gray-100 text-gray-500'
                            }`}
                          >
                            {c.isPassFlag
                              ? (c.certificateStatusCode === 'PENDING_ELIGIBILITY'
                                  ? 'Pending Attendance'
                                  : 'Pending Issuance')
                              : 'No Certificate'}
                          </span>
                          {roster?.managedBatch && c.assessmentResultId && <AssessmentDocumentButton resultId={c.assessmentResultId} />}
                        </div>
                      ) : (
                        <input
                          type="text"
                          disabled={scoringLocked}
                          value={c.rowState.remarks}
                          onChange={(e) => handleScoreChange(c.traineeId, 'remarks', e.target.value)}
                          placeholder="Evaluator note..."
                          className="w-full min-w-[140px] px-2 py-1 text-xs border border-gray-300 rounded focus:outline-none focus:ring-1 focus:ring-blue-500 disabled:bg-gray-50"
                        />
                      )}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </div>

      {/* Finalize Confirmation Modal */}
      {showFinalizeModal && (
        <div className="fixed inset-0 z-50 overflow-y-auto bg-black/50 flex items-center justify-center p-4">
          <div className="bg-white rounded-xl shadow-xl max-w-md w-full p-6 space-y-4">
            <div className="flex items-start gap-3">
              <div className="p-2.5 rounded-full bg-amber-100 text-amber-800">
                <AlertTriangle className="h-6 w-6 text-amber-600" />
              </div>
              <div>
                <h3 className="text-lg font-bold text-gray-900">Finalize &amp; Issue Certificates</h3>
                <p className="text-xs text-gray-500 mt-1">
                  Finalizing locks the scores. Passing candidates receive certificates when the backend confirms their eligibility.
                </p>
              </div>
            </div>

            {/* Finalization Summary Box */}
            <div className="bg-gray-50 rounded-lg p-3 text-xs space-y-1.5 border border-gray-200">
              <div className="flex justify-between">
                <span className="text-gray-600">Total Candidates:</span>
                <span className="font-bold text-gray-900">{stats.total}</span>
              </div>
              <div className="flex justify-between">
                <span className="text-emerald-700">Expected to pass</span>
                <span className="font-bold text-emerald-800">{stats.passed}</span>
              </div>
              <div className="flex justify-between">
                <span className="text-rose-700">Declared Failed:</span>
                <span className="font-bold text-rose-800">{stats.failed}</span>
              </div>
              <div className="flex justify-between">
                <span className="text-gray-600">Marked Absent:</span>
                <span className="font-bold text-gray-800">{stats.absent}</span>
              </div>
            </div>

            <div className="flex items-center justify-end gap-3 pt-3 border-t border-gray-100">
              <button
                type="button"
                onClick={() => setShowFinalizeModal(false)}
                className="px-3.5 py-2 text-xs font-semibold text-gray-700 bg-white border border-gray-300 rounded-lg hover:bg-gray-50 transition"
              >
                Cancel
              </button>
              <button
                type="button"
                onClick={handleFinalizeConfirm}
                disabled={finalizing}
                className="inline-flex items-center gap-1.5 px-4 py-2 text-xs font-semibold text-white bg-emerald-600 hover:bg-emerald-700 rounded-lg transition shadow-sm disabled:opacity-50"
              >
                <CheckCircle className={`h-3.5 w-3.5 ${finalizing ? 'animate-spin' : ''}`} />
                Confirm &amp; Finalize
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
