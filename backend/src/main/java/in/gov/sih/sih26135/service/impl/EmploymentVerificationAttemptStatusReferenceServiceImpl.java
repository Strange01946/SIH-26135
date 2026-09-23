package in.gov.sih.sih26135.service.impl;

import in.gov.sih.sih26135.dto.response.EmploymentVerificationAttemptStatusResponse;
import in.gov.sih.sih26135.entity.RefEmploymentVerificationAttemptStatus;
import in.gov.sih.sih26135.exception.BadRequestException;
import in.gov.sih.sih26135.exception.ResourceNotFoundException;
import in.gov.sih.sih26135.mapper.EmploymentVerificationAttemptStatusMapper;
import in.gov.sih.sih26135.repository.RefEmploymentVerificationAttemptStatusRepository;
import in.gov.sih.sih26135.service.EmploymentVerificationAttemptStatusReferenceService;
import java.util.List;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class EmploymentVerificationAttemptStatusReferenceServiceImpl
    implements EmploymentVerificationAttemptStatusReferenceService {

  private final RefEmploymentVerificationAttemptStatusRepository repository;
  private final EmploymentVerificationAttemptStatusMapper mapper;

  public EmploymentVerificationAttemptStatusReferenceServiceImpl(
      RefEmploymentVerificationAttemptStatusRepository repository,
      EmploymentVerificationAttemptStatusMapper mapper) {
    this.repository = repository;
    this.mapper = mapper;
  }

  @Override
  public List<EmploymentVerificationAttemptStatusResponse> getAllAttemptStatuses() {
    return repository.findAll(Sort.by(Sort.Direction.ASC, "sortOrder")).stream()
        .map(mapper::toResponse)
        .toList();
  }

  @Override
  public EmploymentVerificationAttemptStatusResponse getById(Long id) {
    if (id == null) {
      throw new BadRequestException("Attempt status ID is required");
    }
    RefEmploymentVerificationAttemptStatus entity = repository.findById(id)
        .orElseThrow(() -> new ResourceNotFoundException("RefEmploymentVerificationAttemptStatus", "id"));
    return mapper.toResponse(entity);
  }

  @Override
  public EmploymentVerificationAttemptStatusResponse getByCode(String statusCode) {
    if (statusCode == null || statusCode.isBlank()) {
      throw new BadRequestException("Status code is required");
    }
    RefEmploymentVerificationAttemptStatus entity = repository.findByStatusCode(statusCode.trim())
        .orElseThrow(() -> new ResourceNotFoundException("RefEmploymentVerificationAttemptStatus", "statusCode"));
    return mapper.toResponse(entity);
  }
}
