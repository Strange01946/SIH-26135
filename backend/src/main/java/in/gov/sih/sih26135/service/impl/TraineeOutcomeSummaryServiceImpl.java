package in.gov.sih.sih26135.service.impl;

import in.gov.sih.sih26135.dto.response.TraineeOutcomeSummaryResponse;
import in.gov.sih.sih26135.entity.analytics.TraineeOutcomeSummary;
import in.gov.sih.sih26135.exception.BadRequestException;
import in.gov.sih.sih26135.exception.ResourceNotFoundException;
import in.gov.sih.sih26135.mapper.TraineeOutcomeSummaryMapper;
import in.gov.sih.sih26135.repository.analytics.TraineeOutcomeSummaryRepository;
import in.gov.sih.sih26135.service.TraineeOutcomeSummaryService;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class TraineeOutcomeSummaryServiceImpl implements TraineeOutcomeSummaryService {

  private final TraineeOutcomeSummaryRepository repository;
  private final TraineeOutcomeSummaryMapper mapper;

  public TraineeOutcomeSummaryServiceImpl(
      TraineeOutcomeSummaryRepository repository,
      TraineeOutcomeSummaryMapper mapper) {
    this.repository = repository;
    this.mapper = mapper;
  }

  @Override
  public List<TraineeOutcomeSummaryResponse> getAllTraineeOutcomes() {
    return repository.findAll().stream()
        .map(mapper::toResponse)
        .toList();
  }

  @Override
  public TraineeOutcomeSummaryResponse getTraineeOutcomeByTraineeId(Long traineeId) {
    if (traineeId == null) {
      throw new BadRequestException("Trainee ID is required");
    }
    TraineeOutcomeSummary entity = repository.findById(traineeId)
        .orElseThrow(() -> new ResourceNotFoundException("TraineeOutcomeSummary", "traineeId"));
    return mapper.toResponse(entity);
  }

  @Override
  public List<TraineeOutcomeSummaryResponse> getTraineeOutcomesByStateId(Long stateId) {
    if (stateId == null) {
      throw new BadRequestException("State ID is required");
    }
    return repository.findByStateId(stateId).stream()
        .map(mapper::toResponse)
        .toList();
  }

  @Override
  public List<TraineeOutcomeSummaryResponse> getTraineeOutcomesByDistrictId(Long districtId) {
    if (districtId == null) {
      throw new BadRequestException("District ID is required");
    }
    return repository.findByDistrictId(districtId).stream()
        .map(mapper::toResponse)
        .toList();
  }

  @Override
  public List<TraineeOutcomeSummaryResponse> getTraineeOutcomesByCurrentEmploymentStatusId(Long currentEmploymentStatusId) {
    if (currentEmploymentStatusId == null) {
      throw new BadRequestException("Current employment status ID is required");
    }
    return repository.findByCurrentEmploymentStatusId(currentEmploymentStatusId).stream()
        .map(mapper::toResponse)
        .toList();
  }

  @Override
  public List<TraineeOutcomeSummaryResponse> getTraineeOutcomesBySnapshotIsEmployedFlag(Boolean snapshotIsEmployedFlag) {
    if (snapshotIsEmployedFlag == null) {
      throw new BadRequestException("snapshotIsEmployedFlag is required");
    }
    return repository.findBySnapshotIsEmployedFlag(snapshotIsEmployedFlag).stream()
        .map(mapper::toResponse)
        .toList();
  }
}
