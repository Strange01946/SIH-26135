package in.gov.sih.sih26135.service.impl;

import in.gov.sih.sih26135.dto.registration.OnboardingApplicationResponse;
import in.gov.sih.sih26135.dto.registration.ProvisionAssessorRequest;
import in.gov.sih.sih26135.dto.registration.ProvisionGovernmentAdminRequest;
import in.gov.sih.sih26135.dto.registration.ProvisionSuperAdminRequest;
import in.gov.sih.sih26135.dto.response.UserResponse;
import in.gov.sih.sih26135.entity.Employer;
import in.gov.sih.sih26135.entity.OnboardingApplication;
import in.gov.sih.sih26135.entity.Organization;
import in.gov.sih.sih26135.entity.RefAccreditationStatus;
import in.gov.sih.sih26135.entity.RefRecordVerificationStatus;
import in.gov.sih.sih26135.entity.Role;
import in.gov.sih.sih26135.entity.TrainingProvider;
import in.gov.sih.sih26135.entity.User;
import in.gov.sih.sih26135.entity.UserRole;
import in.gov.sih.sih26135.exception.BadRequestException;
import in.gov.sih.sih26135.exception.ConflictException;
import in.gov.sih.sih26135.exception.ResourceNotFoundException;
import in.gov.sih.sih26135.mapper.UserMapper;
import in.gov.sih.sih26135.repository.EmployerRepository;
import in.gov.sih.sih26135.repository.OnboardingApplicationRepository;
import in.gov.sih.sih26135.repository.OrganizationRepository;
import in.gov.sih.sih26135.repository.RefAccreditationStatusRepository;
import in.gov.sih.sih26135.repository.RefRecordVerificationStatusRepository;
import in.gov.sih.sih26135.repository.RoleRepository;
import in.gov.sih.sih26135.repository.TrainingProviderRepository;
import in.gov.sih.sih26135.repository.UserRepository;
import in.gov.sih.sih26135.repository.UserRoleRepository;
import in.gov.sih.sih26135.service.ActiveGeography;
import in.gov.sih.sih26135.service.OnboardingAdminService;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class OnboardingAdminServiceImpl implements OnboardingAdminService {

  private static final Logger log = LoggerFactory.getLogger(OnboardingAdminServiceImpl.class);

  private static final Long ROLE_ID_SUPER_ADMIN = 1L;
  private static final Long ROLE_ID_TRAINING_PROVIDER = 5L;
  private static final Long ROLE_ID_EMPLOYER = 6L;
  private static final Long ROLE_ID_ASSESSOR = 10L;
  private static final Long STATUS_ACTIVE_ID = 2L;
  private static final Long STATUS_DISABLED_ID = 4L;

  private static final Set<String> ALLOWED_GOVERNMENT_ROLES = Set.of(
      "government_admin",
      "state_admin",
      "government_officer",
      "district_officer",
      "analyst"
  );

  private final OnboardingApplicationRepository onboardingApplicationRepository;
  private final UserRepository userRepository;
  private final UserRoleRepository userRoleRepository;
  private final RoleRepository roleRepository;
  private final TrainingProviderRepository trainingProviderRepository;
  private final EmployerRepository employerRepository;
  private final RefAccreditationStatusRepository refAccreditationStatusRepository;
  private final RefRecordVerificationStatusRepository refRecordVerificationStatusRepository;
  private final UserMapper userMapper;
  private final PasswordEncoder passwordEncoder;
  private final JdbcTemplate jdbcTemplate;
  private final OrganizationRepository organizationRepository;

  public OnboardingAdminServiceImpl(
      OnboardingApplicationRepository onboardingApplicationRepository,
      UserRepository userRepository,
      UserRoleRepository userRoleRepository,
      RoleRepository roleRepository,
      TrainingProviderRepository trainingProviderRepository,
      EmployerRepository employerRepository,
      RefAccreditationStatusRepository refAccreditationStatusRepository,
      RefRecordVerificationStatusRepository refRecordVerificationStatusRepository,
      UserMapper userMapper,
      PasswordEncoder passwordEncoder,
      JdbcTemplate jdbcTemplate,
      OrganizationRepository organizationRepository) {
    this.onboardingApplicationRepository = onboardingApplicationRepository;
    this.userRepository = userRepository;
    this.userRoleRepository = userRoleRepository;
    this.roleRepository = roleRepository;
    this.trainingProviderRepository = trainingProviderRepository;
    this.employerRepository = employerRepository;
    this.refAccreditationStatusRepository = refAccreditationStatusRepository;
    this.refRecordVerificationStatusRepository = refRecordVerificationStatusRepository;
    this.userMapper = userMapper;
    this.passwordEncoder = passwordEncoder;
    this.jdbcTemplate = jdbcTemplate;
    this.organizationRepository = organizationRepository;
  }

  @Override
  public List<OnboardingApplicationResponse> getApplications(String status) {
    List<OnboardingApplication> applications;
    if (status != null && !status.isBlank()) {
      applications = onboardingApplicationRepository.findByStatusOrderByCreatedAtDesc(status.trim().toUpperCase());
    } else {
      applications = onboardingApplicationRepository.findAllByOrderByCreatedAtDesc();
    }
    return applications.stream().map(this::mapToResponse).toList();
  }

  @Override
  public OnboardingApplicationResponse getApplicationById(Long id) {
    OnboardingApplication app = onboardingApplicationRepository.findById(id)
        .orElseThrow(() -> new ResourceNotFoundException("OnboardingApplication", "id"));
    return mapToResponse(app);
  }

  @Override
  @Transactional
  public OnboardingApplicationResponse approveApplication(Long id, Long reviewerUserId) {
    // Acquire pessimistic row lock to prevent concurrent approval/rejection races
    OnboardingApplication app = onboardingApplicationRepository.findByIdForUpdate(id)
        .orElseThrow(() -> new ResourceNotFoundException("OnboardingApplication", "id"));

    if (!"PENDING".equalsIgnoreCase(app.getStatus())) {
      throw new BadRequestException("Only PENDING applications can be approved", "APPLICATION_ALREADY_PROCESSED");
    }

    // Explicitly validate associated user exists and is not soft-deleted
    User user = userRepository.findById(app.getUserId())
        .orElseThrow(() -> new ResourceNotFoundException("User", "userId=" + app.getUserId()));

    if (user.getDeletedAt() != null) {
      throw new IllegalStateException("Associated user account is deleted and cannot be approved");
    }

    // Validate the associated entity and bind its organisation before activating the account.
    LocalDateTime now = LocalDateTime.now();
    Long organizationId;
    if ("TRAINING_PROVIDER".equalsIgnoreCase(app.getApplicantType())) {
      TrainingProvider provider = trainingProviderRepository.findById(app.getEntityId())
          .orElseThrow(() -> new ResourceNotFoundException("TrainingProvider", "providerId=" + app.getEntityId()));
      if (!userRoleRepository.existsByUserIdAndRoleId(user.getId(), ROLE_ID_TRAINING_PROVIDER)) {
        throw new IllegalStateException("Associated user does not hold required training_provider role");
      }
      organizationId = provider.getOrganizationId();
      if (organizationId == null) {
        organizationId = createOrganization(app, provider.getOrganizationTypeId(), provider.getStateId(), provider.getDistrictId(), now);
        provider.setOrganizationId(organizationId);
      }
    } else if ("EMPLOYER".equalsIgnoreCase(app.getApplicantType())) {
      Employer employer = employerRepository.findById(app.getEntityId())
          .orElseThrow(() -> new ResourceNotFoundException("Employer", "employerId=" + app.getEntityId()));
      if (!userRoleRepository.existsByUserIdAndRoleId(user.getId(), ROLE_ID_EMPLOYER)) {
        throw new IllegalStateException("Associated user does not hold required employer role");
      }
      organizationId = employer.getOrganizationId();
      if (organizationId == null) {
        organizationId = createOrganization(app, employer.getOrganizationTypeId(), employer.getStateId(), employer.getDistrictId(), now);
        employer.setOrganizationId(organizationId);
      }
    } else {
      throw new BadRequestException("Unknown applicant type: " + app.getApplicantType(), "INVALID_APPLICANT_TYPE");
    }
    if (user.getOrganizationId() != null && !user.getOrganizationId().equals(organizationId)) {
      throw new ConflictException("The account and application have different organisations", "ONBOARDING_ORGANIZATION_MISMATCH");
    }
    user.setOrganizationId(organizationId);
    app.setStatus("APPROVED");
    app.setReviewedByUserId(reviewerUserId);
    app.setReviewedAt(now);
    app.setUpdatedAt(now);
    OnboardingApplication savedApp = onboardingApplicationRepository.save(app);

    // Explicitly activate the associated user account
    user.setUserStatusId(STATUS_ACTIVE_ID);
    user.setUpdatedAt(now);
    userRepository.save(user);

    // Note: Portal onboarding approval activates the user account, but does NOT unilaterally
    // confer statutory institutional accreditation or employer verification, preserving independent lifecycle checks.
    log.info("Application approved: appId={}, type={}, entityId={}, reviewerId={}",
        id, app.getApplicantType(), app.getEntityId(), reviewerUserId);

    OnboardingApplicationResponse response = mapToResponse(savedApp);
    response.setMessage("Application approved successfully. User account has been activated.");
    return response;
  }

  private Long createOrganization(OnboardingApplication app, Long type, Long state, Long district, LocalDateTime now) {
    Organization organization = new Organization(null, "ONB-" + app.getId(), app.getOrganizationName(), type, 1L);
    organization.setStateId(state);
    organization.setDistrictId(district);
    organization.setCreatedAt(now);
    organization.setUpdatedAt(now);
    return organizationRepository.save(organization).getId();
  }

  @Override
  @Transactional
  public OnboardingApplicationResponse rejectApplication(Long id, String reason, Long reviewerUserId) {
    // Acquire pessimistic row lock to prevent concurrent approval/rejection races
    OnboardingApplication app = onboardingApplicationRepository.findByIdForUpdate(id)
        .orElseThrow(() -> new ResourceNotFoundException("OnboardingApplication", "id"));

    if (!"PENDING".equalsIgnoreCase(app.getStatus())) {
      throw new BadRequestException("Only PENDING applications can be rejected", "APPLICATION_ALREADY_PROCESSED");
    }

    // Explicitly validate associated user exists
    User user = userRepository.findById(app.getUserId())
        .orElseThrow(() -> new ResourceNotFoundException("User", "userId=" + app.getUserId()));

    // Explicitly validate associated entity exists
    if ("TRAINING_PROVIDER".equalsIgnoreCase(app.getApplicantType())) {
      if (!trainingProviderRepository.existsById(app.getEntityId())) {
        throw new ResourceNotFoundException("TrainingProvider", "providerId=" + app.getEntityId());
      }
    } else if ("EMPLOYER".equalsIgnoreCase(app.getApplicantType())) {
      if (!employerRepository.existsById(app.getEntityId())) {
        throw new ResourceNotFoundException("Employer", "employerId=" + app.getEntityId());
      }
    }

    LocalDateTime now = LocalDateTime.now();
    app.setStatus("REJECTED");
    app.setRejectionReason(reason != null && !reason.isBlank() ? reason.trim() : "Application rejected by administrator");
    app.setReviewedByUserId(reviewerUserId);
    app.setReviewedAt(now);
    app.setUpdatedAt(now);
    OnboardingApplication savedApp = onboardingApplicationRepository.save(app);

    // Explicitly disable the associated user account
    user.setUserStatusId(STATUS_DISABLED_ID);
    user.setUpdatedAt(now);
    userRepository.save(user);

    log.info("Application rejected: appId={}, reviewerId={}, reason={}", id, reviewerUserId, app.getRejectionReason());

    OnboardingApplicationResponse response = mapToResponse(savedApp);
    response.setMessage("Application has been rejected.");
    return response;
  }

  @Override
  @Transactional
  public UserResponse provisionAssessor(ProvisionAssessorRequest request, Long actorUserId) {
    if (request == null) {
      throw new BadRequestException("Provisioning request cannot be null");
    }
    ActiveGeography.validate(jdbcTemplate, request.getStateId(), request.getDistrictId());

    String username = request.getUsername().trim();
    String email = request.getEmail().trim().toLowerCase();

    if (userRepository.existsByUsername(username)) {
      throw new ConflictException("Username is already taken", "USERNAME_ALREADY_EXISTS");
    }
    if (userRepository.existsByEmail(email)) {
      throw new ConflictException("Email is already registered", "EMAIL_ALREADY_EXISTS");
    }

    LocalDateTime now = LocalDateTime.now();

    User user = new User();
    user.setUsername(username);
    user.setEmail(email);
    user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
    user.setPasswordAlgo("argon2id");
    user.setUserStatusId(STATUS_ACTIVE_ID);
    user.setOrganizationId(request.getOrganizationId());
    user.setStateId(request.getStateId());
    user.setDistrictId(request.getDistrictId());
    user.setFailedLoginCount(0);
    user.setMustChangePassword(false);
    user.setCreatedByUserId(actorUserId);
    user.setCreatedAt(now);
    user.setUpdatedAt(now);
    User savedUser = userRepository.save(user);

    // Assign Role 10 (assessor)
    UserRole userRole = new UserRole(savedUser.getId(), ROLE_ID_ASSESSOR, actorUserId);
    userRole.setAssignedAt(now);
    userRoleRepository.save(userRole);

    log.info("Assessor provisioned: username={}, userId={}, by={}", username, savedUser.getId(), actorUserId);
    return userMapper.toResponse(savedUser, "ACTIVE");
  }

  @Override
  @Transactional
  public UserResponse provisionGovernmentAdmin(ProvisionGovernmentAdminRequest request, Long actorUserId) {
    if (request == null) {
      throw new BadRequestException("Provisioning request cannot be null");
    }
    ActiveGeography.validate(jdbcTemplate, request.getStateId(), request.getDistrictId());

    String username = request.getUsername().trim();
    String email = request.getEmail().trim().toLowerCase();

    if (userRepository.existsByUsername(username)) {
      throw new ConflictException("Username is already taken", "USERNAME_ALREADY_EXISTS");
    }
    if (userRepository.existsByEmail(email)) {
      throw new ConflictException("Email is already registered", "EMAIL_ALREADY_EXISTS");
    }

    String rawRoleCode = request.getRoleCode() != null ? request.getRoleCode().trim().toLowerCase() : "";
    if (!ALLOWED_GOVERNMENT_ROLES.contains(rawRoleCode)) {
      throw new BadRequestException("Role code must be one of: " + ALLOWED_GOVERNMENT_ROLES, "INVALID_GOVERNMENT_ROLE");
    }
    if ("state_admin".equals(rawRoleCode) && (request.getStateId() == null || request.getDistrictId() != null)) {
      throw new BadRequestException("Assign a state or union territory to a State Administrator.", "STATE_SCOPE_REQUIRED");
    }

    Role targetRole = roleRepository.findByRoleCode(rawRoleCode)
        .orElseThrow(() -> new BadRequestException("Invalid role code: " + rawRoleCode, "INVALID_ROLE"));

    LocalDateTime now = LocalDateTime.now();

    User user = new User();
    user.setUsername(username);
    user.setEmail(email);
    user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
    user.setPasswordAlgo("argon2id");
    user.setUserStatusId(STATUS_ACTIVE_ID);
    user.setOrganizationId(request.getOrganizationId());
    user.setStateId(request.getStateId());
    user.setDistrictId(request.getDistrictId());
    user.setFailedLoginCount(0);
    user.setMustChangePassword(false);
    user.setCreatedByUserId(actorUserId);
    user.setCreatedAt(now);
    user.setUpdatedAt(now);
    User savedUser = userRepository.save(user);

    UserRole userRole = new UserRole(savedUser.getId(), targetRole.getId(), actorUserId);
    userRole.setAssignedAt(now);
    userRoleRepository.save(userRole);

    log.info("Government user provisioned: username={}, role={}, by={}", username, targetRole.getRoleCode(), actorUserId);
    return userMapper.toResponse(savedUser, "ACTIVE");
  }

  @Override
  @Transactional
  public UserResponse provisionSuperAdmin(ProvisionSuperAdminRequest request, Long actorUserId) {
    if (request == null) {
      throw new BadRequestException("Provisioning request cannot be null");
    }

    String username = request.getUsername().trim();
    String email = request.getEmail().trim().toLowerCase();

    if (userRepository.existsByUsername(username)) {
      throw new ConflictException("Username is already taken", "USERNAME_ALREADY_EXISTS");
    }
    if (userRepository.existsByEmail(email)) {
      throw new ConflictException("Email is already registered", "EMAIL_ALREADY_EXISTS");
    }

    LocalDateTime now = LocalDateTime.now();

    User user = new User();
    user.setUsername(username);
    user.setEmail(email);
    user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
    user.setPasswordAlgo("argon2id");
    user.setUserStatusId(STATUS_ACTIVE_ID);
    user.setFailedLoginCount(0);
    user.setMustChangePassword(false);
    user.setCreatedByUserId(actorUserId);
    user.setCreatedAt(now);
    user.setUpdatedAt(now);
    User savedUser = userRepository.save(user);

    UserRole userRole = new UserRole(savedUser.getId(), ROLE_ID_SUPER_ADMIN, actorUserId);
    userRole.setAssignedAt(now);
    userRoleRepository.save(userRole);

    log.info("Super Admin provisioned: username={}, userId={}, by={}", username, savedUser.getId(), actorUserId);
    return userMapper.toResponse(savedUser, "ACTIVE");
  }

  private OnboardingApplicationResponse mapToResponse(OnboardingApplication app) {
    OnboardingApplicationResponse resp = new OnboardingApplicationResponse();
    resp.setApplicationId(app.getId());
    resp.setApplicationNumber(app.getApplicationNumber());
    resp.setApplicantType(app.getApplicantType());
    resp.setUserId(app.getUserId());
    userRepository.findById(app.getUserId()).ifPresent(u -> resp.setUsername(u.getUsername()));
    resp.setEntityType(app.getEntityType());
    resp.setEntityId(app.getEntityId());
    resp.setOrganizationName(app.getOrganizationName());
    resp.setContactPerson(app.getContactPerson());
    resp.setContactEmail(app.getContactEmail());
    resp.setContactPhone(app.getContactPhone());
    resp.setStatus(app.getStatus());
    resp.setRejectionReason(app.getRejectionReason());
    resp.setReviewedByUserId(app.getReviewedByUserId());
    resp.setReviewedAt(app.getReviewedAt());
    resp.setCreatedAt(app.getCreatedAt());
    return resp;
  }
}
