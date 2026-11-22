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

function RootRedirect(): React.JSX.Element {
  const { isAuthenticated, defaultLandingRoute, isInitializing } = useAuth();
  if (isInitializing) {
    return <PageLoading message="Initializing platform..." />;
  }
  return <Navigate to={isAuthenticated ? defaultLandingRoute : '/login'} replace />;
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
          {/* General Platform Dashboard */}
          <Route
            path="/dashboard"
            element={
              <PlaceholderPage
                title="Platform Overview"
                description="General multi-tenant statistics and announcements across all skilling domains."
              />
            }
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

          {/* Operational Domain Routes Guarded by Persona */}
          <Route
            path="/trainees/*"
            element={
              <RoleGuard allowedPersonas={['ADMIN', 'TRAINING_PROVIDER', 'ASSESSOR']}>
                <PlaceholderPage title="Trainee Management" />
              </RoleGuard>
            }
          />
          <Route
            path="/batches/*"
            element={
              <RoleGuard allowedPersonas={['ADMIN', 'TRAINING_PROVIDER']}>
                <PlaceholderPage title="Training Batches" />
              </RoleGuard>
            }
          />
          <Route
            path="/assessments/*"
            element={
              <RoleGuard allowedPersonas={['ADMIN', 'ASSESSOR']}>
                <PlaceholderPage title="Assessments &amp; Certifications" />
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
            path="/data-quality/*"
            element={
              <RoleGuard allowedPersonas={['ADMIN']}>
                <PlaceholderPage title="Data Quality Scorecard" />
              </RoleGuard>
            }
          />
          <Route
            path="/import/*"
            element={
              <RoleGuard allowedPersonas={['ADMIN', 'TRAINING_PROVIDER']}>
                <PlaceholderPage title="Bulk Data Ingestion" />
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
          <Route
            path="/users/*"
            element={
              <RoleGuard allowedPersonas={['ADMIN']}>
                <PlaceholderPage title="User Management &amp; Permissions" />
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
