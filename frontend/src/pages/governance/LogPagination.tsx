import type { PagedResponse } from '@/api/types';

export function LogPagination({ result, onPage }: { result: PagedResponse<unknown>; onPage: (page: number) => void }) {
  return (
    <div className="flex flex-wrap items-center justify-between gap-3 text-sm text-gray-700">
      <p aria-live="polite">{result.totalElements.toLocaleString('en-IN')} matching records</p>
      {result.totalPages > 1 && <div className="flex flex-wrap items-center gap-3">
        <span>Page {result.page + 1} of {result.totalPages}</span>
        <button type="button" className="btn-secondary" disabled={!result.hasPrevious} onClick={() => onPage(result.page - 1)}>Previous</button>
        <button type="button" className="btn-secondary" disabled={!result.hasNext} onClick={() => onPage(result.page + 1)}>Next</button>
      </div>}
    </div>
  );
}

export const recordedTime = (value: string) => value.replace('T', ' ').slice(0, 19);
