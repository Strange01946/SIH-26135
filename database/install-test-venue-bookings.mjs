import { readFileSync } from 'node:fs';
import { sql } from '../frontend/e2e/support/database.mjs';
for (const migration of ['phase-21-training-venue-bookings.sql', 'phase-22-remove-dedicated-centers.sql']) {
  const schema = readFileSync(new URL('./' + migration, import.meta.url), 'utf8').replace(/^\uFEFF/, '');
  sql(schema);
}
console.log('Installed the temporary and shared venue schema in SIH26135_TEST.');
