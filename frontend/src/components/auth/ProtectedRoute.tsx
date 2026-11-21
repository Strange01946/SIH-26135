import React from 'react';
import { Navigate, Outlet, useLocation } from 'react-router-dom';
import { useAuth } from '@/context/AuthContext';
import { PageLoading } from '@/components/feedback/LoadingSpinner';

interface ProtectedRouteProps {
  children?: React.ReactNode;
}

export function ProtectedRoute({
  children,
}: ProtectedRouteProps): React.JSX.Element {
  const { isAuthenticated, isInitializing } = useAuth();
  const location = useLocation();

  // Temporary initialization state: prevents content flash during startup session check
  if (isInitializing) {
    return <PageLoading message="Verifying session..." />;
  }

  // Unauthenticated: safely redirect to login with original destination in state
  if (!isAuthenticated) {
    return <Navigate to="/login" state={{ from: location }} replace />;
  }

  return children ? <>{children}</> : <Outlet />;
}
