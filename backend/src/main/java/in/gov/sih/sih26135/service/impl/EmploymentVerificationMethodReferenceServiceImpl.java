package in.gov.sih.sih26135.service.impl;

import in.gov.sih.sih26135.dto.response.EmploymentVerificationMethodResponse;
import in.gov.sih.sih26135.entity.RefEmploymentVerificationMethod;
import in.gov.sih.sih26135.exception.BadRequestException;
import in.gov.sih.sih26135.exception.ResourceNotFoundException;
import in.gov.sih.sih26135.mapper.EmploymentVerificationMethodMapper;
import in.gov.sih.sih26135.repository.RefEmploymentVerificationMethodRepository;
import in.gov.sih.sih26135.service.EmploymentVerificationMethodReferenceService;
import java.util.List;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class EmploymentVerificationMethodReferenceServiceImpl
    implements EmploymentVerificationMethodReferenceService {

  private final RefEmploymentVerificationMethodRepository repository;
  private final EmploymentVerificationMethodMapper mapper;

  public EmploymentVerificationMethodReferenceServiceImpl(
      RefEmploymentVerificationMethodRepository repository,
      EmploymentVerificationMethodMapper mapper) {
    this.repository = repository;
    this.mapper = mapper;
  }

  @Override
  public List<EmploymentVerificationMethodResponse> getAllMethods() {
    return repository.findAll(Sort.by(Sort.Direction.ASC, "sortOrder")).stream()
        .map(mapper::toResponse)
        .toList();
  }

  @Override
  public EmploymentVerificationMethodResponse getById(Long id) {
    if (id == null) {
      throw new BadRequestException("Verification method ID is required");
    }
    RefEmploymentVerificationMethod entity = repository.findById(id)
        .orElseThrow(() -> new ResourceNotFoundException("RefEmploymentVerificationMethod", "id"));
    return mapper.toResponse(entity);
  }

  @Override
  public EmploymentVerificationMethodResponse getByCode(String methodCode) {
    if (methodCode == null || methodCode.isBlank()) {
      throw new BadRequestException("Method code is required");
    }
    RefEmploymentVerificationMethod entity = repository.findByMethodCode(methodCode.trim())
        .orElseThrow(() -> new ResourceNotFoundException("RefEmploymentVerificationMethod", "methodCode"));
    return mapper.toResponse(entity);
  }
}
