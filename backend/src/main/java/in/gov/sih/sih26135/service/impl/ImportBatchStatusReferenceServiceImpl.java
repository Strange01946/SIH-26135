package in.gov.sih.sih26135.service.impl;

import in.gov.sih.sih26135.dto.response.ImportBatchStatusResponse;
import in.gov.sih.sih26135.entity.RefImportBatchStatus;
import in.gov.sih.sih26135.exception.BadRequestException;
import in.gov.sih.sih26135.exception.ResourceNotFoundException;
import in.gov.sih.sih26135.mapper.ImportBatchStatusMapper;
import in.gov.sih.sih26135.repository.RefImportBatchStatusRepository;
import in.gov.sih.sih26135.service.ImportBatchStatusReferenceService;
import java.util.List;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class ImportBatchStatusReferenceServiceImpl implements ImportBatchStatusReferenceService {

  private final RefImportBatchStatusRepository repository;
  private final ImportBatchStatusMapper mapper;

  public ImportBatchStatusReferenceServiceImpl(
      RefImportBatchStatusRepository repository,
      ImportBatchStatusMapper mapper) {
    this.repository = repository;
    this.mapper = mapper;
  }

  @Override
  public List<ImportBatchStatusResponse> getAllStatuses() {
    return repository.findAll(Sort.by(Sort.Direction.ASC, "sortOrder")).stream()
        .map(mapper::toResponse)
        .toList();
  }

  @Override
  public ImportBatchStatusResponse getById(Long id) {
    if (id == null) {
      throw new BadRequestException("Import batch status ID is required");
    }
    RefImportBatchStatus entity = repository.findById(id)
        .orElseThrow(() -> new ResourceNotFoundException("RefImportBatchStatus", "id"));
    return mapper.toResponse(entity);
  }

  @Override
  public ImportBatchStatusResponse getByCode(String statusCode) {
    if (statusCode == null || statusCode.isBlank()) {
      throw new BadRequestException("Status code is required");
    }
    RefImportBatchStatus entity = repository.findByStatusCode(statusCode.trim())
        .orElseThrow(() -> new ResourceNotFoundException("RefImportBatchStatus", "statusCode"));
    return mapper.toResponse(entity);
  }
}
