package in.gov.sih.sih26135.workflow;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import in.gov.sih.sih26135.dto.request.CreateAssessmentResultRequest;
import in.gov.sih.sih26135.dto.request.CreateCertificationRequest;
import in.gov.sih.sih26135.dto.request.CreateEmploymentRecordRequest;
import in.gov.sih.sih26135.dto.request.CreateFollowupTaskRequest;
import in.gov.sih.sih26135.dto.request.CreateJobApplicationRequest;
import in.gov.sih.sih26135.dto.request.CreatePlacementRecordRequest;
import in.gov.sih.sih26135.dto.request.CreateSurveyResponseRequest;
import in.gov.sih.sih26135.dto.request.CreateTrainingEnrollmentRequest;
import in.gov.sih.sih26135.dto.request.PlacementToEmploymentTransitionRequest;
import in.gov.sih.sih26135.dto.response.EmploymentRecordResponse;
import in.gov.sih.sih26135.dto.response.PlacementRecordResponse;
import in.gov.sih.sih26135.dto.response.PlacementToEmploymentTransitionResponse;
import in.gov.sih.sih26135.exception.ConflictException;
import in.gov.sih.sih26135.exception.ErrorResponse;
import in.gov.sih.sih26135.exception.GlobalExceptionHandler;
import in.gov.sih.sih26135.repository.AssessmentResultRepository;
import in.gov.sih.sih26135.repository.CertificationRepository;
import in.gov.sih.sih26135.repository.EmploymentRecordRepository;
import in.gov.sih.sih26135.repository.FollowupTaskRepository;
import in.gov.sih.sih26135.repository.JobApplicationRepository;
import in.gov.sih.sih26135.repository.PlacementRecordRepository;
import in.gov.sih.sih26135.repository.SurveyResponseRepository;
import in.gov.sih.sih26135.repository.TrainingEnrollmentRepository;
import in.gov.sih.sih26135.service.EmploymentRecordService;
import in.gov.sih.sih26135.service.PlacementRecordService;
import in.gov.sih.sih26135.security.WorkflowSecurityScopeValidator;
import in.gov.sih.sih26135.service.impl.AssessmentResultServiceImpl;
import in.gov.sih.sih26135.service.impl.CertificationServiceImpl;
import in.gov.sih.sih26135.service.impl.FollowupTaskServiceImpl;
import in.gov.sih.sih26135.service.impl.JobApplicationServiceImpl;
import in.gov.sih.sih26135.service.impl.PlacementEmploymentWorkflowServiceImpl;
import in.gov.sih.sih26135.service.impl.PlacementRecordServiceImpl;
import in.gov.sih.sih26135.service.impl.SurveyResponseServiceImpl;
import in.gov.sih.sih26135.service.impl.TrainingEnrollmentServiceImpl;
import jakarta.servlet.http.HttpServletRequest;
import java.math.BigDecimal;
import java.sql.SQLIntegrityConstraintViolationException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

@ExtendWith(MockitoExtension.class)
@DisplayName("Phase 25.10 — Duplicate Requests & Concurrency Safety Verification")
class DuplicateRequestsAndConcurrencySafetyTest {

  private GlobalExceptionHandler exceptionHandler;
  @Mock private HttpServletRequest httpServletRequest;

  @BeforeEach
  void setUpHandler() {
    exceptionHandler = new GlobalExceptionHandler();
  }

  private DataIntegrityViolationException buildIntegrityException(String keyName, String entry) {
    SQLIntegrityConstraintViolationException sqlEx =
        new SQLIntegrityConstraintViolationException("Duplicate entry '" + entry + "' for key '" + keyName + "'");
    return new DataIntegrityViolationException("could not execute statement", sqlEx);
  }

  // =========================================================================
  // 1. Dual-Layer Protection: Application Pre-check vs Database Constraint
  // =========================================================================

  @Nested
  @DisplayName("1. Dual-Layer Error Code Consistency")
  class DualLayerErrorCodeConsistencyTests {

    @Test
    @DisplayName("Placement number conflict produces identical 409 code across both layers")
    void testPlacementNumberConflict_identicalCodes() {
      // Layer 1: Application-level check
      PlacementRecordRepository repo = mock(PlacementRecordRepository.class);
      when(repo.existsByPlacementNumber("PLC-001")).thenReturn(true);
      PlacementRecordServiceImpl service = new PlacementRecordServiceImpl(
          repo, null, null, null, null, null, null, null, null, null, null, null, null, null, null
      );

      CreatePlacementRecordRequest req = new CreatePlacementRecordRequest();
      req.setPlacementNumber("PLC-001");
      req.setTraineeId(1L);
      req.setEnrollmentId(2L);
      req.setPlacementSourceId(1L);
      req.setPlacementStatusId(1L);
      req.setJoiningStatusId(1L);
      req.setRecordVerificationStatusId(1L);

      assertThatThrownBy(() -> service.createPlacementRecord(req))
          .isInstanceOf(ConflictException.class)
          .satisfies(ex -> {
            ConflictException ce = (ConflictException) ex;
            assertThat(ce.getErrorCode()).isEqualTo("PLACEMENT_NUMBER_ALREADY_EXISTS");
          });

      // Layer 2: Database unique constraint violation
      DataIntegrityViolationException dbEx = buildIntegrityException("uk_placement_records_number", "PLC-001");
      ResponseEntity<ErrorResponse> response = exceptionHandler.handleDataIntegrityViolation(dbEx, httpServletRequest);

      assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
      assertThat(response.getBody()).isNotNull();
      assertThat(response.getBody().getCode()).isEqualTo("PLACEMENT_NUMBER_ALREADY_EXISTS");
    }

    @Test
    @DisplayName("Duplicate job application produces identical 409 code across both layers")
    void testJobApplicationConflict_identicalCodes() {
      // Layer 1: Application-level check
      JobApplicationRepository repo = mock(JobApplicationRepository.class);
      when(repo.existsByTraineeIdAndJobPostingId(10L, 20L)).thenReturn(true);
      JobApplicationServiceImpl service = new JobApplicationServiceImpl(
          repo, null, null, null, null, null, null, null
      );

      CreateJobApplicationRequest req = new CreateJobApplicationRequest();
      req.setTraineeId(10L);
      req.setJobPostingId(20L);
      req.setAppliedDate(LocalDate.now());
      req.setApplicationStatusId(1L);

      assertThatThrownBy(() -> service.createJobApplication(req))
          .isInstanceOf(ConflictException.class)
          .satisfies(ex -> {
            ConflictException ce = (ConflictException) ex;
            assertThat(ce.getErrorCode()).isEqualTo("DUPLICATE_JOB_APPLICATION");
          });

      // Layer 2: Database unique constraint violation
      DataIntegrityViolationException dbEx = buildIntegrityException("uk_job_applications_trainee_posting", "10-20");
      ResponseEntity<ErrorResponse> response = exceptionHandler.handleDataIntegrityViolation(dbEx, httpServletRequest);

      assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
      assertThat(response.getBody()).isNotNull();
      assertThat(response.getBody().getCode()).isEqualTo("DUPLICATE_JOB_APPLICATION");
    }

    @Test
    @DisplayName("Assessment result conflict produces identical 409 code across both layers")
    void testAssessmentResultConflict_identicalCodes() {
      // Layer 1: Application-level check
      AssessmentResultRepository repo = mock(AssessmentResultRepository.class);
      when(repo.existsByTraineeAssessmentId(100L)).thenReturn(true);
      AssessmentResultServiceImpl service = new AssessmentResultServiceImpl(
          repo, null, null, null, null
      );

      CreateAssessmentResultRequest req = new CreateAssessmentResultRequest();
      req.setTraineeAssessmentId(100L);
      req.setMaximumScore(new BigDecimal("100.00"));
      req.setAssessmentOutcomeId(1L);

      assertThatThrownBy(() -> service.createAssessmentResult(req))
          .isInstanceOf(ConflictException.class)
          .satisfies(ex -> {
            ConflictException ce = (ConflictException) ex;
            assertThat(ce.getErrorCode()).isEqualTo("ASSESSMENT_RESULT_ALREADY_EXISTS");
          });

      // Layer 2: Database unique constraint violation
      DataIntegrityViolationException dbEx = buildIntegrityException("uk_assessment_results_attempt", "100");
      ResponseEntity<ErrorResponse> response = exceptionHandler.handleDataIntegrityViolation(dbEx, httpServletRequest);

      assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
      assertThat(response.getBody()).isNotNull();
      assertThat(response.getBody().getCode()).isEqualTo("ASSESSMENT_RESULT_ALREADY_EXISTS");
    }

    @Test
    @DisplayName("Certificate number conflict produces identical 409 code across both layers")
    void testCertificateNumberConflict_identicalCodes() {
      // Layer 1: Application-level check
      CertificationRepository repo = mock(CertificationRepository.class);
      when(repo.existsByCertificateNumber("CERT-1001")).thenReturn(true);
      CertificationServiceImpl service = new CertificationServiceImpl(
          repo, null, null, null, null, null, null, null, null, null
      );

      CreateCertificationRequest req = new CreateCertificationRequest();
      req.setCertificateNumber("CERT-1001");
      req.setIssueDate(LocalDate.now());
      req.setCertificateStatusId(1L);
      req.setCertificateVerificationStatusId(1L);

      assertThatThrownBy(() -> service.createCertification(req))
          .isInstanceOf(ConflictException.class)
          .satisfies(ex -> {
            ConflictException ce = (ConflictException) ex;
            assertThat(ce.getErrorCode()).isEqualTo("CERTIFICATE_NUMBER_ALREADY_EXISTS");
          });

      // Layer 2: Database unique constraint violation
      DataIntegrityViolationException dbEx = buildIntegrityException("uk_certifications_certificate_number", "CERT-1001");
      ResponseEntity<ErrorResponse> response = exceptionHandler.handleDataIntegrityViolation(dbEx, httpServletRequest);

      assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
      assertThat(response.getBody()).isNotNull();
      assertThat(response.getBody().getCode()).isEqualTo("CERTIFICATE_NUMBER_ALREADY_EXISTS");
    }

    @Test
    @DisplayName("Training enrollment number conflict produces identical 409 code across both layers")
    void testEnrollmentNumberConflict_identicalCodes() {
      // Layer 1: Application-level check
      TrainingEnrollmentRepository repo = mock(TrainingEnrollmentRepository.class);
      when(repo.existsByEnrollmentNumber("ENR-555")).thenReturn(true);
      TrainingEnrollmentServiceImpl service = new TrainingEnrollmentServiceImpl(
          repo, null, null, null, null, null
      );

      CreateTrainingEnrollmentRequest req = new CreateTrainingEnrollmentRequest();
      req.setEnrollmentNumber("ENR-555");
      req.setTraineeId(10L);
      req.setBatchId(20L);
      req.setEnrollmentDate(LocalDate.now());
      req.setEnrollmentStatusId(1L);

      assertThatThrownBy(() -> service.createEnrollment(req))
          .isInstanceOf(ConflictException.class)
          .satisfies(ex -> {
            ConflictException ce = (ConflictException) ex;
            assertThat(ce.getErrorCode()).isEqualTo("DUPLICATE_ENROLLMENT_NUMBER");
          });

      // Layer 2: Database unique constraint violation
      DataIntegrityViolationException dbEx = buildIntegrityException("uk_training_enrollments_number", "ENR-555");
      ResponseEntity<ErrorResponse> response = exceptionHandler.handleDataIntegrityViolation(dbEx, httpServletRequest);

      assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
      assertThat(response.getBody()).isNotNull();
      assertThat(response.getBody().getCode()).isEqualTo("DUPLICATE_ENROLLMENT_NUMBER");
    }

    @Test
    @DisplayName("Follow-up task duplicate produces identical 409 code across both layers")
    void testFollowupTaskConflict_identicalCodes() {
      // Layer 1: Application-level check
      FollowupTaskRepository repo = mock(FollowupTaskRepository.class);
      when(repo.existsByFollowupCampaignIdAndTraineeId(5L, 10L)).thenReturn(true);
      FollowupTaskServiceImpl service = new FollowupTaskServiceImpl(
          repo, null, null, null, null, null, null, null, null, null, null
      );

      CreateFollowupTaskRequest req = new CreateFollowupTaskRequest();
      req.setFollowupCampaignId(5L);
      req.setTraineeId(10L);
      req.setFollowupStatusId(1L);
      req.setScheduledDate(LocalDate.now());

      assertThatThrownBy(() -> service.createFollowupTask(req))
          .isInstanceOf(ConflictException.class)
          .satisfies(ex -> {
            ConflictException ce = (ConflictException) ex;
            assertThat(ce.getErrorCode()).isEqualTo("DUPLICATE_FOLLOWUP_TASK");
          });

      // Layer 2: Database unique constraint violation
      DataIntegrityViolationException dbEx = buildIntegrityException("uk_followup_tasks_campaign_trainee", "5-10");
      ResponseEntity<ErrorResponse> response = exceptionHandler.handleDataIntegrityViolation(dbEx, httpServletRequest);

      assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
      assertThat(response.getBody()).isNotNull();
      assertThat(response.getBody().getCode()).isEqualTo("DUPLICATE_FOLLOWUP_TASK");
    }

    @Test
    @DisplayName("Survey response duplicate produces identical 409 code across both layers")
    void testSurveyResponseConflict_identicalCodes() {
      // Layer 1: Application-level check
      SurveyResponseRepository repo = mock(SurveyResponseRepository.class);
      when(repo.existsBySurveyIdAndTraineeIdAndAttemptNumber(1L, 10L, 1)).thenReturn(true);
      SurveyResponseServiceImpl service = new SurveyResponseServiceImpl(
          repo, null, null, null, null, null, null
      );

      CreateSurveyResponseRequest req = new CreateSurveyResponseRequest();
      req.setSurveyId(1L);
      req.setSurveyTemplateVersionId(1L);
      req.setTraineeId(10L);
      req.setSurveyResponseStatusId(1L);
      req.setAttemptNumber(1);

      assertThatThrownBy(() -> service.createSurveyResponse(req))
          .isInstanceOf(ConflictException.class)
          .satisfies(ex -> {
            ConflictException ce = (ConflictException) ex;
            assertThat(ce.getErrorCode()).isEqualTo("DUPLICATE_SURVEY_RESPONSE");
          });

      // Layer 2: Database unique constraint violation
      DataIntegrityViolationException dbEx = buildIntegrityException("uk_survey_responses_attempt", "1-10-1");
      ResponseEntity<ErrorResponse> response = exceptionHandler.handleDataIntegrityViolation(dbEx, httpServletRequest);

      assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
      assertThat(response.getBody()).isNotNull();
      assertThat(response.getBody().getCode()).isEqualTo("DUPLICATE_SURVEY_RESPONSE");
    }

    @Test
    @DisplayName("Placement-to-employment transition duplicate produces identical 409 code across both layers")
    void testPlacementEmploymentTransitionConflict_identicalCodes() {
      // Layer 1: Application-level check
      EmploymentRecordRepository empRepo = mock(EmploymentRecordRepository.class);
      PlacementRecordService placementService = mock(PlacementRecordService.class);
      PlacementRecordResponse placement = new PlacementRecordResponse(
          10L, "PLC-001", 100L, "REG-100", "Aditya", "Kumar",
          200L, "ENR-200", 300L, "Course A", 400L, "Prog A",
          500L, "Provider A", 600L, "Tech Corp", 700L, "Branch A",
          null, null, null, 800L, "Software Engineer",
          1L, "REGULAR_WAGE", 1L, "CAMPUS",
          2L, "OFFERED", 3L, "PENDING",
          new BigDecimal("50000.00"), new BigDecimal("50000.00"),
          1L, "MONTHLY", "INR",
          LocalDate.of(2026, 1, 15), LocalDate.of(2026, 2, 1), LocalDate.of(2026, 2, 1),
          null, null, "Hired",
          1L, 1L, 1L, "VERIFIED",
          LocalDateTime.now(), 1L, LocalDateTime.now(), LocalDateTime.now(),
          null
      );
      when(placementService.getPlacementRecordById(10L)).thenReturn(placement);
      when(empRepo.findByPlacementRecordId(10L)).thenReturn(Optional.of(new in.gov.sih.sih26135.entity.EmploymentRecord()));

      PlacementEmploymentWorkflowServiceImpl workflowService = new PlacementEmploymentWorkflowServiceImpl(
          placementService, null, null, empRepo, null, null, mock(WorkflowSecurityScopeValidator.class)
      );

      PlacementToEmploymentTransitionRequest req = new PlacementToEmploymentTransitionRequest();
      req.setPlacementId(10L);
      req.setEmploymentRecordRequest(new CreateEmploymentRecordRequest());

      assertThatThrownBy(() -> workflowService.transitionPlacementToEmployment(req))
          .isInstanceOf(ConflictException.class)
          .satisfies(ex -> {
            ConflictException ce = (ConflictException) ex;
            assertThat(ce.getErrorCode()).isEqualTo("DUPLICATE_PLACEMENT_EMPLOYMENT");
          });

      // Layer 2: Database unique constraint violation
      DataIntegrityViolationException dbEx = buildIntegrityException("uk_employment_records_placement", "10");
      ResponseEntity<ErrorResponse> response = exceptionHandler.handleDataIntegrityViolation(dbEx, httpServletRequest);

      assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
      assertThat(response.getBody()).isNotNull();
      assertThat(response.getBody().getCode()).isEqualTo("DUPLICATE_PLACEMENT_EMPLOYMENT");
    }
  }

  // =========================================================================
  // 2. Multi-threaded Race Condition Simulation
  // =========================================================================

  @Nested
  @DisplayName("2. Multi-Threaded Concurrency Race Condition Safety")
  class MultiThreadedConcurrencySafetyTests {

    @Test
    @DisplayName("Simulated concurrent placement-to-employment requests: exactly one succeeds, other fails with 409 DUPLICATE_PLACEMENT_EMPLOYMENT")
    void testConcurrentPlacementToEmploymentRequests_oneWinsOneFailsConflict() throws Exception {
      PlacementRecordService placementService = mock(PlacementRecordService.class);
      EmploymentRecordService employmentRecordService = mock(EmploymentRecordService.class);
      EmploymentRecordRepository employmentRecordRepository = mock(EmploymentRecordRepository.class);

      PlacementRecordResponse placement = new PlacementRecordResponse(
          10L, "PLC-001", 100L, "REG-100", "Aditya", "Kumar",
          200L, "ENR-200", 300L, "Course A", 400L, "Prog A",
          500L, "Provider A", 600L, "Tech Corp", 700L, "Branch A",
          null, null, null, 800L, "Software Engineer",
          1L, "REGULAR_WAGE", 1L, "CAMPUS",
          2L, "OFFERED", 3L, "PENDING",
          new BigDecimal("50000.00"), new BigDecimal("50000.00"),
          1L, "MONTHLY", "INR",
          LocalDate.of(2026, 1, 15), LocalDate.of(2026, 2, 1), LocalDate.of(2026, 2, 1),
          null, null, "Hired",
          1L, 1L, 1L, "VERIFIED",
          LocalDateTime.now(), 1L, LocalDateTime.now(), LocalDateTime.now(),
          null
      );
      when(placementService.getPlacementRecordById(10L)).thenReturn(placement);

      EmploymentRecordResponse successResponse = new EmploymentRecordResponse(
          1L, "EMP-001", 100L, "REG-100", "Aditya", "Kumar",
          200L, "ENR-200", 10L, "PLC-001", 600L, "Tech Corp",
          700L, "Branch A", 800L, "Software Engineer", 1L, "REGULAR_WAGE",
          1L, "ACTIVE", "Active spell", true,
          LocalDate.of(2026, 2, 1), null, true,
          new BigDecimal("50000.00"), 1L, "MONTHLY", "INR",
          null, 1L, 1L, 1L, "DIRECT", "Direct",
          1L, "VERIFIED", LocalDateTime.now(), 1L,
          null, null, null, null,
          LocalDateTime.now(), LocalDateTime.now(), null
      );

      // Thread 1 succeeds; Thread 2 encounters database unique constraint during race
      AtomicInteger callCount = new AtomicInteger(0);
      when(employmentRecordRepository.findByPlacementRecordId(10L)).thenReturn(Optional.empty());
      when(employmentRecordService.createEmploymentRecord(any())).thenAnswer(invocation -> {
        if (callCount.incrementAndGet() == 1) {
          return successResponse;
        } else {
          // Second thread racing concurrently triggers unique constraint at DB level
          throw buildIntegrityException("uk_employment_records_placement", "10");
        }
      });

      PlacementEmploymentWorkflowServiceImpl workflow = new PlacementEmploymentWorkflowServiceImpl(
          placementService, employmentRecordService, null, employmentRecordRepository, null, null,
          mock(WorkflowSecurityScopeValidator.class)
      );

      int threadCount = 2;
      ExecutorService executor = Executors.newFixedThreadPool(threadCount);
      CountDownLatch readyLatch = new CountDownLatch(threadCount);
      CountDownLatch startLatch = new CountDownLatch(1);

      List<Future<Object>> futures = new ArrayList<>();
      for (int i = 0; i < threadCount; i++) {
        futures.add(executor.submit(() -> {
          readyLatch.countDown();
          startLatch.await();
          try {
            PlacementToEmploymentTransitionRequest req = new PlacementToEmploymentTransitionRequest();
            req.setPlacementId(10L);
            CreateEmploymentRecordRequest empReq = new CreateEmploymentRecordRequest();
            empReq.setTraineeId(100L);
            req.setEmploymentRecordRequest(empReq);
            return workflow.transitionPlacementToEmployment(req);
          } catch (Exception ex) {
            return ex;
          }
        }));
      }

      readyLatch.await();
      startLatch.countDown();

      int successCount = 0;
      int conflictCount = 0;

      for (Future<Object> f : futures) {
        Object res = f.get();
        if (res instanceof PlacementToEmploymentTransitionResponse) {
          successCount++;
        } else if (res instanceof DataIntegrityViolationException dbEx) {
          ResponseEntity<ErrorResponse> response = exceptionHandler.handleDataIntegrityViolation(dbEx, httpServletRequest);
          assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
          assertThat(response.getBody().getCode()).isEqualTo("DUPLICATE_PLACEMENT_EMPLOYMENT");
          conflictCount++;
        }
      }

      executor.shutdown();

      assertThat(successCount).isEqualTo(1);
      assertThat(conflictCount).isEqualTo(1);
    }
  }
}
