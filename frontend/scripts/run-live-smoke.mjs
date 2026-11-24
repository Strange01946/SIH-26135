import assert from 'node:assert/strict';
import { spawn } from 'node:child_process';
import { randomBytes } from 'node:crypto';
import { closeSync, existsSync, mkdtempSync, openSync, readFileSync, realpathSync, rmSync, writeFileSync } from 'node:fs';
import { createServer } from 'node:net';
import { tmpdir } from 'node:os';
import { dirname, join, resolve } from 'node:path';
import { fileURLToPath } from 'node:url';
import { setTimeout as delay } from 'node:timers/promises';
import {
  assertBackendCatalog, backendPort, catalogGuard, databaseEnvironment, frontendPort, jdbcUrl, loopbackBackend, loopbackFrontend, sql,
} from '../e2e/support/database.mjs';
import { cleanFixtures, seedFixtures } from '../e2e/support/fixtures.mjs';
import { checkContract } from './check-contract.mjs';

const frontend = fileURLToPath(new URL('../', import.meta.url));
const backend = resolve(frontend, '../backend');
const openApiDirectory = resolve(frontend, '../docs/openapi');
const jar = resolve(backend, process.env.SIH_E2E_BACKEND_JAR ?? 'target/sih26135-backend-0.1.0-SNAPSHOT.jar');
const temporaryRoot = realpathSync(tmpdir());
const id = 'p314_' + randomBytes(5).toString('hex');
const ttlMs = 15_000;
const captureContract = process.argv[2] === '--capture-contract' && process.argv.length === 3;

async function assertFree(port) {
  await new Promise((done, fail) => {
    const server = createServer().once('error', () => fail(new Error('Free loopback port ' + port + ' before running the isolated smoke suite.')));
    server.listen(port, '127.0.0.1', () => server.close(done));
  });
}

function launch(executable, args, cwd, env, stdio = 'inherit') {
  const processHandle = spawn(executable, args, { cwd, env, stdio, windowsHide: true, shell: false });
  const closed = new Promise((done) => {
    processHandle.once('error', (error) => done({ error }));
    processHandle.once('close', (code, signal) => done({ code, signal }));
  });
  return { processHandle, closed };
}

async function successful(child) {
  const result = await child.closed;
  if (result.error) throw result.error;
  if (result.code !== 0) throw new Error('Child exited with status ' + (result.code ?? result.signal));
}

async function stop(child) {
  if (!child) return;
  child.processHandle.kill();
  await Promise.race([child.closed, delay(4000)]);
  if (child.processHandle.exitCode == null && child.processHandle.signalCode == null) {
    child.processHandle.kill('SIGKILL');
    await child.closed;
  }
}

async function ready(child, url, validate, label) {
  const deadline = Date.now() + 70_000;
  while (Date.now() < deadline) {
    if (!child.processHandle.pid || child.processHandle.exitCode !== null) throw new Error(label + ' exited during startup.');
    try {
      const result = await fetch(url, { signal: AbortSignal.timeout(1500), redirect: 'error' });
      if (result.ok && await validate(result)) return;
    } catch {
      // Wait for the owned process to accept loopback connections.
    }
    await delay(400);
  }
  throw new Error(label + ' did not become ready.');
}

const { user, password } = databaseEnvironment();
assert(existsSync(jar), 'Build the frozen backend first with mvn -q -DskipTests package.');
assert.equal(sql('SELECT LOWER(DATABASE());'), 'sih26135_test');
await Promise.all([assertFree(backendPort), assertFree(frontendPort)]);
const workspace = mkdtempSync(join(temporaryRoot, 'sih-smoke-'));
const stateFile = join(workspace, 'run.json');
const logPath = join(workspace, 'backend.log');
const smokeEnv = {
  ...process.env,
  VITE_LOCAL_DEMO: 'true',
  VITE_API_BASE_URL: '',
  SIH_VITE_PROXY_PORT: String(backendPort),
  SIH_E2E_RUN_FILE: stateFile,
  SIH_E2E_RUN_ID: id,
};
delete smokeEnv.NO_COLOR;
let spring;
let vite;
let suite;
let fixturesAttempted = false;

function interrupt() {
  if (suite) suite.processHandle.kill();
  else {
    spring?.processHandle.kill();
    vite?.processHandle.kill();
  }
}
process.once('SIGINT', interrupt);
process.once('SIGTERM', interrupt);

try {
  const file = openSync(logPath, 'w');
  try {
    spring = launch(process.env.SIH_E2E_JAVA ?? 'java', ['-jar', jar, '--spring.profiles.active=local-demo',
      '--server.address=127.0.0.1', '--server.port=' + backendPort,
      '--spring.datasource.url=' + jdbcUrl + '&connectionAttributes=sihSmokeRun:' + id,
      '--spring.datasource.hikari.connection-init-sql=' + catalogGuard,
      '--spring.jpa.hibernate.ddl-auto=validate', '--communication.mode=MOCK', '--communication.queue.worker-enabled=false',
      '--communication.allow-test-webhook-signatures=false', '--security.jwt.access-token-expiration-ms=' + ttlMs,
      '--server.tomcat.basedir=' + join(workspace, 'tomcat'),
      '--import-pipeline.staging-directory=' + join(workspace, 'imports'),
    ], backend, {
      ...process.env, DB_USERNAME: user, DB_PASSWORD: password, DB_NAME: 'SIH26135_TEST',
      SPRING_DATASOURCE_USERNAME: user, SPRING_DATASOURCE_PASSWORD: password,
      JWT_SECRET: randomBytes(32).toString('hex'),
      SIH_VERIFICATION_EVIDENCE_ROOT: join(workspace, 'evidence'),
      SPRING_APPLICATION_JSON: '{}',
    }, ['ignore', file, file]);
  } finally {
    closeSync(file);
  }
  await Promise.all([
    ready(spring, loopbackBackend + '/actuator/health', async (result) =>
      (await result.json()).status === 'UP' && readFileSync(logPath, 'utf8').includes('Tomcat started on port ' + backendPort + ' (http)'), 'Spring Boot'),
    captureContract ? Promise.resolve() : (async () => {
      await successful(launch(process.execPath, ['node_modules/typescript/bin/tsc', '--noEmit'], frontend, smokeEnv));
      await successful(launch(process.execPath, ['node_modules/vite/bin/vite.js', 'build'], frontend, smokeEnv));
    })(),
  ]);
  assertBackendCatalog(id);
  if (captureContract) {
    const capture = async (suffix) => {
      const response = await fetch(`${loopbackBackend}/v3/api-docs${suffix}/0-all-apis`, { redirect: 'error', signal: AbortSignal.timeout(20_000) });
      assert.equal(response.status, 200, 'Spring Boot OpenAPI' + suffix);
      return response.text();
    };
    const [json, yaml] = await Promise.all([capture(''), capture('.yaml')]);
    assert.equal(JSON.parse(json).openapi, '3.1.0');
    assert.match(yaml, /^openapi: 3\.1\.0\r?\n/);
    writeFileSync(join(openApiDirectory, 'openapi-0-all-apis.json'), json);
    writeFileSync(join(openApiDirectory, 'openapi-0-all-apis.yaml'), yaml);
    console.log('Captured the frozen Spring Boot contract. Run npm run generate:types and npm run check:contract.');
  } else {
    await checkContract({ liveUrl: loopbackBackend });
    fixturesAttempted = true;
    const fixtures = seedFixtures(id);
    writeFileSync(stateFile, JSON.stringify(fixtures), { mode: 0o600 });
    vite = launch(process.execPath, ['node_modules/vite/bin/vite.js', 'preview', '--host', '127.0.0.1', '--port', String(frontendPort), '--strictPort'],
      frontend, smokeEnv);
    await ready(vite, loopbackFrontend + '/actuator/health', async (result) => (await result.json()).status === 'UP', 'Frontend preview and proxy');
    assertBackendCatalog(id);
    console.log('Live smoke target verified on SIH26135_TEST. Run ' + id + '.');
    suite = launch(process.execPath, ['node_modules/@playwright/test/cli.js', 'test', ...process.argv.slice(2)], frontend, smokeEnv);
    await successful(suite);
  }
} catch (error) {
  process.exitCode = 1;
  console.error(error.message);
  if (existsSync(logPath) && !fixturesAttempted) {
    console.error(readFileSync(logPath, 'utf8').split(/\r?\n/).slice(-35).join('\n'));
  }
} finally {
  await Promise.all([stop(suite), stop(vite), stop(spring)]);
  try {
    if (fixturesAttempted) {
      cleanFixtures(id);
      console.log('Verified cleanup for ' + id + '.');
    }
  } finally {
    const ownedFolder = realpathSync(workspace);
    assert.equal(dirname(ownedFolder), temporaryRoot);
    assert(ownedFolder === workspace && ownedFolder.startsWith(join(temporaryRoot, 'sih-smoke-')));
    rmSync(ownedFolder, { recursive: true, force: true });
  }
}
