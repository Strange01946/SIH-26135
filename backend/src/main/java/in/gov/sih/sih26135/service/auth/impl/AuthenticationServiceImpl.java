package in.gov.sih.sih26135.service.auth.impl;

import in.gov.sih.sih26135.dto.auth.AuthResponse;
import in.gov.sih.sih26135.dto.auth.AuthenticatedUserResponse;
import in.gov.sih.sih26135.dto.auth.LoginRequest;
import in.gov.sih.sih26135.dto.auth.RefreshTokenRequest;
import in.gov.sih.sih26135.dto.request.CreateAuditLogRequest;
import in.gov.sih.sih26135.dto.request.CreateSystemEventLogRequest;
import in.gov.sih.sih26135.dto.response.RolePermissionResponse;
import in.gov.sih.sih26135.dto.response.UserRoleResponse;
import in.gov.sih.sih26135.entity.RefAuditAction;
import in.gov.sih.sih26135.entity.RefSystemEventCategory;
import in.gov.sih.sih26135.entity.RefSystemEventSeverity;
import in.gov.sih.sih26135.entity.RefUserStatus;
import in.gov.sih.sih26135.entity.User;
import in.gov.sih.sih26135.exception.BadRequestException;
import in.gov.sih.sih26135.exception.InvalidCredentialsException;
import in.gov.sih.sih26135.exception.ResourceNotFoundException;
import in.gov.sih.sih26135.repository.RefAuditActionRepository;
import in.gov.sih.sih26135.repository.RefSystemEventCategoryRepository;
import in.gov.sih.sih26135.repository.RefSystemEventSeverityRepository;
import in.gov.sih.sih26135.repository.RefUserStatusRepository;
import in.gov.sih.sih26135.repository.UserRepository;
import in.gov.sih.sih26135.security.UserPrincipal;
import in.gov.sih.sih26135.security.config.JwtProperties;
import in.gov.sih.sih26135.security.config.SecurityLockoutProperties;
import in.gov.sih.sih26135.security.jwt.JwtTokenProvider;
import in.gov.sih.sih26135.security.jwt.JwtValidationException;
import in.gov.sih.sih26135.service.AuditLogService;
import in.gov.sih.sih26135.service.RoleService;
import in.gov.sih.sih26135.service.SystemEventLogService;
import in.gov.sih.sih26135.service.UserService;
import in.gov.sih.sih26135.service.auth.AuthenticationService;
import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Production implementation of {@link AuthenticationService} with account lockout,
 * brute-force protection, concurrency-safe row locking, and security audit event logging.
 *
 * <p>Enforces:
 * <ul>
 *   <li>Concurrency-safe user retrieval via pessimistic row-level locking.</li>
 *   <li>Configurable account lockout threshold and duration via {@link SecurityLockoutProperties}.</li>
 *   <li>Automatic expired lock recovery for LOCKED accounts while preserving non-active states.</li>
 *   <li>Counter reset and last_login_at timestamp update on successful authentication.</li>
 *   <li>Authoritative database reference resolution with explicit failure policy (zero fallback IDs/synthetic entities).</li>
 *   <li>Structured security audit events via {@link SystemEventLogService} and {@link AuditLogService}.</li>
 *   <li>Strictly zero client exposure of sensitive internal account, counter, or lockout details.</li>
 * </ul>
 */
@Service
@Transactional(readOnly = true)
public class AuthenticationServiceImpl implements AuthenticationService {

  private static final Logger log = LoggerFactory.getLogger(AuthenticationServiceImpl.class);

  private static final String STATUS_ACTIVE = "ACTIVE";
  private static final String STATUS_LOCKED = "LOCKED";
  private static final String CATEGORY_SECURITY = "SECURITY";
  private static final String SEVERITY_INFO = "INFO";
  private static final String SEVERITY_WARNING = "WARNING";
  private static final String SEVERITY_ERROR = "ERROR";
  private static final String ACTION_UPDATE = "UPDATE";

  private final UserRepository userRepository;
  private final RefUserStatusRepository refUserStatusRepository;
  private final UserService userService;
  private final RoleService roleService;
  private final PasswordEncoder passwordEncoder;
  private final JwtTokenProvider jwtTokenProvider;
  private final JwtProperties jwtProperties;
  private final SecurityLockoutProperties securityLockoutProperties;
  private final SystemEventLogService systemEventLogService;
  private final AuditLogService auditLogService;
  private final RefSystemEventCategoryRepository refSystemEventCategoryRepository;
  private final RefSystemEventSeverityRepository refSystemEventSeverityRepository;
  private final RefAuditActionRepository refAuditActionRepository;

  public AuthenticationServiceImpl(
      UserRepository userRepository,
      RefUserStatusRepository refUserStatusRepository,
      UserService userService,
      RoleService roleService,
      PasswordEncoder passwordEncoder,
      JwtTokenProvider jwtTokenProvider,
      JwtProperties jwtProperties,
      SecurityLockoutProperties securityLockoutProperties,
      SystemEventLogService systemEventLogService,
      AuditLogService auditLogService,
      RefSystemEventCategoryRepository refSystemEventCategoryRepository,
      RefSystemEventSeverityRepository refSystemEventSeverityRepository,
      RefAuditActionRepository refAuditActionRepository) {
    this.userRepository = userRepository;
    this.refUserStatusRepository = refUserStatusRepository;
    this.userService = userService;
    this.roleService = roleService;
    this.passwordEncoder = passwordEncoder;
    this.jwtTokenProvider = jwtTokenProvider;
    this.jwtProperties = jwtProperties;
    this.securityLockoutProperties = securityLockoutProperties;
    this.systemEventLogService = systemEventLogService;
    this.auditLogService = auditLogService;
    this.refSystemEventCategoryRepository = refSystemEventCategoryRepository;
    this.refSystemEventSeverityRepository = refSystemEventSeverityRepository;
    this.refAuditActionRepository = refAuditActionRepository;
  }

  @Override
  @Transactional(noRollbackFor = {InvalidCredentialsException.class})
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

    // Concurrency-safe lookup acquiring PESSIMISTIC_WRITE lock on the user row
    User user = userRepository.findByUsernameOrEmailForUpdate(identifier, identifier).orElse(null);

    if (user == null) {
      recordSecurityEvent("LOGIN_FAILURE", null, null,
          "Authentication failed: invalid credentials", SEVERITY_WARNING);
      log.warn("Authentication failed: invalid credentials");
      throw new InvalidCredentialsException();
    }

    // Reject soft-deleted accounts
    if (user.getDeletedAt() != null) {
      recordSecurityEvent("LOGIN_FAILURE", user.getId(), user.getId(),
          "Authentication failed: invalid credentials", SEVERITY_WARNING);
      log.warn("Authentication failed: invalid credentials for user id={}", user.getId());
      throw new InvalidCredentialsException();
    }

    // Retrieve current user status
    RefUserStatus currentStatus = refUserStatusRepository.findById(user.getUserStatusId())
        .orElseThrow(InvalidCredentialsException::new);
    String currentStatusCode = currentStatus.getStatusCode().toUpperCase();
    LocalDateTime now = LocalDateTime.now();

    // Check if account is currently locked with active locked_until in the future
    if (user.getLockedUntil() != null && user.getLockedUntil().isAfter(now)) {
      recordSecurityEvent("LOGIN_BLOCKED_LOCKED_ACCOUNT", user.getId(), user.getId(),
          "Login blocked: account is currently locked", SEVERITY_WARNING);
      log.warn("Authentication blocked: account id={} is currently locked", user.getId());
      throw new InvalidCredentialsException();
    }

    // Check if timed lockout has expired
    if (user.getLockedUntil() != null && !user.getLockedUntil().isAfter(now)) {
      // Only restore to ACTIVE if status was LOCKED (do NOT alter DISABLED, ARCHIVED, PENDING)
      if (STATUS_LOCKED.equalsIgnoreCase(currentStatusCode)) {
        RefUserStatus activeStatus = getStatusByCode(STATUS_ACTIVE);
        user.setUserStatusId(activeStatus.getId());
        user.setLockedUntil(null);
        user.setFailedLoginCount(0);
        userRepository.save(user);
        currentStatusCode = STATUS_ACTIVE;
        log.info("Account lock expired: user id={} restored to active state", user.getId());
        recordSecurityEvent("ACCOUNT_UNLOCKED", user.getId(), user.getId(),
            "Account lock expired; account restored to active state", SEVERITY_INFO);
      }
    }

    // Verify account status is strictly ACTIVE (reject PENDING, LOCKED, DISABLED, ARCHIVED)
    if (!STATUS_ACTIVE.equalsIgnoreCase(currentStatusCode)) {
      recordSecurityEvent("LOGIN_FAILURE", user.getId(), user.getId(),
          "Authentication failed: invalid credentials", SEVERITY_WARNING);
      log.warn("Authentication failed: account inactive for user id={}", user.getId());
      throw new InvalidCredentialsException();
    }

    // Verify raw password against stored Argon2id hash
    if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
      int currentFailed = user.getFailedLoginCount() != null ? user.getFailedLoginCount() : 0;
      int newFailed = currentFailed + 1;
      user.setFailedLoginCount(newFailed);

      int maxAttempts = securityLockoutProperties.getMaxFailedAttempts();
      if (newFailed >= maxAttempts) {
        LocalDateTime lockExpiry = now.plus(securityLockoutProperties.getLockDuration());
        user.setLockedUntil(lockExpiry);
        RefUserStatus lockedStatus = getStatusByCode(STATUS_LOCKED);
        user.setUserStatusId(lockedStatus.getId());
        userRepository.save(user);

        log.warn("Account locked after repeated authentication failures for user id={}", user.getId());
        recordSecurityEvent("LOGIN_FAILURE", user.getId(), user.getId(),
            "Authentication failed: invalid credentials", SEVERITY_WARNING);
        recordSecurityEvent("ACCOUNT_LOCKED", user.getId(), user.getId(),
            "Account locked after repeated authentication failures", SEVERITY_ERROR);
        recordAuditLog(user.getId(), "User", user.getId(),
            "Account locked after repeated authentication failures");
      } else {
        userRepository.save(user);
        log.warn("Authentication failed: invalid credentials for user id={}", user.getId());
        recordSecurityEvent("LOGIN_FAILURE", user.getId(), user.getId(),
            "Authentication failed: invalid credentials", SEVERITY_WARNING);
      }

      throw new InvalidCredentialsException();
    }

    // Successful authentication: reset failed count, clear lock, update last_login_at, ensure ACTIVE
    user.setFailedLoginCount(0);
    user.setLockedUntil(null);
    user.setLastLoginAt(now);
    RefUserStatus activeStatus = getStatusByCode(STATUS_ACTIVE);
    user.setUserStatusId(activeStatus.getId());
    userRepository.save(user);

    log.info("Authentication successful for user id={}", user.getId());
    recordSecurityEvent("LOGIN_SUCCESS", user.getId(), user.getId(),
        "Authentication successful", SEVERITY_INFO);

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
  @Transactional(noRollbackFor = {InvalidCredentialsException.class})
  public AuthResponse refresh(RefreshTokenRequest request) {
    if (request == null || request.getRefreshToken() == null || request.getRefreshToken().isBlank()) {
      throw new BadRequestException("Refresh token is required");
    }

    String rawToken = request.getRefreshToken().trim();

    // Validate token strictly as REFRESH token type
    try {
      jwtTokenProvider.parseAndValidateToken(rawToken, JwtTokenProvider.TOKEN_TYPE_REFRESH);
    } catch (JwtValidationException e) {
      recordSecurityEvent("TOKEN_REFRESH_FAILURE", null, null,
          "Refresh token validation failed", SEVERITY_WARNING);
      log.warn("Token refresh failed");
      throw new InvalidCredentialsException("Invalid or expired refresh token");
    }

    Long userId = jwtTokenProvider.extractUserId(rawToken);

    // Re-verify current user existence, soft-delete state, and ACTIVE status
    User user = userRepository.findById(userId)
        .orElseThrow(() -> {
          recordSecurityEvent("TOKEN_REFRESH_FAILURE", null, null,
              "Refresh token validation failed", SEVERITY_WARNING);
          log.warn("Token refresh failed");
          return new InvalidCredentialsException("Invalid or expired refresh token");
        });

    if (user.getDeletedAt() != null) {
      recordSecurityEvent("TOKEN_REFRESH_FAILURE", user.getId(), user.getId(),
          "Refresh token validation failed", SEVERITY_WARNING);
      log.warn("Token refresh failed for user id={}", user.getId());
      throw new InvalidCredentialsException("Invalid or expired refresh token");
    }

    RefUserStatus status = refUserStatusRepository.findById(user.getUserStatusId())
        .orElseThrow(() -> new InvalidCredentialsException("Invalid or expired refresh token"));
    if (!STATUS_ACTIVE.equalsIgnoreCase(status.getStatusCode())) {
      recordSecurityEvent("TOKEN_REFRESH_FAILURE", user.getId(), user.getId(),
          "Refresh token validation failed", SEVERITY_WARNING);
      log.warn("Token refresh failed for user id={}", user.getId());
      throw new InvalidCredentialsException("Invalid or expired refresh token");
    }

    // Rebuild CURRENT authorities from database to reflect any recent role/permission changes
    List<String> currentAuthorities = buildEffectiveAuthorities(user.getId());

    // Issue renewed access token and rotated refresh token
    String newAccessToken = jwtTokenProvider.generateAccessToken(user.getId(), user.getUsername(), currentAuthorities);
    String newRefreshToken = jwtTokenProvider.generateRefreshToken(user.getId());
    long expiresInSeconds = jwtProperties.getAccessTokenExpirationMs() / 1000L;

    recordSecurityEvent("TOKEN_REFRESH_SUCCESS", user.getId(), user.getId(),
        "Refresh token renewed successfully", SEVERITY_INFO);
    log.info("Token refresh successful for user id={}", user.getId());

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

  private void recordSecurityEvent(String eventCode, Long actorUserId, Long entityId, String summary, String severityCode) {
    Long categoryId = resolveCategoryId(CATEGORY_SECURITY);
    Long severityId = resolveSeverityId(severityCode);

    CreateSystemEventLogRequest eventRequest = new CreateSystemEventLogRequest();
    eventRequest.setSystemEventCategoryId(categoryId);
    eventRequest.setSystemEventSeverityId(severityId);
    eventRequest.setEventCode(eventCode);
    eventRequest.setActorUserId(actorUserId);
    eventRequest.setEntityType("USER");
    eventRequest.setEntityId(entityId);
    eventRequest.setSourceComponent("AUTH_SERVICE");
    eventRequest.setEventSummary(summary);
    eventRequest.setOccurredAt(LocalDateTime.now());

    systemEventLogService.recordSystemEvent(eventRequest);
  }

  private void recordAuditLog(Long actorUserId, String entityType, Long entityId, String summary) {
    Long actionId = resolveAuditActionId(ACTION_UPDATE);

    CreateAuditLogRequest auditRequest = new CreateAuditLogRequest();
    auditRequest.setAuditActionId(actionId);
    auditRequest.setActorUserId(actorUserId);
    auditRequest.setEntityType(entityType);
    auditRequest.setEntityId(entityId);
    auditRequest.setChangeSummary(summary);
    auditRequest.setOccurredAt(LocalDateTime.now());

    auditLogService.recordAuditLog(auditRequest);
  }

  private Long resolveCategoryId(String categoryCode) {
    return refSystemEventCategoryRepository.findByCategoryCode(categoryCode)
        .map(RefSystemEventCategory::getId)
        .orElseThrow(() -> new ResourceNotFoundException("RefSystemEventCategory", "categoryCode: " + categoryCode));
  }

  private Long resolveSeverityId(String severityCode) {
    return refSystemEventSeverityRepository.findBySeverityCode(severityCode)
        .map(RefSystemEventSeverity::getId)
        .orElseThrow(() -> new ResourceNotFoundException("RefSystemEventSeverity", "severityCode: " + severityCode));
  }

  private Long resolveAuditActionId(String actionCode) {
    return refAuditActionRepository.findByActionCode(actionCode)
        .map(RefAuditAction::getId)
        .orElseThrow(() -> new ResourceNotFoundException("RefAuditAction", "actionCode: " + actionCode));
  }

  private RefUserStatus getStatusByCode(String statusCode) {
    return refUserStatusRepository.findByStatusCode(statusCode)
        .orElseThrow(() -> new ResourceNotFoundException("RefUserStatus", "statusCode: " + statusCode));
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
