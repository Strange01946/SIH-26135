import assert from 'node:assert/strict';
import { readFileSync } from 'node:fs';
import { expect, test as playwright } from '@playwright/test';
import { loopbackFrontend } from './database.mjs';
import { expectJson } from './contract.mjs';

export const test = playwright.extend({
  smoke: async ({ page }, use) => {
    const runPath = process.env.SIH_E2E_RUN_FILE;
    if (!runPath) throw new Error('Use the live smoke runner.');
    const run = JSON.parse(readFileSync(runPath, 'utf8'));
    const failures = [];
    const expected = new Set();
    const allowHttpFailure = (method, path, status) => expected.add(method + ' ' + path + ' ' + status);

    await page.route('**/*', (route) => {
      const { origin, protocol } = new URL(route.request().url());
      if (['http:', 'https:'].includes(protocol) && origin !== loopbackFrontend) return route.abort('blockedbyclient');
      return route.continue();
    });
    page.on('pageerror', (error) => failures.push('Uncaught browser error: ' + error.message));
    page.on('response', (response) => {
      const { pathname } = new URL(response.url());
      if (response.status() >= 400) {
        const entry = response.request().method() + ' ' + pathname + ' ' + response.status();
        if (!expected.has(entry)) failures.push('Unexpected browser HTTP failure: ' + entry);
      }
      if (pathname.startsWith('/__sih_simulation/') || response.fromServiceWorker()) failures.push('A simulated response reached the live suite.');
    });
    page.on('requestfailed', (request) => {
      const error = request.failure()?.errorText;
      if (error !== 'net::ERR_ABORTED') failures.push('Browser request failed: ' + new URL(request.url()).pathname + ' ' + error);
    });
    page.on('console', (message) => {
      if (message.type() !== 'error') return;
      const status = /Failed to load resource:.*\b(\d{3})\b/.exec(message.text())?.[1];
      const location = message.location().url;
      if (status && location.startsWith(loopbackFrontend)) {
        const path = new URL(location).pathname;
        if ([...expected].some((entry) => entry.endsWith(' ' + path + ' ' + status))) return;
      }
      failures.push('Browser console error: ' + message.text());
    });

    const waitApi = (method, path) => page.waitForResponse((response) =>
      new URL(response.url()).pathname === path && response.request().method() === method);
    const signIn = async (role, destination) => {
      await page.goto(destination);
      await expect(page.getByRole('heading', { name: 'Sign in', exact: true })).toBeVisible();
      await page.locator('#usernameOrEmail').fill(run.users[role]);
      await page.locator('#password').fill(run.password);
      const [response] = await Promise.all([waitApi('POST', '/api/v1/auth/login'),
        page.getByRole('button', { name: 'Sign In', exact: true }).click()]);
      const data = await expectJson(response, 'POST', '/api/v1/auth/login', 200, ['accessToken', 'refreshToken', 'authorities']);
      assert.equal(data.username, run.users[role], 'Logged in as the fixture account');
      await expect(page.getByRole('banner').first()).toBeVisible();
      return data;
    };
    await use({ run, signIn, waitApi, allowHttpFailure });
    assert.deepEqual(failures, [], 'No unexpected browser console, runtime, network or HTTP errors');
  },
});

export { expect };
