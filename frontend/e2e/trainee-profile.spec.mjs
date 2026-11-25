import { expect, test } from './support/browser.mjs';
import { quote, sql } from './support/database.mjs';

const registration = '/api/v1/auth/register/trainee';
const own = '/api/v1/trainees/me/profile';
const path = '/trainee/profile';
const auth = token => ({ Authorization: 'Bearer ' + token });

async function data(response, status) {
  const json = await response.json();
  expect(response.status(), json.message).toBe(status);
  return json.data;
}

function district(state, name) {
  const [stateId, districtId] = sql(`SELECT s.state_id,d.district_id FROM states s JOIN districts d USING(state_id)
    WHERE s.state_code=${quote(state)} AND d.district_name=${quote(name)} AND d.lgd_code IS NOT NULL;`).split('\t');
  expect(stateId).toMatch(/^\d+$/);
  expect(districtId).toMatch(/^\d+$/);
  return { stateId, districtId };
}

test('a newly registered trainee completes their own profile, edits residence and keeps their account identity', async ({ page, request, smoke }) => {
  const { run } = smoke;
  const username = run.users.profile_registration;
  const loginEmail = username + '@example.invalid';
  const contactEmail = username + '.contact@example.invalid';
  const pune = district('MH', 'Pune');
  const karaikal = district('PY', 'Karaikal');

  await page.goto('/register');
  await page.locator('#trainee-username').fill(username);
  await page.locator('#trainee-email').fill(loginEmail);
  await page.locator('#trainee-password').fill(run.password);
  await page.locator('#trainee-confirmPassword').fill(run.password);
  await page.locator('#trainee-firstName').fill('Aarav');
  await page.locator('#trainee-gender').selectOption({ index: 1 });
  await page.locator('#trainee-state').selectOption(pune.stateId);
  await page.locator('#trainee-district').selectOption(pune.districtId);
  await page.locator('#trainee-pincode').fill('411001');
  await page.locator('#trainee-processing-consent').check();
  const [created] = await Promise.all([smoke.waitApi('POST', registration),
    page.getByRole('button', { name: 'Complete Candidate Registration' }).click()]);
  const account = await data(created, 201);
  const [opened] = await Promise.all([smoke.waitApi('GET', own), page.getByRole('link', { name: 'Edit profile', exact: true }).click()]);
  const original = await data(opened, 200);
  await expect(page.getByRole('heading', { name: 'My Profile', exact: true })).toBeVisible();
  await expect(page.getByText('No training enrollments recorded.')).toBeVisible();
  await expect(page.getByLabel('Last name', { exact: true })).toHaveValue('');
  await expect(page.getByLabel('Education level', { exact: true })).toHaveValue('');
  await expect(page.getByText(original.profile.registrationNumber, { exact: true })).toBeVisible();

  const education = original.educationLevels.at(-1);
  expect(education.id).toBeGreaterThan(0);
  await page.getByLabel('First name *').fill('Aarav Updated');
  await page.getByLabel('Middle name', { exact: true }).fill('Kumar');
  await page.getByLabel('Last name', { exact: true }).fill('Sharma');
  await page.getByLabel('Date of birth', { exact: true }).fill('2003-04-12');
  await page.getByLabel('Education level', { exact: true }).selectOption(String(education.id));
  await page.getByRole('textbox', { name: /^Contact email/ }).fill(contactEmail);
  await page.getByLabel('Phone number', { exact: true }).fill('+919812345678');
  await page.getByLabel('Address line 1', { exact: true }).fill('42 Training Lane');
  await page.getByLabel('Address line 2', { exact: true }).fill('Second floor');
  await page.getByLabel('State or union territory *').selectOption(karaikal.stateId);
  const districtField = page.getByLabel('District *', { exact: true });
  await expect(districtField).toHaveValue('');
  await expect(districtField.getByRole('option', { name: 'Pune', exact: true })).toHaveCount(0);
  await districtField.selectOption(karaikal.districtId);
  const pin = page.getByRole('textbox', { name: /^PIN code/ });
  const save = page.getByRole('button', { name: 'Save profile', exact: true });
  await pin.fill('');
  await save.click();
  await expect(pin).toBeFocused();
  await pin.fill('609602');
  const [saved] = await Promise.all([smoke.waitApi('PUT', own), save.click()]);
  const updated = await data(saved, 200);
  const payload = saved.request().postDataJSON();
  expect(updated.profile).toMatchObject({
    id: original.profile.id, registrationNumber: original.profile.registrationNumber,
    currentEmploymentStatusId: original.profile.currentEmploymentStatusId, profileStatusId: original.profile.profileStatusId,
    firstName: 'Aarav Updated', phone: '+919812345678', email: contactEmail, educationLevelId: education.id,
    stateId: Number(karaikal.stateId), districtId: Number(karaikal.districtId), pincode: '609602',
  });
  await expect(page.getByText('Profile saved.', { exact: true })).toBeVisible();
  await expect(page.getByText(loginEmail, { exact: true })).toBeVisible();
  expect(sql(`SELECT u.email,u.state_id,u.district_id,t.email,t.state_id,t.district_id,t.date_of_birth,
      t.address_line2,t.education_level_id,t.pincode
    FROM trainees t JOIN users u USING(user_id) WHERE t.trainee_id=${updated.profile.id};`)).toBe([
    loginEmail, pune.stateId, pune.districtId, contactEmail, karaikal.stateId, karaikal.districtId, '2003-04-12',
    'Second floor', education.id, '609602',
  ].join('\t'));
  const audit = sql(`SELECT change_summary FROM audit_logs WHERE entity_type='Trainee' AND entity_id=${updated.profile.id}
    AND actor_user_id=${account.userId} AND change_summary LIKE '@audit-state-v1 %' ORDER BY audit_log_id DESC LIMIT 1;`);
  expect(audit).toMatch(/^@audit-state-v1 /);
  const snapshot = JSON.parse(audit.slice('@audit-state-v1 '.length));
  expect(snapshot).toMatchObject({ summary: 'Candidate profile updated', after: { educationLevelId: education.id } });
  expect(snapshot.redactedFields).toEqual(expect.arrayContaining(['email', 'phone', 'dateOfBirth', 'addressLine1']));
  expect(audit).not.toContain(contactEmail);
  expect(audit).not.toContain('+919812345678');

  const signedIn = await request.post('/api/v1/auth/login', {
    data: { usernameOrEmail: loginEmail, password: run.password },
  });
  const headers = auth((await data(signedIn, 200)).accessToken);
  for (const [change, code] of [
    [{ stateId: Number(pune.stateId) }, 'INVALID_STATE_DISTRICT'],
    [{ educationLevelId: 2147483647 }, 'INVALID_EDUCATION_LEVEL'],
    [{ dateOfBirth: '2099-01-01' }, 'VALIDATION_ERROR'],
    [{ pincode: '000001' }, 'VALIDATION_ERROR'],
  ]) {
    const denied = await request.put(own, { headers, data: { ...payload, ...change } });
    expect(denied.status()).toBe(400);
    expect((await denied.json()).code).toBe(code);
  }
  const forged = await request.put(own, { headers, data: {
    ...payload, id: run.trainee, userId: account.userId, profileStatusId: 1, currentEmploymentStatusId: 1,
  } });
  expect(forged.status()).toBe(400);
  const crossRead = await request.get('/api/v1/trainees/' + run.trainee, { headers });
  const crossWrite = await request.put('/api/v1/trainees/' + run.trainee, { headers, data: { firstName: 'Wrong trainee' } });
  expect(crossRead.status()).toBe(400);
  expect((await crossRead.json()).code).toBe('TRAINEE_MISMATCH');
  expect(crossWrite.status()).toBe(403);

  await page.reload();
  await expect(page.getByLabel('Phone number', { exact: true })).toHaveValue('+919812345678');
  await expect(page.getByLabel('Education level', { exact: true })).toHaveValue(String(education.id));
  await expect(districtField).toHaveValue(karaikal.districtId);
  await page.getByLabel('Address line 2', { exact: true }).fill('');
  await page.getByLabel('Education level', { exact: true }).selectOption('');
  const [cleared] = await Promise.all([smoke.waitApi('PUT', own), save.click()]);
  const clearedProfile = (await data(cleared, 200)).profile;
  expect(clearedProfile.addressLine2 ?? null).toBeNull();
  expect(clearedProfile.educationLevelId ?? null).toBeNull();
  await page.getByRole('link', { name: 'Overview', exact: true }).click();
  await expect(page.getByText('Aarav Updated Kumar Sharma', { exact: true })).toBeVisible();
});

test('the self-service endpoint is trainee-only and existing trainees see their training record', async ({ page, request, smoke }) => {
  const { run } = smoke;
  const login = await request.post('/api/v1/auth/login', {
    data: { usernameOrEmail: run.users.provider, password: run.password },
  });
  const provider = auth((await data(login, 200)).accessToken);
  expect((await request.get(own, { headers: provider })).status()).toBe(403);
  const validProfile = { firstName: 'Example', genderId: 1, pincode: '110001', stateId: run.state, districtId: run.district };
  expect((await request.put(own, { headers: provider, data: validProfile })).status()).toBe(403);

  await smoke.signIn('trainee', path);
  const history = page.getByRole('region', { name: 'Training history', exact: true });
  await expect(history.getByText('Fixture training course ' + run.id, { exact: true })).toBeVisible();
  await expect(history.getByRole('link', { name: 'Manage consent' })).toHaveAttribute('href', '/trainee/consent');
});

test('a trainee can withdraw a recent grant and independently manage privacy permissions', async ({ page, request, smoke }) => {
  const { run } = smoke;
  const consentPath = '/api/v1/consents';
  const coreId = sql(`SELECT trainee_consent_id FROM trainee_consents WHERE trainee_id=${run.trainee} AND consent_type_id=1
    ORDER BY trainee_consent_id DESC LIMIT 1;`);
  sql(`UPDATE trainee_consents SET granted_at=NOW(), updated_at=NOW() WHERE trainee_consent_id=${coreId};`);
  const session = await smoke.signIn('trainee', '/trainee/consent');
  const headers = auth(session.accessToken);
  const coreStatus = consentPath + '/' + coreId + '/status';
  const beforeGrant = await request.put(coreStatus, {
    headers, data: { consentStatusId: 2, revokedAt: '2000-01-01T00:00:00' },
  });
  expect(await data(beforeGrant, 200)).toMatchObject({ id: Number(coreId), consentStatusCode: 'REVOKED' });
  await page.reload();

  const core = page.getByRole('group', { name: 'Core Scheme Data Processing' });
  await expect(core.getByText('○ Revoked')).toBeVisible();
  const grantCore = core.getByRole('button', { name: 'Grant Consent' });
  await expect(grantCore).toHaveCSS('background-color', 'rgb(26, 60, 110)');
  await expect(grantCore).toHaveCSS('color', 'rgb(255, 255, 255)');
  const [regranted] = await Promise.all([smoke.waitApi('POST', consentPath),
    grantCore.click()]);
  const latest = await data(regranted, 201);
  expect(latest).toMatchObject({ traineeId: run.trainee, consentStatusCode: 'GRANTED', capturedByUserId: session.userId });
  expect(latest.id).not.toBe(Number(coreId));
  await expect(core.getByRole('button', { name: 'Revoke Consent' })).toBeVisible();
  const stale = await request.put(coreStatus, { headers, data: { consentStatusId: 2 } });
  expect(stale.status()).toBe(409);
  expect((await stale.json()).code).toBe('CONSENT_CHANGED');

  const phone = page.getByRole('group', { name: 'Phone Follow-Up' });
  await expect(page.getByRole('group', { name: 'Outcome Analytics Export' }).getByText('○ Not Granted')).toBeVisible();
  await page.setViewportSize({ width: 390, height: 844 });
  const grantPhone = phone.getByRole('button', { name: 'Grant Consent' });
  await expect(grantPhone).toHaveCSS('background-color', 'rgb(26, 60, 110)');
  await expect(grantPhone).toHaveCSS('min-height', '44px');
  const [granted] = await Promise.all([smoke.waitApi('POST', consentPath),
    grantPhone.click()]);
  const choice = await data(granted, 201);
  expect(choice).toMatchObject({ consentTypeCode: 'COMMUNICATION_PHONE', consentStatusCode: 'GRANTED' });
  await expect(phone.getByText('● Granted', { exact: true })).toBeVisible();
  await expect(phone.getByRole('status')).toHaveText('Consent granted.');
  const [revoked] = await Promise.all([smoke.waitApi('PUT', consentPath + '/' + choice.id + '/status'),
    phone.getByRole('button', { name: 'Revoke Consent' }).click()]);
  expect(await data(revoked, 200)).toMatchObject({ consentStatusCode: 'REVOKED' });
  await expect(phone.getByText('○ Revoked')).toBeVisible();
  await expect(phone.getByRole('status')).toHaveText('Consent withdrawn.');
  await page.reload();
  await expect(phone.getByText('○ Revoked')).toBeVisible();
  await expect(core.getByText('● Granted', { exact: true })).toBeVisible();

  const otherId = sql(`SELECT trainee_consent_id FROM trainee_consents WHERE trainee_id=${run.prospect} LIMIT 1;`);
  const denied = await request.put(consentPath + '/' + otherId + '/status', { headers, data: { consentStatusId: 2 } });
  expect(denied.ok()).toBe(false);
  expect(sql(`SELECT cs.status_code, c.revoked_at >= c.granted_at FROM trainee_consents c
    JOIN ref_consent_status cs USING(consent_status_id) WHERE c.trainee_consent_id IN (${coreId}, ${choice.id}) ORDER BY c.trainee_consent_id;`))
    .toBe('REVOKED\t1\nREVOKED\t1');
});
