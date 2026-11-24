import { describe, it, expect, vi, beforeEach } from 'vitest';
import { requestVenue } from '@/api/venueBookingApi';
import * as client from '@/api/client';
import {
  getTrainingCenters,
  updateTrainingBatchStatus,
  searchTraineeByRegNumber,
  createTrainingEnrollment,
  recordAttendance,
  getImportBatchStatus,
  getQuarantinedRecords,
  downloadQuarantineExport,
} from '@/api/providerPortalApi';

vi.mock('@/api/client', () => ({
  apiGet: vi.fn(),
  apiGetPaged: vi.fn(),
  apiPost: vi.fn(),
  apiPut: vi.fn(),
  apiUpload: vi.fn(),
  apiDelete: vi.fn(),
  apiDownload: vi.fn(),
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

  it('requests a booked batch through the venue workflow', async () => {
    const payload = {
      batchCode: 'BATCH-2026-001', centerId: 1, seats: 30, skillsToTeach: 'Equipment inspection',
      sessions: [{ startsAt: '2027-06-01T09:00:00', endsAt: '2027-06-01T11:00:00' }],
    };
    const mockResponse = { booking: { id: 50, status: 'PENDING' }, sessions: payload.sessions };
    vi.mocked(client.apiPost).mockResolvedValue(mockResponse);
    expect(await requestVenue(payload)).toEqual(mockResponse);
    expect(client.apiPost).toHaveBeenCalledWith('/api/v1/venue-bookings', payload);
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
    expect(client.apiGet).toHaveBeenCalledWith('/api/v1/import-batches/12/status', { signal: undefined });
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

  it('downloads authenticated quarantine error export as blob', async () => {
    const mockBlob = new Blob(['source_row,error_code\n1,TEST_ERROR'], { type: 'text/csv' });
    vi.mocked(client.apiDownload).mockResolvedValue(mockBlob);

    const result = await downloadQuarantineExport(12, false);
    expect(client.apiDownload).toHaveBeenCalledWith('/api/v1/import-batches/12/quarantine/export', {
      params: {
        includeDuplicates: false,
      },
      headers: {
        Accept: 'text/csv, application/octet-stream, */*',
      },
    });
    expect(result).toBe(mockBlob);
  });
});
