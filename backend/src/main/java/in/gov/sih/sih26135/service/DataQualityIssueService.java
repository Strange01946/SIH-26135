package in.gov.sih.sih26135.service;

import in.gov.sih.sih26135.dto.request.CreateDataQualityIssueRequest;
import in.gov.sih.sih26135.dto.request.ResolveDataQualityIssueRequest;
import in.gov.sih.sih26135.dto.request.UpdateDataQualityIssueRequest;
import in.gov.sih.sih26135.dto.response.DataQualityIssueResponse;
import java.util.List;

public interface DataQualityIssueService {

  DataQualityIssueResponse reportIssue(CreateDataQualityIssueRequest request);

  DataQualityIssueResponse updateIssue(Long id, UpdateDataQualityIssueRequest request);

  DataQualityIssueResponse assignIssue(Long id, Long assignedUserId);

  DataQualityIssueResponse resolveIssue(Long id, ResolveDataQualityIssueRequest request);

  DataQualityIssueResponse getIssueById(Long id);

  List<DataQualityIssueResponse> getIssuesByRuleId(Long ruleId);

  List<DataQualityIssueResponse> getIssuesByCategoryId(Long categoryId);

  List<DataQualityIssueResponse> getIssuesBySeverityId(Long severityId);

  List<DataQualityIssueResponse> getIssuesByStatusId(Long statusId);

  List<DataQualityIssueResponse> getIssuesByDetectionSourceId(Long sourceId);

  List<DataQualityIssueResponse> getIssuesByEntityType(String entityType);

  List<DataQualityIssueResponse> getIssuesByEntity(String entityType, Long entityId);

  List<DataQualityIssueResponse> getIssuesByAssignedUserId(Long assignedUserId);

  List<DataQualityIssueResponse> getIssuesByResolvedByUserId(Long resolvedByUserId);
}
