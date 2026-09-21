package in.gov.sih.sih26135.service.impl;

import in.gov.sih.sih26135.dto.request.CreatePermissionRequest;
import in.gov.sih.sih26135.dto.response.PermissionResponse;
import in.gov.sih.sih26135.entity.Permission;
import in.gov.sih.sih26135.exception.BadRequestException;
import in.gov.sih.sih26135.exception.ConflictException;
import in.gov.sih.sih26135.exception.ResourceNotFoundException;
import in.gov.sih.sih26135.mapper.PermissionMapper;
import in.gov.sih.sih26135.repository.PermissionRepository;
import in.gov.sih.sih26135.service.PermissionService;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class PermissionServiceImpl implements PermissionService {

  private final PermissionRepository permissionRepository;
  private final PermissionMapper permissionMapper;

  public PermissionServiceImpl(
      PermissionRepository permissionRepository,
      PermissionMapper permissionMapper) {
    this.permissionRepository = permissionRepository;
    this.permissionMapper = permissionMapper;
  }

  @Override
  public PermissionResponse getById(Long id) {
    Permission permission = permissionRepository.findById(id)
        .orElseThrow(() -> new ResourceNotFoundException("Permission", "id"));
    return permissionMapper.toResponse(permission);
  }

  @Override
  public PermissionResponse getByCode(String permissionCode) {
    Permission permission = permissionRepository.findByPermissionCode(permissionCode)
        .orElseThrow(() -> new ResourceNotFoundException("Permission", "permissionCode"));
    return permissionMapper.toResponse(permission);
  }

  @Override
  public List<PermissionResponse> getAllPermissions() {
    return permissionRepository.findAll().stream()
        .map(permissionMapper::toResponse)
        .toList();
  }

  @Override
  public List<PermissionResponse> getByModuleCode(String moduleCode) {
    return permissionRepository.findByModuleCode(moduleCode).stream()
        .map(permissionMapper::toResponse)
        .toList();
  }

  @Override
  @Transactional
  public PermissionResponse createPermission(CreatePermissionRequest request) {
    if (request == null) {
      throw new BadRequestException("Permission creation request cannot be null");
    }
    if (request.getPermissionCode() == null || request.getPermissionCode().isBlank()) {
      throw new BadRequestException("Permission code is required");
    }
    if (request.getPermissionName() == null || request.getPermissionName().isBlank()) {
      throw new BadRequestException("Permission name is required");
    }
    if (request.getModuleCode() == null || request.getModuleCode().isBlank()) {
      throw new BadRequestException("Module code is required");
    }

    if (permissionRepository.existsByPermissionCode(request.getPermissionCode().trim())) {
      throw new ConflictException("Permission code already exists", "PERMISSION_CODE_ALREADY_EXISTS");
    }

    Permission permission = permissionMapper.toEntity(request);
    permission.setPermissionCode(request.getPermissionCode().trim());
    permission.setPermissionName(request.getPermissionName().trim());
    permission.setModuleCode(request.getModuleCode().trim());
    LocalDateTime now = LocalDateTime.now();
    permission.setCreatedAt(now);
    permission.setUpdatedAt(now);

    Permission saved = permissionRepository.save(permission);
    return permissionMapper.toResponse(saved);
  }
}
