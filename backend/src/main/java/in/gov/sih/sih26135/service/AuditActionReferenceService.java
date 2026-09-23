package in.gov.sih.sih26135.service;

import in.gov.sih.sih26135.dto.response.AuditActionResponse;
import java.util.List;

public interface AuditActionReferenceService {

  List<AuditActionResponse> getAllAuditActions();

  AuditActionResponse getById(Long id);

  AuditActionResponse getByCode(String actionCode);
}
