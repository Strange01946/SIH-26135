package in.gov.sih.sih26135.service;

import in.gov.sih.sih26135.dto.response.UnemploymentReasonResponse;
import java.util.List;

public interface UnemploymentReasonReferenceService {

  List<UnemploymentReasonResponse> getAllReasons();

  UnemploymentReasonResponse getById(Long id);

  UnemploymentReasonResponse getByCode(String reasonCode);
}
