package in.gov.sih.sih26135.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

import in.gov.sih.sih26135.entity.Employer;
import in.gov.sih.sih26135.entity.TrainingProvider;
import in.gov.sih.sih26135.entity.Trainee;
import in.gov.sih.sih26135.entity.User;
import in.gov.sih.sih26135.exception.BadRequestException;
import in.gov.sih.sih26135.repository.EmployerRepository;
import in.gov.sih.sih26135.repository.OrganizationRepository;
import in.gov.sih.sih26135.repository.TraineeRepository;
import in.gov.sih.sih26135.repository.TrainingProviderRepository;
import in.gov.sih.sih26135.repository.UserRepository;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

@ExtendWith(MockitoExtension.class)
@DisplayName("WorkflowSecurityScopeValidator Unit Tests")
class WorkflowSecurityScopeValidatorTest {

  @Mock private UserRepository userRepository;
  @Mock private TraineeRepository traineeRepository;
  @Mock private EmployerRepository employerRepository;
  @Mock private TrainingProviderRepository trainingProviderRepository;
  @Mock private OrganizationRepository organizationRepository;

  private WorkflowSecurityScopeValidator validator;

  @BeforeEach
  void setUp() {
    validator = new WorkflowSecurityScopeValidator(
        userRepository, traineeRepository, employerRepository, trainingProviderRepository, organizationRepository);

    lenient().when(organizationRepository.existsById(500L)).thenReturn(true);
    lenient().when(organizationRepository.existsById(700L)).thenReturn(true);
    lenient().when(organizationRepository.existsById(800L)).thenReturn(true);
    lenient().when(organizationRepository.existsById(900L)).thenReturn(true);

    Employer defaultEmployer = new Employer();
    defaultEmployer.setId(100L);
    defaultEmployer.setOrganizationId(500L);
    lenient().when(employerRepository.findById(100L)).thenReturn(Optional.of(defaultEmployer));

    Employer employer200 = new Employer();
    employer200.setId(200L);
    employer200.setOrganizationId(500L);
    lenient().when(employerRepository.findById(200L)).thenReturn(Optional.of(employer200));

    Employer employer999 = new Employer();
    employer999.setId(999L);
    employer999.setOrganizationId(500L);
    lenient().when(employerRepository.findById(999L)).thenReturn(Optional.of(employer999));

    TrainingProvider defaultProvider = new TrainingProvider();
    defaultProvider.setId(300L);
    defaultProvider.setOrganizationId(500L);
    lenient().when(trainingProviderRepository.findById(300L)).thenReturn(Optional.of(defaultProvider));

    TrainingProvider provider400 = new TrainingProvider();
    provider400.setId(400L);
    provider400.setOrganizationId(500L);
    lenient().when(trainingProviderRepository.findById(400L)).thenReturn(Optional.of(provider400));

    TrainingProvider provider999 = new TrainingProvider();
    provider999.setId(999L);
    provider999.setOrganizationId(500L);
    lenient().when(trainingProviderRepository.findById(999L)).thenReturn(Optional.of(provider999));

    lenient().when(traineeRepository.existsById(500L)).thenReturn(true);
    lenient().when(traineeRepository.existsById(600L)).thenReturn(true);
    lenient().when(traineeRepository.existsById(999L)).thenReturn(true);
  }

  @AfterEach
  void tearDown() {
    SecurityContextHolder.clearContext();
  }

  private void setAuthenticatedUser(Long userId, String username, List<String> roles) {
    UserPrincipal principal = new UserPrincipal(userId, username);
    List<SimpleGrantedAuthority> authorities = roles.stream()
        .map(SimpleGrantedAuthority::new)
        .toList();
    SecurityContextHolder.getContext().setAuthentication(
        new UsernamePasswordAuthenticationToken(principal, null, authorities));
  }

  // =========================================================================
  // 1. Employer Scope Validation Tests
  // =========================================================================
  @Nested
  @DisplayName("1. Employer Scope Enforcement")
  class EmployerScopeTests {

    @Test
    @DisplayName("Permits in-scope operation when authenticated employer matches target employer")
    void testValidateEmployerScope_inScopePermitted() {
      setAuthenticatedUser(10L, "emp_user", List.of("ROLE_employer", "placement.manage"));

      User user = new User();
      user.setId(10L);
      user.setEmployerId(100L);
      user.setOrganizationId(500L);
      lenient().when(userRepository.findById(10L)).thenReturn(Optional.of(user));

      Employer employer = new Employer();
      employer.setId(100L);
      employer.setOrganizationId(500L);
      when(employerRepository.findById(100L)).thenReturn(Optional.of(employer));

      assertThatCode(() -> validator.validateEmployerScope(100L))
          .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("Rejects out-of-scope operation when authenticated employer differs from target employer")
    void testValidateEmployerScope_crossEmployerRejected() {
      setAuthenticatedUser(10L, "emp_user", List.of("ROLE_employer", "placement.manage"));

      User user = new User();
      user.setId(10L);
      user.setEmployerId(100L);
      lenient().when(userRepository.findById(10L)).thenReturn(Optional.of(user));

      assertThatThrownBy(() -> validator.validateEmployerScope(200L))
          .isInstanceOf(BadRequestException.class)
          .extracting("errorCode").isEqualTo("EMPLOYER_MISMATCH");
    }

    @Test
    @DisplayName("Administrative user bypasses employer scope checks across multiple employers")
    void testValidateEmployerScope_adminBypasses() {
      setAuthenticatedUser(1L, "admin_user", List.of("ROLE_super_admin"));

      assertThatCode(() -> validator.validateEmployerScope(999L))
          .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("Rejects employer scope check when authenticated user has no employer assignment")
    void testValidateEmployerScope_userWithoutEmployerAssignmentRejected() {
      setAuthenticatedUser(20L, "gov_user", List.of("placement.manage"));

      User user = new User();
      user.setId(20L);
      user.setEmployerId(null);
      when(userRepository.findById(20L)).thenReturn(Optional.of(user));

      assertThatThrownBy(() -> validator.validateEmployerScope(100L))
          .isInstanceOf(BadRequestException.class)
          .extracting("errorCode").isEqualTo("EMPLOYER_ASSIGNMENT_REQUIRED");
    }

    @Test
    @DisplayName("Rejects employer scope check when authenticated user is unresolved")
    void testValidateEmployerScope_unresolvedUserRejected() {
      SecurityContextHolder.clearContext();

      assertThatThrownBy(() -> validator.validateEmployerScope(100L))
          .isInstanceOf(BadRequestException.class)
          .extracting("errorCode").isEqualTo("AUTHENTICATED_USER_UNRESOLVED");
    }

    @Test
    @DisplayName("Rejects employer scope check when target employer ID is null")
    void testValidateEmployerScope_nullTargetIdRejected() {
      setAuthenticatedUser(10L, "emp_user", List.of("ROLE_employer"));

      assertThatThrownBy(() -> validator.validateEmployerScope(null))
          .isInstanceOf(BadRequestException.class)
          .extracting("errorCode").isEqualTo("EMPLOYER_ID_REQUIRED");
    }
  }

  // =========================================================================
  // 2. Training Provider Scope Validation Tests
  // =========================================================================
  @Nested
  @DisplayName("2. Training Provider Scope Enforcement")
  class ProviderScopeTests {

    @Test
    @DisplayName("Permits in-scope operation when authenticated provider matches target provider")
    void testValidateProviderScope_inScopePermitted() {
      setAuthenticatedUser(30L, "tp_user", List.of("ROLE_training_provider", "enrollment.manage"));

      User user = new User();
      user.setId(30L);
      user.setTrainingProviderId(300L);
      user.setOrganizationId(500L);
      lenient().when(userRepository.findById(30L)).thenReturn(Optional.of(user));

      TrainingProvider provider = new TrainingProvider();
      provider.setId(300L);
      provider.setOrganizationId(500L);
      when(trainingProviderRepository.findById(300L)).thenReturn(Optional.of(provider));

      assertThatCode(() -> validator.validateProviderScope(300L))
          .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("Rejects out-of-scope operation when authenticated provider differs from target provider")
    void testValidateProviderScope_crossProviderRejected() {
      setAuthenticatedUser(30L, "tp_user", List.of("ROLE_training_provider", "enrollment.manage"));

      User user = new User();
      user.setId(30L);
      user.setTrainingProviderId(300L);
      lenient().when(userRepository.findById(30L)).thenReturn(Optional.of(user));

      assertThatThrownBy(() -> validator.validateProviderScope(400L))
          .isInstanceOf(BadRequestException.class)
          .extracting("errorCode").isEqualTo("PROVIDER_MISMATCH");
    }

    @Test
    @DisplayName("Government admin bypasses provider scope checks")
    void testValidateProviderScope_govAdminBypasses() {
      setAuthenticatedUser(2L, "gov_admin", List.of("ROLE_government_admin"));

      assertThatCode(() -> validator.validateProviderScope(999L))
          .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("Rejects provider scope check when authenticated user has no provider assignment")
    void testValidateProviderScope_userWithoutProviderAssignmentRejected() {
      setAuthenticatedUser(31L, "unassigned_user", List.of("ROLE_user"));

      User user = new User();
      user.setId(31L);
      user.setTrainingProviderId(null);
      when(userRepository.findById(31L)).thenReturn(Optional.of(user));

      assertThatThrownBy(() -> validator.validateProviderScope(300L))
          .isInstanceOf(BadRequestException.class)
          .extracting("errorCode").isEqualTo("PROVIDER_ASSIGNMENT_REQUIRED");
    }

    @Test
    @DisplayName("Rejects provider scope check when authenticated user is unresolved")
    void testValidateProviderScope_unresolvedUserRejected() {
      SecurityContextHolder.clearContext();

      assertThatThrownBy(() -> validator.validateProviderScope(300L))
          .isInstanceOf(BadRequestException.class)
          .extracting("errorCode").isEqualTo("AUTHENTICATED_USER_UNRESOLVED");
    }

    @Test
    @DisplayName("Rejects provider scope check when target provider ID is null")
    void testValidateProviderScope_nullTargetIdRejected() {
      setAuthenticatedUser(30L, "tp_user", List.of("ROLE_training_provider"));

      assertThatThrownBy(() -> validator.validateProviderScope(null))
          .isInstanceOf(BadRequestException.class)
          .extracting("errorCode").isEqualTo("PROVIDER_ID_REQUIRED");
    }
  }

  // =========================================================================
  // 3. Trainee Scope Validation Tests
  // =========================================================================
  @Nested
  @DisplayName("3. Trainee Self-Service Scope Enforcement")
  class TraineeScopeTests {

    @Test
    @DisplayName("Permits self-service action when authenticated trainee matches target trainee")
    void testValidateTraineeScope_selfServicePermitted() {
      setAuthenticatedUser(50L, "trainee_user", List.of("ROLE_trainee"));

      User user = new User();
      user.setId(50L);
      when(userRepository.findById(50L)).thenReturn(Optional.of(user));

      Trainee trainee = new Trainee();
      trainee.setId(500L);
      when(traineeRepository.findByUserId(50L)).thenReturn(Optional.of(trainee));

      assertThatCode(() -> validator.validateTraineeScope(500L))
          .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("Rejects out-of-scope action when authenticated trainee attempts to access another trainee")
    void testValidateTraineeScope_crossTraineeRejected() {
      setAuthenticatedUser(50L, "trainee_user", List.of("ROLE_trainee"));

      User user = new User();
      user.setId(50L);
      when(userRepository.findById(50L)).thenReturn(Optional.of(user));

      Trainee trainee = new Trainee();
      trainee.setId(500L);
      when(traineeRepository.findByUserId(50L)).thenReturn(Optional.of(trainee));

      assertThatThrownBy(() -> validator.validateTraineeScope(600L))
          .isInstanceOf(BadRequestException.class)
          .extracting("errorCode").isEqualTo("TRAINEE_MISMATCH");
    }

    @Test
    @DisplayName("Administrator bypasses trainee scope checks")
    void testValidateTraineeScope_adminBypasses() {
      setAuthenticatedUser(1L, "admin_user", List.of("ROLE_government_admin"));

      assertThatCode(() -> validator.validateTraineeScope(999L))
          .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("Rejects trainee scope check when authenticated user has no trainee profile")
    void testValidateTraineeScope_userWithoutTraineeProfileRejected() {
      setAuthenticatedUser(51L, "non_trainee_user", List.of("ROLE_user"));

      User user = new User();
      user.setId(51L);
      when(userRepository.findById(51L)).thenReturn(Optional.of(user));
      when(traineeRepository.findByUserId(51L)).thenReturn(Optional.empty());

      assertThatThrownBy(() -> validator.validateTraineeScope(500L))
          .isInstanceOf(BadRequestException.class)
          .extracting("errorCode").isEqualTo("TRAINEE_ASSIGNMENT_REQUIRED");
    }

    @Test
    @DisplayName("Rejects trainee scope check when authenticated user is unresolved")
    void testValidateTraineeScope_unresolvedUserRejected() {
      SecurityContextHolder.clearContext();

      assertThatThrownBy(() -> validator.validateTraineeScope(500L))
          .isInstanceOf(BadRequestException.class)
          .extracting("errorCode").isEqualTo("AUTHENTICATED_USER_UNRESOLVED");
    }

    @Test
    @DisplayName("Rejects trainee scope check when target trainee ID is null")
    void testValidateTraineeScope_nullTargetIdRejected() {
      setAuthenticatedUser(50L, "trainee_user", List.of("ROLE_trainee"));

      assertThatThrownBy(() -> validator.validateTraineeScope(null))
          .isInstanceOf(BadRequestException.class)
          .extracting("errorCode").isEqualTo("TRAINEE_ID_REQUIRED");
    }
  }

  // =========================================================================
  // 4. Organization Scope Validation Tests
  // =========================================================================
  @Nested
  @DisplayName("4. Organization Scope Enforcement")
  class OrganizationScopeTests {

    @Test
    @DisplayName("Permits in-scope operation when authenticated organization matches target organization")
    void testValidateOrganizationScope_inScopePermitted() {
      setAuthenticatedUser(70L, "org_user", List.of("ROLE_analyst"));

      User user = new User();
      user.setId(70L);
      user.setOrganizationId(700L);
      when(userRepository.findById(70L)).thenReturn(Optional.of(user));

      assertThatCode(() -> validator.validateOrganizationScope(700L))
          .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("Non-admin organization scope check fails closed when target organization record not found")
    void testValidateOrganizationScope_targetOrgNotFound_failsClosed() {
      setAuthenticatedUser(70L, "org_user", List.of("ROLE_analyst"));

      User user = new User();
      user.setId(70L);
      user.setOrganizationId(700L);
      when(userRepository.findById(70L)).thenReturn(Optional.of(user));
      when(organizationRepository.existsById(700L)).thenReturn(false);

      assertThatThrownBy(() -> validator.validateOrganizationScope(700L))
          .isInstanceOf(BadRequestException.class)
          .extracting("errorCode").isEqualTo("ORGANIZATION_NOT_FOUND");
    }

    @Test
    @DisplayName("Rejects out-of-scope operation when authenticated organization differs from target organization")
    void testValidateOrganizationScope_crossOrganizationRejected() {
      setAuthenticatedUser(70L, "org_user", List.of("ROLE_analyst"));

      User user = new User();
      user.setId(70L);
      user.setOrganizationId(700L);
      when(userRepository.findById(70L)).thenReturn(Optional.of(user));

      assertThatThrownBy(() -> validator.validateOrganizationScope(800L))
          .isInstanceOf(BadRequestException.class)
          .extracting("errorCode").isEqualTo("ORGANIZATION_MISMATCH");
    }

    @Test
    @DisplayName("Rejects organization scope check when authenticated user has no organization assignment")
    void testValidateOrganizationScope_userWithoutOrganizationAssignmentRejected() {
      setAuthenticatedUser(73L, "unassigned_user", List.of("ROLE_user"));

      User user = new User();
      user.setId(73L);
      user.setOrganizationId(null);
      when(userRepository.findById(73L)).thenReturn(Optional.of(user));

      assertThatThrownBy(() -> validator.validateOrganizationScope(700L))
          .isInstanceOf(BadRequestException.class)
          .extracting("errorCode").isEqualTo("ORGANIZATION_ASSIGNMENT_REQUIRED");
    }

    @Test
    @DisplayName("Rejects organization scope check when authenticated user is unresolved")
    void testValidateOrganizationScope_unresolvedUserRejected() {
      SecurityContextHolder.clearContext();

      assertThatThrownBy(() -> validator.validateOrganizationScope(700L))
          .isInstanceOf(BadRequestException.class)
          .extracting("errorCode").isEqualTo("AUTHENTICATED_USER_UNRESOLVED");
    }

    @Test
    @DisplayName("Rejects organization scope check when target organization ID is null")
    void testValidateOrganizationScope_nullTargetIdRejected() {
      setAuthenticatedUser(70L, "org_user", List.of("ROLE_analyst"));

      assertThatThrownBy(() -> validator.validateOrganizationScope(null))
          .isInstanceOf(BadRequestException.class)
          .extracting("errorCode").isEqualTo("ORGANIZATION_ID_REQUIRED");
    }

    @Test
    @DisplayName("Employer scope check rejects when employer belongs to a different organization")
    void testValidateEmployerScope_crossOrganizationRejected() {
      setAuthenticatedUser(71L, "org_officer", List.of("ROLE_analyst"));

      User user = new User();
      user.setId(71L);
      user.setEmployerId(25L);
      user.setOrganizationId(700L);
      when(userRepository.findById(71L)).thenReturn(Optional.of(user));

      Employer employer = new Employer();
      employer.setId(25L);
      employer.setOrganizationId(800L);
      when(employerRepository.findById(25L)).thenReturn(Optional.of(employer));

      assertThatThrownBy(() -> validator.validateEmployerScope(25L))
          .isInstanceOf(BadRequestException.class)
          .extracting("errorCode").isEqualTo("ORGANIZATION_MISMATCH");
    }

    @Test
    @DisplayName("Employer scope check permits when employer belongs to same organization")
    void testValidateEmployerScope_inScopeOrganizationPermitted() {
      setAuthenticatedUser(71L, "org_officer", List.of("ROLE_analyst"));

      User user = new User();
      user.setId(71L);
      user.setEmployerId(25L);
      user.setOrganizationId(700L);
      when(userRepository.findById(71L)).thenReturn(Optional.of(user));

      Employer employer = new Employer();
      employer.setId(25L);
      employer.setOrganizationId(700L);
      when(employerRepository.findById(25L)).thenReturn(Optional.of(employer));

      assertThatCode(() -> validator.validateEmployerScope(25L))
          .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("Provider scope check rejects when training provider belongs to a different organization")
    void testValidateProviderScope_crossOrganizationRejected() {
      setAuthenticatedUser(72L, "org_officer", List.of("ROLE_analyst"));

      User user = new User();
      user.setId(72L);
      user.setTrainingProviderId(35L);
      user.setOrganizationId(700L);
      when(userRepository.findById(72L)).thenReturn(Optional.of(user));

      TrainingProvider provider = new TrainingProvider();
      provider.setId(35L);
      provider.setOrganizationId(900L);
      when(trainingProviderRepository.findById(35L)).thenReturn(Optional.of(provider));

      assertThatThrownBy(() -> validator.validateProviderScope(35L))
          .isInstanceOf(BadRequestException.class)
          .extracting("errorCode").isEqualTo("ORGANIZATION_MISMATCH");
    }

    @Test
    @DisplayName("Provider scope check permits when training provider belongs to same organization")
    void testValidateProviderScope_inScopeOrganizationPermitted() {
      setAuthenticatedUser(72L, "org_officer", List.of("ROLE_analyst"));

      User user = new User();
      user.setId(72L);
      user.setTrainingProviderId(35L);
      user.setOrganizationId(700L);
      when(userRepository.findById(72L)).thenReturn(Optional.of(user));

      TrainingProvider provider = new TrainingProvider();
      provider.setId(35L);
      provider.setOrganizationId(700L);
      when(trainingProviderRepository.findById(35L)).thenReturn(Optional.of(provider));

      assertThatCode(() -> validator.validateProviderScope(35L))
          .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("Employer scope check fails closed when target employer record is missing")
    void testValidateEmployerScope_targetEmployerNotFound_failsClosed() {
      setAuthenticatedUser(71L, "org_officer", List.of("ROLE_analyst"));

      User user = new User();
      user.setId(71L);
      user.setEmployerId(25L);
      user.setOrganizationId(700L);
      lenient().when(userRepository.findById(71L)).thenReturn(Optional.of(user));
      when(employerRepository.findById(25L)).thenReturn(Optional.empty());

      assertThatThrownBy(() -> validator.validateEmployerScope(25L))
          .isInstanceOf(BadRequestException.class)
          .extracting("errorCode").isEqualTo("EMPLOYER_NOT_FOUND");
    }

    @Test
    @DisplayName("Employer scope check fails closed when target employer is missing organization affiliation")
    void testValidateEmployerScope_targetEmployerMissingOrganizationAffiliation_failsClosed() {
      setAuthenticatedUser(71L, "org_officer", List.of("ROLE_analyst"));

      User user = new User();
      user.setId(71L);
      user.setEmployerId(25L);
      user.setOrganizationId(700L);
      lenient().when(userRepository.findById(71L)).thenReturn(Optional.of(user));

      Employer employer = new Employer();
      employer.setId(25L);
      employer.setOrganizationId(null);
      when(employerRepository.findById(25L)).thenReturn(Optional.of(employer));

      assertThatThrownBy(() -> validator.validateEmployerScope(25L))
          .isInstanceOf(BadRequestException.class)
          .extracting("errorCode").isEqualTo("ORGANIZATION_ASSIGNMENT_REQUIRED");
    }

    @Test
    @DisplayName("Provider scope check fails closed when target provider record is missing")
    void testValidateProviderScope_targetProviderNotFound_failsClosed() {
      setAuthenticatedUser(72L, "org_officer", List.of("ROLE_analyst"));

      User user = new User();
      user.setId(72L);
      user.setTrainingProviderId(35L);
      user.setOrganizationId(700L);
      lenient().when(userRepository.findById(72L)).thenReturn(Optional.of(user));
      when(trainingProviderRepository.findById(35L)).thenReturn(Optional.empty());

      assertThatThrownBy(() -> validator.validateProviderScope(35L))
          .isInstanceOf(BadRequestException.class)
          .extracting("errorCode").isEqualTo("PROVIDER_NOT_FOUND");
    }

    @Test
    @DisplayName("Provider scope check fails closed when target provider is missing organization affiliation")
    void testValidateProviderScope_targetProviderMissingOrganizationAffiliation_failsClosed() {
      setAuthenticatedUser(72L, "org_officer", List.of("ROLE_analyst"));

      User user = new User();
      user.setId(72L);
      user.setTrainingProviderId(35L);
      user.setOrganizationId(700L);
      lenient().when(userRepository.findById(72L)).thenReturn(Optional.of(user));

      TrainingProvider provider = new TrainingProvider();
      provider.setId(35L);
      provider.setOrganizationId(null);
      when(trainingProviderRepository.findById(35L)).thenReturn(Optional.of(provider));

      assertThatThrownBy(() -> validator.validateProviderScope(35L))
          .isInstanceOf(BadRequestException.class)
          .extracting("errorCode").isEqualTo("ORGANIZATION_ASSIGNMENT_REQUIRED");
    }

    @Test
    @DisplayName("Employer scope check fails closed when target employer organization affiliation record does not exist")
    void testValidateEmployerScope_targetEmployerNonexistentOrganization_failsClosed() {
      setAuthenticatedUser(71L, "org_officer", List.of("ROLE_analyst"));

      User user = new User();
      user.setId(71L);
      user.setEmployerId(25L);
      user.setOrganizationId(777L);
      lenient().when(userRepository.findById(71L)).thenReturn(Optional.of(user));

      Employer employer = new Employer();
      employer.setId(25L);
      employer.setOrganizationId(777L);
      when(employerRepository.findById(25L)).thenReturn(Optional.of(employer));
      when(organizationRepository.existsById(777L)).thenReturn(false);

      assertThatThrownBy(() -> validator.validateEmployerScope(25L))
          .isInstanceOf(BadRequestException.class)
          .extracting("errorCode").isEqualTo("ORGANIZATION_NOT_FOUND");
    }

    @Test
    @DisplayName("Provider scope check fails closed when target provider organization affiliation record does not exist")
    void testValidateProviderScope_targetProviderNonexistentOrganization_failsClosed() {
      setAuthenticatedUser(72L, "org_officer", List.of("ROLE_analyst"));

      User user = new User();
      user.setId(72L);
      user.setTrainingProviderId(35L);
      user.setOrganizationId(777L);
      lenient().when(userRepository.findById(72L)).thenReturn(Optional.of(user));

      TrainingProvider provider = new TrainingProvider();
      provider.setId(35L);
      provider.setOrganizationId(777L);
      when(trainingProviderRepository.findById(35L)).thenReturn(Optional.of(provider));
      when(organizationRepository.existsById(777L)).thenReturn(false);

      assertThatThrownBy(() -> validator.validateProviderScope(35L))
          .isInstanceOf(BadRequestException.class)
          .extracting("errorCode").isEqualTo("ORGANIZATION_NOT_FOUND");
    }

    @Test
    @DisplayName("Constructor rejects null employerRepository, trainingProviderRepository, or organizationRepository")
    void testConstructor_rejectsNullRepositories() {
      assertThatThrownBy(() -> new WorkflowSecurityScopeValidator(userRepository, traineeRepository, null, trainingProviderRepository, organizationRepository))
          .isInstanceOf(NullPointerException.class)
          .hasMessageContaining("EmployerRepository is required");

      assertThatThrownBy(() -> new WorkflowSecurityScopeValidator(userRepository, traineeRepository, employerRepository, null, organizationRepository))
          .isInstanceOf(NullPointerException.class)
          .hasMessageContaining("TrainingProviderRepository is required");

      assertThatThrownBy(() -> new WorkflowSecurityScopeValidator(userRepository, traineeRepository, employerRepository, trainingProviderRepository, null))
          .isInstanceOf(NullPointerException.class)
          .hasMessageContaining("OrganizationRepository is required");
    }
  }

  // =========================================================================
  // 5. Fail-Closed Organization & Admin-Role Variant Enforcement
  // =========================================================================
  @Nested
  @DisplayName("5. Fail-Closed Organization & Admin-Role Variant Enforcement")
  class FailClosedOrganizationAndAdminRoleTests {

    @Test
    @DisplayName("Employer scope check fails closed when caller organization cannot be resolved")
    void testValidateEmployerScope_callerOrganizationUnresolved_failsClosed() {
      setAuthenticatedUser(10L, "emp_user", List.of("ROLE_employer"));

      User user = new User();
      user.setId(10L);
      user.setEmployerId(100L);
      user.setOrganizationId(null);
      lenient().when(userRepository.findById(10L)).thenReturn(Optional.of(user));

      Employer employer = new Employer();
      employer.setId(100L);
      employer.setOrganizationId(null);
      when(employerRepository.findById(100L)).thenReturn(Optional.of(employer));

      assertThatThrownBy(() -> validator.validateEmployerScope(100L))
          .isInstanceOf(BadRequestException.class)
          .extracting("errorCode").isEqualTo("ORGANIZATION_ASSIGNMENT_REQUIRED");
    }

    @Test
    @DisplayName("Provider scope check fails closed when caller organization cannot be resolved")
    void testValidateProviderScope_callerOrganizationUnresolved_failsClosed() {
      setAuthenticatedUser(30L, "tp_user", List.of("ROLE_training_provider"));

      User user = new User();
      user.setId(30L);
      user.setTrainingProviderId(300L);
      user.setOrganizationId(null);
      lenient().when(userRepository.findById(30L)).thenReturn(Optional.of(user));

      TrainingProvider provider = new TrainingProvider();
      provider.setId(300L);
      provider.setOrganizationId(null);
      when(trainingProviderRepository.findById(300L)).thenReturn(Optional.of(provider));

      assertThatThrownBy(() -> validator.validateProviderScope(300L))
          .isInstanceOf(BadRequestException.class)
          .extracting("errorCode").isEqualTo("ORGANIZATION_ASSIGNMENT_REQUIRED");
    }

    @Test
    @DisplayName("Unauthorized admin-role variant ROLE_government_officer does NOT bypass scope validation")
    void testUnauthorizedVariant_govOfficer_doesNotBypass() {
      setAuthenticatedUser(2L, "officer", List.of("ROLE_government_officer"));

      User user = new User();
      user.setId(2L);
      user.setEmployerId(null);
      when(userRepository.findById(2L)).thenReturn(Optional.of(user));

      assertThatThrownBy(() -> validator.validateEmployerScope(100L))
          .isInstanceOf(BadRequestException.class)
          .extracting("errorCode").isEqualTo("EMPLOYER_ASSIGNMENT_REQUIRED");
    }

    @Test
    @DisplayName("Unauthorized admin-role variant ROLE_ADMIN does NOT bypass scope validation")
    void testUnauthorizedVariant_roleAdmin_doesNotBypass() {
      setAuthenticatedUser(3L, "admin_user", List.of("ROLE_ADMIN"));

      User user = new User();
      user.setId(3L);
      user.setEmployerId(null);
      when(userRepository.findById(3L)).thenReturn(Optional.of(user));

      assertThatThrownBy(() -> validator.validateEmployerScope(100L))
          .isInstanceOf(BadRequestException.class)
          .extracting("errorCode").isEqualTo("EMPLOYER_ASSIGNMENT_REQUIRED");
    }

    @Test
    @DisplayName("Unauthorized admin shortcut 'admin' without prefix does NOT bypass scope validation")
    void testUnauthorizedVariant_plainAdmin_doesNotBypass() {
      setAuthenticatedUser(4L, "admin_user", List.of("admin"));

      User user = new User();
      user.setId(4L);
      user.setEmployerId(null);
      when(userRepository.findById(4L)).thenReturn(Optional.of(user));

      assertThatThrownBy(() -> validator.validateEmployerScope(100L))
          .isInstanceOf(BadRequestException.class)
          .extracting("errorCode").isEqualTo("EMPLOYER_ASSIGNMENT_REQUIRED");
    }

    @Test
    @DisplayName("Unauthorized admin shortcut 'super_admin' without ROLE_ prefix does NOT bypass scope validation")
    void testUnauthorizedVariant_superAdminWithoutPrefix_doesNotBypass() {
      setAuthenticatedUser(5L, "super_user", List.of("super_admin"));

      User user = new User();
      user.setId(5L);
      user.setEmployerId(null);
      when(userRepository.findById(5L)).thenReturn(Optional.of(user));

      assertThatThrownBy(() -> validator.validateEmployerScope(100L))
          .isInstanceOf(BadRequestException.class)
          .extracting("errorCode").isEqualTo("EMPLOYER_ASSIGNMENT_REQUIRED");
    }

    @Test
    @DisplayName("Permission-based shortcuts assessment.manage and enrollment.manage do NOT bypass scope validation")
    void testUnauthorizedVariant_permissionShortcuts_doNotBypass() {
      setAuthenticatedUser(6L, "mgr_user", List.of("assessment.manage", "enrollment.manage"));

      User user = new User();
      user.setId(6L);
      user.setTrainingProviderId(null);
      when(userRepository.findById(6L)).thenReturn(Optional.of(user));

      assertThatThrownBy(() -> validator.validateProviderScope(300L))
          .isInstanceOf(BadRequestException.class)
          .extracting("errorCode").isEqualTo("PROVIDER_ASSIGNMENT_REQUIRED");
    }
  }

  // =========================================================================
  // 6. Administrative Target & Organization Affiliation Enforcement
  // =========================================================================
  @Nested
  @DisplayName("6. Administrative Target & Organization Affiliation Enforcement")
  class AdminTargetAndAffiliationEnforcementTests {

    @Test
    @DisplayName("Admin role CANNOT bypass null target employer ID")
    void adminCannotBypassNullTargetEmployerId() {
      setAuthenticatedUser(1L, "super_admin", List.of("ROLE_super_admin"));

      assertThatThrownBy(() -> validator.validateEmployerScope(null))
          .isInstanceOf(BadRequestException.class)
          .extracting("errorCode").isEqualTo("EMPLOYER_ID_REQUIRED");
    }

    @Test
    @DisplayName("Admin role CANNOT bypass missing target employer record")
    void adminCannotBypassMissingTargetEmployer() {
      setAuthenticatedUser(1L, "super_admin", List.of("ROLE_super_admin"));
      when(employerRepository.findById(9999L)).thenReturn(Optional.empty());

      assertThatThrownBy(() -> validator.validateEmployerScope(9999L))
          .isInstanceOf(BadRequestException.class)
          .extracting("errorCode").isEqualTo("EMPLOYER_NOT_FOUND");
    }

    @Test
    @DisplayName("Admin role CANNOT bypass missing target employer organization affiliation")
    void adminCannotBypassMissingEmployerOrganizationAffiliation() {
      setAuthenticatedUser(1L, "super_admin", List.of("ROLE_super_admin"));

      Employer employerWithoutOrg = new Employer();
      employerWithoutOrg.setId(9998L);
      employerWithoutOrg.setOrganizationId(null);
      when(employerRepository.findById(9998L)).thenReturn(Optional.of(employerWithoutOrg));

      assertThatThrownBy(() -> validator.validateEmployerScope(9998L))
          .isInstanceOf(BadRequestException.class)
          .extracting("errorCode").isEqualTo("ORGANIZATION_ASSIGNMENT_REQUIRED");
    }

    @Test
    @DisplayName("Admin role CANNOT bypass null target training provider ID")
    void adminCannotBypassNullTargetProviderId() {
      setAuthenticatedUser(1L, "super_admin", List.of("ROLE_super_admin"));

      assertThatThrownBy(() -> validator.validateProviderScope(null))
          .isInstanceOf(BadRequestException.class)
          .extracting("errorCode").isEqualTo("PROVIDER_ID_REQUIRED");
    }

    @Test
    @DisplayName("Admin role CANNOT bypass missing target training provider record")
    void adminCannotBypassMissingTargetProvider() {
      setAuthenticatedUser(1L, "super_admin", List.of("ROLE_super_admin"));
      when(trainingProviderRepository.findById(9999L)).thenReturn(Optional.empty());

      assertThatThrownBy(() -> validator.validateProviderScope(9999L))
          .isInstanceOf(BadRequestException.class)
          .extracting("errorCode").isEqualTo("PROVIDER_NOT_FOUND");
    }

    @Test
    @DisplayName("Admin role CANNOT bypass missing target provider organization affiliation")
    void adminCannotBypassMissingProviderOrganizationAffiliation() {
      setAuthenticatedUser(1L, "super_admin", List.of("ROLE_super_admin"));

      TrainingProvider providerWithoutOrg = new TrainingProvider();
      providerWithoutOrg.setId(9998L);
      providerWithoutOrg.setOrganizationId(null);
      when(trainingProviderRepository.findById(9998L)).thenReturn(Optional.of(providerWithoutOrg));

      assertThatThrownBy(() -> validator.validateProviderScope(9998L))
          .isInstanceOf(BadRequestException.class)
          .extracting("errorCode").isEqualTo("ORGANIZATION_ASSIGNMENT_REQUIRED");
    }

    @Test
    @DisplayName("Admin role CANNOT bypass null target trainee ID")
    void adminCannotBypassNullTargetTraineeId() {
      setAuthenticatedUser(1L, "super_admin", List.of("ROLE_super_admin"));

      assertThatThrownBy(() -> validator.validateTraineeScope(null))
          .isInstanceOf(BadRequestException.class)
          .extracting("errorCode").isEqualTo("TRAINEE_ID_REQUIRED");
    }

    @Test
    @DisplayName("Admin role CANNOT bypass missing target trainee record")
    void adminCannotBypassMissingTargetTrainee() {
      setAuthenticatedUser(1L, "super_admin", List.of("ROLE_super_admin"));
      when(traineeRepository.existsById(9999L)).thenReturn(false);

      assertThatThrownBy(() -> validator.validateTraineeScope(9999L))
          .isInstanceOf(BadRequestException.class)
          .extracting("errorCode").isEqualTo("TRAINEE_NOT_FOUND");
    }

    @Test
    @DisplayName("Admin role CANNOT bypass null target organization ID")
    void adminCannotBypassNullTargetOrganizationId() {
      setAuthenticatedUser(1L, "super_admin", List.of("ROLE_super_admin"));

      assertThatThrownBy(() -> validator.validateOrganizationScope(null))
          .isInstanceOf(BadRequestException.class)
          .extracting("errorCode").isEqualTo("ORGANIZATION_ID_REQUIRED");
    }

    @Test
    @DisplayName("Admin role CANNOT bypass missing target organization record")
    void adminCannotBypassMissingTargetOrganization() {
      setAuthenticatedUser(1L, "super_admin", List.of("ROLE_super_admin"));
      when(organizationRepository.existsById(9999L)).thenReturn(false);

      assertThatThrownBy(() -> validator.validateOrganizationScope(9999L))
          .isInstanceOf(BadRequestException.class)
          .extracting("errorCode").isEqualTo("ORGANIZATION_NOT_FOUND");
    }

    @Test
    @DisplayName("Admin role CANNOT bypass target employer with nonexistent organization affiliation")
    void adminCannotBypassEmployerWithNonexistentOrganizationAffiliation() {
      setAuthenticatedUser(1L, "super_admin", List.of("ROLE_super_admin"));

      Employer employerWithInvalidOrg = new Employer();
      employerWithInvalidOrg.setId(9997L);
      employerWithInvalidOrg.setOrganizationId(8888L);
      when(employerRepository.findById(9997L)).thenReturn(Optional.of(employerWithInvalidOrg));
      when(organizationRepository.existsById(8888L)).thenReturn(false);

      assertThatThrownBy(() -> validator.validateEmployerScope(9997L))
          .isInstanceOf(BadRequestException.class)
          .extracting("errorCode").isEqualTo("ORGANIZATION_NOT_FOUND");
    }

    @Test
    @DisplayName("Admin role CANNOT bypass target provider with nonexistent organization affiliation")
    void adminCannotBypassProviderWithNonexistentOrganizationAffiliation() {
      setAuthenticatedUser(1L, "super_admin", List.of("ROLE_super_admin"));

      TrainingProvider providerWithInvalidOrg = new TrainingProvider();
      providerWithInvalidOrg.setId(9997L);
      providerWithInvalidOrg.setOrganizationId(8888L);
      when(trainingProviderRepository.findById(9997L)).thenReturn(Optional.of(providerWithInvalidOrg));
      when(organizationRepository.existsById(8888L)).thenReturn(false);

      assertThatThrownBy(() -> validator.validateProviderScope(9997L))
          .isInstanceOf(BadRequestException.class)
          .extracting("errorCode").isEqualTo("ORGANIZATION_NOT_FOUND");
    }
  }
}
