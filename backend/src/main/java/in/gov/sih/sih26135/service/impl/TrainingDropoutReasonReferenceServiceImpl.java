package in.gov.sih.sih26135.service.impl;

import in.gov.sih.sih26135.dto.response.TrainingDropoutReasonResponse;
import in.gov.sih.sih26135.entity.RefTrainingDropoutReason;
import in.gov.sih.sih26135.exception.BadRequestException;
import in.gov.sih.sih26135.exception.ResourceNotFoundException;
import in.gov.sih.sih26135.mapper.TrainingDropoutReasonMapper;
import in.gov.sih.sih26135.repository.RefTrainingDropoutReasonRepository;
import in.gov.sih.sih26135.service.TrainingDropoutReasonReferenceService;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class TrainingDropoutReasonReferenceServiceImpl implements TrainingDropoutReasonReferenceService {

  private final RefTrainingDropoutReasonRepository refTrainingDropoutReasonRepository;
  private final TrainingDropoutReasonMapper trainingDropoutReasonMapper;

  public TrainingDropoutReasonReferenceServiceImpl(
      RefTrainingDropoutReasonRepository refTrainingDropoutReasonRepository,
      TrainingDropoutReasonMapper trainingDropoutReasonMapper) {
    this.refTrainingDropoutReasonRepository = refTrainingDropoutReasonRepository;
    this.trainingDropoutReasonMapper = trainingDropoutReasonMapper;
  }

  @Override
  public List<TrainingDropoutReasonResponse> getAllDropoutReasons() {
    return refTrainingDropoutReasonRepository.findAllByOrderBySortOrderAsc().stream()
        .map(trainingDropoutReasonMapper::toResponse)
        .toList();
  }

  @Override
  public TrainingDropoutReasonResponse getById(Long id) {
    if (id == null) {
      throw new BadRequestException("Dropout reason ID is required");
    }
    RefTrainingDropoutReason reason = refTrainingDropoutReasonRepository.findById(id)
        .orElseThrow(() -> new ResourceNotFoundException("RefTrainingDropoutReason", "id"));
    return trainingDropoutReasonMapper.toResponse(reason);
  }

  @Override
  public TrainingDropoutReasonResponse getByCode(String reasonCode) {
    if (reasonCode == null || reasonCode.isBlank()) {
      throw new BadRequestException("Reason code is required");
    }
    RefTrainingDropoutReason reason = refTrainingDropoutReasonRepository.findByReasonCode(reasonCode.trim())
        .orElseThrow(() -> new ResourceNotFoundException("RefTrainingDropoutReason", "reasonCode"));
    return trainingDropoutReasonMapper.toResponse(reason);
  }
}
