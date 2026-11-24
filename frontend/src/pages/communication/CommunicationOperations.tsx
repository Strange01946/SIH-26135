import { useState } from 'react';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { communicationApi } from '@/api/communicationApi';
import type { ApiError, PagedResponse } from '@/api/types';
import { Modal } from '@/components/common/Modal';
import { ErrorState } from '@/components/feedback/ErrorState';

const channels: Record<string, { icon: string; label: string }> = {
  EMAIL: { icon: '✉️', label: 'Email' },
  SMS: { icon: '📱', label: 'SMS' },
  WHATSAPP: { icon: '💬', label: 'WhatsApp' },
  PHONE: { icon: '☎️', label: 'Phone' },
};
const logStatuses = ['QUEUED', 'ATTEMPTED', 'SENT', 'DELIVERED', 'READ', 'REPLIED', 'FAILED', 'OPTED_OUT', 'BLOCKED_NO_CONSENT', 'SIMULATED'];
const queueStatuses = ['PENDING', 'PROCESSING', 'COMPLETED', 'DEAD_LETTER', 'CANCELLED'];
const statusColors: Record<string, string> = {
  QUEUED: 'bg-blue-50 text-blue-800', SENT: 'bg-indigo-50 text-indigo-800',
  DELIVERED: 'bg-green-50 text-green-800', COMPLETED: 'bg-green-50 text-green-800',
  FAILED: 'bg-red-50 text-red-800', DEAD_LETTER: 'bg-red-50 text-red-800',
  OPTED_OUT: 'bg-amber-50 text-amber-900', BLOCKED_NO_CONSENT: 'bg-amber-50 text-amber-900',
  PENDING: 'bg-blue-50 text-blue-800', PROCESSING: 'bg-indigo-50 text-indigo-800',
};
const statusLabel = (code: string) => {
  if (code === 'BLOCKED_NO_CONSENT') return 'Blocked by consent';
  return code.charAt(0) + code.slice(1).toLowerCase().replace(/_/g, ' ');
};
const timestamp = (value?: string) => value ? value.replace('T', ' ').slice(0, 19) : '—';

function StatusPill({ code }: { code: string }) {
  return <span className={`inline-flex whitespace-nowrap rounded-full px-2.5 py-1 text-xs font-semibold ${statusColors[code] ?? 'bg-gray-100 text-gray-700'}`}>
    {statusLabel(code)}
  </span>;
}

function ChannelLabel({ code }: { code: string }) {
  const channel = channels[code];
  return <span className="inline-flex items-center gap-2 whitespace-nowrap">
    {channel && <span aria-hidden="true">{channel.icon}</span>}{channel?.label ?? statusLabel(code)}
  </span>;
}

function PageControls({ result, onPage }: { result: PagedResponse<unknown>; onPage: (page: number) => void }) {
  if (result.totalPages < 2) return null;
  return <div className="flex flex-wrap items-center justify-end gap-3 border-t border-gray-200 pt-3 text-sm">
    <span className="text-gray-700">Page {result.page + 1} of {result.totalPages}</span>
    <button type="button" className="btn-secondary" disabled={!result.hasPrevious} onClick={() => onPage(result.page - 1)}>Previous</button>
    <button type="button" className="btn-secondary" disabled={!result.hasNext} onClick={() => onPage(result.page + 1)}>Next</button>
  </div>;
}

export function DeliveryLogs({ initialLogId }: { initialLogId?: number }) {
  const initialSearch = initialLogId?.toString() ?? '';
  const [searchInput, setSearchInput] = useState(initialSearch);
  const [filters, setFilters] = useState({ q: initialSearch, channel: '', status: '', page: 0 });
  const result = useQuery({
    queryKey: ['communication', 'logs', filters],
    queryFn: ({ signal }) => communicationApi.logs(filters, signal),
    refetchInterval: 15_000,
  });
  const capabilities = useQuery({
    queryKey: ['communication', 'capabilities'], queryFn: ({ signal }) => communicationApi.capabilities(signal),
  });
  const showLog = (id: number) => {
    setSearchInput(String(id));
    setFilters({ q: String(id), channel: '', status: '', page: 0 });
  };

  return (
    <div className="space-y-5">
      {capabilities.data?.demoAvailable && <LocalDemonstrations onShowLog={showLog} />}
      <section className="card space-y-4" aria-labelledby="delivery-logs">
        <div>
          <h2 id="delivery-logs" className="text-lg font-bold text-navy">Outbound communication logs</h2>
          <p className="mt-1 text-sm text-gray-600">Recipient hints use masked contact details currently on file.</p>
        </div>
        <form className="flex flex-wrap items-end gap-3" onSubmit={(event) => {
          event.preventDefault();
          setFilters({ ...filters, q: searchInput.trim(), page: 0 });
        }}>
          <label className="min-w-52 flex-1 text-sm font-semibold text-gray-700">
            Search logs
            <input type="search" className="form-input mt-1 w-full" placeholder="Log ID, template code, or purpose" maxLength={64}
              value={searchInput} onChange={(event) => setSearchInput(event.target.value)} />
          </label>
          <button type="submit" className="btn-secondary">Search</button>
          <label className="text-sm font-semibold text-gray-700">
            Channel
            <select className="form-input mt-1 block" value={filters.channel}
              onChange={(event) => setFilters({ ...filters, channel: event.target.value, page: 0 })}>
              <option value="">All channels</option>
              {Object.entries(channels).map(([code, { label }]) => <option key={code} value={code}>{label}</option>)}
            </select>
          </label>
          <label className="text-sm font-semibold text-gray-700">
            Delivery status
            <select className="form-input mt-1 block" value={filters.status}
              onChange={(event) => setFilters({ ...filters, status: event.target.value, page: 0 })}>
              <option value="">All statuses</option>
              {logStatuses.map((code) => <option key={code} value={code}>{statusLabel(code)}</option>)}
            </select>
          </label>
          <button type="button" className="btn-secondary" onClick={() => {
            setSearchInput('');
            setFilters({ q: '', channel: '', status: '', page: 0 });
          }}>Clear</button>
        </form>
        {result.isPending && <p role="status">Loading delivery logs…</p>}
        {result.isError && <ErrorState title="Unable to load delivery logs" error={result.error} onRetry={() => void result.refetch()} />}
        {result.data && (
          <>
            <p className="text-sm text-gray-600">{result.data.totalElements.toLocaleString('en-IN')} matching records</p>
            {result.data.content.length > 0 ? (
              <div className="overflow-x-auto">
                <table className="w-full text-left text-sm">
                  <thead className="bg-gray-50 text-gray-700">
                    <tr>{['Log ID', 'Template and purpose', 'Channel', 'Masked recipient', 'Status', 'Recorded', 'Delivered'].map((label) => (
                      <th key={label} scope="col" className="px-3 py-2">{label}</th>
                    ))}</tr>
                  </thead>
                  <tbody className="divide-y divide-gray-100">
                    {result.data.content.map((log) => (
                      <tr key={log.id}>
                        <th scope="row" className="px-3 py-3 font-semibold text-navy">#{log.id}</th>
                        <td className="px-3 py-3">
                          <div className="font-medium">{log.templateCode ?? '—'}</div>
                          <div className="text-xs text-gray-600">{statusLabel(log.purpose)}</div>
                        </td>
                        <td className="px-3 py-3"><ChannelLabel code={log.channel} /></td>
                        <td className="whitespace-nowrap px-3 py-3 font-mono text-xs">{log.recipientMasked ?? 'Unavailable'}</td>
                        <td className="px-3 py-3">
                          <StatusPill code={log.optedOut ? 'OPTED_OUT' : log.status} />
                          {log.demoReceipt && <div className="mt-1 text-xs font-semibold text-gray-600">Demo receipt</div>}
                          {log.optedOut && log.status === 'BLOCKED_NO_CONSENT' && <div className="mt-1 text-xs text-gray-600">Consent withdrawn</div>}
                        </td>
                        <td className="whitespace-nowrap px-3 py-3 text-gray-700">{timestamp(log.sentAt ?? log.createdAt)}</td>
                        <td className="whitespace-nowrap px-3 py-3 text-gray-700">{timestamp(log.deliveredAt)}</td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            ) : <p className="rounded-lg bg-gray-50 p-4 text-sm text-gray-700">No outbound logs match these filters.</p>}
            <PageControls result={result.data} onPage={(page) => setFilters({ ...filters, page })} />
          </>
        )}
      </section>
    </div>
  );
}

function LocalDemonstrations({ onShowLog }: { onShowLog: (id: number) => void }) {
  const queryClient = useQueryClient();
  const [receipt, setReceipt] = useState<{ id: number; status: string }>();
  const [consentDialog, setConsentDialog] = useState(false);
  const refreshLogs = () => void queryClient.invalidateQueries({ queryKey: ['communication', 'logs'] });
  const showReceipt = (value: { id: number; status: string }) => {
    setReceipt(value);
    onShowLog(value.id);
    refreshLogs();
  };
  const createReceipt = useMutation({ mutationFn: communicationApi.createDemo, onSuccess: showReceipt });
  const simulate = useMutation({ mutationFn: communicationApi.deliverDemo, onSuccess: showReceipt });
  const consent = useMutation({
    mutationFn: communicationApi.checkConsent,
    onSettled: () => { setConsentDialog(true); refreshLogs(); },
  });
  const consentError = consent.error as ApiError | null;
  const withdrawn = consentError?.code === 'CONSENT_WITHDRAWN';
  const missing = consentError?.code === 'CONSENT_NOT_GRANTED';
  const pendingReceipt = createReceipt.isPending || simulate.isPending;
  const receiptError = createReceipt.error ?? simulate.error;

  return (
    <section className="card space-y-3" aria-labelledby="local-demonstrations">
      <div>
        <h2 id="local-demonstrations" className="text-lg font-bold text-navy">Local demonstrations</h2>
        <p className="mt-1 text-sm text-gray-600">The receipt and consent tests use a sample trainee and local mock delivery.</p>
      </div>
      <div className="flex flex-wrap items-center gap-2">
        <button type="button" className="btn-secondary" disabled={pendingReceipt} onClick={() => {
          simulate.reset();
          createReceipt.mutate();
        }}>{createReceipt.isPending ? 'Creating sample…' : 'Create sent sample'}</button>
        <button type="button" className="btn-primary" disabled={pendingReceipt || receipt?.status !== 'SENT'} onClick={() => {
          if (receipt) simulate.mutate(receipt.id);
        }}>{simulate.isPending ? 'Reconciling receipt…' : 'Simulate delivery receipt'}</button>
        <button type="button" className="btn-secondary" disabled={consent.isPending}
          onClick={() => consent.mutate()}>{consent.isPending ? 'Checking consent…' : 'Test withdrawn consent'}</button>
      </div>
      {receipt && <p role="status" className="text-sm text-gray-700">Demo log #{receipt.id} <StatusPill code={receipt.status} /></p>}
      {receiptError && <p role="alert" className="text-sm text-red-800">{receiptError.message ?? 'The demo receipt could not be processed.'}</p>}
      <Modal open={consentDialog} onClose={() => setConsentDialog(false)}
        title={withdrawn ? 'Consent withdrawn' : missing ? 'Consent required' : 'Consent demonstration result'}>
        <div className="space-y-4 p-6 text-sm text-gray-700">
          {withdrawn || missing ? (
            <p className="rounded-lg border border-amber-200 bg-amber-50 p-3 text-amber-950">
              {withdrawn ? 'The sample trainee withdrew consent.' : 'The sample trainee has no active consent.'}
              {' The backend blocked dispatch before it reached a delivery provider.'}
            </p>
          ) : <p role="alert">The expected consent rejection was unavailable. {consentError?.message ?? 'Review the delivery log for the sample outcome.'}</p>}
          <button type="button" className="btn-secondary" onClick={() => setConsentDialog(false)}>Close</button>
        </div>
      </Modal>
    </section>
  );
}

export function DispatchQueue({ onOpenLog }: { onOpenLog: (id: number) => void }) {
  const [filters, setFilters] = useState({ status: '', page: 0 });
  const result = useQuery({
    queryKey: ['communication', 'queue', filters], queryFn: ({ signal }) => communicationApi.queue(filters, signal),
    refetchInterval: 5_000,
  });
  if (result.isPending) return <p role="status">Loading the dispatch queue…</p>;
  if (result.isError) return <ErrorState title="Unable to load the dispatch queue" error={result.error} onRetry={() => void result.refetch()} />;
  const { jobs, counts } = result.data;
  const statuses = [...new Set([...queueStatuses, ...counts.map(({ status }) => status)])];

  return (
    <section className="card space-y-4" aria-labelledby="dispatch-queue">
      <div className="flex flex-wrap items-center justify-between gap-3">
        <div>
          <h2 id="dispatch-queue" className="text-lg font-bold text-navy">Dispatch queue health</h2>
          <p className="mt-1 text-sm text-gray-600">Review job states, retry attempts, and the resulting delivery records.</p>
        </div>
        <button type="button" className="btn-secondary" onClick={() => void result.refetch()}>Refresh queue</button>
      </div>
      <div className="grid gap-2 sm:grid-cols-3 lg:grid-cols-5">
        {statuses.map((status) => (
          <button type="button" key={status} aria-pressed={filters.status === status}
            className={`rounded-lg border px-3 py-3 text-left focus-visible:outline-2 ${filters.status === status ? 'border-navy bg-navy-50' : 'border-gray-200 hover:border-navy'}`}
            onClick={() => setFilters({ status: filters.status === status ? '' : status, page: 0 })}>
            <span className="block text-sm text-gray-700">{statusLabel(status)}</span>
            <span className="mt-1 block text-xl font-bold text-navy">{(counts.find((count) => count.status === status)?.count ?? 0).toLocaleString('en-IN')}</span>
          </button>
        ))}
      </div>
      {jobs.content.length > 0 ? (
        <div className="overflow-x-auto">
          <table className="w-full text-left text-sm">
            <thead className="bg-gray-50 text-gray-700">
              <tr>{['Job ID', 'Template', 'Channel', 'State', 'Attempts', 'Mode', 'Scheduled', 'Updated', 'Log'].map((label) => (
                <th key={label} scope="col" className="px-3 py-2">{label}</th>
              ))}</tr>
            </thead>
            <tbody className="divide-y divide-gray-100">
              {jobs.content.map((job) => (
                <tr key={job.id}>
                  <th scope="row" className="px-3 py-3 font-semibold text-navy">#{job.id}</th>
                  <td className="px-3 py-3">{job.templateCode}</td>
                  <td className="px-3 py-3"><ChannelLabel code={job.channel} /></td>
                  <td className="px-3 py-3"><StatusPill code={job.status} /></td>
                  <td className="whitespace-nowrap px-3 py-3">{job.attemptCount} / {job.maxAttempts}</td>
                  <td className="px-3 py-3">{statusLabel(job.deliveryMode)}</td>
                  <td className="whitespace-nowrap px-3 py-3">{timestamp(job.scheduledAt)}</td>
                  <td className="whitespace-nowrap px-3 py-3">{timestamp(job.updatedAt)}</td>
                  <td className="px-3 py-3">{job.communicationLogId ? <button type="button" className="font-semibold text-navy underline"
                    onClick={() => onOpenLog(job.communicationLogId!)}>#{job.communicationLogId}</button> : '—'}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      ) : <p className="rounded-lg bg-gray-50 p-4 text-sm text-gray-700">No dispatch jobs in this view.</p>}
      <PageControls result={jobs} onPage={(page) => setFilters({ ...filters, page })} />
    </section>
  );
}
