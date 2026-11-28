import { apiGet } from './client';
import type { components } from './generated/api.types';

type Schema = components['schemas'];
const dashboards = '/api/v1/dashboards';

export type MisFilters = {
  fiscalYear?: string;
  schemeId?: number;
  sectorId?: number;
  stateId?: number;
  districtId?: number;
  gender?: string;
};

export type Measure = {
  value: number | null;
  availability: 'RELEASED' | 'SUPPRESSED' | 'NO_DATA' | 'NOT_APPLICABLE';
};
export type ExecutiveKpi = { key: string; current: Measure; changePct?: number | null };
export type ExecutiveHeadline = { kpis: ExecutiveKpi[]; previousFinancialYear?: string | null };
export type PipelineStage = { key: string; candidates: Measure };
export type ProviderQuality = { providerId: number; scorePct: Measure };
export type SchemeOption = { id: number; name: string; administration: 'CENTRAL' | 'STATE' | 'OTHER' };
type Option = { id: number; name: string };
export interface MisOptions {
  financialYears: string[];
  schemes: SchemeOption[];
  sectors: Option[];
  states: (Option & { code: string })[];
  districts: (Option & { stateId: number })[];
  genders: { code: string; name: string }[];
  socialCategorySupported: boolean;
  suppressionThreshold: number;
}

export type SchemeProgram = Schema['SchemeProgramPerformanceResponse'];
export type Provider = Schema['ProviderPerformanceResponse'];
export type Course = Schema['CourseEmployabilityResponse'];
export type Employer = Schema['EmployerHiringPerformanceResponse'];
export type StateSummary = Schema['StatePerformanceResponse'];
export type DistrictSummary = Schema['DistrictPerformanceResponse'];
export type Geography = Schema['GeographicOverviewResponse'];
export type Economic = Schema['EconomicOutcomeSummaryResponse'];
export type Category = Schema['EmploymentCategoryBreakdownResponse'];
export type CourseWages = Schema['CourseWageProgressionResponse'];

export const getMisOptions = (signal?: AbortSignal) => apiGet<MisOptions>(`${dashboards}/mis/filters`, { signal });

export async function getExecutiveHeadline(filters: MisFilters, signal?: AbortSignal): Promise<ExecutiveHeadline> {
  const result = await apiGet<{ executiveKpis: ExecutiveHeadline }>(`${dashboards}/kpis`, {
    params: { ...filters, executive: true }, signal,
  });
  return result.executiveKpis;
}

export function getStateOutcomes(stateId: number, filters: MisFilters, signal?: AbortSignal) {
  return apiGet<Schema['StateGeographicResponse']>(`${dashboards}/states/${stateId}`, {
    params: { ...filters, stateId }, signal,
  });
}

export async function getMisOverview(filters: MisFilters, signal: AbortSignal) {
  const [headline, pipeline, geography] = await Promise.all([
    getExecutiveHeadline(filters, signal),
    apiGet<{ candidateStages: PipelineStage[] }>(`${dashboards}/pipeline-funnel`, {
      params: { ...filters, executive: true }, signal,
    }),
    apiGet<Geography>(`${dashboards}/geography`, { params: filters, signal }),
  ]);
  return { headline, pipeline: pipeline.candidateStages, geography };
}

export async function getMisComparisons(filters: MisFilters, signal: AbortSignal) {
  const get = <T,>(path: string) => apiGet<T>(`${dashboards}/${path}`, { params: filters, signal });
  const [programs, providers, courses, employers, economic, quality] = await Promise.all([
    get<{ items: SchemeProgram[] }>('scheme-matrix'),
    get<{ items: Provider[] }>('providers'),
    get<{ items: Course[] }>('courses'),
    get<{ items: Employer[] }>('employers'),
    get<Economic>('economic-outcomes'),
    get<ProviderQuality[]>('mis/provider-quality'),
  ]);
  return {
    programs: programs.items,
    providers: providers.items,
    courses: courses.items,
    employers: employers.items,
    economic,
    quality,
  };
}

export type MisOverview = Awaited<ReturnType<typeof getMisOverview>>;
export type MisComparisons = Awaited<ReturnType<typeof getMisComparisons>>;
