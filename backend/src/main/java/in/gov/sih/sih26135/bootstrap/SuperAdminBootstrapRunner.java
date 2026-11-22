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
 *
 * Adheres strictly to the Active Super Admin invariant:
 * - An account is active iff deletedAt IS NULL AND userStatusId == 2L (ACTIVE).
 * - Password reset is strictly separated from account reactivation.
 * - Prevents silent privilege escalation of arbitrary existing accounts.
 */
@Component
@Order(10)
public class SuperAdminBootstrapRunner implements ApplicationRunner {

  private static final Logger log = LoggerFactory.getLogger(SuperAdminBootstrapRunner.class);
  private static final Long SUPER_ADMIN_ROLE_ID = 1L;
  private static final Long STATUS_PENDING_ID = 1L;
  private static final Long STATUS_ACTIVE_ID = 2L;
  private static final Long STATUS_LOCKED_ID = 3L;
  private static final Long STATUS_DISABLED_ID = 4L;
  private static final Long STATUS_ARCHIVED_ID = 5L;

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

  @Value("${app.bootstrap.superadmin.reactivate-disabled:false}")
  private boolean reactivateDisabled;

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

    // Find all non-deleted Super Admin user accounts
    List<User> existingSuperAdminUsers = userRoleRepository.findByRoleId(SUPER_ADMIN_ROLE_ID).stream()
        .map(ur -> userRepository.findById(ur.getUserId()).orElse(null))
        .filter(u -> u != null && u.getDeletedAt() == null)
        .toList();

    List<User> activeSuperAdmins = existingSuperAdminUsers.stream()
        .filter(u -> STATUS_ACTIVE_ID.equals(u.getUserStatusId()))
        .toList();

    // Case 1: Active Super Admin(s) exist in the system
    if (!activeSuperAdmins.isEmpty()) {
      if (resetPassword) {
        handlePasswordReset();
      } else {
        log.info("Super Admin account(s) already exist and are active (count: {}). Bootstrap check completed with no action needed.",
            activeSuperAdmins.size());
      }
      return;
    }

    // Case 2: Super Admin account(s) exist, but none are active (e.g. disabled, locked, or archived)
    if (!existingSuperAdminUsers.isEmpty()) {
      log.warn("Super Admin account(s) exist (count: {}), but NO active Super Admin was found. Evaluating status...",
          existingSuperAdminUsers.size());

      if (resetPassword) {
        handlePasswordReset();
        return;
      }

      // Check if bootstrap username belongs to an existing disabled Super Admin
      var targetOpt = userRepository.findByUsername(bootstrapUsername);
      if (targetOpt.isPresent() && userRoleRepository.existsByUserIdAndRoleId(targetOpt.get().getId(), SUPER_ADMIN_ROLE_ID)) {
        User target = targetOpt.get();
        if (reactivateDisabled) {
          target.setUserStatusId(STATUS_ACTIVE_ID);
          target.setPasswordHash(passwordEncoder.encode(bootstrapPassword));
          target.setPasswordAlgo("argon2id");
          target.setFailedLoginCount(0);
          target.setLockedUntil(null);
          target.setUpdatedAt(LocalDateTime.now());
          userRepository.save(target);
          log.warn("Reactivated disabled Super Admin '{}' via explicit recovery mode (reactivate-disabled=true).", bootstrapUsername);
          return;
        } else {
          log.error("Super Admin account '{}' is disabled (statusId={}). Refusing to silently reactivate without explicit configuration (app.bootstrap.superadmin.reactivate-disabled=true).",
              bootstrapUsername, target.getUserStatusId());
          return;
        }
      }

      log.error("Found {} existing Super Admin account(s), but none are active. Refusing to create a duplicate Super Admin without explicit recovery configuration.",
          existingSuperAdminUsers.size());
      return;
    }

    // Case 3: No Super Admin account exists in the system (fresh installation or soft-deleted)
    log.info("No Super Admin found in system. Evaluating initial bootstrap provisioning...");

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
      existingUser.setUserStatusId(STATUS_ACTIVE_ID);
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
    newUser.setUserStatusId(STATUS_ACTIVE_ID);
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

  private void handlePasswordReset() {
    log.warn("Super admin password reset requested via configuration. Validating bootstrap target: {}", bootstrapUsername);
    userRepository.findByUsername(bootstrapUsername).ifPresentOrElse(user -> {
      boolean holdsSuperAdminRole = userRoleRepository.existsByUserIdAndRoleId(user.getId(), SUPER_ADMIN_ROLE_ID);
      if (!holdsSuperAdminRole) {
        log.error("Password reset requested for '{}', but this user does NOT hold the Super Admin role. Skipping password reset to prevent unintended privilege overwrite.", bootstrapUsername);
        return;
      }
      if (user.getDeletedAt() != null) {
        log.error("Password reset requested for '{}', but this account has been deleted. Skipping password reset.", bootstrapUsername);
        return;
      }

      user.setPasswordHash(passwordEncoder.encode(bootstrapPassword));
      user.setPasswordAlgo("argon2id");
      user.setFailedLoginCount(0);
      user.setLockedUntil(null);

      // Separate password reset from account reactivation:
      // If locked due to failed attempts, reset unlocks it to ACTIVE
      if (STATUS_LOCKED_ID.equals(user.getUserStatusId())) {
        user.setUserStatusId(STATUS_ACTIVE_ID);
        log.info("Unlocked locked Super Admin account '{}' upon password reset.", bootstrapUsername);
      } else if (STATUS_DISABLED_ID.equals(user.getUserStatusId()) || STATUS_ARCHIVED_ID.equals(user.getUserStatusId())) {
        if (reactivateDisabled) {
          user.setUserStatusId(STATUS_ACTIVE_ID);
          log.warn("Reactivated disabled/archived Super Admin account '{}' via explicit configuration (reactivate-disabled=true).", bootstrapUsername);
        } else {
          log.info("Password updated for Super Admin account '{}', but account remains {} (silent reactivation prohibited).",
              bootstrapUsername, STATUS_DISABLED_ID.equals(user.getUserStatusId()) ? "DISABLED" : "ARCHIVED");
        }
      }

      user.setUpdatedAt(LocalDateTime.now());
      userRepository.save(user);
      log.info("Super admin password successfully updated for verified Super Admin user: {}", bootstrapUsername);
    }, () -> {
      log.warn("Configured bootstrap user '{}' not found. Password reset skipped.", bootstrapUsername);
    });
  }
}
