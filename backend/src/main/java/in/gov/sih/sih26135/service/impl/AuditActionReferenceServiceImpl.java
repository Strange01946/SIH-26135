package in.gov.sih.sih26135.service.impl;

import in.gov.sih.sih26135.dto.response.AuditActionResponse;
import in.gov.sih.sih26135.entity.RefAuditAction;
import in.gov.sih.sih26135.exception.BadRequestException;
import in.gov.sih.sih26135.exception.ResourceNotFoundException;
import in.gov.sih.sih26135.mapper.AuditActionMapper;
import in.gov.sih.sih26135.repository.RefAuditActionRepository;
import in.gov.sih.sih26135.service.AuditActionReferenceService;
import java.util.List;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class AuditActionReferenceServiceImpl implements AuditActionReferenceService {

  private final RefAuditActionRepository repository;
  private final AuditActionMapper mapper;

  public AuditActionReferenceServiceImpl(
      RefAuditActionRepository repository,
      AuditActionMapper mapper) {
    this.repository = repository;
    this.mapper = mapper;
  }

  @Override
  public List<AuditActionResponse> getAllAuditActions() {
    return repository.findAll(Sort.by(Sort.Direction.ASC, "sortOrder")).stream()
        .map(mapper::toResponse)
        .toList();
  }

  @Override
  public AuditActionResponse getById(Long id) {
    if (id == null) {
      throw new BadRequestException("Audit action ID is required");
    }
    RefAuditAction entity = repository.findById(id)
        .orElseThrow(() -> new ResourceNotFoundException("RefAuditAction", "id"));
    return mapper.toResponse(entity);
  }

  @Override
  public AuditActionResponse getByCode(String actionCode) {
    if (actionCode == null || actionCode.isBlank()) {
      throw new BadRequestException("Action code is required");
    }
    RefAuditAction entity = repository.findByActionCode(actionCode.trim())
        .orElseThrow(() -> new ResourceNotFoundException("RefAuditAction", "actionCode"));
    return mapper.toResponse(entity);
  }
}
