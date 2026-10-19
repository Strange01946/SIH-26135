package in.gov.sih.sih26135.workflow;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
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
import in.gov.sih.sih26135.dto.request.TraineeRecommendationDecisionRequest;
import in.gov.sih.sih26135.dto.request.UpdateSkillGapRecommendationRequest;
import in.gov.sih.sih26135.dto.response.RecordSkillGapAssessmentWithGapsResponse;
import in.gov.sih.sih26135.dto.response.SkillGapAssessmentResponse;
import in.gov.sih.sih26135.dto.response.SkillGapRecommendationResponse;
import in.gov.sih.sih26135.dto.response.SkillGapRemediationEnrollmentResponse;
import in.gov.sih.sih26135.dto.response.SkillGapResponse;
import in.gov.sih.sih26135.dto.response.TraineeSkillGapProfileResponse;
import in.gov.sih.sih26135.dto.response.TrainingEnrollmentResponse;
import in.gov.sih.sih26135.entity.Course;
import in.gov.sih.sih26135.entity.RefSkillGapActionType;
import in.gov.sih.sih26135.entity.RefSkillGapStatus;
import in.gov.sih.sih26135.entity.Skill;
import in.gov.sih.sih26135.entity.SkillGap;
import in.gov.sih.sih26135.entity.SkillGapRecommendation;
import in.gov.sih.sih26135.entity.Trainee;
import in.gov.sih.sih26135.exception.BadRequestException;
import in.gov.sih.sih26135.exception.InvalidCredentialsException;
import in.gov.sih.sih26135.exception.ResourceNotFoundException;
import in.gov.sih.sih26135.mapper.SkillGapRecommendationMapper;
import in.gov.sih.sih26135.repository.CourseSkillRepository;
import in.gov.sih.sih26135.repository.RefSkillGapStatusRepository;
import in.gov.sih.sih26135.repository.SkillGapRecommendationRepository;
import in.gov.sih.sih26135.repository.SkillGapRepository;
import in.gov.sih.sih26135.repository.TraineeRepository;
import in.gov.sih.sih26135.security.UserPrincipal;
import in.gov.sih.sih26135.service.SkillGapAssessmentService;
import in.gov.sih.sih26135.service.SkillGapRecommendationService;
import in.gov.sih.sih26135.service.SkillGapService;
import in.gov.sih.sih26135.service.TrainingEnrollmentService;
import in.gov.sih.sih26135.service.impl.SkillGapRemediationWorkflowServiceImpl;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

@ExtendWith(MockitoExtension.class)
class SkillGapRemediationWorkflowAssociationTest {

  @Mock private SkillGapAssessmentService skillGapAssessmentService;
  @Mock private SkillGapService skillGapService;
  @Mock private SkillGapRecommendationService skillGapRecommendationService;
  @Mock private TrainingEnrollmentService trainingEnrollmentService;
  @Mock private CourseSkillRepository courseSkillRepository;
  @Mock private SkillGapRepository skillGapRepository;
  @Mock private SkillGapRecommendationRepository skillGapRecommendationRepository;
  @Mock private RefSkillGapStatusRepository refSkillGapStatusRepository;
  @Mock private TraineeRepository traineeRepository;

  private SkillGapRemediationWorkflowServiceImpl workflowService;

  private final Long ASSESSMENT_ID = 10L;
  private final Long TRAINEE_ID = 100L;

  @BeforeEach
  void setUp() {
    workflowService = new SkillGapRemediationWorkflowServiceImpl(
        skillGapAssessmentService,
        skillGapService,
        skillGapRecommendationService,
        trainingEnrollmentService,
        courseSkillRepository,
        skillGapRepository,
        skillGapRecommendationRepository,
        refSkillGapStatusRepository,
        traineeRepository,
        new SkillGapRecommendationMapper()
    );
    lenient().when(courseSkillRepository.existsByCourseIdAndSkillId(any(), any())).thenReturn(true);

    UserPrincipal adminPrincipal = new UserPrincipal(1L, "adminUser");
    SecurityContextHolder.getContext().setAuthentication(
        new UsernamePasswordAuthenticationToken(
            adminPrincipal,
            null,
            List.of(
                new SimpleGrantedAuthority("assessment.manage"),
                new SimpleGrantedAuthority("enrollment.manage")
            )
        )
    );
  }

  @AfterEach
  void tearDown() {
    SecurityContextHolder.clearContext();
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
    return buildRecommendationResponse(id, skillGapId, courseId, false);
  }

  private SkillGapRecommendationResponse buildRecommendationResponse(
      Long id, Long skillGapId, Long courseId, Boolean isAccepted) {
    return new SkillGapRecommendationResponse(
        id, skillGapId, "SG-2026-" + skillGapId, 1, 1L, "UPSKILL", "Upskill",
        courseId, "CRS-" + courseId, "Course " + courseId,
        null, null, null, isAccepted, isAccepted ? LocalDate.now() : null, "Recommended upskilling",
        1L, LocalDateTime.now().minusDays(1), LocalDateTime.now()
    );
  }

  private SkillGapRecommendation buildRecommendationEntity(
      Long id, Long skillGapId, Long courseId, Long skillId, Boolean isAccepted,
      String gapStatusCode, Boolean gapIsCurrent) {
    Trainee trainee = new Trainee();
    trainee.setId(TRAINEE_ID);

    Skill skill = new Skill();
    skill.setId(skillId != null ? skillId : 101L);
    skill.setSkillCode("SK-" + (skillId != null ? skillId : 101L));
    skill.setSkillName("Skill " + (skillId != null ? skillId : 101L));

    RefSkillGapStatus gapStatus = new RefSkillGapStatus();
    gapStatus.setId(1L);
    gapStatus.setStatusCode(gapStatusCode != null ? gapStatusCode : "OPEN");
    gapStatus.setStatusName("Status " + (gapStatusCode != null ? gapStatusCode : "OPEN"));

    SkillGap gap = new SkillGap();
    gap.setId(skillGapId);
    gap.setSkillGapNumber("SG-2026-" + skillGapId);
    gap.setTrainee(trainee);
    gap.setSkill(skill);
    gap.setSkillGapStatus(gapStatus);
    gap.setIsCurrent(gapIsCurrent != null ? gapIsCurrent : true);

    Course course = new Course();
    course.setId(courseId != null ? courseId : 501L);
    course.setCourseCode("CRS-" + (courseId != null ? courseId : 501L));
    course.setCourseName("Course " + (courseId != null ? courseId : 501L));

    RefSkillGapActionType actionType = new RefSkillGapActionType();
    actionType.setId(1L);
    actionType.setActionCode("UPSKILL");
    actionType.setActionName("Upskill");

    SkillGapRecommendation rec = new SkillGapRecommendation();
    rec.setId(id);
    rec.setSkillGap(gap);
    rec.setRecommendationNumber(1);
    rec.setSkillGapActionType(actionType);
    rec.setRecommendedCourse(course);
    rec.setRecommendedSkill(skill);
    rec.setIsAcceptedFlag(isAccepted);
    rec.setAcceptedOn(Boolean.TRUE.equals(isAccepted) ? LocalDate.now() : null);
    rec.setRemarks("Recommendation notes");
    rec.setCreatedByUserId(1L);
    rec.setCreatedAt(LocalDateTime.now().minusDays(1));
    rec.setUpdatedAt(LocalDateTime.now());
    return rec;
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

    @Test
    @DisplayName("Rejects recommendation when recommended course does not teach the identified gap skill")
    void shouldRejectRecommendationWhenCourseDoesNotTeachGapSkill() {
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
      when(courseSkillRepository.existsByCourseIdAndSkillId(501L, 101L)).thenReturn(false);

      assertThatThrownBy(() -> workflowService.recordAssessmentWithGapsAndRecommendations(request))
          .isInstanceOf(BadRequestException.class)
          .hasMessageContaining("Recommended course does not teach the identified gap skill")
          .extracting("errorCode").isEqualTo("COURSE_SKILL_MISMATCH");

      verify(skillGapRecommendationService, never()).createRecommendation(any());
    }

    @Test
    @DisplayName("Rejects single-new-gap default recommendation when course does not teach gap skill")
    void shouldRejectSingleGapDefaultRecommendationWhenCourseDoesNotTeachGapSkill() {
      RecordSkillGapAssessmentWithGapsRequest request = new RecordSkillGapAssessmentWithGapsRequest();
      request.setAssessmentRequest(new CreateSkillGapAssessmentRequest());

      CreateSkillGapRequest gapReq = new CreateSkillGapRequest();
      gapReq.setSkillId(101L);
      request.setGapRequests(List.of(gapReq));

      CreateSkillGapRecommendationRequest recReq = new CreateSkillGapRecommendationRequest();
      recReq.setSkillGapId(null); // default to single created gap
      recReq.setRecommendedCourseId(501L);
      request.setRecommendationRequests(List.of(recReq));

      when(skillGapAssessmentService.createAssessment(any())).thenReturn(buildAssessment(ASSESSMENT_ID, TRAINEE_ID));
      when(skillGapService.createSkillGap(any())).thenReturn(buildSkillGap(201L, ASSESSMENT_ID, TRAINEE_ID, 101L));
      when(courseSkillRepository.existsByCourseIdAndSkillId(501L, 101L)).thenReturn(false);

      assertThatThrownBy(() -> workflowService.recordAssessmentWithGapsAndRecommendations(request))
          .isInstanceOf(BadRequestException.class)
          .hasMessageContaining("Recommended course does not teach the identified gap skill")
          .extracting("errorCode").isEqualTo("COURSE_SKILL_MISMATCH");

      verify(skillGapRecommendationService, never()).createRecommendation(any());
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
    @DisplayName("Preserves valid remediation course enrollment workflow and transitions gap to IN_REMEDIATION")
    void shouldPreserveRemediationCourseEnrollmentWorkflow() {
      SkillGapRemediationEnrollmentRequest request = new SkillGapRemediationEnrollmentRequest();
      request.setRecommendationId(1001L);

      CreateTrainingEnrollmentRequest enrolReq = new CreateTrainingEnrollmentRequest();
      enrolReq.setCourseId(501L);
      request.setEnrollmentRequest(enrolReq);

      SkillGapRecommendationResponse mockRec = buildRecommendationResponse(1001L, 201L, 501L, true);
      when(skillGapRecommendationService.getRecommendationById(1001L)).thenReturn(mockRec);

      SkillGap mockGapEntity = buildRecommendationEntity(1001L, 201L, 501L, 101L, true, "OPEN", true).getSkillGap();
      when(skillGapRepository.findById(201L)).thenReturn(Optional.of(mockGapEntity));

      RefSkillGapStatus inRemediationStatus = new RefSkillGapStatus();
      inRemediationStatus.setId(2L);
      inRemediationStatus.setStatusCode("IN_REMEDIATION");
      inRemediationStatus.setStatusName("In Remediation");
      when(refSkillGapStatusRepository.findByStatusCode("IN_REMEDIATION")).thenReturn(Optional.of(inRemediationStatus));

      TrainingEnrollmentResponse mockEnrollment = new TrainingEnrollmentResponse();
      mockEnrollment.setId(3001L);
      when(trainingEnrollmentService.createEnrollment(any())).thenReturn(mockEnrollment);

      SkillGapRemediationEnrollmentResponse response =
          workflowService.enrollTraineeInRemediationCourse(request);

      assertThat(response).isNotNull();
      assertThat(response.createdEnrollment().getId()).isEqualTo(3001L);
      verify(trainingEnrollmentService).createEnrollment(any());
      verify(skillGapRepository).save(mockGapEntity);
      assertThat(mockGapEntity.getSkillGapStatus().getStatusCode()).isEqualTo("IN_REMEDIATION");
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

    @Test
    @DisplayName("Rejects profile retrieval when trainee ID is null")
    void shouldRejectProfileRetrievalWhenTraineeIdIsNull() {
      assertThatThrownBy(() -> workflowService.getTraineeSkillGapProfile(null))
          .isInstanceOf(BadRequestException.class)
          .extracting("errorCode").isEqualTo("TRAINEE_ID_REQUIRED");
    }
  }

  @Nested
  @DisplayName("4. Recommendation Decision Lifecycle Tests (Accept / Reject)")
  class RecommendationLifecycleTests {

    @Test
    @DisplayName("Accept recommendation succeeds and updates isAcceptedFlag, acceptedOn, and remarks")
    void shouldAcceptRecommendationSuccessfully() {
      SkillGapRecommendation recEntity = buildRecommendationEntity(
          1001L, 201L, 501L, 101L, false, "OPEN", true);
      when(skillGapRecommendationRepository.findById(1001L)).thenReturn(Optional.of(recEntity));
      when(skillGapRecommendationRepository.save(any(SkillGapRecommendation.class)))
          .thenAnswer(invocation -> invocation.getArgument(0));

      TraineeRecommendationDecisionRequest request = new TraineeRecommendationDecisionRequest(
          TRAINEE_ID, "Ready to start upskilling", 42L);

      SkillGapRecommendationResponse response = workflowService.acceptRecommendation(1001L, request);

      assertThat(response).isNotNull();
      assertThat(response.isAcceptedFlag()).isTrue();
      assertThat(response.acceptedOn()).isEqualTo(LocalDate.now());
      assertThat(response.remarks()).isEqualTo("Ready to start upskilling");
      assertThat(response.createdByUserId()).isEqualTo(1L);

      ArgumentCaptor<SkillGapRecommendation> captor = ArgumentCaptor.forClass(SkillGapRecommendation.class);
      verify(skillGapRecommendationRepository).save(captor.capture());
      assertThat(captor.getValue().getIsAcceptedFlag()).isTrue();
      assertThat(captor.getValue().getAcceptedOn()).isEqualTo(LocalDate.now());
      assertThat(captor.getValue().getCreatedByUserId()).isEqualTo(1L);
    }

    @Test
    @DisplayName("Accept recommendation fails when recommendation ID is null")
    void shouldRejectAcceptanceWhenRecommendationIdIsNull() {
      TraineeRecommendationDecisionRequest request = new TraineeRecommendationDecisionRequest();
      assertThatThrownBy(() -> workflowService.acceptRecommendation(null, request))
          .isInstanceOf(BadRequestException.class)
          .extracting("errorCode").isEqualTo("RECOMMENDATION_ID_REQUIRED");
    }

    @Test
    @DisplayName("Accept recommendation fails when recommendation not found")
    void shouldRejectAcceptanceWhenRecommendationNotFound() {
      when(skillGapRecommendationRepository.findById(9999L)).thenReturn(Optional.empty());

      TraineeRecommendationDecisionRequest request = new TraineeRecommendationDecisionRequest();
      assertThatThrownBy(() -> workflowService.acceptRecommendation(9999L, request))
          .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("Accept recommendation fails when trainee ID does not match recommendation trainee")
    void shouldRejectAcceptanceWhenTraineeMismatch() {
      SkillGapRecommendation recEntity = buildRecommendationEntity(
          1001L, 201L, 501L, 101L, false, "OPEN", true);
      when(skillGapRecommendationRepository.findById(1001L)).thenReturn(Optional.of(recEntity));

      TraineeRecommendationDecisionRequest request = new TraineeRecommendationDecisionRequest(
          999L, "Not my recommendation", null);

      assertThatThrownBy(() -> workflowService.acceptRecommendation(1001L, request))
          .isInstanceOf(BadRequestException.class)
          .extracting("errorCode").isEqualTo("TRAINEE_MISMATCH");
    }

    @Test
    @DisplayName("Accept recommendation fails when already accepted")
    void shouldRejectAcceptanceWhenAlreadyAccepted() {
      SkillGapRecommendation recEntity = buildRecommendationEntity(
          1001L, 201L, 501L, 101L, true, "OPEN", true);
      when(skillGapRecommendationRepository.findById(1001L)).thenReturn(Optional.of(recEntity));

      TraineeRecommendationDecisionRequest request = new TraineeRecommendationDecisionRequest();

      assertThatThrownBy(() -> workflowService.acceptRecommendation(1001L, request))
          .isInstanceOf(BadRequestException.class)
          .extracting("errorCode").isEqualTo("ALREADY_ACCEPTED");
    }

    @Test
    @DisplayName("Accept recommendation fails when linked skill gap is already resolved or closed")
    void shouldRejectAcceptanceWhenSkillGapAlreadyResolvedOrClosed() {
      SkillGapRecommendation recEntity = buildRecommendationEntity(
          1001L, 201L, 501L, 101L, false, "RESOLVED", false);
      when(skillGapRecommendationRepository.findById(1001L)).thenReturn(Optional.of(recEntity));

      TraineeRecommendationDecisionRequest request = new TraineeRecommendationDecisionRequest();

      assertThatThrownBy(() -> workflowService.acceptRecommendation(1001L, request))
          .isInstanceOf(BadRequestException.class)
          .extracting("errorCode").isEqualTo("INVALID_GAP_STATUS");
    }

    @Test
    @DisplayName("Reject recommendation succeeds with mandatory remarks")
    void shouldRejectRecommendationSuccessfullyWithMandatoryRemarks() {
      SkillGapRecommendation recEntity = buildRecommendationEntity(
          1001L, 201L, 501L, 101L, false, "OPEN", true);
      when(skillGapRecommendationRepository.findById(1001L)).thenReturn(Optional.of(recEntity));
      when(skillGapRecommendationRepository.save(any(SkillGapRecommendation.class)))
          .thenAnswer(invocation -> invocation.getArgument(0));

      TraineeRecommendationDecisionRequest request = new TraineeRecommendationDecisionRequest(
          TRAINEE_ID, "Prefer alternative certification", 42L);

      SkillGapRecommendationResponse response = workflowService.rejectRecommendation(1001L, request);

      assertThat(response).isNotNull();
      assertThat(response.isAcceptedFlag()).isFalse();
      assertThat(response.acceptedOn()).isNull();
      assertThat(response.remarks()).isEqualTo("Prefer alternative certification");

      ArgumentCaptor<SkillGapRecommendation> captor = ArgumentCaptor.forClass(SkillGapRecommendation.class);
      verify(skillGapRecommendationRepository).save(captor.capture());
      assertThat(captor.getValue().getIsAcceptedFlag()).isFalse();
      assertThat(captor.getValue().getAcceptedOn()).isNull();
      assertThat(captor.getValue().getRemarks()).isEqualTo("Prefer alternative certification");
    }

    @Test
    @DisplayName("Reject recommendation fails when recommendation is already accepted")
    void shouldRejectRecommendationRejectionWhenAlreadyAccepted() {
      SkillGapRecommendation recEntity = buildRecommendationEntity(
          1001L, 201L, 501L, 101L, true, "OPEN", true);
      when(skillGapRecommendationRepository.findById(1001L)).thenReturn(Optional.of(recEntity));

      TraineeRecommendationDecisionRequest request = new TraineeRecommendationDecisionRequest(
          TRAINEE_ID, "Cannot reject after accepting", null);

      assertThatThrownBy(() -> workflowService.rejectRecommendation(1001L, request))
          .isInstanceOf(BadRequestException.class)
          .extracting("errorCode").isEqualTo("INVALID_STATE_TRANSITION");
    }

    @Test
    @DisplayName("Reject recommendation fails when remarks are missing or blank")
    void shouldRejectRecommendationRejectionWhenRemarksMissingOrBlank() {
      TraineeRecommendationDecisionRequest nullRemarks = new TraineeRecommendationDecisionRequest(
          TRAINEE_ID, null, null);
      assertThatThrownBy(() -> workflowService.rejectRecommendation(1001L, nullRemarks))
          .isInstanceOf(BadRequestException.class)
          .extracting("errorCode").isEqualTo("REJECTION_REMARKS_REQUIRED");

      TraineeRecommendationDecisionRequest blankRemarks = new TraineeRecommendationDecisionRequest(
          TRAINEE_ID, "   ", null);
      assertThatThrownBy(() -> workflowService.rejectRecommendation(1001L, blankRemarks))
          .isInstanceOf(BadRequestException.class)
          .extracting("errorCode").isEqualTo("REJECTION_REMARKS_REQUIRED");

      assertThatThrownBy(() -> workflowService.rejectRecommendation(1001L, null))
          .isInstanceOf(BadRequestException.class)
          .extracting("errorCode").isEqualTo("REJECTION_REMARKS_REQUIRED");
    }

    @Test
    @DisplayName("Reject recommendation fails when recommendation ID is null")
    void shouldRejectRecommendationRejectionWhenIdIsNull() {
      TraineeRecommendationDecisionRequest request = new TraineeRecommendationDecisionRequest(
          TRAINEE_ID, "Some reason", null);
      assertThatThrownBy(() -> workflowService.rejectRecommendation(null, request))
          .isInstanceOf(BadRequestException.class)
          .extracting("errorCode").isEqualTo("RECOMMENDATION_ID_REQUIRED");
    }

    @Test
    @DisplayName("Reject recommendation fails when recommendation not found")
    void shouldRejectRecommendationRejectionWhenNotFound() {
      when(skillGapRecommendationRepository.findById(9999L)).thenReturn(Optional.empty());

      TraineeRecommendationDecisionRequest request = new TraineeRecommendationDecisionRequest(
          TRAINEE_ID, "Valid reason", null);
      assertThatThrownBy(() -> workflowService.rejectRecommendation(9999L, request))
          .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("Reject recommendation fails when trainee ID does not match recommendation trainee")
    void shouldRejectRecommendationRejectionWhenTraineeMismatch() {
      SkillGapRecommendation recEntity = buildRecommendationEntity(
          1001L, 201L, 501L, 101L, false, "OPEN", true);
      when(skillGapRecommendationRepository.findById(1001L)).thenReturn(Optional.of(recEntity));

      TraineeRecommendationDecisionRequest request = new TraineeRecommendationDecisionRequest(
          999L, "Wrong trainee rejecting", null);

      assertThatThrownBy(() -> workflowService.rejectRecommendation(1001L, request))
          .isInstanceOf(BadRequestException.class)
          .extracting("errorCode").isEqualTo("TRAINEE_MISMATCH");
    }

    @Test
    @DisplayName("Reject recommendation fails when gap is already in active remediation")
    void shouldRejectRecommendationRejectionWhenGapAlreadyInRemediation() {
      SkillGapRecommendation recEntity = buildRecommendationEntity(
          1001L, 201L, 501L, 101L, false, "IN_REMEDIATION", true);
      when(skillGapRecommendationRepository.findById(1001L)).thenReturn(Optional.of(recEntity));

      TraineeRecommendationDecisionRequest request = new TraineeRecommendationDecisionRequest(
          TRAINEE_ID, "Want to drop out now", null);

      assertThatThrownBy(() -> workflowService.rejectRecommendation(1001L, request))
          .isInstanceOf(BadRequestException.class)
          .extracting("errorCode").isEqualTo("INVALID_STATE_TRANSITION");
    }

    @Test
    @DisplayName("Accept recommendation fails when authenticated trainee does not match recommendation trainee")
    void shouldRejectAcceptanceWhenAuthenticatedTraineeMismatch() {
      SkillGapRecommendation recEntity = buildRecommendationEntity(
          1001L, 201L, 501L, 101L, false, "OPEN", true);
      when(skillGapRecommendationRepository.findById(1001L)).thenReturn(Optional.of(recEntity));

      // Authenticate as a different trainee (userId = 99L, traineeId = 999L)
      UserPrincipal principal = new UserPrincipal(99L, "otherTrainee");
      SecurityContextHolder.getContext().setAuthentication(
          new UsernamePasswordAuthenticationToken(principal, null, Collections.emptyList()));

      Trainee otherTrainee = new Trainee();
      otherTrainee.setId(999L);
      when(traineeRepository.findByUserId(99L)).thenReturn(Optional.of(otherTrainee));

      TraineeRecommendationDecisionRequest request = new TraineeRecommendationDecisionRequest();

      assertThatThrownBy(() -> workflowService.acceptRecommendation(1001L, request))
          .isInstanceOf(BadRequestException.class)
          .extracting("errorCode").isEqualTo("TRAINEE_MISMATCH");
    }

    @Test
    @DisplayName("Accept recommendation uses server-side authenticated user for audit attribution instead of client actorUserId when creator is null")
    void shouldPreferAuthenticatedUserForAuditAttribution() {
      SkillGapRecommendation recEntity = buildRecommendationEntity(
          1001L, 201L, 501L, 101L, false, "OPEN", true);
      recEntity.setCreatedByUserId(null);
      when(skillGapRecommendationRepository.findById(1001L)).thenReturn(Optional.of(recEntity));
      when(skillGapRecommendationRepository.save(any(SkillGapRecommendation.class)))
          .thenAnswer(invocation -> invocation.getArgument(0));

      UserPrincipal principal = new UserPrincipal(888L, "trustedCounselor");
      SecurityContextHolder.getContext().setAuthentication(
          new UsernamePasswordAuthenticationToken(
              principal,
              null,
              List.of(new SimpleGrantedAuthority("assessment.manage"))));

      // Client attempts to claim actorUserId = 42L, but trusted principal is 888L
      TraineeRecommendationDecisionRequest request = new TraineeRecommendationDecisionRequest(
          TRAINEE_ID, "Accepted by counselor", 42L);

      SkillGapRecommendationResponse response = workflowService.acceptRecommendation(1001L, request);

      assertThat(response).isNotNull();
      assertThat(response.createdByUserId()).isEqualTo(888L);
    }

    @Test
    @DisplayName("Accept recommendation preserves original creator when createdByUserId already exists")
    void shouldPreserveOriginalCreatorOnAcceptance() {
      SkillGapRecommendation recEntity = buildRecommendationEntity(
          1001L, 201L, 501L, 101L, false, "OPEN", true);
      recEntity.setCreatedByUserId(50L); // original creator is user 50
      when(skillGapRecommendationRepository.findById(1001L)).thenReturn(Optional.of(recEntity));
      when(skillGapRecommendationRepository.save(any(SkillGapRecommendation.class)))
          .thenAnswer(invocation -> invocation.getArgument(0));

      UserPrincipal principal = new UserPrincipal(888L, "trustedCounselor");
      SecurityContextHolder.getContext().setAuthentication(
          new UsernamePasswordAuthenticationToken(
              principal,
              null,
              List.of(new SimpleGrantedAuthority("assessment.manage"))));

      TraineeRecommendationDecisionRequest request = new TraineeRecommendationDecisionRequest(
          TRAINEE_ID, "Accepted by counselor", 42L);

      SkillGapRecommendationResponse response = workflowService.acceptRecommendation(1001L, request);

      assertThat(response).isNotNull();
      assertThat(response.createdByUserId()).isEqualTo(50L); // original creator preserved!
    }

    @Test
    @DisplayName("Reject recommendation preserves original creator when createdByUserId already exists")
    void shouldPreserveOriginalCreatorOnRejection() {
      SkillGapRecommendation recEntity = buildRecommendationEntity(
          1001L, 201L, 501L, 101L, false, "OPEN", true);
      recEntity.setCreatedByUserId(50L); // original creator is user 50
      when(skillGapRecommendationRepository.findById(1001L)).thenReturn(Optional.of(recEntity));
      when(skillGapRecommendationRepository.save(any(SkillGapRecommendation.class)))
          .thenAnswer(invocation -> invocation.getArgument(0));

      UserPrincipal counselor = new UserPrincipal(777L, "counselor");
      SecurityContextHolder.getContext().setAuthentication(
          new UsernamePasswordAuthenticationToken(
              counselor, null, List.of(new SimpleGrantedAuthority("assessment.manage"))));

      TraineeRecommendationDecisionRequest request = new TraineeRecommendationDecisionRequest(
          TRAINEE_ID, "Rejection justified", 12345L);

      SkillGapRecommendationResponse response = workflowService.rejectRecommendation(1001L, request);

      assertThat(response).isNotNull();
      assertThat(response.isAcceptedFlag()).isFalse();
      assertThat(response.createdByUserId()).isEqualTo(50L); // original creator preserved!
    }

    @Test
    @DisplayName("Accept recommendation fails closed when TraineeRepository is unavailable")
    void shouldFailClosedWhenTraineeRepositoryIsNullOnAccept() {
      SkillGapRemediationWorkflowServiceImpl serviceWithoutTraineeRepo =
          new SkillGapRemediationWorkflowServiceImpl(
              skillGapAssessmentService,
              skillGapService,
              skillGapRecommendationService,
              trainingEnrollmentService,
              courseSkillRepository,
              skillGapRepository,
              skillGapRecommendationRepository,
              refSkillGapStatusRepository,
              null, // traineeRepository is null
              new SkillGapRecommendationMapper()
          );

      SkillGapRecommendation recEntity = buildRecommendationEntity(
          1001L, 201L, 501L, 101L, false, "OPEN", true);
      when(skillGapRecommendationRepository.findById(1001L)).thenReturn(Optional.of(recEntity));

      TraineeRecommendationDecisionRequest request = new TraineeRecommendationDecisionRequest();
      assertThatThrownBy(() -> serviceWithoutTraineeRepo.acceptRecommendation(1001L, request))
          .isInstanceOf(IllegalStateException.class)
          .hasMessageContaining("TraineeRepository is required for trainee ownership verification");
    }

    @Test
    @DisplayName("Reject recommendation fails closed when TraineeRepository is unavailable")
    void shouldFailClosedWhenTraineeRepositoryIsNullOnReject() {
      SkillGapRemediationWorkflowServiceImpl serviceWithoutTraineeRepo =
          new SkillGapRemediationWorkflowServiceImpl(
              skillGapAssessmentService,
              skillGapService,
              skillGapRecommendationService,
              trainingEnrollmentService,
              courseSkillRepository,
              skillGapRepository,
              skillGapRecommendationRepository,
              refSkillGapStatusRepository,
              null, // traineeRepository is null
              new SkillGapRecommendationMapper()
          );

      SkillGapRecommendation recEntity = buildRecommendationEntity(
          1001L, 201L, 501L, 101L, false, "OPEN", true);
      when(skillGapRecommendationRepository.findById(1001L)).thenReturn(Optional.of(recEntity));

      TraineeRecommendationDecisionRequest request = new TraineeRecommendationDecisionRequest(
          TRAINEE_ID, "Rejection remarks", null);
      assertThatThrownBy(() -> serviceWithoutTraineeRepo.rejectRecommendation(1001L, request))
          .isInstanceOf(IllegalStateException.class)
          .hasMessageContaining("TraineeRepository is required for trainee ownership verification");
    }

    @Test
    @DisplayName("Accept recommendation fails when security context has no authentication")
    void shouldRejectAcceptanceWhenUnauthenticated() {
      SecurityContextHolder.clearContext();
      TraineeRecommendationDecisionRequest request = new TraineeRecommendationDecisionRequest();
      assertThatThrownBy(() -> workflowService.acceptRecommendation(1001L, request))
          .isInstanceOf(InvalidCredentialsException.class)
          .hasMessageContaining("User is not authenticated");
    }

    @Test
    @DisplayName("Accept recommendation fails when principal is anonymousUser")
    void shouldRejectAcceptanceWhenAnonymousUser() {
      SecurityContextHolder.getContext().setAuthentication(
          new UsernamePasswordAuthenticationToken("anonymousUser", null, Collections.emptyList()));
      TraineeRecommendationDecisionRequest request = new TraineeRecommendationDecisionRequest();
      assertThatThrownBy(() -> workflowService.acceptRecommendation(1001L, request))
          .isInstanceOf(InvalidCredentialsException.class)
          .hasMessageContaining("User is not authenticated");
    }

    @Test
    @DisplayName("Accept recommendation fails when authenticated user has neither trainee profile nor admin role")
    void shouldRejectAcceptanceWhenNonAdminNonTraineeUser() {
      SkillGapRecommendation recEntity = buildRecommendationEntity(
          1001L, 201L, 501L, 101L, false, "OPEN", true);
      when(skillGapRecommendationRepository.findById(1001L)).thenReturn(Optional.of(recEntity));

      UserPrincipal principal = new UserPrincipal(99L, "regularUser");
      SecurityContextHolder.getContext().setAuthentication(
          new UsernamePasswordAuthenticationToken(principal, null, Collections.emptyList()));

      when(traineeRepository.findByUserId(99L)).thenReturn(Optional.empty());

      TraineeRecommendationDecisionRequest request = new TraineeRecommendationDecisionRequest();
      assertThatThrownBy(() -> workflowService.acceptRecommendation(1001L, request))
          .isInstanceOf(BadRequestException.class)
          .extracting("errorCode").isEqualTo("TRAINEE_MISMATCH");
    }

    @Test
    @DisplayName("Accept recommendation succeeds for administrative caller on behalf of trainee, preserving original creator")
    void shouldAllowAuthorizedAdministrativeAccessForTraineeRecommendation() {
      SkillGapRecommendation recEntity = buildRecommendationEntity(
          1001L, 201L, 501L, 101L, false, "OPEN", true);
      when(skillGapRecommendationRepository.findById(1001L)).thenReturn(Optional.of(recEntity));
      when(skillGapRecommendationRepository.save(any(SkillGapRecommendation.class)))
          .thenAnswer(invocation -> invocation.getArgument(0));

      UserPrincipal admin = new UserPrincipal(999L, "adminOfficer");
      SecurityContextHolder.getContext().setAuthentication(
          new UsernamePasswordAuthenticationToken(
              admin, null, List.of(new SimpleGrantedAuthority("assessment.manage"))));

      when(traineeRepository.findByUserId(999L)).thenReturn(Optional.empty());

      TraineeRecommendationDecisionRequest request = new TraineeRecommendationDecisionRequest(
          TRAINEE_ID, "Admin accepted on behalf of trainee", null);

      SkillGapRecommendationResponse response = workflowService.acceptRecommendation(1001L, request);
      assertThat(response).isNotNull();
      assertThat(response.isAcceptedFlag()).isTrue();
      assertThat(response.createdByUserId()).isEqualTo(1L); // Original creator preserved
    }

    @Test
    @DisplayName("Reject recommendation ignores client-supplied actorUserId and sets authenticated userId when creator is null")
    void shouldPreventClientSuppliedActorSpoofingOnReject() {
      SkillGapRecommendation recEntity = buildRecommendationEntity(
          1001L, 201L, 501L, 101L, false, "OPEN", true);
      recEntity.setCreatedByUserId(null);
      when(skillGapRecommendationRepository.findById(1001L)).thenReturn(Optional.of(recEntity));
      when(skillGapRecommendationRepository.save(any(SkillGapRecommendation.class)))
          .thenAnswer(invocation -> invocation.getArgument(0));

      UserPrincipal counselor = new UserPrincipal(777L, "counselor");
      SecurityContextHolder.getContext().setAuthentication(
          new UsernamePasswordAuthenticationToken(
              counselor, null, List.of(new SimpleGrantedAuthority("assessment.manage"))));

      TraineeRecommendationDecisionRequest request = new TraineeRecommendationDecisionRequest(
          TRAINEE_ID, "Rejection justified", 12345L);

      SkillGapRecommendationResponse response = workflowService.rejectRecommendation(1001L, request);
      assertThat(response).isNotNull();
      assertThat(response.isAcceptedFlag()).isFalse();
      assertThat(response.createdByUserId()).isEqualTo(777L); // Authenticated counselor used instead of spoofed 12345L
    }
  }

  @Nested
  @DisplayName("5. Remediation Enrollment Invariant Tests")
  class RemediationEnrollmentInvariantsTests {

    @Test
    @DisplayName("Remediation enrollment fails if recommendation is not accepted")
    void shouldRejectRemediationEnrollmentWhenRecommendationNotAccepted() {
      SkillGapRemediationEnrollmentRequest request = new SkillGapRemediationEnrollmentRequest();
      request.setRecommendationId(1001L);

      CreateTrainingEnrollmentRequest enrolReq = new CreateTrainingEnrollmentRequest();
      enrolReq.setCourseId(501L);
      request.setEnrollmentRequest(enrolReq);

      SkillGapRecommendationResponse unacceptedRec = buildRecommendationResponse(1001L, 201L, 501L, false);
      when(skillGapRecommendationService.getRecommendationById(1001L)).thenReturn(unacceptedRec);

      assertThatThrownBy(() -> workflowService.enrollTraineeInRemediationCourse(request))
          .isInstanceOf(BadRequestException.class)
          .extracting("errorCode").isEqualTo("RECOMMENDATION_NOT_ACCEPTED");

      verify(trainingEnrollmentService, never()).createEnrollment(any());
    }

    @Test
    @DisplayName("Remediation enrollment fails if recommendation is unaccepted even when recommendationStatusUpdate requests acceptance")
    void shouldRejectRemediationEnrollmentWhenRecommendationNotAcceptedEvenIfStatusUpdateRequestsAcceptance() {
      SkillGapRemediationEnrollmentRequest request = new SkillGapRemediationEnrollmentRequest();
      request.setRecommendationId(1001L);

      CreateTrainingEnrollmentRequest enrolReq = new CreateTrainingEnrollmentRequest();
      enrolReq.setCourseId(501L);
      request.setEnrollmentRequest(enrolReq);

      UpdateSkillGapRecommendationRequest updateReq = new UpdateSkillGapRecommendationRequest();
      updateReq.setIsAcceptedFlag(true);
      request.setRecommendationStatusUpdate(updateReq);

      // Persisted record in database is unaccepted (isAcceptedFlag = false)
      SkillGapRecommendationResponse unacceptedRec = buildRecommendationResponse(1001L, 201L, 501L, false);
      when(skillGapRecommendationService.getRecommendationById(1001L)).thenReturn(unacceptedRec);

      assertThatThrownBy(() -> workflowService.enrollTraineeInRemediationCourse(request))
          .isInstanceOf(BadRequestException.class)
          .extracting("errorCode").isEqualTo("RECOMMENDATION_NOT_ACCEPTED");

      verify(trainingEnrollmentService, never()).createEnrollment(any());
      verify(skillGapRecommendationService, never()).updateRecommendation(any(), any());
    }

    @Test
    @DisplayName("Remediation enrollment fails when recommendation was rejected (isAcceptedFlag=false, acceptedOn=null)")
    void shouldRejectRemediationEnrollmentWhenRecommendationIsRejected() {
      SkillGapRemediationEnrollmentRequest request = new SkillGapRemediationEnrollmentRequest();
      request.setRecommendationId(1001L);

      CreateTrainingEnrollmentRequest enrolReq = new CreateTrainingEnrollmentRequest();
      enrolReq.setCourseId(501L);
      request.setEnrollmentRequest(enrolReq);

      SkillGapRecommendationResponse rejectedRec = new SkillGapRecommendationResponse(
          1001L, 201L, "SG-2026-201", 1, 1L, "UPSKILL", "Upskill",
          501L, "CRS-501", "Course 501", null, null, null, false, null, "Declined by trainee",
          1L, LocalDateTime.now().minusDays(2), LocalDateTime.now());
      when(skillGapRecommendationService.getRecommendationById(1001L)).thenReturn(rejectedRec);

      assertThatThrownBy(() -> workflowService.enrollTraineeInRemediationCourse(request))
          .isInstanceOf(BadRequestException.class)
          .extracting("errorCode").isEqualTo("RECOMMENDATION_NOT_ACCEPTED");

      verify(trainingEnrollmentService, never()).createEnrollment(any());
    }

    @Test
    @DisplayName("Remediation enrollment succeeds when recommendation is already accepted and all validations pass")
    void shouldEnrollWhenRecommendationAlreadyAcceptedAndAllValidationsPass() {
      SkillGapRemediationEnrollmentRequest request = new SkillGapRemediationEnrollmentRequest();
      request.setRecommendationId(1001L);

      CreateTrainingEnrollmentRequest enrolReq = new CreateTrainingEnrollmentRequest();
      enrolReq.setCourseId(501L);
      request.setEnrollmentRequest(enrolReq);

      SkillGapRecommendationResponse acceptedRec = buildRecommendationResponse(1001L, 201L, 501L, true);
      when(skillGapRecommendationService.getRecommendationById(1001L)).thenReturn(acceptedRec);

      SkillGap mockGapEntity = buildRecommendationEntity(1001L, 201L, 501L, 101L, true, "OPEN", true).getSkillGap();
      when(skillGapRepository.findById(201L)).thenReturn(Optional.of(mockGapEntity));

      RefSkillGapStatus inRemediationStatus = new RefSkillGapStatus();
      inRemediationStatus.setStatusCode("IN_REMEDIATION");
      when(refSkillGapStatusRepository.findByStatusCode("IN_REMEDIATION")).thenReturn(Optional.of(inRemediationStatus));

      TrainingEnrollmentResponse mockEnrollment = new TrainingEnrollmentResponse();
      mockEnrollment.setId(3001L);
      when(trainingEnrollmentService.createEnrollment(any())).thenReturn(mockEnrollment);

      SkillGapRemediationEnrollmentResponse response = workflowService.enrollTraineeInRemediationCourse(request);

      assertThat(response).isNotNull();
      assertThat(response.createdEnrollment().getId()).isEqualTo(3001L);
      assertThat(response.recommendation().isAcceptedFlag()).isTrue();
      verify(skillGapRepository).save(mockGapEntity);
      assertThat(mockGapEntity.getSkillGapStatus().getStatusCode()).isEqualTo("IN_REMEDIATION");
    }

    @Test
    @DisplayName("Remediation enrollment fails closed when linked gap entity not found")
    void shouldFailClosedWhenLinkedGapEntityNotFound() {
      SkillGapRemediationEnrollmentRequest request = new SkillGapRemediationEnrollmentRequest();
      request.setRecommendationId(1001L);
      CreateTrainingEnrollmentRequest enrolReq = new CreateTrainingEnrollmentRequest();
      enrolReq.setCourseId(501L);
      request.setEnrollmentRequest(enrolReq);

      SkillGapRecommendationResponse acceptedRec = buildRecommendationResponse(1001L, 201L, 501L, true);
      when(skillGapRecommendationService.getRecommendationById(1001L)).thenReturn(acceptedRec);
      when(skillGapRepository.findById(201L)).thenReturn(Optional.empty());

      assertThatThrownBy(() -> workflowService.enrollTraineeInRemediationCourse(request))
          .isInstanceOf(ResourceNotFoundException.class);

      verify(trainingEnrollmentService, never()).createEnrollment(any());
    }

    @Test
    @DisplayName("Remediation enrollment fails closed when IN_REMEDIATION reference status record missing")
    void shouldFailClosedWhenInRemediationStatusMissing() {
      SkillGapRemediationEnrollmentRequest request = new SkillGapRemediationEnrollmentRequest();
      request.setRecommendationId(1001L);
      CreateTrainingEnrollmentRequest enrolReq = new CreateTrainingEnrollmentRequest();
      enrolReq.setCourseId(501L);
      request.setEnrollmentRequest(enrolReq);

      SkillGapRecommendationResponse acceptedRec = buildRecommendationResponse(1001L, 201L, 501L, true);
      when(skillGapRecommendationService.getRecommendationById(1001L)).thenReturn(acceptedRec);

      SkillGap mockGapEntity = buildRecommendationEntity(1001L, 201L, 501L, 101L, true, "OPEN", true).getSkillGap();
      when(skillGapRepository.findById(201L)).thenReturn(Optional.of(mockGapEntity));
      when(refSkillGapStatusRepository.findByStatusCode("IN_REMEDIATION")).thenReturn(Optional.empty());

      assertThatThrownBy(() -> workflowService.enrollTraineeInRemediationCourse(request))
          .isInstanceOf(ResourceNotFoundException.class);

      verify(trainingEnrollmentService, never()).createEnrollment(any());
      verify(skillGapRepository, never()).save(any());
    }

    @Test
    @DisplayName("Remediation enrollment fails when authenticated trainee does not match recommendation trainee")
    void shouldRejectEnrollmentWhenAuthenticatedTraineeMismatch() {
      SkillGapRemediationEnrollmentRequest request = new SkillGapRemediationEnrollmentRequest();
      request.setRecommendationId(1001L);
      CreateTrainingEnrollmentRequest enrolReq = new CreateTrainingEnrollmentRequest();
      enrolReq.setCourseId(501L);
      request.setEnrollmentRequest(enrolReq);

      SkillGapRecommendationResponse acceptedRec = buildRecommendationResponse(1001L, 201L, 501L, true);
      when(skillGapRecommendationService.getRecommendationById(1001L)).thenReturn(acceptedRec);

      SkillGap mockGapEntity = buildRecommendationEntity(1001L, 201L, 501L, 101L, true, "OPEN", true).getSkillGap();
      when(skillGapRepository.findById(201L)).thenReturn(Optional.of(mockGapEntity));

      // Authenticate as a different trainee
      UserPrincipal principal = new UserPrincipal(99L, "otherTrainee");
      SecurityContextHolder.getContext().setAuthentication(
          new UsernamePasswordAuthenticationToken(principal, null, Collections.emptyList()));

      Trainee otherTrainee = new Trainee();
      otherTrainee.setId(999L);
      when(traineeRepository.findByUserId(99L)).thenReturn(Optional.of(otherTrainee));

      assertThatThrownBy(() -> workflowService.enrollTraineeInRemediationCourse(request))
          .isInstanceOf(BadRequestException.class)
          .extracting("errorCode").isEqualTo("TRAINEE_MISMATCH");

      verify(trainingEnrollmentService, never()).createEnrollment(any());
    }

    @Test
    @DisplayName("Remediation enrollment fails when course does not teach the gap skill")
    void shouldRejectRemediationEnrollmentWhenCourseDoesNotTeachSkill() {
      SkillGapRemediationEnrollmentRequest request = new SkillGapRemediationEnrollmentRequest();
      request.setRecommendationId(1001L);

      CreateTrainingEnrollmentRequest enrolReq = new CreateTrainingEnrollmentRequest();
      enrolReq.setCourseId(501L);
      request.setEnrollmentRequest(enrolReq);

      SkillGapRecommendationResponse acceptedRec = buildRecommendationResponse(1001L, 201L, 501L, true);
      when(skillGapRecommendationService.getRecommendationById(1001L)).thenReturn(acceptedRec);

      SkillGap mockGapEntity = buildRecommendationEntity(1001L, 201L, 501L, 101L, true, "OPEN", true).getSkillGap();
      when(skillGapRepository.findById(201L)).thenReturn(Optional.of(mockGapEntity));

      when(courseSkillRepository.existsByCourseIdAndSkillId(501L, 101L)).thenReturn(false);

      assertThatThrownBy(() -> workflowService.enrollTraineeInRemediationCourse(request))
          .isInstanceOf(BadRequestException.class)
          .extracting("errorCode").isEqualTo("COURSE_SKILL_MISMATCH");

      verify(trainingEnrollmentService, never()).createEnrollment(any());
    }

    @Test
    @DisplayName("Remediation enrollment fails when gap is already in active remediation (prevent duplicates)")
    void shouldRejectRemediationEnrollmentWhenDuplicateActiveRemediation() {
      SkillGapRemediationEnrollmentRequest request = new SkillGapRemediationEnrollmentRequest();
      request.setRecommendationId(1001L);

      CreateTrainingEnrollmentRequest enrolReq = new CreateTrainingEnrollmentRequest();
      enrolReq.setCourseId(501L);
      request.setEnrollmentRequest(enrolReq);

      SkillGapRecommendationResponse acceptedRec = buildRecommendationResponse(1001L, 201L, 501L, true);
      when(skillGapRecommendationService.getRecommendationById(1001L)).thenReturn(acceptedRec);

      SkillGap mockGapEntity = buildRecommendationEntity(1001L, 201L, 501L, 101L, true, "IN_REMEDIATION", true).getSkillGap();
      when(skillGapRepository.findById(201L)).thenReturn(Optional.of(mockGapEntity));

      assertThatThrownBy(() -> workflowService.enrollTraineeInRemediationCourse(request))
          .isInstanceOf(BadRequestException.class)
          .extracting("errorCode").isEqualTo("DUPLICATE_REMEDIATION_ENROLLMENT");

      verify(trainingEnrollmentService, never()).createEnrollment(any());
    }

    @Test
    @DisplayName("Remediation enrollment fails when enrollment trainee does not match gap trainee")
    void shouldRejectRemediationEnrollmentWhenTraineeMismatch() {
      SkillGapRemediationEnrollmentRequest request = new SkillGapRemediationEnrollmentRequest();
      request.setRecommendationId(1001L);

      CreateTrainingEnrollmentRequest enrolReq = new CreateTrainingEnrollmentRequest();
      enrolReq.setCourseId(501L);
      enrolReq.setTraineeId(999L); // mismatch with TRAINEE_ID (100L)
      request.setEnrollmentRequest(enrolReq);

      SkillGapRecommendationResponse acceptedRec = buildRecommendationResponse(1001L, 201L, 501L, true);
      when(skillGapRecommendationService.getRecommendationById(1001L)).thenReturn(acceptedRec);

      SkillGap mockGapEntity = buildRecommendationEntity(1001L, 201L, 501L, 101L, true, "OPEN", true).getSkillGap();
      when(skillGapRepository.findById(201L)).thenReturn(Optional.of(mockGapEntity));

      assertThatThrownBy(() -> workflowService.enrollTraineeInRemediationCourse(request))
          .isInstanceOf(BadRequestException.class)
          .extracting("errorCode").isEqualTo("TRAINEE_MISMATCH");

      verify(trainingEnrollmentService, never()).createEnrollment(any());
    }

    @Test
    @DisplayName("Remediation enrollment fails when gap is inactive or already resolved")
    void shouldRejectRemediationEnrollmentWhenGapNotActiveOrResolved() {
      SkillGapRemediationEnrollmentRequest request = new SkillGapRemediationEnrollmentRequest();
      request.setRecommendationId(1001L);

      CreateTrainingEnrollmentRequest enrolReq = new CreateTrainingEnrollmentRequest();
      enrolReq.setCourseId(501L);
      request.setEnrollmentRequest(enrolReq);

      SkillGapRecommendationResponse acceptedRec = buildRecommendationResponse(1001L, 201L, 501L, true);
      when(skillGapRecommendationService.getRecommendationById(1001L)).thenReturn(acceptedRec);

      SkillGap mockGapEntity = buildRecommendationEntity(1001L, 201L, 501L, 101L, true, "RESOLVED", false).getSkillGap();
      when(skillGapRepository.findById(201L)).thenReturn(Optional.of(mockGapEntity));

      assertThatThrownBy(() -> workflowService.enrollTraineeInRemediationCourse(request))
          .isInstanceOf(BadRequestException.class)
          .extracting("errorCode").isEqualTo("SKILL_GAP_NOT_ACTIVE");

      verify(trainingEnrollmentService, never()).createEnrollment(any());
    }

    @Test
    @DisplayName("Remediation enrollment fails when recommendation does not specify a course")
    void shouldRejectRemediationEnrollmentWhenRecommendationMissingCourse() {
      SkillGapRemediationEnrollmentRequest request = new SkillGapRemediationEnrollmentRequest();
      request.setRecommendationId(1001L);

      CreateTrainingEnrollmentRequest enrolReq = new CreateTrainingEnrollmentRequest();
      request.setEnrollmentRequest(enrolReq);

      SkillGapRecommendationResponse recWithoutCourse = new SkillGapRecommendationResponse(
          1001L, 201L, "SG-2026-201", 1, 1L, "UPSKILL", "Upskill",
          null, null, null, null, null, null, true, LocalDate.now(), "Notes",
          1L, LocalDateTime.now().minusDays(1), LocalDateTime.now());
      when(skillGapRecommendationService.getRecommendationById(1001L)).thenReturn(recWithoutCourse);

      assertThatThrownBy(() -> workflowService.enrollTraineeInRemediationCourse(request))
          .isInstanceOf(BadRequestException.class)
          .extracting("errorCode").isEqualTo("RECOMMENDED_COURSE_MISSING");

      verify(trainingEnrollmentService, never()).createEnrollment(any());
    }

    @Test
    @DisplayName("Remediation enrollment fails when caller is unauthenticated")
    void shouldRejectEnrollmentWhenUnauthenticated() {
      SecurityContextHolder.clearContext();
      SkillGapRemediationEnrollmentRequest request = new SkillGapRemediationEnrollmentRequest();
      request.setRecommendationId(1001L);
      CreateTrainingEnrollmentRequest enrolReq = new CreateTrainingEnrollmentRequest();
      enrolReq.setCourseId(501L);
      request.setEnrollmentRequest(enrolReq);

      assertThatThrownBy(() -> workflowService.enrollTraineeInRemediationCourse(request))
          .isInstanceOf(InvalidCredentialsException.class)
          .hasMessageContaining("User is not authenticated");

      verify(trainingEnrollmentService, never()).createEnrollment(any());
    }

    @Test
    @DisplayName("Remediation enrollment fails when gap status is RESOLVED")
    void shouldRejectEnrollmentWhenGapStatusIsResolved() {
      SkillGapRemediationEnrollmentRequest request = new SkillGapRemediationEnrollmentRequest();
      request.setRecommendationId(1001L);
      CreateTrainingEnrollmentRequest enrolReq = new CreateTrainingEnrollmentRequest();
      enrolReq.setCourseId(501L);
      request.setEnrollmentRequest(enrolReq);

      SkillGapRecommendationResponse acceptedRec = buildRecommendationResponse(1001L, 201L, 501L, true);
      when(skillGapRecommendationService.getRecommendationById(1001L)).thenReturn(acceptedRec);

      SkillGap mockGapEntity = buildRecommendationEntity(1001L, 201L, 501L, 101L, true, "RESOLVED", true).getSkillGap();
      when(skillGapRepository.findById(201L)).thenReturn(Optional.of(mockGapEntity));

      assertThatThrownBy(() -> workflowService.enrollTraineeInRemediationCourse(request))
          .isInstanceOf(BadRequestException.class)
          .extracting("errorCode").isEqualTo("SKILL_GAP_NOT_ACTIVE");

      verify(trainingEnrollmentService, never()).createEnrollment(any());
      verify(skillGapRepository, never()).save(any());
    }

    @Test
    @DisplayName("Remediation enrollment fails when gap status is CLOSED_UNRESOLVED")
    void shouldRejectEnrollmentWhenGapStatusIsClosedUnresolved() {
      SkillGapRemediationEnrollmentRequest request = new SkillGapRemediationEnrollmentRequest();
      request.setRecommendationId(1001L);
      CreateTrainingEnrollmentRequest enrolReq = new CreateTrainingEnrollmentRequest();
      enrolReq.setCourseId(501L);
      request.setEnrollmentRequest(enrolReq);

      SkillGapRecommendationResponse acceptedRec = buildRecommendationResponse(1001L, 201L, 501L, true);
      when(skillGapRecommendationService.getRecommendationById(1001L)).thenReturn(acceptedRec);

      SkillGap mockGapEntity = buildRecommendationEntity(1001L, 201L, 501L, 101L, true, "CLOSED_UNRESOLVED", true).getSkillGap();
      when(skillGapRepository.findById(201L)).thenReturn(Optional.of(mockGapEntity));

      assertThatThrownBy(() -> workflowService.enrollTraineeInRemediationCourse(request))
          .isInstanceOf(BadRequestException.class)
          .extracting("errorCode").isEqualTo("SKILL_GAP_NOT_ACTIVE");

      verify(trainingEnrollmentService, never()).createEnrollment(any());
      verify(skillGapRepository, never()).save(any());
    }

    @Test
    @DisplayName("Remediation enrollment fails when gap status is SUPERSEDED")
    void shouldRejectEnrollmentWhenGapStatusIsSuperseded() {
      SkillGapRemediationEnrollmentRequest request = new SkillGapRemediationEnrollmentRequest();
      request.setRecommendationId(1001L);
      CreateTrainingEnrollmentRequest enrolReq = new CreateTrainingEnrollmentRequest();
      enrolReq.setCourseId(501L);
      request.setEnrollmentRequest(enrolReq);

      SkillGapRecommendationResponse acceptedRec = buildRecommendationResponse(1001L, 201L, 501L, true);
      when(skillGapRecommendationService.getRecommendationById(1001L)).thenReturn(acceptedRec);

      SkillGap mockGapEntity = buildRecommendationEntity(1001L, 201L, 501L, 101L, true, "SUPERSEDED", true).getSkillGap();
      when(skillGapRepository.findById(201L)).thenReturn(Optional.of(mockGapEntity));

      assertThatThrownBy(() -> workflowService.enrollTraineeInRemediationCourse(request))
          .isInstanceOf(BadRequestException.class)
          .extracting("errorCode").isEqualTo("SKILL_GAP_NOT_ACTIVE");

      verify(trainingEnrollmentService, never()).createEnrollment(any());
      verify(skillGapRepository, never()).save(any());
    }

    @Test
    @DisplayName("Remediation enrollment fails when gap is inactive (isCurrent = false)")
    void shouldRejectEnrollmentWhenGapIsInactive() {
      SkillGapRemediationEnrollmentRequest request = new SkillGapRemediationEnrollmentRequest();
      request.setRecommendationId(1001L);
      CreateTrainingEnrollmentRequest enrolReq = new CreateTrainingEnrollmentRequest();
      enrolReq.setCourseId(501L);
      request.setEnrollmentRequest(enrolReq);

      SkillGapRecommendationResponse acceptedRec = buildRecommendationResponse(1001L, 201L, 501L, true);
      when(skillGapRecommendationService.getRecommendationById(1001L)).thenReturn(acceptedRec);

      SkillGap mockGapEntity = buildRecommendationEntity(1001L, 201L, 501L, 101L, true, "OPEN", false).getSkillGap();
      when(skillGapRepository.findById(201L)).thenReturn(Optional.of(mockGapEntity));

      assertThatThrownBy(() -> workflowService.enrollTraineeInRemediationCourse(request))
          .isInstanceOf(BadRequestException.class)
          .extracting("errorCode").isEqualTo("SKILL_GAP_NOT_ACTIVE");

      verify(trainingEnrollmentService, never()).createEnrollment(any());
      verify(skillGapRepository, never()).save(any());
    }

    @Test
    @DisplayName("Remediation enrollment fails when gap status is not OPEN")
    void shouldRejectEnrollmentWhenGapStatusIsNotOpen() {
      SkillGapRemediationEnrollmentRequest request = new SkillGapRemediationEnrollmentRequest();
      request.setRecommendationId(1001L);
      CreateTrainingEnrollmentRequest enrolReq = new CreateTrainingEnrollmentRequest();
      enrolReq.setCourseId(501L);
      request.setEnrollmentRequest(enrolReq);

      SkillGapRecommendationResponse acceptedRec = buildRecommendationResponse(1001L, 201L, 501L, true);
      when(skillGapRecommendationService.getRecommendationById(1001L)).thenReturn(acceptedRec);

      SkillGap mockGapEntity = buildRecommendationEntity(1001L, 201L, 501L, 101L, true, "PENDING", true).getSkillGap();
      when(skillGapRepository.findById(201L)).thenReturn(Optional.of(mockGapEntity));

      assertThatThrownBy(() -> workflowService.enrollTraineeInRemediationCourse(request))
          .isInstanceOf(BadRequestException.class)
          .extracting("errorCode").isEqualTo("INVALID_GAP_STATUS");

      verify(trainingEnrollmentService, never()).createEnrollment(any());
      verify(skillGapRepository, never()).save(any());
    }

    @Test
    @DisplayName("Remediation enrollment fails if recommendationStatusUpdate attempts to mutate acceptance status, preserving enrollment and gap status")
    void shouldRejectEnrollmentWhenStatusUpdateAttemptsToMutateAcceptance() {
      SkillGapRemediationEnrollmentRequest request = new SkillGapRemediationEnrollmentRequest();
      request.setRecommendationId(1001L);
      CreateTrainingEnrollmentRequest enrolReq = new CreateTrainingEnrollmentRequest();
      enrolReq.setCourseId(501L);
      request.setEnrollmentRequest(enrolReq);

      UpdateSkillGapRecommendationRequest updateReq = new UpdateSkillGapRecommendationRequest();
      updateReq.setIsAcceptedFlag(false);
      request.setRecommendationStatusUpdate(updateReq);

      SkillGapRecommendationResponse acceptedRec = buildRecommendationResponse(1001L, 201L, 501L, true);
      when(skillGapRecommendationService.getRecommendationById(1001L)).thenReturn(acceptedRec);

      SkillGap mockGapEntity = buildRecommendationEntity(1001L, 201L, 501L, 101L, true, "OPEN", true).getSkillGap();
      when(skillGapRepository.findById(201L)).thenReturn(Optional.of(mockGapEntity));

      RefSkillGapStatus inRemediationStatus = new RefSkillGapStatus();
      inRemediationStatus.setStatusCode("IN_REMEDIATION");
      when(refSkillGapStatusRepository.findByStatusCode("IN_REMEDIATION")).thenReturn(Optional.of(inRemediationStatus));

      assertThatThrownBy(() -> workflowService.enrollTraineeInRemediationCourse(request))
          .isInstanceOf(BadRequestException.class)
          .extracting("errorCode").isEqualTo("INVALID_STATE_TRANSITION");

      // Verify that due to upfront validation, neither enrollment nor gap status transition occurred
      verify(trainingEnrollmentService, never()).createEnrollment(any());
      verify(skillGapRepository, never()).save(any());
      verify(skillGapRecommendationService, never()).updateRecommendation(any(), any());
      assertThat(mockGapEntity.getSkillGapStatus().getStatusCode()).isEqualTo("OPEN");
    }

    @Test
    @DisplayName("Remediation enrollment fails closed when TraineeRepository is unavailable")
    void shouldFailClosedWhenTraineeRepositoryIsNullOnEnrollment() {
      SkillGapRemediationWorkflowServiceImpl serviceWithoutTraineeRepo =
          new SkillGapRemediationWorkflowServiceImpl(
              skillGapAssessmentService,
              skillGapService,
              skillGapRecommendationService,
              trainingEnrollmentService,
              courseSkillRepository,
              skillGapRepository,
              skillGapRecommendationRepository,
              refSkillGapStatusRepository,
              null,
              new SkillGapRecommendationMapper()
          );

      SkillGapRemediationEnrollmentRequest request = new SkillGapRemediationEnrollmentRequest();
      request.setRecommendationId(1001L);
      CreateTrainingEnrollmentRequest enrolReq = new CreateTrainingEnrollmentRequest();
      enrolReq.setCourseId(501L);
      request.setEnrollmentRequest(enrolReq);

      SkillGapRecommendationResponse acceptedRec = buildRecommendationResponse(1001L, 201L, 501L, true);
      when(skillGapRecommendationService.getRecommendationById(1001L)).thenReturn(acceptedRec);

      SkillGap mockGapEntity = buildRecommendationEntity(1001L, 201L, 501L, 101L, true, "OPEN", true).getSkillGap();
      when(skillGapRepository.findById(201L)).thenReturn(Optional.of(mockGapEntity));

      assertThatThrownBy(() -> serviceWithoutTraineeRepo.enrollTraineeInRemediationCourse(request))
          .isInstanceOf(IllegalStateException.class)
          .hasMessageContaining("TraineeRepository is required for trainee ownership verification");

      verify(trainingEnrollmentService, never()).createEnrollment(any());
      verify(skillGapRepository, never()).save(any());
    }

    @Test
    @DisplayName("Remediation enrollment allows recommendation remarks update without acceptance mutation")
    void shouldAllowMetadataUpdateWithoutAcceptanceMutationDuringEnrollment() {
      SkillGapRemediationEnrollmentRequest request = new SkillGapRemediationEnrollmentRequest();
      request.setRecommendationId(1001L);
      CreateTrainingEnrollmentRequest enrolReq = new CreateTrainingEnrollmentRequest();
      enrolReq.setCourseId(501L);
      request.setEnrollmentRequest(enrolReq);

      UpdateSkillGapRecommendationRequest updateReq = new UpdateSkillGapRecommendationRequest();
      updateReq.setIsAcceptedFlag(null);
      updateReq.setRemarks("Updated enrollment remarks");
      request.setRecommendationStatusUpdate(updateReq);

      SkillGapRecommendationResponse acceptedRec = buildRecommendationResponse(1001L, 201L, 501L, true);
      when(skillGapRecommendationService.getRecommendationById(1001L)).thenReturn(acceptedRec);

      SkillGap mockGapEntity = buildRecommendationEntity(1001L, 201L, 501L, 101L, true, "OPEN", true).getSkillGap();
      when(skillGapRepository.findById(201L)).thenReturn(Optional.of(mockGapEntity));

      RefSkillGapStatus inRemediationStatus = new RefSkillGapStatus();
      inRemediationStatus.setStatusCode("IN_REMEDIATION");
      when(refSkillGapStatusRepository.findByStatusCode("IN_REMEDIATION")).thenReturn(Optional.of(inRemediationStatus));

      TrainingEnrollmentResponse mockEnrollment = new TrainingEnrollmentResponse();
      mockEnrollment.setId(3001L);
      when(trainingEnrollmentService.createEnrollment(any())).thenReturn(mockEnrollment);

      SkillGapRecommendationResponse updatedRec = new SkillGapRecommendationResponse(
          1001L, 201L, "SG-2026-201", 1, 1L, "UPSKILL", "Upskill",
          501L, "CRS-501", "Course 501", null, null, null, true, LocalDate.now(), "Updated enrollment remarks",
          1L, LocalDateTime.now().minusDays(1), LocalDateTime.now());
      when(skillGapRecommendationService.updateRecommendation(1001L, updateReq)).thenReturn(updatedRec);

      SkillGapRemediationEnrollmentResponse response = workflowService.enrollTraineeInRemediationCourse(request);

      assertThat(response).isNotNull();
      assertThat(response.recommendation().remarks()).isEqualTo("Updated enrollment remarks");
      verify(skillGapRecommendationService).updateRecommendation(1001L, updateReq);
      verify(trainingEnrollmentService).createEnrollment(any());
    }
  }
}
