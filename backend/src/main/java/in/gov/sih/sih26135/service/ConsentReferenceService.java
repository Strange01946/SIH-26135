package in.gov.sih.sih26135.service;

import in.gov.sih.sih26135.dto.response.ConsentStatusResponse;
import in.gov.sih.sih26135.dto.response.ConsentTypeResponse;
import java.util.List;

public interface ConsentReferenceService {

  List<ConsentTypeResponse> getAllConsentTypes();

  ConsentTypeResponse getConsentTypeById(Long id);

  ConsentTypeResponse getConsentTypeByCode(String consentCode);

  List<ConsentStatusResponse> getAllConsentStatuses();

  ConsentStatusResponse getConsentStatusById(Long id);

  ConsentStatusResponse getConsentStatusByCode(String statusCode);
}
