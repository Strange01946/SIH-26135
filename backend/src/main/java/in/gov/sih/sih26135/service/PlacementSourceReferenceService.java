package in.gov.sih.sih26135.service;

import in.gov.sih.sih26135.dto.response.PlacementSourceResponse;
import java.util.List;

public interface PlacementSourceReferenceService {

  List<PlacementSourceResponse> getAllPlacementSources();

  PlacementSourceResponse getById(Long id);

  PlacementSourceResponse getByCode(String sourceCode);
}
