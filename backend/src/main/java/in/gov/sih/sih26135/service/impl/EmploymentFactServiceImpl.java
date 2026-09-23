package in.gov.sih.sih26135.service.impl;

import in.gov.sih.sih26135.dto.response.EmploymentFactResponse;
import in.gov.sih.sih26135.entity.analytics.EmploymentFact;
import in.gov.sih.sih26135.exception.BadRequestException;
import in.gov.sih.sih26135.exception.ResourceNotFoundException;
import in.gov.sih.sih26135.mapper.EmploymentFactMapper;
import in.gov.sih.sih26135.repository.analytics.EmploymentFactRepository;
import in.gov.sih.sih26135.service.EmploymentFactService;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class EmploymentFactServiceImpl implements EmploymentFactService {

  private final EmploymentFactRepository repository;
  private final EmploymentFactMapper mapper;

  public EmploymentFactServiceImpl(
      EmploymentFactRepository repository,
      EmploymentFactMapper mapper) {
    this.repository = repository;
    this.mapper = mapper;
  }

  @Override
  public List<EmploymentFactResponse> getAllEmployments() {
    return repository.findAll().stream()
        .map(mapper::toResponse)
        .toList();
  }

  @Override
  public EmploymentFactResponse getEmploymentById(Long employmentId) {
    if (employmentId == null) {
      throw new BadRequestException("Employment ID is required");
    }
    EmploymentFact entity = repository.findById(employmentId)
        .orElseThrow(() -> new ResourceNotFoundException("EmploymentFact", "employmentId"));
    return mapper.toResponse(entity);
  }

  @Override
  public EmploymentFactResponse getEmploymentByEmploymentNumber(String employmentNumber) {
    if (employmentNumber == null || employmentNumber.isBlank()) {
      throw new BadRequestException("Employment number is required");
    }
    EmploymentFact entity = repository.findByEmploymentNumber(employmentNumber.trim())
        .orElseThrow(() -> new ResourceNotFoundException("EmploymentFact", "employmentNumber"));
    return mapper.toResponse(entity);
  }

  @Override
  public List<EmploymentFactResponse> getEmploymentsByTraineeId(Long traineeId) {
    if (traineeId == null) {
      throw new BadRequestException("Trainee ID is required");
    }
    return repository.findByTraineeId(traineeId).stream()
        .map(mapper::toResponse)
        .toList();
  }

  @Override
  public List<EmploymentFactResponse> getEmploymentsByEmployerId(Long employerId) {
    if (employerId == null) {
      throw new BadRequestException("Employer ID is required");
    }
    return repository.findByEmployerId(employerId).stream()
        .map(mapper::toResponse)
        .toList();
  }

  @Override
  public List<EmploymentFactResponse> getEmploymentsByEnrollmentId(Long enrollmentId) {
    if (enrollmentId == null) {
      throw new BadRequestException("Enrollment ID is required");
    }
    return repository.findByEnrollmentId(enrollmentId).stream()
        .map(mapper::toResponse)
        .toList();
  }

  @Override
  public List<EmploymentFactResponse> getEmploymentsByPlacementId(Long placementId) {
    if (placementId == null) {
      throw new BadRequestException("Placement ID is required");
    }
    return repository.findByPlacementId(placementId).stream()
        .map(mapper::toResponse)
        .toList();
  }

  @Override
  public List<EmploymentFactResponse> getEmploymentsByJobRoleId(Long jobRoleId) {
    if (jobRoleId == null) {
      throw new BadRequestException("Job role ID is required");
    }
    return repository.findByJobRoleId(jobRoleId).stream()
        .map(mapper::toResponse)
        .toList();
  }

  @Override
  public List<EmploymentFactResponse> getCurrentEmployments() {
    return repository.findByIsCurrentTrue().stream()
        .map(mapper::toResponse)
        .toList();
  }

  @Override
  public List<EmploymentFactResponse> getEmploymentsByEngagementTypeId(Long engagementTypeId) {
    if (engagementTypeId == null) {
      throw new BadRequestException("Engagement type ID is required");
    }
    return repository.findByEngagementTypeId(engagementTypeId).stream()
        .map(mapper::toResponse)
        .toList();
  }

  @Override
  public List<EmploymentFactResponse> getEmploymentsByTraineeDistrictId(Long traineeDistrictId) {
    if (traineeDistrictId == null) {
      throw new BadRequestException("Trainee district ID is required");
    }
    return repository.findByTraineeDistrictId(traineeDistrictId).stream()
        .map(mapper::toResponse)
        .toList();
  }
}
