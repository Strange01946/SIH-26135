/**
 * Type definitions for Phase 30.4 Training Provider Portal
 * Centers, Courses, Batches, Trainee Enrollment, and Bulk Ingestion
 */

export interface TrainingCenter {
  id: number;
  centerCode: string;
  centerName: string;
  providerId: number;
  providerCode?: string;
  providerName?: string;
  stateId: number;
  stateName?: string;
  districtId: number;
  districtName?: string;
  addressLine1?: string;
  addressLine2?: string;
  pincode?: string;
  contactPersonName?: string;
  contactEmail?: string;
  contactPhone?: string;
  lifecycleStatusId?: number;
  lifecycleStatusCode?: string;
  createdAt?: string;
  updatedAt?: string;
}

export interface Course {
  id: number;
  courseCode: string;
  courseName: string;
  sectorId?: number;
  sectorCode?: string;
  sectorName?: string;
  industryId?: number;
  industryName?: string;
  deliveryModeId?: number;
  deliveryModeName?: string;
  durationHours?: number;
  nsqfLevel?: number;
  description?: string;
  lifecycleStatusId?: number;
  lifecycleStatusCode?: string;
  createdAt?: string;
  updatedAt?: string;
}

export interface TrainingBatch {
  id: number;
  batchCode: string;
  courseId: number;
  courseCode?: string;
  courseName?: string;
  providerId: number;
  providerCode?: string;
  providerName?: string;
  centerId: number;
  centerCode?: string;
  centerName?: string;
  programId?: number;
  programCode?: string;
  programName?: string;
  startDate: string;
  endDate: string;
  capacity: number;
  enrolledCount?: number;
  availableSeats?: number;
  batchStatusId: number;
  batchStatusCode?: string;
  batchStatusName?: string;
  createdAt?: string;
  updatedAt?: string;
}

export interface CreateBatchPayload {
  batchCode: string;
  courseId: number;
  providerId: number;
  centerId: number;
  programId?: number;
  startDate: string;
  endDate: string;
  capacity: number;
  batchStatusId?: number;
}

export interface ChangeBatchStatusPayload {
  batchStatusId: number;
  remarks?: string;
}

export interface TrainingEnrollmentItem {
  id: number;
  enrollmentNumber: string;
  traineeId: number;
  traineeName?: string;
  programId?: number;
  courseId?: number;
  providerId: number;
  centerId: number;
  batchId: number;
  enrollmentDate: string;
  startDate?: string;
  expectedCompletionDate?: string;
  actualCompletionDate?: string;
  enrollmentStatusId: number;
  enrollmentStatusCode?: string;
  enrollmentStatusName?: string;
  dropoutReasonId?: number;
  dropoutRemarks?: string;
  createdAt?: string;
}

export interface CreateEnrollmentPayload {
  traineeId: number;
  batchId: number;
  enrollmentDate: string;
  enrollmentNumber?: string;
  programId?: number;
  courseId?: number;
  providerId?: number;
  centerId?: number;
  enrollmentStatusId?: number;
  startDate?: string;
  expectedCompletionDate?: string;
}

export interface AttendanceRecordItem {
  id: number;
  enrollmentId: number;
  sessionDate: string;
  attendanceStatusId: number;
  attendanceStatusCode?: string;
  attendanceStatusName?: string;
  sessionSequence?: number;
  sessionStartTime?: string;
  sessionEndTime?: string;
  checkInTime?: string;
  remarks?: string;
  createdAt?: string;
}

export interface CreateAttendancePayload {
  enrollmentId: number;
  sessionDate: string;
  attendanceStatusId: number;
  sessionSequence?: number;
  checkInTime?: string;
  remarks?: string;
}

export interface ImportBatchItem {
  id: number;
  batchCode: string;
  sourceSystem?: string;
  entityType?: string;
  statusId?: number;
  statusCode?: string;
  statusName?: string;
  totalRecords?: number;
  processedRecords?: number;
  successRecords?: number;
  failedRecords?: number;
  duplicateRecords?: number;
  createdAt?: string;
}

export interface ImportBatchProgress {
  importBatchId: number;
  batchCode: string;
  sourceSystem?: string;
  entityType?: string;
  status: string;
  isTerminal: boolean;
  initiatedByUserId?: number;
  rowCount: number;
  successCount: number;
  failureCount: number;
  duplicateCount: number;
  skippedCount: number;
  startedAt?: string;
  completedAt?: string;
  durationMs?: number;
}

export interface ImportRecordItem {
  id: number;
  importBatchId: number;
  importBatchCode?: string;
  sourceRowNumber: number;
  entityType?: string;
  entityId?: number;
  importRecordStatusId?: number;
  importRecordStatusCode?: string;
  importRecordStatusName?: string;
  errorCode?: string;
  errorMessage?: string;
  createdAt?: string;
  updatedAt?: string;
}
