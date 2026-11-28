import { expect, test } from './support/browser.mjs';
import { sql } from './support/database.mjs';

const api = '/api/v1/dashboards';
const headline = '/kpis?executive=true';
const comparisonReports = ['/overview', '/kpis', '/pipeline-funnel', '/scheme-matrix', '/providers', '/courses',
  '/employers', '/economic-outcomes', '/mis/provider-quality'];

for (const role of ['stateadmin', 'district']) {
  test(`${role} sees exact MIS outcomes within their assigned geography`, async ({ page, request, smoke }) => {
    const { run } = smoke;
    const districtOfficer = role === 'district';
    const [otherState, otherDistrict] = sql(`
      SELECT (SELECT MIN(state_id) FROM states WHERE state_id<>${run.state} AND deleted_at IS NULL),
             (SELECT MIN(district_id) FROM districts WHERE state_id=${run.state}
                AND district_id<>${run.district} AND deleted_at IS NULL);
    `).split('\t').map(Number);
    expect(otherState).toBeGreaterThan(0);
    expect(otherDistrict).toBeGreaterThan(0);
    const districtPredicate = districtOfficer ? ` AND trainee_district_id=${run.district}` : '';
    const exactEnrollmentCount = Number(sql(`SELECT COUNT(*) FROM vw_enrollment_outcome_fact
      WHERE trainee_state_id=${run.state}${districtPredicate};`));

    const { accessToken } = await smoke.signIn(role, `/analytics?stateId=${otherState}`);
    const state = page.getByLabel('State', { exact: true });
    const district = page.getByLabel('District', { exact: true });
    await expect(page.getByRole('link', { name: 'Government MIS' })).toBeVisible();
    await expect(state).toHaveValue(String(run.state));
    await expect(state).toBeDisabled();
    await expect(state.locator('option')).toHaveCount(1);
    if (districtOfficer) {
      await expect(district).toHaveValue(String(run.district));
      await expect(district).toBeDisabled();
      await expect(district.locator('option')).toHaveCount(1);
    } else {
      await expect(district).toBeEnabled();
      await expect(district.locator(`option[value="${otherDistrict}"]`)).toHaveCount(1);
    }
    const card = page.getByRole('listitem').filter({ has: page.getByRole('heading', { name: 'Total enrolled' }) });
    await expect(card.getByText(new Intl.NumberFormat('en-IN').format(exactEnrollmentCount), { exact: true })).toBeVisible();

    const headers = { Authorization: 'Bearer ' + accessToken };
    const get = (path, params) => request.get(api + path, { headers, params });
    const [kpis, geography, detail, ...comparisons] = await Promise.all([
      get(headline), get('/geography'), get('/states/' + run.state), ...comparisonReports.map(path => get(path)),
    ]);
    for (const response of [kpis, geography, detail, ...comparisons]) expect(response.status(), response.url()).toBe(200);
    const enrolled = (await kpis.json()).data.executiveKpis.kpis.find(({ key }) => key === 'enrolled');
    expect(enrolled.current).toEqual({ value: exactEnrollmentCount, availability: 'RELEASED' });
    const map = (await geography.json()).data;
    expect(map.stateSummaries.map(({ stateId }) => stateId)).toEqual([run.state]);
    const rows = (await detail.json()).data.districts;
    if (districtOfficer) {
      expect(rows.map(({ districtId }) => districtId)).toEqual([run.district]);
      expect(map.totalDistricts).toBe(1);
      expect(rows[0].enrollmentCount).toBe(exactEnrollmentCount);
      const economy = comparisons.find(response => new URL(response.url()).pathname.endsWith('/economic-outcomes'));
      expect((await economy.json()).data.employmentMix.wageSpellCount).toBeGreaterThan(0);
    } else {
      expect(rows.map(({ districtId }) => districtId)).toContain(otherDistrict);
    }

    const deniedPaths = [headline, '/pipeline-funnel?executive=true', '/geography', ...comparisonReports];
    await Promise.all(deniedPaths.map(async path => {
      expect((await get(path, { stateId: otherState })).status(), path).toBe(403);
    }));
    expect((await get('/states/' + otherState)).status()).toBe(403);
    if (districtOfficer) {
      expect((await get(headline, { districtId: otherDistrict })).status()).toBe(403);
      expect((await get('/states/' + run.state, { districtId: otherDistrict })).status()).toBe(403);
      expect((await get('/districts/' + otherDistrict)).status()).toBe(403);
    }

    await page.getByRole('button', { name: 'Programs and providers' }).click();
    await expect(page.getByRole('heading', { name: 'Training provider leaderboard' })).toBeVisible();
    await page.getByRole('button', { name: 'Employment', exact: true }).click();
    await expect(page.getByRole('heading', { name: 'Employment mix' })).toBeVisible();
    await page.getByRole('button', { name: 'Wages', exact: true }).click();
    await expect(page.getByRole('heading', { name: 'Wage progression', exact: false }).first()).toBeVisible();
    await expect(page.getByText('Suppressed', { exact: true })).toHaveCount(0);
  });
}

test('one scoped monthly wage appears in Government MIS', async ({ page, smoke }) => {
  const { run } = smoke;
  const historyId = Number(sql(`
    INSERT INTO salary_history(employment_id, trainee_id, salary_amount, salary_frequency_id,
      effective_from, employment_info_source_id, record_verification_status_id)
    SELECT employment_id, trainee_id, 18500, salary_frequency_id, start_date,
      employment_info_source_id, record_verification_status_id
    FROM employment_records WHERE employment_number='${run.id}-employment';
    SELECT LAST_INSERT_ID();
  `));
  expect(historyId).toBeGreaterThan(0);
  try {
    const [report] = await Promise.all([smoke.waitApi('GET', api + '/economic-outcomes'),
      smoke.signIn('district', '/analytics?view=wages')]);
    expect(report.status()).toBe(200);
    expect((await report.json()).data.wageProgression).toMatchObject({
      firstSalaryCount: 1, latestSalaryCount: 1, avgFirstRecordedSalary: 18500, avgLatestRecordedSalary: 18500,
    });
    await expect(page.getByText('₹18,500', { exact: true }).first()).toBeVisible();
  } finally {
    sql(`DELETE FROM salary_history WHERE salary_history_id=${historyId} AND trainee_id=${run.trainee};`);
  }
});
