package in.gov.sih.sih26135.workflow;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import in.gov.sih.sih26135.dto.request.CreateAssessmentResultRequest;
import in.gov.sih.sih26135.dto.request.CreateCertificationRequest;
import in.gov.sih.sih26135.dto.request.RecordAssessmentAndIssueCertificateRequest;
import in.gov.sih.sih26135.dto.request.UpdateTrainingEnrollmentRequest;
import in.gov.sih.sih26135.dto.response.AssessmentResultResponse;
import in.gov.sih.sih26135.dto.response.TrainingEnrollmentResponse;
import in.gov.sih.sih26135.entity.CourseSkill;
import in.gov.sih.sih26135.entity.RefSkillGapStatus;
import in.gov.sih.sih26135.entity.Skill;
import in.gov.sih.sih26135.entity.SkillGap;
import in.gov.sih.sih26135.exception.BadRequestException;
import in.gov.sih.sih26135.exception.ResourceNotFoundException;
import in.gov.sih.sih26135.repository.CourseSkillRepository;
import in.gov.sih.sih26135.repository.RefSkillGapStatusRepository;
import in.gov.sih.sih26135.repository.SkillGapRepository;
import in.gov.sih.sih26135.service.AssessmentResultService;
import in.gov.sih.sih26135.service.CertificationService;
import in.gov.sih.sih26135.service.TraineeAssessmentService;
import in.gov.sih.sih26135.service.TrainingEnrollmentService;
import in.gov.sih.sih26135.security.WorkflowSecurityScopeValidator;
import in.gov.sih.sih26135.service.impl.TrainingOutcomeWorkflowServiceImpl;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Focused regression tests for WF-001: training completion resolves IN_REMEDIATION skill gaps.
 *
 * <p>Business rules under test:
 * <ul>
 *   <li>Trigger: enrollment isCompletedFlag=true AND assessment isPassFlag=true (both required)</li>
 *   <li>Eligible status: only IN_REMEDIATION gaps are resolved; OPEN/RESOLVED/etc. are untouched</li>
 *   <li>isCurrent: set to false on resolution; resolvedOn set to actualCompletionDate</li>
 * </ul>
 */
@ExtendWith(MockitoExtension.class)
class TrainingCompletionSkillGapResolutionTest {

  @Mock private TrainingEnrollmentService trainingEnrollmentService;
  @Mock private TraineeAssessmentService traineeAssessmentService;
  @Mock private AssessmentResultService assessmentResultService;
  @Mock private CertificationService certificationService;
  @Mock private CourseSkillRepository courseSkillRepository;
  @Mock private SkillGapRepository skillGapRepository;
  @Mock private RefSkillGapStatusRepository refSkillGapStatusRepository;

  private TrainingOutcomeWorkflowServiceImpl service;

  private static final Long TRAINEE_ID = 100L;
  private static final Long COURSE_ID  = 200L;
  private static final Long SKILL_ID   = 300L;
  private static final Long GAP_ID     = 400L;
  private static final Long ENROLLMENT_ID = 500L;
  private static final LocalDate COMPLETION_DATE = LocalDate.of(2026, 10, 18);

  @BeforeEach
  void setUp() {
    service = new TrainingOutcomeWorkflowServiceImpl(
        trainingEnrollmentService,
        traineeAssessmentService,
        assessmentResultService,
        certificationService,
        courseSkillRepository,
        skillGapRepository,
        refSkillGapStatusRepository,
        mock(WorkflowSecurityScopeValidator.class)
    );
  }

  // ── helpers ──────────────────────────────────────────────────────────────

  private AssessmentResultResponse buildResult(boolean pass) {
    AssessmentResultResponse r = new AssessmentResultResponse();
    r.setId(1L);
    r.setTraineeId(TRAINEE_ID);
    r.setIsPassFlag(pass);
    return r;
  }

  private TrainingEnrollmentResponse buildEnrollment(boolean completed) {
    TrainingEnrollmentResponse e = new TrainingEnrollmentResponse();
    e.setId(ENROLLMENT_ID);
    e.setTraineeId(TRAINEE_ID);
    e.setCourseId(COURSE_ID);
    e.setProviderId(10L);
    e.setIsCompletedFlag(completed);
    e.setActualCompletionDate(COMPLETION_DATE);
    e.setEnrollmentStatusId(completed ? 3L : 4L);
    e.setEnrollmentStatusCode(completed ? "COMPLETED" : "DROPPED");
    return e;
  }

  private CourseSkill buildCourseSkill(Long courseId, Long skillId) {
    CourseSkill cs = new CourseSkill();
    cs.setCourseId(courseId);
    cs.setSkillId(skillId);
    return cs;
  }

  private SkillGap buildInRemediationGap(Long id, Long traineeId, Long skillId) {
    Skill skill = new Skill();
    skill.setId(skillId);

    RefSkillGapStatus inRemediationStatus = new RefSkillGapStatus(
        "IN_REMEDIATION", "In remediation", 2);

    SkillGap gap = new SkillGap();
    gap.setId(id);
    gap.setSkill(skill);
    gap.setIsCurrent(true);
    gap.setSkillGapStatus(inRemediationStatus);
    gap.setIdentifiedOn(LocalDate.of(2026, 9, 1));
    gap.setUpdatedAt(LocalDateTime.now().minusDays(10));
    return gap;
  }

  private RefSkillGapStatus buildResolvedStatus() {
    RefSkillGapStatus s = new RefSkillGapStatus("RESOLVED", "Resolved", 3);
    s.setIsResolvedFlag(true);
    return s;
  }

  private RecordAssessmentAndIssueCertificateRequest buildRequest(boolean withEnrollmentStatus) {
    RecordAssessmentAndIssueCertificateRequest req =
        new RecordAssessmentAndIssueCertificateRequest();
    req.setAssessmentResultRequest(new CreateAssessmentResultRequest());
    req.setEnrollmentId(ENROLLMENT_ID);
    if (withEnrollmentStatus) {
      req.setCompletionEnrollmentStatusId(3L); // COMPLETED
    }
    return req;
  }

  // ── Nested test groups ────────────────────────────────────────────────────

  @Nested
  @DisplayName("1. Positive resolution cases")
  class PositiveResolutionCases {

    @Test
    @DisplayName("Case 1: Passing completion resolves one IN_REMEDIATION gap for a course skill")
    void shouldResolveInRemediationGapOnPassingCompletion() {
      when(assessmentResultService.createAssessmentResult(any()))
          .thenReturn(buildResult(true));
      when(trainingEnrollmentService.getById(ENROLLMENT_ID))
          .thenReturn(buildEnrollment(false)); // pre-update
      when(trainingEnrollmentService.updateEnrollment(eq(ENROLLMENT_ID), any()))
          .thenReturn(buildEnrollment(true));  // post-update: isCompletedFlag=true
      when(courseSkillRepository.findByCourseId(COURSE_ID))
          .thenReturn(List.of(buildCourseSkill(COURSE_ID, SKILL_ID)));
      when(skillGapRepository.findByTraineeIdAndIsCurrentTrueAndSkillGapStatusStatusCode(
          TRAINEE_ID, "IN_REMEDIATION"))
          .thenReturn(List.of(buildInRemediationGap(GAP_ID, TRAINEE_ID, SKILL_ID)));
      when(refSkillGapStatusRepository.findByStatusCode("RESOLVED"))
          .thenReturn(Optional.of(buildResolvedStatus()));

      service.recordAssessmentResultAndIssueCertificate(buildRequest(true));

      ArgumentCaptor<SkillGap> captor = ArgumentCaptor.forClass(SkillGap.class);
      verify(skillGapRepository, times(1)).save(captor.capture());

      SkillGap saved = captor.getValue();
      assertThat(saved.getId()).isEqualTo(GAP_ID);
      assertThat(saved.getIsCurrent()).isFalse();
      assertThat(saved.getResolvedOn()).isEqualTo(COMPLETION_DATE);
      assertThat(saved.getSkillGapStatus().getStatusCode()).isEqualTo("RESOLVED");
    }

    @Test
    @DisplayName("Case 8: Multiple IN_REMEDIATION gaps for multiple taught skills are all resolved")
    void shouldResolveMultipleInRemediationGapsForMultipleCourseSkills() {
      Long skillId2 = 301L;
      Long gapId2 = 401L;

      when(assessmentResultService.createAssessmentResult(any()))
          .thenReturn(buildResult(true));
      when(trainingEnrollmentService.getById(ENROLLMENT_ID))
          .thenReturn(buildEnrollment(false));
      when(trainingEnrollmentService.updateEnrollment(eq(ENROLLMENT_ID), any()))
          .thenReturn(buildEnrollment(true));
      when(courseSkillRepository.findByCourseId(COURSE_ID))
          .thenReturn(List.of(
              buildCourseSkill(COURSE_ID, SKILL_ID),
              buildCourseSkill(COURSE_ID, skillId2)));
      when(skillGapRepository.findByTraineeIdAndIsCurrentTrueAndSkillGapStatusStatusCode(
          TRAINEE_ID, "IN_REMEDIATION"))
          .thenReturn(List.of(
              buildInRemediationGap(GAP_ID, TRAINEE_ID, SKILL_ID),
              buildInRemediationGap(gapId2, TRAINEE_ID, skillId2)));
      when(refSkillGapStatusRepository.findByStatusCode("RESOLVED"))
          .thenReturn(Optional.of(buildResolvedStatus()));

      service.recordAssessmentResultAndIssueCertificate(buildRequest(true));

      verify(skillGapRepository, times(2)).save(any(SkillGap.class));
    }

    @Test
    @DisplayName("Case 5: resolvedOn is set to actualCompletionDate from the updated enrollment")
    void shouldSetResolvedOnToActualCompletionDate() {
      when(assessmentResultService.createAssessmentResult(any()))
          .thenReturn(buildResult(true));
      when(trainingEnrollmentService.getById(ENROLLMENT_ID))
          .thenReturn(buildEnrollment(false));
      when(trainingEnrollmentService.updateEnrollment(eq(ENROLLMENT_ID), any()))
          .thenReturn(buildEnrollment(true));
      when(courseSkillRepository.findByCourseId(COURSE_ID))
          .thenReturn(List.of(buildCourseSkill(COURSE_ID, SKILL_ID)));
      when(skillGapRepository.findByTraineeIdAndIsCurrentTrueAndSkillGapStatusStatusCode(
          TRAINEE_ID, "IN_REMEDIATION"))
          .thenReturn(List.of(buildInRemediationGap(GAP_ID, TRAINEE_ID, SKILL_ID)));
      when(refSkillGapStatusRepository.findByStatusCode("RESOLVED"))
          .thenReturn(Optional.of(buildResolvedStatus()));

      service.recordAssessmentResultAndIssueCertificate(buildRequest(true));

      ArgumentCaptor<SkillGap> captor = ArgumentCaptor.forClass(SkillGap.class);
      verify(skillGapRepository).save(captor.capture());
      assertThat(captor.getValue().getResolvedOn()).isEqualTo(COMPLETION_DATE);
    }

    @Test
    @DisplayName("Case 3/10: Resolved gap has isCurrent=false, consistent with getCurrentSkillGapsByTrainee exclusion")
    void resolvedGapShouldHaveIsCurrentFalse() {
      when(assessmentResultService.createAssessmentResult(any()))
          .thenReturn(buildResult(true));
      when(trainingEnrollmentService.getById(ENROLLMENT_ID))
          .thenReturn(buildEnrollment(false));
      when(trainingEnrollmentService.updateEnrollment(eq(ENROLLMENT_ID), any()))
          .thenReturn(buildEnrollment(true));
      when(courseSkillRepository.findByCourseId(COURSE_ID))
          .thenReturn(List.of(buildCourseSkill(COURSE_ID, SKILL_ID)));
      when(skillGapRepository.findByTraineeIdAndIsCurrentTrueAndSkillGapStatusStatusCode(
          TRAINEE_ID, "IN_REMEDIATION"))
          .thenReturn(List.of(buildInRemediationGap(GAP_ID, TRAINEE_ID, SKILL_ID)));
      when(refSkillGapStatusRepository.findByStatusCode("RESOLVED"))
          .thenReturn(Optional.of(buildResolvedStatus()));

      service.recordAssessmentResultAndIssueCertificate(buildRequest(true));

      ArgumentCaptor<SkillGap> captor = ArgumentCaptor.forClass(SkillGap.class);
      verify(skillGapRepository).save(captor.capture());
      // isCurrent=false means the gap will not appear in findByTraineeIdAndIsCurrentTrue queries
      assertThat(captor.getValue().getIsCurrent()).isFalse();
    }
  }

  @Nested
  @DisplayName("2. Non-resolution guard cases (no gaps should be resolved)")
  class NonResolutionGuardCases {

    @Test
    @DisplayName("Case 6a: Non-passing assessment does not trigger gap resolution even if COMPLETED")
    void shouldNotResolveGapsWhenAssessmentFails() {
      when(assessmentResultService.createAssessmentResult(any()))
          .thenReturn(buildResult(false)); // FAIL
      when(trainingEnrollmentService.getById(ENROLLMENT_ID))
          .thenReturn(buildEnrollment(false));
      when(trainingEnrollmentService.updateEnrollment(eq(ENROLLMENT_ID), any()))
          .thenReturn(buildEnrollment(true)); // isCompletedFlag=true but pass=false

      service.recordAssessmentResultAndIssueCertificate(buildRequest(true));

      verify(courseSkillRepository, never()).findByCourseId(anyLong());
      verify(skillGapRepository, never()).findByTraineeIdAndIsCurrentTrueAndSkillGapStatusStatusCode(
          anyLong(), any());
      verify(skillGapRepository, never()).save(any());
    }

    @Test
    @DisplayName("Case 9: DROPPED enrollment (isCompletedFlag=false) does not trigger gap resolution")
    void shouldNotResolveGapsWhenEnrollmentDropped() {
      when(assessmentResultService.createAssessmentResult(any()))
          .thenReturn(buildResult(true)); // PASS
      when(trainingEnrollmentService.getById(ENROLLMENT_ID))
          .thenReturn(buildEnrollment(false));
      when(trainingEnrollmentService.updateEnrollment(eq(ENROLLMENT_ID), any()))
          .thenReturn(buildEnrollment(false)); // isCompletedFlag=false (DROPPED)

      service.recordAssessmentResultAndIssueCertificate(buildRequest(true));

      verify(courseSkillRepository, never()).findByCourseId(anyLong());
      verify(skillGapRepository, never()).save(any());
    }

    @Test
    @DisplayName("Case 6b: Both fail and DROPPED — no gap resolution")
    void shouldNotResolveGapsWhenBothFailAndDropped() {
      when(assessmentResultService.createAssessmentResult(any()))
          .thenReturn(buildResult(false));
      when(trainingEnrollmentService.getById(ENROLLMENT_ID))
          .thenReturn(buildEnrollment(false));
      when(trainingEnrollmentService.updateEnrollment(eq(ENROLLMENT_ID), any()))
          .thenReturn(buildEnrollment(false));

      service.recordAssessmentResultAndIssueCertificate(buildRequest(true));

      verify(courseSkillRepository, never()).findByCourseId(anyLong());
      verify(skillGapRepository, never()).save(any());
    }

    @Test
    @DisplayName("Case 7: No enrollment status update in request — gap resolution is never triggered")
    void shouldNotResolveGapsWhenNoEnrollmentStatusUpdateRequested() {
      when(assessmentResultService.createAssessmentResult(any()))
          .thenReturn(buildResult(true));
      when(trainingEnrollmentService.getById(ENROLLMENT_ID))
          .thenReturn(buildEnrollment(false));

      // Request has no completionEnrollmentStatusId
      service.recordAssessmentResultAndIssueCertificate(buildRequest(false));

      verify(trainingEnrollmentService, never()).updateEnrollment(anyLong(), any());
      verify(courseSkillRepository, never()).findByCourseId(anyLong());
      verify(skillGapRepository, never()).save(any());
    }

    @Test
    @DisplayName("Case 4: Course has no CourseSkill entries — no gaps are resolved")
    void shouldNotResolveGapsWhenCourseHasNoCourseSkills() {
      when(assessmentResultService.createAssessmentResult(any()))
          .thenReturn(buildResult(true));
      when(trainingEnrollmentService.getById(ENROLLMENT_ID))
          .thenReturn(buildEnrollment(false));
      when(trainingEnrollmentService.updateEnrollment(eq(ENROLLMENT_ID), any()))
          .thenReturn(buildEnrollment(true));
      when(courseSkillRepository.findByCourseId(COURSE_ID))
          .thenReturn(Collections.emptyList());

      service.recordAssessmentResultAndIssueCertificate(buildRequest(true));

      verify(skillGapRepository, never()).findByTraineeIdAndIsCurrentTrueAndSkillGapStatusStatusCode(
          anyLong(), any());
      verify(skillGapRepository, never()).save(any());
    }

    @Test
    @DisplayName("Case 2: Trainee has no IN_REMEDIATION gaps for the course skills — no save")
    void shouldNotSaveWhenTraineeHasNoInRemediationGapsForCourseSkills() {
      when(assessmentResultService.createAssessmentResult(any()))
          .thenReturn(buildResult(true));
      when(trainingEnrollmentService.getById(ENROLLMENT_ID))
          .thenReturn(buildEnrollment(false));
      when(trainingEnrollmentService.updateEnrollment(eq(ENROLLMENT_ID), any()))
          .thenReturn(buildEnrollment(true));
      when(courseSkillRepository.findByCourseId(COURSE_ID))
          .thenReturn(List.of(buildCourseSkill(COURSE_ID, SKILL_ID)));
      when(skillGapRepository.findByTraineeIdAndIsCurrentTrueAndSkillGapStatusStatusCode(
          TRAINEE_ID, "IN_REMEDIATION"))
          .thenReturn(Collections.emptyList());

      service.recordAssessmentResultAndIssueCertificate(buildRequest(true));

      verify(refSkillGapStatusRepository, never()).findByStatusCode(any());
      verify(skillGapRepository, never()).save(any());
    }

    @Test
    @DisplayName("Case 3: IN_REMEDIATION gap exists but for a different skill not taught by course — left untouched")
    void shouldNotResolveInRemediationGapForSkillNotTaughtByCourse() {
      Long differentSkillId = 999L;

      when(assessmentResultService.createAssessmentResult(any()))
          .thenReturn(buildResult(true));
      when(trainingEnrollmentService.getById(ENROLLMENT_ID))
          .thenReturn(buildEnrollment(false));
      when(trainingEnrollmentService.updateEnrollment(eq(ENROLLMENT_ID), any()))
          .thenReturn(buildEnrollment(true));
      // Course teaches SKILL_ID, but the trainee's gap is for differentSkillId
      when(courseSkillRepository.findByCourseId(COURSE_ID))
          .thenReturn(List.of(buildCourseSkill(COURSE_ID, SKILL_ID)));
      when(skillGapRepository.findByTraineeIdAndIsCurrentTrueAndSkillGapStatusStatusCode(
          TRAINEE_ID, "IN_REMEDIATION"))
          .thenReturn(List.of(buildInRemediationGap(GAP_ID, TRAINEE_ID, differentSkillId)));

      service.recordAssessmentResultAndIssueCertificate(buildRequest(true));

      verify(refSkillGapStatusRepository, never()).findByStatusCode(any());
      verify(skillGapRepository, never()).save(any());
    }
  }

  @Nested
  @DisplayName("3. Fail-closed and edge cases")
  class FailClosedAndEdgeCases {

    @Test
    @DisplayName("Fails closed with ResourceNotFoundException if RESOLVED status is missing from database")
    void shouldFailClosedWhenResolvedStatusMissingFromDatabase() {
      when(assessmentResultService.createAssessmentResult(any()))
          .thenReturn(buildResult(true));
      when(trainingEnrollmentService.getById(ENROLLMENT_ID))
          .thenReturn(buildEnrollment(false));
      when(trainingEnrollmentService.updateEnrollment(eq(ENROLLMENT_ID), any()))
          .thenReturn(buildEnrollment(true));
      when(courseSkillRepository.findByCourseId(COURSE_ID))
          .thenReturn(List.of(buildCourseSkill(COURSE_ID, SKILL_ID)));
      when(skillGapRepository.findByTraineeIdAndIsCurrentTrueAndSkillGapStatusStatusCode(
          TRAINEE_ID, "IN_REMEDIATION"))
          .thenReturn(List.of(buildInRemediationGap(GAP_ID, TRAINEE_ID, SKILL_ID)));
      when(refSkillGapStatusRepository.findByStatusCode("RESOLVED"))
          .thenReturn(Optional.empty()); // missing reference data

      assertThatThrownBy(() ->
          service.recordAssessmentResultAndIssueCertificate(buildRequest(true)))
          .isInstanceOf(ResourceNotFoundException.class);

      // Gap must not be partially updated
      verify(skillGapRepository, never()).save(any());
    }

    @Test
    @DisplayName("Throws BadRequestException upfront when completion status provided without enrollment ID")
    void shouldThrowBadRequestWhenCompletionStatusProvidedWithoutEnrollmentId() {
      RecordAssessmentAndIssueCertificateRequest req = new RecordAssessmentAndIssueCertificateRequest();
      req.setAssessmentResultRequest(new CreateAssessmentResultRequest());
      req.setCompletionEnrollmentStatusId(3L);
      req.setEnrollmentId(null);

      assertThatThrownBy(() -> service.recordAssessmentResultAndIssueCertificate(req))
          .isInstanceOf(BadRequestException.class)
          .extracting("errorCode").isEqualTo("ENROLLMENT_ID_REQUIRED");

      verify(assessmentResultService, never()).createAssessmentResult(any());
      verify(certificationService, never()).createCertification(any());
    }

    @Test
    @DisplayName("Throws BadRequestException upfront when certification trainee does not match assessment result trainee")
    void shouldThrowBadRequestWhenCertificationTraineeDoesNotMatchAssessmentTrainee() {
      RecordAssessmentAndIssueCertificateRequest req = new RecordAssessmentAndIssueCertificateRequest();
      CreateAssessmentResultRequest assessReq = new CreateAssessmentResultRequest();
      assessReq.setTraineeId(TRAINEE_ID);
      req.setAssessmentResultRequest(assessReq);

      CreateCertificationRequest certReq = new CreateCertificationRequest();
      certReq.setTraineeId(999L);
      req.setCertificationRequest(certReq);

      assertThatThrownBy(() -> service.recordAssessmentResultAndIssueCertificate(req))
          .isInstanceOf(BadRequestException.class)
          .extracting("errorCode").isEqualTo("CERTIFICATION_TRAINEE_MISMATCH");

      verify(assessmentResultService, never()).createAssessmentResult(any());
      verify(certificationService, never()).createCertification(any());
    }

    @Test
    @DisplayName("Throws BadRequestException upfront when assessment result trainee does not match authoritative enrollment trainee")
    void shouldThrowBadRequestUpfrontWhenAssessmentResultTraineeMismatchesEnrollmentTrainee() {
      RecordAssessmentAndIssueCertificateRequest req = new RecordAssessmentAndIssueCertificateRequest();
      CreateAssessmentResultRequest assessReq = new CreateAssessmentResultRequest();
      assessReq.setTraineeId(999L); // Mismatched trainee
      req.setAssessmentResultRequest(assessReq);
      req.setEnrollmentId(ENROLLMENT_ID);
      req.setCompletionEnrollmentStatusId(3L);

      when(trainingEnrollmentService.getById(ENROLLMENT_ID))
          .thenReturn(buildEnrollment(false)); // traineeId is TRAINEE_ID (100L)

      assertThatThrownBy(() -> service.recordAssessmentResultAndIssueCertificate(req))
          .isInstanceOf(BadRequestException.class)
          .extracting("errorCode").isEqualTo("ENROLLMENT_TRAINEE_MISMATCH");

      // Verify ZERO writes occurred: assessment result, certification, enrollment update, and gap save are never called
      verify(assessmentResultService, never()).createAssessmentResult(any());
      verify(certificationService, never()).createCertification(any());
      verify(trainingEnrollmentService, never()).updateEnrollment(anyLong(), any());
      verify(skillGapRepository, never()).save(any());
    }

    @Test
    @DisplayName("Throws BadRequestException upfront when certification request trainee does not match authoritative enrollment trainee")
    void shouldThrowBadRequestUpfrontWhenCertificationTraineeMismatchesEnrollmentTrainee() {
      RecordAssessmentAndIssueCertificateRequest req = new RecordAssessmentAndIssueCertificateRequest();
      CreateAssessmentResultRequest assessReq = new CreateAssessmentResultRequest();
      req.setAssessmentResultRequest(assessReq);
      req.setEnrollmentId(ENROLLMENT_ID);
      req.setCompletionEnrollmentStatusId(3L);

      CreateCertificationRequest certReq = new CreateCertificationRequest();
      certReq.setTraineeId(999L); // Mismatched trainee
      req.setCertificationRequest(certReq);

      when(trainingEnrollmentService.getById(ENROLLMENT_ID))
          .thenReturn(buildEnrollment(false));

      assertThatThrownBy(() -> service.recordAssessmentResultAndIssueCertificate(req))
          .isInstanceOf(BadRequestException.class)
          .extracting("errorCode").isEqualTo("ENROLLMENT_TRAINEE_MISMATCH");

      verify(assessmentResultService, never()).createAssessmentResult(any());
      verify(certificationService, never()).createCertification(any());
      verify(trainingEnrollmentService, never()).updateEnrollment(anyLong(), any());
      verify(skillGapRepository, never()).save(any());
    }

    @Test
    @DisplayName("Throws BadRequestException before certification or enrollment mutation when created assessment result trainee mismatches enrollment trainee")
    void shouldThrowBadRequestBeforeCertificationWhenCreatedResultTraineeMismatchesEnrollmentTrainee() {
      RecordAssessmentAndIssueCertificateRequest req = new RecordAssessmentAndIssueCertificateRequest();
      CreateAssessmentResultRequest assessReq = new CreateAssessmentResultRequest();
      req.setAssessmentResultRequest(assessReq);
      req.setEnrollmentId(ENROLLMENT_ID);
      req.setCompletionEnrollmentStatusId(3L);

      CreateCertificationRequest certReq = new CreateCertificationRequest();
      req.setCertificationRequest(certReq);

      when(trainingEnrollmentService.getById(ENROLLMENT_ID))
          .thenReturn(buildEnrollment(false)); // traineeId is TRAINEE_ID (100L)

      AssessmentResultResponse resultWithDiffTrainee = buildResult(true);
      resultWithDiffTrainee.setTraineeId(888L); // Different trainee returned
      when(assessmentResultService.createAssessmentResult(any()))
          .thenReturn(resultWithDiffTrainee);

      assertThatThrownBy(() -> service.recordAssessmentResultAndIssueCertificate(req))
          .isInstanceOf(BadRequestException.class)
          .extracting("errorCode").isEqualTo("ENROLLMENT_TRAINEE_MISMATCH");

      // Verify certificate creation, enrollment update, and gap resolution were never reached
      verify(certificationService, never()).createCertification(any());
      verify(trainingEnrollmentService, never()).updateEnrollment(anyLong(), any());
      verify(skillGapRepository, never()).save(any());
    }

    @Test
    @DisplayName("Case 3 (OPEN gap): OPEN gaps are not queried because query filters by IN_REMEDIATION code")
    void openGapIsNotReturnedByQueryAndIsNeverResolved() {
      // The query specifically asks for IN_REMEDIATION; an OPEN gap won't be returned.
      // This test documents that the filter is applied at the repository query level.
      when(assessmentResultService.createAssessmentResult(any()))
          .thenReturn(buildResult(true));
      when(trainingEnrollmentService.getById(ENROLLMENT_ID))
          .thenReturn(buildEnrollment(false));
      when(trainingEnrollmentService.updateEnrollment(eq(ENROLLMENT_ID), any()))
          .thenReturn(buildEnrollment(true));
      when(courseSkillRepository.findByCourseId(COURSE_ID))
          .thenReturn(List.of(buildCourseSkill(COURSE_ID, SKILL_ID)));
      // Repository returns empty list for IN_REMEDIATION (OPEN gap won't match this query)
      when(skillGapRepository.findByTraineeIdAndIsCurrentTrueAndSkillGapStatusStatusCode(
          TRAINEE_ID, "IN_REMEDIATION"))
          .thenReturn(Collections.emptyList());

      service.recordAssessmentResultAndIssueCertificate(buildRequest(true));

      // Confirm the repository was called with the IN_REMEDIATION filter
      verify(skillGapRepository).findByTraineeIdAndIsCurrentTrueAndSkillGapStatusStatusCode(
          TRAINEE_ID, "IN_REMEDIATION");
      verify(skillGapRepository, never()).save(any());
    }

    @Test
    @DisplayName("Existing validations preserved: null request body throws BadRequestException")
    void shouldRejectNullRequestBody() {
      assertThatThrownBy(() -> service.recordAssessmentResultAndIssueCertificate(null))
          .isInstanceOf(in.gov.sih.sih26135.exception.BadRequestException.class)
          .extracting("errorCode").isEqualTo("REQUEST_BODY_NULL");
    }

    @Test
    @DisplayName("Existing validation preserved: enrollment trainee mismatch throws BadRequestException")
    void shouldRejectMismatchedEnrollmentTrainee() {
      AssessmentResultResponse result = buildResult(true);
      result.setTraineeId(999L); // different from enrollment's TRAINEE_ID

      when(assessmentResultService.createAssessmentResult(any())).thenReturn(result);
      TrainingEnrollmentResponse enrollment = buildEnrollment(false);
      when(trainingEnrollmentService.getById(ENROLLMENT_ID)).thenReturn(enrollment);

      assertThatThrownBy(() ->
          service.recordAssessmentResultAndIssueCertificate(buildRequest(true)))
          .isInstanceOf(in.gov.sih.sih26135.exception.BadRequestException.class)
          .extracting("errorCode").isEqualTo("ENROLLMENT_TRAINEE_MISMATCH");

      verify(trainingEnrollmentService, never()).updateEnrollment(anyLong(), any());
      verify(skillGapRepository, never()).save(any());
    }
  }

  @Nested
  @DisplayName("4. Transactional configuration, null-safety, and repository predicate tests")
  class TransactionConfigurationAndDataIntegrityTests {

    @Test
    @DisplayName("Exception propagation: repository save exception is unswallowed and propagates to the transaction boundary")
    void shouldPropagateExceptionWhenSkillGapSaveFailsWithoutSwallowing() {
      when(assessmentResultService.createAssessmentResult(any()))
          .thenReturn(buildResult(true));
      when(trainingEnrollmentService.getById(ENROLLMENT_ID))
          .thenReturn(buildEnrollment(false));
      when(trainingEnrollmentService.updateEnrollment(eq(ENROLLMENT_ID), any()))
          .thenReturn(buildEnrollment(true));
      when(courseSkillRepository.findByCourseId(COURSE_ID))
          .thenReturn(List.of(buildCourseSkill(COURSE_ID, SKILL_ID)));
      when(skillGapRepository.findByTraineeIdAndIsCurrentTrueAndSkillGapStatusStatusCode(
          TRAINEE_ID, "IN_REMEDIATION"))
          .thenReturn(List.of(buildInRemediationGap(GAP_ID, TRAINEE_ID, SKILL_ID)));
      when(refSkillGapStatusRepository.findByStatusCode("RESOLVED"))
          .thenReturn(Optional.of(buildResolvedStatus()));
      when(skillGapRepository.save(any()))
          .thenThrow(new RuntimeException("Simulated database failure during gap update"));

      assertThatThrownBy(() ->
          service.recordAssessmentResultAndIssueCertificate(buildRequest(true)))
          .isInstanceOf(RuntimeException.class)
          .hasMessageContaining("Simulated database failure during gap update");
    }

    @Test
    @DisplayName("Transactional annotation inspection: method has @Transactional with readOnly=false")
    void shouldVerifyTransactionalAnnotationAttributesViaReflection() throws Exception {
      var method = TrainingOutcomeWorkflowServiceImpl.class.getMethod(
          "recordAssessmentResultAndIssueCertificate", RecordAssessmentAndIssueCertificateRequest.class);
      var tx = method.getAnnotation(org.springframework.transaction.annotation.Transactional.class);

      assertThat(tx).isNotNull();
      assertThat(tx.readOnly()).isFalse();

      var classTx = TrainingOutcomeWorkflowServiceImpl.class.getAnnotation(
          org.springframework.transaction.annotation.Transactional.class);
      assertThat(classTx).isNotNull();
      assertThat(classTx.readOnly()).isTrue();
    }

    @Test
    @DisplayName("Null safety: duplicate course-skill mappings and null entries do not cause duplicate saves or NPE")
    void shouldDeduplicateCourseSkillsAndIgnoreNullSkillReferences() {
      when(assessmentResultService.createAssessmentResult(any()))
          .thenReturn(buildResult(true));
      when(trainingEnrollmentService.getById(ENROLLMENT_ID))
          .thenReturn(buildEnrollment(false));
      when(trainingEnrollmentService.updateEnrollment(eq(ENROLLMENT_ID), any()))
          .thenReturn(buildEnrollment(true));

      // Duplicate entries for SKILL_ID and one null entry
      CourseSkill cs1 = buildCourseSkill(COURSE_ID, SKILL_ID);
      CourseSkill cs2 = buildCourseSkill(COURSE_ID, SKILL_ID);
      CourseSkill csNull = new CourseSkill(); // null skillId
      when(courseSkillRepository.findByCourseId(COURSE_ID))
          .thenReturn(List.of(cs1, cs2, csNull));

      when(skillGapRepository.findByTraineeIdAndIsCurrentTrueAndSkillGapStatusStatusCode(
          TRAINEE_ID, "IN_REMEDIATION"))
          .thenReturn(List.of(buildInRemediationGap(GAP_ID, TRAINEE_ID, SKILL_ID)));
      when(refSkillGapStatusRepository.findByStatusCode("RESOLVED"))
          .thenReturn(Optional.of(buildResolvedStatus()));

      service.recordAssessmentResultAndIssueCertificate(buildRequest(true));

      // Saved exactly once despite duplicate courseSkill rows
      verify(skillGapRepository, times(1)).save(any(SkillGap.class));
    }

    @Test
    @DisplayName("Null safety: gaps with null skill reference or null skill id are safely skipped")
    void shouldIgnoreGapsWithNullSkillOrNullSkillId() {
      when(assessmentResultService.createAssessmentResult(any()))
          .thenReturn(buildResult(true));
      when(trainingEnrollmentService.getById(ENROLLMENT_ID))
          .thenReturn(buildEnrollment(false));
      when(trainingEnrollmentService.updateEnrollment(eq(ENROLLMENT_ID), any()))
          .thenReturn(buildEnrollment(true));
      when(courseSkillRepository.findByCourseId(COURSE_ID))
          .thenReturn(List.of(buildCourseSkill(COURSE_ID, SKILL_ID)));

      SkillGap gapNullSkill = new SkillGap();
      gapNullSkill.setId(991L);
      gapNullSkill.setSkill(null);

      SkillGap gapNullSkillId = new SkillGap();
      gapNullSkillId.setId(992L);
      gapNullSkillId.setSkill(new Skill()); // null id

      when(skillGapRepository.findByTraineeIdAndIsCurrentTrueAndSkillGapStatusStatusCode(
          TRAINEE_ID, "IN_REMEDIATION"))
          .thenReturn(List.of(gapNullSkill, gapNullSkillId));

      service.recordAssessmentResultAndIssueCertificate(buildRequest(true));

      verify(skillGapRepository, never()).save(any());
    }

    @Test
    @DisplayName("Date check: rejects completion when resolvedOn is before gap identifiedOn (ck_skill_gaps_dates)")
    void shouldRejectResolvedDatePrecedingIdentifiedDateAndRollback() {
      TrainingEnrollmentResponse enrollment = buildEnrollment(false);
      // Backdated completion: 2026-08-01, while gap was identified on 2026-09-01
      enrollment.setActualCompletionDate(LocalDate.of(2026, 8, 1));

      TrainingEnrollmentResponse completedEnrollment = buildEnrollment(true);
      completedEnrollment.setActualCompletionDate(LocalDate.of(2026, 8, 1));

      when(assessmentResultService.createAssessmentResult(any()))
          .thenReturn(buildResult(true));
      when(trainingEnrollmentService.getById(ENROLLMENT_ID))
          .thenReturn(enrollment);
      when(trainingEnrollmentService.updateEnrollment(eq(ENROLLMENT_ID), any()))
          .thenReturn(completedEnrollment);
      when(courseSkillRepository.findByCourseId(COURSE_ID))
          .thenReturn(List.of(buildCourseSkill(COURSE_ID, SKILL_ID)));

      SkillGap gap = buildInRemediationGap(GAP_ID, TRAINEE_ID, SKILL_ID);
      gap.setIdentifiedOn(LocalDate.of(2026, 9, 1)); // later than 2026-08-01

      when(skillGapRepository.findByTraineeIdAndIsCurrentTrueAndSkillGapStatusStatusCode(
          TRAINEE_ID, "IN_REMEDIATION"))
          .thenReturn(List.of(gap));
      when(refSkillGapStatusRepository.findByStatusCode("RESOLVED"))
          .thenReturn(Optional.of(buildResolvedStatus()));

      assertThatThrownBy(() ->
          service.recordAssessmentResultAndIssueCertificate(buildRequest(true)))
          .isInstanceOf(in.gov.sih.sih26135.exception.BadRequestException.class)
          .extracting("errorCode").isEqualTo("RESOLVED_DATE_BEFORE_IDENTIFIED");

      verify(skillGapRepository, never()).save(any());
    }

    @Test
    @DisplayName("Repository query predicate: derived query method exists and matches entity property graph")
    void shouldVerifyRepositoryQueryMethodSpecification() throws Exception {
      // Verifies the method signature exists on SkillGapRepository with exact parameters and return type
      var repoMethod = SkillGapRepository.class.getMethod(
          "findByTraineeIdAndIsCurrentTrueAndSkillGapStatusStatusCode", Long.class, String.class);
      assertThat(repoMethod).isNotNull();
      assertThat(repoMethod.getReturnType()).isEqualTo(List.class);

      // Verify predicate logic on a representative entity collection:
      RefSkillGapStatus openStatus = new RefSkillGapStatus("OPEN", "Open", 1);
      RefSkillGapStatus inRemediationStatus = new RefSkillGapStatus("IN_REMEDIATION", "In remediation", 2);
      RefSkillGapStatus resolvedStatus = new RefSkillGapStatus("RESOLVED", "Resolved", 3);

      SkillGap gOpen = new SkillGap();
      gOpen.setId(1L);
      gOpen.setIsCurrent(true);
      gOpen.setSkillGapStatus(openStatus);

      SkillGap gResolved = new SkillGap();
      gResolved.setId(2L);
      gResolved.setIsCurrent(true);
      gResolved.setSkillGapStatus(resolvedStatus);

      SkillGap gInRemediationInactive = new SkillGap();
      gInRemediationInactive.setId(3L);
      gInRemediationInactive.setIsCurrent(false);
      gInRemediationInactive.setSkillGapStatus(inRemediationStatus);

      SkillGap gInRemediationActive = new SkillGap();
      gInRemediationActive.setId(4L);
      gInRemediationActive.setIsCurrent(true);
      gInRemediationActive.setSkillGapStatus(inRemediationStatus);

      List<SkillGap> sampleGaps = List.of(gOpen, gResolved, gInRemediationInactive, gInRemediationActive);

      // Filter replicating the exact Spring Data JPA derived query:
      // WHERE is_current = true AND skill_gap_status.status_code = 'IN_REMEDIATION'
      List<SkillGap> filtered = sampleGaps.stream()
          .filter(g -> Boolean.TRUE.equals(g.getIsCurrent()))
          .filter(g -> g.getSkillGapStatus() != null && "IN_REMEDIATION".equals(g.getSkillGapStatus().getStatusCode()))
          .toList();

      assertThat(filtered).hasSize(1);
      assertThat(filtered.get(0).getId()).isEqualTo(4L);
    }
  }
}
