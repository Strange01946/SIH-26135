package in.gov.sih.sih26135.service.impl;

import in.gov.sih.sih26135.dto.response.CertificateVerificationStatusResponse;
import in.gov.sih.sih26135.entity.RefCertificateVerificationStatus;
import in.gov.sih.sih26135.exception.BadRequestException;
import in.gov.sih.sih26135.exception.ResourceNotFoundException;
import in.gov.sih.sih26135.mapper.CertificateVerificationStatusMapper;
import in.gov.sih.sih26135.repository.RefCertificateVerificationStatusRepository;
import in.gov.sih.sih26135.service.CertificateVerificationStatusReferenceService;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class CertificateVerificationStatusReferenceServiceImpl implements CertificateVerificationStatusReferenceService {

  private final RefCertificateVerificationStatusRepository refCertificateVerificationStatusRepository;
  private final CertificateVerificationStatusMapper certificateVerificationStatusMapper;

  public CertificateVerificationStatusReferenceServiceImpl(
      RefCertificateVerificationStatusRepository refCertificateVerificationStatusRepository,
      CertificateVerificationStatusMapper certificateVerificationStatusMapper) {
    this.refCertificateVerificationStatusRepository = refCertificateVerificationStatusRepository;
    this.certificateVerificationStatusMapper = certificateVerificationStatusMapper;
  }

  @Override
  public List<CertificateVerificationStatusResponse> getAllCertificateVerificationStatuses() {
    return refCertificateVerificationStatusRepository.findAllByOrderBySortOrderAsc().stream()
        .map(certificateVerificationStatusMapper::toResponse)
        .toList();
  }

  @Override
  public CertificateVerificationStatusResponse getById(Long id) {
    if (id == null) {
      throw new BadRequestException("Certificate verification status ID is required");
    }
    RefCertificateVerificationStatus status = refCertificateVerificationStatusRepository.findById(id)
        .orElseThrow(() -> new ResourceNotFoundException("RefCertificateVerificationStatus", "id"));
    return certificateVerificationStatusMapper.toResponse(status);
  }

  @Override
  public CertificateVerificationStatusResponse getByCode(String statusCode) {
    if (statusCode == null || statusCode.isBlank()) {
      throw new BadRequestException("Certificate verification status code is required");
    }
    RefCertificateVerificationStatus status = refCertificateVerificationStatusRepository.findByStatusCode(statusCode.trim())
        .orElseThrow(() -> new ResourceNotFoundException("RefCertificateVerificationStatus", "statusCode"));
    return certificateVerificationStatusMapper.toResponse(status);
  }
}
