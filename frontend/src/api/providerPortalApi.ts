import {
  apiGet,
  apiGetPaged,
  apiPost,
  apiPut,
  apiUpload,
  apiDownload,
} from './client';
import type { ApiError } from './types';
import type { components } from './generated/api.types';
import type { PagedResponse } from './types';
import { apiBaseUrl } from '@/lib/demoEnvironment';
import type {
  TrainingCenter,
  TrainingBatch,
  ChangeBatchStatusPayload,
  TrainingEnrollmentItem,
  CreateEnrollmentPayload,
  AttendanceRecordItem,
  CreateAttendancePayload,
  ImportBatchItem,
  ImportBatchProgress,
  ImportRecordItem,
} from '@/types/provider';
import type { TraineeProfile } from '@/types/trainee';

/**
 * Phase 30.4 Training Provider Portal API Client
 * Real Spring Boot backend endpoints.
 */

// ---------------------------------------------------------------------------
// 1. Training Centers
// ---------------------------------------------------------------------------

export async function getTrainingCenters(params?: {
  providerId?: number;
  code?: string;
  districtId?: number;
  stateId?: number;
  page?: number;
  size?: number;
  sort?: string;
  direction?: string;
}): Promise<PagedResponse<TrainingCenter>> {
  if (params?.code && params.code.trim()) {
    try {
      const single = await apiGet<TrainingCenter>('/api/v1/training-centers', {
        params: { code: params.code.trim() },
      });
      return {
        content: single ? [single] : [],
        page: 0,
        size: 1,
        totalElements: single ? 1 : 0,
        totalPages: single ? 1 : 0,
        first: true,
        last: true,
        hasNext: false,
        hasPrevious: false,
      };
    } catch (error) {
      if ((error as ApiError)?.status !== 404) throw error;
      return {
        content: [],
        page: 0,
        size: 1,
        totalElements: 0,
        totalPages: 0,
        first: true,
        last: true,
        hasNext: false,
        hasPrevious: false,
      };
    }
  }
  return apiGetPaged<TrainingCenter>('/api/v1/training-centers', { params });
}

export async function getTrainingCenterById(id: number): Promise<TrainingCenter> {
  return apiGet<TrainingCenter>(`/api/v1/training-centers/${id}`);
}

// ---------------------------------------------------------------------------
// 3. Batch Management Workbench
// ---------------------------------------------------------------------------

export async function getTrainingBatches(params?: {
  providerId?: number;
  centerId?: number;
  courseId?: number;
  programId?: number;
  statusId?: number;
  code?: string;
  page?: number;
  size?: number;
  sort?: string;
  direction?: string;
}): Promise<PagedResponse<TrainingBatch>> {
  if (params?.code && params.code.trim()) {
    try {
      const single = await apiGet<TrainingBatch>('/api/v1/training-batches', {
        params: { code: params.code.trim() },
      });
      return {
        content: single ? [single] : [],
        page: 0,
        size: 1,
        totalElements: single ? 1 : 0,
        totalPages: single ? 1 : 0,
        first: true,
        last: true,
        hasNext: false,
        hasPrevious: false,
      };
    } catch (error) {
      if ((error as ApiError)?.status !== 404) throw error;
      return {
        content: [],
        page: 0,
        size: 1,
        totalElements: 0,
        totalPages: 0,
        first: true,
        last: true,
        hasNext: false,
        hasPrevious: false,
      };
    }
  }
  return apiGetPaged<TrainingBatch>('/api/v1/training-batches', { params });
}

export async function getTrainingBatchById(id: number): Promise<TrainingBatch> {
  return apiGet<TrainingBatch>(`/api/v1/training-batches/${id}`);
}

export async function updateTrainingBatchStatus(
  batchId: number,
  payload: ChangeBatchStatusPayload,
): Promise<TrainingBatch> {
  return apiPut<ChangeBatchStatusPayload, TrainingBatch>(
    `/api/v1/training-batches/${batchId}/status`,
    payload,
  );
}

// ---------------------------------------------------------------------------
// 4. Single Trainee Enrollment & Trainee Lookup
// ---------------------------------------------------------------------------

export async function searchTraineeByRegNumber(registrationNumber: string): Promise<TraineeProfile> {
  return apiGet<TraineeProfile>('/api/v1/trainees', {
    params: { registrationNumber: registrationNumber.trim() },
  });
}

export async function searchTraineeByUserId(userId: number): Promise<TraineeProfile> {
  return apiGet<TraineeProfile>('/api/v1/trainees', {
    params: { userId },
  });
}

export async function getTraineeById(id: number): Promise<TraineeProfile> {
  return apiGet<TraineeProfile>(`/api/v1/trainees/${id}`);
}

export async function getTrainingEnrollments(params?: {
  batchId?: number;
  providerId?: number;
  centerId?: number;
  traineeId?: number;
  statusId?: number;
  page?: number;
  size?: number;
  sort?: string;
  direction?: string;
}): Promise<PagedResponse<TrainingEnrollmentItem>> {
  return apiGetPaged<TrainingEnrollmentItem>('/api/v1/training-enrollments', { params });
}

export async function createTrainingEnrollment(
  payload: CreateEnrollmentPayload,
): Promise<TrainingEnrollmentItem> {
  return apiPost<CreateEnrollmentPayload, TrainingEnrollmentItem>(
    '/api/v1/training-enrollments',
    payload,
  );
}

export type EnrollmentCorrection = components['schemas']['UpdateTrainingEnrollmentRequest'];

export async function getTrainingEnrollmentById(id: number, signal?: AbortSignal) {
  return apiGet<components['schemas']['TrainingEnrollmentResponse']>(`/api/v1/training-enrollments/${id}`, { signal });
}

export async function correctTrainingEnrollment(id: number, payload: EnrollmentCorrection) {
  return apiPut<EnrollmentCorrection, components['schemas']['TrainingEnrollmentResponse']>(`/api/v1/training-enrollments/${id}`, payload);
}

// ---------------------------------------------------------------------------
// 5. Attendance Recording
// ---------------------------------------------------------------------------

export async function getAttendanceByBatchAndDate(
  batchId: number,
  sessionDate: string,
): Promise<AttendanceRecordItem[]> {
  return apiGet<AttendanceRecordItem[]>('/api/v1/attendance-records', {
    params: { batchId, sessionDate },
  });
}

export async function getAttendanceByEnrollmentId(
  enrollmentId: number,
): Promise<AttendanceRecordItem[]> {
  return apiGet<AttendanceRecordItem[]>('/api/v1/attendance-records', {
    params: { enrollmentId },
  });
}

export async function recordAttendance(
  payload: CreateAttendancePayload,
): Promise<AttendanceRecordItem> {
  return apiPost<CreateAttendancePayload, AttendanceRecordItem>(
    '/api/v1/attendance-records',
    payload,
  );
}

// ---------------------------------------------------------------------------
// 6. Multipart Bulk Ingestion Workbench
// ---------------------------------------------------------------------------

export async function uploadBulkImportBatch(
  file: File,
  entityType = 'Trainee',
  sourceSystem = 'PORTAL_UPLOAD',
): Promise<ImportBatchItem> {
  const formData = new FormData();
  formData.append('file', file);
  return apiUpload<ImportBatchItem>(
    `/api/v1/import-batches?entityType=${encodeURIComponent(entityType)}&sourceSystem=${encodeURIComponent(sourceSystem)}`,
    formData,
  );
}

export async function processImportBatch(batchId: number): Promise<ImportBatchProgress> {
  return apiPost<Record<string, never>, ImportBatchProgress>(
    `/api/v1/import-batches/${batchId}/process`,
    {},
    { timeout: 180_000 },
  );
}

export async function getImportBatchStatus(batchId: number, signal?: AbortSignal): Promise<ImportBatchProgress> {
  return apiGet<ImportBatchProgress>(`/api/v1/import-batches/${batchId}/status`, { signal });
}

export async function recoverImportBatch(batchId: number): Promise<ImportBatchProgress> {
  return apiPost<Record<string, never>, ImportBatchProgress>(`/api/v1/import-batches/${batchId}/recover`, {}, {
    params: { confirmWorkerTerminated: true },
  });
}

// ---------------------------------------------------------------------------
// 7. Quarantine Inspection & Error CSV Export
// ---------------------------------------------------------------------------

export async function getQuarantinedRecords(
  batchId: number,
  includeDuplicates = true,
  page = 0,
  size = 20,
): Promise<PagedResponse<ImportRecordItem>> {
  return apiGetPaged<ImportRecordItem>(`/api/v1/import-batches/${batchId}/quarantine`, {
    params: { includeDuplicates, page, size },
  });
}

export async function downloadQuarantineExport(
  batchId: number,
  includeDuplicates = true,
): Promise<Blob> {
  return apiDownload(`/api/v1/import-batches/${batchId}/quarantine/export`, {
    params: { includeDuplicates },
    headers: {
      Accept: 'text/csv, application/octet-stream, */*',
    },
  });
}

export function getQuarantineExportUrl(batchId: number, includeDuplicates = true): string {
  const base = apiBaseUrl();
  return `${base}/api/v1/import-batches/${batchId}/quarantine/export?includeDuplicates=${includeDuplicates}`;
}
