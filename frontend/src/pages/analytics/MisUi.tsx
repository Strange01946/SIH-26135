import { useState, type ReactNode } from 'react';
import type { Measure } from '@/api/misApi';

export type Format = 'count' | 'percent' | 'rupees' | 'days';
const number = new Intl.NumberFormat('en-IN', { maximumFractionDigits: 2 });
const rupees = new Intl.NumberFormat('en-IN', { style: 'currency', currency: 'INR', maximumFractionDigits: 2, minimumFractionDigits: 0 });

export function formatMetric(value: number, format: Format) {
  if (format === 'rupees') return rupees.format(value);
  if (format === 'percent') return `${number.format(value)}%`;
  if (format === 'days') return `${number.format(value)} days`;
  return number.format(value);
}

export function Suppressed({ ambiguous = false, focusable = true }: { ambiguous?: boolean; focusable?: boolean }) {
  const title = ambiguous
    ? 'No eligible observations were available, or the backend withheld the value under the k = 5 disclosure policy.'
    : 'The backend withheld this value under the k = 5 disclosure policy. Related values may also be withheld to protect small groups.';
  return (
    <span title={title} aria-label={title} tabIndex={focusable ? 0 : undefined}
      className="inline-flex cursor-help items-center rounded border border-amber-200 bg-amber-50 px-2 py-0.5 text-xs font-semibold text-amber-900 focus-visible:outline-2">
      {ambiguous ? 'Withheld or unavailable' : 'Suppressed'}
    </span>
  );
}

export function Value({ value, format = 'count', noData = false, ambiguous = false }: {
  value?: number | null; format?: Format; noData?: boolean; ambiguous?: boolean;
}) {
  return value == null ? noData ? <span className="text-sm text-slate-600">No data</span>
    : <Suppressed ambiguous={ambiguous} /> : <>{formatMetric(value, format)}</>;
}

export function MeasureValue({ measure, format = 'count', focusable = true }: { measure?: Measure; format?: Format; focusable?: boolean }) {
  if (measure?.availability === 'SUPPRESSED') return <Suppressed focusable={focusable} />;
  if (measure?.availability === 'NOT_APPLICABLE') return <span className="text-sm text-slate-600">Outside this cohort</span>;
  return <Value value={measure?.availability === 'RELEASED' ? measure.value : null} format={format} noData />;
}

export function Panel({ code, title, children, className }: {
  code: string; title: string; children: ReactNode; className?: string;
}) {
  return (
    <section className={`card min-w-0 space-y-4 p-4 sm:p-5 ${className ?? ''}`} aria-labelledby={`mis-${code}`}>
      <h2 id={`mis-${code}`} className="section-title flex flex-wrap items-center gap-x-3 gap-y-1 text-navy">
        {title}<span className="text-xs font-semibold tracking-wide text-slate-500">{code}</span>
      </h2>
      {children}
    </section>
  );
}

type CellValue = string | number | null | undefined;
export type TableColumn<T> = {
  key: string; title: string; value: (row: T) => CellValue;
  format?: Format; sortable?: boolean; ambiguous?: boolean;
  noData?: (row: T) => boolean; render?: (row: T) => ReactNode;
};

export function MisTable<T>({ title, rows, columns, rowKey, initialSort }: {
  title: string; rows: T[]; columns: TableColumn<T>[]; rowKey: (row: T) => string | number | undefined; initialSort?: string;
}) {
  const [sort, setSort] = useState({ key: initialSort ?? '', ascending: false });
  const [page, setPage] = useState(0);
  const field = columns.find((column) => column.key === sort.key);
  const ordered = [...rows].sort((a, b) => {
    const x = field?.value(a);
    const y = field?.value(b);
    if (x == null || y == null) {
      if (x == null && y != null) return 1;
      if (y == null && x != null) return -1;
    } else if (x !== y) {
      return (typeof x === 'number' && typeof y === 'number' ? x - y : String(x).localeCompare(String(y)))
        * (sort.ascending ? 1 : -1);
    }
    return String(columns[0].value(a) ?? '').localeCompare(String(columns[0].value(b) ?? ''));
  });
  const size = 12;
  const currentPage = Math.min(page, Math.max(0, Math.ceil(ordered.length / size) - 1));
  if (rows.length === 0) return <p className="rounded-md bg-slate-50 p-4 text-sm text-slate-600">No matching records for these filters.</p>;
  return (
    <div className="space-y-3">
      <div className="overflow-x-auto rounded-md border border-slate-200">
        <table aria-label={title} className="w-full min-w-max text-left text-sm">
          <thead className="bg-slate-100 text-xs font-semibold text-slate-700">
            <tr>{columns.map((column) => (
              <th key={column.key} scope="col" className="px-3 py-2.5" aria-sort={sort.key === column.key
                ? sort.ascending ? 'ascending' : 'descending' : undefined}>
                {column.sortable ? (
                  <button className="rounded px-1 py-1 text-left font-semibold hover:bg-slate-200 focus-visible:outline-2" type="button"
                    onClick={() => { setPage(0); setSort({ key: column.key, ascending: sort.key === column.key && !sort.ascending }); }}>
                    {column.title} <span aria-hidden="true">{sort.key === column.key ? sort.ascending ? '↑' : '↓' : '↕'}</span>
                  </button>
                ) : column.title}
              </th>
            ))}</tr>
          </thead>
          <tbody className="divide-y divide-slate-200">
            {ordered.slice(currentPage * size, currentPage * size + size).map((row, index) => (
              <tr key={rowKey(row) ?? index} className="odd:bg-white even:bg-slate-50/70">
                {columns.map((column, colIndex) => {
                  const raw = column.value(row);
                  const content = column.render ? column.render(row) : column.format
                    ? <Value value={typeof raw === 'number' ? raw : null} format={column.format}
                        noData={column.noData?.(row)} ambiguous={column.ambiguous} />
                    : raw ?? '—';
                  return colIndex === 0
                    ? <th key={column.key} scope="row" className="px-3 py-3 font-semibold text-navy">{content}</th>
                    : <td key={column.key} className="px-3 py-3 tabular-nums">{content}</td>;
                })}
              </tr>
            ))}
          </tbody>
        </table>
      </div>
      {rows.length > size && (
        <div className="flex flex-wrap items-center justify-between gap-2">
          <p className="text-xs text-slate-600">Showing {currentPage * size + 1}–{Math.min((currentPage + 1) * size, rows.length)} of {rows.length}</p>
          <div className="flex gap-2">
            <button type="button" className="btn-secondary" disabled={currentPage === 0} onClick={() => setPage(currentPage - 1)}>Previous</button>
            <button type="button" className="btn-secondary" disabled={(currentPage + 1) * size >= rows.length} onClick={() => setPage(currentPage + 1)}>Next</button>
          </div>
        </div>
      )}
    </div>
  );
}
