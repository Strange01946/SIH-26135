import { readFile } from 'node:fs/promises';
import { setTimeout as delay } from 'node:timers/promises';
import { expect, test } from './support/browser.mjs';
import { expectJson } from './support/contract.mjs';
import { sql } from './support/database.mjs';

const kpis = '/api/v1/dashboards/kpis';
const batches = '/api/v1/training-batches';
const enrollments = '/api/v1/training-enrollments';
const auth = (token) => ({ Authorization: 'Bearer ' + token });

test('admin signs in and the district KPI withholds a small cohort', async ({ page, smoke }) => {
  const { run } = smoke;
  const [response] = await Promise.all([smoke.waitApi('GET', kpis),
    smoke.signIn('admin', `/analytics?stateId=${run.state}&districtId=${run.district}`)]);
  const data = await expectJson(response, 'GET', kpis, 200, ['executiveKpis']);
  expect(new URL(response.url()).searchParams.get('districtId')).toBe(String(run.district));
  await expect(page.getByRole('heading', { name: 'Government MIS & executive analytics' })).toBeVisible();
  const enrolled = data.executiveKpis.kpis.find(({ key }) => key === 'enrolled');
  expect(enrolled.current.availability).toBe('SUPPRESSED');
  expect(enrolled.current.value ?? null).toBeNull();
  const card = page.getByRole('listitem').filter({ has: page.getByRole('heading', { name: 'Total enrolled', exact: true }) });
  await expect(card.getByText('Suppressed', { exact: true })).toBeVisible();
  await expect(page.getByRole('heading', { name: 'State and district outcomes' })).toBeVisible();
});

test('provider enrolls a trainee in a batch created through an approved venue', async ({ page, request, smoke }) => {
  const { run } = smoke;
  const provider = await smoke.signIn('provider', '/provider/batches');
  const headers = auth(provider.accessToken);
  await expect(page.getByRole('link', { name: 'Request a training venue' })).toBeVisible();
  await expect(page.getByRole('button', { name: 'Create New Batch' })).toHaveCount(0);
  const venueRequests = '/api/v1/venue-bookings';
  const payload = { batchCode: run.batchCode, seats: 10, skillsToTeach: run.id + ' Inspect worksite equipment',
    temporaryVenue: { name: 'Enrollment fixture lab', address: 'Temporary training fixture',
      stateId: run.state, districtId: run.district, capacity: 15, permissionToUse: 'Test venue approval' },
    sessions: [{ startsAt: run.starts + 'T09:00:00', endsAt: run.starts + 'T11:00:00' }],
  };
  const proposed = await request.post(venueRequests, { headers, data: payload });
  expect(proposed.status()).toBe(201);
  const id = (await proposed.json()).data.booking.id;
  const login = await request.post('/api/v1/auth/login', { data: { usernameOrEmail: run.users.district, password: run.password } });
  expect(login.status()).toBe(200);
  const reviewer = auth((await login.json()).data.accessToken);
  const acceptedBooking = await request.post(venueRequests + '/' + id + '/review', {
    headers: reviewer, data: { approved: true },
  });
  expect(acceptedBooking.status()).toBe(200);
  const details = (await acceptedBooking.json()).data.booking;
  expect(details).toMatchObject({ batchCode: run.batchCode, providerId: run.provider, status: 'APPROVED' });
  const batch = { id: details.batchId };
  const opened = await request.put(batches + '/' + batch.id + '/status', { headers, data: { batchStatusId: 2 } });
  expect(opened.status()).toBe(200);
  const duplicate = await request.post(venueRequests, { headers, data: payload });
  expect(duplicate.status()).toBe(409);
  expect(await duplicate.json()).toMatchObject({ status: 409, code: 'BATCH_CODE_ALREADY_EXISTS' });

  await page.getByRole('link', { name: 'Candidate Enrollment' }).click();
  await page.getByPlaceholder('e.g. TRN-2026-0001').fill(run.enrollmentCandidate);
  await page.getByRole('button', { name: 'Find Trainee' }).click();
  await expect(page.getByText('Neel Smoke', { exact: true }).first()).toBeVisible();
  const batchSelector = page.getByRole('combobox', { name: /Target Training Batch/i });
  await batchSelector.selectOption(String(batch.id));
  const [accepted] = await Promise.all([smoke.waitApi('POST', enrollments),
    page.getByRole('button', { name: 'Confirm Candidate Enrollment' }).click()]);
  const enrollment = await expectJson(accepted, 'POST', enrollments, 201, ['id', 'traineeId', 'batchId']);
  expect(enrollment).toMatchObject({ traineeId: run.prospect, batchId: batch.id, providerId: run.provider });
  expect(sql(`SELECT course_id,program_id FROM training_enrollments WHERE enrollment_id=${enrollment.id} AND trainee_id=${run.prospect};`))
    .toBe([details.courseId, details.programId].join('\t'));
});

test('assessor validates marks, finalizes and downloads the issued certificate', async ({ page, smoke }) => {
  const { run } = smoke;
  const base = `/api/v1/assessor/assessments/${run.assessment}`;
  const [roster] = await Promise.all([smoke.waitApi('GET', base + '/roster'),
    smoke.signIn('assessor', '/assessor/assessments/' + run.assessment)]);
  await expectJson(roster, 'GET', '/api/v1/assessor/assessments/{id}/roster', 200, ['candidates']);
  await expect(page.getByRole('heading', { name: 'Smoke Solar Assessment' })).toBeVisible();
  const score = (column) => page.locator(`input[data-row="0"][data-col="${column}"]`);
  const finalize = page.getByRole('button', { name: 'Finalize & Issue Certificates' });
  await score(0).fill('100');
  await score(1).fill('45');
  await score(2).fill('15');
  await expect(finalize).toBeDisabled();
  await score(0).fill('25');
  await finalize.click();
  const [response] = await Promise.all([smoke.waitApi('POST', base + '/finalize'),
    page.getByRole('button', { name: 'Confirm & Finalize' }).click()]);
  const data = await expectJson(response, 'POST', '/api/v1/assessor/assessments/{id}/finalize', 200, ['certificatesIssued', 'results']);
  expect(data).toMatchObject({ isFinalized: true, certificatesIssued: 1, passedCount: 1 });
  const result = data.results.find(({ traineeId }) => traineeId === run.trainee);
  expect(result.totalScore).toBe(85);
  expect(result.certificateId).toBeGreaterThan(0);
  const [download, pdf] = await Promise.all([page.waitForEvent('download'),
    smoke.waitApi('GET', `/api/v1/certifications/${result.certificateId}/download`),
    page.getByTitle('Download PDF Certificate').click()]);
  expect(pdf.status()).toBe(200);
  expect(pdf.headers()['content-type']).toContain('application/pdf');
  expect((await readFile(await download.path())).subarray(0, 5).toString()).toBe('%PDF-');
});

test('employer attests a pending verification with multipart salary proof', async ({ page, smoke }) => {
  const { run } = smoke;
  const base = `/api/v1/employer/verifications/${run.request}`;
  const evidence = Buffer.from('%PDF-1.4\n% Synthetic Phase 30.14 payslip\n%%EOF\n');
  const [pending] = await Promise.all([smoke.waitApi('GET', '/api/v1/employer/verifications/pending'),
    smoke.signIn('employer', '/employer/verifications')]);
  const queue = await expectJson(pending, 'GET', '/api/v1/employer/verifications/pending', 200, ['content']);
  expect(queue.content.some(({ id }) => id === run.request)).toBe(true);
  const record = page.getByRole('listitem').filter({ hasText: run.id + '-request' });
  await record.getByRole('button', { name: 'Respond to request' }).click();
  await page.getByRole('radio', { name: 'Attest employment' }).check();
  await page.getByLabel(/Payslip or salary proof/).setInputFiles({ name: 'smoke-payslip.pdf', mimeType: 'application/pdf', buffer: evidence });
  const [verdict] = await Promise.all([smoke.waitApi('POST', base + '/verdict'),
    page.getByRole('button', { name: 'Submit response' }).click()]);
  expect(verdict.request().headers()['content-type']).toContain('multipart/form-data; boundary=');
  const completed = await expectJson(verdict, 'POST', '/api/v1/employer/verifications/{requestId}/verdict', 201,
    ['requestStatus', 'verdict', 'evidenceId']);
  expect(completed).toMatchObject({ id: run.request, requestStatus: 'COMPLETED', verdict: 'VERIFIED' });
  const [download, proof] = await Promise.all([page.waitForEvent('download'), smoke.waitApi('GET', `${base}/evidence/${completed.evidenceId}`),
    record.getByRole('button', { name: 'Download evidence' }).click()]);
  expect(proof.status()).toBe(200);
  expect(await readFile(await download.path())).toEqual(evidence);
});

test('trainee reports employment and authorized staff can assign and review follow-ups', async ({ page, request, smoke }) => {
  const { run } = smoke;
  const consentPath = '/api/v1/consents';
  const surveyPath = `/api/v1/trainees/me/followup-surveys/${run.task}/responses`;
  const surveyRoot = '/api/v1/trainees/me/followup-surveys';
  const careerUpdate = surveyRoot + '/career-update';
  const staffSurveys = '/api/v1/career-progress/candidates/' + run.trainee + '/surveys';
  sql(`UPDATE trainees SET current_employment_status_id=(SELECT employment_status_id FROM ref_employment_status
    WHERE status_code='UNKNOWN') WHERE trainee_id=${run.trainee};`);
  const initialTrainee = await smoke.signIn('trainee', '/trainee/journey');
  expect((await request.get(careerUpdate, { headers: auth(initialTrainee.accessToken) })).ok()).toBe(false);
  const careerReport = page.getByRole('region', { name: 'Report employment update' });
  await expect(careerReport.getByText(/Employment follow-up consent is required/)).toBeVisible();
  await expect(careerReport.getByRole('button', { name: 'Report employment update' })).toHaveCount(0);
  await careerReport.getByRole('link', { name: 'Review employment follow-up consent' }).click();
  await expect(page).toHaveURL(/\/trainee\/consent#employment-follow-up$/);
  const consentTitle = page.getByRole('heading', { name: 'Employment Outcome Follow-Up & Verification' });
  await expect(consentTitle).toBeFocused();
  const consentButton = consentTitle.locator('xpath=ancestor::*[.//button[normalize-space()="Grant Consent"]][1]')
    .getByRole('button', { name: 'Grant Consent' });
  const [saved] = await Promise.all([smoke.waitApi('POST', consentPath), consentButton.click()]);
  const consent = await expectJson(saved, 'POST', consentPath, 201, ['consentTypeCode', 'consentStatusCode']);
  expect(consent).toMatchObject({ traineeId: run.trainee, consentTypeCode: 'EMPLOYMENT_FOLLOW_UP', consentStatusCode: 'GRANTED' });
  const surveyOverview = await request.get(surveyRoot, { headers: { authorization: saved.request().headers().authorization } });
  expect((await surveyOverview.json()).data).toMatchObject({ followupConsentGranted: true, careerUpdateAvailable: true });
  await page.getByRole('link', { name: 'Return to Career Journey & Surveys' }).click();
  await expect(careerReport.getByRole('button', { name: 'Report employment update' })).toBeVisible();
  await expect(page.getByText('Wage employed', { exact: true })).toBeVisible();
  await expect(page.getByText(/Starting pay.*18,500/)).toBeVisible();
  await page.getByRole('button', { name: 'Open Smoke Six-month Follow-up' }).click();
  const submit = page.getByRole('button', { name: 'Submit survey' });
  await submit.click();
  await expect(page.getByText('Answer this question to continue.')).toBeVisible();
  await page.getByRole('combobox').filter({ has: page.getByRole('option', { name: 'Yes', exact: true }) }).selectOption('true');
  const [response] = await Promise.all([smoke.waitApi('POST', surveyPath), submit.click()]);
  const receipt = await expectJson(response, 'POST', '/api/v1/trainees/me/followup-surveys/{taskId}/responses', 201, ['id', 'name', 'submittedAt']);
  expect(receipt.name).toBe('Smoke Six-month Follow-up');
  await expect(page.getByText('Survey submitted · Smoke Six-month Follow-up')).toBeVisible();
  const duplicate = await request.post(surveyPath, { headers: { authorization: response.request().headers().authorization },
    data: response.request().postDataJSON() });
  expect(duplicate.status()).toBe(409);
  expect(await duplicate.json()).toMatchObject({ status: 409, code: 'SURVEY_ASSIGNMENT_CLOSED' });

  await page.getByRole('button', { name: 'Report employment update' }).click();
  const next = page.getByRole('button', { name: 'Next question' });
  await page.getByRole('combobox', { name: /current work or education status/ }).selectOption({ label: 'Self-employed' });
  await next.click();
  await page.getByRole('textbox', { name: /name of your employer or business/ }).fill('Repair workshop');
  await next.click();
  await page.getByRole('textbox', { name: /role, occupation or apprenticeship/ }).fill('Workshop technician');
  await next.click();
  await next.click();
  await page.getByRole('spinbutton', { name: /current monthly wage/ }).fill('26000.50');
  await next.click();
  await next.click();
  await page.getByRole('textbox', { name: /skills from your training/ }).fill('Motor diagnostics');
  await next.click();
  const [reported] = await Promise.all([smoke.waitApi('POST', careerUpdate), submit.click()]);
  const report = await expectJson(reported, 'POST', careerUpdate, 201, ['id', 'name', 'submittedAt']);
  await expect(page.getByText('Survey submitted · Employment outcome update')).toBeVisible();
  const reportRow = page.getByRole('listitem').filter({ hasText: 'Employment outcome update' });
  await reportRow.getByRole('button', { name: 'View answers' }).click();
  await expect(reportRow.getByText('Self-employed', { exact: true })).toBeVisible();
  await expect(reportRow.getByText('26000.50', { exact: true })).toBeVisible();
  const current = page.getByRole('region', { name: 'Current career information' });
  await expect(current).toContainText('Self-employed');
  await expect(current).toContainText('₹26,000.50');
  await expect(current).toContainText('Workshop technician');
  await expect(current).toContainText('Motor diagnostics');
  await expect(page.getByText(/Starting pay.*18,500/)).toBeVisible();
  expect(sql(`SELECT r.followup_task_id IS NULL,a.numeric_value FROM survey_responses r
    JOIN survey_response_answers a USING(survey_response_id) JOIN survey_questions q USING(survey_question_id)
    WHERE r.survey_response_id=${report.id} AND r.trainee_id=${run.trainee} AND q.question_code='MONTHLY_INCOME';`))
    .toBe('1\t26000.50');
  expect(sql(`SELECT COUNT(*) FROM employment_records WHERE trainee_id=${run.trainee} AND deleted_at IS NULL;`)).toBe('1');

  const traineeHeaders = { authorization: reported.request().headers().authorization };
  await page.getByRole('button', { name: 'Report employment update' }).click();
  const workStatus = page.getByRole('combobox', { name: /current work or education status/ });
  await expect(workStatus.locator('option:checked')).toHaveText('Self-employed');
  await next.click();
  await expect(page.getByRole('textbox', { name: /name of your employer or business/ })).toHaveValue('Repair workshop');
  await page.getByRole('button', { name: 'Close survey' }).click();
  const wrongVersion = await request.post(careerUpdate, {
    headers: traineeHeaders, data: { ...reported.request().postDataJSON(), versionId: 2147483647 },
  });
  expect(wrongVersion.status()).toBe(409);
  const denied = await request.get(staffSurveys, { headers: traineeHeaders });
  expect(denied.status()).toBe(403);

  await page.getByRole('button', { name: 'Sign Out' }).click();
  await smoke.signIn('admin', '/admin/careers');
  const registration = sql(`SELECT registration_number FROM trainees WHERE trainee_id=${run.trainee};`);
  await page.getByLabel('Candidate name or registration number').fill(registration);
  await page.getByRole('button', { name: 'Search candidates' }).click();
  await page.getByRole('button', { name: new RegExp(registration) }).click();
  await expect(current).toContainText('Self-employed');
  await expect(current).toContainText('₹26,000.50');
  await expect(current).toContainText('Motor diagnostics');
  const workbench = page.getByRole('region', { name: 'Candidate follow-up surveys' });
  const pastReport = workbench.getByRole('listitem').filter({ hasText: 'Employment outcome update' });
  await pastReport.getByRole('button', { name: 'View answers' }).click();
  await expect(pastReport.getByText('Repair workshop', { exact: true })).toBeVisible();

  const traineeLogin = await request.post('/api/v1/auth/login', {
    data: { usernameOrEmail: run.users.trainee, password: run.password },
  });
  expect(traineeLogin.status()).toBe(200);
  const freshTrainee = auth((await traineeLogin.json()).data.accessToken);
  const fields = (await (await request.get(careerUpdate, { headers: freshTrainee })).json()).data;
  const incomeId = fields.questions.find(question => question.text.includes('current monthly wage')).id;
  const skillsId = fields.questions.find(question => question.text.includes('skills from your training')).id;
  const revisedAnswers = fields.initialAnswers.map(answer => {
    if (answer.questionId === incomeId) return { questionId: incomeId, number: 29500 };
    if (answer.questionId === skillsId) return { questionId: skillsId, text: 'Electrical troubleshooting' };
    return answer;
  });
  const updated = await request.post(careerUpdate, { headers: freshTrainee,
    data: { versionId: fields.versionId, answers: revisedAnswers } });
  expect(updated.status()).toBe(201);
  const currentReport = (await updated.json()).data;
  expect(currentReport.id).not.toBe(report.id);
  await expect(current).toContainText('₹29,500.00', { timeout: 13_000 });
  await expect(current).toContainText('Electrical troubleshooting');
  await expect(current).toContainText('Repair workshop');
  await expect(pastReport).toHaveCount(2);

  await workbench.getByLabel('Campaign', { exact: true }).selectOption({ label: 'Employment outcome follow-up' });
  const [scheduled] = await Promise.all([smoke.waitApi('POST', staffSurveys),
    workbench.getByRole('button', { name: 'Assign survey' }).click()]);
  const task = await expectJson(scheduled, 'POST', '/api/v1/career-progress/candidates/{id}/surveys', 201, ['taskId', 'name']);
  await expect(workbench.getByText('Survey assigned.', { exact: true })).toBeVisible();
  expect(sql(`SELECT trainee_id,completed_at IS NULL FROM followup_tasks WHERE followup_task_id=${task.taskId};`))
    .toBe([run.trainee, 1].join('\t'));
  const staffHeaders = { authorization: scheduled.request().headers().authorization };
  const assignmentRetry = await request.post(staffSurveys, { headers: staffHeaders, data: scheduled.request().postDataJSON() });
  expect(assignmentRetry.status()).toBe(409);
  const crossCandidate = await request.get('/api/v1/career-progress/candidates/' + run.prospect + '/surveys/' + report.id,
    { headers: staffHeaders });
  expect(crossCandidate.ok()).toBe(false);

  await page.getByRole('button', { name: 'Sign Out' }).click();
  await smoke.signIn('trainee', '/trainee/journey');
  const [opened] = await Promise.all([smoke.waitApi('GET', surveyRoot + '/' + task.taskId),
    page.getByRole('button', { name: 'Open Employment outcome update' }).click()]);
  expect(opened.status()).toBe(200);
  await expect(page.getByRole('combobox', { name: /current work or education status/ })).toBeVisible();
  const [profile] = await Promise.all([smoke.waitApi('GET', '/api/v1/trainees/me/profile'),
    page.getByRole('link', { name: 'My Profile', exact: true }).last().click()]);
  expect((await profile.json()).data).toMatchObject({ employmentStatus: 'Self-employed',
    reportedUpdate: { submissionId: currentReport.id, statusCode: 'SELF_EMPLOYED', monthlyIncome: 29500, skills: 'Electrical troubleshooting' } });
  await expect(page.getByRole('heading', { name: 'My Profile', exact: true })).toBeVisible();
  await expect(current).toContainText('Self-employed');
  await expect(current).toContainText('₹29,500.00');
  await expect(current).toContainText('Electrical troubleshooting');
  await page.getByRole('link', { name: 'Overview', exact: true }).click();
  await expect(current).toContainText('Self-employed');
  await expect(current).toContainText('₹29,500.00');
  await expect(current).toContainText('Electrical troubleshooting');
  await page.getByRole('link', { name: 'My Profile', exact: true }).last().click();
  await page.getByRole('link', { name: 'Manage consent' }).click();
  const followup = page.getByRole('group', { name: 'Employment Outcome Follow-Up & Verification' });
  const consentStatusPath = `${consentPath}/${consent.id}/status`;
  const [withdrawn] = await Promise.all([smoke.waitApi('PUT', consentStatusPath),
    followup.getByRole('button', { name: 'Revoke Consent' }).click()]);
  expect((await withdrawn.json()).data.consentStatusCode).toBe('REVOKED');
  await expect(followup.getByRole('status')).toHaveText('Consent withdrawn.');
  const deniedUpdate = await request.get(careerUpdate, { headers: { authorization: withdrawn.request().headers().authorization } });
  expect(deniedUpdate.ok()).toBe(false);
  await page.getByRole('link', { name: 'Career Journey & Surveys' }).last().click();
  await expect(careerReport.getByRole('link', { name: 'Review employment follow-up consent' })).toBeVisible();
  await expect(current).toContainText('₹29,500.00');
  const adminLogin = await request.post('/api/v1/auth/login', {
    data: { usernameOrEmail: run.users.admin, password: run.password },
  });
  expect(adminLogin.status()).toBe(200);
  const freshAdmin = auth((await adminLogin.json()).data.accessToken);
  const history = await request.get('/api/v1/career-progress/candidates/' + run.trainee, { headers: freshAdmin });
  expect(history.status()).toBe(200);
  const restricted = (await history.json()).data;
  expect(restricted).not.toHaveProperty('reportedUpdate');
  expect(restricted.candidate.status).toBe('Wage employed');
});

test('backend enforces authority and provider tenancy', async ({ page, request, smoke }) => {
  const user = await smoke.signIn('outsider', '/provider/batches');
  const otherBatch = sql(`SELECT batch_id FROM training_batches WHERE batch_code='${smoke.run.id}-assess-batch';`);
  expect(Number(otherBatch)).toBeGreaterThan(0);
  for (const path of [kpis, batches + '/' + otherBatch]) {
    const denied = await request.get(path, { headers: auth(user.accessToken) });
    expect(denied.status(), path).toBe(403);
    expect((await denied.json()).success).toBe(false);
  }
  await page.goto('/analytics');
  await expect(page).toHaveURL(/\/forbidden$/);
});

test('expired access is rejected and concurrent portal reads share one live refresh', async ({ page, request, smoke }) => {
  const refresh = '/api/v1/auth/refresh';
  const center = '/api/v1/communication/center';
  const logs = '/api/v1/communication/logs';
  const user = await smoke.signIn('admin', '/communication');
  await expect(page.getByRole('heading', { name: 'Communication Center' })).toBeVisible();
  const { exp } = JSON.parse(Buffer.from(user.accessToken.split('.')[1], 'base64url').toString('utf8'));
  expect(user.expiresIn).toBe(15);
  await delay(Math.max(0, exp * 1000 + 1100 - Date.now()));
  const expired = await request.get('/api/v1/auth/me', { headers: auth(user.accessToken) });
  expect(expired.status()).toBe(401);
  const refreshes = [];
  page.on('request', (entry) => { if (new URL(entry.url()).pathname === refresh) refreshes.push(entry); });
  const [replacement, settings, result] = await Promise.all([smoke.waitApi('POST', refresh), smoke.waitApi('GET', center), smoke.waitApi('GET', logs),
    page.getByRole('button', { name: 'Delivery logs' }).click()]);
  const next = await expectJson(replacement, 'POST', refresh, 200, ['accessToken', 'refreshToken']);
  expect(next.accessToken).not.toBe(user.accessToken);
  for (const response of [settings, result]) {
    expect(response.status()).toBe(200);
    expect(response.request().headers().authorization).toBe('Bearer ' + next.accessToken);
  }
  expect(refreshes).toHaveLength(1);
});

test('provider uploads a CSV with an invalid row, quarantines it and downloads the errors', async ({ page, smoke }) => {
  const imports = '/api/v1/import-batches';
  const { run } = smoke;
  const csv = Buffer.from('registrationNumber,firstName,lastName,genderId,stateId,districtId,currentEmploymentStatusId,profileStatusId\n'
    + `${run.uploadCandidate},,Smoke,2,${run.state},${run.district},1,3\n`);
  await smoke.signIn('provider', '/provider/imports');
  await page.locator('input[type=file]').setInputFiles({ name: 'smoke-roster.csv', mimeType: 'text/csv', buffer: csv });
  const [response] = await Promise.all([smoke.waitApi('POST', imports),
    page.getByRole('button', { name: 'Upload & Stage Import Batch' }).click()]);
  expect(response.request().headers()['content-type']).toContain('multipart/form-data; boundary=');
  const upload = await expectJson(response, 'POST', imports, 201, ['id', 'importBatchStatusCode']);
  const base = `${imports}/${upload.id}`;
  const [processed] = await Promise.all([smoke.waitApi('POST', base + '/process'),
    page.getByRole('button', { name: 'Execute Batch Processing' }).click()]);
  const result = await expectJson(processed, 'POST', imports + '/{id}/process', 200, ['status', 'rowCount', 'failureCount']);
  expect(result).toMatchObject({ rowCount: 1, failureCount: 1, successCount: 0 });
  await page.getByRole('link', { name: /Inspect Quarantined Records \(1 rows\)/ }).click({ timeout: 25_000 });
  const [download, report] = await Promise.all([page.waitForEvent('download'), smoke.waitApi('GET', base + '/quarantine/export'),
    page.getByRole('button', { name: 'Export Error CSV' }).click()]);
  expect(report.status()).toBe(200);
  expect(report.headers()['content-type']).toContain('text/csv');
  const output = await readFile(await download.path(), 'utf8');
  expect(output.trim().split(/\r?\n/)).toHaveLength(2);
  expect(output).toMatch(/first.?name|required|validation/i);
});

test('analytics renders a controlled service failure and retries against Spring Boot', async ({ page, smoke }) => {
  smoke.allowHttpFailure('GET', kpis, 503);
  let attempts = 0;
  await page.route('**/api/v1/dashboards/kpis*', (route) => {
    attempts++;
    return route.fulfill({ status: 503, json: { status: 503, code: 'SERVICE_UNAVAILABLE', message: 'Smoke test temporary unavailability' } });
  });
  await smoke.signIn('admin', '/analytics');
  await expect(page.getByText('Unable to load the selected analytics cohort')).toBeVisible();
  expect(attempts).toBeGreaterThan(0);
  await page.unroute('**/api/v1/dashboards/kpis*');
  const [response] = await Promise.all([smoke.waitApi('GET', kpis), page.getByRole('button', { name: 'Retry' }).click()]);
  await expectJson(response, 'GET', kpis, 200, ['executiveKpis']);
  await expect(page.getByText('Total enrolled', { exact: true })).toBeVisible();
});
