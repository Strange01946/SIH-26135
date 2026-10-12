package in.gov.sih.sih26135.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import in.gov.sih.sih26135.dto.request.CreateEmploymentRecordRequest;
import in.gov.sih.sih26135.dto.request.UpdateEmploymentRecordRequest;
import in.gov.sih.sih26135.dto.response.EmploymentRecordResponse;
import in.gov.sih.sih26135.entity.Employer;
import in.gov.sih.sih26135.entity.EmploymentRecord;
import in.gov.sih.sih26135.entity.RefEmploymentInfoSource;
import in.gov.sih.sih26135.entity.RefEmploymentSpellStatus;
import in.gov.sih.sih26135.entity.RefEngagementType;
import in.gov.sih.sih26135.entity.RefRecordVerificationStatus;
import in.gov.sih.sih26135.entity.Trainee;
import in.gov.sih.sih26135.exception.ConflictException;
import in.gov.sih.sih26135.mapper.EmploymentRecordMapper;
import in.gov.sih.sih26135.repository.EmployerBranchRepository;
import in.gov.sih.sih26135.repository.EmployerRepository;
import in.gov.sih.sih26135.repository.EmploymentRecordRepository;
import in.gov.sih.sih26135.repository.JobRoleRepository;
import in.gov.sih.sih26135.repository.PlacementRecordRepository;
import in.gov.sih.sih26135.repository.RefEmploymentExitReasonRepository;
import in.gov.sih.sih26135.repository.RefEmploymentInfoSourceRepository;
import in.gov.sih.sih26135.repository.RefEmploymentSpellStatusRepository;
import in.gov.sih.sih26135.repository.RefEngagementTypeRepository;
import in.gov.sih.sih26135.repository.RefRecordVerificationStatusRepository;
import in.gov.sih.sih26135.repository.RefSalaryFrequencyRepository;
import in.gov.sih.sih26135.repository.TraineeRepository;
import in.gov.sih.sih26135.repository.TrainingEnrollmentRepository;
import in.gov.sih.sih26135.service.impl.EmploymentRecordServiceImpl;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class EmploymentSpellUniquenessTest {

  @Mock private EmploymentRecordRepository employmentRecordRepository;
  @Mock private TraineeRepository traineeRepository;
  @Mock private TrainingEnrollmentRepository trainingEnrollmentRepository;
  @Mock private PlacementRecordRepository placementRecordRepository;
  @Mock private EmployerRepository employerRepository;
  @Mock private EmployerBranchRepository employerBranchRepository;
  @Mock private JobRoleRepository jobRoleRepository;
  @Mock private RefEngagementTypeRepository refEngagementTypeRepository;
  @Mock private RefEmploymentSpellStatusRepository refEmploymentSpellStatusRepository;
  @Mock private RefSalaryFrequencyRepository refSalaryFrequencyRepository;
  @Mock private RefEmploymentInfoSourceRepository refEmploymentInfoSourceRepository;
  @Mock private RefRecordVerificationStatusRepository refRecordVerificationStatusRepository;
  @Mock private RefEmploymentExitReasonRepository refEmploymentExitReasonRepository;
  @Mock private EmploymentRecordMapper employmentRecordMapper;

  private EmploymentRecordServiceImpl employmentRecordService;

  private Trainee testTrainee;
  private RefEngagementType testEngagementType;
  private RefEmploymentSpellStatus testSpellStatus;
  private RefEmploymentInfoSource testInfoSource;
  private RefRecordVerificationStatus testVerificationStatus;

  @BeforeEach
  void setUp() {
    employmentRecordService = new EmploymentRecordServiceImpl(
        employmentRecordRepository, traineeRepository, trainingEnrollmentRepository,
        placementRecordRepository, employerRepository, employerBranchRepository,
        jobRoleRepository, refEngagementTypeRepository, refEmploymentSpellStatusRepository,
        refSalaryFrequencyRepository, refEmploymentInfoSourceRepository,
        refRecordVerificationStatusRepository, refEmploymentExitReasonRepository,
        employmentRecordMapper
    );

    testTrainee = new Trainee();
    testTrainee.setId(100L);

    testEngagementType = new RefEngagementType();
    testEngagementType.setId(1L);
    testEngagementType.setTypeCode("REGULAR_WAGE");
    testEngagementType.setTypeName("Regular wage");
    testEngagementType.setRequiresEmployer(false);

    testSpellStatus = new RefEmploymentSpellStatus();
    testSpellStatus.setId(1L);
    testSpellStatus.setStatusCode("ACTIVE");
    testSpellStatus.setStatusName("Active spell");

    testInfoSource = new RefEmploymentInfoSource();
    testInfoSource.setId(1L);
    testInfoSource.setSourceCode("DIRECT_EMPLOYER");
    testInfoSource.setSourceName("Direct employer");

    testVerificationStatus = new RefRecordVerificationStatus("VERIFIED", "Verified", true, 1);
    testVerificationStatus.setId(1L);
  }

  @Test
  @DisplayName("Creating a current employment record preserves isCurrent=true when no end date is provided")
  void testCreateCurrentEmploymentRecord_setsIsCurrentTrue() {
    CreateEmploymentRecordRequest request = new CreateEmploymentRecordRequest();
    request.setEmploymentNumber("EMP-101");
    request.setTraineeId(100L);
    request.setEngagementTypeId(1L);
    request.setStartDate(LocalDate.of(2026, 1, 1));
    request.setEmploymentSpellStatusId(1L);
    request.setEmploymentInfoSourceId(1L);
    request.setRecordVerificationStatusId(1L);

    when(employmentRecordRepository.existsByEmploymentNumber("EMP-101")).thenReturn(false);
    when(traineeRepository.findById(100L)).thenReturn(Optional.of(testTrainee));
    when(refEngagementTypeRepository.findById(1L)).thenReturn(Optional.of(testEngagementType));
    when(refEmploymentSpellStatusRepository.findById(1L)).thenReturn(Optional.of(testSpellStatus));
    when(refEmploymentInfoSourceRepository.findById(1L)).thenReturn(Optional.of(testInfoSource));
    when(refRecordVerificationStatusRepository.findById(1L)).thenReturn(Optional.of(testVerificationStatus));
    when(employmentRecordRepository.findByTraineeId(100L)).thenReturn(Collections.emptyList());
    when(employmentRecordRepository.findByTraineeIdAndIsCurrent(100L, true)).thenReturn(Collections.emptyList());

    EmploymentRecord mockEntity = new EmploymentRecord();
    mockEntity.setId(1L);
    when(employmentRecordMapper.toEntity(any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any()))
        .thenReturn(mockEntity);
    when(employmentRecordRepository.save(any(EmploymentRecord.class))).thenAnswer(invocation -> invocation.getArgument(0));
    when(employmentRecordMapper.toResponse(any())).thenReturn(null);

    employmentRecordService.createEmploymentRecord(request);

    ArgumentCaptor<EmploymentRecord> captor = ArgumentCaptor.forClass(EmploymentRecord.class);
    verify(employmentRecordRepository).save(captor.capture());
    assertThat(captor.getValue().getIsCurrent()).isTrue();
  }

  @Test
  @DisplayName("Creating non-current employment record remains supported when end date is supplied")
  void testCreateNonCurrentEmploymentRecord_supportedWithEndDate() {
    CreateEmploymentRecordRequest request = new CreateEmploymentRecordRequest();
    request.setEmploymentNumber("EMP-102");
    request.setTraineeId(100L);
    request.setEngagementTypeId(1L);
    request.setStartDate(LocalDate.of(2025, 1, 1));
    request.setEndDate(LocalDate.of(2025, 12, 31));
    request.setIsCurrent(false);
    request.setEmploymentSpellStatusId(1L);
    request.setEmploymentInfoSourceId(1L);
    request.setRecordVerificationStatusId(1L);

    when(employmentRecordRepository.existsByEmploymentNumber("EMP-102")).thenReturn(false);
    when(traineeRepository.findById(100L)).thenReturn(Optional.of(testTrainee));
    when(refEngagementTypeRepository.findById(1L)).thenReturn(Optional.of(testEngagementType));
    when(refEmploymentSpellStatusRepository.findById(1L)).thenReturn(Optional.of(testSpellStatus));
    when(refEmploymentInfoSourceRepository.findById(1L)).thenReturn(Optional.of(testInfoSource));
    when(refRecordVerificationStatusRepository.findById(1L)).thenReturn(Optional.of(testVerificationStatus));
    when(employmentRecordRepository.findByTraineeId(100L)).thenReturn(Collections.emptyList());

    EmploymentRecord mockEntity = new EmploymentRecord();
    mockEntity.setId(2L);
    when(employmentRecordMapper.toEntity(any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any()))
        .thenReturn(mockEntity);
    when(employmentRecordRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

    employmentRecordService.createEmploymentRecord(request);

    ArgumentCaptor<EmploymentRecord> captor = ArgumentCaptor.forClass(EmploymentRecord.class);
    verify(employmentRecordRepository).save(captor.capture());
    assertThat(captor.getValue().getIsCurrent()).isFalse();
  }

  @Test
  @DisplayName("Conflicting current employment attempt is rejected with ACTIVE_SPELL_ALREADY_EXISTS")
  void testCreateCurrentEmployment_whenActiveSpellExists_throwsConflictException() {
    CreateEmploymentRecordRequest request = new CreateEmploymentRecordRequest();
    request.setEmploymentNumber("EMP-103");
    request.setTraineeId(100L);
    request.setEngagementTypeId(1L);
    request.setStartDate(LocalDate.of(2026, 1, 1));
    request.setEmploymentSpellStatusId(1L);
    request.setEmploymentInfoSourceId(1L);
    request.setRecordVerificationStatusId(1L);

    when(employmentRecordRepository.existsByEmploymentNumber("EMP-103")).thenReturn(false);
    when(traineeRepository.findById(100L)).thenReturn(Optional.of(testTrainee));
    when(refEngagementTypeRepository.findById(1L)).thenReturn(Optional.of(testEngagementType));
    when(refEmploymentSpellStatusRepository.findById(1L)).thenReturn(Optional.of(testSpellStatus));
    when(employmentRecordRepository.findByTraineeId(100L)).thenReturn(Collections.emptyList());

    EmploymentRecord existingActive = new EmploymentRecord();
    existingActive.setId(10L);
    existingActive.setEngagementType(testEngagementType);
    existingActive.setIsCurrent(true);
    when(employmentRecordRepository.findByTraineeIdAndIsCurrent(100L, true))
        .thenReturn(List.of(existingActive));

    assertThatThrownBy(() -> employmentRecordService.createEmploymentRecord(request))
        .isInstanceOf(ConflictException.class)
        .hasMessageContaining("active current spell")
        .extracting("errorCode").isEqualTo("ACTIVE_SPELL_ALREADY_EXISTS");
  }

  @Test
  @DisplayName("Transitioning out of current employment releases the current spell slot")
  void testUpdateEmploymentRecord_transitionOutOfCurrentEmployment_releasesSlot() {
    EmploymentRecord existing = new EmploymentRecord();
    existing.setId(50L);
    existing.setTrainee(testTrainee);
    existing.setEngagementType(testEngagementType);
    existing.setStartDate(LocalDate.of(2025, 1, 1));
    existing.setIsCurrent(true);

    when(employmentRecordRepository.findById(50L)).thenReturn(Optional.of(existing));
    when(employmentRecordRepository.findByTraineeId(100L)).thenReturn(List.of(existing));
    when(employmentRecordRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

    UpdateEmploymentRecordRequest update = new UpdateEmploymentRecordRequest();
    update.setEndDate(LocalDate.of(2026, 3, 31));
    update.setIsCurrent(false);

    employmentRecordService.updateEmploymentRecord(50L, update);

    ArgumentCaptor<EmploymentRecord> captor = ArgumentCaptor.forClass(EmploymentRecord.class);
    verify(employmentRecordRepository).save(captor.capture());
    assertThat(captor.getValue().getIsCurrent()).isFalse();
    assertThat(captor.getValue().getEndDate()).isEqualTo(LocalDate.of(2026, 3, 31));
  }

  @Test
  @DisplayName("Transitioning non-current record to current claims the current spell slot")
  void testUpdateEmploymentRecord_transitionNonCurrentToCurrent_claimsSlot() {
    EmploymentRecord existing = new EmploymentRecord();
    existing.setId(50L);
    existing.setTrainee(testTrainee);
    existing.setEngagementType(testEngagementType);
    existing.setStartDate(LocalDate.of(2025, 1, 1));
    existing.setEndDate(LocalDate.of(2025, 12, 31));
    existing.setIsCurrent(false);

    when(employmentRecordRepository.findById(50L)).thenReturn(Optional.of(existing));
    when(employmentRecordRepository.findByTraineeId(100L)).thenReturn(List.of(existing));
    when(employmentRecordRepository.findByTraineeIdAndIsCurrent(100L, true)).thenReturn(Collections.emptyList());
    when(employmentRecordRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

    UpdateEmploymentRecordRequest update = new UpdateEmploymentRecordRequest();
    update.setEndDate(null);
    update.setIsCurrent(true);

    employmentRecordService.updateEmploymentRecord(50L, update);

    ArgumentCaptor<EmploymentRecord> captor = ArgumentCaptor.forClass(EmploymentRecord.class);
    verify(employmentRecordRepository).save(captor.capture());
    assertThat(captor.getValue().getIsCurrent()).isTrue();
  }

  @Test
  @DisplayName("Transitioning non-current record to current fails when another active spell exists")
  void testUpdateEmploymentRecord_transitionNonCurrentToCurrent_whenActiveSpellExists_throwsConflict() {
    EmploymentRecord existing = new EmploymentRecord();
    existing.setId(50L);
    existing.setTrainee(testTrainee);
    existing.setEngagementType(testEngagementType);
    existing.setStartDate(LocalDate.of(2025, 1, 1));
    existing.setIsCurrent(false);

    EmploymentRecord otherActive = new EmploymentRecord();
    otherActive.setId(51L);
    otherActive.setTrainee(testTrainee);
    otherActive.setEngagementType(testEngagementType);
    otherActive.setStartDate(LocalDate.of(2026, 1, 1));
    otherActive.setIsCurrent(true);

    when(employmentRecordRepository.findById(50L)).thenReturn(Optional.of(existing));
    when(employmentRecordRepository.findByTraineeId(100L)).thenReturn(List.of(existing, otherActive));
    when(employmentRecordRepository.findByTraineeIdAndIsCurrent(100L, true)).thenReturn(List.of(otherActive));

    UpdateEmploymentRecordRequest update = new UpdateEmploymentRecordRequest();
    update.setIsCurrent(true);

    assertThatThrownBy(() -> employmentRecordService.updateEmploymentRecord(50L, update))
        .isInstanceOf(ConflictException.class)
        .extracting("errorCode").isEqualTo("ACTIVE_SPELL_ALREADY_EXISTS");
  }

  @Test
  @DisplayName("Updating unrelated fields preserves the current spell state")
  void testUpdateEmploymentRecord_unrelatedFields_preservesCurrentSpellState() {
    EmploymentRecord existing = new EmploymentRecord();
    existing.setId(50L);
    existing.setTrainee(testTrainee);
    existing.setEngagementType(testEngagementType);
    existing.setStartDate(LocalDate.of(2025, 1, 1));
    existing.setIsCurrent(true);
    existing.setStartingSalary(new BigDecimal("25000.00"));

    when(employmentRecordRepository.findById(50L)).thenReturn(Optional.of(existing));
    when(employmentRecordRepository.findByTraineeId(100L)).thenReturn(List.of(existing));
    when(employmentRecordRepository.findByTraineeIdAndIsCurrent(100L, true)).thenReturn(List.of(existing));
    when(employmentRecordRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

    UpdateEmploymentRecordRequest update = new UpdateEmploymentRecordRequest();
    update.setStartingSalary(new BigDecimal("30000.00"));

    employmentRecordService.updateEmploymentRecord(50L, update);

    ArgumentCaptor<EmploymentRecord> captor = ArgumentCaptor.forClass(EmploymentRecord.class);
    verify(employmentRecordRepository).save(captor.capture());
    assertThat(captor.getValue().getIsCurrent()).isTrue();
    assertThat(captor.getValue().getStartingSalary()).isEqualTo(new BigDecimal("30000.00"));
  }

  @Test
  @DisplayName("Creating non-current employment record without end date fails with NON_CURRENT_SPELL_MISSING_END_DATE")
  void testCreateNonCurrentEmploymentRecord_withoutEndDate_throwsBadRequest() {
    CreateEmploymentRecordRequest request = new CreateEmploymentRecordRequest();
    request.setEmploymentNumber("EMP-102B");
    request.setTraineeId(100L);
    request.setEngagementTypeId(1L);
    request.setStartDate(LocalDate.of(2025, 1, 1));
    request.setEndDate(null);
    request.setIsCurrent(false);
    request.setEmploymentSpellStatusId(1L);
    request.setEmploymentInfoSourceId(1L);
    request.setRecordVerificationStatusId(1L);

    when(employmentRecordRepository.existsByEmploymentNumber("EMP-102B")).thenReturn(false);
    when(traineeRepository.findById(100L)).thenReturn(Optional.of(testTrainee));
    when(refEngagementTypeRepository.findById(1L)).thenReturn(Optional.of(testEngagementType));
    when(refEmploymentSpellStatusRepository.findById(1L)).thenReturn(Optional.of(testSpellStatus));

    assertThatThrownBy(() -> employmentRecordService.createEmploymentRecord(request))
        .isInstanceOf(in.gov.sih.sih26135.exception.BadRequestException.class)
        .extracting("errorCode").isEqualTo("NON_CURRENT_SPELL_MISSING_END_DATE");
  }

  @Test
  @DisplayName("Setting isCurrent=false without an end date is rejected with NON_CURRENT_SPELL_MISSING_END_DATE")
  void testUpdateEmploymentRecord_setIsCurrentFalseWithoutEndDate_throwsBadRequest() {
    EmploymentRecord existing = new EmploymentRecord();
    existing.setId(50L);
    existing.setTrainee(testTrainee);
    existing.setEngagementType(testEngagementType);
    existing.setStartDate(LocalDate.of(2025, 1, 1));
    existing.setIsCurrent(true);
    existing.setEndDate(null);

    when(employmentRecordRepository.findById(50L)).thenReturn(Optional.of(existing));

    UpdateEmploymentRecordRequest update = new UpdateEmploymentRecordRequest();
    update.setIsCurrent(false);
    update.setEndDate(null);

    assertThatThrownBy(() -> employmentRecordService.updateEmploymentRecord(50L, update))
        .isInstanceOf(in.gov.sih.sih26135.exception.BadRequestException.class)
        .extracting("errorCode").isEqualTo("NON_CURRENT_SPELL_MISSING_END_DATE");
  }

  @Test
  @DisplayName("Setting isCurrent=true while an end date is supplied is rejected with CURRENT_SPELL_HAS_END_DATE")
  void testUpdateEmploymentRecord_setIsCurrentTrueWhileEndDateSupplied_throwsBadRequest() {
    EmploymentRecord existing = new EmploymentRecord();
    existing.setId(50L);
    existing.setTrainee(testTrainee);
    existing.setEngagementType(testEngagementType);
    existing.setStartDate(LocalDate.of(2025, 1, 1));
    existing.setIsCurrent(false);
    existing.setEndDate(LocalDate.of(2025, 12, 31));

    when(employmentRecordRepository.findById(50L)).thenReturn(Optional.of(existing));

    UpdateEmploymentRecordRequest update = new UpdateEmploymentRecordRequest();
    update.setIsCurrent(true);
    update.setEndDate(LocalDate.of(2026, 3, 31));

    assertThatThrownBy(() -> employmentRecordService.updateEmploymentRecord(50L, update))
        .isInstanceOf(in.gov.sih.sih26135.exception.BadRequestException.class)
        .extracting("errorCode").isEqualTo("CURRENT_SPELL_HAS_END_DATE");
  }

  @Test
  @DisplayName("Reactivating a non-current record with an existing end date clears the end date to null")
  void testUpdateEmploymentRecord_reactivatingRecordWithExistingEndDate_clearsEndDate() {
    EmploymentRecord existing = new EmploymentRecord();
    existing.setId(50L);
    existing.setTrainee(testTrainee);
    existing.setEngagementType(testEngagementType);
    existing.setStartDate(LocalDate.of(2025, 1, 1));
    existing.setEndDate(LocalDate.of(2025, 12, 31));
    existing.setIsCurrent(false);

    when(employmentRecordRepository.findById(50L)).thenReturn(Optional.of(existing));
    when(employmentRecordRepository.findByTraineeId(100L)).thenReturn(List.of(existing));
    when(employmentRecordRepository.findByTraineeIdAndIsCurrent(100L, true)).thenReturn(Collections.emptyList());
    when(employmentRecordRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

    UpdateEmploymentRecordRequest update = new UpdateEmploymentRecordRequest();
    update.setIsCurrent(true);

    employmentRecordService.updateEmploymentRecord(50L, update);

    ArgumentCaptor<EmploymentRecord> captor = ArgumentCaptor.forClass(EmploymentRecord.class);
    verify(employmentRecordRepository).save(captor.capture());
    assertThat(captor.getValue().getIsCurrent()).isTrue();
    assertThat(captor.getValue().getEndDate()).isNull();
  }

  @Test
  @DisplayName("Updating unrelated fields on a non-current record preserves existing end date and isCurrent=false")
  void testUpdateEmploymentRecord_unrelatedFieldOnNonCurrentRecord_preservesEndDate() {
    EmploymentRecord existing = new EmploymentRecord();
    existing.setId(50L);
    existing.setTrainee(testTrainee);
    existing.setEngagementType(testEngagementType);
    existing.setStartDate(LocalDate.of(2025, 1, 1));
    existing.setEndDate(LocalDate.of(2025, 12, 31));
    existing.setIsCurrent(false);
    existing.setStartingSalary(new BigDecimal("25000.00"));

    when(employmentRecordRepository.findById(50L)).thenReturn(Optional.of(existing));
    when(employmentRecordRepository.findByTraineeId(100L)).thenReturn(List.of(existing));
    when(employmentRecordRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

    UpdateEmploymentRecordRequest update = new UpdateEmploymentRecordRequest();
    update.setStartingSalary(new BigDecimal("35000.00"));

    employmentRecordService.updateEmploymentRecord(50L, update);

    ArgumentCaptor<EmploymentRecord> captor = ArgumentCaptor.forClass(EmploymentRecord.class);
    verify(employmentRecordRepository).save(captor.capture());
    assertThat(captor.getValue().getIsCurrent()).isFalse();
    assertThat(captor.getValue().getEndDate()).isEqualTo(LocalDate.of(2025, 12, 31));
    assertThat(captor.getValue().getStartingSalary()).isEqualTo(new BigDecimal("35000.00"));
  }

  @Test
  @DisplayName("Soft-deleting an employment record releases the current spell slot")
  void testDeleteEmploymentRecord_releasesCurrentSpellSlot() {
    EmploymentRecord existing = new EmploymentRecord();
    existing.setId(50L);
    existing.setIsCurrent(true);

    when(employmentRecordRepository.findById(50L)).thenReturn(Optional.of(existing));
    when(employmentRecordRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

    employmentRecordService.deleteEmploymentRecord(50L);

    ArgumentCaptor<EmploymentRecord> captor = ArgumentCaptor.forClass(EmploymentRecord.class);
    verify(employmentRecordRepository).save(captor.capture());
    assertThat(captor.getValue().getDeletedAt()).isNotNull();
    assertThat(captor.getValue().getIsCurrent()).isFalse();
  }
}
