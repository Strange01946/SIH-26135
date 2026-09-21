package in.gov.sih.sih26135.service.impl;

import in.gov.sih.sih26135.dto.request.AssignUserRoleRequest;
import in.gov.sih.sih26135.dto.request.CreateUserRequest;
import in.gov.sih.sih26135.dto.request.UpdateUserRequest;
import in.gov.sih.sih26135.dto.response.UserResponse;
import in.gov.sih.sih26135.dto.response.UserRoleResponse;
import in.gov.sih.sih26135.entity.RefUserStatus;
import in.gov.sih.sih26135.entity.Role;
import in.gov.sih.sih26135.entity.User;
import in.gov.sih.sih26135.entity.UserRole;
import in.gov.sih.sih26135.entity.UserRoleId;
import in.gov.sih.sih26135.exception.BadRequestException;
import in.gov.sih.sih26135.exception.ConflictException;
import in.gov.sih.sih26135.exception.ResourceNotFoundException;
import in.gov.sih.sih26135.mapper.UserMapper;
import in.gov.sih.sih26135.mapper.UserRoleMapper;
import in.gov.sih.sih26135.repository.RefUserStatusRepository;
import in.gov.sih.sih26135.repository.RoleRepository;
import in.gov.sih.sih26135.repository.UserRepository;
import in.gov.sih.sih26135.repository.UserRoleRepository;
import in.gov.sih.sih26135.service.UserService;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class UserServiceImpl implements UserService {

  private final UserRepository userRepository;
  private final RefUserStatusRepository refUserStatusRepository;
  private final UserRoleRepository userRoleRepository;
  private final RoleRepository roleRepository;
  private final UserMapper userMapper;
  private final UserRoleMapper userRoleMapper;

  public UserServiceImpl(
      UserRepository userRepository,
      RefUserStatusRepository refUserStatusRepository,
      UserRoleRepository userRoleRepository,
      RoleRepository roleRepository,
      UserMapper userMapper,
      UserRoleMapper userRoleMapper) {
    this.userRepository = userRepository;
    this.refUserStatusRepository = refUserStatusRepository;
    this.userRoleRepository = userRoleRepository;
    this.roleRepository = roleRepository;
    this.userMapper = userMapper;
    this.userRoleMapper = userRoleMapper;
  }

  @Override
  public UserResponse getById(Long id) {
    User user = userRepository.findById(id)
        .orElseThrow(() -> new ResourceNotFoundException("User", "id"));
    return userMapper.toResponse(user, resolveStatusCode(user.getUserStatusId()));
  }

  @Override
  public UserResponse getByUsername(String username) {
    User user = userRepository.findByUsername(username)
        .orElseThrow(() -> new ResourceNotFoundException("User", "username"));
    return userMapper.toResponse(user, resolveStatusCode(user.getUserStatusId()));
  }

  @Override
  public UserResponse getByEmail(String email) {
    User user = userRepository.findByEmail(email)
        .orElseThrow(() -> new ResourceNotFoundException("User", "email"));
    return userMapper.toResponse(user, resolveStatusCode(user.getUserStatusId()));
  }

  @Override
  public List<UserResponse> getAllUsers() {
    return userRepository.findAll().stream()
        .map(u -> userMapper.toResponse(u, resolveStatusCode(u.getUserStatusId())))
        .toList();
  }

  @Override
  @Transactional
  public UserResponse createUser(CreateUserRequest request, Long actorUserId) {
    if (request == null) {
      throw new BadRequestException("User creation request cannot be null");
    }
    if (request.getUsername() == null || request.getUsername().isBlank()) {
      throw new BadRequestException("Username is required");
    }
    if (request.getEmail() == null || request.getEmail().isBlank()) {
      throw new BadRequestException("Email is required");
    }
    if (request.getPasswordHash() == null || request.getPasswordHash().isBlank()) {
      throw new BadRequestException("Password hash is required");
    }
    if (request.getUserStatusId() == null) {
      throw new BadRequestException("User status ID is required");
    }

    if (userRepository.existsByUsername(request.getUsername().trim())) {
      throw new ConflictException("Username already exists", "USERNAME_ALREADY_EXISTS");
    }
    if (userRepository.existsByEmail(request.getEmail().trim())) {
      throw new ConflictException("Email already exists", "EMAIL_ALREADY_EXISTS");
    }
    if (!refUserStatusRepository.existsById(request.getUserStatusId())) {
      throw new BadRequestException("Invalid user status ID: status does not exist", "INVALID_USER_STATUS");
    }

    User user = userMapper.toEntity(request);
    user.setUsername(request.getUsername().trim());
    user.setEmail(request.getEmail().trim());
    LocalDateTime now = LocalDateTime.now();
    user.setCreatedAt(now);
    user.setUpdatedAt(now);
    user.setCreatedByUserId(actorUserId);

    User saved = userRepository.save(user);
    return userMapper.toResponse(saved, resolveStatusCode(saved.getUserStatusId()));
  }

  @Override
  @Transactional
  public UserResponse updateUser(Long id, UpdateUserRequest request, Long actorUserId) {
    if (request == null) {
      throw new BadRequestException("User update request cannot be null");
    }

    User user = userRepository.findById(id)
        .orElseThrow(() -> new ResourceNotFoundException("User", "id"));

    if (request.getEmail() != null && !request.getEmail().isBlank()) {
      String newEmail = request.getEmail().trim();
      if (!newEmail.equalsIgnoreCase(user.getEmail())) {
        if (userRepository.existsByEmail(newEmail)) {
          throw new ConflictException("Email already exists", "EMAIL_ALREADY_EXISTS");
        }
        user.setEmail(newEmail);
      }
    }

    if (request.getUserStatusId() != null && !request.getUserStatusId().equals(user.getUserStatusId())) {
      if (!refUserStatusRepository.existsById(request.getUserStatusId())) {
        throw new BadRequestException("Invalid user status ID: status does not exist", "INVALID_USER_STATUS");
      }
      user.setUserStatusId(request.getUserStatusId());
    }

    if (request.getOrganizationId() != null) {
      user.setOrganizationId(request.getOrganizationId());
    }
    if (request.getDepartmentId() != null) {
      user.setDepartmentId(request.getDepartmentId());
    }
    if (request.getStateId() != null) {
      user.setStateId(request.getStateId());
    }
    if (request.getDistrictId() != null) {
      user.setDistrictId(request.getDistrictId());
    }
    if (request.getTrainingProviderId() != null) {
      user.setTrainingProviderId(request.getTrainingProviderId());
    }
    if (request.getEmployerId() != null) {
      user.setEmployerId(request.getEmployerId());
    }

    user.setUpdatedAt(LocalDateTime.now());
    User saved = userRepository.save(user);
    return userMapper.toResponse(saved, resolveStatusCode(saved.getUserStatusId()));
  }

  @Override
  @Transactional
  public void deleteUser(Long id, Long actorUserId) {
    User user = userRepository.findById(id)
        .orElseThrow(() -> new ResourceNotFoundException("User", "id"));

    LocalDateTime now = LocalDateTime.now();
    user.setDeletedAt(now);
    user.setUpdatedAt(now);
    userRepository.save(user);
  }

  @Override
  @Transactional
  public UserRoleResponse assignRole(Long userId, AssignUserRoleRequest request, Long actorUserId) {
    if (request == null || request.getRoleId() == null) {
      throw new BadRequestException("Role ID is required for assignment");
    }

    if (!userRepository.existsById(userId)) {
      throw new ResourceNotFoundException("User", "id");
    }

    Role role = roleRepository.findById(request.getRoleId())
        .orElseThrow(() -> new ResourceNotFoundException("Role", "id"));

    if (userRoleRepository.existsByUserIdAndRoleId(userId, request.getRoleId())) {
      throw new ConflictException("Role already assigned to user", "ROLE_ALREADY_ASSIGNED");
    }

    UserRole userRole = new UserRole(userId, request.getRoleId(), actorUserId);
    userRole.setAssignedAt(LocalDateTime.now());
    UserRole saved = userRoleRepository.save(userRole);

    return userRoleMapper.toResponse(saved, role);
  }

  @Override
  @Transactional
  public void removeRole(Long userId, Long roleId, Long actorUserId) {
    if (!userRepository.existsById(userId)) {
      throw new ResourceNotFoundException("User", "id");
    }
    if (!roleRepository.existsById(roleId)) {
      throw new ResourceNotFoundException("Role", "id");
    }

    UserRoleId userRoleId = new UserRoleId(userId, roleId);
    if (!userRoleRepository.existsById(userRoleId)) {
      throw new ResourceNotFoundException("UserRole assignment", "userId and roleId");
    }

    userRoleRepository.deleteById(userRoleId);
  }

  @Override
  public List<UserRoleResponse> getUserRoles(Long userId) {
    if (!userRepository.existsById(userId)) {
      throw new ResourceNotFoundException("User", "id");
    }

    List<UserRole> assignments = userRoleRepository.findByUserId(userId);
    return assignments.stream()
        .map(ur -> {
          Role role = roleRepository.findById(ur.getRoleId()).orElse(null);
          return userRoleMapper.toResponse(ur, role);
        })
        .toList();
  }

  private String resolveStatusCode(Long userStatusId) {
    if (userStatusId == null) {
      return null;
    }
    return refUserStatusRepository.findById(userStatusId)
        .map(RefUserStatus::getStatusCode)
        .orElse(null);
  }
}
