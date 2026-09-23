package in.gov.sih.sih26135.mapper;

import in.gov.sih.sih26135.dto.response.AuditActionResponse;
import in.gov.sih.sih26135.entity.RefAuditAction;
import org.springframework.stereotype.Component;

@Component
public class AuditActionMapper {

  public AuditActionResponse toResponse(RefAuditAction entity) {
    if (entity == null) {
      return null;
    }
    return new AuditActionResponse(
        entity.getId(),
        entity.getActionCode(),
        entity.getActionName(),
        entity.getSortOrder(),
        entity.getCreatedAt(),
        entity.getUpdatedAt()
    );
  }
}
