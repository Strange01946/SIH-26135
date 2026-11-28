import { readFileSync } from 'node:fs';
import { sql } from '../frontend/e2e/support/database.mjs';

const migration = readFileSync(new URL('./phase-33-trainee-employment.sql', import.meta.url), 'utf8');
for (const statement of migration.split(';').map(part => part.trim()).filter(Boolean)) {
  const [, column] = /^ALTER TABLE employment_records ADD COLUMN (\w+)/.exec(statement);
  const installed = sql(`SELECT COUNT(*) FROM information_schema.columns WHERE table_schema=DATABASE()
    AND table_name='employment_records' AND column_name='${column}';`);
  if (installed === '0') sql(statement + ';');
}
sql(readFileSync(new URL('./phase-34-employment-training-links.sql', import.meta.url), 'utf8'));
console.log('Installed trainee employment fields and training links in SIH26135_TEST.');
