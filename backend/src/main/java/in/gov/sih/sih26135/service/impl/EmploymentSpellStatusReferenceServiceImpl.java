package in.gov.sih.sih26135.service.impl;

import in.gov.sih.sih26135.dto.response.EmploymentSpellStatusResponse;
import in.gov.sih.sih26135.entity.RefEmploymentSpellStatus;
import in.gov.sih.sih26135.exception.BadRequestException;
import in.gov.sih.sih26135.exception.ResourceNotFoundException;
import in.gov.sih.sih26135.mapper.EmploymentSpellStatusMapper;
import in.gov.sih.sih26135.repository.RefEmploymentSpellStatusRepository;
import in.gov.sih.sih26135.service.EmploymentSpellStatusReferenceService;
import java.util.List;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class EmploymentSpellStatusReferenceServiceImpl implements EmploymentSpellStatusReferenceService {

  private final RefEmploymentSpellStatusRepository refEmploymentSpellStatusRepository;
  private final EmploymentSpellStatusMapper employmentSpellStatusMapper;

  public EmploymentSpellStatusReferenceServiceImpl(
      RefEmploymentSpellStatusRepository refEmploymentSpellStatusRepository,
      EmploymentSpellStatusMapper employmentSpellStatusMapper) {
    this.refEmploymentSpellStatusRepository = refEmploymentSpellStatusRepository;
    this.employmentSpellStatusMapper = employmentSpellStatusMapper;
  }

  @Override
  public List<EmploymentSpellStatusResponse> getAllSpellStatuses() {
    return refEmploymentSpellStatusRepository.findAll(Sort.by(Sort.Direction.ASC, "sortOrder")).stream()
        .map(employmentSpellStatusMapper::toResponse)
        .toList();
  }

  @Override
  public EmploymentSpellStatusResponse getById(Long id) {
    if (id == null) {
      throw new BadRequestException("Employment spell status ID is required");
    }
    RefEmploymentSpellStatus entity = refEmploymentSpellStatusRepository.findById(id)
        .orElseThrow(() -> new ResourceNotFoundException("RefEmploymentSpellStatus", "id"));
    return employmentSpellStatusMapper.toResponse(entity);
  }

  @Override
  public EmploymentSpellStatusResponse getByCode(String statusCode) {
    if (statusCode == null || statusCode.isBlank()) {
      throw new BadRequestException("Status code is required");
    }
    RefEmploymentSpellStatus entity = refEmploymentSpellStatusRepository.findByStatusCode(statusCode.trim())
        .orElseThrow(() -> new ResourceNotFoundException("RefEmploymentSpellStatus", "statusCode"));
    return employmentSpellStatusMapper.toResponse(entity);
  }

  @Override
  public List<EmploymentSpellStatusResponse> getByIsActive(Boolean isActiveFlag) {
    if (isActiveFlag == null) {
      throw new BadRequestException("isActiveFlag is required");
    }
    return refEmploymentSpellStatusRepository.findByIsActiveFlag(isActiveFlag).stream()
        .map(employmentSpellStatusMapper::toResponse)
        .toList();
  }
}
