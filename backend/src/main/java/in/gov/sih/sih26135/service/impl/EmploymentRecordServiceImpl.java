package in.gov.sih.sih26135.service.impl;

import in.gov.sih.sih26135.dto.request.CreateEmploymentRecordRequest;
import in.gov.sih.sih26135.dto.request.UpdateEmploymentRecordRequest;
import in.gov.sih.sih26135.dto.response.EmploymentRecordResponse;
import in.gov.sih.sih26135.entity.Employer;
import in.gov.sih.sih26135.entity.EmployerBranch;
import in.gov.sih.sih26135.entity.EmploymentRecord;
import in.gov.sih.sih26135.entity.JobRole;
import in.gov.sih.sih26135.entity.PlacementRecord;
import in.gov.sih.sih26135.entity.RefEmploymentExitReason;
import in.gov.sih.sih26135.entity.RefEmploymentInfoSource;
import in.gov.sih.sih26135.entity.RefEmploymentSpellStatus;
import in.gov.sih.sih26135.entity.RefEngagementType;
import in.gov.sih.sih26135.entity.RefRecordVerificationStatus;
import in.gov.sih.sih26135.entity.RefSalaryFrequency;
import in.gov.sih.sih26135.entity.Trainee;
import in.gov.sih.sih26135.entity.TrainingEnrollment;
import in.gov.sih.sih26135.exception.BadRequestException;
import in.gov.sih.sih26135.exception.ConflictException;
import in.gov.sih.sih26135.exception.ResourceNotFoundException;
import in.gov.sih.sih26135.mapper.EmploymentRecordMapper;
import in.gov.sih.sih26135.repository.EmployerBranchRepository;
import in.gov.sih.sih26135.repository.EmployerRepository;
import in.gov.sih.sih26135.repository.EmploymentRecordRepository;
import in.gov.sih.sih26135.repository.JobRoleRepository;
import in.gov.sih.sih26135.repository.PlacementRecordRepository;
import in.gov.sih.sih26135.repository.RefEmploymentExitReasonRepository;
import in.gov.sih.sih26135.repository.RefEmploymentInfoSourceRepository;
import in.gov.sih.sih26135.repository.RefEmploymentSpellStatusRepository;
import in.gov.sih.sih26135.repository.RefEngagementTypeRepository;
import in.gov.sih.sih26135.repository.RefRecordVerificationStatusRepository;
import in.gov.sih.sih26135.repository.RefSalaryFrequencyRepository;
import in.gov.sih.sih26135.repository.TraineeRepository;
import in.gov.sih.sih26135.repository.TrainingEnrollmentRepository;
import in.gov.sih.sih26135.service.EmploymentRecordService;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class EmploymentRecordServiceImpl implements EmploymentRecordService {

  private final EmploymentRecordRepository employmentRecordRepository;
  private final TraineeRepository traineeRepository;
  private final TrainingEnrollmentRepository trainingEnrollmentRepository;
  private final PlacementRecordRepository placementRecordRepository;
  private final EmployerRepository employerRepository;
  private final EmployerBranchRepository employerBranchRepository;
  private final JobRoleRepository jobRoleRepository;
  private final RefEngagementTypeRepository refEngagementTypeRepository;
  private final RefEmploymentSpellStatusRepository refEmploymentSpellStatusRepository;
  private final RefSalaryFrequencyRepository refSalaryFrequencyRepository;
  private final RefEmploymentInfoSourceRepository refEmploymentInfoSourceRepository;
  private final RefRecordVerificationStatusRepository refRecordVerificationStatusRepository;
  private final RefEmploymentExitReasonRepository refEmploymentExitReasonRepository;
  private final EmploymentRecordMapper employmentRecordMapper;

  public EmploymentRecordServiceImpl(
      EmploymentRecordRepository employmentRecordRepository,
      TraineeRepository traineeRepository,
      TrainingEnrollmentRepository trainingEnrollmentRepository,
      PlacementRecordRepository placementRecordRepository,
      EmployerRepository employerRepository,
      EmployerBranchRepository employerBranchRepository,
      JobRoleRepository jobRoleRepository,
      RefEngagementTypeRepository refEngagementTypeRepository,
      RefEmploymentSpellStatusRepository refEmploymentSpellStatusRepository,
      RefSalaryFrequencyRepository refSalaryFrequencyRepository,
      RefEmploymentInfoSourceRepository refEmploymentInfoSourceRepository,
      RefRecordVerificationStatusRepository refRecordVerificationStatusRepository,
      RefEmploymentExitReasonRepository refEmploymentExitReasonRepository,
      EmploymentRecordMapper employmentRecordMapper) {
    this.employmentRecordRepository = employmentRecordRepository;
    this.traineeRepository = traineeRepository;
    this.trainingEnrollmentRepository = trainingEnrollmentRepository;
    this.placementRecordRepository = placementRecordRepository;
    this.employerRepository = employerRepository;
    this.employerBranchRepository = employerBranchRepository;
    this.jobRoleRepository = jobRoleRepository;
    this.refEngagementTypeRepository = refEngagementTypeRepository;
    this.refEmploymentSpellStatusRepository = refEmploymentSpellStatusRepository;
    this.refSalaryFrequencyRepository = refSalaryFrequencyRepository;
    this.refEmploymentInfoSourceRepository = refEmploymentInfoSourceRepository;
    this.refRecordVerificationStatusRepository = refRecordVerificationStatusRepository;
    this.refEmploymentExitReasonRepository = refEmploymentExitReasonRepository;
    this.employmentRecordMapper = employmentRecordMapper;
  }

  @Override
  public EmploymentRecordResponse getEmploymentRecordById(Long id) {
    if (id == null) {
      throw new BadRequestException("Employment record ID is required");
    }
    EmploymentRecord record = employmentRecordRepository.findById(id)
        .orElseThrow(() -> new ResourceNotFoundException("EmploymentRecord", "id"));
    return employmentRecordMapper.toResponse(record);
  }

  @Override
  public EmploymentRecordResponse getEmploymentRecordByNumber(String employmentNumber) {
    if (employmentNumber == null || employmentNumber.isBlank()) {
      throw new BadRequestException("Employment number is required");
    }
    EmploymentRecord record = employmentRecordRepository.findByEmploymentNumber(employmentNumber.trim())
        .orElseThrow(() -> new ResourceNotFoundException("EmploymentRecord", "employmentNumber"));
    return employmentRecordMapper.toResponse(record);
  }

  @Override
  public EmploymentRecordResponse getEmploymentRecordByPlacementId(Long placementId) {
    if (placementId == null) {
      throw new BadRequestException("Placement ID is required");
    }
    EmploymentRecord record = employmentRecordRepository.findByPlacementRecordId(placementId)
        .orElseThrow(() -> new ResourceNotFoundException("EmploymentRecord", "placementId"));
    return employmentRecordMapper.toResponse(record);
  }

  @Override
  public List<EmploymentRecordResponse> getAllEmploymentRecords(boolean includeDeleted) {
    List<EmploymentRecord> records = includeDeleted
        ? employmentRecordRepository.findAll()
        : employmentRecordRepository.findByDeletedAtIsNull();
    return records.stream()
        .map(employmentRecordMapper::toResponse)
        .toList();
  }

  @Override
  public List<EmploymentRecordResponse> getEmploymentRecordsByTrainee(Long traineeId) {
    if (traineeId == null) {
      throw new BadRequestException("Trainee ID is required");
    }
    return employmentRecordRepository.findByTraineeId(traineeId).stream()
        .map(employmentRecordMapper::toResponse)
        .toList();
  }

  @Override
  public List<EmploymentRecordResponse> getEmploymentRecordsByEmployer(Long employerId) {
    if (employerId == null) {
      throw new BadRequestException("Employer ID is required");
    }
    return employmentRecordRepository.findByEmployerId(employerId).stream()
        .map(employmentRecordMapper::toResponse)
        .toList();
  }

  @Override
  public List<EmploymentRecordResponse> getEmploymentRecordsByStatus(Long statusId) {
    if (statusId == null) {
      throw new BadRequestException("Employment spell status ID is required");
    }
    return employmentRecordRepository.findByEmploymentSpellStatusId(statusId).stream()
        .map(employmentRecordMapper::toResponse)
        .toList();
  }

  @Override
  public List<EmploymentRecordResponse> getCurrentEmploymentRecordsByTrainee(Long traineeId) {
    if (traineeId == null) {
      throw new BadRequestException("Trainee ID is required");
    }
    return employmentRecordRepository.findByTraineeIdAndIsCurrent(traineeId, true).stream()
        .filter(r -> r.getDeletedAt() == null)
        .map(employmentRecordMapper::toResponse)
        .toList();
  }

  @Override
  @Transactional
  public EmploymentRecordResponse createEmploymentRecord(CreateEmploymentRecordRequest request) {
    if (request == null) {
      throw new BadRequestException("Employment record creation request cannot be null");
    }
    if (request.getEmploymentNumber() == null || request.getEmploymentNumber().isBlank()) {
      throw new BadRequestException("Employment number is required");
    }
    if (request.getTraineeId() == null) {
      throw new BadRequestException("Trainee ID is required");
    }
    if (request.getStartDate() == null) {
      throw new BadRequestException("Start date is required");
    }
    if (request.getEmploymentSpellStatusId() == null) {
      throw new BadRequestException("Employment spell status ID is required");
    }
    if (request.getEmploymentInfoSourceId() == null) {
      throw new BadRequestException("Employment info source ID is required");
    }
    if (request.getRecordVerificationStatusId() == null) {
      throw new BadRequestException("Record verification status ID is required");
    }

    String employmentNumber = request.getEmploymentNumber().trim();
    if (employmentRecordRepository.existsByEmploymentNumber(employmentNumber)) {
      throw new ConflictException("Employment number already exists", "EMPLOYMENT_NUMBER_ALREADY_EXISTS");
    }

    Trainee trainee = traineeRepository.findById(request.getTraineeId())
        .orElseThrow(() -> new ResourceNotFoundException("Trainee", "traineeId"));

    PlacementRecord placement = null;
    if (request.getPlacementId() != null) {
      if (employmentRecordRepository.findByPlacementRecordId(request.getPlacementId()).isPresent()) {
        throw new ConflictException("Placement record is already linked to another employment record", "DUPLICATE_PLACEMENT_EMPLOYMENT");
      }
      placement = placementRecordRepository.findById(request.getPlacementId())
          .orElseThrow(() -> new ResourceNotFoundException("PlacementRecord", "placementId"));

      if (!placement.getTrainee().getId().equals(trainee.getId())) {
        throw new BadRequestException("Placement record does not belong to the trainee", "TRAINEE_PLACEMENT_MISMATCH");
      }
    }

    TrainingEnrollment enrollment = null;
    Long effectiveEnrollmentId = request.getEnrollmentId() != null
        ? request.getEnrollmentId()
        : (placement != null ? placement.getEnrollment().getId() : null);
    if (effectiveEnrollmentId != null) {
      enrollment = trainingEnrollmentRepository.findById(effectiveEnrollmentId)
          .orElseThrow(() -> new ResourceNotFoundException("TrainingEnrollment", "enrollmentId"));

      if (enrollment.getDeletedAt() != null) {
        throw new BadRequestException("Training enrollment is not active", "INACTIVE_ENROLLMENT");
      }
      if (enrollment.getEnrollmentStatus() != null && Boolean.TRUE.equals(enrollment.getEnrollmentStatus().getIsTerminal())) {
        throw new BadRequestException("Training enrollment is not active", "INACTIVE_ENROLLMENT");
      }
      if (!enrollment.getTrainee().getId().equals(trainee.getId())) {
        throw new BadRequestException("Enrollment does not belong to the trainee", "TRAINEE_ENROLLMENT_MISMATCH");
      }
      if (placement != null && !placement.getEnrollment().getId().equals(enrollment.getId())) {
        throw new BadRequestException("Enrollment does not match placement enrollment", "PLACEMENT_ENROLLMENT_MISMATCH");
      }
    }

    Long effectiveEngagementTypeId = request.getEngagementTypeId() != null
        ? request.getEngagementTypeId()
        : (placement != null ? placement.getEngagementType().getId() : null);
    if (effectiveEngagementTypeId == null) {
      throw new BadRequestException("Engagement type ID is required", "ENGAGEMENT_TYPE_REQUIRED");
    }
    RefEngagementType engagementType = refEngagementTypeRepository.findById(effectiveEngagementTypeId)
        .orElseThrow(() -> new ResourceNotFoundException("RefEngagementType", "engagementTypeId"));

    if (placement != null && !placement.getEngagementType().getId().equals(engagementType.getId())) {
      throw new BadRequestException("Engagement type does not match placement engagement type", "PLACEMENT_ENGAGEMENT_MISMATCH");
    }

    Long effectiveEmployerId = request.getEmployerId() != null
        ? request.getEmployerId()
        : (placement != null && placement.getEmployer() != null ? placement.getEmployer().getId() : null);

    if (Boolean.TRUE.equals(engagementType.getRequiresEmployer()) && effectiveEmployerId == null) {
      throw new BadRequestException("Employer is required for this engagement type", "EMPLOYER_REQUIRED_FOR_ENGAGEMENT");
    }

    Employer employer = null;
    if (effectiveEmployerId != null) {
      employer = employerRepository.findById(effectiveEmployerId)
          .orElseThrow(() -> new ResourceNotFoundException("Employer", "employerId"));

      if (placement != null && placement.getEmployer() != null && !placement.getEmployer().getId().equals(employer.getId())) {
        throw new BadRequestException("Employer does not match placement employer", "PLACEMENT_EMPLOYER_MISMATCH");
      }
    }

    Long effectiveBranchId = request.getEmployerBranchId() != null
        ? request.getEmployerBranchId()
        : (placement != null && placement.getEmployerBranch() != null ? placement.getEmployerBranch().getId() : null);

    if (placement != null && placement.getEmployerBranch() != null
        && !Objects.equals(effectiveBranchId, placement.getEmployerBranch().getId())) {
      throw new BadRequestException("Employer branch does not match placement employer branch", "PLACEMENT_BRANCH_MISMATCH");
    }

    EmployerBranch employerBranch = null;
    if (effectiveBranchId != null) {
      employerBranch = employerBranchRepository.findById(effectiveBranchId)
          .orElseThrow(() -> new ResourceNotFoundException("EmployerBranch", "employerBranchId"));
      if (employer == null || !employerBranch.getEmployer().getId().equals(employer.getId())) {
        throw new BadRequestException("Employer branch does not belong to specified employer", "BRANCH_EMPLOYER_MISMATCH");
      }
    }

    Long effectiveJobRoleId = request.getJobRoleId() != null
        ? request.getJobRoleId()
        : (placement != null && placement.getJobRole() != null ? placement.getJobRole().getId() : null);
    JobRole jobRole = null;
    if (effectiveJobRoleId != null) {
      jobRole = jobRoleRepository.findById(effectiveJobRoleId)
          .orElseThrow(() -> new ResourceNotFoundException("JobRole", "jobRoleId"));
      if (placement != null && placement.getJobRole() != null && !placement.getJobRole().getId().equals(jobRole.getId())) {
        throw new BadRequestException("Job role does not match placement job role", "PLACEMENT_JOB_ROLE_MISMATCH");
      }
    }

    RefEmploymentSpellStatus spellStatus = refEmploymentSpellStatusRepository.findById(request.getEmploymentSpellStatusId())
        .orElseThrow(() -> new ResourceNotFoundException("RefEmploymentSpellStatus", "employmentSpellStatusId"));

    LocalDate startDate = request.getStartDate();
    LocalDate endDate = request.getEndDate();
    if (endDate != null && endDate.isBefore(startDate)) {
      throw new BadRequestException("End date cannot be before start date", "INVALID_DATE_RANGE");
    }

    boolean isCurrent = request.getIsCurrent() != null ? request.getIsCurrent() : (endDate == null);
    if (isCurrent && endDate != null) {
      throw new BadRequestException("Current employment spell cannot have an end date", "CURRENT_SPELL_HAS_END_DATE");
    }

    // uk_employment_records_trainee_employer_start: (trainee_id, employer_id, start_date)
    boolean duplicateStartExists = employmentRecordRepository.findByTraineeId(trainee.getId()).stream()
        .anyMatch(r -> Objects.equals(r.getEmployer() != null ? r.getEmployer().getId() : null, effectiveEmployerId)
            && r.getStartDate().equals(startDate));
    if (duplicateStartExists) {
      throw new ConflictException("Employment record already exists for trainee, employer and start date", "DUPLICATE_TRAINEE_EMPLOYER_START");
    }

    // uk_employment_records_current_per_type: at most one current spell per trainee and engagement type
    if (isCurrent) {
      boolean activeSpellExists = employmentRecordRepository.findByTraineeIdAndIsCurrent(trainee.getId(), true).stream()
          .anyMatch(r -> r.getDeletedAt() == null && r.getEngagementType().getId().equals(engagementType.getId()));
      if (activeSpellExists) {
        throw new ConflictException("Trainee already has an active current spell for this engagement type", "ACTIVE_SPELL_ALREADY_EXISTS");
      }
    }

    if (request.getStartingSalary() != null && request.getStartingSalary().compareTo(BigDecimal.ZERO) < 0) {
      throw new BadRequestException("Starting salary cannot be negative", "INVALID_SALARY");
    }

    RefSalaryFrequency salaryFrequency = null;
    if (request.getSalaryFrequencyId() != null) {
      salaryFrequency = refSalaryFrequencyRepository.findById(request.getSalaryFrequencyId())
          .orElseThrow(() -> new ResourceNotFoundException("RefSalaryFrequency", "salaryFrequencyId"));
    }

    RefEmploymentInfoSource infoSource = refEmploymentInfoSourceRepository.findById(request.getEmploymentInfoSourceId())
        .orElseThrow(() -> new ResourceNotFoundException("RefEmploymentInfoSource", "employmentInfoSourceId"));

    RefRecordVerificationStatus verificationStatus = refRecordVerificationStatusRepository
        .findById(request.getRecordVerificationStatusId())
        .orElseThrow(() -> new ResourceNotFoundException("RefRecordVerificationStatus", "recordVerificationStatusId"));

    RefEmploymentExitReason exitReason = null;
    if (request.getEmploymentExitReasonId() != null) {
      exitReason = refEmploymentExitReasonRepository.findById(request.getEmploymentExitReasonId())
          .orElseThrow(() -> new ResourceNotFoundException("RefEmploymentExitReason", "employmentExitReasonId"));
    }

    EmploymentRecord entity = employmentRecordMapper.toEntity(
        request, trainee, enrollment, placement, employer, employerBranch, jobRole,
        engagementType, spellStatus, salaryFrequency, infoSource, verificationStatus, exitReason);

    entity.setIsCurrent(isCurrent);

    EmploymentRecord saved = employmentRecordRepository.save(entity);
    return employmentRecordMapper.toResponse(saved);
  }

  @Override
  @Transactional
  public EmploymentRecordResponse updateEmploymentRecord(Long id, UpdateEmploymentRecordRequest request) {
    if (id == null) {
      throw new BadRequestException("Employment record ID is required");
    }
    if (request == null) {
      throw new BadRequestException("Employment record update request cannot be null");
    }

    EmploymentRecord record = employmentRecordRepository.findById(id)
        .orElseThrow(() -> new ResourceNotFoundException("EmploymentRecord", "id"));

    if (record.getDeletedAt() != null) {
      throw new BadRequestException("Cannot update a deleted employment record", "EMPLOYMENT_RECORD_DELETED");
    }

    Employer employer = record.getEmployer();
    if (request.getEmployerBranchId() != null) {
      EmployerBranch branch = employerBranchRepository.findById(request.getEmployerBranchId())
          .orElseThrow(() -> new ResourceNotFoundException("EmployerBranch", "employerBranchId"));
      if (employer == null || !branch.getEmployer().getId().equals(employer.getId())) {
        throw new BadRequestException("Employer branch does not belong to specified employer", "BRANCH_EMPLOYER_MISMATCH");
      }
      record.setEmployerBranch(branch);
    }

    if (request.getJobRoleId() != null) {
      JobRole jobRole = jobRoleRepository.findById(request.getJobRoleId())
          .orElseThrow(() -> new ResourceNotFoundException("JobRole", "jobRoleId"));
      record.setJobRole(jobRole);
    }

    RefEngagementType engagementType = record.getEngagementType();
    if (request.getEngagementTypeId() != null) {
      engagementType = refEngagementTypeRepository.findById(request.getEngagementTypeId())
          .orElseThrow(() -> new ResourceNotFoundException("RefEngagementType", "engagementTypeId"));
      if (Boolean.TRUE.equals(engagementType.getRequiresEmployer()) && employer == null) {
        throw new BadRequestException("Employer is required for this engagement type", "EMPLOYER_REQUIRED_FOR_ENGAGEMENT");
      }
      record.setEngagementType(engagementType);
    }

    if (request.getEmploymentSpellStatusId() != null) {
      RefEmploymentSpellStatus spellStatus = refEmploymentSpellStatusRepository.findById(request.getEmploymentSpellStatusId())
          .orElseThrow(() -> new ResourceNotFoundException("RefEmploymentSpellStatus", "employmentSpellStatusId"));
      record.setEmploymentSpellStatus(spellStatus);
    }

    LocalDate startDate = request.getStartDate() != null ? request.getStartDate() : record.getStartDate();
    LocalDate endDate = request.getEndDate() != null ? request.getEndDate() : record.getEndDate();
    if (endDate != null && endDate.isBefore(startDate)) {
      throw new BadRequestException("End date cannot be before start date", "INVALID_DATE_RANGE");
    }

    // uk_employment_records_trainee_employer_start: (trainee_id, employer_id, start_date)
    Long effectiveEmployerId = record.getEmployer() != null ? record.getEmployer().getId() : null;
    boolean duplicateStartExists = employmentRecordRepository.findByTraineeId(record.getTrainee().getId()).stream()
        .anyMatch(r -> !r.getId().equals(id)
            && Objects.equals(r.getEmployer() != null ? r.getEmployer().getId() : null, effectiveEmployerId)
            && r.getStartDate().equals(startDate));
    if (duplicateStartExists) {
      throw new ConflictException("Employment record already exists for trainee, employer and start date", "DUPLICATE_TRAINEE_EMPLOYER_START");
    }

    if (request.getStartDate() != null) {
      record.setStartDate(request.getStartDate());
    }
    if (request.getEndDate() != null) {
      record.setEndDate(request.getEndDate());
    }

    Boolean isCurrent = request.getIsCurrent() != null ? request.getIsCurrent() : record.getIsCurrent();
    if (Boolean.TRUE.equals(isCurrent) && record.getEndDate() != null) {
      throw new BadRequestException("Current employment spell cannot have an end date", "CURRENT_SPELL_HAS_END_DATE");
    }

    if (Boolean.TRUE.equals(isCurrent)) {
      final Long engTypeId = engagementType.getId();
      boolean activeSpellExists = employmentRecordRepository.findByTraineeIdAndIsCurrent(record.getTrainee().getId(), true).stream()
          .anyMatch(r -> !r.getId().equals(id) && r.getDeletedAt() == null && r.getEngagementType().getId().equals(engTypeId));
      if (activeSpellExists) {
        throw new ConflictException("Trainee already has an active current spell for this engagement type", "ACTIVE_SPELL_ALREADY_EXISTS");
      }
    }
    record.setIsCurrent(isCurrent);

    if (request.getStartingSalary() != null) {
      if (request.getStartingSalary().compareTo(BigDecimal.ZERO) < 0) {
        throw new BadRequestException("Starting salary cannot be negative", "INVALID_SALARY");
      }
      record.setStartingSalary(request.getStartingSalary());
    }

    if (request.getSalaryFrequencyId() != null) {
      RefSalaryFrequency frequency = refSalaryFrequencyRepository.findById(request.getSalaryFrequencyId())
          .orElseThrow(() -> new ResourceNotFoundException("RefSalaryFrequency", "salaryFrequencyId"));
      record.setSalaryFrequency(frequency);
    }

    if (request.getCurrencyCode() != null && !request.getCurrencyCode().isBlank()) {
      record.setCurrencyCode(request.getCurrencyCode().trim());
    }
    if (request.getWorkLocationId() != null) {
      record.setWorkLocationId(request.getWorkLocationId());
    }
    if (request.getWorkStateId() != null) {
      record.setWorkStateId(request.getWorkStateId());
    }
    if (request.getWorkDistrictId() != null) {
      record.setWorkDistrictId(request.getWorkDistrictId());
    }

    if (request.getEmploymentInfoSourceId() != null) {
      RefEmploymentInfoSource source = refEmploymentInfoSourceRepository.findById(request.getEmploymentInfoSourceId())
          .orElseThrow(() -> new ResourceNotFoundException("RefEmploymentInfoSource", "employmentInfoSourceId"));
      record.setEmploymentInfoSource(source);
    }

    if (request.getRecordVerificationStatusId() != null) {
      RefRecordVerificationStatus verificationStatus = refRecordVerificationStatusRepository
          .findById(request.getRecordVerificationStatusId())
          .orElseThrow(() -> new ResourceNotFoundException("RefRecordVerificationStatus", "recordVerificationStatusId"));
      record.setRecordVerificationStatus(verificationStatus);
    }

    if (request.getVerifiedAt() != null) {
      record.setVerifiedAt(request.getVerifiedAt());
    }
    if (request.getVerifiedByUserId() != null) {
      record.setVerifiedByUserId(request.getVerifiedByUserId());
    }

    if (request.getEmploymentExitReasonId() != null) {
      RefEmploymentExitReason exitReason = refEmploymentExitReasonRepository.findById(request.getEmploymentExitReasonId())
          .orElseThrow(() -> new ResourceNotFoundException("RefEmploymentExitReason", "employmentExitReasonId"));
      record.setEmploymentExitReason(exitReason);
    }

    if (request.getExitRemarks() != null) {
      record.setExitRemarks(request.getExitRemarks());
    }

    EmploymentRecord saved = employmentRecordRepository.save(record);
    return employmentRecordMapper.toResponse(saved);
  }

  @Override
  @Transactional
  public void deleteEmploymentRecord(Long id) {
    if (id == null) {
      throw new BadRequestException("Employment record ID is required");
    }
    EmploymentRecord record = employmentRecordRepository.findById(id)
        .orElseThrow(() -> new ResourceNotFoundException("EmploymentRecord", "id"));
    if (record.getDeletedAt() == null) {
      record.setDeletedAt(LocalDateTime.now());
      employmentRecordRepository.save(record);
    }
  }
}
