package in.gov.sih.sih26135.mapper;

import in.gov.sih.sih26135.dto.request.CreateJobApplicationRequest;
import in.gov.sih.sih26135.dto.response.JobApplicationResponse;
import in.gov.sih.sih26135.entity.JobApplication;
import in.gov.sih.sih26135.entity.JobPosting;
import in.gov.sih.sih26135.entity.RefApplicationStatus;
import in.gov.sih.sih26135.entity.RefNonSelectionReason;
import in.gov.sih.sih26135.entity.Trainee;
import in.gov.sih.sih26135.entity.TrainingEnrollment;
import org.springframework.stereotype.Component;

@Component
public class JobApplicationMapper {

  public JobApplicationResponse toResponse(JobApplication entity) {
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

    Long jobPostingId = null;
    String postingCode = null;
    String postingTitle = null;
    if (entity.getJobPosting() != null) {
      jobPostingId = entity.getJobPosting().getId();
      postingCode = entity.getJobPosting().getPostingCode();
      postingTitle = entity.getJobPosting().getPostingTitle();
    }

    Long statusId = null;
    String statusCode = null;
    if (entity.getApplicationStatus() != null) {
      statusId = entity.getApplicationStatus().getId();
      statusCode = entity.getApplicationStatus().getStatusCode();
    }

    Long reasonId = null;
    String reasonCode = null;
    if (entity.getNonSelectionReason() != null) {
      reasonId = entity.getNonSelectionReason().getId();
      reasonCode = entity.getNonSelectionReason().getReasonCode();
    }

    return new JobApplicationResponse(
        entity.getId(),
        traineeId,
        traineeReg,
        traineeFirst,
        traineeLast,
        enrollmentId,
        enrollmentNumber,
        jobPostingId,
        postingCode,
        postingTitle,
        statusId,
        statusCode,
        entity.getAppliedDate(),
        entity.getReferredByUserId(),
        reasonId,
        reasonCode,
        entity.getRemarks(),
        entity.getCreatedAt(),
        entity.getUpdatedAt()
    );
  }

  public JobApplication toEntity(
      CreateJobApplicationRequest request,
      Trainee trainee,
      TrainingEnrollment enrollment,
      JobPosting jobPosting,
      RefApplicationStatus applicationStatus,
      RefNonSelectionReason nonSelectionReason) {
    if (request == null) {
      return null;
    }

    JobApplication entity = new JobApplication();
    entity.setTrainee(trainee);
    entity.setEnrollment(enrollment);
    entity.setJobPosting(jobPosting);
    entity.setApplicationStatus(applicationStatus);
    entity.setAppliedDate(request.getAppliedDate());
    entity.setReferredByUserId(request.getReferredByUserId());
    entity.setNonSelectionReason(nonSelectionReason);
    entity.setRemarks(request.getRemarks());
    return entity;
  }
}
