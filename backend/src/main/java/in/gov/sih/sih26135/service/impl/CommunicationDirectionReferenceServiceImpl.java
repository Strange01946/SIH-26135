package in.gov.sih.sih26135.service.impl;

import in.gov.sih.sih26135.dto.response.CommunicationDirectionResponse;
import in.gov.sih.sih26135.entity.RefCommunicationDirection;
import in.gov.sih.sih26135.exception.BadRequestException;
import in.gov.sih.sih26135.exception.ResourceNotFoundException;
import in.gov.sih.sih26135.mapper.CommunicationDirectionMapper;
import in.gov.sih.sih26135.repository.RefCommunicationDirectionRepository;
import in.gov.sih.sih26135.service.CommunicationDirectionReferenceService;
import java.util.List;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class CommunicationDirectionReferenceServiceImpl implements CommunicationDirectionReferenceService {

  private final RefCommunicationDirectionRepository refCommunicationDirectionRepository;
  private final CommunicationDirectionMapper communicationDirectionMapper;

  public CommunicationDirectionReferenceServiceImpl(
      RefCommunicationDirectionRepository refCommunicationDirectionRepository,
      CommunicationDirectionMapper communicationDirectionMapper) {
    this.refCommunicationDirectionRepository = refCommunicationDirectionRepository;
    this.communicationDirectionMapper = communicationDirectionMapper;
  }

  @Override
  public List<CommunicationDirectionResponse> getAllDirections() {
    return refCommunicationDirectionRepository.findAll(Sort.by(Sort.Direction.ASC, "sortOrder")).stream()
        .map(communicationDirectionMapper::toResponse)
        .toList();
  }

  @Override
  public CommunicationDirectionResponse getById(Long id) {
    if (id == null) {
      throw new BadRequestException("Communication direction ID is required");
    }
    RefCommunicationDirection entity = refCommunicationDirectionRepository.findById(id)
        .orElseThrow(() -> new ResourceNotFoundException("RefCommunicationDirection", "id"));
    return communicationDirectionMapper.toResponse(entity);
  }

  @Override
  public CommunicationDirectionResponse getByCode(String directionCode) {
    if (directionCode == null || directionCode.isBlank()) {
      throw new BadRequestException("Direction code is required");
    }
    RefCommunicationDirection entity = refCommunicationDirectionRepository.findByDirectionCode(directionCode.trim())
        .orElseThrow(() -> new ResourceNotFoundException("RefCommunicationDirection", "directionCode"));
    return communicationDirectionMapper.toResponse(entity);
  }
}
