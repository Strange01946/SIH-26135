package in.gov.sih.sih26135.mapper;

import in.gov.sih.sih26135.dto.request.CreateRoleRequest;
import in.gov.sih.sih26135.dto.response.RoleResponse;
import in.gov.sih.sih26135.entity.Role;
import org.springframework.stereotype.Component;

@Component
public class RoleMapper {

  public RoleResponse toResponse(Role entity) {
    if (entity == null) {
      return null;
    }

    return new RoleResponse(
        entity.getId(),
        entity.getRoleCode(),
        entity.getRoleName(),
        entity.getDescription(),
        entity.getIsSystemRole(),
        entity.getLifecycleStatusId(),
        entity.getCreatedAt(),
        entity.getUpdatedAt(),
        entity.getDeletedAt()
    );
  }

  public Role toEntity(CreateRoleRequest request) {
    if (request == null) {
      return null;
    }

    Role role = new Role();
    role.setRoleCode(request.getRoleCode());
    role.setRoleName(request.getRoleName());
    role.setDescription(request.getDescription());
    role.setIsSystemRole(request.getIsSystemRole() != null ? request.getIsSystemRole() : true);
    role.setLifecycleStatusId(request.getLifecycleStatusId());
    return role;
  }
}
