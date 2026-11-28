import { expect, test } from './support/browser.mjs';
import { sql } from './support/database.mjs';

const history = '/api/v1/trainees/me/employment-history';
const authorise = token => ({ Authorization: 'Bearer ' + token });

async function data(response, status) {
  const body = await response.json();
  expect(response.status(), body.message).toBe(status);
  return body.data;
}

async function signInApi(request, run, role) {
  const response = await request.post('/api/v1/auth/login', {
    data: { usernameOrEmail: run.users[role], password: run.password },
  });
  return authorise((await data(response, 200)).accessToken);
}

test('trainee employment and employer posting hires feed live MIS without a verification request', async ({ page, request, smoke }) => {
  const { run } = smoke;
  const [enrollment, supporting, supportingCourse, foreignEnrollment, role, joined, correctedJoin, ended, hired, skill] = sql(`
    UPDATE trainees SET user_id=(SELECT user_id FROM users WHERE username='${run.users.trainee2}')
      WHERE trainee_id=${run.prospect} AND registration_number='${run.enrollmentCandidate}';
    INSERT INTO training_enrollments(enrollment_number,trainee_id,course_id,provider_id,center_id,batch_id,program_id,
      enrollment_status_id,enrollment_date,start_date,actual_completion_date)
    SELECT '${run.id}-completed',${run.prospect},course_id,provider_id,center_id,batch_id,program_id,3,
      CURDATE()-INTERVAL 12 MONTH,CURDATE()-INTERVAL 12 MONTH,CURDATE()-INTERVAL 11 MONTH
    FROM training_batches WHERE batch_code='${run.id}-assess-batch';
    SET @completed=LAST_INSERT_ID();
    INSERT INTO courses(course_code,course_name,sector_id,delivery_mode_id,lifecycle_status_id)
    SELECT '${run.id}-sql-course','SQL Fundamentals',sector_id,delivery_mode_id,1
    FROM courses WHERE course_id=${run.course};
    SET @supportCourse=LAST_INSERT_ID();
    INSERT INTO course_programs(course_id,program_id) VALUES (@supportCourse,${run.program});
    INSERT INTO training_batches(batch_code,course_id,provider_id,center_id,program_id,start_date,end_date,capacity,batch_status_id)
    SELECT '${run.id}-venue-sql',@supportCourse,provider_id,center_id,program_id,
      CURDATE()-INTERVAL 12 MONTH,CURDATE()-INTERVAL 11 MONTH,10,4
    FROM training_batches WHERE batch_code='${run.id}-assess-batch';
    SET @supportBatch=LAST_INSERT_ID();
    INSERT INTO training_enrollments(enrollment_number,trainee_id,course_id,provider_id,center_id,batch_id,program_id,
      enrollment_status_id,enrollment_date,start_date,actual_completion_date)
    SELECT '${run.id}-sql',${run.prospect},course_id,provider_id,center_id,batch_id,program_id,3,start_date,start_date,end_date
    FROM training_batches WHERE batch_id=@supportBatch;
    SET @supporting=LAST_INSERT_ID();
    INSERT INTO trainee_consents(trainee_id,consent_type_id,consent_status_id,purpose,policy_version,granted_at)
      SELECT ${run.prospect},consent_type_id,1,'Fixture employer sharing','1.0',NOW()
      FROM ref_consent_type WHERE consent_code='EMPLOYER_SHARING';
    SELECT @completed,@supporting,@supportCourse,(SELECT enrollment_id FROM training_enrollments WHERE enrollment_number='${run.id}-assess-enroll'),
      (SELECT job_role_id FROM job_roles WHERE job_role_code='${run.id}-role'),
      CURDATE()-INTERVAL 10 MONTH,CURDATE()-INTERVAL 10 MONTH+INTERVAL 1 DAY,CURDATE()-INTERVAL 9 MONTH,CURDATE(),
      (SELECT skill_id FROM skills WHERE deleted_at IS NULL AND lifecycle_status_id=1 ORDER BY skill_id LIMIT 1);
  `).split('\t');
  expect(Number(enrollment)).toBeGreaterThan(0);
  const params = { stateId: run.state, districtId: run.district, executive: true };
  const dashboard = async (endpoint, courseId, executive = false) => {
    const headers = await signInApi(request, run, 'admin');
    return data(await request.get('/api/v1/dashboards/' + endpoint, {
      headers, params: { ...params, executive, ...(courseId ? { courseId } : {}) },
    }), 200);
  };
  const mis = async (courseId) => {
    const overview = await dashboard('kpis', courseId, true);
    return Object.fromEntries(overview.executiveKpis.kpis.map(row => [row.key, row.current.value]));
  };
  const initial = await mis();
  await smoke.signIn('trainee2', '/trainee/journey');
  await page.getByRole('button', { name: 'Add employment', exact: true }).click();
  const add = page.getByRole('dialog', { name: 'Add employment' });
  await add.getByLabel('Job role').fill('Independent lab technician');
  await add.getByLabel('Employer name').fill('Local laboratory');
  const primaryCheckbox = add.getByRole('checkbox', { name: new RegExp(run.id + '-assess-batch') });
  const supportingCheckbox = add.getByRole('checkbox', { name: /SQL Fundamentals/ });
  await expect(primaryCheckbox).toBeEnabled();
  await expect(supportingCheckbox).toBeEnabled();
  await primaryCheckbox.check();
  await supportingCheckbox.check();
  const primarySelect = add.getByLabel('Primary training for MIS');
  await add.getByLabel('Joining date').fill('2000-01-01');
  await expect(primaryCheckbox).toBeChecked();
  await expect(supportingCheckbox).toBeChecked();
  await expect(add.getByRole('button', { name: 'Save employment' })).toBeDisabled();
  await expect(add.getByText(/remove the unavailable training links or correct the joining date/i)).toBeVisible();
  await add.getByLabel('Joining date').fill(joined);
  const payFrequency = add.getByLabel('Pay frequency');
  await payFrequency.selectOption({ index: 1 });
  const payFrequencyId = Number(await payFrequency.inputValue());
  await primarySelect.selectOption(enrollment);
  const [created] = await Promise.all([smoke.waitApi('POST', history), add.getByRole('button', { name: 'Save employment' }).click()]);
  const id = await data(created, 200);
  expect(sql(`SELECT starting_salary IS NULL FROM employment_records WHERE employment_id=${id};`)).toBe('1');
  const job = page.getByRole('listitem').filter({ has: page.getByText('Independent lab technician', { exact: true }) });
  await expect(job).toContainText('Self-reported');
  await expect(job).toContainText('SQL Fundamentals');
  expect(sql(`SELECT enrollment_id FROM employment_training_links WHERE employment_id=${id};`)).toBe(supporting);
  expect(await mis(run.course)).toMatchObject({ placed: initial.placed + 1, retention6m: 100 });
  const supportingHeadline = await mis(supportingCourse);
  expect(supportingHeadline.placed).toBe(0);
  expect(supportingHeadline.retention6m ?? null).toBeNull();
  const [programs, providers, courses, funnel, total] = await Promise.all([
    dashboard('scheme-matrix', run.course), dashboard('providers', run.course), dashboard('courses', run.course),
    dashboard('pipeline-funnel', run.course, true), dashboard('kpis', run.course),
  ]);
  for (const payload of [programs, providers, courses]) {
    expect(payload.items[0]).toMatchObject({ enrollmentCount: 2, joinedPlacementCount: 1, placementRatePct: 50 });
  }
  expect(funnel.candidateStages.find(stage => stage.key === 'placed').candidates.value).toBe(1);
  expect(total).toMatchObject({ joinedPlacementCount: 1, placementRatePct: 50 });
  const owner = await signInApi(request, run, 'trainee2');
  const owned = await data(await request.get(history, { headers: owner }), 200);
  expect(owned.content.find(period => period.id === id).relatedTraining)
    .toEqual([expect.objectContaining({ id: Number(enrollment), primary: true }),
      expect.objectContaining({ id: Number(supporting), primary: false })]);
  const invalidPrimary = await request.put(`${history}/${id}`, { headers: owner, data: {
    jobTitle: 'Independent lab technician', startDate: joined, payFrequencyId,
    enrollmentId: Number(enrollment), trainingEnrollmentIds: [Number(supporting)],
  } });
  expect(invalidPrimary.status()).toBe(400);
  const otherTraining = await request.put(`${history}/${id}`, { headers: owner, data: {
    jobTitle: 'Independent lab technician', startDate: joined, payFrequencyId,
    enrollmentId: Number(enrollment), trainingEnrollmentIds: [Number(enrollment), Number(foreignEnrollment)],
  } });
  expect(otherTraining.status()).toBe(400);
  expect((await otherTraining.json()).code).toBe('INVALID_EMPLOYMENT_ENROLLMENT');
  const otherTrainee = await signInApi(request, run, 'trainee');
  expect((await request.put(`${history}/${id}`, { headers: otherTrainee, data: {
    jobTitle: 'Someone else’s job', startDate: joined, payFrequencyId,
  } })).status()).toBe(404);

  await job.getByRole('button', { name: 'Edit employment', exact: true }).click();
  const edit = page.getByRole('dialog', { name: 'Edit employment' });
  await expect(edit.getByRole('checkbox', { name: /SQL Fundamentals/ })).toBeChecked();
  await edit.getByLabel('Primary training for MIS').selectOption(supporting);
  await edit.getByLabel('Job role').fill('Senior lab technician');
  await edit.getByLabel('Joining date').fill(correctedJoin);
  await edit.getByLabel('Last working day').fill(ended);
  await edit.getByLabel('Starting pay').fill('18500');
  await edit.getByLabel('Pay frequency').selectOption({ index: 2 });
  const correctedFrequency = await edit.getByLabel('Pay frequency').inputValue();
  const [updated] = await Promise.all([smoke.waitApi('PUT', `${history}/${id}`), edit.getByRole('button', { name: 'Save employment' }).click()]);
  expect(await data(updated, 200)).toBe(id);
  await expect(page.getByRole('listitem').filter({ hasText: 'Senior lab technician' })).toContainText('18,500');
  expect(sql(`SELECT salary_amount,effective_from,salary_frequency_id FROM salary_history WHERE employment_id=${id};`))
    .toBe(`18500.00\t${correctedJoin}\t${correctedFrequency}`);
  expect(sql(`SELECT enrollment_id FROM employment_records WHERE employment_id=${id};`)).toBe(supporting);
  expect(sql(`SELECT enrollment_id FROM employment_training_links WHERE employment_id=${id};`)).toBe(enrollment);
  expect((await mis(run.course)).retention6m ?? null).toBeNull();
  expect((await mis(run.course)).placed).toBe(0);
  expect(await mis(supportingCourse)).toMatchObject({ placed: 1, retention6m: 0 });

  await page.getByRole('button', { name: 'Sign Out' }).click();
  const employer = await smoke.signIn('employer', '/employer/vacancies');
  const employerHeaders = authorise(employer.accessToken);
  const references = await data(await request.get('/api/v1/employer/reference-data', { headers: employerHeaders }), 200);
  const vacancy = await data(await request.post('/api/v1/employer/job-postings', { headers: employerHeaders, data: {
    title: run.id + ' hiring vacancy', jobRoleId: Number(role), districtId: run.district, vacancies: 2,
    engagementTypeId: references.engagementTypes[0].id, qualificationId: references.qualifications[0].id,
    payFrequencyId, minimumPay: 12000, maximumPay: 30000,
    skills: [{ skillId: Number(skill), levelId: references.skillLevels[0].id }],
  } }), 201);
  await page.reload();
  await page.getByRole('listitem').filter({ has: page.getByRole('heading', { name: vacancy.title }) })
    .getByRole('button', { name: 'Record joining' }).click();
  const joinForm = page.getByRole('dialog', { name: 'Record candidate joining' });
  await joinForm.getByLabel('Trainee registration number').fill(run.enrollmentCandidate);
  await joinForm.getByLabel('Training enrollment number').fill(run.id + '-completed');
  await joinForm.getByLabel('Joining date').fill(hired);
  await joinForm.getByLabel('Starting pay').fill('24000');
  await joinForm.getByLabel('Pay frequency').selectOption(String(payFrequencyId));
  const hirePath = `/api/v1/employer/job-postings/${vacancy.id}/hires`;
  const [confirmed] = await Promise.all([smoke.waitApi('POST', hirePath),
    joinForm.getByRole('button', { name: 'Confirm joining' }).click()]);
  const transition = await data(confirmed, 201);
  expect(transition.placement).toMatchObject({ placementStatusCode: 'JOINED', jobPostingId: vacancy.id });
  expect(transition.employmentRecord).toMatchObject({ recordVerificationStatusCode: 'VERIFIED',
    employmentInfoSourceCode: 'EMPLOYER_CONFIRMATION', enrollmentId: Number(enrollment) });
  expect(sql(`SELECT COUNT(*) FROM employment_verification_requests WHERE employment_id=${transition.employmentRecord.id};`))
    .toBe('0');
  const sameHire = { registrationNumber: run.enrollmentCandidate, enrollmentNumber: run.id + '-completed', joiningDate: hired, pay: 24000, payFrequencyId };
  expect((await request.post(hirePath, { headers: otherTrainee, data: sameHire })).status()).toBe(403);
  expect((await request.post(hirePath, { headers: employerHeaders, data: sameHire })).status()).toBe(409);
  expect(await mis()).toMatchObject({ placed: initial.placed + 2, retention6m: 0 });
  expect(await mis(run.course)).toMatchObject({ placed: 1 });

  await page.getByRole('button', { name: 'Sign Out' }).click();
  await smoke.signIn('trainee2', '/trainee/journey');
  const confirmedJob = page.getByRole('listitem').filter({ hasText: 'Employer-confirmed' });
  await expect(confirmedJob).toContainText('24,000');
  await expect(confirmedJob.getByText('Primary', { exact: true })).toBeVisible();
  await expect(confirmedJob.getByRole('button', { name: 'Edit employment' })).toHaveCount(0);
  const selfReported = page.getByRole('listitem').filter({ hasText: 'Senior lab technician' });
  await selfReported.getByRole('button', { name: 'Edit employment', exact: true }).click();
  const changePrimary = page.getByRole('dialog', { name: 'Edit employment' });
  await changePrimary.getByLabel('Primary training for MIS').selectOption(enrollment);
  const [mergedAttribution] = await Promise.all([smoke.waitApi('PUT', `${history}/${id}`),
    changePrimary.getByRole('button', { name: 'Save employment' }).click()]);
  expect(await data(mergedAttribution, 200)).toBe(id);
  expect(await mis()).toMatchObject({ placed: initial.placed + 1, retention6m: 0 });
  expect((await mis(supportingCourse)).placed).toBe(0);
});
