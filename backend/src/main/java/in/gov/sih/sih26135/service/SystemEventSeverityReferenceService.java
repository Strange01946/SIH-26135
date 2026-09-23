package in.gov.sih.sih26135.service;

import in.gov.sih.sih26135.dto.response.SystemEventSeverityResponse;
import java.util.List;

public interface SystemEventSeverityReferenceService {

  List<SystemEventSeverityResponse> getAllSeverities();

  SystemEventSeverityResponse getById(Long id);

  SystemEventSeverityResponse getByCode(String severityCode);
}
