package in.gov.sih.sih26135.service.impl;

import in.gov.sih.sih26135.dto.response.BatchStatusResponse;
import in.gov.sih.sih26135.entity.RefBatchStatus;
import in.gov.sih.sih26135.exception.ResourceNotFoundException;
import in.gov.sih.sih26135.mapper.BatchStatusMapper;
import in.gov.sih.sih26135.repository.RefBatchStatusRepository;
import in.gov.sih.sih26135.service.BatchStatusReferenceService;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class BatchStatusReferenceServiceImpl implements BatchStatusReferenceService {

  private final RefBatchStatusRepository refBatchStatusRepository;
  private final BatchStatusMapper batchStatusMapper;

  public BatchStatusReferenceServiceImpl(
      RefBatchStatusRepository refBatchStatusRepository,
      BatchStatusMapper batchStatusMapper) {
    this.refBatchStatusRepository = refBatchStatusRepository;
    this.batchStatusMapper = batchStatusMapper;
  }

  @Override
  public List<BatchStatusResponse> getAllBatchStatuses() {
    return refBatchStatusRepository.findAll().stream()
        .map(batchStatusMapper::toResponse)
        .toList();
  }

  @Override
  public BatchStatusResponse getById(Long id) {
    RefBatchStatus status = refBatchStatusRepository.findById(id)
        .orElseThrow(() -> new ResourceNotFoundException("RefBatchStatus", "id"));
    return batchStatusMapper.toResponse(status);
  }

  @Override
  public BatchStatusResponse getByCode(String statusCode) {
    RefBatchStatus status = refBatchStatusRepository.findByStatusCode(statusCode)
        .orElseThrow(() -> new ResourceNotFoundException("RefBatchStatus", "statusCode"));
    return batchStatusMapper.toResponse(status);
  }
}
