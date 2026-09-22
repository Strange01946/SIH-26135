package in.gov.sih.sih26135.service.impl;

import in.gov.sih.sih26135.dto.response.EnrollmentStatusResponse;
import in.gov.sih.sih26135.entity.RefEnrollmentStatus;
import in.gov.sih.sih26135.exception.BadRequestException;
import in.gov.sih.sih26135.exception.ResourceNotFoundException;
import in.gov.sih.sih26135.mapper.EnrollmentStatusMapper;
import in.gov.sih.sih26135.repository.RefEnrollmentStatusRepository;
import in.gov.sih.sih26135.service.EnrollmentStatusReferenceService;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class EnrollmentStatusReferenceServiceImpl implements EnrollmentStatusReferenceService {

  private final RefEnrollmentStatusRepository refEnrollmentStatusRepository;
  private final EnrollmentStatusMapper enrollmentStatusMapper;

  public EnrollmentStatusReferenceServiceImpl(
      RefEnrollmentStatusRepository refEnrollmentStatusRepository,
      EnrollmentStatusMapper enrollmentStatusMapper) {
    this.refEnrollmentStatusRepository = refEnrollmentStatusRepository;
    this.enrollmentStatusMapper = enrollmentStatusMapper;
  }

  @Override
  public List<EnrollmentStatusResponse> getAllEnrollmentStatuses() {
    return refEnrollmentStatusRepository.findAllByOrderBySortOrderAsc().stream()
        .map(enrollmentStatusMapper::toResponse)
        .toList();
  }

  @Override
  public EnrollmentStatusResponse getById(Long id) {
    if (id == null) {
      throw new BadRequestException("Enrollment status ID is required");
    }
    RefEnrollmentStatus status = refEnrollmentStatusRepository.findById(id)
        .orElseThrow(() -> new ResourceNotFoundException("RefEnrollmentStatus", "id"));
    return enrollmentStatusMapper.toResponse(status);
  }

  @Override
  public EnrollmentStatusResponse getByCode(String statusCode) {
    if (statusCode == null || statusCode.isBlank()) {
      throw new BadRequestException("Status code is required");
    }
    RefEnrollmentStatus status = refEnrollmentStatusRepository.findByStatusCode(statusCode.trim())
        .orElseThrow(() -> new ResourceNotFoundException("RefEnrollmentStatus", "statusCode"));
    return enrollmentStatusMapper.toResponse(status);
  }
}
