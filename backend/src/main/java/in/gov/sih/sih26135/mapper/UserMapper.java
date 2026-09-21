package in.gov.sih.sih26135.mapper;

import in.gov.sih.sih26135.dto.request.CreateUserRequest;
import in.gov.sih.sih26135.dto.response.UserResponse;
import in.gov.sih.sih26135.entity.User;
import org.springframework.stereotype.Component;

@Component
public class UserMapper {

  public UserResponse toResponse(User entity, String userStatusCode) {
    if (entity == null) {
      return null;
    }

    return new UserResponse(
        entity.getId(),
        entity.getUsername(),
        entity.getEmail(),
        entity.getUserStatusId(),
        userStatusCode,
        entity.getLastLoginAt(),
        entity.getFailedLoginCount(),
        entity.getLockedUntil(),
        entity.getMustChangePassword(),
        entity.getOrganizationId(),
        entity.getDepartmentId(),
        entity.getStateId(),
        entity.getDistrictId(),
        entity.getTrainingProviderId(),
        entity.getEmployerId(),
        entity.getCreatedByUserId(),
        entity.getCreatedAt(),
        entity.getUpdatedAt(),
        entity.getDeletedAt()
    );
  }

  public User toEntity(CreateUserRequest request) {
    if (request == null) {
      return null;
    }

    User user = new User();
    user.setUsername(request.getUsername());
    user.setEmail(request.getEmail());
    user.setPasswordHash(request.getPasswordHash());
    if (request.getPasswordAlgo() != null && !request.getPasswordAlgo().isBlank()) {
      user.setPasswordAlgo(request.getPasswordAlgo());
    }
    user.setUserStatusId(request.getUserStatusId());
    user.setOrganizationId(request.getOrganizationId());
    user.setDepartmentId(request.getDepartmentId());
    user.setStateId(request.getStateId());
    user.setDistrictId(request.getDistrictId());
    user.setTrainingProviderId(request.getTrainingProviderId());
    user.setEmployerId(request.getEmployerId());
    return user;
  }
}
