package in.gov.sih.sih26135.bootstrap;

import in.gov.sih.sih26135.entity.User;
import in.gov.sih.sih26135.entity.UserRole;
import in.gov.sih.sih26135.repository.RoleRepository;
import in.gov.sih.sih26135.repository.UserRepository;
import in.gov.sih.sih26135.repository.UserRoleRepository;
import java.time.LocalDateTime;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Idempotent first-time bootstrap runner for Super Admin account.
 * Ensures a functional Super Admin exists without mutating existing setups or duplicating accounts.
 */
@Component
@Order(10)
public class SuperAdminBootstrapRunner implements ApplicationRunner {

  private static final Logger log = LoggerFactory.getLogger(SuperAdminBootstrapRunner.class);
  private static final Long SUPER_ADMIN_ROLE_ID = 1L;
  private static final Long ACTIVE_STATUS_ID = 2L;

  private final UserRepository userRepository;
  private final UserRoleRepository userRoleRepository;
  private final RoleRepository roleRepository;
  private final PasswordEncoder passwordEncoder;

  @Value("${app.bootstrap.superadmin.enabled:true}")
  private boolean bootstrapEnabled;

  @Value("${app.bootstrap.superadmin.username:admin_user}")
  private String bootstrapUsername;

  @Value("${app.bootstrap.superadmin.password:RawPassword123}")
  private String bootstrapPassword;

  @Value("${app.bootstrap.superadmin.email:admin@example.com}")
  private String bootstrapEmail;

  @Value("${app.bootstrap.superadmin.reset-password:false}")
  private boolean resetPassword;

  @Value("${app.bootstrap.superadmin.allow-existing-user-escalation:false}")
  private boolean allowExistingUserEscalation;

  public SuperAdminBootstrapRunner(
      UserRepository userRepository,
      UserRoleRepository userRoleRepository,
      RoleRepository roleRepository,
      PasswordEncoder passwordEncoder) {
    this.userRepository = userRepository;
    this.userRoleRepository = userRoleRepository;
    this.roleRepository = roleRepository;
    this.passwordEncoder = passwordEncoder;
  }

  @Override
  @Transactional
  public void run(ApplicationArguments args) {
    if (!bootstrapEnabled) {
      log.info("Super admin bootstrap is disabled by configuration");
      return;
    }

    // Filter existing Super Admins to valid non-deleted accounts
    List<UserRole> existingSuperAdmins = userRoleRepository.findByRoleId(SUPER_ADMIN_ROLE_ID).stream()
        .filter(ur -> userRepository.findById(ur.getUserId()).map(u -> u.getDeletedAt() == null).orElse(false))
        .toList();

    if (!existingSuperAdmins.isEmpty()) {
      if (resetPassword) {
        log.warn("Super admin password reset requested via configuration. Validating bootstrap target: {}", bootstrapUsername);
        userRepository.findByUsername(bootstrapUsername).ifPresentOrElse(user -> {
          boolean holdsSuperAdminRole = userRoleRepository.existsByUserIdAndRoleId(user.getId(), SUPER_ADMIN_ROLE_ID);
          if (!holdsSuperAdminRole) {
            log.error("Password reset requested for '{}', but this user does NOT hold the Super Admin role. Skipping password reset to prevent unintended privilege overwrite.", bootstrapUsername);
            return;
          }
          user.setPasswordHash(passwordEncoder.encode(bootstrapPassword));
          user.setPasswordAlgo("argon2id");
          user.setFailedLoginCount(0);
          user.setLockedUntil(null);
          user.setUserStatusId(ACTIVE_STATUS_ID);
          user.setUpdatedAt(LocalDateTime.now());
          userRepository.save(user);
          log.info("Super admin password successfully reset for verified Super Admin user: {}", bootstrapUsername);
        }, () -> {
          log.warn("Configured bootstrap user '{}' not found. Password reset skipped.", bootstrapUsername);
        });
      } else {
        log.info("Super Admin account(s) already exist (count: {}). Bootstrap check completed with no action needed.", existingSuperAdmins.size());
      }
      return;
    }

    log.info("No active Super Admin found in system. Evaluating Super Admin bootstrap initialization...");

    // Check if an existing account already claims the bootstrap username
    var existingUserOpt = userRepository.findByUsername(bootstrapUsername);

    if (existingUserOpt.isPresent()) {
      User existingUser = existingUserOpt.get();
      if (!allowExistingUserEscalation) {
        log.error("Found existing user with username '{}' (userId={}) without Super Admin role. "
            + "Refusing to silently escalate an arbitrary existing account to Super Admin without explicit recovery configuration "
            + "(app.bootstrap.superadmin.allow-existing-user-escalation=true).",
            bootstrapUsername, existingUser.getId());
        return;
      }

      log.warn("Explicit recovery mode authorized (allow-existing-user-escalation=true). "
          + "Granting Super Admin role (roleId={}) to existing user '{}' (userId={}).",
          SUPER_ADMIN_ROLE_ID, bootstrapUsername, existingUser.getId());

      if (!userRoleRepository.existsByUserIdAndRoleId(existingUser.getId(), SUPER_ADMIN_ROLE_ID)) {
        UserRole superAdminRole = new UserRole(existingUser.getId(), SUPER_ADMIN_ROLE_ID, existingUser.getId());
        superAdminRole.setAssignedAt(LocalDateTime.now());
        userRoleRepository.save(superAdminRole);
      }
      existingUser.setUserStatusId(ACTIVE_STATUS_ID);
      existingUser.setUpdatedAt(LocalDateTime.now());
      userRepository.save(existingUser);
      log.info("Existing user '{}' escalated to Super Admin via explicit recovery intent.", bootstrapUsername);
      return;
    }

    // Fresh installation: create dedicated Super Admin
    User newUser = new User();
    newUser.setUsername(bootstrapUsername);
    newUser.setEmail(bootstrapEmail);
    newUser.setPasswordHash(passwordEncoder.encode(bootstrapPassword));
    newUser.setPasswordAlgo("argon2id");
    newUser.setUserStatusId(ACTIVE_STATUS_ID);
    newUser.setFailedLoginCount(0);
    newUser.setMustChangePassword(false);
    LocalDateTime now = LocalDateTime.now();
    newUser.setCreatedAt(now);
    newUser.setUpdatedAt(now);
    User adminUser = userRepository.save(newUser);
    if (adminUser != null && adminUser.getId() != null) {
      UserRole superAdminRole = new UserRole(adminUser.getId(), SUPER_ADMIN_ROLE_ID, adminUser.getId());
      superAdminRole.setAssignedAt(now);
      userRoleRepository.save(superAdminRole);
      log.info("Bootstrap super admin account successfully initialized: {} (email: {})", adminUser.getUsername(), adminUser.getEmail());
    }
  }
}
