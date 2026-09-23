package in.gov.sih.sih26135.service.impl;

import in.gov.sih.sih26135.dto.request.CreateCertificationRequest;
import in.gov.sih.sih26135.dto.request.RecordAssessmentAndIssueCertificateRequest;
import in.gov.sih.sih26135.dto.request.UpdateTrainingEnrollmentRequest;
import in.gov.sih.sih26135.dto.response.AssessmentResultResponse;
import in.gov.sih.sih26135.dto.response.CertificationResponse;
import in.gov.sih.sih26135.dto.response.RecordAssessmentAndIssueCertificateResponse;
import in.gov.sih.sih26135.dto.response.TraineeAssessmentResponse;
import in.gov.sih.sih26135.dto.response.TraineeTrainingOutcomeSummaryResponse;
import in.gov.sih.sih26135.dto.response.TrainingEnrollmentResponse;
import in.gov.sih.sih26135.exception.BadRequestException;
import in.gov.sih.sih26135.service.AssessmentResultService;
import in.gov.sih.sih26135.service.CertificationService;
import in.gov.sih.sih26135.service.TraineeAssessmentService;
import in.gov.sih.sih26135.service.TrainingEnrollmentService;
import in.gov.sih.sih26135.service.TrainingOutcomeWorkflowService;
import java.time.LocalDate;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class TrainingOutcomeWorkflowServiceImpl implements TrainingOutcomeWorkflowService {

  private final TrainingEnrollmentService trainingEnrollmentService;
  private final TraineeAssessmentService traineeAssessmentService;
  private final AssessmentResultService assessmentResultService;
  private final CertificationService certificationService;

  public TrainingOutcomeWorkflowServiceImpl(
      TrainingEnrollmentService trainingEnrollmentService,
      TraineeAssessmentService traineeAssessmentService,
      AssessmentResultService assessmentResultService,
      CertificationService certificationService) {
    this.trainingEnrollmentService = trainingEnrollmentService;
    this.traineeAssessmentService = traineeAssessmentService;
    this.assessmentResultService = assessmentResultService;
    this.certificationService = certificationService;
  }

  @Override
  @Transactional
  public RecordAssessmentAndIssueCertificateResponse recordAssessmentResultAndIssueCertificate(
      RecordAssessmentAndIssueCertificateRequest request) {
    if (request == null) {
      throw new BadRequestException("Request body cannot be null", "REQUEST_BODY_NULL");
    }
    if (request.getAssessmentResultRequest() == null) {
      throw new BadRequestException("Assessment result request is required", "ASSESSMENT_RESULT_REQUEST_REQUIRED");
    }

    AssessmentResultResponse result = assessmentResultService.createAssessmentResult(
        request.getAssessmentResultRequest());

    CertificationResponse certification = null;
    CreateCertificationRequest certRequest = request.getCertificationRequest();
    if (certRequest != null) {
      if (!Boolean.TRUE.equals(result.getIsPassFlag())) {
        throw new BadRequestException(
            "Cannot issue certificate for non-passing assessment result",
            "ASSESSMENT_NOT_PASSED"
        );
      }
      if (certRequest.getTraineeId() != null && !certRequest.getTraineeId().equals(result.getTraineeId())) {
        throw new BadRequestException(
            "Certification trainee ID does not match assessment result trainee",
            "CERTIFICATION_TRAINEE_MISMATCH"
        );
      }
      certRequest.setAssessmentResultId(result.getId());
      certRequest.setTraineeId(result.getTraineeId());
      certification = certificationService.createCertification(certRequest);
    }

    TrainingEnrollmentResponse updatedEnrollment = null;
    if (request.getCompletionEnrollmentStatusId() != null) {
      Long targetEnrollmentId = request.getEnrollmentId();
      if (targetEnrollmentId == null && certRequest != null) {
        targetEnrollmentId = certRequest.getEnrollmentId();
      }
      if (targetEnrollmentId == null) {
        throw new BadRequestException(
            "Enrollment ID is required to update enrollment status",
            "ENROLLMENT_ID_REQUIRED"
        );
      }

      TrainingEnrollmentResponse enrollment = trainingEnrollmentService.getById(targetEnrollmentId);
      if (!enrollment.getTraineeId().equals(result.getTraineeId())) {
        throw new BadRequestException(
            "Enrollment trainee does not match assessment result trainee",
            "ENROLLMENT_TRAINEE_MISMATCH"
        );
      }

      UpdateTrainingEnrollmentRequest updateReq = new UpdateTrainingEnrollmentRequest();
      updateReq.setStartDate(enrollment.getStartDate());
      updateReq.setExpectedCompletionDate(enrollment.getExpectedCompletionDate());
      updateReq.setActualCompletionDate(
          enrollment.getActualCompletionDate() != null
              ? enrollment.getActualCompletionDate()
              : LocalDate.now()
      );
      updateReq.setEnrollmentStatusId(request.getCompletionEnrollmentStatusId());
      updateReq.setDropoutReasonId(enrollment.getDropoutReasonId());
      updateReq.setDropoutRemarks(enrollment.getDropoutRemarks());

      updatedEnrollment = trainingEnrollmentService.updateEnrollment(targetEnrollmentId, updateReq);
    }

    return new RecordAssessmentAndIssueCertificateResponse(result, certification, updatedEnrollment);
  }

  @Override
  public TraineeTrainingOutcomeSummaryResponse getTraineeTrainingOutcomeSummary(
      Long traineeId, Long enrollmentId) {
    if (traineeId == null) {
      throw new BadRequestException("Trainee ID is required", "TRAINEE_ID_REQUIRED");
    }
    if (enrollmentId == null) {
      throw new BadRequestException("Enrollment ID is required", "ENROLLMENT_ID_REQUIRED");
    }

    TrainingEnrollmentResponse enrollment = trainingEnrollmentService.getById(enrollmentId);
    if (!enrollment.getTraineeId().equals(traineeId)) {
      throw new BadRequestException(
          "Enrollment does not belong to the specified trainee",
          "TRAINEE_ENROLLMENT_MISMATCH"
      );
    }

    List<TraineeAssessmentResponse> assessments = traineeAssessmentService.getByEnrollmentId(enrollmentId);
    List<AssessmentResultResponse> results = assessmentResultService.getByTraineeId(traineeId);
    List<CertificationResponse> certs = certificationService.getByEnrollmentId(enrollmentId);

    return new TraineeTrainingOutcomeSummaryResponse(enrollment, assessments, results, certs);
  }
}
