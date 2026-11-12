package in.gov.sih.sih26135.service.ingestion;

import in.gov.sih.sih26135.dto.request.CreatePlacementRecordRequest;
import in.gov.sih.sih26135.entity.Employer;
import in.gov.sih.sih26135.entity.EmployerBranch;
import in.gov.sih.sih26135.entity.JobApplication;
import in.gov.sih.sih26135.entity.JobPosting;
import in.gov.sih.sih26135.entity.JobRole;
import in.gov.sih.sih26135.entity.PlacementRecord;
import in.gov.sih.sih26135.entity.RefEngagementType;
import in.gov.sih.sih26135.entity.RefJoiningStatus;
import in.gov.sih.sih26135.entity.RefNonSelectionReason;
import in.gov.sih.sih26135.entity.RefPlacementSource;
import in.gov.sih.sih26135.entity.RefPlacementStatus;
import in.gov.sih.sih26135.entity.RefRecordVerificationStatus;
import in.gov.sih.sih26135.entity.RefSalaryFrequency;
import in.gov.sih.sih26135.entity.TrainingCenter;
import in.gov.sih.sih26135.entity.TrainingEnrollment;
import in.gov.sih.sih26135.mapper.PlacementRecordMapper;
import in.gov.sih.sih26135.repository.EmployerBranchRepository;
import in.gov.sih.sih26135.repository.EmployerRepository;
import in.gov.sih.sih26135.repository.JobApplicationRepository;
import in.gov.sih.sih26135.repository.JobPostingRepository;
import in.gov.sih.sih26135.repository.JobRoleRepository;
import in.gov.sih.sih26135.repository.PlacementRecordRepository;
import in.gov.sih.sih26135.repository.RefEngagementTypeRepository;
import in.gov.sih.sih26135.repository.RefJoiningStatusRepository;
import in.gov.sih.sih26135.repository.RefNonSelectionReasonRepository;
import in.gov.sih.sih26135.repository.RefPlacementSourceRepository;
import in.gov.sih.sih26135.repository.RefPlacementStatusRepository;
import in.gov.sih.sih26135.repository.RefRecordVerificationStatusRepository;
import in.gov.sih.sih26135.repository.RefSalaryFrequencyRepository;
import in.gov.sih.sih26135.repository.TrainingEnrollmentRepository;
import in.gov.sih.sih26135.service.ingestion.ImportRowFieldParser.RowValidationFailureException;
import in.gov.sih.sih26135.service.ingestion.ImportSecurityScopeValidator.CallerScope;
import in.gov.sih.sih26135.service.ingestion.RowOutcomeResult.ChunkSeenKeys;
import jakarta.validation.Validator;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * Validates, deduplicates, enforces tenant ownership, and persists single {@link PlacementRecord} import rows
 * aligned with {@link in.gov.sih.sih26135.service.impl.PlacementRecordServiceImpl} domain rules.
 */
@Component
public class PlacementImportRowProcessor {

  private final PlacementRecordRepository placementRecordRepository;
  private final TrainingEnrollmentRepository trainingEnrollmentRepository;
  private final JobApplicationRepository jobApplicationRepository;
  private final JobPostingRepository jobPostingRepository;
  private final EmployerRepository employerRepository;
  private final EmployerBranchRepository employerBranchRepository;
  private final JobRoleRepository jobRoleRepository;
  private final RefEngagementTypeRepository refEngagementTypeRepository;
  private final RefPlacementSourceRepository refPlacementSourceRepository;
  private final RefPlacementStatusRepository refPlacementStatusRepository;
  private final RefJoiningStatusRepository refJoiningStatusRepository;
  private final RefSalaryFrequencyRepository refSalaryFrequencyRepository;
  private final RefNonSelectionReasonRepository refNonSelectionReasonRepository;
  private final RefRecordVerificationStatusRepository refRecordVerificationStatusRepository;
  private final PlacementRecordMapper placementRecordMapper;
  private final JdbcTemplate jdbcTemplate;
  private final Validator validator;

  public PlacementImportRowProcessor(
      PlacementRecordRepository placementRecordRepository,
      TrainingEnrollmentRepository trainingEnrollmentRepository,
      JobApplicationRepository jobApplicationRepository,
      JobPostingRepository jobPostingRepository,
      EmployerRepository employerRepository,
      EmployerBranchRepository employerBranchRepository,
      JobRoleRepository jobRoleRepository,
      RefEngagementTypeRepository refEngagementTypeRepository,
      RefPlacementSourceRepository refPlacementSourceRepository,
      RefPlacementStatusRepository refPlacementStatusRepository,
      RefJoiningStatusRepository refJoiningStatusRepository,
      RefSalaryFrequencyRepository refSalaryFrequencyRepository,
      RefNonSelectionReasonRepository refNonSelectionReasonRepository,
      RefRecordVerificationStatusRepository refRecordVerificationStatusRepository,
      PlacementRecordMapper placementRecordMapper,
      JdbcTemplate jdbcTemplate,
      Validator validator) {
    this.placementRecordRepository = Objects.requireNonNull(placementRecordRepository);
    this.trainingEnrollmentRepository = Objects.requireNonNull(trainingEnrollmentRepository);
    this.jobApplicationRepository = Objects.requireNonNull(jobApplicationRepository);
    this.jobPostingRepository = Objects.requireNonNull(jobPostingRepository);
    this.employerRepository = Objects.requireNonNull(employerRepository);
    this.employerBranchRepository = Objects.requireNonNull(employerBranchRepository);
    this.jobRoleRepository = Objects.requireNonNull(jobRoleRepository);
    this.refEngagementTypeRepository = Objects.requireNonNull(refEngagementTypeRepository);
    this.refPlacementSourceRepository = Objects.requireNonNull(refPlacementSourceRepository);
    this.refPlacementStatusRepository = Objects.requireNonNull(refPlacementStatusRepository);
    this.refJoiningStatusRepository = Objects.requireNonNull(refJoiningStatusRepository);
    this.refSalaryFrequencyRepository = Objects.requireNonNull(refSalaryFrequencyRepository);
    this.refNonSelectionReasonRepository = Objects.requireNonNull(refNonSelectionReasonRepository);
    this.refRecordVerificationStatusRepository = Objects.requireNonNull(refRecordVerificationStatusRepository);
    this.placementRecordMapper = Objects.requireNonNull(placementRecordMapper);
    this.jdbcTemplate = Objects.requireNonNull(jdbcTemplate);
    this.validator = Objects.requireNonNull(validator);
  }

  public RowOutcomeResult validateAndPersist(
      ParsedImportRow row,
      CallerScope callerScope,
      ChunkSeenKeys seenKeys) {
    Map<String, String> fields = row.fields();
    try {
      CreatePlacementRecordRequest dto = parseAndValidateDto(fields);
      validateWorkGeography(dto);

      if (dto.getActualJoiningDate() != null
          && dto.getOfferDate() != null
          && dto.getActualJoiningDate().isBefore(dto.getOfferDate())) {
        throw new RowValidationFailureException("INVALID_JOINING_DATE",
            "Actual joining date cannot be before offer date");
      }

      TrainingEnrollment enrollment = trainingEnrollmentRepository.findById(dto.getEnrollmentId())
          .orElseThrow(() -> new RowValidationFailureException("FOREIGN_KEY_NOT_FOUND",
              "Referenced enrollmentId does not exist"));

      if (enrollment.getDeletedAt() != null) {
        throw new RowValidationFailureException("ENROLLMENT_DELETED",
            "Cannot create placement record for a deleted training enrollment");
      }

      if (dto.getTraineeId() != null && !dto.getTraineeId().equals(enrollment.getTrainee().getId())) {
        throw new RowValidationFailureException("TRAINEE_ENROLLMENT_MISMATCH",
            "Trainee ID does not match enrollment trainee");
      }
      if (dto.getCourseId() != null && !dto.getCourseId().equals(enrollment.getCourse().getId())) {
        throw new RowValidationFailureException("COURSE_ENROLLMENT_MISMATCH",
            "Course ID does not match enrollment course");
      }
      if (dto.getProgramId() != null && !dto.getProgramId().equals(enrollment.getProgram().getId())) {
        throw new RowValidationFailureException("PROGRAM_ENROLLMENT_MISMATCH",
            "Program ID does not match enrollment program");
      }
      if (dto.getProviderId() != null && !dto.getProviderId().equals(enrollment.getTrainingProvider().getId())) {
        throw new RowValidationFailureException("PROVIDER_ENROLLMENT_MISMATCH",
            "Provider ID does not match enrollment training provider");
      }

      JobApplication jobApplication = null;
      if (dto.getJobApplicationId() != null) {
        jobApplication = jobApplicationRepository.findById(dto.getJobApplicationId())
            .orElseThrow(() -> new RowValidationFailureException("FOREIGN_KEY_NOT_FOUND",
                "Referenced jobApplicationId does not exist"));
        if (!jobApplication.getTrainee().getId().equals(enrollment.getTrainee().getId())) {
          throw new RowValidationFailureException("APPLICATION_TRAINEE_MISMATCH",
              "Job application does not belong to the enrollment trainee");
        }
        if (jobApplication.getEnrollment() != null
            && !jobApplication.getEnrollment().getId().equals(enrollment.getId())) {
          throw new RowValidationFailureException("APPLICATION_ENROLLMENT_MISMATCH",
              "Job application enrollment does not match placement enrollment");
        }
      }

      JobPosting jobPosting = null;
      if (dto.getJobPostingId() != null) {
        jobPosting = jobPostingRepository.findById(dto.getJobPostingId())
            .orElseThrow(() -> new RowValidationFailureException("FOREIGN_KEY_NOT_FOUND",
                "Referenced jobPostingId does not exist"));
        if (jobApplication != null && !jobApplication.getJobPosting().getId().equals(jobPosting.getId())) {
          throw new RowValidationFailureException("POSTING_APPLICATION_MISMATCH",
              "Job posting does not match job application posting");
        }
      } else if (jobApplication != null) {
        jobPosting = jobApplication.getJobPosting();
      }

      if (jobPosting != null && jobPosting.getEmployer() != null && dto.getEmployerId() != null
          && !dto.getEmployerId().equals(jobPosting.getEmployer().getId())) {
        throw new RowValidationFailureException("EMPLOYER_POSTING_MISMATCH",
            "Employer ID does not match job posting employer");
      }
      if (jobPosting != null && jobPosting.getJobRole() != null && dto.getJobRoleId() != null
          && !dto.getJobRoleId().equals(jobPosting.getJobRole().getId())) {
        throw new RowValidationFailureException("JOB_ROLE_POSTING_MISMATCH",
            "Job role ID does not match job posting job role");
      }

      Employer employer = null;
      Long effectiveEmployerId = dto.getEmployerId() != null
          ? dto.getEmployerId()
          : (jobPosting != null && jobPosting.getEmployer() != null ? jobPosting.getEmployer().getId() : null);
      if (effectiveEmployerId != null) {
        employer = employerRepository.findById(effectiveEmployerId)
            .filter(e -> e.getDeletedAt() == null)
            .orElseThrow(() -> new RowValidationFailureException("FOREIGN_KEY_NOT_FOUND",
                "Referenced employerId does not exist"));
      }

      EmployerBranch employerBranch = null;
      Long effectiveBranchId = dto.getEmployerBranchId() != null
          ? dto.getEmployerBranchId()
          : (jobPosting != null && jobPosting.getEmployerBranch() != null ? jobPosting.getEmployerBranch().getId() : null);
      if (effectiveBranchId != null) {
        employerBranch = employerBranchRepository.findById(effectiveBranchId)
            .orElseThrow(() -> new RowValidationFailureException("FOREIGN_KEY_NOT_FOUND",
                "Referenced employerBranchId does not exist"));
        if (employer == null || !employerBranch.getEmployer().getId().equals(employer.getId())) {
          throw new RowValidationFailureException("BRANCH_EMPLOYER_MISMATCH",
              "Employer branch does not belong to specified employer");
        }
      }

      JobRole jobRole = null;
      Long effectiveJobRoleId = dto.getJobRoleId() != null
          ? dto.getJobRoleId()
          : (jobPosting != null && jobPosting.getJobRole() != null ? jobPosting.getJobRole().getId() : null);
      if (effectiveJobRoleId != null) {
        jobRole = jobRoleRepository.findById(effectiveJobRoleId)
            .orElseThrow(() -> new RowValidationFailureException("FOREIGN_KEY_NOT_FOUND",
                "Referenced jobRoleId does not exist"));
      }

      Long effectiveEngagementTypeId = dto.getEngagementTypeId() != null
          ? dto.getEngagementTypeId()
          : (jobPosting != null && jobPosting.getEngagementType() != null ? jobPosting.getEngagementType().getId() : null);
      if (effectiveEngagementTypeId == null) {
        throw new RowValidationFailureException("MISSING_REQUIRED_FIELD",
            "Required field 'engagementTypeId' is missing");
      }
      RefEngagementType engagementType = refEngagementTypeRepository.findById(effectiveEngagementTypeId)
          .orElseThrow(() -> new RowValidationFailureException("FOREIGN_KEY_NOT_FOUND",
              "Referenced engagementTypeId does not exist"));

      if (Boolean.TRUE.equals(engagementType.getRequiresEmployer()) && employer == null) {
        throw new RowValidationFailureException("EMPLOYER_REQUIRED_FOR_ENGAGEMENT",
            "Employer is required for this engagement type");
      }

      RefPlacementSource placementSource = refPlacementSourceRepository.findById(dto.getPlacementSourceId())
          .orElseThrow(() -> new RowValidationFailureException("FOREIGN_KEY_NOT_FOUND",
              "Referenced placementSourceId does not exist"));
      RefPlacementStatus placementStatus = refPlacementStatusRepository.findById(dto.getPlacementStatusId())
          .orElseThrow(() -> new RowValidationFailureException("FOREIGN_KEY_NOT_FOUND",
              "Referenced placementStatusId does not exist"));
      RefJoiningStatus joiningStatus = refJoiningStatusRepository.findById(dto.getJoiningStatusId())
          .orElseThrow(() -> new RowValidationFailureException("FOREIGN_KEY_NOT_FOUND",
              "Referenced joiningStatusId does not exist"));

      RefSalaryFrequency salaryFrequency = null;
      if (dto.getSalaryFrequencyId() != null) {
        salaryFrequency = refSalaryFrequencyRepository.findById(dto.getSalaryFrequencyId())
            .orElseThrow(() -> new RowValidationFailureException("FOREIGN_KEY_NOT_FOUND",
                "Referenced salaryFrequencyId does not exist"));
      }

      RefNonSelectionReason nonSelectionReason = null;
      if (dto.getNonSelectionReasonId() != null) {
        nonSelectionReason = refNonSelectionReasonRepository.findById(dto.getNonSelectionReasonId())
            .orElseThrow(() -> new RowValidationFailureException("FOREIGN_KEY_NOT_FOUND",
                "Referenced nonSelectionReasonId does not exist"));
      }

      RefRecordVerificationStatus verificationStatus = refRecordVerificationStatusRepository
          .findById(dto.getRecordVerificationStatusId())
          .orElseThrow(() -> new RowValidationFailureException("FOREIGN_KEY_NOT_FOUND",
              "Referenced recordVerificationStatusId does not exist"));

      validateTenantOwnership(callerScope, enrollment, employer, dto);

      RowOutcomeResult duplicateResult = checkDuplicatePlacement(dto, enrollment.getId(), jobPosting, seenKeys);
      if (duplicateResult != null) {
        return duplicateResult;
      }

      PlacementRecord entity = placementRecordMapper.toEntity(
          dto,
          enrollment.getTrainee(),
          enrollment,
          enrollment.getCourse(),
          enrollment.getProgram(),
          enrollment.getTrainingProvider(),
          employer,
          employerBranch,
          jobPosting,
          jobApplication,
          jobRole,
          engagementType,
          placementSource,
          placementStatus,
          joiningStatus,
          salaryFrequency,
          nonSelectionReason,
          verificationStatus
      );
      LocalDateTime now = LocalDateTime.now();
      entity.setCreatedAt(now);
      entity.setUpdatedAt(now);
      PlacementRecord saved = placementRecordRepository.save(entity);

      recordIntraChunkSeenKeys(dto, enrollment.getId(), jobPosting, seenKeys);
      return RowOutcomeResult.success(ImportContractSchema.ENTITY_PLACEMENT_RECORD, saved.getId());
    } catch (RowValidationFailureException ex) {
      return RowOutcomeResult.failed(ImportContractSchema.ENTITY_PLACEMENT_RECORD, ex.getErrorCode(), ex.getMessage());
    }
  }

  private CreatePlacementRecordRequest parseAndValidateDto(Map<String, String> fields) {
    String placementNumber = ImportRowFieldParser.requireNonBlank(fields, "placementNumber", 32);
    Long enrollmentId = ImportRowFieldParser.requirePositiveLong(fields, "enrollmentId");
    Long traineeId = ImportRowFieldParser.optionalPositiveLong(fields, "traineeId");
    Long courseId = ImportRowFieldParser.optionalPositiveLong(fields, "courseId");
    Long programId = ImportRowFieldParser.optionalPositiveLong(fields, "programId");
    Long providerId = ImportRowFieldParser.optionalPositiveLong(fields, "providerId");
    Long employerId = ImportRowFieldParser.optionalPositiveLong(fields, "employerId");
    Long employerBranchId = ImportRowFieldParser.optionalPositiveLong(fields, "employerBranchId");
    Long jobPostingId = ImportRowFieldParser.optionalPositiveLong(fields, "jobPostingId");
    Long jobApplicationId = ImportRowFieldParser.optionalPositiveLong(fields, "jobApplicationId");
    Long jobRoleId = ImportRowFieldParser.optionalPositiveLong(fields, "jobRoleId");
    Long engagementTypeId = ImportRowFieldParser.optionalPositiveLong(fields, "engagementTypeId");
    Long placementSourceId = ImportRowFieldParser.requirePositiveLong(fields, "placementSourceId");
    Long placementStatusId = ImportRowFieldParser.requirePositiveLong(fields, "placementStatusId");
    Long joiningStatusId = ImportRowFieldParser.requirePositiveLong(fields, "joiningStatusId");
    BigDecimal offeredSalary = ImportRowFieldParser.optionalNonNegativeDecimal(fields, "offeredSalary");
    BigDecimal joiningSalary = ImportRowFieldParser.optionalNonNegativeDecimal(fields, "joiningSalary");
    Long salaryFrequencyId = ImportRowFieldParser.optionalPositiveLong(fields, "salaryFrequencyId");
    String currencyCode = ImportRowFieldParser.optionalTrimmed(fields, "currencyCode", 3);
    if (currencyCode != null && currencyCode.length() != 3) {
      throw new RowValidationFailureException("INVALID_CURRENCY_CODE",
          "Field 'currencyCode' must be a valid 3-letter ISO currency code");
    }
    LocalDate offerDate = ImportRowFieldParser.optionalDate(fields, "offerDate");
    LocalDate expectedJoiningDate = ImportRowFieldParser.optionalDate(fields, "expectedJoiningDate");
    LocalDate actualJoiningDate = ImportRowFieldParser.optionalDate(fields, "actualJoiningDate");
    Long nonSelectionReasonId = ImportRowFieldParser.optionalPositiveLong(fields, "nonSelectionReasonId");
    String outcomeRemarks = ImportRowFieldParser.optionalTrimmed(fields, "outcomeRemarks", 500);
    Long workStateId = ImportRowFieldParser.optionalPositiveLong(fields, "workStateId");
    Long workDistrictId = ImportRowFieldParser.optionalPositiveLong(fields, "workDistrictId");
    Long verificationStatusId = ImportRowFieldParser.requirePositiveLong(fields, "recordVerificationStatusId");

    CreatePlacementRecordRequest dto = new CreatePlacementRecordRequest(
        placementNumber, enrollmentId, engagementTypeId, placementSourceId,
        placementStatusId, joiningStatusId, verificationStatusId
    );
    dto.setTraineeId(traineeId);
    dto.setCourseId(courseId);
    dto.setProgramId(programId);
    dto.setProviderId(providerId);
    dto.setEmployerId(employerId);
    dto.setEmployerBranchId(employerBranchId);
    dto.setJobPostingId(jobPostingId);
    dto.setJobApplicationId(jobApplicationId);
    dto.setJobRoleId(jobRoleId);
    dto.setOfferedSalary(offeredSalary);
    dto.setJoiningSalary(joiningSalary);
    dto.setSalaryFrequencyId(salaryFrequencyId);
    dto.setCurrencyCode(currencyCode != null ? currencyCode.toUpperCase(Locale.ROOT) : "INR");
    dto.setOfferDate(offerDate);
    dto.setExpectedJoiningDate(expectedJoiningDate);
    dto.setActualJoiningDate(actualJoiningDate);
    dto.setNonSelectionReasonId(nonSelectionReasonId);
    dto.setOutcomeRemarks(outcomeRemarks);
    dto.setWorkStateId(workStateId);
    dto.setWorkDistrictId(workDistrictId);

    ImportRowFieldParser.runBeanValidation(validator, dto);
    return dto;
  }

  private void validateWorkGeography(CreatePlacementRecordRequest dto) {
    if (dto.getWorkStateId() != null
        && !ImportRowFieldParser.existsActiveById(jdbcTemplate, "states", "state_id", dto.getWorkStateId(), true)) {
      throw new RowValidationFailureException("FOREIGN_KEY_NOT_FOUND", "Referenced workStateId does not exist");
    }
    if (dto.getWorkDistrictId() != null) {
      Long districtStateId = ImportRowFieldParser.queryOptionalLong(
          jdbcTemplate,
          "SELECT state_id FROM districts WHERE district_id = ? AND deleted_at IS NULL",
          dto.getWorkDistrictId());
      if (districtStateId == null) {
        throw new RowValidationFailureException("FOREIGN_KEY_NOT_FOUND", "Referenced workDistrictId does not exist");
      }
      if (dto.getWorkStateId() != null && !districtStateId.equals(dto.getWorkStateId())) {
        throw new RowValidationFailureException("GEOGRAPHIC_MISMATCH",
            "Referenced workDistrictId does not belong to specified workStateId");
      }
    }
  }

  private void validateTenantOwnership(
      CallerScope scope,
      TrainingEnrollment enrollment,
      Employer employer,
      CreatePlacementRecordRequest dto) {
    if (scope.nationwideAdmin()) {
      return;
    }

    if (scope.isEmployerScoped()) {
      if (employer == null || !employer.getId().equals(scope.employerId())) {
        throw new RowValidationFailureException("CROSS_TENANT_REFERENCE",
            "Placement row employerId does not match caller employer tenant scope");
      }
      return;
    }

    if (scope.isProviderScoped()) {
      Long enrollmentProviderId = enrollment.getTrainingProvider().getId();
      if (!enrollmentProviderId.equals(scope.trainingProviderId())) {
        throw new RowValidationFailureException("CROSS_TENANT_REFERENCE",
            "Placement row enrollment belongs to another training provider");
      }
      return;
    }

    if (scope.isStateOrDistrictScoped()) {
      TrainingCenter center = enrollment.getTrainingCenter();
      Long centerStateId = center != null ? center.getStateId() : enrollment.getTrainee().getStateId();
      Long centerDistrictId = center != null ? center.getDistrictId() : enrollment.getTrainee().getDistrictId();

      if (centerStateId == null || !centerStateId.equals(scope.stateId())) {
        throw new RowValidationFailureException("CROSS_TENANT_REFERENCE",
            "Placement row enrollment is outside caller state scope");
      }
      if (dto.getWorkStateId() != null && !dto.getWorkStateId().equals(scope.stateId())) {
        throw new RowValidationFailureException("CROSS_TENANT_REFERENCE",
            "Placement row workStateId is outside caller state scope");
      }
      if (scope.districtId() != null) {
        if (centerDistrictId == null || !centerDistrictId.equals(scope.districtId())) {
          throw new RowValidationFailureException("CROSS_TENANT_REFERENCE",
              "Placement row enrollment is outside caller district scope");
        }
        if (dto.getWorkDistrictId() != null && !dto.getWorkDistrictId().equals(scope.districtId())) {
          throw new RowValidationFailureException("CROSS_TENANT_REFERENCE",
              "Placement row workDistrictId is outside caller district scope");
        }
      }
      return;
    }

    throw new RowValidationFailureException("CROSS_TENANT_REFERENCE",
        "Caller lacks valid tenant scope for placement import");
  }

  private RowOutcomeResult checkDuplicatePlacement(
      CreatePlacementRecordRequest dto,
      Long enrollmentId,
      JobPosting jobPosting,
      ChunkSeenKeys seenKeys) {
    String normNum = dto.getPlacementNumber().toLowerCase(Locale.ROOT);
    if (seenKeys.placementNumbers.contains(normNum)
        || placementRecordRepository.existsByPlacementNumber(dto.getPlacementNumber())) {
      return RowOutcomeResult.duplicate(
          ImportContractSchema.ENTITY_PLACEMENT_RECORD,
          "PLACEMENT_NUMBER_ALREADY_EXISTS",
          "Placement number already exists in database or earlier in batch");
    }

    if (dto.getJobApplicationId() != null) {
      if (seenKeys.placementJobAppIds.contains(dto.getJobApplicationId())
          || placementRecordRepository.findByJobApplicationId(dto.getJobApplicationId()).isPresent()) {
        return RowOutcomeResult.duplicate(
            ImportContractSchema.ENTITY_PLACEMENT_RECORD,
            "DUPLICATE_PLACEMENT_APPLICATION",
            "Placement record already exists for referenced jobApplicationId");
      }
    }

    if (jobPosting != null) {
      String epKey = enrollmentId + ":" + jobPosting.getId();
      if (seenKeys.placementEnrollmentPostingKeys.contains(epKey)
          || placementRecordRepository.findByEnrollmentIdAndJobPostingId(enrollmentId, jobPosting.getId()).isPresent()) {
        return RowOutcomeResult.duplicate(
            ImportContractSchema.ENTITY_PLACEMENT_RECORD,
            "DUPLICATE_PLACEMENT_ENROLLMENT_POSTING",
            "Placement record already exists for referenced enrollmentId and jobPostingId");
      }
    }
    return null;
  }

  private void recordIntraChunkSeenKeys(
      CreatePlacementRecordRequest dto,
      Long enrollmentId,
      JobPosting jobPosting,
      ChunkSeenKeys seenKeys) {
    seenKeys.placementNumbers.add(dto.getPlacementNumber().toLowerCase(Locale.ROOT));
    if (dto.getJobApplicationId() != null) {
      seenKeys.placementJobAppIds.add(dto.getJobApplicationId());
    }
    if (jobPosting != null) {
      seenKeys.placementEnrollmentPostingKeys.add(enrollmentId + ":" + jobPosting.getId());
    }
  }
}
