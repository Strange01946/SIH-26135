package in.gov.sih.sih26135.service;

import in.gov.sih.sih26135.dto.request.AssignRolePermissionRequest;
import in.gov.sih.sih26135.dto.request.CreateRoleRequest;
import in.gov.sih.sih26135.dto.request.UpdateRoleRequest;
import in.gov.sih.sih26135.dto.response.RolePermissionResponse;
import in.gov.sih.sih26135.dto.response.RoleResponse;
import java.util.List;

public interface RoleService {

  RoleResponse getById(Long id);

  RoleResponse getByCode(String roleCode);

  List<RoleResponse> getAllRoles();

  RoleResponse createRole(CreateRoleRequest request);

  RoleResponse updateRole(Long id, UpdateRoleRequest request);

  RolePermissionResponse assignPermission(Long roleId, AssignRolePermissionRequest request);

  void removePermission(Long roleId, Long permissionId);

  List<RolePermissionResponse> getRolePermissions(Long roleId);
}
