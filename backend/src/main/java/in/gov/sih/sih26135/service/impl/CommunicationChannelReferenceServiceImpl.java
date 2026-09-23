package in.gov.sih.sih26135.service.impl;

import in.gov.sih.sih26135.dto.response.CommunicationChannelResponse;
import in.gov.sih.sih26135.entity.RefCommunicationChannel;
import in.gov.sih.sih26135.exception.BadRequestException;
import in.gov.sih.sih26135.exception.ResourceNotFoundException;
import in.gov.sih.sih26135.mapper.CommunicationChannelMapper;
import in.gov.sih.sih26135.repository.RefCommunicationChannelRepository;
import in.gov.sih.sih26135.service.CommunicationChannelReferenceService;
import java.util.List;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class CommunicationChannelReferenceServiceImpl implements CommunicationChannelReferenceService {

  private final RefCommunicationChannelRepository refCommunicationChannelRepository;
  private final CommunicationChannelMapper communicationChannelMapper;

  public CommunicationChannelReferenceServiceImpl(
      RefCommunicationChannelRepository refCommunicationChannelRepository,
      CommunicationChannelMapper communicationChannelMapper) {
    this.refCommunicationChannelRepository = refCommunicationChannelRepository;
    this.communicationChannelMapper = communicationChannelMapper;
  }

  @Override
  public List<CommunicationChannelResponse> getAllChannels() {
    return refCommunicationChannelRepository.findAll(Sort.by(Sort.Direction.ASC, "sortOrder")).stream()
        .map(communicationChannelMapper::toResponse)
        .toList();
  }

  @Override
  public CommunicationChannelResponse getById(Long id) {
    if (id == null) {
      throw new BadRequestException("Communication channel ID is required");
    }
    RefCommunicationChannel entity = refCommunicationChannelRepository.findById(id)
        .orElseThrow(() -> new ResourceNotFoundException("RefCommunicationChannel", "id"));
    return communicationChannelMapper.toResponse(entity);
  }

  @Override
  public CommunicationChannelResponse getByCode(String channelCode) {
    if (channelCode == null || channelCode.isBlank()) {
      throw new BadRequestException("Channel code is required");
    }
    RefCommunicationChannel entity = refCommunicationChannelRepository.findByChannelCode(channelCode.trim())
        .orElseThrow(() -> new ResourceNotFoundException("RefCommunicationChannel", "channelCode"));
    return communicationChannelMapper.toResponse(entity);
  }

  @Override
  public List<CommunicationChannelResponse> getByRequiredConsentType(Long consentTypeId) {
    if (consentTypeId == null) {
      throw new BadRequestException("Consent type ID is required");
    }
    return refCommunicationChannelRepository.findByRequiredConsentTypeId(consentTypeId).stream()
        .map(communicationChannelMapper::toResponse)
        .toList();
  }
}
