package in.gov.sih.sih26135.service.impl;

import in.gov.sih.sih26135.dto.response.EmploymentVerificationEvidenceTypeResponse;
import in.gov.sih.sih26135.entity.RefEmploymentVerificationEvidenceType;
import in.gov.sih.sih26135.exception.BadRequestException;
import in.gov.sih.sih26135.exception.ResourceNotFoundException;
import in.gov.sih.sih26135.mapper.EmploymentVerificationEvidenceTypeMapper;
import in.gov.sih.sih26135.repository.RefEmploymentVerificationEvidenceTypeRepository;
import in.gov.sih.sih26135.service.EmploymentVerificationEvidenceTypeReferenceService;
import java.util.List;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class EmploymentVerificationEvidenceTypeReferenceServiceImpl
    implements EmploymentVerificationEvidenceTypeReferenceService {

  private final RefEmploymentVerificationEvidenceTypeRepository repository;
  private final EmploymentVerificationEvidenceTypeMapper mapper;

  public EmploymentVerificationEvidenceTypeReferenceServiceImpl(
      RefEmploymentVerificationEvidenceTypeRepository repository,
      EmploymentVerificationEvidenceTypeMapper mapper) {
    this.repository = repository;
    this.mapper = mapper;
  }

  @Override
  public List<EmploymentVerificationEvidenceTypeResponse> getAllEvidenceTypes() {
    return repository.findAll(Sort.by(Sort.Direction.ASC, "sortOrder")).stream()
        .map(mapper::toResponse)
        .toList();
  }

  @Override
  public EmploymentVerificationEvidenceTypeResponse getById(Long id) {
    if (id == null) {
      throw new BadRequestException("Evidence type ID is required");
    }
    RefEmploymentVerificationEvidenceType entity = repository.findById(id)
        .orElseThrow(() -> new ResourceNotFoundException("RefEmploymentVerificationEvidenceType", "id"));
    return mapper.toResponse(entity);
  }

  @Override
  public EmploymentVerificationEvidenceTypeResponse getByCode(String typeCode) {
    if (typeCode == null || typeCode.isBlank()) {
      throw new BadRequestException("Type code is required");
    }
    RefEmploymentVerificationEvidenceType entity = repository.findByTypeCode(typeCode.trim())
        .orElseThrow(() -> new ResourceNotFoundException("RefEmploymentVerificationEvidenceType", "typeCode"));
    return mapper.toResponse(entity);
  }
}
