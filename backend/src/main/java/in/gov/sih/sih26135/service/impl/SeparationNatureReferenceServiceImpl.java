package in.gov.sih.sih26135.service.impl;

import in.gov.sih.sih26135.dto.response.SeparationNatureResponse;
import in.gov.sih.sih26135.entity.RefSeparationNature;
import in.gov.sih.sih26135.exception.BadRequestException;
import in.gov.sih.sih26135.exception.ResourceNotFoundException;
import in.gov.sih.sih26135.mapper.SeparationNatureMapper;
import in.gov.sih.sih26135.repository.RefSeparationNatureRepository;
import in.gov.sih.sih26135.service.SeparationNatureReferenceService;
import java.util.List;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class SeparationNatureReferenceServiceImpl implements SeparationNatureReferenceService {

  private final RefSeparationNatureRepository repository;
  private final SeparationNatureMapper mapper;

  public SeparationNatureReferenceServiceImpl(
      RefSeparationNatureRepository repository,
      SeparationNatureMapper mapper) {
    this.repository = repository;
    this.mapper = mapper;
  }

  @Override
  public List<SeparationNatureResponse> getAllNatures() {
    return repository.findAll(Sort.by(Sort.Direction.ASC, "sortOrder")).stream()
        .map(mapper::toResponse)
        .toList();
  }

  @Override
  public SeparationNatureResponse getById(Long id) {
    if (id == null) {
      throw new BadRequestException("Separation nature ID is required");
    }
    RefSeparationNature entity = repository.findById(id)
        .orElseThrow(() -> new ResourceNotFoundException("RefSeparationNature", "id"));
    return mapper.toResponse(entity);
  }

  @Override
  public SeparationNatureResponse getByCode(String natureCode) {
    if (natureCode == null || natureCode.isBlank()) {
      throw new BadRequestException("Nature code is required");
    }
    RefSeparationNature entity = repository.findByNatureCode(natureCode.trim())
        .orElseThrow(() -> new ResourceNotFoundException("RefSeparationNature", "natureCode"));
    return mapper.toResponse(entity);
  }
}
