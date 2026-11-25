import { readFileSync } from 'node:fs';
import { sql } from '../frontend/e2e/support/database.mjs';

sql(readFileSync(new URL('./phase-28-training-contact-consent.sql', import.meta.url), 'utf8'));
console.log('Installed training provider contact consent in SIH26135_TEST.');
