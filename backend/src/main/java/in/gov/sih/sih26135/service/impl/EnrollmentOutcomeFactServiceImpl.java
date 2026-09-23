package in.gov.sih.sih26135.service.impl;

import in.gov.sih.sih26135.dto.response.EnrollmentOutcomeFactResponse;
import in.gov.sih.sih26135.entity.analytics.EnrollmentOutcomeFact;
import in.gov.sih.sih26135.exception.BadRequestException;
import in.gov.sih.sih26135.exception.ResourceNotFoundException;
import in.gov.sih.sih26135.mapper.EnrollmentOutcomeFactMapper;
import in.gov.sih.sih26135.repository.analytics.EnrollmentOutcomeFactRepository;
import in.gov.sih.sih26135.service.EnrollmentOutcomeFactService;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class EnrollmentOutcomeFactServiceImpl implements EnrollmentOutcomeFactService {

  private final EnrollmentOutcomeFactRepository repository;
  private final EnrollmentOutcomeFactMapper mapper;

  public EnrollmentOutcomeFactServiceImpl(
      EnrollmentOutcomeFactRepository repository,
      EnrollmentOutcomeFactMapper mapper) {
    this.repository = repository;
    this.mapper = mapper;
  }

  @Override
  public List<EnrollmentOutcomeFactResponse> getAllEnrollmentOutcomes() {
    return repository.findAll().stream()
        .map(mapper::toResponse)
        .toList();
  }

  @Override
  public EnrollmentOutcomeFactResponse getEnrollmentOutcomeById(Long enrollmentId) {
    if (enrollmentId == null) {
      throw new BadRequestException("Enrollment ID is required");
    }
    EnrollmentOutcomeFact entity = repository.findById(enrollmentId)
        .orElseThrow(() -> new ResourceNotFoundException("EnrollmentOutcomeFact", "enrollmentId"));
    return mapper.toResponse(entity);
  }

  @Override
  public EnrollmentOutcomeFactResponse getEnrollmentOutcomeByEnrollmentNumber(String enrollmentNumber) {
    if (enrollmentNumber == null || enrollmentNumber.isBlank()) {
      throw new BadRequestException("Enrollment number is required");
    }
    EnrollmentOutcomeFact entity = repository.findByEnrollmentNumber(enrollmentNumber.trim())
        .orElseThrow(() -> new ResourceNotFoundException("EnrollmentOutcomeFact", "enrollmentNumber"));
    return mapper.toResponse(entity);
  }

  @Override
  public List<EnrollmentOutcomeFactResponse> getEnrollmentOutcomesByTraineeId(Long traineeId) {
    if (traineeId == null) {
      throw new BadRequestException("Trainee ID is required");
    }
    return repository.findByTraineeId(traineeId).stream()
        .map(mapper::toResponse)
        .toList();
  }

  @Override
  public List<EnrollmentOutcomeFactResponse> getEnrollmentOutcomesByProgramId(Long programId) {
    if (programId == null) {
      throw new BadRequestException("Program ID is required");
    }
    return repository.findByProgramId(programId).stream()
        .map(mapper::toResponse)
        .toList();
  }

  @Override
  public List<EnrollmentOutcomeFactResponse> getEnrollmentOutcomesByCourseId(Long courseId) {
    if (courseId == null) {
      throw new BadRequestException("Course ID is required");
    }
    return repository.findByCourseId(courseId).stream()
        .map(mapper::toResponse)
        .toList();
  }

  @Override
  public List<EnrollmentOutcomeFactResponse> getEnrollmentOutcomesByProviderId(Long providerId) {
    if (providerId == null) {
      throw new BadRequestException("Provider ID is required");
    }
    return repository.findByProviderId(providerId).stream()
        .map(mapper::toResponse)
        .toList();
  }

  @Override
  public List<EnrollmentOutcomeFactResponse> getEnrollmentOutcomesByBatchId(Long batchId) {
    if (batchId == null) {
      throw new BadRequestException("Batch ID is required");
    }
    return repository.findByBatchId(batchId).stream()
        .map(mapper::toResponse)
        .toList();
  }

  @Override
  public List<EnrollmentOutcomeFactResponse> getEnrollmentOutcomesByTraineeDistrictId(Long traineeDistrictId) {
    if (traineeDistrictId == null) {
      throw new BadRequestException("Trainee district ID is required");
    }
    return repository.findByTraineeDistrictId(traineeDistrictId).stream()
        .map(mapper::toResponse)
        .toList();
  }

  @Override
  public List<EnrollmentOutcomeFactResponse> getEnrollmentOutcomesByTraineeStateId(Long traineeStateId) {
    if (traineeStateId == null) {
      throw new BadRequestException("Trainee state ID is required");
    }
    return repository.findByTraineeStateId(traineeStateId).stream()
        .map(mapper::toResponse)
        .toList();
  }
}
