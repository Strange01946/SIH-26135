package in.gov.sih.sih26135.service;

import in.gov.sih.sih26135.dto.response.PlacementStatusResponse;
import java.util.List;

public interface PlacementStatusReferenceService {

  List<PlacementStatusResponse> getAllPlacementStatuses();

  PlacementStatusResponse getById(Long id);

  PlacementStatusResponse getByCode(String statusCode);

  List<PlacementStatusResponse> getByIsOfferFlag(Boolean isOfferFlag);

  List<PlacementStatusResponse> getByIsJoinedFlag(Boolean isJoinedFlag);

  List<PlacementStatusResponse> getByIsUnsuccessfulFlag(Boolean isUnsuccessfulFlag);
}
