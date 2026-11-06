package in.gov.sih.sih26135.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import in.gov.sih.sih26135.dto.request.AnalyticsFilterCriteria;
import in.gov.sih.sih26135.entity.Trainee;
import in.gov.sih.sih26135.entity.User;
import in.gov.sih.sih26135.exception.BadRequestException;
import in.gov.sih.sih26135.repository.EmployerRepository;
import in.gov.sih.sih26135.repository.OrganizationRepository;
import in.gov.sih.sih26135.repository.TraineeRepository;
import in.gov.sih.sih26135.repository.TrainingProviderRepository;
import in.gov.sih.sih26135.repository.UserRepository;
import in.gov.sih.sih26135.security.jwt.JwtTokenProvider;
import in.gov.sih.sih26135.service.ExecutiveDashboardService;
import in.gov.sih.sih26135.service.GeographicDashboardService;
import in.gov.sih.sih26135.service.InstitutionalAnalyticsService;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@DisplayName("Phase 27.11 — Multi-Dimensional Query Filtering & Tenancy Security Tests")
class AnalyticsFilterSecurityTest {

  @Autowired
  private MockMvc mockMvc;

  @Autowired
  private JwtTokenProvider jwtTokenProvider;

  @MockBean
  private ExecutiveDashboardService executiveDashboardService;

  @MockBean
  private GeographicDashboardService geographicDashboardService;

  @MockBean
  private InstitutionalAnalyticsService institutionalAnalyticsService;

  private UserRepository userRepository;
  private TraineeRepository traineeRepository;
  private EmployerRepository employerRepository;
  private TrainingProviderRepository trainingProviderRepository;
  private OrganizationRepository organizationRepository;
  private WorkflowSecurityScopeValidator scopeValidator;

  // Test Personas
  private static final Long NATIONAL_ADMIN_ID = 91001L;
  private static final Long STATE_ADMIN_A_ID = 91002L;
  private static final Long PROVIDER_USER_ID = 91003L;
  private static final Long EMPLOYER_USER_ID = 91004L;
  private static final Long TRAINEE_USER_ID = 91005L;
  private static final Long PERMISSION_ONLY_ID = 91006L;

  private static final Long STATE_A_ID = 101L;
  private static final Long DISTRICT_A_ID = 1001L;
  private static final Long STATE_B_ID = 102L;
  private static final Long DISTRICT_B_ID = 1002L;

  private String tokenNationalAdmin;
  private String tokenStateAdminA;
  private String tokenProvider;
  private String tokenEmployer;
  private String tokenTrainee;
  private String tokenPermissionOnly;

  @BeforeEach
  void setUp() {
    userRepository = mock(UserRepository.class);
    traineeRepository = mock(TraineeRepository.class);
    employerRepository = mock(EmployerRepository.class);
    trainingProviderRepository = mock(TrainingProviderRepository.class);
    organizationRepository = mock(OrganizationRepository.class);

    scopeValidator = new WorkflowSecurityScopeValidator(
        userRepository,
        traineeRepository,
        employerRepository,
        trainingProviderRepository,
        organizationRepository
    );

    // District-to-State test lookup mapping
    scopeValidator.setDistrictStateLookup(districtId -> {
      if (DISTRICT_A_ID.equals(districtId)) return STATE_A_ID;
      if (DISTRICT_B_ID.equals(districtId)) return STATE_B_ID;
      return null;
    });

    // 1. National Admin User
    User natAdmin = new User();
    natAdmin.setId(NATIONAL_ADMIN_ID);
    natAdmin.setUsername("national_admin");
    natAdmin.setEmail("natadmin@msde.gov.in");
    when(userRepository.findById(NATIONAL_ADMIN_ID)).thenReturn(Optional.of(natAdmin));
    when(userRepository.findByUsernameOrEmail("national_admin", "national_admin")).thenReturn(Optional.of(natAdmin));

    // 2. State Admin User (State A)
    User stateAdminA = new User();
    stateAdminA.setId(STATE_ADMIN_A_ID);
    stateAdminA.setUsername("state_admin_a");
    stateAdminA.setEmail("statea@gov.in");
    stateAdminA.setStateId(STATE_A_ID);
    when(userRepository.findById(STATE_ADMIN_A_ID)).thenReturn(Optional.of(stateAdminA));
    when(userRepository.findByUsernameOrEmail("state_admin_a", "state_admin_a")).thenReturn(Optional.of(stateAdminA));

    // 3. Provider User
    User providerUser = new User();
    providerUser.setId(PROVIDER_USER_ID);
    providerUser.setUsername("tp_user");
    providerUser.setTrainingProviderId(501L);
    when(userRepository.findById(PROVIDER_USER_ID)).thenReturn(Optional.of(providerUser));
    when(userRepository.findByUsernameOrEmail("tp_user", "tp_user")).thenReturn(Optional.of(providerUser));

    // 4. Employer User
    User employerUser = new User();
    employerUser.setId(EMPLOYER_USER_ID);
    employerUser.setUsername("emp_user");
    employerUser.setEmployerId(601L);
    when(userRepository.findById(EMPLOYER_USER_ID)).thenReturn(Optional.of(employerUser));
    when(userRepository.findByUsernameOrEmail("emp_user", "emp_user")).thenReturn(Optional.of(employerUser));

    // 5. Trainee User
    User traineeUser = new User();
    traineeUser.setId(TRAINEE_USER_ID);
    traineeUser.setUsername("trainee_user");
    when(userRepository.findById(TRAINEE_USER_ID)).thenReturn(Optional.of(traineeUser));
    when(userRepository.findByUsernameOrEmail("trainee_user", "trainee_user")).thenReturn(Optional.of(traineeUser));

    Trainee trainee = new Trainee();
    trainee.setId(701L);
    trainee.setUser(traineeUser);
    when(traineeRepository.findByUserId(TRAINEE_USER_ID)).thenReturn(Optional.of(trainee));

    // 6. Permission-Only User
    User permUser = new User();
    permUser.setId(PERMISSION_ONLY_ID);
    permUser.setUsername("perm_user");
    when(userRepository.findById(PERMISSION_ONLY_ID)).thenReturn(Optional.of(permUser));
    when(userRepository.findByUsernameOrEmail("perm_user", "perm_user")).thenReturn(Optional.of(permUser));

    // Generate JWT tokens
    tokenNationalAdmin = "Bearer " + jwtTokenProvider.generateAccessToken(
        NATIONAL_ADMIN_ID, "national_admin", List.of("ROLE_government_admin", "analytics.read"));
    tokenStateAdminA = "Bearer " + jwtTokenProvider.generateAccessToken(
        STATE_ADMIN_A_ID, "state_admin_a", List.of("ROLE_government_admin", "analytics.read"));
    tokenProvider = "Bearer " + jwtTokenProvider.generateAccessToken(
        PROVIDER_USER_ID, "tp_user", List.of("ROLE_training_provider", "analytics.read"));
    tokenEmployer = "Bearer " + jwtTokenProvider.generateAccessToken(
        EMPLOYER_USER_ID, "emp_user", List.of("ROLE_employer", "analytics.read"));
    tokenTrainee = "Bearer " + jwtTokenProvider.generateAccessToken(
        TRAINEE_USER_ID, "trainee_user", List.of("ROLE_trainee", "analytics.read"));
    tokenPermissionOnly = "Bearer " + jwtTokenProvider.generateAccessToken(
        PERMISSION_ONLY_ID, "perm_user", List.of("analytics.read"));
  }

  @AfterEach
  void tearDown() {
    SecurityContextHolder.clearContext();
  }

  private void setAuth(Long userId, String username, String... roles) {
    List<SimpleGrantedAuthority> auths = java.util.Arrays.stream(roles)
        .map(SimpleGrantedAuthority::new)
        .toList();
    UserPrincipal principal = new UserPrincipal(userId, username);
    SecurityContextHolder.getContext().setAuthentication(
        new UsernamePasswordAuthenticationToken(principal, null, auths)
    );
  }

  @Nested
  @DisplayName("1. National Administrator Permitted Filter Scope")
  class NationalAdminScopeTests {

    @Test
    @DisplayName("National Admin can filter by any valid state and district")
    void testNationalAdminValidSpatialFilter() {
      setAuth(NATIONAL_ADMIN_ID, "national_admin", "ROLE_government_admin", "analytics.read");

      AnalyticsFilterCriteria criteriaA = new AnalyticsFilterCriteria();
      criteriaA.setStateId(STATE_A_ID);
      criteriaA.setDistrictId(DISTRICT_A_ID);
      assertThatCode(() -> scopeValidator.validateAnalyticsFilterScope(criteriaA))
          .doesNotThrowAnyException();

      AnalyticsFilterCriteria criteriaB = new AnalyticsFilterCriteria();
      criteriaB.setStateId(STATE_B_ID);
      criteriaB.setDistrictId(DISTRICT_B_ID);
      assertThatCode(() -> scopeValidator.validateAnalyticsFilterScope(criteriaB))
          .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("National Admin supplying contradictory state & district throws BadRequestException")
    void testNationalAdminContradictorySpatialFilter() {
      setAuth(NATIONAL_ADMIN_ID, "national_admin", "ROLE_government_admin", "analytics.read");

      // District B (in State B) requested alongside State A -> Contradictory!
      AnalyticsFilterCriteria criteria = new AnalyticsFilterCriteria();
      criteria.setStateId(STATE_A_ID);
      criteria.setDistrictId(DISTRICT_B_ID);

      assertThatThrownBy(() -> scopeValidator.validateAnalyticsFilterScope(criteria))
          .isInstanceOf(BadRequestException.class)
          .hasFieldOrPropertyWithValue("errorCode", "CONTRADICTORY_SPATIAL_FILTER");
    }
  }

  @Nested
  @DisplayName("2. State Administrator Tenancy Boundaries")
  class StateAdminScopeTests {

    @Test
    @DisplayName("State Admin A filtering by assigned State A and constituent District A is permitted")
    void testStateAdminOwnStatePermitted() {
      setAuth(STATE_ADMIN_A_ID, "state_admin_a", "ROLE_government_admin", "analytics.read");

      AnalyticsFilterCriteria criteria = new AnalyticsFilterCriteria();
      criteria.setStateId(STATE_A_ID);
      criteria.setDistrictId(DISTRICT_A_ID);

      assertThatCode(() -> scopeValidator.validateAnalyticsFilterScope(criteria))
          .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("State Admin A without explicit stateId automatically inherits State A tenancy")
    void testStateAdminAutoInheritsState() {
      setAuth(STATE_ADMIN_A_ID, "state_admin_a", "ROLE_government_admin", "analytics.read");

      AnalyticsFilterCriteria criteria = new AnalyticsFilterCriteria();
      criteria.setFiscalYear("FY2024-25");

      scopeValidator.validateAnalyticsFilterScope(criteria);
      assertThat(criteria.getStateId()).isEqualTo(STATE_A_ID);
    }

    @Test
    @DisplayName("State Admin A attempting cross-state filter (State B) throws AccessDeniedException")
    void testStateAdminCrossStateForbidden() {
      setAuth(STATE_ADMIN_A_ID, "state_admin_a", "ROLE_government_admin", "analytics.read");

      AnalyticsFilterCriteria criteria = new AnalyticsFilterCriteria();
      criteria.setStateId(STATE_B_ID);

      assertThatThrownBy(() -> scopeValidator.validateAnalyticsFilterScope(criteria))
          .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    @DisplayName("State Admin A attempting cross-district filter (District B) throws AccessDeniedException")
    void testStateAdminCrossDistrictForbidden() {
      setAuth(STATE_ADMIN_A_ID, "state_admin_a", "ROLE_government_admin", "analytics.read");

      AnalyticsFilterCriteria criteria = new AnalyticsFilterCriteria();
      criteria.setDistrictId(DISTRICT_B_ID);

      assertThatThrownBy(() -> scopeValidator.validateAnalyticsFilterScope(criteria))
          .isInstanceOf(AccessDeniedException.class);
    }
  }

  @Nested
  @DisplayName("3. Restricted Persona Boundary Isolation (Provider, Employer, Trainee)")
  class RestrictedPersonaBoundaryTests {

    @Test
    @DisplayName("Training Provider attempting spatial filter throws AccessDeniedException")
    void testProviderSpatialForbidden() {
      setAuth(PROVIDER_USER_ID, "tp_user", "ROLE_training_provider", "analytics.read");

      AnalyticsFilterCriteria criteria = new AnalyticsFilterCriteria();
      criteria.setStateId(STATE_A_ID);

      assertThatThrownBy(() -> scopeValidator.validateAnalyticsFilterScope(criteria))
          .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    @DisplayName("Employer attempting spatial filter throws AccessDeniedException")
    void testEmployerSpatialForbidden() {
      setAuth(EMPLOYER_USER_ID, "emp_user", "ROLE_employer", "analytics.read");

      AnalyticsFilterCriteria criteria = new AnalyticsFilterCriteria();
      criteria.setDistrictId(DISTRICT_A_ID);

      assertThatThrownBy(() -> scopeValidator.validateAnalyticsFilterScope(criteria))
          .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    @DisplayName("Trainee attempting spatial filter throws AccessDeniedException")
    void testTraineeSpatialForbidden() {
      setAuth(TRAINEE_USER_ID, "trainee_user", "ROLE_trainee", "analytics.read");

      AnalyticsFilterCriteria criteria = new AnalyticsFilterCriteria();
      criteria.setStateId(STATE_A_ID);

      assertThatThrownBy(() -> scopeValidator.validateAnalyticsFilterScope(criteria))
          .isInstanceOf(AccessDeniedException.class);
    }
  }

  @Nested
  @DisplayName("4. Permission-Only and Unauthenticated Fail-Closed Protection")
  class FailClosedSecurityTests {

    @Test
    @DisplayName("Permission-only user attempting spatial filtering throws AccessDeniedException")
    void testPermissionOnlySpatialForbidden() {
      setAuth(PERMISSION_ONLY_ID, "perm_user", "analytics.read");

      AnalyticsFilterCriteria criteria = new AnalyticsFilterCriteria();
      criteria.setStateId(STATE_A_ID);

      assertThatThrownBy(() -> scopeValidator.validateAnalyticsFilterScope(criteria))
          .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    @DisplayName("Unauthenticated request attempting spatial filter throws AccessDeniedException")
    void testUnauthenticatedSpatialForbidden() {
      SecurityContextHolder.clearContext();

      AnalyticsFilterCriteria criteria = new AnalyticsFilterCriteria();
      criteria.setStateId(STATE_A_ID);

      assertThatThrownBy(() -> scopeValidator.validateAnalyticsFilterScope(criteria))
          .isInstanceOf(AccessDeniedException.class);
    }
  }
}
