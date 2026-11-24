import { readFileSync, readdirSync } from 'node:fs';
import { test, expect } from '@playwright/test';
import { loopbackFrontend } from './support/database.mjs';

test('measures cold sign-in rendering and checks the production chunks', async ({ browser }) => {
  const dist = new URL('../dist/', import.meta.url);
  const manifest = JSON.parse(readFileSync(new URL('.vite/manifest.json', dist), 'utf8'));
  const entry = manifest['index.html'];
  expect(entry.isEntry).toBe(true);
  expect(manifest['src/pages/assessor/AssessorScoringPage.tsx'].isDynamicEntry).toBe(true);
  expect(manifest['src/pages/analytics/ExecutiveMisPage.tsx'].isDynamicEntry).toBe(true);
  const assets = readdirSync(new URL('assets/', dist));
  expect(assets.some((name) => name.endsWith('.map'))).toBe(false);

  const samples = [];
  for (let index = 0; index < 5; index++) {
    const context = await browser.newContext({ baseURL: loopbackFrontend, serviceWorkers: 'block',
      viewport: { width: 1360, height: 900 }, locale: 'en-IN', timezoneId: 'Asia/Kolkata' });
    try {
      const page = await context.newPage();
      const errors = [];
      page.on('pageerror', (error) => errors.push(error.message));
      page.on('console', (message) => { if (message.type() === 'error') errors.push(message.text()); });
      await page.goto('/login', { waitUntil: 'load' });
      await expect(page.getByRole('heading', { name: 'Sign in', exact: true })).toBeVisible();
      await page.waitForFunction(() => performance.getEntriesByName('first-contentful-paint').length === 1);
      const timing = await page.evaluate(() => {
        const modules = performance.getEntriesByType('resource').filter(({ name }) => new URL(name).pathname.endsWith('.js'));
        return {
          fcpMs: Math.round(performance.getEntriesByName('first-contentful-paint')[0].startTime),
          loadMs: Math.round(performance.getEntriesByType('navigation')[0].loadEventEnd),
          jsTransferredBytes: modules.reduce((sum, resource) => sum + resource.transferSize, 0),
          modules: modules.map(({ name }) => new URL(name).pathname),
        };
      });
      expect(errors).toEqual([]);
      expect(timing.jsTransferredBytes).toBeGreaterThan(0);
      expect(timing.modules.join(' ')).not.toMatch(/AssessorScoringPage|ExecutiveMisPage|mockServiceWorker/i);
      samples.push(timing);
    } finally {
      await context.close();
    }
  }
  const summary = (key) => {
    const values = samples.map((sample) => sample[key]).sort((a, b) => a - b);
    return { min: values[0], median: values[2], max: values[4] };
  };
  console.log('Cold local Chromium /login (5 fresh contexts, unthrottled): ' + JSON.stringify({
    fcpMs: summary('fcpMs'), loadMs: summary('loadMs'), jsTransferredBytes: summary('jsTransferredBytes'),
    mainEntryBytes: readFileSync(new URL(entry.file, dist)).length,
  }));
});
