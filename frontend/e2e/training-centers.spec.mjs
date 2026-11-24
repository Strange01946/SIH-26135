import { expect, test } from './support/browser.mjs';
import { quote, sql } from './support/database.mjs';

const centers = '/api/v1/training-centers';
const venues = '/api/v1/venue-bookings/venues';

function locationIds(state, district) {
  const [stateId, districtId] = sql(`SELECT s.state_id,d.district_id FROM districts d JOIN states s USING (state_id)
    WHERE s.state_code=${quote(state)} AND d.district_name=${quote(district)} AND d.lgd_code IS NOT NULL;`).split('\t').map(Number);
  expect(stateId).toBeGreaterThan(0);
  expect(districtId).toBeGreaterThan(0);
  return { stateId, districtId };
}

async function authorization(request, smoke, role) {
  const response = await request.post('/api/v1/auth/login', {
    data: { usernameOrEmail: smoke.run.users[role], password: smoke.run.password },
  });
  expect(response.status()).toBe(200);
  return { Authorization: 'Bearer ' + (await response.json()).data.accessToken };
}

test('admin manages a shared center through the venue directory and the center editor', async ({ page, request, smoke }) => {
  const code = smoke.run.id + '-center-admin';
  const pune = locationIds('MH', 'Pune');
  const karaikal = locationIds('PY', 'Karaikal');
  const admin = await smoke.signIn('admin', '/admin/centers');
  const headers = { Authorization: 'Bearer ' + admin.accessToken };
  await expect(page.getByRole('button', { name: 'Add training center' })).toHaveCount(0);
  await expect(page.getByRole('link', { name: 'Manage shared centers' })).toBeVisible();
  expect((await request.post(centers, { headers, data: {} })).status()).toBe(405);
  const registered = await request.post(venues, { headers, data: {
    code, name: 'Smoke Shared Center', address: 'Fixture lab', capacity: 28, pincode: '411001', ...pune,
  } });
  expect(registered.status()).toBe(201);
  const { data: created } = await registered.json();
  const path = centers + '/' + created.id;
  const clearPin = await request.put(path, { headers, data: { pincode: null } });
  expect(clearPin.status()).toBe(400);
  expect(await clearPin.json()).toMatchObject({ code: 'PINCODE_REQUIRED' });
  expect(sql(`SELECT pincode FROM training_centers WHERE center_id=${created.id};`)).toBe('411001');

  sql(`UPDATE training_centers SET pincode=NULL WHERE center_id=${created.id} AND center_code=${quote(code)};`);
  const missingPin = await request.put(path, { headers, data: { addressLine2: 'Legacy address edit' } });
  expect(missingPin.status()).toBe(400);
  expect(await missingPin.json()).toMatchObject({ code: 'PINCODE_REQUIRED' });
  await page.getByLabel('Center code', { exact: true }).fill(code);
  await page.getByRole('button', { name: 'Search', exact: true }).click();
  const row = page.getByRole('row').filter({ hasText: code });
  await expect(row.getByText('Pune', { exact: true })).toBeVisible();
  await row.getByRole('button', { name: 'Edit Smoke Shared Center' }).click();
  const modal = page.getByRole('dialog', { name: 'Edit training center' });
  await modal.getByLabel('Center name *', { exact: true }).fill('Smoke Updated Shared Center');
  await modal.getByLabel(/State or union territory/).selectOption(String(karaikal.stateId));
  await expect(modal.getByLabel(/District/)).toHaveValue('');
  await modal.getByLabel(/District/).selectOption(String(karaikal.districtId));
  await modal.getByLabel(/Operational capacity/).fill('30');
  const pincode = modal.getByLabel(/PIN code/);
  const save = modal.getByRole('button', { name: 'Save center' });
  await expect(pincode).toHaveValue('');
  await save.click();
  await expect(pincode).toBeFocused();
  await pincode.fill('609602');
  const [updated] = await Promise.all([smoke.waitApi('PUT', path), save.click()]);
  expect(updated.status()).toBe(200);
  await expect(row.getByText('Karaikal', { exact: true })).toBeVisible();
  const preservePin = await request.put(path, { headers, data: { addressLine2: 'Updated address' } });
  expect(preservePin.status()).toBe(200);
  expect(sql(`SELECT provider_id IS NULL,venue_type,state_id,district_id,pincode,operational_capacity
    FROM training_centers WHERE center_id=${created.id};`))
    .toBe([1, 'SHARED', karaikal.stateId, karaikal.districtId, '609602', 30].join('\t'));

  await row.getByRole('button', { name: 'Remove Smoke Updated Shared Center' }).click();
  await page.getByRole('dialog').getByRole('button', { name: 'Cancel' }).click();
  await expect(row).toBeVisible();
  await row.getByRole('button', { name: 'Remove Smoke Updated Shared Center' }).click();
  const [removed] = await Promise.all([smoke.waitApi('DELETE', path),
    page.getByRole('dialog').getByRole('button', { name: 'Remove center' }).click()]);
  expect(removed.status()).toBe(200);
  await expect(row).toHaveCount(0);
  expect(sql(`SELECT deleted_at IS NOT NULL FROM training_centers WHERE center_id=${created.id};`)).toBe('1');
});

test('provider sees its own temporary site and manages new training through venue bookings', async ({ page, request, smoke }) => {
  const provider = await smoke.signIn('provider', '/provider/centers');
  const headers = { Authorization: 'Bearer ' + provider.accessToken };
  await expect(page.getByRole('link', { name: 'Book a training venue' })).toHaveAttribute('href', '/provider/venue-bookings?view=shared');
  const row = page.getByRole('row').filter({ hasText: smoke.run.id + '-center' });
  await expect(row.getByRole('link', { name: 'View venue bookings' })).toBeVisible();
  await expect(row.getByRole('button', { name: /Edit|Remove/ })).toHaveCount(0);
  const center = centers + '/' + smoke.run.center;
  expect((await request.get(center, { headers })).status()).toBe(200);
  const removal = await request.delete(center, { headers });
  expect(removal.status()).toBe(409);
  expect((await removal.json()).code).toBe('VENUE_WORKFLOW_REQUIRED');
  const outsider = await authorization(request, smoke, 'outsider');
  expect((await request.get(center, { headers: outsider })).status()).toBe(403);
});

test('district offices use venue permissions and the shared directory stays scoped', async ({ page, request, smoke }) => {
  const auth = await smoke.signIn('district', '/admin/dashboard');
  await expect(page.getByRole('link', { name: 'Training Centers', exact: true })).toHaveCount(0);
  await page.getByRole('link', { name: 'Venue Bookings', exact: true }).click();
  await expect(page).toHaveURL(/\/admin\/venue-bookings$/);
  expect((await request.get(centers, { headers: { Authorization: 'Bearer ' + auth.accessToken } })).status()).toBe(403);
});
