import { expect, test } from './support/browser.mjs';
import { quote, sql } from './support/database.mjs';

const queue = '/api/v1/admin/onboarding/applications';
const register = '/api/v1/auth/register';

async function apply(request, run, name, type, stateId, districtId) {
  const username = run.users[name];
  const organizationName = run.id + '-' + name.replaceAll('_', '-');
  const organization = type === 'training-provider'
    ? { providerName: organizationName, registrationNumber: organizationName, pincode: '110001' }
    : { employerName: organizationName };
  const response = await request.post(register + '/' + type, { data: {
    ...organization, username, email: username + '@example.invalid', password: run.password,
    contactPersonName: 'Smoke contact', stateId, districtId,
  } });
  expect(response.status()).toBe(202);
  return (await response.json()).data;
}

function reviewerDecision(applicationId) {
  return sql(`SELECT a.status,u.user_status_id,COALESCE(r.username,''),COALESCE(a.rejection_reason,'')
    FROM onboarding_applications a JOIN users u USING (user_id)
    LEFT JOIN users r ON r.user_id=a.reviewed_by_user_id WHERE a.application_id=${applicationId};`).split('\t');
}

test('state and district onboarding reviewers can adjudicate only within their assigned jurisdiction', async ({ page, request, smoke }) => {
  const { run } = smoke;
  const [sameStateDistrict, outsideState, outsideDistrict] = sql(`SELECT
    (SELECT district_id FROM districts WHERE state_id=${run.state} AND district_id<>${run.district}
      AND lgd_code IS NOT NULL AND lifecycle_status_id=1 AND deleted_at IS NULL ORDER BY district_id LIMIT 1),
    o.state_id,o.district_id
    FROM districts o JOIN states s USING (state_id)
    WHERE o.state_id<>${run.state} AND o.lgd_code IS NOT NULL AND o.lifecycle_status_id=1
      AND o.deleted_at IS NULL AND s.lifecycle_status_id=1 AND s.deleted_at IS NULL ORDER BY o.district_id LIMIT 1;`).split('\t').map(Number);
  expect([sameStateDistrict, outsideState, outsideDistrict].every((id) => Number.isSafeInteger(id) && id > 0)).toBe(true);

  const districtProvider = await apply(request, run, 'onboarding_district_provider', 'training-provider', run.state, run.district);
  const districtEmployer = await apply(request, run, 'onboarding_district_employer', 'employer', run.state, run.district);
  const stateProvider = await apply(request, run, 'onboarding_state_provider', 'training-provider', run.state, sameStateDistrict);
  const stateEmployer = await apply(request, run, 'onboarding_state_employer', 'employer', run.state, sameStateDistrict);
  const outsideProvider = await apply(request, run, 'onboarding_outside_provider', 'training-provider', outsideState, outsideDistrict);

  const review = [
    {
      role: 'district', label: 'District jurisdiction (ID ' + run.district + ')',
      approve: districtProvider, reject: districtEmployer, visible: [districtProvider, districtEmployer],
      hidden: [stateProvider, stateEmployer, outsideProvider],
    },
    {
      role: 'stateadmin', label: 'State or territory jurisdiction (ID ' + run.state + ')',
      approve: stateEmployer, reject: stateProvider, visible: [districtProvider, districtEmployer, stateProvider, stateEmployer],
      hidden: [outsideProvider],
    },
  ];

  for (const reviewer of review) {
    const { accessToken } = await smoke.signIn(reviewer.role, '/admin/dashboard');
    const headers = { Authorization: 'Bearer ' + accessToken };
    const preview = page.getByRole('region', { name: 'Organisation onboarding' });
    await expect(preview.getByRole('link', { name: 'Review pending applications' })).toBeVisible();
    await expect(page.getByRole('link', { name: 'Onboarding Requests', exact: true })).toBeVisible();
    await preview.getByRole('link', { name: 'Review pending applications' }).click();
    await expect(page.getByText(reviewer.label, { exact: true })).toBeVisible();
    const row = (app) => page.getByRole('row').filter({ hasText: app.applicationNumber });

    await page.getByLabel('Filter status').selectOption('ALL');
    for (const app of reviewer.visible) await expect(row(app)).toBeVisible();
    for (const app of reviewer.hidden) await expect(row(app)).toHaveCount(0);
    const all = await request.get(queue, { headers });
    expect(all.status()).toBe(200);
    const ids = (await all.json()).data.map(({ applicationId }) => applicationId);
    for (const app of reviewer.visible) expect(ids).toContain(app.applicationId);
    for (const app of reviewer.hidden) {
      expect(ids).not.toContain(app.applicationId);
      const path = queue + '/' + app.applicationId;
      expect((await request.get(path, { headers })).status()).toBe(404);
      expect((await request.post(path + '/approve', { headers })).status()).toBe(404);
      expect((await request.post(path + '/reject', { headers, data: { rejectionReason: 'Out of area' } })).status()).toBe(404);
      expect(reviewerDecision(app.applicationId)[0]).toBe('PENDING');
    }
    expect((await request.get(queue + '/' + reviewer.approve.applicationId, { headers })).status()).toBe(200);

    page.once('dialog', (dialog) => dialog.accept());
    const [approved] = await Promise.all([
      smoke.waitApi('POST', queue + '/' + reviewer.approve.applicationId + '/approve'),
      row(reviewer.approve).getByRole('button', { name: 'Approve', exact: true }).click(),
    ]);
    expect(approved.status()).toBe(200);
    expect(reviewerDecision(reviewer.approve.applicationId).slice(0, 3))
      .toEqual(['APPROVED', '2', run.users[reviewer.role]]);

    const reason = 'Organisation documents must be corrected.';
    await row(reviewer.reject).getByRole('button', { name: 'Reject', exact: true }).click();
    await page.getByLabel('Reason for Rejection *').fill(reason);
    const [rejected] = await Promise.all([
      smoke.waitApi('POST', queue + '/' + reviewer.reject.applicationId + '/reject'),
      page.getByRole('button', { name: 'Confirm Rejection' }).click(),
    ]);
    expect(rejected.status()).toBe(200);
    expect(reviewerDecision(reviewer.reject.applicationId)).toEqual(['REJECTED', '4', run.users[reviewer.role], reason]);

    await page.getByLabel('Filter status').selectOption('REJECTED');
    await expect(row(reviewer.reject)).toBeVisible();
    await expect(row(reviewer.approve)).toHaveCount(0);

    if (reviewer.role === 'district') {
      const username = quote(run.users.district);
      sql(`UPDATE users SET district_id=NULL WHERE username=${username};`);
      try {
        expect((await request.get(queue, { headers })).status()).toBe(403);
        expect((await request.get(queue + '/' + reviewer.approve.applicationId, { headers })).status()).toBe(403);
      } finally {
        sql(`UPDATE users SET district_id=${run.district} WHERE username=${username};`);
      }
    }
    await page.getByRole('button', { name: 'Sign Out' }).click();
    await expect(page.getByRole('heading', { name: 'Sign in', exact: true })).toBeVisible();
  }
});
