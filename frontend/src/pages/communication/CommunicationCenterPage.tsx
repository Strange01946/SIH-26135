import { useState } from 'react';
import { useSearchParams } from 'react-router-dom';
import { useAuth } from '@/context/AuthContext';
import { DeliveryLogs, DispatchQueue } from './CommunicationOperations';
import { TemplateManager } from './TemplateManager';

export default function CommunicationCenterPage() {
  const { activePersona, primaryPersona, hasAuthority } = useAuth();
  const persona = activePersona ?? primaryPersona;
  const canReviewOperations = (persona === 'SUPER_ADMIN' || persona === 'GOVERNMENT_ADMIN')
    && ['communication.manage', 'survey.manage', 'system.manage'].some(hasAuthority);
  const [params, setParams] = useSearchParams();
  const requestedView = params.get('view');
  const view = requestedView === 'logs' || requestedView === 'queue' ? requestedView : 'templates';
  const [selectedLog, setSelectedLog] = useState<number>();
  const visibleView = canReviewOperations ? view : 'templates';
  const views = [
    { id: 'templates', label: 'Templates' },
    { id: 'logs', label: 'Delivery logs' },
    { id: 'queue', label: 'Dispatch queue' },
  ] as const;

  return (
    <div className="space-y-5">
      <header>
        <h1 className="text-2xl font-bold text-navy sm:text-3xl">Communication Center</h1>
        <p className="mt-1 text-sm text-gray-600">
          {canReviewOperations
            ? 'Preview candidate notifications and review dispatch, delivery, and consent outcomes.'
            : 'Preview the standard notifications used across the candidate journey.'}
        </p>
      </header>
      {canReviewOperations && (
        <nav aria-label="Communication Center sections" className="flex flex-wrap gap-1 border-b border-gray-200">
          {views.map(({ id, label }) => (
            <button key={id} type="button" aria-current={visibleView === id ? 'page' : undefined}
              onClick={() => { setSelectedLog(undefined); setParams(id === 'templates' ? {} : { view: id }); }}
              className={`border-b-2 px-3 py-2 text-sm font-semibold focus-visible:outline-2 ${visibleView === id
                ? 'border-navy text-navy' : 'border-transparent text-gray-600 hover:text-navy'}`}>
              {label}
            </button>
          ))}
        </nav>
      )}
      {visibleView === 'templates' && <TemplateManager />}
      {visibleView === 'logs' && <DeliveryLogs initialLogId={selectedLog} />}
      {visibleView === 'queue' && <DispatchQueue onOpenLog={(id) => { setSelectedLog(id); setParams({ view: 'logs' }); }} />}
    </div>
  );
}
