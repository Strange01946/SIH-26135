package in.gov.sih.sih26135.service.impl;

import in.gov.sih.sih26135.dto.response.SystemEventCategoryResponse;
import in.gov.sih.sih26135.entity.RefSystemEventCategory;
import in.gov.sih.sih26135.exception.BadRequestException;
import in.gov.sih.sih26135.exception.ResourceNotFoundException;
import in.gov.sih.sih26135.mapper.SystemEventCategoryMapper;
import in.gov.sih.sih26135.repository.RefSystemEventCategoryRepository;
import in.gov.sih.sih26135.service.SystemEventCategoryReferenceService;
import java.util.List;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class SystemEventCategoryReferenceServiceImpl implements SystemEventCategoryReferenceService {

  private final RefSystemEventCategoryRepository repository;
  private final SystemEventCategoryMapper mapper;

  public SystemEventCategoryReferenceServiceImpl(
      RefSystemEventCategoryRepository repository,
      SystemEventCategoryMapper mapper) {
    this.repository = repository;
    this.mapper = mapper;
  }

  @Override
  public List<SystemEventCategoryResponse> getAllCategories() {
    return repository.findAll(Sort.by(Sort.Direction.ASC, "sortOrder")).stream()
        .map(mapper::toResponse)
        .toList();
  }

  @Override
  public SystemEventCategoryResponse getById(Long id) {
    if (id == null) {
      throw new BadRequestException("System event category ID is required");
    }
    RefSystemEventCategory entity = repository.findById(id)
        .orElseThrow(() -> new ResourceNotFoundException("RefSystemEventCategory", "id"));
    return mapper.toResponse(entity);
  }

  @Override
  public SystemEventCategoryResponse getByCode(String categoryCode) {
    if (categoryCode == null || categoryCode.isBlank()) {
      throw new BadRequestException("Category code is required");
    }
    RefSystemEventCategory entity = repository.findByCategoryCode(categoryCode.trim())
        .orElseThrow(() -> new ResourceNotFoundException("RefSystemEventCategory", "categoryCode"));
    return mapper.toResponse(entity);
  }
}
