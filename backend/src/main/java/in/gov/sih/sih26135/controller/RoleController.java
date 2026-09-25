package in.gov.sih.sih26135.controller;

import in.gov.sih.sih26135.dto.request.AssignRolePermissionRequest;
import in.gov.sih.sih26135.dto.request.CreateRoleRequest;
import in.gov.sih.sih26135.dto.request.UpdateRoleRequest;
import in.gov.sih.sih26135.dto.response.RolePermissionResponse;
import in.gov.sih.sih26135.dto.response.RoleResponse;
import in.gov.sih.sih26135.response.ApiResponse;
import in.gov.sih.sih26135.service.RoleService;
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
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.security.access.prepost.PreAuthorize;

/**
 * REST controller for managing Role domain resources and role-permission mappings.
 *
 * <p>Base Route: /api/v1/roles
 * Consumes: CreateRoleRequest, UpdateRoleRequest, AssignRolePermissionRequest
 * Produces: RoleResponse, RolePermissionResponse enveloped in ApiResponse
 */
@RestController
@RequestMapping("/api/v1/roles")
@PreAuthorize("hasAuthority('user.manage')")
public class RoleController {

  private final RoleService roleService;

  public RoleController(RoleService roleService) {
    this.roleService = roleService;
  }

  /**
   * Retrieves a role by primary key identifier.
   *
   * @param id primary key identifier of the role
   * @return 200 OK with RoleResponse enveloped in ApiResponse
   */
  @GetMapping("/{id}")
  public ResponseEntity<ApiResponse<RoleResponse>> getById(@PathVariable Long id) {
    RoleResponse response = roleService.getById(id);
    return ResponseEntity.ok(ApiResponse.ok(response));
  }

  /**
   * Retrieves roles with optional filtering by role code.
   *
   * @param code optional role code filter
   * @return 200 OK with list of roles or single matched role enveloped in ApiResponse
   */
  @GetMapping
  public ResponseEntity<ApiResponse<?>> getRoles(
      @RequestParam(value = "code", required = false) String code) {
    if (code != null && !code.isBlank()) {
      RoleResponse response = roleService.getByCode(code.trim());
      return ResponseEntity.ok(ApiResponse.ok(response));
    }
    List<RoleResponse> roles = roleService.getAllRoles();
    return ResponseEntity.ok(ApiResponse.ok(roles));
  }

  /**
   * Creates a new role record.
   *
   * @param request role creation payload
   * @param httpRequest HTTP servlet request for URI extraction
   * @return 201 Created with created RoleResponse enveloped in ApiResponse
   */
  @PostMapping
  public ResponseEntity<ApiResponse<RoleResponse>> createRole(
      @RequestBody CreateRoleRequest request,
      HttpServletRequest httpRequest) {
    RoleResponse response = roleService.createRole(request);
    return ResponseEntity.status(HttpStatus.CREATED)
        .body(ApiResponse.success("Role created successfully", response, httpRequest.getRequestURI()));
  }

  /**
   * Updates an existing role record.
   *
   * @param id primary key identifier of the role to update
   * @param request role update payload
   * @return 200 OK with updated RoleResponse enveloped in ApiResponse
   */
  @PutMapping("/{id}")
  public ResponseEntity<ApiResponse<RoleResponse>> updateRole(
      @PathVariable Long id,
      @RequestBody UpdateRoleRequest request) {
    RoleResponse response = roleService.updateRole(id, request);
    return ResponseEntity.ok(ApiResponse.success("Role updated successfully", response));
  }

  /**
   * Retrieves all permissions assigned to a specific role.
   *
   * @param roleId primary key identifier of the role
   * @return 200 OK with list of RolePermissionResponse enveloped in ApiResponse
   */
  @GetMapping("/{roleId}/permissions")
  public ResponseEntity<ApiResponse<List<RolePermissionResponse>>> getRolePermissions(
      @PathVariable Long roleId) {
    List<RolePermissionResponse> permissions = roleService.getRolePermissions(roleId);
    return ResponseEntity.ok(ApiResponse.ok(permissions));
  }

  /**
   * Assigns a permission to a specific role.
   *
   * @param roleId primary key identifier of the role
   * @param request role-permission assignment payload
   * @param httpRequest HTTP servlet request for URI extraction
   * @return 201 Created with RolePermissionResponse enveloped in ApiResponse
   */
  @PostMapping("/{roleId}/permissions")
  public ResponseEntity<ApiResponse<RolePermissionResponse>> assignPermission(
      @PathVariable Long roleId,
      @RequestBody AssignRolePermissionRequest request,
      HttpServletRequest httpRequest) {
    RolePermissionResponse response = roleService.assignPermission(roleId, request);
    return ResponseEntity.status(HttpStatus.CREATED)
        .body(ApiResponse.success("Permission assigned to role successfully", response, httpRequest.getRequestURI()));
  }

  /**
   * Removes an assigned permission from a specific role.
   *
   * @param roleId primary key identifier of the role
   * @param permissionId primary key identifier of the permission to remove
   * @return 200 OK with success confirmation message
   */
  @DeleteMapping("/{roleId}/permissions/{permissionId}")
  public ResponseEntity<ApiResponse<Void>> removePermission(
      @PathVariable Long roleId,
      @PathVariable Long permissionId) {
    roleService.removePermission(roleId, permissionId);
    return ResponseEntity.ok(ApiResponse.success("Permission removed from role successfully"));
  }
}
