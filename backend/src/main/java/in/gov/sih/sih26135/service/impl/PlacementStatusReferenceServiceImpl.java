package in.gov.sih.sih26135.service.impl;

import in.gov.sih.sih26135.dto.response.PlacementStatusResponse;
import in.gov.sih.sih26135.entity.RefPlacementStatus;
import in.gov.sih.sih26135.exception.BadRequestException;
import in.gov.sih.sih26135.exception.ResourceNotFoundException;
import in.gov.sih.sih26135.mapper.PlacementStatusMapper;
import in.gov.sih.sih26135.repository.RefPlacementStatusRepository;
import in.gov.sih.sih26135.service.PlacementStatusReferenceService;
import java.util.List;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class PlacementStatusReferenceServiceImpl implements PlacementStatusReferenceService {

  private final RefPlacementStatusRepository refPlacementStatusRepository;
  private final PlacementStatusMapper placementStatusMapper;

  public PlacementStatusReferenceServiceImpl(
      RefPlacementStatusRepository refPlacementStatusRepository,
      PlacementStatusMapper placementStatusMapper) {
    this.refPlacementStatusRepository = refPlacementStatusRepository;
    this.placementStatusMapper = placementStatusMapper;
  }

  @Override
  public List<PlacementStatusResponse> getAllPlacementStatuses() {
    return refPlacementStatusRepository.findAll(Sort.by(Sort.Direction.ASC, "sortOrder")).stream()
        .map(placementStatusMapper::toResponse)
        .toList();
  }

  @Override
  public PlacementStatusResponse getById(Long id) {
    if (id == null) {
      throw new BadRequestException("Placement status ID is required");
    }
    RefPlacementStatus entity = refPlacementStatusRepository.findById(id)
        .orElseThrow(() -> new ResourceNotFoundException("RefPlacementStatus", "id"));
    return placementStatusMapper.toResponse(entity);
  }

  @Override
  public PlacementStatusResponse getByCode(String statusCode) {
    if (statusCode == null || statusCode.isBlank()) {
      throw new BadRequestException("Status code is required");
    }
    RefPlacementStatus entity = refPlacementStatusRepository.findByStatusCode(statusCode.trim())
        .orElseThrow(() -> new ResourceNotFoundException("RefPlacementStatus", "statusCode"));
    return placementStatusMapper.toResponse(entity);
  }

  @Override
  public List<PlacementStatusResponse> getByIsOfferFlag(Boolean isOfferFlag) {
    if (isOfferFlag == null) {
      throw new BadRequestException("isOfferFlag is required");
    }
    return refPlacementStatusRepository.findByIsOfferFlag(isOfferFlag).stream()
        .map(placementStatusMapper::toResponse)
        .toList();
  }

  @Override
  public List<PlacementStatusResponse> getByIsJoinedFlag(Boolean isJoinedFlag) {
    if (isJoinedFlag == null) {
      throw new BadRequestException("isJoinedFlag is required");
    }
    return refPlacementStatusRepository.findByIsJoinedFlag(isJoinedFlag).stream()
        .map(placementStatusMapper::toResponse)
        .toList();
  }

  @Override
  public List<PlacementStatusResponse> getByIsUnsuccessfulFlag(Boolean isUnsuccessfulFlag) {
    if (isUnsuccessfulFlag == null) {
      throw new BadRequestException("isUnsuccessfulFlag is required");
    }
    return refPlacementStatusRepository.findByIsUnsuccessfulFlag(isUnsuccessfulFlag).stream()
        .map(placementStatusMapper::toResponse)
        .toList();
  }
}
