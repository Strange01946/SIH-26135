package in.gov.sih.sih26135.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;

import in.gov.sih.sih26135.dto.response.RolePermissionResponse;
import in.gov.sih.sih26135.dto.response.UserRoleResponse;
import in.gov.sih.sih26135.entity.RefUserStatus;
import in.gov.sih.sih26135.entity.User;
import in.gov.sih.sih26135.repository.RefUserStatusRepository;
import in.gov.sih.sih26135.repository.UserRepository;
import in.gov.sih.sih26135.service.RoleService;
import in.gov.sih.sih26135.service.UserService;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;

@ExtendWith(MockitoExtension.class)
class CustomUserDetailsServiceTest {

  @Mock
  private UserRepository userRepository;

  @Mock
  private RefUserStatusRepository refUserStatusRepository;

  @Mock
  private UserService userService;

  @Mock
  private RoleService roleService;

  private CustomUserDetailsService userDetailsService;

  @BeforeEach
  void setUp() {
    userDetailsService = new CustomUserDetailsService(
        userRepository,
        refUserStatusRepository,
        userService,
        roleService
    );
  }

  @Test
  void loadUserByUsername_blankOrNull_throwsException() {
    assertThatThrownBy(() -> userDetailsService.loadUserByUsername(null))
        .isInstanceOf(UsernameNotFoundException.class)
        .hasMessageContaining("blank");

    assertThatThrownBy(() -> userDetailsService.loadUserByUsername("   "))
        .isInstanceOf(UsernameNotFoundException.class)
        .hasMessageContaining("blank");
  }

  @Test
  void loadUserByUsername_userNotFound_throwsException() {
    when(userRepository.findByUsernameOrEmail("unknown", "unknown")).thenReturn(Optional.empty());

    assertThatThrownBy(() -> userDetailsService.loadUserByUsername("unknown"))
        .isInstanceOf(UsernameNotFoundException.class)
        .hasMessageContaining("User not found");
  }

  @Test
  void loadUserByUsername_softDeletedUser_throwsException() {
    User user = new User();
    user.setId(10L);
    user.setUsername("deleted_user");
    user.setDeletedAt(LocalDateTime.now());

    when(userRepository.findByUsernameOrEmail("deleted_user", "deleted_user")).thenReturn(Optional.of(user));

    assertThatThrownBy(() -> userDetailsService.loadUserByUsername("deleted_user"))
        .isInstanceOf(UsernameNotFoundException.class)
        .hasMessageContaining("deleted");
  }

  @Test
  void loadUserByUsername_inactiveStatus_throwsException() {
    User user = new User();
    user.setId(10L);
    user.setUsername("inactive_user");
    user.setUserStatusId(2L);

    RefUserStatus status = new RefUserStatus();
    status.setId(2L);
    status.setStatusCode("LOCKED");

    when(userRepository.findByUsernameOrEmail("inactive_user", "inactive_user")).thenReturn(Optional.of(user));
    when(refUserStatusRepository.findById(2L)).thenReturn(Optional.of(status));

    assertThatThrownBy(() -> userDetailsService.loadUserByUsername("inactive_user"))
        .isInstanceOf(UsernameNotFoundException.class)
        .hasMessageContaining("not active");
  }

  @Test
  void loadUserByUsername_activeUser_returnsUserDetailsWithAuthorities() {
    User user = new User();
    user.setId(100L);
    user.setUsername("portal_admin");
    user.setPasswordHash("$argon2id$v=19$m=65536,t=3,p=4$dummyHash");
    user.setUserStatusId(1L);

    RefUserStatus status = new RefUserStatus();
    status.setId(1L);
    status.setStatusCode("ACTIVE");

    UserRoleResponse roleResp = new UserRoleResponse();
    roleResp.setRoleId(5L);
    roleResp.setRoleCode("SUPER_ADMIN");

    RolePermissionResponse permResp = new RolePermissionResponse();
    permResp.setPermissionId(20L);
    permResp.setPermissionCode("USER_CREATE");

    when(userRepository.findByUsernameOrEmail("portal_admin", "portal_admin")).thenReturn(Optional.of(user));
    when(refUserStatusRepository.findById(1L)).thenReturn(Optional.of(status));
    when(userService.getUserRoles(100L)).thenReturn(List.of(roleResp));
    when(roleService.getRolePermissions(5L)).thenReturn(List.of(permResp));

    UserDetails details = userDetailsService.loadUserByUsername("portal_admin");

    assertThat(details).isNotNull();
    assertThat(details.getUsername()).isEqualTo("portal_admin");
    assertThat(details.getPassword()).isEqualTo("$argon2id$v=19$m=65536,t=3,p=4$dummyHash");
    assertThat(details.isEnabled()).isTrue();
    assertThat(details.isAccountNonLocked()).isTrue();

    List<String> authorities = details.getAuthorities().stream()
        .map(GrantedAuthority::getAuthority)
        .toList();
    assertThat(authorities).containsExactlyInAnyOrder("ROLE_SUPER_ADMIN", "USER_CREATE");
  }

  @Test
  void autoConfiguration_doesNotCreateInMemoryUserDetailsManager() {
    new ApplicationContextRunner()
        .withConfiguration(AutoConfigurations.of(
            UserDetailsServiceAutoConfiguration.class
        ))
        .withBean(CustomUserDetailsService.class, () -> userDetailsService)
        .run(context -> {
          assertThat(context).hasSingleBean(UserDetailsService.class);
          assertThat(context).getBean(UserDetailsService.class).isSameAs(userDetailsService);
          assertThat(context).doesNotHaveBean(InMemoryUserDetailsManager.class);
        });
  }
}
