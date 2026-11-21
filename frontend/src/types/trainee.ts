/**
 * SIH-26135 Trainee Domain TypeScript Type Definitions
 * Exact alignment with Java Spring Boot Backend DTOs:
 * - TraineeResponse
 * - TrainingEnrollmentResponse
 * - AttendanceRecordResponse
 * - TraineeConsentResponse
 * - AssessmentResultResponse
 * - CertificationResponse
 * - SurveyResponseResponse & SurveyQuestionResponse
 * - TraineeOutcomeSummaryResponse
 */

export interface TraineeProfile {
  id: number;
  userId: number | null;
  registrationNumber: string;
  firstName: string;
  middleName?: string | null;
  lastName?: string | null;
  dateOfBirth?: string | null;
  genderId?: number | null;
  email?: string | null;
  phone?: string | null;
  addressLine1?: string | null;
  addressLine2?: string | null;
  pincode?: string | null;
  locationId?: number | null;
  stateId: number;
  districtId: number;
  blockId?: number | null;
  educationLevelId?: number | null;
  currentEmploymentStatusId: number;
  profileStatusId: number;
  createdAt?: string | null;
  updatedAt?: string | null;
  deletedAt?: string | null;
}

export interface TrainingEnrollment {
  id: number;
  enrollmentNumber: string;
  traineeId: number;
  traineeRegistrationNumber?: string | null;
  traineeFullName?: string | null;
  programId?: number | null;
  programCode?: string | null;
  programName?: string | null;
  courseId?: number | null;
  courseCode?: string | null;
  courseName?: string | null;
  providerId?: number | null;
  providerCode?: string | null;
  providerName?: string | null;
  centerId?: number | null;
  centerCode?: string | null;
  centerName?: string | null;
  batchId?: number | null;
  batchCode?: string | null;
  enrollmentDate?: string | null;
  startDate?: string | null;
  expectedCompletionDate?: string | null;
  actualCompletionDate?: string | null;
  enrollmentStatusId?: number | null;
  enrollmentStatusCode?: string | null;
  enrollmentStatusName?: string | null;
  isTerminal?: boolean | null;
  isCompletedFlag?: boolean | null;
  dropoutReasonId?: number | null;
  dropoutReasonName?: string | null;
  dropoutRemarks?: string | null;
  createdAt?: string | null;
  updatedAt?: string | null;
}

export interface AttendanceRecord {
  id: number;
  enrollmentId: number;
  enrollmentNumber?: string | null;
  traineeId: number;
  batchId: number;
  batchCode?: string | null;
  sessionDate: string;
  sessionSequence?: number | null;
  sessionStartTime?: string | null;
  sessionEndTime?: string | null;
  attendanceStatusId?: number | null;
  attendanceStatusCode: string;
  attendanceStatusName: string;
  countsAsPresent: boolean;
  markedAt?: string | null;
  markedByUserId?: number | null;
  remarks?: string | null;
  createdAt?: string | null;
  updatedAt?: string | null;
}

export type ConsentTypeCode =
  | 'DATA_PROCESSING'
  | 'COMMUNICATION_SMS'
  | 'COMMUNICATION_EMAIL'
  | 'COMMUNICATION_WHATSAPP'
  | 'EMPLOYMENT_FOLLOW_UP';

export type ConsentStatusCode = 'GRANTED' | 'REVOKED' | 'EXPIRED' | 'UNSPECIFIED';

export interface TraineeConsent {
  id: number;
  traineeId: number;
  traineeRegistrationNumber?: string | null;
  consentTypeId: number;
  consentTypeCode: string;
  consentTypeName: string;
  consentStatusId: number;
  consentStatusCode: string;
  consentStatusName: string;
  purpose: string;
  policyVersion?: string | null;
  grantedAt?: string | null;
  revokedAt?: string | null;
  expiresAt?: string | null;
  capturedChannel?: string | null;
  capturedByUserId?: number | null;
  createdAt?: string | null;
  updatedAt?: string | null;
}

export interface CreateTraineeConsentPayload {
  traineeId: number;
  consentTypeId: number;
  consentStatusId: number;
  purpose: string;
  policyVersion?: string;
  grantedAt?: string;
  expiresAt?: string;
  capturedChannel?: string;
}

export interface UpdateTraineeConsentStatusPayload {
  consentStatusId: number;
  revokedAt?: string | null;
}

export interface AssessmentResult {
  id: number;
  traineeAssessmentId?: number | null;
  traineeId: number;
  enrollmentId?: number | null;
  assessmentId?: number | null;
  assessmentName?: string | null;
  assessmentOutcomeId?: number | null;
  assessmentOutcomeCode?: string | null;
  assessmentOutcomeName?: string | null;
  outcomeCode?: string | null;
  outcomeName?: string | null;
  isPassFlag?: boolean | null;
  obtainedScore?: number | null;
  maximumScore?: number | null;
  scorePercentage?: number | null;
  totalScore?: number | null;
  maxScore?: number | null;
  percentage?: number | null;
  grade?: string | null;
  assessedAt?: string | null;
  resultDeclaredAt?: string | null;
  remarks?: string | null;
  createdAt?: string | null;
  updatedAt?: string | null;
}

export interface Certification {
  id: number;
  certificateNumber: string;
  traineeId: number;
  traineeRegistrationNumber?: string | null;
  traineeFullName?: string | null;
  enrollmentId?: number | null;
  enrollmentNumber?: string | null;
  courseId?: number | null;
  courseCode?: string | null;
  courseName?: string | null;
  programId?: number | null;
  programCode?: string | null;
  programName?: string | null;
  assessmentResultId?: number | null;
  issuingBody?: string | null;
  issueDate?: string | null;
  expiryDate?: string | null;
  certificateStatusId?: number | null;
  certificateStatusCode?: string | null;
  certificateStatusName?: string | null;
  certificateVerificationStatusId?: number | null;
  certificateVerificationStatusCode?: string | null;
  certificateVerificationStatusName?: string | null;
  verifiedAt?: string | null;
  verifiedByUserId?: number | null;
  createdAt?: string | null;
}

export interface SurveyResponse {
  id: number;
  surveyId: number;
  traineeId: number;
  attemptNumber: number;
  statusId?: number | null;
  statusCode?: string | null;
  followupTaskId?: number | null;
  enrollmentId?: number | null;
  versionId?: number | null;
  submittedAt?: string | null;
  createdAt?: string | null;
}

export interface SurveyQuestion {
  id: number;
  surveyId: number;
  questionText: string;
  questionType: 'SINGLE_CHOICE' | 'MULTIPLE_CHOICE' | 'TEXT' | 'NUMERIC' | 'RATING';
  isRequired: boolean;
  sortOrder: number;
  optionsJson?: string | null;
}

export interface CreateSurveyResponsePayload {
  surveyId: number;
  traineeId: number;
  attemptNumber?: number;
  statusId?: number;
  followupTaskId?: number;
  enrollmentId?: number;
  versionId?: number;
}

export interface CreateSurveyResponseAnswerPayload {
  surveyResponseId: number;
  surveyQuestionId: number;
  answerText?: string;
  selectedOptionId?: number;
  numericRating?: number;
}

export interface TraineeOutcomeSummary {
  traineeId: number;
  stateId?: number | null;
  districtId?: number | null;
  currentEmploymentStatusId?: number | null;
  snapshotIsEmployedFlag?: boolean | null;
  lastVerifiedEmployerName?: string | null;
  monthlySalary?: number | null;
  lastPlacementDate?: string | null;
}

export interface TraineeSkillGapProfile {
  traineeId: number;
  hasAssessment: boolean;
  identifiedGapsCount?: number;
  recommendationsCount?: number;
  recommendations?: Array<{
    id: number;
    recommendedCourseName?: string;
    status?: string;
    remarks?: string;
  }>;
}
