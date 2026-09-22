package in.gov.sih.sih26135.service.impl;

import in.gov.sih.sih26135.dto.response.OfferStatusResponse;
import in.gov.sih.sih26135.entity.RefOfferStatus;
import in.gov.sih.sih26135.exception.BadRequestException;
import in.gov.sih.sih26135.exception.ResourceNotFoundException;
import in.gov.sih.sih26135.mapper.OfferStatusMapper;
import in.gov.sih.sih26135.repository.RefOfferStatusRepository;
import in.gov.sih.sih26135.service.OfferStatusReferenceService;
import java.util.List;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class OfferStatusReferenceServiceImpl implements OfferStatusReferenceService {

  private final RefOfferStatusRepository refOfferStatusRepository;
  private final OfferStatusMapper offerStatusMapper;

  public OfferStatusReferenceServiceImpl(
      RefOfferStatusRepository refOfferStatusRepository,
      OfferStatusMapper offerStatusMapper) {
    this.refOfferStatusRepository = refOfferStatusRepository;
    this.offerStatusMapper = offerStatusMapper;
  }

  @Override
  public List<OfferStatusResponse> getAllOfferStatuses() {
    return refOfferStatusRepository.findAll(Sort.by(Sort.Direction.ASC, "sortOrder")).stream()
        .map(offerStatusMapper::toResponse)
        .toList();
  }

  @Override
  public OfferStatusResponse getById(Long id) {
    if (id == null) {
      throw new BadRequestException("Offer status ID is required");
    }
    RefOfferStatus entity = refOfferStatusRepository.findById(id)
        .orElseThrow(() -> new ResourceNotFoundException("RefOfferStatus", "id"));
    return offerStatusMapper.toResponse(entity);
  }

  @Override
  public OfferStatusResponse getByCode(String statusCode) {
    if (statusCode == null || statusCode.isBlank()) {
      throw new BadRequestException("Status code is required");
    }
    RefOfferStatus entity = refOfferStatusRepository.findByStatusCode(statusCode.trim())
        .orElseThrow(() -> new ResourceNotFoundException("RefOfferStatus", "statusCode"));
    return offerStatusMapper.toResponse(entity);
  }
}
