package in.gov.sih.sih26135.service.impl;

import in.gov.sih.sih26135.dto.response.SalaryProgressionFactResponse;
import in.gov.sih.sih26135.entity.analytics.SalaryProgressionFact;
import in.gov.sih.sih26135.exception.BadRequestException;
import in.gov.sih.sih26135.exception.ResourceNotFoundException;
import in.gov.sih.sih26135.mapper.SalaryProgressionFactMapper;
import in.gov.sih.sih26135.repository.analytics.SalaryProgressionFactRepository;
import in.gov.sih.sih26135.response.PagedResponse;
import in.gov.sih.sih26135.service.SalaryProgressionFactService;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class SalaryProgressionFactServiceImpl implements SalaryProgressionFactService {

  private final SalaryProgressionFactRepository repository;
  private final SalaryProgressionFactMapper mapper;

  public SalaryProgressionFactServiceImpl(
      SalaryProgressionFactRepository repository,
      SalaryProgressionFactMapper mapper) {
    this.repository = repository;
    this.mapper = mapper;
  }

  @Override
  public PagedResponse<SalaryProgressionFactResponse> getAllSalaryProgressions(Pageable pageable) {
    Page<SalaryProgressionFact> page = repository.findAll(pageable);
    return PagedResponse.from(page, mapper::toResponse);
  }

  @Override
  public List<SalaryProgressionFactResponse> getAllSalaryProgressions() {
    return repository.findAll().stream()
        .map(mapper::toResponse)
        .toList();
  }

  @Override
  public SalaryProgressionFactResponse getSalaryProgressionById(Long employmentId) {
    if (employmentId == null) {
      throw new BadRequestException("Employment ID is required");
    }
    SalaryProgressionFact entity = repository.findById(employmentId)
        .orElseThrow(() -> new ResourceNotFoundException("SalaryProgressionFact", "employmentId"));
    return mapper.toResponse(entity);
  }

  @Override
  public List<SalaryProgressionFactResponse> getSalaryProgressionsByTraineeId(Long traineeId) {
    if (traineeId == null) {
      throw new BadRequestException("Trainee ID is required");
    }
    return repository.findByTraineeId(traineeId).stream()
        .map(mapper::toResponse)
        .toList();
  }

  @Override
  public PagedResponse<SalaryProgressionFactResponse> getSalaryProgressionsByTraineeId(Long traineeId, Pageable pageable) {
    if (traineeId == null) {
      throw new BadRequestException("Trainee ID is required");
    }
    Page<SalaryProgressionFact> page = repository.findByTraineeId(traineeId, pageable);
    return PagedResponse.from(page, mapper::toResponse);
  }

  @Override
  public List<SalaryProgressionFactResponse> getSalaryProgressionsByEnrollmentId(Long enrollmentId) {
    if (enrollmentId == null) {
      throw new BadRequestException("Enrollment ID is required");
    }
    return repository.findByEnrollmentId(enrollmentId).stream()
        .map(mapper::toResponse)
        .toList();
  }

  @Override
  public PagedResponse<SalaryProgressionFactResponse> getSalaryProgressionsByEnrollmentId(
      Long enrollmentId, Pageable pageable) {
    if (enrollmentId == null) {
      throw new BadRequestException("Enrollment ID is required");
    }
    Page<SalaryProgressionFact> page = repository.findByEnrollmentId(enrollmentId, pageable);
    return PagedResponse.from(page, mapper::toResponse);
  }

  @Override
  public List<SalaryProgressionFactResponse> getSalaryProgressionsByCourseId(Long courseId) {
    if (courseId == null) {
      throw new BadRequestException("Course ID is required");
    }
    return repository.findByCourseId(courseId).stream()
        .map(mapper::toResponse)
        .toList();
  }

  @Override
  public PagedResponse<SalaryProgressionFactResponse> getSalaryProgressionsByCourseId(Long courseId, Pageable pageable) {
    if (courseId == null) {
      throw new BadRequestException("Course ID is required");
    }
    Page<SalaryProgressionFact> page = repository.findByCourseId(courseId, pageable);
    return PagedResponse.from(page, mapper::toResponse);
  }

  @Override
  public List<SalaryProgressionFactResponse> getSalaryProgressionsByProviderId(Long providerId) {
    if (providerId == null) {
      throw new BadRequestException("Provider ID is required");
    }
    return repository.findByProviderId(providerId).stream()
        .map(mapper::toResponse)
        .toList();
  }

  @Override
  public PagedResponse<SalaryProgressionFactResponse> getSalaryProgressionsByProviderId(
      Long providerId, Pageable pageable) {
    if (providerId == null) {
      throw new BadRequestException("Provider ID is required");
    }
    Page<SalaryProgressionFact> page = repository.findByProviderId(providerId, pageable);
    return PagedResponse.from(page, mapper::toResponse);
  }

  @Override
  public List<SalaryProgressionFactResponse> getSalaryProgressionsByEngagementTypeId(Long engagementTypeId) {
    if (engagementTypeId == null) {
      throw new BadRequestException("Engagement type ID is required");
    }
    return repository.findByEngagementTypeId(engagementTypeId).stream()
        .map(mapper::toResponse)
        .toList();
  }

  @Override
  public PagedResponse<SalaryProgressionFactResponse> getSalaryProgressionsByEngagementTypeId(
      Long engagementTypeId, Pageable pageable) {
    if (engagementTypeId == null) {
      throw new BadRequestException("Engagement type ID is required");
    }
    Page<SalaryProgressionFact> page = repository.findByEngagementTypeId(engagementTypeId, pageable);
    return PagedResponse.from(page, mapper::toResponse);
  }
}
