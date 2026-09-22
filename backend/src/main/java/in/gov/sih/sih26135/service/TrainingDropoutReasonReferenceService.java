package in.gov.sih.sih26135.service;

import in.gov.sih.sih26135.dto.response.TrainingDropoutReasonResponse;
import java.util.List;

public interface TrainingDropoutReasonReferenceService {

  List<TrainingDropoutReasonResponse> getAllDropoutReasons();

  TrainingDropoutReasonResponse getById(Long id);

  TrainingDropoutReasonResponse getByCode(String reasonCode);
}
