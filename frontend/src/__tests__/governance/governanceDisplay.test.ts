import { describe, expect, it } from 'vitest';
import { diffFields, eventTrace, readTelemetry } from '@/pages/governance/governanceDisplay';

describe('audit state comparison', () => {
  it('distinguishes missing and null values, keeps redacted changes, and ignores object key order', () => {
    const fields = diffFields(
      { status: 'OPEN', note: null, nested: { a: 1, b: 2 }, removed: 0 },
      { status: 'ONGOING', note: null, nested: { b: 2, a: 1 }, added: false },
      ['email'],
    );
    expect(fields.filter((row) => row.changed)).toEqual([
      { field: 'added', before: undefined, after: 'false', changed: true },
      { field: 'email', before: '"[REDACTED]"', after: '"[REDACTED]"', changed: true },
      { field: 'removed', before: '0', after: undefined, changed: true },
      { field: 'status', before: '"OPEN"', after: '"ONGOING"', changed: true },
    ]);
    expect(fields.find((row) => row.field === 'note')).toMatchObject({ before: 'null', after: 'null', changed: false });
  });
});

describe('incident and telemetry display', () => {
  it('recognizes only the leading server correlation marker', () => {
    expect(eventTrace('[cid=qa-1|rid=req-1] Dispatch failed')).toEqual({
      correlationId: 'qa-1', requestId: 'req-1', message: 'Dispatch failed',
    });
    expect(eventTrace('Message contains [cid=forged]').correlationId).toBeUndefined();
  });

  it('aggregates only selected Prometheus metrics and ignores invalid samples', () => {
    expect(readTelemetry([
      'http_server_requests_seconds_count{uri="/api/one"} 12',
      'http_server_requests_seconds_count{uri="/api/two"} 8',
      'jvm_threads_live_threads 3.4e1',
      'hikaricp_connections_active{pool="main"} NaN',
      'private_metric{label="hidden"} 7',
    ].join('\n'))).toEqual({ http_server_requests_seconds_count: 20, jvm_threads_live_threads: 34 });
  });
});
