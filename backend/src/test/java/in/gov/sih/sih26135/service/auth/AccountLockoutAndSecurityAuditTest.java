package in.gov.sih.sih26135.service.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import in.gov.sih.sih26135.dto.auth.AuthResponse;
import in.gov.sih.sih26135.dto.auth.LoginRequest;
import in.gov.sih.sih26135.entity.RefAuditAction;
import in.gov.sih.sih26135.entity.RefSystemEventCategory;
import in.gov.sih.sih26135.entity.RefSystemEventSeverity;
import in.gov.sih.sih26135.entity.RefUserStatus;
import in.gov.sih.sih26135.entity.User;
import in.gov.sih.sih26135.exception.InvalidCredentialsException;
import in.gov.sih.sih26135.exception.ResourceNotFoundException;
import in.gov.sih.sih26135.repository.RefAuditActionRepository;
import in.gov.sih.sih26135.repository.RefSystemEventCategoryRepository;
import in.gov.sih.sih26135.repository.RefSystemEventSeverityRepository;
import in.gov.sih.sih26135.repository.RefUserStatusRepository;
import in.gov.sih.sih26135.repository.UserRepository;
import in.gov.sih.sih26135.security.CustomUserDetailsService;
import in.gov.sih.sih26135.security.config.JwtProperties;
import in.gov.sih.sih26135.security.config.SecurityLockoutProperties;
import in.gov.sih.sih26135.security.jwt.JwtTokenProvider;
import in.gov.sih.sih26135.service.AuditLogService;
import in.gov.sih.sih26135.service.RoleService;
import in.gov.sih.sih26135.service.SystemEventLogService;
import in.gov.sih.sih26135.service.UserService;
import in.gov.sih.sih26135.service.auth.impl.AuthenticationServiceImpl;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class AccountLockoutAndSecurityAuditTest {

  @Mock
  private UserRepository userRepository;

  @Mock
  private RefUserStatusRepository refUserStatusRepository;

  @Mock
  private UserService userService;

  @Mock
  private RoleService roleService;

  @Mock
  private PasswordEncoder passwordEncoder;

  @Mock
  private JwtTokenProvider jwtTokenProvider;

  @Mock
  private JwtProperties jwtProperties;

  @Mock
  private SecurityLockoutProperties securityLockoutProperties;

  @Mock
  private SystemEventLogService systemEventLogService;

  @Mock
  private AuditLogService auditLogService;

  @Mock
  private RefSystemEventCategoryRepository refSystemEventCategoryRepository;

  @Mock
  private RefSystemEventSeverityRepository refSystemEventSeverityRepository;

  @Mock
  private RefAuditActionRepository refAuditActionRepository;

  private AuthenticationServiceImpl authenticationService;
  private CustomUserDetailsService customUserDetailsService;

  private RefUserStatus activeStatus;
  private RefUserStatus lockedStatus;
  private RefUserStatus disabledStatus;
  private RefUserStatus archivedStatus;

  private RefSystemEventCategory securityCategory;
  private RefSystemEventSeverity infoSeverity;
  private RefSystemEventSeverity warningSeverity;
  private RefSystemEventSeverity errorSeverity;
  private RefAuditAction updateAuditAction;

  @BeforeEach
  void setUp() {
    activeStatus = new RefUserStatus("ACTIVE", "Active", 2);
    activeStatus.setId(2L);

    lockedStatus = new RefUserStatus("LOCKED", "Locked", 3);
    lockedStatus.setId(3L);

    disabledStatus = new RefUserStatus("DISABLED", "Disabled", 4);
    disabledStatus.setId(4L);

    archivedStatus = new RefUserStatus("ARCHIVED", "Archived", 5);
    archivedStatus.setId(5L);

    securityCategory = new RefSystemEventCategory("SECURITY", "Security", 1);
    securityCategory.setId(1L);

    infoSeverity = new RefSystemEventSeverity("INFO", "Info", 1);
    infoSeverity.setId(1L);

    warningSeverity = new RefSystemEventSeverity("WARNING", "Warning", 2);
    warningSeverity.setId(2L);

    errorSeverity = new RefSystemEventSeverity("ERROR", "Error", 3);
    errorSeverity.setId(3L);

    updateAuditAction = new RefAuditAction("UPDATE", "Update", 2);
    updateAuditAction.setId(2L);

    authenticationService = new AuthenticationServiceImpl(
        userRepository,
        refUserStatusRepository,
        userService,
        roleService,
        passwordEncoder,
        jwtTokenProvider,
        jwtProperties,
        securityLockoutProperties,
        systemEventLogService,
        auditLogService,
        refSystemEventCategoryRepository,
        refSystemEventSeverityRepository,
        refAuditActionRepository
    );

    customUserDetailsService = new CustomUserDetailsService(
        userRepository,
        refUserStatusRepository,
        userService,
        roleService
    );
  }

  private User createSampleUser(Long id, String username, Long statusId, int failedCount, LocalDateTime lockedUntil) {
    User user = new User();
    user.setId(id);
    user.setUsername(username);
    user.setEmail(username + "@example.com");
    user.setPasswordHash("$argon2id$v=19$m=65536,t=3,p=1$fakehash");
    user.setUserStatusId(statusId);
    user.setFailedLoginCount(failedCount);
    user.setLockedUntil(lockedUntil);
    user.setCreatedAt(LocalDateTime.now().minusDays(10));
    user.setUpdatedAt(LocalDateTime.now().minusDays(10));
    return user;
  }

  @Test
  @DisplayName("1. Successful login resets failed login count to zero")
  void testSuccessfulLogin_resetsFailedCount() {
    User user = createSampleUser(1L, "user1", 2L, 3, null);

    when(userRepository.findByUsernameOrEmailForUpdate("user1", "user1")).thenReturn(Optional.of(user));
    when(refUserStatusRepository.findById(2L)).thenReturn(Optional.of(activeStatus));
    when(passwordEncoder.matches("ValidPassword123!", user.getPasswordHash())).thenReturn(true);
    when(refUserStatusRepository.findByStatusCode("ACTIVE")).thenReturn(Optional.of(activeStatus));
    when(refSystemEventCategoryRepository.findByCategoryCode("SECURITY")).thenReturn(Optional.of(securityCategory));
    when(refSystemEventSeverityRepository.findBySeverityCode("INFO")).thenReturn(Optional.of(infoSeverity));
    when(jwtProperties.getAccessTokenExpirationMs()).thenReturn(900_000L);
    when(jwtTokenProvider.generateAccessToken(anyLong(), anyString(), any())).thenReturn("access_token");
    when(jwtTokenProvider.generateRefreshToken(anyLong())).thenReturn("refresh_token");

    LoginRequest request = new LoginRequest("user1", "ValidPassword123!");
    AuthResponse response = authenticationService.login(request);

    assertThat(response).isNotNull();
    assertThat(user.getFailedLoginCount()).isEqualTo(0);
    assertThat(user.getLockedUntil()).isNull();
    assertThat(user.getLastLoginAt()).isNotNull();
    verify(userRepository).save(user);
  }

  @Test
  @DisplayName("2. Successful login clears expired lock state and restores ACTIVE status")
  void testSuccessfulLogin_clearsExpiredLockState() {
    LocalDateTime pastLock = LocalDateTime.now().minusMinutes(5);
    User user = createSampleUser(2L, "user2", 3L, 5, pastLock);

    when(userRepository.findByUsernameOrEmailForUpdate("user2", "user2")).thenReturn(Optional.of(user));
    when(refUserStatusRepository.findById(3L)).thenReturn(Optional.of(lockedStatus));
    when(refUserStatusRepository.findByStatusCode("ACTIVE")).thenReturn(Optional.of(activeStatus));
    when(refSystemEventCategoryRepository.findByCategoryCode("SECURITY")).thenReturn(Optional.of(securityCategory));
    when(refSystemEventSeverityRepository.findBySeverityCode("INFO")).thenReturn(Optional.of(infoSeverity));
    when(passwordEncoder.matches("ValidPassword123!", user.getPasswordHash())).thenReturn(true);
    when(jwtProperties.getAccessTokenExpirationMs()).thenReturn(900_000L);
    when(jwtTokenProvider.generateAccessToken(anyLong(), anyString(), any())).thenReturn("access_token");
    when(jwtTokenProvider.generateRefreshToken(anyLong())).thenReturn("refresh_token");

    LoginRequest request = new LoginRequest("user2", "ValidPassword123!");
    AuthResponse response = authenticationService.login(request);

    assertThat(response).isNotNull();
    assertThat(user.getUserStatusId()).isEqualTo(2L);
    assertThat(user.getLockedUntil()).isNull();
    assertThat(user.getFailedLoginCount()).isEqualTo(0);
  }

  @Test
  @DisplayName("3. Failed login increments failed count")
  void testFailedLogin_incrementsFailedCount() {
    User user = createSampleUser(3L, "user3", 2L, 1, null);

    when(userRepository.findByUsernameOrEmailForUpdate("user3", "user3")).thenReturn(Optional.of(user));
    when(refUserStatusRepository.findById(2L)).thenReturn(Optional.of(activeStatus));
    when(passwordEncoder.matches("WrongPassword", user.getPasswordHash())).thenReturn(false);
    when(securityLockoutProperties.getMaxFailedAttempts()).thenReturn(5);
    when(refSystemEventCategoryRepository.findByCategoryCode("SECURITY")).thenReturn(Optional.of(securityCategory));
    when(refSystemEventSeverityRepository.findBySeverityCode("WARNING")).thenReturn(Optional.of(warningSeverity));

    LoginRequest request = new LoginRequest("user3", "WrongPassword");
    assertThatThrownBy(() -> authenticationService.login(request))
        .isInstanceOf(InvalidCredentialsException.class);

    assertThat(user.getFailedLoginCount()).isEqualTo(2);
    assertThat(user.getLockedUntil()).isNull();
    verify(userRepository).save(user);
  }

  @Test
  @DisplayName("4. Fifth failed attempt locks account using configured threshold")
  void testFifthFailedAttempt_locksAccountWithThreshold() {
    User user = createSampleUser(4L, "user4", 2L, 4, null);

    when(userRepository.findByUsernameOrEmailForUpdate("user4", "user4")).thenReturn(Optional.of(user));
    when(refUserStatusRepository.findById(2L)).thenReturn(Optional.of(activeStatus));
    when(passwordEncoder.matches("WrongPassword", user.getPasswordHash())).thenReturn(false);
    when(securityLockoutProperties.getMaxFailedAttempts()).thenReturn(5);
    when(securityLockoutProperties.getLockDuration()).thenReturn(Duration.ofMinutes(15));
    when(refUserStatusRepository.findByStatusCode("LOCKED")).thenReturn(Optional.of(lockedStatus));
    when(refSystemEventCategoryRepository.findByCategoryCode("SECURITY")).thenReturn(Optional.of(securityCategory));
    when(refSystemEventSeverityRepository.findBySeverityCode("WARNING")).thenReturn(Optional.of(warningSeverity));
    when(refSystemEventSeverityRepository.findBySeverityCode("ERROR")).thenReturn(Optional.of(errorSeverity));
    when(refAuditActionRepository.findByActionCode("UPDATE")).thenReturn(Optional.of(updateAuditAction));

    LoginRequest request = new LoginRequest("user4", "WrongPassword");
    assertThatThrownBy(() -> authenticationService.login(request))
        .isInstanceOf(InvalidCredentialsException.class);

    assertThat(user.getFailedLoginCount()).isEqualTo(5);
    assertThat(user.getUserStatusId()).isEqualTo(3L); // LOCKED
    assertThat(user.getLockedUntil()).isNotNull();
    assertThat(user.getLockedUntil()).isAfter(LocalDateTime.now());
    verify(userRepository).save(user);
  }

  @Test
  @DisplayName("5. Active locked account rejects login without password verification")
  void testLockedAccount_rejectsLogin() {
    LocalDateTime futureLock = LocalDateTime.now().plusMinutes(10);
    User user = createSampleUser(5L, "user5", 3L, 5, futureLock);

    when(userRepository.findByUsernameOrEmailForUpdate("user5", "user5")).thenReturn(Optional.of(user));
    when(refUserStatusRepository.findById(3L)).thenReturn(Optional.of(lockedStatus));
    when(refSystemEventCategoryRepository.findByCategoryCode("SECURITY")).thenReturn(Optional.of(securityCategory));
    when(refSystemEventSeverityRepository.findBySeverityCode("WARNING")).thenReturn(Optional.of(warningSeverity));

    LoginRequest request = new LoginRequest("user5", "AnyPassword");
    assertThatThrownBy(() -> authenticationService.login(request))
        .isInstanceOf(InvalidCredentialsException.class);

    verify(passwordEncoder, never()).matches(anyString(), anyString());
    verify(jwtTokenProvider, never()).generateAccessToken(anyLong(), anyString(), any());
  }

  @Test
  @DisplayName("6. Expired lock can be handled according to status model")
  void testExpiredLock_handledAccordingToStatusModel() {
    LocalDateTime pastLock = LocalDateTime.now().minusMinutes(2);
    User user = createSampleUser(6L, "user6", 3L, 5, pastLock);

    when(userRepository.findByUsernameOrEmailForUpdate("user6", "user6")).thenReturn(Optional.of(user));
    when(refUserStatusRepository.findById(3L)).thenReturn(Optional.of(lockedStatus));
    when(refUserStatusRepository.findByStatusCode("ACTIVE")).thenReturn(Optional.of(activeStatus));
    when(refSystemEventCategoryRepository.findByCategoryCode("SECURITY")).thenReturn(Optional.of(securityCategory));
    when(refSystemEventSeverityRepository.findBySeverityCode("INFO")).thenReturn(Optional.of(infoSeverity));
    when(passwordEncoder.matches("CorrectPassword", user.getPasswordHash())).thenReturn(true);
    when(jwtProperties.getAccessTokenExpirationMs()).thenReturn(900_000L);
    when(jwtTokenProvider.generateAccessToken(anyLong(), anyString(), any())).thenReturn("access_token");
    when(jwtTokenProvider.generateRefreshToken(anyLong())).thenReturn("refresh_token");

    LoginRequest request = new LoginRequest("user6", "CorrectPassword");
    AuthResponse response = authenticationService.login(request);

    assertThat(response).isNotNull();
    assertThat(user.getUserStatusId()).isEqualTo(2L);
  }

  @Test
  @DisplayName("7. Disabled account is not accidentally reactivated")
  void testDisabledAccount_notAccidentallyReactivated() {
    LocalDateTime pastLock = LocalDateTime.now().minusMinutes(20);
    User user = createSampleUser(7L, "user7", 4L, 5, pastLock); // 4L = DISABLED

    when(userRepository.findByUsernameOrEmailForUpdate("user7", "user7")).thenReturn(Optional.of(user));
    when(refUserStatusRepository.findById(4L)).thenReturn(Optional.of(disabledStatus));
    when(refSystemEventCategoryRepository.findByCategoryCode("SECURITY")).thenReturn(Optional.of(securityCategory));
    when(refSystemEventSeverityRepository.findBySeverityCode("WARNING")).thenReturn(Optional.of(warningSeverity));

    LoginRequest request = new LoginRequest("user7", "CorrectPassword");
    assertThatThrownBy(() -> authenticationService.login(request))
        .isInstanceOf(InvalidCredentialsException.class);

    assertThat(user.getUserStatusId()).isEqualTo(4L); // remains DISABLED
    verify(passwordEncoder, never()).matches(anyString(), anyString());
  }

  @Test
  @DisplayName("8. Archived account is not accidentally reactivated")
  void testArchivedAccount_notAccidentallyReactivated() {
    LocalDateTime pastLock = LocalDateTime.now().minusMinutes(20);
    User user = createSampleUser(8L, "user8", 5L, 5, pastLock); // 5L = ARCHIVED

    when(userRepository.findByUsernameOrEmailForUpdate("user8", "user8")).thenReturn(Optional.of(user));
    when(refUserStatusRepository.findById(5L)).thenReturn(Optional.of(archivedStatus));
    when(refSystemEventCategoryRepository.findByCategoryCode("SECURITY")).thenReturn(Optional.of(securityCategory));
    when(refSystemEventSeverityRepository.findBySeverityCode("WARNING")).thenReturn(Optional.of(warningSeverity));

    LoginRequest request = new LoginRequest("user8", "CorrectPassword");
    assertThatThrownBy(() -> authenticationService.login(request))
        .isInstanceOf(InvalidCredentialsException.class);

    assertThat(user.getUserStatusId()).isEqualTo(5L); // remains ARCHIVED
    verify(passwordEncoder, never()).matches(anyString(), anyString());
  }

  @Test
  @DisplayName("9. Nonexistent username does not reveal account existence")
  void testNonexistentUsername_doesNotRevealAccountExistence() {
    when(userRepository.findByUsernameOrEmailForUpdate("ghost", "ghost")).thenReturn(Optional.empty());
    when(refSystemEventCategoryRepository.findByCategoryCode("SECURITY")).thenReturn(Optional.of(securityCategory));
    when(refSystemEventSeverityRepository.findBySeverityCode("WARNING")).thenReturn(Optional.of(warningSeverity));

    LoginRequest request = new LoginRequest("ghost", "password123");
    assertThatThrownBy(() -> authenticationService.login(request))
        .isInstanceOf(InvalidCredentialsException.class)
        .hasMessage("Invalid username/email or password");

    verify(systemEventLogService).recordSystemEvent(argThat(req ->
        "LOGIN_FAILURE".equals(req.getEventCode())
            && req.getActorUserId() == null
            && "Authentication failed: invalid credentials".equals(req.getEventSummary())));
  }

  @Test
  @DisplayName("10. Failed login never issues JWT tokens")
  void testFailedLogin_neverIssuesTokens() {
    User user = createSampleUser(10L, "user10", 2L, 0, null);

    when(userRepository.findByUsernameOrEmailForUpdate("user10", "user10")).thenReturn(Optional.of(user));
    when(refUserStatusRepository.findById(2L)).thenReturn(Optional.of(activeStatus));
    when(passwordEncoder.matches("BadPassword", user.getPasswordHash())).thenReturn(false);
    when(securityLockoutProperties.getMaxFailedAttempts()).thenReturn(5);
    when(refSystemEventCategoryRepository.findByCategoryCode("SECURITY")).thenReturn(Optional.of(securityCategory));
    when(refSystemEventSeverityRepository.findBySeverityCode("WARNING")).thenReturn(Optional.of(warningSeverity));

    LoginRequest request = new LoginRequest("user10", "BadPassword");
    assertThatThrownBy(() -> authenticationService.login(request))
        .isInstanceOf(InvalidCredentialsException.class);

    verify(jwtTokenProvider, never()).generateAccessToken(anyLong(), anyString(), any());
    verify(jwtTokenProvider, never()).generateRefreshToken(anyLong());
  }

  @Test
  @DisplayName("11. Successful login issues valid JWT access and refresh token pair")
  void testSuccessfulLogin_issuesExpectedJwtPair() {
    User user = createSampleUser(11L, "user11", 2L, 0, null);

    when(userRepository.findByUsernameOrEmailForUpdate("user11", "user11")).thenReturn(Optional.of(user));
    when(refUserStatusRepository.findById(2L)).thenReturn(Optional.of(activeStatus));
    when(passwordEncoder.matches("GoodPassword", user.getPasswordHash())).thenReturn(true);
    when(refUserStatusRepository.findByStatusCode("ACTIVE")).thenReturn(Optional.of(activeStatus));
    when(refSystemEventCategoryRepository.findByCategoryCode("SECURITY")).thenReturn(Optional.of(securityCategory));
    when(refSystemEventSeverityRepository.findBySeverityCode("INFO")).thenReturn(Optional.of(infoSeverity));
    when(jwtProperties.getAccessTokenExpirationMs()).thenReturn(900_000L);
    when(jwtTokenProvider.generateAccessToken(anyLong(), anyString(), any())).thenReturn("access_token_123");
    when(jwtTokenProvider.generateRefreshToken(anyLong())).thenReturn("refresh_token_456");

    LoginRequest request = new LoginRequest("user11", "GoodPassword");
    AuthResponse response = authenticationService.login(request);

    assertThat(response.getAccessToken()).isEqualTo("access_token_123");
    assertThat(response.getRefreshToken()).isEqualTo("refresh_token_456");
    assertThat(response.getTokenType()).isEqualTo("Bearer");
    assertThat(response.getUserId()).isEqualTo(11L);
    assertThat(response.getUsername()).isEqualTo("user11");
  }

  @Test
  @DisplayName("12. Password/token/hash/secret values are not exposed in authentication error responses")
  void testAuthenticationErrors_doNotExposeSensitiveData() {
    User user = createSampleUser(12L, "user12", 2L, 0, null);

    when(userRepository.findByUsernameOrEmailForUpdate("user12", "user12")).thenReturn(Optional.of(user));
    when(refUserStatusRepository.findById(2L)).thenReturn(Optional.of(activeStatus));
    when(passwordEncoder.matches("SuperSecretPassword!", user.getPasswordHash())).thenReturn(false);
    when(securityLockoutProperties.getMaxFailedAttempts()).thenReturn(5);
    when(refSystemEventCategoryRepository.findByCategoryCode("SECURITY")).thenReturn(Optional.of(securityCategory));
    when(refSystemEventSeverityRepository.findBySeverityCode("WARNING")).thenReturn(Optional.of(warningSeverity));

    LoginRequest request = new LoginRequest("user12", "SuperSecretPassword!");
    assertThatThrownBy(() -> authenticationService.login(request))
        .isInstanceOf(InvalidCredentialsException.class)
        .satisfies(ex -> {
          InvalidCredentialsException ice = (InvalidCredentialsException) ex;
          assertThat(ice.getMessage()).isEqualTo("Invalid username/email or password");
          assertThat(ice.getErrorCode()).isEqualTo("INVALID_CREDENTIALS");
          assertThat(ice.getMessage()).doesNotContain("SuperSecretPassword!");
          assertThat(ice.getMessage()).doesNotContain("argon2");
          assertThat(ice.getMessage()).doesNotContain("user12");
        });
  }

  @Test
  @DisplayName("13. Security audit event summaries are strictly sanitized and contain no counter, timestamp, or password details")
  void testSecurityAuditEventSummaries_sanitizedWithoutSensitiveDetails() {
    User user = createSampleUser(13L, "user13", 2L, 4, null);

    when(userRepository.findByUsernameOrEmailForUpdate("user13", "user13")).thenReturn(Optional.of(user));
    when(refUserStatusRepository.findById(2L)).thenReturn(Optional.of(activeStatus));
    when(passwordEncoder.matches("BadPass", user.getPasswordHash())).thenReturn(false);
    when(securityLockoutProperties.getMaxFailedAttempts()).thenReturn(5);
    when(securityLockoutProperties.getLockDuration()).thenReturn(Duration.ofMinutes(15));
    when(refUserStatusRepository.findByStatusCode("LOCKED")).thenReturn(Optional.of(lockedStatus));
    when(refSystemEventCategoryRepository.findByCategoryCode("SECURITY")).thenReturn(Optional.of(securityCategory));
    when(refSystemEventSeverityRepository.findBySeverityCode("WARNING")).thenReturn(Optional.of(warningSeverity));
    when(refSystemEventSeverityRepository.findBySeverityCode("ERROR")).thenReturn(Optional.of(errorSeverity));
    when(refAuditActionRepository.findByActionCode("UPDATE")).thenReturn(Optional.of(updateAuditAction));

    assertThatThrownBy(() -> authenticationService.login(new LoginRequest("user13", "BadPass")))
        .isInstanceOf(InvalidCredentialsException.class);

    // Verify LOGIN_FAILURE summary has no counter, attempt, or password text
    verify(systemEventLogService).recordSystemEvent(argThat(req -> {
      if ("LOGIN_FAILURE".equals(req.getEventCode())) {
        assertThat(req.getEventSummary()).isEqualTo("Authentication failed: invalid credentials");
        assertThat(req.getEventSummary()).doesNotContain("attempt");
        assertThat(req.getEventSummary()).doesNotContain("password");
        assertThat(req.getEventSummary()).doesNotContain("BadPass");
        return true;
      }
      return false;
    }));

    // Verify ACCOUNT_LOCKED summary has no timestamp, duration, or counter text
    verify(systemEventLogService).recordSystemEvent(argThat(req -> {
      if ("ACCOUNT_LOCKED".equals(req.getEventCode())) {
        assertThat(req.getEventSummary()).isEqualTo("Account locked after repeated authentication failures");
        assertThat(req.getEventSummary()).doesNotContain("until");
        assertThat(req.getEventSummary()).doesNotContain("attempt");
        assertThat(req.getEventSummary()).doesNotContain("minutes");
        return true;
      }
      return false;
    }));

    // Verify AuditLog summary is sanitized
    verify(auditLogService).recordAuditLog(argThat(req -> {
      assertThat(req.getChangeSummary()).isEqualTo("Account locked after repeated authentication failures");
      assertThat(req.getChangeSummary()).doesNotContain("until");
      assertThat(req.getChangeSummary()).doesNotContain("attempt");
      return true;
    }));
  }

  @Test
  @DisplayName("14. Missing reference data fails explicitly with ResourceNotFoundException (no hardcoded fallback)")
  void testMissingReferenceData_failsExplicitlyWithoutFallbacks() {
    User user = createSampleUser(14L, "user14", 2L, 0, null);

    when(userRepository.findByUsernameOrEmailForUpdate("user14", "user14")).thenReturn(Optional.of(user));
    when(refUserStatusRepository.findById(2L)).thenReturn(Optional.of(activeStatus));
    when(passwordEncoder.matches("SomePass", user.getPasswordHash())).thenReturn(true);
    when(refUserStatusRepository.findByStatusCode("ACTIVE")).thenReturn(Optional.of(activeStatus));
    // Category missing in repository
    when(refSystemEventCategoryRepository.findByCategoryCode("SECURITY")).thenReturn(Optional.empty());

    assertThatThrownBy(() -> authenticationService.login(new LoginRequest("user14", "SomePass")))
        .isInstanceOf(ResourceNotFoundException.class)
        .hasMessageContaining("RefSystemEventCategory");
  }

  @Test
  @DisplayName("15. Audit/security-event persistence failure propagates without being swallowed")
  void testAuditFailure_propagatesExplicitly() {
    User user = createSampleUser(15L, "user15", 2L, 0, null);

    when(userRepository.findByUsernameOrEmailForUpdate("user15", "user15")).thenReturn(Optional.of(user));
    when(refUserStatusRepository.findById(2L)).thenReturn(Optional.of(activeStatus));
    when(passwordEncoder.matches("Pass", user.getPasswordHash())).thenReturn(true);
    when(refUserStatusRepository.findByStatusCode("ACTIVE")).thenReturn(Optional.of(activeStatus));
    when(refSystemEventCategoryRepository.findByCategoryCode("SECURITY")).thenReturn(Optional.of(securityCategory));
    when(refSystemEventSeverityRepository.findBySeverityCode("INFO")).thenReturn(Optional.of(infoSeverity));

    // Simulate database error during audit logging
    when(systemEventLogService.recordSystemEvent(any()))
        .thenThrow(new RuntimeException("Audit database connectivity error"));

    assertThatThrownBy(() -> authenticationService.login(new LoginRequest("user15", "Pass")))
        .isInstanceOf(RuntimeException.class)
        .hasMessage("Audit database connectivity error");
  }

  @Test
  @DisplayName("16. CustomUserDetailsService remains strictly read-only and respects locked_until")
  void testCustomUserDetailsService_remainsReadOnlyAndRespectsLock() {
    User lockedUser = createSampleUser(16L, "lockedUser", 2L, 5, LocalDateTime.now().plusMinutes(10));
    when(userRepository.findByUsernameOrEmail("lockedUser", "lockedUser")).thenReturn(Optional.of(lockedUser));
    when(refUserStatusRepository.findById(2L)).thenReturn(Optional.of(activeStatus));

    assertThatThrownBy(() -> customUserDetailsService.loadUserByUsername("lockedUser"))
        .isInstanceOf(UsernameNotFoundException.class)
        .hasMessageContaining("User account is locked");

    // Verify zero database mutations or audit calls from CustomUserDetailsService
    verify(userRepository, never()).save(any());
    verify(systemEventLogService, never()).recordSystemEvent(any());
    verify(auditLogService, never()).recordAuditLog(any());
  }

  @Test
  @DisplayName("17. Concurrency-safe lookup uses pessimistic write row locking")
  void testConcurrentAttempts_usePessimisticWriteLocking() {
    User user = createSampleUser(17L, "user17", 2L, 0, null);

    when(userRepository.findByUsernameOrEmailForUpdate("user17", "user17")).thenReturn(Optional.of(user));
    when(refUserStatusRepository.findById(2L)).thenReturn(Optional.of(activeStatus));
    when(passwordEncoder.matches("Pass", user.getPasswordHash())).thenReturn(true);
    when(refUserStatusRepository.findByStatusCode("ACTIVE")).thenReturn(Optional.of(activeStatus));
    when(refSystemEventCategoryRepository.findByCategoryCode("SECURITY")).thenReturn(Optional.of(securityCategory));
    when(refSystemEventSeverityRepository.findBySeverityCode("INFO")).thenReturn(Optional.of(infoSeverity));
    when(jwtProperties.getAccessTokenExpirationMs()).thenReturn(900_000L);
    when(jwtTokenProvider.generateAccessToken(anyLong(), anyString(), any())).thenReturn("access_token");
    when(jwtTokenProvider.generateRefreshToken(anyLong())).thenReturn("refresh_token");

    authenticationService.login(new LoginRequest("user17", "Pass"));

    // Verify findByUsernameOrEmailForUpdate is called, NOT the non-locking findByUsernameOrEmail
    verify(userRepository).findByUsernameOrEmailForUpdate("user17", "user17");
    verify(userRepository, never()).findByUsernameOrEmail(anyString(), anyString());
  }
}
