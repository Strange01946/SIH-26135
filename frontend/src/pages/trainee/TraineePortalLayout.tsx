import React from 'react';
import { NavLink, Outlet } from 'react-router-dom';
import { useTraineeIdentity } from '@/hooks/useTraineeIdentity';
import { LoadingSpinner } from '@/components/feedback/LoadingSpinner';

const TABS = [
  { name: 'Overview', to: '/trainee/dashboard', end: true },
  { name: 'Training & Attendance', to: '/trainee/training' },
  { name: 'DPDP Privacy & Consent', to: '/trainee/consent' },
  { name: 'Assessments & Certificates', to: '/trainee/assessments' },
  { name: 'Career Journey & Surveys', to: '/trainee/journey' },
];

export const TraineePortalLayout: React.FC = () => {
  const { trainee, traineeId, isUnlinked, isLoading, error, refetch } = useTraineeIdentity();

  return (
    <div className="space-y-6">
      {/* Portal Header / Banner */}
      <div className="bg-white rounded-lg shadow-sm border border-gov-neutral-200 overflow-hidden">
        <div className="bg-gradient-to-r from-gov-navy to-gov-navy-light px-6 py-5 text-white flex flex-col md:flex-row md:items-center md:justify-between gap-4">
          <div>
            <div className="flex items-center gap-2 mb-1">
              <span className="text-xs font-semibold uppercase tracking-wider bg-white/20 text-white px-2 py-0.5 rounded">
                GIGW / NCVET Compliant
              </span>
              <span className="text-xs font-medium text-gov-neutral-200">
                National Skill Development Mission
              </span>
            </div>
            <h1 className="text-xl md:text-2xl font-bold tracking-tight">
              Candidate Self-Service &amp; Career Portal
            </h1>
            <p className="text-sm text-gov-neutral-200 mt-0.5">
              Unified portal for skill development training, attendance, verified certifications, and DPDP privacy governance.
            </p>
          </div>

          {/* Profile Identifier Badge */}
          <div className="bg-white/10 backdrop-blur-sm rounded-lg px-4 py-2.5 border border-white/20 text-right">
            {isLoading ? (
              <div className="flex items-center gap-2 text-sm text-gov-neutral-200">
                <LoadingSpinner size="sm" />
                <span>Loading Candidate Profile...</span>
              </div>
            ) : trainee ? (
              <div>
                <p className="text-xs uppercase tracking-wider text-gov-neutral-300 font-medium">Candidate</p>
                <p className="text-base font-bold text-white leading-tight">
                  {trainee.firstName} {trainee.lastName ?? ''}
                </p>
                <p className="text-xs text-gov-neutral-200 font-mono mt-0.5">
                  Reg: {trainee.registrationNumber}
                </p>
              </div>
            ) : isUnlinked ? (
              <div className="text-amber-200 text-sm font-medium">
                <p className="font-semibold text-amber-100">Profile Unlinked</p>
                <p className="text-xs text-amber-200/80">Pending Center Registration</p>
              </div>
            ) : (
              <div className="text-gov-neutral-300 text-sm">
                <span>Self-Service Candidate</span>
              </div>
            )}
          </div>
        </div>

        {/* Global Unlinked Alert Banner */}
        {isUnlinked && (
          <div
            role="alert"
            className="bg-amber-50 border-b border-amber-200 px-6 py-3.5 flex items-start gap-3 text-amber-900"
          >
            <svg
              className="w-5 h-5 text-amber-600 mt-0.5 shrink-0"
              fill="none"
              stroke="currentColor"
              viewBox="0 0 24 24"
              aria-hidden="true"
            >
              <path
                strokeLinecap="round"
                strokeLinejoin="round"
                strokeWidth={2}
                d="M12 9v2m0 4h.01m-6.938 4h13.856c1.54 0 2.502-1.667 1.732-3L13.732 4c-.77-1.333-2.694-1.333-3.464 0L3.34 16c-.77 1.333.192 3 1.732 3z"
              />
            </svg>
            <div className="text-xs md:text-sm">
              <span className="font-bold">Candidate Profile Registration Notice:</span> Your user login account is active, but your registration has not yet been linked to a specific Trainee Identification Record in the National Skills Database. Please contact your Training Center Coordinator or NCVET administrator to complete registration linkage.
            </div>
          </div>
        )}

        {/* Tab Navigation */}
        <nav
          className="flex border-b border-gov-neutral-200 px-6 space-x-1 overflow-x-auto bg-gov-neutral-50"
          aria-label="Trainee Portal Navigation"
        >
          {TABS.map((tab) => (
            <NavLink
              key={tab.to}
              to={tab.to}
              end={tab.end}
              className={({ isActive }) =>
                `px-4 py-3 text-sm font-medium whitespace-nowrap border-b-2 transition-colors duration-150 focus:outline-none focus:ring-2 focus:ring-gov-navy focus:ring-offset-2 ${
                  isActive
                    ? 'border-gov-navy text-gov-navy font-bold bg-white'
                    : 'border-transparent text-gov-neutral-600 hover:text-gov-neutral-900 hover:border-gov-neutral-300'
                }`
              }
            >
              {tab.name}
            </NavLink>
          ))}
        </nav>
      </div>

      {/* Tab Content Routed through Outlet */}
      <Outlet context={{ trainee, traineeId, isUnlinked, isLoading, error, refetch }} />
    </div>
  );
};

export default TraineePortalLayout;
