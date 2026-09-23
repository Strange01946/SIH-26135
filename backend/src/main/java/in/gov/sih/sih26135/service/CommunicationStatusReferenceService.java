package in.gov.sih.sih26135.service;

import in.gov.sih.sih26135.dto.response.CommunicationStatusResponse;
import java.util.List;

public interface CommunicationStatusReferenceService {

  List<CommunicationStatusResponse> getAllStatuses();

  CommunicationStatusResponse getById(Long id);

  CommunicationStatusResponse getByCode(String statusCode);

  List<CommunicationStatusResponse> getSuccessStatuses();

  List<CommunicationStatusResponse> getFailureStatuses();
}
