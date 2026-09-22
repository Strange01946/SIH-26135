package in.gov.sih.sih26135.service;

import in.gov.sih.sih26135.dto.request.CreateCertificationRequest;
import in.gov.sih.sih26135.dto.request.UpdateCertificationRequest;
import in.gov.sih.sih26135.dto.response.CertificationResponse;
import java.time.LocalDate;
import java.util.List;

public interface CertificationService {

  CertificationResponse getById(Long id);

  CertificationResponse getByCertificateNumber(String certificateNumber);

  List<CertificationResponse> getAllCertifications();

  List<CertificationResponse> getByTraineeId(Long traineeId);

  List<CertificationResponse> getByEnrollmentId(Long enrollmentId);

  List<CertificationResponse> getByCourseId(Long courseId);

  List<CertificationResponse> getByProgramId(Long programId);

  List<CertificationResponse> getByAssessmentResultId(Long assessmentResultId);

  List<CertificationResponse> getByCertificateStatusId(Long statusId);

  List<CertificationResponse> getByCertificateVerificationStatusId(Long verificationStatusId);

  List<CertificationResponse> getByIssueDate(LocalDate issueDate);

  CertificationResponse createCertification(CreateCertificationRequest request);

  CertificationResponse updateCertification(Long id, UpdateCertificationRequest request);

  void deleteCertification(Long id);
}
