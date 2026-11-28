import { expect, test } from './support/browser.mjs';
import { sql } from './support/database.mjs';

test('the pipeline and all three map metrics match the selected training records', async ({ page, request, smoke }) => {
  const { run } = smoke;
  const enrolled = Number(sql(`SELECT enrollment_id FROM training_enrollments WHERE enrollment_number='${run.id}-assess-enroll';`));
  const placement = `${run.id}-mis-joined`;
  sql(`START TRANSACTION;
    INSERT INTO trainee_assessments(assessment_id,enrollment_id,trainee_id,attempt_number,appeared_flag,assessment_date)
      VALUES (${run.assessment},${enrolled},${run.trainee},1,1,CURDATE()+INTERVAL 1 DAY);
    INSERT INTO placement_records(placement_number,trainee_id,enrollment_id,course_id,program_id,provider_id,
        employer_id,engagement_type_id,placement_source_id,placement_status_id,joining_status_id,actual_joining_date,
        record_verification_status_id)
      VALUES ('${placement}',${run.trainee},${enrolled},${run.course},${run.program},${run.provider},${run.employer},1,1,4,3,CURDATE(),1);
    COMMIT;`);

  try {
    const { accessToken } = await smoke.signIn('admin', `/analytics?stateId=${run.state}&districtId=${run.district}`);
    const headers = { Authorization: 'Bearer ' + accessToken };
    const cohort = { stateId: run.state, districtId: run.district };
    const get = async (path, params = cohort) => {
      const response = await request.get('/api/v1/dashboards/' + path, { headers, params });
      expect(response.status(), path).toBe(200);
      return (await response.json()).data;
    };
    const stage = async () => (await get('pipeline-funnel', { ...cohort, executive: true })).candidateStages
      .find(row => row.key === 'assessed').candidates.value;
    const funnel = page.getByRole('list', { name: 'Candidate lifecycle stages' });
    await expect(funnel.getByRole('button', { name: 'Assessed 0', exact: true })).toBeVisible();
    await expect(funnel.getByRole('button', { name: 'Placed 0', exact: true })).toBeVisible();
    sql(`UPDATE trainee_assessments SET assessment_date=CURDATE() WHERE assessment_id=${run.assessment} AND enrollment_id=${enrolled};`);
    expect(await stage()).toBe(1);
    sql(`UPDATE assessments SET deleted_at=NOW() WHERE assessment_id=${run.assessment};`);
    expect(await stage()).toBe(0);

    const map = (await get('geography')).stateSummaries[0];
    expect(map).toMatchObject({ enrollmentCount: 1, certifiedCount: 0, joinedPlacementCount: 1, employedCount: 1,
      certificationRatePct: 0, placementRatePct: 100, employmentRatePct: 100 });
    const region = page.getByRole('region', { name: /State and district outcomes/ });
    for (const [metric, rate, color] of [
      ['certificationRatePct', '0%', '#e0f2fe'], ['placementRatePct', '100%', '#0369a1'],
      ['employmentRatePct', '100%', '#0369a1'],
    ]) {
      await region.getByLabel('Map metric').selectOption(metric);
      const shape = region.locator('svg [role="button"]');
      await expect(shape).toHaveAttribute('aria-label', new RegExp(` · ${rate}$`));
      await expect(shape).toHaveAttribute('fill', color);
    }
    await region.locator('svg [role="button"]').click();
    const district = page.getByRole('dialog').getByRole('table').getByRole('row').filter({ hasText: 'Smoke district' });
    await expect(district.getByRole('cell')).toHaveText(['1', '0%', '100%', '100%']);

    sql(`UPDATE districts SET deleted_at=NOW() WHERE district_id=${run.district};`);
    const expected = Number(sql(`SELECT COUNT(*) FROM vw_enrollment_outcome_fact WHERE trainee_state_id=${run.state};`));
    const stateOnly = (await get('geography', { stateId: run.state })).stateSummaries[0];
    expect(stateOnly.enrollmentCount).toBe(expected);
    const districts = (await get('states/' + run.state, { stateId: run.state })).districts;
    expect(districts.find(row => row.districtId === run.district)).toBeUndefined();
  } finally {
    sql(`START TRANSACTION;
      UPDATE districts SET deleted_at=NULL WHERE district_id=${run.district};
      UPDATE assessments SET deleted_at=NULL WHERE assessment_id=${run.assessment};
      DELETE FROM trainee_assessments WHERE assessment_id=${run.assessment} AND enrollment_id=${enrolled};
      DELETE FROM placement_records WHERE placement_number='${placement}' AND enrollment_id=${enrolled};
      COMMIT;`);
  }
});

test('issued credentials count once per trainee across certification reports', async ({ page, request, smoke }) => {
  const { run } = smoke;
  const duplicateBatch = `${run.id}-venue-dedup`;
  const repeat = `${run.id}-repeat-cert`;
  const pending = `${run.id}-pending-cert`;
  const prefix = `${run.id}-mis-cert-`;
  sql(`START TRANSACTION;
    INSERT INTO training_batches(batch_code,course_id,provider_id,center_id,program_id,start_date,end_date,capacity,batch_status_id)
      SELECT '${duplicateBatch}',course_id,provider_id,center_id,program_id,start_date,end_date,capacity,batch_status_id
      FROM training_batches WHERE batch_code='${run.id}-assess-batch';
    SET @repeated_batch=LAST_INSERT_ID();
    INSERT INTO training_enrollments(enrollment_number,trainee_id,course_id,provider_id,center_id,batch_id,program_id,
        enrollment_status_id,enrollment_date,start_date)
      SELECT '${repeat}',trainee_id,course_id,provider_id,center_id,@repeated_batch,program_id,enrollment_status_id,enrollment_date,start_date
      FROM training_enrollments WHERE enrollment_number='${run.id}-assess-enroll'
      UNION ALL
      SELECT '${pending}',${run.prospect},course_id,provider_id,center_id,batch_id,program_id,enrollment_status_id,enrollment_date,start_date
      FROM training_enrollments WHERE enrollment_number='${run.id}-assess-enroll';
    INSERT INTO certifications(certificate_number,trainee_id,enrollment_id,course_id,program_id,issuing_body,issue_date,
        certificate_status_id,certificate_verification_status_id)
      SELECT CONCAT('${prefix}',enrollment_id),trainee_id,enrollment_id,course_id,program_id,'Fixture awarding body',CURDATE(),
        IF(trainee_id=${run.trainee},1,3),1
      FROM training_enrollments WHERE enrollment_number IN ('${run.id}-assess-enroll','${repeat}','${pending}');
    COMMIT;`);

  try {
    const [schemeId, fiscalYear] = sql(`SELECT p.scheme_id,CONCAT('FY',YEAR(DATE_SUB(e.enrollment_date,INTERVAL 3 MONTH)),'-',
        RIGHT(YEAR(DATE_SUB(e.enrollment_date,INTERVAL 3 MONTH))+1,2))
      FROM training_enrollments e JOIN programs p USING(program_id) WHERE enrollment_number='${repeat}';`).split('\t');
    const params = { stateId: String(run.state), districtId: String(run.district), schemeId, fiscalYear };
    const { accessToken } = await smoke.signIn('admin', '/analytics?' + new URLSearchParams(params));
    const certified = page.getByRole('listitem').filter({ has: page.getByRole('heading', { name: 'Total certified' }) });
    await expect(certified.getByText('1', { exact: true })).toBeVisible();
    const funnel = page.getByRole('list', { name: 'Candidate lifecycle stages' });
    await expect(funnel.getByRole('button', { name: 'Assessed 1', exact: true })).toBeVisible();
    await expect(funnel.getByRole('button', { name: 'Certified 1', exact: true })).toBeVisible();
    await page.getByLabel('Map metric').selectOption('certificationRatePct');
    const region = page.getByRole('region', { name: /State and district outcomes/ });
    const shape = region.getByRole('button', { name: / · 50%$/ });
    await expect(shape).toBeVisible();
    await shape.click();
    await expect(page.getByRole('dialog').getByRole('cell', { name: '50%', exact: true })).toBeVisible();

    const programs = await request.get('/api/v1/dashboards/scheme-matrix', {
      params, headers: { Authorization: 'Bearer ' + accessToken },
    });
    expect(programs.status()).toBe(200);
    expect((await programs.json()).data.items).toEqual(expect.arrayContaining([
      expect.objectContaining({ programId: run.program, enrollmentCount: 3, traineeCount: 2,
        certifiedCount: 1, certificationRatePct: 50 }),
    ]));
  } finally {
    sql(`START TRANSACTION;
      DELETE FROM certifications WHERE LEFT(certificate_number,LENGTH('${prefix}'))='${prefix}';
      DELETE FROM training_enrollments WHERE enrollment_number IN ('${repeat}','${pending}');
      DELETE FROM training_batches WHERE batch_code='${duplicateBatch}';
      COMMIT;`);
  }
});
