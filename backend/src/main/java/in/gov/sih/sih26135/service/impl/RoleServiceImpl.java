package in.gov.sih.sih26135.service.impl;

import in.gov.sih.sih26135.dto.request.AssignRolePermissionRequest;
import in.gov.sih.sih26135.dto.request.CreateRoleRequest;
import in.gov.sih.sih26135.dto.request.UpdateRoleRequest;
import in.gov.sih.sih26135.dto.response.RolePermissionResponse;
import in.gov.sih.sih26135.dto.response.RoleResponse;
import in.gov.sih.sih26135.entity.Permission;
import in.gov.sih.sih26135.entity.Role;
import in.gov.sih.sih26135.entity.RolePermission;
import in.gov.sih.sih26135.entity.RolePermissionId;
import in.gov.sih.sih26135.exception.BadRequestException;
import in.gov.sih.sih26135.exception.ConflictException;
import in.gov.sih.sih26135.exception.ResourceNotFoundException;
import in.gov.sih.sih26135.mapper.RoleMapper;
import in.gov.sih.sih26135.mapper.RolePermissionMapper;
import in.gov.sih.sih26135.repository.PermissionRepository;
import in.gov.sih.sih26135.repository.RolePermissionRepository;
import in.gov.sih.sih26135.repository.RoleRepository;
import in.gov.sih.sih26135.service.RoleService;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class RoleServiceImpl implements RoleService {

  private final RoleRepository roleRepository;
  private final PermissionRepository permissionRepository;
  private final RolePermissionRepository rolePermissionRepository;
  private final RoleMapper roleMapper;
  private final RolePermissionMapper rolePermissionMapper;

  public RoleServiceImpl(
      RoleRepository roleRepository,
      PermissionRepository permissionRepository,
      RolePermissionRepository rolePermissionRepository,
      RoleMapper roleMapper,
      RolePermissionMapper rolePermissionMapper) {
    this.roleRepository = roleRepository;
    this.permissionRepository = permissionRepository;
    this.rolePermissionRepository = rolePermissionRepository;
    this.roleMapper = roleMapper;
    this.rolePermissionMapper = rolePermissionMapper;
  }

  @Override
  public RoleResponse getById(Long id) {
    Role role = roleRepository.findById(id)
        .orElseThrow(() -> new ResourceNotFoundException("Role", "id"));
    return roleMapper.toResponse(role);
  }

  @Override
  public RoleResponse getByCode(String roleCode) {
    Role role = roleRepository.findByRoleCode(roleCode)
        .orElseThrow(() -> new ResourceNotFoundException("Role", "roleCode"));
    return roleMapper.toResponse(role);
  }

  @Override
  public List<RoleResponse> getAllRoles() {
    return roleRepository.findAll().stream()
        .map(roleMapper::toResponse)
        .toList();
  }

  @Override
  @Transactional
  public RoleResponse createRole(CreateRoleRequest request) {
    if (request == null) {
      throw new BadRequestException("Role creation request cannot be null");
    }
    if (request.getRoleCode() == null || request.getRoleCode().isBlank()) {
      throw new BadRequestException("Role code is required");
    }
    if (request.getRoleName() == null || request.getRoleName().isBlank()) {
      throw new BadRequestException("Role name is required");
    }
    if (request.getLifecycleStatusId() == null) {
      throw new BadRequestException("Lifecycle status ID is required");
    }

    if (roleRepository.existsByRoleCode(request.getRoleCode().trim())) {
      throw new ConflictException("Role code already exists", "ROLE_CODE_ALREADY_EXISTS");
    }

    Role role = roleMapper.toEntity(request);
    role.setRoleCode(request.getRoleCode().trim());
    role.setRoleName(request.getRoleName().trim());
    LocalDateTime now = LocalDateTime.now();
    role.setCreatedAt(now);
    role.setUpdatedAt(now);

    Role saved = roleRepository.save(role);
    return roleMapper.toResponse(saved);
  }

  @Override
  @Transactional
  public RoleResponse updateRole(Long id, UpdateRoleRequest request) {
    if (request == null) {
      throw new BadRequestException("Role update request cannot be null");
    }

    Role role = roleRepository.findById(id)
        .orElseThrow(() -> new ResourceNotFoundException("Role", "id"));

    if (request.getRoleName() != null && !request.getRoleName().isBlank()) {
      role.setRoleName(request.getRoleName().trim());
    }
    if (request.getDescription() != null) {
      role.setDescription(request.getDescription());
    }
    if (request.getIsSystemRole() != null) {
      role.setIsSystemRole(request.getIsSystemRole());
    }
    if (request.getLifecycleStatusId() != null) {
      role.setLifecycleStatusId(request.getLifecycleStatusId());
    }

    role.setUpdatedAt(LocalDateTime.now());
    Role saved = roleRepository.save(role);
    return roleMapper.toResponse(saved);
  }

  @Override
  @Transactional
  public RolePermissionResponse assignPermission(Long roleId, AssignRolePermissionRequest request) {
    if (request == null || request.getPermissionId() == null) {
      throw new BadRequestException("Permission ID is required for assignment");
    }

    if (!roleRepository.existsById(roleId)) {
      throw new ResourceNotFoundException("Role", "id");
    }

    Permission permission = permissionRepository.findById(request.getPermissionId())
        .orElseThrow(() -> new ResourceNotFoundException("Permission", "id"));

    if (rolePermissionRepository.existsByRoleIdAndPermissionId(roleId, request.getPermissionId())) {
      throw new ConflictException("Permission already assigned to role", "PERMISSION_ALREADY_ASSIGNED");
    }

    RolePermission rp = new RolePermission(roleId, request.getPermissionId());
    rp.setGrantedAt(LocalDateTime.now());
    RolePermission saved = rolePermissionRepository.save(rp);

    return rolePermissionMapper.toResponse(saved, permission);
  }

  @Override
  @Transactional
  public void removePermission(Long roleId, Long permissionId) {
    if (!roleRepository.existsById(roleId)) {
      throw new ResourceNotFoundException("Role", "id");
    }
    if (!permissionRepository.existsById(permissionId)) {
      throw new ResourceNotFoundException("Permission", "id");
    }

    RolePermissionId rpId = new RolePermissionId(roleId, permissionId);
    if (!rolePermissionRepository.existsById(rpId)) {
      throw new ResourceNotFoundException("RolePermission assignment", "roleId and permissionId");
    }

    rolePermissionRepository.deleteById(rpId);
  }

  @Override
  public List<RolePermissionResponse> getRolePermissions(Long roleId) {
    if (!roleRepository.existsById(roleId)) {
      throw new ResourceNotFoundException("Role", "id");
    }

    List<RolePermission> assignments = rolePermissionRepository.findByRoleId(roleId);
    return assignments.stream()
        .map(rp -> {
          Permission permission = permissionRepository.findById(rp.getPermissionId()).orElse(null);
          return rolePermissionMapper.toResponse(rp, permission);
        })
        .toList();
  }
}
