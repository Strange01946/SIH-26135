package in.gov.sih.sih26135.service.impl;

import in.gov.sih.sih26135.dto.response.EmploymentExitFactResponse;
import in.gov.sih.sih26135.entity.analytics.EmploymentExitFact;
import in.gov.sih.sih26135.exception.BadRequestException;
import in.gov.sih.sih26135.exception.ResourceNotFoundException;
import in.gov.sih.sih26135.mapper.EmploymentExitFactMapper;
import in.gov.sih.sih26135.repository.analytics.EmploymentExitFactRepository;
import in.gov.sih.sih26135.service.EmploymentExitFactService;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class EmploymentExitFactServiceImpl implements EmploymentExitFactService {

  private final EmploymentExitFactRepository repository;
  private final EmploymentExitFactMapper mapper;

  public EmploymentExitFactServiceImpl(
      EmploymentExitFactRepository repository,
      EmploymentExitFactMapper mapper) {
    this.repository = repository;
    this.mapper = mapper;
  }

  @Override
  public List<EmploymentExitFactResponse> getAllEmploymentExits() {
    return repository.findAll().stream()
        .map(mapper::toResponse)
        .toList();
  }

  @Override
  public EmploymentExitFactResponse getEmploymentExitById(Long employmentExitEventId) {
    if (employmentExitEventId == null) {
      throw new BadRequestException("Employment exit event ID is required");
    }
    EmploymentExitFact entity = repository.findById(employmentExitEventId)
        .orElseThrow(() -> new ResourceNotFoundException("EmploymentExitFact", "employmentExitEventId"));
    return mapper.toResponse(entity);
  }

  @Override
  public List<EmploymentExitFactResponse> getEmploymentExitsByTraineeId(Long traineeId) {
    if (traineeId == null) {
      throw new BadRequestException("Trainee ID is required");
    }
    return repository.findByTraineeId(traineeId).stream()
        .map(mapper::toResponse)
        .toList();
  }

  @Override
  public List<EmploymentExitFactResponse> getEmploymentExitsByEmploymentId(Long employmentId) {
    if (employmentId == null) {
      throw new BadRequestException("Employment ID is required");
    }
    return repository.findByEmploymentId(employmentId).stream()
        .map(mapper::toResponse)
        .toList();
  }

  @Override
  public List<EmploymentExitFactResponse> getEmploymentExitsByEmploymentExitReasonId(Long employmentExitReasonId) {
    if (employmentExitReasonId == null) {
      throw new BadRequestException("Employment exit reason ID is required");
    }
    return repository.findByEmploymentExitReasonId(employmentExitReasonId).stream()
        .map(mapper::toResponse)
        .toList();
  }

  @Override
  public List<EmploymentExitFactResponse> getEmploymentExitsBySeparationNatureId(Long separationNatureId) {
    if (separationNatureId == null) {
      throw new BadRequestException("Separation nature ID is required");
    }
    return repository.findBySeparationNatureId(separationNatureId).stream()
        .map(mapper::toResponse)
        .toList();
  }

  @Override
  public List<EmploymentExitFactResponse> getEmploymentExitsByIsVoluntaryFlag(Boolean isVoluntaryFlag) {
    if (isVoluntaryFlag == null) {
      throw new BadRequestException("isVoluntaryFlag is required");
    }
    return repository.findByIsVoluntaryFlag(isVoluntaryFlag).stream()
        .map(mapper::toResponse)
        .toList();
  }

  @Override
  public List<EmploymentExitFactResponse> getEmploymentExitsByIsInvoluntaryFlag(Boolean isInvoluntaryFlag) {
    if (isInvoluntaryFlag == null) {
      throw new BadRequestException("isInvoluntaryFlag is required");
    }
    return repository.findByIsInvoluntaryFlag(isInvoluntaryFlag).stream()
        .map(mapper::toResponse)
        .toList();
  }
}
