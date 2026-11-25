import { expect, test } from './support/browser.mjs';
import { sql } from './support/database.mjs';

const career = '/api/v1/career-skills';
const bookings = '/api/v1/venue-bookings';
const day = offset => new Intl.DateTimeFormat('sv-SE', { timeZone: 'Asia/Kolkata' })
  .format(new Date(Date.now() + offset * 86_400_000));
const auth = token => ({ Authorization: `Bearer ${token}` });

async function result(response, status) {
  const payload = await response.json();
  expect(response.status(), payload.message).toBe(status);
  return payload.data;
}

async function login(request, run, role) {
  const response = await request.post('/api/v1/auth/login', { data: { usernameOrEmail: run.users[role], password: run.password } });
  return auth((await result(response, 200)).accessToken);
}

const signOut = page => page.getByRole('button', { name: 'Sign Out' }).click();

test('trainee skills, job requirements, assessed levels and upcoming training connect across roles', async ({ page, request, smoke }) => {
  const { run } = smoke;
  const skillName = 'Torque diagnostics ' + run.id;
  const typedSkill = 'Torque diagnositcs ' + run.id;
  const typedRole = run.id + ' Torque Technicain';
  await smoke.signIn('admin', '/admin/careers');
  await page.getByText('Skills and job role catalog', { exact: true }).click();
  const create = page.getByRole('button', { name: 'Add catalog entry' });
  await page.getByLabel('Category name').fill(run.id + ' Workshop Skills');
  const [categoryResponse] = await Promise.all([smoke.waitApi('POST', `${career}/catalog/CATEGORY`), create.click()]);
  const category = await result(categoryResponse, 201);
  await page.getByLabel('Entry type').selectOption('SKILL');
  await page.getByLabel('Skill name', { exact: true }).fill(skillName);
  await page.getByLabel('Skill category', { exact: true }).selectOption(String(category.id));
  const [skillResponse] = await Promise.all([smoke.waitApi('POST', `${career}/catalog/SKILL`), create.click()]);
  const { id: skillId } = await result(skillResponse, 201);
  await page.getByLabel('Entry type').selectOption('ROLE');
  await page.getByLabel('Job role name').fill(run.id + ' Torque Technician');
  await page.getByLabel('Sector', { exact: true }).selectOption({ index: 1 });
  const [roleResponse] = await Promise.all([smoke.waitApi('POST', `${career}/catalog/ROLE`), create.click()]);
  const { id: roleId } = await result(roleResponse, 201);
  expect(skillId).toBeGreaterThan(0);
  expect(roleId).toBeGreaterThan(0);
  await signOut(page);

  const employer = await login(request, run, 'employer');
  const references = await result(await request.get('/api/v1/employer/reference-data', { headers: employer }), 200);
  const vacancy = await result(await request.post('/api/v1/employer/job-postings', { headers: employer, data: {
    title: run.id + ' career skills vacancy', jobRoleId: roleId, districtId: run.district, vacancies: 2,
    engagementTypeId: references.engagementTypes[0].id, qualificationId: references.qualifications[0].id,
    payFrequencyId: references.payFrequencies[0].id, minimumPay: 12000, maximumPay: 20000,
    skills: [{ skillId, levelId: references.skillLevels.find(level => level.label === 'Basic').id }],
  } }), 201);
  expect(vacancy.skills).toEqual(expect.arrayContaining([expect.objectContaining({ id: skillId })]));
  const skillMatches = await result(await request.get('/api/v1/employer/skills', {
    headers: employer, params: { search: typedSkill },
  }), 200);
  expect(skillMatches[0]).toMatchObject({ id: skillId });
  const vacancies = await result(await request.get('/api/v1/employer/job-postings', {
    headers: employer, params: { search: 'torque technicain' },
  }), 200);
  expect(vacancies.content[0]).toMatchObject({ id: vacancy.id });

  const provider = await login(request, run, 'provider');
  const booking = await result(await request.post(bookings, { headers: provider, data: {
    batchCode: run.id + '-venue-career', seats: 8,
    temporaryVenue: { name: 'Career skills workshop', address: 'Fixture training site', stateId: run.state,
      districtId: run.district, capacity: 12, permissionToUse: 'Local fixture authorization' },
    skillsToTeach: run.id + ' hands-on torque diagnostics and workshop safety',
    sessions: [{ startsAt: day(8) + 'T10:00', endsAt: day(8) + 'T13:00' }],
  } }), 201);
  const reviewer = await login(request, run, 'admin');
  const approved = await result(await request.post(`${bookings}/${booking.booking.id}/review`, {
    headers: reviewer, data: { approved: true },
  }), 200);
  const batchId = approved.booking.batchId;
  expect(batchId).toBeGreaterThan(0);

  const trainee = await smoke.signIn('trainee', '/trainee/profile');
  const own = page.getByRole('region', { name: 'My skills and job goal' });
  await own.getByLabel('Search desired job role', { exact: true }).fill(typedRole);
  await expect(own.getByLabel('Desired job role', { exact: true }).locator(`option[value="${roleId}"]`)).toHaveCount(1);
  await own.getByLabel('Desired job title', { exact: true }).fill(typedRole);
  await own.getByRole('button', { name: 'Add a skill' }).click();
  await own.getByLabel('Skill name 1').fill(typedSkill);
  await own.getByLabel('Skill level 1').selectOption({ label: 'Basic' });
  await own.getByRole('button', { name: 'Add a skill' }).click();
  await own.getByLabel('Skill name 2').fill('Workshop planning');
  const [saved] = await Promise.all([smoke.waitApi('PUT', career + '/me'),
    own.getByRole('button', { name: 'Save skills and job goal' }).click()]);
  const savedProfile = await result(saved, 200);
  expect(savedProfile).toMatchObject({ revision: 1, desiredRoleId: roleId, desiredJobTitle: typedRole });
  expect(savedProfile.skills).toEqual(expect.arrayContaining([expect.objectContaining({ name: 'Workshop planning' }),
    expect.objectContaining({ name: typedSkill, skillId, level: 'Basic' })]));
  expect(savedProfile.requirements).toEqual(expect.arrayContaining([
    expect.objectContaining({ skillId, requiredLevel: 'Basic', needsTraining: false }),
  ]));
  const referenced = await result(await request.get(career + '/me', { headers: auth(trainee.accessToken) }), 200);
  expect(referenced.desiredRoleId).toBe(roleId);
  expect(referenced.requirements).toEqual(expect.arrayContaining([
    expect.objectContaining({ skillId, currentLevel: 'Basic', needsTraining: false }),
  ]));
  const activeFollowup = await request.get('/api/v1/trainees/me/followup-surveys', { headers: auth(trainee.accessToken) });
  expect((await result(activeFollowup, 200)).followupConsentGranted).toBe(false);
  await expect(own.getByRole('status')).toHaveText('Skills and job goal saved.');
  const stale = await request.put(career + '/me', { headers: auth(trainee.accessToken),
    data: { revision: 0, skills: [{ name: 'Outdated edit' }] } });
  expect(stale.status()).toBe(409);
  await signOut(page);

  await smoke.signIn('admin', '/admin/careers');
  await page.getByText('Skills and job role catalog', { exact: true }).click();
  const roleRequirements = page.getByRole('region', { name: 'Set a job role skill requirement' });
  const saveRequirement = roleRequirements.getByRole('button', { name: 'Save role requirement' });
  await expect(page.getByText('Select a candidate to open their career record.')).toBeVisible();
  await expect(saveRequirement).toBeDisabled();
  await roleRequirements.getByLabel('Job role', { exact: true }).selectOption(String(roleId));
  await roleRequirements.getByLabel('Skill', { exact: true }).selectOption(String(skillId));
  await roleRequirements.getByLabel('Required skill level').selectOption({ label: 'Intermediate' });
  const [requirement] = await Promise.all([smoke.waitApi('PUT', `${career}/roles/${roleId}/requirements`),
    saveRequirement.click()]);
  expect(requirement.status()).toBe(204);
  await expect(roleRequirements.getByRole('status')).toHaveText('Role requirement saved.');

  await page.getByLabel('Candidate name or registration number').fill(run.candidateCode);
  await page.getByRole('button', { name: 'Search candidates' }).click();
  await page.getByRole('button', { name: new RegExp(run.candidateCode) }).click();
  const staff = page.getByRole('region', { name: 'Candidate skills and job goal' });
  await expect(staff).toContainText('Workshop planning');
  await expect(staff.getByText('Skill gap', { exact: true })).toBeVisible();
  await expect(staff.getByRole('combobox')).toHaveCount(0);
  await expect(staff.getByRole('button', { name: 'Save role requirement' })).toHaveCount(0);
  const admin = { authorization: requirement.request().headers().authorization };
  for (const path of ['/api/v1/career-progress/recommendations/1/enrollment', '/api/v1/workflows/skill-gap-remediation/enroll-remediation-course']) {
    expect((await request.post(path, { headers: admin, data: {} })).ok()).toBe(false);
  }
  await signOut(page);

  const assessor = await smoke.signIn('assessor', '/assessor/assessments/' + run.assessment);
  const assessmentMatches = await result(await request.get('/api/v1/assessor/assessments', {
    headers: auth(assessor.accessToken), params: { search: 'fixture crourse' },
  }), 200);
  expect(assessmentMatches).toEqual(expect.arrayContaining([expect.objectContaining({ id: run.assessment })]));
  await page.getByText('Candidate skills and job goals', { exact: true }).click();
  await page.getByLabel('Candidate for skill observations').selectOption(String(run.trainee));
  const observations = page.getByRole('region', { name: 'Candidate skill observations' });
  await expect(observations).toContainText('Workshop planning');
  await observations.getByLabel('Skill', { exact: true }).selectOption(String(skillId));
  await observations.getByLabel('Observed skill level').selectOption({ label: 'Awareness' });
  const [observation] = await Promise.all([smoke.waitApi('PUT', `${career}/assessments/${run.assessment}/trainees/${run.trainee}`),
    observations.getByRole('button', { name: 'Save observation' }).click()]);
  expect((await result(observation, 200)).evidence).toEqual(expect.arrayContaining([
    expect.objectContaining({ skillId, level: 'Awareness', source: 'ASSESSMENT' }),
  ]));
  expect((await request.get(`${career}/assessments/${run.assessment}/trainees/${run.prospect}`,
    { headers: auth(assessor.accessToken) })).status()).toBe(404);
  await signOut(page);

  await smoke.signIn('trainee', '/trainee/journey');
  await expect(page.getByRole('region', { name: 'My skills and job goal' })).toContainText('Awareness (assessed)');
  const section = page.getByRole('region', { name: 'Upcoming training suggestions' });
  const session = section.getByRole('listitem').filter({ hasText: 'Career skills workshop' });
  await expect(session).toContainText('Matches your gaps');
  await expect(session).toContainText(skillName);
  const [interested] = await Promise.all([smoke.waitApi('PUT', `${career}/me/training/${batchId}/interest`),
    session.getByRole('button', { name: 'Mark interested', exact: true }).click()]);
  expect(interested.status()).toBe(204);
  await expect(session.getByRole('button', { name: 'Interested', exact: true })).toBeDisabled();
  await section.getByLabel('Search upcoming training by skill or location').fill('diagnositcs torque');
  await section.getByRole('button', { name: 'Search training' }).click();
  await expect(session).toContainText('Career skills workshop');
  expect(sql(`SELECT COUNT(*) FROM training_enrollments WHERE batch_id=${batchId};`)).toBe('0');
  await signOut(page);

  const ownProvider = await smoke.signIn('provider', '/provider/venue-bookings?booking=' + approved.booking.id);
  await expect(page.getByText('Trainees interested', { exact: true }).locator('..')).toHaveText('Trainees interested 1');
  const denied = await request.get(`${career}/trainees/${run.trainee}`, { headers: auth(ownProvider.accessToken) });
  expect(denied.status()).toBe(403);
  const outsider = await login(request, run, 'outsider');
  expect((await request.get(`${career}/provider/batches/${batchId}/interest`, { headers: outsider })).status()).toBe(404);

  await signOut(page);
  await smoke.signIn('trainee', '/trainee/profile');
  await expect(own).toContainText('Workshop planning');
  await own.getByLabel('Desired job role', { exact: true }).selectOption('');
  await own.getByLabel('Desired job title').fill('Independent workshop planner');
  const [changed] = await Promise.all([smoke.waitApi('PUT', career + '/me'),
    own.getByRole('button', { name: 'Save skills and job goal' }).click()]);
  expect(await result(changed, 200)).toMatchObject({ revision: 2, desiredJobTitle: 'Independent workshop planner', requirements: [] });
  await own.getByRole('link', { name: 'See upcoming training' }).click();
  await expect(session.getByRole('button', { name: 'Interested', exact: true })).toBeDisabled();
  const [dismissed] = await Promise.all([smoke.waitApi('PUT', `${career}/me/training/${batchId}/interest`),
    session.getByRole('button', { name: 'Mark not interested' }).click()]);
  expect(dismissed.status()).toBe(204);
  await expect(session.getByRole('button', { name: 'Not interested', exact: true })).toBeDisabled();
  const providerAgain = await login(request, run, 'provider');
  expect(await result(await request.get(`${career}/provider/batches/${batchId}/interest`, { headers: providerAgain }), 200)).toEqual({ interested: 0 });
});
