import React, { Suspense } from 'react';
import { Navigate, Route, Routes } from 'react-router-dom';
import { AppLayout } from '@/components/layout/AppLayout';
import { ProtectedRoute } from '@/components/auth/ProtectedRoute';
import { RoleGuard } from '@/components/auth/RoleGuard';
import { LoginPage } from '@/pages/auth/LoginPage';
import { RegistrationPage } from '@/pages/auth/RegistrationPage';
import { AdminOnboardingPage } from '@/pages/admin/AdminOnboardingPage';
import { AdminUsersPage } from '@/pages/admin/AdminUsersPage';
import { ForbiddenPage } from '@/pages/auth/ForbiddenPage';
import { NotFoundPage } from '@/pages/NotFound';
import { PlaceholderPage } from '@/pages/placeholder/PlaceholderPage';
import { PageLoading } from '@/components/feedback/LoadingSpinner';
import {
  AdminDashboard,
  EmployerDashboard,
} from '@/pages/personas/PersonaDashboards';
import { AssessorWorkbenchPage } from '@/pages/assessor/AssessorWorkbenchPage';
import { AssessorScoringPage } from '@/pages/assessor/AssessorScoringPage';
import { ProviderOverviewPage } from '@/pages/provider/ProviderOverviewPage';
import { ProviderCentersPage } from '@/pages/provider/ProviderCentersPage';
import { ProviderCoursesPage } from '@/pages/provider/ProviderCoursesPage';
import { ProviderBatchesPage } from '@/pages/provider/ProviderBatchesPage';
import { ProviderEnrollmentPage } from '@/pages/provider/ProviderEnrollmentPage';
import { ProviderBulkImportPage } from '@/pages/provider/ProviderBulkImportPage';
import { ProviderQuarantinePage } from '@/pages/provider/ProviderQuarantinePage';
import { ProviderDataQualityPage } from '@/pages/provider/ProviderDataQualityPage';
import { ProviderScorecardsPage } from '@/pages/provider/ProviderScorecardsPage';
import { TraineePortalLayout } from '@/pages/trainee/TraineePortalLayout';
import { TraineeOverviewPage } from '@/pages/trainee/TraineeOverviewPage';
import { TraineeTrainingPage } from '@/pages/trainee/TraineeTrainingPage';
import { TraineeConsentPage } from '@/pages/trainee/TraineeConsentPage';
import { TraineeAssessmentsPage } from '@/pages/trainee/TraineeAssessmentsPage';
import { TraineeJourneyPage } from '@/pages/trainee/TraineeJourneyPage';
import { useAuth } from '@/context/AuthContext';
import { getDefaultLandingRouteForPersona } from '@/api/types';

function RootRedirect(): React.JSX.Element {
  const { isAuthenticated, defaultLandingRoute, isInitializing } = useAuth();
  if (isInitializing) {
    return <PageLoading message="Initializing platform..." />;
  }
  return <Navigate to={isAuthenticated ? defaultLandingRoute : '/login'} replace />;
}

function DashboardRedirect(): React.JSX.Element {
  const { activePersona, primaryPersona } = useAuth();
  const currentPersona = activePersona || primaryPersona;
  if (currentPersona) {
    return <Navigate to={getDefaultLandingRouteForPersona(currentPersona)} replace />;
  }
  return <Navigate to="/login" replace />;
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
  return <Navigate to="/admin/onboarding" replace />;
}

export function AppRouter(): React.JSX.Element {
  return (
    <Suspense fallback={<PageLoading />}>
      <Routes>
        {/* Public Routes */}
        <Route path="/login" element={<LoginPage />} />
        <Route path="/register" element={<RegistrationPage />} />
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
            <Route path="onboarding" element={<AdminOnboardingPage />} />
            <Route path="users" element={<AdminUsersPage />} />
            <Route index element={<Navigate to="dashboard" replace />} />
          </Route>

          <Route
            path="/provider"
            element={<RoleGuard allowedPersonas={['TRAINING_PROVIDER']} />}
          >
            <Route path="dashboard" element={<ProviderOverviewPage />} />
            <Route path="centers" element={<ProviderCentersPage />} />
            <Route path="courses" element={<ProviderCoursesPage />} />
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
            <Route path="dashboard" element={<EmployerDashboard />} />
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
                <Navigate to="/provider/batches" replace />
              </RoleGuard>
            }
          />
          <Route
            path="/assessments/*"
            element={
              <RoleGuard allowedPersonas={['ADMIN', 'ASSESSOR']}>
                <Navigate to="/assessor/dashboard" replace />
              </RoleGuard>
            }
          />
          <Route
            path="/import/*"
            element={
              <RoleGuard allowedPersonas={['ADMIN', 'TRAINING_PROVIDER']}>
                <Navigate to="/provider/imports" replace />
              </RoleGuard>
            }
          />
          <Route
            path="/data-quality/*"
            element={
              <RoleGuard allowedPersonas={['ADMIN', 'TRAINING_PROVIDER']}>
                <Navigate to="/provider/data-quality" replace />
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
                <PlaceholderPage title="Placements &amp; Apprenticeships" />
              </RoleGuard>
            }
          />
          <Route
            path="/employment/*"
            element={
              <RoleGuard allowedPersonas={['ADMIN', 'EMPLOYER']}>
                <PlaceholderPage title="Employment Records &amp; Retention" />
              </RoleGuard>
            }
          />
          <Route
            path="/analytics/*"
            element={
              <RoleGuard allowedPersonas={['ADMIN']}>
                <PlaceholderPage title="Analytics &amp; Exports" />
              </RoleGuard>
            }
          />
          <Route
            path="/audit/*"
            element={
              <RoleGuard allowedPersonas={['ADMIN']}>
                <PlaceholderPage title="System Audit Log" />
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
