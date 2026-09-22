package in.gov.sih.sih26135.service.impl;

import in.gov.sih.sih26135.dto.request.CreateJobPostingRequest;
import in.gov.sih.sih26135.dto.request.UpdateJobPostingRequest;
import in.gov.sih.sih26135.dto.response.JobPostingResponse;
import in.gov.sih.sih26135.entity.Employer;
import in.gov.sih.sih26135.entity.EmployerBranch;
import in.gov.sih.sih26135.entity.JobPosting;
import in.gov.sih.sih26135.entity.JobRole;
import in.gov.sih.sih26135.entity.RefEngagementType;
import in.gov.sih.sih26135.entity.RefJobPostingStatus;
import in.gov.sih.sih26135.entity.RefQualificationLevel;
import in.gov.sih.sih26135.entity.RefSalaryFrequency;
import in.gov.sih.sih26135.exception.BadRequestException;
import in.gov.sih.sih26135.exception.ConflictException;
import in.gov.sih.sih26135.exception.ResourceNotFoundException;
import in.gov.sih.sih26135.mapper.JobPostingMapper;
import in.gov.sih.sih26135.repository.EmployerBranchRepository;
import in.gov.sih.sih26135.repository.EmployerRepository;
import in.gov.sih.sih26135.repository.JobPostingRepository;
import in.gov.sih.sih26135.repository.JobRoleRepository;
import in.gov.sih.sih26135.repository.RefEngagementTypeRepository;
import in.gov.sih.sih26135.repository.RefJobPostingStatusRepository;
import in.gov.sih.sih26135.repository.RefQualificationLevelRepository;
import in.gov.sih.sih26135.repository.RefSalaryFrequencyRepository;
import in.gov.sih.sih26135.service.JobPostingService;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class JobPostingServiceImpl implements JobPostingService {

  private final JobPostingRepository jobPostingRepository;
  private final EmployerRepository employerRepository;
  private final EmployerBranchRepository employerBranchRepository;
  private final JobRoleRepository jobRoleRepository;
  private final RefEngagementTypeRepository refEngagementTypeRepository;
  private final RefQualificationLevelRepository refQualificationLevelRepository;
  private final RefSalaryFrequencyRepository refSalaryFrequencyRepository;
  private final RefJobPostingStatusRepository refJobPostingStatusRepository;
  private final JobPostingMapper jobPostingMapper;

  public JobPostingServiceImpl(
      JobPostingRepository jobPostingRepository,
      EmployerRepository employerRepository,
      EmployerBranchRepository employerBranchRepository,
      JobRoleRepository jobRoleRepository,
      RefEngagementTypeRepository refEngagementTypeRepository,
      RefQualificationLevelRepository refQualificationLevelRepository,
      RefSalaryFrequencyRepository refSalaryFrequencyRepository,
      RefJobPostingStatusRepository refJobPostingStatusRepository,
      JobPostingMapper jobPostingMapper) {
    this.jobPostingRepository = jobPostingRepository;
    this.employerRepository = employerRepository;
    this.employerBranchRepository = employerBranchRepository;
    this.jobRoleRepository = jobRoleRepository;
    this.refEngagementTypeRepository = refEngagementTypeRepository;
    this.refQualificationLevelRepository = refQualificationLevelRepository;
    this.refSalaryFrequencyRepository = refSalaryFrequencyRepository;
    this.refJobPostingStatusRepository = refJobPostingStatusRepository;
    this.jobPostingMapper = jobPostingMapper;
  }

  @Override
  public JobPostingResponse getJobPostingById(Long id) {
    if (id == null) {
      throw new BadRequestException("Job posting ID is required");
    }
    JobPosting posting = jobPostingRepository.findById(id)
        .orElseThrow(() -> new ResourceNotFoundException("JobPosting", "id"));
    return jobPostingMapper.toResponse(posting);
  }

  @Override
  public JobPostingResponse getJobPostingByCode(String postingCode) {
    if (postingCode == null || postingCode.isBlank()) {
      throw new BadRequestException("Posting code is required");
    }
    JobPosting posting = jobPostingRepository.findByPostingCode(postingCode.trim())
        .orElseThrow(() -> new ResourceNotFoundException("JobPosting", "postingCode"));
    return jobPostingMapper.toResponse(posting);
  }

  @Override
  public List<JobPostingResponse> getAllJobPostings(boolean includeDeleted) {
    List<JobPosting> postings = includeDeleted
        ? jobPostingRepository.findAll()
        : jobPostingRepository.findByDeletedAtIsNull();
    return postings.stream()
        .map(jobPostingMapper::toResponse)
        .toList();
  }

  @Override
  public List<JobPostingResponse> getJobPostingsByEmployer(Long employerId) {
    if (employerId == null) {
      throw new BadRequestException("Employer ID is required");
    }
    return jobPostingRepository.findByEmployerId(employerId).stream()
        .map(jobPostingMapper::toResponse)
        .toList();
  }

  @Override
  public List<JobPostingResponse> getJobPostingsByJobRole(Long jobRoleId) {
    if (jobRoleId == null) {
      throw new BadRequestException("Job role ID is required");
    }
    return jobPostingRepository.findByJobRoleId(jobRoleId).stream()
        .map(jobPostingMapper::toResponse)
        .toList();
  }

  @Override
  public List<JobPostingResponse> getJobPostingsByDistrict(Long districtId) {
    if (districtId == null) {
      throw new BadRequestException("District ID is required");
    }
    return jobPostingRepository.findByDistrictId(districtId).stream()
        .map(jobPostingMapper::toResponse)
        .toList();
  }

  @Override
  @Transactional
  public JobPostingResponse createJobPosting(CreateJobPostingRequest request) {
    if (request == null) {
      throw new BadRequestException("Job posting creation request cannot be null");
    }
    if (request.getPostingCode() == null || request.getPostingCode().isBlank()) {
      throw new BadRequestException("Posting code is required");
    }
    if (request.getPostingTitle() == null || request.getPostingTitle().isBlank()) {
      throw new BadRequestException("Posting title is required");
    }
    if (request.getJobRoleId() == null) {
      throw new BadRequestException("Job role ID is required");
    }
    if (request.getEngagementTypeId() == null) {
      throw new BadRequestException("Engagement type ID is required");
    }
    if (request.getStateId() == null) {
      throw new BadRequestException("State ID is required");
    }
    if (request.getDistrictId() == null) {
      throw new BadRequestException("District ID is required");
    }
    if (request.getPostedDate() == null) {
      throw new BadRequestException("Posted date is required");
    }
    if (request.getJobPostingStatusId() == null) {
      throw new BadRequestException("Job posting status ID is required");
    }

    String postingCode = request.getPostingCode().trim();
    if (jobPostingRepository.existsByPostingCode(postingCode)) {
      throw new ConflictException("Job posting code already exists", "POSTING_CODE_ALREADY_EXISTS");
    }

    if (request.getVacancies() != null && request.getVacancies() < 1) {
      throw new BadRequestException("Vacancies must be at least 1", "INVALID_VACANCIES");
    }

    validateSalaryRange(request.getMinSalary(), request.getMaxSalary());

    if (request.getClosingDate() != null && request.getClosingDate().isBefore(request.getPostedDate())) {
      throw new BadRequestException("Closing date cannot be before posted date", "INVALID_CLOSING_DATE");
    }

    JobRole jobRole = jobRoleRepository.findById(request.getJobRoleId())
        .orElseThrow(() -> new ResourceNotFoundException("JobRole", "jobRoleId"));

    RefEngagementType engagementType = refEngagementTypeRepository.findById(request.getEngagementTypeId())
        .orElseThrow(() -> new ResourceNotFoundException("RefEngagementType", "engagementTypeId"));

    if (Boolean.TRUE.equals(engagementType.getRequiresEmployer()) && request.getEmployerId() == null) {
      throw new BadRequestException("Employer is required for this engagement type", "EMPLOYER_REQUIRED_FOR_ENGAGEMENT");
    }

    Employer employer = null;
    if (request.getEmployerId() != null) {
      employer = employerRepository.findById(request.getEmployerId())
          .orElseThrow(() -> new ResourceNotFoundException("Employer", "employerId"));
    }

    EmployerBranch employerBranch = null;
    if (request.getEmployerBranchId() != null) {
      employerBranch = employerBranchRepository.findById(request.getEmployerBranchId())
          .orElseThrow(() -> new ResourceNotFoundException("EmployerBranch", "employerBranchId"));
      if (employer == null || !employerBranch.getEmployer().getId().equals(employer.getId())) {
        throw new BadRequestException("Employer branch does not belong to specified employer", "BRANCH_EMPLOYER_MISMATCH");
      }
    }

    RefQualificationLevel qualificationLevel = null;
    if (request.getQualificationLevelId() != null) {
      qualificationLevel = refQualificationLevelRepository.findById(request.getQualificationLevelId())
          .orElseThrow(() -> new ResourceNotFoundException("RefQualificationLevel", "qualificationLevelId"));
    }

    RefSalaryFrequency salaryFrequency = null;
    if (request.getSalaryFrequencyId() != null) {
      salaryFrequency = refSalaryFrequencyRepository.findById(request.getSalaryFrequencyId())
          .orElseThrow(() -> new ResourceNotFoundException("RefSalaryFrequency", "salaryFrequencyId"));
    }

    RefJobPostingStatus jobPostingStatus = refJobPostingStatusRepository.findById(request.getJobPostingStatusId())
        .orElseThrow(() -> new ResourceNotFoundException("RefJobPostingStatus", "jobPostingStatusId"));

    JobPosting entity = jobPostingMapper.toEntity(
        request, employer, employerBranch, jobRole, engagementType,
        qualificationLevel, salaryFrequency, jobPostingStatus);
    JobPosting saved = jobPostingRepository.save(entity);
    return jobPostingMapper.toResponse(saved);
  }

  @Override
  @Transactional
  public JobPostingResponse updateJobPosting(Long id, UpdateJobPostingRequest request) {
    if (id == null) {
      throw new BadRequestException("Job posting ID is required");
    }
    if (request == null) {
      throw new BadRequestException("Job posting update request cannot be null");
    }

    JobPosting posting = jobPostingRepository.findById(id)
        .orElseThrow(() -> new ResourceNotFoundException("JobPosting", "id"));

    if (posting.getDeletedAt() != null) {
      throw new BadRequestException("Cannot update a deleted job posting", "JOB_POSTING_DELETED");
    }

    if (request.getPostingTitle() != null && !request.getPostingTitle().isBlank()) {
      posting.setPostingTitle(request.getPostingTitle().trim());
    }
    if (request.getDescription() != null) {
      posting.setDescription(request.getDescription());
    }

    Employer effectiveEmployer = posting.getEmployer();

    RefEngagementType effectiveEngagementType = posting.getEngagementType();
    if (request.getEngagementTypeId() != null) {
      effectiveEngagementType = refEngagementTypeRepository.findById(request.getEngagementTypeId())
          .orElseThrow(() -> new ResourceNotFoundException("RefEngagementType", "engagementTypeId"));
      posting.setEngagementType(effectiveEngagementType);
    }

    if (Boolean.TRUE.equals(effectiveEngagementType.getRequiresEmployer()) && effectiveEmployer == null) {
      throw new BadRequestException("Employer is required for this engagement type", "EMPLOYER_REQUIRED_FOR_ENGAGEMENT");
    }

    if (request.getEmployerBranchId() != null) {
      EmployerBranch branch = employerBranchRepository.findById(request.getEmployerBranchId())
          .orElseThrow(() -> new ResourceNotFoundException("EmployerBranch", "employerBranchId"));
      if (effectiveEmployer == null || !branch.getEmployer().getId().equals(effectiveEmployer.getId())) {
        throw new BadRequestException("Employer branch does not belong to specified employer", "BRANCH_EMPLOYER_MISMATCH");
      }
      posting.setEmployerBranch(branch);
    }

    if (request.getJobRoleId() != null) {
      JobRole jobRole = jobRoleRepository.findById(request.getJobRoleId())
          .orElseThrow(() -> new ResourceNotFoundException("JobRole", "jobRoleId"));
      posting.setJobRole(jobRole);
    }

    if (request.getQualificationLevelId() != null) {
      RefQualificationLevel qualLevel = refQualificationLevelRepository.findById(request.getQualificationLevelId())
          .orElseThrow(() -> new ResourceNotFoundException("RefQualificationLevel", "qualificationLevelId"));
      posting.setQualificationLevel(qualLevel);
    }

    if (request.getVacancies() != null) {
      if (request.getVacancies() < 1) {
        throw new BadRequestException("Vacancies must be at least 1", "INVALID_VACANCIES");
      }
      posting.setVacancies(request.getVacancies());
    }

    BigDecimal minSalary = request.getMinSalary() != null ? request.getMinSalary() : posting.getMinSalary();
    BigDecimal maxSalary = request.getMaxSalary() != null ? request.getMaxSalary() : posting.getMaxSalary();
    if (request.getMinSalary() != null || request.getMaxSalary() != null) {
      validateSalaryRange(minSalary, maxSalary);
      if (request.getMinSalary() != null) {
        posting.setMinSalary(request.getMinSalary());
      }
      if (request.getMaxSalary() != null) {
        posting.setMaxSalary(request.getMaxSalary());
      }
    }

    if (request.getSalaryFrequencyId() != null) {
      RefSalaryFrequency frequency = refSalaryFrequencyRepository.findById(request.getSalaryFrequencyId())
          .orElseThrow(() -> new ResourceNotFoundException("RefSalaryFrequency", "salaryFrequencyId"));
      posting.setSalaryFrequency(frequency);
    }

    if (request.getCurrencyCode() != null && !request.getCurrencyCode().isBlank()) {
      posting.setCurrencyCode(request.getCurrencyCode().trim());
    }
    if (request.getStateId() != null) {
      posting.setStateId(request.getStateId());
    }
    if (request.getDistrictId() != null) {
      posting.setDistrictId(request.getDistrictId());
    }
    if (request.getLocationId() != null) {
      posting.setLocationId(request.getLocationId());
    }

    LocalDate postedDate = request.getPostedDate() != null ? request.getPostedDate() : posting.getPostedDate();
    LocalDate closingDate = request.getClosingDate() != null ? request.getClosingDate() : posting.getClosingDate();
    if (closingDate != null && postedDate != null && closingDate.isBefore(postedDate)) {
      throw new BadRequestException("Closing date cannot be before posted date", "INVALID_CLOSING_DATE");
    }
    if (request.getPostedDate() != null) {
      posting.setPostedDate(request.getPostedDate());
    }
    if (request.getClosingDate() != null) {
      posting.setClosingDate(request.getClosingDate());
    }

    if (request.getJobPostingStatusId() != null) {
      RefJobPostingStatus status = refJobPostingStatusRepository.findById(request.getJobPostingStatusId())
          .orElseThrow(() -> new ResourceNotFoundException("RefJobPostingStatus", "jobPostingStatusId"));
      posting.setJobPostingStatus(status);
    }

    JobPosting saved = jobPostingRepository.save(posting);
    return jobPostingMapper.toResponse(saved);
  }

  @Override
  @Transactional
  public void deleteJobPosting(Long id) {
    if (id == null) {
      throw new BadRequestException("Job posting ID is required");
    }
    JobPosting posting = jobPostingRepository.findById(id)
        .orElseThrow(() -> new ResourceNotFoundException("JobPosting", "id"));
    if (posting.getDeletedAt() == null) {
      posting.setDeletedAt(LocalDateTime.now());
      jobPostingRepository.save(posting);
    }
  }

  private void validateSalaryRange(BigDecimal minSalary, BigDecimal maxSalary) {
    if (minSalary != null && minSalary.compareTo(BigDecimal.ZERO) < 0) {
      throw new BadRequestException("Minimum salary cannot be negative", "INVALID_SALARY");
    }
    if (maxSalary != null && maxSalary.compareTo(BigDecimal.ZERO) < 0) {
      throw new BadRequestException("Maximum salary cannot be negative", "INVALID_SALARY");
    }
    if (minSalary != null && maxSalary != null && maxSalary.compareTo(minSalary) < 0) {
      throw new BadRequestException("Maximum salary cannot be less than minimum salary", "INVALID_SALARY_RANGE");
    }
  }
}
