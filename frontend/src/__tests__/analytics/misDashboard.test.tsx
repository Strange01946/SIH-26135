import { fireEvent, render, screen, within } from '@testing-library/react';
import { describe, expect, it, vi } from 'vitest';
import { getMisSnapshot, type PipelineStage } from '@/api/misApi';
import { apiGet } from '@/api/client';
import { TrainingFunnel } from '@/pages/analytics/MisOverview';
import { MisTable, type TableColumn } from '@/pages/analytics/MisUi';

vi.mock('@/api/client', () => ({ apiGet: vi.fn() }));

describe('Government MIS', () => {
  it('requests Q01–Q11 with the same cohort and abort signal', async () => {
    const filters = { fiscalYear: 'FY2026-27', schemeId: 1, sectorId: 2, stateId: 3, districtId: 4, gender: 'FEMALE' };
    const { signal } = new AbortController();
    vi.mocked(apiGet).mockResolvedValue({ items: [] });

    await getMisSnapshot(filters, signal);

    expect(vi.mocked(apiGet).mock.calls.map(([path]) => path.split('/dashboards/')[1])).toEqual([
      'kpis', 'pipeline-funnel', 'scheme-matrix', 'providers', 'courses',
      'geography', 'employers', 'economic-outcomes', 'mis/provider-quality',
    ]);
    for (const [path, config] of vi.mocked(apiGet).mock.calls) {
      const executive = path.endsWith('/kpis') || path.endsWith('/pipeline-funnel');
      expect(config?.params).toEqual(executive ? { ...filters, executive: true } : filters);
      expect(config?.signal).toBe(signal);
    }
  });

  it('shows suppression without plotting a hidden stage or changing its type to zero', () => {
    const stages: PipelineStage[] = [
      { key: 'registered', candidates: { availability: 'NOT_APPLICABLE', value: null } },
      { key: 'enrolled', candidates: { availability: 'SUPPRESSED', value: null } },
      { key: 'assessed', candidates: { availability: 'RELEASED', value: 0 } },
    ];
    const { container } = render(<TrainingFunnel pipeline={stages} />);
    const [registered, enrolled, assessed] = within(screen.getByRole('list', { name: 'Candidate lifecycle stages' })).getAllByRole('listitem');

    expect(registered).toHaveTextContent('Outside this cohort');
    expect(enrolled).toHaveTextContent('Suppressed');
    expect(enrolled.querySelector('[style*="width"]')).toBeNull();
    expect(assessed).toHaveTextContent('0');
    expect(container.querySelectorAll('[style*="width"]')).toHaveLength(1);
    fireEvent.click(within(assessed).getByRole('button'));
    expect(screen.getByText('Candidates who appeared for an assessment linked to a qualifying enrollment.')).toBeInTheDocument();
  });

  it('ranks released values and assigns no position from hidden values', () => {
    const rows = [{ name: 'Withheld', score: null }, { name: 'Published zero', score: 0 }, { name: 'Published high', score: 90 }];
    const columns: TableColumn<typeof rows[number]>[] = [
      { key: 'name', title: 'Provider', value: (row) => row.name },
      { key: 'score', title: 'Placement', value: (row) => row.score, format: 'percent', sortable: true },
    ];
    render(<MisTable title="Providers" rows={rows} columns={columns} rowKey={(row) => row.name} initialSort="score" />);
    const table = screen.getByRole('table', { name: 'Providers' });
    const order = () => within(table).getAllByRole('rowheader').map((header) => header.textContent);
    expect(order()).toEqual(['Published high', 'Published zero', 'Withheld']);
    expect(within(table).getByText('0%')).toBeInTheDocument();
    expect(within(table).getByText('Suppressed')).toBeInTheDocument();
    fireEvent.click(within(table).getByRole('button', { name: 'Placement' }));
    expect(order()).toEqual(['Published zero', 'Published high', 'Withheld']);
  });
});
