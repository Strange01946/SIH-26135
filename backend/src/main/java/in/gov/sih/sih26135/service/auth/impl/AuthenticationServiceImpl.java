package in.gov.sih.sih26135.service.auth.impl;

import in.gov.sih.sih26135.dto.auth.AuthResponse;
import in.gov.sih.sih26135.dto.auth.AuthenticatedUserResponse;
import in.gov.sih.sih26135.dto.auth.LoginRequest;
import in.gov.sih.sih26135.dto.auth.RefreshTokenRequest;
import in.gov.sih.sih26135.dto.response.RolePermissionResponse;
import in.gov.sih.sih26135.dto.response.UserRoleResponse;
import in.gov.sih.sih26135.entity.RefUserStatus;
import in.gov.sih.sih26135.entity.User;
import in.gov.sih.sih26135.exception.BadRequestException;
import in.gov.sih.sih26135.exception.InvalidCredentialsException;
import in.gov.sih.sih26135.exception.ResourceNotFoundException;
import in.gov.sih.sih26135.security.UserPrincipal;
import in.gov.sih.sih26135.security.config.JwtProperties;
import in.gov.sih.sih26135.security.jwt.JwtTokenProvider;
import in.gov.sih.sih26135.security.jwt.JwtValidationException;
import in.gov.sih.sih26135.repository.RefUserStatusRepository;
import in.gov.sih.sih26135.repository.UserRepository;
import in.gov.sih.sih26135.service.RoleService;
import in.gov.sih.sih26135.service.UserService;
import in.gov.sih.sih26135.service.auth.AuthenticationService;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Production implementation of {@link AuthenticationService}.
 *
 * <p>Handles credential verification via Argon2id {@link PasswordEncoder}, enforces ACTIVE account status
 * and soft-delete checks, re-evaluates effective RBAC authorities, and issues signed JWT tokens.
 */
@Service
@Transactional(readOnly = true)
public class AuthenticationServiceImpl implements AuthenticationService {

  private static final String STATUS_ACTIVE = "ACTIVE";

  private final UserRepository userRepository;
  private final RefUserStatusRepository refUserStatusRepository;
  private final UserService userService;
  private final RoleService roleService;
  private final PasswordEncoder passwordEncoder;
  private final JwtTokenProvider jwtTokenProvider;
  private final JwtProperties jwtProperties;

  public AuthenticationServiceImpl(
      UserRepository userRepository,
      RefUserStatusRepository refUserStatusRepository,
      UserService userService,
      RoleService roleService,
      PasswordEncoder passwordEncoder,
      JwtTokenProvider jwtTokenProvider,
      JwtProperties jwtProperties) {
    this.userRepository = userRepository;
    this.refUserStatusRepository = refUserStatusRepository;
    this.userService = userService;
    this.roleService = roleService;
    this.passwordEncoder = passwordEncoder;
    this.jwtTokenProvider = jwtTokenProvider;
    this.jwtProperties = jwtProperties;
  }

  @Override
  public AuthResponse login(LoginRequest request) {
    if (request == null) {
      throw new BadRequestException("Login request cannot be null");
    }
    if (request.getUsernameOrEmail() == null || request.getUsernameOrEmail().isBlank()) {
      throw new BadRequestException("Username or email is required");
    }
    if (request.getPassword() == null || request.getPassword().isBlank()) {
      throw new BadRequestException("Password is required");
    }

    String identifier = request.getUsernameOrEmail().trim();

    // Single-query lookup supporting both username and email
    User user = userRepository.findByUsernameOrEmail(identifier, identifier)
        .orElseThrow(InvalidCredentialsException::new);

    // Reject soft-deleted accounts
    if (user.getDeletedAt() != null) {
      throw new InvalidCredentialsException();
    }

    // Verify account status is strictly ACTIVE (reject PENDING, LOCKED, DISABLED, ARCHIVED)
    RefUserStatus status = refUserStatusRepository.findById(user.getUserStatusId())
        .orElseThrow(InvalidCredentialsException::new);
    if (!STATUS_ACTIVE.equalsIgnoreCase(status.getStatusCode())) {
      throw new InvalidCredentialsException();
    }

    // Verify raw password against stored Argon2id hash
    if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
      throw new InvalidCredentialsException();
    }

    // Construct effective authorities (ROLE_<roleCode> + permissionCode)
    List<String> authorities = buildEffectiveAuthorities(user.getId());

    // Generate token pair
    String accessToken = jwtTokenProvider.generateAccessToken(user.getId(), user.getUsername(), authorities);
    String refreshToken = jwtTokenProvider.generateRefreshToken(user.getId());
    long expiresInSeconds = jwtProperties.getAccessTokenExpirationMs() / 1000L;

    return new AuthResponse(
        accessToken,
        refreshToken,
        "Bearer",
        expiresInSeconds,
        user.getId(),
        user.getUsername(),
        authorities
    );
  }

  @Override
  public AuthResponse refresh(RefreshTokenRequest request) {
    if (request == null || request.getRefreshToken() == null || request.getRefreshToken().isBlank()) {
      throw new BadRequestException("Refresh token is required");
    }

    String rawToken = request.getRefreshToken().trim();

    // Validate token strictly as REFRESH token type
    try {
      jwtTokenProvider.parseAndValidateToken(rawToken, JwtTokenProvider.TOKEN_TYPE_REFRESH);
    } catch (JwtValidationException e) {
      throw new InvalidCredentialsException("Invalid or expired refresh token");
    }

    Long userId = jwtTokenProvider.extractUserId(rawToken);

    // Re-verify current user existence, soft-delete state, and ACTIVE status
    User user = userRepository.findById(userId)
        .orElseThrow(() -> new InvalidCredentialsException("Invalid or expired refresh token"));

    if (user.getDeletedAt() != null) {
      throw new InvalidCredentialsException("Invalid or expired refresh token");
    }

    RefUserStatus status = refUserStatusRepository.findById(user.getUserStatusId())
        .orElseThrow(() -> new InvalidCredentialsException("Invalid or expired refresh token"));
    if (!STATUS_ACTIVE.equalsIgnoreCase(status.getStatusCode())) {
      throw new InvalidCredentialsException("Invalid or expired refresh token");
    }

    // Rebuild CURRENT authorities from database to reflect any recent role/permission changes
    List<String> currentAuthorities = buildEffectiveAuthorities(user.getId());

    // Issue renewed access token and rotated refresh token
    String newAccessToken = jwtTokenProvider.generateAccessToken(user.getId(), user.getUsername(), currentAuthorities);
    String newRefreshToken = jwtTokenProvider.generateRefreshToken(user.getId());
    long expiresInSeconds = jwtProperties.getAccessTokenExpirationMs() / 1000L;

    return new AuthResponse(
        newAccessToken,
        newRefreshToken,
        "Bearer",
        expiresInSeconds,
        user.getId(),
        user.getUsername(),
        currentAuthorities
    );
  }

  @Override
  public AuthenticatedUserResponse getCurrentUser(UserPrincipal principal) {
    if (principal == null || principal.getId() == null) {
      throw new InvalidCredentialsException("User is not authenticated");
    }

    User user = userRepository.findById(principal.getId())
        .orElseThrow(() -> new ResourceNotFoundException("User", "id"));

    List<String> authorities = buildEffectiveAuthorities(user.getId());

    return new AuthenticatedUserResponse(
        user.getId(),
        user.getUsername(),
        user.getEmail(),
        authorities,
        user.getOrganizationId(),
        user.getDepartmentId(),
        user.getStateId(),
        user.getDistrictId(),
        user.getTrainingProviderId(),
        user.getEmployerId()
    );
  }

  private List<String> buildEffectiveAuthorities(Long userId) {
    Set<String> authorities = new LinkedHashSet<>();
    List<UserRoleResponse> userRoles = userService.getUserRoles(userId);

    for (UserRoleResponse ur : userRoles) {
      if (ur.getRoleCode() != null && !ur.getRoleCode().isBlank()) {
        authorities.add("ROLE_" + ur.getRoleCode().trim());
      }
      if (ur.getRoleId() != null) {
        List<RolePermissionResponse> permissions = roleService.getRolePermissions(ur.getRoleId());
        for (RolePermissionResponse perm : permissions) {
          if (perm.getPermissionCode() != null && !perm.getPermissionCode().isBlank()) {
            authorities.add(perm.getPermissionCode().trim());
          }
        }
      }
    }

    return authorities.stream().toList();
  }
}
