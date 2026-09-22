package in.gov.sih.sih26135.mapper;

import in.gov.sih.sih26135.dto.request.CreateJobPostingRequest;
import in.gov.sih.sih26135.dto.response.JobPostingResponse;
import in.gov.sih.sih26135.entity.Employer;
import in.gov.sih.sih26135.entity.EmployerBranch;
import in.gov.sih.sih26135.entity.JobPosting;
import in.gov.sih.sih26135.entity.JobRole;
import in.gov.sih.sih26135.entity.RefEngagementType;
import in.gov.sih.sih26135.entity.RefJobPostingStatus;
import in.gov.sih.sih26135.entity.RefQualificationLevel;
import in.gov.sih.sih26135.entity.RefSalaryFrequency;
import org.springframework.stereotype.Component;

@Component
public class JobPostingMapper {

  public JobPostingResponse toResponse(JobPosting entity) {
    if (entity == null) {
      return null;
    }

    Long employerId = null;
    String employerName = null;
    if (entity.getEmployer() != null) {
      employerId = entity.getEmployer().getId();
      employerName = entity.getEmployer().getEmployerName();
    }

    Long branchId = null;
    String branchName = null;
    if (entity.getEmployerBranch() != null) {
      branchId = entity.getEmployerBranch().getId();
      branchName = entity.getEmployerBranch().getBranchName();
    }

    Long jobRoleId = null;
    String jobRoleName = null;
    if (entity.getJobRole() != null) {
      jobRoleId = entity.getJobRole().getId();
      jobRoleName = entity.getJobRole().getJobRoleName();
    }

    Long engagementTypeId = null;
    String engagementTypeCode = null;
    if (entity.getEngagementType() != null) {
      engagementTypeId = entity.getEngagementType().getId();
      engagementTypeCode = entity.getEngagementType().getTypeCode();
    }

    Long qualificationLevelId = null;
    String qualificationLevelName = null;
    if (entity.getQualificationLevel() != null) {
      qualificationLevelId = entity.getQualificationLevel().getId();
      qualificationLevelName = entity.getQualificationLevel().getLevelName();
    }

    Long salaryFrequencyId = null;
    String salaryFrequencyCode = null;
    if (entity.getSalaryFrequency() != null) {
      salaryFrequencyId = entity.getSalaryFrequency().getId();
      salaryFrequencyCode = entity.getSalaryFrequency().getFrequencyCode();
    }

    Long jobPostingStatusId = null;
    String jobPostingStatusCode = null;
    if (entity.getJobPostingStatus() != null) {
      jobPostingStatusId = entity.getJobPostingStatus().getId();
      jobPostingStatusCode = entity.getJobPostingStatus().getStatusCode();
    }

    return new JobPostingResponse(
        entity.getId(),
        entity.getPostingCode(),
        entity.getPostingTitle(),
        entity.getDescription(),
        employerId,
        employerName,
        branchId,
        branchName,
        jobRoleId,
        jobRoleName,
        engagementTypeId,
        engagementTypeCode,
        qualificationLevelId,
        qualificationLevelName,
        entity.getVacancies(),
        entity.getMinSalary(),
        entity.getMaxSalary(),
        salaryFrequencyId,
        salaryFrequencyCode,
        entity.getCurrencyCode(),
        entity.getStateId(),
        entity.getDistrictId(),
        entity.getLocationId(),
        entity.getPostedDate(),
        entity.getClosingDate(),
        jobPostingStatusId,
        jobPostingStatusCode,
        entity.getCreatedByUserId(),
        entity.getCreatedAt(),
        entity.getUpdatedAt(),
        entity.getDeletedAt()
    );
  }

  public JobPosting toEntity(
      CreateJobPostingRequest request,
      Employer employer,
      EmployerBranch employerBranch,
      JobRole jobRole,
      RefEngagementType engagementType,
      RefQualificationLevel qualificationLevel,
      RefSalaryFrequency salaryFrequency,
      RefJobPostingStatus jobPostingStatus) {
    if (request == null) {
      return null;
    }

    JobPosting entity = new JobPosting();
    entity.setPostingCode(request.getPostingCode());
    entity.setPostingTitle(request.getPostingTitle());
    entity.setDescription(request.getDescription());
    entity.setEmployer(employer);
    entity.setEmployerBranch(employerBranch);
    entity.setJobRole(jobRole);
    entity.setEngagementType(engagementType);
    entity.setQualificationLevel(qualificationLevel);
    entity.setVacancies(request.getVacancies() != null ? request.getVacancies() : 1);
    entity.setMinSalary(request.getMinSalary());
    entity.setMaxSalary(request.getMaxSalary());
    entity.setSalaryFrequency(salaryFrequency);
    entity.setCurrencyCode(request.getCurrencyCode() != null ? request.getCurrencyCode() : "INR");
    entity.setStateId(request.getStateId());
    entity.setDistrictId(request.getDistrictId());
    entity.setLocationId(request.getLocationId());
    entity.setPostedDate(request.getPostedDate());
    entity.setClosingDate(request.getClosingDate());
    entity.setJobPostingStatus(jobPostingStatus);
    entity.setCreatedByUserId(request.getCreatedByUserId());
    return entity;
  }
}
