import React from 'react';
import { Link } from 'react-router-dom';
import { useAuth } from '@/context/AuthContext';

export function ForbiddenPage(): React.JSX.Element {
  const { defaultLandingRoute } = useAuth();

  return (
    <div className="flex flex-col items-center justify-center min-h-[450px] text-center px-4">
      <div className="text-6xl mb-4" aria-hidden="true">
        🚫
      </div>
      <h1 className="text-2xl font-bold text-navy mb-2">
        403 — Access Restricted
      </h1>
      <p className="text-gray-600 mb-6 max-w-md text-sm leading-relaxed">
        You do not have the required role or authority to access this section of
        the National Skill &amp; Placement Tracking Platform with your current
        credentials.
      </p>
      <div className="flex items-center gap-3">
        <Link to={defaultLandingRoute} className="btn-primary">
          Return to Authorized Dashboard
        </Link>
      </div>
    </div>
  );
}
