import { readFileSync } from 'node:fs';
import { sql } from '../frontend/e2e/support/database.mjs';

sql(readFileSync(new URL('./phase-26-career-followup.sql', import.meta.url), 'utf8'));
console.log('Installed the standard career follow-up questionnaire in SIH26135_TEST.');
