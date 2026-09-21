package in.gov.sih.sih26135.service;

import in.gov.sih.sih26135.dto.request.AssignUserRoleRequest;
import in.gov.sih.sih26135.dto.request.CreateUserRequest;
import in.gov.sih.sih26135.dto.request.UpdateUserRequest;
import in.gov.sih.sih26135.dto.response.UserResponse;
import in.gov.sih.sih26135.dto.response.UserRoleResponse;
import java.util.List;

public interface UserService {

  UserResponse getById(Long id);

  UserResponse getByUsername(String username);

  UserResponse getByEmail(String email);

  List<UserResponse> getAllUsers();

  UserResponse createUser(CreateUserRequest request, Long actorUserId);

  UserResponse updateUser(Long id, UpdateUserRequest request, Long actorUserId);

  void deleteUser(Long id, Long actorUserId);

  UserRoleResponse assignRole(Long userId, AssignUserRoleRequest request, Long actorUserId);

  void removeRole(Long userId, Long roleId, Long actorUserId);

  List<UserRoleResponse> getUserRoles(Long userId);
}
