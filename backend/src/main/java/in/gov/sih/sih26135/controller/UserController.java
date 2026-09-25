package in.gov.sih.sih26135.controller;

import in.gov.sih.sih26135.dto.request.AssignUserRoleRequest;
import in.gov.sih.sih26135.dto.request.CreateUserRequest;
import in.gov.sih.sih26135.dto.request.UpdateUserRequest;
import in.gov.sih.sih26135.dto.response.UserResponse;
import in.gov.sih.sih26135.dto.response.UserRoleResponse;
import in.gov.sih.sih26135.response.ApiResponse;
import in.gov.sih.sih26135.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.security.access.prepost.PreAuthorize;

/**
 * REST controller for managing User domain resources.
 *
 * <p>Base Route: /api/v1/users
 * Consumes: CreateUserRequest, UpdateUserRequest, AssignUserRoleRequest
 * Produces: UserResponse, UserRoleResponse enveloped in ApiResponse
 */
@RestController
@RequestMapping("/api/v1/users")
@PreAuthorize("hasAuthority('user.manage')")
public class UserController {

  private final UserService userService;

  public UserController(UserService userService) {
    this.userService = userService;
  }

  /**
   * Retrieves a user by primary key identifier.
   *
   * @param id primary key identifier of the user
   * @return 200 OK with UserResponse enveloped in ApiResponse
   */
  @GetMapping("/{id}")
  public ResponseEntity<ApiResponse<UserResponse>> getById(@PathVariable Long id) {
    UserResponse response = userService.getById(id);
    return ResponseEntity.ok(ApiResponse.ok(response));
  }

  /**
   * Retrieves users with optional filtering by username or email.
   *
   * @param username optional username filter
   * @param email optional email filter
   * @return 200 OK with list of users or single matched user enveloped in ApiResponse
   */
  @GetMapping
  public ResponseEntity<ApiResponse<?>> getUsers(
      @RequestParam(value = "username", required = false) String username,
      @RequestParam(value = "email", required = false) String email) {
    if (username != null && !username.isBlank()) {
      UserResponse response = userService.getByUsername(username.trim());
      return ResponseEntity.ok(ApiResponse.ok(response));
    }
    if (email != null && !email.isBlank()) {
      UserResponse response = userService.getByEmail(email.trim());
      return ResponseEntity.ok(ApiResponse.ok(response));
    }
    List<UserResponse> users = userService.getAllUsers();
    return ResponseEntity.ok(ApiResponse.ok(users));
  }

  /**
   * Creates a new user record.
   *
   * @param request user creation payload
   * @param actorUserId optional ID of the actor creating the user
   * @param httpRequest HTTP servlet request for URI extraction
   * @return 201 Created with created UserResponse enveloped in ApiResponse
   */
  @PostMapping
  public ResponseEntity<ApiResponse<UserResponse>> createUser(
      @RequestBody CreateUserRequest request,
      @RequestHeader(value = "X-Actor-User-Id", required = false) Long actorUserId,
      HttpServletRequest httpRequest) {
    UserResponse response = userService.createUser(request, actorUserId);
    return ResponseEntity.status(HttpStatus.CREATED)
        .body(ApiResponse.success("User created successfully", response, httpRequest.getRequestURI()));
  }

  /**
   * Updates an existing user record.
   *
   * @param id primary key identifier of the user to update
   * @param request user update payload
   * @param actorUserId optional ID of the actor performing the update
   * @return 200 OK with updated UserResponse enveloped in ApiResponse
   */
  @PutMapping("/{id}")
  public ResponseEntity<ApiResponse<UserResponse>> updateUser(
      @PathVariable Long id,
      @RequestBody UpdateUserRequest request,
      @RequestHeader(value = "X-Actor-User-Id", required = false) Long actorUserId) {
    UserResponse response = userService.updateUser(id, request, actorUserId);
    return ResponseEntity.ok(ApiResponse.success("User updated successfully", response));
  }

  /**
   * Soft-deletes a user record.
   *
   * @param id primary key identifier of the user to delete
   * @param actorUserId optional ID of the actor performing the deletion
   * @return 200 OK with success confirmation message
   */
  @DeleteMapping("/{id}")
  public ResponseEntity<ApiResponse<Void>> deleteUser(
      @PathVariable Long id,
      @RequestHeader(value = "X-Actor-User-Id", required = false) Long actorUserId) {
    userService.deleteUser(id, actorUserId);
    return ResponseEntity.ok(ApiResponse.success("User deleted successfully"));
  }

  /**
   * Retrieves all roles assigned to a specific user.
   *
   * @param userId primary key identifier of the user
   * @return 200 OK with list of UserRoleResponse enveloped in ApiResponse
   */
  @GetMapping("/{userId}/roles")
  public ResponseEntity<ApiResponse<List<UserRoleResponse>>> getUserRoles(@PathVariable Long userId) {
    List<UserRoleResponse> roles = userService.getUserRoles(userId);
    return ResponseEntity.ok(ApiResponse.ok(roles));
  }

  /**
   * Assigns a role to a specific user.
   *
   * @param userId primary key identifier of the user
   * @param request role assignment payload
   * @param actorUserId optional ID of the actor assigning the role
   * @param httpRequest HTTP servlet request for URI extraction
   * @return 201 Created with UserRoleResponse enveloped in ApiResponse
   */
  @PostMapping("/{userId}/roles")
  public ResponseEntity<ApiResponse<UserRoleResponse>> assignRole(
      @PathVariable Long userId,
      @RequestBody AssignUserRoleRequest request,
      @RequestHeader(value = "X-Actor-User-Id", required = false) Long actorUserId,
      HttpServletRequest httpRequest) {
    UserRoleResponse response = userService.assignRole(userId, request, actorUserId);
    return ResponseEntity.status(HttpStatus.CREATED)
        .body(ApiResponse.success("Role assigned successfully", response, httpRequest.getRequestURI()));
  }

  /**
   * Removes an assigned role from a specific user.
   *
   * @param userId primary key identifier of the user
   * @param roleId primary key identifier of the role to remove
   * @param actorUserId optional ID of the actor removing the role
   * @return 200 OK with success confirmation message
   */
  @DeleteMapping("/{userId}/roles/{roleId}")
  public ResponseEntity<ApiResponse<Void>> removeRole(
      @PathVariable Long userId,
      @PathVariable Long roleId,
      @RequestHeader(value = "X-Actor-User-Id", required = false) Long actorUserId) {
    userService.removeRole(userId, roleId, actorUserId);
    return ResponseEntity.ok(ApiResponse.success("Role removed successfully"));
  }
}
