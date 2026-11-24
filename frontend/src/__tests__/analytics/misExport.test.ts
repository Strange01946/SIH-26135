import { describe, expect, it, vi } from 'vitest';
import { makeExportUrl } from '@/api/specializedAnalyticsApi';
import { writeMisStream } from '@/lib/streamMisExport';

const encoder = new TextEncoder();

function streamResponse(chunks: string[], headers: HeadersInit = {}) {
  return new Response(new ReadableStream<Uint8Array>({
    start(controller) {
      for (const chunk of chunks) controller.enqueue(encoder.encode(chunk));
      controller.close();
    },
  }), { headers });
}

describe('Streaming MIS exports', () => {
  it('writes report chunks and reports a measurable byte total', async () => {
    const chunks: string[] = [];
    const written = new WritableStream<Uint8Array>({
      write(chunk) { chunks.push(new TextDecoder().decode(chunk)); },
    });
    const progress = vi.fn();
    const result = await writeMisStream(streamResponse(['A,', '₹12'], { 'content-length': '7' }),
      written, new AbortController().signal, progress);
    expect(chunks).toEqual(['A,', '₹12']);
    expect(result).toEqual({ bytes: 7, totalBytes: 7 });
    expect(progress).toHaveBeenLastCalledWith(result);
  });

  it('does not claim a percentage when the stream length is unknown or compressed', async () => {
    for (const headers of [new Headers(), new Headers({ 'content-length': '55', 'content-encoding': 'gzip' })]) {
      const result = await writeMisStream(streamResponse(['a', 'b'], headers), new WritableStream(),
        new AbortController().signal, vi.fn());
      expect(result).toEqual({ bytes: 2, totalBytes: undefined });
    }
  });

  it('aborts the destination for a truncated report with a declared length', async () => {
    const abort = vi.fn();
    const close = vi.fn();
    await expect(writeMisStream(streamResponse(['abc'], { 'content-length': '4' }),
      new WritableStream({ abort, close }), new AbortController().signal, vi.fn())).rejects.toThrow('ended before');
    expect(abort).toHaveBeenCalledOnce();
    expect(close).not.toHaveBeenCalled();
  });

  it('uses the candidate column allowlist parameters and preserves report-specific filters', () => {
    const cohort = { fiscalYear: 'FY2026-27', districtId: 1001, gender: 'FEMALE' };
    const candidate = new URL(makeExportUrl('candidates', 'csv', cohort, ['state', 'outcomeStage'], 'Q3'), 'http://localhost');
    expect(candidate.pathname).toBe('/api/v1/analytics/export/candidates/csv');
    expect(Object.fromEntries(candidate.searchParams)).toEqual({
      fiscalYear: 'FY2026-27', districtId: '1001', gender: 'FEMALE', columns: 'state,outcomeStage',
    });
    const statutory = new URL(makeExportUrl('qpr', 'json', cohort, ['state'], 'Q3'), 'http://localhost');
    expect(statutory.searchParams.get('quarter')).toBe('Q3');
    expect(statutory.searchParams.has('columns')).toBe(false);
  });
});
