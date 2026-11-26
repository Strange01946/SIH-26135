import { readFileSync } from 'node:fs';
import { sql } from '../frontend/e2e/support/database.mjs';

for (const migration of [
  'phase-29-batch-assessment-publication.sql', 'phase-30-assessor-scheduling.sql', 'phase-31-assessment-completion.sql',
]) {
  sql(readFileSync(new URL(migration, import.meta.url), 'utf8'));
}
console.log('Installed batch assessment workflow in SIH26135_TEST.');
