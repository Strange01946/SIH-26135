import { useId, type ReactNode, type SelectHTMLAttributes } from 'react';
import { useQuery } from '@tanstack/react-query';
import { getEmployerReferences, type EmployerReference } from '@/api/employerApi';
import type { ApiError, PagedResponse } from '@/api/types';

const employerErrorMessages: Record<string, string> = {
  INVALID_EVIDENCE_TYPE: 'Evidence must be a valid PDF or JPEG file.',
  INVALID_EVIDENCE_SIZE: 'Choose a PDF or JPEG file up to 5 MB.',
  INVALID_EVIDENCE_NAME: 'Choose an evidence file with a name of 200 characters or fewer.',
  EVIDENCE_READ_FAILED: 'The evidence file could not be read. Choose the file again.',
  DUPLICATE_PLACEMENT_EMPLOYMENT: 'Joining has already been confirmed for this placement.',
  PLACEMENT_NOT_CONFIRMABLE: 'This placement is unavailable for joining confirmation.',
  VERIFICATION_REQUEST_CLOSED: 'This verification request has already been closed. Refresh the inbox to see the decision.',
  REJECTION_REASON_REQUIRED: 'Select a reason for disputing this employment claim.',
  INVALID_EMPLOYMENT_EXIT: 'Enter a valid separation date, type, and reason for an active employment record.',
};

export function useEmployerReferences() {
  return useQuery({
    queryKey: ['employer', 'references'],
    queryFn: ({ signal }) => getEmployerReferences(signal),
    staleTime: 5 * 60_000,
  });
}

export function EmployerPageHeading({ title, children }: { title: string; children?: ReactNode }) {
  return (
    <div className="flex flex-wrap items-center justify-between gap-3 border-b border-slate-200 pb-4">
      <h1 className="text-2xl font-bold text-navy">{title}</h1>
      {children}
    </div>
  );
}

export function EmployerSelect({ label, options, id, ...props }: SelectHTMLAttributes<HTMLSelectElement> & {
  label: string;
  options: EmployerReference[];
}) {
  const generatedId = useId();
  const fieldId = id ?? generatedId;
  return (
    <div className="space-y-1 text-sm font-medium text-slate-800">
      <label htmlFor={fieldId} className="block">{label}</label>
      <select {...props} id={fieldId} className="form-input">
        <option value="">Select {label.toLowerCase()}</option>
        {options.map((option) => <option key={option.id} value={option.id}>{option.label}</option>)}
      </select>
    </div>
  );
}

export function EmployerPager({ data, onPage }: {
  data: Pick<PagedResponse<unknown>, 'page' | 'totalPages' | 'hasPrevious' | 'hasNext'>;
  onPage: (page: number) => void;
}) {
  if (data.totalPages <= 1 && !data.hasPrevious) return null;
  return (
    <nav aria-label="Pagination" className="flex flex-wrap items-center gap-3 text-sm">
      <button type="button" className="btn-outline" disabled={!data.hasPrevious} onClick={() => onPage(data.page - 1)}>Previous</button>
      <span>Page {data.page + 1} of {Math.max(1, data.totalPages)}</span>
      <button type="button" className="btn-outline" disabled={!data.hasNext} onClick={() => onPage(data.page + 1)}>Next</button>
    </nav>
  );
}

export function EmployerFormError({ error }: { error: Error | ApiError }) {
  const fields = 'fieldErrors' in error ? error.fieldErrors : undefined;
  const message = 'code' in error ? (employerErrorMessages[error.code] ?? error.message) : error.message;
  return (
    <div role="alert" className="rounded border border-red-200 bg-red-50 p-3 text-sm text-red-900">
      <p>{message}</p>
      {fields?.length ? (
        <ul className="mt-1 list-disc pl-5">
          {fields.map((field, index) => <li key={`${field.field}-${index}`}>{field.message}</li>)}
        </ul>
      ) : null}
    </div>
  );
}

export function showStatus(code?: string) {
  return code ? code.toLowerCase().replace(/_/g, ' ').replace(/^./, (letter) => letter.toUpperCase()) : '—';
}

export function formatPay(amount?: number | null, currency = 'INR') {
  if (amount == null) return '—';
  return new Intl.NumberFormat('en-IN', { style: 'currency', currency, maximumFractionDigits: 2 }).format(amount);
}

export function indiaToday() {
  const fields = new Intl.DateTimeFormat('en-IN', {
    timeZone: 'Asia/Kolkata', year: 'numeric', month: '2-digit', day: '2-digit',
  }).formatToParts(new Date());
  return ['year', 'month', 'day'].map((field) => fields.find((part) => part.type === field)?.value).join('-');
}
