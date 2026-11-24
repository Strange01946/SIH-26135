import { useState } from 'react';
import { useQuery } from '@tanstack/react-query';
import { governanceApi, type EventFilters, type Incident } from '@/api/governanceApi';
import { ErrorState } from '@/components/feedback/ErrorState';
import { Modal } from '@/components/common/Modal';
import { eventTrace } from './governanceDisplay';
import { LogPagination, recordedTime } from './LogPagination';

const severities = [
  { code: 'INFO', label: 'Info', style: 'bg-blue-50 text-blue-900' },
  { code: 'WARNING', label: 'Warn', style: 'bg-amber-50 text-amber-950' },
  { code: 'ERROR', label: 'Error', style: 'bg-red-50 text-red-900' },
  { code: 'CRITICAL', label: 'Critical', style: 'bg-red-900 text-white' },
];

export function IncidentConsole({ initialCorrelation, canViewThrottles, onTrace }: {
  initialCorrelation: string;
  canViewThrottles: boolean;
  onTrace?: (id: string, entityType?: string, entityId?: number) => void;
}) {
  const [draft, setDraft] = useState<EventFilters>({ severityCode: '', eventCode: '', correlationId: initialCorrelation });
  const [filters, setFilters] = useState(draft);
  const [page, setPage] = useState(0);
  const [selected, setSelected] = useState<Incident>();
  const events = useQuery({
    queryKey: ['system-events', filters, page],
    queryFn: ({ signal }) => governanceApi.incidents(filters, page, signal),
    refetchInterval: 3_000,
    retry: false,
  });
  const update = (key: keyof EventFilters, value: string) => setDraft({ ...draft, [key]: value });
  const showRequest = (correlationId: string) => {
    const trace = { severityCode: '', eventCode: '', correlationId };
    setDraft(trace); setFilters(trace); setPage(0); setSelected(undefined);
  };

  return (
    <div className="space-y-5">
      {canViewThrottles && <ThrottlePanel />}
      <section className="card space-y-4" aria-labelledby="incident-feed">
        <div className="flex flex-wrap items-start justify-between gap-3">
          <div>
            <h2 id="incident-feed" className="text-lg font-bold text-navy">System event feed</h2>
            <p className="mt-1 text-sm text-gray-600">Search by X-Correlation-ID to follow events linked to one request.</p>
          </div>
          <button type="button" className="btn-secondary" disabled={events.isFetching} onClick={() => void events.refetch()}>Refresh events</button>
        </div>
        <form className="grid items-end gap-3 sm:grid-cols-2 lg:grid-cols-4" onSubmit={(event) => {
          event.preventDefault(); setFilters(draft); setPage(0);
        }}>
          <label className="text-sm font-semibold text-gray-700">X-Correlation-ID
            <input className="form-input mt-1 w-full font-mono" maxLength={64} pattern="[A-Za-z0-9_\-]{1,64}"
              value={draft.correlationId} onChange={(event) => update('correlationId', event.target.value)} />
          </label>
          <label className="text-sm font-semibold text-gray-700">Severity
            <select className="form-input mt-1 w-full" value={draft.severityCode} onChange={(event) => update('severityCode', event.target.value)}>
              <option value="">All severities</option>
              {severities.map(({ code, label }) => <option key={code} value={code}>{label}</option>)}
            </select>
          </label>
          <label className="text-sm font-semibold text-gray-700">Event code
            <input className="form-input mt-1 w-full" maxLength={64} value={draft.eventCode}
              onChange={(event) => update('eventCode', event.target.value)} />
          </label>
          <div className="flex flex-wrap gap-2">
            <button type="submit" className="btn-primary">Search events</button>
            <button type="button" className="btn-secondary" onClick={() => showRequest('')}>Clear</button>
          </div>
        </form>
        {events.isPending && <p role="status">Loading system events…</p>}
        {events.isError && <ErrorState title="Unable to load system events" error={events.error} onRetry={() => void events.refetch()} />}
        {events.data && <>
          <LogPagination result={events.data} onPage={setPage} />
          {events.data.content.length ? (
            <div className="overflow-x-auto">
              <table className="w-full min-w-[42rem] text-left text-sm">
                <thead className="bg-gray-50 text-gray-700"><tr>
                  {['Recorded', 'Severity', 'Event', 'Summary', 'X-Correlation-ID', 'Details'].map((heading) =>
                    <th key={heading} className="px-3 py-2" scope="col">{heading}</th>)}
                </tr></thead>
                <tbody className="divide-y divide-gray-100">
                  {events.data.content.map((incident) => {
                    const severity = severities.find(({ code }) => code === incident.systemEventSeverityCode);
                    const trace = eventTrace(incident.eventSummary);
                    return <tr key={incident.id}>
                      <td className="whitespace-nowrap px-3 py-3">{recordedTime(incident.occurredAt)}</td>
                      <td className="px-3 py-3"><span className={`whitespace-nowrap rounded-full px-2.5 py-1 text-xs font-semibold ${severity?.style ?? 'bg-gray-100 text-gray-700'}`}>
                        {severity?.label ?? incident.systemEventSeverityCode}</span></td>
                      <td className="px-3 py-3"><div className="break-words font-semibold text-navy">{incident.eventCode}</div>
                        <div className="text-xs text-gray-600">{incident.systemEventCategoryName}</div></td>
                      <td className="min-w-40 break-words px-3 py-3">{trace.message || '—'}</td>
                      <td className="break-all px-3 py-3 font-mono text-xs">{trace.correlationId ?? '—'}</td>
                      <td className="whitespace-nowrap px-3 py-3"><button type="button" className="font-semibold text-navy underline"
                        aria-label={`View event #${incident.id}`} onClick={() => setSelected(incident)}>View event</button></td>
                    </tr>;
                  })}
                </tbody>
              </table>
            </div>
          ) : <p className="rounded-lg bg-gray-50 p-4 text-sm text-gray-700">No system events match these filters.</p>}
        </>}
      </section>
      {selected && <IncidentDetail event={selected} onClose={() => setSelected(undefined)} onFilter={showRequest} onTrace={onTrace} />}
    </div>
  );
}

function IncidentDetail({ event, onClose, onFilter, onTrace }: {
  event: Incident;
  onClose: () => void;
  onFilter: (id: string) => void;
  onTrace?: (id: string, entityType?: string, entityId?: number) => void;
}) {
  const { correlationId, requestId, message } = eventTrace(event.eventSummary);
  return (
    <Modal open onClose={onClose} title={`Event #${event.id}`}>
      <div className="space-y-4 p-6 text-sm text-gray-700">
        <p className="font-semibold text-navy">{event.eventCode}</p>
        <p className="break-words">{message}</p>
        <dl className="grid gap-2 sm:grid-cols-[auto_minmax(0,1fr)]">
          <dt>Recorded</dt><dd>{recordedTime(event.occurredAt)}</dd>
          <dt>Source</dt><dd className="break-words">{event.sourceComponent ?? '—'}</dd>
          <dt>X-Correlation-ID</dt><dd className="break-all font-mono">{correlationId ?? '—'}</dd>
          <dt>X-Request-ID</dt><dd className="break-all font-mono">{requestId ?? '—'}</dd>
        </dl>
        {correlationId && <div className="flex flex-wrap gap-2">
          <button type="button" className="btn-secondary" onClick={() => onFilter(correlationId)}>Filter by this request</button>
          {onTrace && <button type="button" className="btn-secondary"
            onClick={() => onTrace(correlationId, event.entityType, event.entityId)}>Find related audit entries</button>}
        </div>}
      </div>
    </Modal>
  );
}

function ThrottlePanel() {
  const throttles = useQuery({
    queryKey: ['system-events', 'throttle-status'], queryFn: ({ signal }) => governanceApi.throttles(signal), refetchInterval: 8_000, retry: false,
  });
  const data = throttles.data;
  const guards = data ? [
    { label: 'Duplicate events', seconds: data.eventDedupWindowSeconds, suppressed: data.suppressedSinceStartup.duplicateEvents },
    { label: 'Rate-limit alerts', seconds: data.rateLimitCooldownSeconds, suppressed: data.suppressedSinceStartup.rateLimitAlerts },
    { label: 'Data-quality spike alerts', seconds: data.dataQualityCooldownSeconds, suppressed: data.suppressedSinceStartup.dataQualityAlerts },
  ] : [];
  return (
    <section className="card space-y-3" aria-labelledby="throttling">
      <h2 id="throttling" className="text-lg font-bold text-navy">Event throttling</h2>
      {throttles.isPending && <p role="status">Loading throttle status…</p>}
      {throttles.isError && <ErrorState title="Unable to load throttle status" error={throttles.error} onRetry={() => void throttles.refetch()} />}
      {data && <dl className="grid gap-3 sm:grid-cols-3">
        {guards.map(({ label, seconds, suppressed }) => <div key={label} className="rounded-lg border border-gray-200 p-3 text-sm">
          <dt className="font-semibold text-navy">{label}</dt>
          <dd className="mt-1 text-gray-700">{seconds > 0 ? `Active · ${seconds}s window` : 'Disabled'}</dd>
          <dd className="text-gray-600">{suppressed.toLocaleString('en-IN')} suppressed since startup</dd>
        </div>)}
      </dl>}
    </section>
  );
}
