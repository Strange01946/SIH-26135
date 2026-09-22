package in.gov.sih.sih26135.service.impl;

import in.gov.sih.sih26135.dto.response.PlacementSourceResponse;
import in.gov.sih.sih26135.entity.RefPlacementSource;
import in.gov.sih.sih26135.exception.BadRequestException;
import in.gov.sih.sih26135.exception.ResourceNotFoundException;
import in.gov.sih.sih26135.mapper.PlacementSourceMapper;
import in.gov.sih.sih26135.repository.RefPlacementSourceRepository;
import in.gov.sih.sih26135.service.PlacementSourceReferenceService;
import java.util.List;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class PlacementSourceReferenceServiceImpl implements PlacementSourceReferenceService {

  private final RefPlacementSourceRepository refPlacementSourceRepository;
  private final PlacementSourceMapper placementSourceMapper;

  public PlacementSourceReferenceServiceImpl(
      RefPlacementSourceRepository refPlacementSourceRepository,
      PlacementSourceMapper placementSourceMapper) {
    this.refPlacementSourceRepository = refPlacementSourceRepository;
    this.placementSourceMapper = placementSourceMapper;
  }

  @Override
  public List<PlacementSourceResponse> getAllPlacementSources() {
    return refPlacementSourceRepository.findAll(Sort.by(Sort.Direction.ASC, "sortOrder")).stream()
        .map(placementSourceMapper::toResponse)
        .toList();
  }

  @Override
  public PlacementSourceResponse getById(Long id) {
    if (id == null) {
      throw new BadRequestException("Placement source ID is required");
    }
    RefPlacementSource entity = refPlacementSourceRepository.findById(id)
        .orElseThrow(() -> new ResourceNotFoundException("RefPlacementSource", "id"));
    return placementSourceMapper.toResponse(entity);
  }

  @Override
  public PlacementSourceResponse getByCode(String sourceCode) {
    if (sourceCode == null || sourceCode.isBlank()) {
      throw new BadRequestException("Source code is required");
    }
    RefPlacementSource entity = refPlacementSourceRepository.findBySourceCode(sourceCode.trim())
        .orElseThrow(() -> new ResourceNotFoundException("RefPlacementSource", "sourceCode"));
    return placementSourceMapper.toResponse(entity);
  }
}
