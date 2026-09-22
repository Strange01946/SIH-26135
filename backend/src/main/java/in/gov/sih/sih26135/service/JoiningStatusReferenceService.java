package in.gov.sih.sih26135.service;

import in.gov.sih.sih26135.dto.response.JoiningStatusResponse;
import java.util.List;

public interface JoiningStatusReferenceService {

  List<JoiningStatusResponse> getAllJoiningStatuses();

  JoiningStatusResponse getById(Long id);

  JoiningStatusResponse getByCode(String statusCode);

  List<JoiningStatusResponse> getByIsJoinedFlag(Boolean isJoinedFlag);
}
