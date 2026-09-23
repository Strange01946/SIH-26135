package in.gov.sih.sih26135.service.impl;

import in.gov.sih.sih26135.dto.response.CommunicationPurposeResponse;
import in.gov.sih.sih26135.entity.RefCommunicationPurpose;
import in.gov.sih.sih26135.exception.BadRequestException;
import in.gov.sih.sih26135.exception.ResourceNotFoundException;
import in.gov.sih.sih26135.mapper.CommunicationPurposeMapper;
import in.gov.sih.sih26135.repository.RefCommunicationPurposeRepository;
import in.gov.sih.sih26135.service.CommunicationPurposeReferenceService;
import java.util.List;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class CommunicationPurposeReferenceServiceImpl implements CommunicationPurposeReferenceService {

  private final RefCommunicationPurposeRepository refCommunicationPurposeRepository;
  private final CommunicationPurposeMapper communicationPurposeMapper;

  public CommunicationPurposeReferenceServiceImpl(
      RefCommunicationPurposeRepository refCommunicationPurposeRepository,
      CommunicationPurposeMapper communicationPurposeMapper) {
    this.refCommunicationPurposeRepository = refCommunicationPurposeRepository;
    this.communicationPurposeMapper = communicationPurposeMapper;
  }

  @Override
  public List<CommunicationPurposeResponse> getAllPurposes() {
    return refCommunicationPurposeRepository.findAll(Sort.by(Sort.Direction.ASC, "sortOrder")).stream()
        .map(communicationPurposeMapper::toResponse)
        .toList();
  }

  @Override
  public CommunicationPurposeResponse getById(Long id) {
    if (id == null) {
      throw new BadRequestException("Communication purpose ID is required");
    }
    RefCommunicationPurpose entity = refCommunicationPurposeRepository.findById(id)
        .orElseThrow(() -> new ResourceNotFoundException("RefCommunicationPurpose", "id"));
    return communicationPurposeMapper.toResponse(entity);
  }

  @Override
  public CommunicationPurposeResponse getByCode(String purposeCode) {
    if (purposeCode == null || purposeCode.isBlank()) {
      throw new BadRequestException("Purpose code is required");
    }
    RefCommunicationPurpose entity = refCommunicationPurposeRepository.findByPurposeCode(purposeCode.trim())
        .orElseThrow(() -> new ResourceNotFoundException("RefCommunicationPurpose", "purposeCode"));
    return communicationPurposeMapper.toResponse(entity);
  }

  @Override
  public List<CommunicationPurposeResponse> getByRequiredConsentType(Long consentTypeId) {
    if (consentTypeId == null) {
      throw new BadRequestException("Consent type ID is required");
    }
    return refCommunicationPurposeRepository.findByRequiredConsentTypeId(consentTypeId).stream()
        .map(communicationPurposeMapper::toResponse)
        .toList();
  }
}
