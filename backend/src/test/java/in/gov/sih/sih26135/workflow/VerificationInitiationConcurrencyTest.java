package in.gov.sih.sih26135.workflow;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import in.gov.sih.sih26135.dto.request.CreateEmploymentVerificationRequestRequest;
import in.gov.sih.sih26135.dto.request.InitiateVerificationFromSurveyRequest;
import in.gov.sih.sih26135.dto.response.EmploymentRecordResponse;
import in.gov.sih.sih26135.dto.response.EmploymentVerificationRequestResponse;
import in.gov.sih.sih26135.dto.response.SurveyResponseResponse;
import in.gov.sih.sih26135.entity.EmploymentRecord;
import in.gov.sih.sih26135.entity.EmploymentVerificationRequest;
import in.gov.sih.sih26135.entity.RefEmploymentVerificationRequestStatus;
import in.gov.sih.sih26135.entity.Trainee;
import in.gov.sih.sih26135.exception.ConflictException;
import in.gov.sih.sih26135.repository.EmployerRepository;
import in.gov.sih.sih26135.repository.EmploymentRecordRepository;
import in.gov.sih.sih26135.repository.EmploymentVerificationAttemptRepository;
import in.gov.sih.sih26135.repository.EmploymentVerificationRepository;
import in.gov.sih.sih26135.repository.EmploymentVerificationRequestRepository;
import in.gov.sih.sih26135.repository.FollowupTaskRepository;
import in.gov.sih.sih26135.repository.PlacementRecordRepository;
import in.gov.sih.sih26135.repository.RefEmploymentInfoSourceRepository;
import in.gov.sih.sih26135.repository.RefEmploymentVerificationMethodRepository;
import in.gov.sih.sih26135.repository.RefEmploymentVerificationRequestStatusRepository;
import in.gov.sih.sih26135.repository.SurveyResponseRepository;
import in.gov.sih.sih26135.repository.UserRepository;
import in.gov.sih.sih26135.service.EmploymentRecordService;
import in.gov.sih.sih26135.service.EmploymentVerificationAttemptService;
import in.gov.sih.sih26135.service.EmploymentVerificationEvidenceService;
import in.gov.sih.sih26135.service.EmploymentVerificationRequestService;
import in.gov.sih.sih26135.service.EmploymentVerificationService;
import in.gov.sih.sih26135.service.FollowupTaskService;
import in.gov.sih.sih26135.service.SurveyResponseService;
import in.gov.sih.sih26135.service.impl.EmploymentVerificationRequestServiceImpl;
import in.gov.sih.sih26135.service.impl.VerificationWorkflowServiceImpl;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class VerificationInitiationConcurrencyTest {

  @Mock private EmploymentRecordService employmentRecordService;
  @Mock private FollowupTaskService followupTaskService;
  @Mock private SurveyResponseService surveyResponseService;
  @Mock private EmploymentVerificationRequestService employmentVerificationRequestService;
  @Mock private EmploymentVerificationAttemptService employmentVerificationAttemptService;
  @Mock private EmploymentVerificationService employmentVerificationService;
  @Mock private EmploymentVerificationEvidenceService employmentVerificationEvidenceService;

  // Direct service mocks for cycle number guard verification
  @Mock private EmploymentVerificationRequestRepository requestRepository;
  @Mock private EmploymentRecordRepository employmentRecordRepository;
  @Mock private PlacementRecordRepository placementRecordRepository;
  @Mock private EmployerRepository employerRepository;
  @Mock private RefEmploymentVerificationRequestStatusRepository requestStatusRepository;
  @Mock private RefEmploymentVerificationMethodRepository verificationMethodRepository;
  @Mock private RefEmploymentInfoSourceRepository infoSourceRepository;
  @Mock private UserRepository userRepository;
  @Mock private FollowupTaskRepository followupTaskRepository;
  @Mock private SurveyResponseRepository surveyResponseRepository;
  @Mock private EmploymentVerificationAttemptRepository attemptRepository;
  @Mock private EmploymentVerificationRepository verificationRepository;

  private VerificationWorkflowServiceImpl workflowService;
  private EmploymentVerificationRequestServiceImpl requestService;

  @BeforeEach
  void setUp() {
    workflowService = new VerificationWorkflowServiceImpl(
        employmentRecordService, followupTaskService, surveyResponseService,
        employmentVerificationRequestService, employmentVerificationAttemptService,
        employmentVerificationService, employmentVerificationEvidenceService
    );

    requestService = new EmploymentVerificationRequestServiceImpl(
        requestRepository, employmentRecordRepository, placementRecordRepository,
        employerRepository, requestStatusRepository, verificationMethodRepository,
        infoSourceRepository, userRepository, followupTaskRepository,
        surveyResponseRepository, attemptRepository, verificationRepository, null
    );
  }

  private EmploymentRecordResponse buildEmpResponse(Long empId, Long traineeId) {
    return new EmploymentRecordResponse(
        empId, "EMP-" + empId, traineeId, "REG-" + traineeId, "Aditya", "Kumar",
        200L, "ENR-200", 10L, "PLC-001", 600L, "Tech Corp",
        700L, "Branch A", 800L, "Software Engineer", 1L, "REGULAR_WAGE",
        1L, "ACTIVE", "Active spell", true,
        LocalDate.of(2026, 1, 1), null, true,
        new BigDecimal("50000.00"), 1L, "MONTHLY", "INR",
        null, 1L, 1L, 1L, "DIRECT", "Direct",
        1L, "VERIFIED", LocalDateTime.now(), 1L,
        null, null, null, null,
        LocalDateTime.now(), LocalDateTime.now(), null
    );
  }

  private SurveyResponseResponse buildSurveyResponse(Long surveyId, Long traineeId) {
    SurveyResponseResponse resp = new SurveyResponseResponse();
    resp.setId(surveyId);
    resp.setTraineeId(traineeId);
    return resp;
  }

  private EmploymentVerificationRequestResponse buildRequestResponse(
      Long reqId, Long empId, int cycle, String statusCode, boolean isOpen, Long surveyId) {
    return new EmploymentVerificationRequestResponse(
        reqId, "VR-" + reqId, empId, "EMP-" + empId,
        100L, "REG-100", "Aditya", "Kumar",
        10L, "PLC-001", 600L, "Tech Corp",
        cycle, cycle > 1, 1L, statusCode, statusCode,
        isOpen, !isOpen, 1L, "PHYSICAL", "Physical",
        1L, "DIRECT", "Direct", 1L, 2L,
        null, surveyId, LocalDateTime.now(), null,
        null, null, isOpen ? 1 : null, "Remarks",
        LocalDateTime.now(), LocalDateTime.now()
    );
  }

  @Test
  @DisplayName("First verification initiation from survey succeeds when no pending requests exist")
  void testInitiateVerification_firstInitiation_success() {
    when(surveyResponseService.getSurveyResponseById(10L)).thenReturn(buildSurveyResponse(10L, 100L));
    when(employmentRecordService.getEmploymentRecordById(1L)).thenReturn(buildEmpResponse(1L, 100L));
    when(employmentVerificationRequestService.getRequestsByEmploymentId(1L)).thenReturn(Collections.emptyList());

    EmploymentVerificationRequestResponse expectedResponse = buildRequestResponse(101L, 1L, 1, "REQUESTED", true, 10L);
    when(employmentVerificationRequestService.createVerificationRequest(any())).thenReturn(expectedResponse);

    CreateEmploymentVerificationRequestRequest details = new CreateEmploymentVerificationRequestRequest();
    details.setEmploymentRecordId(1L);
    details.setRequestNumber("VR-101");

    InitiateVerificationFromSurveyRequest request = new InitiateVerificationFromSurveyRequest(10L, details);

    EmploymentVerificationRequestResponse result = workflowService.initiateVerificationFromSurvey(request);

    assertThat(result).isNotNull();
    assertThat(result.cycleNumber()).isEqualTo(1);
    verify(employmentVerificationRequestService).createVerificationRequest(any());
  }

  @Test
  @DisplayName("Duplicate initiation while previous request is pending is rejected with OPEN_REQUEST_EXISTS")
  void testInitiateVerification_duplicateWhileOpen_rejectedWithConflict() {
    when(surveyResponseService.getSurveyResponseById(10L)).thenReturn(buildSurveyResponse(10L, 100L));
    when(employmentRecordService.getEmploymentRecordById(1L)).thenReturn(buildEmpResponse(1L, 100L));

    EmploymentVerificationRequestResponse pendingRequest = buildRequestResponse(101L, 1L, 1, "REQUESTED", true, null);
    when(employmentVerificationRequestService.getRequestsByEmploymentId(1L)).thenReturn(List.of(pendingRequest));

    CreateEmploymentVerificationRequestRequest details = new CreateEmploymentVerificationRequestRequest();
    details.setEmploymentRecordId(1L);

    InitiateVerificationFromSurveyRequest request = new InitiateVerificationFromSurveyRequest(10L, details);

    assertThatThrownBy(() -> workflowService.initiateVerificationFromSurvey(request))
        .isInstanceOf(ConflictException.class)
        .extracting("errorCode").isEqualTo("OPEN_REQUEST_EXISTS");

    verify(employmentVerificationRequestService, never()).createVerificationRequest(any());
  }

  @Test
  @DisplayName("Duplicate initiation for the exact same survey response is rejected with OPEN_REQUEST_EXISTS")
  void testInitiateVerification_duplicateForSameSurveyResponse_rejected() {
    when(surveyResponseService.getSurveyResponseById(10L)).thenReturn(buildSurveyResponse(10L, 100L));
    when(employmentRecordService.getEmploymentRecordById(1L)).thenReturn(buildEmpResponse(1L, 100L));

    EmploymentVerificationRequestResponse priorSurveyRequest = buildRequestResponse(101L, 1L, 1, "IN_PROGRESS", true, 10L);
    when(employmentVerificationRequestService.getRequestsByEmploymentId(1L)).thenReturn(List.of(priorSurveyRequest));

    CreateEmploymentVerificationRequestRequest details = new CreateEmploymentVerificationRequestRequest();
    details.setEmploymentRecordId(1L);

    InitiateVerificationFromSurveyRequest request = new InitiateVerificationFromSurveyRequest(10L, details);

    assertThatThrownBy(() -> workflowService.initiateVerificationFromSurvey(request))
        .isInstanceOf(ConflictException.class)
        .extracting("errorCode").isEqualTo("OPEN_REQUEST_EXISTS");

    verify(employmentVerificationRequestService, never()).createVerificationRequest(any());
  }

  @Test
  @DisplayName("Re-initiation after a previous cycle reaches terminal status (COMPLETED) succeeds with cycle 2")
  void testInitiateVerification_reinitiationAfterTerminalStatus_success() {
    when(surveyResponseService.getSurveyResponseById(20L)).thenReturn(buildSurveyResponse(20L, 100L));
    when(employmentRecordService.getEmploymentRecordById(1L)).thenReturn(buildEmpResponse(1L, 100L));

    // Previous cycle completed (isOpen = false)
    EmploymentVerificationRequestResponse terminalRequest = buildRequestResponse(101L, 1L, 1, "COMPLETED", false, 10L);
    when(employmentVerificationRequestService.getRequestsByEmploymentId(1L)).thenReturn(List.of(terminalRequest));

    EmploymentVerificationRequestResponse cycle2Response = buildRequestResponse(102L, 1L, 2, "REQUESTED", true, 20L);
    when(employmentVerificationRequestService.createVerificationRequest(any())).thenReturn(cycle2Response);

    CreateEmploymentVerificationRequestRequest details = new CreateEmploymentVerificationRequestRequest();
    details.setEmploymentRecordId(1L);
    details.setRequestNumber("VR-102");

    InitiateVerificationFromSurveyRequest request = new InitiateVerificationFromSurveyRequest(20L, details);

    EmploymentVerificationRequestResponse result = workflowService.initiateVerificationFromSurvey(request);

    assertThat(result).isNotNull();
    assertThat(result.cycleNumber()).isEqualTo(2);
    assertThat(result.isReverification()).isTrue();
    verify(employmentVerificationRequestService).createVerificationRequest(any());
  }

  @Test
  @DisplayName("createVerificationRequest rejects open request before incrementing cycle counter")
  void testCreateVerificationRequest_openRequestExists_failsBeforeIncrementingCycle() {
    EmploymentRecord employment = new EmploymentRecord();
    employment.setId(1L);
    Trainee trainee = new Trainee();
    trainee.setId(100L);
    employment.setTrainee(trainee);

    when(employmentRecordRepository.findById(1L)).thenReturn(Optional.of(employment));
    when(requestRepository.existsByRequestNumber("VR-200")).thenReturn(false);

    RefEmploymentVerificationRequestStatus requestedStatus = new RefEmploymentVerificationRequestStatus("REQUESTED", "Requested", true, false, 1);
    when(requestStatusRepository.findByStatusCode("REQUESTED")).thenReturn(Optional.of(requestedStatus));

    // Existing open request
    EmploymentVerificationRequest openEntity = new EmploymentVerificationRequest();
    openEntity.setId(10L);
    openEntity.setCycleNumber(1);
    openEntity.setEmploymentVerificationRequestStatus(requestedStatus);
    when(requestRepository.findByEmploymentRecordId(1L)).thenReturn(List.of(openEntity));

    CreateEmploymentVerificationRequestRequest request = new CreateEmploymentVerificationRequestRequest();
    request.setEmploymentRecordId(1L);
    request.setRequestNumber("VR-200");

    assertThatThrownBy(() -> requestService.createVerificationRequest(request))
        .isInstanceOf(ConflictException.class)
        .extracting("errorCode").isEqualTo("OPEN_REQUEST_EXISTS");

    // Verified that cycle counter is not incremented and no entity is saved
    verify(requestRepository, never()).save(any());
  }
}
