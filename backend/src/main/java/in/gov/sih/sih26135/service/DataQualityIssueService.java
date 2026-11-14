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

  default List<DataQualityIssueResponse> getIssuesByRuleId(Long ruleId) {
    return getIssuesByRuleId(ruleId, 0, 100);
  }

  List<DataQualityIssueResponse> getIssuesByRuleId(Long ruleId, int page, int size);

  default List<DataQualityIssueResponse> getIssuesByCategoryId(Long categoryId) {
    return getIssuesByCategoryId(categoryId, 0, 100);
  }

  List<DataQualityIssueResponse> getIssuesByCategoryId(Long categoryId, int page, int size);

  default List<DataQualityIssueResponse> getIssuesBySeverityId(Long severityId) {
    return getIssuesBySeverityId(severityId, 0, 100);
  }

  List<DataQualityIssueResponse> getIssuesBySeverityId(Long severityId, int page, int size);

  default List<DataQualityIssueResponse> getIssuesByStatusId(Long statusId) {
    return getIssuesByStatusId(statusId, 0, 100);
  }

  List<DataQualityIssueResponse> getIssuesByStatusId(Long statusId, int page, int size);

  default List<DataQualityIssueResponse> getIssuesByDetectionSourceId(Long sourceId) {
    return getIssuesByDetectionSourceId(sourceId, 0, 100);
  }

  List<DataQualityIssueResponse> getIssuesByDetectionSourceId(Long sourceId, int page, int size);

  default List<DataQualityIssueResponse> getIssuesByEntityType(String entityType) {
    return getIssuesByEntityType(entityType, 0, 100);
  }

  List<DataQualityIssueResponse> getIssuesByEntityType(String entityType, int page, int size);

  default List<DataQualityIssueResponse> getIssuesByEntity(String entityType, Long entityId) {
    return getIssuesByEntity(entityType, entityId, 0, 100);
  }

  List<DataQualityIssueResponse> getIssuesByEntity(String entityType, Long entityId, int page, int size);

  default List<DataQualityIssueResponse> getIssuesByAssignedUserId(Long assignedUserId) {
    return getIssuesByAssignedUserId(assignedUserId, 0, 100);
  }

  List<DataQualityIssueResponse> getIssuesByAssignedUserId(Long assignedUserId, int page, int size);

  default List<DataQualityIssueResponse> getIssuesByResolvedByUserId(Long resolvedByUserId) {
    return getIssuesByResolvedByUserId(resolvedByUserId, 0, 100);
  }

  List<DataQualityIssueResponse> getIssuesByResolvedByUserId(Long resolvedByUserId, int page, int size);
}
