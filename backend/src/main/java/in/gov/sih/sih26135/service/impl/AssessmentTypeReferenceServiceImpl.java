package in.gov.sih.sih26135.service.impl;

import in.gov.sih.sih26135.dto.response.AssessmentTypeResponse;
import in.gov.sih.sih26135.entity.RefAssessmentType;
import in.gov.sih.sih26135.exception.BadRequestException;
import in.gov.sih.sih26135.exception.ResourceNotFoundException;
import in.gov.sih.sih26135.mapper.AssessmentTypeMapper;
import in.gov.sih.sih26135.repository.RefAssessmentTypeRepository;
import in.gov.sih.sih26135.service.AssessmentTypeReferenceService;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class AssessmentTypeReferenceServiceImpl implements AssessmentTypeReferenceService {

  private final RefAssessmentTypeRepository refAssessmentTypeRepository;
  private final AssessmentTypeMapper assessmentTypeMapper;

  public AssessmentTypeReferenceServiceImpl(
      RefAssessmentTypeRepository refAssessmentTypeRepository,
      AssessmentTypeMapper assessmentTypeMapper) {
    this.refAssessmentTypeRepository = refAssessmentTypeRepository;
    this.assessmentTypeMapper = assessmentTypeMapper;
  }

  @Override
  public List<AssessmentTypeResponse> getAllAssessmentTypes() {
    return refAssessmentTypeRepository.findAllByOrderBySortOrderAsc().stream()
        .map(assessmentTypeMapper::toResponse)
        .toList();
  }

  @Override
  public AssessmentTypeResponse getById(Long id) {
    if (id == null) {
      throw new BadRequestException("Assessment type ID is required");
    }
    RefAssessmentType type = refAssessmentTypeRepository.findById(id)
        .orElseThrow(() -> new ResourceNotFoundException("RefAssessmentType", "id"));
    return assessmentTypeMapper.toResponse(type);
  }

  @Override
  public AssessmentTypeResponse getByCode(String typeCode) {
    if (typeCode == null || typeCode.isBlank()) {
      throw new BadRequestException("Assessment type code is required");
    }
    RefAssessmentType type = refAssessmentTypeRepository.findByTypeCode(typeCode.trim())
        .orElseThrow(() -> new ResourceNotFoundException("RefAssessmentType", "typeCode"));
    return assessmentTypeMapper.toResponse(type);
  }
}
