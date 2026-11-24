import { readFileSync } from 'node:fs';
import { expect, test } from './support/browser.mjs';
import { sql, quote } from './support/database.mjs';

const directory = JSON.parse(readFileSync(new URL('../../database/reference/india-lgd.json', import.meta.url), 'utf8'));
const references = '/api/v1/auth/register/reference-data';
const registration = '/api/v1/auth/register/trainee';

function locationIds(stateCode, districtName) {
  const row = sql(`SELECT s.state_id, d.district_id FROM districts d JOIN states s USING (state_id)
    WHERE s.state_code=${quote(stateCode)} AND d.district_name=${quote(districtName)} AND d.lgd_code IS NOT NULL;`);
  const [state, district] = row.split('\t');
  expect(state).toMatch(/^\d+$/);
  expect(district).toMatch(/^\d+$/);
  return { state, district };
}

test('nationwide directory populates registration and persists the selected union territory and district', async ({ page, request, smoke }) => {
  const [response] = await Promise.all([smoke.waitApi('GET', references), page.goto('/register')]);
  expect(response.status()).toBe(200);
  const data = (await response.json()).data;
  const persistedStates = new Map(sql('SELECT lgd_code,state_id FROM states WHERE lgd_code IS NOT NULL;')
    .split('\n').map((row) => row.split('\t')));
  const persistedDistricts = new Map(sql('SELECT lgd_code,district_id FROM districts WHERE lgd_code IS NOT NULL;')
    .split('\n').map((row) => row.split('\t')));
  const referenceDistrictParents = new Map(data.districts.map(({ id, parentId }) => [String(id), String(parentId)]));
  expect(data.states.map(({ id }) => String(id))).toEqual(expect.arrayContaining(directory.states.map(({ lgdCode }) => persistedStates.get(lgdCode))));
  expect(directory.districts.every(({ lgdCode, stateLgdCode }) =>
    persistedDistricts.has(lgdCode) && referenceDistrictParents.get(persistedDistricts.get(lgdCode)) === persistedStates.get(stateLgdCode))).toBe(true);

  const { run } = smoke;
  const pune = locationIds('MH', 'Pune');
  const karaikal = locationIds('PY', 'Karaikal');
  const state = page.locator('#trainee-state');
  const district = page.locator('#trainee-district');
  await expect(state).toHaveValue('');
  await expect(district).toBeDisabled();
  await expect(state.locator('option')).toHaveCount(directory.states.length + 1);
  await state.selectOption(pune.state);
  await district.selectOption(pune.district);
  await state.selectOption(karaikal.state);
  await expect(district).toHaveValue('');
  await expect(district.getByRole('option', { name: 'Pune', exact: true })).toHaveCount(0);
  await district.selectOption(karaikal.district);
  await page.locator('#trainee-username').fill(run.users.registered);
  await page.locator('#trainee-email').fill(run.users.registered + '@example.invalid');
  await page.locator('#trainee-password').fill(run.password);
  await page.locator('#trainee-confirmPassword').fill(run.password);
  await page.locator('#trainee-firstName').fill('Smoke');
  const gender = page.locator('#trainee-gender');
  const pincode = page.locator('#trainee-pincode');
  const consent = page.locator('#trainee-processing-consent');
  const submit = page.getByRole('button', { name: 'Complete Candidate Registration' });
  await expect(gender).toHaveValue('');
  await expect(gender).toHaveJSProperty('required', true);
  await gender.selectOption(String(data.genders.find(({ code }) => code === 'PREFER_NOT_TO_SAY').id));
  await submit.click();
  await expect(pincode).toBeFocused();
  await pincode.fill('609602');
  await expect(consent).not.toBeChecked();
  await submit.click();
  await expect(consent).toBeFocused();
  await consent.check();
  const [created] = await Promise.all([smoke.waitApi('POST', registration), submit.click()]);
  expect(created.status()).toBe(201);
  expect((await created.json()).data.username).toBe(run.users.registered);
  const payload = created.request().postDataJSON();
  expect(payload).toMatchObject({ dataProcessingConsent: true, pincode: '609602' });
  for (const optional of ['confirmPassword', 'middleName', 'lastName', 'dateOfBirth', 'phone', 'addressLine1',
    'currentEmploymentStatusId']) expect(payload).not.toHaveProperty(optional);

  expect(sql(`SELECT u.state_id,u.district_id,t.state_id,t.district_id,e.status_code,
      t.date_of_birth IS NULL,t.phone IS NULL,t.address_line1 IS NULL,t.pincode
    FROM users u JOIN trainees t USING (user_id) JOIN ref_employment_status e ON e.employment_status_id=t.current_employment_status_id
    WHERE u.username=${quote(run.users.registered)};`)).toBe(
    [karaikal.state, karaikal.district, karaikal.state, karaikal.district, 'UNKNOWN', 1, 1, 1, '609602'].join('\t'));
  expect(sql(`SELECT ct.consent_code,cs.status_code,c.policy_version,c.captured_channel,
      c.captured_by_user_id=u.user_id,c.granted_at IS NOT NULL
    FROM trainee_consents c JOIN trainees t USING (trainee_id) JOIN users u USING (user_id)
    JOIN ref_consent_type ct USING (consent_type_id) JOIN ref_consent_status cs USING (consent_status_id)
    WHERE u.username=${quote(run.users.registered)};`)).toBe(['DATA_PROCESSING', 'GRANTED', 'v1.0', 'WEB_PORTAL', 1, 1].join('\t'));

  const invalid = { ...payload, username: run.users.invalid_registration, email: run.users.invalid_registration + '@example.invalid' };
  for (const invalidPin of [undefined, '012345']) {
    const refused = await request.post(registration, { data: { ...invalid, pincode: invalidPin } });
    expect(refused.status()).toBe(400);
    const error = await refused.json();
    expect(error.code).toBe('VALIDATION_ERROR');
    expect(JSON.stringify(error.details ?? error.fieldErrors ?? error)).toContain('pincode');
  }
  for (const dataProcessingConsent of [undefined, false]) {
    const refused = await request.post(registration, { data: { ...invalid, dataProcessingConsent } });
    expect(refused.status()).toBe(400);
    const error = await refused.json();
    expect(error.code).toBe('VALIDATION_ERROR');
    expect(JSON.stringify(error.details ?? error.fieldErrors ?? error)).toContain('dataProcessingConsent');
  }
  const wrong = await request.post(registration, { data: { ...invalid, districtId: Number(pune.district) } });
  expect(wrong.status()).toBe(400);
  expect(await wrong.json()).toMatchObject({ code: 'INVALID_STATE_DISTRICT' });
  expect(sql(`SELECT COUNT(*) FROM users WHERE username=${quote(run.users.invalid_registration)};`)).toBe('0');
});

test('nationwide directory filters administrator provisioning and rejects a forged jurisdiction pair', async ({ page, request, smoke }) => {
  const pune = locationIds('MH', 'Pune');
  const karaikal = locationIds('PY', 'Karaikal');
  const user = await smoke.signIn('admin', '/admin/users');
  await page.getByRole('button', { name: /provision.*(?:account|user)/i }).click();
  const state = page.locator('#assessor-state');
  const district = page.locator('#assessor-district');
  await expect(state.locator('option')).toHaveCount(directory.states.length + 1);
  await expect(state).toHaveValue('');
  await state.selectOption(pune.state);
  await district.selectOption(pune.district);
  await state.selectOption(karaikal.state);
  await expect(district).toHaveValue('');
  await expect(district.getByRole('option', { name: 'Karaikal', exact: true })).toHaveCount(1);
  await expect(district.getByRole('option', { name: 'Pune', exact: true })).toHaveCount(0);
  const username = smoke.run.users.invalid_provisioning;
  const invalid = await request.post('/api/v1/admin/users/provision-assessor', {
    headers: { authorization: 'Bearer ' + user.accessToken }, data: {
      username, email: username + '@example.invalid', password: smoke.run.password,
      stateId: Number(karaikal.state), districtId: Number(pune.district),
    },
  });
  expect(invalid.status()).toBe(400);
  expect(await invalid.json()).toMatchObject({ code: 'INVALID_STATE_DISTRICT' });
});

test('nationwide directory supports MIS filters and district drilldown outside Delhi', async ({ page, smoke }) => {
  const pune = locationIds('MH', 'Pune');
  const karaikal = locationIds('PY', 'Karaikal');
  await smoke.signIn('admin', '/analytics');
  const state = page.locator('#mis-filter-stateId');
  const district = page.locator('#mis-filter-districtId');
  await expect(state.locator('option')).toHaveCount(directory.states.length + 1);
  await expect(district.getByRole('option', { name: 'Pune · Maharashtra', exact: true })).toHaveCount(1);
  await state.selectOption(pune.state);
  await district.selectOption(pune.district);
  await state.selectOption(karaikal.state);
  await expect(district).toHaveValue('');
  await expect(district.getByRole('option', { name: 'Karaikal', exact: true })).toHaveCount(1);
  await expect(district.getByRole('option', { name: 'Pune', exact: true })).toHaveCount(0);
  await state.selectOption(pune.state);
  const mapState = page.getByRole('button', { name: /^Maharashtra · (No data|Suppressed|\d)/ });
  await expect(mapState).toBeVisible();
  const [response] = await Promise.all([smoke.waitApi('GET', '/api/v1/dashboards/states/' + pune.state), mapState.press('Enter')]);
  const expected = directory.districts.filter(({ stateLgdCode }) => stateLgdCode === '27').length;
  expect(response.status()).toBe(200);
  expect((await response.json()).data.districts).toHaveLength(expected);
  const drilldown = page.getByRole('dialog', { name: 'Maharashtra district outcomes' });
  await expect(drilldown.getByRole('rowheader', { name: 'Pune', exact: true })).toBeVisible();
  await expect(drilldown.getByText(`Showing 1–12 of ${expected}`)).toBeVisible();
  await drilldown.getByRole('button', { name: 'Next', exact: true }).click();
  await expect(drilldown.getByText(`Showing 13–24 of ${expected}`)).toBeVisible();
});

test('nationwide directory supplies work districts for employer vacancies', async ({ page, smoke }) => {
  const [response] = await Promise.all([smoke.waitApi('GET', '/api/v1/employer/reference-data'),
    smoke.signIn('employer', '/employer/vacancies')]);
  expect(response.status()).toBe(200);
  const { districts } = (await response.json()).data;
  expect(districts.length).toBeGreaterThanOrEqual(directory.districts.length);
  await page.getByRole('button', { name: 'New vacancy' }).click();
  const workDistrict = page.getByRole('combobox', { name: /Work district/ });
  await expect(workDistrict.getByRole('option', { name: 'Pune · Maharashtra', exact: true })).toHaveCount(1);
  await expect(workDistrict.getByRole('option', { name: 'Karaikal · Puducherry', exact: true })).toHaveCount(1);
});
