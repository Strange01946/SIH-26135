package in.gov.sih.sih26135.service.impl;

import in.gov.sih.sih26135.dto.response.EmploymentExitReasonResponse;
import in.gov.sih.sih26135.entity.RefEmploymentExitReason;
import in.gov.sih.sih26135.exception.BadRequestException;
import in.gov.sih.sih26135.exception.ResourceNotFoundException;
import in.gov.sih.sih26135.mapper.EmploymentExitReasonMapper;
import in.gov.sih.sih26135.repository.RefEmploymentExitReasonRepository;
import in.gov.sih.sih26135.service.EmploymentExitReasonReferenceService;
import java.util.List;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class EmploymentExitReasonReferenceServiceImpl implements EmploymentExitReasonReferenceService {

  private final RefEmploymentExitReasonRepository refEmploymentExitReasonRepository;
  private final EmploymentExitReasonMapper employmentExitReasonMapper;

  public EmploymentExitReasonReferenceServiceImpl(
      RefEmploymentExitReasonRepository refEmploymentExitReasonRepository,
      EmploymentExitReasonMapper employmentExitReasonMapper) {
    this.refEmploymentExitReasonRepository = refEmploymentExitReasonRepository;
    this.employmentExitReasonMapper = employmentExitReasonMapper;
  }

  @Override
  public List<EmploymentExitReasonResponse> getAllExitReasons() {
    return refEmploymentExitReasonRepository.findAll(Sort.by(Sort.Direction.ASC, "sortOrder")).stream()
        .map(employmentExitReasonMapper::toResponse)
        .toList();
  }

  @Override
  public EmploymentExitReasonResponse getById(Long id) {
    if (id == null) {
      throw new BadRequestException("Employment exit reason ID is required");
    }
    RefEmploymentExitReason entity = refEmploymentExitReasonRepository.findById(id)
        .orElseThrow(() -> new ResourceNotFoundException("RefEmploymentExitReason", "id"));
    return employmentExitReasonMapper.toResponse(entity);
  }

  @Override
  public EmploymentExitReasonResponse getByCode(String reasonCode) {
    if (reasonCode == null || reasonCode.isBlank()) {
      throw new BadRequestException("Reason code is required");
    }
    RefEmploymentExitReason entity = refEmploymentExitReasonRepository.findByReasonCode(reasonCode.trim())
        .orElseThrow(() -> new ResourceNotFoundException("RefEmploymentExitReason", "reasonCode"));
    return employmentExitReasonMapper.toResponse(entity);
  }
}
