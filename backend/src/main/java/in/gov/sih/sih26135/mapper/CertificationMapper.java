package in.gov.sih.sih26135.mapper;

import in.gov.sih.sih26135.dto.request.CreateCertificationRequest;
import in.gov.sih.sih26135.dto.response.CertificationResponse;
import in.gov.sih.sih26135.entity.AssessmentResult;
import in.gov.sih.sih26135.entity.Certification;
import in.gov.sih.sih26135.entity.Course;
import in.gov.sih.sih26135.entity.Program;
import in.gov.sih.sih26135.entity.RefCertificateStatus;
import in.gov.sih.sih26135.entity.RefCertificateVerificationStatus;
import in.gov.sih.sih26135.entity.Trainee;
import in.gov.sih.sih26135.entity.TrainingEnrollment;
import org.springframework.stereotype.Component;

@Component
public class CertificationMapper {

  public CertificationResponse toResponse(Certification entity) {
    if (entity == null) {
      return null;
    }

    Long traineeId = null;
    String traineeReg = null;
    String traineeFullName = null;
    if (entity.getTrainee() != null) {
      traineeId = entity.getTrainee().getId();
      traineeReg = entity.getTrainee().getRegistrationNumber();
      String first = entity.getTrainee().getFirstName() != null ? entity.getTrainee().getFirstName() : "";
      String last = entity.getTrainee().getLastName() != null ? entity.getTrainee().getLastName() : "";
      traineeFullName = (first + " " + last).trim();
      if (traineeFullName.isEmpty()) {
        traineeFullName = null;
      }
    }

    Long enrollmentId = null;
    String enrollmentNumber = null;
    if (entity.getTrainingEnrollment() != null) {
      enrollmentId = entity.getTrainingEnrollment().getId();
      enrollmentNumber = entity.getTrainingEnrollment().getEnrollmentNumber();
    }

    Long courseId = null;
    String courseCode = null;
    String courseName = null;
    if (entity.getCourse() != null) {
      courseId = entity.getCourse().getId();
      courseCode = entity.getCourse().getCourseCode();
      courseName = entity.getCourse().getCourseName();
    }

    Long programId = null;
    String programCode = null;
    String programName = null;
    if (entity.getProgram() != null) {
      programId = entity.getProgram().getId();
      programCode = entity.getProgram().getProgramCode();
      programName = entity.getProgram().getProgramName();
    }

    Long assessmentResultId = null;
    if (entity.getAssessmentResult() != null) {
      assessmentResultId = entity.getAssessmentResult().getId();
    }

    Long statusId = null;
    String statusCode = null;
    String statusName = null;
    if (entity.getCertificateStatus() != null) {
      statusId = entity.getCertificateStatus().getId();
      statusCode = entity.getCertificateStatus().getStatusCode();
      statusName = entity.getCertificateStatus().getStatusName();
    }

    Long verificationStatusId = null;
    String verificationStatusCode = null;
    String verificationStatusName = null;
    if (entity.getCertificateVerificationStatus() != null) {
      verificationStatusId = entity.getCertificateVerificationStatus().getId();
      verificationStatusCode = entity.getCertificateVerificationStatus().getStatusCode();
      verificationStatusName = entity.getCertificateVerificationStatus().getStatusName();
    }

    return new CertificationResponse(
        entity.getId(),
        entity.getCertificateNumber(),
        traineeId,
        traineeReg,
        traineeFullName,
        enrollmentId,
        enrollmentNumber,
        courseId,
        courseCode,
        courseName,
        programId,
        programCode,
        programName,
        assessmentResultId,
        entity.getIssuingBody(),
        entity.getIssueDate(),
        entity.getExpiryDate(),
        statusId,
        statusCode,
        statusName,
        verificationStatusId,
        verificationStatusCode,
        verificationStatusName,
        entity.getVerifiedAt(),
        entity.getVerifiedByUserId(),
        entity.getCreatedAt(),
        entity.getUpdatedAt(),
        entity.getDeletedAt()
    );
  }

  public Certification toEntity(
      CreateCertificationRequest request,
      Trainee trainee,
      TrainingEnrollment enrollment,
      Course course,
      Program program,
      AssessmentResult result,
      RefCertificateStatus status,
      RefCertificateVerificationStatus verificationStatus) {
    if (request == null) {
      return null;
    }

    Certification entity = new Certification();
    entity.setCertificateNumber(request.getCertificateNumber());
    entity.setTrainee(trainee);
    entity.setTrainingEnrollment(enrollment);
    entity.setCourse(course);
    entity.setProgram(program);
    entity.setAssessmentResult(result);
    entity.setIssuingBody(request.getIssuingBody());
    entity.setIssueDate(request.getIssueDate());
    entity.setExpiryDate(request.getExpiryDate());
    entity.setCertificateStatus(status);
    entity.setCertificateVerificationStatus(verificationStatus);
    entity.setVerifiedAt(request.getVerifiedAt());
    entity.setVerifiedByUserId(request.getVerifiedByUserId());
    return entity;
  }
}
