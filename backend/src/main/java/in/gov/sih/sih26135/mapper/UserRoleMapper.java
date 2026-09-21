package in.gov.sih.sih26135.mapper;

import in.gov.sih.sih26135.dto.response.UserRoleResponse;
import in.gov.sih.sih26135.entity.Role;
import in.gov.sih.sih26135.entity.UserRole;
import org.springframework.stereotype.Component;

@Component
public class UserRoleMapper {

  public UserRoleResponse toResponse(UserRole entity, Role role) {
    if (entity == null) {
      return null;
    }

    return new UserRoleResponse(
        entity.getUserId(),
        entity.getRoleId(),
        role != null ? role.getRoleCode() : null,
        role != null ? role.getRoleName() : null,
        entity.getAssignedAt(),
        entity.getAssignedByUserId()
    );
  }
}
