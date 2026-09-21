package in.gov.sih.sih26135.mapper;

import in.gov.sih.sih26135.dto.request.CreatePermissionRequest;
import in.gov.sih.sih26135.dto.response.PermissionResponse;
import in.gov.sih.sih26135.entity.Permission;
import org.springframework.stereotype.Component;

@Component
public class PermissionMapper {

  public PermissionResponse toResponse(Permission entity) {
    if (entity == null) {
      return null;
    }

    return new PermissionResponse(
        entity.getId(),
        entity.getPermissionCode(),
        entity.getPermissionName(),
        entity.getModuleCode(),
        entity.getDescription(),
        entity.getCreatedAt(),
        entity.getUpdatedAt()
    );
  }

  public Permission toEntity(CreatePermissionRequest request) {
    if (request == null) {
      return null;
    }

    Permission permission = new Permission();
    permission.setPermissionCode(request.getPermissionCode());
    permission.setPermissionName(request.getPermissionName());
    permission.setModuleCode(request.getModuleCode());
    permission.setDescription(request.getDescription());
    return permission;
  }
}
