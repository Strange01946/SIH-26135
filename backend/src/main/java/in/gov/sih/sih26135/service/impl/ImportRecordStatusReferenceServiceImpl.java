package in.gov.sih.sih26135.service.impl;

import in.gov.sih.sih26135.dto.response.ImportRecordStatusResponse;
import in.gov.sih.sih26135.entity.RefImportRecordStatus;
import in.gov.sih.sih26135.exception.BadRequestException;
import in.gov.sih.sih26135.exception.ResourceNotFoundException;
import in.gov.sih.sih26135.mapper.ImportRecordStatusMapper;
import in.gov.sih.sih26135.repository.RefImportRecordStatusRepository;
import in.gov.sih.sih26135.service.ImportRecordStatusReferenceService;
import java.util.List;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class ImportRecordStatusReferenceServiceImpl implements ImportRecordStatusReferenceService {

  private final RefImportRecordStatusRepository repository;
  private final ImportRecordStatusMapper mapper;

  public ImportRecordStatusReferenceServiceImpl(
      RefImportRecordStatusRepository repository,
      ImportRecordStatusMapper mapper) {
    this.repository = repository;
    this.mapper = mapper;
  }

  @Override
  public List<ImportRecordStatusResponse> getAllStatuses() {
    return repository.findAll(Sort.by(Sort.Direction.ASC, "sortOrder")).stream()
        .map(mapper::toResponse)
        .toList();
  }

  @Override
  public ImportRecordStatusResponse getById(Long id) {
    if (id == null) {
      throw new BadRequestException("Import record status ID is required");
    }
    RefImportRecordStatus entity = repository.findById(id)
        .orElseThrow(() -> new ResourceNotFoundException("RefImportRecordStatus", "id"));
    return mapper.toResponse(entity);
  }

  @Override
  public ImportRecordStatusResponse getByCode(String statusCode) {
    if (statusCode == null || statusCode.isBlank()) {
      throw new BadRequestException("Status code is required");
    }
    RefImportRecordStatus entity = repository.findByStatusCode(statusCode.trim())
        .orElseThrow(() -> new ResourceNotFoundException("RefImportRecordStatus", "statusCode"));
    return mapper.toResponse(entity);
  }
}
