package in.gov.sih.sih26135.service.impl;

import in.gov.sih.sih26135.dto.response.CertificateStatusResponse;
import in.gov.sih.sih26135.entity.RefCertificateStatus;
import in.gov.sih.sih26135.exception.BadRequestException;
import in.gov.sih.sih26135.exception.ResourceNotFoundException;
import in.gov.sih.sih26135.mapper.CertificateStatusMapper;
import in.gov.sih.sih26135.repository.RefCertificateStatusRepository;
import in.gov.sih.sih26135.service.CertificateStatusReferenceService;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class CertificateStatusReferenceServiceImpl implements CertificateStatusReferenceService {

  private final RefCertificateStatusRepository refCertificateStatusRepository;
  private final CertificateStatusMapper certificateStatusMapper;

  public CertificateStatusReferenceServiceImpl(
      RefCertificateStatusRepository refCertificateStatusRepository,
      CertificateStatusMapper certificateStatusMapper) {
    this.refCertificateStatusRepository = refCertificateStatusRepository;
    this.certificateStatusMapper = certificateStatusMapper;
  }

  @Override
  public List<CertificateStatusResponse> getAllCertificateStatuses() {
    return refCertificateStatusRepository.findAllByOrderBySortOrderAsc().stream()
        .map(certificateStatusMapper::toResponse)
        .toList();
  }

  @Override
  public CertificateStatusResponse getById(Long id) {
    if (id == null) {
      throw new BadRequestException("Certificate status ID is required");
    }
    RefCertificateStatus status = refCertificateStatusRepository.findById(id)
        .orElseThrow(() -> new ResourceNotFoundException("RefCertificateStatus", "id"));
    return certificateStatusMapper.toResponse(status);
  }

  @Override
  public CertificateStatusResponse getByCode(String statusCode) {
    if (statusCode == null || statusCode.isBlank()) {
      throw new BadRequestException("Certificate status code is required");
    }
    RefCertificateStatus status = refCertificateStatusRepository.findByStatusCode(statusCode.trim())
        .orElseThrow(() -> new ResourceNotFoundException("RefCertificateStatus", "statusCode"));
    return certificateStatusMapper.toResponse(status);
  }
}
