import { useEffect, useState } from 'react';
import { useQuery } from '@tanstack/react-query';
import { communicationApi, type TemplateDefinition } from '@/api/communicationApi';
import type { ApiError } from '@/api/types';
import { ErrorState } from '@/components/feedback/ErrorState';

const standardTemplates = [
  ['TPL-ENROLL-001', 'Training enrollment'],
  ['TPL-ASSESS-002', 'Assessment and certificate'],
  ['TPL-VERIF-003', 'Employment verification'],
  ['TPL-SURVEY-004', 'Post-placement survey'],
  ['TPL-ALERT-005', 'Operational alert'],
] as const;

const samples: Record<string, string> = {
  traineeName: 'Asha Sharma',
  courseName: 'Solar PV Installer',
  batchCode: 'DEMO-SOLAR-101',
  startDate: '12 January 2027',
  trainingCenterName: 'Sample District Skill Centre',
  resultStatus: 'Passed',
  certificateNumber: 'CERT-DEMO-001',
  certificateUrl: 'https://sih26135.gov.in/certificates/demo',
  employerName: 'Sample Solar Enterprise',
  verificationId: 'VERIF-DEMO-001',
  verificationUrl: 'https://sih26135.gov.in/verifications/demo',
  milestoneMonths: '6',
  surveyId: 'SURVEY-DEMO-001',
  surveyUrl: 'https://sih26135.gov.in/surveys/demo',
  alertType: 'Training schedule changed',
  actionRequired: 'Review the revised schedule in your portal',
  referenceId: 'ALERT-DEMO-001',
};

function renderError(error: Error | ApiError) {
  const code = 'code' in error ? error.code : undefined;
  switch (code) {
    case 'CREDENTIALS_IN_URL':
    case 'HTTPS_REQUIRED':
    case 'INVALID_URL':
    case 'INVALID_URL_SCHEME':
    case 'MALFORMED_URL':
    case 'MISSING_URL_HOST':
    case 'UNTRUSTED_URL_HOST':
    case 'UNTRUSTED_URL_SCHEME':
      return 'Use a trusted application URL for the link variable.';
    case 'SENSITIVE_PARAMETER_REJECTED':
      return 'Use sample values without identity numbers or access tokens.';
    default:
      return error.message || 'Preview could not be rendered.';
  }
}

export function TemplateManager() {
  const [selectedCode, setSelectedCode] = useState<string>();
  const templates = useQuery({
    queryKey: ['communication', 'templates'], queryFn: ({ signal }) => communicationApi.templates(signal),
  });
  if (templates.isPending) return <p role="status">Loading notification templates…</p>;
  if (templates.isError) return <ErrorState title="Unable to load templates" error={templates.error} onRetry={() => void templates.refetch()} />;

  const available = standardTemplates.flatMap(([code, title]) => {
    const definition = templates.data.find((template) => template.templateCode === code);
    return definition ? [{ definition, title }] : [];
  });
  const selected = available.find(({ definition }) => definition.templateCode === selectedCode) ?? available[0];

  return (
    <div className="grid gap-5 xl:grid-cols-2">
      <section className="card space-y-4" aria-labelledby="notification-templates">
        <h2 id="notification-templates" className="text-lg font-bold text-navy">Standard templates</h2>
        <div className="grid gap-3 sm:grid-cols-2">
          {standardTemplates.map(([code, title]) => {
            const template = available.find(({ definition }) => definition.templateCode === code);
            const active = code === selected?.definition.templateCode;
            return (
              <button key={code} type="button" disabled={!template} aria-pressed={active} onClick={() => setSelectedCode(code)}
                className={`rounded-lg border px-4 py-3 text-left focus-visible:outline-2 disabled:opacity-60 ${active
                  ? 'border-navy bg-navy-50' : 'border-gray-200 hover:border-navy'}`}>
                <span className="block text-xs font-semibold tracking-wide text-gray-600">{code}</span>
                <span className="mt-1 block font-semibold text-navy">{title}</span>
                {!template && <span className="mt-1 block text-sm text-gray-600">Unavailable</span>}
              </button>
            );
          })}
        </div>
      </section>
      {selected && <TemplatePreview key={selected.definition.templateCode} definition={selected.definition} />}
    </div>
  );
}

function TemplatePreview({ definition }: { definition: TemplateDefinition }) {
  const keys = [...definition.allowedParameterKeys].sort();
  const [variables, setVariables] = useState(() => Object.fromEntries(keys.map((key) => [key, samples[key] ?? ''])));
  const [settled, setSettled] = useState(variables);
  useEffect(() => {
    const timer = window.setTimeout(() => setSettled(variables), 300);
    return () => window.clearTimeout(timer);
  }, [variables]);
  const preview = useQuery({
    queryKey: ['communication', 'preview', definition.templateCode, settled],
    queryFn: ({ signal }) => communicationApi.render(definition.templateCode, settled, signal),
    retry: false,
    gcTime: 0,
    enabled: Object.values(settled).every((value) => value.trim().length > 0),
  });
  const needsValues = Object.values(variables).some((value) => !value.trim());

  return (
    <section className="card space-y-4" aria-labelledby="template-preview">
      <div>
        <h2 id="template-preview" className="text-lg font-bold text-navy">Live preview</h2>
        <p className="mt-1 text-sm text-gray-600">Edit the sample variables to see the backend-rendered message.</p>
      </div>
      <div className="grid gap-3 sm:grid-cols-2">
        {keys.map((key) => (
          <label className={`min-w-0 text-sm font-semibold text-gray-700 ${key.endsWith('Url') ? 'sm:col-span-2' : ''}`} key={key}>
            <span className="font-mono text-xs">{'{{' + key + '}}'}</span>
            {key === 'milestoneMonths' ? (
              <select className="form-input mt-1 w-full" value={variables[key]}
                onChange={(event) => setVariables({ ...variables, [key]: event.target.value })}>
                {['3', '6', '12'].map((months) => <option key={months} value={months}>{months} months</option>)}
              </select>
            ) : (
              <input className="form-input mt-1 w-full" value={variables[key]} maxLength={300}
                onChange={(event) => setVariables({ ...variables, [key]: event.target.value })} />
            )}
          </label>
        ))}
      </div>
      <div aria-live="polite" className="min-h-28 space-y-2 rounded-lg border border-gray-200 bg-gray-50 p-4 text-sm">
        {needsValues ? <p>Fill in each variable to render the preview.</p> :
          variables !== settled || preview.isPending ? <p>Rendering preview…</p> :
          preview.isError ? <p role="alert" className="text-red-800">{renderError(preview.error)}</p> :
          <><p className="font-semibold text-navy">{preview.data.subject}</p>
            <p className="whitespace-pre-wrap break-words leading-relaxed text-gray-700">{preview.data.bodyText}</p></>}
      </div>
    </section>
  );
}
