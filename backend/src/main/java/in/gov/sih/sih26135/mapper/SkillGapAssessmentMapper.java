package in.gov.sih.sih26135.mapper;

import in.gov.sih.sih26135.dto.response.SkillGapAssessmentResponse;
import in.gov.sih.sih26135.entity.SkillGapAssessment;
import org.springframework.stereotype.Component;

@Component
public class SkillGapAssessmentMapper {

  public SkillGapAssessmentResponse toResponse(SkillGapAssessment entity) {
    if (entity == null) {
      return null;
    }

    Long traineeId = entity.getTrainee() != null ? entity.getTrainee().getId() : null;
    String traineeRegistrationNumber = entity.getTrainee() != null ? entity.getTrainee().getRegistrationNumber() : null;
    String traineeFirstName = entity.getTrainee() != null ? entity.getTrainee().getFirstName() : null;
    String traineeLastName = entity.getTrainee() != null ? entity.getTrainee().getLastName() : null;

    Long enrollmentId = entity.getEnrollment() != null ? entity.getEnrollment().getId() : null;
    String enrollmentNumber = entity.getEnrollment() != null ? entity.getEnrollment().getEnrollmentNumber() : null;

    Long courseId = entity.getCourse() != null ? entity.getCourse().getId() : null;
    String courseCode = entity.getCourse() != null ? entity.getCourse().getCourseCode() : null;
    String courseName = entity.getCourse() != null ? entity.getCourse().getCourseName() : null;

    Long batchId = entity.getBatch() != null ? entity.getBatch().getId() : null;
    String batchCode = entity.getBatch() != null ? entity.getBatch().getBatchCode() : null;

    Long jobRoleId = entity.getJobRole() != null ? entity.getJobRole().getId() : null;
    String jobRoleCode = entity.getJobRole() != null ? entity.getJobRole().getJobRoleCode() : null;
    String jobRoleName = entity.getJobRole() != null ? entity.getJobRole().getJobRoleName() : null;

    Long jobPostingId = entity.getJobPosting() != null ? entity.getJobPosting().getId() : null;
    String jobPostingCode = entity.getJobPosting() != null ? entity.getJobPosting().getPostingCode() : null;
    String jobPostingTitle = entity.getJobPosting() != null ? entity.getJobPosting().getPostingTitle() : null;

    Long employmentRecordId = entity.getEmploymentRecord() != null ? entity.getEmploymentRecord().getId() : null;
    String employmentRecordNumber = entity.getEmploymentRecord() != null ? entity.getEmploymentRecord().getEmploymentNumber() : null;

    Long placementRecordId = entity.getPlacementRecord() != null ? entity.getPlacementRecord().getId() : null;
    String placementRecordNumber = entity.getPlacementRecord() != null ? entity.getPlacementRecord().getPlacementNumber() : null;

    Long traineeAssessmentId = entity.getTraineeAssessment() != null ? entity.getTraineeAssessment().getId() : null;
    Long assessmentResultId = entity.getAssessmentResult() != null ? entity.getAssessmentResult().getId() : null;

    Long certificationId = entity.getCertification() != null ? entity.getCertification().getId() : null;
    String certificateNumber = entity.getCertification() != null ? entity.getCertification().getCertificateNumber() : null;

    Long surveyResponseId = entity.getSurveyResponse() != null ? entity.getSurveyResponse().getId() : null;
    Long followupTaskId = entity.getFollowupTask() != null ? entity.getFollowupTask().getId() : null;

    Long employmentVerificationId = entity.getEmploymentVerification() != null ? entity.getEmploymentVerification().getId() : null;
    String employmentVerificationNumber = entity.getEmploymentVerification() != null ? entity.getEmploymentVerification().getVerificationNumber() : null;

    Long skillGapSourceId = entity.getSkillGapSource() != null ? entity.getSkillGapSource().getId() : null;
    String skillGapSourceCode = entity.getSkillGapSource() != null ? entity.getSkillGapSource().getSourceCode() : null;
    String skillGapSourceName = entity.getSkillGapSource() != null ? entity.getSkillGapSource().getSourceName() : null;

    Long skillGapAssessmentStatusId = entity.getSkillGapAssessmentStatus() != null ? entity.getSkillGapAssessmentStatus().getId() : null;
    String skillGapAssessmentStatusCode = entity.getSkillGapAssessmentStatus() != null ? entity.getSkillGapAssessmentStatus().getStatusCode() : null;
    String skillGapAssessmentStatusName = entity.getSkillGapAssessmentStatus() != null ? entity.getSkillGapAssessmentStatus().getStatusName() : null;

    return new SkillGapAssessmentResponse(
        entity.getId(),
        entity.getAssessmentNumber(),
        traineeId,
        traineeRegistrationNumber,
        traineeFirstName,
        traineeLastName,
        enrollmentId,
        enrollmentNumber,
        courseId,
        courseCode,
        courseName,
        batchId,
        batchCode,
        jobRoleId,
        jobRoleCode,
        jobRoleName,
        jobPostingId,
        jobPostingCode,
        jobPostingTitle,
        employmentRecordId,
        employmentRecordNumber,
        placementRecordId,
        placementRecordNumber,
        traineeAssessmentId,
        assessmentResultId,
        certificationId,
        certificateNumber,
        surveyResponseId,
        followupTaskId,
        employmentVerificationId,
        employmentVerificationNumber,
        skillGapSourceId,
        skillGapSourceCode,
        skillGapSourceName,
        skillGapAssessmentStatusId,
        skillGapAssessmentStatusCode,
        skillGapAssessmentStatusName,
        entity.getAssessedOn(),
        entity.getAssessedByUserId(),
        entity.getNotes(),
        entity.getCreatedAt(),
        entity.getUpdatedAt()
    );
  }
}
