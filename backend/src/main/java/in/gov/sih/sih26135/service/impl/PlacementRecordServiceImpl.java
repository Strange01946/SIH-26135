package in.gov.sih.sih26135.service.impl;

import in.gov.sih.sih26135.dto.request.CreatePlacementRecordRequest;
import in.gov.sih.sih26135.dto.request.UpdatePlacementRecordRequest;
import in.gov.sih.sih26135.dto.response.PlacementRecordResponse;
import in.gov.sih.sih26135.entity.Course;
import in.gov.sih.sih26135.entity.Employer;
import in.gov.sih.sih26135.entity.EmployerBranch;
import in.gov.sih.sih26135.entity.JobApplication;
import in.gov.sih.sih26135.entity.JobPosting;
import in.gov.sih.sih26135.entity.JobRole;
import in.gov.sih.sih26135.entity.PlacementRecord;
import in.gov.sih.sih26135.entity.Program;
import in.gov.sih.sih26135.entity.RefEngagementType;
import in.gov.sih.sih26135.entity.RefJoiningStatus;
import in.gov.sih.sih26135.entity.RefNonSelectionReason;
import in.gov.sih.sih26135.entity.RefPlacementSource;
import in.gov.sih.sih26135.entity.RefPlacementStatus;
import in.gov.sih.sih26135.entity.RefRecordVerificationStatus;
import in.gov.sih.sih26135.entity.RefSalaryFrequency;
import in.gov.sih.sih26135.entity.Trainee;
import in.gov.sih.sih26135.entity.TrainingEnrollment;
import in.gov.sih.sih26135.entity.TrainingProvider;
import in.gov.sih.sih26135.exception.BadRequestException;
import in.gov.sih.sih26135.exception.ConflictException;
import in.gov.sih.sih26135.exception.ResourceNotFoundException;
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
import in.gov.sih.sih26135.service.PlacementRecordService;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class PlacementRecordServiceImpl implements PlacementRecordService {

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

  public PlacementRecordServiceImpl(
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
      PlacementRecordMapper placementRecordMapper) {
    this.placementRecordRepository = placementRecordRepository;
    this.trainingEnrollmentRepository = trainingEnrollmentRepository;
    this.jobApplicationRepository = jobApplicationRepository;
    this.jobPostingRepository = jobPostingRepository;
    this.employerRepository = employerRepository;
    this.employerBranchRepository = employerBranchRepository;
    this.jobRoleRepository = jobRoleRepository;
    this.refEngagementTypeRepository = refEngagementTypeRepository;
    this.refPlacementSourceRepository = refPlacementSourceRepository;
    this.refPlacementStatusRepository = refPlacementStatusRepository;
    this.refJoiningStatusRepository = refJoiningStatusRepository;
    this.refSalaryFrequencyRepository = refSalaryFrequencyRepository;
    this.refNonSelectionReasonRepository = refNonSelectionReasonRepository;
    this.refRecordVerificationStatusRepository = refRecordVerificationStatusRepository;
    this.placementRecordMapper = placementRecordMapper;
  }

  @Override
  public PlacementRecordResponse getPlacementRecordById(Long id) {
    if (id == null) {
      throw new BadRequestException("Placement record ID is required");
    }
    PlacementRecord record = placementRecordRepository.findById(id)
        .orElseThrow(() -> new ResourceNotFoundException("PlacementRecord", "id"));
    return placementRecordMapper.toResponse(record);
  }

  @Override
  public PlacementRecordResponse getPlacementRecordByNumber(String placementNumber) {
    if (placementNumber == null || placementNumber.isBlank()) {
      throw new BadRequestException("Placement number is required");
    }
    PlacementRecord record = placementRecordRepository.findByPlacementNumber(placementNumber.trim())
        .orElseThrow(() -> new ResourceNotFoundException("PlacementRecord", "placementNumber"));
    return placementRecordMapper.toResponse(record);
  }

  @Override
  public PlacementRecordResponse getPlacementRecordByJobApplicationId(Long jobApplicationId) {
    if (jobApplicationId == null) {
      throw new BadRequestException("Job application ID is required");
    }
    PlacementRecord record = placementRecordRepository.findByJobApplicationId(jobApplicationId)
        .orElseThrow(() -> new ResourceNotFoundException("PlacementRecord", "jobApplicationId"));
    return placementRecordMapper.toResponse(record);
  }

  @Override
  public List<PlacementRecordResponse> getAllPlacementRecords(boolean includeDeleted) {
    List<PlacementRecord> records = includeDeleted
        ? placementRecordRepository.findAll()
        : placementRecordRepository.findByDeletedAtIsNull();
    return records.stream()
        .map(placementRecordMapper::toResponse)
        .toList();
  }

  @Override
  public List<PlacementRecordResponse> getPlacementRecordsByTrainee(Long traineeId) {
    if (traineeId == null) {
      throw new BadRequestException("Trainee ID is required");
    }
    return placementRecordRepository.findByTraineeId(traineeId).stream()
        .map(placementRecordMapper::toResponse)
        .toList();
  }

  @Override
  public List<PlacementRecordResponse> getPlacementRecordsByEnrollment(Long enrollmentId) {
    if (enrollmentId == null) {
      throw new BadRequestException("Enrollment ID is required");
    }
    return placementRecordRepository.findByEnrollmentId(enrollmentId).stream()
        .map(placementRecordMapper::toResponse)
        .toList();
  }

  @Override
  public List<PlacementRecordResponse> getPlacementRecordsByEmployer(Long employerId) {
    if (employerId == null) {
      throw new BadRequestException("Employer ID is required");
    }
    return placementRecordRepository.findByEmployerId(employerId).stream()
        .map(placementRecordMapper::toResponse)
        .toList();
  }

  @Override
  public List<PlacementRecordResponse> getPlacementRecordsByJobPosting(Long jobPostingId) {
    if (jobPostingId == null) {
      throw new BadRequestException("Job posting ID is required");
    }
    return placementRecordRepository.findByJobPostingId(jobPostingId).stream()
        .map(placementRecordMapper::toResponse)
        .toList();
  }

  @Override
  public List<PlacementRecordResponse> getPlacementRecordsByCourse(Long courseId) {
    if (courseId == null) {
      throw new BadRequestException("Course ID is required");
    }
    return placementRecordRepository.findByCourseId(courseId).stream()
        .map(placementRecordMapper::toResponse)
        .toList();
  }

  @Override
  public List<PlacementRecordResponse> getPlacementRecordsByStatus(Long placementStatusId) {
    if (placementStatusId == null) {
      throw new BadRequestException("Placement status ID is required");
    }
    return placementRecordRepository.findByPlacementStatusId(placementStatusId).stream()
        .map(placementRecordMapper::toResponse)
        .toList();
  }

  @Override
  @Transactional
  public PlacementRecordResponse createPlacementRecord(CreatePlacementRecordRequest request) {
    if (request == null) {
      throw new BadRequestException("Placement record creation request cannot be null");
    }
    if (request.getPlacementNumber() == null || request.getPlacementNumber().isBlank()) {
      throw new BadRequestException("Placement number is required");
    }
    if (request.getEnrollmentId() == null) {
      throw new BadRequestException("Enrollment ID is required");
    }
    if (request.getPlacementSourceId() == null) {
      throw new BadRequestException("Placement source ID is required");
    }
    if (request.getPlacementStatusId() == null) {
      throw new BadRequestException("Placement status ID is required");
    }
    if (request.getJoiningStatusId() == null) {
      throw new BadRequestException("Joining status ID is required");
    }
    if (request.getRecordVerificationStatusId() == null) {
      throw new BadRequestException("Record verification status ID is required");
    }

    String placementNumber = request.getPlacementNumber().trim();
    if (placementRecordRepository.existsByPlacementNumber(placementNumber)) {
      throw new ConflictException("Placement number already exists", "PLACEMENT_NUMBER_ALREADY_EXISTS");
    }

    if (request.getJobApplicationId() != null) {
      if (placementRecordRepository.findByJobApplicationId(request.getJobApplicationId()).isPresent()) {
        throw new ConflictException("Job application is already associated with another placement record", "DUPLICATE_JOB_APPLICATION_PLACEMENT");
      }
    }

    if (request.getJobPostingId() != null) {
      if (placementRecordRepository.findByEnrollmentIdAndJobPostingId(request.getEnrollmentId(), request.getJobPostingId()).isPresent()) {
        throw new ConflictException("Placement record already exists for this enrollment and job posting", "DUPLICATE_ENROLLMENT_JOB_POSTING_PLACEMENT");
      }
    }

    TrainingEnrollment enrollment = trainingEnrollmentRepository.findById(request.getEnrollmentId())
        .orElseThrow(() -> new ResourceNotFoundException("TrainingEnrollment", "enrollmentId"));

    if (enrollment.getDeletedAt() != null) {
      throw new BadRequestException("Cannot create placement record for a deleted enrollment", "ENROLLMENT_DELETED");
    }

    Trainee authoritativeTrainee = enrollment.getTrainee();
    if (request.getTraineeId() != null && !request.getTraineeId().equals(authoritativeTrainee.getId())) {
      throw new BadRequestException("Trainee ID does not match enrollment trainee", "TRAINEE_ENROLLMENT_MISMATCH");
    }

    Course authoritativeCourse = enrollment.getCourse();
    if (request.getCourseId() != null && !request.getCourseId().equals(authoritativeCourse.getId())) {
      throw new BadRequestException("Course ID does not match enrollment course", "COURSE_ENROLLMENT_MISMATCH");
    }

    Program authoritativeProgram = enrollment.getProgram();
    if (request.getProgramId() != null && !request.getProgramId().equals(authoritativeProgram.getId())) {
      throw new BadRequestException("Program ID does not match enrollment program", "PROGRAM_ENROLLMENT_MISMATCH");
    }

    TrainingProvider authoritativeProvider = enrollment.getTrainingProvider();
    if (request.getProviderId() != null && !request.getProviderId().equals(authoritativeProvider.getId())) {
      throw new BadRequestException("Training provider ID does not match enrollment provider", "PROVIDER_ENROLLMENT_MISMATCH");
    }

    JobApplication jobApplication = null;
    if (request.getJobApplicationId() != null) {
      jobApplication = jobApplicationRepository.findById(request.getJobApplicationId())
          .orElseThrow(() -> new ResourceNotFoundException("JobApplication", "jobApplicationId"));

      if (!jobApplication.getTrainee().getId().equals(authoritativeTrainee.getId())) {
        throw new BadRequestException("Job application does not belong to the enrollment trainee", "APPLICATION_TRAINEE_MISMATCH");
      }
      if (jobApplication.getEnrollment() != null && !jobApplication.getEnrollment().getId().equals(enrollment.getId())) {
        throw new BadRequestException("Job application enrollment does not match placement enrollment", "APPLICATION_ENROLLMENT_MISMATCH");
      }
    }

    JobPosting jobPosting = null;
    if (request.getJobPostingId() != null) {
      jobPosting = jobPostingRepository.findById(request.getJobPostingId())
          .orElseThrow(() -> new ResourceNotFoundException("JobPosting", "jobPostingId"));
      if (jobApplication != null && !jobApplication.getJobPosting().getId().equals(jobPosting.getId())) {
        throw new BadRequestException("Job posting does not match job application posting", "POSTING_APPLICATION_MISMATCH");
      }
    } else if (jobApplication != null) {
      jobPosting = jobApplication.getJobPosting();
    }

    if (jobPosting != null && jobPosting.getEmployer() != null && request.getEmployerId() != null) {
      if (!request.getEmployerId().equals(jobPosting.getEmployer().getId())) {
        throw new BadRequestException("Employer ID does not match job posting employer", "EMPLOYER_POSTING_MISMATCH");
      }
    }

    if (jobPosting != null && jobPosting.getJobRole() != null && request.getJobRoleId() != null) {
      if (!request.getJobRoleId().equals(jobPosting.getJobRole().getId())) {
        throw new BadRequestException("Job role ID does not match job posting job role", "JOB_ROLE_POSTING_MISMATCH");
      }
    }

    Employer employer = null;
    Long effectiveEmployerId = request.getEmployerId() != null
        ? request.getEmployerId()
        : (jobPosting != null && jobPosting.getEmployer() != null ? jobPosting.getEmployer().getId() : null);
    if (effectiveEmployerId != null) {
      employer = employerRepository.findById(effectiveEmployerId)
          .orElseThrow(() -> new ResourceNotFoundException("Employer", "employerId"));
    }

    EmployerBranch employerBranch = null;
    Long effectiveBranchId = request.getEmployerBranchId() != null
        ? request.getEmployerBranchId()
        : (jobPosting != null && jobPosting.getEmployerBranch() != null ? jobPosting.getEmployerBranch().getId() : null);
    if (effectiveBranchId != null) {
      employerBranch = employerBranchRepository.findById(effectiveBranchId)
          .orElseThrow(() -> new ResourceNotFoundException("EmployerBranch", "employerBranchId"));
      if (employer == null || !employerBranch.getEmployer().getId().equals(employer.getId())) {
        throw new BadRequestException("Employer branch does not belong to specified employer", "BRANCH_EMPLOYER_MISMATCH");
      }
    }

    JobRole jobRole = null;
    Long effectiveJobRoleId = request.getJobRoleId() != null
        ? request.getJobRoleId()
        : (jobPosting != null && jobPosting.getJobRole() != null ? jobPosting.getJobRole().getId() : null);
    if (effectiveJobRoleId != null) {
      jobRole = jobRoleRepository.findById(effectiveJobRoleId)
          .orElseThrow(() -> new ResourceNotFoundException("JobRole", "jobRoleId"));
    }

    Long effectiveEngagementTypeId = request.getEngagementTypeId() != null
        ? request.getEngagementTypeId()
        : (jobPosting != null ? jobPosting.getEngagementType().getId() : null);
    if (effectiveEngagementTypeId == null) {
      throw new BadRequestException("Engagement type ID is required", "ENGAGEMENT_TYPE_REQUIRED");
    }
    RefEngagementType engagementType = refEngagementTypeRepository.findById(effectiveEngagementTypeId)
        .orElseThrow(() -> new ResourceNotFoundException("RefEngagementType", "engagementTypeId"));

    if (Boolean.TRUE.equals(engagementType.getRequiresEmployer()) && employer == null) {
      throw new BadRequestException("Employer is required for this engagement type", "EMPLOYER_REQUIRED_FOR_ENGAGEMENT");
    }

    RefPlacementSource placementSource = refPlacementSourceRepository.findById(request.getPlacementSourceId())
        .orElseThrow(() -> new ResourceNotFoundException("RefPlacementSource", "placementSourceId"));

    RefPlacementStatus placementStatus = refPlacementStatusRepository.findById(request.getPlacementStatusId())
        .orElseThrow(() -> new ResourceNotFoundException("RefPlacementStatus", "placementStatusId"));

    RefJoiningStatus joiningStatus = refJoiningStatusRepository.findById(request.getJoiningStatusId())
        .orElseThrow(() -> new ResourceNotFoundException("RefJoiningStatus", "joiningStatusId"));

    RefSalaryFrequency salaryFrequency = null;
    if (request.getSalaryFrequencyId() != null) {
      salaryFrequency = refSalaryFrequencyRepository.findById(request.getSalaryFrequencyId())
          .orElseThrow(() -> new ResourceNotFoundException("RefSalaryFrequency", "salaryFrequencyId"));
    }

    RefNonSelectionReason nonSelectionReason = null;
    if (request.getNonSelectionReasonId() != null) {
      nonSelectionReason = refNonSelectionReasonRepository.findById(request.getNonSelectionReasonId())
          .orElseThrow(() -> new ResourceNotFoundException("RefNonSelectionReason", "nonSelectionReasonId"));
    }

    RefRecordVerificationStatus recordVerificationStatus = refRecordVerificationStatusRepository
        .findById(request.getRecordVerificationStatusId())
        .orElseThrow(() -> new ResourceNotFoundException("RefRecordVerificationStatus", "recordVerificationStatusId"));

    validatePlacementSalaries(request.getOfferedSalary(), request.getJoiningSalary());
    validatePlacementDates(request.getOfferDate(), request.getActualJoiningDate());

    PlacementRecord entity = placementRecordMapper.toEntity(
        request, authoritativeTrainee, enrollment, authoritativeCourse, authoritativeProgram, authoritativeProvider,
        employer, employerBranch, jobPosting, jobApplication, jobRole, engagementType,
        placementSource, placementStatus, joiningStatus, salaryFrequency, nonSelectionReason, recordVerificationStatus);

    PlacementRecord saved = placementRecordRepository.save(entity);
    return placementRecordMapper.toResponse(saved);
  }

  @Override
  @Transactional
  public PlacementRecordResponse updatePlacementRecord(Long id, UpdatePlacementRecordRequest request) {
    if (id == null) {
      throw new BadRequestException("Placement record ID is required");
    }
    if (request == null) {
      throw new BadRequestException("Placement record update request cannot be null");
    }

    PlacementRecord record = placementRecordRepository.findById(id)
        .orElseThrow(() -> new ResourceNotFoundException("PlacementRecord", "id"));

    if (record.getDeletedAt() != null) {
      throw new BadRequestException("Cannot update a deleted placement record", "PLACEMENT_DELETED");
    }

    Employer effectiveEmployer = record.getEmployer();

    if (request.getEmployerBranchId() != null) {
      EmployerBranch branch = employerBranchRepository.findById(request.getEmployerBranchId())
          .orElseThrow(() -> new ResourceNotFoundException("EmployerBranch", "employerBranchId"));
      if (effectiveEmployer == null || !branch.getEmployer().getId().equals(effectiveEmployer.getId())) {
        throw new BadRequestException("Employer branch does not belong to specified employer", "BRANCH_EMPLOYER_MISMATCH");
      }
      record.setEmployerBranch(branch);
    }

    if (request.getJobRoleId() != null) {
      JobRole jobRole = jobRoleRepository.findById(request.getJobRoleId())
          .orElseThrow(() -> new ResourceNotFoundException("JobRole", "jobRoleId"));
      record.setJobRole(jobRole);
    }

    if (request.getEngagementTypeId() != null) {
      RefEngagementType engagementType = refEngagementTypeRepository.findById(request.getEngagementTypeId())
          .orElseThrow(() -> new ResourceNotFoundException("RefEngagementType", "engagementTypeId"));
      if (Boolean.TRUE.equals(engagementType.getRequiresEmployer()) && effectiveEmployer == null) {
        throw new BadRequestException("Employer is required for this engagement type", "EMPLOYER_REQUIRED_FOR_ENGAGEMENT");
      }
      record.setEngagementType(engagementType);
    }

    if (request.getPlacementSourceId() != null) {
      RefPlacementSource source = refPlacementSourceRepository.findById(request.getPlacementSourceId())
          .orElseThrow(() -> new ResourceNotFoundException("RefPlacementSource", "placementSourceId"));
      record.setPlacementSource(source);
    }

    if (request.getPlacementStatusId() != null) {
      RefPlacementStatus status = refPlacementStatusRepository.findById(request.getPlacementStatusId())
          .orElseThrow(() -> new ResourceNotFoundException("RefPlacementStatus", "placementStatusId"));
      record.setPlacementStatus(status);
    }

    if (request.getJoiningStatusId() != null) {
      RefJoiningStatus joiningStatus = refJoiningStatusRepository.findById(request.getJoiningStatusId())
          .orElseThrow(() -> new ResourceNotFoundException("RefJoiningStatus", "joiningStatusId"));
      record.setJoiningStatus(joiningStatus);
    }

    BigDecimal offeredSalary = request.getOfferedSalary() != null ? request.getOfferedSalary() : record.getOfferedSalary();
    BigDecimal joiningSalary = request.getJoiningSalary() != null ? request.getJoiningSalary() : record.getJoiningSalary();
    validatePlacementSalaries(offeredSalary, joiningSalary);
    if (request.getOfferedSalary() != null) {
      record.setOfferedSalary(request.getOfferedSalary());
    }
    if (request.getJoiningSalary() != null) {
      record.setJoiningSalary(request.getJoiningSalary());
    }

    if (request.getSalaryFrequencyId() != null) {
      RefSalaryFrequency frequency = refSalaryFrequencyRepository.findById(request.getSalaryFrequencyId())
          .orElseThrow(() -> new ResourceNotFoundException("RefSalaryFrequency", "salaryFrequencyId"));
      record.setSalaryFrequency(frequency);
    }

    if (request.getCurrencyCode() != null && !request.getCurrencyCode().isBlank()) {
      record.setCurrencyCode(request.getCurrencyCode().trim());
    }

    LocalDate offerDate = request.getOfferDate() != null ? request.getOfferDate() : record.getOfferDate();
    LocalDate actualJoiningDate = request.getActualJoiningDate() != null ? request.getActualJoiningDate() : record.getActualJoiningDate();
    validatePlacementDates(offerDate, actualJoiningDate);
    if (request.getOfferDate() != null) {
      record.setOfferDate(request.getOfferDate());
    }
    if (request.getExpectedJoiningDate() != null) {
      record.setExpectedJoiningDate(request.getExpectedJoiningDate());
    }
    if (request.getActualJoiningDate() != null) {
      record.setActualJoiningDate(request.getActualJoiningDate());
    }

    if (request.getNonSelectionReasonId() != null) {
      RefNonSelectionReason reason = refNonSelectionReasonRepository.findById(request.getNonSelectionReasonId())
          .orElseThrow(() -> new ResourceNotFoundException("RefNonSelectionReason", "nonSelectionReasonId"));
      record.setNonSelectionReason(reason);
    }

    if (request.getOutcomeRemarks() != null) {
      record.setOutcomeRemarks(request.getOutcomeRemarks());
    }
    if (request.getWorkStateId() != null) {
      record.setWorkStateId(request.getWorkStateId());
    }
    if (request.getWorkDistrictId() != null) {
      record.setWorkDistrictId(request.getWorkDistrictId());
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

    PlacementRecord saved = placementRecordRepository.save(record);
    return placementRecordMapper.toResponse(saved);
  }

  @Override
  @Transactional
  public void deletePlacementRecord(Long id) {
    if (id == null) {
      throw new BadRequestException("Placement record ID is required");
    }
    PlacementRecord record = placementRecordRepository.findById(id)
        .orElseThrow(() -> new ResourceNotFoundException("PlacementRecord", "id"));
    if (record.getDeletedAt() == null) {
      record.setDeletedAt(LocalDateTime.now());
      placementRecordRepository.save(record);
    }
  }

  private void validatePlacementSalaries(BigDecimal offeredSalary, BigDecimal joiningSalary) {
    if (offeredSalary != null && offeredSalary.compareTo(BigDecimal.ZERO) < 0) {
      throw new BadRequestException("Offered salary cannot be negative", "INVALID_SALARY");
    }
    if (joiningSalary != null && joiningSalary.compareTo(BigDecimal.ZERO) < 0) {
      throw new BadRequestException("Joining salary cannot be negative", "INVALID_SALARY");
    }
  }

  private void validatePlacementDates(LocalDate offerDate, LocalDate actualJoiningDate) {
    if (actualJoiningDate != null && offerDate != null && actualJoiningDate.isBefore(offerDate)) {
      throw new BadRequestException("Actual joining date cannot be before offer date", "INVALID_JOINING_DATE");
    }
  }
}
