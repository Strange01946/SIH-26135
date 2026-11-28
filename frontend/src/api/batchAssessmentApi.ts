import { apiClient, apiGet, apiPost } from './client';
import type { components } from './generated/api.types';

type PublishBatchAssessmentRequest = components['schemas']['PublishBatchAssessmentRequest'];
type ClaimBatchAssessmentRequest = components['schemas']['ClaimBatchAssessmentRequest'];

export interface BatchAssessmentAppointment {
  batchCode: string;
  title: string;
  startsAt?: string | null;
  venue?: string | null;
  completedAt?: string | null;
  brief: string;
  assessor?: string;
  status: 'OPEN' | 'CLAIMED' | 'FINALIZED';
  resultId?: number;
  outcome?: string;
  theory?: number;
  practical?: number;
  viva?: number;
  total?: number;
  certificateId?: number;
}

export interface BatchAssessmentPublication extends BatchAssessmentAppointment {
  assessmentId: number;
  batchId: number;
  provider: string;
  trainingVenue: string;
  district: string;
  state: string;
  skills?: string;
  candidates: number;
  checkedIn: boolean;
  canComplete: boolean;
}

export interface BatchAssessmentCandidate {
  traineeId: number;
  name: string;
  registrationNumber: string;
  appeared: boolean | null;
  present: number;
  trainingSessions: number;
  sessionMarks?: { startsAt: string; endsAt: string; status?: string | null; remarks?: string | null }[];
  resultId?: number;
  outcome?: string;
  theory?: number;
  practical?: number;
  viva?: number;
  total?: number;
  certificateId?: number;
}

export interface BatchAssessmentReview {
  publication: BatchAssessmentPublication;
  candidates: BatchAssessmentCandidate[];
}

interface BatchAssessmentReadiness {
  batchCode: string;
  batchStatus: string;
  candidates: number;
  sessions: number;
  outstandingMarks: number;
  canPublish: boolean;
  blocker?: string;
  publication?: BatchAssessmentPublication;
}

const batchPath = (id: number) => `/api/v1/training-batches/${id}/assessment`;
const openPath = '/api/v1/assessor/open-batch-assessments';

export const getBatchAssessmentReadiness = (id: number) => apiGet<BatchAssessmentReadiness>(batchPath(id));
export const publishBatchAssessment = (id: number, input: PublishBatchAssessmentRequest) =>
  apiPost<PublishBatchAssessmentRequest, BatchAssessmentPublication>(batchPath(id), input);
export const getProviderAssessmentReview = (id: number) => apiGet<BatchAssessmentReview>(`${batchPath(id)}/roster`);
export const getOpenBatchAssessments = () => apiGet<BatchAssessmentPublication[]>(openPath);
export const claimBatchAssessment = ({ id, ...input }: { id: number } & ClaimBatchAssessmentRequest) =>
  apiPost<ClaimBatchAssessmentRequest, BatchAssessmentPublication>(`${openPath}/${id}/claim`, input);
export const completeBatchAssessment = (id: number) =>
  apiPost<undefined, BatchAssessmentPublication>(`/api/v1/assessor/assessments/${id}/complete`, undefined);
export const getPublishedBatch = (id: number) => apiGet<BatchAssessmentReview>(`/api/v1/assessor/assessments/${id}/published-batch`);
export const getOwnAssessmentAppointments = () => apiGet<BatchAssessmentAppointment[]>('/api/v1/trainees/me/batch-assessments');

export async function downloadAssessmentDocument(resultId: number): Promise<void> {
  const response = await apiClient.get<Blob>(`/api/v1/assessment-results/${resultId}/document`, {
    responseType: 'blob',
    headers: { Accept: 'application/pdf' },
  });
  const disposition = String(response.headers['content-disposition'] ?? '');
  const filename = /filename="?((?:Assessment_Result|Certificate)_\d+\.pdf)"?/i.exec(disposition)?.[1];
  const url = URL.createObjectURL(response.data);
  const link = document.createElement('a');
  link.href = url;
  link.download = filename ?? `Assessment_Result_${resultId}.pdf`;
  document.body.appendChild(link);
  link.click();
  link.remove();
  window.setTimeout(() => URL.revokeObjectURL(url), 1000);
}

export function formatAssessmentStart(value: string): string {
  return new Date(`${value}+05:30`).toLocaleString('en-IN', {
    dateStyle: 'medium', timeStyle: 'short', timeZone: 'Asia/Kolkata',
  }) + ' IST';
}
