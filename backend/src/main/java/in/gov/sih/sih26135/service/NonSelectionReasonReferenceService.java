package in.gov.sih.sih26135.service;

import in.gov.sih.sih26135.dto.response.NonSelectionReasonResponse;
import java.util.List;

public interface NonSelectionReasonReferenceService {

  List<NonSelectionReasonResponse> getAllNonSelectionReasons();

  NonSelectionReasonResponse getById(Long id);

  NonSelectionReasonResponse getByCode(String reasonCode);
}
