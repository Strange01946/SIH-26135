package in.gov.sih.sih26135.service.impl;

import in.gov.sih.sih26135.dto.response.DataQualityCategoryResponse;
import in.gov.sih.sih26135.entity.RefDataQualityCategory;
import in.gov.sih.sih26135.exception.BadRequestException;
import in.gov.sih.sih26135.exception.ResourceNotFoundException;
import in.gov.sih.sih26135.mapper.DataQualityCategoryMapper;
import in.gov.sih.sih26135.repository.RefDataQualityCategoryRepository;
import in.gov.sih.sih26135.service.DataQualityCategoryReferenceService;
import java.util.List;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class DataQualityCategoryReferenceServiceImpl implements DataQualityCategoryReferenceService {

  private final RefDataQualityCategoryRepository repository;
  private final DataQualityCategoryMapper mapper;

  public DataQualityCategoryReferenceServiceImpl(
      RefDataQualityCategoryRepository repository,
      DataQualityCategoryMapper mapper) {
    this.repository = repository;
    this.mapper = mapper;
  }

  @Override
  public List<DataQualityCategoryResponse> getAllCategories() {
    return repository.findAll(Sort.by(Sort.Direction.ASC, "sortOrder")).stream()
        .map(mapper::toResponse)
        .toList();
  }

  @Override
  public DataQualityCategoryResponse getById(Long id) {
    if (id == null) {
      throw new BadRequestException("Data quality category ID is required");
    }
    RefDataQualityCategory entity = repository.findById(id)
        .orElseThrow(() -> new ResourceNotFoundException("RefDataQualityCategory", "id"));
    return mapper.toResponse(entity);
  }

  @Override
  public DataQualityCategoryResponse getByCode(String categoryCode) {
    if (categoryCode == null || categoryCode.isBlank()) {
      throw new BadRequestException("Category code is required");
    }
    RefDataQualityCategory entity = repository.findByCategoryCode(categoryCode.trim())
        .orElseThrow(() -> new ResourceNotFoundException("RefDataQualityCategory", "categoryCode"));
    return mapper.toResponse(entity);
  }
}
