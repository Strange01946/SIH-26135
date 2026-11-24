import { useSearchParams } from 'react-router-dom';
import { useAuth } from '@/context/AuthContext';
import { AuditExplorer } from './AuditExplorer';
import { IncidentConsole } from './IncidentConsole';
import { HealthPanel } from './HealthPanel';

export default function GovernancePage() {
  const { hasAuthority } = useAuth();
  const audit = ['audit.read', 'system.manage'].some(hasAuthority);
  const events = audit || ['system.event.read', 'system.event.manage'].some(hasAuthority);
  const health = ['system.manage', 'ROLE_ADMIN', 'ROLE_super_admin'].some(hasAuthority);
  const [params, setParams] = useSearchParams();
  const tabs = [
    ...(audit ? [{ id: 'audit', label: 'Audit trail' }] : []),
    ...(events ? [{ id: 'events', label: 'Incidents' }] : []),
    ...(health ? [{ id: 'health', label: 'Actuator health' }] : []),
  ];
  const view = tabs.find((tab) => tab.id === params.get('view'))?.id ?? tabs[0]?.id;
  const correlation = params.get('correlationId') ?? '';
  const entityType = params.get('entityType') ?? '';
  const entityId = params.get('entityId') ?? '';
  const trace = (to: 'audit' | 'events', correlationId: string, type?: string, id?: number) => {
    const next = new URLSearchParams({ view: to, correlationId });
    if (type && id) {
      next.set('entityType', type);
      next.set('entityId', String(id));
    }
    setParams(next);
  };

  return (
    <div className="space-y-5">
      <header>
        <h1 className="text-2xl font-bold text-navy sm:text-3xl">Governance and diagnostics</h1>
        <p className="mt-1 text-sm text-gray-600">Review recorded changes and trace operational incidents.</p>
      </header>
      <nav aria-label="Governance sections" className="flex flex-wrap gap-1 border-b border-gray-200">
        {tabs.map(({ id, label }) => (
          <button type="button" key={id} aria-current={view === id ? 'page' : undefined}
            className={`border-b-2 px-3 py-2 text-sm font-semibold focus-visible:outline-2 ${view === id
              ? 'border-navy text-navy' : 'border-transparent text-gray-600 hover:text-navy'}`}
            onClick={() => setParams({ view: id })}>{label}</button>
        ))}
      </nav>
      {view === 'audit' && <AuditExplorer key={`${correlation}:${entityType}:${entityId}`}
        initial={{ correlationId: correlation, entityType, entityId }}
        onTrace={events ? (id) => trace('events', id) : undefined} />}
      {view === 'events' && <IncidentConsole key={correlation} initialCorrelation={correlation} canViewThrottles={health}
        onTrace={audit ? (id, type, ref) => trace('audit', id, type, ref) : undefined} />}
      {view === 'health' && <HealthPanel />}
    </div>
  );
}
