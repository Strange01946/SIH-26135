package in.gov.sih.sih26135.service.impl;

import in.gov.sih.sih26135.dto.response.EmploymentRetentionFactResponse;
import in.gov.sih.sih26135.entity.analytics.EmploymentRetentionFact;
import in.gov.sih.sih26135.exception.BadRequestException;
import in.gov.sih.sih26135.exception.ResourceNotFoundException;
import in.gov.sih.sih26135.mapper.EmploymentRetentionFactMapper;
import in.gov.sih.sih26135.repository.analytics.EmploymentRetentionFactRepository;
import in.gov.sih.sih26135.service.EmploymentRetentionFactService;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class EmploymentRetentionFactServiceImpl implements EmploymentRetentionFactService {

  private final EmploymentRetentionFactRepository repository;
  private final EmploymentRetentionFactMapper mapper;

  public EmploymentRetentionFactServiceImpl(
      EmploymentRetentionFactRepository repository,
      EmploymentRetentionFactMapper mapper) {
    this.repository = repository;
    this.mapper = mapper;
  }

  @Override
  public List<EmploymentRetentionFactResponse> getAllEmploymentRetentions() {
    return repository.findAll().stream()
        .map(mapper::toResponse)
        .toList();
  }

  @Override
  public EmploymentRetentionFactResponse getEmploymentRetentionById(Long employmentId) {
    if (employmentId == null) {
      throw new BadRequestException("Employment ID is required");
    }
    EmploymentRetentionFact entity = repository.findById(employmentId)
        .orElseThrow(() -> new ResourceNotFoundException("EmploymentRetentionFact", "employmentId"));
    return mapper.toResponse(entity);
  }

  @Override
  public List<EmploymentRetentionFactResponse> getEmploymentRetentionsByTraineeId(Long traineeId) {
    if (traineeId == null) {
      throw new BadRequestException("Trainee ID is required");
    }
    return repository.findByTraineeId(traineeId).stream()
        .map(mapper::toResponse)
        .toList();
  }

  @Override
  public List<EmploymentRetentionFactResponse> getEmploymentRetentionsByEnrollmentId(Long enrollmentId) {
    if (enrollmentId == null) {
      throw new BadRequestException("Enrollment ID is required");
    }
    return repository.findByEnrollmentId(enrollmentId).stream()
        .map(mapper::toResponse)
        .toList();
  }

  @Override
  public List<EmploymentRetentionFactResponse> getEmploymentRetentionsByEngagementTypeId(Long engagementTypeId) {
    if (engagementTypeId == null) {
      throw new BadRequestException("Engagement type ID is required");
    }
    return repository.findByEngagementTypeId(engagementTypeId).stream()
        .map(mapper::toResponse)
        .toList();
  }

  @Override
  public List<EmploymentRetentionFactResponse> getCurrentEmploymentRetentions() {
    return repository.findByIsCurrentTrue().stream()
        .map(mapper::toResponse)
        .toList();
  }
}
