import React from 'react';
import { useAuth } from '@/context/AuthContext';

interface PersonaDashboardLayoutProps {
  title: string;
  subtitle: string;
  personaTag: string;
  badgeColor?: string;
  description: string;
  nextPhases: string;
}

function BasePersonaDashboard({
  title,
  subtitle,
  personaTag,
  description,
  nextPhases,
}: PersonaDashboardLayoutProps): React.JSX.Element {
  const { user } = useAuth();

  return (
    <div className="space-y-6">
      {/* Persona Header Banner */}
      <div className="card p-6 border-l-4 border-l-navy">
        <div className="flex flex-col md:flex-row md:items-center justify-between gap-4">
          <div>
            <div className="flex items-center gap-2 mb-1">
              <h1 className="text-2xl font-bold text-navy">{title}</h1>
              <span className="badge-info font-semibold">{personaTag}</span>
            </div>
            <p className="text-sm text-gray-600">{subtitle}</p>
          </div>

          {/* User Session Metadata Badge */}
          {user && (
            <div className="bg-gray-50 border border-gray-200 rounded-md p-3 text-xs space-y-1 text-gray-700">
              <div>
                <span className="font-semibold text-gray-900">Signed In As:</span> {user.username} (ID: {user.id})
              </div>
              {user.email && (
                <div>
                  <span className="font-semibold text-gray-900">Email:</span> {user.email}
                </div>
              )}
              <div>
                <span className="font-semibold text-gray-900">Authorities:</span> {user.authorities.length} granted
              </div>
              {(user.trainingProviderId || user.employerId || user.stateId) && (
                <div className="pt-1 text-[11px] text-gray-500">
                  {user.trainingProviderId && `TP ID: ${user.trainingProviderId} `}
                  {user.employerId && `Employer ID: ${user.employerId} `}
                  {user.stateId && `State ID: ${user.stateId} `}
                  {user.districtId && `District ID: ${user.districtId}`}
                </div>
              )}
            </div>
          )}
        </div>
      </div>

      {/* Scope Notice Card */}
      <div className="card p-6">
        <h2 className="text-base font-semibold text-gray-900 mb-2">Portal Status &amp; Roadmap Scope</h2>
        <p className="text-sm text-gray-600 leading-relaxed mb-4">{description}</p>
        <div className="p-4 rounded-md bg-navy-50 border border-blue-100 text-xs text-navy flex items-start gap-2.5">
          <span className="text-base leading-none" aria-hidden="true">
            📋
          </span>
          <div>
            <span className="font-semibold">Subsequent Implementation Scope:</span> {nextPhases}
          </div>
        </div>
      </div>

      {/* Empty State Container */}
      <div className="card p-12 text-center text-gray-500">
        <div className="text-4xl mb-3" aria-hidden="true">
          📊
        </div>
        <h3 className="text-base font-semibold text-gray-900 mb-1">Live Records Pending Domain Implementation</h3>
        <p className="text-sm text-gray-500 max-w-md mx-auto">
          In strict accordance with the project governance policy, no synthetic or placeholder records are displayed.
          Domain workflows and dynamic query hooks will connect in the subsequent dedicated phase.
        </p>
      </div>
    </div>
  );
}

export function AdminDashboard(): React.JSX.Element {
  return (
    <BasePersonaDashboard
      title="Government Administrator Portal"
      subtitle="National and State-Level Oversight, Governance & System Auditing"
      personaTag="ROLE_ADMIN"
      description="Central command console for government officers and system administrators. Authorized for trainee lifecycle management, data quality audits, batch oversight, and analytics exports."
      nextPhases="Phase 30.3 (Admin Dashboard & Trainee Management), Phase 30.4 (Batches, Centers & Attendance), Phase 30.6 (Analytics, Visualizations & Data Exports), Phase 30.7 (Data Quality, System Auditing & User Management)."
    />
  );
}

export function ProviderDashboard(): React.JSX.Element {
  return (
    <BasePersonaDashboard
      title="Training Provider Portal"
      subtitle="Training Partner Operations, Batch Scheduling & Candidate Enrollment"
      personaTag="ROLE_TRAINING_PROVIDER"
      description="Operations console for affiliated vocational training providers and training centers. Authorized to submit candidates, manage batches, and monitor certification progress."
      nextPhases="Phase 30.5 (Training Provider Portal — Batches, Centers, Candidate Rosters, Attendance Uploads)."
    />
  );
}

export function AssessorDashboard(): React.JSX.Element {
  return (
    <BasePersonaDashboard
      title="Assessor &amp; Verifier Portal"
      subtitle="Assessment Scheduling, Result Evaluation & Employment Verification"
      personaTag="ROLE_ASSESSOR"
      description="Independent evaluation console for accredited assessment agencies and verification officers. Authorized to record assessment outcomes, grade evaluations, and conduct employment verifications."
      nextPhases="Phase 30.8 (Assessor & Verifier Portal — Assessment Rosters, Scoring, Employment Evidence Verification)."
    />
  );
}

export function EmployerDashboard(): React.JSX.Element {
  return (
    <BasePersonaDashboard
      title="Employer Portal"
      subtitle="Talent Acquisition, Job Postings & Placement Milestone Tracking"
      personaTag="ROLE_EMPLOYER"
      description="Corporate and industrial portal for partner enterprises hiring certified vocational candidates. Authorized to confirm candidate placements, verify ongoing employment, and report retention milestones."
      nextPhases="Phase 30.9 (Employer Portal — Candidate Search, Offer Records, Employment Retention Confirmations)."
    />
  );
}

export function TraineeDashboard(): React.JSX.Element {
  return (
    <BasePersonaDashboard
      title="Trainee Self-Service Portal"
      subtitle="Candidate Training Record, Certifications & Placement Tracking"
      personaTag="ROLE_TRAINEE"
      description="Candidate-facing portal for registered learners across national skilling schemes. Authorized to review enrollment history, download authentic certificates, and update employment status."
      nextPhases="Phase 30.10 (Trainee Portal — Profile, Training History, Digital Certificates, Job Opportunities)."
    />
  );
}
