import { useState } from 'react';
import { useQuery } from '@tanstack/react-query';
import { governanceApi, type AuditEntry, type AuditFilters } from '@/api/governanceApi';
import { ErrorState } from '@/components/feedback/ErrorState';
import { Modal } from '@/components/common/Modal';
import { auditAction, diffFields } from './governanceDisplay';
import { LogPagination, recordedTime } from './LogPagination';

const emptyFilters: AuditFilters = { entityType: '', entityId: '', actorUserId: '', auditActionCode: '', start: '', end: '', correlationId: '' };
const actionCodes = ['CREATE', 'UPDATE', 'DELETE', 'SOFT_DELETE', 'STATUS_CHANGE', 'RESTORE', 'EXPORT'];
const entityTypes = ['Trainee', 'TrainingEnrollment', 'TrainingBatch', 'TrainingCenter', 'AssessmentResult', 'Placement',
  'EmploymentRecord', 'EmploymentVerification', 'FollowupTask', 'SurveyResponse', 'SkillGapRecommendation', 'ImportBatch', 'User'];

export function AuditExplorer({ initial, onTrace }: { initial: Partial<AuditFilters>; onTrace?: (id: string) => void }) {
  const [draft, setDraft] = useState<AuditFilters>({ ...emptyFilters, ...initial });
  const [filters, setFilters] = useState(draft);
  const [page, setPage] = useState(0);
  const [selected, setSelected] = useState<AuditEntry>();
  const scope = useQuery({ queryKey: ['audit', 'scope'], queryFn: ({ signal }) => governanceApi.auditScope(signal) });
  const allowedSearch = scope.isSuccess && (scope.data.nationwide || Boolean(filters.entityType.trim() && filters.entityId));
  const entries = useQuery({
    queryKey: ['audit', 'entries', filters, page],
    queryFn: ({ signal }) => governanceApi.audits(filters, page, signal),
    enabled: allowedSearch,
    refetchInterval: 2_000,
    retry: false,
  });
  const badRange = draft.start && draft.end && draft.start > draft.end;
  const update = (key: keyof AuditFilters, value: string) => setDraft({ ...draft, [key]: value });

  return (
    <section className="card space-y-4" aria-labelledby="audit-trail">
      <div className="flex flex-wrap items-start justify-between gap-3">
        <div>
          <h2 id="audit-trail" className="text-lg font-bold text-navy">Immutable audit trail</h2>
          <p className="mt-1 text-sm text-gray-600">Changes appear as they are recorded. Use backend local time for date filters.</p>
        </div>
        <button type="button" className="btn-secondary" disabled={!allowedSearch || entries.isFetching}
          onClick={() => void entries.refetch()}>Refresh audit trail</button>
      </div>
      <form className="grid gap-3 sm:grid-cols-2 lg:grid-cols-4" onSubmit={(event) => {
        event.preventDefault();
        if (!badRange) { setFilters(draft); setPage(0); }
      }}>
        <label className="text-sm font-semibold text-gray-700">Entity type
          <input className="form-input mt-1 w-full" list="audit-entity-types" maxLength={64} value={draft.entityType}
            onChange={(event) => update('entityType', event.target.value)} />
          <datalist id="audit-entity-types">{entityTypes.map((type) => <option value={type} key={type} />)}</datalist>
        </label>
        <label className="text-sm font-semibold text-gray-700">Entity ID
          <input className="form-input mt-1 w-full" type="number" min={1} value={draft.entityId}
            onChange={(event) => update('entityId', event.target.value)} />
        </label>
        <label className="text-sm font-semibold text-gray-700">User ID
          <input className="form-input mt-1 w-full" type="number" min={1} value={draft.actorUserId}
            onChange={(event) => update('actorUserId', event.target.value)} />
        </label>
        <label className="text-sm font-semibold text-gray-700">Audit action
          <select className="form-input mt-1 w-full" value={draft.auditActionCode}
            onChange={(event) => update('auditActionCode', event.target.value)}>
            <option value="">All actions</option>
            {actionCodes.map((code) => <option value={code} key={code}>{auditAction(code)}</option>)}
          </select>
        </label>
        <label className="text-sm font-semibold text-gray-700">From
          <input className="form-input mt-1 w-full" type="datetime-local" step={1} value={draft.start}
            onChange={(event) => update('start', event.target.value)} />
        </label>
        <label className="text-sm font-semibold text-gray-700">To
          <input className="form-input mt-1 w-full" type="datetime-local" step={1} value={draft.end}
            onChange={(event) => update('end', event.target.value)} />
        </label>
        <label className="text-sm font-semibold text-gray-700">X-Correlation-ID
          <input className="form-input mt-1 w-full font-mono" maxLength={64} value={draft.correlationId}
            onChange={(event) => update('correlationId', event.target.value)} />
        </label>
        <div className="flex flex-wrap items-end gap-2">
          <button type="submit" className="btn-primary" disabled={Boolean(badRange)}>Apply filters</button>
          <button type="button" className="btn-secondary" onClick={() => {
            setDraft(emptyFilters); setFilters(emptyFilters); setPage(0);
          }}>Clear</button>
        </div>
      </form>
      {badRange && <p role="alert" className="text-sm text-red-800">The end time must follow the start time.</p>}
      {scope.isPending && <p role="status">Checking audit access…</p>}
      {scope.isError && <ErrorState title="Unable to check audit access" error={scope.error} onRetry={() => void scope.refetch()} />}
      {scope.isSuccess && !allowedSearch && <p className="rounded-lg bg-gray-50 p-4 text-sm text-gray-700">Enter an entity type and ID within your assigned scope.</p>}
      {allowedSearch && entries.isPending && <p role="status">Loading audit entries…</p>}
      {entries.isError && <ErrorState title="Unable to load audit entries" error={entries.error} onRetry={() => void entries.refetch()} />}
      {entries.data && <>
        <LogPagination result={entries.data} onPage={setPage} />
        {entries.data.content.length ? (
          <div className="overflow-x-auto">
            <table className="w-full min-w-[40rem] text-left text-sm">
              <thead className="bg-gray-50 text-gray-700">
                <tr>{['Recorded', 'Action', 'Entity', 'User ID', 'Summary', 'Details'].map((text) => (
                  <th key={text} className="px-3 py-2" scope="col">{text}</th>
                ))}</tr>
              </thead>
              <tbody className="divide-y divide-gray-100">
                {entries.data.content.map((row) => (
                  <tr key={row.id}>
                    <td className="whitespace-nowrap px-3 py-3">{recordedTime(row.occurredAt)}</td>
                    <td className="px-3 py-3"><span className="whitespace-nowrap rounded-full bg-navy-50 px-2.5 py-1 text-xs font-semibold text-navy">{auditAction(row.auditActionCode)}</span></td>
                    <td className="whitespace-nowrap px-3 py-3">{row.entityType}{row.entityId != null && ' #' + row.entityId}</td>
                    <td className="px-3 py-3">{row.actorUserId ?? 'System'}</td>
                    <td className="px-3 py-3 break-words">{row.changeSummary || '—'}</td>
                    <td className="whitespace-nowrap px-3 py-3">
                      <button type="button" className="font-semibold text-navy underline" onClick={() => setSelected(row)}
                        aria-label={`${row.stateChange ? 'View diff' : 'View entry'} #${row.id}`}>{row.stateChange ? 'View diff' : 'View entry'}</button>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        ) : <p className="rounded-lg bg-gray-50 p-4 text-sm text-gray-700">No audit entries match these filters.</p>}
      </>}
      {selected && <AuditDetail key={selected.id} entry={selected} onClose={() => setSelected(undefined)} onTrace={onTrace} />}
    </section>
  );
}

function AuditDetail({ entry, onClose, onTrace }: { entry: AuditEntry; onClose: () => void; onTrace?: (id: string) => void }) {
  const [changedOnly, setChangedOnly] = useState(true);
  const state = entry.stateChange;
  const rows = state ? diffFields(state.before, state.after, state.redactedFields) : [];
  const shown = changedOnly ? rows.filter((row) => row.changed) : rows;

  return (
    <Modal open onClose={onClose} title={`Audit entry #${entry.id}`} className="sm:max-w-3xl">
      <div className="space-y-4 p-5 text-sm sm:p-6">
        <p className="font-semibold text-navy">{entry.entityType}{entry.entityId != null && ' #' + entry.entityId} · {auditAction(entry.auditActionCode)}</p>
        <p className="break-words text-gray-700">{entry.changeSummary || 'No summary was recorded.'}</p>
        {entry.correlationId && (
          <div className="flex flex-wrap items-center gap-2 text-gray-700">
            <span>X-Correlation-ID</span><code className="break-all">{entry.correlationId}</code>
            {onTrace && <button type="button" className="btn-secondary" onClick={() => onTrace(entry.correlationId!)}>Find related incidents</button>}
          </div>
        )}
        {state ? <>
          <div className="flex flex-wrap items-center justify-between gap-2">
            <h3 className="font-semibold text-navy">{rows.filter((row) => row.changed).length} changed properties</h3>
            <label className="flex items-center gap-2 text-gray-700">
              <input type="checkbox" checked={changedOnly} onChange={(event) => setChangedOnly(event.target.checked)} /> Changed properties only
            </label>
          </div>
          <div className="overflow-x-auto rounded-lg border border-gray-200">
            <table className="w-full min-w-[28rem] table-fixed text-left">
              <thead className="bg-gray-50 text-gray-700"><tr>
                <th className="w-1/2 border-r border-gray-200 px-3 py-2" scope="col">Before</th>
                <th className="w-1/2 px-3 py-2" scope="col">After</th>
              </tr></thead>
              <tbody className="divide-y divide-gray-200">
                {shown.map((row) => <tr key={row.field} data-changed={row.changed ? 'true' : 'false'}>
                  <td className={`border-r border-gray-200 px-3 py-2 align-top ${row.changed && row.before !== undefined ? 'bg-red-50 text-red-900' : 'text-gray-700'}`}>
                    <code className="break-all">{row.before !== undefined ? `${JSON.stringify(row.field)}: ${row.before}` : '—'}</code>
                  </td>
                  <td className={`px-3 py-2 align-top ${row.changed && row.after !== undefined ? 'bg-green-50 text-green-900' : 'text-gray-700'}`}>
                    <code className="break-all">{row.after !== undefined ? `${JSON.stringify(row.field)}: ${row.after}` : '—'}</code>
                  </td>
                </tr>)}
              </tbody>
            </table>
          </div>
          {state.redactedFields.length > 0 && <p className="text-gray-700">Private values are redacted. The highlighted field names identify the recorded changes.</p>}
        </> : <p className="rounded-lg bg-gray-50 p-3 text-gray-700">This entry contains a recorded summary. Before and after snapshots are unavailable.</p>}
      </div>
    </Modal>
  );
}
