package in.gov.sih.sih26135.mapper;

import in.gov.sih.sih26135.dto.response.RolePermissionResponse;
import in.gov.sih.sih26135.entity.Permission;
import in.gov.sih.sih26135.entity.RolePermission;
import org.springframework.stereotype.Component;

@Component
public class RolePermissionMapper {

  public RolePermissionResponse toResponse(RolePermission entity, Permission permission) {
    if (entity == null) {
      return null;
    }

    return new RolePermissionResponse(
        entity.getRoleId(),
        entity.getPermissionId(),
        permission != null ? permission.getPermissionCode() : null,
        permission != null ? permission.getPermissionName() : null,
        permission != null ? permission.getModuleCode() : null,
        entity.getGrantedAt()
    );
  }
}
