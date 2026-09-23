package in.gov.sih.sih26135.service.impl;

import in.gov.sih.sih26135.dto.response.UnemploymentReasonResponse;
import in.gov.sih.sih26135.entity.RefUnemploymentReason;
import in.gov.sih.sih26135.exception.BadRequestException;
import in.gov.sih.sih26135.exception.ResourceNotFoundException;
import in.gov.sih.sih26135.mapper.UnemploymentReasonMapper;
import in.gov.sih.sih26135.repository.RefUnemploymentReasonRepository;
import in.gov.sih.sih26135.service.UnemploymentReasonReferenceService;
import java.util.List;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class UnemploymentReasonReferenceServiceImpl implements UnemploymentReasonReferenceService {

  private final RefUnemploymentReasonRepository repository;
  private final UnemploymentReasonMapper mapper;

  public UnemploymentReasonReferenceServiceImpl(
      RefUnemploymentReasonRepository repository,
      UnemploymentReasonMapper mapper) {
    this.repository = repository;
    this.mapper = mapper;
  }

  @Override
  public List<UnemploymentReasonResponse> getAllReasons() {
    return repository.findAll(Sort.by(Sort.Direction.ASC, "sortOrder")).stream()
        .map(mapper::toResponse)
        .toList();
  }

  @Override
  public UnemploymentReasonResponse getById(Long id) {
    if (id == null) {
      throw new BadRequestException("Unemployment reason ID is required");
    }
    RefUnemploymentReason entity = repository.findById(id)
        .orElseThrow(() -> new ResourceNotFoundException("RefUnemploymentReason", "id"));
    return mapper.toResponse(entity);
  }

  @Override
  public UnemploymentReasonResponse getByCode(String reasonCode) {
    if (reasonCode == null || reasonCode.isBlank()) {
      throw new BadRequestException("Reason code is required");
    }
    RefUnemploymentReason entity = repository.findByReasonCode(reasonCode.trim())
        .orElseThrow(() -> new ResourceNotFoundException("RefUnemploymentReason", "reasonCode"));
    return mapper.toResponse(entity);
  }
}
