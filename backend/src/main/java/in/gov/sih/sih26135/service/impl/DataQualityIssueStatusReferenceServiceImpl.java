package in.gov.sih.sih26135.service.impl;

import in.gov.sih.sih26135.dto.response.DataQualityIssueStatusResponse;
import in.gov.sih.sih26135.entity.RefDataQualityIssueStatus;
import in.gov.sih.sih26135.exception.BadRequestException;
import in.gov.sih.sih26135.exception.ResourceNotFoundException;
import in.gov.sih.sih26135.mapper.DataQualityIssueStatusMapper;
import in.gov.sih.sih26135.repository.RefDataQualityIssueStatusRepository;
import in.gov.sih.sih26135.service.DataQualityIssueStatusReferenceService;
import java.util.List;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class DataQualityIssueStatusReferenceServiceImpl implements DataQualityIssueStatusReferenceService {

  private final RefDataQualityIssueStatusRepository repository;
  private final DataQualityIssueStatusMapper mapper;

  public DataQualityIssueStatusReferenceServiceImpl(
      RefDataQualityIssueStatusRepository repository,
      DataQualityIssueStatusMapper mapper) {
    this.repository = repository;
    this.mapper = mapper;
  }

  @Override
  public List<DataQualityIssueStatusResponse> getAllStatuses() {
    return repository.findAll(Sort.by(Sort.Direction.ASC, "sortOrder")).stream()
        .map(mapper::toResponse)
        .toList();
  }

  @Override
  public List<DataQualityIssueStatusResponse> getOpenStatuses() {
    return repository.findByIsOpenFlagTrue().stream()
        .map(mapper::toResponse)
        .toList();
  }

  @Override
  public DataQualityIssueStatusResponse getById(Long id) {
    if (id == null) {
      throw new BadRequestException("Data quality issue status ID is required");
    }
    RefDataQualityIssueStatus entity = repository.findById(id)
        .orElseThrow(() -> new ResourceNotFoundException("RefDataQualityIssueStatus", "id"));
    return mapper.toResponse(entity);
  }

  @Override
  public DataQualityIssueStatusResponse getByCode(String statusCode) {
    if (statusCode == null || statusCode.isBlank()) {
      throw new BadRequestException("Status code is required");
    }
    RefDataQualityIssueStatus entity = repository.findByStatusCode(statusCode.trim())
        .orElseThrow(() -> new ResourceNotFoundException("RefDataQualityIssueStatus", "statusCode"));
    return mapper.toResponse(entity);
  }
}
