package in.gov.sih.sih26135.workflow;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import in.gov.sih.sih26135.dto.request.CreateEmploymentRecordRequest;
import in.gov.sih.sih26135.dto.request.CreateSalaryHistoryRequest;
import in.gov.sih.sih26135.dto.request.UnemploymentToEmploymentTransitionRequest;
import in.gov.sih.sih26135.dto.request.UpdateTraineeUnemploymentEventRequest;
import in.gov.sih.sih26135.dto.response.EmploymentRecordResponse;
import in.gov.sih.sih26135.dto.response.TraineeUnemploymentEventResponse;
import in.gov.sih.sih26135.dto.response.UnemploymentToEmploymentTransitionResponse;
import in.gov.sih.sih26135.entity.EmploymentRecord;
import in.gov.sih.sih26135.entity.RefEmploymentInfoSource;
import in.gov.sih.sih26135.entity.RefRecordVerificationStatus;
import in.gov.sih.sih26135.entity.RefUnemploymentReason;
import in.gov.sih.sih26135.entity.Trainee;
import in.gov.sih.sih26135.entity.TraineeUnemploymentEvent;
import in.gov.sih.sih26135.exception.BadRequestException;
import in.gov.sih.sih26135.exception.ConflictException;
import in.gov.sih.sih26135.repository.TraineeUnemploymentEventRepository;
import in.gov.sih.sih26135.service.EmploymentExitEventService;
import in.gov.sih.sih26135.service.EmploymentRecordService;
import in.gov.sih.sih26135.service.SalaryHistoryService;
import in.gov.sih.sih26135.service.TraineeUnemploymentEventService;
import in.gov.sih.sih26135.service.impl.EmploymentLifecycleWorkflowServiceImpl;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class UnemploymentToEmploymentTransitionTest {

  @Mock private EmploymentRecordService employmentRecordService;
  @Mock private EmploymentExitEventService employmentExitEventService;
  @Mock private TraineeUnemploymentEventService traineeUnemploymentEventService;
  @Mock private SalaryHistoryService salaryHistoryService;
  @Mock private TraineeUnemploymentEventRepository traineeUnemploymentEventRepository;

  private EmploymentLifecycleWorkflowServiceImpl workflowService;

  @BeforeEach
  void setUp() {
    workflowService = new EmploymentLifecycleWorkflowServiceImpl(
        employmentRecordService, employmentExitEventService,
        traineeUnemploymentEventService, salaryHistoryService,
        traineeUnemploymentEventRepository
    );
  }

  private TraineeUnemploymentEvent buildUnemploymentEntity(
      Long id, Long traineeId, Boolean isCurrent, LocalDate endDate, EmploymentRecord succeedingEmployment) {
    Trainee trainee = new Trainee();
    trainee.setId(traineeId);

    TraineeUnemploymentEvent event = new TraineeUnemploymentEvent();
    event.setId(id);
    event.setTrainee(trainee);
    event.setPeriodNumber(1);
    event.setStartDate(LocalDate.of(2025, 10, 1));
    event.setEndDate(endDate);
    event.setIsCurrent(isCurrent);
    event.setLabourStatusId(1L);

    RefUnemploymentReason reason = new RefUnemploymentReason();
    reason.setId(1L);
    reason.setReasonCode("COMPLETION");
    reason.setReasonName("Program completed");
    event.setUnemploymentReason(reason);

    RefEmploymentInfoSource source = new RefEmploymentInfoSource();
    source.setId(1L);
    source.setSourceCode("SURVEY");
    source.setSourceName("Survey");
    event.setEmploymentInfoSource(source);

    RefRecordVerificationStatus status = new RefRecordVerificationStatus();
    status.setId(1L);
    status.setStatusCode("VERIFIED");
    status.setStatusName("Verified");
    event.setRecordVerificationStatus(status);

    event.setSucceedingEmployment(succeedingEmployment);
    return event;
  }

  private TraineeUnemploymentEventResponse buildUnemploymentResponse(
      Boolean isCurrent, LocalDate endDate, Long succeedingEmploymentId) {
    return new TraineeUnemploymentEventResponse(
        50L, 100L, "REG-100", "Aditya", "Kumar",
        1, LocalDate.of(2025, 10, 1), endDate, isCurrent,
        isCurrent ? 1 : null, 1L, 1L, "COMPLETION", "Program completed",
        null, null, null, succeedingEmploymentId,
        succeedingEmploymentId != null ? "EMP-OLD" : null,
        200L, "ENR-200", null, null, null, null,
        1L, "SURVEY", "Survey",
        1L, "VERIFIED", "Verified",
        LocalDateTime.now(), 1L, "Remarks",
        LocalDateTime.now(), LocalDateTime.now()
    );
  }

  @Test
  @DisplayName("Currently eligible unemployment event successfully transitions to employment using locked entity")
  void testTransitionUnemployment_currentEligibleEvent_success() {
    TraineeUnemploymentEvent activeEntity = buildUnemploymentEntity(50L, 100L, true, null, null);
    when(traineeUnemploymentEventRepository.findByIdForUpdate(50L)).thenReturn(Optional.of(activeEntity));

    CreateEmploymentRecordRequest empReq = new CreateEmploymentRecordRequest();
    empReq.setEmploymentNumber("EMP-NEW");
    empReq.setStartDate(LocalDate.of(2026, 1, 15));
    empReq.setEmploymentSpellStatusId(1L);
    empReq.setEmploymentInfoSourceId(1L);
    empReq.setRecordVerificationStatusId(1L);

    UnemploymentToEmploymentTransitionRequest request = new UnemploymentToEmploymentTransitionRequest();
    request.setUnemploymentEventId(50L);
    request.setNewEmploymentRequest(empReq);

    EmploymentRecordResponse empResp = org.mockito.Mockito.mock(EmploymentRecordResponse.class);
    when(empResp.id()).thenReturn(88L);
    when(empResp.startDate()).thenReturn(LocalDate.of(2026, 1, 15));
    when(employmentRecordService.createEmploymentRecord(any())).thenReturn(empResp);

    TraineeUnemploymentEventResponse updatedEvent = buildUnemploymentResponse(false, LocalDate.of(2026, 1, 15), 88L);
    when(traineeUnemploymentEventService.updateUnemploymentEvent(anyLong(), any())).thenReturn(updatedEvent);

    UnemploymentToEmploymentTransitionResponse result = workflowService.transitionUnemploymentToEmployment(request);

    assertThat(result).isNotNull();
    assertThat(result.newEmploymentRecord().id()).isEqualTo(88L);
    assertThat(result.updatedUnemploymentEvent().isCurrent()).isFalse();

    ArgumentCaptor<UpdateTraineeUnemploymentEventRequest> captor =
        ArgumentCaptor.forClass(UpdateTraineeUnemploymentEventRequest.class);
    verify(traineeUnemploymentEventService).updateUnemploymentEvent(anyLong(), captor.capture());
    assertThat(captor.getValue().getIsCurrent()).isFalse();
    assertThat(captor.getValue().getEndDate()).isEqualTo(LocalDate.of(2026, 1, 15));
    assertThat(captor.getValue().getSucceedingEmploymentId()).isEqualTo(88L);
  }

  @Test
  @DisplayName("Stale unemployment event with isCurrent=false is rejected before creating employment")
  void testTransitionUnemployment_staleEventWithIsCurrentFalse_rejected() {
    TraineeUnemploymentEvent closedEntity = buildUnemploymentEntity(50L, 100L, false, LocalDate.of(2025, 12, 1), null);
    when(traineeUnemploymentEventRepository.findByIdForUpdate(50L)).thenReturn(Optional.of(closedEntity));

    UnemploymentToEmploymentTransitionRequest request = new UnemploymentToEmploymentTransitionRequest();
    request.setUnemploymentEventId(50L);
    request.setNewEmploymentRequest(new CreateEmploymentRecordRequest());

    assertThatThrownBy(() -> workflowService.transitionUnemploymentToEmployment(request))
        .isInstanceOf(ConflictException.class)
        .extracting("errorCode").isEqualTo("UNEMPLOYMENT_EVENT_NOT_ACTIVE");

    verify(employmentRecordService, never()).createEmploymentRecord(any());
    verify(traineeUnemploymentEventService, never()).updateUnemploymentEvent(anyLong(), any());
    verify(salaryHistoryService, never()).createSalaryHistory(any());
  }

  @Test
  @DisplayName("Closed unemployment event with endDate set is rejected before creating employment")
  void testTransitionUnemployment_staleEventWithEndDate_rejected() {
    TraineeUnemploymentEvent closedEntity = buildUnemploymentEntity(50L, 100L, true, LocalDate.of(2025, 12, 1), null);
    when(traineeUnemploymentEventRepository.findByIdForUpdate(50L)).thenReturn(Optional.of(closedEntity));

    UnemploymentToEmploymentTransitionRequest request = new UnemploymentToEmploymentTransitionRequest();
    request.setUnemploymentEventId(50L);
    request.setNewEmploymentRequest(new CreateEmploymentRecordRequest());

    assertThatThrownBy(() -> workflowService.transitionUnemploymentToEmployment(request))
        .isInstanceOf(ConflictException.class)
        .extracting("errorCode").isEqualTo("UNEMPLOYMENT_EVENT_NOT_ACTIVE");

    verify(employmentRecordService, never()).createEmploymentRecord(any());
    verify(salaryHistoryService, never()).createSalaryHistory(any());
  }

  @Test
  @DisplayName("Unemployment event already transitioned (succeedingEmployment != null) is rejected without duplicate employment")
  void testTransitionUnemployment_staleEventWithSucceedingEmployment_rejected() {
    EmploymentRecord priorEmployment = new EmploymentRecord();
    priorEmployment.setId(99L);
    TraineeUnemploymentEvent transitionedEntity = buildUnemploymentEntity(50L, 100L, true, null, priorEmployment);
    when(traineeUnemploymentEventRepository.findByIdForUpdate(50L)).thenReturn(Optional.of(transitionedEntity));

    UnemploymentToEmploymentTransitionRequest request = new UnemploymentToEmploymentTransitionRequest();
    request.setUnemploymentEventId(50L);
    request.setNewEmploymentRequest(new CreateEmploymentRecordRequest());

    assertThatThrownBy(() -> workflowService.transitionUnemploymentToEmployment(request))
        .isInstanceOf(ConflictException.class)
        .extracting("errorCode").isEqualTo("UNEMPLOYMENT_EVENT_NOT_ACTIVE");

    verify(employmentRecordService, never()).createEmploymentRecord(any());
    verify(salaryHistoryService, never()).createSalaryHistory(any());
  }

  @Test
  @DisplayName("Trainee mismatch between unemployment event and new employment request is rejected")
  void testTransitionUnemployment_traineeMismatch_rejected() {
    TraineeUnemploymentEvent activeEntity = buildUnemploymentEntity(50L, 100L, true, null, null);
    when(traineeUnemploymentEventRepository.findByIdForUpdate(50L)).thenReturn(Optional.of(activeEntity));

    CreateEmploymentRecordRequest empReq = new CreateEmploymentRecordRequest();
    empReq.setTraineeId(999L); // Mismatched trainee

    UnemploymentToEmploymentTransitionRequest request = new UnemploymentToEmploymentTransitionRequest();
    request.setUnemploymentEventId(50L);
    request.setNewEmploymentRequest(empReq);

    assertThatThrownBy(() -> workflowService.transitionUnemploymentToEmployment(request))
        .isInstanceOf(BadRequestException.class)
        .extracting("errorCode").isEqualTo("TRAINEE_UNEMPLOYMENT_MISMATCH");

    verify(employmentRecordService, never()).createEmploymentRecord(any());
    verify(salaryHistoryService, never()).createSalaryHistory(any());
  }
}
