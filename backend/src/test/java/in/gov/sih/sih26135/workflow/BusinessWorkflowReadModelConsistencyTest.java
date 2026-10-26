package in.gov.sih.sih26135.workflow;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import in.gov.sih.sih26135.dto.response.AssessmentResultResponse;
import in.gov.sih.sih26135.dto.response.CertificationResponse;
import in.gov.sih.sih26135.dto.response.EmploymentExitEventResponse;
import in.gov.sih.sih26135.dto.response.EmploymentRecordResponse;
import in.gov.sih.sih26135.dto.response.EmploymentVerificationAttemptResponse;
import in.gov.sih.sih26135.dto.response.EmploymentVerificationRequestResponse;
import in.gov.sih.sih26135.dto.response.EmploymentVerificationResponse;
import in.gov.sih.sih26135.dto.response.EmploymentVerificationSummaryResponse;
import in.gov.sih.sih26135.dto.response.PlacementRecordResponse;
import in.gov.sih.sih26135.dto.response.TraineeAssessmentResponse;
import in.gov.sih.sih26135.dto.response.TraineeCareerTimelineResponse;
import in.gov.sih.sih26135.dto.response.TraineeOutcomeSummaryResponse;
import in.gov.sih.sih26135.dto.response.TraineePlacementEmploymentSummaryResponse;
import in.gov.sih.sih26135.dto.response.TraineeTrainingOutcomeSummaryResponse;
import in.gov.sih.sih26135.dto.response.TraineeUnemploymentEventResponse;
import in.gov.sih.sih26135.dto.response.TrainingEnrollmentResponse;
import in.gov.sih.sih26135.entity.Certification;
import in.gov.sih.sih26135.entity.EmploymentRecord;
import in.gov.sih.sih26135.entity.PlacementRecord;
import in.gov.sih.sih26135.entity.RefEmploymentInfoSource;
import in.gov.sih.sih26135.entity.RefRecordVerificationStatus;
import in.gov.sih.sih26135.entity.analytics.TraineeOutcomeSummary;
import in.gov.sih.sih26135.exception.BadRequestException;
import in.gov.sih.sih26135.exception.ResourceNotFoundException;
import in.gov.sih.sih26135.mapper.CertificationMapper;
import in.gov.sih.sih26135.mapper.EmploymentRecordMapper;
import in.gov.sih.sih26135.mapper.PlacementRecordMapper;
import in.gov.sih.sih26135.mapper.TraineeOutcomeSummaryMapper;
import in.gov.sih.sih26135.repository.CertificationRepository;
import in.gov.sih.sih26135.repository.CourseSkillRepository;
import in.gov.sih.sih26135.repository.EmploymentRecordRepository;
import in.gov.sih.sih26135.repository.PlacementRecordRepository;
import in.gov.sih.sih26135.repository.RefSkillGapStatusRepository;
import in.gov.sih.sih26135.repository.SkillGapRepository;
import in.gov.sih.sih26135.security.WorkflowSecurityScopeValidator;
import in.gov.sih.sih26135.service.AssessmentResultService;
import in.gov.sih.sih26135.service.CertificationService;
import in.gov.sih.sih26135.service.EmploymentExitEventService;
import in.gov.sih.sih26135.service.EmploymentRecordService;
import in.gov.sih.sih26135.service.EmploymentVerificationAttemptService;
import in.gov.sih.sih26135.service.EmploymentVerificationRequestService;
import in.gov.sih.sih26135.service.EmploymentVerificationService;
import in.gov.sih.sih26135.service.PlacementRecordService;
import in.gov.sih.sih26135.service.TraineeAssessmentService;
import in.gov.sih.sih26135.service.TraineeUnemploymentEventService;
import in.gov.sih.sih26135.service.TrainingEnrollmentService;
import in.gov.sih.sih26135.service.impl.CertificationServiceImpl;
import in.gov.sih.sih26135.service.impl.EmploymentLifecycleWorkflowServiceImpl;
import in.gov.sih.sih26135.service.impl.EmploymentRecordServiceImpl;
import in.gov.sih.sih26135.service.impl.PlacementEmploymentWorkflowServiceImpl;
import in.gov.sih.sih26135.service.impl.PlacementRecordServiceImpl;
import in.gov.sih.sih26135.service.impl.TrainingOutcomeWorkflowServiceImpl;
import in.gov.sih.sih26135.service.impl.VerificationWorkflowServiceImpl;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("Phase 25.14 — Business Workflow Read Model Consistency Tests")
class BusinessWorkflowReadModelConsistencyTest {

  @Nested
  @DisplayName("1. Training Outcome Summary Read Model & Scope Boundary")
  class TrainingOutcomeSummaryTests {

    @Mock private TrainingEnrollmentService trainingEnrollmentService;
    @Mock private TraineeAssessmentService traineeAssessmentService;
    @Mock private AssessmentResultService assessmentResultService;
    @Mock private CertificationService certificationService;
    @Mock private CourseSkillRepository courseSkillRepository;
    @Mock private SkillGapRepository skillGapRepository;
    @Mock private RefSkillGapStatusRepository refSkillGapStatusRepository;
    @Mock private WorkflowSecurityScopeValidator scopeValidator;

    private TrainingOutcomeWorkflowServiceImpl workflowService;

    @BeforeEach
    void setUp() {
      workflowService = new TrainingOutcomeWorkflowServiceImpl(
          trainingEnrollmentService,
          traineeAssessmentService,
          assessmentResultService,
          certificationService,
          courseSkillRepository,
          skillGapRepository,
          refSkillGapStatusRepository,
          scopeValidator
      );
    }

    @Test
    @DisplayName("Filters assessment results strictly to the queried enrollment, excluding cross-enrollment leakage")
    void testGetSummary_filtersResultsToEnrollmentOnly() {
      Long traineeId = 100L;
      Long enrollmentId = 1L;

      TrainingEnrollmentResponse enrol = new TrainingEnrollmentResponse();
      enrol.setId(enrollmentId);
      enrol.setTraineeId(traineeId);
      enrol.setEnrollmentStatusCode("IN_PROGRESS");

      TraineeAssessmentResponse a1 = new TraineeAssessmentResponse();
      a1.setId(10L);
      a1.setTraineeId(traineeId);

      // Result R1 belongs to assessment a1 (enrollment 1)
      AssessmentResultResponse r1 = new AssessmentResultResponse();
      r1.setId(501L);
      r1.setTraineeAssessmentId(10L);
      r1.setTraineeId(traineeId);
      r1.setAssessmentOutcomeCode("PASS");
      r1.setIsPassFlag(true);

      // Result R2 belongs to assessment 999 (from an entirely different enrollment)
      AssessmentResultResponse r2 = new AssessmentResultResponse();
      r2.setId(502L);
      r2.setTraineeAssessmentId(999L);
      r2.setTraineeId(traineeId);
      r2.setAssessmentOutcomeCode("FAIL");
      r2.setIsPassFlag(false);

      when(trainingEnrollmentService.getById(enrollmentId)).thenReturn(enrol);
      when(traineeAssessmentService.getByEnrollmentId(enrollmentId)).thenReturn(List.of(a1));
      when(assessmentResultService.getByTraineeId(traineeId)).thenReturn(List.of(r1, r2));
      when(certificationService.getByEnrollmentId(enrollmentId)).thenReturn(Collections.emptyList());

      TraineeTrainingOutcomeSummaryResponse summary =
          workflowService.getTraineeTrainingOutcomeSummary(traineeId, enrollmentId);

      assertThat(summary).isNotNull();
      assertThat(summary.enrollment()).isEqualTo(enrol);
      assertThat(summary.assessments()).hasSize(1);
      // Crucial: Only r1 is returned in this enrollment's summary. Cross-enrollment result r2 is excluded!
      assertThat(summary.results()).hasSize(1);
      assertThat(summary.results().get(0).getId()).isEqualTo(501L);
      assertThat(summary.results().get(0).getTraineeAssessmentId()).isEqualTo(10L);
      verify(scopeValidator).validateTraineeScope(traineeId);
    }

    @Test
    @DisplayName("When enrollment has no assessments, outcome summary results are empty without fabricating results")
    void testGetSummary_noAssessments_returnsEmptyResults() {
      Long traineeId = 100L;
      Long enrollmentId = 2L;

      TrainingEnrollmentResponse enrol = new TrainingEnrollmentResponse();
      enrol.setId(enrollmentId);
      enrol.setTraineeId(traineeId);
      enrol.setEnrollmentStatusCode("ENROLLED");

      // Trainee has other results from previous enrollments
      AssessmentResultResponse rOther = new AssessmentResultResponse();
      rOther.setId(999L);
      rOther.setTraineeAssessmentId(888L);
      rOther.setTraineeId(traineeId);

      when(trainingEnrollmentService.getById(enrollmentId)).thenReturn(enrol);
      when(traineeAssessmentService.getByEnrollmentId(enrollmentId)).thenReturn(Collections.emptyList());
      when(assessmentResultService.getByTraineeId(traineeId)).thenReturn(List.of(rOther));
      when(certificationService.getByEnrollmentId(enrollmentId)).thenReturn(Collections.emptyList());

      TraineeTrainingOutcomeSummaryResponse summary =
          workflowService.getTraineeTrainingOutcomeSummary(traineeId, enrollmentId);

      assertThat(summary.assessments()).isEmpty();
      assertThat(summary.results()).isEmpty();
      assertThat(summary.certifications()).isEmpty();
    }

    @Test
    @DisplayName("Throws ResourceNotFoundException when enrollment is soft-deleted")
    void testGetSummary_softDeletedEnrollment_throwsNotFound() {
      Long traineeId = 100L;
      Long enrollmentId = 3L;

      TrainingEnrollmentResponse deletedEnrol = new TrainingEnrollmentResponse();
      deletedEnrol.setId(enrollmentId);
      deletedEnrol.setTraineeId(traineeId);
      deletedEnrol.setDeletedAt(LocalDateTime.now().minusDays(1));

      when(trainingEnrollmentService.getById(enrollmentId)).thenReturn(deletedEnrol);

      assertThatThrownBy(() -> workflowService.getTraineeTrainingOutcomeSummary(traineeId, enrollmentId))
          .isInstanceOf(ResourceNotFoundException.class);

      verify(traineeAssessmentService, never()).getByEnrollmentId(anyLong());
    }

    @Test
    @DisplayName("Rejects outcome summary read when enrollment belongs to a different trainee")
    void testGetSummary_traineeEnrollmentMismatch_throwsBadRequest() {
      Long requestedTraineeId = 100L;
      Long actualTraineeId = 200L;
      Long enrollmentId = 4L;

      TrainingEnrollmentResponse enrol = new TrainingEnrollmentResponse();
      enrol.setId(enrollmentId);
      enrol.setTraineeId(actualTraineeId);

      when(trainingEnrollmentService.getById(enrollmentId)).thenReturn(enrol);

      assertThatThrownBy(() -> workflowService.getTraineeTrainingOutcomeSummary(requestedTraineeId, enrollmentId))
          .isInstanceOf(BadRequestException.class)
          .extracting("errorCode").isEqualTo("TRAINEE_ENROLLMENT_MISMATCH");
    }
  }

  @Nested
  @DisplayName("2. Employment Lifecycle & Placement Consistency (Soft-Delete Exclusions)")
  class EmploymentLifecycleAndPlacementTests {

    @Mock private EmploymentRecordRepository employmentRecordRepository;
    @Mock private EmploymentRecordMapper employmentRecordMapper;
    @Mock private PlacementRecordRepository placementRecordRepository;
    @Mock private PlacementRecordMapper placementRecordMapper;
    @Mock private CertificationRepository certificationRepository;
    @Mock private CertificationMapper certificationMapper;

    @Test
    @DisplayName("getEmploymentRecordsByTrainee excludes soft-deleted records to agree with analytical views")
    void testGetEmploymentRecordsByTrainee_excludesSoftDeleted() {
      EmploymentRecordServiceImpl empService = new EmploymentRecordServiceImpl(
          employmentRecordRepository,
          null, null, null, null, null, null, null, null, null, null, null, null,
          employmentRecordMapper
      );

      Long traineeId = 10L;

      EmploymentRecord activeRec = new EmploymentRecord();
      activeRec.setEmploymentNumber("EMP-ACT-1");
      activeRec.setDeletedAt(null);

      EmploymentRecord deletedRec = new EmploymentRecord();
      deletedRec.setEmploymentNumber("EMP-DEL-1");
      deletedRec.setDeletedAt(LocalDateTime.now().minusDays(5));

      EmploymentRecordResponse activeResp = mock(EmploymentRecordResponse.class);
      when(activeResp.employmentNumber()).thenReturn("EMP-ACT-1");

      when(employmentRecordRepository.findByTraineeId(traineeId)).thenReturn(List.of(activeRec, deletedRec));
      when(employmentRecordMapper.toResponse(activeRec)).thenReturn(activeResp);

      List<EmploymentRecordResponse> result = empService.getEmploymentRecordsByTrainee(traineeId);

      assertThat(result).hasSize(1);
      assertThat(result.get(0).employmentNumber()).isEqualTo("EMP-ACT-1");
      verify(employmentRecordMapper, never()).toResponse(deletedRec);
    }

    @Test
    @DisplayName("getPlacementRecordsByTrainee excludes soft-deleted records to agree with analytical views")
    void testGetPlacementRecordsByTrainee_excludesSoftDeleted() {
      PlacementRecordServiceImpl placementService = new PlacementRecordServiceImpl(
          placementRecordRepository,
          null, null, null, null, null, null, null, null, null, null, null, null, null,
          placementRecordMapper
      );

      Long traineeId = 10L;

      PlacementRecord activePl = new PlacementRecord();
      activePl.setPlacementNumber("PL-ACT-1");
      activePl.setDeletedAt(null);

      PlacementRecord deletedPl = new PlacementRecord();
      deletedPl.setPlacementNumber("PL-DEL-1");
      deletedPl.setDeletedAt(LocalDateTime.now().minusDays(3));

      PlacementRecordResponse activeResp = mock(PlacementRecordResponse.class);
      when(activeResp.placementNumber()).thenReturn("PL-ACT-1");

      when(placementRecordRepository.findByTraineeId(traineeId)).thenReturn(List.of(activePl, deletedPl));
      when(placementRecordMapper.toResponse(activePl)).thenReturn(activeResp);

      List<PlacementRecordResponse> result = placementService.getPlacementRecordsByTrainee(traineeId);

      assertThat(result).hasSize(1);
      assertThat(result.get(0).placementNumber()).isEqualTo("PL-ACT-1");
      verify(placementRecordMapper, never()).toResponse(deletedPl);
    }

    @Test
    @DisplayName("CertificationService.getByEnrollmentId excludes soft-deleted certifications")
    void testGetCertificationsByEnrollmentId_excludesSoftDeleted() {
      CertificationServiceImpl certService = new CertificationServiceImpl(
          certificationRepository,
          null, null, null, null, null, null, null, null,
          certificationMapper
      );

      Long enrollmentId = 5L;

      Certification activeCert = new Certification();
      activeCert.setCertificateNumber("CERT-ACT-1");
      activeCert.setDeletedAt(null);

      Certification deletedCert = new Certification();
      deletedCert.setCertificateNumber("CERT-DEL-1");
      deletedCert.setDeletedAt(LocalDateTime.now().minusDays(2));

      CertificationResponse activeResp = new CertificationResponse();
      activeResp.setId(101L);

      when(certificationRepository.findByTrainingEnrollmentId(enrollmentId))
          .thenReturn(List.of(activeCert, deletedCert));
      when(certificationMapper.toResponse(activeCert)).thenReturn(activeResp);

      List<CertificationResponse> result = certService.getByEnrollmentId(enrollmentId);

      assertThat(result).hasSize(1);
      assertThat(result.get(0).getId()).isEqualTo(101L);
      verify(certificationMapper, never()).toResponse(deletedCert);
    }

    @Test
    @DisplayName("TraineeCareerTimeline and PlacementEmploymentSummary return empty collections for trainees without records, never inferring negative outcome")
    void testCareerTimelineAndPlacementSummary_absentRecords_returnsCleanEmpty() {
      Long traineeId = 99L;
      WorkflowSecurityScopeValidator validator = mock(WorkflowSecurityScopeValidator.class);
      EmploymentRecordService empService = mock(EmploymentRecordService.class);
      EmploymentExitEventService exitService = mock(EmploymentExitEventService.class);
      TraineeUnemploymentEventService unempService = mock(TraineeUnemploymentEventService.class);
      PlacementRecordService placementService = mock(PlacementRecordService.class);

      when(empService.getEmploymentRecordsByTrainee(traineeId)).thenReturn(Collections.emptyList());
      when(exitService.getExitEventsByTraineeId(traineeId)).thenReturn(Collections.emptyList());
      when(unempService.getUnemploymentEventsByTraineeId(traineeId)).thenReturn(Collections.emptyList());
      when(placementService.getPlacementRecordsByTrainee(traineeId)).thenReturn(Collections.emptyList());

      EmploymentLifecycleWorkflowServiceImpl lifecycleService = new EmploymentLifecycleWorkflowServiceImpl(
          empService, exitService, unempService, null, null, validator
      );
      PlacementEmploymentWorkflowServiceImpl placementEmpService = new PlacementEmploymentWorkflowServiceImpl(
          placementService, empService, null, null, null, null, validator
      );

      TraineeCareerTimelineResponse timeline = lifecycleService.getTraineeCareerTimeline(traineeId);
      TraineePlacementEmploymentSummaryResponse placementSummary =
          placementEmpService.getTraineePlacementEmploymentSummary(traineeId);

      assertThat(timeline.employmentRecords()).isEmpty();
      assertThat(timeline.exitEvents()).isEmpty();
      assertThat(timeline.unemploymentEvents()).isEmpty();

      assertThat(placementSummary.placements()).isEmpty();
      assertThat(placementSummary.employmentRecords()).isEmpty();

      // Crucial: Both read models agree completely on the underlying facts
      assertThat(timeline.employmentRecords()).isEqualTo(placementSummary.employmentRecords());
    }
  }

  @Nested
  @DisplayName("3. Employment Verification Summary Read Model")
  class VerificationSummaryTests {

    @Mock private EmploymentRecordService employmentRecordService;
    @Mock private EmploymentVerificationRequestService requestService;
    @Mock private EmploymentVerificationAttemptService attemptService;
    @Mock private EmploymentVerificationService verificationService;
    @Mock private WorkflowSecurityScopeValidator scopeValidator;

    private VerificationWorkflowServiceImpl verificationWorkflowService;

    @BeforeEach
    void setUp() {
      verificationWorkflowService = new VerificationWorkflowServiceImpl(
          employmentRecordService,
          null, null,
          requestService,
          attemptService,
          verificationService,
          null, null, null,
          scopeValidator
      );
    }

    @Test
    @DisplayName("Throws ResourceNotFoundException when employment record is soft-deleted")
    void testGetEmploymentVerificationSummary_softDeletedEmployment_throwsNotFound() {
      Long employmentId = 44L;
      EmploymentRecordResponse deletedEmp = mock(EmploymentRecordResponse.class);
      when(deletedEmp.deletedAt()).thenReturn(LocalDateTime.now().minusDays(1));

      when(employmentRecordService.getEmploymentRecordById(employmentId)).thenReturn(deletedEmp);

      assertThatThrownBy(() -> verificationWorkflowService.getEmploymentVerificationSummary(employmentId))
          .isInstanceOf(ResourceNotFoundException.class);

      verify(requestService, never()).getRequestsByEmploymentId(anyLong());
    }

    @Test
    @DisplayName("Retrieves verified employment summary and linked multi-attempt verification chain")
    void testGetEmploymentVerificationSummary_success() {
      Long employmentId = 55L;
      Long employerId = 12L;

      EmploymentRecordResponse emp = mock(EmploymentRecordResponse.class);
      when(emp.id()).thenReturn(employmentId);
      when(emp.employerId()).thenReturn(employerId);
      when(emp.deletedAt()).thenReturn(null);

      EmploymentVerificationRequestResponse req = mock(EmploymentVerificationRequestResponse.class);
      when(req.id()).thenReturn(101L);

      EmploymentVerificationAttemptResponse att = mock(EmploymentVerificationAttemptResponse.class);

      EmploymentVerificationResponse ver = mock(EmploymentVerificationResponse.class);
      when(ver.recordVerificationStatusCode()).thenReturn("VERIFIED");

      when(employmentRecordService.getEmploymentRecordById(employmentId)).thenReturn(emp);
      when(requestService.getRequestsByEmploymentId(employmentId)).thenReturn(List.of(req));
      when(attemptService.getAttemptsByRequestId(101L)).thenReturn(List.of(att));
      when(verificationService.getVerificationsByEmploymentId(employmentId)).thenReturn(List.of(ver));

      EmploymentVerificationSummaryResponse summary =
          verificationWorkflowService.getEmploymentVerificationSummary(employmentId);

      assertThat(summary).isNotNull();
      assertThat(summary.employmentRecord().id()).isEqualTo(employmentId);
      assertThat(summary.requests()).hasSize(1);
      assertThat(summary.attempts()).hasSize(1);
      assertThat(summary.verifications()).hasSize(1);
      assertThat(summary.verifications().get(0).recordVerificationStatusCode()).isEqualTo("VERIFIED");
      verify(scopeValidator).validateEmployerScope(employerId);
    }
  }

  @Nested
  @DisplayName("4. Outcome State Semantics Distinction (Unknown vs Pending vs Self-Reported vs Verified)")
  class OutcomeStateSemanticsTests {

    @Test
    @DisplayName("Self-reported verification status has is_verified_flag = false and is never treated as verified")
    void testSelfReportedVsVerified_explicitDistinction() {
      RefRecordVerificationStatus selfReported = new RefRecordVerificationStatus();
      selfReported.setId(2L);
      selfReported.setStatusCode("SELF_REPORTED");
      selfReported.setStatusName("Self-reported");
      selfReported.setIsVerifiedFlag(false);

      RefRecordVerificationStatus verified = new RefRecordVerificationStatus();
      verified.setId(4L);
      verified.setStatusCode("VERIFIED");
      verified.setStatusName("Verified");
      verified.setIsVerifiedFlag(true);

      RefRecordVerificationStatus unverified = new RefRecordVerificationStatus();
      unverified.setId(1L);
      unverified.setStatusCode("UNVERIFIED");
      unverified.setStatusName("Unverified");
      unverified.setIsVerifiedFlag(false);

      RefRecordVerificationStatus pending = new RefRecordVerificationStatus();
      pending.setId(3L);
      pending.setStatusCode("PENDING");
      pending.setStatusName("Verification pending");
      pending.setIsVerifiedFlag(false);

      // Distinct codes
      assertThat(selfReported.getStatusCode()).isEqualTo("SELF_REPORTED");
      assertThat(verified.getStatusCode()).isEqualTo("VERIFIED");
      assertThat(unverified.getStatusCode()).isEqualTo("UNVERIFIED");
      assertThat(pending.getStatusCode()).isEqualTo("PENDING");

      // Flag distinction: ONLY verified has isVerifiedFlag = true
      assertThat(selfReported.getIsVerifiedFlag()).isFalse();
      assertThat(unverified.getIsVerifiedFlag()).isFalse();
      assertThat(pending.getIsVerifiedFlag()).isFalse();
      assertThat(verified.getIsVerifiedFlag()).isTrue();
    }

    @Test
    @DisplayName("Employment info source distinguishes self-reported trainee declaration from third-party / official conversion")
    void testEmploymentInfoSource_selfReportedFlag() {
      RefEmploymentInfoSource traineeDec = new RefEmploymentInfoSource();
      traineeDec.setId(2L);
      traineeDec.setSourceCode("TRAINEE_DECLARATION");
      traineeDec.setIsSelfReportedFlag(true);

      RefEmploymentInfoSource placementConv = new RefEmploymentInfoSource();
      placementConv.setId(1L);
      placementConv.setSourceCode("PLACEMENT_CONVERSION");
      placementConv.setIsSelfReportedFlag(false);

      RefEmploymentInfoSource employerConf = new RefEmploymentInfoSource();
      employerConf.setId(3L);
      employerConf.setSourceCode("EMPLOYER_CONFIRMATION");
      employerConf.setIsSelfReportedFlag(false);

      assertThat(traineeDec.getIsSelfReportedFlag()).isTrue();
      assertThat(placementConv.getIsSelfReportedFlag()).isFalse();
      assertThat(employerConf.getIsSelfReportedFlag()).isFalse();
    }
  }

  @Nested
  @DisplayName("5. Analytics Reuse & Direct View Mapping")
  class AnalyticsViewReuseTests {

    private final TraineeOutcomeSummaryMapper mapper = new TraineeOutcomeSummaryMapper();

    @Test
    @DisplayName("TraineeOutcomeSummaryMapper maps view entity without recalculating or altering counts")
    void testTraineeOutcomeSummaryMapper_faithfulMapping() {
      TraineeOutcomeSummary viewEntity = new TraineeOutcomeSummary();
      viewEntity.setTraineeId(500L);
      viewEntity.setStateId(1L);
      viewEntity.setDistrictId(10L);
      viewEntity.setCurrentEmploymentStatusId(8L);
      viewEntity.setSnapshotEmploymentStatusCode("UNKNOWN");
      viewEntity.setSnapshotIsEmployedFlag(false);
      viewEntity.setEnrollmentCount(2L);
      viewEntity.setCompletedEnrollmentCount(BigDecimal.valueOf(1));
      viewEntity.setCertifiedEnrollmentCount(BigDecimal.valueOf(1));
      viewEntity.setPlacedEnrollmentCount(BigDecimal.valueOf(1));
      viewEntity.setEmployedEnrollmentCount(BigDecimal.valueOf(1));
      viewEntity.setEmploymentSpellCount(1L);
      viewEntity.setCurrentEmploymentCount(BigDecimal.valueOf(1));
      viewEntity.setCurrentWageEmploymentCount(BigDecimal.valueOf(1));
      viewEntity.setCurrentSelfEmploymentCount(BigDecimal.ZERO);
      viewEntity.setUnemploymentPeriodCount(0L);
      viewEntity.setCurrentUnemploymentCount(BigDecimal.ZERO);
      viewEntity.setCurrentSkillGapCount(0L);
      viewEntity.setCompletionRatePct(BigDecimal.valueOf(50.00));

      TraineeOutcomeSummaryResponse response = mapper.toResponse(viewEntity);

      assertThat(response).isNotNull();
      assertThat(response.traineeId()).isEqualTo(500L);
      assertThat(response.snapshotEmploymentStatusCode()).isEqualTo("UNKNOWN");
      assertThat(response.snapshotIsEmployedFlag()).isFalse();
      assertThat(response.enrollmentCount()).isEqualTo(2L);
      assertThat(response.completedEnrollmentCount()).isEqualByComparingTo("1");
      assertThat(response.currentEmploymentCount()).isEqualByComparingTo("1");
      assertThat(response.completionRatePct()).isEqualByComparingTo("50.00");
    }

    @Test
    @DisplayName("When trainee has no enrollments, completionRatePct is null and counts are zero")
    void testTraineeOutcomeSummaryMapper_zeroEnrollments() {
      TraineeOutcomeSummary viewEntity = new TraineeOutcomeSummary();
      viewEntity.setTraineeId(600L);
      viewEntity.setStateId(1L);
      viewEntity.setDistrictId(10L);
      viewEntity.setCurrentEmploymentStatusId(2L);
      viewEntity.setSnapshotEmploymentStatusCode("IN_TRAINING");
      viewEntity.setSnapshotIsEmployedFlag(false);
      viewEntity.setEnrollmentCount(0L);
      viewEntity.setCompletedEnrollmentCount(BigDecimal.ZERO);
      viewEntity.setCertifiedEnrollmentCount(BigDecimal.ZERO);
      viewEntity.setPlacedEnrollmentCount(BigDecimal.ZERO);
      viewEntity.setEmployedEnrollmentCount(BigDecimal.ZERO);
      viewEntity.setEmploymentSpellCount(0L);
      viewEntity.setCurrentEmploymentCount(BigDecimal.ZERO);
      viewEntity.setCurrentWageEmploymentCount(BigDecimal.ZERO);
      viewEntity.setCurrentSelfEmploymentCount(BigDecimal.ZERO);
      viewEntity.setUnemploymentPeriodCount(0L);
      viewEntity.setCurrentUnemploymentCount(BigDecimal.ZERO);
      viewEntity.setCurrentSkillGapCount(0L);
      viewEntity.setCompletionRatePct(null);

      TraineeOutcomeSummaryResponse response = mapper.toResponse(viewEntity);

      assertThat(response).isNotNull();
      assertThat(response.enrollmentCount()).isZero();
      assertThat(response.completionRatePct()).isNull();
      assertThat(response.snapshotEmploymentStatusCode()).isEqualTo("IN_TRAINING");
    }
  }
}
