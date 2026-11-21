import React, { useState } from 'react';
import { useOutletContext } from 'react-router-dom';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import type {
  TraineeProfile,
  TraineeConsent,
  ConsentTypeCode,
  CreateTraineeConsentPayload,
} from '@/types/trainee';
import {
  getTraineeConsents,
  createTraineeConsent,
  updateTraineeConsentStatus,
} from '@/api/traineeApi';
import { LoadingSpinner } from '@/components/feedback/LoadingSpinner';
import { EmptyState } from '@/components/feedback/EmptyState';

interface TraineeContextType {
  trainee: TraineeProfile | null;
  traineeId?: number;
  isUnlinked: boolean;
  isLoading: boolean;
}

export interface ConsentTypeConfig {
  code: ConsentTypeCode;
  id: number;
  name: string;
  description: string;
  isRequired: boolean;
  defaultPurpose: string;
}

export const CONSENT_TYPES_CATALOG: ConsentTypeConfig[] = [
  {
    code: 'DATA_PROCESSING',
    id: 1,
    name: 'Core Scheme Data Processing',
    description:
      'Processing of candidate demographics, educational history, biometric attendance, and qualification outcomes in accordance with National Skill Development Scheme rules.',
    isRequired: true,
    defaultPurpose: 'Process registration and training records',
  },
  {
    code: 'EMPLOYMENT_FOLLOW_UP',
    id: 2,
    name: 'Employment Outcome Follow-Up & Verification',
    description:
      'Permission to follow up regarding post-training job placement, apprenticeship opportunities, employer verification, and career tracking.',
    isRequired: false,
    defaultPurpose: 'Contact the trainee about employment outcomes over time',
  },
  {
    code: 'COMMUNICATION_SMS',
    id: 5,
    name: 'Transactional SMS Communication',
    description:
      'Delivery of mandatory operational alerts, batch schedule reminders, examination hall tickets, and authentication OTPs via SMS.',
    isRequired: false,
    defaultPurpose: 'Send operational SMS messages',
  },
  {
    code: 'COMMUNICATION_EMAIL',
    id: 7,
    name: 'Official Email Communications',
    description:
      'Transmission of digitally signed course completion certificates, survey invitations, and circular notices via email.',
    isRequired: false,
    defaultPurpose: 'Send operational email messages',
  },
  {
    code: 'COMMUNICATION_WHATSAPP',
    id: 6,
    name: 'WhatsApp Operational Messaging',
    description:
      'Optional channel for receiving automated timetable updates, skill assessment tips, and interview schedules on WhatsApp.',
    isRequired: false,
    defaultPurpose: 'Send operational WhatsApp messages',
  },
];

export interface EffectiveConsentResolution {
  authoritativeConsent?: TraineeConsent;
  isCurrentlyGranted: boolean;
  statusName: string;
  revokedAt?: string | null;
  grantedAt?: string | null;
}

/**
 * Resolves the authoritative effective consent record and its active grant status
 * in strict alignment with Spring Boot TraineeConsentServiceImpl.hasActiveConsent().
 *
 * Algorithm (matches lines 182-203 of TraineeConsentServiceImpl.java):
 * 1. Filter matching records by consentTypeCode or consentTypeId.
 * 2. If no records exist -> not granted, no authoritative record.
 * 3. Sort matching records by:
 *    - updatedAt (or createdAt if updatedAt is null), descending
 *    - id (tiebreaker), descending
 *    The top record [0] is the authoritative consent record.
 * 4. Determine isCurrentlyGranted:
 *    - authoritative.consentStatusCode === 'GRANTED'
 *    - AND (authoritative.revokedAt is null OR new Date(authoritative.revokedAt) > now)
 *    - AND (authoritative.expiresAt is null OR new Date(authoritative.expiresAt) > now)
 *    - AND (authoritative.grantedAt is null OR new Date(authoritative.grantedAt) <= now)
 */
export function resolveEffectiveConsent(
  consents: TraineeConsent[],
  typeCode: string,
  typeId?: number,
  now: Date = new Date(),
): EffectiveConsentResolution {
  const matching = (consents || []).filter(
    (c) =>
      c.consentTypeCode === typeCode ||
      (typeId !== undefined && c.consentTypeId === typeId),
  );

  if (matching.length === 0) {
    return {
      authoritativeConsent: undefined,
      isCurrentlyGranted: false,
      statusName: 'Not Granted',
    };
  }

  // Sort by updatedAt/createdAt descending, then ID descending
  const sorted = [...matching].sort((a, b) => {
    const timeA = new Date(a.updatedAt || a.createdAt || 0).getTime();
    const timeB = new Date(b.updatedAt || b.createdAt || 0).getTime();
    if (timeB !== timeA) {
      return timeB - timeA;
    }
    return (b.id || 0) - (a.id || 0);
  });

  const authoritative = sorted[0];
  const nowMs = now.getTime();

  const isGrantedStatus =
    authoritative.consentStatusCode?.toUpperCase() === 'GRANTED';

  const notRevoked =
    !authoritative.revokedAt ||
    new Date(authoritative.revokedAt).getTime() > nowMs;

  const notExpired =
    !authoritative.expiresAt ||
    new Date(authoritative.expiresAt).getTime() > nowMs;

  const alreadyGranted =
    !authoritative.grantedAt ||
    new Date(authoritative.grantedAt).getTime() <= nowMs;

  const isCurrentlyGranted =
    isGrantedStatus && notRevoked && notExpired && alreadyGranted;

  return {
    authoritativeConsent: authoritative,
    isCurrentlyGranted,
    statusName: isCurrentlyGranted
      ? 'Granted'
      : (authoritative.consentStatusName || authoritative.consentStatusCode || 'Revoked'),
    revokedAt: authoritative.revokedAt,
    grantedAt: authoritative.grantedAt,
  };
}

/**
 * Normalizes API and network error states into clear, safe user-facing notices
 * without leaking backend internal technical details, SQL messages, or stack traces.
 */
export function getConsentErrorMessage(error: any): string {
  if (!error) {
    return 'Unable to retrieve consent records from the Central Privacy Register.';
  }

  const status = error?.status || error?.response?.status;
  const code = error?.code;

  if (status === 401 || status === 403) {
    return 'Access restricted. You do not have authorization to view or update DPDP statutory consent records.';
  }

  if (status === 404) {
    return 'Candidate consent profile not found in the Central Privacy Register.';
  }

  if (typeof status === 'number' && status >= 500 && status < 600) {
    return 'The Central Privacy Register is temporarily unavailable. Please retry shortly.';
  }

  const msg = typeof error?.message === 'string' ? error.message.toLowerCase() : '';
  if (
    code === 'ERR_NETWORK' ||
    code === 'NETWORK_ERROR' ||
    msg.includes('network') ||
    msg.includes('failed to fetch') ||
    msg.includes('connection refused')
  ) {
    return 'Network communication failure. Please verify your network connection and retry.';
  }

  return 'Unable to synchronize consent records with the Central Privacy Register. Please retry.';
}

export const TraineeConsentPage: React.FC = () => {
  const { traineeId, isUnlinked, isLoading: isIdentityLoading } =
    useOutletContext<TraineeContextType>();
  const queryClient = useQueryClient();
  const [actionError, setActionError] = useState<string | null>(null);

  // Query consents
  const consentsQuery = useQuery({
    queryKey: ['trainee', 'consents', traineeId],
    queryFn: () => getTraineeConsents(traineeId!),
    enabled: Boolean(traineeId),
    retry: false,
  });

  // Mutation to create new consent
  const createConsentMutation = useMutation({
    mutationFn: (payload: CreateTraineeConsentPayload) => createTraineeConsent(payload),
    onSuccess: () => {
      setActionError(null);
      queryClient.invalidateQueries({ queryKey: ['trainee', 'consents', traineeId] });
    },
    onError: (err: any) => {
      setActionError(err?.message || 'Failed to record consent. Please try again.');
    },
  });

  // Mutation to update existing consent status
  const updateConsentMutation = useMutation({
    mutationFn: ({
      consentId,
      statusId,
      revokedAt,
    }: {
      consentId: number;
      statusId: number;
      revokedAt?: string | null;
    }) =>
      updateTraineeConsentStatus(consentId, {
        consentStatusId: statusId,
        revokedAt: revokedAt ?? null,
      }),
    onSuccess: () => {
      setActionError(null);
      queryClient.invalidateQueries({ queryKey: ['trainee', 'consents', traineeId] });
    },
    onError: (err: any) => {
      setActionError(err?.message || 'Failed to update consent status. Please try again.');
    },
  });

  const isMutating =
    createConsentMutation.isPending || updateConsentMutation.isPending;

  if (isIdentityLoading) {
    return (
      <div className="bg-white rounded-lg p-12 border border-gov-neutral-200 flex justify-center">
        <LoadingSpinner size="md" />
      </div>
    );
  }

  if (isUnlinked || !traineeId) {
    return (
      <div className="bg-white rounded-lg p-8 border border-gov-neutral-200">
        <EmptyState
          title="Candidate Profile Not Linked"
          description="A linked candidate identification record is required to manage DPDP Act statutory consents."
        />
      </div>
    );
  }

  const existingConsents = consentsQuery.data ?? [];

  // Handle Grant action
  const handleGrant = (typeConfig: ConsentTypeConfig) => {
    setActionError(null);
    if (!consentsQuery.isSuccess || consentsQuery.isLoading || consentsQuery.isError) {
      setActionError(
        'Consent state is unavailable. Please retry loading consent records before updating preferences.',
      );
      return;
    }

    const { authoritativeConsent } = resolveEffectiveConsent(
      existingConsents,
      typeConfig.code,
      typeConfig.id,
    );

    if (authoritativeConsent) {
      // Re-grant existing record (statusId: 1 for GRANTED per ref_consent_status)
      updateConsentMutation.mutate({
        consentId: authoritativeConsent.id,
        statusId: 1, // GRANTED
        revokedAt: null,
      });
    } else {
      // Create new consent record
      createConsentMutation.mutate({
        traineeId: traineeId!,
        consentTypeId: typeConfig.id,
        consentStatusId: 1, // GRANTED
        purpose: typeConfig.defaultPurpose,
        policyVersion: 'v1.0',
        grantedAt: new Date().toISOString(),
        capturedChannel: 'WEB_PORTAL',
      });
    }
  };

  // Handle Revoke action
  const handleRevoke = (consent: TraineeConsent) => {
    setActionError(null);
    if (!consentsQuery.isSuccess || consentsQuery.isLoading || consentsQuery.isError) {
      setActionError(
        'Consent state is unavailable. Please retry loading consent records before updating preferences.',
      );
      return;
    }

    updateConsentMutation.mutate({
      consentId: consent.id,
      statusId: 2, // REVOKED per ref_consent_status
      revokedAt: new Date().toISOString(),
    });
  };

  return (
    <div className="space-y-6">
      {/* Statutory DPDP Act Section 6 Notice */}
      <section
        aria-labelledby="dpdp-statutory-notice-heading"
        className="bg-gov-neutral-50 rounded-lg border border-gov-neutral-200 p-6"
      >
        <div className="flex items-start gap-4">
          <div className="w-10 h-10 rounded-full bg-gov-navy/10 flex items-center justify-center shrink-0 text-gov-navy">
            <svg className="w-6 h-6" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path
                strokeLinecap="round"
                strokeLinejoin="round"
                strokeWidth={2}
                d="M9 12l2 2 4-4m5.618-4.016A11.955 11.955 0 0112 2.944a11.955 11.955 0 01-8.618 3.04A12.02 12.02 0 003 9c0 5.591 3.824 10.29 9 11.622 5.176-1.332 9-6.03 9-11.622 0-1.042-.133-2.052-.382-3.016z"
              />
            </svg>
          </div>
          <div className="space-y-2">
            <h2
              id="dpdp-statutory-notice-heading"
              className="text-base font-bold text-gov-neutral-900"
            >
              Statutory Privacy &amp; Data Consent Notice (Digital Personal Data Protection Act, 2023 § 6)
            </h2>
            <p className="text-xs md:text-sm text-gov-neutral-700 leading-relaxed">
              Under Section 6 of the DPDP Act, personal data is processed solely for the specified purposes of skill qualification, certification verification, and employment outcome analytics. You have the statutory right to give, manage, or withdraw consent at any time without punitive consequences, except where processing is mandated by law for educational qualification records.
            </p>
            <div className="text-xs text-gov-neutral-500 pt-1 flex flex-wrap gap-x-6 gap-y-1">
              <span><strong>Data Fiduciary:</strong> National Council for Vocational Education and Training (NCVET)</span>
              <span><strong>Data Protection Officer:</strong> dpo@skills.gov.in</span>
              <span><strong>Policy Version:</strong> v1.0 (Statutory 2024 Revision)</span>
            </div>
          </div>
        </div>
      </section>

      {/* Mutation Error Notification */}
      {actionError && (
        <div
          role="alert"
          className="bg-red-50 border border-red-200 p-4 rounded-lg text-red-900 text-sm flex items-center justify-between"
        >
          <span>{actionError}</span>
          <button
            type="button"
            onClick={() => setActionError(null)}
            className="text-xs font-bold text-red-700 hover:underline ml-4"
          >
            Dismiss
          </button>
        </div>
      )}

      {/* Consent Categories Management Cards */}
      <section
        aria-labelledby="consent-management-heading"
        className="space-y-4"
      >
        <div className="flex items-center justify-between">
          <h3
            id="consent-management-heading"
            className="text-base font-bold text-gov-neutral-900"
          >
            Consent Preferences &amp; Permissions
          </h3>
          {isMutating && (
            <div className="flex items-center gap-2 text-xs text-gov-navy font-semibold">
              <LoadingSpinner size="sm" />
              <span>Updating Central Privacy Register...</span>
            </div>
          )}
        </div>

        {consentsQuery.isLoading ? (
          <div className="py-12 bg-white rounded-lg border border-gov-neutral-200 flex justify-center">
            <LoadingSpinner size="md" />
          </div>
        ) : consentsQuery.isError ? (
          <div
            role="alert"
            className="bg-white rounded-lg border border-red-200 p-8 text-center space-y-4 shadow-sm"
          >
            <div className="w-12 h-12 mx-auto rounded-full bg-red-100 flex items-center justify-center text-red-600">
              <svg className="w-6 h-6" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path
                  strokeLinecap="round"
                  strokeLinejoin="round"
                  strokeWidth={2}
                  d="M12 9v2m0 4h.01m-6.938 4h13.856c1.54 0 2.502-1.667 1.732-3L13.732 4c-.77-1.333-2.694-1.333-3.464 0L3.34 16c-.77 1.333.192 3 1.732 3z"
                />
              </svg>
            </div>
            <div>
              <h4 className="text-base font-bold text-gov-neutral-900">
                Unable to Load Consent Records
              </h4>
              <p className="text-xs md:text-sm text-gov-neutral-600 mt-1 max-w-lg mx-auto">
                {getConsentErrorMessage(consentsQuery.error)}
              </p>
            </div>
            <div>
              <button
                type="button"
                onClick={() => consentsQuery.refetch()}
                className="px-4 py-2 text-xs font-bold rounded bg-gov-navy text-white hover:bg-gov-navy-light focus:outline-none focus:ring-2 focus:ring-gov-navy focus:ring-offset-2 transition-colors"
              >
                Retry Loading Consents
              </button>
            </div>
          </div>
        ) : (
          <div className="grid grid-cols-1 gap-4">
            {CONSENT_TYPES_CATALOG.map((typeConfig) => {
              const {
                authoritativeConsent,
                isCurrentlyGranted,
                statusName,
                grantedAt,
              } = resolveEffectiveConsent(
                existingConsents,
                typeConfig.code,
                typeConfig.id,
              );

              return (
                <div
                  key={typeConfig.code}
                  className="bg-white rounded-lg border border-gov-neutral-200 p-5 shadow-sm flex flex-col md:flex-row md:items-center justify-between gap-4"
                >
                  <div className="space-y-1 max-w-2xl">
                    <div className="flex items-center gap-2">
                      <h4 className="text-sm font-bold text-gov-neutral-900">
                        {typeConfig.name}
                      </h4>
                      {typeConfig.isRequired && (
                        <span className="text-[10px] font-bold uppercase tracking-wider px-2 py-0.5 rounded bg-gov-neutral-100 text-gov-neutral-700 border border-gov-neutral-300">
                          Mandatory
                        </span>
                      )}
                      <span
                        className={`text-xs font-semibold px-2 py-0.5 rounded ${
                          isCurrentlyGranted
                            ? 'bg-emerald-50 text-emerald-700 border border-emerald-200'
                            : 'bg-gov-neutral-100 text-gov-neutral-600 border border-gov-neutral-300'
                        }`}
                      >
                        {isCurrentlyGranted ? '● Granted' : `○ ${statusName}`}
                      </span>
                    </div>
                    <p className="text-xs text-gov-neutral-600 leading-normal">
                      {typeConfig.description}
                    </p>
                    {authoritativeConsent && isCurrentlyGranted && (
                      <p className="text-[11px] text-gov-neutral-400 font-mono">
                        Active consent recorded on: {grantedAt ? new Date(grantedAt).toLocaleString() : 'System Default'}
                      </p>
                    )}
                  </div>

                  <div className="flex items-center gap-3 shrink-0">
                    {isCurrentlyGranted ? (
                      <button
                        type="button"
                        disabled={isMutating}
                        onClick={() => handleRevoke(authoritativeConsent!)}
                        className="px-4 py-2 text-xs font-bold rounded border border-red-300 bg-white text-red-700 hover:bg-red-50 focus:outline-none focus:ring-2 focus:ring-red-500 focus:ring-offset-2 disabled:opacity-50 transition-colors"
                      >
                        {isMutating ? 'Updating...' : 'Revoke Consent'}
                      </button>
                    ) : (
                      <button
                        type="button"
                        disabled={isMutating}
                        onClick={() => handleGrant(typeConfig)}
                        className="px-4 py-2 text-xs font-bold rounded bg-gov-navy text-white hover:bg-gov-navy-light focus:outline-none focus:ring-2 focus:ring-gov-navy focus:ring-offset-2 disabled:opacity-50 transition-colors"
                      >
                        {isMutating ? 'Recording...' : 'Grant Consent'}
                      </button>
                    )}
                  </div>
                </div>
              );
            })}
          </div>
        )}
      </section>

      {/* Statutory Consent Register */}
      <section
        aria-labelledby="consent-audit-heading"
        className="bg-white rounded-lg shadow-sm border border-gov-neutral-200 overflow-hidden"
      >
        <div className="p-6 border-b border-gov-neutral-200">
          <h3
            id="consent-audit-heading"
            className="text-base font-bold text-gov-neutral-900"
          >
            Statutory Consent Register
          </h3>
          <p className="text-xs text-gov-neutral-500">
            Registered consent preferences and current status captured under Section 6 of the DPDP Act.
          </p>
        </div>

        {consentsQuery.isLoading ? (
          <div className="p-8 flex justify-center">
            <LoadingSpinner size="md" />
          </div>
        ) : consentsQuery.isError ? (
          <div className="p-8 text-center text-xs text-gov-neutral-600">
            <p className="font-semibold text-gov-neutral-800">
              Consent records are currently unavailable.
            </p>
            <p className="mt-1 text-gov-neutral-500">
              Register records could not be synchronized with the Central Privacy Register.
            </p>
          </div>
        ) : existingConsents.length === 0 ? (
          <div className="p-8">
            <EmptyState
              title="No Consent Records Found"
              description="No historical consent records exist in the database for this candidate."
            />
          </div>
        ) : (
          <div className="overflow-x-auto">
            <table className="w-full text-left border-collapse text-xs">
              <thead className="bg-gov-neutral-50 border-b border-gov-neutral-200 text-gov-neutral-700 uppercase tracking-wider">
                <tr>
                  <th scope="col" className="px-6 py-3 font-semibold">
                    Consent Type
                  </th>
                  <th scope="col" className="px-6 py-3 font-semibold">
                    Status
                  </th>
                  <th scope="col" className="px-6 py-3 font-semibold">
                    Purpose
                  </th>
                  <th scope="col" className="px-6 py-3 font-semibold">
                    Granted At
                  </th>
                  <th scope="col" className="px-6 py-3 font-semibold">
                    Revoked At
                  </th>
                  <th scope="col" className="px-6 py-3 font-semibold">
                    Channel / Version
                  </th>
                </tr>
              </thead>
              <tbody className="divide-y divide-gov-neutral-200 font-sans">
                {existingConsents.map((item) => (
                  <tr key={item.id} className="hover:bg-gov-neutral-50 transition-colors">
                    <td className="px-6 py-3.5 font-bold text-gov-neutral-900 whitespace-nowrap">
                      {item.consentTypeName || item.consentTypeCode}
                    </td>
                    <td className="px-6 py-3.5 whitespace-nowrap">
                      <span
                        className={`inline-flex items-center px-2 py-0.5 rounded text-xs font-semibold ${
                          item.consentStatusCode === 'GRANTED'
                            ? 'bg-emerald-50 text-emerald-700'
                            : 'bg-red-50 text-red-700'
                        }`}
                      >
                        {item.consentStatusCode}
                      </span>
                    </td>
                    <td className="px-6 py-3.5 text-gov-neutral-600 max-w-xs truncate">
                      {item.purpose}
                    </td>
                    <td className="px-6 py-3.5 text-gov-neutral-700 whitespace-nowrap">
                      {item.grantedAt ? new Date(item.grantedAt).toLocaleString() : '—'}
                    </td>
                    <td className="px-6 py-3.5 text-gov-neutral-500 whitespace-nowrap">
                      {item.revokedAt ? new Date(item.revokedAt).toLocaleString() : '—'}
                    </td>
                    <td className="px-6 py-3.5 text-gov-neutral-500 whitespace-nowrap font-mono">
                      {item.capturedChannel || 'WEB'} ({item.policyVersion || 'v1.0'})
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </section>
    </div>
  );
};

export default TraineeConsentPage;
