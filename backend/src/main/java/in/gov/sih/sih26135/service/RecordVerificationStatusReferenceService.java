package in.gov.sih.sih26135.service;

import in.gov.sih.sih26135.dto.response.RecordVerificationStatusResponse;
import java.util.List;

public interface RecordVerificationStatusReferenceService {

  List<RecordVerificationStatusResponse> getAllRecordVerificationStatuses();

  RecordVerificationStatusResponse getById(Long id);

  RecordVerificationStatusResponse getByCode(String statusCode);

  List<RecordVerificationStatusResponse> getByIsVerifiedFlag(Boolean isVerifiedFlag);
}
