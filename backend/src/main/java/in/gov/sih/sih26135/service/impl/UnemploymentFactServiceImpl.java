package in.gov.sih.sih26135.service.impl;

import in.gov.sih.sih26135.dto.response.UnemploymentFactResponse;
import in.gov.sih.sih26135.entity.analytics.UnemploymentFact;
import in.gov.sih.sih26135.exception.BadRequestException;
import in.gov.sih.sih26135.exception.ResourceNotFoundException;
import in.gov.sih.sih26135.mapper.UnemploymentFactMapper;
import in.gov.sih.sih26135.repository.analytics.UnemploymentFactRepository;
import in.gov.sih.sih26135.service.UnemploymentFactService;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class UnemploymentFactServiceImpl implements UnemploymentFactService {

  private final UnemploymentFactRepository repository;
  private final UnemploymentFactMapper mapper;

  public UnemploymentFactServiceImpl(
      UnemploymentFactRepository repository,
      UnemploymentFactMapper mapper) {
    this.repository = repository;
    this.mapper = mapper;
  }

  @Override
  public List<UnemploymentFactResponse> getAllUnemploymentFacts() {
    return repository.findAll().stream()
        .map(mapper::toResponse)
        .toList();
  }

  @Override
  public UnemploymentFactResponse getUnemploymentFactById(Long unemploymentEventId) {
    if (unemploymentEventId == null) {
      throw new BadRequestException("Unemployment event ID is required");
    }
    UnemploymentFact entity = repository.findById(unemploymentEventId)
        .orElseThrow(() -> new ResourceNotFoundException("UnemploymentFact", "unemploymentEventId"));
    return mapper.toResponse(entity);
  }

  @Override
  public List<UnemploymentFactResponse> getUnemploymentFactsByTraineeId(Long traineeId) {
    if (traineeId == null) {
      throw new BadRequestException("Trainee ID is required");
    }
    return repository.findByTraineeId(traineeId).stream()
        .map(mapper::toResponse)
        .toList();
  }

  @Override
  public List<UnemploymentFactResponse> getUnemploymentFactsByUnemploymentReasonId(Long unemploymentReasonId) {
    if (unemploymentReasonId == null) {
      throw new BadRequestException("Unemployment reason ID is required");
    }
    return repository.findByUnemploymentReasonId(unemploymentReasonId).stream()
        .map(mapper::toResponse)
        .toList();
  }

  @Override
  public List<UnemploymentFactResponse> getUnemploymentFactsByLabourStatusId(Long labourStatusId) {
    if (labourStatusId == null) {
      throw new BadRequestException("Labour status ID is required");
    }
    return repository.findByLabourStatusId(labourStatusId).stream()
        .map(mapper::toResponse)
        .toList();
  }

  @Override
  public List<UnemploymentFactResponse> getCurrentUnemploymentFacts() {
    return repository.findByIsCurrentTrue().stream()
        .map(mapper::toResponse)
        .toList();
  }

  @Override
  public List<UnemploymentFactResponse> getUnemploymentFactsByTraineeDistrictId(Long traineeDistrictId) {
    if (traineeDistrictId == null) {
      throw new BadRequestException("Trainee district ID is required");
    }
    return repository.findByTraineeDistrictId(traineeDistrictId).stream()
        .map(mapper::toResponse)
        .toList();
  }
}
