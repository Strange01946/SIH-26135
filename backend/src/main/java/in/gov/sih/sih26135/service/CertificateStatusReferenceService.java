package in.gov.sih.sih26135.service;

import in.gov.sih.sih26135.dto.response.CertificateStatusResponse;
import java.util.List;

public interface CertificateStatusReferenceService {

  List<CertificateStatusResponse> getAllCertificateStatuses();

  CertificateStatusResponse getById(Long id);

  CertificateStatusResponse getByCode(String statusCode);
}
