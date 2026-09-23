package in.gov.sih.sih26135.mapper;

import in.gov.sih.sih26135.dto.response.DataQualityIssueEventResponse;
import in.gov.sih.sih26135.entity.DataQualityIssueEvent;
import org.springframework.stereotype.Component;

@Component
public class DataQualityIssueEventMapper {

  public DataQualityIssueEventResponse toResponse(DataQualityIssueEvent entity) {
    if (entity == null) {
      return null;
    }

    Long issueId = null;
    if (entity.getDataQualityIssue() != null) {
      issueId = entity.getDataQualityIssue().getId();
    }

    Long statusId = null;
    String statusCode = null;
    String statusName = null;
    if (entity.getDataQualityIssueStatus() != null) {
      statusId = entity.getDataQualityIssueStatus().getId();
      statusCode = entity.getDataQualityIssueStatus().getStatusCode();
      statusName = entity.getDataQualityIssueStatus().getStatusName();
    }

    return new DataQualityIssueEventResponse(
        entity.getId(),
        issueId,
        statusId,
        statusCode,
        statusName,
        entity.getChangedByUserId(),
        entity.getChangedAt(),
        entity.getRemarks(),
        entity.getCreatedAt()
    );
  }
}
