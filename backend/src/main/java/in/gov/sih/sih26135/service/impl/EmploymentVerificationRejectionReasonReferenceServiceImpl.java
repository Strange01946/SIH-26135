package in.gov.sih.sih26135.service.impl;

import in.gov.sih.sih26135.dto.response.EmploymentVerificationRejectionReasonResponse;
import in.gov.sih.sih26135.entity.RefEmploymentVerificationRejectionReason;
import in.gov.sih.sih26135.exception.BadRequestException;
import in.gov.sih.sih26135.exception.ResourceNotFoundException;
import in.gov.sih.sih26135.mapper.EmploymentVerificationRejectionReasonMapper;
import in.gov.sih.sih26135.repository.RefEmploymentVerificationRejectionReasonRepository;
import in.gov.sih.sih26135.service.EmploymentVerificationRejectionReasonReferenceService;
import java.util.List;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class EmploymentVerificationRejectionReasonReferenceServiceImpl
    implements EmploymentVerificationRejectionReasonReferenceService {

  private final RefEmploymentVerificationRejectionReasonRepository repository;
  private final EmploymentVerificationRejectionReasonMapper mapper;

  public EmploymentVerificationRejectionReasonReferenceServiceImpl(
      RefEmploymentVerificationRejectionReasonRepository repository,
      EmploymentVerificationRejectionReasonMapper mapper) {
    this.repository = repository;
    this.mapper = mapper;
  }

  @Override
  public List<EmploymentVerificationRejectionReasonResponse> getAllRejectionReasons() {
    return repository.findAll(Sort.by(Sort.Direction.ASC, "sortOrder")).stream()
        .map(mapper::toResponse)
        .toList();
  }

  @Override
  public EmploymentVerificationRejectionReasonResponse getById(Long id) {
    if (id == null) {
      throw new BadRequestException("Rejection reason ID is required");
    }
    RefEmploymentVerificationRejectionReason entity = repository.findById(id)
        .orElseThrow(() -> new ResourceNotFoundException("RefEmploymentVerificationRejectionReason", "id"));
    return mapper.toResponse(entity);
  }

  @Override
  public EmploymentVerificationRejectionReasonResponse getByCode(String reasonCode) {
    if (reasonCode == null || reasonCode.isBlank()) {
      throw new BadRequestException("Reason code is required");
    }
    RefEmploymentVerificationRejectionReason entity = repository.findByReasonCode(reasonCode.trim())
        .orElseThrow(() -> new ResourceNotFoundException("RefEmploymentVerificationRejectionReason", "reasonCode"));
    return mapper.toResponse(entity);
  }
}
