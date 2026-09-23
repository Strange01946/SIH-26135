package in.gov.sih.sih26135.service.impl;

import in.gov.sih.sih26135.dto.response.EmploymentVerificationRequestStatusResponse;
import in.gov.sih.sih26135.entity.RefEmploymentVerificationRequestStatus;
import in.gov.sih.sih26135.exception.BadRequestException;
import in.gov.sih.sih26135.exception.ResourceNotFoundException;
import in.gov.sih.sih26135.mapper.EmploymentVerificationRequestStatusMapper;
import in.gov.sih.sih26135.repository.RefEmploymentVerificationRequestStatusRepository;
import in.gov.sih.sih26135.service.EmploymentVerificationRequestStatusReferenceService;
import java.util.List;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class EmploymentVerificationRequestStatusReferenceServiceImpl
    implements EmploymentVerificationRequestStatusReferenceService {

  private final RefEmploymentVerificationRequestStatusRepository repository;
  private final EmploymentVerificationRequestStatusMapper mapper;

  public EmploymentVerificationRequestStatusReferenceServiceImpl(
      RefEmploymentVerificationRequestStatusRepository repository,
      EmploymentVerificationRequestStatusMapper mapper) {
    this.repository = repository;
    this.mapper = mapper;
  }

  @Override
  public List<EmploymentVerificationRequestStatusResponse> getAllStatuses() {
    return repository.findAll(Sort.by(Sort.Direction.ASC, "sortOrder")).stream()
        .map(mapper::toResponse)
        .toList();
  }

  @Override
  public EmploymentVerificationRequestStatusResponse getById(Long id) {
    if (id == null) {
      throw new BadRequestException("Request status ID is required");
    }
    RefEmploymentVerificationRequestStatus entity = repository.findById(id)
        .orElseThrow(() -> new ResourceNotFoundException("RefEmploymentVerificationRequestStatus", "id"));
    return mapper.toResponse(entity);
  }

  @Override
  public EmploymentVerificationRequestStatusResponse getByCode(String statusCode) {
    if (statusCode == null || statusCode.isBlank()) {
      throw new BadRequestException("Status code is required");
    }
    RefEmploymentVerificationRequestStatus entity = repository.findByStatusCode(statusCode.trim())
        .orElseThrow(() -> new ResourceNotFoundException("RefEmploymentVerificationRequestStatus", "statusCode"));
    return mapper.toResponse(entity);
  }
}
