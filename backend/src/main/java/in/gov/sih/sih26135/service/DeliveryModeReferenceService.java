package in.gov.sih.sih26135.service;

import in.gov.sih.sih26135.dto.response.DeliveryModeResponse;
import java.util.List;

public interface DeliveryModeReferenceService {

  List<DeliveryModeResponse> getAllDeliveryModes();

  DeliveryModeResponse getById(Long id);

  DeliveryModeResponse getByCode(String modeCode);
}
