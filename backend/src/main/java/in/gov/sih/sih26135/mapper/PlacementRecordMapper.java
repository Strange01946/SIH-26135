package in.gov.sih.sih26135.mapper;

import in.gov.sih.sih26135.dto.request.CreatePlacementRecordRequest;
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
import org.springframework.stereotype.Component;

@Component
public class PlacementRecordMapper {

  public PlacementRecordResponse toResponse(PlacementRecord entity) {
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

    Long courseId = null;
    String courseName = null;
    if (entity.getCourse() != null) {
      courseId = entity.getCourse().getId();
      courseName = entity.getCourse().getCourseName();
    }

    Long programId = null;
    String programName = null;
    if (entity.getProgram() != null) {
      programId = entity.getProgram().getId();
      programName = entity.getProgram().getProgramName();
    }

    Long trainingProviderId = null;
    String trainingProviderName = null;
    if (entity.getTrainingProvider() != null) {
      trainingProviderId = entity.getTrainingProvider().getId();
      trainingProviderName = entity.getTrainingProvider().getProviderName();
    }

    Long employerId = null;
    String employerName = null;
    if (entity.getEmployer() != null) {
      employerId = entity.getEmployer().getId();
      employerName = entity.getEmployer().getEmployerName();
    }

    Long employerBranchId = null;
    String employerBranchName = null;
    if (entity.getEmployerBranch() != null) {
      employerBranchId = entity.getEmployerBranch().getId();
      employerBranchName = entity.getEmployerBranch().getBranchName();
    }

    Long jobPostingId = null;
    String jobPostingCode = null;
    if (entity.getJobPosting() != null) {
      jobPostingId = entity.getJobPosting().getId();
      jobPostingCode = entity.getJobPosting().getPostingCode();
    }

    Long jobApplicationId = null;
    if (entity.getJobApplication() != null) {
      jobApplicationId = entity.getJobApplication().getId();
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

    Long placementSourceId = null;
    String placementSourceCode = null;
    if (entity.getPlacementSource() != null) {
      placementSourceId = entity.getPlacementSource().getId();
      placementSourceCode = entity.getPlacementSource().getSourceCode();
    }

    Long placementStatusId = null;
    String placementStatusCode = null;
    if (entity.getPlacementStatus() != null) {
      placementStatusId = entity.getPlacementStatus().getId();
      placementStatusCode = entity.getPlacementStatus().getStatusCode();
    }

    Long joiningStatusId = null;
    String joiningStatusCode = null;
    if (entity.getJoiningStatus() != null) {
      joiningStatusId = entity.getJoiningStatus().getId();
      joiningStatusCode = entity.getJoiningStatus().getStatusCode();
    }

    Long salaryFrequencyId = null;
    String salaryFrequencyCode = null;
    if (entity.getSalaryFrequency() != null) {
      salaryFrequencyId = entity.getSalaryFrequency().getId();
      salaryFrequencyCode = entity.getSalaryFrequency().getFrequencyCode();
    }

    Long nonSelectionReasonId = null;
    String nonSelectionReasonCode = null;
    if (entity.getNonSelectionReason() != null) {
      nonSelectionReasonId = entity.getNonSelectionReason().getId();
      nonSelectionReasonCode = entity.getNonSelectionReason().getReasonCode();
    }

    Long recordVerificationStatusId = null;
    String recordVerificationStatusCode = null;
    if (entity.getRecordVerificationStatus() != null) {
      recordVerificationStatusId = entity.getRecordVerificationStatus().getId();
      recordVerificationStatusCode = entity.getRecordVerificationStatus().getStatusCode();
    }

    return new PlacementRecordResponse(
        entity.getId(),
        entity.getPlacementNumber(),
        traineeId,
        traineeReg,
        traineeFirst,
        traineeLast,
        enrollmentId,
        enrollmentNumber,
        courseId,
        courseName,
        programId,
        programName,
        trainingProviderId,
        trainingProviderName,
        employerId,
        employerName,
        employerBranchId,
        employerBranchName,
        jobPostingId,
        jobPostingCode,
        jobApplicationId,
        jobRoleId,
        jobRoleName,
        engagementTypeId,
        engagementTypeCode,
        placementSourceId,
        placementSourceCode,
        placementStatusId,
        placementStatusCode,
        joiningStatusId,
        joiningStatusCode,
        entity.getOfferedSalary(),
        entity.getJoiningSalary(),
        salaryFrequencyId,
        salaryFrequencyCode,
        entity.getCurrencyCode(),
        entity.getOfferDate(),
        entity.getExpectedJoiningDate(),
        entity.getActualJoiningDate(),
        nonSelectionReasonId,
        nonSelectionReasonCode,
        entity.getOutcomeRemarks(),
        entity.getWorkStateId(),
        entity.getWorkDistrictId(),
        recordVerificationStatusId,
        recordVerificationStatusCode,
        entity.getVerifiedAt(),
        entity.getVerifiedByUserId(),
        entity.getCreatedAt(),
        entity.getUpdatedAt(),
        entity.getDeletedAt()
    );
  }

  public PlacementRecord toEntity(
      CreatePlacementRecordRequest request,
      Trainee trainee,
      TrainingEnrollment enrollment,
      Course course,
      Program program,
      TrainingProvider trainingProvider,
      Employer employer,
      EmployerBranch employerBranch,
      JobPosting jobPosting,
      JobApplication jobApplication,
      JobRole jobRole,
      RefEngagementType engagementType,
      RefPlacementSource placementSource,
      RefPlacementStatus placementStatus,
      RefJoiningStatus joiningStatus,
      RefSalaryFrequency salaryFrequency,
      RefNonSelectionReason nonSelectionReason,
      RefRecordVerificationStatus recordVerificationStatus) {
    if (request == null) {
      return null;
    }

    PlacementRecord entity = new PlacementRecord();
    entity.setPlacementNumber(request.getPlacementNumber());
    entity.setTrainee(trainee);
    entity.setEnrollment(enrollment);
    entity.setCourse(course);
    entity.setProgram(program);
    entity.setTrainingProvider(trainingProvider);
    entity.setEmployer(employer);
    entity.setEmployerBranch(employerBranch);
    entity.setJobPosting(jobPosting);
    entity.setJobApplication(jobApplication);
    entity.setJobRole(jobRole);
    entity.setEngagementType(engagementType);
    entity.setPlacementSource(placementSource);
    entity.setPlacementStatus(placementStatus);
    entity.setJoiningStatus(joiningStatus);
    entity.setOfferedSalary(request.getOfferedSalary());
    entity.setJoiningSalary(request.getJoiningSalary());
    entity.setSalaryFrequency(salaryFrequency);
    entity.setCurrencyCode(request.getCurrencyCode() != null ? request.getCurrencyCode() : "INR");
    entity.setOfferDate(request.getOfferDate());
    entity.setExpectedJoiningDate(request.getExpectedJoiningDate());
    entity.setActualJoiningDate(request.getActualJoiningDate());
    entity.setNonSelectionReason(nonSelectionReason);
    entity.setOutcomeRemarks(request.getOutcomeRemarks());
    entity.setWorkStateId(request.getWorkStateId());
    entity.setWorkDistrictId(request.getWorkDistrictId());
    entity.setRecordVerificationStatus(recordVerificationStatus);
    entity.setVerifiedAt(request.getVerifiedAt());
    entity.setVerifiedByUserId(request.getVerifiedByUserId());
    return entity;
  }
}
