import { readFileSync } from 'node:fs';
import { sql } from '../frontend/e2e/support/database.mjs';

const migration = readFileSync(new URL('./phase-32-training-admission.sql', import.meta.url), 'utf8');
for (const statement of migration.split(';').map(part => part.trim()).filter(Boolean)) {
  const table = /^ALTER TABLE (\w+)/.exec(statement)[1];
  const installed = sql(`SELECT COUNT(*) FROM information_schema.columns WHERE table_schema=DATABASE()
    AND table_name='${table}' AND column_name='minimum_education_level_id';`);
  if (installed === '0') sql(statement + ';');
}
console.log('Installed training admission fields in SIH26135_TEST.');
