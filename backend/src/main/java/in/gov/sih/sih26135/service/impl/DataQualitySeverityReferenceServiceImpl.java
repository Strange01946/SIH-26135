package in.gov.sih.sih26135.service.impl;

import in.gov.sih.sih26135.dto.response.DataQualitySeverityResponse;
import in.gov.sih.sih26135.entity.RefDataQualitySeverity;
import in.gov.sih.sih26135.exception.BadRequestException;
import in.gov.sih.sih26135.exception.ResourceNotFoundException;
import in.gov.sih.sih26135.mapper.DataQualitySeverityMapper;
import in.gov.sih.sih26135.repository.RefDataQualitySeverityRepository;
import in.gov.sih.sih26135.service.DataQualitySeverityReferenceService;
import java.util.List;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class DataQualitySeverityReferenceServiceImpl implements DataQualitySeverityReferenceService {

  private final RefDataQualitySeverityRepository repository;
  private final DataQualitySeverityMapper mapper;

  public DataQualitySeverityReferenceServiceImpl(
      RefDataQualitySeverityRepository repository,
      DataQualitySeverityMapper mapper) {
    this.repository = repository;
    this.mapper = mapper;
  }

  @Override
  public List<DataQualitySeverityResponse> getAllSeverities() {
    return repository.findAll(Sort.by(Sort.Direction.ASC, "sortOrder")).stream()
        .map(mapper::toResponse)
        .toList();
  }

  @Override
  public DataQualitySeverityResponse getById(Long id) {
    if (id == null) {
      throw new BadRequestException("Data quality severity ID is required");
    }
    RefDataQualitySeverity entity = repository.findById(id)
        .orElseThrow(() -> new ResourceNotFoundException("RefDataQualitySeverity", "id"));
    return mapper.toResponse(entity);
  }

  @Override
  public DataQualitySeverityResponse getByCode(String severityCode) {
    if (severityCode == null || severityCode.isBlank()) {
      throw new BadRequestException("Severity code is required");
    }
    RefDataQualitySeverity entity = repository.findBySeverityCode(severityCode.trim())
        .orElseThrow(() -> new ResourceNotFoundException("RefDataQualitySeverity", "severityCode"));
    return mapper.toResponse(entity);
  }
}
