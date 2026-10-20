package in.gov.sih.sih26135.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import in.gov.sih.sih26135.dto.request.ChangeBatchStatusRequest;
import in.gov.sih.sih26135.dto.request.CreateCertificationRequest;
import in.gov.sih.sih26135.dto.request.CreateJobApplicationRequest;
import in.gov.sih.sih26135.dto.request.CreateTrainingEnrollmentRequest;
import in.gov.sih.sih26135.dto.request.UpdateAssessmentRequest;
import in.gov.sih.sih26135.dto.request.UpdateCertificationRequest;
import in.gov.sih.sih26135.dto.request.UpdateTrainingBatchRequest;
import in.gov.sih.sih26135.dto.request.UpdateTrainingEnrollmentRequest;
import in.gov.sih.sih26135.entity.Assessment;
import in.gov.sih.sih26135.entity.Certification;
import in.gov.sih.sih26135.entity.JobPosting;
import in.gov.sih.sih26135.entity.RefCertificateStatus;
import in.gov.sih.sih26135.entity.RefCertificateVerificationStatus;
import in.gov.sih.sih26135.entity.Trainee;
import in.gov.sih.sih26135.entity.TrainingBatch;
import in.gov.sih.sih26135.entity.TrainingEnrollment;
import in.gov.sih.sih26135.exception.BadRequestException;
import in.gov.sih.sih26135.mapper.AssessmentMapper;
import in.gov.sih.sih26135.mapper.CertificationMapper;
import in.gov.sih.sih26135.mapper.JobApplicationMapper;
import in.gov.sih.sih26135.mapper.TrainingBatchMapper;
import in.gov.sih.sih26135.mapper.TrainingEnrollmentMapper;
import in.gov.sih.sih26135.repository.AssessmentRepository;
import in.gov.sih.sih26135.repository.AssessmentResultRepository;
import in.gov.sih.sih26135.repository.CertificationRepository;
import in.gov.sih.sih26135.repository.CourseProgramRepository;
import in.gov.sih.sih26135.repository.CourseRepository;
import in.gov.sih.sih26135.repository.JobApplicationRepository;
import in.gov.sih.sih26135.repository.JobPostingRepository;
import in.gov.sih.sih26135.repository.PlacementRecordRepository;
import in.gov.sih.sih26135.repository.ProgramRepository;
import in.gov.sih.sih26135.repository.RefApplicationStatusRepository;
import in.gov.sih.sih26135.repository.RefAssessmentTypeRepository;
import in.gov.sih.sih26135.repository.RefBatchStatusRepository;
import in.gov.sih.sih26135.repository.RefCertificateStatusRepository;
import in.gov.sih.sih26135.repository.RefCertificateVerificationStatusRepository;
import in.gov.sih.sih26135.repository.RefEnrollmentStatusRepository;
import in.gov.sih.sih26135.repository.RefNonSelectionReasonRepository;
import in.gov.sih.sih26135.repository.RefTrainingDropoutReasonRepository;
import in.gov.sih.sih26135.repository.TraineeRepository;
import in.gov.sih.sih26135.repository.TrainingBatchRepository;
import in.gov.sih.sih26135.repository.TrainingCenterRepository;
import in.gov.sih.sih26135.repository.TrainingEnrollmentRepository;
import in.gov.sih.sih26135.repository.TrainingProviderRepository;
import in.gov.sih.sih26135.repository.UserRepository;
import in.gov.sih.sih26135.service.impl.AssessmentServiceImpl;
import in.gov.sih.sih26135.service.impl.CertificationServiceImpl;
import in.gov.sih.sih26135.service.impl.JobApplicationServiceImpl;
import in.gov.sih.sih26135.service.impl.TrainingBatchServiceImpl;
import in.gov.sih.sih26135.service.impl.TrainingEnrollmentServiceImpl;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class StateTransitionAndBusinessInvariantsTest {

  @Nested
  @DisplayName("TrainingEnrollment Invariant Hardening Tests")
  class TrainingEnrollmentTests {

    @Mock private TrainingEnrollmentRepository trainingEnrollmentRepository;
    @Mock private TraineeRepository traineeRepository;
    @Mock private TrainingBatchRepository trainingBatchRepository;
    @Mock private RefEnrollmentStatusRepository refEnrollmentStatusRepository;
    @Mock private RefTrainingDropoutReasonRepository refTrainingDropoutReasonRepository;
    @Mock private TrainingEnrollmentMapper trainingEnrollmentMapper;

    private TrainingEnrollmentServiceImpl trainingEnrollmentService;

    @BeforeEach
    void setUp() {
      trainingEnrollmentService = new TrainingEnrollmentServiceImpl(
          trainingEnrollmentRepository,
          traineeRepository,
          trainingBatchRepository,
          refEnrollmentStatusRepository,
          refTrainingDropoutReasonRepository,
          trainingEnrollmentMapper
      );
    }

    @Test
    @DisplayName("Updating a soft-deleted enrollment throws BadRequestException with ENROLLMENT_DELETED")
    void testUpdateEnrollment_whenSoftDeleted_throwsBadRequestExceptionEnrollmentDeleted() {
      TrainingEnrollment deletedEnrollment = new TrainingEnrollment();
      deletedEnrollment.setId(10L);
      deletedEnrollment.setEnrollmentDate(LocalDate.of(2026, 1, 1));
      deletedEnrollment.setDeletedAt(LocalDateTime.now().minusDays(1));

      when(trainingEnrollmentRepository.findById(10L)).thenReturn(Optional.of(deletedEnrollment));

      UpdateTrainingEnrollmentRequest request = new UpdateTrainingEnrollmentRequest();
      request.setStartDate(LocalDate.of(2026, 1, 5));

      assertThatThrownBy(() -> trainingEnrollmentService.updateEnrollment(10L, request))
          .isInstanceOf(BadRequestException.class)
          .satisfies(ex -> {
            BadRequestException bre = (BadRequestException) ex;
            assertThat(bre.getMessage()).isEqualTo("Cannot update a deleted training enrollment");
            assertThat(bre.getErrorCode()).isEqualTo("ENROLLMENT_DELETED");
          });

      verify(trainingEnrollmentRepository, never()).save(any());
    }

    @Test
    @DisplayName("Deleting an already soft-deleted enrollment is idempotent and does not overwrite deletedAt")
    void testDeleteEnrollment_whenAlreadySoftDeleted_isIdempotent() {
      LocalDateTime originalDeletedAt = LocalDateTime.now().minusDays(3);
      TrainingEnrollment deletedEnrollment = new TrainingEnrollment();
      deletedEnrollment.setId(10L);
      deletedEnrollment.setDeletedAt(originalDeletedAt);

      when(trainingEnrollmentRepository.findById(10L)).thenReturn(Optional.of(deletedEnrollment));

      trainingEnrollmentService.deleteEnrollment(10L);

      verify(trainingEnrollmentRepository, never()).save(any());
      assertThat(deletedEnrollment.getDeletedAt()).isEqualTo(originalDeletedAt);
    }

    @Test
    @DisplayName("Creating an enrollment for a soft-deleted trainee throws BadRequestException with TRAINEE_DELETED")
    void testCreateEnrollment_whenTraineeSoftDeleted_throwsBadRequestExceptionTraineeDeleted() {
      CreateTrainingEnrollmentRequest request = new CreateTrainingEnrollmentRequest(
          "ENR-2026-001", 100L, 200L, LocalDate.of(2026, 1, 1), 1L);

      when(trainingEnrollmentRepository.existsByEnrollmentNumber("ENR-2026-001")).thenReturn(false);
      when(trainingEnrollmentRepository.existsByTraineeIdAndTrainingBatchId(100L, 200L)).thenReturn(false);

      Trainee deletedTrainee = new Trainee();
      deletedTrainee.setId(100L);
      deletedTrainee.setDeletedAt(LocalDateTime.now().minusDays(1));
      when(traineeRepository.findById(100L)).thenReturn(Optional.of(deletedTrainee));

      assertThatThrownBy(() -> trainingEnrollmentService.createEnrollment(request))
          .isInstanceOf(BadRequestException.class)
          .satisfies(ex -> {
            BadRequestException bre = (BadRequestException) ex;
            assertThat(bre.getMessage()).isEqualTo("Cannot create training enrollment for soft-deleted trainee");
            assertThat(bre.getErrorCode()).isEqualTo("TRAINEE_DELETED");
          });

      verify(trainingEnrollmentRepository, never()).save(any());
    }

    @Test
    @DisplayName("Creating an enrollment for a soft-deleted batch throws BadRequestException with BATCH_DELETED")
    void testCreateEnrollment_whenBatchSoftDeleted_throwsBadRequestExceptionBatchDeleted() {
      CreateTrainingEnrollmentRequest request = new CreateTrainingEnrollmentRequest(
          "ENR-2026-002", 100L, 200L, LocalDate.of(2026, 1, 1), 1L);

      when(trainingEnrollmentRepository.existsByEnrollmentNumber("ENR-2026-002")).thenReturn(false);
      when(trainingEnrollmentRepository.existsByTraineeIdAndTrainingBatchId(100L, 200L)).thenReturn(false);

      Trainee activeTrainee = new Trainee();
      activeTrainee.setId(100L);
      when(traineeRepository.findById(100L)).thenReturn(Optional.of(activeTrainee));

      TrainingBatch deletedBatch = new TrainingBatch();
      deletedBatch.setId(200L);
      deletedBatch.setDeletedAt(LocalDateTime.now().minusDays(1));
      when(trainingBatchRepository.findById(200L)).thenReturn(Optional.of(deletedBatch));

      assertThatThrownBy(() -> trainingEnrollmentService.createEnrollment(request))
          .isInstanceOf(BadRequestException.class)
          .satisfies(ex -> {
            BadRequestException bre = (BadRequestException) ex;
            assertThat(bre.getMessage()).isEqualTo("Cannot create training enrollment for soft-deleted training batch");
            assertThat(bre.getErrorCode()).isEqualTo("BATCH_DELETED");
          });

      verify(trainingEnrollmentRepository, never()).save(any());
    }
  }

  @Nested
  @DisplayName("Certification Invariant Hardening Tests")
  class CertificationTests {

    @Mock private CertificationRepository certificationRepository;
    @Mock private TraineeRepository traineeRepository;
    @Mock private TrainingEnrollmentRepository trainingEnrollmentRepository;
    @Mock private CourseRepository courseRepository;
    @Mock private ProgramRepository programRepository;
    @Mock private AssessmentResultRepository assessmentResultRepository;
    @Mock private RefCertificateStatusRepository refCertificateStatusRepository;
    @Mock private RefCertificateVerificationStatusRepository refCertificateVerificationStatusRepository;
    @Mock private UserRepository userRepository;
    @Mock private CertificationMapper certificationMapper;

    private CertificationServiceImpl certificationService;

    @BeforeEach
    void setUp() {
      certificationService = new CertificationServiceImpl(
          certificationRepository,
          traineeRepository,
          trainingEnrollmentRepository,
          courseRepository,
          programRepository,
          assessmentResultRepository,
          refCertificateStatusRepository,
          refCertificateVerificationStatusRepository,
          userRepository,
          certificationMapper
      );
    }

    @Test
    @DisplayName("Updating a soft-deleted certification throws BadRequestException with CERTIFICATION_DELETED")
    void testUpdateCertification_whenSoftDeleted_throwsBadRequestExceptionCertificationDeleted() {
      Certification deletedCert = new Certification();
      deletedCert.setId(50L);
      deletedCert.setIssueDate(LocalDate.of(2026, 1, 1));
      deletedCert.setDeletedAt(LocalDateTime.now().minusDays(2));

      when(certificationRepository.findById(50L)).thenReturn(Optional.of(deletedCert));

      UpdateCertificationRequest request = new UpdateCertificationRequest();
      request.setIssuingBody("Updated Board");

      assertThatThrownBy(() -> certificationService.updateCertification(50L, request))
          .isInstanceOf(BadRequestException.class)
          .satisfies(ex -> {
            BadRequestException bre = (BadRequestException) ex;
            assertThat(bre.getMessage()).isEqualTo("Cannot update a deleted certification");
            assertThat(bre.getErrorCode()).isEqualTo("CERTIFICATION_DELETED");
          });

      verify(certificationRepository, never()).save(any());
    }

    @Test
    @DisplayName("Updating verification status to VERIFIED on a REVOKED certificate throws BadRequestException with INVALID_CERTIFICATE_STATUS")
    void testUpdateCertification_whenVerifyingRevokedCertificate_throwsBadRequestExceptionInvalidCertificateStatus() {
      RefCertificateStatus revokedStatus = new RefCertificateStatus("REVOKED", "Revoked", 3);
      revokedStatus.setId(3L);

      Certification activeCert = new Certification();
      activeCert.setId(50L);
      activeCert.setIssueDate(LocalDate.of(2026, 1, 1));
      activeCert.setCertificateStatus(revokedStatus);

      RefCertificateVerificationStatus verifiedStatus = new RefCertificateVerificationStatus("VERIFIED", "Verified", 3);
      verifiedStatus.setId(3L);

      when(certificationRepository.findById(50L)).thenReturn(Optional.of(activeCert));
      when(refCertificateVerificationStatusRepository.findById(3L)).thenReturn(Optional.of(verifiedStatus));

      UpdateCertificationRequest request = new UpdateCertificationRequest();
      request.setCertificateVerificationStatusId(3L);

      assertThatThrownBy(() -> certificationService.updateCertification(50L, request))
          .isInstanceOf(BadRequestException.class)
          .satisfies(ex -> {
            BadRequestException bre = (BadRequestException) ex;
            assertThat(bre.getMessage()).isEqualTo("Cannot verify a revoked or expired certification");
            assertThat(bre.getErrorCode()).isEqualTo("INVALID_CERTIFICATE_STATUS");
          });

      verify(certificationRepository, never()).save(any());
    }

    @Test
    @DisplayName("Updating certificate status to REVOKED when verification status is already VERIFIED throws BadRequestException")
    void testUpdateCertification_whenRevokingAlreadyVerifiedCertificate_throwsBadRequestExceptionInvalidCertificateStatus() {
      RefCertificateStatus issuedStatus = new RefCertificateStatus("ISSUED", "Issued", 1);
      issuedStatus.setId(1L);

      RefCertificateVerificationStatus verifiedStatus = new RefCertificateVerificationStatus("VERIFIED", "Verified", 3);
      verifiedStatus.setId(3L);

      Certification activeCert = new Certification();
      activeCert.setId(50L);
      activeCert.setIssueDate(LocalDate.of(2026, 1, 1));
      activeCert.setCertificateStatus(issuedStatus);
      activeCert.setCertificateVerificationStatus(verifiedStatus);

      RefCertificateStatus revokedStatus = new RefCertificateStatus("REVOKED", "Revoked", 3);
      revokedStatus.setId(3L);

      when(certificationRepository.findById(50L)).thenReturn(Optional.of(activeCert));
      when(refCertificateStatusRepository.findById(3L)).thenReturn(Optional.of(revokedStatus));

      UpdateCertificationRequest request = new UpdateCertificationRequest();
      request.setCertificateStatusId(3L);

      assertThatThrownBy(() -> certificationService.updateCertification(50L, request))
          .isInstanceOf(BadRequestException.class)
          .satisfies(ex -> {
            BadRequestException bre = (BadRequestException) ex;
            assertThat(bre.getMessage()).isEqualTo("Cannot verify a revoked or expired certification");
            assertThat(bre.getErrorCode()).isEqualTo("INVALID_CERTIFICATE_STATUS");
          });

      verify(certificationRepository, never()).save(any());
      assertThat(activeCert.getCertificateStatus().getStatusCode()).isEqualTo("ISSUED");
    }

    @Test
    @DisplayName("Updating certificate status to EXPIRED when verification status is already VERIFIED throws BadRequestException")
    void testUpdateCertification_whenExpiringAlreadyVerifiedCertificate_throwsBadRequestExceptionInvalidCertificateStatus() {
      RefCertificateStatus issuedStatus = new RefCertificateStatus("ISSUED", "Issued", 1);
      issuedStatus.setId(1L);

      RefCertificateVerificationStatus verifiedStatus = new RefCertificateVerificationStatus("VERIFIED", "Verified", 3);
      verifiedStatus.setId(3L);

      Certification activeCert = new Certification();
      activeCert.setId(50L);
      activeCert.setIssueDate(LocalDate.of(2026, 1, 1));
      activeCert.setCertificateStatus(issuedStatus);
      activeCert.setCertificateVerificationStatus(verifiedStatus);

      RefCertificateStatus expiredStatus = new RefCertificateStatus("EXPIRED", "Expired", 4);
      expiredStatus.setId(4L);

      when(certificationRepository.findById(50L)).thenReturn(Optional.of(activeCert));
      when(refCertificateStatusRepository.findById(4L)).thenReturn(Optional.of(expiredStatus));

      UpdateCertificationRequest request = new UpdateCertificationRequest();
      request.setCertificateStatusId(4L);

      assertThatThrownBy(() -> certificationService.updateCertification(50L, request))
          .isInstanceOf(BadRequestException.class)
          .satisfies(ex -> {
            BadRequestException bre = (BadRequestException) ex;
            assertThat(bre.getMessage()).isEqualTo("Cannot verify a revoked or expired certification");
            assertThat(bre.getErrorCode()).isEqualTo("INVALID_CERTIFICATE_STATUS");
          });

      verify(certificationRepository, never()).save(any());
      assertThat(activeCert.getCertificateStatus().getStatusCode()).isEqualTo("ISSUED");
    }

    @Test
    @DisplayName("Updating verification status to VERIFIED on an ISSUED certificate succeeds and persists")
    void testUpdateCertification_validStatusUpdate_succeedsAndPersistsChanges() {
      RefCertificateStatus issuedStatus = new RefCertificateStatus("ISSUED", "Issued", 1);
      issuedStatus.setId(1L);

      RefCertificateVerificationStatus pendingStatus = new RefCertificateVerificationStatus("PENDING", "Pending", 2);
      pendingStatus.setId(2L);

      Certification activeCert = new Certification();
      activeCert.setId(50L);
      activeCert.setIssueDate(LocalDate.of(2026, 1, 1));
      activeCert.setCertificateStatus(issuedStatus);
      activeCert.setCertificateVerificationStatus(pendingStatus);

      RefCertificateVerificationStatus verifiedStatus = new RefCertificateVerificationStatus("VERIFIED", "Verified", 3);
      verifiedStatus.setId(3L);

      when(certificationRepository.findById(50L)).thenReturn(Optional.of(activeCert));
      when(refCertificateVerificationStatusRepository.findById(3L)).thenReturn(Optional.of(verifiedStatus));
      when(certificationRepository.save(any(Certification.class))).thenAnswer(inv -> inv.getArgument(0));

      in.gov.sih.sih26135.dto.response.CertificationResponse mockResponse = org.mockito.Mockito.mock(in.gov.sih.sih26135.dto.response.CertificationResponse.class);
      when(certificationMapper.toResponse(any())).thenReturn(mockResponse);

      UpdateCertificationRequest request = new UpdateCertificationRequest();
      request.setCertificateVerificationStatusId(3L);

      in.gov.sih.sih26135.dto.response.CertificationResponse result = certificationService.updateCertification(50L, request);

      assertThat(result).isNotNull();
      verify(certificationRepository).save(activeCert);
      assertThat(activeCert.getCertificateVerificationStatus().getStatusCode()).isEqualTo("VERIFIED");
    }

    @Test
    @DisplayName("Revoking a verified certificate succeeds when verification status is simultaneously updated away from VERIFIED")
    void testUpdateCertification_revokingVerifiedCertificateWithSimultaneousVerificationStatusChange_succeeds() {
      RefCertificateStatus issuedStatus = new RefCertificateStatus("ISSUED", "Issued", 1);
      issuedStatus.setId(1L);

      RefCertificateVerificationStatus verifiedStatus = new RefCertificateVerificationStatus("VERIFIED", "Verified", 3);
      verifiedStatus.setId(3L);

      Certification activeCert = new Certification();
      activeCert.setId(50L);
      activeCert.setIssueDate(LocalDate.of(2026, 1, 1));
      activeCert.setCertificateStatus(issuedStatus);
      activeCert.setCertificateVerificationStatus(verifiedStatus);

      RefCertificateStatus revokedStatus = new RefCertificateStatus("REVOKED", "Revoked", 3);
      revokedStatus.setId(3L);

      RefCertificateVerificationStatus rejectedStatus = new RefCertificateVerificationStatus("REJECTED", "Rejected", 4);
      rejectedStatus.setId(4L);

      when(certificationRepository.findById(50L)).thenReturn(Optional.of(activeCert));
      when(refCertificateStatusRepository.findById(3L)).thenReturn(Optional.of(revokedStatus));
      when(refCertificateVerificationStatusRepository.findById(4L)).thenReturn(Optional.of(rejectedStatus));
      when(certificationRepository.save(any(Certification.class))).thenAnswer(inv -> inv.getArgument(0));

      in.gov.sih.sih26135.dto.response.CertificationResponse mockResponse = org.mockito.Mockito.mock(in.gov.sih.sih26135.dto.response.CertificationResponse.class);
      when(certificationMapper.toResponse(any())).thenReturn(mockResponse);

      UpdateCertificationRequest request = new UpdateCertificationRequest();
      request.setCertificateStatusId(3L);
      request.setCertificateVerificationStatusId(4L);

      in.gov.sih.sih26135.dto.response.CertificationResponse result = certificationService.updateCertification(50L, request);

      assertThat(result).isNotNull();
      verify(certificationRepository).save(activeCert);
      assertThat(activeCert.getCertificateStatus().getStatusCode()).isEqualTo("REVOKED");
      assertThat(activeCert.getCertificateVerificationStatus().getStatusCode()).isEqualTo("REJECTED");
    }

    @Test
    @DisplayName("Updating verification status to VERIFIED on an EXPIRED certificate throws BadRequestException with INVALID_CERTIFICATE_STATUS")
    void testUpdateCertification_whenVerifyingExpiredCertificate_throwsBadRequestExceptionInvalidCertificateStatus() {
      RefCertificateStatus expiredStatus = new RefCertificateStatus("EXPIRED", "Expired", 4);
      expiredStatus.setId(4L);

      Certification activeCert = new Certification();
      activeCert.setId(50L);
      activeCert.setIssueDate(LocalDate.of(2026, 1, 1));
      activeCert.setCertificateStatus(expiredStatus);

      RefCertificateVerificationStatus verifiedStatus = new RefCertificateVerificationStatus("VERIFIED", "Verified", 3);
      verifiedStatus.setId(3L);

      when(certificationRepository.findById(50L)).thenReturn(Optional.of(activeCert));
      when(refCertificateVerificationStatusRepository.findById(3L)).thenReturn(Optional.of(verifiedStatus));

      UpdateCertificationRequest request = new UpdateCertificationRequest();
      request.setCertificateVerificationStatusId(3L);

      assertThatThrownBy(() -> certificationService.updateCertification(50L, request))
          .isInstanceOf(BadRequestException.class)
          .satisfies(ex -> {
            BadRequestException bre = (BadRequestException) ex;
            assertThat(bre.getMessage()).isEqualTo("Cannot verify a revoked or expired certification");
            assertThat(bre.getErrorCode()).isEqualTo("INVALID_CERTIFICATE_STATUS");
          });

      verify(certificationRepository, never()).save(any());
    }

    @Test
    @DisplayName("Creating a certification with REVOKED status and VERIFIED verification status throws BadRequestException")
    void testCreateCertification_whenRevokedAndVerified_throwsBadRequestExceptionInvalidCertificateStatus() {
      CreateCertificationRequest request = new CreateCertificationRequest();
      request.setCertificateNumber("CERT-REVOKED-001");
      request.setIssueDate(LocalDate.of(2026, 1, 1));
      request.setCertificateStatusId(3L);
      request.setCertificateVerificationStatusId(3L);
      request.setTraineeId(100L);
      request.setEnrollmentId(200L);
      request.setCourseId(300L);
      request.setProgramId(400L);

      when(certificationRepository.existsByCertificateNumber("CERT-REVOKED-001")).thenReturn(false);

      Trainee activeTrainee = new Trainee();
      activeTrainee.setId(100L);
      when(traineeRepository.findById(100L)).thenReturn(Optional.of(activeTrainee));

      TrainingEnrollment activeEnrollment = new TrainingEnrollment();
      activeEnrollment.setId(200L);
      activeEnrollment.setTrainee(activeTrainee);
      when(trainingEnrollmentRepository.findById(200L)).thenReturn(Optional.of(activeEnrollment));

      when(courseRepository.findById(300L)).thenReturn(Optional.of(new in.gov.sih.sih26135.entity.Course()));
      when(programRepository.findById(400L)).thenReturn(Optional.of(new in.gov.sih.sih26135.entity.Program()));

      RefCertificateStatus revokedStatus = new RefCertificateStatus("REVOKED", "Revoked", 3);
      RefCertificateVerificationStatus verifiedStatus = new RefCertificateVerificationStatus("VERIFIED", "Verified", 3);

      when(refCertificateStatusRepository.findById(3L)).thenReturn(Optional.of(revokedStatus));
      when(refCertificateVerificationStatusRepository.findById(3L)).thenReturn(Optional.of(verifiedStatus));

      assertThatThrownBy(() -> certificationService.createCertification(request))
          .isInstanceOf(BadRequestException.class)
          .satisfies(ex -> {
            BadRequestException bre = (BadRequestException) ex;
            assertThat(bre.getMessage()).isEqualTo("Cannot verify a revoked or expired certification");
            assertThat(bre.getErrorCode()).isEqualTo("INVALID_CERTIFICATE_STATUS");
          });

      verify(certificationRepository, never()).save(any());
    }

    @Test
    @DisplayName("Creating a certification for a soft-deleted trainee throws BadRequestException with TRAINEE_DELETED")
    void testCreateCertification_whenTraineeSoftDeleted_throwsBadRequestExceptionTraineeDeleted() {
      CreateCertificationRequest request = new CreateCertificationRequest();
      request.setCertificateNumber("CERT-TRAINEE-DEL-001");
      request.setIssueDate(LocalDate.of(2026, 1, 1));
      request.setCertificateStatusId(1L);
      request.setCertificateVerificationStatusId(1L);
      request.setTraineeId(100L);
      request.setEnrollmentId(200L);
      request.setCourseId(300L);
      request.setProgramId(400L);

      when(certificationRepository.existsByCertificateNumber("CERT-TRAINEE-DEL-001")).thenReturn(false);

      Trainee deletedTrainee = new Trainee();
      deletedTrainee.setId(100L);
      deletedTrainee.setDeletedAt(LocalDateTime.now().minusDays(1));
      when(traineeRepository.findById(100L)).thenReturn(Optional.of(deletedTrainee));

      assertThatThrownBy(() -> certificationService.createCertification(request))
          .isInstanceOf(BadRequestException.class)
          .satisfies(ex -> {
            BadRequestException bre = (BadRequestException) ex;
            assertThat(bre.getMessage()).isEqualTo("Cannot issue certification for soft-deleted trainee");
            assertThat(bre.getErrorCode()).isEqualTo("TRAINEE_DELETED");
          });

      verify(certificationRepository, never()).save(any());
    }

    @Test
    @DisplayName("Deleting an already soft-deleted certification is idempotent and does not overwrite deletedAt")
    void testDeleteCertification_whenAlreadySoftDeleted_isIdempotent() {
      LocalDateTime originalDeletedAt = LocalDateTime.now().minusDays(2);
      Certification deletedCert = new Certification();
      deletedCert.setId(50L);
      deletedCert.setDeletedAt(originalDeletedAt);

      when(certificationRepository.findById(50L)).thenReturn(Optional.of(deletedCert));

      certificationService.deleteCertification(50L);

      verify(certificationRepository, never()).save(any());
      assertThat(deletedCert.getDeletedAt()).isEqualTo(originalDeletedAt);
    }
  }

  @Nested
  @DisplayName("TrainingBatch Invariant Hardening Tests")
  class TrainingBatchTests {

    @Mock private TrainingBatchRepository trainingBatchRepository;
    @Mock private CourseRepository courseRepository;
    @Mock private TrainingProviderRepository trainingProviderRepository;
    @Mock private TrainingCenterRepository trainingCenterRepository;
    @Mock private ProgramRepository programRepository;
    @Mock private RefBatchStatusRepository refBatchStatusRepository;
    @Mock private CourseProgramRepository courseProgramRepository;
    @Mock private TrainingBatchMapper trainingBatchMapper;

    private TrainingBatchServiceImpl trainingBatchService;

    @BeforeEach
    void setUp() {
      trainingBatchService = new TrainingBatchServiceImpl(
          trainingBatchRepository,
          courseRepository,
          trainingProviderRepository,
          trainingCenterRepository,
          programRepository,
          refBatchStatusRepository,
          courseProgramRepository,
          trainingBatchMapper
      );
    }

    @Test
    @DisplayName("Updating a soft-deleted batch throws BadRequestException with BATCH_DELETED")
    void testUpdateBatch_whenSoftDeleted_throwsBadRequestExceptionBatchDeleted() {
      TrainingBatch deletedBatch = new TrainingBatch();
      deletedBatch.setId(30L);
      deletedBatch.setStartDate(LocalDate.of(2026, 1, 1));
      deletedBatch.setEndDate(LocalDate.of(2026, 3, 1));
      deletedBatch.setDeletedAt(LocalDateTime.now().minusDays(2));

      when(trainingBatchRepository.findById(30L)).thenReturn(Optional.of(deletedBatch));

      UpdateTrainingBatchRequest request = new UpdateTrainingBatchRequest();
      request.setCapacity(25);

      assertThatThrownBy(() -> trainingBatchService.updateBatch(30L, request))
          .isInstanceOf(BadRequestException.class)
          .satisfies(ex -> {
            BadRequestException bre = (BadRequestException) ex;
            assertThat(bre.getMessage()).isEqualTo("Cannot update a deleted training batch");
            assertThat(bre.getErrorCode()).isEqualTo("BATCH_DELETED");
          });

      verify(trainingBatchRepository, never()).save(any());
    }

    @Test
    @DisplayName("Updating status of a soft-deleted batch throws BadRequestException with BATCH_DELETED")
    void testUpdateBatchStatus_whenSoftDeleted_throwsBadRequestExceptionBatchDeleted() {
      TrainingBatch deletedBatch = new TrainingBatch();
      deletedBatch.setId(30L);
      deletedBatch.setDeletedAt(LocalDateTime.now().minusDays(1));

      when(trainingBatchRepository.findById(30L)).thenReturn(Optional.of(deletedBatch));

      ChangeBatchStatusRequest request = new ChangeBatchStatusRequest(2L);

      assertThatThrownBy(() -> trainingBatchService.updateBatchStatus(30L, request))
          .isInstanceOf(BadRequestException.class)
          .satisfies(ex -> {
            BadRequestException bre = (BadRequestException) ex;
            assertThat(bre.getMessage()).isEqualTo("Cannot update status of a deleted training batch");
            assertThat(bre.getErrorCode()).isEqualTo("BATCH_DELETED");
          });

      verify(trainingBatchRepository, never()).save(any());
    }

    @Test
    @DisplayName("Deleting an already soft-deleted batch is idempotent and does not overwrite deletedAt")
    void testDeleteBatch_whenAlreadySoftDeleted_isIdempotent() {
      LocalDateTime originalDeletedAt = LocalDateTime.now().minusDays(3);
      TrainingBatch deletedBatch = new TrainingBatch();
      deletedBatch.setId(30L);
      deletedBatch.setDeletedAt(originalDeletedAt);

      when(trainingBatchRepository.findById(30L)).thenReturn(Optional.of(deletedBatch));

      trainingBatchService.deleteBatch(30L);

      verify(trainingBatchRepository, never()).save(any());
      assertThat(deletedBatch.getDeletedAt()).isEqualTo(originalDeletedAt);
    }
  }

  @Nested
  @DisplayName("Assessment Invariant Hardening Tests")
  class AssessmentTests {

    @Mock private AssessmentRepository assessmentRepository;
    @Mock private in.gov.sih.sih26135.repository.RefAssessmentTypeRepository refAssessmentTypeRepository;
    @Mock private CourseRepository courseRepository;
    @Mock private TrainingBatchRepository trainingBatchRepository;
    @Mock private ProgramRepository programRepository;
    @Mock private UserRepository userRepository;
    @Mock private AssessmentMapper assessmentMapper;

    private AssessmentServiceImpl assessmentService;

    @BeforeEach
    void setUp() {
      assessmentService = new AssessmentServiceImpl(
          assessmentRepository,
          refAssessmentTypeRepository,
          courseRepository,
          trainingBatchRepository,
          programRepository,
          userRepository,
          assessmentMapper
      );
    }

    @Test
    @DisplayName("Updating a soft-deleted assessment throws BadRequestException with ASSESSMENT_DELETED")
    void testUpdateAssessment_whenSoftDeleted_throwsBadRequestExceptionAssessmentDeleted() {
      Assessment deletedAssessment = new Assessment();
      deletedAssessment.setId(40L);
      deletedAssessment.setDeletedAt(LocalDateTime.now().minusDays(1));

      when(assessmentRepository.findById(40L)).thenReturn(Optional.of(deletedAssessment));

      UpdateAssessmentRequest request = new UpdateAssessmentRequest();
      request.setAssessmentName("Updated Assessment Name");

      assertThatThrownBy(() -> assessmentService.updateAssessment(40L, request))
          .isInstanceOf(BadRequestException.class)
          .satisfies(ex -> {
            BadRequestException bre = (BadRequestException) ex;
            assertThat(bre.getMessage()).isEqualTo("Cannot update a deleted assessment");
            assertThat(bre.getErrorCode()).isEqualTo("ASSESSMENT_DELETED");
          });

      verify(assessmentRepository, never()).save(any());
    }

    @Test
    @DisplayName("Deleting an already soft-deleted assessment is idempotent and does not overwrite deletedAt")
    void testDeleteAssessment_whenAlreadySoftDeleted_isIdempotent() {
      LocalDateTime originalDeletedAt = LocalDateTime.now().minusDays(5);
      Assessment deletedAssessment = new Assessment();
      deletedAssessment.setId(40L);
      deletedAssessment.setDeletedAt(originalDeletedAt);

      when(assessmentRepository.findById(40L)).thenReturn(Optional.of(deletedAssessment));

      assessmentService.deleteAssessment(40L);

      verify(assessmentRepository, never()).save(any());
      assertThat(deletedAssessment.getDeletedAt()).isEqualTo(originalDeletedAt);
    }
  }

  @Nested
  @DisplayName("JobApplication Invariant Hardening Tests")
  class JobApplicationTests {

    @Mock private JobApplicationRepository jobApplicationRepository;
    @Mock private PlacementRecordRepository placementRecordRepository;
    @Mock private TraineeRepository traineeRepository;
    @Mock private TrainingEnrollmentRepository trainingEnrollmentRepository;
    @Mock private JobPostingRepository jobPostingRepository;
    @Mock private RefApplicationStatusRepository refApplicationStatusRepository;
    @Mock private RefNonSelectionReasonRepository refNonSelectionReasonRepository;
    @Mock private JobApplicationMapper jobApplicationMapper;

    private JobApplicationServiceImpl jobApplicationService;

    @BeforeEach
    void setUp() {
      jobApplicationService = new JobApplicationServiceImpl(
          jobApplicationRepository,
          placementRecordRepository,
          traineeRepository,
          trainingEnrollmentRepository,
          jobPostingRepository,
          refApplicationStatusRepository,
          refNonSelectionReasonRepository,
          jobApplicationMapper
      );
    }

    @Test
    @DisplayName("Creating a job application for a soft-deleted trainee throws BadRequestException with TRAINEE_DELETED")
    void testCreateJobApplication_whenTraineeSoftDeleted_throwsBadRequestExceptionTraineeDeleted() {
      CreateJobApplicationRequest request = new CreateJobApplicationRequest(
          100L, 200L, 1L, LocalDate.of(2026, 1, 1));

      when(jobApplicationRepository.existsByTraineeIdAndJobPostingId(100L, 200L)).thenReturn(false);

      Trainee deletedTrainee = new Trainee();
      deletedTrainee.setId(100L);
      deletedTrainee.setDeletedAt(LocalDateTime.now().minusDays(1));
      when(traineeRepository.findById(100L)).thenReturn(Optional.of(deletedTrainee));

      assertThatThrownBy(() -> jobApplicationService.createJobApplication(request))
          .isInstanceOf(BadRequestException.class)
          .satisfies(ex -> {
            BadRequestException bre = (BadRequestException) ex;
            assertThat(bre.getMessage()).isEqualTo("Cannot create job application for soft-deleted trainee");
            assertThat(bre.getErrorCode()).isEqualTo("TRAINEE_DELETED");
          });

      verify(jobApplicationRepository, never()).save(any());
    }
  }
}
