import React, { useEffect } from 'react';
import { Navigate, Outlet, useLocation } from 'react-router-dom';
import { useAuth } from '@/context/AuthContext';
import { PageLoading } from '@/components/feedback/LoadingSpinner';
import { type UserPersona, isAdministrativePersona } from '@/api/types';

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
  const {
    isAuthenticated,
    isInitializing,
    personas,
    activePersona,
    setActivePersona,
    hasAnyRole,
    hasAuthority,
  } = useAuth();
  const location = useLocation();

  // Auto-synchronize activePersona if a multi-role user directly accesses an authorized route
  // that requires a different granted persona, keeping the sidebar and header aligned with the view.
  useEffect(() => {
    if (!isAuthenticated || personas.length === 0 || allowedPersonas.length === 0) {
      return;
    }
    const isAllowedAdmin = allowedPersonas.includes('ADMIN') && isAdministrativePersona(activePersona);
    if (!activePersona || (!allowedPersonas.includes(activePersona) && !isAllowedAdmin)) {
      const matchingPersona = allowedPersonas.find(
        (p) => personas.includes(p) || (p === 'ADMIN' && personas.some(isAdministrativePersona)),
      );
      if (matchingPersona && matchingPersona !== activePersona) {
        const target =
          (matchingPersona === 'ADMIN' && personas.find(isAdministrativePersona)) || matchingPersona;
        setActivePersona(target);
      }
    }
  }, [isAuthenticated, personas, allowedPersonas, activePersona, setActivePersona]);

  if (isInitializing) {
    return <PageLoading message="Verifying permissions..." />;
  }

  if (!isAuthenticated) {
    return <Navigate to="/login" state={{ from: location }} replace />;
  }

  // Check matching persona
  const hasPersonaMatch =
    allowedPersonas.length > 0 &&
    allowedPersonas.some((p) => {
      if (personas.includes(p)) return true;
      if (p === 'ADMIN' && personas.some(isAdministrativePersona)) return true;
      return false;
    });

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
