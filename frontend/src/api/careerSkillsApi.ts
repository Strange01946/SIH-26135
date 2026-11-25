import { apiClient, apiGet, apiPost, apiPut } from './client';

export type CareerOption = { id: number; name: string };
export type CareerSkillsReference = { roles: CareerOption[]; skills: CareerOption[]; levels: CareerOption[] };
export type CareerSkill = { name: string; skillId?: number; levelId?: number; level?: string; rank?: number };
export type CareerEvidence = { skillId: number; skill: string; level: string; rank: number; observedAt: string; source: string };
export type CareerRequirement = { skillId: number; skill: string; requiredLevel: string; requiredRank: number;
  currentLevel?: string; currentRank?: number; source: string; needsTraining: boolean };
export type CareerSkillsProfile = { revision: number; desiredRoleId?: number; desiredJobTitle?: string; skills: CareerSkill[];
  evidence: CareerEvidence[]; requirements: CareerRequirement[] };
export type SaveCareerSkills = { revision: number; desiredRoleId?: number; desiredJobTitle?: string;
  skills: { name: string; levelId?: number }[] };
export type TrainingChoice = 'INTERESTED' | 'DISMISSED' | 'UNDECIDED';
export type CareerTraining = { batchId: number; code: string; topic: string; skillsToTeach: string; provider: string;
  venue: string; district: string; state: string; startsOn: string; endsOn?: string; firstSession?: string;
  status: 'PLANNED' | 'OPEN'; seatsAvailable: number; choice: TrainingChoice; matchedSkills: string[]; enrolled: boolean;
  relatedToJobGoal: boolean };

const root = '/api/v1/career-skills';
export const careerSkillsKey = ['career-skills'] as const;
export const getCareerSkillsReference = (signal?: AbortSignal) => apiGet<CareerSkillsReference>(`${root}/reference`, { signal });
export const getOwnCareerSkills = (signal?: AbortSignal) => apiGet<CareerSkillsProfile>(`${root}/me`, { signal });
export const saveOwnCareerSkills = (data: SaveCareerSkills) => apiPut<SaveCareerSkills, CareerSkillsProfile>(`${root}/me`, data);
export const getCareerTraining = (search: string, signal?: AbortSignal) =>
  apiGet<CareerTraining[]>(`${root}/me/training`, { params: { search: search || undefined }, signal });
export const saveCareerTrainingChoice = async (batchId: number, choice: TrainingChoice) => {
  await apiClient.put(`${root}/me/training/${batchId}/interest`, { choice });
};
export const getStaffCareerSkills = (traineeId: number, signal?: AbortSignal) =>
  apiGet<CareerSkillsProfile>(`${root}/trainees/${traineeId}`, { signal });
const assessmentProfile = (assessmentId: number, traineeId: number) => `${root}/assessments/${assessmentId}/trainees/${traineeId}`;
export const getAssessmentCareerSkills = (assessmentId: number, traineeId: number, signal?: AbortSignal) =>
  apiGet<CareerSkillsProfile>(assessmentProfile(assessmentId, traineeId), { signal });
export const recordAssessmentSkill = (assessmentId: number, traineeId: number, data: { skillId: number; levelId: number }) =>
  apiPut<typeof data, CareerSkillsProfile>(assessmentProfile(assessmentId, traineeId), data);
export const saveCareerRoleRequirement = async (roleId: number, data: { skillId: number; levelId: number }) => {
  await apiClient.put(`${root}/roles/${roleId}/requirements`, data);
};
export const getProviderTrainingInterest = (batchId: number, signal?: AbortSignal) =>
  apiGet<{ interested: number }>(`${root}/provider/batches/${batchId}/interest`, { signal });
export type CareerCatalogKind = 'CATEGORY' | 'SKILL' | 'ROLE';
export const getCareerCatalogOptions = (signal?: AbortSignal) =>
  apiGet<{ categories: CareerOption[]; sectors: CareerOption[] }>(`${root}/catalog`, { signal });
export const createCareerCatalogEntry = (kind: CareerCatalogKind, data: { name: string; parentId?: number }) =>
  apiPost<typeof data, CareerOption>(`${root}/catalog/${kind}`, data);
