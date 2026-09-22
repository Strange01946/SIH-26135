package in.gov.sih.sih26135.mapper;

import in.gov.sih.sih26135.dto.response.JobPostingStatusResponse;
import in.gov.sih.sih26135.entity.RefJobPostingStatus;
import org.springframework.stereotype.Component;

@Component
public class JobPostingStatusMapper {

  public JobPostingStatusResponse toResponse(RefJobPostingStatus entity) {
    if (entity == null) {
      return null;
    }
    return new JobPostingStatusResponse(
        entity.getId(),
        entity.getStatusCode(),
        entity.getStatusName(),
        entity.getIsOpenFlag(),
        entity.getSortOrder(),
        entity.getCreatedAt(),
        entity.getUpdatedAt()
    );
  }
}
