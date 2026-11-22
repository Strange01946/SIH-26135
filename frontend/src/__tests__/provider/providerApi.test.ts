import { describe, it, expect, vi, beforeEach } from 'vitest';
import * as client from '@/api/client';
import {
  getTrainingCenters,
  getCourses,
  createTrainingBatch,
  updateTrainingBatchStatus,
  searchTraineeByRegNumber,
  createTrainingEnrollment,
  recordAttendance,
  getImportBatchStatus,
  getQuarantinedRecords,
} from '@/api/providerPortalApi';

vi.mock('@/api/client', () => ({
  apiGet: vi.fn(),
  apiGetPaged: vi.fn(),
  apiPost: vi.fn(),
  apiPut: vi.fn(),
  apiUpload: vi.fn(),
  apiDelete: vi.fn(),
}));

describe('providerPortalApi', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('fetches training centers with providerId and pagination', async () => {
    const mockPaged = {
      content: [{ id: 1, centerName: 'Center A', centerCode: 'TC-01', providerId: 10, stateId: 1, districtId: 2 }],
      totalElements: 1,
      page: 0,
      size: 10,
      totalPages: 1,
      first: true,
      last: true,
      hasNext: false,
      hasPrevious: false,
    };
    vi.mocked(client.apiGetPaged).mockResolvedValue(mockPaged);

    const result = await getTrainingCenters({ providerId: 10, page: 0, size: 10 });
    expect(client.apiGetPaged).toHaveBeenCalledWith('/api/v1/training-centers', {
      params: {
        providerId: 10,
        page: 0,
        size: 10,
        code: undefined,
        sort: undefined,
        direction: undefined,
      },
    });
    expect(result).toEqual(mockPaged);
  });

  it('fetches approved courses', async () => {
    const mockPaged = {
      content: [{ id: 101, courseName: 'Solar Technician', courseCode: 'SOL-01', durationHours: 120 }],
      totalElements: 1,
      page: 0,
      size: 20,
      totalPages: 1,
      first: true,
      last: true,
      hasNext: false,
      hasPrevious: false,
    };
    vi.mocked(client.apiGetPaged).mockResolvedValue(mockPaged);

    const result = await getCourses({ page: 0, size: 20 });
    expect(client.apiGetPaged).toHaveBeenCalledWith('/api/v1/courses', {
      params: {
        page: 0,
        size: 20,
        code: undefined,
        sector: undefined,
      },
    });
    expect(result).toEqual(mockPaged);
  });

  it('creates training batch with payload', async () => {
    const payload = {
      batchCode: 'BATCH-2026-001',
      centerId: 1,
      courseId: 101,
      providerId: 10,
      capacity: 30,
      startDate: '2026-06-01',
      endDate: '2026-08-31',
    };
    const mockResponse = { id: 50, ...payload };
    vi.mocked(client.apiPost).mockResolvedValue(mockResponse);

    const result = await createTrainingBatch(payload);
    expect(client.apiPost).toHaveBeenCalledWith('/api/v1/training-batches', payload);
    expect(result).toEqual(mockResponse);
  });

  it('updates training batch status', async () => {
    const mockResponse = { id: 50, batchStatusId: 2 };
    vi.mocked(client.apiPut).mockResolvedValue(mockResponse);

    const result = await updateTrainingBatchStatus(50, { batchStatusId: 2 });
    expect(client.apiPut).toHaveBeenCalledWith('/api/v1/training-batches/50/status', { batchStatusId: 2 });
    expect(result).toEqual(mockResponse);
  });

  it('searches trainee by registration number', async () => {
    const mockTrainee = {
      id: 7,
      userId: 1,
      registrationNumber: 'TRN-2026-0007',
      firstName: 'Anita',
      stateId: 1,
      districtId: 2,
      currentEmploymentStatusId: 1,
      profileStatusId: 2,
    };
    vi.mocked(client.apiGet).mockResolvedValue(mockTrainee);

    const result = await searchTraineeByRegNumber('TRN-2026-0007');
    expect(client.apiGet).toHaveBeenCalledWith('/api/v1/trainees', {
      params: { registrationNumber: 'TRN-2026-0007' },
    });
    expect(result).toEqual(mockTrainee);
  });

  it('creates enrollment record', async () => {
    const payload = {
      traineeId: 7,
      batchId: 50,
      providerId: 10,
      enrollmentDate: '2026-06-01',
    };
    const mockResponse = { id: 88, enrollmentNumber: 'ENR-001' };
    vi.mocked(client.apiPost).mockResolvedValue(mockResponse);

    const result = await createTrainingEnrollment(payload);
    expect(client.apiPost).toHaveBeenCalledWith('/api/v1/training-enrollments', payload);
    expect(result).toEqual(mockResponse);
  });

  it('records attendance record', async () => {
    const payload = {
      enrollmentId: 88,
      sessionDate: '2026-06-02',
      attendanceStatusId: 1,
      remarks: 'Present in class',
    };
    const mockResponse = { id: 99, ...payload };
    vi.mocked(client.apiPost).mockResolvedValue(mockResponse);

    const result = await recordAttendance(payload);
    expect(client.apiPost).toHaveBeenCalledWith('/api/v1/attendance-records', payload);
    expect(result).toEqual(mockResponse);
  });

  it('fetches import batch progress status', async () => {
    const mockStatus = {
      batchId: 12,
      batchCode: 'IMP-01',
      totalRows: 100,
      processedRows: 100,
      successCount: 95,
      errorCount: 5,
      duplicateCount: 0,
      skippedCount: 0,
      status: 'COMPLETED',
      isTerminal: true,
    };
    vi.mocked(client.apiGet).mockResolvedValue(mockStatus);

    const result = await getImportBatchStatus(12);
    expect(client.apiGet).toHaveBeenCalledWith('/api/v1/import-batches/12/status');
    expect(result).toEqual(mockStatus);
  });

  it('fetches quarantine import records', async () => {
    const mockPaged = {
      content: [{ id: 1, batchId: 12, rowNumber: 4, errorCode: 'FOREIGN_KEY_NOT_FOUND', status: 'FAILED' }],
      totalElements: 1,
      page: 0,
      size: 20,
      totalPages: 1,
      first: true,
      last: true,
      hasNext: false,
      hasPrevious: false,
    };
    vi.mocked(client.apiGetPaged).mockResolvedValue(mockPaged);

    const result = await getQuarantinedRecords(12, true, 0, 20);
    expect(client.apiGetPaged).toHaveBeenCalledWith('/api/v1/import-batches/12/quarantine', {
      params: {
        includeDuplicates: true,
        page: 0,
        size: 20,
      },
    });
    expect(result).toEqual(mockPaged);
  });
});
