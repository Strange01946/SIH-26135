package in.gov.sih.sih26135.service;

import in.gov.sih.sih26135.dto.response.CommunicationPurposeResponse;
import java.util.List;

public interface CommunicationPurposeReferenceService {

  List<CommunicationPurposeResponse> getAllPurposes();

  CommunicationPurposeResponse getById(Long id);

  CommunicationPurposeResponse getByCode(String purposeCode);

  List<CommunicationPurposeResponse> getByRequiredConsentType(Long consentTypeId);
}
