package in.gov.sih.sih26135.service;

import in.gov.sih.sih26135.dto.response.OfferStatusResponse;
import java.util.List;

public interface OfferStatusReferenceService {

  List<OfferStatusResponse> getAllOfferStatuses();

  OfferStatusResponse getById(Long id);

  OfferStatusResponse getByCode(String statusCode);
}
