package in.gov.sih.sih26135.service.impl;

import in.gov.sih.sih26135.dto.auth.AuthResponse;
import in.gov.sih.sih26135.dto.auth.LoginRequest;
import in.gov.sih.sih26135.dto.registration.ApplicationStatusResponse;
import in.gov.sih.sih26135.dto.registration.OnboardingApplicationResponse;
import in.gov.sih.sih26135.dto.registration.RegisterEmployerRequest;
import in.gov.sih.sih26135.dto.registration.RegisterTraineeRequest;
import in.gov.sih.sih26135.dto.registration.RegisterTrainingProviderRequest;
import in.gov.sih.sih26135.dto.registration.RegistrationReferenceDataResponse;
import in.gov.sih.sih26135.dto.registration.RegistrationReferenceDataResponse.ReferenceItem;
import in.gov.sih.sih26135.entity.Employer;
import in.gov.sih.sih26135.entity.OnboardingApplication;
import in.gov.sih.sih26135.entity.RefAccreditationStatus;
import in.gov.sih.sih26135.entity.RefRecordVerificationStatus;
import in.gov.sih.sih26135.entity.Trainee;
import in.gov.sih.sih26135.entity.TrainingProvider;
import in.gov.sih.sih26135.entity.User;
import in.gov.sih.sih26135.entity.UserRole;
import in.gov.sih.sih26135.event.CommunicationTriggerEvent;
import in.gov.sih.sih26135.exception.BadRequestException;
import in.gov.sih.sih26135.exception.ConflictException;
import in.gov.sih.sih26135.exception.ResourceNotFoundException;
import in.gov.sih.sih26135.repository.EmployerRepository;
import in.gov.sih.sih26135.repository.IndustryRepository;
import in.gov.sih.sih26135.repository.OnboardingApplicationRepository;
import in.gov.sih.sih26135.repository.RefAccreditationStatusRepository;
import in.gov.sih.sih26135.repository.RefRecordVerificationStatusRepository;
import in.gov.sih.sih26135.repository.RoleRepository;
import in.gov.sih.sih26135.repository.SectorRepository;
import in.gov.sih.sih26135.repository.TraineeRepository;
import in.gov.sih.sih26135.repository.TrainingProviderRepository;
import in.gov.sih.sih26135.repository.UserRepository;
import in.gov.sih.sih26135.repository.UserRoleRepository;
import in.gov.sih.sih26135.service.ActiveGeography;
import in.gov.sih.sih26135.service.RegistrationService;
import in.gov.sih.sih26135.service.TraineeConsentService;
import in.gov.sih.sih26135.service.auth.AuthenticationService;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class RegistrationServiceImpl implements RegistrationService {

  private static final Logger log = LoggerFactory.getLogger(RegistrationServiceImpl.class);

  private static final Long ROLE_ID_TRAINING_PROVIDER = 5L;
  private static final Long ROLE_ID_EMPLOYER = 6L;
  private static final Long ROLE_ID_TRAINEE = 9L;

  private static final Long STATUS_PENDING_ID = 1L;
  private static final Long STATUS_ACTIVE_ID = 2L;

  private final UserRepository userRepository;
  private final UserRoleRepository userRoleRepository;
  private final RoleRepository roleRepository;
  private final TraineeRepository traineeRepository;
  private final TrainingProviderRepository trainingProviderRepository;
  private final EmployerRepository employerRepository;
  private final OnboardingApplicationRepository onboardingApplicationRepository;
  private final RefAccreditationStatusRepository refAccreditationStatusRepository;
  private final RefRecordVerificationStatusRepository refRecordVerificationStatusRepository;
  private final IndustryRepository industryRepository;
  private final SectorRepository sectorRepository;
  private final PasswordEncoder passwordEncoder;
  private final AuthenticationService authenticationService;
  private final JdbcTemplate jdbcTemplate;
  private final TraineeConsentService traineeConsentService;

  public RegistrationServiceImpl(
      UserRepository userRepository,
      UserRoleRepository userRoleRepository,
      RoleRepository roleRepository,
      TraineeRepository traineeRepository,
      TrainingProviderRepository trainingProviderRepository,
      EmployerRepository employerRepository,
      OnboardingApplicationRepository onboardingApplicationRepository,
      RefAccreditationStatusRepository refAccreditationStatusRepository,
      RefRecordVerificationStatusRepository refRecordVerificationStatusRepository,
      IndustryRepository industryRepository,
      SectorRepository sectorRepository,
      PasswordEncoder passwordEncoder,
      AuthenticationService authenticationService,
      JdbcTemplate jdbcTemplate,
      TraineeConsentService traineeConsentService) {
    this.userRepository = userRepository;
    this.userRoleRepository = userRoleRepository;
    this.roleRepository = roleRepository;
    this.traineeRepository = traineeRepository;
    this.trainingProviderRepository = trainingProviderRepository;
    this.employerRepository = employerRepository;
    this.onboardingApplicationRepository = onboardingApplicationRepository;
    this.refAccreditationStatusRepository = refAccreditationStatusRepository;
    this.refRecordVerificationStatusRepository = refRecordVerificationStatusRepository;
    this.industryRepository = industryRepository;
    this.sectorRepository = sectorRepository;
    this.passwordEncoder = passwordEncoder;
    this.authenticationService = authenticationService;
    this.jdbcTemplate = jdbcTemplate;
    this.traineeConsentService = traineeConsentService;
  }

  @Override
  @Transactional
  public AuthResponse registerTrainee(RegisterTraineeRequest request) {
    if (request == null) {
      throw new BadRequestException("Registration request cannot be null");
    }
    ActiveGeography.validate(jdbcTemplate, request.getStateId(), request.getDistrictId());
    if (!Boolean.TRUE.equals(request.getDataProcessingConsent())) {
      throw new BadRequestException("Core data processing consent is required", "REGISTRATION_CONSENT_REQUIRED");
    }
    if (request.getGenderId() == null || !Boolean.TRUE.equals(jdbcTemplate.queryForObject(
        "SELECT EXISTS(SELECT 1 FROM ref_gender WHERE gender_id = ?)", Boolean.class, request.getGenderId()))) {
      throw new BadRequestException("Select a valid gender option", "INVALID_GENDER");
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

    // 1. Create User
    User user = new User();
    user.setUsername(username);
    user.setEmail(email);
    user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
    user.setPasswordAlgo("argon2id");
    user.setUserStatusId(STATUS_ACTIVE_ID); // Trainees are active immediately
    user.setStateId(request.getStateId());
    user.setDistrictId(request.getDistrictId());
    user.setFailedLoginCount(0);
    user.setMustChangePassword(false);
    user.setCreatedAt(now);
    user.setUpdatedAt(now);
    User savedUser = userRepository.save(user);

    // 2. Assign Role Trainee (9)
    UserRole userRole = new UserRole(savedUser.getId(), ROLE_ID_TRAINEE, savedUser.getId());
    userRole.setAssignedAt(now);
    userRoleRepository.save(userRole);

    // 3. Create Trainee Profile Record
    Trainee trainee = new Trainee();
    String regNum = "TRN-" + System.currentTimeMillis() + "-" + UUID.randomUUID().toString().substring(0, 4).toUpperCase();
    trainee.setRegistrationNumber(regNum);
    trainee.setUser(savedUser);
    trainee.setFirstName(request.getFirstName().trim());
    trainee.setMiddleName(optionalText(request.getMiddleName()));
    trainee.setLastName(optionalText(request.getLastName()));
    trainee.setDateOfBirth(request.getDateOfBirth());
    trainee.setGenderId(request.getGenderId());
    trainee.setEmail(email);
    trainee.setPhone(optionalText(request.getPhone()));
    trainee.setStateId(request.getStateId());
    trainee.setDistrictId(request.getDistrictId());
    trainee.setAddressLine1(optionalText(request.getAddressLine1()));
    trainee.setAddressLine2(optionalText(request.getAddressLine2()));
    trainee.setPincode(optionalText(request.getPincode()));
    trainee.setEducationLevelId(request.getEducationLevelId());
    trainee.setCurrentEmploymentStatusId(request.getCurrentEmploymentStatusId() != null ? request.getCurrentEmploymentStatusId()
        : jdbcTemplate.queryForObject("SELECT employment_status_id FROM ref_employment_status WHERE status_code = 'UNKNOWN'", Long.class));
    trainee.setProfileStatusId(3L); // Active
    trainee.setCreatedAt(now);
    trainee.setUpdatedAt(now);
    Trainee savedTrainee = traineeRepository.save(trainee);
    traineeConsentService.recordRegistrationConsent(savedTrainee, savedUser.getId(), now);

    log.info("Successfully registered trainee: username={}, traineeId={}, regNumber={}", username, trainee.getId(), regNum);

    // 4. Return authenticated session directly
    return authenticationService.login(new LoginRequest(username, request.getPassword()));
  }

  @Override
  @Transactional
  public OnboardingApplicationResponse registerTrainingProvider(RegisterTrainingProviderRequest request) {
    if (request == null) {
      throw new BadRequestException("Training provider registration request cannot be null");
    }
    ActiveGeography.validate(jdbcTemplate, request.getStateId(), request.getDistrictId());

    String username = request.getUsername().trim();
    String email = request.getEmail().trim().toLowerCase();
    String regNumber = request.getRegistrationNumber().trim();

    if (userRepository.existsByUsername(username)) {
      throw new ConflictException("Username is already taken", "USERNAME_ALREADY_EXISTS");
    }
    if (userRepository.existsByEmail(email)) {
      throw new ConflictException("Email is already registered", "EMAIL_ALREADY_EXISTS");
    }
    if (trainingProviderRepository.existsByRegistrationNumber(regNumber)) {
      throw new ConflictException("Provider registration number already registered", "REGISTRATION_NUMBER_EXISTS");
    }

    LocalDateTime now = LocalDateTime.now();

    // 1. Create User in PENDING state
    User user = new User();
    user.setUsername(username);
    user.setEmail(email);
    user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
    user.setPasswordAlgo("argon2id");
    user.setUserStatusId(STATUS_PENDING_ID); // Awaiting administrator approval
    user.setStateId(request.getStateId());
    user.setDistrictId(request.getDistrictId());
    user.setFailedLoginCount(0);
    user.setMustChangePassword(false);
    user.setCreatedAt(now);
    user.setUpdatedAt(now);
    User savedUser = userRepository.save(user);

    // 2. Assign Role Training Provider (5)
    UserRole userRole = new UserRole(savedUser.getId(), ROLE_ID_TRAINING_PROVIDER, savedUser.getId());
    userRole.setAssignedAt(now);
    userRoleRepository.save(userRole);

    // 3. Create Training Provider entity
    TrainingProvider provider = new TrainingProvider();
    String providerCode = "TP-" + UUID.randomUUID().toString().replace("-", "").substring(0, 8).toUpperCase();
    provider.setProviderCode(providerCode);
    provider.setProviderName(request.getProviderName().trim());
    provider.setRegistrationNumber(regNumber);
    provider.setOrganizationTypeId(organizationType(request.getOrganizationTypeId(), "TRAINING_PROVIDER"));
    provider.setContactPersonName(request.getContactPersonName().trim());
    provider.setContactEmail(contactEmail(request.getContactEmail(), email));
    provider.setContactPhone(optionalText(request.getContactPhone()));
    provider.setStateId(request.getStateId());
    provider.setDistrictId(request.getDistrictId());
    provider.setAddressLine1(optionalText(request.getAddressLine1()));
    provider.setAddressLine2(optionalText(request.getAddressLine2()));
    provider.setPincode(optionalText(request.getPincode()));

    RefAccreditationStatus appliedStatus = refAccreditationStatusRepository.findById(2L)
        .orElseGet(() -> refAccreditationStatusRepository.findById(1L).orElse(null));
    provider.setAccreditationStatus(appliedStatus);
    provider.setLifecycleStatusId(1L);
    provider.setCreatedAt(now);
    provider.setUpdatedAt(now);
    TrainingProvider savedProvider = trainingProviderRepository.save(provider);

    // 4. Update user with training provider link
    savedUser.setTrainingProviderId(savedProvider.getId());
    userRepository.save(savedUser);

    // 5. Create OnboardingApplication record
    String appNumber = "APP-TP-" + System.currentTimeMillis() + "-" + UUID.randomUUID().toString().substring(0, 4).toUpperCase();
    OnboardingApplication app = new OnboardingApplication();
    app.setApplicationNumber(appNumber);
    app.setApplicantType("TRAINING_PROVIDER");
    app.setUserId(savedUser.getId());
    app.setEntityType("TRAINING_PROVIDER");
    app.setEntityId(savedProvider.getId());
    app.setOrganizationName(savedProvider.getProviderName());
    app.setContactPerson(savedProvider.getContactPersonName());
    app.setContactEmail(savedProvider.getContactEmail());
    app.setContactPhone(savedProvider.getContactPhone());
    app.setStatus("PENDING");
    app.setCreatedAt(now);
    app.setUpdatedAt(now);
    OnboardingApplication savedApp = onboardingApplicationRepository.save(app);

    log.info("Training provider application submitted: appNumber={}, providerId={}, userId={}",
        appNumber, savedProvider.getId(), savedUser.getId());

    OnboardingApplicationResponse response = mapToApplicationResponse(savedApp);
    response.setUsername(username);
    response.setMessage("Application submitted successfully. Your account is pending administrator verification.");
    return response;
  }

  @Override
  @Transactional
  public OnboardingApplicationResponse registerEmployer(RegisterEmployerRequest request) {
    if (request == null) {
      throw new BadRequestException("Employer registration request cannot be null");
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
    String registrationNumber = optionalText(request.getRegistrationNumber());
    String gstin = optionalText(request.getGstin());
    if (gstin != null) gstin = gstin.toUpperCase(Locale.ROOT);
    if (registrationNumber != null) {
      if (employerRepository.findByRegistrationNumber(registrationNumber).isPresent()) {
        throw new ConflictException("Registration number already registered", "REGISTRATION_NUMBER_EXISTS");
      }
    }
    if (gstin != null) {
      if (employerRepository.findByGstin(gstin).isPresent()) {
        throw new ConflictException("GSTIN already registered", "GSTIN_EXISTS");
      }
    }

    LocalDateTime now = LocalDateTime.now();

    // 1. Create User in PENDING state
    User user = new User();
    user.setUsername(username);
    user.setEmail(email);
    user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
    user.setPasswordAlgo("argon2id");
    user.setUserStatusId(STATUS_PENDING_ID);
    user.setStateId(request.getStateId());
    user.setDistrictId(request.getDistrictId());
    user.setFailedLoginCount(0);
    user.setMustChangePassword(false);
    user.setCreatedAt(now);
    user.setUpdatedAt(now);
    User savedUser = userRepository.save(user);

    // 2. Assign Role Employer (6)
    UserRole userRole = new UserRole(savedUser.getId(), ROLE_ID_EMPLOYER, savedUser.getId());
    userRole.setAssignedAt(now);
    userRoleRepository.save(userRole);

    // 3. Create Employer entity
    Employer employer = new Employer();
    String employerCode = "EMP-" + UUID.randomUUID().toString().replace("-", "").substring(0, 8).toUpperCase();
    employer.setEmployerCode(employerCode);
    employer.setEmployerName(request.getEmployerName().trim());
    employer.setRegistrationNumber(registrationNumber);
    employer.setGstin(gstin);
    employer.setOrganizationTypeId(organizationType(request.getOrganizationTypeId(), "EMPLOYER"));
    if (request.getIndustryId() != null) {
      industryRepository.findById(request.getIndustryId()).ifPresent(employer::setIndustry);
    }
    if (request.getSectorId() != null) {
      sectorRepository.findById(request.getSectorId()).ifPresent(employer::setSector);
    }
    employer.setContactPersonName(request.getContactPersonName().trim());
    employer.setContactEmail(contactEmail(request.getContactEmail(), email));
    employer.setContactPhone(optionalText(request.getContactPhone()));
    employer.setStateId(request.getStateId());
    employer.setDistrictId(request.getDistrictId());
    employer.setAddressLine1(optionalText(request.getAddressLine1()));
    employer.setAddressLine2(optionalText(request.getAddressLine2()));
    employer.setPincode(optionalText(request.getPincode()));

    RefRecordVerificationStatus pendingStatus = refRecordVerificationStatusRepository.findById(3L)
        .orElseGet(() -> refRecordVerificationStatusRepository.findById(1L).orElse(null));
    employer.setRecordVerificationStatus(pendingStatus);
    employer.setLifecycleStatusId(1L);
    employer.setCreatedAt(now);
    employer.setUpdatedAt(now);
    Employer savedEmployer = employerRepository.save(employer);

    // 4. Update user with employer link
    savedUser.setEmployerId(savedEmployer.getId());
    userRepository.save(savedUser);

    // 5. Create OnboardingApplication record
    String appNumber = "APP-EMP-" + System.currentTimeMillis() + "-" + UUID.randomUUID().toString().substring(0, 4).toUpperCase();
    OnboardingApplication app = new OnboardingApplication();
    app.setApplicationNumber(appNumber);
    app.setApplicantType("EMPLOYER");
    app.setUserId(savedUser.getId());
    app.setEntityType("EMPLOYER");
    app.setEntityId(savedEmployer.getId());
    app.setOrganizationName(savedEmployer.getEmployerName());
    app.setContactPerson(savedEmployer.getContactPersonName());
    app.setContactEmail(savedEmployer.getContactEmail());
    app.setContactPhone(savedEmployer.getContactPhone());
    app.setStatus("PENDING");
    app.setCreatedAt(now);
    app.setUpdatedAt(now);
    OnboardingApplication savedApp = onboardingApplicationRepository.save(app);

    log.info("Employer application submitted: appNumber={}, employerId={}, userId={}",
        appNumber, savedEmployer.getId(), savedUser.getId());

    OnboardingApplicationResponse response = mapToApplicationResponse(savedApp);
    response.setUsername(username);
    response.setMessage("Application submitted successfully. Your account is pending administrator verification.");
    return response;
  }

  private static String optionalText(String value) {
    return value == null || value.isBlank() ? null : value.strip();
  }

  private static String contactEmail(String value, String accountEmail) {
    String supplied = optionalText(value);
    return supplied == null ? accountEmail : supplied.toLowerCase(Locale.ROOT);
  }

  private Long organizationType(Long supplied, String defaultCode) {
    return supplied != null ? supplied : jdbcTemplate.queryForObject(
        "SELECT organization_type_id FROM ref_organization_type WHERE type_code = ?", Long.class, defaultCode);
  }

  @Override
  public ApplicationStatusResponse getApplicationStatus(String applicationNumber) {
    if (applicationNumber == null || applicationNumber.isBlank()) {
      throw new BadRequestException("Application number is required");
    }

    OnboardingApplication app = onboardingApplicationRepository.findByApplicationNumber(applicationNumber.trim())
        .orElseThrow(() -> new ResourceNotFoundException("OnboardingApplication", "applicationNumber"));

    return new ApplicationStatusResponse(
        app.getApplicationNumber(),
        app.getApplicantType(),
        app.getOrganizationName(),
        CommunicationTriggerEvent.maskRecipient(app.getContactEmail()),
        app.getStatus(),
        app.getRejectionReason(),
        app.getCreatedAt(),
        app.getReviewedAt()
    );
  }

  @Override
  public RegistrationReferenceDataResponse getReferenceData() {
    RegistrationReferenceDataResponse response = new RegistrationReferenceDataResponse();

    // Genders
    List<ReferenceItem> genders = new ArrayList<>();
    try {
      jdbcTemplate.query("SELECT gender_id, gender_code, gender_name FROM ref_gender ORDER BY sort_order", (rs) -> {
        genders.add(new ReferenceItem(rs.getLong("gender_id"), rs.getString("gender_code"), rs.getString("gender_name")));
      });
    } catch (Exception e) {
      log.warn("Could not query ref_gender: {}", e.getMessage());
    }
    if (genders.isEmpty()) {
      genders.add(new ReferenceItem(1L, "FEMALE", "Female"));
      genders.add(new ReferenceItem(2L, "MALE", "Male"));
      genders.add(new ReferenceItem(3L, "TRANSGENDER", "Transgender"));
      genders.add(new ReferenceItem(4L, "OTHER", "Other"));
      genders.add(new ReferenceItem(5L, "PREFER_NOT_TO_SAY", "Prefer not to say"));
    }
    response.setGenders(genders);

    response.setStates(jdbcTemplate.query("SELECT s.state_id, s.state_code, s.state_name " + ActiveGeography.STATES
        + " ORDER BY s.state_name", (rs, row) -> new ReferenceItem(rs.getLong(1), rs.getString(2), rs.getString(3))));
    response.setDistricts(jdbcTemplate.query("SELECT d.district_id, d.district_name, d.state_id " + ActiveGeography.DISTRICTS
        + " ORDER BY d.district_name", (rs, row) -> new ReferenceItem(rs.getLong(1), rs.getString(1), rs.getString(2), rs.getLong(3))));

    // Organization Types
    List<ReferenceItem> orgTypes = new ArrayList<>();
    try {
      jdbcTemplate.query("SELECT organization_type_id, type_code, type_name FROM ref_organization_type ORDER BY sort_order", (rs) -> {
        orgTypes.add(new ReferenceItem(rs.getLong("organization_type_id"), rs.getString("type_code"), rs.getString("type_name")));
      });
    } catch (Exception e) {
      log.warn("Could not query ref_organization_type: {}", e.getMessage());
    }
    if (orgTypes.isEmpty()) {
      orgTypes.add(new ReferenceItem(1L, "CENTRAL_MINISTRY", "Central Ministry"));
      orgTypes.add(new ReferenceItem(2L, "STATE_DEPARTMENT", "State Department"));
      orgTypes.add(new ReferenceItem(5L, "TRAINING_PROVIDER", "Training Provider"));
      orgTypes.add(new ReferenceItem(6L, "CORPORATE_EMPLOYER", "Corporate Employer"));
    }
    response.setOrganizationTypes(orgTypes);

    // Employment Statuses
    List<ReferenceItem> empStatuses = new ArrayList<>();
    try {
      jdbcTemplate.query("SELECT employment_status_id, status_code, status_name FROM ref_employment_status ORDER BY sort_order", (rs) -> {
        empStatuses.add(new ReferenceItem(rs.getLong("employment_status_id"), rs.getString("status_code"), rs.getString("status_name")));
      });
    } catch (Exception e) {
      log.warn("Could not query ref_employment_status: {}", e.getMessage());
    }
    if (empStatuses.isEmpty()) {
      empStatuses.add(new ReferenceItem(1L, "UNEMPLOYED", "Unemployed"));
      empStatuses.add(new ReferenceItem(2L, "IN_TRAINING", "In Training"));
      empStatuses.add(new ReferenceItem(3L, "WAGE_EMPLOYED", "Wage Employed"));
      empStatuses.add(new ReferenceItem(4L, "SELF_EMPLOYED", "Self Employed"));
      empStatuses.add(new ReferenceItem(5L, "APPRENTICE", "Apprentice"));
    }
    response.setEmploymentStatuses(empStatuses);

    return response;
  }

  private OnboardingApplicationResponse mapToApplicationResponse(OnboardingApplication app) {
    OnboardingApplicationResponse resp = new OnboardingApplicationResponse();
    resp.setApplicationId(app.getId());
    resp.setApplicationNumber(app.getApplicationNumber());
    resp.setApplicantType(app.getApplicantType());
    resp.setUserId(app.getUserId());
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
