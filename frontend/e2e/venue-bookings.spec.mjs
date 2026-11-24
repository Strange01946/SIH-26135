import { expect, test } from './support/browser.mjs';
import { quote, sql } from './support/database.mjs';

const api = '/api/v1/venue-bookings';
const batches = '/api/v1/training-batches';
const providerPage = '/provider/venue-bookings';
const adminPage = '/admin/venue-bookings';

const localDay = (offset) => new Intl.DateTimeFormat('sv-SE', { timeZone: 'Asia/Kolkata' })
  .format(new Date(Date.now() + offset * 86_400_000));
const session = (offset, start = '09:00', end = '11:00') => ({
  startsAt: localDay(offset) + 'T' + start, endsAt: localDay(offset) + 'T' + end,
});

async function userHeaders(request, smoke, role) {
  const response = await request.post('/api/v1/auth/login', {
    data: { usernameOrEmail: smoke.run.users[role], password: smoke.run.password },
  });
  expect(response.status()).toBe(200);
  return { Authorization: 'Bearer ' + (await response.json()).data.accessToken };
}

async function signOut(page) {
  await page.getByRole('button', { name: 'Sign Out', exact: true }).click();
  await expect(page.getByRole('heading', { name: 'Sign in', exact: true })).toBeVisible();
}

async function action(page, smoke, method, path, button, expected) {
  const [response] = await Promise.all([smoke.waitApi(method, path),
    page.getByRole('dialog').getByRole('button', { name: button, exact: true }).click()]);
  expect(response.status()).toBe(expected);
  return (await response.json()).data;
}

async function openBooking(page, code) {
  await page.getByRole('row').filter({ hasText: code })
    .getByRole('button', { name: 'View booking', exact: true }).click();
}

async function fillSessions(dialog, sessions) {
  for (const [index, slot] of sessions.entries()) {
    if (index > 0) await dialog.getByRole('button', { name: 'Add session', exact: true }).click();
    await dialog.getByLabel(`Session ${index + 1} start *`, { exact: true }).fill(slot.startsAt);
    await dialog.getByLabel(`Session ${index + 1} end *`, { exact: true }).fill(slot.endsAt);
  }
}

async function fillBatch(dialog, run, code) {
  await dialog.getByLabel('Batch code *', { exact: true }).fill(code);
  await dialog.getByLabel('Requested seats *', { exact: true }).fill('12');
  await dialog.getByLabel('Skills to teach *', { exact: true }).fill(run.id + ' Fault identification and safe equipment handling');
  await expect(dialog.getByLabel('Course *', { exact: true })).toHaveCount(0);
  await expect(dialog.getByLabel('Program *', { exact: true })).toHaveCount(0);
}

test('district approval, rescheduling and cancellation reuse one shared center and preserve the batch', async ({ page, request, smoke }) => {
  const { run } = smoke;
  const centerName = 'Shared Skills Lab ' + run.id;
  const centerCode = run.id + '-shared';
  const code = run.id + '-venue-shared';
  const original = [session(7), session(8)];
  const postponed = [session(10, '14:00', '16:00'), session(11, '14:00', '16:00')];

  await smoke.signIn('district', adminPage);
  await expect(page.getByRole('link', { name: 'Venue Bookings', exact: true })).toHaveAttribute('href', adminPage);
  await page.getByRole('button', { name: 'Shared centers', exact: true }).click();
  await page.getByRole('button', { name: 'Register shared center', exact: true }).click();
  const centerDialog = page.getByRole('dialog', { name: 'Register shared center' });
  await centerDialog.getByLabel('Center code *').fill(centerCode);
  await centerDialog.getByLabel('Center name *').fill(centerName);
  await centerDialog.getByLabel('Address *', { exact: true }).fill('Fixture shared lab');
  await centerDialog.getByLabel('Seating capacity *').fill('24');
  await expect(centerDialog.getByLabel(/District/)).toHaveValue(String(run.district));
  const pincode = centerDialog.getByLabel(/PIN code/);
  await centerDialog.getByRole('button', { name: 'Save shared center' }).click();
  await expect(pincode).toBeFocused();
  await pincode.fill('110001');
  const venue = await action(page, smoke, 'POST', api + '/venues', 'Save shared center', 201);
  expect(sql(`SELECT provider_id IS NULL,venue_type,pincode FROM training_centers WHERE center_id=${venue.id};`))
    .toBe('1\tSHARED\t110001');

  await page.getByRole('button', { name: 'View availability for ' + centerName }).click();
  const closure = session(6);
  await page.getByLabel('Closure start').fill(closure.startsAt);
  await page.getByLabel('Closure end').fill(closure.endsAt);
  await page.getByLabel('Closure reason').fill('Scheduled lab maintenance');
  await page.getByRole('button', { name: 'Add closure', exact: true }).click();
  await expect(page.getByText('Closure added.')).toBeVisible();
  await signOut(page);

  const provider = await smoke.signIn('provider', providerPage);
  const providerHeaders = { Authorization: 'Bearer ' + provider.accessToken };
  await page.getByRole('button', { name: 'Shared centers', exact: true }).click();
  await page.getByRole('button', { name: 'View availability for ' + centerName }).click();
  await expect(page.getByText(/Unavailable$/)).toBeVisible();
  await expect(page.getByText(/Scheduled lab maintenance/)).toHaveCount(0);
  const blocked = await request.post(api, { headers: providerHeaders, data: {
    centerId: venue.id, batchCode: code, seats: 12, sessions: [closure],
    skillsToTeach: 'Fault identification',
  } });
  expect(blocked.status()).toBe(409);
  expect((await blocked.json()).code).toBe('VENUE_SLOT_UNAVAILABLE');
  await page.getByRole('button', { name: 'Request this center' }).click();
  const bookingDialog = page.getByRole('dialog', { name: 'Request shared center' });
  await fillBatch(bookingDialog, run, code);
  await fillSessions(bookingDialog, original);
  const requested = await action(page, smoke, 'POST', api, 'Submit request', 201);
  expect(requested.booking).toMatchObject({ providerId: run.provider, centerId: venue.id, status: 'PENDING', batchId: null });
  expect(requested.booking).toMatchObject({
    requirementId: null, courseId: null, programId: null,
    skillsToTeach: run.id + ' Fault identification and safe equipment handling',
  });
  expect(sql(`SELECT COUNT(*) FROM training_batches WHERE batch_code=${quote(code)};`)).toBe('0');
  const id = requested.booking.id;
  const otherHeaders = await userHeaders(request, smoke, 'outsider');
  expect((await request.get(api + '/' + id, { headers: otherHeaders })).status()).toBe(403);
  expect((await request.post(api + '/' + id + '/review', { headers: otherHeaders, data: { approved: true } })).status()).toBe(403);
  await signOut(page);

  await smoke.signIn('district', adminPage);
  await openBooking(page, code);
  await page.getByRole('button', { name: 'Approve booking', exact: true }).click();
  const approved = await action(page, smoke, 'POST', api + '/' + id + '/review', 'Confirm approval', 200);
  const batchId = approved.booking.batchId;
  expect(approved.booking).toMatchObject({ status: 'APPROVED', batchStatus: 'PLANNED' });
  expect(approved.sessions.every(slot => slot.reservedAt && !slot.releasedAt)).toBe(true);
  expect(sql(`SELECT provider_id,center_id FROM training_batches WHERE batch_id=${batchId};`)).toBe(run.provider + '\t' + venue.id);
  await signOut(page);

  await smoke.signIn('provider', providerPage);
  await openBooking(page, code);
  await page.getByRole('button', { name: 'Open enrollment' }).click();
  await expect(page.getByText('Batch open', { exact: true })).toBeVisible();
  await page.getByRole('button', { name: 'Postpone training' }).click();
  await fillSessions(page.getByRole('dialog', { name: 'Postpone training' }), postponed);
  const proposal = await action(page, smoke, 'POST', api + '/' + id + '/reschedule', 'Submit request', 201);
  expect(proposal.booking).toMatchObject({ batchId, replacesBookingId: id, status: 'PENDING' });
  expect(sql(`SELECT COUNT(*) FROM venue_booking_sessions WHERE booking_id=${id} AND released_at IS NULL;`)).toBe('2');
  const reviewer = await userHeaders(request, smoke, 'stateadmin');
  const revision = await request.post(api + '/' + proposal.booking.id + '/review', { headers: reviewer, data: { approved: true } });
  expect(revision.status()).toBe(200);
  expect((await revision.json()).data.booking).toMatchObject({ status: 'APPROVED', batchId });
  expect(sql(`SELECT booking_status,
      (SELECT COUNT(*) FROM venue_booking_sessions WHERE booking_id=${id} AND released_at IS NOT NULL)
    FROM venue_bookings WHERE booking_id=${id};`)).toBe('SUPERSEDED\t2');
  await page.reload();
  await page.getByRole('row').filter({ hasText: code }).filter({ hasText: 'Postponement' })
    .getByRole('button', { name: 'View booking' }).click();
  await page.getByRole('button', { name: 'Cancel booking', exact: true }).click();
  const cancelled = await action(page, smoke, 'POST', api + '/' + proposal.booking.id + '/close', 'Confirm cancellation', 200);
  expect(cancelled.booking).toMatchObject({ status: 'CANCELLED', batchId, batchStatus: 'CANCELLED' });
  expect(cancelled.sessions.every(slot => slot.releasedAt)).toBe(true);
});

test('concurrent approvals reserve a shared slot exactly once and let the next provider use a released slot', async ({ request, smoke }) => {
  const { run } = smoke;
  const admin = await userHeaders(request, smoke, 'district');
  const providerA = await userHeaders(request, smoke, 'provider');
  const providerB = await userHeaders(request, smoke, 'outsider');
  const venueData = {
    code: run.id + '-shared-race', name: 'Race test lab', address: 'Fixture race lab',
    stateId: run.state, districtId: run.district, capacity: 20,
  };
  const invalid = await request.post(api + '/venues', { headers: admin, data: venueData });
  expect(invalid.status()).toBe(400);
  expect(await invalid.json()).toMatchObject({ code: 'VALIDATION_ERROR' });
  const created = await request.post(api + '/venues', { headers: admin, data: { ...venueData, pincode: '110001' } });
  expect(created.status()).toBe(201);
  const centerId = (await created.json()).data.id;
  const slot = session(20);
  const reqPayload = { centerId, seats: 12, sessions: [slot], skillsToTeach: run.id + ' Lab safety' };
  const missingSkills = await request.post(api, { headers: providerA, data: {
    ...reqPayload, batchCode: run.id + '-venue-skills', skillsToTeach: ' ',
  } });
  expect(missingSkills.status()).toBe(400);
  expect((await missingSkills.json()).code).toBe('TRAINING_SKILLS_REQUIRED');
  const tooLarge = await request.post(api, { headers: providerA, data: {
    ...reqPayload, batchCode: run.id + '-venue-size', seats: 21,
  } });
  expect(tooLarge.status()).toBe(400);
  expect((await tooLarge.json()).code).toBe('VENUE_CAPACITY_EXCEEDED');
  const direct = await request.post(batches, { headers: providerA, data: {
    providerId: run.provider, centerId, courseId: run.course, programId: run.program,
    batchCode: run.id + '-venue-direct', capacity: 12, startDate: localDay(20), batchStatusId: 1,
  } });
  expect(direct.status()).toBe(405);
  expect((await direct.json()).code).toBe('METHOD_NOT_ALLOWED');

  const pending = [];
  for (const [headers, suffix] of [[providerA, 'a'], [providerB, 'b']]) {
    const candidate = await request.post(api, { headers, data: { ...reqPayload, batchCode: run.id + '-venue-race-' + suffix } });
    expect(candidate.status()).toBe(201);
    pending.push({ id: (await candidate.json()).data.booking.id, headers });
  }
  const approve = ({ id }) => request.post(api + '/' + id + '/review', { headers: admin, data: { approved: true } });
  const results = await Promise.all(pending.map(approve));
  expect(results.map(response => response.status()).sort()).toEqual([200, 409]);
  const winnerIndex = results.findIndex(response => response.status() === 200);
  const loserIndex = 1 - winnerIndex;
  expect((await results[loserIndex].json()).code).toBe('VENUE_SLOT_UNAVAILABLE');
  const winner = pending[winnerIndex];
  const loser = pending[loserIndex];
  expect(sql(`SELECT COUNT(*) FROM venue_booking_sessions s JOIN venue_bookings b USING (booking_id)
    WHERE b.center_id=${centerId} AND s.reserved_at IS NOT NULL AND s.released_at IS NULL;`)).toBe('1');
  const released = await request.post(api + '/' + winner.id + '/close', { headers: winner.headers, data: { action: 'CANCEL' } });
  expect(released.status()).toBe(200);
  expect((await approve(loser)).status()).toBe(200);
  expect(sql(`SELECT COUNT(DISTINCT provider_id),COUNT(DISTINCT center_id)
    FROM training_batches WHERE center_id=${centerId};`)).toBe('2\t1');
  const unauthorizedBatch = (await (await request.get(api + '/' + loser.id, { headers: loser.headers })).json()).data.booking.batchId;
  expect((await request.get(batches + '/' + unauthorizedBatch, { headers: winner.headers })).status()).toBe(403);
});

test('temporary venue proposal is reviewed in jurisdiction and completion releases future sessions', async ({ page, request, smoke }) => {
  const { run } = smoke;
  const name = 'Temporary Community Hall ' + run.id;
  const code = run.id + '-venue-temp';
  const districtId = Number(sql(`SELECT district_id FROM districts WHERE state_id=${run.state}
    AND district_id<>${run.district} AND deleted_at IS NULL AND lifecycle_status_id=1 LIMIT 1;`));
  expect(districtId).toBeGreaterThan(0);
  await smoke.signIn('provider', providerPage);
  await page.getByRole('button', { name: 'Propose temporary location', exact: true }).click();
  const dialog = page.getByRole('dialog', { name: 'Propose temporary location' });
  await dialog.getByLabel(/Venue name/).fill(name);
  await dialog.getByLabel('Address *', { exact: true }).fill('Fixture community hall, main road');
  await dialog.getByLabel(/State or union territory/).selectOption(String(run.state));
  await dialog.getByLabel(/District/).selectOption(String(districtId));
  await dialog.getByLabel(/Venue seating capacity/).fill('25');
  await dialog.getByLabel(/Premises permission reference/).fill('Fixture host consent reference');
  await fillBatch(dialog, run, code);
  await fillSessions(dialog, [session(14), session(15)]);
  const requested = await action(page, smoke, 'POST', api, 'Submit request', 201);
  const id = requested.booking.id;
  expect(requested.booking).toMatchObject({ venueType: 'TEMPORARY', status: 'PENDING', centerId: null, batchId: null,
    premisesPermission: 'Fixture host consent reference' });

  const assignedOtherDistrict = await userHeaders(request, smoke, 'district');
  expect((await request.get(api + '/' + id, { headers: assignedOtherDistrict })).status()).toBe(403);
  expect((await request.post(api + '/' + id + '/review', {
    headers: assignedOtherDistrict, data: { approved: true },
  })).status()).toBe(403);
  const stateAdmin = await userHeaders(request, smoke, 'stateadmin');
  const approved = await request.post(api + '/' + id + '/review', { headers: stateAdmin, data: { approved: true } });
  expect(approved.status()).toBe(200);
  const { booking, sessions } = (await approved.json()).data;
  expect(booking).toMatchObject({ status: 'APPROVED', batchStatus: 'PLANNED' });
  expect(sql(`SELECT provider_id,venue_type,district_id FROM training_centers WHERE center_id=${booking.centerId};`))
    .toBe([run.provider, 'TEMPORARY', districtId].join('\t'));
  expect(sessions.every(slot => slot.reservedAt)).toBe(true);
  const beforeStart = await request.post(api + '/' + id + '/close', {
    headers: await userHeaders(request, smoke, 'provider'), data: { action: 'COMPLETE' },
  });
  expect(beforeStart.status()).toBe(409);
  expect((await beforeStart.json()).code).toBe('VENUE_BATCH_NOT_STARTED');

  // Move only this run's first session into the past to exercise the real start/completion rules.
  sql(`UPDATE venue_booking_sessions s JOIN venue_bookings b USING(booking_id)
    SET s.starts_at=${quote(localDay(-1) + ' 09:00:00')},s.ends_at=${quote(localDay(-1) + ' 11:00:00')}
    WHERE b.batch_code=${quote(code)} AND s.booking_id=${id} AND s.starts_at=${quote(localDay(14) + ' 09:00:00')};
    UPDATE training_batches SET start_date=${quote(localDay(-1))} WHERE batch_code=${quote(code)};`);
  await page.reload();
  await openBooking(page, code);
  await page.getByRole('button', { name: 'Open enrollment' }).click();
  await page.getByRole('button', { name: 'Start training' }).click();
  await page.getByRole('button', { name: 'Complete training', exact: true }).click();
  const done = await action(page, smoke, 'POST', api + '/' + id + '/close', 'Confirm completion', 200);
  expect(done.booking).toMatchObject({ status: 'COMPLETED', batchStatus: 'COMPLETED', centerId: booking.centerId });
  expect(done.sessions.map(slot => Boolean(slot.releasedAt))).toEqual([false, true]);
  expect(sql(`SELECT lifecycle_status_id FROM training_centers WHERE center_id=${booking.centerId};`)).toBe('2');
});
