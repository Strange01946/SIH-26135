import React, { Suspense } from 'react';
import { Navigate, Route, Routes } from 'react-router-dom';
import { AppLayout } from '@/components/layout/AppLayout';
import { ProtectedRoute } from '@/components/auth/ProtectedRoute';
import { RoleGuard } from '@/components/auth/RoleGuard';
import { LoginPage } from '@/pages/auth/LoginPage';
import { ForbiddenPage } from '@/pages/auth/ForbiddenPage';
import { NotFoundPage } from '@/pages/NotFound';
import { PendingFeaturePage } from '@/pages/placeholder/PlaceholderPage';
import { PageLoading } from '@/components/feedback/LoadingSpinner';
import { localEvaluatorEnabled } from '@/lib/demoEnvironment';
import { onboardingViewAuthorities } from '@/api/adminApi';
const EvaluatorGuide = React.lazy(() => import('@/evaluator/EvaluatorGuide'));
const ExecutiveMisPage = React.lazy(() => import('@/pages/analytics/ExecutiveMisPage'));
const SpecializedAnalyticsPage = React.lazy(() => import('@/pages/analytics/SpecializedAnalyticsPage'));
const CommunicationCenterPage = React.lazy(() => import('@/pages/communication/CommunicationCenterPage'));
const GovernancePage = React.lazy(() => import('@/pages/governance/GovernancePage'));

const RegistrationPage = React.lazy(() => import('@/pages/auth/RegistrationPage').then((module) => ({ default: module.RegistrationPage })));
const AdminOnboardingPage = React.lazy(() => import('@/pages/admin/AdminOnboardingPage').then((module) => ({ default: module.AdminOnboardingPage })));
const AdminUsersPage = React.lazy(() => import('@/pages/admin/AdminUsersPage').then((module) => ({ default: module.AdminUsersPage })));
const AdminCareersPage = React.lazy(() => import('@/pages/admin/AdminCareersPage').then((module) => ({ default: module.AdminCareersPage })));
const TrainingRequirementsPage = React.lazy(() => import('@/pages/training/TrainingRequirementsPage').then((module) => ({ default: module.TrainingRequirementsPage })));
const AdminDashboard = React.lazy(() => import('@/pages/admin/AdminDashboard').then((module) => ({ default: module.AdminDashboard })));
const AssessorWorkbenchPage = React.lazy(() => import('@/pages/assessor/AssessorWorkbenchPage').then((module) => ({ default: module.AssessorWorkbenchPage })));
const AssessorScoringPage = React.lazy(() => import('@/pages/assessor/AssessorScoringPage').then((module) => ({ default: module.AssessorScoringPage })));
const ProviderOverviewPage = React.lazy(() => import('@/pages/provider/ProviderOverviewPage').then((module) => ({ default: module.ProviderOverviewPage })));
const ProviderCentersPage = React.lazy(() => import('@/pages/provider/ProviderCentersPage').then((module) => ({ default: module.ProviderCentersPage })));
const VenueBookingsPage = React.lazy(() => import('@/pages/provider/VenueBookingsPage').then((module) => ({ default: module.VenueBookingsPage })));
const ProviderBatchesPage = React.lazy(() => import('@/pages/provider/ProviderBatchesPage').then((module) => ({ default: module.ProviderBatchesPage })));
const ProviderEnrollmentPage = React.lazy(() => import('@/pages/provider/ProviderEnrollmentPage').then((module) => ({ default: module.ProviderEnrollmentPage })));
const ProviderBulkImportPage = React.lazy(() => import('@/pages/provider/ProviderBulkImportPage').then((module) => ({ default: module.ProviderBulkImportPage })));
const ProviderQuarantinePage = React.lazy(() => import('@/pages/provider/ProviderQuarantinePage').then((module) => ({ default: module.ProviderQuarantinePage })));
const ProviderDataQualityPage = React.lazy(() => import('@/pages/provider/ProviderDataQualityPage').then((module) => ({ default: module.ProviderDataQualityPage })));
const ProviderScorecardsPage = React.lazy(() => import('@/pages/provider/ProviderScorecardsPage').then((module) => ({ default: module.ProviderScorecardsPage })));
const TraineeOverviewPage = React.lazy(() => import('@/pages/trainee/TraineeOverviewPage').then((module) => ({ default: module.TraineeOverviewPage })));
const TraineeTrainingPage = React.lazy(() => import('@/pages/trainee/TraineeTrainingPage').then((module) => ({ default: module.TraineeTrainingPage })));
const TraineeConsentPage = React.lazy(() => import('@/pages/trainee/TraineeConsentPage').then((module) => ({ default: module.TraineeConsentPage })));
const TraineeAssessmentsPage = React.lazy(() => import('@/pages/trainee/TraineeAssessmentsPage').then((module) => ({ default: module.TraineeAssessmentsPage })));
const TraineeJourneyPage = React.lazy(() => import('@/pages/trainee/TraineeJourneyPage').then((module) => ({ default: module.TraineeJourneyPage })));
const EmployerDashboardPage = React.lazy(() => import('@/pages/employer/EmployerDashboardPage').then((module) => ({ default: module.EmployerDashboardPage })));
const EmployerVacanciesPage = React.lazy(() => import('@/pages/employer/EmployerVacanciesPage').then((module) => ({ default: module.EmployerVacanciesPage })));
const EmployerPlacementsPage = React.lazy(() => import('@/pages/employer/EmployerPlacementsPage').then((module) => ({ default: module.EmployerPlacementsPage })));
const EmployerVerificationsPage = React.lazy(() => import('@/pages/employer/EmployerVerificationsPage').then((module) => ({ default: module.EmployerVerificationsPage })));
import { TraineePortalLayout } from '@/pages/trainee/TraineePortalLayout';
import { useAuth } from '@/context/AuthContext';
import { getDefaultLandingRouteForPersona, isAdministrativePersona } from '@/api/types';

function RootRedirect(): React.JSX.Element {
  const { isAuthenticated, defaultLandingRoute, isInitializing, personas } = useAuth();
  if (isInitializing) {
    return <PageLoading message="Initializing platform..." />;
  }
  if (!isAuthenticated) {
    return <Navigate to="/login" replace />;
  }
  if (!personas || personas.length === 0) {
    return <Navigate to="/forbidden" replace />;
  }
  return <Navigate to={defaultLandingRoute} replace />;
}

function DashboardRedirect(): React.JSX.Element {
  const { activePersona, primaryPersona, isAuthenticated } = useAuth();
  if (!isAuthenticated) {
    return <Navigate to="/login" replace />;
  }
  const currentPersona = activePersona || primaryPersona;
  if (currentPersona) {
    return <Navigate to={getDefaultLandingRouteForPersona(currentPersona)} replace />;
  }
  return <Navigate to="/forbidden" replace />;
}

function EmployerAlias({ to, children }: { to: string; children: React.ReactNode }) {
  const { activePersona, primaryPersona } = useAuth();
  return (activePersona ?? primaryPersona) === 'EMPLOYER' ? <Navigate to={to} replace /> : children;
}

function TraineeRouteRedirect(): React.JSX.Element {
  const { activePersona, primaryPersona } = useAuth();
  const currentPersona = activePersona || primaryPersona;
  if (currentPersona === 'TRAINING_PROVIDER') {
    return <Navigate to="/provider/enrollment" replace />;
  }
  if (currentPersona === 'ASSESSOR') {
    return <Navigate to="/assessor/dashboard" replace />;
  }
  if (currentPersona === 'TRAINEE') {
    return <Navigate to="/trainee/dashboard" replace />;
  }
  if (isAdministrativePersona(currentPersona)) {
    return <Navigate to="/admin/onboarding" replace />;
  }
  return <Navigate to="/forbidden" replace />;
}

function BatchesRouteRedirect(): React.JSX.Element {
  const { activePersona, primaryPersona } = useAuth();
  const currentPersona = activePersona || primaryPersona;
  if (currentPersona === 'TRAINING_PROVIDER') {
    return <Navigate to="/provider/batches" replace />;
  }
  if (isAdministrativePersona(currentPersona)) {
    return (
      <PendingFeaturePage
        title="Administrative Training Batches Oversight"
        plannedPhase="Phase 30.11"
        subsystemCode="SIH-BATCH-ADMIN"
        description="Administrative monitoring, compliance tracking, and cross-provider inspection of vocational training batches across accredited training centers."
        guidanceNote="Training Providers manage batches directly from the Training Provider Batches workbench."
      />
    );
  }
  return <Navigate to="/forbidden" replace />;
}

function AssessmentsRouteRedirect(): React.JSX.Element {
  const { activePersona, primaryPersona } = useAuth();
  const currentPersona = activePersona || primaryPersona;
  if (currentPersona === 'ASSESSOR') {
    return <Navigate to="/assessor/dashboard" replace />;
  }
  if (isAdministrativePersona(currentPersona)) {
    return (
      <PendingFeaturePage
        title="Administrative Assessment & Certification Governance"
        plannedPhase="Phase 30.11"
        subsystemCode="SIH-ASSESSMENT-ADMIN"
        description="Centralized administration, integrity auditing, and compliance monitoring of sector skill council assessments and certification records."
        guidanceNote="Accredited assessors perform candidate evaluation and scoring directly within the Assessor Workbench."
      />
    );
  }
  return <Navigate to="/forbidden" replace />;
}

function ImportRouteRedirect(): React.JSX.Element {
  const { activePersona, primaryPersona } = useAuth();
  const currentPersona = activePersona || primaryPersona;
  if (currentPersona === 'TRAINING_PROVIDER') {
    return <Navigate to="/provider/imports" replace />;
  }
  if (isAdministrativePersona(currentPersona)) {
    return (
      <PendingFeaturePage
        title="National Bulk Ingestion Monitoring"
        plannedPhase="Phase 30.11"
        subsystemCode="SIH-INGEST-ADMIN"
        description="System-wide monitoring of asynchronous batch ingestion, validation queues, quarantine records, and processing telemetry across all training providers."
        guidanceNote="Training Providers initiate and review bulk CSV/Excel ingestions via the Provider Bulk Ingestion console."
      />
    );
  }
  return <Navigate to="/forbidden" replace />;
}

function DataQualityRouteRedirect(): React.JSX.Element {
  const { activePersona, primaryPersona } = useAuth();
  const currentPersona = activePersona || primaryPersona;
  if (currentPersona === 'TRAINING_PROVIDER') {
    return <Navigate to="/provider/data-quality" replace />;
  }
  if (isAdministrativePersona(currentPersona)) {
    return (
      <PendingFeaturePage
        title="National Data Quality & Center Scorecard Oversight"
        plannedPhase="Phase 30.12"
        subsystemCode="SIH-DQ-ADMIN"
        description="Nationwide data quality telemetry, anomaly resolution audit trails, and multi-center compliance scoring across accredited training partners."
        guidanceNote="Training Providers review and remediate flagged center and candidate anomalies via the Provider Data Quality Portal."
      />
    );
  }
  return <Navigate to="/forbidden" replace />;
}

export function AppRouter(): React.JSX.Element {
  return (
    <Suspense fallback={<PageLoading />}>
      <Routes>
        {/* Public Routes */}
        <Route path="/login" element={<LoginPage />} />
        <Route path="/register" element={<RegistrationPage />} />
        {localEvaluatorEnabled && <Route path="/evaluator" element={<EvaluatorGuide />} />}
        <Route path="/" element={<RootRedirect />} />

        {/* Protected App Shell */}
        <Route
          element={
            <ProtectedRoute>
              <AppLayout />
            </ProtectedRoute>
          }
        >
          {/* General Platform Dashboard -> Persona Default Dashboard */}
          <Route
            path="/dashboard"
            element={<DashboardRedirect />}
          />

          {/* Access Denied Page */}
          <Route path="/forbidden" element={<ForbiddenPage />} />

          {/* Persona Dedicated Dashboards */}
          <Route
            path="/admin"
            element={<RoleGuard allowedPersonas={['ADMIN']} />}
          >
            <Route path="dashboard" element={<AdminDashboard />} />
            <Route path="onboarding" element={
              <RoleGuard allowedAuthorities={onboardingViewAuthorities}><AdminOnboardingPage /></RoleGuard>
            } />
            <Route path="users" element={<AdminUsersPage />} />
            <Route path="venue-bookings" element={
              <RoleGuard allowedAuthorities={['venue.booking.review']}><VenueBookingsPage admin /></RoleGuard>
            } />
            <Route path="training-requirements" element={
              <RoleGuard allowedAuthorities={['venue.booking.review']}><TrainingRequirementsPage government /></RoleGuard>
            } />
            <Route path="centers" element={
              <RoleGuard allowedPersonas={['SUPER_ADMIN', 'GOVERNMENT_ADMIN']}>
                <RoleGuard allowedAuthorities={['center.manage']}><ProviderCentersPage admin /></RoleGuard>
              </RoleGuard>
            } />
            <Route path="courses" element={<Navigate to="/admin/training-requirements" replace />} />
            <Route path="careers" element={
              <RoleGuard allowedPersonas={['SUPER_ADMIN', 'GOVERNMENT_ADMIN']}>
                <RoleGuard allowedAuthorities={['employment.manage']}><AdminCareersPage /></RoleGuard>
              </RoleGuard>
            } />
            <Route index element={<Navigate to="dashboard" replace />} />
          </Route>

          <Route
            path="/provider"
            element={<RoleGuard allowedPersonas={['TRAINING_PROVIDER']} />}
          >
            <Route path="dashboard" element={<ProviderOverviewPage />} />
            <Route path="venue-bookings" element={
              <RoleGuard allowedAuthorities={['venue.booking.request']}><VenueBookingsPage /></RoleGuard>
            } />
            <Route path="training-opportunities" element={
              <RoleGuard allowedAuthorities={['venue.booking.request']}><TrainingRequirementsPage /></RoleGuard>
            } />
            <Route path="centers" element={
              <RoleGuard allowedAuthorities={['center.manage']}><ProviderCentersPage /></RoleGuard>
            } />
            <Route path="courses" element={<Navigate to="/provider/training-opportunities" replace />} />
            <Route path="batches" element={<ProviderBatchesPage />} />
            <Route path="enrollment" element={<ProviderEnrollmentPage />} />
            <Route path="imports" element={<ProviderBulkImportPage />} />
            <Route path="quarantine" element={<ProviderQuarantinePage />} />
            <Route path="data-quality" element={<ProviderDataQualityPage />} />
            <Route path="scorecards" element={<ProviderScorecardsPage />} />
            <Route index element={<Navigate to="dashboard" replace />} />
          </Route>

          <Route
            path="/assessor"
            element={<RoleGuard allowedPersonas={['ASSESSOR']} />}
          >
            <Route path="dashboard" element={<AssessorWorkbenchPage />} />
            <Route path="assessments/:assessmentId" element={<AssessorScoringPage />} />
            <Route index element={<Navigate to="dashboard" replace />} />
          </Route>

          <Route
            path="/employer"
            element={<RoleGuard allowedPersonas={['EMPLOYER']} />}
          >
            <Route path="dashboard" element={<EmployerDashboardPage />} />
            <Route path="vacancies" element={<EmployerVacanciesPage />} />
            <Route path="placements" element={<EmployerPlacementsPage />} />
            <Route path="verifications" element={<EmployerVerificationsPage />} />
            <Route index element={<Navigate to="dashboard" replace />} />
          </Route>

          <Route
            path="/trainee"
            element={<RoleGuard allowedPersonas={['TRAINEE']} />}
          >
            <Route element={<TraineePortalLayout />}>
              <Route path="dashboard" element={<TraineeOverviewPage />} />
              <Route path="training" element={<TraineeTrainingPage />} />
              <Route path="consent" element={<TraineeConsentPage />} />
              <Route path="assessments" element={<TraineeAssessmentsPage />} />
              <Route path="journey" element={<TraineeJourneyPage />} />
              <Route index element={<Navigate to="dashboard" replace />} />
            </Route>
          </Route>

          {/* Operational Domain Aliases: Seamlessly redirect to authoritative persona workflows */}
          <Route
            path="/trainees/*"
            element={
              <RoleGuard allowedPersonas={['ADMIN', 'TRAINING_PROVIDER', 'ASSESSOR', 'TRAINEE']}>
                <TraineeRouteRedirect />
              </RoleGuard>
            }
          />
          <Route
            path="/batches/*"
            element={
              <RoleGuard allowedPersonas={['ADMIN', 'TRAINING_PROVIDER']}>
                <BatchesRouteRedirect />
              </RoleGuard>
            }
          />
          <Route
            path="/assessments/*"
            element={
              <RoleGuard allowedPersonas={['ADMIN', 'ASSESSOR']}>
                <AssessmentsRouteRedirect />
              </RoleGuard>
            }
          />
          <Route
            path="/import/*"
            element={
              <RoleGuard allowedPersonas={['ADMIN', 'TRAINING_PROVIDER']}>
                <ImportRouteRedirect />
              </RoleGuard>
            }
          />
          <Route
            path="/data-quality/*"
            element={
              <RoleGuard allowedPersonas={['ADMIN', 'TRAINING_PROVIDER']}>
                <DataQualityRouteRedirect />
              </RoleGuard>
            }
          />
          <Route
            path="/users/*"
            element={
              <RoleGuard allowedPersonas={['ADMIN']}>
                <Navigate to="/admin/users" replace />
              </RoleGuard>
            }
          />
          <Route
            path="/placements/*"
            element={
              <RoleGuard allowedPersonas={['ADMIN', 'EMPLOYER', 'TRAINING_PROVIDER']}>
                <EmployerAlias to="/employer/placements">
                  <PendingFeaturePage
                    title="Placements &amp; Apprenticeships Management"
                    plannedPhase="Phase 30.7"
                    subsystemCode="SIH-PLACEMENT"
                    description="Placement confirmation is available in the Employer portal for candidates assigned to the employer."
                  />
                </EmployerAlias>
              </RoleGuard>
            }
          />
          <Route
            path="/employment/*"
            element={
              <RoleGuard allowedPersonas={['ADMIN', 'EMPLOYER']}>
                <EmployerAlias to="/employer/verifications">
                  <Navigate to="/admin/careers" replace />
                </EmployerAlias>
              </RoleGuard>
            }
          />
          <Route path="/communication" element={
            <RoleGuard allowedPersonas={['SUPER_ADMIN', 'GOVERNMENT_ADMIN', 'ASSESSOR']}>
              <RoleGuard allowedAuthorities={['communication.manage', 'survey.manage', 'system.manage', 'assessment.manage']}>
                <CommunicationCenterPage />
              </RoleGuard>
            </RoleGuard>
          } />
          <Route path="/analytics" element={
            <RoleGuard allowedPersonas={['SUPER_ADMIN', 'GOVERNMENT_ADMIN', 'ANALYST']}>
              <RoleGuard allowedAuthorities={['analytics.read']} />
            </RoleGuard>
          }>
            <Route index element={<ExecutiveMisPage />} />
            <Route path="specialized" element={<SpecializedAnalyticsPage />} />
          </Route>
          <Route
            path="/audit"
            element={
              <RoleGuard allowedAuthorities={['audit.read', 'system.manage', 'system.event.read', 'system.event.manage', 'ROLE_ADMIN', 'ROLE_super_admin']}>
                <GovernancePage />
              </RoleGuard>
            }
          />
        </Route>

        {/* 404 Fallback */}
        <Route path="*" element={<NotFoundPage />} />
      </Routes>
    </Suspense>
  );
}
