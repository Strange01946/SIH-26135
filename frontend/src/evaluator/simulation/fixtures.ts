import type { AuthenticatedUserResponse } from '@/api/types';
import type { EmployerReferences, EmployerPlacement, EmployerVerification } from '@/api/employerApi';
import type { MisOptions } from '@/api/misApi';
import type { AssessorAssessmentSummary, AssessorCandidateRosterItem } from '@/types/assessor';
import type { TraineeConsent, TraineeProfile } from '@/types/trainee';
import { demoAccounts } from '@/lib/demoAccounts';

export const sampleBatchId = 7301;
export const sampleAssessmentId = 7401;
export const sampleVerificationId = 7501;
export const mockSignInProof = 'SIH browser simulation';
export const sampleCsv = [
  'registration_number,first_name,last_name,gender_code,state_code,district_code',
  'SIM-PUNE-001,Asha,Patil,FEMALE,MH,PUNE',
  'SIM-PUNE-002,Neel,Shah,MALE,MH,UNKNOWN',
  'SIM-PUNE-001,Asha,Patil,FEMALE,MH,PUNE',
  '',
].join('\n');

export type DemoPersona = (typeof demoAccounts)[number]['persona'];
const roles: Record<DemoPersona, string[]> = {
  SUPER_ADMIN: ['ROLE_super_admin', 'analytics.read', 'system.manage', 'audit.read', 'communication.manage'],
  TRAINING_PROVIDER: ['ROLE_training_provider', 'training.manage', 'trainee.read', 'enrollment.manage'],
  ASSESSOR: ['ROLE_assessor', 'assessment.manage', 'trainee.read'],
  EMPLOYER: ['ROLE_employer', 'placement.manage', 'employment.manage', 'verification.manage'],
  TRAINEE: ['ROLE_trainee', 'trainee.read', 'consent.manage', 'enrollment.manage', 'assessment.manage'],
};
export const accounts = new Map<string, AuthenticatedUserResponse>(demoAccounts.map(({ username, persona, label }, index) => [
  username, {
    id: 9_600_000 + index, username, fullName: label + ' (sample)', authorities: roles[persona],
    ...(persona === 'TRAINING_PROVIDER' ? { trainingProviderId: 81 } : {}),
    ...(persona === 'EMPLOYER' ? { employerId: 91 } : {}),
  },
]));

export function sampleDate(days = 0): string {
  return new Date(Date.now() + days * 86_400_000).toISOString().slice(0, 10);
}
export const timestamp = () => new Date().toISOString();
export const candidate: TraineeProfile = {
  id: 7101, userId: accounts.get('trainee_user')!.id, registrationNumber: 'SIM-PUNE-001',
  firstName: 'Asha', lastName: 'Patil', genderId: 1, email: 'asha@example.invalid',
  stateId: 27, districtId: 271, currentEmploymentStatusId: 3, profileStatusId: 3,
};
const consentTypes: Record<number, [string, string]> = {
  1: ['DATA_PROCESSING', 'Core Scheme Data Processing'],
  2: ['EMPLOYMENT_FOLLOW_UP', 'Employment Outcome Follow-Up & Verification'],
  3: ['EMPLOYER_SHARING', 'Share with employers'],
  4: ['ANALYTICS', 'Outcome Analytics Export'],
  5: ['COMMUNICATION_SMS', 'Transactional SMS Communication'],
  6: ['COMMUNICATION_WHATSAPP', 'WhatsApp Operational Messaging'],
  7: ['COMMUNICATION_EMAIL', 'Official Email Communications'],
  8: ['COMMUNICATION_PHONE', 'Phone Follow-Up'],
};

export function sampleConsent(id: number, traineeId: number, typeId: number, granted: boolean): TraineeConsent | undefined {
  const type = consentTypes[typeId];
  if (!type) return undefined;
  return {
    id, traineeId, consentTypeId: typeId, consentTypeCode: type[0], consentTypeName: type[1],
    consentStatusId: granted ? 1 : 2, consentStatusCode: granted ? 'GRANTED' : 'REVOKED',
    consentStatusName: granted ? 'Granted' : 'Revoked', purpose: type[1],
    policyVersion: '1.0', grantedAt: sampleDate(-45) + 'T09:00:00',
    revokedAt: granted ? null : timestamp(), updatedAt: timestamp(), capturedChannel: 'WEB',
  };
}

export const reference = {
  genders: [{ id: 1, code: 'FEMALE', name: 'Female' }, { id: 2, code: 'MALE', name: 'Male' }, { id: 4, code: 'OTHER', name: 'Other' },
    { id: 5, code: 'PREFER_NOT_TO_SAY', name: 'Prefer not to say' }],
  states: [{ id: 27, code: 'MH', name: 'Maharashtra' }, { id: 29, code: 'KA', name: 'Karnataka' }],
  districts: [{ id: 271, code: 'PUNE', name: 'Pune', parentId: 27 }, { id: 272, code: 'NASHIK', name: 'Nashik', parentId: 27 },
    { id: 291, code: 'BENGALURU', name: 'Bengaluru Urban', parentId: 29 }],
  organizationTypes: [{ id: 1, code: 'PRIVATE', name: 'Private enterprise' }],
  employmentStatuses: [{ id: 1, code: 'UNEMPLOYED', name: 'Unemployed' }, { id: 3, code: 'WAGE_EMPLOYED', name: 'Wage employed' }, { id: 8, code: 'UNKNOWN', name: 'Unknown' }],
};
export const course = 'Solar PV Installation';
export const candidateName = 'Asha Patil';
export const employerName = 'Sahyadri Solar Services (sample)';

export const assessment: AssessorAssessmentSummary = {
  id: sampleAssessmentId, assessmentCode: 'SIM-ASSESS-001', assessmentName: 'Solar PV practical assessment',
  courseName: course, batchCode: 'SIM-SOLAR-PUNE', centerName: 'Pune Skills Centre (sample)',
  assessmentDate: sampleDate(), maximumScore: 100, passScore: 60,
  theoryMaxScore: 40, practicalMaxScore: 50, vivaMaxScore: 10, lifecycleStatusCode: 'IN_PROGRESS',
  totalCandidates: 1, scoredCandidates: 0, isFinalized: false,
};
export const assessmentCandidate: AssessorCandidateRosterItem = {
  traineeId: candidate.id, registrationNumber: candidate.registrationNumber, fullName: candidateName, enrollmentId: 7201,
  attemptNumber: 1, appeared: true, theoryScore: null, practicalScore: null, vivaScore: null,
  totalScore: null, scorePercentage: null, isFinalized: false, outcome: 'UNASSESSED',
};
export const certificate = {
  id: 7402, certificateNumber: 'SIM-CERT-PUNE-001', traineeId: candidate.id, courseName: course,
  issuingBody: 'Maharashtra Skills Programme (sample)', issueDate: sampleDate(-40), certificateStatusCode: 'ISSUED',
  certificateVerificationStatusCode: 'VERIFIED', assessmentResultId: 7403,
};

export const placement: EmployerPlacement = {
  id: 7502, number: 'SIM-PLACE-PUNE-001', candidateName, course, jobRoleId: 1, jobRole: 'Solar Technician',
  placementStatus: 'JOINED', joiningStatus: 'JOINED', joinedOn: sampleDate(-30),
  offeredPay: 18500, payFrequencyId: 1, payFrequency: 'Monthly', currency: 'INR', employmentId: 7503, canConfirm: false,
};
export const verification: EmployerVerification = {
  id: sampleVerificationId, number: 'SIM-VERIFY-PUNE-001', candidateName, employmentNumber: 'SIM-EMP-PUNE-001',
  jobRole: 'Solar Technician', startedOn: sampleDate(-30), startingPay: 18500, payFrequency: 'Monthly',
  currency: 'INR', requestedAt: timestamp(), dueAt: sampleDate(7) + 'T17:00:00',
  requestStatus: 'PENDING', employmentVerificationStatus: 'PENDING', canRespond: true, canReportExit: true,
};
export const employerReference: EmployerReferences = {
  employerName, jobRoles: [{ id: 1, label: 'Solar Technician' }],
  qualifications: [], districts: reference.districts.map(({ id, name: label }) => ({ id, label })),
  engagementTypes: [], payFrequencies: [{ id: 1, label: 'Monthly' }], skillLevels: [],
  rejectionReasons: [{ id: 1, label: 'Employment record mismatch' }], exitReasons: [{ id: 1, label: 'Voluntary resignation' }],
  separationNatures: [{ id: 1, label: 'Voluntary' }],
};

export const misOptions: MisOptions = {
  financialYears: ['2025-26', '2026-27'], schemes: [{ id: 1, name: 'State Solar Skills Programme (sample)', administration: 'STATE' }],
  sectors: [{ id: 1, name: 'Renewable energy' }], states: reference.states,
  districts: reference.districts.map((item) => ({ ...item, stateId: item.parentId })),
  genders: reference.genders, socialCategorySupported: false, suppressionThreshold: 5,
};

export const templates = [
  ['TPL-ENROLL-001', 'Training enrollment', 'Dear {traineeName}, your enrollment in {courseName}, batch {batchCode}, starts on {startDate} at {trainingCenterName}.'],
  ['TPL-ASSESS-002', 'Assessment and certificate', 'Dear {traineeName}, your result for {courseName} is {resultStatus}. Certificate {certificateNumber} is available at {certificateUrl}.'],
  ['TPL-VERIF-003', 'Employment verification', 'Dear {traineeName}, verification {verificationId} for {employerName} is ready at {verificationUrl}.'],
  ['TPL-SURVEY-004', 'Post-placement survey', 'Dear {traineeName}, complete your {milestoneMonths}-month survey {surveyId} at {surveyUrl}.'],
  ['TPL-ALERT-005', 'Operational alert', 'Dear {traineeName}, {alertType}. {actionRequired}. Reference {referenceId}.'],
].map(([templateCode, subject, bodyTemplate]) => ({
  templateCode, subject, bodyTemplate, allowedParameterKeys: [...bodyTemplate.matchAll(/\{(\w+)\}/g)].map((match) => match[1]),
}));
