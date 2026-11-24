import { useQuery } from '@tanstack/react-query';
import { governanceApi } from '@/api/governanceApi';
import { ErrorState } from '@/components/feedback/ErrorState';
import { readTelemetry } from './governanceDisplay';

const diagnosticLabels: Record<string, string> = {
  db: 'Database', databaseLatency: 'Database latency', caffeineCache: 'Cache',
  bulkImportStorage: 'Bulk-import storage', communicationGateway: 'Communication gateway', diskSpace: 'Disk space',
};
const metricLabels = [
  ['process_uptime_seconds', 'Process uptime', 's'],
  ['jvm_threads_live_threads', 'Live JVM threads', ''],
  ['hikaricp_connections_active', 'Active database connections', ''],
  ['http_server_requests_seconds_count', 'HTTP requests since startup', ''],
] as const;

function HealthBadge({ status }: { status: string }) {
  return <span className={`rounded-full px-2.5 py-1 text-xs font-semibold ${status === 'UP'
    ? 'bg-green-50 text-green-900' : status === 'DOWN' || status === 'OUT_OF_SERVICE'
      ? 'bg-red-50 text-red-900' : 'bg-amber-50 text-amber-950'}`}>{status.replace(/_/g, ' ')}</span>;
}

export function HealthPanel() {
  const health = useQuery({
    queryKey: ['actuator', 'health'], queryFn: ({ signal }) => governanceApi.health(signal), refetchInterval: 15_000, retry: false,
  });
  const diagnostics = useQuery({
    queryKey: ['actuator', 'diagnostics'], queryFn: ({ signal }) => governanceApi.diagnostics(signal), refetchInterval: 15_000, retry: false,
  });
  const metrics = useQuery({
    queryKey: ['actuator', 'prometheus'], queryFn: async ({ signal }) => readTelemetry(await governanceApi.prometheus(signal)),
    refetchInterval: 15_000, retry: false,
  });
  const info = useQuery({ queryKey: ['actuator', 'info'], queryFn: ({ signal }) => governanceApi.info(signal), retry: false });
  const components = Object.entries(diagnostics.data?.components ?? {}).filter(([name]) => diagnosticLabels[name]);
  const metadata = info.data?.build ?? info.data?.app;

  return (
    <div className="grid gap-5 xl:grid-cols-2">
      <section className="card space-y-4" aria-labelledby="actuator-health">
        <div className="flex flex-wrap items-center justify-between gap-3">
          <h2 id="actuator-health" className="text-lg font-bold text-navy">Spring Boot Actuator health</h2>
          {health.data && <HealthBadge status={health.data.status} />}
        </div>
        {health.isPending && <p role="status">Loading backend health…</p>}
        {health.isError && <ErrorState title="Unable to load health" error={health.error} onRetry={() => void health.refetch()} />}
        {diagnostics.isPending && <p role="status">Loading component checks…</p>}
        {diagnostics.isError && <ErrorState title="Unable to load component checks" error={diagnostics.error} onRetry={() => void diagnostics.refetch()} />}
        {diagnostics.data && (components.length ? <dl className="divide-y divide-gray-100">
          {components.map(([name, component]) => <div key={name} className="flex items-center justify-between gap-4 py-2 text-sm">
            <dt className="text-gray-700">{diagnosticLabels[name]}</dt><dd><HealthBadge status={component.status} /></dd>
          </div>)}
        </dl> : <p className="text-sm text-gray-700">Component details are unavailable for this account.</p>)}
        <div className="border-t border-gray-200 pt-3">
          <h3 className="font-semibold text-navy">Application information</h3>
          {info.isPending && <p role="status" className="text-sm">Loading application information…</p>}
          {info.isError && <ErrorState title="Unable to load application information" error={info.error} onRetry={() => void info.refetch()} />}
          {info.data && <p className="mt-1 text-sm text-gray-700">{metadata?.name || metadata?.version
            ? [metadata.name, metadata.version].filter(Boolean).join(' · ') : 'Build metadata is unavailable from Actuator.'}</p>}
        </div>
      </section>
      <section className="card space-y-4" aria-labelledby="prometheus-telemetry">
        <div>
          <h2 id="prometheus-telemetry" className="text-lg font-bold text-navy">Prometheus telemetry</h2>
          <p className="mt-1 text-sm text-gray-600">Readings for this backend process.</p>
        </div>
        {metrics.isPending && <p role="status">Loading Prometheus metrics…</p>}
        {metrics.isError && <ErrorState title="Unable to load Prometheus metrics" error={metrics.error} onRetry={() => void metrics.refetch()} />}
        {metrics.data && <dl className="grid gap-3 sm:grid-cols-2">
          {metricLabels.map(([key, label, unit]) => {
            const reading = metrics.data[key];
            return <div key={key} className="rounded-lg border border-gray-200 p-3">
              <dt className="text-sm text-gray-700">{label}</dt>
              <dd className="mt-1 text-xl font-bold text-navy">{reading == null ? 'Unavailable' : `${Math.round(reading).toLocaleString('en-IN')}${unit ? ' ' + unit : ''}`}</dd>
            </div>;
          })}
        </dl>}
      </section>
    </div>
  );
}
