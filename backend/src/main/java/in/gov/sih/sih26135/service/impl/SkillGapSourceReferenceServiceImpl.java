package in.gov.sih.sih26135.service.impl;

import in.gov.sih.sih26135.dto.response.SkillGapSourceResponse;
import in.gov.sih.sih26135.entity.RefSkillGapSource;
import in.gov.sih.sih26135.exception.BadRequestException;
import in.gov.sih.sih26135.exception.ResourceNotFoundException;
import in.gov.sih.sih26135.mapper.SkillGapSourceMapper;
import in.gov.sih.sih26135.repository.RefSkillGapSourceRepository;
import in.gov.sih.sih26135.service.SkillGapSourceReferenceService;
import java.util.List;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class SkillGapSourceReferenceServiceImpl implements SkillGapSourceReferenceService {

  private final RefSkillGapSourceRepository repository;
  private final SkillGapSourceMapper mapper;

  public SkillGapSourceReferenceServiceImpl(
      RefSkillGapSourceRepository repository,
      SkillGapSourceMapper mapper) {
    this.repository = repository;
    this.mapper = mapper;
  }

  @Override
  public List<SkillGapSourceResponse> getAllSources() {
    return repository.findAll(Sort.by(Sort.Direction.ASC, "sortOrder")).stream()
        .map(mapper::toResponse)
        .toList();
  }

  @Override
  public SkillGapSourceResponse getById(Long id) {
    if (id == null) {
      throw new BadRequestException("Skill gap source ID is required");
    }
    RefSkillGapSource entity = repository.findById(id)
        .orElseThrow(() -> new ResourceNotFoundException("RefSkillGapSource", "id"));
    return mapper.toResponse(entity);
  }

  @Override
  public SkillGapSourceResponse getByCode(String sourceCode) {
    if (sourceCode == null || sourceCode.isBlank()) {
      throw new BadRequestException("Source code is required");
    }
    RefSkillGapSource entity = repository.findBySourceCode(sourceCode.trim())
        .orElseThrow(() -> new ResourceNotFoundException("RefSkillGapSource", "sourceCode"));
    return mapper.toResponse(entity);
  }
}
