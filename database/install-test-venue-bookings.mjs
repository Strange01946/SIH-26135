import { readFileSync } from 'node:fs';
import { sql } from '../frontend/e2e/support/database.mjs';
for (const migration of ['phase-21-training-venue-bookings.sql', 'phase-22-remove-dedicated-centers.sql',
  'phase-23-training-requirements.sql', 'phase-24-skills-first-training.sql', 'phase-25-government-training-centres.sql']) {
  const schema = readFileSync(new URL('./' + migration, import.meta.url), 'utf8').replace(/^\uFEFF/, '');
  sql(schema);
}
console.log('Installed the skills-first training workflow schema in SIH26135_TEST.');
