package in.gov.sih.sih26135.service;

import in.gov.sih.sih26135.dto.response.CommunicationDirectionResponse;
import java.util.List;

public interface CommunicationDirectionReferenceService {

  List<CommunicationDirectionResponse> getAllDirections();

  CommunicationDirectionResponse getById(Long id);

  CommunicationDirectionResponse getByCode(String directionCode);
}
