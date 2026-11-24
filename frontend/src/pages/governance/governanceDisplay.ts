import type { JsonObject, JsonValue } from '@/api/governanceApi';

export function auditAction(code: string) {
  if (code === 'STATUS_CHANGE') return 'Transition';
  if (code === 'SOFT_DELETE') return 'Soft delete';
  return code.charAt(0) + code.slice(1).toLowerCase().replace(/_/g, ' ');
}

export function eventTrace(summary: string) {
  const match = summary.match(/^\[cid=([A-Za-z0-9_-]{1,64})(?:\|rid=([A-Za-z0-9_-]{1,64}))?\](?:\s|$)/);
  return match ? { correlationId: match[1], requestId: match[2], message: summary.slice(match[0].length) }
    : { correlationId: undefined, requestId: undefined, message: summary };
}

function canonicalJson(value: JsonValue | undefined): string | undefined {
  if (value === undefined) return undefined;
  if (value === null || typeof value !== 'object') return JSON.stringify(value);
  if (Array.isArray(value)) return '[' + value.map(canonicalJson).join(',') + ']';
  return '{' + Object.keys(value).sort().map((key) => JSON.stringify(key) + ':' + canonicalJson(value[key])).join(',') + '}';
}

export function diffFields(before: JsonObject | null, after: JsonObject | null, redactedFields: string[]) {
  const redacted = new Set(redactedFields);
  const fields = [...new Set([...Object.keys(before ?? {}), ...Object.keys(after ?? {}), ...redacted])].sort();
  return fields.map((field) => {
    const oldValue = before && Object.prototype.hasOwnProperty.call(before, field) ? before[field] : undefined;
    const newValue = after && Object.prototype.hasOwnProperty.call(after, field) ? after[field] : undefined;
    const withheld = redacted.has(field);
    const from = canonicalJson(oldValue);
    const to = canonicalJson(newValue);
    return {
      field,
      before: withheld ? '"[REDACTED]"' : from,
      after: withheld ? '"[REDACTED]"' : to,
      changed: withheld || from !== to,
    };
  });
}

const telemetryMetrics = new Set([
  'process_uptime_seconds', 'jvm_threads_live_threads', 'hikaricp_connections_active', 'http_server_requests_seconds_count',
]);

export function readTelemetry(text: string) {
  const totals: Record<string, number> = {};
  for (const line of text.split(/\r?\n/)) {
    if (line.startsWith('#')) continue;
    const match = /^([\w:]+)(?:\{.*\})?\s+(\S+)/.exec(line);
    if (!match || !telemetryMetrics.has(match[1])) continue;
    const reading = Number(match[2]);
    if (Number.isFinite(reading)) totals[match[1]] = (totals[match[1]] ?? 0) + reading;
  }
  return totals;
}
