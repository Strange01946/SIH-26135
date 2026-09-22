package in.gov.sih.sih26135.service.impl;

import in.gov.sih.sih26135.dto.response.JobPostingStatusResponse;
import in.gov.sih.sih26135.entity.RefJobPostingStatus;
import in.gov.sih.sih26135.exception.BadRequestException;
import in.gov.sih.sih26135.exception.ResourceNotFoundException;
import in.gov.sih.sih26135.mapper.JobPostingStatusMapper;
import in.gov.sih.sih26135.repository.RefJobPostingStatusRepository;
import in.gov.sih.sih26135.service.JobPostingStatusReferenceService;
import java.util.List;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class JobPostingStatusReferenceServiceImpl implements JobPostingStatusReferenceService {

  private final RefJobPostingStatusRepository refJobPostingStatusRepository;
  private final JobPostingStatusMapper jobPostingStatusMapper;

  public JobPostingStatusReferenceServiceImpl(
      RefJobPostingStatusRepository refJobPostingStatusRepository,
      JobPostingStatusMapper jobPostingStatusMapper) {
    this.refJobPostingStatusRepository = refJobPostingStatusRepository;
    this.jobPostingStatusMapper = jobPostingStatusMapper;
  }

  @Override
  public List<JobPostingStatusResponse> getAllJobPostingStatuses() {
    return refJobPostingStatusRepository.findAll(Sort.by(Sort.Direction.ASC, "sortOrder")).stream()
        .map(jobPostingStatusMapper::toResponse)
        .toList();
  }

  @Override
  public JobPostingStatusResponse getById(Long id) {
    if (id == null) {
      throw new BadRequestException("Job posting status ID is required");
    }
    RefJobPostingStatus entity = refJobPostingStatusRepository.findById(id)
        .orElseThrow(() -> new ResourceNotFoundException("RefJobPostingStatus", "id"));
    return jobPostingStatusMapper.toResponse(entity);
  }

  @Override
  public JobPostingStatusResponse getByCode(String statusCode) {
    if (statusCode == null || statusCode.isBlank()) {
      throw new BadRequestException("Status code is required");
    }
    RefJobPostingStatus entity = refJobPostingStatusRepository.findByStatusCode(statusCode.trim())
        .orElseThrow(() -> new ResourceNotFoundException("RefJobPostingStatus", "statusCode"));
    return jobPostingStatusMapper.toResponse(entity);
  }

  @Override
  public List<JobPostingStatusResponse> getByIsOpenFlag(Boolean isOpenFlag) {
    if (isOpenFlag == null) {
      throw new BadRequestException("isOpenFlag is required");
    }
    return refJobPostingStatusRepository.findByIsOpenFlag(isOpenFlag).stream()
        .map(jobPostingStatusMapper::toResponse)
        .toList();
  }
}
