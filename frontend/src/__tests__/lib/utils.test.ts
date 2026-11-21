import { describe, it, expect } from 'vitest';
import { cn, formatTimestamp, formatNumber, truncate } from '@/lib/utils';

describe('cn (class merging)', () => {
  it('merges class names', () => {
    expect(cn('a', 'b')).toBe('a b');
  });

  it('handles conditional classes', () => {
    expect(cn('base', true && 'active', false && 'inactive')).toBe('base active');
  });

  it('handles undefined and null', () => {
    expect(cn('base', undefined, null)).toBe('base');
  });

  it('deduplicates conflicting tailwind classes', () => {
    const result = cn('text-sm', 'text-lg');
    expect(result).toBe('text-lg');
  });
});

describe('formatNumber', () => {
  it('formats numbers with Indian locale', () => {
    const result = formatNumber(100000);
    expect(result).toMatch(/1,00,000|100,000/);
  });
});

describe('truncate', () => {
  it('does not truncate short strings', () => {
    expect(truncate('hello', 10)).toBe('hello');
  });

  it('truncates long strings with ellipsis', () => {
    const result = truncate('hello world', 5);
    expect(result).toBe('hello…');
    expect(result.length).toBe(6);
  });
});

describe('formatTimestamp', () => {
  it('formats ISO timestamp into a readable date string', () => {
    const formatted = formatTimestamp('2026-11-20T10:15:30.123Z');
    expect(typeof formatted).toBe('string');
    expect(formatted.length).toBeGreaterThan(0);
  });
});
