import React from 'react';
import { Navigate, Outlet, useLocation } from 'react-router-dom';
import { useAuth } from '@/context/AuthContext';
import { PageLoading } from '@/components/feedback/LoadingSpinner';
import type { UserPersona } from '@/api/types';

interface RoleGuardProps {
  allowedRoles?: string[];
  allowedPersonas?: UserPersona[];
  allowedAuthorities?: string[];
  children?: React.ReactNode;
}

export function RoleGuard({
  allowedRoles = [],
  allowedPersonas = [],
  allowedAuthorities = [],
  children,
}: RoleGuardProps): React.JSX.Element {
  const { isAuthenticated, isInitializing, personas, hasAnyRole, hasAuthority } = useAuth();
  const location = useLocation();

  if (isInitializing) {
    return <PageLoading message="Verifying permissions..." />;
  }

  if (!isAuthenticated) {
    return <Navigate to="/login" state={{ from: location }} replace />;
  }

  // Check matching persona
  const hasPersonaMatch =
    allowedPersonas.length > 0 &&
    allowedPersonas.some((p) => personas.includes(p));

  // Check matching roles
  const hasRoleMatch =
    allowedRoles.length > 0 && hasAnyRole(allowedRoles);

  // Check matching granular authorities
  const hasAuthorityMatch =
    allowedAuthorities.length > 0 &&
    allowedAuthorities.some((a) => hasAuthority(a));

  const isAuthorized =
    (allowedPersonas.length === 0 &&
      allowedRoles.length === 0 &&
      allowedAuthorities.length === 0) ||
    hasPersonaMatch ||
    hasRoleMatch ||
    hasAuthorityMatch;

  if (!isAuthorized) {
    return <Navigate to="/forbidden" replace />;
  }

  return children ? <>{children}</> : <Outlet />;
}
