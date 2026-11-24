import { expect, test } from './support/browser.mjs';
import { quote, sql } from './support/database.mjs';

const register = '/api/v1/auth/register';
const onboarding = '/api/v1/admin/onboarding/applications';
const login = '/api/v1/auth/login';

const applications = [
  {
    type: 'TRAINING_PROVIDER', username: 'provider_registration', prefix: 'provider', state: 'MH', district: 'Pune',
    tab: 'Training Provider', nameField: 'provider-name', contactField: 'provider-contact-person',
    submit: 'Submit Training Provider Application', path: 'training-provider', table: 'training_providers',
    key: 'provider_id', userKey: 'training_provider_id', extraColumns: 'org.registration_number,org.pincode',
    portal: (id) => '/api/v1/training-batches?providerId=' + id,
  },
  {
    type: 'EMPLOYER', username: 'employer_registration', prefix: 'emp', state: 'PY', district: 'Karaikal',
    tab: 'Employer / Industry', nameField: 'emp-company-name', contactField: 'emp-contact-person',
    submit: 'Submit Employer Application', path: 'employer', table: 'employers',
    key: 'employer_id', userKey: 'employer_id', extraColumns: 'org.registration_number IS NULL,org.gstin IS NULL',
    portal: () => '/api/v1/employer/reference-data',
  },
];

for (const application of applications) {
  test(application.type + ' can apply with required information and access its portal after approval', async ({ page, request, smoke }) => {
    const { run, waitApi } = smoke;
    const { prefix, table, key, userKey, type } = application;
    const username = run.users[application.username];
    const email = username + '@example.invalid';
    const organizationName = run.id + '-' + (prefix === 'emp' ? 'employer' : 'provider') + '-registration';
    const [stateId, districtId] = sql(`SELECT s.state_id,d.district_id FROM districts d JOIN states s USING (state_id)
      WHERE s.state_code=${quote(application.state)} AND d.district_name=${quote(application.district)} AND d.lgd_code IS NOT NULL;`).split('\t');
    expect(stateId).toMatch(/^\d+$/);
    expect(districtId).toMatch(/^\d+$/);

    await page.goto('/register');
    await page.getByRole('button', { name: application.tab, exact: true }).click();
    await page.locator('#' + application.nameField).fill(organizationName);
    if (type === 'TRAINING_PROVIDER') await page.locator('#provider-reg-number').fill(run.id + '-nsdc');
    await page.locator('#' + application.contactField).fill('Smoke contact');
    await page.locator('#' + prefix + '-state').selectOption(stateId);
    await page.locator('#' + prefix + '-district').selectOption(districtId);
    await page.locator('#' + prefix + '-username').fill(username);
    await page.locator('#' + prefix + '-email').fill(email);
    await page.locator('#' + prefix + '-password').fill(run.password);
    await page.locator('#' + prefix + '-confirmPassword').fill(run.password);
    const alternate = page.locator('#' + prefix + '-contact-email');
    await expect(alternate).toHaveValue('');
    await expect(alternate).toHaveJSProperty('required', false);

    const submit = page.getByRole('button', { name: application.submit });
    if (type === 'TRAINING_PROVIDER') {
      const pincode = page.locator('#provider-pincode');
      await submit.click();
      await expect(pincode).toBeFocused();
      await pincode.fill('411001');
    }

    const [response] = await Promise.all([waitApi('POST', register + '/' + application.path), submit.click()]);
    expect(response.status()).toBe(202);
    const submitted = response.request().postDataJSON();
    for (const optional of ['contactEmail', 'contactPhone', 'confirmPassword', 'organizationTypeId', 'gstin']) {
      expect(submitted).not.toHaveProperty(optional);
    }
    if (type === 'TRAINING_PROVIDER') {
      expect(submitted.pincode).toBe('411001');
      const refused = await request.post(register + '/' + application.path, { data: { ...submitted, pincode: undefined } });
      expect(refused.status()).toBe(400);
      expect(await refused.json()).toMatchObject({ code: 'VALIDATION_ERROR' });
    } else {
      expect(submitted).not.toHaveProperty('pincode');
    }
    const { data: receipt } = await response.json();
    expect(receipt).toMatchObject({ organizationName, status: 'PENDING', applicantType: type, contactEmail: email });
    await expect(page.getByRole('heading', { name: 'Application Submitted Successfully' })).toBeVisible();
    await expect(page.getByText(receipt.applicationNumber, { exact: true })).toBeVisible();
    const credentials = { usernameOrEmail: username, password: run.password };
    const pendingLogin = await request.post(login, { data: credentials });
    expect(pendingLogin.ok()).toBe(false);
    expect(sql(`SELECT a.status,a.contact_email=u.email,a.contact_phone IS NULL,rt.type_code,
        org.state_id,org.district_id,${application.extraColumns}
      FROM onboarding_applications a JOIN users u ON u.user_id=a.user_id
      JOIN ${table} org ON org.${key}=a.entity_id JOIN ref_organization_type rt USING (organization_type_id)
      WHERE a.application_id=${receipt.applicationId} AND u.username=${quote(username)};`)).toBe(
      ['PENDING', 1, 1, type, stateId, districtId, ...(type === 'TRAINING_PROVIDER' ? [run.id + '-nsdc', '411001'] : [1, 1])].join('\t'));

    await smoke.signIn('admin', '/admin/onboarding');
    const row = page.getByRole('row').filter({ hasText: receipt.applicationNumber });
    await expect(row).toBeVisible();
    page.once('dialog', (dialog) => dialog.accept());
    const [approved] = await Promise.all([waitApi('POST', onboarding + '/' + receipt.applicationId + '/approve'),
      row.getByRole('button', { name: 'Approve', exact: true }).click()]);
    expect(approved.status()).toBe(200);
    expect((await approved.json()).data.status).toBe('APPROVED');
    expect(sql(`SELECT us.status_code,u.organization_id=org.organization_id,o.organization_code,a.status
      FROM onboarding_applications a JOIN users u ON u.user_id=a.user_id JOIN ref_user_status us USING (user_status_id)
      JOIN ${table} org ON org.${key}=u.${userKey} JOIN organizations o ON o.organization_id=org.organization_id
      WHERE a.application_id=${receipt.applicationId} AND u.username=${quote(username)};`)).toBe(
      ['ACTIVE', 1, 'ONB-' + receipt.applicationId, 'APPROVED'].join('\t'));

    const status = await request.get(register + '/status/' + receipt.applicationNumber);
    expect(status.status()).toBe(200);
    const tracking = (await status.json()).data;
    expect(tracking.status).toBe('APPROVED');
    expect(tracking.contactEmail).not.toBe(email);
    expect(tracking.contactEmail).toContain('*');

    const activated = await request.post(login, { data: credentials });
    expect(activated.status()).toBe(200);
    const { accessToken } = (await activated.json()).data;
    const portal = await request.get(application.portal(receipt.entityId), {
      headers: { Authorization: 'Bearer ' + accessToken },
    });
    expect(portal.status()).toBe(200);
    expect((await portal.json()).success).toBe(true);
  });
}
