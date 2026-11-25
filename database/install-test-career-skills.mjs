import { readFileSync } from 'node:fs';
import { sql } from '../frontend/e2e/support/database.mjs';

sql(readFileSync(new URL('./phase-27-trainee-career-skills.sql', import.meta.url), 'utf8'));
console.log('Installed trainee career skills tables in SIH26135_TEST.');
