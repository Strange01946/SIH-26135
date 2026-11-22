package in.gov.sih.sih26135.bootstrap;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import in.gov.sih.sih26135.entity.User;
import in.gov.sih.sih26135.entity.UserRole;
import in.gov.sih.sih26135.repository.RoleRepository;
import in.gov.sih.sih26135.repository.UserRepository;
import in.gov.sih.sih26135.repository.UserRoleRepository;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.DefaultApplicationArguments;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
@DisplayName("Phase 30.3A — SuperAdminBootstrapRunner Functional & Security Tests")
class SuperAdminBootstrapRunnerTest {

  @Mock
  private UserRepository userRepository;

  @Mock
  private UserRoleRepository userRoleRepository;

  @Mock
  private RoleRepository roleRepository;

  @Mock
  private PasswordEncoder passwordEncoder;

  @InjectMocks
  private SuperAdminBootstrapRunner runner;

  @BeforeEach
  void setUp() {
    ReflectionTestUtils.setField(runner, "bootstrapEnabled", true);
    ReflectionTestUtils.setField(runner, "bootstrapUsername", "admin_user");
    ReflectionTestUtils.setField(runner, "bootstrapPassword", "RawPassword123");
    ReflectionTestUtils.setField(runner, "bootstrapEmail", "admin@example.com");
    ReflectionTestUtils.setField(runner, "resetPassword", false);
    ReflectionTestUtils.setField(runner, "allowExistingUserEscalation", false);
    ReflectionTestUtils.setField(runner, "reactivateDisabled", false);
  }

  @Test
  @DisplayName("1. Bootstrap disabled takes no action")
  void testBootstrapDisabled() {
    ReflectionTestUtils.setField(runner, "bootstrapEnabled", false);
    runner.run(new DefaultApplicationArguments());

    verify(userRoleRepository, never()).findByRoleId(any());
    verify(userRepository, never()).findByUsername(any());
  }

  @Test
  @DisplayName("2. Existing active Super Admin and resetPassword=false takes no action (idempotent repeated startup)")
  void testExistingActiveSuperAdminNoReset() {
    User admin = new User();
    admin.setId(10L);
    admin.setUsername("admin_user");
    admin.setUserStatusId(2L); // ACTIVE
    admin.setDeletedAt(null);

    UserRole userRole = new UserRole(10L, 1L, 10L);

    when(userRoleRepository.findByRoleId(1L)).thenReturn(List.of(userRole));
    when(userRepository.findById(10L)).thenReturn(Optional.of(admin));

    runner.run(new DefaultApplicationArguments());

    verify(userRepository, never()).save(any());
  }

  @Test
  @DisplayName("3. Existing active Super Admin and resetPassword=true resets password while preserving ACTIVE status")
  void testExistingActiveSuperAdminResetPassword() {
    ReflectionTestUtils.setField(runner, "resetPassword", true);

    User admin = new User();
    admin.setId(10L);
    admin.setUsername("admin_user");
    admin.setUserStatusId(2L); // ACTIVE
    admin.setDeletedAt(null);
    admin.setFailedLoginCount(2);

    UserRole userRole = new UserRole(10L, 1L, 10L);

    when(userRoleRepository.findByRoleId(1L)).thenReturn(List.of(userRole));
    when(userRepository.findById(10L)).thenReturn(Optional.of(admin));
    when(userRepository.findByUsername("admin_user")).thenReturn(Optional.of(admin));
    when(userRoleRepository.existsByUserIdAndRoleId(10L, 1L)).thenReturn(true);
    when(passwordEncoder.encode("RawPassword123")).thenReturn("encoded_new_password");

    runner.run(new DefaultApplicationArguments());

    ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
    verify(userRepository).save(userCaptor.capture());

    User savedUser = userCaptor.getValue();
    assertThat(savedUser.getPasswordHash()).isEqualTo("encoded_new_password");
    assertThat(savedUser.getFailedLoginCount()).isEqualTo(0);
    assertThat(savedUser.getUserStatusId()).isEqualTo(2L); // Remains ACTIVE
  }

  @Test
  @DisplayName("4. Existing LOCKED Super Admin resets password and unlocks account to ACTIVE")
  void testLockedSuperAdminPasswordResetUnlocksAccount() {
    ReflectionTestUtils.setField(runner, "resetPassword", true);

    User admin = new User();
    admin.setId(10L);
    admin.setUsername("admin_user");
    admin.setUserStatusId(3L); // LOCKED
    admin.setDeletedAt(null);
    admin.setFailedLoginCount(5);
    admin.setLockedUntil(LocalDateTime.now().plusHours(1));

    UserRole userRole = new UserRole(10L, 1L, 10L);

    when(userRoleRepository.findByRoleId(1L)).thenReturn(List.of(userRole));
    when(userRepository.findById(10L)).thenReturn(Optional.of(admin));
    when(userRepository.findByUsername("admin_user")).thenReturn(Optional.of(admin));
    when(userRoleRepository.existsByUserIdAndRoleId(10L, 1L)).thenReturn(true);
    when(passwordEncoder.encode("RawPassword123")).thenReturn("encoded_new_password");

    runner.run(new DefaultApplicationArguments());

    ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
    verify(userRepository).save(userCaptor.capture());

    User savedUser = userCaptor.getValue();
    assertThat(savedUser.getPasswordHash()).isEqualTo("encoded_new_password");
    assertThat(savedUser.getFailedLoginCount()).isEqualTo(0);
    assertThat(savedUser.getLockedUntil()).isNull();
    assertThat(savedUser.getUserStatusId()).isEqualTo(2L); // Unlocked to ACTIVE
  }

  @Test
  @DisplayName("5. Existing DISABLED Super Admin: resetPassword=true does NOT silently reactivate (remains DISABLED)")
  void testDisabledSuperAdminPasswordResetSeparatedFromReactivation() {
    ReflectionTestUtils.setField(runner, "resetPassword", true);
    ReflectionTestUtils.setField(runner, "reactivateDisabled", false);

    User admin = new User();
    admin.setId(10L);
    admin.setUsername("admin_user");
    admin.setUserStatusId(4L); // DISABLED
    admin.setDeletedAt(null);

    UserRole userRole = new UserRole(10L, 1L, 10L);

    when(userRoleRepository.findByRoleId(1L)).thenReturn(List.of(userRole));
    when(userRepository.findById(10L)).thenReturn(Optional.of(admin));
    when(userRepository.findByUsername("admin_user")).thenReturn(Optional.of(admin));
    when(userRoleRepository.existsByUserIdAndRoleId(10L, 1L)).thenReturn(true);
    when(passwordEncoder.encode("RawPassword123")).thenReturn("encoded_new_password");

    runner.run(new DefaultApplicationArguments());

    ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
    verify(userRepository).save(userCaptor.capture());

    User savedUser = userCaptor.getValue();
    assertThat(savedUser.getPasswordHash()).isEqualTo("encoded_new_password");
    assertThat(savedUser.getUserStatusId()).isEqualTo(4L); // Strictly remains DISABLED
  }

  @Test
  @DisplayName("6. Existing DISABLED Super Admin with reactivateDisabled=true reactivates account upon recovery")
  void testDisabledSuperAdminReactivationWithExplicitFlag() {
    ReflectionTestUtils.setField(runner, "resetPassword", true);
    ReflectionTestUtils.setField(runner, "reactivateDisabled", true);

    User admin = new User();
    admin.setId(10L);
    admin.setUsername("admin_user");
    admin.setUserStatusId(4L); // DISABLED
    admin.setDeletedAt(null);

    UserRole userRole = new UserRole(10L, 1L, 10L);

    when(userRoleRepository.findByRoleId(1L)).thenReturn(List.of(userRole));
    when(userRepository.findById(10L)).thenReturn(Optional.of(admin));
    when(userRepository.findByUsername("admin_user")).thenReturn(Optional.of(admin));
    when(userRoleRepository.existsByUserIdAndRoleId(10L, 1L)).thenReturn(true);
    when(passwordEncoder.encode("RawPassword123")).thenReturn("encoded_new_password");

    runner.run(new DefaultApplicationArguments());

    ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
    verify(userRepository).save(userCaptor.capture());

    User savedUser = userCaptor.getValue();
    assertThat(savedUser.getPasswordHash()).isEqualTo("encoded_new_password");
    assertThat(savedUser.getUserStatusId()).isEqualTo(2L); // Explicitly reactivated to ACTIVE
  }

  @Test
  @DisplayName("7. System has disabled Super Admin and reactivateDisabled=false: takes no action and logs error")
  void testAllSuperAdminsDisabledRefusesActionWithoutFlag() {
    User admin = new User();
    admin.setId(10L);
    admin.setUsername("admin_user");
    admin.setUserStatusId(4L); // DISABLED
    admin.setDeletedAt(null);

    UserRole userRole = new UserRole(10L, 1L, 10L);

    when(userRoleRepository.findByRoleId(1L)).thenReturn(List.of(userRole));
    when(userRepository.findById(10L)).thenReturn(Optional.of(admin));
    when(userRepository.findByUsername("admin_user")).thenReturn(Optional.of(admin));
    when(userRoleRepository.existsByUserIdAndRoleId(10L, 1L)).thenReturn(true);

    runner.run(new DefaultApplicationArguments());

    verify(userRepository, never()).save(any());
  }

  @Test
  @DisplayName("8. Reset password requested, but target user does NOT hold Super Admin role -> skips reset (security guard)")
  void testResetPasswordRefusesNonSuperAdmin() {
    ReflectionTestUtils.setField(runner, "resetPassword", true);

    User realAdmin = new User();
    realAdmin.setId(99L);
    realAdmin.setUsername("super_gov_admin");
    realAdmin.setUserStatusId(2L); // ACTIVE
    realAdmin.setDeletedAt(null);

    User targetNonAdmin = new User();
    targetNonAdmin.setId(10L);
    targetNonAdmin.setUsername("admin_user"); // same name as bootstrapUsername, but trainee
    targetNonAdmin.setPasswordHash("original_hash");

    UserRole adminRole = new UserRole(99L, 1L, 99L);

    when(userRoleRepository.findByRoleId(1L)).thenReturn(List.of(adminRole));
    when(userRepository.findById(99L)).thenReturn(Optional.of(realAdmin));
    when(userRepository.findByUsername("admin_user")).thenReturn(Optional.of(targetNonAdmin));
    when(userRoleRepository.existsByUserIdAndRoleId(10L, 1L)).thenReturn(false);

    runner.run(new DefaultApplicationArguments());

    verify(userRepository, never()).save(targetNonAdmin);
    assertThat(targetNonAdmin.getPasswordHash()).isEqualTo("original_hash");
  }

  @Test
  @DisplayName("9. Reset password requested for soft-deleted Super Admin -> skips reset")
  void testResetPasswordRefusesDeletedSuperAdmin() {
    ReflectionTestUtils.setField(runner, "resetPassword", true);

    User realAdmin = new User();
    realAdmin.setId(99L);
    realAdmin.setUsername("super_gov_admin");
    realAdmin.setUserStatusId(2L);
    realAdmin.setDeletedAt(null);

    User deletedAdmin = new User();
    deletedAdmin.setId(10L);
    deletedAdmin.setUsername("admin_user");
    deletedAdmin.setUserStatusId(2L);
    deletedAdmin.setDeletedAt(LocalDateTime.now().minusDays(1)); // SOFT-DELETED

    UserRole activeRole = new UserRole(99L, 1L, 99L);

    when(userRoleRepository.findByRoleId(1L)).thenReturn(List.of(activeRole));
    when(userRepository.findById(99L)).thenReturn(Optional.of(realAdmin));
    when(userRepository.findByUsername("admin_user")).thenReturn(Optional.of(deletedAdmin));
    when(userRoleRepository.existsByUserIdAndRoleId(10L, 1L)).thenReturn(true);

    runner.run(new DefaultApplicationArguments());

    verify(userRepository, never()).save(deletedAdmin);
  }

  @Test
  @DisplayName("10. Fresh installation: No super admin exists -> creates new user and assigns Super Admin role")
  void testFreshInstallationCreatesSuperAdmin() {
    when(userRoleRepository.findByRoleId(1L)).thenReturn(Collections.emptyList());
    when(userRepository.findByUsername("admin_user")).thenReturn(Optional.empty());
    when(passwordEncoder.encode("RawPassword123")).thenReturn("encoded_raw_pass");

    User createdUser = new User();
    createdUser.setId(1L);
    createdUser.setUsername("admin_user");
    when(userRepository.save(any(User.class))).thenReturn(createdUser);

    runner.run(new DefaultApplicationArguments());

    verify(userRepository).save(any(User.class));
    verify(userRoleRepository).save(any(UserRole.class));
  }

  @Test
  @DisplayName("11. Existing user with bootstrapUsername but no super admin exists: escalation=false -> refuses to escalate")
  void testExistingUserWithoutEscalationFlagRefusesEscalation() {
    when(userRoleRepository.findByRoleId(1L)).thenReturn(Collections.emptyList());

    User existingTrainee = new User();
    existingTrainee.setId(55L);
    existingTrainee.setUsername("admin_user");
    when(userRepository.findByUsername("admin_user")).thenReturn(Optional.of(existingTrainee));

    runner.run(new DefaultApplicationArguments());

    verify(userRoleRepository, never()).save(any(UserRole.class));
    verify(userRepository, never()).save(any(User.class));
  }

  @Test
  @DisplayName("12. Existing user with bootstrapUsername and escalation=true -> safely escalates in recovery mode")
  void testExistingUserWithEscalationFlagEscalates() {
    ReflectionTestUtils.setField(runner, "allowExistingUserEscalation", true);
    when(userRoleRepository.findByRoleId(1L)).thenReturn(Collections.emptyList());

    User existingUser = new User();
    existingUser.setId(55L);
    existingUser.setUsername("admin_user");
    when(userRepository.findByUsername("admin_user")).thenReturn(Optional.of(existingUser));
    when(userRoleRepository.existsByUserIdAndRoleId(55L, 1L)).thenReturn(false);

    runner.run(new DefaultApplicationArguments());

    verify(userRoleRepository).save(any(UserRole.class));
    verify(userRepository).save(existingUser);
    assertThat(existingUser.getUserStatusId()).isEqualTo(2L);
  }
}
