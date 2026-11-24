import assert from 'node:assert/strict';
import { readFileSync } from 'node:fs';
import Ajv2020 from 'ajv/dist/2020.js';

const apiId = 'https://sih26135.invalid/verified-openapi';
const spec = JSON.parse(readFileSync(new URL('../../../docs/openapi/openapi-0-all-apis.json', import.meta.url), 'utf8'));
const ajv = new Ajv2020({ strict: false, allErrors: true, validateFormats: false });
ajv.addSchema({ $id: apiId, components: spec.components, paths: spec.paths });
const validators = new Map();
const pointer = (value) => String(value).replaceAll('~', '~0').replaceAll('/', '~1');

export async function expectJson(response, method, path, status, keys = []) {
  if (response.status() !== status) {
    const error = await response.json().catch(() => undefined);
    assert.equal(response.status(), status, method + ' ' + path + (error?.code ? ' returned ' + error.code : ''));
  }
  assert.match(response.headers()['content-type'] ?? '', /application\/json/i);
  const json = await response.json();
  assert(json.success === true && json.data != null, method + ' ' + path + ' must return a successful data envelope');
  for (const key of keys) assert(key in json.data, path + ' missing runtime field ' + key);
  const operation = spec.paths[path]?.[method.toLowerCase()];
  const responseSpec = operation?.responses?.[status];
  const content = responseSpec?.content;
  const contentType = content?.['application/json'] ? 'application/json' : '*/*';
  assert(content?.[contentType]?.schema, 'No verified JSON response schema for ' + method + ' ' + path + ' ' + status);
  const ref = apiId + '#/paths/' + pointer(path) + '/' + method.toLowerCase() + '/responses/' + status + '/content/' + pointer(contentType) + '/schema';
  if (!validators.has(ref)) validators.set(ref, ajv.compile({ $ref: ref }));
  const validate = validators.get(ref);
  assert(validate(json), method + ' ' + path + ' violated OpenAPI: ' + ajv.errorsText(validate.errors));
  return json.data;
}
