package in.gov.sih.sih26135.service.impl;

import in.gov.sih.sih26135.dto.request.CreateTrainingEnrollmentRequest;
import in.gov.sih.sih26135.dto.request.UpdateTrainingEnrollmentRequest;
import in.gov.sih.sih26135.dto.response.TrainingEnrollmentResponse;
import in.gov.sih.sih26135.entity.Course;
import in.gov.sih.sih26135.entity.Program;
import in.gov.sih.sih26135.entity.RefEnrollmentStatus;
import in.gov.sih.sih26135.entity.RefTrainingDropoutReason;
import in.gov.sih.sih26135.entity.Trainee;
import in.gov.sih.sih26135.entity.TrainingBatch;
import in.gov.sih.sih26135.entity.TrainingCenter;
import in.gov.sih.sih26135.entity.TrainingEnrollment;
import in.gov.sih.sih26135.entity.TrainingProvider;
import in.gov.sih.sih26135.exception.BadRequestException;
import in.gov.sih.sih26135.exception.ConflictException;
import in.gov.sih.sih26135.exception.ResourceNotFoundException;
import in.gov.sih.sih26135.mapper.TrainingEnrollmentMapper;
import in.gov.sih.sih26135.repository.RefEnrollmentStatusRepository;
import in.gov.sih.sih26135.repository.RefTrainingDropoutReasonRepository;
import in.gov.sih.sih26135.repository.TraineeRepository;
import in.gov.sih.sih26135.repository.TrainingBatchRepository;
import in.gov.sih.sih26135.repository.TrainingEnrollmentRepository;
import in.gov.sih.sih26135.service.TrainingEnrollmentService;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class TrainingEnrollmentServiceImpl implements TrainingEnrollmentService {

  private final TrainingEnrollmentRepository trainingEnrollmentRepository;
  private final TraineeRepository traineeRepository;
  private final TrainingBatchRepository trainingBatchRepository;
  private final RefEnrollmentStatusRepository refEnrollmentStatusRepository;
  private final RefTrainingDropoutReasonRepository refTrainingDropoutReasonRepository;
  private final TrainingEnrollmentMapper trainingEnrollmentMapper;

  public TrainingEnrollmentServiceImpl(
      TrainingEnrollmentRepository trainingEnrollmentRepository,
      TraineeRepository traineeRepository,
      TrainingBatchRepository trainingBatchRepository,
      RefEnrollmentStatusRepository refEnrollmentStatusRepository,
      RefTrainingDropoutReasonRepository refTrainingDropoutReasonRepository,
      TrainingEnrollmentMapper trainingEnrollmentMapper) {
    this.trainingEnrollmentRepository = trainingEnrollmentRepository;
    this.traineeRepository = traineeRepository;
    this.trainingBatchRepository = trainingBatchRepository;
    this.refEnrollmentStatusRepository = refEnrollmentStatusRepository;
    this.refTrainingDropoutReasonRepository = refTrainingDropoutReasonRepository;
    this.trainingEnrollmentMapper = trainingEnrollmentMapper;
  }

  @Override
  public TrainingEnrollmentResponse getById(Long id) {
    if (id == null) {
      throw new BadRequestException("Enrollment ID is required");
    }
    TrainingEnrollment enrollment = trainingEnrollmentRepository.findById(id)
        .orElseThrow(() -> new ResourceNotFoundException("TrainingEnrollment", "id"));
    return trainingEnrollmentMapper.toResponse(enrollment);
  }

  @Override
  public TrainingEnrollmentResponse getByEnrollmentNumber(String enrollmentNumber) {
    if (enrollmentNumber == null || enrollmentNumber.isBlank()) {
      throw new BadRequestException("Enrollment number is required");
    }
    TrainingEnrollment enrollment = trainingEnrollmentRepository.findByEnrollmentNumber(enrollmentNumber.trim())
        .orElseThrow(() -> new ResourceNotFoundException("TrainingEnrollment", "enrollmentNumber"));
    return trainingEnrollmentMapper.toResponse(enrollment);
  }

  @Override
  public List<TrainingEnrollmentResponse> getAllEnrollments() {
    return trainingEnrollmentRepository.findAll().stream()
        .map(trainingEnrollmentMapper::toResponse)
        .toList();
  }

  @Override
  public List<TrainingEnrollmentResponse> getByTraineeId(Long traineeId) {
    if (traineeId == null) {
      throw new BadRequestException("Trainee ID is required");
    }
    return trainingEnrollmentRepository.findByTraineeId(traineeId).stream()
        .map(trainingEnrollmentMapper::toResponse)
        .toList();
  }

  @Override
  public List<TrainingEnrollmentResponse> getByBatchId(Long batchId) {
    if (batchId == null) {
      throw new BadRequestException("Batch ID is required");
    }
    return trainingEnrollmentRepository.findByTrainingBatchId(batchId).stream()
        .map(trainingEnrollmentMapper::toResponse)
        .toList();
  }

  @Override
  public List<TrainingEnrollmentResponse> getByProgramId(Long programId) {
    if (programId == null) {
      throw new BadRequestException("Program ID is required");
    }
    return trainingEnrollmentRepository.findByProgramId(programId).stream()
        .map(trainingEnrollmentMapper::toResponse)
        .toList();
  }

  @Override
  public List<TrainingEnrollmentResponse> getByCourseId(Long courseId) {
    if (courseId == null) {
      throw new BadRequestException("Course ID is required");
    }
    return trainingEnrollmentRepository.findByCourseId(courseId).stream()
        .map(trainingEnrollmentMapper::toResponse)
        .toList();
  }

  @Override
  public List<TrainingEnrollmentResponse> getByProviderId(Long providerId) {
    if (providerId == null) {
      throw new BadRequestException("Provider ID is required");
    }
    return trainingEnrollmentRepository.findByTrainingProviderId(providerId).stream()
        .map(trainingEnrollmentMapper::toResponse)
        .toList();
  }

  @Override
  public List<TrainingEnrollmentResponse> getByCenterId(Long centerId) {
    if (centerId == null) {
      throw new BadRequestException("Center ID is required");
    }
    return trainingEnrollmentRepository.findByTrainingCenterId(centerId).stream()
        .map(trainingEnrollmentMapper::toResponse)
        .toList();
  }

  @Override
  public List<TrainingEnrollmentResponse> getByStatusId(Long statusId) {
    if (statusId == null) {
      throw new BadRequestException("Enrollment status ID is required");
    }
    return trainingEnrollmentRepository.findByEnrollmentStatusId(statusId).stream()
        .map(trainingEnrollmentMapper::toResponse)
        .toList();
  }

  @Override
  @Transactional
  public TrainingEnrollmentResponse createEnrollment(CreateTrainingEnrollmentRequest request) {
    if (request == null) {
      throw new BadRequestException("Training enrollment creation request cannot be null");
    }
    if (request.getEnrollmentNumber() == null || request.getEnrollmentNumber().isBlank()) {
      throw new BadRequestException("Enrollment number is required");
    }
    if (request.getTraineeId() == null) {
      throw new BadRequestException("Trainee ID is required");
    }
    if (request.getBatchId() == null) {
      throw new BadRequestException("Training batch ID is required");
    }
    if (request.getEnrollmentDate() == null) {
      throw new BadRequestException("Enrollment date is required");
    }
    if (request.getEnrollmentStatusId() == null) {
      throw new BadRequestException("Enrollment status ID is required");
    }

    String enrollmentNumber = request.getEnrollmentNumber().trim();
    if (trainingEnrollmentRepository.existsByEnrollmentNumber(enrollmentNumber)) {
      throw new ConflictException("Enrollment number already exists", "ENROLLMENT_NUMBER_ALREADY_EXISTS");
    }

    if (trainingEnrollmentRepository.existsByTraineeIdAndTrainingBatchId(request.getTraineeId(), request.getBatchId())) {
      throw new ConflictException("Trainee is already enrolled in this training batch", "DUPLICATE_ENROLLMENT");
    }

    Trainee trainee = traineeRepository.findById(request.getTraineeId())
        .orElseThrow(() -> new ResourceNotFoundException("Trainee", "traineeId"));

    TrainingBatch batch = trainingBatchRepository.findById(request.getBatchId())
        .orElseThrow(() -> new ResourceNotFoundException("TrainingBatch", "batchId"));

    Program program = batch.getProgram();
    if (request.getProgramId() != null && !request.getProgramId().equals(program.getId())) {
      throw new BadRequestException("Program ID does not match batch program", "PROGRAM_BATCH_MISMATCH");
    }

    Course course = batch.getCourse();
    if (request.getCourseId() != null && !request.getCourseId().equals(course.getId())) {
      throw new BadRequestException("Course ID does not match batch course", "COURSE_BATCH_MISMATCH");
    }

    TrainingProvider provider = batch.getTrainingProvider();
    if (request.getProviderId() != null && !request.getProviderId().equals(provider.getId())) {
      throw new BadRequestException("Provider ID does not match batch provider", "PROVIDER_BATCH_MISMATCH");
    }

    TrainingCenter center = batch.getTrainingCenter();
    if (request.getCenterId() != null && !request.getCenterId().equals(center.getId())) {
      throw new BadRequestException("Center ID does not match batch center", "CENTER_BATCH_MISMATCH");
    }

    // Date validations
    if (request.getStartDate() != null && request.getStartDate().isBefore(request.getEnrollmentDate())) {
      throw new BadRequestException("Start date cannot precede enrollment date", "INVALID_START_DATE");
    }

    if (request.getExpectedCompletionDate() != null && request.getStartDate() != null
        && request.getExpectedCompletionDate().isBefore(request.getStartDate())) {
      throw new BadRequestException("Expected completion date cannot precede start date", "INVALID_EXPECTED_COMPLETION_DATE");
    }

    if (request.getActualCompletionDate() != null && request.getStartDate() != null
        && request.getActualCompletionDate().isBefore(request.getStartDate())) {
      throw new BadRequestException("Actual completion date cannot precede start date", "INVALID_ACTUAL_COMPLETION_DATE");
    }

    RefEnrollmentStatus status = refEnrollmentStatusRepository.findById(request.getEnrollmentStatusId())
        .orElseThrow(() -> new ResourceNotFoundException("RefEnrollmentStatus", "enrollmentStatusId"));

    RefTrainingDropoutReason dropoutReason = null;
    if (request.getDropoutReasonId() != null) {
      dropoutReason = refTrainingDropoutReasonRepository.findById(request.getDropoutReasonId())
          .orElseThrow(() -> new ResourceNotFoundException("RefTrainingDropoutReason", "dropoutReasonId"));
    }

    TrainingEnrollment entity = trainingEnrollmentMapper.toEntity(
        request, trainee, program, course, provider, center, batch, status, dropoutReason);
    entity.setEnrollmentNumber(enrollmentNumber);
    if (request.getDropoutRemarks() != null) {
      entity.setDropoutRemarks(request.getDropoutRemarks().trim());
    }

    LocalDateTime now = LocalDateTime.now();
    entity.setCreatedAt(now);
    entity.setUpdatedAt(now);

    TrainingEnrollment saved = trainingEnrollmentRepository.save(entity);
    return trainingEnrollmentMapper.toResponse(saved);
  }

  @Override
  @Transactional
  public TrainingEnrollmentResponse updateEnrollment(Long id, UpdateTrainingEnrollmentRequest request) {
    if (id == null) {
      throw new BadRequestException("Enrollment ID is required");
    }
    if (request == null) {
      throw new BadRequestException("Training enrollment update request cannot be null");
    }

    TrainingEnrollment enrollment = trainingEnrollmentRepository.findById(id)
        .orElseThrow(() -> new ResourceNotFoundException("TrainingEnrollment", "id"));

    LocalDate effectiveStartDate = request.getStartDate() != null ? request.getStartDate() : enrollment.getStartDate();
    LocalDate enrollmentDate = enrollment.getEnrollmentDate();

    if (effectiveStartDate != null && effectiveStartDate.isBefore(enrollmentDate)) {
      throw new BadRequestException("Start date cannot precede enrollment date", "INVALID_START_DATE");
    }
    if (request.getStartDate() != null) {
      enrollment.setStartDate(request.getStartDate());
    }

    LocalDate effectiveExpectedCompletion = request.getExpectedCompletionDate() != null
        ? request.getExpectedCompletionDate()
        : enrollment.getExpectedCompletionDate();

    if (effectiveExpectedCompletion != null && effectiveStartDate != null
        && effectiveExpectedCompletion.isBefore(effectiveStartDate)) {
      throw new BadRequestException("Expected completion date cannot precede start date", "INVALID_EXPECTED_COMPLETION_DATE");
    }
    if (request.getExpectedCompletionDate() != null) {
      enrollment.setExpectedCompletionDate(request.getExpectedCompletionDate());
    }

    LocalDate effectiveActualCompletion = request.getActualCompletionDate() != null
        ? request.getActualCompletionDate()
        : enrollment.getActualCompletionDate();

    if (effectiveActualCompletion != null && effectiveStartDate != null
        && effectiveActualCompletion.isBefore(effectiveStartDate)) {
      throw new BadRequestException("Actual completion date cannot precede start date", "INVALID_ACTUAL_COMPLETION_DATE");
    }
    if (request.getActualCompletionDate() != null) {
      enrollment.setActualCompletionDate(request.getActualCompletionDate());
    }

    if (request.getEnrollmentStatusId() != null) {
      RefEnrollmentStatus status = refEnrollmentStatusRepository.findById(request.getEnrollmentStatusId())
          .orElseThrow(() -> new ResourceNotFoundException("RefEnrollmentStatus", "enrollmentStatusId"));
      enrollment.setEnrollmentStatus(status);
    }

    if (request.getDropoutReasonId() != null) {
      RefTrainingDropoutReason reason = refTrainingDropoutReasonRepository.findById(request.getDropoutReasonId())
          .orElseThrow(() -> new ResourceNotFoundException("RefTrainingDropoutReason", "dropoutReasonId"));
      enrollment.setDropoutReason(reason);
    }

    if (request.getDropoutRemarks() != null) {
      enrollment.setDropoutRemarks(request.getDropoutRemarks().trim());
    }

    LocalDateTime now = LocalDateTime.now();
    enrollment.setUpdatedAt(now);

    TrainingEnrollment saved = trainingEnrollmentRepository.save(enrollment);
    return trainingEnrollmentMapper.toResponse(saved);
  }

  @Override
  @Transactional
  public void deleteEnrollment(Long id) {
    if (id == null) {
      throw new BadRequestException("Enrollment ID is required");
    }
    TrainingEnrollment enrollment = trainingEnrollmentRepository.findById(id)
        .orElseThrow(() -> new ResourceNotFoundException("TrainingEnrollment", "id"));

    LocalDateTime now = LocalDateTime.now();
    enrollment.setDeletedAt(now);
    enrollment.setUpdatedAt(now);
    trainingEnrollmentRepository.save(enrollment);
  }
}
