import { useState } from 'react';
import { useQuery } from '@tanstack/react-query';
import type { FollowupSubmission, FollowupSubmissionDetail } from '@/api/traineeApi';
import { ErrorState } from '@/components/feedback/ErrorState';
import { LoadingSpinner } from '@/components/feedback/LoadingSpinner';
import { careerDate } from './CareerInsights';

export function SurveySubmissions({ entries, scopeKey, load }: {
  entries: FollowupSubmission[];
  scopeKey: readonly unknown[];
  load: (id: number, signal: AbortSignal) => Promise<FollowupSubmissionDetail>;
}) {
  const [selected, setSelected] = useState<number>();
  const detail = useQuery({
    queryKey: [...scopeKey, 'submission', selected],
    queryFn: ({ signal }) => load(selected!, signal),
    enabled: selected !== undefined && entries.some(entry => entry.id === selected),
    retry: false,
  });

  return (
    <div className="space-y-2 border-t pt-4">
      <h3 className="font-semibold">Recent submissions</h3>
      {entries.length ? <ul className="space-y-2 text-sm">
        {entries.map(entry => <li key={entry.id} className="space-y-3 rounded border p-3">
          <div className="flex flex-wrap items-center justify-between gap-2">
            <p><strong>{entry.name}</strong>{entry.submittedAt && ` · ${careerDate(entry.submittedAt.slice(0, 10))}`}</p>
            <button type="button" className="btn-outline" aria-expanded={selected === entry.id}
              onClick={() => setSelected(selected === entry.id ? undefined : entry.id)}>
              {selected === entry.id ? 'Hide answers' : 'View answers'}
            </button>
          </div>
          {selected === entry.id && (detail.isPending ? <LoadingSpinner label="Loading submitted answers" />
            : detail.isError ? <ErrorState title="Could not load submitted answers" error={detail.error}
              onRetry={() => void detail.refetch()} />
              : detail.data.answers.length > 0 ? <dl className="grid gap-3 border-t pt-3 sm:grid-cols-2">
                {detail.data.answers.map((answer, index) => <div key={index} className="min-w-0">
                  <dt className="text-slate-600">{answer.question}</dt>
                  <dd className="whitespace-pre-wrap break-words font-medium">{answer.value || '—'}</dd>
                </div>)}
              </dl> : <p className="text-slate-600">No answers were recorded.</p>)}
        </li>)}
      </ul> : <p className="text-sm text-slate-600">There are no previous survey submissions.</p>}
    </div>
  );
}
