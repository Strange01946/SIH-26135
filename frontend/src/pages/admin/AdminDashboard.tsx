import { useQuery, type UseQueryResult } from '@tanstack/react-query';
import type { ReactNode } from 'react';
import { Link } from 'react-router-dom';
import { getAdminImportMetrics, listOnboardingApplications, pendingOnboardingKey } from '@/api/adminApi';
import { getExecutiveHeadline } from '@/api/misApi';
import { getCleanlinessScore, getSpecializedOutreach } from '@/api/specializedAnalyticsApi';
import type { AuthenticatedUserResponse, UserPersona } from '@/api/types';
import { ErrorState } from '@/components/feedback/ErrorState';
import { LoadingSpinner } from '@/components/feedback/LoadingSpinner';
import { useAuth } from '@/context/AuthContext';
import { simulationActive } from '@/lib/demoEnvironment';
import { ExecutiveKpiCards } from '@/pages/analytics/ExecutiveKpiCards';
import { Value } from '@/pages/analytics/MisUi';

const titles: Partial<Record<UserPersona, string>> = {
  SUPER_ADMIN: 'National Super Administrator Console',
  GOVERNMENT_ADMIN: 'Government Scheme Administrator Console',
  STATE_ADMIN: 'State Administrator Console',
  GOVERNMENT_OFFICER: 'Government Operations Officer Console',
  DISTRICT_OFFICER: 'District Skill Officer Console',
  ANALYST: 'National Data & Policy Analyst Console',
};

function consoleAccess(user: AuthenticatedUserResponse | null) {
  const authorities = new Set(user?.authorities.map((authority) => authority.toLowerCase()));
  const has = (...permissions: string[]) => permissions.some((permission) => authorities.has(permission));
  const centralAdmin = has('role_super_admin', 'role_government_admin');
  const organisationScoped = user?.trainingProviderId != null || user?.employerId != null;
  const national = has('role_super_admin', 'role_government_admin', 'role_analyst')
    && !has('role_training_provider', 'role_employer', 'role_trainee', 'role_government_officer', 'role_district_officer')
    && !organisationScoped && user?.stateId == null && user?.districtId == null;
  const assignedOperations = !organisationScoped && user?.stateId != null
    && !has('role_training_provider', 'role_employer', 'role_trainee')
    && (has('role_government_officer', 'role_district_officer')
      ? user.districtId != null : has('role_government_admin', 'role_state_admin'));
  return {
    national,
    outcomes: national && has('analytics.read'),
    // The onboarding API returns a nationwide list. Only render its preview in a nationwide admin session.
    onboarding: national && centralAdmin,
    operations: (national || assignedOperations)
      && has('analytics.read', 'audit.read', 'system.manage', 'system.event.read', 'system.event.manage'),
    careers: national && centralAdmin && has('employment.manage'),
    users: centralAdmin && has('user.manage'),
    centers: national && centralAdmin && has('center.manage'),
    venueReview: (national || assignedOperations) && has('venue.booking.review'),
    delivery: national && centralAdmin && has('communication.manage', 'survey.manage', 'system.manage'),
    audit: has('audit.read', 'system.manage'),
    incidents: has('audit.read', 'system.manage', 'system.event.read', 'system.event.manage'),
    health: has('system.manage', 'role_super_admin', 'role_admin'),
  };
}

export function AdminDashboard() {
  const { user, activePersona, primaryPersona } = useAuth();
  const persona = activePersona ?? primaryPersona;
  const access = consoleAccess(user);
  const showOperations = !simulationActive && (access.onboarding || access.operations || access.outcomes);
  const scope = access.national ? 'Nationwide'
    : user?.districtId ? `District ${user.districtId}` : user?.stateId ? `State ${user.stateId}` : undefined;
  const workspaces = [
    { show: access.careers, title: 'Candidate career records', to: '/admin/careers', detail: 'Find a candidate, review employment and wage history, record exits, or arrange skill-gap training.' },
    { show: access.outcomes, title: 'Retention, skill gaps and attrition', to: '/analytics/specialized?view=outcomes', detail: 'Compare longer-term employment outcomes and identify where candidates need more support.' },
    { show: access.outcomes, title: 'Data quality reports', to: '/analytics/specialized?view=operations', detail: 'Inspect data-quality rules, system health and incident trends.' },
    { show: access.users, title: 'User provisioning', to: '/admin/users', detail: 'Review accounts and provision roles within your permissions.' },
    { show: access.centers, title: 'Training center management', to: '/admin/centers', detail: 'Register centers for training providers and manage their details and operational status.' },
    { show: access.venueReview, title: 'Venue booking review', to: '/admin/venue-bookings', detail: 'Review temporary locations, approve session requests and manage shared availability.' },
    { show: access.delivery, title: 'Communication delivery logs', to: '/communication?view=logs', detail: 'Review outreach delivery and consent outcomes using masked recipient details.' },
    { show: access.audit, title: 'Audit trail', to: '/audit?view=audit', detail: 'Trace recorded changes and inspect entity state differences.' },
    { show: access.incidents, title: 'System incidents', to: '/audit?view=events', detail: 'Trace errors and warnings by correlation ID.' },
    { show: access.health, title: 'Service health', to: '/audit?view=health', detail: 'Inspect application health and authorized operational metrics.' },
  ].filter(({ show, to }) => show && (!simulationActive || to.startsWith('/communication')));

  return (
    <div className="mx-auto max-w-screen-2xl space-y-5" key={user?.id}>
      <header className="flex flex-wrap items-start justify-between gap-3 rounded-xl bg-navy px-5 py-6 text-white sm:px-7">
        <div>
          <h1 className="text-2xl font-semibold sm:text-3xl">{(persona && titles[persona]) || 'Government Administrator Portal'}</h1>
          <p className="mt-1 text-sm text-blue-100">Track skilling outcomes and act on organisation reviews, candidate follow-up and data quality.</p>
        </div>
        {scope && <span className="rounded-md border border-white/30 px-2.5 py-1 text-sm font-medium text-white">{scope}</span>}
      </header>

      {simulationActive && <p className="text-sm text-slate-700">Use live mode to review administrative queues and operational metrics.</p>}
      {access.outcomes && <SkillingOutcomes userId={user?.id} />}
      {showOperations && (
        <section aria-labelledby="admin-operations" className="space-y-3">
          <h2 id="admin-operations" className="section-title">Operations overview</h2>
          <div className="grid items-start gap-3 md:grid-cols-2 2xl:grid-cols-4">
            {access.onboarding && <PendingOnboarding userId={user?.id} />}
            {access.outcomes && <Outreach userId={user?.id} />}
            {access.operations && <><DataQuality userId={user?.id} /><ImportMetrics userId={user?.id} /></>}
          </div>
        </section>
      )}
      {workspaces.length > 0 && (
        <section aria-labelledby="admin-workspaces" className="space-y-3">
          <h2 id="admin-workspaces" className="section-title">Workspaces</h2>
          <ul className="grid gap-3 md:grid-cols-2 2xl:grid-cols-4">
            {workspaces.map(({ title, to, detail }) => (
              <li key={to} className="card p-4">
                <Link className="font-semibold text-navy underline underline-offset-4 hover:text-cyan-800 focus-visible:outline-2" to={to}>{title}</Link>
                <p className="mt-1 text-sm text-slate-600">{detail}</p>
              </li>
            ))}
          </ul>
        </section>
      )}
      {!access.outcomes && !showOperations && workspaces.length === 0 && (
        <p className="card p-5 text-sm text-slate-600" role="status">There are no administration workspaces available to this account.</p>
      )}
    </div>
  );
}

function SummaryData<T>({ query, label, children }: {
  query: UseQueryResult<T>; label: string; children: (data: T) => ReactNode;
}) {
  if (query.isError) return <ErrorState error={query.error} title={`Unable to load ${label}`} onRetry={() => void query.refetch()} />;
  if (query.isPending) return <LoadingSpinner label={`Loading ${label}`} />;
  return children(query.data);
}

function SummaryCard({ id, title, caption, children, link }: {
  id: string; title: string; caption?: string; children: ReactNode; link?: { to: string; label: string };
}) {
  return (
    <section aria-labelledby={id} className="card min-w-0 space-y-3 p-4">
      <div>
        <h3 id={id} className="font-semibold text-navy">{title}</h3>
        {caption && <p className="mt-1 text-xs text-slate-600">{caption}</p>}
      </div>
      {children}
      {link && <Link to={link.to} className="inline-block text-sm font-semibold text-navy underline underline-offset-4 focus-visible:outline-2">{link.label}</Link>}
    </section>
  );
}

function Metric({ label, children }: { label: string; children: ReactNode }) {
  return <div className="flex flex-wrap items-baseline justify-between gap-x-3 gap-y-1 py-1.5">
    <dt className="text-sm text-slate-700">{label}</dt>
    <dd className="font-semibold tabular-nums text-navy">{children}</dd>
  </div>;
}

function SkillingOutcomes({ userId }: { userId: number | undefined }) {
  const query = useQuery({ queryKey: ['admin', 'outcomes', userId], queryFn: ({ signal }) => getExecutiveHeadline({}, signal) });
  return (
    <section aria-labelledby="admin-outcomes" className="card space-y-4 p-4 sm:p-5">
      <header className="flex flex-wrap items-center justify-between gap-3">
        <div>
          <h2 id="admin-outcomes" className="section-title">Skilling outcomes</h2>
          <p className="text-sm text-slate-600">Nationwide · all reporting periods</p>
        </div>
        <Link to="/analytics" className="btn-secondary">Open Government MIS</Link>
      </header>
      <SummaryData query={query} label="skilling outcomes">{(headline) => <ExecutiveKpiCards headline={headline} />}</SummaryData>
    </section>
  );
}

function PendingOnboarding({ userId }: { userId: number | undefined }) {
  const query = useQuery({
    queryKey: [...pendingOnboardingKey, userId],
    queryFn: async ({ signal }) => {
      const applications = await listOnboardingApplications('PENDING', signal);
      return {
        count: applications.length,
        latest: applications.slice(0, 3).map(({ applicationId, applicationNumber, organizationName }) =>
          ({ applicationId, applicationNumber, organizationName })),
      };
    },
    staleTime: 0,
  });
  return (
    <SummaryCard id="admin-onboarding" title="Organisation onboarding" link={{ to: '/admin/onboarding', label: 'Review pending applications' }}>
      <SummaryData query={query} label="pending applications">{({ count, latest }) => count === 0
        ? <p className="text-sm text-slate-600">No pending applications.</p>
        : <>
          <dl><Metric label="Pending applications"><Value value={count} /></Metric></dl>
          <ul aria-label="Latest pending applications" className="divide-y divide-slate-200 border-t border-slate-200">
            {latest.map((item) => <li key={item.applicationId} className="min-w-0 py-1.5">
              <p className="break-words text-sm font-medium text-slate-800">{item.organizationName}</p>
              <p className="text-xs text-slate-600">{item.applicationNumber}</p>
            </li>)}
          </ul>
        </>}</SummaryData>
    </SummaryCard>
  );
}

function Outreach({ userId }: { userId: number | undefined }) {
  const query = useQuery({ queryKey: ['admin', 'outreach', userId], queryFn: ({ signal }) => getSpecializedOutreach(signal) });
  return (
    <SummaryCard id="admin-outreach" title="Follow-up and verification" caption="Nationwide · all reporting periods"
      link={{ to: '/analytics/specialized?view=outreach', label: 'Open outreach and verification reports' }}>
      <SummaryData query={query} label="outreach and verification">{({ followups, surveys, verifications }) => (
        <dl className="divide-y divide-slate-200">
          <Metric label="Pending follow-ups"><Value value={followups.pendingTasks} ambiguous /></Metric>
          <Metric label="Submitted surveys"><Value value={surveys.submittedCount} ambiguous /></Metric>
          <Metric label="Claims pending verification"><Value value={verifications.pendingCount} ambiguous /></Metric>
        </dl>
      )}</SummaryData>
    </SummaryCard>
  );
}

function DataQuality({ userId }: { userId: number | undefined }) {
  const query = useQuery({ queryKey: ['admin', 'data-quality', userId], queryFn: ({ signal }) => getCleanlinessScore(signal) });
  return (
    <SummaryCard id="admin-quality" title="Data quality" caption="Current records and unresolved issues">
      <SummaryData query={query} label="data quality scorecard">{(scorecard) => (
        <dl className="divide-y divide-slate-200">
          <Metric label="Data cleanliness"><Value value={scorecard.dataCleanlinessIndexPct} format="percent" noData /></Metric>
          <Metric label="Active issues"><Value value={scorecard.totalActiveIssues} noData /></Metric>
        </dl>
      )}</SummaryData>
    </SummaryCard>
  );
}

function ImportMetrics({ userId }: { userId: number | undefined }) {
  const query = useQuery({ queryKey: ['admin', 'imports', userId], queryFn: ({ signal }) => getAdminImportMetrics(signal) });
  return (
    <SummaryCard id="admin-imports" title="Bulk CSV and JSON intake" caption="Batches created in the last 30 calendar days">
      <SummaryData query={query} label="bulk intake metrics">{(imports) => (
        <dl className="divide-y divide-slate-200">
          <Metric label="Batches awaiting processing"><Value value={imports.pendingBatches} noData /></Metric>
          <Metric label="Failed rows"><Value value={imports.failedRecordCount} noData /></Metric>
          <Metric label="Duplicate rows"><Value value={imports.duplicateRecordCount} noData /></Metric>
        </dl>
      )}</SummaryData>
    </SummaryCard>
  );
}
