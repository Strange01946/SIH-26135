/**
 * Phase 30.5 Training Provider Data Quality Remediation & Center Scorecard Types
 */

export interface ProviderDataQualityIssue {
  id: number;
  ruleId: number | null;
  ruleCode: string | null;
  ruleName: string | null;
  categoryId: number;
  categoryCode: string;
  categoryName: string;
  severityId: number;
  severityCode: string;
  severityName: string;
  statusId: number;
  statusCode: string;
  statusName: string;
  isOpen: boolean;
  entityType: string;
  entityId: number | null;
  centerId: number | null;
  centerName: string | null;
  batchId: number | null;
  batchCode: string | null;
  issueSummary: string;
  detectedAt: string;
  updatedAt: string | null;
  resolvedAt: string | null;
  resolvedByUserId: number | null;
  resolutionNotes: string | null;
  remediationPermitted: boolean;
  remediationReason: string | null;
}

export interface DataQualityIssueEvent {
  id: number;
  issueId: number;
  statusId: number;
  statusCode: string;
  statusName: string;
  changedByUserId: number;
  changedAt: string;
  remarks: string | null;
}

export interface CenterScorecard {
  centerId: number;
  centerCode: string;
  centerName: string;
  providerId: number;
  cleanlinessIndexPct: number | null;
  cleanlinessFormula: string;
  cleanlinessScope: string;
  eligiblePopulation: number;
  distinctAffectedEntities: number;
  cleanEntitiesEstimate: number;
  totalActiveIssues: number;
  activeIssuesBySeverity: Record<string, number>;
  activeIssuesByCategory: Record<string, number>;
  totalEnrollments: number;
  completedEnrollments: number;
  droppedEnrollments: number;
  inProgressEnrollments: number;
  completionRatePct: number | null;
  dropoutRatePct: number | null;
  evaluatedAt: string;
}

export interface IssueFilterParams {
  severity?: string;
  category?: string;
  status?: string;
  entityType?: string;
  centerId?: number;
  activeOnly?: boolean;
  page?: number;
  size?: number;
  sort?: string;
  direction?: string;
}

export interface ResolveIssuePayload {
  resolutionNotes: string;
}

export interface DismissIssuePayload {
  remarks: string;
}
