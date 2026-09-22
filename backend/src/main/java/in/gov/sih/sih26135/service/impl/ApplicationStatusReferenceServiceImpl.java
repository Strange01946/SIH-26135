package in.gov.sih.sih26135.service.impl;

import in.gov.sih.sih26135.dto.response.ApplicationStatusResponse;
import in.gov.sih.sih26135.entity.RefApplicationStatus;
import in.gov.sih.sih26135.exception.BadRequestException;
import in.gov.sih.sih26135.exception.ResourceNotFoundException;
import in.gov.sih.sih26135.mapper.ApplicationStatusMapper;
import in.gov.sih.sih26135.repository.RefApplicationStatusRepository;
import in.gov.sih.sih26135.service.ApplicationStatusReferenceService;
import java.util.List;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class ApplicationStatusReferenceServiceImpl implements ApplicationStatusReferenceService {

  private final RefApplicationStatusRepository refApplicationStatusRepository;
  private final ApplicationStatusMapper applicationStatusMapper;

  public ApplicationStatusReferenceServiceImpl(
      RefApplicationStatusRepository refApplicationStatusRepository,
      ApplicationStatusMapper applicationStatusMapper) {
    this.refApplicationStatusRepository = refApplicationStatusRepository;
    this.applicationStatusMapper = applicationStatusMapper;
  }

  @Override
  public List<ApplicationStatusResponse> getAllApplicationStatuses() {
    return refApplicationStatusRepository.findAll(Sort.by(Sort.Direction.ASC, "sortOrder")).stream()
        .map(applicationStatusMapper::toResponse)
        .toList();
  }

  @Override
  public ApplicationStatusResponse getById(Long id) {
    if (id == null) {
      throw new BadRequestException("Application status ID is required");
    }
    RefApplicationStatus entity = refApplicationStatusRepository.findById(id)
        .orElseThrow(() -> new ResourceNotFoundException("RefApplicationStatus", "id"));
    return applicationStatusMapper.toResponse(entity);
  }

  @Override
  public ApplicationStatusResponse getByCode(String statusCode) {
    if (statusCode == null || statusCode.isBlank()) {
      throw new BadRequestException("Status code is required");
    }
    RefApplicationStatus entity = refApplicationStatusRepository.findByStatusCode(statusCode.trim())
        .orElseThrow(() -> new ResourceNotFoundException("RefApplicationStatus", "statusCode"));
    return applicationStatusMapper.toResponse(entity);
  }
}
