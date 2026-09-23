package in.gov.sih.sih26135.service;

import in.gov.sih.sih26135.dto.response.CommunicationChannelResponse;
import java.util.List;

public interface CommunicationChannelReferenceService {

  List<CommunicationChannelResponse> getAllChannels();

  CommunicationChannelResponse getById(Long id);

  CommunicationChannelResponse getByCode(String channelCode);

  List<CommunicationChannelResponse> getByRequiredConsentType(Long consentTypeId);
}
