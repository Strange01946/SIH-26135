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

    List<UserRole> existingSuperAdmins = userRoleRepository.findByRoleId(SUPER_ADMIN_ROLE_ID);

    if (!existingSuperAdmins.isEmpty()) {
      if (resetPassword) {
        log.warn("Super admin password reset requested via configuration. Updating password for bootstrap username: {}", bootstrapUsername);
        userRepository.findByUsername(bootstrapUsername).ifPresent(user -> {
          user.setPasswordHash(passwordEncoder.encode(bootstrapPassword));
          user.setPasswordAlgo("argon2id");
          user.setFailedLoginCount(0);
          user.setLockedUntil(null);
          user.setUserStatusId(ACTIVE_STATUS_ID);
          user.setUpdatedAt(LocalDateTime.now());
          userRepository.save(user);
          log.info("Super admin password successfully reset for user: {}", bootstrapUsername);
        });
      } else {
        log.info("Super Admin account(s) already exist (count: {}). Bootstrap check completed with no action needed.", existingSuperAdmins.size());
      }
      return;
    }

    log.info("No Super Admin found in system. Initializing default Super Admin bootstrap account...");

    User adminUser = userRepository.findByUsername(bootstrapUsername).orElseGet(() -> {
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
      return userRepository.save(newUser);
    });

    if (!userRoleRepository.existsByUserIdAndRoleId(adminUser.getId(), SUPER_ADMIN_ROLE_ID)) {
      UserRole superAdminRole = new UserRole(adminUser.getId(), SUPER_ADMIN_ROLE_ID, adminUser.getId());
      superAdminRole.setAssignedAt(LocalDateTime.now());
      userRoleRepository.save(superAdminRole);
    }

    log.info("Bootstrap super admin account initialized: {} (email: {})", adminUser.getUsername(), adminUser.getEmail());
  }
}
