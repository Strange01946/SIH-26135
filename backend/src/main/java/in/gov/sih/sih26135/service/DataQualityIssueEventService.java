package in.gov.sih.sih26135.service;

import in.gov.sih.sih26135.dto.request.CreateDataQualityIssueEventRequest;
import in.gov.sih.sih26135.dto.response.DataQualityIssueEventResponse;
import java.util.List;

public interface DataQualityIssueEventService {

  DataQualityIssueEventResponse recordIssueEvent(CreateDataQualityIssueEventRequest request);

  DataQualityIssueEventResponse getIssueEventById(Long id);

  default List<DataQualityIssueEventResponse> getIssueEventsByIssueId(Long issueId) {
    return getIssueEventsByIssueId(issueId, 0, 100);
  }

  List<DataQualityIssueEventResponse> getIssueEventsByIssueId(Long issueId, int page, int size);

  default List<DataQualityIssueEventResponse> getIssueEventsByStatusId(Long statusId) {
    return getIssueEventsByStatusId(statusId, 0, 100);
  }

  List<DataQualityIssueEventResponse> getIssueEventsByStatusId(Long statusId, int page, int size);

  default List<DataQualityIssueEventResponse> getIssueEventsByChangedByUserId(Long changedByUserId) {
    return getIssueEventsByChangedByUserId(changedByUserId, 0, 100);
  }

  List<DataQualityIssueEventResponse> getIssueEventsByChangedByUserId(Long changedByUserId, int page, int size);
}
