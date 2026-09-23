package in.gov.sih.sih26135.service;

import in.gov.sih.sih26135.dto.request.CreateDataQualityIssueEventRequest;
import in.gov.sih.sih26135.dto.response.DataQualityIssueEventResponse;
import java.util.List;

public interface DataQualityIssueEventService {

  DataQualityIssueEventResponse recordIssueEvent(CreateDataQualityIssueEventRequest request);

  DataQualityIssueEventResponse getIssueEventById(Long id);

  List<DataQualityIssueEventResponse> getIssueEventsByIssueId(Long issueId);

  List<DataQualityIssueEventResponse> getIssueEventsByStatusId(Long statusId);

  List<DataQualityIssueEventResponse> getIssueEventsByChangedByUserId(Long changedByUserId);
}
