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
import in.gov.sih.sih26135.dto.request.PlacementToEmploymentTransitionRequest;
import in.gov.sih.sih26135.dto.response.EmploymentRecordResponse;
import in.gov.sih.sih26135.dto.response.PlacementRecordResponse;
import in.gov.sih.sih26135.dto.response.PlacementToEmploymentTransitionResponse;
import in.gov.sih.sih26135.dto.response.SalaryHistoryResponse;
import in.gov.sih.sih26135.entity.EmploymentRecord;
import in.gov.sih.sih26135.entity.RefPlacementStatus;
import in.gov.sih.sih26135.exception.BadRequestException;
import in.gov.sih.sih26135.exception.ConflictException;
import in.gov.sih.sih26135.repository.EmploymentRecordRepository;
import in.gov.sih.sih26135.repository.RefJoiningStatusRepository;
import in.gov.sih.sih26135.repository.RefPlacementStatusRepository;
import in.gov.sih.sih26135.service.EmploymentRecordService;
import in.gov.sih.sih26135.service.PlacementRecordService;
import in.gov.sih.sih26135.service.SalaryHistoryService;
import in.gov.sih.sih26135.service.impl.PlacementEmploymentWorkflowServiceImpl;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PlacementToEmploymentTransitionTest {

  @Mock private PlacementRecordService placementRecordService;
  @Mock private EmploymentRecordService employmentRecordService;
  @Mock private SalaryHistoryService salaryHistoryService;
  @Mock private EmploymentRecordRepository employmentRecordRepository;
  @Mock private RefPlacementStatusRepository refPlacementStatusRepository;
  @Mock private RefJoiningStatusRepository refJoiningStatusRepository;

  private PlacementEmploymentWorkflowServiceImpl workflowService;

  @BeforeEach
  void setUp() {
    workflowService = new PlacementEmploymentWorkflowServiceImpl(
        placementRecordService, employmentRecordService, salaryHistoryService,
        employmentRecordRepository, refPlacementStatusRepository, refJoiningStatusRepository
    );
  }

  private PlacementRecordResponse buildPlacementResponse(String statusCode, String joiningCode, LocalDateTime deletedAt) {
    return new PlacementRecordResponse(
        10L, "PLC-001", 100L, "REG-100", "Aditya", "Kumar",
        200L, "ENR-200", 300L, "Course A", 400L, "Prog A",
        500L, "Provider A", 600L, "Tech Corp", 700L, "Branch A",
        null, null, null, 800L, "Software Engineer",
        1L, "REGULAR_WAGE", 1L, "CAMPUS",
        2L, statusCode, 3L, joiningCode,
        new BigDecimal("50000.00"), new BigDecimal("50000.00"),
        1L, "MONTHLY", "INR",
        LocalDate.of(2026, 1, 15), LocalDate.of(2026, 2, 1), LocalDate.of(2026, 2, 1),
        null, null, "Hired",
        1L, 1L, 1L, "VERIFIED",
        LocalDateTime.now(), 1L, LocalDateTime.now(), LocalDateTime.now(),
        deletedAt
    );
  }

  @Test
  @DisplayName("Valid source status (OFFERED) successfully transitions placement to employment")
  void testTransitionPlacement_validSourceStatusOffered_success() {
    PlacementRecordResponse placement = buildPlacementResponse("OFFERED", "PENDING", null);
    when(placementRecordService.getPlacementRecordById(10L)).thenReturn(placement);
    when(employmentRecordRepository.findByPlacementRecordId(10L)).thenReturn(Optional.empty());

    CreateEmploymentRecordRequest empReq = new CreateEmploymentRecordRequest();
    empReq.setEmploymentNumber("EMP-OFFERED");
    empReq.setEmploymentSpellStatusId(1L);
    empReq.setEmploymentInfoSourceId(1L);
    empReq.setRecordVerificationStatusId(1L);

    PlacementToEmploymentTransitionRequest request = new PlacementToEmploymentTransitionRequest();
    request.setPlacementId(10L);
    request.setEmploymentRecordRequest(empReq);

    EmploymentRecordResponse empResp = new EmploymentRecordResponse(
        1L, "EMP-OFFERED", 100L, "REG-100", "Aditya", "Kumar",
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
    when(employmentRecordService.createEmploymentRecord(any())).thenReturn(empResp);

    PlacementToEmploymentTransitionResponse result = workflowService.transitionPlacementToEmployment(request);

    assertThat(result).isNotNull();
    assertThat(result.employmentRecord()).isNotNull();
    assertThat(result.employmentRecord().employmentNumber()).isEqualTo("EMP-OFFERED");
    verify(employmentRecordService).createEmploymentRecord(any());
  }

  @Test
  @DisplayName("Valid source status (ACCEPTED) successfully transitions placement to employment")
  void testTransitionPlacement_validSourceStatusAccepted_success() {
    PlacementRecordResponse placement = buildPlacementResponse("ACCEPTED", "PENDING", null);
    when(placementRecordService.getPlacementRecordById(10L)).thenReturn(placement);
    when(employmentRecordRepository.findByPlacementRecordId(10L)).thenReturn(Optional.empty());

    CreateEmploymentRecordRequest empReq = new CreateEmploymentRecordRequest();
    empReq.setEmploymentNumber("EMP-ACCEPTED");
    empReq.setEmploymentSpellStatusId(1L);
    empReq.setEmploymentInfoSourceId(1L);
    empReq.setRecordVerificationStatusId(1L);

    PlacementToEmploymentTransitionRequest request = new PlacementToEmploymentTransitionRequest();
    request.setPlacementId(10L);
    request.setEmploymentRecordRequest(empReq);

    EmploymentRecordResponse empResp = new EmploymentRecordResponse(
        1L, "EMP-ACCEPTED", 100L, "REG-100", "Aditya", "Kumar",
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
    when(employmentRecordService.createEmploymentRecord(any())).thenReturn(empResp);

    PlacementToEmploymentTransitionResponse result = workflowService.transitionPlacementToEmployment(request);

    assertThat(result).isNotNull();
    assertThat(result.employmentRecord()).isNotNull();
    assertThat(result.employmentRecord().employmentNumber()).isEqualTo("EMP-ACCEPTED");
    verify(employmentRecordService).createEmploymentRecord(any());
  }

  @Test
  @DisplayName("Valid source status (JOINED) successfully transitions placement to employment")
  void testTransitionPlacement_validSourceStatusJoined_success() {
    PlacementRecordResponse placement = buildPlacementResponse("JOINED", "JOINED", null);
    when(placementRecordService.getPlacementRecordById(10L)).thenReturn(placement);
    when(employmentRecordRepository.findByPlacementRecordId(10L)).thenReturn(Optional.empty());

    CreateEmploymentRecordRequest empReq = new CreateEmploymentRecordRequest();
    empReq.setEmploymentNumber("EMP-JOINED");
    empReq.setEmploymentSpellStatusId(1L);
    empReq.setEmploymentInfoSourceId(1L);
    empReq.setRecordVerificationStatusId(1L);

    PlacementToEmploymentTransitionRequest request = new PlacementToEmploymentTransitionRequest();
    request.setPlacementId(10L);
    request.setEmploymentRecordRequest(empReq);

    EmploymentRecordResponse empResp = new EmploymentRecordResponse(
        1L, "EMP-JOINED", 100L, "REG-100", "Aditya", "Kumar",
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
    when(employmentRecordService.createEmploymentRecord(any())).thenReturn(empResp);

    PlacementToEmploymentTransitionResponse result = workflowService.transitionPlacementToEmployment(request);

    assertThat(result).isNotNull();
    assertThat(result.employmentRecord()).isNotNull();
    assertThat(result.employmentRecord().employmentNumber()).isEqualTo("EMP-JOINED");
    verify(employmentRecordService).createEmploymentRecord(any());
  }

  @Test
  @DisplayName("Invalid source status (REJECTED) is rejected before creating employment")
  void testTransitionPlacement_invalidSourceStatusRejected_rejected() {
    PlacementRecordResponse placement = buildPlacementResponse("REJECTED", "NOT_APPLICABLE", null);
    when(placementRecordService.getPlacementRecordById(10L)).thenReturn(placement);

    PlacementToEmploymentTransitionRequest request = new PlacementToEmploymentTransitionRequest();
    request.setPlacementId(10L);
    request.setEmploymentRecordRequest(new CreateEmploymentRecordRequest());

    assertThatThrownBy(() -> workflowService.transitionPlacementToEmployment(request))
        .isInstanceOf(BadRequestException.class)
        .hasMessageContaining("REJECTED")
        .extracting("errorCode").isEqualTo("INVALID_PLACEMENT_STATUS");

    verify(employmentRecordService, never()).createEmploymentRecord(any());
    verify(salaryHistoryService, never()).createSalaryHistory(any());
  }

  @Test
  @DisplayName("Invalid source status (WITHDRAWN) is rejected before creating employment")
  void testTransitionPlacement_invalidSourceStatusWithdrawn_rejected() {
    PlacementRecordResponse placementWithdrawn = buildPlacementResponse("WITHDRAWN", "NOT_APPLICABLE", null);
    when(placementRecordService.getPlacementRecordById(10L)).thenReturn(placementWithdrawn);

    PlacementToEmploymentTransitionRequest request = new PlacementToEmploymentTransitionRequest();
    request.setPlacementId(10L);
    request.setEmploymentRecordRequest(new CreateEmploymentRecordRequest());

    assertThatThrownBy(() -> workflowService.transitionPlacementToEmployment(request))
        .isInstanceOf(BadRequestException.class)
        .hasMessageContaining("WITHDRAWN")
        .extracting("errorCode").isEqualTo("INVALID_PLACEMENT_STATUS");

    verify(employmentRecordService, never()).createEmploymentRecord(any());
    verify(salaryHistoryService, never()).createSalaryHistory(any());
  }

  @Test
  @DisplayName("Invalid source status (CANCELLED) is rejected before creating employment")
  void testTransitionPlacement_invalidSourceStatusCancelled_rejected() {
    PlacementRecordResponse placementCancelled = buildPlacementResponse("CANCELLED", "NOT_APPLICABLE", null);
    when(placementRecordService.getPlacementRecordById(10L)).thenReturn(placementCancelled);

    PlacementToEmploymentTransitionRequest request = new PlacementToEmploymentTransitionRequest();
    request.setPlacementId(10L);
    request.setEmploymentRecordRequest(new CreateEmploymentRecordRequest());

    assertThatThrownBy(() -> workflowService.transitionPlacementToEmployment(request))
        .isInstanceOf(BadRequestException.class)
        .hasMessageContaining("CANCELLED")
        .extracting("errorCode").isEqualTo("INVALID_PLACEMENT_STATUS");

    verify(employmentRecordService, never()).createEmploymentRecord(any());
    verify(salaryHistoryService, never()).createSalaryHistory(any());
  }

  @Test
  @DisplayName("Invalid source status (INITIATED) is rejected before creating employment")
  void testTransitionPlacement_invalidSourceStatusInitiated_rejected() {
    PlacementRecordResponse placementInitiated = buildPlacementResponse("INITIATED", "NOT_APPLICABLE", null);
    when(placementRecordService.getPlacementRecordById(10L)).thenReturn(placementInitiated);

    PlacementToEmploymentTransitionRequest request = new PlacementToEmploymentTransitionRequest();
    request.setPlacementId(10L);
    request.setEmploymentRecordRequest(new CreateEmploymentRecordRequest());

    assertThatThrownBy(() -> workflowService.transitionPlacementToEmployment(request))
        .isInstanceOf(BadRequestException.class)
        .hasMessageContaining("INITIATED")
        .extracting("errorCode").isEqualTo("INVALID_PLACEMENT_STATUS");

    verify(employmentRecordService, never()).createEmploymentRecord(any());
    verify(salaryHistoryService, never()).createSalaryHistory(any());
  }


  @Test
  @DisplayName("Joining status NO_SHOW is rejected before creating employment")
  void testTransitionPlacement_joiningStatusNoShow_rejected() {
    PlacementRecordResponse placement = buildPlacementResponse("OFFERED", "NO_SHOW", null);
    when(placementRecordService.getPlacementRecordById(10L)).thenReturn(placement);

    PlacementToEmploymentTransitionRequest request = new PlacementToEmploymentTransitionRequest();
    request.setPlacementId(10L);
    request.setEmploymentRecordRequest(new CreateEmploymentRecordRequest());

    assertThatThrownBy(() -> workflowService.transitionPlacementToEmployment(request))
        .isInstanceOf(BadRequestException.class)
        .hasMessageContaining("NO_SHOW")
        .extracting("errorCode").isEqualTo("INVALID_JOINING_STATUS");

    verify(employmentRecordService, never()).createEmploymentRecord(any());
  }

  @Test
  @DisplayName("Deleted placement record is rejected before creating employment")
  void testTransitionPlacement_deletedPlacement_rejected() {
    PlacementRecordResponse placement = buildPlacementResponse("ACCEPTED", "JOINED", LocalDateTime.now());
    when(placementRecordService.getPlacementRecordById(10L)).thenReturn(placement);

    PlacementToEmploymentTransitionRequest request = new PlacementToEmploymentTransitionRequest();
    request.setPlacementId(10L);
    request.setEmploymentRecordRequest(new CreateEmploymentRecordRequest());

    assertThatThrownBy(() -> workflowService.transitionPlacementToEmployment(request))
        .isInstanceOf(BadRequestException.class)
        .extracting("errorCode").isEqualTo("PLACEMENT_RECORD_DELETED");

    verify(employmentRecordService, never()).createEmploymentRecord(any());
  }

  @Test
  @DisplayName("Repeated transition of an already converted placement is rejected without duplicate employment")
  void testTransitionPlacement_alreadyTransitioned_rejected() {
    PlacementRecordResponse placement = buildPlacementResponse("JOINED", "JOINED", null);
    when(placementRecordService.getPlacementRecordById(10L)).thenReturn(placement);
    when(employmentRecordRepository.findByPlacementRecordId(10L)).thenReturn(Optional.of(new EmploymentRecord()));

    PlacementToEmploymentTransitionRequest request = new PlacementToEmploymentTransitionRequest();
    request.setPlacementId(10L);
    request.setEmploymentRecordRequest(new CreateEmploymentRecordRequest());

    assertThatThrownBy(() -> workflowService.transitionPlacementToEmployment(request))
        .isInstanceOf(ConflictException.class)
        .extracting("errorCode").isEqualTo("DUPLICATE_PLACEMENT_EMPLOYMENT");

    verify(employmentRecordService, never()).createEmploymentRecord(any());
  }

  @Test
  @DisplayName("Transition specifying invalid target status is rejected before creating employment")
  void testTransitionPlacement_invalidTargetStatus_rejected() {
    PlacementRecordResponse placement = buildPlacementResponse("OFFERED", "PENDING", null);
    when(placementRecordService.getPlacementRecordById(10L)).thenReturn(placement);

    RefPlacementStatus rejectedStatus = new RefPlacementStatus("REJECTED", "Rejected", false, false, true, 6);
    when(refPlacementStatusRepository.findById(6L)).thenReturn(Optional.of(rejectedStatus));

    PlacementToEmploymentTransitionRequest request = new PlacementToEmploymentTransitionRequest();
    request.setPlacementId(10L);
    request.setPlacementStatusId(6L);
    request.setEmploymentRecordRequest(new CreateEmploymentRecordRequest());

    assertThatThrownBy(() -> workflowService.transitionPlacementToEmployment(request))
        .isInstanceOf(BadRequestException.class)
        .extracting("errorCode").isEqualTo("INVALID_PLACEMENT_STATUS");

    verify(employmentRecordService, never()).createEmploymentRecord(any());
  }

  @Test
  @DisplayName("Initial salary trainee mismatch is rejected upfront before updating placement or creating employment")
  void testTransitionPlacement_initialSalaryTraineeMismatch_rejectedUpfront() {
    PlacementRecordResponse placement = buildPlacementResponse("OFFERED", "PENDING", null);
    when(placementRecordService.getPlacementRecordById(10L)).thenReturn(placement);
    when(employmentRecordRepository.findByPlacementRecordId(10L)).thenReturn(Optional.empty());

    CreateEmploymentRecordRequest empReq = new CreateEmploymentRecordRequest();
    empReq.setTraineeId(100L);

    CreateSalaryHistoryRequest salaryReq = new CreateSalaryHistoryRequest();
    salaryReq.setTraineeId(999L); // Mismatched trainee

    PlacementToEmploymentTransitionRequest request = new PlacementToEmploymentTransitionRequest();
    request.setPlacementId(10L);
    request.setEmploymentRecordRequest(empReq);
    request.setInitialSalaryRequest(salaryReq);

    assertThatThrownBy(() -> workflowService.transitionPlacementToEmployment(request))
        .isInstanceOf(BadRequestException.class)
        .extracting("errorCode").isEqualTo("TRAINEE_PLACEMENT_MISMATCH");

    verify(employmentRecordService, never()).createEmploymentRecord(any());
    verify(salaryHistoryService, never()).createSalaryHistory(any());
  }

  @Test
  @DisplayName("Salary history creation failure propagates cleanly for transaction rollback")
  void testTransitionPlacement_salaryCreationFailure_propagatesException() {
    PlacementRecordResponse placement = buildPlacementResponse("OFFERED", "PENDING", null);
    when(placementRecordService.getPlacementRecordById(10L)).thenReturn(placement);
    when(employmentRecordRepository.findByPlacementRecordId(10L)).thenReturn(Optional.empty());

    CreateEmploymentRecordRequest empReq = new CreateEmploymentRecordRequest();
    empReq.setTraineeId(100L);

    CreateSalaryHistoryRequest salaryReq = new CreateSalaryHistoryRequest();
    salaryReq.setTraineeId(100L);

    PlacementToEmploymentTransitionRequest request = new PlacementToEmploymentTransitionRequest();
    request.setPlacementId(10L);
    request.setEmploymentRecordRequest(empReq);
    request.setInitialSalaryRequest(salaryReq);

    EmploymentRecordResponse empResp = new EmploymentRecordResponse(
        1L, "EMP-OFFERED", 100L, "REG-100", "Aditya", "Kumar",
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
    when(employmentRecordService.createEmploymentRecord(any())).thenReturn(empResp);
    when(salaryHistoryService.createSalaryHistory(any()))
        .thenThrow(new IllegalStateException("Simulated database failure during salary history creation"));

    assertThatThrownBy(() -> workflowService.transitionPlacementToEmployment(request))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("Simulated database failure during salary history creation");

    verify(employmentRecordService).createEmploymentRecord(any());
    verify(salaryHistoryService).createSalaryHistory(any());
  }
}
