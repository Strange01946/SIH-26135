import { apiGet } from './client';
import type { components } from './generated/api.types';
import type { MisFilters } from './misApi';
import { apiBaseUrl } from '@/lib/apiEnvironment';

export type Schema = components['schemas'];
const dashboards = '/api/v1/dashboards';
const specialized = `${dashboards}/specialized`;
const operations = '/api/v1/operational';
const exportsApi = '/api/v1/analytics/export';
export type Bucket = { label: string; count: number | null };
export type WageDistributions = {
  currentWages: Bucket[]; beforeTraining: Bucket[]; afterTraining: Bucket[]; pairedChange: Bucket[];
};
export type ExitArea = { key: string; name: string; exits: number | null };
export type ExitAreas = { sectors: ExitArea[]; districts: ExitArea[] };
export type DropoutSignal = {
  key: string; district: string; provider: string;
  attendanceDips: number | null; assessmentLags: number | null; candidatesWithSignal: number | null;
};
export type IncidentDay = { day: string; errors: number; critical: number };
export type ExportColumn = { key: string; label: string };
export type Report = 'candidates' | 'dsdp' | 'qpr' | 'provider-scorecard';
export type ExportFormat = 'csv' | 'json';

export async function getSpecializedOutcomes(signal?: AbortSignal) {
  const [retention, skills, attrition] = await Promise.all([
    apiGet<Schema['LongitudinalRetentionResponse']>(`${dashboards}/retention`, { signal }),
    apiGet<Schema['SkillGapDashboardResponse']>(`${dashboards}/skill-gaps`, { signal }),
    apiGet<Schema['AttritionDashboardResponse']>(`${dashboards}/attrition`, { signal }),
  ]);
  return { retention, skills, attrition };
}

export async function getSpecializedOutreach(signal?: AbortSignal) {
  const [followups, surveys, verifications] = await Promise.all([
    apiGet<Schema['FollowupDashboardResponse']>(`${dashboards}/followups`, { signal }),
    apiGet<Schema['SurveyDashboardResponse']>(`${dashboards}/surveys`, { signal }),
    apiGet<Schema['VerificationDashboardResponse']>(`${dashboards}/verifications`, { signal }),
  ]);
  return { followups, surveys, verifications };
}

export async function getSpecializedCohort(filters: MisFilters, signal?: AbortSignal) {
  const [wages, exits, signals] = await Promise.all([
    apiGet<WageDistributions>(`${specialized}/wage-distributions`, { params: filters, signal }),
    apiGet<ExitAreas>(`${specialized}/exit-areas`, { params: filters, signal }),
    apiGet<DropoutSignal[]>(`${specialized}/dropout-signals`, { params: filters, signal }),
  ]);
  return { wages, exits, signals };
}

export const getCleanlinessScore = (signal?: AbortSignal) =>
  apiGet<Schema['OperationalDataQualityScorecardResponse']>(`${operations}/data-quality-scorecard`, { signal });

export const getIncidentTrend = (days: number, signal?: AbortSignal) =>
  apiGet<IncidentDay[]>(`${specialized}/incident-trend`, { params: { days }, signal });

export const getSystemHealth = (signal?: AbortSignal) =>
  apiGet<Schema['OperationalSystemHealthResponse']>(`${operations}/system-health`, { signal });

export const getCandidateExportColumns = (signal?: AbortSignal) =>
  apiGet<ExportColumn[]>(`${exportsApi}/candidates/columns`, { signal });

export function makeExportUrl(report: Report, format: ExportFormat, filters: MisFilters, columns: string[], quarter: string) {
  const params = new URLSearchParams();
  for (const [name, value] of Object.entries(filters)) {
    if (value != null) params.set(name, String(value));
  }
  if (report === 'qpr') params.set('quarter', quarter);
  if (report === 'candidates') params.set('columns', columns.join(','));
  const base = apiBaseUrl().replace(/\/$/, '');
  return `${base}${exportsApi}/${report}/${format}?${params}`;
}
