package in.gov.sih.sih26135.service.impl;

import in.gov.sih.sih26135.dto.request.CreateSalaryHistoryRequest;
import in.gov.sih.sih26135.dto.request.UpdateSalaryHistoryRequest;
import in.gov.sih.sih26135.dto.response.SalaryHistoryResponse;
import in.gov.sih.sih26135.entity.EmploymentRecord;
import in.gov.sih.sih26135.entity.RefEmploymentInfoSource;
import in.gov.sih.sih26135.entity.RefRecordVerificationStatus;
import in.gov.sih.sih26135.entity.RefSalaryFrequency;
import in.gov.sih.sih26135.entity.SalaryHistory;
import in.gov.sih.sih26135.entity.Trainee;
import in.gov.sih.sih26135.exception.BadRequestException;
import in.gov.sih.sih26135.exception.ConflictException;
import in.gov.sih.sih26135.exception.ResourceNotFoundException;
import in.gov.sih.sih26135.mapper.SalaryHistoryMapper;
import in.gov.sih.sih26135.repository.EmploymentRecordRepository;
import in.gov.sih.sih26135.repository.RefEmploymentInfoSourceRepository;
import in.gov.sih.sih26135.repository.RefRecordVerificationStatusRepository;
import in.gov.sih.sih26135.repository.RefSalaryFrequencyRepository;
import in.gov.sih.sih26135.repository.SalaryHistoryRepository;
import in.gov.sih.sih26135.service.SalaryHistoryService;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class SalaryHistoryServiceImpl implements SalaryHistoryService {

  private static final Set<Integer> VALID_MILESTONE_OFFSETS = Set.of(0, 6, 12, 24, 36);

  private final SalaryHistoryRepository salaryHistoryRepository;
  private final EmploymentRecordRepository employmentRecordRepository;
  private final RefSalaryFrequencyRepository refSalaryFrequencyRepository;
  private final RefEmploymentInfoSourceRepository refEmploymentInfoSourceRepository;
  private final RefRecordVerificationStatusRepository refRecordVerificationStatusRepository;
  private final SalaryHistoryMapper salaryHistoryMapper;

  public SalaryHistoryServiceImpl(
      SalaryHistoryRepository salaryHistoryRepository,
      EmploymentRecordRepository employmentRecordRepository,
      RefSalaryFrequencyRepository refSalaryFrequencyRepository,
      RefEmploymentInfoSourceRepository refEmploymentInfoSourceRepository,
      RefRecordVerificationStatusRepository refRecordVerificationStatusRepository,
      SalaryHistoryMapper salaryHistoryMapper) {
    this.salaryHistoryRepository = salaryHistoryRepository;
    this.employmentRecordRepository = employmentRecordRepository;
    this.refSalaryFrequencyRepository = refSalaryFrequencyRepository;
    this.refEmploymentInfoSourceRepository = refEmploymentInfoSourceRepository;
    this.refRecordVerificationStatusRepository = refRecordVerificationStatusRepository;
    this.salaryHistoryMapper = salaryHistoryMapper;
  }

  @Override
  public SalaryHistoryResponse getSalaryHistoryById(Long id) {
    if (id == null) {
      throw new BadRequestException("Salary history ID is required");
    }
    SalaryHistory history = salaryHistoryRepository.findById(id)
        .orElseThrow(() -> new ResourceNotFoundException("SalaryHistory", "id"));
    return salaryHistoryMapper.toResponse(history);
  }

  @Override
  public List<SalaryHistoryResponse> getSalaryHistoryByEmploymentRecord(Long employmentRecordId) {
    if (employmentRecordId == null) {
      throw new BadRequestException("Employment record ID is required");
    }
    return salaryHistoryRepository.findByEmploymentRecordId(employmentRecordId).stream()
        .map(salaryHistoryMapper::toResponse)
        .toList();
  }

  @Override
  public List<SalaryHistoryResponse> getSalaryHistoryByTrainee(Long traineeId) {
    if (traineeId == null) {
      throw new BadRequestException("Trainee ID is required");
    }
    return salaryHistoryRepository.findByTraineeId(traineeId).stream()
        .map(salaryHistoryMapper::toResponse)
        .toList();
  }

  @Override
  public List<SalaryHistoryResponse> getSalaryHistoryByEffectiveFrom(LocalDate effectiveFrom) {
    if (effectiveFrom == null) {
      throw new BadRequestException("Effective from date is required");
    }
    return salaryHistoryRepository.findByEffectiveFrom(effectiveFrom).stream()
        .map(salaryHistoryMapper::toResponse)
        .toList();
  }

  @Override
  public List<SalaryHistoryResponse> getSalaryHistoryByObservationMilestone(Integer observationMonthOffset) {
    if (observationMonthOffset == null) {
      throw new BadRequestException("Observation month offset is required");
    }
    if (!VALID_MILESTONE_OFFSETS.contains(observationMonthOffset)) {
      throw new BadRequestException("Observation month offset must be one of: 0, 6, 12, 24, 36", "INVALID_OBSERVATION_MONTH_OFFSET");
    }
    return salaryHistoryRepository.findByObservationMonthOffset(observationMonthOffset).stream()
        .map(salaryHistoryMapper::toResponse)
        .toList();
  }

  @Override
  @Transactional
  public SalaryHistoryResponse createSalaryHistory(CreateSalaryHistoryRequest request) {
    if (request == null) {
      throw new BadRequestException("Salary history creation request cannot be null");
    }
    if (request.getEmploymentId() == null) {
      throw new BadRequestException("Employment record ID is required");
    }
    if (request.getSalaryAmount() == null) {
      throw new BadRequestException("Salary amount is required");
    }
    if (request.getSalaryFrequencyId() == null) {
      throw new BadRequestException("Salary frequency ID is required");
    }
    if (request.getEffectiveFrom() == null) {
      throw new BadRequestException("Effective from date is required");
    }
    if (request.getEmploymentInfoSourceId() == null) {
      throw new BadRequestException("Employment info source ID is required");
    }
    if (request.getRecordVerificationStatusId() == null) {
      throw new BadRequestException("Record verification status ID is required");
    }

    if (request.getSalaryAmount().compareTo(BigDecimal.ZERO) < 0) {
      throw new BadRequestException("Salary amount cannot be negative", "INVALID_SALARY");
    }

    EmploymentRecord employment = employmentRecordRepository.findById(request.getEmploymentId())
        .orElseThrow(() -> new ResourceNotFoundException("EmploymentRecord", "employmentId"));

    if (employment.getDeletedAt() != null) {
      throw new BadRequestException("Cannot add salary history to a deleted employment record", "EMPLOYMENT_RECORD_DELETED");
    }

    Trainee authoritativeTrainee = employment.getTrainee();
    if (request.getTraineeId() != null && !request.getTraineeId().equals(authoritativeTrainee.getId())) {
      throw new BadRequestException("Trainee ID does not match employment record trainee", "TRAINEE_EMPLOYMENT_MISMATCH");
    }

    LocalDate effectiveFrom = request.getEffectiveFrom();
    if (effectiveFrom.isBefore(employment.getStartDate())) {
      throw new BadRequestException("Effective from date cannot be before employment start date", "INVALID_EFFECTIVE_DATE");
    }
    if (employment.getEndDate() != null && effectiveFrom.isAfter(employment.getEndDate())) {
      throw new BadRequestException("Effective from date cannot be after employment end date", "INVALID_EFFECTIVE_DATE");
    }

    LocalDate effectiveTo = request.getEffectiveTo();
    if (effectiveTo != null && effectiveTo.isBefore(effectiveFrom)) {
      throw new BadRequestException("Effective to date cannot be before effective from date", "INVALID_DATE_RANGE");
    }

    Integer offset = request.getObservationMonthOffset();
    if (offset != null && !VALID_MILESTONE_OFFSETS.contains(offset)) {
      throw new BadRequestException("Observation month offset must be one of: 0, 6, 12, 24, 36", "INVALID_OBSERVATION_MONTH_OFFSET");
    }

    // uk_salary_history_employment_from: (employment_id, effective_from)
    if (salaryHistoryRepository.findByEmploymentRecordIdAndEffectiveFrom(employment.getId(), effectiveFrom).isPresent()) {
      throw new ConflictException("Salary history already exists for this employment record and effective from date", "DUPLICATE_EMPLOYMENT_EFFECTIVE_FROM");
    }

    // uk_salary_history_employment_milestone: (employment_id, observation_month_offset)
    if (offset != null && salaryHistoryRepository.findByEmploymentRecordIdAndObservationMonthOffset(employment.getId(), offset).isPresent()) {
      throw new ConflictException("Salary history already exists for this employment record and observation milestone", "DUPLICATE_EMPLOYMENT_MILESTONE");
    }

    RefSalaryFrequency frequency = refSalaryFrequencyRepository.findById(request.getSalaryFrequencyId())
        .orElseThrow(() -> new ResourceNotFoundException("RefSalaryFrequency", "salaryFrequencyId"));

    RefEmploymentInfoSource infoSource = refEmploymentInfoSourceRepository.findById(request.getEmploymentInfoSourceId())
        .orElseThrow(() -> new ResourceNotFoundException("RefEmploymentInfoSource", "employmentInfoSourceId"));

    RefRecordVerificationStatus verificationStatus = refRecordVerificationStatusRepository
        .findById(request.getRecordVerificationStatusId())
        .orElseThrow(() -> new ResourceNotFoundException("RefRecordVerificationStatus", "recordVerificationStatusId"));

    SalaryHistory entity = salaryHistoryMapper.toEntity(
        request, employment, authoritativeTrainee, frequency, infoSource, verificationStatus);
    SalaryHistory saved = salaryHistoryRepository.save(entity);
    return salaryHistoryMapper.toResponse(saved);
  }

  @Override
  @Transactional
  public SalaryHistoryResponse updateSalaryHistory(Long id, UpdateSalaryHistoryRequest request) {
    if (id == null) {
      throw new BadRequestException("Salary history ID is required");
    }
    if (request == null) {
      throw new BadRequestException("Salary history update request cannot be null");
    }

    SalaryHistory history = salaryHistoryRepository.findById(id)
        .orElseThrow(() -> new ResourceNotFoundException("SalaryHistory", "id"));

    if (history.getEmploymentRecord().getDeletedAt() != null) {
      throw new BadRequestException("Cannot update salary history for a deleted employment record", "EMPLOYMENT_RECORD_DELETED");
    }

    if (request.getSalaryAmount() != null) {
      if (request.getSalaryAmount().compareTo(BigDecimal.ZERO) < 0) {
        throw new BadRequestException("Salary amount cannot be negative", "INVALID_SALARY");
      }
      history.setSalaryAmount(request.getSalaryAmount());
    }

    if (request.getSalaryFrequencyId() != null) {
      RefSalaryFrequency frequency = refSalaryFrequencyRepository.findById(request.getSalaryFrequencyId())
          .orElseThrow(() -> new ResourceNotFoundException("RefSalaryFrequency", "salaryFrequencyId"));
      history.setSalaryFrequency(frequency);
    }

    if (request.getCurrencyCode() != null && !request.getCurrencyCode().isBlank()) {
      history.setCurrencyCode(request.getCurrencyCode().trim());
    }

    if (request.isEffectiveToSpecified() || request.getEffectiveTo() != null) {
      LocalDate effectiveTo = request.getEffectiveTo();
      if (effectiveTo != null && effectiveTo.isBefore(history.getEffectiveFrom())) {
        throw new BadRequestException("Effective to date cannot be before effective from date", "INVALID_DATE_RANGE");
      }
      history.setEffectiveTo(effectiveTo);
    }

    if (request.isObservationMonthOffsetSpecified() || request.getObservationMonthOffset() != null) {
      Integer offset = request.getObservationMonthOffset();
      if (offset != null) {
        if (!VALID_MILESTONE_OFFSETS.contains(offset)) {
          throw new BadRequestException("Observation month offset must be one of: 0, 6, 12, 24, 36", "INVALID_OBSERVATION_MONTH_OFFSET");
        }
        salaryHistoryRepository.findByEmploymentRecordIdAndObservationMonthOffset(history.getEmploymentRecord().getId(), offset)
            .filter(sh -> !sh.getId().equals(id))
            .ifPresent(sh -> {
              throw new ConflictException("Salary history already exists for this employment record and observation milestone", "DUPLICATE_EMPLOYMENT_MILESTONE");
            });
      }
      history.setObservationMonthOffset(offset);
    }

    if (request.getEmploymentInfoSourceId() != null) {
      RefEmploymentInfoSource source = refEmploymentInfoSourceRepository.findById(request.getEmploymentInfoSourceId())
          .orElseThrow(() -> new ResourceNotFoundException("RefEmploymentInfoSource", "employmentInfoSourceId"));
      history.setEmploymentInfoSource(source);
    }

    if (request.getRecordVerificationStatusId() != null) {
      RefRecordVerificationStatus verificationStatus = refRecordVerificationStatusRepository
          .findById(request.getRecordVerificationStatusId())
          .orElseThrow(() -> new ResourceNotFoundException("RefRecordVerificationStatus", "recordVerificationStatusId"));
      history.setRecordVerificationStatus(verificationStatus);
    }

    if (request.getVerifiedAt() != null) {
      history.setVerifiedAt(request.getVerifiedAt());
    }
    if (request.getVerifiedByUserId() != null) {
      history.setVerifiedByUserId(request.getVerifiedByUserId());
    }

    SalaryHistory saved = salaryHistoryRepository.save(history);
    return salaryHistoryMapper.toResponse(saved);
  }

  @Override
  @Transactional
  public void deleteSalaryHistory(Long id) {
    if (id == null) {
      throw new BadRequestException("Salary history ID is required");
    }
    SalaryHistory history = salaryHistoryRepository.findById(id)
        .orElseThrow(() -> new ResourceNotFoundException("SalaryHistory", "id"));
    salaryHistoryRepository.delete(history);
  }
}
