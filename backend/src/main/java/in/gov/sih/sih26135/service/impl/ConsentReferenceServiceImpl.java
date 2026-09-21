package in.gov.sih.sih26135.service.impl;

import in.gov.sih.sih26135.dto.response.ConsentStatusResponse;
import in.gov.sih.sih26135.dto.response.ConsentTypeResponse;
import in.gov.sih.sih26135.entity.RefConsentStatus;
import in.gov.sih.sih26135.entity.RefConsentType;
import in.gov.sih.sih26135.exception.ResourceNotFoundException;
import in.gov.sih.sih26135.mapper.ConsentReferenceMapper;
import in.gov.sih.sih26135.repository.RefConsentStatusRepository;
import in.gov.sih.sih26135.repository.RefConsentTypeRepository;
import in.gov.sih.sih26135.service.ConsentReferenceService;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class ConsentReferenceServiceImpl implements ConsentReferenceService {

  private final RefConsentTypeRepository refConsentTypeRepository;
  private final RefConsentStatusRepository refConsentStatusRepository;
  private final ConsentReferenceMapper consentReferenceMapper;

  public ConsentReferenceServiceImpl(
      RefConsentTypeRepository refConsentTypeRepository,
      RefConsentStatusRepository refConsentStatusRepository,
      ConsentReferenceMapper consentReferenceMapper) {
    this.refConsentTypeRepository = refConsentTypeRepository;
    this.refConsentStatusRepository = refConsentStatusRepository;
    this.consentReferenceMapper = consentReferenceMapper;
  }

  @Override
  public List<ConsentTypeResponse> getAllConsentTypes() {
    return refConsentTypeRepository.findAll().stream()
        .map(consentReferenceMapper::toTypeResponse)
        .toList();
  }

  @Override
  public ConsentTypeResponse getConsentTypeById(Long id) {
    RefConsentType consentType = refConsentTypeRepository.findById(id)
        .orElseThrow(() -> new ResourceNotFoundException("ConsentType", "id"));
    return consentReferenceMapper.toTypeResponse(consentType);
  }

  @Override
  public ConsentTypeResponse getConsentTypeByCode(String consentCode) {
    RefConsentType consentType = refConsentTypeRepository.findByConsentCode(consentCode)
        .orElseThrow(() -> new ResourceNotFoundException("ConsentType", "consentCode"));
    return consentReferenceMapper.toTypeResponse(consentType);
  }

  @Override
  public List<ConsentStatusResponse> getAllConsentStatuses() {
    return refConsentStatusRepository.findAll().stream()
        .map(consentReferenceMapper::toStatusResponse)
        .toList();
  }

  @Override
  public ConsentStatusResponse getConsentStatusById(Long id) {
    RefConsentStatus consentStatus = refConsentStatusRepository.findById(id)
        .orElseThrow(() -> new ResourceNotFoundException("ConsentStatus", "id"));
    return consentReferenceMapper.toStatusResponse(consentStatus);
  }

  @Override
  public ConsentStatusResponse getConsentStatusByCode(String statusCode) {
    RefConsentStatus consentStatus = refConsentStatusRepository.findByStatusCode(statusCode)
        .orElseThrow(() -> new ResourceNotFoundException("ConsentStatus", "statusCode"));
    return consentReferenceMapper.toStatusResponse(consentStatus);
  }
}
