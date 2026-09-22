package in.gov.sih.sih26135.service.impl;

import in.gov.sih.sih26135.dto.response.EmploymentInfoSourceResponse;
import in.gov.sih.sih26135.entity.RefEmploymentInfoSource;
import in.gov.sih.sih26135.exception.BadRequestException;
import in.gov.sih.sih26135.exception.ResourceNotFoundException;
import in.gov.sih.sih26135.mapper.EmploymentInfoSourceMapper;
import in.gov.sih.sih26135.repository.RefEmploymentInfoSourceRepository;
import in.gov.sih.sih26135.service.EmploymentInfoSourceReferenceService;
import java.util.List;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class EmploymentInfoSourceReferenceServiceImpl implements EmploymentInfoSourceReferenceService {

  private final RefEmploymentInfoSourceRepository refEmploymentInfoSourceRepository;
  private final EmploymentInfoSourceMapper employmentInfoSourceMapper;

  public EmploymentInfoSourceReferenceServiceImpl(
      RefEmploymentInfoSourceRepository refEmploymentInfoSourceRepository,
      EmploymentInfoSourceMapper employmentInfoSourceMapper) {
    this.refEmploymentInfoSourceRepository = refEmploymentInfoSourceRepository;
    this.employmentInfoSourceMapper = employmentInfoSourceMapper;
  }

  @Override
  public List<EmploymentInfoSourceResponse> getAllInfoSources() {
    return refEmploymentInfoSourceRepository.findAll(Sort.by(Sort.Direction.ASC, "sortOrder")).stream()
        .map(employmentInfoSourceMapper::toResponse)
        .toList();
  }

  @Override
  public EmploymentInfoSourceResponse getById(Long id) {
    if (id == null) {
      throw new BadRequestException("Employment info source ID is required");
    }
    RefEmploymentInfoSource entity = refEmploymentInfoSourceRepository.findById(id)
        .orElseThrow(() -> new ResourceNotFoundException("RefEmploymentInfoSource", "id"));
    return employmentInfoSourceMapper.toResponse(entity);
  }

  @Override
  public EmploymentInfoSourceResponse getByCode(String sourceCode) {
    if (sourceCode == null || sourceCode.isBlank()) {
      throw new BadRequestException("Source code is required");
    }
    RefEmploymentInfoSource entity = refEmploymentInfoSourceRepository.findBySourceCode(sourceCode.trim())
        .orElseThrow(() -> new ResourceNotFoundException("RefEmploymentInfoSource", "sourceCode"));
    return employmentInfoSourceMapper.toResponse(entity);
  }

  @Override
  public List<EmploymentInfoSourceResponse> getByIsSelfReported(Boolean isSelfReportedFlag) {
    if (isSelfReportedFlag == null) {
      throw new BadRequestException("isSelfReportedFlag is required");
    }
    return refEmploymentInfoSourceRepository.findByIsSelfReportedFlag(isSelfReportedFlag).stream()
        .map(employmentInfoSourceMapper::toResponse)
        .toList();
  }
}
