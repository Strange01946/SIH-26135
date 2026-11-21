import React, { Suspense } from 'react';
import { Navigate, Route, Routes } from 'react-router-dom';
import { AppLayout } from '@/components/layout/AppLayout';
import { NotFoundPage } from '@/pages/NotFound';
import { PlaceholderPage } from '@/pages/placeholder/PlaceholderPage';
import { PageLoading } from '@/components/feedback/LoadingSpinner';

export function AppRouter(): React.JSX.Element {
  return (
    <Suspense fallback={<PageLoading />}>
      <Routes>
        {/* Redirect root → dashboard */}
        <Route path="/" element={<Navigate to="/dashboard" replace />} />

        {/* Login — no layout (Phase 30.2 will implement) */}
        <Route
          path="/login"
          element={
            <PlaceholderPage
              title="Login"
              description="Authentication will be implemented in Phase 30.2."
            />
          }
        />

        {/* Main application shell */}
        <Route element={<AppLayout />}>
          <Route
            path="/dashboard"
            element={
              <PlaceholderPage
                title="Dashboard"
                description="Role-aware dashboards will be implemented per persona."
              />
            }
          />

          {/* Admin routes */}
          <Route
            path="/trainees/*"
            element={
              <PlaceholderPage title="Trainee Management" />
            }
          />
          <Route
            path="/batches/*"
            element={
              <PlaceholderPage title="Training Batches" />
            }
          />
          <Route
            path="/assessments/*"
            element={
              <PlaceholderPage title="Assessments" />
            }
          />
          <Route
            path="/placements/*"
            element={
              <PlaceholderPage title="Placements" />
            }
          />
          <Route
            path="/employment/*"
            element={
              <PlaceholderPage title="Employment Records" />
            }
          />
          <Route
            path="/analytics/*"
            element={
              <PlaceholderPage title="Analytics & Exports" />
            }
          />
          <Route
            path="/data-quality/*"
            element={
              <PlaceholderPage title="Data Quality" />
            }
          />
          <Route
            path="/import/*"
            element={
              <PlaceholderPage title="Bulk Import" />
            }
          />
          <Route
            path="/audit/*"
            element={
              <PlaceholderPage title="Audit Log" />
            }
          />
          <Route
            path="/users/*"
            element={
              <PlaceholderPage title="User Management" />
            }
          />
        </Route>

        {/* 404 */}
        <Route path="*" element={<NotFoundPage />} />
      </Routes>
    </Suspense>
  );
}
