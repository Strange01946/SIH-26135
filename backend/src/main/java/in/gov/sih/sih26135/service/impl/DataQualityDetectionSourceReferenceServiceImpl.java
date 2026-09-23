package in.gov.sih.sih26135.service.impl;

import in.gov.sih.sih26135.dto.response.DataQualityDetectionSourceResponse;
import in.gov.sih.sih26135.entity.RefDataQualityDetectionSource;
import in.gov.sih.sih26135.exception.BadRequestException;
import in.gov.sih.sih26135.exception.ResourceNotFoundException;
import in.gov.sih.sih26135.mapper.DataQualityDetectionSourceMapper;
import in.gov.sih.sih26135.repository.RefDataQualityDetectionSourceRepository;
import in.gov.sih.sih26135.service.DataQualityDetectionSourceReferenceService;
import java.util.List;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class DataQualityDetectionSourceReferenceServiceImpl implements DataQualityDetectionSourceReferenceService {

  private final RefDataQualityDetectionSourceRepository repository;
  private final DataQualityDetectionSourceMapper mapper;

  public DataQualityDetectionSourceReferenceServiceImpl(
      RefDataQualityDetectionSourceRepository repository,
      DataQualityDetectionSourceMapper mapper) {
    this.repository = repository;
    this.mapper = mapper;
  }

  @Override
  public List<DataQualityDetectionSourceResponse> getAllSources() {
    return repository.findAll(Sort.by(Sort.Direction.ASC, "sortOrder")).stream()
        .map(mapper::toResponse)
        .toList();
  }

  @Override
  public DataQualityDetectionSourceResponse getById(Long id) {
    if (id == null) {
      throw new BadRequestException("Data quality detection source ID is required");
    }
    RefDataQualityDetectionSource entity = repository.findById(id)
        .orElseThrow(() -> new ResourceNotFoundException("RefDataQualityDetectionSource", "id"));
    return mapper.toResponse(entity);
  }

  @Override
  public DataQualityDetectionSourceResponse getByCode(String sourceCode) {
    if (sourceCode == null || sourceCode.isBlank()) {
      throw new BadRequestException("Source code is required");
    }
    RefDataQualityDetectionSource entity = repository.findBySourceCode(sourceCode.trim())
        .orElseThrow(() -> new ResourceNotFoundException("RefDataQualityDetectionSource", "sourceCode"));
    return mapper.toResponse(entity);
  }
}
