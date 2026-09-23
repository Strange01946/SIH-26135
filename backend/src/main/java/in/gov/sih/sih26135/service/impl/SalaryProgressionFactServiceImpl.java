package in.gov.sih.sih26135.service.impl;

import in.gov.sih.sih26135.dto.response.SalaryProgressionFactResponse;
import in.gov.sih.sih26135.entity.analytics.SalaryProgressionFact;
import in.gov.sih.sih26135.exception.BadRequestException;
import in.gov.sih.sih26135.exception.ResourceNotFoundException;
import in.gov.sih.sih26135.mapper.SalaryProgressionFactMapper;
import in.gov.sih.sih26135.repository.analytics.SalaryProgressionFactRepository;
import in.gov.sih.sih26135.service.SalaryProgressionFactService;
import java.util.List;
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
  public List<SalaryProgressionFactResponse> getSalaryProgressionsByEnrollmentId(Long enrollmentId) {
    if (enrollmentId == null) {
      throw new BadRequestException("Enrollment ID is required");
    }
    return repository.findByEnrollmentId(enrollmentId).stream()
        .map(mapper::toResponse)
        .toList();
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
  public List<SalaryProgressionFactResponse> getSalaryProgressionsByProviderId(Long providerId) {
    if (providerId == null) {
      throw new BadRequestException("Provider ID is required");
    }
    return repository.findByProviderId(providerId).stream()
        .map(mapper::toResponse)
        .toList();
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
}
