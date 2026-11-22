package in.gov.sih.sih26135.service;

import in.gov.sih.sih26135.dto.request.DismissDataQualityIssueRequest;
import in.gov.sih.sih26135.dto.request.ResolveDataQualityIssueRequest;
import in.gov.sih.sih26135.dto.response.CenterScorecardResponse;
import in.gov.sih.sih26135.dto.response.DataQualityIssueEventResponse;
import in.gov.sih.sih26135.dto.response.ProviderDataQualityIssueResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

/**
 * Service interface for Training Provider Data Quality remediation and Center Scorecards.
 */
public interface ProviderDataQualityService {

  Page<ProviderDataQualityIssueResponse> getProviderIssues(
      String severity,
      String category,
      String status,
      String entityType,
      Long centerId,
      Boolean activeOnly,
      Pageable pageable);

  ProviderDataQualityIssueResponse getIssueById(Long issueId);

  List<DataQualityIssueEventResponse> getIssueEvents(Long issueId, int page, int size);

  ProviderDataQualityIssueResponse startInvestigation(Long issueId);

  ProviderDataQualityIssueResponse resolveIssue(Long issueId, ResolveDataQualityIssueRequest request);

  ProviderDataQualityIssueResponse dismissIssue(Long issueId, DismissDataQualityIssueRequest request);

  CenterScorecardResponse getCenterScorecard(Long centerId);

  void triggerReEvaluation(Long issueId);
}
