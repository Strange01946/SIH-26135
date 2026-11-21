import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, waitFor, fireEvent } from '@testing-library/react';
import { MemoryRouter, Outlet, Route, Routes } from 'react-router-dom';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import {
  TraineeConsentPage,
  resolveEffectiveConsent,
  getConsentErrorMessage,
} from '@/pages/trainee/TraineeConsentPage';
import * as traineeApi from '@/api/traineeApi';
import type { TraineeConsent } from '@/types/trainee';

vi.mock('@/api/traineeApi');

function renderConsentWithContext(contextValue: any) {
  const queryClient = new QueryClient({
    defaultOptions: { queries: { retry: false }, mutations: { retry: false } },
  });

  const LayoutMock = () => <Outlet context={contextValue} />;

  return render(
    <QueryClientProvider client={queryClient}>
      <MemoryRouter initialEntries={['/trainee/consent']}>
        <Routes>
          <Route element={<LayoutMock />}>
            <Route path="/trainee/consent" element={<TraineeConsentPage />} />
          </Route>
        </Routes>
      </MemoryRouter>
    </QueryClientProvider>,
  );
}

describe('TraineeConsentPage Component', () => {
  const mockConsents: TraineeConsent[] = [
    {
      id: 301,
      traineeId: 42,
      consentTypeId: 1,
      consentTypeCode: 'DATA_PROCESSING',
      consentTypeName: 'Data processing',
      consentStatusId: 1,
      consentStatusCode: 'GRANTED',
      consentStatusName: 'Granted',
      purpose: 'Process registration and training records',
      policyVersion: 'v1.0',
      grantedAt: '2026-09-01T08:00:00Z',
      capturedChannel: 'WEB_PORTAL',
    },
    {
      id: 302,
      traineeId: 42,
      consentTypeId: 5,
      consentTypeCode: 'COMMUNICATION_SMS',
      consentTypeName: 'SMS communication',
      consentStatusId: 2,
      consentStatusCode: 'REVOKED',
      consentStatusName: 'Revoked',
      purpose: 'Send operational SMS messages',
      policyVersion: 'v1.0',
      grantedAt: '2026-09-01T08:00:00Z',
      revokedAt: '2026-10-01T10:00:00Z',
      capturedChannel: 'WEB_PORTAL',
    },
  ];

  beforeEach(() => {
    vi.restoreAllMocks();
  });

  it('renders statutory DPDP notice, compliance metadata, and audit log', async () => {
    vi.spyOn(traineeApi, 'getTraineeConsents').mockResolvedValue(mockConsents);

    renderConsentWithContext({ traineeId: 42, isUnlinked: false, isLoading: false });

    await waitFor(() => {
      expect(screen.getByText(/Digital Personal Data Protection Act, 2023 § 6/i)).toBeInTheDocument();
      expect(screen.getByText('Process registration and training records')).toBeInTheDocument();
    });

    expect(screen.getByText(/Data Fiduciary:/i)).toBeInTheDocument();
    expect(screen.getByText(/Skill India Training & Placement Platform/i)).toBeInTheDocument();
    expect(screen.getByText(/privacy@skillindia.gov.in/i)).toBeInTheDocument();
    expect(screen.getByText(/Standard DPDP Consent Form/i)).toBeInTheDocument();

    // Audit log table
    expect(screen.getByText('Statutory Consent Register')).toBeInTheDocument();
    expect(screen.getByText('Send operational SMS messages')).toBeInTheDocument();
  });

  it('renders consent categories showing correct granted/not granted state', async () => {
    vi.spyOn(traineeApi, 'getTraineeConsents').mockResolvedValueOnce(mockConsents);

    renderConsentWithContext({ traineeId: 42, isUnlinked: false, isLoading: false });

    await waitFor(() => {
      expect(screen.getByText('Core Scheme Data Processing')).toBeInTheDocument();
    });

    // DATA_PROCESSING is granted -> Revoke Consent button visible
    expect(screen.getByRole('button', { name: /Revoke Consent/i })).toBeInTheDocument();

    // COMMUNICATION_SMS was revoked -> Grant Consent button visible
    const grantButtons = screen.getAllByRole('button', { name: /Grant Consent/i });
    expect(grantButtons.length).toBeGreaterThan(0);
  });

  it('revoking a consent calls updateTraineeConsentStatus with statusId 2', async () => {
    vi.spyOn(traineeApi, 'getTraineeConsents').mockResolvedValueOnce(mockConsents);
    const updateSpy = vi.spyOn(traineeApi, 'updateTraineeConsentStatus').mockResolvedValueOnce({
      ...mockConsents[0],
      consentStatusId: 2,
      consentStatusCode: 'REVOKED',
    });

    renderConsentWithContext({ traineeId: 42, isUnlinked: false, isLoading: false });

    await waitFor(() => {
      expect(screen.getByRole('button', { name: /Revoke Consent/i })).toBeInTheDocument();
    });

    const revokeButton = screen.getByRole('button', { name: /Revoke Consent/i });
    fireEvent.click(revokeButton);

    await waitFor(() => {
      expect(updateSpy).toHaveBeenCalledWith(
        301,
        expect.objectContaining({
          consentStatusId: 2,
        }),
      );
    });
  });

  it('granting a revoked consent creates a new clean record via createTraineeConsent avoiding stale revocation timestamps', async () => {
    vi.spyOn(traineeApi, 'getTraineeConsents').mockResolvedValueOnce(mockConsents);
    const createSpy = vi.spyOn(traineeApi, 'createTraineeConsent').mockResolvedValueOnce({
      id: 303,
      traineeId: 42,
      consentTypeId: 5,
      consentTypeCode: 'COMMUNICATION_SMS',
      consentTypeName: 'SMS communication',
      consentStatusId: 1,
      consentStatusCode: 'GRANTED',
      consentStatusName: 'Granted',
      purpose: 'Send operational SMS messages',
      policyVersion: 'v1.0',
    });

    renderConsentWithContext({ traineeId: 42, isUnlinked: false, isLoading: false });

    await waitFor(() => {
      expect(screen.getByText('Transactional SMS Communication')).toBeInTheDocument();
    });

    // Find the Grant button next to SMS communication
    const smsCard = screen.getByText('Transactional SMS Communication').closest('div[class*="rounded-lg"]')!;
    const grantButton = smsCard.querySelector('button')!;
    expect(grantButton).toHaveTextContent('Grant Consent');

    fireEvent.click(grantButton);

    await waitFor(() => {
      expect(createSpy).toHaveBeenCalledWith(
        expect.objectContaining({
          traineeId: 42,
          consentTypeId: 5,
          consentStatusId: 1,
        }),
      );
    });
  });

  it('granting a brand new consent category calls createTraineeConsent', async () => {
    vi.spyOn(traineeApi, 'getTraineeConsents').mockResolvedValue(mockConsents);
    const createSpy = vi.spyOn(traineeApi, 'createTraineeConsent').mockResolvedValueOnce({
      id: 305,
      traineeId: 42,
      consentTypeId: 7,
      consentTypeCode: 'COMMUNICATION_EMAIL',
      consentTypeName: 'Email communication',
      consentStatusId: 1,
      consentStatusCode: 'GRANTED',
      consentStatusName: 'Granted',
      purpose: 'Send operational email messages',
      policyVersion: 'v1.0',
    });

    renderConsentWithContext({ traineeId: 42, isUnlinked: false, isLoading: false });

    await waitFor(() => {
      expect(screen.getByText('Official Email Communications')).toBeInTheDocument();
    });

    const emailCard = screen.getByText('Official Email Communications').closest('div[class*="rounded-lg"]')!;
    const grantButton = emailCard.querySelector('button')!;
    fireEvent.click(grantButton);

    await waitFor(() => {
      expect(createSpy).toHaveBeenCalledWith(
        expect.objectContaining({
          traineeId: 42,
          consentTypeId: 7,
          consentStatusId: 1,
        }),
      );
    });
  });

  it('renders error notification when consent mutation fails', async () => {
    vi.spyOn(traineeApi, 'getTraineeConsents').mockResolvedValueOnce(mockConsents);
    vi.spyOn(traineeApi, 'updateTraineeConsentStatus').mockRejectedValueOnce(
      new Error('Consent modification not permitted'),
    );

    renderConsentWithContext({ traineeId: 42, isUnlinked: false, isLoading: false });

    await waitFor(() => {
      expect(screen.getByRole('button', { name: /Revoke Consent/i })).toBeInTheDocument();
    });

    const revokeButton = screen.getByRole('button', { name: /Revoke Consent/i });
    fireEvent.click(revokeButton);

    await waitFor(() => {
      expect(screen.getByRole('alert')).toBeInTheDocument();
    });
    expect(screen.getByText(/Consent modification not permitted/i)).toBeInTheDocument();
  });

  describe('Consent API Failure Handling & Resilience', () => {
    it('handles HTTP 403 Forbidden: renders authorization alert, never displays "Not Granted" or "No Consent Records Found", and offers retry', async () => {
      vi.spyOn(traineeApi, 'getTraineeConsents').mockRejectedValueOnce({
        status: 403,
        message: 'Access Denied',
      });

      renderConsentWithContext({ traineeId: 42, isUnlinked: false, isLoading: false });

      await waitFor(() => {
        expect(screen.getByRole('alert')).toBeInTheDocument();
      });

      expect(
        screen.getByText(/Access restricted. You do not have authorization to view or update DPDP statutory consent records/i),
      ).toBeInTheDocument();

      // Must NEVER display "Not Granted" when query failed
      expect(screen.queryByText(/○ Not Granted/i)).not.toBeInTheDocument();

      // Must NEVER display "No Consent Records Found" when query failed
      expect(screen.queryByText(/No Consent Records Found/i)).not.toBeInTheDocument();

      // Grant/Revoke action buttons must NOT be accessible while state is unknown
      expect(screen.queryByRole('button', { name: /Grant Consent/i })).not.toBeInTheDocument();
      expect(screen.queryByRole('button', { name: /Revoke Consent/i })).not.toBeInTheDocument();

      // Accessible Retry action must be provided
      expect(screen.getByRole('button', { name: /Retry Loading Consents/i })).toBeInTheDocument();
    });

    it('handles HTTP 500 Internal Server Error without leaking internal technical details', async () => {
      vi.spyOn(traineeApi, 'getTraineeConsents').mockRejectedValueOnce({
        status: 500,
        message: 'java.sql.SQLSyntaxErrorException: Table sih26135.trainee_consent does not exist',
      });

      renderConsentWithContext({ traineeId: 42, isUnlinked: false, isLoading: false });

      await waitFor(() => {
        expect(screen.getByRole('alert')).toBeInTheDocument();
      });

      // User-friendly safe message
      expect(
        screen.getByText(/The Central Privacy Register is temporarily unavailable. Please retry shortly/i),
      ).toBeInTheDocument();

      // Never leak backend internal technical details
      expect(screen.queryByText(/SQLSyntaxErrorException/i)).not.toBeInTheDocument();
      expect(screen.queryByText(/sih26135\.trainee_consent/i)).not.toBeInTheDocument();

      expect(screen.getByRole('button', { name: /Retry Loading Consents/i })).toBeInTheDocument();
    });

    it('handles network failure with clear communication advice', async () => {
      vi.spyOn(traineeApi, 'getTraineeConsents').mockRejectedValueOnce({
        code: 'ERR_NETWORK',
        message: 'Network Error: connection refused',
      });

      renderConsentWithContext({ traineeId: 42, isUnlinked: false, isLoading: false });

      await waitFor(() => {
        expect(screen.getByRole('alert')).toBeInTheDocument();
      });

      expect(
        screen.getByText(/Network communication failure. Please verify your network connection and retry/i),
      ).toBeInTheDocument();

      expect(screen.queryByText(/○ Not Granted/i)).not.toBeInTheDocument();
      expect(screen.getByRole('button', { name: /Retry Loading Consents/i })).toBeInTheDocument();
    });

    it('handles successful empty response: renders authoritative "Not Granted" with enabled "Grant Consent" and empty register', async () => {
      vi.spyOn(traineeApi, 'getTraineeConsents').mockResolvedValueOnce([]);

      renderConsentWithContext({ traineeId: 42, isUnlinked: false, isLoading: false });

      await waitFor(() => {
        expect(screen.getByText('Core Scheme Data Processing')).toBeInTheDocument();
      });

      // On successful empty, no error alert is rendered
      expect(screen.queryByRole('alert')).not.toBeInTheDocument();

      // Categories correctly show "Not Granted" because backend authoritatively returned 0 records
      const notGrantedBadges = screen.getAllByText(/○ Not Granted/i);
      expect(notGrantedBadges.length).toBeGreaterThan(0);

      // Grant buttons are enabled and available
      const grantButtons = screen.getAllByRole('button', { name: /Grant Consent/i });
      expect(grantButtons.length).toBeGreaterThan(0);

      // Empty register notice is shown because retrieval was successful
      expect(screen.getByText('No Consent Records Found')).toBeInTheDocument();
    });

    it('clicking Retry Loading Consents invokes refetch and loads data', async () => {
      const getSpy = vi
        .spyOn(traineeApi, 'getTraineeConsents')
        .mockRejectedValueOnce({ status: 503, message: 'Service Unavailable' })
        .mockResolvedValueOnce(mockConsents);

      renderConsentWithContext({ traineeId: 42, isUnlinked: false, isLoading: false });

      await waitFor(() => {
        expect(screen.getByRole('button', { name: /Retry Loading Consents/i })).toBeInTheDocument();
      });

      fireEvent.click(screen.getByRole('button', { name: /Retry Loading Consents/i }));

      await waitFor(() => {
        expect(screen.getByText('Core Scheme Data Processing')).toBeInTheDocument();
        expect(screen.getByRole('button', { name: /Revoke Consent/i })).toBeInTheDocument();
      });

      expect(getSpy).toHaveBeenCalledTimes(2);
    });
  });

  describe('getConsentErrorMessage() unit tests', () => {
    it('returns authorization message on 401 and 403', () => {
      expect(getConsentErrorMessage({ status: 403 })).toContain('Access restricted');
      expect(getConsentErrorMessage({ response: { status: 401 } })).toContain('Access restricted');
    });

    it('returns not found message on 404', () => {
      expect(getConsentErrorMessage({ status: 404 })).toContain('profile not found');
    });

    it('returns temporary unavailable message on 5xx', () => {
      expect(getConsentErrorMessage({ status: 500 })).toContain('temporarily unavailable');
      expect(getConsentErrorMessage({ status: 503 })).toContain('temporarily unavailable');
    });

    it('returns network message on network failures', () => {
      expect(getConsentErrorMessage({ code: 'ERR_NETWORK' })).toContain('Network communication failure');
      expect(getConsentErrorMessage({ message: 'Failed to fetch' })).toContain('Network communication failure');
    });

    it('returns fallback message when error is undefined or unknown', () => {
      expect(getConsentErrorMessage(null)).toContain('Unable to retrieve consent records');
      expect(getConsentErrorMessage({})).toContain('Unable to synchronize consent records');
    });
  });

  describe('resolveEffectiveConsent() authoritative resolution tests', () => {
    const fixedNow = new Date('2026-11-20T12:00:00Z');

    it('resolves to granted when a single valid active grant exists', () => {
      const consents = [
        {
          id: 10,
          traineeId: 1,
          consentTypeId: 1,
          consentTypeCode: 'DATA_PROCESSING',
          consentStatusId: 1,
          consentStatusCode: 'GRANTED',
          grantedAt: '2026-01-01T00:00:00Z',
          createdAt: '2026-01-01T00:00:00Z',
        },
      ] as TraineeConsent[];

      const result = resolveEffectiveConsent(consents, 'DATA_PROCESSING', 1, fixedNow);
      expect(result.isCurrentlyGranted).toBe(true);
      expect(result.statusName).toBe('Granted');
      expect(result.authoritativeConsent?.id).toBe(10);
    });

    it('resolves to revoked when grant was later revoked (even if unordered)', () => {
      // Historical grant comes second in array to simulate random order
      const consents = [
        {
          id: 11,
          traineeId: 1,
          consentTypeId: 1,
          consentTypeCode: 'DATA_PROCESSING',
          consentStatusId: 2,
          consentStatusCode: 'REVOKED',
          grantedAt: '2026-01-01T00:00:00Z',
          revokedAt: '2026-06-01T00:00:00Z',
          updatedAt: '2026-06-01T00:00:00Z',
        },
        {
          id: 10,
          traineeId: 1,
          consentTypeId: 1,
          consentTypeCode: 'DATA_PROCESSING',
          consentStatusId: 1,
          consentStatusCode: 'GRANTED',
          grantedAt: '2026-01-01T00:00:00Z',
          updatedAt: '2026-01-01T00:00:00Z',
        },
      ] as TraineeConsent[];

      const result = resolveEffectiveConsent(consents, 'DATA_PROCESSING', 1, fixedNow);
      expect(result.isCurrentlyGranted).toBe(false);
      expect(result.authoritativeConsent?.id).toBe(11);
      expect(result.statusName).toBe('REVOKED');
    });

    it('resolves to granted when consent is re-granted after prior revocation', () => {
      const consents = [
        {
          id: 10,
          traineeId: 1,
          consentTypeId: 2,
          consentTypeCode: 'EMPLOYMENT_FOLLOW_UP',
          consentStatusId: 2,
          consentStatusCode: 'REVOKED',
          updatedAt: '2026-05-01T00:00:00Z',
        },
        {
          id: 12,
          traineeId: 1,
          consentTypeId: 2,
          consentTypeCode: 'EMPLOYMENT_FOLLOW_UP',
          consentStatusId: 1,
          consentStatusCode: 'GRANTED',
          grantedAt: '2026-09-01T00:00:00Z',
          updatedAt: '2026-09-01T00:00:00Z',
        },
      ] as TraineeConsent[];

      const result = resolveEffectiveConsent(consents, 'EMPLOYMENT_FOLLOW_UP', 2, fixedNow);
      expect(result.isCurrentlyGranted).toBe(true);
      expect(result.statusName).toBe('Granted');
      expect(result.authoritativeConsent?.id).toBe(12);
    });

    it('resolves to not granted when consent has an expired date in past', () => {
      const consents = [
        {
          id: 15,
          traineeId: 1,
          consentTypeId: 5,
          consentTypeCode: 'COMMUNICATION_SMS',
          consentStatusId: 1,
          consentStatusCode: 'GRANTED',
          grantedAt: '2026-01-01T00:00:00Z',
          expiresAt: '2026-06-01T00:00:00Z', // Expired before fixedNow
          updatedAt: '2026-01-01T00:00:00Z',
        },
      ] as TraineeConsent[];

      const result = resolveEffectiveConsent(consents, 'COMMUNICATION_SMS', 5, fixedNow);
      expect(result.isCurrentlyGranted).toBe(false);
    });

    it('resolves to not granted when grantedAt is in future', () => {
      const consents = [
        {
          id: 16,
          traineeId: 1,
          consentTypeId: 6,
          consentTypeCode: 'COMMUNICATION_WHATSAPP',
          consentStatusId: 1,
          consentStatusCode: 'GRANTED',
          grantedAt: '2026-12-01T00:00:00Z', // Future grant
          updatedAt: '2026-11-01T00:00:00Z',
        },
      ] as TraineeConsent[];

      const result = resolveEffectiveConsent(consents, 'COMMUNICATION_WHATSAPP', 6, fixedNow);
      expect(result.isCurrentlyGranted).toBe(false);
    });

    it('breaks ties using higher ID when timestamps are equal', () => {
      const consents = [
        {
          id: 20,
          traineeId: 1,
          consentTypeId: 7,
          consentTypeCode: 'COMMUNICATION_EMAIL',
          consentStatusId: 1,
          consentStatusCode: 'GRANTED',
          updatedAt: '2026-08-01T00:00:00Z',
        },
        {
          id: 25,
          traineeId: 1,
          consentTypeId: 7,
          consentTypeCode: 'COMMUNICATION_EMAIL',
          consentStatusId: 2,
          consentStatusCode: 'REVOKED',
          updatedAt: '2026-08-01T00:00:00Z',
        },
      ] as TraineeConsent[];

      const result = resolveEffectiveConsent(consents, 'COMMUNICATION_EMAIL', 7, fixedNow);
      expect(result.authoritativeConsent?.id).toBe(25);
      expect(result.isCurrentlyGranted).toBe(false);
    });

    it('resolves to not granted when no records match type', () => {
      const result = resolveEffectiveConsent([], 'DATA_PROCESSING', 1, fixedNow);
      expect(result.isCurrentlyGranted).toBe(false);
      expect(result.statusName).toBe('Not Granted');
      expect(result.authoritativeConsent).toBeUndefined();
    });
  });

  describe('Consent Full Lifecycle: Grant → Revoke → Re-Grant & Backend Semantics Integration', () => {
    const fixedNow = new Date('2026-11-20T12:00:00Z');

    it('demonstrates why re-granting via record update fails due to backend ignoring null revokedAt', () => {
      // Backend TraineeConsentServiceImpl.updateConsentStatus() only updates revokedAt when non-null:
      // if (request.getRevokedAt() != null) { consent.setRevokedAt(request.getRevokedAt()); }
      // If an update is performed setting status = GRANTED while revokedAt remains the past revocation date:
      const flawedUpdatedRecord: TraineeConsent = {
        id: 101,
        traineeId: 42,
        consentTypeId: 1,
        consentTypeCode: 'DATA_PROCESSING',
        consentTypeName: 'Data processing',
        consentStatusId: 1,
        consentStatusCode: 'GRANTED',
        consentStatusName: 'Granted',
        purpose: 'Process registration and training records',
        grantedAt: '2026-09-01T08:00:00Z',
        revokedAt: '2026-10-01T10:00:00Z', // Stale timestamp left uncleared
        updatedAt: '2026-11-01T12:00:00Z',
      };

      const result = resolveEffectiveConsent([flawedUpdatedRecord], 'DATA_PROCESSING', 1, fixedNow);
      // Because revokedAt is in the past, both backend hasActiveConsent() and frontend resolveEffectiveConsent()
      // evaluate notRevoked to false, rendering the re-grant ineffective!
      expect(result.isCurrentlyGranted).toBe(false);
      expect(result.statusName).toBe('Revoked');
    });

    it('proves that creating a fresh consent record cleanly supersedes the revoked record and returns active granted state', () => {
      // In contrast, creating a new record via POST /api/v1/trainee-consents creates a new entity
      // with revokedAt: null and a newer timestamp/higher ID.
      const multiRecordAuditTrail: TraineeConsent[] = [
        {
          id: 101,
          traineeId: 42,
          consentTypeId: 1,
          consentTypeCode: 'DATA_PROCESSING',
          consentTypeName: 'Data processing',
          consentStatusId: 2,
          consentStatusCode: 'REVOKED',
          consentStatusName: 'Revoked',
          purpose: 'Process registration and training records',
          grantedAt: '2026-09-01T08:00:00Z',
          revokedAt: '2026-10-01T10:00:00Z',
          updatedAt: '2026-10-01T10:00:00Z',
        },
        {
          id: 102,
          traineeId: 42,
          consentTypeId: 1,
          consentTypeCode: 'DATA_PROCESSING',
          consentTypeName: 'Data processing',
          consentStatusId: 1,
          consentStatusCode: 'GRANTED',
          consentStatusName: 'Granted',
          purpose: 'Process registration and training records',
          grantedAt: '2026-11-01T12:00:00Z',
          revokedAt: null, // Clean null
          updatedAt: '2026-11-01T12:00:00Z',
        },
      ];

      const result = resolveEffectiveConsent(multiRecordAuditTrail, 'DATA_PROCESSING', 1, fixedNow);
      expect(result.authoritativeConsent?.id).toBe(102);
      expect(result.isCurrentlyGranted).toBe(true);
      expect(result.statusName).toBe('Granted');
      expect(result.revokedAt).toBeNull();
    });

    it('executes full UI lifecycle flow: grant (create) -> revoke (update) -> re-grant (create clean record)', async () => {
      // 1. Initial empty state
      let currentRecords: TraineeConsent[] = [];
      const getSpy = vi.spyOn(traineeApi, 'getTraineeConsents').mockImplementation(async () => currentRecords);

      const createSpy = vi.spyOn(traineeApi, 'createTraineeConsent').mockImplementation(async (payload) => {
        const newRecord: TraineeConsent = {
          id: currentRecords.length === 0 ? 201 : 202,
          traineeId: payload.traineeId,
          consentTypeId: payload.consentTypeId,
          consentTypeCode: 'DATA_PROCESSING',
          consentTypeName: 'Data processing',
          consentStatusId: payload.consentStatusId,
          consentStatusCode: payload.consentStatusId === 1 ? 'GRANTED' : 'REVOKED',
          consentStatusName: payload.consentStatusId === 1 ? 'Granted' : 'Revoked',
          purpose: payload.purpose,
          policyVersion: payload.policyVersion || 'v1.0',
          grantedAt: payload.grantedAt || new Date().toISOString(),
          revokedAt: null,
          createdAt: new Date().toISOString(),
          updatedAt: new Date().toISOString(),
        };
        currentRecords = [newRecord, ...currentRecords];
        return newRecord;
      });

      const updateSpy = vi.spyOn(traineeApi, 'updateTraineeConsentStatus').mockImplementation(async (id, payload) => {
        currentRecords = currentRecords.map((r) => {
          if (r.id === id) {
            return {
              ...r,
              consentStatusId: payload.consentStatusId,
              consentStatusCode: payload.consentStatusId === 2 ? 'REVOKED' : 'GRANTED',
              consentStatusName: payload.consentStatusId === 2 ? 'Revoked' : 'Granted',
              revokedAt: payload.revokedAt || r.revokedAt,
              updatedAt: new Date().toISOString(),
            };
          }
          return r;
        });
        return currentRecords.find((r) => r.id === id)!;
      });

      renderConsentWithContext({ traineeId: 42, isUnlinked: false, isLoading: false });

      // Step 1: Initial grant
      await waitFor(() => {
        expect(screen.getByText('Core Scheme Data Processing')).toBeInTheDocument();
      });

      const coreCard = screen.getByText('Core Scheme Data Processing').closest('div[class*="rounded-lg"]')!;
      let button = coreCard.querySelector('button')!;
      expect(button).toHaveTextContent('Grant Consent');

      fireEvent.click(button);

      await waitFor(() => {
        expect(createSpy).toHaveBeenCalledTimes(1);
        expect(createSpy).toHaveBeenLastCalledWith(
          expect.objectContaining({
            traineeId: 42,
            consentTypeId: 1,
            consentStatusId: 1,
          }),
        );
      });

      // Step 2: Now granted -> revoke it
      await waitFor(() => {
        button = coreCard.querySelector('button')!;
        expect(button).toHaveTextContent('Revoke Consent');
      });

      fireEvent.click(button);

      await waitFor(() => {
        expect(updateSpy).toHaveBeenCalledTimes(1);
        expect(updateSpy).toHaveBeenLastCalledWith(
          201,
          expect.objectContaining({
            consentStatusId: 2,
            revokedAt: expect.any(String),
          }),
        );
      });

      // Step 3: Now revoked -> re-grant it
      await waitFor(() => {
        button = coreCard.querySelector('button')!;
        expect(button).toHaveTextContent('Grant Consent');
      });

      fireEvent.click(button);

      await waitFor(() => {
        // Must call createTraineeConsent again, NOT updateTraineeConsentStatus
        expect(createSpy).toHaveBeenCalledTimes(2);
        expect(createSpy).toHaveBeenLastCalledWith(
          expect.objectContaining({
            traineeId: 42,
            consentTypeId: 1,
            consentStatusId: 1,
          }),
        );
        // updateSpy was NOT called for re-grant
        expect(updateSpy).toHaveBeenCalledTimes(1);
      });

      // Final state: both records exist in audit register, and category shows Granted
      await waitFor(() => {
        button = coreCard.querySelector('button')!;
        expect(button).toHaveTextContent('Revoke Consent');
      });

      expect(getSpy).toHaveBeenCalled();
    });
  });
});
