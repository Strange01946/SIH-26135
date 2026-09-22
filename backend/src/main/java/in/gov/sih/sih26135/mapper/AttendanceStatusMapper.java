package in.gov.sih.sih26135.mapper;

import in.gov.sih.sih26135.dto.response.AttendanceStatusResponse;
import in.gov.sih.sih26135.entity.RefAttendanceStatus;
import org.springframework.stereotype.Component;

@Component
public class AttendanceStatusMapper {

  public AttendanceStatusResponse toResponse(RefAttendanceStatus entity) {
    if (entity == null) {
      return null;
    }

    return new AttendanceStatusResponse(
        entity.getId(),
        entity.getStatusCode(),
        entity.getStatusName(),
        entity.getCountsAsPresent(),
        entity.getSortOrder(),
        entity.getCreatedAt(),
        entity.getUpdatedAt()
    );
  }
}
