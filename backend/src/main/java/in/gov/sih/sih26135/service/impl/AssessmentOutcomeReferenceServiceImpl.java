package in.gov.sih.sih26135.service.impl;

import in.gov.sih.sih26135.dto.response.AssessmentOutcomeResponse;
import in.gov.sih.sih26135.entity.RefAssessmentOutcome;
import in.gov.sih.sih26135.exception.BadRequestException;
import in.gov.sih.sih26135.exception.ResourceNotFoundException;
import in.gov.sih.sih26135.mapper.AssessmentOutcomeMapper;
import in.gov.sih.sih26135.repository.RefAssessmentOutcomeRepository;
import in.gov.sih.sih26135.service.AssessmentOutcomeReferenceService;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class AssessmentOutcomeReferenceServiceImpl implements AssessmentOutcomeReferenceService {

  private final RefAssessmentOutcomeRepository refAssessmentOutcomeRepository;
  private final AssessmentOutcomeMapper assessmentOutcomeMapper;

  public AssessmentOutcomeReferenceServiceImpl(
      RefAssessmentOutcomeRepository refAssessmentOutcomeRepository,
      AssessmentOutcomeMapper assessmentOutcomeMapper) {
    this.refAssessmentOutcomeRepository = refAssessmentOutcomeRepository;
    this.assessmentOutcomeMapper = assessmentOutcomeMapper;
  }

  @Override
  public List<AssessmentOutcomeResponse> getAllAssessmentOutcomes() {
    return refAssessmentOutcomeRepository.findAllByOrderBySortOrderAsc().stream()
        .map(assessmentOutcomeMapper::toResponse)
        .toList();
  }

  @Override
  public AssessmentOutcomeResponse getById(Long id) {
    if (id == null) {
      throw new BadRequestException("Assessment outcome ID is required");
    }
    RefAssessmentOutcome outcome = refAssessmentOutcomeRepository.findById(id)
        .orElseThrow(() -> new ResourceNotFoundException("RefAssessmentOutcome", "id"));
    return assessmentOutcomeMapper.toResponse(outcome);
  }

  @Override
  public AssessmentOutcomeResponse getByCode(String outcomeCode) {
    if (outcomeCode == null || outcomeCode.isBlank()) {
      throw new BadRequestException("Assessment outcome code is required");
    }
    RefAssessmentOutcome outcome = refAssessmentOutcomeRepository.findByOutcomeCode(outcomeCode.trim())
        .orElseThrow(() -> new ResourceNotFoundException("RefAssessmentOutcome", "outcomeCode"));
    return assessmentOutcomeMapper.toResponse(outcome);
  }

  @Override
  public List<AssessmentOutcomeResponse> getPassingOutcomes() {
    return refAssessmentOutcomeRepository.findAllByOrderBySortOrderAsc().stream()
        .filter(o -> Boolean.TRUE.equals(o.getIsPassFlag()))
        .map(assessmentOutcomeMapper::toResponse)
        .toList();
  }
}
