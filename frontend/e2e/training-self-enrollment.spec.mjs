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

test('trainees find and register for approved batches, providers see their roster and record attendance', async ({ page, request, smoke }) => {
  test.setTimeout(90_000);
  const { run } = smoke;
  const districtId = Number(sql(`SELECT district_id FROM districts WHERE state_id=${run.state}
    AND lgd_code IS NOT NULL AND lifecycle_status_id=1 AND deleted_at IS NULL ORDER BY district_id LIMIT 1;`));
  expect(districtId).toBeGreaterThan(0);
  const code = run.id + '-venue-self';
  const venueName = 'Solar practical workshop';
  const address = '14 Workshop Road, Training Block';
  const skills = run.id + ' Solar panel installation and electrical wiring';
  const sessions = [slot(6, '10:15', '12:00'), slot(7, '13:30', '15:00')];
  const provider = await login(request, run, 'provider');
  const admin = await login(request, run, 'admin');
  const temporary = await result(await request.post(venueApi, { headers: provider, data: {
    batchCode: code, seats: 2, skillsToTeach: skills, sessions,
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
  } }), 201);
  const offer = await result(await request.post(requirementApi + '/' + government.id + '/applications', { headers: provider,
    data: { startDate: day(10), endDate: day(11), dailyStartTime: '10:00', dailyEndTime: '12:00' },
  }), 201);
  const award = await result(await request.post(requirementApi + '/government/' + government.id + '/select', {
    headers: admin, data: { applicationId: offer.id },
  }), 200);
  const governmentBooking = await result(await request.get(venueApi + '/' + award.bookingId, { headers: admin }), 200);
  const governmentBatch = governmentBooking.booking.batchId;

  sql(`UPDATE trainees SET user_id=(SELECT user_id FROM users WHERE username=${quote(run.users.trainee2)})
    WHERE trainee_id=${run.prospect};`);
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

  const currentCore = sql(`SELECT trainee_consent_id FROM trainee_consents WHERE trainee_id=${run.trainee}
    AND consent_type_id=1 ORDER BY trainee_consent_id DESC LIMIT 1;`);
  const revoked = await request.put(consentApi + '/' + currentCore + '/status', { headers: trainee, data: { consentStatusId: 2 } });
  expect(revoked.status()).toBe(200);
  const noConsent = await register(request, batchId, trainee);
  expect(noConsent.status()).toBe(400);
  expect((await noConsent.json()).code).toBe('CONSENT_REVOKED');
  expect(sql(`SELECT COUNT(*) FROM training_enrollments WHERE batch_id=${batchId};`)).toBe('0');

  await smoke.signIn('trainee', find);
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
    UPDATE training_enrollments SET enrollment_date='${priorDay}' WHERE batch_id=${batchId};`);
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
  sql(`UPDATE trainees SET user_id=NULL WHERE trainee_id=${run.prospect};`);
});
