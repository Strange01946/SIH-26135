package in.gov.sih.sih26135.security;

import in.gov.sih.sih26135.dto.response.RolePermissionResponse;
import in.gov.sih.sih26135.dto.response.UserRoleResponse;
import in.gov.sih.sih26135.entity.RefUserStatus;
import in.gov.sih.sih26135.entity.User;
import in.gov.sih.sih26135.repository.RefUserStatusRepository;
import in.gov.sih.sih26135.repository.UserRepository;
import in.gov.sih.sih26135.service.RoleService;
import in.gov.sih.sih26135.service.UserService;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Production database-backed {@link UserDetailsService} bridging the SIH 26135 User
 * database entity, role mappings, and granular permissions with Spring Security.
 *
 * <p>Enforces:
 * <ul>
 *   <li>Database user retrieval by username or email via {@link UserRepository}.</li>
 *   <li>Soft-delete rejection (deletedAt must be null).</li>
 *   <li>Active account status validation (RefUserStatus must be "ACTIVE").</li>
 *   <li>Effective authority compilation from assigned roles (ROLE_&lt;code&gt;) and permissions.</li>
 *   <li>Strictly zero in-memory, dummy, or hardcoded credentials.</li>
 * </ul>
 */
@Service
@Transactional(readOnly = true)
public class CustomUserDetailsService implements UserDetailsService {

  private static final String STATUS_ACTIVE = "ACTIVE";

  private final UserRepository userRepository;
  private final RefUserStatusRepository refUserStatusRepository;
  private final UserService userService;
  private final RoleService roleService;

  public CustomUserDetailsService(
      UserRepository userRepository,
      RefUserStatusRepository refUserStatusRepository,
      UserService userService,
      RoleService roleService) {
    this.userRepository = userRepository;
    this.refUserStatusRepository = refUserStatusRepository;
    this.userService = userService;
    this.roleService = roleService;
  }

  @Override
  public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
    if (username == null || username.isBlank()) {
      throw new UsernameNotFoundException("Username or email cannot be blank");
    }

    String identifier = username.trim();

    User user = userRepository.findByUsernameOrEmail(identifier, identifier)
        .orElseThrow(() -> new UsernameNotFoundException("User not found with identifier: " + identifier));

    if (user.getDeletedAt() != null) {
      throw new UsernameNotFoundException("User account has been deleted: " + identifier);
    }

    RefUserStatus status = refUserStatusRepository.findById(user.getUserStatusId())
        .orElseThrow(() -> new UsernameNotFoundException("User status not found for user: " + identifier));

    if (!STATUS_ACTIVE.equalsIgnoreCase(status.getStatusCode())) {
      throw new UsernameNotFoundException("User account is not active (status: " + status.getStatusCode() + "): " + identifier);
    }

    List<GrantedAuthority> authorities = buildEffectiveAuthorities(user.getId());

    return org.springframework.security.core.userdetails.User.builder()
        .username(user.getUsername())
        .password(user.getPasswordHash())
        .authorities(authorities)
        .accountExpired(false)
        .accountLocked(false)
        .credentialsExpired(false)
        .disabled(false)
        .build();
  }

  private List<GrantedAuthority> buildEffectiveAuthorities(Long userId) {
    Set<String> authorityStrings = new LinkedHashSet<>();
    List<UserRoleResponse> userRoles = userService.getUserRoles(userId);

    if (userRoles != null) {
      for (UserRoleResponse ur : userRoles) {
        if (ur.getRoleCode() != null && !ur.getRoleCode().isBlank()) {
          authorityStrings.add("ROLE_" + ur.getRoleCode().trim());
        }
        if (ur.getRoleId() != null) {
          List<RolePermissionResponse> permissions = roleService.getRolePermissions(ur.getRoleId());
          if (permissions != null) {
            for (RolePermissionResponse perm : permissions) {
              if (perm.getPermissionCode() != null && !perm.getPermissionCode().isBlank()) {
                authorityStrings.add(perm.getPermissionCode().trim());
              }
            }
          }
        }
      }
    }

    List<GrantedAuthority> authorities = new ArrayList<>(authorityStrings.size());
    for (String auth : authorityStrings) {
      authorities.add(new SimpleGrantedAuthority(auth));
    }
    return authorities;
  }
}
