import { http, HttpResponse } from 'msw';
import { setupWorker } from 'msw/browser';
import type { RegisterTraineePayload } from '@/api/registrationApi';
import type { AuthenticatedUserResponse } from '@/api/types';
import type { CandidateScoreInput } from '@/types/assessor';
import type { ImportBatchProgress, ImportRecordItem } from '@/types/provider';
import type { TraineeConsent, TraineeProfile } from '@/types/trainee';
import { localEvaluatorEnabled, simulationActive, simulationBaseUrl } from '@/lib/demoEnvironment';
import {
  accounts, assessment, assessmentCandidate, candidate, certificate, employerReference,
  misOptions, mockSignInProof, placement, reference, sampleAssessmentId, sampleBatchId, sampleConsent,
  sampleCsv, sampleDate, sampleVerificationId, templates, timestamp, verification,
} from './fixtures';

type Scenario = 'journey' | 'edgeCases';
const ok = (data: unknown, status = 200) => HttpResponse.json({ success: true, data }, { status });
const failure = (status: number, code: string, message: string) =>
  HttpResponse.json({ success: false, code, message }, { status });
const missing = () => failure(404, 'RESOURCE_NOT_FOUND', 'This sample record is unavailable.');
const unsupported = () => failure(501, 'SIMULATION_WORKFLOW_UNAVAILABLE', 'This workflow is outside the offline sample.');

function page<T>(items: T[], url: URL) {
  const pageNumber = Math.max(0, Number(url.searchParams.get('page')) || 0);
  const size = Math.min(100, Math.max(1, Number(url.searchParams.get('size')) || 20));
  const totalPages = Math.ceil(items.length / size);
  const hasNext = pageNumber + 1 < totalPages;
  return { content: items.slice(pageNumber * size, (pageNumber + 1) * size), page: pageNumber, size,
    totalElements: items.length, totalPages, first: pageNumber === 0, last: !hasNext, hasNext, hasPrevious: pageNumber > 0 };
}

function seededImport(id: number, processed: boolean): ImportBatchProgress {
  return { importBatchId: id, batchCode: 'SIM-CSV-' + id, rowCount: 3, successCount: processed ? 1 : 0,
    failureCount: processed ? 1 : 0, duplicateCount: processed ? 1 : 0, skippedCount: 0,
    status: processed ? 'PARTIALLY_COMPLETED' : 'PENDING', isTerminal: processed, entityType: 'Trainee',
    sourceSystem: 'PROVIDER_PORTAL', completedAt: processed ? timestamp() : undefined };
}

function newScenario(scenario: Scenario) {
  return {
    scenario,
    users: new Map(accounts),
    profiles: new Map([[candidate.userId!, candidate]]),
    consents: new Map<number, TraineeConsent>([1, 2, 5, 7].map((type) =>
      [type, sampleConsent(type, candidate.id, type, scenario === 'journey' || type !== 5)!])),
    imports: new Map([[sampleBatchId, seededImport(sampleBatchId, true)]]),
    assessment: { ...assessment },
    score: { ...assessmentCandidate },
    verification: { ...verification },
    evidence: undefined as File | undefined,
  };
}

let state = newScenario('journey');
type SampleState = ReturnType<typeof newScenario>;

export function resetSimulation(scenario: Scenario) {
  if (simulationActive && localEvaluatorEnabled) state = newScenario(scenario);
}

function session(account: AuthenticatedUserResponse) {
  return {
    accessToken: 'sim-access:' + account.username, refreshToken: 'sim-refresh:' + account.username,
    tokenType: 'Bearer', expiresIn: 3600, userId: account.id, username: account.username, authorities: account.authorities,
  };
}

async function authRequest(path: string, request: Request, sample: SampleState) {
  if (path === '/auth/register/reference-data' && request.method === 'GET') return ok(reference);
  if (path === '/auth/login' && request.method === 'POST') {
    const { usernameOrEmail, password } = await request.json() as { usernameOrEmail: string; password: string };
    const account = accounts.get(usernameOrEmail);
    return account && password === mockSignInProof
      ? ok(session(account)) : failure(401, 'INVALID_CREDENTIALS', 'Choose a sample persona in the evaluator drawer.');
  }
  if (path === '/auth/refresh' && request.method === 'POST') {
    const { refreshToken } = await request.json() as { refreshToken: string };
    const account = refreshToken?.startsWith('sim-refresh:') && sample.users.get(refreshToken.slice('sim-refresh:'.length));
    return account ? ok(session(account)) : failure(401, 'INVALID_TOKEN', 'The sample session has expired.');
  }
  if (path === '/auth/register/trainee' && request.method === 'POST') {
    const values = await request.json() as RegisterTraineePayload;
    if (!values.username || !values.firstName) return failure(400, 'VALIDATION_ERROR', 'Enter a sample username and name.');
    if (values.dataProcessingConsent !== true) return failure(400, 'VALIDATION_ERROR', 'Confirm core data processing consent.');
    if (!/^[1-9][0-9]{5}$/.test(values.pincode ?? '')) return failure(400, 'VALIDATION_ERROR', 'Enter a valid six-digit PIN code.');
    if (sample.users.has(values.username)) return failure(409, 'RESOURCE_CONFLICT', 'That sample username is already in use.');
    const id = 9_700_000 + sample.profiles.size;
    const user = { id, username: values.username, authorities: [...accounts.get('trainee_user')!.authorities] };
    const profile: TraineeProfile = {
      id, userId: id, registrationNumber: 'SIM-REGISTERED-' + id, firstName: values.firstName, lastName: values.lastName,
      stateId: values.stateId, districtId: values.districtId, genderId: values.genderId,
      pincode: values.pincode,
      currentEmploymentStatusId: values.currentEmploymentStatusId ?? 8, profileStatusId: 3,
    };
    sample.users.set(user.username, user);
    sample.profiles.set(user.id, profile);
    const consent = sampleConsent(id, profile.id, 1, true);
    if (consent) sample.consents.set(consent.id, { ...consent, grantedAt: timestamp() });
    return ok(session(user), 201);
  }
  return undefined;
}

async function traineeRequest(path: string, request: Request, url: URL, sample: SampleState, user: AuthenticatedUserResponse) {
  const profile = sample.profiles.get(user.id);
  if (!profile) return failure(403, 'ACCESS_DENIED', 'Use the sample trainee account.');
  const hasHistory = profile.id === candidate.id;
  if (request.method === 'GET') {
    if (path === '/trainees' && url.searchParams.get('userId') === String(user.id)) return ok(profile);
    if (path === '/consents') return ok([...sample.consents.values()].filter((consent) => consent.traineeId === profile.id));
    if (path === '/certifications') return ok(hasHistory ? [certificate, ...(sample.score.certificateId
      ? [{ ...certificate, id: sample.score.certificateId, certificateNumber: sample.score.certificateNumber }] : [])] : []);
    if (path === '/assessment-results') return ok(hasHistory ? [{
      id: 7403, traineeId: profile.id, assessmentName: 'Solar PV final assessment', assessmentOutcomeCode: 'PASS',
      assessmentOutcomeName: 'Pass', isPassFlag: true, totalScore: 82, maxScore: 100, scorePercentage: 82, resultDeclaredAt: sampleDate(-43),
    }] : []);
    if (path === '/training-enrollments') return ok(page(hasHistory ? [{
      id: 7201, traineeId: profile.id, enrollmentNumber: 'SIM-ENROLL-001', courseName: 'Solar PV Installation',
      enrollmentStatusCode: 'COMPLETED', enrollmentStatusName: 'Completed', startDate: sampleDate(-130),
      actualCompletionDate: sampleDate(-42), isCompletedFlag: true,
    }] : [], url));
    if (path === '/attendance-records') return ok([]);
    if (path === '/trainees/me/employment-history') return ok(page([], url));
    if (path === '/trainees/me/career-status') return ok({
      status: { code: hasHistory ? 'WAGE_EMPLOYED' : 'UNEMPLOYED',
        name: hasHistory ? 'Wage employed' : 'Unemployed', source: 'PROFILE' },
      unemploymentPeriods: [],
    });
    if (path === '/analytics/summaries/trainee-outcomes') return ok({ traineeId: profile.id, snapshotEmploymentStatusCode: hasHistory ? 'WAGE_EMPLOYED' : 'UNEMPLOYED',
      snapshotIsEmployedFlag: hasHistory, placedEnrollmentCount: hasHistory ? 1 : 0, currentWageEmploymentCount: hasHistory ? 1 : 0 });
    if (path.endsWith('/skill-gap-profile')) return ok({ traineeId: profile.id, hasAssessment: false, recommendations: [] });
    if (path === '/trainees/me/followup-surveys') return ok({
      followupConsentGranted: false, careerUpdateAvailable: false, assignments: [], recentSubmissions: [],
    });
  }
  if (path === '/consents' && request.method === 'POST') {
    const data = await request.json() as { traineeId: number; consentTypeId: number };
    if (data.traineeId !== profile.id) return failure(403, 'ACCESS_DENIED', 'This sample profile is outside your session.');
    const latest = latestSampleConsent(sample, profile.id, data.consentTypeId);
    if (latest?.consentStatusCode === 'GRANTED') return ok(latest, 201);
    const created = sampleConsent(100 + sample.consents.size, profile.id, data.consentTypeId, true);
    if (!created) return missing();
    const saved = { ...created, grantedAt: timestamp() };
    sample.consents.set(saved.id, saved);
    return ok(saved, 201);
  }
  const consentId = /^\/consents\/(\d+)\/status$/.exec(path);
  if (request.method === 'PUT' && consentId) {
    const stored = sample.consents.get(Number(consentId[1]));
    if (!stored || stored.traineeId !== profile.id) return missing();
    const { consentStatusId } = await request.json() as { consentStatusId: number };
    if (consentStatusId !== 2) return failure(400, 'INVALID_CONSENT_STATUS', 'Use the grant action to grant consent.');
    if (latestSampleConsent(sample, profile.id, stored.consentTypeId)?.id !== stored.id) {
      return failure(409, 'CONSENT_CHANGED', 'This consent changed. Review the current choice and try again.');
    }
    if (stored.consentStatusCode === 'REVOKED') return ok(stored);
    const next = { ...stored, consentStatusId: 2, consentStatusCode: 'REVOKED', consentStatusName: 'Revoked',
      revokedAt: timestamp(), updatedAt: timestamp() };
    sample.consents.set(next.id, next);
    return ok(next);
  }
  return unsupported();
}

function latestSampleConsent(sample: SampleState, traineeId: number, consentTypeId: number) {
  return [...sample.consents.values()].reverse().find((consent) =>
    consent.traineeId === traineeId && consent.consentTypeId === consentTypeId);
}

async function importRequest(path: string, request: Request, url: URL, sample: SampleState) {
  if (path === '/import-batches' && request.method === 'POST') {
    const form = await request.formData();
    const file = form.get('file');
    if (!(file instanceof File) || file.size > 4096 || (await file.text()).replace(/\r\n/g, '\n').trim() !== sampleCsv.trim()) {
      return failure(400, 'SIMULATION_SAMPLE_REQUIRED', 'Upload the sample trainee CSV from the evaluator drawer.');
    }
    if (url.searchParams.get('entityType') !== 'Trainee') return failure(400, 'VALIDATION_ERROR', 'Select Trainee for the sample CSV.');
    const id = sampleBatchId + sample.imports.size;
    const batch = seededImport(id, false);
    sample.imports.set(id, batch);
    return ok({ id, batchCode: batch.batchCode, totalRecords: batch.rowCount, statusCode: batch.status }, 201);
  }
  const match = /^\/import-batches\/(\d+)\/(status|process|recover|quarantine|quarantine\/export)$/.exec(path);
  if (!match) return unsupported();
  const id = Number(match[1]);
  const action = match[2];
  const batch = sample.imports.get(id);
  if (!batch) return missing();
  if (request.method === 'POST' && (action === 'process' || action === 'recover')) {
    if (batch.isTerminal && action === 'recover') return failure(409, 'IMPORT_ALREADY_TERMINAL', 'This sample import has finished processing.');
    const next = seededImport(id, action === 'process');
    sample.imports.set(id, next);
    return ok(next);
  }
  if (action === 'status' && request.method === 'GET') return ok(batch);
  if (request.method !== 'GET') return unsupported();
  const rows: ImportRecordItem[] = !batch.isTerminal ? [] : [
    { id: id * 10 + 1, importBatchId: id, sourceRowNumber: 3, importRecordStatusCode: 'FAILED',
      errorCode: 'DISTRICT_NOT_FOUND', errorMessage: 'The sample district code UNKNOWN is unavailable.' },
    { id: id * 10 + 2, importBatchId: id, sourceRowNumber: 4, importRecordStatusCode: 'DUPLICATE',
      errorCode: 'DUPLICATE_REGISTRATION', errorMessage: 'The sample registration number is already present.' },
  ];
  const displayed = url.searchParams.get('includeDuplicates') === 'false'
    ? rows.filter((row) => row.importRecordStatusCode !== 'DUPLICATE') : rows;
  if (action === 'quarantine') return ok(page(displayed, url));
  if (action === 'quarantine/export') return new HttpResponse('source_row,error_code\n'
    + displayed.map((row) => row.sourceRowNumber + ',' + row.errorCode).join('\n'), { headers: { 'Content-Type': 'text/csv' } });
  return unsupported();
}

async function assessorRequest(path: string, request: Request, sample: SampleState) {
  const root = '/assessor/assessments';
  const target = root + '/' + sampleAssessmentId;
  const roster = () => ({ assessment: sample.assessment, candidates: [sample.score] });
  if (request.method === 'GET') {
    if (path === root) return ok([sample.assessment]);
    if (path === target) return ok(sample.assessment);
    if (path === target + '/roster') return ok(roster());
  }
  if (request.method === 'POST' && [target + '/draft', target + '/finalize', target + '/issue-pending'].includes(path)) {
    const finalized = path.endsWith('/finalize');
    if (sample.assessment.isFinalized && !path.endsWith('/issue-pending')) {
      return failure(409, 'ASSESSMENT_ALREADY_FINALIZED', 'The sample assessment is already finalized.');
    }
    if (!path.endsWith('/issue-pending')) {
      const payload = await request.json() as { candidates?: CandidateScoreInput[] };
      const score = payload.candidates?.find((item) => item.traineeId === candidate.id);
      if (!score) return failure(400, 'VALIDATION_ERROR', 'Enter scores for the sample candidate.');
      const parts = [score.theoryScore, score.practicalScore, score.vivaScore];
      if (parts.some((part, index) => part != null && (part < 0 || part > [40, 50, 10][index]))) {
        return failure(400, 'VALIDATION_ERROR', 'Sample component scores exceed the assessment limits.');
      }
      const total = (score.theoryScore ?? 0) + (score.practicalScore ?? 0) + (score.vivaScore ?? 0);
      const passed = finalized && score.appeared && total >= sample.assessment.passScore;
      const outcome = !finalized ? 'PENDING' : !score.appeared ? 'ABSENT' : passed ? 'PASS' : 'FAIL';
      sample.score = { ...sample.score, ...score, theoryScore: score.theoryScore ?? null, practicalScore: score.practicalScore ?? null,
        vivaScore: score.vivaScore ?? null, totalScore: total, scorePercentage: total, outcome,
        assessmentOutcomeCode: outcome, isFinalized: finalized, certificateId: passed ? 7404 : undefined,
        certificateNumber: passed ? 'SIM-CERT-PUNE-002' : undefined, certificateStatusCode: passed ? 'ISSUED' : undefined };
      sample.assessment = { ...sample.assessment, scoredCandidates: 1, isFinalized: finalized,
        lifecycleStatusCode: finalized ? 'FINALIZED' : 'IN_PROGRESS' };
    }
    return ok({ assessmentId: sampleAssessmentId, isFinalized: sample.assessment.isFinalized, totalProcessed: 1,
      passedCount: Number(sample.score.outcome === 'PASS'), failedCount: Number(sample.score.outcome === 'FAIL'),
      absentCount: Number(sample.score.outcome === 'ABSENT'), certificatesIssued: Number(!!sample.score.certificateId),
      results: [sample.score], roster: roster() });
  }
  return unsupported();
}

async function employerRequest(path: string, request: Request, url: URL, sample: SampleState) {
  const prefix = '/employer';
  const item = prefix + '/verifications/' + sampleVerificationId;
  if (request.method === 'GET') {
    if (path === prefix + '/reference-data') return ok(employerReference);
    if (path === prefix + '/placements') return ok(page([placement], url));
    if (path === prefix + '/verifications/pending') return ok(page(sample.verification.canRespond ? [sample.verification] : [], url));
    if (path === prefix + '/verifications') return ok(page([sample.verification], url));
    if (path === item + '/evidence/1' && sample.evidence) {
      return new HttpResponse(await sample.evidence.arrayBuffer(), { headers: { 'Content-Type': sample.evidence.type } });
    }
  }
  if (request.method === 'POST' && path === item + '/verdict') {
    if (!sample.verification.canRespond) return failure(409, 'VERIFICATION_ALREADY_RESOLVED', 'The sample request already has a verdict.');
    const form = await request.formData();
    const part = form.get('verdict');
    if (!(part instanceof File)) return failure(400, 'VALIDATION_ERROR', 'Provide the verdict part.');
    const values = JSON.parse(await part.text()) as {
      decision: string; exitDate?: string; exitReasonId?: number; separationNatureId?: number; rejectionReasonId?: number;
    };
    if (!['ATTEST', 'DISPUTE', 'REPORT_EXIT'].includes(values.decision)) return failure(400, 'VALIDATION_ERROR', 'Select a supported verdict.');
    if (values.decision === 'DISPUTE' && !values.rejectionReasonId) return failure(400, 'VALIDATION_ERROR', 'Select a dispute reason.');
    if (values.decision === 'REPORT_EXIT' && (!values.exitDate || values.exitDate < sample.verification.startedOn
        || !values.exitReasonId || !values.separationNatureId)) {
      return failure(400, 'INVALID_EMPLOYMENT_EXIT', 'Choose a separation date, reason and type.');
    }
    const evidence = form.get('evidence');
    if (evidence instanceof File && (evidence.size > 5 * 1024 * 1024 || !['application/pdf', 'image/jpeg'].includes(evidence.type))) {
      return failure(400, 'INVALID_EVIDENCE', 'Choose a sample PDF or JPEG up to 5 MB.');
    }
    sample.evidence = evidence instanceof File ? evidence : undefined;
    const status = values.decision === 'DISPUTE' ? 'REJECTED' : 'VERIFIED';
    sample.verification = { ...sample.verification, canRespond: false, requestStatus: 'COMPLETED', verdict: status,
      employmentVerificationStatus: status, endedOn: values.exitDate, evidenceId: sample.evidence ? 1 : undefined,
      evidenceName: sample.evidence?.name };
    return ok(sample.verification);
  }
  return unsupported();
}

function misRequest(path: string, url: URL, sample: SampleState) {
  const root = '/dashboards/';
  const name = path.slice(root.length);
  const filtered = url.searchParams.has('stateId') || url.searchParams.has('schemeId') || url.searchParams.has('fiscalYear');
  const suppressed = url.searchParams.get('districtId') === '272';
  const factor = filtered ? 0.6 : 1;
  const kpi = (value: number) => suppressed ? { availability: 'SUPPRESSED', value: null }
    : { availability: 'RELEASED', value: Math.round(value * factor) };
  const geo = reference.states.map((ref, index) => ({
    stateId: ref.id, stateCode: ref.code, stateName: ref.name, enrollmentCount: suppressed ? null : 420 - index * 180,
    placementRatePct: suppressed ? null : 64 - index * 14, employmentRatePct: suppressed ? null : 68 - index * 9,
    certificationRatePct: suppressed ? null : 83 - index * 13,
  }));
  if (name === 'mis/filters') return ok(misOptions);
  if (name === 'kpis') {
    const values = [
      ['enrolled', kpi(660)], ['certified', kpi(512)], ['placed', kpi(381)],
      ['retention6m', { ...kpi(72), value: suppressed || sample.scenario === 'edgeCases' ? null : 72,
        availability: suppressed || sample.scenario === 'edgeCases' ? 'SUPPRESSED' : 'RELEASED' }],
      ['activeCenters', kpi(12)],
    ] as const;
    return ok({ executiveKpis: {
      previousFinancialYear: url.searchParams.has('fiscalYear') ? '2024-25' : undefined,
      kpis: values.map(([key, current]) => ({ key, current, changePct: current.availability === 'RELEASED' ? 8 : null })),
    } });
  }
  if (name === 'pipeline-funnel') {
    return ok({ candidateStages: ['registered', 'enrolled', 'assessed', 'certified', 'placed', 'retained']
      .map((key, index) => ({ key, candidates: kpi([780, 660, 570, 512, 381, 274][index]) })) });
  }
  if (name === 'geography') return ok({ stateSummaries: geo });
  const stateId = /^states\/(\d+)$/.exec(name);
  if (stateId) {
    const stateRow = geo.find((row) => row.stateId === Number(stateId[1]));
    if (!stateRow) return missing();
    const districts = reference.districts.filter((row) => row.parentId === stateRow.stateId).map((row, index) => ({
      districtId: row.id, districtCode: row.code, districtName: row.name,
      enrollmentCount: index || stateRow.enrollmentCount == null ? null : stateRow.enrollmentCount - 14,
      joinedPlacementCount: index || suppressed ? null : 138, placementRatePct: index ? null : stateRow.placementRatePct,
      certificationRatePct: index ? null : stateRow.certificationRatePct,
      employmentRatePct: index ? null : stateRow.employmentRatePct,
    }));
    return ok({ ...stateRow, stateSummary: stateRow, districts });
  }
  if (['scheme-matrix', 'providers', 'courses', 'employers'].includes(name)) return ok({ items: [] });
  if (name === 'economic-outcomes') return ok({});
  if (name === 'mis/provider-quality') return ok([]);
  return unsupported();
}

async function communicationRequest(path: string, request: Request, url: URL, sample: SampleState) {
  const root = '/communication';
  if (path === root + '/center' && request.method === 'GET') return ok({ demoAvailable: false });
  if (path === root + '/templates' && request.method === 'GET') return ok(templates);
  if (path === root + '/jobs' && request.method === 'GET') return ok({ counts: [], jobs: page([], url) });
  if (path === root + '/logs' && request.method === 'GET') {
    const rows = [
      { channel: 'EMAIL', status: 'DELIVERED', recipientMasked: 'a***a@example.invalid', optedOut: false },
      { channel: 'SMS', status: sample.scenario === 'edgeCases' ? 'BLOCKED_NO_CONSENT' : 'DELIVERED',
        recipientMasked: '+91-XXXXX-XX010', optedOut: sample.scenario === 'edgeCases' },
      { channel: 'WHATSAPP', status: 'SENT', recipientMasked: '+91-XXXXX-XX010', optedOut: false },
    ].map((row, index) => ({
      ...row, id: 7600 + index, templateCode: 'TPL-VERIF-003', purpose: 'EMPLOYMENT_CHECK', demoReceipt: false,
      createdAt: timestamp(), sentAt: row.optedOut ? undefined : timestamp(), deliveredAt: row.status === 'DELIVERED' ? timestamp() : undefined,
    }));
    const ch = url.searchParams.get('channel');
    const status = url.searchParams.get('status');
    const q = url.searchParams.get('q')?.toUpperCase();
    return ok(page(rows.filter((row) => (!ch || row.channel === ch)
      && (!status || (status === 'OPTED_OUT' ? row.optedOut : row.status === status))
      && (!q || row.templateCode.includes(q) || row.purpose.includes(q) || String(row.id) === q)), url));
  }
  const render = /^\/communication\/templates\/([A-Z0-9-]+)\/render$/.exec(path);
  if (render && request.method === 'POST') {
    const template = templates.find((entry) => entry.templateCode === render[1]);
    if (!template) return missing();
    const params = await request.json() as Record<string, string>;
    if (template.allowedParameterKeys.some((key) => !params[key]?.trim())) {
      return failure(400, 'VALIDATION_ERROR', 'Enter each sample template variable.');
    }
    return ok({ templateCode: template.templateCode, subject: template.subject,
      bodyText: template.bodyTemplate.replace(/\{(\w+)\}/g, (_, key: string) => params[key]) });
  }
  return unsupported();
}

function sampleCertificatePdf() {
  const contents = 'BT /F1 18 Tf 48 740 Td (SIMULATION - Sample Solar PV Certificate) Tj ET';
  const objects = ['<< /Type /Catalog /Pages 2 0 R >>', '<< /Type /Pages /Kids [3 0 R] /Count 1 >>',
    '<< /Type /Page /Parent 2 0 R /MediaBox [0 0 612 792] /Resources << /Font << /F1 4 0 R >> >> /Contents 5 0 R >>',
    '<< /Type /Font /Subtype /Type1 /BaseFont /Helvetica >>', '<< /Length ' + contents.length + ' >>\nstream\n' + contents + '\nendstream'];
  let pdf = '%PDF-1.4\n';
  const offsets = objects.map((value, index) => {
    const offset = pdf.length;
    pdf += (index + 1) + ' 0 obj\n' + value + '\nendobj\n';
    return offset.toString().padStart(10, '0') + ' 00000 n \n';
  });
  const startXref = pdf.length;
  return pdf + 'xref\n0 6\n0000000000 65535 f \n' + offsets.join('') +
    'trailer\n<< /Size 6 /Root 1 0 R >>\nstartxref\n' + startXref + '\n%%EOF\n';
}

const worker = setupWorker(
  http.all(simulationBaseUrl + '/api/v1/*', async ({ request }) => {
    const sample = state;
    const url = new URL(request.url);
    const path = url.pathname.slice((simulationBaseUrl + '/api/v1').length);
    try {
      if (path.startsWith('/auth/')) {
        const publicResponse = await authRequest(path, request, sample);
        if (publicResponse) return publicResponse;
      }
      const authorization = request.headers.get('Authorization');
      const prefix = 'Bearer sim-access:';
      const user = authorization?.startsWith(prefix) && sample.users.get(authorization.slice(prefix.length));
      if (!user) return failure(401, 'UNAUTHORIZED', 'Choose a sample persona in the evaluator drawer.');
      if (path === '/auth/me' && request.method === 'GET') return ok(user);
      const roles = user.authorities;
      const allowed = (role: string) => roles.includes('ROLE_' + role);
      if (/^\/certifications\/(7402|7404)\/download$/.test(path) && request.method === 'GET'
          && (allowed('assessor') || (allowed('trainee') && user.id === candidate.userId))) {
        if (path.includes('7404') && !sample.score.certificateId) return missing();
        return new HttpResponse(sampleCertificatePdf(), { headers: { 'Content-Type': 'application/pdf' } });
      }
      if (path.startsWith('/assessor/')) return allowed('assessor') ? await assessorRequest(path, request, sample) : accessDenied();
      if (path.startsWith('/employer/')) return allowed('employer') ? await employerRequest(path, request, url, sample) : accessDenied();
      if (path.startsWith('/import-batches')) return allowed('training_provider') ? await importRequest(path, request, url, sample) : accessDenied();
      if (path.startsWith('/dashboards/')) {
        return allowed('super_admin') && request.method === 'GET' ? misRequest(path, url, sample) : accessDenied();
      }
      if (path.startsWith('/communication/')) return allowed('super_admin') ? await communicationRequest(path, request, url, sample) : accessDenied();
      if (allowed('trainee')) return await traineeRequest(path, request, url, sample, user);
      return unsupported();
    } catch {
      return failure(400, 'INVALID_SIMULATION_INPUT', 'The sample request could not be read.');
    }
  }),
  http.all(simulationBaseUrl + '/*', () => unsupported()),
  // Fail closed for any accidental direct API or management request from this simulated document.
  http.all(/^https?:\/\/[^/]+\/(?:api\/|actuator(?:\/|$))/, () =>
    failure(403, 'LIVE_API_DISABLED', 'Live API access is disabled in simulation mode.')),
);

function accessDenied() {
  return failure(403, 'ACCESS_DENIED', 'Choose the sample persona for this workflow.');
}

export async function startSimulation() {
  if (!simulationActive || !localEvaluatorEnabled || !window.isSecureContext || !navigator.serviceWorker) {
    throw new Error('Simulator requires an explicit loopback session with service workers enabled.');
  }
  const registration = await worker.start({ serviceWorker: { url: '/mockServiceWorker.js' },
    onUnhandledFrame: 'bypass', quiet: true });
  if (!registration) throw new Error('The evaluator service worker did not start.');
}
