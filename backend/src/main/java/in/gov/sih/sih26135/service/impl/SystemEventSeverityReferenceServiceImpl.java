package in.gov.sih.sih26135.service.impl;

import in.gov.sih.sih26135.dto.response.SystemEventSeverityResponse;
import in.gov.sih.sih26135.entity.RefSystemEventSeverity;
import in.gov.sih.sih26135.exception.BadRequestException;
import in.gov.sih.sih26135.exception.ResourceNotFoundException;
import in.gov.sih.sih26135.mapper.SystemEventSeverityMapper;
import in.gov.sih.sih26135.repository.RefSystemEventSeverityRepository;
import in.gov.sih.sih26135.service.SystemEventSeverityReferenceService;
import java.util.List;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class SystemEventSeverityReferenceServiceImpl implements SystemEventSeverityReferenceService {

  private final RefSystemEventSeverityRepository repository;
  private final SystemEventSeverityMapper mapper;

  public SystemEventSeverityReferenceServiceImpl(
      RefSystemEventSeverityRepository repository,
      SystemEventSeverityMapper mapper) {
    this.repository = repository;
    this.mapper = mapper;
  }

  @Override
  public List<SystemEventSeverityResponse> getAllSeverities() {
    return repository.findAll(Sort.by(Sort.Direction.ASC, "sortOrder")).stream()
        .map(mapper::toResponse)
        .toList();
  }

  @Override
  public SystemEventSeverityResponse getById(Long id) {
    if (id == null) {
      throw new BadRequestException("System event severity ID is required");
    }
    RefSystemEventSeverity entity = repository.findById(id)
        .orElseThrow(() -> new ResourceNotFoundException("RefSystemEventSeverity", "id"));
    return mapper.toResponse(entity);
  }

  @Override
  public SystemEventSeverityResponse getByCode(String severityCode) {
    if (severityCode == null || severityCode.isBlank()) {
      throw new BadRequestException("Severity code is required");
    }
    RefSystemEventSeverity entity = repository.findBySeverityCode(severityCode.trim())
        .orElseThrow(() -> new ResourceNotFoundException("RefSystemEventSeverity", "severityCode"));
    return mapper.toResponse(entity);
  }
}
