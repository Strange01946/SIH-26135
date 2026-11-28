import { expect, test } from './support/browser.mjs';
import { expectJson } from './support/contract.mjs';
import { sql } from './support/database.mjs';

const onboarding = '/api/v1/admin/onboarding/applications';
const kpis = '/api/v1/dashboards/kpis';
const quality = '/api/v1/operational/data-quality-scorecard';
const imports = '/api/v1/operational/import-metrics';
const number = new Intl.NumberFormat('en-IN', { maximumFractionDigits: 2 });
const display = (value) => value == null ? 'No data' : number.format(value);

test('admin console shows live outcomes and updates the pending queue after approval', async ({ page, smoke }) => {
  const { run } = smoke;
  const [kpiResponse, pendingResponse, qualityResponse, importResponse] = await Promise.all([
    smoke.waitApi('GET', kpis), smoke.waitApi('GET', onboarding), smoke.waitApi('GET', quality), smoke.waitApi('GET', imports),
    smoke.signIn('admin', '/admin/dashboard'),
  ]);
  await expect(page.getByRole('heading', { name: 'National Super Administrator Console' })).toBeVisible();
  await expect(page.getByText('Live Records Pending Domain Implementation')).toHaveCount(0);
  const headline = (await expectJson(kpiResponse, 'GET', kpis, 200, ['executiveKpis'])).executiveKpis;
  const outcomes = page.getByRole('region', { name: 'Skilling outcomes' });
  await expect(outcomes.getByRole('heading', { name: '6-month retention rate' })).toBeVisible();
  const enrolled = headline.kpis.find(({ key }) => key === 'enrolled').current;
  const enrolledCard = outcomes.getByRole('listitem').filter({ has: page.getByRole('heading', { name: 'Total enrolled' }) });
  const availabilityLabel = { SUPPRESSED: 'Suppressed', NO_DATA: 'No data', NOT_APPLICABLE: 'Outside this cohort' };
  await expect(enrolledCard.getByText(enrolled.availability === 'RELEASED' ? display(enrolled.value) : availabilityLabel[enrolled.availability], { exact: true })).toBeVisible();

  expect(qualityResponse.status()).toBe(200);
  const scorecard = (await qualityResponse.json()).data;
  const qualityCard = page.getByRole('region', { name: 'Data quality', exact: true });
  await expect(qualityCard.locator('dd').first()).toHaveText(scorecard.dataCleanlinessIndexPct == null ? 'No data' : display(scorecard.dataCleanlinessIndexPct) + '%');
  await expect(qualityCard.locator('dd').last()).toHaveText(display(scorecard.totalActiveIssues));
  expect(importResponse.status()).toBe(200);
  const importTotals = (await importResponse.json()).data;
  const intake = page.getByRole('region', { name: 'Bulk CSV and JSON intake' });
  await expect(intake.locator('dd').nth(1)).toHaveText(display(importTotals.failedRecordCount));
  await expect(page.getByRole('region', { name: 'Follow-up and verification' }).getByText('Claims pending verification')).toBeVisible();

  const queue = page.getByRole('region', { name: 'Organisation onboarding' });
  const reference = `${run.id}-application`;
  expect(new URL(pendingResponse.url()).searchParams.get('status')).toBe('PENDING');
  expect(pendingResponse.status()).toBe(200);
  const pending = (await pendingResponse.json()).data;
  expect(pending.some(({ applicationNumber }) => applicationNumber === reference)).toBe(true);
  await expect(queue.locator('dd')).toHaveText(display(pending.length));
  await expect(queue.getByText(reference, { exact: true })).toBeVisible();
  await queue.getByRole('link', { name: 'Review pending applications' }).click();
  await expect(page.getByRole('heading', { name: 'Organisation Onboarding Requests' })).toBeVisible();
  const application = page.getByRole('row').filter({ hasText: reference });
  page.once('dialog', (dialog) => dialog.accept());
  const [approved] = await Promise.all([smoke.waitApi('POST', `${onboarding}/${run.application}/approve`),
    application.getByRole('button', { name: 'Approve', exact: true }).click()]);
  expect(approved.status()).toBe(200);
  expect((await approved.json()).data).toMatchObject({ applicationNumber: reference, status: 'APPROVED' });
  await expect(page.getByText(`Application for "Smoke onboarding ${run.id}" approved successfully.`)).toBeVisible();
  expect(sql(`SELECT a.status,u.user_status_id,p.accreditation_status_id FROM onboarding_applications a
    JOIN users u USING(user_id) JOIN training_providers p ON p.provider_id=a.entity_id
    WHERE a.application_id=${run.application};`)).toBe('APPROVED\t2\t2');

  const [updated] = await Promise.all([smoke.waitApi('GET', onboarding), page.getByRole('link', { name: 'Admin Console', exact: true }).click()]);
  const current = (await updated.json()).data;
  expect(current.some(({ applicationNumber }) => applicationNumber === reference)).toBe(false);
  await expect(queue.getByText(reference, { exact: true })).toHaveCount(0);
  if (current.length === 0) await expect(queue.getByText('No pending applications.')).toBeVisible();
  else await expect(queue.locator('dd')).toHaveText(display(current.length));

  await page.setViewportSize({ width: 390, height: 844 });
  await expect(outcomes.getByRole('heading', { name: 'Total enrolled' })).toBeVisible();
  expect(await outcomes.evaluate((element) => element.scrollWidth <= element.clientWidth)).toBe(true);
  await page.setViewportSize({ width: 1360, height: 900 });
  await page.getByRole('link', { name: 'Open Government MIS' }).click();
  await expect(page.getByRole('heading', { name: 'Government MIS & executive analytics' })).toBeVisible();
  await page.getByRole('link', { name: 'Admin Console', exact: true }).click();
  await page.getByRole('link', { name: 'Candidate career records' }).click();
  await page.getByRole('textbox', { name: 'Candidate name or registration number' }).fill(run.candidateCode);
  await page.getByRole('button', { name: 'Search candidates' }).click();
  await page.getByRole('button', { name: new RegExp(run.candidateCode) }).click();
  const history = page.getByRole('region', { name: 'Employment and retention' });
  await expect(history.getByRole('option', { name: /Smoke Solar Services/ })).toHaveCount(1);
});

test('admin console gives district officers scoped operations and an onboarding queue', async ({ page, request, smoke }) => {
  const seen = [];
  page.on('request', (entry) => seen.push(new URL(entry.url()).pathname));
  const [qualityResponse, importResponse, onboardingResponse, auth] = await Promise.all([
    smoke.waitApi('GET', quality), smoke.waitApi('GET', imports), smoke.waitApi('GET', onboarding),
    smoke.signIn('district', '/admin/dashboard'),
  ]);
  await expect(page.getByRole('heading', { name: 'District Skill Officer Console' })).toBeVisible();
  await expect(page.getByText(`District ${smoke.run.district}`, { exact: true })).toBeVisible();
  expect(qualityResponse.status()).toBe(200);
  expect(importResponse.status()).toBe(200);
  await expect(page.getByRole('region', { name: 'Data quality', exact: true }).getByText('Active issues')).toBeVisible();
  await expect(page.getByRole('region', { name: 'Skilling outcomes' })).toHaveCount(0);
  expect(onboardingResponse.status()).toBe(200);
  const pending = (await onboardingResponse.json()).data;
  const queue = page.getByRole('region', { name: 'Organisation onboarding' });
  if (pending.length === 0) await expect(queue.getByText('No pending applications.')).toBeVisible();
  else await expect(queue.locator('dd')).toHaveText(display(pending.length));
  expect(seen).not.toContain(kpis);
  const headers = { Authorization: `Bearer ${auth.accessToken}` };
  const districtKpis = await request.get(kpis, { headers });
  expect(districtKpis.status()).toBe(200);
  const districtEnrollments = Number(sql(`SELECT COUNT(*) FROM vw_enrollment_outcome_fact
    WHERE trainee_district_id=${smoke.run.district};`));
  expect((await districtKpis.json()).data.enrollmentCount).toBe(districtEnrollments);
  expect((await request.get(onboarding + '/' + smoke.run.application, { headers })).status()).toBe(200);
});

test('admin console retries one failed card while other live sections remain usable', async ({ page, smoke }) => {
  smoke.allowHttpFailure('GET', quality, 503);
  await page.route('**/api/v1/operational/data-quality-scorecard', (route) => route.fulfill({
    status: 503, json: { status: 503, code: 'SERVICE_UNAVAILABLE', message: 'Scorecard briefly unavailable' },
  }));
  await smoke.signIn('admin', '/admin/dashboard');
  const card = page.getByRole('region', { name: 'Data quality', exact: true });
  await expect(card.getByText('Unable to load data quality scorecard')).toBeVisible();
  await expect(page.getByRole('link', { name: 'Candidate career records' })).toBeVisible();
  await expect(page.getByRole('region', { name: 'Skilling outcomes' }).getByRole('heading', { name: 'Total enrolled' })).toBeVisible();
  await page.unroute('**/api/v1/operational/data-quality-scorecard');
  const [live] = await Promise.all([smoke.waitApi('GET', quality), card.getByRole('button', { name: 'Retry' }).click()]);
  expect(live.status()).toBe(200);
  await expect(card.getByText('Active issues')).toBeVisible();
});
