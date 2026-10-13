package in.gov.sih.sih26135.workflow;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import in.gov.sih.sih26135.dto.request.CreateSkillGapAssessmentRequest;
import in.gov.sih.sih26135.dto.request.CreateSkillGapRecommendationRequest;
import in.gov.sih.sih26135.dto.request.CreateSkillGapRequest;
import in.gov.sih.sih26135.dto.request.CreateTrainingEnrollmentRequest;
import in.gov.sih.sih26135.dto.request.RecordSkillGapAssessmentWithGapsRequest;
import in.gov.sih.sih26135.dto.request.SkillGapRemediationEnrollmentRequest;
import in.gov.sih.sih26135.dto.response.RecordSkillGapAssessmentWithGapsResponse;
import in.gov.sih.sih26135.dto.response.SkillGapAssessmentResponse;
import in.gov.sih.sih26135.dto.response.SkillGapRecommendationResponse;
import in.gov.sih.sih26135.dto.response.SkillGapRemediationEnrollmentResponse;
import in.gov.sih.sih26135.dto.response.SkillGapResponse;
import in.gov.sih.sih26135.dto.response.TraineeSkillGapProfileResponse;
import in.gov.sih.sih26135.dto.response.TrainingEnrollmentResponse;
import in.gov.sih.sih26135.exception.BadRequestException;
import in.gov.sih.sih26135.exception.ResourceNotFoundException;
import in.gov.sih.sih26135.service.SkillGapAssessmentService;
import in.gov.sih.sih26135.service.SkillGapRecommendationService;
import in.gov.sih.sih26135.service.SkillGapService;
import in.gov.sih.sih26135.service.TrainingEnrollmentService;
import in.gov.sih.sih26135.service.impl.SkillGapRemediationWorkflowServiceImpl;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class SkillGapRemediationWorkflowAssociationTest {

  @Mock private SkillGapAssessmentService skillGapAssessmentService;
  @Mock private SkillGapService skillGapService;
  @Mock private SkillGapRecommendationService skillGapRecommendationService;
  @Mock private TrainingEnrollmentService trainingEnrollmentService;

  private SkillGapRemediationWorkflowServiceImpl workflowService;

  private final Long ASSESSMENT_ID = 10L;
  private final Long TRAINEE_ID = 100L;

  @BeforeEach
  void setUp() {
    workflowService = new SkillGapRemediationWorkflowServiceImpl(
        skillGapAssessmentService,
        skillGapService,
        skillGapRecommendationService,
        trainingEnrollmentService
    );
  }

  private SkillGapAssessmentResponse buildAssessment(Long id, Long traineeId) {
    return new SkillGapAssessmentResponse(
        id, "SGA-2026-001", traineeId, "TR-001", "Aditya", "Sharma",
        null, null, null, null, null, null, null, null, null, null,
        null, null, null, null, null, null, null, null, null, null,
        null, null, null, null, null, null, null, null, null, null,
        null, LocalDate.now(), 1L, "Initial assessment notes",
        LocalDateTime.now().minusDays(1), LocalDateTime.now()
    );
  }

  private SkillGapResponse buildSkillGap(Long id, Long assessmentId, Long traineeId, Long skillId) {
    return new SkillGapResponse(
        id, "SG-2026-" + id, assessmentId, "SGA-2026-001", traineeId,
        "TR-001", "Aditya", "Sharma", skillId, "SK-" + skillId, "Skill " + skillId,
        1L, "BEG", "Beginner", 1, 3L, "ADV", "Advanced", 3,
        4L, "EXP", "Expert", 4, 2, 1L, "HIGH", "High",
        2L, "MOD", "Moderate", 2, 1L, "OPEN", "Open",
        1L, "ASSESSMENT", "Assessment", true, 1,
        LocalDate.now().minusDays(5), null, "Identified gap",
        LocalDateTime.now().minusDays(5), LocalDateTime.now()
    );
  }

  private SkillGapRecommendationResponse buildRecommendationResponse(
      Long id, Long skillGapId, Long courseId) {
    return new SkillGapRecommendationResponse(
        id, skillGapId, "SG-2026-" + skillGapId, 1, 1L, "UPSKILL", "Upskill",
        courseId, "CRS-" + courseId, "Course " + courseId,
        null, null, null, false, null, "Recommended upskilling",
        1L, LocalDateTime.now().minusDays(1), LocalDateTime.now()
    );
  }

  @Nested
  @DisplayName("1. Valid Skill-Gap Association Tests")
  class ValidAssociationTests {

    @Test
    @DisplayName("Recommendation with explicit valid gap ID links accurately to that exact gap")
    void shouldAssociateRecommendationWithExplicitValidGapId() {
      RecordSkillGapAssessmentWithGapsRequest request = new RecordSkillGapAssessmentWithGapsRequest();
      request.setAssessmentRequest(new CreateSkillGapAssessmentRequest());

      CreateSkillGapRequest gapReq1 = new CreateSkillGapRequest();
      gapReq1.setSkillId(101L);
      request.setGapRequests(List.of(gapReq1));

      CreateSkillGapRecommendationRequest recReq = new CreateSkillGapRecommendationRequest();
      recReq.setSkillGapId(201L);
      recReq.setRecommendedCourseId(501L);
      request.setRecommendationRequests(List.of(recReq));

      when(skillGapAssessmentService.createAssessment(any())).thenReturn(buildAssessment(ASSESSMENT_ID, TRAINEE_ID));
      when(skillGapService.createSkillGap(any())).thenReturn(buildSkillGap(201L, ASSESSMENT_ID, TRAINEE_ID, 101L));
      when(skillGapRecommendationService.createRecommendation(any()))
          .thenReturn(buildRecommendationResponse(1001L, 201L, 501L));

      RecordSkillGapAssessmentWithGapsResponse response =
          workflowService.recordAssessmentWithGapsAndRecommendations(request);

      assertThat(response).isNotNull();
      assertThat(response.skillGaps()).hasSize(1);
      assertThat(response.recommendations()).hasSize(1);

      ArgumentCaptor<CreateSkillGapRecommendationRequest> captor =
          ArgumentCaptor.forClass(CreateSkillGapRecommendationRequest.class);
      verify(skillGapRecommendationService).createRecommendation(captor.capture());
      assertThat(captor.getValue().getSkillGapId()).isEqualTo(201L);
    }

    @Test
    @DisplayName("Multiple recommendations with distinct authoritative gap IDs are linked correctly")
    void shouldAssociateMultipleRecommendationsWithDistinctAuthoritativeGapIds() {
      RecordSkillGapAssessmentWithGapsRequest request = new RecordSkillGapAssessmentWithGapsRequest();
      request.setAssessmentRequest(new CreateSkillGapAssessmentRequest());

      CreateSkillGapRequest gapReq1 = new CreateSkillGapRequest();
      gapReq1.setSkillId(101L);
      CreateSkillGapRequest gapReq2 = new CreateSkillGapRequest();
      gapReq2.setSkillId(102L);
      request.setGapRequests(List.of(gapReq1, gapReq2));

      CreateSkillGapRecommendationRequest recReq1 = new CreateSkillGapRecommendationRequest();
      recReq1.setSkillGapId(201L);
      recReq1.setRecommendedCourseId(501L);

      CreateSkillGapRecommendationRequest recReq2 = new CreateSkillGapRecommendationRequest();
      recReq2.setSkillGapId(202L);
      recReq2.setRecommendedCourseId(502L);

      request.setRecommendationRequests(List.of(recReq1, recReq2));

      when(skillGapAssessmentService.createAssessment(any())).thenReturn(buildAssessment(ASSESSMENT_ID, TRAINEE_ID));
      when(skillGapService.createSkillGap(any()))
          .thenReturn(buildSkillGap(201L, ASSESSMENT_ID, TRAINEE_ID, 101L))
          .thenReturn(buildSkillGap(202L, ASSESSMENT_ID, TRAINEE_ID, 102L));
      when(skillGapRecommendationService.createRecommendation(any()))
          .thenReturn(buildRecommendationResponse(1001L, 201L, 501L))
          .thenReturn(buildRecommendationResponse(1002L, 202L, 502L));

      RecordSkillGapAssessmentWithGapsResponse response =
          workflowService.recordAssessmentWithGapsAndRecommendations(request);

      assertThat(response.recommendations()).hasSize(2);
      ArgumentCaptor<CreateSkillGapRecommendationRequest> captor =
          ArgumentCaptor.forClass(CreateSkillGapRecommendationRequest.class);
      verify(skillGapRecommendationService, times(2)).createRecommendation(captor.capture());

      List<CreateSkillGapRecommendationRequest> captured = captor.getAllValues();
      assertThat(captured.get(0).getSkillGapId()).isEqualTo(201L);
      assertThat(captured.get(1).getSkillGapId()).isEqualTo(202L);
    }

    @Test
    @DisplayName("Multiple recommendations targeting the same gap are associated with that gap")
    void shouldAllowMultipleRecommendationsTargetingSameGap() {
      RecordSkillGapAssessmentWithGapsRequest request = new RecordSkillGapAssessmentWithGapsRequest();
      request.setAssessmentRequest(new CreateSkillGapAssessmentRequest());

      CreateSkillGapRequest gapReq1 = new CreateSkillGapRequest();
      gapReq1.setSkillId(101L);
      request.setGapRequests(List.of(gapReq1));

      CreateSkillGapRecommendationRequest recReq1 = new CreateSkillGapRecommendationRequest();
      recReq1.setSkillGapId(201L);
      recReq1.setRecommendedCourseId(501L);

      CreateSkillGapRecommendationRequest recReq2 = new CreateSkillGapRecommendationRequest();
      recReq2.setSkillGapId(201L);
      recReq2.setRecommendedCourseId(502L);

      request.setRecommendationRequests(List.of(recReq1, recReq2));

      when(skillGapAssessmentService.createAssessment(any())).thenReturn(buildAssessment(ASSESSMENT_ID, TRAINEE_ID));
      when(skillGapService.createSkillGap(any())).thenReturn(buildSkillGap(201L, ASSESSMENT_ID, TRAINEE_ID, 101L));
      when(skillGapRecommendationService.createRecommendation(any()))
          .thenReturn(buildRecommendationResponse(1001L, 201L, 501L))
          .thenReturn(buildRecommendationResponse(1002L, 201L, 502L));

      RecordSkillGapAssessmentWithGapsResponse response =
          workflowService.recordAssessmentWithGapsAndRecommendations(request);

      assertThat(response.recommendations()).hasSize(2);
      ArgumentCaptor<CreateSkillGapRecommendationRequest> captor =
          ArgumentCaptor.forClass(CreateSkillGapRecommendationRequest.class);
      verify(skillGapRecommendationService, times(2)).createRecommendation(captor.capture());

      List<CreateSkillGapRecommendationRequest> captured = captor.getAllValues();
      assertThat(captured.get(0).getSkillGapId()).isEqualTo(201L);
      assertThat(captured.get(1).getSkillGapId()).isEqualTo(201L);
    }

    @Test
    @DisplayName("When exactly one gap is created, recommendation without gap ID is unambiguously associated with it")
    void shouldAssociateRecommendationUnambiguouslyWhenSingleGapCreated() {
      RecordSkillGapAssessmentWithGapsRequest request = new RecordSkillGapAssessmentWithGapsRequest();
      request.setAssessmentRequest(new CreateSkillGapAssessmentRequest());

      CreateSkillGapRequest gapReq = new CreateSkillGapRequest();
      gapReq.setSkillId(101L);
      request.setGapRequests(List.of(gapReq));

      CreateSkillGapRecommendationRequest recReq = new CreateSkillGapRecommendationRequest();
      recReq.setSkillGapId(null); // omitted by caller in a single-gap assessment
      recReq.setRecommendedCourseId(501L);
      request.setRecommendationRequests(List.of(recReq));

      when(skillGapAssessmentService.createAssessment(any())).thenReturn(buildAssessment(ASSESSMENT_ID, TRAINEE_ID));
      when(skillGapService.createSkillGap(any())).thenReturn(buildSkillGap(201L, ASSESSMENT_ID, TRAINEE_ID, 101L));
      when(skillGapRecommendationService.createRecommendation(any()))
          .thenReturn(buildRecommendationResponse(1001L, 201L, 501L));

      RecordSkillGapAssessmentWithGapsResponse response =
          workflowService.recordAssessmentWithGapsAndRecommendations(request);

      assertThat(response.recommendations()).hasSize(1);
      ArgumentCaptor<CreateSkillGapRecommendationRequest> captor =
          ArgumentCaptor.forClass(CreateSkillGapRecommendationRequest.class);
      verify(skillGapRecommendationService).createRecommendation(captor.capture());
      assertThat(captor.getValue().getSkillGapId()).isEqualTo(201L);
    }

    @Test
    @DisplayName("Reordering recommendation list does not alter semantic associations when explicit IDs are present")
    void shouldPreserveAssociationIntegrityRegardlessOfRecommendationInputListOrder() {
      RecordSkillGapAssessmentWithGapsRequest request = new RecordSkillGapAssessmentWithGapsRequest();
      request.setAssessmentRequest(new CreateSkillGapAssessmentRequest());

      CreateSkillGapRequest gapReq1 = new CreateSkillGapRequest();
      gapReq1.setSkillId(101L);
      CreateSkillGapRequest gapReq2 = new CreateSkillGapRequest();
      gapReq2.setSkillId(102L);
      request.setGapRequests(List.of(gapReq1, gapReq2));

      // Reverse order: recommendation for gap 202 is first in the list
      CreateSkillGapRecommendationRequest recReqForGap2 = new CreateSkillGapRecommendationRequest();
      recReqForGap2.setSkillGapId(202L);
      recReqForGap2.setRecommendedCourseId(502L);

      CreateSkillGapRecommendationRequest recReqForGap1 = new CreateSkillGapRecommendationRequest();
      recReqForGap1.setSkillGapId(201L);
      recReqForGap1.setRecommendedCourseId(501L);

      request.setRecommendationRequests(List.of(recReqForGap2, recReqForGap1));

      when(skillGapAssessmentService.createAssessment(any())).thenReturn(buildAssessment(ASSESSMENT_ID, TRAINEE_ID));
      when(skillGapService.createSkillGap(any()))
          .thenReturn(buildSkillGap(201L, ASSESSMENT_ID, TRAINEE_ID, 101L))
          .thenReturn(buildSkillGap(202L, ASSESSMENT_ID, TRAINEE_ID, 102L));
      when(skillGapRecommendationService.createRecommendation(any()))
          .thenReturn(buildRecommendationResponse(1002L, 202L, 502L))
          .thenReturn(buildRecommendationResponse(1001L, 201L, 501L));

      workflowService.recordAssessmentWithGapsAndRecommendations(request);

      ArgumentCaptor<CreateSkillGapRecommendationRequest> captor =
          ArgumentCaptor.forClass(CreateSkillGapRecommendationRequest.class);
      verify(skillGapRecommendationService, times(2)).createRecommendation(captor.capture());

      List<CreateSkillGapRecommendationRequest> captured = captor.getAllValues();
      assertThat(captured.get(0).getSkillGapId()).isEqualTo(202L);
      assertThat(captured.get(1).getSkillGapId()).isEqualTo(201L);
    }
  }

  @Nested
  @DisplayName("2. Invalid Relationship and Boundary Guard Tests")
  class InvalidRelationshipTests {

    @Test
    @DisplayName("Fails closed when multiple skill gaps exist and recommendation omits skillGapId (WF-002 fix)")
    void shouldRejectUnresolvedRecommendationWhenMultipleGapsCreated() {
      RecordSkillGapAssessmentWithGapsRequest request = new RecordSkillGapAssessmentWithGapsRequest();
      request.setAssessmentRequest(new CreateSkillGapAssessmentRequest());

      CreateSkillGapRequest gapReq1 = new CreateSkillGapRequest();
      gapReq1.setSkillId(101L);
      CreateSkillGapRequest gapReq2 = new CreateSkillGapRequest();
      gapReq2.setSkillId(102L);
      request.setGapRequests(List.of(gapReq1, gapReq2));

      CreateSkillGapRecommendationRequest recReq = new CreateSkillGapRecommendationRequest();
      recReq.setSkillGapId(null); // ambiguous in multi-gap assessment
      request.setRecommendationRequests(List.of(recReq));

      when(skillGapAssessmentService.createAssessment(any())).thenReturn(buildAssessment(ASSESSMENT_ID, TRAINEE_ID));
      when(skillGapService.createSkillGap(any()))
          .thenReturn(buildSkillGap(201L, ASSESSMENT_ID, TRAINEE_ID, 101L))
          .thenReturn(buildSkillGap(202L, ASSESSMENT_ID, TRAINEE_ID, 102L));

      assertThatThrownBy(() -> workflowService.recordAssessmentWithGapsAndRecommendations(request))
          .isInstanceOf(BadRequestException.class)
          .hasMessageContaining("Skill gap ID is required for recommendation when multiple skill gaps are created")
          .extracting("errorCode").isEqualTo("SKILL_GAP_ID_REQUIRED");

      // No recommendation must be persisted
      verify(skillGapRecommendationService, never()).createRecommendation(any());
    }

    @Test
    @DisplayName("Rejects recommendation referencing nonexistent skill gap ID")
    void shouldRejectNonexistentExplicitSkillGapId() {
      RecordSkillGapAssessmentWithGapsRequest request = new RecordSkillGapAssessmentWithGapsRequest();
      request.setAssessmentRequest(new CreateSkillGapAssessmentRequest());

      CreateSkillGapRequest gapReq1 = new CreateSkillGapRequest();
      gapReq1.setSkillId(101L);
      request.setGapRequests(List.of(gapReq1));

      CreateSkillGapRecommendationRequest recReq = new CreateSkillGapRecommendationRequest();
      recReq.setSkillGapId(9999L); // does not match created gap and does not exist in db
      request.setRecommendationRequests(List.of(recReq));

      when(skillGapAssessmentService.createAssessment(any())).thenReturn(buildAssessment(ASSESSMENT_ID, TRAINEE_ID));
      when(skillGapService.createSkillGap(any())).thenReturn(buildSkillGap(201L, ASSESSMENT_ID, TRAINEE_ID, 101L));
      when(skillGapService.getSkillGapById(9999L)).thenThrow(new ResourceNotFoundException("SkillGap", "id"));

      assertThatThrownBy(() -> workflowService.recordAssessmentWithGapsAndRecommendations(request))
          .isInstanceOf(ResourceNotFoundException.class);

      verify(skillGapRecommendationService, never()).createRecommendation(any());
    }

    @Test
    @DisplayName("Rejects recommendation referencing skill gap belonging to another trainee")
    void shouldRejectExplicitSkillGapBelongingToDifferentTrainee() {
      RecordSkillGapAssessmentWithGapsRequest request = new RecordSkillGapAssessmentWithGapsRequest();
      request.setAssessmentRequest(new CreateSkillGapAssessmentRequest());

      CreateSkillGapRequest gapReq1 = new CreateSkillGapRequest();
      gapReq1.setSkillId(101L);
      request.setGapRequests(List.of(gapReq1));

      Long externalGapId = 555L;
      CreateSkillGapRecommendationRequest recReq = new CreateSkillGapRecommendationRequest();
      recReq.setSkillGapId(externalGapId);
      request.setRecommendationRequests(List.of(recReq));

      Long differentTraineeId = 888L;
      when(skillGapAssessmentService.createAssessment(any())).thenReturn(buildAssessment(ASSESSMENT_ID, TRAINEE_ID));
      when(skillGapService.createSkillGap(any())).thenReturn(buildSkillGap(201L, ASSESSMENT_ID, TRAINEE_ID, 101L));
      when(skillGapService.getSkillGapById(externalGapId))
          .thenReturn(buildSkillGap(externalGapId, ASSESSMENT_ID, differentTraineeId, 101L));

      assertThatThrownBy(() -> workflowService.recordAssessmentWithGapsAndRecommendations(request))
          .isInstanceOf(BadRequestException.class)
          .hasMessageContaining("Skill gap does not belong to assessment trainee")
          .extracting("errorCode").isEqualTo("SKILL_GAP_TRAINEE_MISMATCH");

      verify(skillGapRecommendationService, never()).createRecommendation(any());
    }

    @Test
    @DisplayName("Rejects recommendation referencing skill gap from an incompatible assessment")
    void shouldRejectExplicitSkillGapBelongingToDifferentAssessment() {
      RecordSkillGapAssessmentWithGapsRequest request = new RecordSkillGapAssessmentWithGapsRequest();
      request.setAssessmentRequest(new CreateSkillGapAssessmentRequest());

      CreateSkillGapRequest gapReq1 = new CreateSkillGapRequest();
      gapReq1.setSkillId(101L);
      request.setGapRequests(List.of(gapReq1));

      Long externalGapId = 555L;
      CreateSkillGapRecommendationRequest recReq = new CreateSkillGapRecommendationRequest();
      recReq.setSkillGapId(externalGapId);
      request.setRecommendationRequests(List.of(recReq));

      Long differentAssessmentId = 777L;
      when(skillGapAssessmentService.createAssessment(any())).thenReturn(buildAssessment(ASSESSMENT_ID, TRAINEE_ID));
      when(skillGapService.createSkillGap(any())).thenReturn(buildSkillGap(201L, ASSESSMENT_ID, TRAINEE_ID, 101L));
      when(skillGapService.getSkillGapById(externalGapId))
          .thenReturn(buildSkillGap(externalGapId, differentAssessmentId, TRAINEE_ID, 101L));

      assertThatThrownBy(() -> workflowService.recordAssessmentWithGapsAndRecommendations(request))
          .isInstanceOf(BadRequestException.class)
          .hasMessageContaining("Skill gap does not belong to the current assessment")
          .extracting("errorCode").isEqualTo("SKILL_GAP_ASSESSMENT_MISMATCH");

      verify(skillGapRecommendationService, never()).createRecommendation(any());
    }

    @Test
    @DisplayName("Rejects gap creation when gap trainee ID does not match assessment trainee ID")
    void shouldRejectAssessmentCreationWhenGapTraineeDoesNotMatchAssessmentTrainee() {
      RecordSkillGapAssessmentWithGapsRequest request = new RecordSkillGapAssessmentWithGapsRequest();
      request.setAssessmentRequest(new CreateSkillGapAssessmentRequest());

      CreateSkillGapRequest gapReq = new CreateSkillGapRequest();
      gapReq.setSkillId(101L);
      gapReq.setTraineeId(999L); // does not match assessment trainee (100L)
      request.setGapRequests(List.of(gapReq));

      when(skillGapAssessmentService.createAssessment(any())).thenReturn(buildAssessment(ASSESSMENT_ID, TRAINEE_ID));

      assertThatThrownBy(() -> workflowService.recordAssessmentWithGapsAndRecommendations(request))
          .isInstanceOf(BadRequestException.class)
          .hasMessageContaining("Skill gap trainee does not match assessment trainee")
          .extracting("errorCode").isEqualTo("SKILL_GAP_TRAINEE_MISMATCH");

      verify(skillGapService, never()).createSkillGap(any());
    }
  }

  @Nested
  @DisplayName("3. Input Invariants and Adjacent Remediation Operations")
  class AdjacentOperationsTests {

    @Test
    @DisplayName("Rejects null request body, missing assessment, or empty gap requests")
    void shouldRejectInvalidInputPayloads() {
      assertThatThrownBy(() -> workflowService.recordAssessmentWithGapsAndRecommendations(null))
          .isInstanceOf(BadRequestException.class)
          .extracting("errorCode").isEqualTo("REQUEST_BODY_NULL");

      RecordSkillGapAssessmentWithGapsRequest noAssessment = new RecordSkillGapAssessmentWithGapsRequest();
      assertThatThrownBy(() -> workflowService.recordAssessmentWithGapsAndRecommendations(noAssessment))
          .isInstanceOf(BadRequestException.class)
          .extracting("errorCode").isEqualTo("ASSESSMENT_REQUEST_REQUIRED");

      RecordSkillGapAssessmentWithGapsRequest noGaps = new RecordSkillGapAssessmentWithGapsRequest();
      noGaps.setAssessmentRequest(new CreateSkillGapAssessmentRequest());
      noGaps.setGapRequests(Collections.emptyList());
      assertThatThrownBy(() -> workflowService.recordAssessmentWithGapsAndRecommendations(noGaps))
          .isInstanceOf(BadRequestException.class)
          .extracting("errorCode").isEqualTo("SKILL_GAPS_REQUIRED");
    }

    @Test
    @DisplayName("Preserves valid remediation course enrollment workflow")
    void shouldPreserveRemediationCourseEnrollmentWorkflow() {
      SkillGapRemediationEnrollmentRequest request = new SkillGapRemediationEnrollmentRequest();
      request.setRecommendationId(1001L);

      CreateTrainingEnrollmentRequest enrolReq = new CreateTrainingEnrollmentRequest();
      enrolReq.setCourseId(501L);
      request.setEnrollmentRequest(enrolReq);

      SkillGapRecommendationResponse mockRec = buildRecommendationResponse(1001L, 201L, 501L);
      when(skillGapRecommendationService.getRecommendationById(1001L)).thenReturn(mockRec);

      TrainingEnrollmentResponse mockEnrollment = new TrainingEnrollmentResponse();
      mockEnrollment.setId(3001L);
      when(trainingEnrollmentService.createEnrollment(any())).thenReturn(mockEnrollment);

      SkillGapRemediationEnrollmentResponse response =
          workflowService.enrollTraineeInRemediationCourse(request);

      assertThat(response).isNotNull();
      assertThat(response.createdEnrollment().getId()).isEqualTo(3001L);
      verify(trainingEnrollmentService).createEnrollment(any());
    }

    @Test
    @DisplayName("Retrieves trainee skill-gap profile without modifying or corrupting data")
    void shouldRetrieveTraineeSkillGapProfileWithoutDataCorruption() {
      when(skillGapAssessmentService.getAssessmentsByTrainee(TRAINEE_ID))
          .thenReturn(List.of(buildAssessment(ASSESSMENT_ID, TRAINEE_ID)));
      when(skillGapService.getCurrentSkillGapsByTrainee(TRAINEE_ID))
          .thenReturn(List.of(buildSkillGap(201L, ASSESSMENT_ID, TRAINEE_ID, 101L)));
      when(skillGapRecommendationService.getRecommendationsBySkillGap(201L))
          .thenReturn(List.of(buildRecommendationResponse(1001L, 201L, 501L)));

      TraineeSkillGapProfileResponse profile = workflowService.getTraineeSkillGapProfile(TRAINEE_ID);

      assertThat(profile).isNotNull();
      assertThat(profile.traineeId()).isEqualTo(TRAINEE_ID);
      assertThat(profile.assessments()).hasSize(1);
      assertThat(profile.currentSkillGaps()).hasSize(1);
      assertThat(profile.recommendations()).hasSize(1);
    }
  }
}
