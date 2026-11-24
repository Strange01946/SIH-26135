import React from 'react';
import { Link } from 'react-router-dom';
import { useAuth } from '@/context/AuthContext';
import { getDefaultLandingRouteForPersona } from '@/api/types';

export interface PendingFeaturePageProps {
  title: string;
  plannedPhase: string;
  description: string;
  subsystemCode?: string;
  guidanceNote?: string;
}

/**
 * Truthful, GIGW-compliant Pending Roadmap Component.
 * Explicitly informs authorized users that an operational module is scheduled for
 * a planned future phase, without inventing fake functionality or displaying dead-end placeholders.
 */
export function PendingFeaturePage({
  title,
  plannedPhase,
  description,
  subsystemCode,
  guidanceNote,
}: PendingFeaturePageProps): React.JSX.Element {
  const { activePersona, primaryPersona } = useAuth();
  const currentPersona = activePersona || primaryPersona;
  const returnRoute = currentPersona
    ? getDefaultLandingRouteForPersona(currentPersona)
    : '/dashboard';

  return (
    <div className="max-w-4xl mx-auto py-8 px-4 sm:px-6">
      <div className="bg-white rounded-lg border border-gray-200 shadow-sm overflow-hidden">
        {/* Top notification bar */}
        <div className="bg-amber-50 border-b border-amber-200 px-6 py-3 flex items-center justify-between">
          <div className="flex items-center gap-2">
            <span className="text-amber-700 text-lg" aria-hidden="true">⏳</span>
            <span className="text-xs font-semibold text-amber-900 uppercase tracking-wide">
              Planned Capability Roadmap · {plannedPhase}
            </span>
          </div>
          {subsystemCode && (
            <span className="text-xs font-mono font-medium px-2 py-0.5 rounded bg-amber-100 text-amber-800 border border-amber-300">
              {subsystemCode}
            </span>
          )}
        </div>

        <div className="p-6 md:p-8 space-y-6">
          <div>
            <h1 className="text-xl md:text-2xl font-bold text-gray-900 tracking-tight">
              {title}
            </h1>
            <p className="mt-2 text-sm text-gray-600 leading-relaxed">
              {description}
            </p>
          </div>

          <div className="bg-blue-50 border border-blue-200 rounded-md p-4 text-xs text-blue-900 space-y-1">
            <div className="font-semibold text-blue-950 flex items-center gap-1.5">
              <span>ℹ️</span> Phase Scope &amp; Implementation Status
            </div>
            <p>
              This operational module is part of the planned SIH-26135 platform evolution and is scheduled for release in <strong>{plannedPhase}</strong>. No mock data or non-authoritative forms are presented.
            </p>
            {guidanceNote && (
              <p className="pt-2 border-t border-blue-200 text-blue-800">
                <strong>Operational Guidance:</strong> {guidanceNote}
              </p>
            )}
          </div>

          <div className="pt-4 border-t border-gray-100 flex items-center justify-between">
            <span className="text-xs text-gray-400">
              National Skill &amp; Placement Tracking Platform · SIH-26135
            </span>
            <Link
              to={returnRoute}
              className="inline-flex items-center gap-1.5 px-4 py-2 text-xs font-medium rounded-md bg-navy text-white hover:bg-navy-800 transition-colors focus:outline-none focus:ring-2 focus:ring-navy"
            >
              ← Return to Dashboard
            </Link>
          </div>
        </div>
      </div>
    </div>
  );
}

// Backwards-compatible export
export const PlaceholderPage = PendingFeaturePage;
