package in.gov.sih.sih26135.service.impl;

import in.gov.sih.sih26135.dto.response.RecordVerificationStatusResponse;
import in.gov.sih.sih26135.entity.RefRecordVerificationStatus;
import in.gov.sih.sih26135.exception.BadRequestException;
import in.gov.sih.sih26135.exception.ResourceNotFoundException;
import in.gov.sih.sih26135.mapper.RecordVerificationStatusMapper;
import in.gov.sih.sih26135.repository.RefRecordVerificationStatusRepository;
import in.gov.sih.sih26135.service.RecordVerificationStatusReferenceService;
import java.util.List;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class RecordVerificationStatusReferenceServiceImpl implements RecordVerificationStatusReferenceService {

  private final RefRecordVerificationStatusRepository refRecordVerificationStatusRepository;
  private final RecordVerificationStatusMapper recordVerificationStatusMapper;

  public RecordVerificationStatusReferenceServiceImpl(
      RefRecordVerificationStatusRepository refRecordVerificationStatusRepository,
      RecordVerificationStatusMapper recordVerificationStatusMapper) {
    this.refRecordVerificationStatusRepository = refRecordVerificationStatusRepository;
    this.recordVerificationStatusMapper = recordVerificationStatusMapper;
  }

  @Override
  public List<RecordVerificationStatusResponse> getAllRecordVerificationStatuses() {
    return refRecordVerificationStatusRepository.findAll(Sort.by(Sort.Direction.ASC, "sortOrder")).stream()
        .map(recordVerificationStatusMapper::toResponse)
        .toList();
  }

  @Override
  public RecordVerificationStatusResponse getById(Long id) {
    if (id == null) {
      throw new BadRequestException("Record verification status ID is required");
    }
    RefRecordVerificationStatus entity = refRecordVerificationStatusRepository.findById(id)
        .orElseThrow(() -> new ResourceNotFoundException("RefRecordVerificationStatus", "id"));
    return recordVerificationStatusMapper.toResponse(entity);
  }

  @Override
  public RecordVerificationStatusResponse getByCode(String statusCode) {
    if (statusCode == null || statusCode.isBlank()) {
      throw new BadRequestException("Status code is required");
    }
    RefRecordVerificationStatus entity = refRecordVerificationStatusRepository.findByStatusCode(statusCode.trim())
        .orElseThrow(() -> new ResourceNotFoundException("RefRecordVerificationStatus", "statusCode"));
    return recordVerificationStatusMapper.toResponse(entity);
  }

  @Override
  public List<RecordVerificationStatusResponse> getByIsVerifiedFlag(Boolean isVerifiedFlag) {
    if (isVerifiedFlag == null) {
      throw new BadRequestException("isVerifiedFlag is required");
    }
    return refRecordVerificationStatusRepository.findByIsVerifiedFlag(isVerifiedFlag).stream()
        .map(recordVerificationStatusMapper::toResponse)
        .toList();
  }
}
