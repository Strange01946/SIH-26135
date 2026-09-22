package in.gov.sih.sih26135.service.impl;

import in.gov.sih.sih26135.dto.response.QualificationLevelResponse;
import in.gov.sih.sih26135.entity.RefQualificationLevel;
import in.gov.sih.sih26135.exception.ResourceNotFoundException;
import in.gov.sih.sih26135.mapper.QualificationLevelMapper;
import in.gov.sih.sih26135.repository.RefQualificationLevelRepository;
import in.gov.sih.sih26135.service.QualificationLevelReferenceService;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class QualificationLevelReferenceServiceImpl implements QualificationLevelReferenceService {

  private final RefQualificationLevelRepository refQualificationLevelRepository;
  private final QualificationLevelMapper qualificationLevelMapper;

  public QualificationLevelReferenceServiceImpl(
      RefQualificationLevelRepository refQualificationLevelRepository,
      QualificationLevelMapper qualificationLevelMapper) {
    this.refQualificationLevelRepository = refQualificationLevelRepository;
    this.qualificationLevelMapper = qualificationLevelMapper;
  }

  @Override
  public List<QualificationLevelResponse> getAllQualificationLevels() {
    return refQualificationLevelRepository.findAll().stream()
        .map(qualificationLevelMapper::toResponse)
        .toList();
  }

  @Override
  public QualificationLevelResponse getById(Long id) {
    RefQualificationLevel level = refQualificationLevelRepository.findById(id)
        .orElseThrow(() -> new ResourceNotFoundException("RefQualificationLevel", "id"));
    return qualificationLevelMapper.toResponse(level);
  }

  @Override
  public QualificationLevelResponse getByCode(String levelCode) {
    RefQualificationLevel level = refQualificationLevelRepository.findByLevelCode(levelCode)
        .orElseThrow(() -> new ResourceNotFoundException("RefQualificationLevel", "levelCode"));
    return qualificationLevelMapper.toResponse(level);
  }
}
