package in.gov.sih.sih26135.service.impl;

import in.gov.sih.sih26135.dto.response.EngagementTypeResponse;
import in.gov.sih.sih26135.entity.RefEngagementType;
import in.gov.sih.sih26135.exception.BadRequestException;
import in.gov.sih.sih26135.exception.ResourceNotFoundException;
import in.gov.sih.sih26135.mapper.EngagementTypeMapper;
import in.gov.sih.sih26135.repository.RefEngagementTypeRepository;
import in.gov.sih.sih26135.service.EngagementTypeReferenceService;
import java.util.List;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class EngagementTypeReferenceServiceImpl implements EngagementTypeReferenceService {

  private final RefEngagementTypeRepository refEngagementTypeRepository;
  private final EngagementTypeMapper engagementTypeMapper;

  public EngagementTypeReferenceServiceImpl(
      RefEngagementTypeRepository refEngagementTypeRepository,
      EngagementTypeMapper engagementTypeMapper) {
    this.refEngagementTypeRepository = refEngagementTypeRepository;
    this.engagementTypeMapper = engagementTypeMapper;
  }

  @Override
  public List<EngagementTypeResponse> getAllEngagementTypes() {
    return refEngagementTypeRepository.findAll(Sort.by(Sort.Direction.ASC, "sortOrder")).stream()
        .map(engagementTypeMapper::toResponse)
        .toList();
  }

  @Override
  public EngagementTypeResponse getById(Long id) {
    if (id == null) {
      throw new BadRequestException("Engagement type ID is required");
    }
    RefEngagementType entity = refEngagementTypeRepository.findById(id)
        .orElseThrow(() -> new ResourceNotFoundException("RefEngagementType", "id"));
    return engagementTypeMapper.toResponse(entity);
  }

  @Override
  public EngagementTypeResponse getByCode(String typeCode) {
    if (typeCode == null || typeCode.isBlank()) {
      throw new BadRequestException("Type code is required");
    }
    RefEngagementType entity = refEngagementTypeRepository.findByTypeCode(typeCode.trim())
        .orElseThrow(() -> new ResourceNotFoundException("RefEngagementType", "typeCode"));
    return engagementTypeMapper.toResponse(entity);
  }

  @Override
  public List<EngagementTypeResponse> getByRequiresEmployer(Boolean requiresEmployer) {
    if (requiresEmployer == null) {
      throw new BadRequestException("requiresEmployer flag is required");
    }
    return refEngagementTypeRepository.findByRequiresEmployer(requiresEmployer).stream()
        .map(engagementTypeMapper::toResponse)
        .toList();
  }
}
