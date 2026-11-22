export type AssessmentOutcome =
  | 'UNASSESSED'
  | 'PENDING'
  | 'PASS'
  | 'FAIL'
  | 'ABSENT';

export interface AssessorAssessmentSummary {
  id: number;
  assessmentId?: number;
  assessmentCode: string;
  assessmentName: string;
  assessmentTypeId?: number;
  assessmentTypeCode?: string;
  assessmentTypeName?: string;
  courseId?: number;
  courseCode?: string;
  courseName?: string;
  batchId?: number;
  batchCode?: string;
  centerId?: number;
  centerCode?: string;
  centerName?: string;
  providerId?: number;
  providerCode?: string;
  providerName?: string;
  programId?: number;
  programCode?: string;
  programName?: string;
  assessmentDate: string;
  maximumScore: number;
  passScore: number;
  theoryMaxScore?: number;
  practicalMaxScore?: number;
  vivaMaxScore?: number;
  theoryMax?: number;
  practicalMax?: number;
  vivaMax?: number;
  lifecycleStatusId?: number;
  lifecycleStatusCode?: string;
  totalCandidates: number;
  candidateCount?: number;
  scoredCandidates: number;
  scoredCount?: number;
  passedCandidates?: number;
  failedCandidates?: number;
  evaluatorUserId?: number;
  evaluatorName?: string;
  isFinalized: boolean;
}

export interface AssessorCandidateRosterItem {
  traineeId: number;
  registrationNumber?: string;
  traineeCode?: string;
  fullName: string;
  gender?: string;
  email?: string;
  phone?: string;
  enrollmentId: number;
  enrollmentNumber?: string;
  enrollmentStatusCode?: string;
  traineeAssessmentId?: number;
  attemptNumber: number;
  appearedFlag?: boolean;
  appeared?: boolean;
  assessmentDate?: string;
  assessmentResultId?: number;
  theoryScore: number | null;
  practicalScore: number | null;
  vivaScore: number | null;
  totalScore: number | null;
  scorePercentage: number | null;
  assessmentOutcomeId?: number;
  assessmentOutcomeCode?: string;
  assessmentOutcomeName?: string;
  outcome?: AssessmentOutcome;
  isPassFlag?: boolean;
  isFinalized: boolean;
  remarks?: string;
  certificateId?: number;
  certificationId?: number;
  certificateNumber?: string;
  certificateStatusCode?: string;
  resultDeclaredAt?: string;
}

export interface AssessorRosterResponse {
  assessment: AssessorAssessmentSummary;
  candidates: AssessorCandidateRosterItem[];
}

export interface CandidateScoreInput {
  traineeId: number;
  enrollmentId: number;
  appeared: boolean;
  theoryScore?: number | null;
  practicalScore?: number | null;
  vivaScore?: number | null;
  remarks?: string;
}

export interface SaveAssessmentScoresRequest {
  isDraft: boolean;
  candidates: CandidateScoreInput[];
}

export interface SaveAssessmentScoresResponse {
  assessmentId: number;
  isFinalized: boolean;
  totalProcessed: number;
  processedCandidates?: number;
  passedCount: number;
  failedCount: number;
  absentCount: number;
  certificatesIssued: number;
  results: AssessorCandidateRosterItem[];
  roster?: AssessorRosterResponse;
}
