package in.gov.sih.sih26135.service;

import in.gov.sih.sih26135.dto.response.CertificateVerificationStatusResponse;
import java.util.List;

public interface CertificateVerificationStatusReferenceService {

  List<CertificateVerificationStatusResponse> getAllCertificateVerificationStatuses();

  CertificateVerificationStatusResponse getById(Long id);

  CertificateVerificationStatusResponse getByCode(String statusCode);
}
