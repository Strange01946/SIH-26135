package in.gov.sih.sih26135.service.impl;

import in.gov.sih.sih26135.dto.response.CommunicationStatusResponse;
import in.gov.sih.sih26135.entity.RefCommunicationStatus;
import in.gov.sih.sih26135.exception.BadRequestException;
import in.gov.sih.sih26135.exception.ResourceNotFoundException;
import in.gov.sih.sih26135.mapper.CommunicationStatusMapper;
import in.gov.sih.sih26135.repository.RefCommunicationStatusRepository;
import in.gov.sih.sih26135.service.CommunicationStatusReferenceService;
import java.util.List;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class CommunicationStatusReferenceServiceImpl implements CommunicationStatusReferenceService {

  private final RefCommunicationStatusRepository refCommunicationStatusRepository;
  private final CommunicationStatusMapper communicationStatusMapper;

  public CommunicationStatusReferenceServiceImpl(
      RefCommunicationStatusRepository refCommunicationStatusRepository,
      CommunicationStatusMapper communicationStatusMapper) {
    this.refCommunicationStatusRepository = refCommunicationStatusRepository;
    this.communicationStatusMapper = communicationStatusMapper;
  }

  @Override
  public List<CommunicationStatusResponse> getAllStatuses() {
    return refCommunicationStatusRepository.findAll(Sort.by(Sort.Direction.ASC, "sortOrder")).stream()
        .map(communicationStatusMapper::toResponse)
        .toList();
  }

  @Override
  public CommunicationStatusResponse getById(Long id) {
    if (id == null) {
      throw new BadRequestException("Communication status ID is required");
    }
    RefCommunicationStatus entity = refCommunicationStatusRepository.findById(id)
        .orElseThrow(() -> new ResourceNotFoundException("RefCommunicationStatus", "id"));
    return communicationStatusMapper.toResponse(entity);
  }

  @Override
  public CommunicationStatusResponse getByCode(String statusCode) {
    if (statusCode == null || statusCode.isBlank()) {
      throw new BadRequestException("Status code is required");
    }
    RefCommunicationStatus entity = refCommunicationStatusRepository.findByStatusCode(statusCode.trim())
        .orElseThrow(() -> new ResourceNotFoundException("RefCommunicationStatus", "statusCode"));
    return communicationStatusMapper.toResponse(entity);
  }

  @Override
  public List<CommunicationStatusResponse> getSuccessStatuses() {
    return refCommunicationStatusRepository.findByIsSuccessFlag(true).stream()
        .map(communicationStatusMapper::toResponse)
        .toList();
  }

  @Override
  public List<CommunicationStatusResponse> getFailureStatuses() {
    return refCommunicationStatusRepository.findByIsFailureFlag(true).stream()
        .map(communicationStatusMapper::toResponse)
        .toList();
  }
}
