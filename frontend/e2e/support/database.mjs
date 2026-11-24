import assert from 'node:assert/strict';
import { existsSync } from 'node:fs';
import { spawnSync } from 'node:child_process';

export const catalog = 'SIH26135_TEST';
export const catalogGuard = "SELECT IF(LOWER(DATABASE())='sih26135_test',1,JSON_EXTRACT('wrong smoke catalog','$'))";
export const jdbcUrl = 'jdbc:mysql://127.0.0.1:3306/SIH26135_TEST?useUnicode=true&characterEncoding=utf8&connectionTimeZone=Asia/Kolkata';
const portOffset = Number(process.env.SIH_E2E_PORT_OFFSET ?? 0);
assert(Number.isSafeInteger(portOffset) && portOffset >= 0 && portOffset <= 57455, 'Invalid smoke loopback port offset');
export const backendPort = 8080 + portOffset;
export const frontendPort = 5173 + portOffset;
export const loopbackBackend = 'http://localhost:' + backendPort;
export const loopbackFrontend = 'http://127.0.0.1:' + frontendPort;
export const assertRunId = (id) => assert.match(id ?? '', /^p314_[a-f0-9]{10}$/);

export function databaseEnvironment() {
  const { SIH_TEST_DB_USERNAME: user, SIH_TEST_DB_PASSWORD: password } = process.env;
  if (!user || !password) throw new Error('Set SIH_TEST_DB_USERNAME and SIH_TEST_DB_PASSWORD for the local test catalog.');
  assert.match(user, /^[A-Za-z0-9_.-]+$/);
  for (const key of ['DB_NAME', 'MYSQL_DATABASE', 'SIH_TEST_DB_NAME']) {
    if (process.env[key] && process.env[key].toUpperCase() !== catalog) throw new Error(key + ' conflicts with SIH26135_TEST.');
  }
  const configured = process.env.SIH_TEST_DB_URL;
  if (configured) {
    const url = new URL(configured.replace(/^jdbc:/, ''));
    if (!['localhost', '127.0.0.1'].includes(url.hostname) || url.pathname.toUpperCase() !== '/' + catalog
        || (url.port && url.port !== '3306')) throw new Error('SIH_TEST_DB_URL conflicts with the local test catalog.');
  }
  return { user, password };
}

function mysql() {
  const defaults = ['C:/Program Files/MySQL/MySQL Server 8.0/bin/mysql.exe', 'C:/Program Files/MySQL/MySQL Server 8.4/bin/mysql.exe'];
  return process.env.SIH_E2E_MYSQL ?? defaults.find((path) => existsSync(path)) ?? 'mysql';
}

export function sql(statement) {
  const { user, password } = databaseEnvironment();
  const result = spawnSync(mysql(), ['--no-defaults', '--protocol=TCP', '--host=127.0.0.1', '--port=3306',
    '--user=' + user, '--database=' + catalog, '--batch', '--raw', '--skip-column-names', '--connect-timeout=5',
    '--default-character-set=utf8mb4'], {
    env: { ...process.env, MYSQL_PWD: password },
    input: catalogGuard + ';\nSELECT LOWER(DATABASE());\n' + statement,
    encoding: 'utf8', shell: false, timeout: 25_000, windowsHide: true,
  });
  if (result.error || result.status !== 0) {
    throw new Error('Test MySQL statement failed. ' + (result.error?.message ?? result.stderr.trim()));
  }
  const [proof, database, ...rows] = result.stdout.trimEnd().split(/\r?\n/);
  assert.equal(proof, '1', 'Same-connection catalog guard');
  assert.equal(database, 'sih26135_test', 'Active MySQL catalog');
  return rows.join('\n');
}

export function assertBackendCatalog(id) {
  assertRunId(id);
  const proof = Number(sql(`SELECT COUNT(*) FROM performance_schema.session_connect_attrs a
    JOIN performance_schema.threads t USING (PROCESSLIST_ID)
    WHERE a.ATTR_NAME='sihSmokeRun' AND a.ATTR_VALUE='${id}'
      AND LOWER(t.PROCESSLIST_DB)='sih26135_test';`));
  if (!Number.isSafeInteger(proof) || proof < 1) throw new Error('The owned backend has no tagged connection to SIH26135_TEST.');
}

export const quote = (value) => "'" + String(value).replace(/'/g, "''") + "'";
