import { defineConfig, devices } from '@playwright/test';
import { dirname, join } from 'node:path';
import { tmpdir } from 'node:os';
import { loopbackFrontend } from './e2e/support/database.mjs';

export default defineConfig({
  testDir: './e2e',
  globalSetup: './e2e/support/verify-target.mjs',
  outputDir: join(process.env.SIH_E2E_RUN_FILE ? dirname(process.env.SIH_E2E_RUN_FILE) : tmpdir(), 'sih-playwright-results'),
  fullyParallel: false,
  workers: 1,
  retries: 0,
  timeout: 55_000,
  expect: { timeout: 10_000 },
  reporter: 'list',
  use: {
    ...devices['Desktop Chrome'],
    baseURL: loopbackFrontend,
    viewport: { width: 1360, height: 900 },
    timezoneId: 'Asia/Kolkata',
    locale: 'en-IN',
    serviceWorkers: 'block',
    headless: true,
    acceptDownloads: true,
    launchOptions: process.env.SIH_E2E_CHROMIUM_PATH ? { executablePath: process.env.SIH_E2E_CHROMIUM_PATH } : {},
  },
});
