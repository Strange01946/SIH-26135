import assert from 'node:assert/strict';
import { readFileSync } from 'node:fs';
import { assertBackendCatalog, assertRunId, loopbackBackend, loopbackFrontend } from './database.mjs';

export default async function verifyTarget() {
  const { SIH_E2E_RUN_FILE: path, SIH_E2E_RUN_ID: id } = process.env;
  if (!path || !id) throw new Error('Use npm run test:e2e to start the isolated Spring Boot runner.');
  assertRunId(id);
  assert.equal(JSON.parse(readFileSync(path, 'utf8')).id, id);
  assertBackendCatalog(id);
  for (const origin of [loopbackBackend, loopbackFrontend]) {
    const reply = await fetch(origin + '/actuator/health', { redirect: 'error', signal: AbortSignal.timeout(5000) });
    assert(reply.ok && (await reply.json()).status === 'UP', 'The isolated test services are ready');
  }
}
