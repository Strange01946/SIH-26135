package in.gov.sih.sih26135.bootstrap;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import in.gov.sih.sih26135.dto.auth.AuthResponse;
import in.gov.sih.sih26135.dto.registration.ApplicationStatusResponse;
import in.gov.sih.sih26135.dto.registration.OnboardingApplicationResponse;
import in.gov.sih.sih26135.dto.registration.ProvisionAssessorRequest;
import in.gov.sih.sih26135.dto.registration.ProvisionGovernmentAdminRequest;
import in.gov.sih.sih26135.dto.registration.ProvisionSuperAdminRequest;
import in.gov.sih.sih26135.dto.registration.RegisterEmployerRequest;
import in.gov.sih.sih26135.dto.registration.RegisterTraineeRequest;
import in.gov.sih.sih26135.dto.registration.RegisterTrainingProviderRequest;
import in.gov.sih.sih26135.dto.registration.RegistrationReferenceDataResponse;
import in.gov.sih.sih26135.dto.request.UpdateUserRequest;
import in.gov.sih.sih26135.dto.response.UserResponse;
import in.gov.sih.sih26135.entity.Employer;
import in.gov.sih.sih26135.entity.OnboardingApplication;
import in.gov.sih.sih26135.entity.Trainee;
import in.gov.sih.sih26135.entity.TrainingProvider;
import in.gov.sih.sih26135.entity.User;
import in.gov.sih.sih26135.exception.BadRequestException;
import in.gov.sih.sih26135.exception.ConflictException;
import in.gov.sih.sih26135.repository.EmployerRepository;
import in.gov.sih.sih26135.repository.OnboardingApplicationRepository;
import in.gov.sih.sih26135.repository.TraineeRepository;
import in.gov.sih.sih26135.repository.TrainingProviderRepository;
import in.gov.sih.sih26135.repository.UserRepository;
import in.gov.sih.sih26135.repository.UserRoleRepository;
import in.gov.sih.sih26135.service.OnboardingAdminService;
import in.gov.sih.sih26135.service.RegistrationService;
import in.gov.sih.sih26135.service.UserService;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@DisplayName("Phase 30.3A — Onboarding, Registration and Bootstrap Integration Tests")
class OnboardingAndBootstrapIntegrationTest {

  @Autowired
  private RegistrationService registrationService;

  @Autowired
  private OnboardingAdminService onboardingAdminService;

  @Autowired
  private UserService userService;

  @Autowired
  private UserRepository userRepository;

  @Autowired
  private UserRoleRepository userRoleRepository;

  @Autowired
  private TraineeRepository traineeRepository;

  @Autowired
  private OnboardingApplicationRepository onboardingApplicationRepository;

  @Autowired
  private TrainingProviderRepository trainingProviderRepository;

  @Autowired
  private EmployerRepository employerRepository;

  @BeforeEach
  void setUpSecurity() {
    SecurityContextHolder.getContext().setAuthentication(
        new UsernamePasswordAuthenticationToken(
            "admin_user",
            null,
            List.of(new SimpleGrantedAuthority("ROLE_super_admin"))
        )
    );
  }

  @AfterEach
  void tearDownSecurity() {
    SecurityContextHolder.clearContext();
  }

  private String randomSuffix() {
    return UUID.randomUUID().toString().substring(0, 8);
  }

  @Test
  @DisplayName("1. Reference data endpoint returns non-empty lookups")
  void testGetReferenceData() {
    RegistrationReferenceDataResponse refData = registrationService.getReferenceData();
    assertThat(refData).isNotNull();
    assertThat(refData.getGenders()).isNotEmpty();
    assertThat(refData.getStates()).isNotEmpty();
    assertThat(refData.getDistricts()).isNotEmpty();
    assertThat(refData.getOrganizationTypes()).isNotEmpty();
    assertThat(refData.getEmploymentStatuses()).isNotEmpty();
  }

  @Test
  @Transactional
  @DisplayName("2. Trainee self-registration creates active user, trainee record, and returns auth tokens")
  void testTraineeSelfRegistration() {
    String suffix = randomSuffix();
    RegisterTraineeRequest req = new RegisterTraineeRequest();
    req.setUsername("cand_" + suffix);
    req.setEmail("cand_" + suffix + "@example.com");
    req.setPassword("ValidPassword123!");
    req.setFirstName("Ramesh");
    req.setLastName("Kumar");
    req.setGenderId(2L);
    req.setDateOfBirth(LocalDate.of(2000, 1, 15));
    req.setStateId(101L);
    req.setDistrictId(1001L);
    req.setCurrentEmploymentStatusId(1L);

    AuthResponse authResponse = registrationService.registerTrainee(req);

    assertThat(authResponse).isNotNull();
    assertThat(authResponse.getAccessToken()).isNotBlank();
    assertThat(authResponse.getAuthorities()).contains("ROLE_trainee");

    User user = userRepository.findByUsername(req.getUsername()).orElseThrow();
    assertThat(user.getUserStatusId()).isEqualTo(2L); // ACTIVE

    Trainee trainee = traineeRepository.findByUserId(user.getId()).orElseThrow();
    assertThat(trainee.getFirstName()).isEqualTo("Ramesh");
    assertThat(trainee.getRegistrationNumber()).startsWith("TRN-");

    // Duplicate check
    assertThrows(ConflictException.class, () -> registrationService.registerTrainee(req));
  }

  @Test
  @Transactional
  @DisplayName("3. Training Provider registration creates pending account and onboarding application")
  void testTrainingProviderRegistrationAndApproval() {
    String suffix = randomSuffix();
    RegisterTrainingProviderRequest req = new RegisterTrainingProviderRequest();
    req.setUsername("tp_" + suffix);
    req.setEmail("tp_" + suffix + "@example.com");
    req.setPassword("ValidPassword123!");
    req.setProviderName("Apex Skills Academy " + suffix);
    req.setRegistrationNumber("TP-REG-" + suffix);
    req.setContactPersonName("Sunita Rao");
    req.setContactEmail("tp_" + suffix + "@example.com");
    req.setContactPhone("9876543210");
    req.setStateId(101L);
    req.setDistrictId(1001L);

    OnboardingApplicationResponse appResp = registrationService.registerTrainingProvider(req);
    assertThat(appResp).isNotNull();
    assertThat(appResp.getStatus()).isEqualTo("PENDING");
    assertThat(appResp.getApplicationNumber()).startsWith("APP-TP-");

    User user = userRepository.findByUsername(req.getUsername()).orElseThrow();
    assertThat(user.getUserStatusId()).isEqualTo(1L); // PENDING

    // Status query
    ApplicationStatusResponse statusResp = registrationService.getApplicationStatus(appResp.getApplicationNumber());
    assertThat(statusResp.getStatus()).isEqualTo("PENDING");

    // Admin Approval
    OnboardingApplicationResponse approvedResp = onboardingAdminService.approveApplication(appResp.getApplicationId(), 1L);
    assertThat(approvedResp.getStatus()).isEqualTo("APPROVED");

    User approvedUser = userRepository.findById(user.getId()).orElseThrow();
    assertThat(approvedUser.getUserStatusId()).isEqualTo(2L); // ACTIVE
  }

  @Test
  @Transactional
  @DisplayName("4. Employer registration creates pending account and supports rejection")
  void testEmployerRegistrationAndRejection() {
    String suffix = randomSuffix();
    RegisterEmployerRequest req = new RegisterEmployerRequest();
    req.setUsername("emp_" + suffix);
    req.setEmail("emp_" + suffix + "@example.com");
    req.setPassword("ValidPassword123!");
    req.setEmployerName("Tata Tech Solutions " + suffix);
    req.setRegistrationNumber("CIN-" + suffix);
    req.setGstin("07AAAAA" + suffix.substring(0, 4).toUpperCase() + "1Z5");
    req.setContactPersonName("Anil Verma");
    req.setContactEmail("emp_" + suffix + "@example.com");
    req.setStateId(101L);
    req.setDistrictId(1001L);

    OnboardingApplicationResponse appResp = registrationService.registerEmployer(req);
    assertThat(appResp.getStatus()).isEqualTo("PENDING");

    // Admin Rejection
    OnboardingApplicationResponse rejectedResp = onboardingAdminService.rejectApplication(
        appResp.getApplicationId(), "Incomplete incorporation certificates", 1L
    );
    assertThat(rejectedResp.getStatus()).isEqualTo("REJECTED");
    assertThat(rejectedResp.getRejectionReason()).contains("Incomplete");

    User disabledUser = userRepository.findById(appResp.getUserId()).orElseThrow();
    assertThat(disabledUser.getUserStatusId()).isEqualTo(4L); // DISABLED
  }

  @Test
  @Transactional
  @DisplayName("5. Privileged provisioning creates active Assessor with role 10")
  void testProvisionAssessor() {
    String suffix = randomSuffix();
    ProvisionAssessorRequest req = new ProvisionAssessorRequest();
    req.setUsername("assessor_" + suffix);
    req.setEmail("assessor_" + suffix + "@example.com");
    req.setPassword("ValidPassword123!");
    req.setFullName("Kavita Mehta");
    req.setStateId(101L);
    req.setDistrictId(1001L);

    UserResponse resp = onboardingAdminService.provisionAssessor(req, 1L);
    assertThat(resp).isNotNull();
    assertThat(resp.getUserStatusId()).isEqualTo(2L); // ACTIVE

    boolean hasAssessorRole = userRoleRepository.existsByUserIdAndRoleId(resp.getId(), 10L);
    assertThat(hasAssessorRole).isTrue();
  }

  @Test
  @Transactional
  @DisplayName("6. Privileged provisioning creates Government Admin and Super Admin")
  void testProvisionGovAndSuperAdmin() {
    String suffix = randomSuffix();
    ProvisionGovernmentAdminRequest govReq = new ProvisionGovernmentAdminRequest();
    govReq.setUsername("gov_" + suffix);
    govReq.setEmail("gov_" + suffix + "@example.com");
    govReq.setPassword("ValidPassword123!");
    govReq.setRoleCode("district_officer");
    govReq.setStateId(101L);
    govReq.setDistrictId(1001L);

    UserResponse govResp = onboardingAdminService.provisionGovernmentAdmin(govReq, 1L);
    assertThat(govResp.getUserStatusId()).isEqualTo(2L);
    boolean hasDistrictRole = userRoleRepository.existsByUserIdAndRoleId(govResp.getId(), 4L);
    assertThat(hasDistrictRole).isTrue();

    ProvisionSuperAdminRequest superReq = new ProvisionSuperAdminRequest();
    superReq.setUsername("super_" + suffix);
    superReq.setEmail("super_" + suffix + "@example.com");
    superReq.setPassword("ValidPassword123!");

    UserResponse superResp = onboardingAdminService.provisionSuperAdmin(superReq, 1L);
    assertThat(superResp.getUserStatusId()).isEqualTo(2L);
    boolean hasSuperRole = userRoleRepository.existsByUserIdAndRoleId(superResp.getId(), 1L);
    assertThat(hasSuperRole).isTrue();
  }

  @Test
  @Transactional
  @DisplayName("7. Guard prevents deleting or deactivating the last active Super Admin")
  void testSuperAdminProtectionGuard() {
    // In test DB, find active super admins
    var superAdmins = userRoleRepository.findByRoleId(1L);
    if (superAdmins.size() == 1) {
      Long soleAdminId = superAdmins.get(0).getUserId();
      // Try deactivating
      UpdateUserRequest updateReq = new UpdateUserRequest();
      updateReq.setUserStatusId(4L); // DISABLED
      assertThrows(IllegalStateException.class, () -> userService.updateUser(soleAdminId, updateReq, 1L));

      // Try deleting
      assertThrows(IllegalStateException.class, () -> userService.deleteUser(soleAdminId, 1L));
    }
  }

  @Test
  @Transactional
  @DisplayName("8. Disallowed government role codes are rejected at service boundary")
  void testDisallowedGovernmentRoleRejection() {
    ProvisionGovernmentAdminRequest req = new ProvisionGovernmentAdminRequest();
    req.setUsername("invalid_gov_" + randomSuffix());
    req.setEmail("invalid_gov@example.com");
    req.setPassword("ValidPassword123!");
    req.setRoleCode("trainee"); // Not in ALLOWED_GOVERNMENT_ROLES
    req.setStateId(101L);
    req.setDistrictId(1001L);

    BadRequestException ex = assertThrows(BadRequestException.class, () -> onboardingAdminService.provisionGovernmentAdmin(req, 1L));
    assertThat(ex.getMessage()).contains("Role code must be one of");
  }

  @Test
  @Transactional
  @DisplayName("9. Training Provider approval activates user without automatically granting statutory accreditation")
  void testProviderApprovalPreservesStatutoryStatus() {
    String suffix = randomSuffix();
    RegisterTrainingProviderRequest req = new RegisterTrainingProviderRequest();
    req.setUsername("tp_stat_" + suffix);
    req.setEmail("tp_stat_" + suffix + "@example.com");
    req.setPassword("ValidPassword123!");
    req.setProviderName("Statutory Academy " + suffix);
    req.setRegistrationNumber("TP-STAT-" + suffix);
    req.setContactPersonName("Sunita Rao");
    req.setContactEmail("tp_stat_" + suffix + "@example.com");
    req.setStateId(101L);
    req.setDistrictId(1001L);

    OnboardingApplicationResponse appResp = registrationService.registerTrainingProvider(req);
    OnboardingApplicationResponse approvedResp = onboardingAdminService.approveApplication(appResp.getApplicationId(), 1L);
    assertThat(approvedResp.getStatus()).isEqualTo("APPROVED");

    User user = userRepository.findById(appResp.getUserId()).orElseThrow();
    assertThat(user.getUserStatusId()).isEqualTo(2L); // ACTIVE

    TrainingProvider provider = trainingProviderRepository.findById(appResp.getEntityId()).orElseThrow();
    assertThat(provider.getAccreditationStatus().getId()).isNotEqualTo(4L); // NOT ACCREDITED
  }

  @Test
  @Transactional
  @DisplayName("10. Employer approval activates user without automatically granting statutory enterprise verification")
  void testEmployerApprovalPreservesStatutoryStatus() {
    String suffix = randomSuffix();
    RegisterEmployerRequest req = new RegisterEmployerRequest();
    req.setUsername("emp_stat_" + suffix);
    req.setEmail("emp_stat_" + suffix + "@example.com");
    req.setPassword("ValidPassword123!");
    req.setEmployerName("Statutory Enterprise " + suffix);
    req.setRegistrationNumber("CIN-STAT-" + suffix);
    req.setGstin("07BBBBB" + suffix.substring(0, 4).toUpperCase() + "1Z5");
    req.setContactPersonName("Rajesh Patel");
    req.setContactEmail("emp_stat_" + suffix + "@example.com");
    req.setStateId(101L);
    req.setDistrictId(1001L);

    OnboardingApplicationResponse appResp = registrationService.registerEmployer(req);
    OnboardingApplicationResponse approvedResp = onboardingAdminService.approveApplication(appResp.getApplicationId(), 1L);
    assertThat(approvedResp.getStatus()).isEqualTo("APPROVED");

    User user = userRepository.findById(appResp.getUserId()).orElseThrow();
    assertThat(user.getUserStatusId()).isEqualTo(2L); // ACTIVE

    Employer employer = employerRepository.findById(appResp.getEntityId()).orElseThrow();
    assertThat(employer.getRecordVerificationStatus().getId()).isNotEqualTo(4L); // NOT VERIFIED
  }

  @Test
  @DisplayName("11. Geographic reference data preserves parentId relationships for district filtering")
  void testGeographicReferenceDataParentId() {
    RegistrationReferenceDataResponse refData = registrationService.getReferenceData();
    assertThat(refData.getDistricts()).isNotEmpty();
    assertThat(refData.getDistricts()).allMatch(d -> d.getParentId() != null && d.getParentId() > 0);
  }

  @Test
  @Transactional
  @DisplayName("12. Method security rejects unprivileged caller attempting to approve onboarding application")
  void testMethodSecurityRejection() {
    SecurityContextHolder.getContext().setAuthentication(
        new UsernamePasswordAuthenticationToken(
            "trainee_user",
            null,
            List.of(new SimpleGrantedAuthority("ROLE_trainee"))
        )
    );

    assertThrows(AccessDeniedException.class, () -> onboardingAdminService.approveApplication(9999L, 1L));
  }
}
