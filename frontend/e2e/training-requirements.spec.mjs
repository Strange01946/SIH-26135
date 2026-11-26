import { expect, test } from './support/browser.mjs';
import { sql } from './support/database.mjs';

const api = '/api/v1/training-requirements';
const venues = '/api/v1/venue-bookings';
const government = '/admin/training-requirements';
const opportunities = '/provider/training-opportunities';
const day = (offset) => new Intl.DateTimeFormat('sv-SE', { timeZone: 'Asia/Kolkata' })
  .format(new Date(Date.now() + offset * 86_400_000));
const auth = (accessToken) => ({ Authorization: 'Bearer ' + accessToken });

async function headers(request, run, role) {
  const response = await request.post('/api/v1/auth/login', {
    data: { usernameOrEmail: run.users[role], password: run.password },
  });
  expect(response.status()).toBe(200);
  return auth((await response.json()).data.accessToken);
}

async function signOut(page) {
  await page.getByRole('button', { name: 'Sign Out', exact: true }).click();
  await expect(page.getByRole('heading', { name: 'Sign in', exact: true })).toBeVisible();
}

async function body(response, status) {
  const payload = await response.json();
  expect(response.status(), payload.code + ': ' + payload.message).toBe(status);
  return payload.data;
}

async function expectError(response, status, code) {
  expect(response.status()).toBe(status);
  expect((await response.json()).code).toBe(code);
}

function createCentre(request, reviewer, run, key, stateId, districtId) {
  return request.post(venues + '/venues', { headers: reviewer, data: {
    code: run.id + '-gov-lab-' + key, name: 'Government Skills Lab ' + run.id + ' ' + key,
    address: 'Training workshop', pincode: '110001', capacity: 25, stateId, districtId,
  } }).then(response => body(response, 201));
}

const publish = (run, center, key) => ({
  title: run.id + '-requirement-' + key,
  skillsToTeach: run.id + ' Equipment inspection and fault reporting',
  description: 'Provide protective equipment for the practical sessions.',
  stateId: center.stateId, districtId: center.districtId, centerId: center.id, seats: 12,
  earliestDate: day(9), latestDate: day(12), dailyStartTime: '09:00', dailyEndTime: '13:00',
});
const slot = (offset, from, to) => ({ startsAt: day(offset) + 'T' + from + ':00', endsAt: day(offset) + 'T' + to + ':00' });

test('government publishes a fixed center and the award reserves exactly the accepted schedule', async ({ page, request, smoke }) => {
  const { run } = smoke;
  const [sameStateDistrict, outsideState, outsideDistrict] = sql('SELECT ' +
    '(SELECT district_id FROM districts WHERE state_id=' + run.state + ' AND district_id<>' + run.district +
      ' AND lgd_code IS NOT NULL AND lifecycle_status_id=1 AND deleted_at IS NULL ORDER BY district_id LIMIT 1),' +
    'd.state_id,d.district_id FROM districts d JOIN states s USING (state_id) WHERE d.state_id<>' + run.state +
      ' AND d.lgd_code IS NOT NULL AND d.lifecycle_status_id=1 AND d.deleted_at IS NULL ' +
      ' AND s.lifecycle_status_id=1 AND s.deleted_at IS NULL ORDER BY d.district_id LIMIT 1;').split('\t').map(Number);
  expect([sameStateDistrict, outsideState, outsideDistrict].every(id => Number.isSafeInteger(id) && id > 0)).toBe(true);
  const [stateAdmin, nationalAdmin] = await Promise.all([headers(request, run, 'stateadmin'), headers(request, run, 'admin')]);
  const center = await createCentre(request, nationalAdmin, run, 'main', run.state, run.district);
  const otherCenter = await createCentre(request, nationalAdmin, run, 'state', run.state, sameStateDistrict);
  const outsideCenter = await createCentre(request, nationalAdmin, run, 'other', outsideState, outsideDistrict);
  const publication = publish(run, center, 'main');
  const secondaryId = sql("SELECT education_level_id FROM ref_education_level WHERE education_code='SECONDARY';");
  const prerequisites = 'Basic Java syntax is recommended.';
  const offer = { startDate: day(10), endDate: day(11), dailyStartTime: '10:00', dailyEndTime: '12:00' };
  const otherOffer = { ...offer, dailyStartTime: '12:00', dailyEndTime: '13:00' };
  const dialog = page.getByRole('dialog');
  const listItem = (title) => page.getByRole('listitem').filter({ hasText: title });

  const district = auth((await smoke.signIn('district', government)).accessToken);
  await page.getByRole('button', { name: 'New requirement' }).click();
  await expect(dialog.getByLabel('State or union territory *')).toBeDisabled();
  await expect(dialog.getByLabel('District *')).toHaveValue(String(run.district));
  await expect(dialog.getByLabel('District *')).toBeDisabled();
  await dialog.getByLabel('Requirement title *').fill(publication.title);
  await expect(dialog.getByLabel('Course *', { exact: true })).toHaveCount(0);
  await expect(dialog.getByLabel('Program *', { exact: true })).toHaveCount(0);
  await dialog.getByLabel('Skills to teach *').fill(publication.skillsToTeach);
  await expect(dialog.getByLabel('Minimum education level')).toHaveValue('');
  await expect(dialog.getByLabel('Minimum education level')).toBeEnabled();
  await dialog.getByLabel('Minimum education level').selectOption(secondaryId);
  await dialog.getByLabel('Recommended prerequisites (optional)').fill(prerequisites);
  await dialog.getByLabel('Seats *').fill('12');
  await dialog.getByLabel('Earliest training date *').fill(publication.earliestDate);
  await dialog.getByLabel('Latest training date *').fill(publication.latestDate);
  await dialog.getByLabel('Daily start time (IST) *').fill('09:00');
  await dialog.getByLabel('Daily end time (IST) *').fill('13:00');
  await dialog.getByLabel('Delivery requirements').fill(publication.description);
  const centerField = dialog.getByLabel('Training center *');
  await dialog.getByRole('button', { name: 'Publish requirement' }).click();
  await expect(centerField).toBeFocused();
  await expect(centerField.getByRole('option', { name: outsideCenter.name, exact: false })).toHaveCount(0);
  await centerField.selectOption(String(center.id));
  const [published] = await Promise.all([smoke.waitApi('POST', api + '/government'),
    dialog.getByRole('button', { name: 'Publish requirement' }).click()]);
  const created = await body(published, 201);
  const id = created.id;
  expect(created).toMatchObject({ centerId: center.id, centerName: center.name, status: 'OPEN', bookingId: null });
  expect(created.admission).toMatchObject({
    minimumEducationLevelId: Number(secondaryId), recommendedPrerequisites: prerequisites,
  });
  expect(published.request().postDataJSON()).toMatchObject({ centerId: center.id, skillsToTeach: publication.skillsToTeach });
  await expect(page.getByRole('heading', { name: publication.title })).toBeVisible();
  await expectError(await request.post(api + '/government', { headers: district, data: { ...publication, centerId: otherCenter.id } }),
    400, 'TRAINING_VENUE_OUTSIDE_AREA');
  await expectError(await request.post(api + '/government', { headers: district, data: { ...publication, seats: 26 } }),
    400, 'VENUE_CAPACITY_EXCEEDED');

  const outside = publish(run, outsideCenter, 'outside');
  expect((await request.post(api + '/government', { headers: stateAdmin, data: outside })).status()).toBe(403);
  const otherState = await body(await request.post(api + '/government', { headers: nationalAdmin, data: outside }), 201);
  const inState = publish(run, otherCenter, 'state');
  expect((await request.post(api + '/government', { headers: district, data: inState })).status()).toBe(403);
  const otherDistrict = await body(await request.post(api + '/government', { headers: stateAdmin, data: inState }), 201);
  const stateList = await body(await request.get(api + '/government', { headers: stateAdmin }), 200);
  expect(stateList.map(item => item.id)).toEqual(expect.arrayContaining([id, otherDistrict.id]));
  expect(stateList.map(item => item.id)).not.toContain(otherState.id);
  expect((await request.get(api + '/government/' + otherDistrict.id, { headers: district })).status()).toBe(403);
  expect((await request.post(api + '/government/' + otherState.id + '/select', { headers: stateAdmin, data: { applicationId: 1 } })).status()).toBe(403);

  await signOut(page);
  const provider = auth((await smoke.signIn('provider', opportunities)).accessToken);
  await listItem(publication.title).getByRole('button', { name: 'View requirement' }).click();
  const requirement = page.getByRole('region', { name: 'Training requirement details' });
  await expect(requirement.getByText(center.name, { exact: true })).toBeVisible();
  await page.getByRole('button', { name: 'Apply to deliver training' }).click();
  await expect(dialog.getByRole('combobox', { name: /center/i })).toHaveCount(0);
  await dialog.getByLabel('Proposed first day *').fill(offer.startDate);
  await dialog.getByLabel('Proposed last day *').fill(offer.endDate);
  await dialog.getByLabel('Daily start time (IST) *').fill(offer.dailyStartTime);
  await dialog.getByLabel('Daily end time (IST) *').fill(offer.dailyEndTime);
  await dialog.getByLabel('Delivery plan and equipment needs').fill('Two practical sessions with safety kits');
  const [applied] = await Promise.all([smoke.waitApi('POST', api + '/' + id + '/applications'),
    dialog.getByRole('button', { name: 'Submit application' }).click()]);
  const winnerApplication = await body(applied, 201);
  await expect(page.getByText('Application submitted.', { exact: true })).toBeVisible();
  expect((await request.post(api + '/government', { headers: provider, data: publication })).status()).toBe(403);
  await expectError(await request.post(api + '/' + id + '/applications', { headers: provider, data: offer }),
    409, 'TRAINING_APPLICATION_EXISTS');
  const competing = await headers(request, run, 'outsider');
  await expectError(await request.post(api + '/' + id + '/applications', {
    headers: competing, data: { ...offer, dailyStartTime: '07:00' },
  }), 400, 'TRAINING_OFFER_OUTSIDE_WINDOW');
  const loserApplication = await body(await request.post(api + '/' + id + '/applications', {
    headers: competing, data: { ...otherOffer, deliveryNotes: 'Alternate equipment proposal' },
  }), 201);
  const providerView = await body(await request.get(api + '/' + id, { headers: provider }), 200);
  expect(providerView.applications).toEqual([]);
  expect(providerView.myApplication.id).toBe(winnerApplication.id);
  const calendar = venues + '/venues/' + center.id;
  const availability = () => request.get(calendar + '/availability', { headers: nationalAdmin, params: { from: day(9), to: day(13) } })
    .then(response => body(response, 200));
  expect(await availability()).toEqual([]);

  await signOut(page);
  const reviewer = auth((await smoke.signIn('stateadmin', government)).accessToken);
  await listItem(publication.title).getByRole('button', { name: 'View requirement' }).click();
  const winnerRow = requirement.getByRole('listitem').filter({ hasText: winnerApplication.providerName });
  await expect(requirement.getByText('Alternate equipment proposal')).toBeVisible();
  const closure = await body(await request.post(calendar + '/blackouts', { headers: nationalAdmin, data: {
    ...slot(11, '11:00', '12:00'), reason: 'Equipment maintenance',
  } }), 201);
  async function selectWinner() {
    page.once('dialog', confirmation => confirmation.accept());
    const [response] = await Promise.all([smoke.waitApi('POST', api + '/government/' + id + '/select'),
      winnerRow.getByRole('button', { name: 'Select provider' }).click()]);
    return response;
  }
  smoke.allowHttpFailure('POST', api + '/government/' + id + '/select', 409);
  await expectError(await selectWinner(), 409, 'VENUE_SLOT_UNAVAILABLE');
  await expect(requirement.getByRole('alert')).toContainText('overlaps an existing reservation or center closure');
  expect(sql('SELECT application_status FROM training_requirement_applications WHERE requirement_id=' + id + ' ORDER BY application_id;'))
    .toBe('APPLIED\nAPPLIED');
  expect(sql('SELECT COUNT(*) FROM venue_booking_training_plans WHERE requirement_id=' + id + ';')).toBe('0');
  expect((await body(await request.get(api + '/government/' + id, { headers: reviewer }), 200)).requirement)
    .toMatchObject({ status: 'OPEN', bookingId: null });

  expect((await request.delete(calendar + '/blackouts/' + closure.blackoutId, { headers: nationalAdmin })).status()).toBe(204);
  const awarded = await body(await selectWinner(), 200);
  expect(awarded).toMatchObject({ status: 'AWARDED', selectedProviderId: run.provider, centerId: center.id, bookingStatus: 'APPROVED' });
  await expect(requirement.getByText('Not selected', { exact: true })).toBeVisible();
  await expectError(await request.post(api + '/government/' + id + '/select', {
    headers: reviewer, data: { applicationId: loserApplication.id },
  }), 409, 'TRAINING_REQUIREMENT_AWARDED');
  const expectedSlots = [slot(10, '10:00', '12:00'), slot(11, '10:00', '12:00')];
  expect(await availability()).toEqual(expectedSlots.map(session => ({ ...session, blackoutId: null, reason: null })));

  const booked = await body(await request.get(venues + '/' + awarded.bookingId, { headers: reviewer }), 200);
  expect(booked.booking).toMatchObject({ status: 'APPROVED', centerId: center.id, seats: 12, providerId: run.provider,
    batchCode: 'GOV-REQ-' + id, requirementId: id, skillsToTeach: publication.skillsToTeach });
  expect(booked.sessions).toMatchObject(expectedSlots);
  expect(booked.sessions.every(session => session.reservedAt && !session.releasedAt)).toBe(true);
  const batch = await body(await request.get('/api/v1/training-batches/' + booked.booking.batchId, { headers: nationalAdmin }), 200);
  expect(batch).toMatchObject({ centerId: center.id, providerId: run.provider, courseId: booked.booking.courseId,
    programId: booked.booking.programId, capacity: 12 });

  await signOut(page);
  const loser = auth((await smoke.signIn('outsider', opportunities)).accessToken);
  await expect(listItem(publication.title)).toHaveCount(0);
  await page.getByRole('button', { name: 'My applications' }).click();
  await expect(listItem(publication.title).getByText('Not selected', { exact: true })).toBeVisible();
  await expect(listItem(publication.title).getByRole('button', { name: 'View assignment' })).toHaveCount(0);
  expect((await request.get(api + '/' + id, { headers: loser })).status()).toBe(404);
  expect((await request.get(venues + '/' + awarded.bookingId, { headers: loser })).status()).toBe(403);
  const independent = { centerId: center.id, batchCode: run.id + '-venue-repeat', seats: 12,
    skillsToTeach: publication.skillsToTeach.toUpperCase(), sessions: [slot(10, '12:00', '13:00')] };
  await expectError(await request.post(venues, { headers: loser, data: { ...independent, requirementId: id } }),
    400, 'TRAINING_GOVERNMENT_BOOKING_REQUIRED');
  await expectError(await request.post(venues, { headers: loser, data: { ...independent, sessions: expectedSlots } }),
    409, 'VENUE_SLOT_UNAVAILABLE');
  const adjacent = await body(await request.post(venues, { headers: loser, data: independent }), 201);
  const adjacentApproval = await body(await request.post(venues + '/' + adjacent.booking.id + '/review', {
    headers: reviewer, data: { approved: true },
  }), 200);
  expect(adjacentApproval.booking).toMatchObject({ courseId: booked.booking.courseId, programId: booked.booking.programId });

  await signOut(page);
  const winner = auth((await smoke.signIn('provider', opportunities)).accessToken);
  await page.getByRole('button', { name: 'My applications' }).click();
  await listItem(publication.title).getByRole('button', { name: 'View assignment' }).click();
  await expect(requirement.getByText(center.name, { exact: true })).toBeVisible();
  await expect(requirement.getByRole('link', { name: 'Schedule assigned training' })).toHaveCount(0);
  await requirement.getByRole('link', { name: 'View venue booking' }).click();
  await expect(page.getByText('Government training requirement #' + id)).toBeVisible();
  await expect(page.getByRole('button', { name: 'Approve booking' })).toHaveCount(0);
  const outsideWindow = [slot(13, '09:00', '11:00')];
  await expectError(await request.post(venues + '/' + awarded.bookingId + '/reschedule', {
    headers: winner, data: { sessions: outsideWindow },
  }), 400, 'TRAINING_BOOKING_OUTSIDE_WINDOW');

  const postponedSlot = slot(12, '11:00', '13:00');
  const pendingPostponement = await body(await request.post(venues + '/' + awarded.bookingId + '/reschedule', {
    headers: winner, data: { sessions: [postponedSlot] },
  }), 201);
  const current = () => request.get(api + '/' + id, { headers: winner }).then(response => body(response, 200));
  expect((await current()).requirement.bookingId).toBe(awarded.bookingId);
  const moved = await body(await request.post(venues + '/' + pendingPostponement.booking.id + '/review', {
    headers: reviewer, data: { approved: true },
  }), 200);
  expect(moved.booking).toMatchObject({ centerId: center.id, batchId: booked.booking.batchId, status: 'APPROVED' });
  expect((await current()).requirement.bookingId).toBe(moved.booking.id);
  expect((await availability()).map(({ startsAt, endsAt }) => ({ startsAt, endsAt })))
    .toEqual([independent.sessions[0], postponedSlot]);
});

test('competing government awards cannot reserve the same center slot twice', async ({ request, smoke }) => {
  const { run } = smoke;
  const admin = await headers(request, run, 'admin');
  const provider = await headers(request, run, 'provider');
  const center = await createCentre(request, admin, run, 'race', run.state, run.district);
  const requirements = await Promise.all(['first', 'second'].map(key =>
    request.post(api + '/government', { headers: admin, data: publish(run, center, key) }).then(response => body(response, 201))));
  const offer = { startDate: day(10), endDate: day(10), dailyStartTime: '10:00', dailyEndTime: '11:00' };
  const applications = await Promise.all(requirements.map(r =>
    request.post(api + '/' + r.id + '/applications', { headers: provider, data: offer }).then(response => body(response, 201))));
  const approvals = await Promise.all(requirements.map((r, i) =>
    request.post(api + '/government/' + r.id + '/select', { headers: admin, data: { applicationId: applications[i].id } })));
  expect(approvals.map(result => result.status()).sort()).toEqual([200, 409]);
  await expectError(approvals.find(result => result.status() === 409), 409, 'VENUE_SLOT_UNAVAILABLE');
  const losingId = requirements[approvals.findIndex(result => result.status() === 409)].id;
  const losing = await body(await request.get(api + '/government/' + losingId, { headers: admin }), 200);
  expect(losing.requirement).toMatchObject({ status: 'OPEN', bookingId: null });
  expect(losing.applications[0].status).toBe('APPLIED');
});

test('a reviewer can give an older award its center without a new provider request', async ({ page, request, smoke }) => {
  const { run } = smoke;
  const admin = auth((await smoke.signIn('admin', government)).accessToken);
  const center = await createCentre(request, admin, run, 'legacy', run.state, run.district);
  const publication = publish(run, center, 'legacy');
  const saved = await body(await request.post(api + '/government', { headers: admin, data: publication }), 201);
  const provider = await headers(request, run, 'provider');
  const offer = { startDate: day(10), endDate: day(10), dailyStartTime: '10:00', dailyEndTime: '11:00' };
  const application = await body(await request.post(api + '/' + saved.id + '/applications', { headers: provider, data: offer }), 201);
  sql('START TRANSACTION; UPDATE training_requirement_applications SET application_status=\'SELECTED\' WHERE application_id=' +
    application.id + '; UPDATE training_requirements SET center_id=NULL,requirement_status=\'AWARDED\',selected_provider_id=' +
    run.provider + ',selected_by=published_by,selected_at=CURRENT_TIMESTAMP WHERE requirement_id=' + saved.id + '; COMMIT;');
  const route = api + '/government/' + saved.id + '/centre';
  expect((await request.post(route, { headers: provider, data: { centerId: center.id } })).status()).toBe(403);
  await page.reload();
  await page.getByRole('listitem').filter({ hasText: publication.title }).getByRole('button', { name: 'View requirement' }).click();
  await page.getByRole('button', { name: 'Assign training center', exact: true }).click();
  const dialog = page.getByRole('dialog', { name: 'Assign training center' });
  await dialog.getByLabel('Training center *').selectOption(String(center.id));
  const [updated] = await Promise.all([smoke.waitApi('POST', route),
    dialog.getByRole('button', { name: 'Confirm training center' }).click()]);
  const recovered = await body(updated, 200);
  expect(recovered).toMatchObject({ centerId: center.id, status: 'AWARDED', bookingStatus: 'APPROVED', selectedProviderId: run.provider });
  expect((await request.post(route, { headers: admin, data: { centerId: center.id } })).status()).toBe(409);
  await expect(page.getByRole('link', { name: 'View venue booking' })).toBeVisible();
});

test('training replaces catalog management while historical course records remain readable', async ({ page, request, smoke }) => {
  const { run } = smoke;
  const admin = await smoke.signIn('admin', '/admin/courses');
  const adminHeaders = auth(admin.accessToken);
  await expect(page.getByRole('heading', { name: 'Training Requirements', exact: true })).toBeVisible();
  await expect(page.getByRole('link', { name: 'Course Management' })).toHaveCount(0);

  const courses = '/api/v1/courses';
  await body(await request.get(courses + '/' + run.course, { headers: adminHeaders }), 200);
  expect((await request.post(courses, { headers: adminHeaders, data: {} })).status()).toBe(405);
  expect((await request.put(courses + '/' + run.course, { headers: adminHeaders, data: {} })).status()).toBe(405);
  expect((await request.delete(courses + '/' + run.course, { headers: adminHeaders })).status()).toBe(405);

  const program = sql("SELECT program_id FROM programs WHERE program_code='SKILLS-REPORTING';");
  expect((await request.delete('/api/v1/programs/' + program, { headers: adminHeaders })).status()).toBe(409);

  await signOut(page);
  await smoke.signIn('provider', '/provider/courses');
  await expect(page.getByRole('heading', { name: 'Training Opportunities', exact: true })).toBeVisible();
  await expect(page.getByRole('link', { name: 'Available Courses' })).toHaveCount(0);
});
