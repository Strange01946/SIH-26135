import { readFile } from 'node:fs/promises';
import { expect, test } from './support/browser.mjs';
import { quote, sql } from './support/database.mjs';

const traineeApi = '/api/v1/trainees/me/training';
const batchApi = '/api/v1/training-batches';
const venueApi = '/api/v1/venue-bookings';
const requirementApi = '/api/v1/training-requirements';
const find = '/trainee/find-training';
const consentApi = '/api/v1/consents';
const day = offset => new Intl.DateTimeFormat('sv-SE', { timeZone: 'Asia/Kolkata' })
  .format(new Date(Date.now() + offset * 86_400_000));
const auth = token => ({ Authorization: 'Bearer ' + token });
const slot = (offset, from, to) => ({ startsAt: day(offset) + 'T' + from + ':00', endsAt: day(offset) + 'T' + to + ':00' });
const signOut = page => page.getByRole('button', { name: 'Sign Out', exact: true }).click();

async function result(response, status) {
  const payload = await response.json();
  expect(response.status(), payload.code + ' ' + payload.message).toBe(status);
  return payload.data;
}

async function login(request, run, role) {
  return auth((await result(await request.post('/api/v1/auth/login', {
    data: { usernameOrEmail: run.users[role], password: run.password },
  }), 200)).accessToken);
}

const register = (request, id, headers) => request.post(`${traineeApi}/${id}/enrollment`, { headers });
const roster = (request, id, headers) => request.get(`${batchApi}/${id}/roster`, { headers });
const advance = (request, id, headers, batchStatusId = 2) =>
  request.put(`${batchApi}/${id}/status`, { headers, data: { batchStatusId } }).then(response => result(response, 200));

test('training registration, attendance, assessor evaluation and published PDFs work across all three portals', async ({ page, request, smoke }) => {
  test.setTimeout(120_000);
  const { run } = smoke;
  const districtId = Number(sql(`SELECT district_id FROM districts WHERE state_id=${run.state}
    AND lgd_code IS NOT NULL AND lifecycle_status_id=1 AND deleted_at IS NULL ORDER BY district_id LIMIT 1;`));
  expect(districtId).toBeGreaterThan(0);
  const code = run.id + '-venue-self';
  const venueName = 'Solar practical workshop';
  const address = '14 Workshop Road, Training Block';
  const skills = run.id + ' Solar panel installation and electrical wiring';
  const providerPrerequisites = 'Familiarity with hand tools is recommended.';
  const governmentPrerequisites = 'Basic programming experience is recommended.';
  const secondary = Number(sql("SELECT education_level_id FROM ref_education_level WHERE education_code='SECONDARY';"));
  const higherSecondary = Number(sql("SELECT education_level_id FROM ref_education_level WHERE education_code='HIGHER_SECONDARY';"));
  const sessions = [slot(6, '10:15', '12:00'), slot(7, '13:30', '15:00')];
  const provider = await login(request, run, 'provider');
  const admin = await login(request, run, 'admin');
  const temporary = await result(await request.post(venueApi, { headers: provider, data: {
    batchCode: code, seats: 2, skillsToTeach: skills, sessions, recommendedPrerequisites: providerPrerequisites,
    temporaryVenue: { name: venueName, address, stateId: run.state, districtId, capacity: 8, permissionToUse: 'Fixture authorization' },
  } }), 201);
  const approved = await result(await request.post(venueApi + '/' + temporary.booking.id + '/review', {
    headers: admin, data: { approved: true },
  }), 200);
  const batchId = approved.booking.batchId;
  const centre = await result(await request.post(venueApi + '/venues', { headers: admin, data: {
    code: run.id + '-gov-lab-self', name: 'Government electrical workshop', address: 'Training campus',
    pincode: '110001', capacity: 10, stateId: run.state, districtId,
  } }), 201);
  const government = await result(await request.post(requirementApi + '/government', { headers: admin, data: {
    title: run.id + '-requirement-self Java Backend Development', skillsToTeach: skills, stateId: run.state, districtId, centerId: centre.id,
    seats: 1, earliestDate: day(10), latestDate: day(11), dailyStartTime: '09:00', dailyEndTime: '13:00',
    minimumEducationLevelId: higherSecondary, recommendedPrerequisites: governmentPrerequisites,
  } }), 201);
  const offer = await result(await request.post(requirementApi + '/' + government.id + '/applications', { headers: provider,
    data: { startDate: day(10), endDate: day(11), dailyStartTime: '10:00', dailyEndTime: '12:00' },
  }), 201);
  const award = await result(await request.post(requirementApi + '/government/' + government.id + '/select', {
    headers: admin, data: { applicationId: offer.id },
  }), 200);
  const governmentBooking = await result(await request.get(venueApi + '/' + award.bookingId, { headers: admin }), 200);
  expect(governmentBooking.booking.admission).toMatchObject({
    minimumEducationLevelId: higherSecondary, recommendedPrerequisites: governmentPrerequisites,
  });
  const governmentBatch = governmentBooking.booking.batchId;

  sql(`UPDATE trainees SET user_id=(SELECT user_id FROM users WHERE username=${quote(run.users.trainee2)}),
      education_level_id=NULL WHERE trainee_id=${run.prospect};
    UPDATE trainees SET education_level_id=NULL WHERE trainee_id=${run.trainee};`);
  const trainee = await login(request, run, 'trainee');
  const params = { search: run.id + ' sol wir', districtId };
  const listing = () => request.get(traineeApi, { headers: trainee, params }).then(response => result(response, 200));
  expect((await listing()).batches).toEqual([]);
  const planned = await register(request, batchId, trainee);
  expect(planned.status()).toBe(409);
  expect((await planned.json()).code).toBe('TRAINING_REGISTRATION_CLOSED');
  await advance(request, batchId, provider);
  await advance(request, governmentBatch, provider);
  expect((await listing()).batches).toMatchObject([{ id: batchId, governmentLed: false, seatsAvailable: 2 },
    { id: governmentBatch, governmentLed: true, title: government.title, seatsAvailable: 1 }]);
  for (const search of ['java developer', 'javvaa']) {
    const found = await result(await request.get(traineeApi, { headers: trainee, params: { search: run.id + ' ' + search } }), 200);
    expect(found.batches.map(item => item.id)).toEqual([governmentBatch]);
  }

  const providerEnroll = async () => request.post('/api/v1/training-enrollments', {
    headers: await login(request, run, 'provider'),
    data: { traineeId: run.prospect, batchId: governmentBatch, enrollmentDate: day(0) },
  });
  const expectEducationDenial = async (response, code) => {
    expect(response.status()).toBe(409);
    expect((await response.json()).code).toBe(code);
  };
  await expectEducationDenial(await register(request, governmentBatch, trainee), 'TRAINING_EDUCATION_REQUIRED');
  await expectEducationDenial(await providerEnroll(), 'TRAINING_EDUCATION_REQUIRED');
  await smoke.signIn('trainee', `${find}/${governmentBatch}`);
  const restrictedDetails = page.getByRole('region', { name: 'Batch details' });
  await expect(restrictedDetails).toContainText('Higher secondary (12th)');
  await expect(restrictedDetails).toContainText(governmentPrerequisites);
  await expect(restrictedDetails.getByRole('link', { name: 'Update education in your profile' })).toBeVisible();
  await expect(restrictedDetails.getByRole('button', { name: 'Register for training' })).toBeDisabled();
  sql(`UPDATE trainees SET education_level_id=${secondary} WHERE trainee_id IN (${run.trainee},${run.prospect});`);
  await expectEducationDenial(await register(request, governmentBatch, await login(request, run, 'trainee')), 'TRAINING_EDUCATION_BELOW_MINIMUM');
  await expectEducationDenial(await providerEnroll(), 'TRAINING_EDUCATION_BELOW_MINIMUM');
  expect(sql(`SELECT COUNT(*) FROM training_enrollments WHERE batch_id=${governmentBatch};`)).toBe('0');

  const currentCore = sql(`SELECT trainee_consent_id FROM trainee_consents WHERE trainee_id=${run.trainee}
    AND consent_type_id=1 ORDER BY trainee_consent_id DESC LIMIT 1;`);
  const consentActor = await login(request, run, 'trainee');
  const revoked = await request.put(consentApi + '/' + currentCore + '/status', { headers: consentActor, data: { consentStatusId: 2 } });
  expect(revoked.status()).toBe(200);
  const noConsent = await register(request, batchId, consentActor);
  expect(noConsent.status()).toBe(400);
  expect((await noConsent.json()).code).toBe('CONSENT_REVOKED');
  expect(sql(`SELECT COUNT(*) FROM training_enrollments WHERE batch_id=${batchId};`)).toBe('0');

  sql(`UPDATE trainees SET education_level_id=NULL WHERE trainee_id=${run.trainee};`);
  await page.goto(find);
  const filter = page.getByRole('form', { name: 'Filter training' });
  await filter.getByRole('searchbox', { name: 'Search by title or skill' }).fill(run.id + ' sol wir');
  await filter.getByLabel('State or union territory', { exact: true }).selectOption(String(run.state));
  await filter.getByLabel('District', { exact: true }).selectOption(String(districtId));
  await filter.getByLabel('Training starts from').fill(day(6));
  await filter.getByLabel('Training starts through').fill(day(6));
  await filter.getByRole('button', { name: 'Search training' }).click();
  const items = page.getByRole('list', { name: 'Open training batches' });
  const chosen = items.getByRole('listitem').filter({ hasText: code });
  await expect(chosen).toContainText('Provider-led');
  await expect(items.getByText(government.title, { exact: true })).toHaveCount(0);
  await chosen.getByRole('link').click();
  const details = page.getByRole('region', { name: 'Batch details' });
  const timetable = page.getByRole('region', { name: 'Session timetable' });
  await expect(details).toContainText(address);
  await expect(details).toContainText(venueName);
  await expect(details).toContainText(skills);
  await expect(details).toContainText('Open to all');
  await expect(details).toContainText(providerPrerequisites);
  await expect(timetable.getByRole('row')).toHaveCount(3);
  await expect(timetable).toContainText('10:15');
  await expect(timetable).toContainText('13:30');
  const button = details.getByRole('button', { name: 'Register for training' });
  await expect(button).toBeDisabled();
  await details.getByRole('link', { name: 'DPDP Privacy & Consent' }).click();
  const core = page.getByRole('group', { name: 'Core Scheme Data Processing' });
  const [regranted] = await Promise.all([smoke.waitApi('POST', consentApi), core.getByRole('button', { name: 'Grant Consent' }).click()]);
  await result(regranted, 201);
  await page.goBack();
  const [confirmed] = await Promise.all([smoke.waitApi('POST', traineeApi + '/' + batchId + '/enrollment'), button.click()]);
  const enrollment = await result(confirmed, 201);
  expect(enrollment).toMatchObject({ traineeId: run.trainee, batchId, enrollmentStatusCode: 'ENROLLED' });
  expect(sql(`SELECT COUNT(*) FROM audit_logs WHERE entity_type='TrainingEnrollment' AND entity_id=${enrollment.id};`)).toBe('1');
  await expect(details.getByRole('status')).toContainText('Seat confirmed');
  await expect(details).toContainText('1 seat available');
  await expect(button).toHaveCount(0);
  await page.getByRole('navigation', { name: 'Training navigation' }).getByRole('link', { name: 'My Training & Attendance' }).click();
  const myTraining = page.getByRole('region', { name: 'Enrolled Training Batches & Schemes' });
  await expect(myTraining).toContainText(enrollment.enrollmentNumber);
  await myTraining.getByRole('link', { name: 'View timetable' }).filter({ visible: true }).first().click();
  await expect(timetable.getByRole('row')).toHaveCount(3);

  sql(`UPDATE trainees SET education_level_id=${higherSecondary} WHERE trainee_id IN (${run.trainee},${run.prospect});`);
  const traineeAgain = await login(request, run, 'trainee');
  const repeated = await register(request, batchId, traineeAgain);
  expect(repeated.status()).toBe(409);
  expect((await repeated.json()).code).toBe('DUPLICATE_ENROLLMENT');
  const second = await login(request, run, 'trainee2');
  const competition = await Promise.all([register(request, governmentBatch, traineeAgain), register(request, governmentBatch, second)]);
  expect(competition.map(response => response.status()).sort()).toEqual([201, 409]);
  expect((await competition.find(response => response.status() === 409).json()).code).toBe('BATCH_CAPACITY_EXCEEDED');
  expect(sql(`SELECT COUNT(*) FROM training_enrollments WHERE batch_id=${governmentBatch};`)).toBe('1');
  sql(`UPDATE trainees SET user_id=NULL WHERE trainee_id=${run.prospect};`);
  expect((await request.get(batchApi + '/' + batchId + '/roster', { headers: traineeAgain })).status()).toBe(403);

  const providerAgain = await login(request, run, 'provider');
  const otherProvider = await login(request, run, 'outsider');
  expect((await roster(request, batchId, otherProvider)).status()).toBe(404);
  expect((await request.get('/api/v1/training-enrollments', { headers: otherProvider, params: { batchId } })).status()).toBe(403);
  expect((await request.get('/api/v1/training-enrollments/' + enrollment.id, { headers: otherProvider })).status()).toBe(403);
  expect((await request.post('/api/v1/attendance-records', { headers: otherProvider, data: {
    enrollmentId: enrollment.id, sessionDate: day(6), sessionSequence: 1, attendanceStatusId: 1,
  } })).status()).toBe(403);
  const before = await result(await roster(request, batchId, providerAgain), 200);
  expect(before.entries).toMatchObject([{ enrollmentId: enrollment.id, name: 'Asha Smoke', registrationNumber: run.candidateCode }]);
  expect(before.entries[0].email ?? null).toBeNull();
  expect(before.entries[0].phone ?? null).toBeNull();
  const lookup = await result(await request.get('/api/v1/trainees', { headers: providerAgain,
    params: { registrationNumber: run.candidateCode } }), 200);
  expect(lookup.email ?? null).toBeNull();
  expect(lookup.phone ?? null).toBeNull();

  await page.goto('/trainee/consent');
  const sharing = page.getByRole('group', { name: 'Share contact details with training providers' });
  const [contactGrant] = await Promise.all([smoke.waitApi('POST', consentApi), sharing.getByRole('button', { name: 'Grant Consent' }).click()]);
  const sharedContact = await result(contactGrant, 201);
  expect(sharedContact.consentTypeCode).toBe('PROVIDER_CONTACT_SHARING');
  const consented = await result(await roster(request, batchId, providerAgain), 200);
  expect(consented.entries[0].email).toBe(run.id + '-learner@example.invalid');
  expect(consented.entries[0].phone).toBe('+919900000314');
  const [contactRevoke] = await Promise.all([smoke.waitApi('PUT', consentApi + '/' + sharedContact.id + '/status'),
    sharing.getByRole('button', { name: 'Revoke Consent' }).click()]);
  await result(contactRevoke, 200);
  const withdrawn = (await result(await roster(request, batchId, providerAgain), 200)).entries[0];
  expect(withdrawn.email ?? null).toBeNull();
  expect(withdrawn.phone ?? null).toBeNull();
  await signOut(page);

  const sessionsPath = `${batchApi}/${batchId}/attendance/sessions`;
  const providerSession = await smoke.signIn('provider', '/provider/venue-bookings?booking=' + approved.booking.id);
  const providerHeaders = auth(providerSession.accessToken);
  await page.getByRole('main').getByRole('link', { name: 'Candidate enrollment' }).click();
  await expect(page.getByRole('combobox', { name: /Target Training Batch/ })).toHaveValue(String(batchId));
  await expect(page.getByRole('heading', { name: 'Session attendance' })).toHaveCount(0);

  const assisted = await result(await request.post('/api/v1/training-enrollments', {
    headers: providerHeaders, data: { traineeId: run.prospect, batchId, enrollmentDate: day(0) },
  }), 201);
  const scheduled = await result(await request.get(sessionsPath, { headers: providerHeaders }), 200);
  const [morning, afternoon] = scheduled.sessions.map(session => session.id);
  const saveAt = (sessionId, marks, headers = providerHeaders) =>
    request.put(`${sessionsPath}/${sessionId}`, { headers, data: { marks } });
  const firstMark = { enrollmentId: enrollment.id, status: 'PRESENT', remarks: 'Practical work completed.' };
  expect((await saveAt(morning, [firstMark])).status()).toBe(409);
  expect((await request.get(sessionsPath, { headers: await login(request, run, 'outsider') })).status()).toBe(404);
  expect((await request.get(sessionsPath, { headers: await login(request, run, 'trainee') })).status()).toBe(403);

  // Move both fixture sessions to the same date and simulate the second session crossing midnight.
  const priorDay = day(-1);
  sql(`UPDATE venue_booking_sessions SET starts_at='${priorDay}T09:00:00',ends_at='${priorDay}T11:00:00' WHERE session_id=${morning};
    UPDATE venue_booking_sessions SET starts_at='${priorDay}T14:00:00',ends_at='${day(0)}T02:00:00' WHERE session_id=${afternoon};
    UPDATE training_batches SET start_date='${priorDay}',end_date='${day(0)}' WHERE batch_id=${batchId};
    UPDATE training_enrollments SET enrollment_date='${priorDay}',start_date='${priorDay}',expected_completion_date='${day(0)}' WHERE batch_id=${batchId};`);
  await page.goto('/provider/batches');
  await page.getByRole('row').filter({ hasText: code }).getByRole('link', { name: 'Session attendance' }).click();
  await expect(page.getByRole('button', { name: 'Start training', exact: true })).toBeEnabled();
  await page.getByRole('button', { name: 'Start training', exact: true }).click();
  const table = page.getByRole('table', { name: 'Session trainee roster' });
  const traineeMark = table.getByRole('combobox', { name: /Attendance for Asha Smoke/ });
  const assistedMark = table.getByRole('combobox', { name: /Attendance for Neel Smoke/ });
  const remarks = table.getByRole('textbox', { name: /Remark for Asha Smoke/ });
  const saveButton = page.getByRole('button', { name: 'Save attendance' });
  await expect(traineeMark).toHaveValue('');
  await expect(assistedMark).toHaveValue('');
  await expect(traineeMark).toBeEnabled();

  const freshProvider = await login(request, run, 'provider');
  const foreignEnrollment = Number(sql(`SELECT enrollment_id FROM training_enrollments WHERE trainee_id=${run.trainee} AND batch_id<>${batchId} LIMIT 1;`));
  const invalid = await saveAt(morning, [firstMark, { enrollmentId: foreignEnrollment, status: 'ABSENT' }], freshProvider);
  expect(invalid.status()).toBe(400);
  expect((await invalid.json()).code).toBe('INVALID_ATTENDANCE_ENROLLMENT');
  expect(sql(`SELECT COUNT(*) FROM attendance_records WHERE batch_id=${batchId};`)).toBe('0');

  await traineeMark.selectOption('PRESENT');
  await remarks.fill(firstMark.remarks);
  await assistedMark.selectOption('ABSENT');
  const [savedMorning] = await Promise.all([smoke.waitApi('PUT', `${sessionsPath}/${morning}`), saveButton.click()]);
  expect((await result(savedMorning, 200)).rows).toEqual(expect.arrayContaining([
    expect.objectContaining(firstMark), expect.objectContaining({ enrollmentId: assisted.id, status: 'ABSENT' }),
  ]));
  await expect(page.getByText('Attendance saved.', { exact: true })).toBeVisible();
  const stale = await saveAt(morning, [{ ...firstMark, status: 'ABSENT' }], freshProvider);
  expect(stale.status()).toBe(409);
  expect((await stale.json()).code).toBe('ATTENDANCE_CHANGED');

  await page.reload();
  await expect(traineeMark).toHaveValue('PRESENT');
  await expect(remarks).toHaveValue(firstMark.remarks);
  await remarks.fill('Completed the morning practical.');
  const [corrected] = await Promise.all([smoke.waitApi('PUT', `${sessionsPath}/${morning}`), saveButton.click()]);
  expect((await result(corrected, 200)).rows).toEqual(expect.arrayContaining([
    expect.objectContaining({ ...firstMark, remarks: 'Completed the morning practical.' }),
  ]));

  await page.getByLabel('Scheduled session (IST)', { exact: true }).selectOption(String(afternoon));
  await expect(traineeMark).toHaveValue('');
  await traineeMark.selectOption('ABSENT');
  await remarks.fill('Missed the second session.');
  const [savedAfternoon] = await Promise.all([smoke.waitApi('PUT', `${sessionsPath}/${afternoon}`), saveButton.click()]);
  const afternoonRows = (await result(savedAfternoon, 200)).rows;
  expect(afternoonRows.find(row => row.enrollmentId === enrollment.id).status).toBe('ABSENT');
  expect(afternoonRows.find(row => row.enrollmentId === assisted.id).status ?? null).toBeNull();
  expect(sql(`SELECT session_sequence,marked_by_user_id FROM attendance_records WHERE enrollment_id=${enrollment.id}
    ORDER BY session_sequence;`)).toBe(`1\t${providerSession.userId}\n2\t${providerSession.userId}`);
  await signOut(page);

  const traineeSession = await smoke.signIn('trainee', '/trainee/training');
  const records = await result(await request.get('/api/v1/attendance-records', { headers: auth(traineeSession.accessToken),
    params: { traineeId: run.trainee },
  }), 200);
  expect(records).toEqual(expect.arrayContaining([
    expect.objectContaining({ batchId, sessionSequence: 1, attendanceStatusCode: 'PRESENT', scheduledStart: priorDay + 'T09:00:00' }),
    expect.objectContaining({ batchId, sessionSequence: 2, attendanceStatusCode: 'ABSENT', scheduledEnd: day(0) + 'T02:00:00' }),
  ]));
  const traineeRows = page.getByRole('region', { name: 'Session-by-Session Attendance Log' }).getByRole('row').filter({ hasText: code });
  await expect(traineeRows).toHaveCount(2);
  await expect(traineeRows.filter({ hasText: '09:00' })).toContainText('Completed the morning practical.');
  await expect(traineeRows.filter({ hasText: '14:00' })).toContainText('ABSENT');
  await expect(traineeRows.filter({ hasText: '14:00' })).toContainText('02:00 IST');
  await signOut(page);

  sql(`UPDATE trainees SET user_id=(SELECT user_id FROM users WHERE username=${quote(run.users.trainee2)})
    WHERE trainee_id=${run.prospect};`);
  await smoke.signIn('trainee2', '/trainee/training');
  await expect(page.getByRole('region', { name: 'Enrolled Training Batches & Schemes' })).toContainText(assisted.enrollmentNumber);
  await expect(traineeRows).toHaveCount(1);
  await expect(traineeRows).toContainText('09:00');
  await expect(traineeRows).toContainText('ABSENT');
  await signOut(page);

  const assessmentPath = `${batchApi}/${batchId}/assessment`;
  const opening = { brief: 'Demonstrate safe wiring and explain the isolation procedure.' };
  const schedule = { startsAt: `${day(1)}T10:00`, venue: 'Assessment workshop' };
  const appointments = '/api/v1/trainees/me/batch-assessments';
  const completingProvider = await login(request, run, 'provider');
  await result(await request.post(`${venueApi}/${approved.booking.id}/close`, { headers: completingProvider, data: { action: 'COMPLETE' } }), 200);
  expect(await result(await request.get(assessmentPath, { headers: completingProvider }), 200))
    .toMatchObject({ batchStatus: 'COMPLETED', canPublish: false, outstandingMarks: 1 });
  await result(await request.post(assessmentPath, { headers: completingProvider, data: opening }), 409);
  await result(await request.post(assessmentPath, { headers: await login(request, run, 'trainee2'), data: opening }), 403);
  const providerUI = await smoke.signIn('provider', '/provider/batches');
  let providerAccess = auth(providerUI.accessToken);
  await page.goto(`/provider/enrollment?batch=${batchId}`);
  const batchSelector = page.getByRole('combobox', { name: /Target Training Batch/ });
  await expect(page.getByText('The linked batch is closed for enrollment.')).toBeVisible();
  await expect(batchSelector).toHaveValue('');
  await expect(batchSelector.getByRole('option', { name: new RegExp(code) })).toHaveCount(0);
  await expect(batchSelector.getByRole('option', { name: /GOV-REQ-/ })).toHaveCount(1);
  await page.getByPlaceholder('e.g. TRN-2026-0001').fill(run.candidateCode);
  await page.getByRole('button', { name: 'Find Trainee' }).click();
  await expect(page.getByText('VERIFIED PROFILE')).toBeVisible();
  await expect(page.getByRole('button', { name: 'Confirm Candidate Enrollment' })).toBeDisabled();
  await page.goto('/provider/batches');
  await page.getByRole('row').filter({ hasText: code }).getByRole('link', { name: 'Batch assessment' }).click();
  const openButton = page.getByRole('button', { name: 'Open for assessment' });
  await expect(openButton).toBeDisabled();
  await expect(page.getByText('Mark every eligible trainee Present or Absent for each training session.')).toBeVisible();
  const lateStatus = Number(sql("SELECT attendance_status_id FROM ref_attendance_status WHERE status_code='LATE';"));
  const legacyMark = await result(await request.post('/api/v1/attendance-records', { headers: providerAccess, data: {
    enrollmentId: assisted.id, sessionDate: priorDay, sessionSequence: 2, attendanceStatusId: lateStatus,
  } }), 201);
  expect(await result(await request.get(assessmentPath, { headers: providerAccess }), 200))
    .toMatchObject({ canPublish: false, outstandingMarks: 1 });
  const currentAfternoon = (await result(await request.get(`${sessionsPath}/${afternoon}`, { headers: providerAccess }), 200)).rows;
  await result(await saveAt(afternoon, currentAfternoon.map(row => ({
    enrollmentId: row.enrollmentId, status: 'PRESENT', expectedVersion: row.version,
  })), providerAccess), 200);
  await page.reload();
  await expect(page.locator('input[type="datetime-local"]')).toHaveCount(0);
  await expect(page.getByLabel('Assessment venue')).toHaveCount(0);
  await page.getByLabel('Assessment instructions').fill(opening.brief);
  const attendanceTab = await page.context().newPage();
  await attendanceTab.goto(`/provider/batches/${batchId}/attendance`);
  await attendanceTab.locator('#usernameOrEmail').fill(run.users.provider);
  await attendanceTab.locator('#password').fill(run.password);
  await attendanceTab.getByRole('button', { name: 'Sign In', exact: true }).click();
  const tabRemark = attendanceTab.getByRole('textbox', { name: /Remark for Asha Smoke/ });
  await expect(tabRemark).toHaveValue('Completed the morning practical.');
  await tabRemark.fill('Unsaved correction in another tab.');
  await expect(attendanceTab.getByRole('button', { name: 'Save attendance' })).toBeEnabled();
  await page.bringToFront();
  const [published] = await Promise.all([smoke.waitApi('POST', assessmentPath), openButton.click()]);
  expect(published.request().postDataJSON()).toEqual(opening);
  const assessment = await result(published, 200);
  expect(assessment).toMatchObject({ status: 'OPEN', candidates: 2, checkedIn: false });
  expect(assessment.startsAt ?? null).toBeNull();
  expect(assessment.venue ?? null).toBeNull();
  await expect(page.getByRole('heading', { name: 'Open to assessors' })).toBeVisible();
  await expect(page.getByText('Assessment date and venue pending')).toBeVisible();
  await attendanceTab.bringToFront();
  await expect(tabRemark).toBeDisabled({ timeout: 18_000 });
  await expect(tabRemark).toHaveValue('Completed the morning practical.');
  await attendanceTab.close();
  providerAccess = await login(request, run, 'provider');
  const openedTimetable = await result(await request.get(sessionsPath, { headers: providerAccess }), 200);
  expect(openedTimetable.attendanceLocked).toBe(true);
  expect(openedTimetable.sessions.map(session => session.canMark)).toEqual([false, false]);
  await page.getByRole('link', { name: 'View locked training attendance' }).click();
  await expect(page.getByText('Attendance is read-only because this batch is open for assessment.')).toBeVisible();
  await expect(traineeMark).toBeDisabled();
  await expect(remarks).toBeDisabled();
  await expect(traineeMark).toHaveValue('PRESENT');
  await expect(remarks).toHaveValue('Completed the morning practical.');
  await expect(saveButton).toHaveCount(0);
  expect(sql(`SELECT a.assessment_date IS NULL, SUM(ta.assessment_date IS NULL), pub.starts_at IS NULL
    FROM assessments a JOIN batch_assessment_publications pub USING (assessment_id)
    JOIN trainee_assessments ta USING (assessment_id) WHERE a.assessment_id=${assessment.assessmentId}
    GROUP BY a.assessment_date,pub.starts_at;`)).toBe('1\t2\t1');
  const lockedSession = await saveAt(afternoon, [{ enrollmentId: assisted.id, status: 'ABSENT' }], providerAccess);
  expect(lockedSession.status()).toBe(409);
  expect((await lockedSession.json()).code).toBe('ASSESSMENT_ATTENDANCE_LOCKED');
  await result(await request.put(`/api/v1/attendance-records/${legacyMark.id}`, {
    headers: providerAccess, data: { attendanceStatusId: lateStatus },
  }), 409);
  for (const response of [
    await request.delete(`/api/v1/attendance-records/${legacyMark.id}`, { headers: providerAccess }),
    await request.put(`/api/v1/training-enrollments/${enrollment.id}`, {
      headers: providerAccess, data: { actualCompletionDate: priorDay },
    }),
    await request.delete(`/api/v1/training-enrollments/${enrollment.id}`, { headers: providerAccess }),
  ]) {
    expect(response.status()).toBe(409);
    expect((await response.json()).code).toBe('ASSESSMENT_ATTENDANCE_LOCKED');
  }
  const assessorPath = `/api/v1/assessor/assessments/${assessment.assessmentId}`;
  const claimPath = `/api/v1/assessor/open-batch-assessments/${assessment.assessmentId}/claim`;
  const participants = [{ traineeId: run.trainee, appeared: true }, { traineeId: run.prospect, appeared: true }];
  const scores = [
    { ...participants[0], enrollmentId: enrollment.id, theoryScore: 25, practicalScore: 42, vivaScore: 17 },
    { ...participants[1], enrollmentId: assisted.id, theoryScore: 10, practicalScore: 15, vivaScore: 4 },
  ];
  const assessor2 = await login(request, run, 'assessor2');
  await result(await request.post(claimPath, { headers: providerAccess, data: schedule }), 403);
  await signOut(page);
  const assessorUI = await smoke.signIn('assessor', '/assessor/dashboard');
  let assessorAccess = auth(assessorUI.accessToken);
  const card = page.getByRole('article').filter({ hasText: code });
  await expect(card).toContainText(opening.brief);
  const [reviewed] = await Promise.all([smoke.waitApi('GET', assessorPath + '/published-batch'),
    card.getByRole('button', { name: 'Review training attendance' }).click()]);
  const lockedReview = await result(reviewed, 200);
  expect(lockedReview.candidates).toEqual(expect.arrayContaining([
    expect.objectContaining({ traineeId: run.trainee, trainingSessions: 2, present: 2,
      sessionMarks: [expect.objectContaining({ status: 'PRESENT', remarks: 'Completed the morning practical.' }),
        expect.objectContaining({ status: 'PRESENT' })] }),
    expect.objectContaining({ traineeId: run.prospect, trainingSessions: 2, present: 1,
      sessionMarks: [expect.objectContaining({ status: 'ABSENT' }), expect.objectContaining({ status: 'PRESENT' })] }),
  ]));
  const attendancePreview = page.getByRole('dialog', { name: 'Locked training attendance' });
  const traineeAttendance = attendancePreview.getByRole('listitem').filter({ hasText: 'Asha Smoke' });
  await traineeAttendance.getByText('2 / 2 sessions present', { exact: true }).click();
  await expect(traineeAttendance.getByText('Completed the morning practical.', { exact: false })).toBeVisible();
  await attendancePreview.getByText('1 / 2 sessions present', { exact: true }).click();
  await expect(attendancePreview.getByText('Absent', { exact: true })).toBeVisible();
  await page.keyboard.press('Escape');
  const invalidSchedule = await request.post(claimPath, { headers: assessorAccess, data: { ...schedule, startsAt: `${priorDay}T09:00` } });
  expect(invalidSchedule.status()).toBe(400);
  expect((await invalidSchedule.json()).code).toBe('ASSESSMENT_TIME_IN_PAST');
  await card.getByLabel('Assessment date and time (IST)').fill(schedule.startsAt);
  await card.getByLabel('Assessment venue').fill(schedule.venue);
  const [claimed] = await Promise.all([smoke.waitApi('POST', claimPath), card.getByRole('button', { name: 'Claim assessment' }).click()]);
  const assignment = await result(claimed, 200);
  expect(assignment.status).toBe('CLAIMED');
  expect(assignment.startsAt.slice(0, 16)).toBe(schedule.startsAt);
  expect(assignment.venue).toBe(schedule.venue);
  const scoringAttendance = page.getByRole('row').filter({ hasText: 'Neel Smoke' });
  await scoringAttendance.getByText('1 / 2 sessions present', { exact: true }).click();
  await expect(scoringAttendance.getByText('Absent', { exact: true })).toBeVisible();
  await expect(page.getByRole('button', { name: 'Mark assessment completed' })).toBeDisabled();
  await expect(page.getByText('Candidate skills and job goals')).toHaveCount(0);
  await result(await request.put(`/api/v1/career-skills/assessments/${assessment.assessmentId}/trainees/${run.trainee}`, {
    headers: assessorAccess, data: { skillId: 1, levelId: 1 },
  }), 409);
  const assignedRoster = await result(await request.get(assessorPath + '/roster', { headers: assessorAccess }), 200);
  const attemptId = assignedRoster.candidates[0].traineeAssessmentId;
  await result(await request.put(`/api/v1/trainee-assessments/${attemptId}`, {
    headers: assessorAccess, data: { appearedFlag: true },
  }), 403);
  await result(await request.delete(`/api/v1/assessments/${assessment.assessmentId}`, { headers: assessorAccess }), 409);
  await result(await request.post(claimPath, { headers: assessor2, data: schedule }), 409);
  await result(await request.get(assessorPath + '/roster', { headers: assessor2 }), 403);
  await result(await request.get(assessorPath + '/published-batch', { headers: assessor2 }), 403);
  await result(await request.post(assessorPath + '/complete', { headers: assessorAccess }), 409);
  await result(await request.post(assessorPath + '/draft', { headers: assessorAccess, data: { candidates: scores } }), 409);
  const appointment = (await result(await request.get(appointments, { headers: await login(request, run, 'trainee2') }), 200))
    .find(item => item.batchCode === code);
  expect(appointment.startsAt.slice(0, 16)).toBe(schedule.startsAt);
  expect(appointment.venue).toBe(schedule.venue);
  expect(sql(`SELECT COUNT(*) FROM trainee_assessments ta JOIN assessments a USING (assessment_id)
    WHERE a.assessment_id=${assessment.assessmentId} AND ta.assessment_date=a.assessment_date AND a.assessment_date='${day(1)}';`)).toBe('2');
  sql(`UPDATE batch_assessment_publications pub JOIN assessments a USING (assessment_id)
    JOIN trainee_assessments ta USING (assessment_id)
    SET pub.starts_at='${priorDay}T19:30:00',a.assessment_date='${priorDay}',ta.assessment_date='${priorDay}'
    WHERE pub.assessment_id=${assessment.assessmentId};`);
  await signOut(page);
  assessorAccess = auth((await smoke.signIn('assessor', `/assessor/assessments/${assessment.assessmentId}`)).accessToken);
  await expect(page.getByRole('button', { name: /^(Present|Absent)$/ })).toHaveCount(0);
  await expect(page.getByRole('button', { name: 'Save Draft' })).toBeDisabled();
  await expect(page.getByRole('button', { name: 'Publish batch results and certificates' })).toBeDisabled();
  const earlyDraft = await request.post(assessorPath + '/draft', { headers: assessorAccess, data: { candidates: scores } });
  expect(earlyDraft.status()).toBe(409);
  expect((await earlyDraft.json()).code).toBe('ASSESSMENT_COMPLETION_REQUIRED');
  await result(await request.post(assessorPath + '/complete', { headers: await login(request, run, 'provider') }), 403);
  await result(await request.post(assessorPath + '/complete', { headers: await login(request, run, 'assessor2') }), 403);
  const [completed] = await Promise.all([smoke.waitApi('POST', assessorPath + '/complete'),
    page.getByRole('button', { name: 'Mark assessment completed' }).click()]);
  const recorded = await result(completed, 200);
  expect(recorded).toMatchObject({ canComplete: false, completedAt: expect.any(String) });
  await expect(page.getByRole('button', { name: 'Save Draft' })).toBeEnabled();
  await result(await request.post(assessorPath + '/draft', { headers: assessorAccess,
    data: { candidates: [{ ...scores[0], appeared: false }] } }), 409);
  await result(await request.post(assessorPath + '/finalize', { headers: assessorAccess, data: { candidates: scores.slice(0, 1) } }), 400);
  await result(await request.post(assessorPath + '/draft', { headers: assessorAccess, data: { candidates: scores } }), 200);
  expect(sql(`SELECT COUNT(*) FROM trainee_skill_evidence WHERE assessment_id=${assessment.assessmentId};`)).toBe('0');
  const draft = (await result(await request.get(appointments, { headers: await login(request, run, 'trainee2') }), 200)).find(item => item.batchCode === code);
  expect(draft).toBeDefined();
  expect(draft.resultId ?? null).toBeNull();
  await page.reload();
  await page.getByRole('button', { name: 'Publish batch results and certificates' }).click();
  const [finalized] = await Promise.all([smoke.waitApi('POST', assessorPath + '/finalize'),
    page.getByRole('button', { name: 'Confirm & Finalize' }).click()]);
  expect(await result(finalized, 200)).toMatchObject({ isFinalized: true, passedCount: 1, failedCount: 1, certificatesIssued: 1 });
  expect(sql(`SELECT s.skill_name,l.level_code FROM trainee_skill_evidence e JOIN skills s USING (skill_id)
    JOIN skill_levels l USING (skill_level_id) WHERE e.assessment_id=${assessment.assessmentId} AND e.trainee_id=${run.trainee};`))
    .toBe(skills + '\tADVANCED');
  expect(sql(`SELECT COUNT(*) FROM trainee_skill_evidence WHERE assessment_id=${assessment.assessmentId} AND trainee_id=${run.prospect};`)).toBe('0');
  const providerReview = await result(await request.get(assessmentPath + '/roster', { headers: await login(request, run, 'provider') }), 200);
  expect(providerReview.candidates).toEqual(expect.arrayContaining([
    expect.objectContaining({ traineeId: run.trainee, outcome: 'PASS', total: 84, certificateId: expect.any(Number) }),
    expect.objectContaining({ traineeId: run.prospect, outcome: 'FAIL', total: 29, certificateId: null }),
  ]));
  const pass = providerReview.candidates.find(candidate => candidate.traineeId === run.trainee);
  expect(sql(`SELECT issuing_body FROM certifications WHERE certification_id=${pass.certificateId};`))
    .toBe(run.users.assessor);
  // Model a passing result that was finalized before its certificate could be issued.
  sql(`DELETE FROM certifications WHERE certification_id=${pass.certificateId} AND enrollment_id=${enrollment.id};`);
  await page.reload();
  const pending = page.getByRole('row').filter({ hasText: 'Asha Smoke' });
  await expect(pending.getByText('Pending Issuance', { exact: true })).toBeVisible();
  await expect(pending.getByTitle('Download PDF Certificate')).toHaveCount(0);
  const resultFile = page.waitForEvent('download');
  await pending.getByRole('button', { name: 'Download result PDF', exact: true }).click();
  expect((await resultFile).suggestedFilename()).toBe(`Assessment_Result_${pass.resultId}.pdf`);
  await signOut(page);
  await smoke.signIn('trainee', '/trainee/assessments');
  const pendingTrainee = page.getByRole('article').filter({ hasText: code });
  await expect(pendingTrainee.getByText(/Certificate pending issuance/)).toBeVisible();
  await expect(pendingTrainee.getByRole('button', { name: 'Download certificate PDF', exact: true })).toHaveCount(0);
  const pendingFile = page.waitForEvent('download');
  await pendingTrainee.getByRole('button', { name: 'Download result PDF', exact: true }).click();
  expect((await pendingFile).suggestedFilename()).toBe(`Assessment_Result_${pass.resultId}.pdf`);
  await signOut(page);
  await smoke.signIn('assessor', `/assessor/assessments/${assessment.assessmentId}`);
  const [issued] = await Promise.all([smoke.waitApi('POST', assessorPath + '/issue-pending'),
    page.getByRole('button', { name: 'Issue Pending Certificates' }).click()]);
  const issuance = await result(issued, 200);
  expect(issuance).toMatchObject({ isFinalized: true, certificatesIssued: 1 });
  await expect(page.getByText('1 certificate issued.', { exact: true })).toBeVisible();
  await expect(pending.getByText('Pending Issuance', { exact: true })).toHaveCount(0);
  await expect(pending.getByTitle('Download PDF Certificate')).toBeVisible();
  const certificateId = issuance.results.find(candidate => candidate.traineeId === run.trainee).certificateId;
  expect(certificateId).toBeGreaterThan(0);
  expect(sql(`SELECT expiry_date IS NULL,v.status_code FROM certifications JOIN ref_certificate_verification_status v
    USING (certificate_verification_status_id) WHERE certification_id=${certificateId};`)).toBe('1\tUNVERIFIED');
  const assessorFile = page.waitForEvent('download');
  await pending.getByTitle('Download PDF Certificate').click();
  const assessorPdf = await assessorFile;
  expect(assessorPdf.suggestedFilename()).toBe(`Certificate_${certificateId}.pdf`);
  expect((await readFile(await assessorPdf.path())).toString('latin1')).toContain('/Title (Certificate of Competency');
  const checkPdf = async (role, label, title) => {
    await signOut(page);
    const account = await smoke.signIn(role, '/trainee/assessments');
    const profile = await result(await request.get('/api/v1/career-skills/me', { headers: auth(account.accessToken) }), 200);
    const awarded = profile.evidence.find(item => item.skill === skills);
    if (title === 'PASS') expect(awarded).toMatchObject({ level: 'Advanced' });
    else expect(awarded).toBeUndefined();
    const own = page.getByRole('article').filter({ hasText: code });
    await expect(own).toContainText(title === 'FAIL' ? 'Total 29' : 'Total 84');
    const file = page.waitForEvent('download');
    await own.getByRole('button', { name: label, exact: true }).click();
    const document = await file;
    if (title === 'PASS') expect(document.suggestedFilename()).toBe(`Certificate_${pass.resultId}.pdf`);
    const bytes = (await readFile(await document.path())).toString('latin1');
    expect(bytes).toContain('/MediaBox [0 0 842 595]');
    expect(bytes).toContain(title === 'FAIL' ? '/Title (Assessment Result - FAIL)' : '/Title (Certificate of Competency');
  };
  await checkPdf('trainee2', 'Download result PDF', 'FAIL');
  await checkPdf('trainee', 'Download certificate PDF', 'PASS');
  sql(`UPDATE certifications SET issuing_body=NULL WHERE certification_id=${certificateId} AND trainee_id=${run.trainee};`);
  const historical = await request.get(`/api/v1/assessment-results/${pass.resultId}/document`, {
    headers: await login(request, run, 'trainee'),
  });
  expect(historical.status()).toBe(200);
  expect(historical.headers()['content-disposition']).toContain(`Certificate_${pass.resultId}.pdf`);
  expect((await historical.body()).toString('latin1')).toContain('/Title (Certificate of Competency');
  const denied = await request.get(`/api/v1/assessment-results/${pass.resultId}/document`, { headers: await login(request, run, 'trainee2') });
  expect(denied.status()).toBe(403);
});
