import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, waitFor, fireEvent } from '@testing-library/react';
import { MemoryRouter, Outlet, Route, Routes } from 'react-router-dom';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import {
  TraineeConsentPage,
  resolveEffectiveConsent,
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
    expect(screen.getByText(/dpo@skills.gov.in/i)).toBeInTheDocument();

    // Audit log table
    expect(screen.getByText('Statutory Consent Audit Log')).toBeInTheDocument();
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

  it('granting a revoked consent calls updateTraineeConsentStatus with statusId 1', async () => {
    vi.spyOn(traineeApi, 'getTraineeConsents').mockResolvedValueOnce(mockConsents);
    const updateSpy = vi.spyOn(traineeApi, 'updateTraineeConsentStatus').mockResolvedValueOnce({
      ...mockConsents[1],
      consentStatusId: 1,
      consentStatusCode: 'GRANTED',
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
      expect(updateSpy).toHaveBeenCalledWith(
        302,
        expect.objectContaining({
          consentStatusId: 1,
          revokedAt: null,
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
});
