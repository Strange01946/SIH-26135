package in.gov.sih.sih26135.mapper;

import in.gov.sih.sih26135.dto.request.CreateEmploymentRecordRequest;
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
import org.springframework.stereotype.Component;

@Component
public class EmploymentRecordMapper {

  public EmploymentRecordResponse toResponse(EmploymentRecord entity) {
    if (entity == null) {
      return null;
    }

    Long traineeId = null;
    String traineeReg = null;
    String traineeFirst = null;
    String traineeLast = null;
    if (entity.getTrainee() != null) {
      traineeId = entity.getTrainee().getId();
      traineeReg = entity.getTrainee().getRegistrationNumber();
      traineeFirst = entity.getTrainee().getFirstName();
      traineeLast = entity.getTrainee().getLastName();
    }

    Long enrollmentId = null;
    String enrollmentNumber = null;
    if (entity.getEnrollment() != null) {
      enrollmentId = entity.getEnrollment().getId();
      enrollmentNumber = entity.getEnrollment().getEnrollmentNumber();
    }

    Long placementId = null;
    String placementNumber = null;
    if (entity.getPlacementRecord() != null) {
      placementId = entity.getPlacementRecord().getId();
      placementNumber = entity.getPlacementRecord().getPlacementNumber();
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

    Long spellStatusId = null;
    String spellStatusCode = null;
    String spellStatusName = null;
    Boolean isSpellActive = null;
    if (entity.getEmploymentSpellStatus() != null) {
      spellStatusId = entity.getEmploymentSpellStatus().getId();
      spellStatusCode = entity.getEmploymentSpellStatus().getStatusCode();
      spellStatusName = entity.getEmploymentSpellStatus().getStatusName();
      isSpellActive = entity.getEmploymentSpellStatus().getIsActiveFlag();
    }

    Long salaryFrequencyId = null;
    String salaryFrequencyCode = null;
    if (entity.getSalaryFrequency() != null) {
      salaryFrequencyId = entity.getSalaryFrequency().getId();
      salaryFrequencyCode = entity.getSalaryFrequency().getFrequencyCode();
    }

    Long infoSourceId = null;
    String infoSourceCode = null;
    String infoSourceName = null;
    if (entity.getEmploymentInfoSource() != null) {
      infoSourceId = entity.getEmploymentInfoSource().getId();
      infoSourceCode = entity.getEmploymentInfoSource().getSourceCode();
      infoSourceName = entity.getEmploymentInfoSource().getSourceName();
    }

    Long recordVerificationStatusId = null;
    String recordVerificationStatusCode = null;
    if (entity.getRecordVerificationStatus() != null) {
      recordVerificationStatusId = entity.getRecordVerificationStatus().getId();
      recordVerificationStatusCode = entity.getRecordVerificationStatus().getStatusCode();
    }

    Long exitReasonId = null;
    String exitReasonCode = null;
    String exitReasonName = null;
    if (entity.getEmploymentExitReason() != null) {
      exitReasonId = entity.getEmploymentExitReason().getId();
      exitReasonCode = entity.getEmploymentExitReason().getReasonCode();
      exitReasonName = entity.getEmploymentExitReason().getReasonName();
    }

    return new EmploymentRecordResponse(
        entity.getId(),
        entity.getEmploymentNumber(),
        traineeId,
        traineeReg,
        traineeFirst,
        traineeLast,
        enrollmentId,
        enrollmentNumber,
        placementId,
        placementNumber,
        employerId,
        employerName,
        branchId,
        branchName,
        jobRoleId,
        jobRoleName,
        engagementTypeId,
        engagementTypeCode,
        spellStatusId,
        spellStatusCode,
        spellStatusName,
        isSpellActive,
        entity.getStartDate(),
        entity.getEndDate(),
        entity.getIsCurrent(),
        entity.getStartingSalary(),
        salaryFrequencyId,
        salaryFrequencyCode,
        entity.getCurrencyCode(),
        entity.getWorkLocationId(),
        entity.getWorkStateId(),
        entity.getWorkDistrictId(),
        infoSourceId,
        infoSourceCode,
        infoSourceName,
        recordVerificationStatusId,
        recordVerificationStatusCode,
        entity.getVerifiedAt(),
        entity.getVerifiedByUserId(),
        exitReasonId,
        exitReasonCode,
        exitReasonName,
        entity.getExitRemarks(),
        entity.getCreatedAt(),
        entity.getUpdatedAt(),
        entity.getDeletedAt()
    );
  }

  public EmploymentRecord toEntity(
      CreateEmploymentRecordRequest request,
      Trainee trainee,
      TrainingEnrollment enrollment,
      PlacementRecord placementRecord,
      Employer employer,
      EmployerBranch employerBranch,
      JobRole jobRole,
      RefEngagementType engagementType,
      RefEmploymentSpellStatus employmentSpellStatus,
      RefSalaryFrequency salaryFrequency,
      RefEmploymentInfoSource employmentInfoSource,
      RefRecordVerificationStatus recordVerificationStatus,
      RefEmploymentExitReason employmentExitReason) {
    if (request == null) {
      return null;
    }

    EmploymentRecord entity = new EmploymentRecord();
    entity.setEmploymentNumber(request.getEmploymentNumber());
    entity.setTrainee(trainee);
    entity.setEnrollment(enrollment);
    entity.setPlacementRecord(placementRecord);
    entity.setEmployer(employer);
    entity.setEmployerBranch(employerBranch);
    entity.setJobRole(jobRole);
    entity.setEngagementType(engagementType);
    entity.setEmploymentSpellStatus(employmentSpellStatus);
    entity.setStartDate(request.getStartDate());
    entity.setEndDate(request.getEndDate());
    entity.setIsCurrent(request.getIsCurrent() != null ? request.getIsCurrent() : (request.getEndDate() == null));
    entity.setStartingSalary(request.getStartingSalary());
    entity.setSalaryFrequency(salaryFrequency);
    entity.setCurrencyCode(request.getCurrencyCode() != null ? request.getCurrencyCode() : "INR");
    entity.setWorkLocationId(request.getWorkLocationId());
    entity.setWorkStateId(request.getWorkStateId());
    entity.setWorkDistrictId(request.getWorkDistrictId());
    entity.setEmploymentInfoSource(employmentInfoSource);
    entity.setRecordVerificationStatus(recordVerificationStatus);
    entity.setVerifiedAt(request.getVerifiedAt());
    entity.setVerifiedByUserId(request.getVerifiedByUserId());
    entity.setEmploymentExitReason(employmentExitReason);
    entity.setExitRemarks(request.getExitRemarks());
    return entity;
  }
}
