import assert from 'node:assert/strict';
import { execFileSync } from 'node:child_process';
import { mkdtempSync, readFileSync, rmSync, rmdirSync } from 'node:fs';
import { tmpdir } from 'node:os';
import { join } from 'node:path';
import { fileURLToPath, pathToFileURL } from 'node:url';

const frontend = fileURLToPath(new URL('../', import.meta.url));
const snapshot = join(frontend, '../docs/openapi/openapi-0-all-apis.json');
const yaml = join(frontend, '../docs/openapi/openapi-0-all-apis.yaml');
const committedTypes = join(frontend, 'src/api/generated/api.types.ts');
const cli = join(frontend, 'node_modules/openapi-typescript/bin/cli.js');
const text = (path) => readFileSync(path, 'utf8').replace(/\r\n/g, '\n');

const operations = [
  ['post', '/api/v1/auth/login'], ['post', '/api/v1/auth/refresh'], ['get', '/api/v1/auth/me'],
  ['post', '/api/v1/auth/register/trainee'], ['post', '/api/v1/auth/register/training-provider'],
  ['post', '/api/v1/auth/register/employer'],
  ['get', '/api/v1/admin/onboarding/applications'], ['get', '/api/v1/admin/onboarding/applications/{id}'],
  ['post', '/api/v1/admin/onboarding/applications/{id}/approve'], ['post', '/api/v1/admin/onboarding/applications/{id}/reject'],
  ['get', '/api/v1/courses'], ['get', '/api/v1/courses/{id}'],
  ['get', '/api/v1/dashboards/kpis'], ['get', '/api/v1/dashboards/geography'],
  ['get', '/api/v1/training-centers'],
  ['put', '/api/v1/training-centers/{id}'], ['delete', '/api/v1/training-centers/{id}'],
  ['get', '/api/v1/training-centers/lifecycle-statuses'],
  ['get', '/api/v1/venue-bookings'], ['post', '/api/v1/venue-bookings'],
  ['get', '/api/v1/venue-bookings/{id}'], ['post', '/api/v1/venue-bookings/{id}/review'],
  ['post', '/api/v1/venue-bookings/{id}/reschedule'], ['post', '/api/v1/venue-bookings/{id}/close'],
  ['get', '/api/v1/venue-bookings/venues'], ['post', '/api/v1/venue-bookings/venues'],
  ['get', '/api/v1/venue-bookings/venues/{centerId}/availability'],
  ['post', '/api/v1/venue-bookings/venues/{centerId}/blackouts'],
  ['delete', '/api/v1/venue-bookings/venues/{centerId}/blackouts/{blackoutId}'],
  ['get', '/api/v1/training-requirements/government'], ['post', '/api/v1/training-requirements/government'],
  ['get', '/api/v1/training-requirements/government/{id}'], ['post', '/api/v1/training-requirements/government/{id}/select'],
  ['post', '/api/v1/training-requirements/government/{id}/centre'],
  ['get', '/api/v1/training-requirements/open'], ['get', '/api/v1/training-requirements/my-applications'],
  ['get', '/api/v1/training-requirements/{id}'], ['post', '/api/v1/training-requirements/{id}/applications'],
  ['get', '/api/v1/training-batches'], ['put', '/api/v1/training-batches/{id}/status'], ['post', '/api/v1/training-enrollments'],
  ['get', '/api/v1/assessor/assessments/{id}/roster'], ['post', '/api/v1/assessor/assessments/{id}/finalize'],
  ['get', '/api/v1/certifications/{id}/download'], ['get', '/api/v1/employer/verifications/pending'],
  ['post', '/api/v1/employer/verifications/{requestId}/verdict'], ['post', '/api/v1/consents'],
  ['post', '/api/v1/trainees/me/followup-surveys/{taskId}/responses'], ['post', '/api/v1/import-batches'],
  ['get', '/api/v1/import-batches/{id}/quarantine/export'],
];

function canonical(value) {
  if (Array.isArray(value)) return value.map(canonical);
  if (!value || typeof value !== 'object') return value;
  return Object.fromEntries(Object.keys(value).sort().map((key) => [key, canonical(value[key])]));
}

export async function checkContract({ liveUrl } = {}) {
  const folder = mkdtempSync(join(tmpdir(), 'sih-openapi-'));
  const generated = join(folder, 'types.ts');
  try {
    const verified = JSON.parse(text(snapshot));
    for (const [method, path] of operations) {
      assert(verified.paths?.[path]?.[method], 'Missing verified operation ' + method + ' ' + path);
    }
    assert(!verified.paths?.['/api/v1/training-centers']?.post, 'Direct center creation must stay disabled');
    assert(!verified.paths?.['/api/v1/training-batches']?.post, 'Direct batch creation must stay disabled');
    assert(!verified.paths?.['/api/v1/courses']?.post && !verified.paths?.['/api/v1/courses/{id}']?.put
      && !verified.paths?.['/api/v1/courses/{id}']?.delete, 'Course reporting references must be read-only over HTTP');
    for (const name of ['Publish', 'NewBooking']) {
      const fields = verified.components.schemas[name].properties;
      assert(fields.skillsToTeach && !fields.courseId && !fields.programId, name + ' must use skills as its training input');
    }
    assert(verified.components.schemas.Publish.required.includes('centerId'), 'Government training must require a fixed center');
    for (const name of ['RegisterTraineeRequest', 'RegisterTrainingProviderRequest', 'NewSharedVenue']) {
      assert(verified.components?.schemas?.[name]?.required?.includes('pincode'), name + ' must require a PIN code');
    }
    const generate = (input) => {
      execFileSync(process.execPath, [cli, input, '-o', generated], { cwd: frontend, stdio: 'pipe' });
      return text(generated);
    };
    assert(generate(snapshot) === text(committedTypes), 'Generated TypeScript differs. Run npm run generate:types.');
    assert(generate(yaml) === text(committedTypes), 'Verified JSON and YAML generate different contracts.');
    if (liveUrl) {
      const response = await fetch(liveUrl + '/v3/api-docs/0-all-apis', { signal: AbortSignal.timeout(20_000), redirect: 'error' });
      assert.equal(response.status, 200);
      const actual = await response.json();
      assert.deepEqual(canonical(actual.paths), canonical(verified.paths), 'Live OpenAPI paths differ from the verified snapshot.');
      assert.deepEqual(canonical(actual.components?.schemas), canonical(verified.components?.schemas), 'Live OpenAPI schemas differ from the verified snapshot.');
      // Swagger can enumerate equivalent paths and schemas in different orders.
      assert(operations.every(([method, path]) => actual.paths?.[path]?.[method]));
    }
    console.log('Verified OpenAPI JSON, YAML and generated TypeScript' + (liveUrl ? ' against Spring Boot.' : '.'));
  } finally {
    rmSync(generated, { force: true });
    rmdirSync(folder);
  }
}

if (process.argv[1] && import.meta.url === pathToFileURL(process.argv[1]).href) await checkContract();
