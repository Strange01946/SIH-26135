package in.gov.sih.sih26135.service;

import in.gov.sih.sih26135.dto.request.AssignDataQualityIssueRequest;
import in.gov.sih.sih26135.dto.request.DismissDataQualityIssueRequest;
import in.gov.sih.sih26135.dto.request.ReopenDataQualityIssueRequest;
import in.gov.sih.sih26135.dto.request.ResolveDataQualityIssueRequest;
import in.gov.sih.sih26135.dto.request.StartInvestigationRequest;
import in.gov.sih.sih26135.dto.response.DataQualityIssueEventResponse;
import in.gov.sih.sih26135.dto.response.DataQualityIssueResponse;
import java.util.List;

/**
 * Service managing transactional, auditable lifecycle transitions and remediation
 * workflows for Data Quality Issues.
 */
public interface DataQualityWorkflowService {

  /**
   * Transitions an open or unassigned issue into ASSIGNED state, assigning an authorized reviewer.
   *
   * @param issueId primary key identifier of the issue
   * @param request assignment request containing assignee user ID and optional remarks
   * @return updated issue representation
   */
  DataQualityIssueResponse assignIssue(Long issueId, AssignDataQualityIssueRequest request);

  /**
   * Transitions an assigned issue into IN_PROGRESS state to commence active anomaly investigation.
   *
   * @param issueId primary key identifier of the issue
   * @param request investigation request containing optional remarks
   * @return updated issue representation
   */
  DataQualityIssueResponse startInvestigation(Long issueId, StartInvestigationRequest request);

  /**
   * Transitions an in-progress issue into RESOLVED state with mandatory non-blank resolution notes.
   * Attribution (resolvedByUserId, resolvedAt) is determined strictly server-side.
   *
   * @param issueId primary key identifier of the issue
   * @param request resolution request containing resolution notes
   * @return updated issue representation
   */
  DataQualityIssueResponse resolveIssue(Long issueId, ResolveDataQualityIssueRequest request);

  /**
   * Transitions an open or in-progress issue into DISMISSED terminal state with mandatory remarks.
   *
   * @param issueId primary key identifier of the issue
   * @param request dismissal request containing mandatory remarks
   * @return updated issue representation
   */
  DataQualityIssueResponse dismissIssue(Long issueId, DismissDataQualityIssueRequest request);

  /**
   * Reopens a previously resolved issue into REOPENED state with mandatory reopening reason.
   *
   * @param issueId primary key identifier of the issue
   * @param request reopening request containing mandatory reason
   * @return updated issue representation
   */
  DataQualityIssueResponse reopenIssue(Long issueId, ReopenDataQualityIssueRequest request);

  /**
   * Retrieves the historical event audit trail for a specific data quality issue.
   *
   * @param issueId primary key identifier of the issue
   * @return ordered list of issue events
   */
  List<DataQualityIssueEventResponse> getIssueEvents(Long issueId);
}
