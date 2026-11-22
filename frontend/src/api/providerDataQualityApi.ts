import {
  apiGet,
  apiGetPaged,
  apiPost,
} from './client';
import type { PagedResponse } from './types';
import type {
  ProviderDataQualityIssue,
  DataQualityIssueEvent,
  CenterScorecard,
  IssueFilterParams,
  ResolveIssuePayload,
  DismissIssuePayload,
} from '@/types/providerDataQuality';

/**
 * Phase 30.5 Training Provider Data Quality & Center Scorecard API Client
 */

export async function getProviderDataQualityIssues(
  params?: IssueFilterParams,
): Promise<PagedResponse<ProviderDataQualityIssue>> {
  return apiGetPaged<ProviderDataQualityIssue>('/api/v1/provider/data-quality/issues', {
    params,
  });
}

export async function getDataQualityIssueById(
  id: number,
): Promise<ProviderDataQualityIssue> {
  return apiGet<ProviderDataQualityIssue>(`/api/v1/provider/data-quality/issues/${id}`);
}

export async function getDataQualityIssueEvents(
  id: number,
  page = 0,
  size = 20,
): Promise<DataQualityIssueEvent[]> {
  return apiGet<DataQualityIssueEvent[]>(`/api/v1/provider/data-quality/issues/${id}/events`, {
    params: { page, size },
  });
}

export async function startIssueInvestigation(
  id: number,
): Promise<ProviderDataQualityIssue> {
  return apiPost<Record<string, never>, ProviderDataQualityIssue>(
    `/api/v1/provider/data-quality/issues/${id}/investigate`,
    {},
  );
}

export async function resolveDataQualityIssue(
  id: number,
  payload: ResolveIssuePayload,
): Promise<ProviderDataQualityIssue> {
  return apiPost<ResolveIssuePayload, ProviderDataQualityIssue>(
    `/api/v1/provider/data-quality/issues/${id}/resolve`,
    payload,
  );
}

export async function dismissDataQualityIssue(
  id: number,
  payload: DismissIssuePayload,
): Promise<ProviderDataQualityIssue> {
  return apiPost<DismissIssuePayload, ProviderDataQualityIssue>(
    `/api/v1/provider/data-quality/issues/${id}/dismiss`,
    payload,
  );
}

export async function getCenterScorecard(
  centerId: number,
): Promise<CenterScorecard> {
  return apiGet<CenterScorecard>(`/api/v1/provider/centers/${centerId}/scorecard`);
}

export async function triggerRuleReEvaluation(
  issueId?: number,
): Promise<void> {
  return apiPost<{ issueId?: number }, void>(
    '/api/v1/provider/data-quality/re-evaluate',
    issueId ? { issueId } : {},
    { params: issueId ? { issueId } : undefined },
  );
}
