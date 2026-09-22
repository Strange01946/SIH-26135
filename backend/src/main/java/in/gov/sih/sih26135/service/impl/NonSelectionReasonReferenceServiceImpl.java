package in.gov.sih.sih26135.service.impl;

import in.gov.sih.sih26135.dto.response.NonSelectionReasonResponse;
import in.gov.sih.sih26135.entity.RefNonSelectionReason;
import in.gov.sih.sih26135.exception.BadRequestException;
import in.gov.sih.sih26135.exception.ResourceNotFoundException;
import in.gov.sih.sih26135.mapper.NonSelectionReasonMapper;
import in.gov.sih.sih26135.repository.RefNonSelectionReasonRepository;
import in.gov.sih.sih26135.service.NonSelectionReasonReferenceService;
import java.util.List;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class NonSelectionReasonReferenceServiceImpl implements NonSelectionReasonReferenceService {

  private final RefNonSelectionReasonRepository refNonSelectionReasonRepository;
  private final NonSelectionReasonMapper nonSelectionReasonMapper;

  public NonSelectionReasonReferenceServiceImpl(
      RefNonSelectionReasonRepository refNonSelectionReasonRepository,
      NonSelectionReasonMapper nonSelectionReasonMapper) {
    this.refNonSelectionReasonRepository = refNonSelectionReasonRepository;
    this.nonSelectionReasonMapper = nonSelectionReasonMapper;
  }

  @Override
  public List<NonSelectionReasonResponse> getAllNonSelectionReasons() {
    return refNonSelectionReasonRepository.findAll(Sort.by(Sort.Direction.ASC, "sortOrder")).stream()
        .map(nonSelectionReasonMapper::toResponse)
        .toList();
  }

  @Override
  public NonSelectionReasonResponse getById(Long id) {
    if (id == null) {
      throw new BadRequestException("Non selection reason ID is required");
    }
    RefNonSelectionReason entity = refNonSelectionReasonRepository.findById(id)
        .orElseThrow(() -> new ResourceNotFoundException("RefNonSelectionReason", "id"));
    return nonSelectionReasonMapper.toResponse(entity);
  }

  @Override
  public NonSelectionReasonResponse getByCode(String reasonCode) {
    if (reasonCode == null || reasonCode.isBlank()) {
      throw new BadRequestException("Reason code is required");
    }
    RefNonSelectionReason entity = refNonSelectionReasonRepository.findByReasonCode(reasonCode.trim())
        .orElseThrow(() -> new ResourceNotFoundException("RefNonSelectionReason", "reasonCode"));
    return nonSelectionReasonMapper.toResponse(entity);
  }
}
