package in.gov.sih.sih26135.bootstrap;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import in.gov.sih.sih26135.entity.Role;
import in.gov.sih.sih26135.entity.User;
import in.gov.sih.sih26135.entity.UserRole;
import in.gov.sih.sih26135.repository.RoleRepository;
import in.gov.sih.sih26135.repository.UserRepository;
import in.gov.sih.sih26135.repository.UserRoleRepository;
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
  @DisplayName("2. Existing active Super Admin and resetPassword=false takes no action (idempotent)")
  void testExistingSuperAdminNoReset() {
    User admin = new User();
    admin.setId(10L);
    admin.setUsername("admin_user");
    admin.setDeletedAt(null);

    UserRole userRole = new UserRole(10L, 1L, 10L);

    when(userRoleRepository.findByRoleId(1L)).thenReturn(List.of(userRole));
    when(userRepository.findById(10L)).thenReturn(Optional.of(admin));

    runner.run(new DefaultApplicationArguments());

    verify(userRepository, never()).save(any());
  }

  @Test
  @DisplayName("3. Existing active Super Admin and resetPassword=true resets password and unlocks account")
  void testExistingSuperAdminResetPassword() {
    ReflectionTestUtils.setField(runner, "resetPassword", true);

    User admin = new User();
    admin.setId(10L);
    admin.setUsername("admin_user");
    admin.setDeletedAt(null);
    admin.setFailedLoginCount(5);

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
    assertThat(savedUser.getUserStatusId()).isEqualTo(2L); // ACTIVE
  }

  @Test
  @DisplayName("4. Reset password requested, but target user does not hold Super Admin role -> skips reset (security guard)")
  void testResetPasswordRefusesNonSuperAdmin() {
    ReflectionTestUtils.setField(runner, "resetPassword", true);

    User realAdmin = new User();
    realAdmin.setId(99L);
    realAdmin.setUsername("super_gov_admin");
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

    // Must NOT save or mutate targetNonAdmin
    verify(userRepository, never()).save(targetNonAdmin);
    assertThat(targetNonAdmin.getPasswordHash()).isEqualTo("original_hash");
  }

  @Test
  @DisplayName("5. Fresh installation: No super admin exists -> creates new user and assigns Super Admin role")
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
  @DisplayName("6. Existing user with bootstrapUsername but no super admin exists: escalation=false -> refuses to escalate")
  void testExistingUserWithoutEscalationFlagRefusesEscalation() {
    when(userRoleRepository.findByRoleId(1L)).thenReturn(Collections.emptyList());

    User existingTrainee = new User();
    existingTrainee.setId(55L);
    existingTrainee.setUsername("admin_user");
    when(userRepository.findByUsername("admin_user")).thenReturn(Optional.of(existingTrainee));

    runner.run(new DefaultApplicationArguments());

    // Refuses to grant role 1 or update user
    verify(userRoleRepository, never()).save(any(UserRole.class));
    verify(userRepository, never()).save(any(User.class));
  }

  @Test
  @DisplayName("7. Existing user with bootstrapUsername and escalation=true -> safely escalates in recovery mode")
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
