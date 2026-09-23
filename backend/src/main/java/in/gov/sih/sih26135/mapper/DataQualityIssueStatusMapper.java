package in.gov.sih.sih26135.mapper;

import in.gov.sih.sih26135.dto.response.DataQualityIssueStatusResponse;
import in.gov.sih.sih26135.entity.RefDataQualityIssueStatus;
import org.springframework.stereotype.Component;

@Component
public class DataQualityIssueStatusMapper {

  public DataQualityIssueStatusResponse toResponse(RefDataQualityIssueStatus entity) {
    if (entity == null) {
      return null;
    }
    return new DataQualityIssueStatusResponse(
        entity.getId(),
        entity.getStatusCode(),
        entity.getStatusName(),
        entity.getIsOpenFlag(),
        entity.getIsTerminalFlag(),
        entity.getSortOrder(),
        entity.getCreatedAt(),
        entity.getUpdatedAt()
    );
  }
}
