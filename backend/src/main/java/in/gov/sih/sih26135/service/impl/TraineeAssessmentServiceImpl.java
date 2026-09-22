package in.gov.sih.sih26135.service.impl;

import in.gov.sih.sih26135.dto.request.CreateTraineeAssessmentRequest;
import in.gov.sih.sih26135.dto.request.UpdateTraineeAssessmentRequest;
import in.gov.sih.sih26135.dto.response.TraineeAssessmentResponse;
import in.gov.sih.sih26135.entity.Assessment;
import in.gov.sih.sih26135.entity.Trainee;
import in.gov.sih.sih26135.entity.TraineeAssessment;
import in.gov.sih.sih26135.entity.TrainingEnrollment;
import in.gov.sih.sih26135.exception.BadRequestException;
import in.gov.sih.sih26135.exception.ConflictException;
import in.gov.sih.sih26135.exception.ResourceNotFoundException;
import in.gov.sih.sih26135.mapper.TraineeAssessmentMapper;
import in.gov.sih.sih26135.repository.AssessmentRepository;
import in.gov.sih.sih26135.repository.AssessmentResultRepository;
import in.gov.sih.sih26135.repository.TraineeAssessmentRepository;
import in.gov.sih.sih26135.repository.TraineeRepository;
import in.gov.sih.sih26135.repository.TrainingEnrollmentRepository;
import in.gov.sih.sih26135.repository.UserRepository;
import in.gov.sih.sih26135.service.TraineeAssessmentService;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class TraineeAssessmentServiceImpl implements TraineeAssessmentService {

  private final TraineeAssessmentRepository traineeAssessmentRepository;
  private final AssessmentRepository assessmentRepository;
  private final TrainingEnrollmentRepository trainingEnrollmentRepository;
  private final TraineeRepository traineeRepository;
  private final UserRepository userRepository;
  private final AssessmentResultRepository assessmentResultRepository;
  private final TraineeAssessmentMapper traineeAssessmentMapper;

  public TraineeAssessmentServiceImpl(
      TraineeAssessmentRepository traineeAssessmentRepository,
      AssessmentRepository assessmentRepository,
      TrainingEnrollmentRepository trainingEnrollmentRepository,
      TraineeRepository traineeRepository,
      UserRepository userRepository,
      AssessmentResultRepository assessmentResultRepository,
      TraineeAssessmentMapper traineeAssessmentMapper) {
    this.traineeAssessmentRepository = traineeAssessmentRepository;
    this.assessmentRepository = assessmentRepository;
    this.trainingEnrollmentRepository = trainingEnrollmentRepository;
    this.traineeRepository = traineeRepository;
    this.userRepository = userRepository;
    this.assessmentResultRepository = assessmentResultRepository;
    this.traineeAssessmentMapper = traineeAssessmentMapper;
  }

  @Override
  public TraineeAssessmentResponse getById(Long id) {
    if (id == null) {
      throw new BadRequestException("Trainee assessment ID is required");
    }
    TraineeAssessment entity = traineeAssessmentRepository.findById(id)
        .orElseThrow(() -> new ResourceNotFoundException("TraineeAssessment", "id"));
    return traineeAssessmentMapper.toResponse(entity);
  }

  @Override
  public TraineeAssessmentResponse getByAssessmentAndTraineeAndAttempt(
      Long assessmentId, Long traineeId, Integer attemptNumber) {
    if (assessmentId == null) {
      throw new BadRequestException("Assessment ID is required");
    }
    if (traineeId == null) {
      throw new BadRequestException("Trainee ID is required");
    }
    if (attemptNumber == null) {
      throw new BadRequestException("Attempt number is required");
    }
    TraineeAssessment entity = traineeAssessmentRepository
        .findByAssessmentIdAndTraineeIdAndAttemptNumber(assessmentId, traineeId, attemptNumber)
        .orElseThrow(() -> new ResourceNotFoundException("TraineeAssessment", "attempt"));
    return traineeAssessmentMapper.toResponse(entity);
  }

  @Override
  public List<TraineeAssessmentResponse> getAllTraineeAssessments() {
    return traineeAssessmentRepository.findAll().stream()
        .map(traineeAssessmentMapper::toResponse)
        .toList();
  }

  @Override
  public List<TraineeAssessmentResponse> getByAssessmentId(Long assessmentId) {
    if (assessmentId == null) {
      throw new BadRequestException("Assessment ID is required");
    }
    return traineeAssessmentRepository.findByAssessmentId(assessmentId).stream()
        .map(traineeAssessmentMapper::toResponse)
        .toList();
  }

  @Override
  public List<TraineeAssessmentResponse> getByEnrollmentId(Long enrollmentId) {
    if (enrollmentId == null) {
      throw new BadRequestException("Enrollment ID is required");
    }
    return traineeAssessmentRepository.findByTrainingEnrollmentId(enrollmentId).stream()
        .map(traineeAssessmentMapper::toResponse)
        .toList();
  }

  @Override
  public List<TraineeAssessmentResponse> getByTraineeId(Long traineeId) {
    if (traineeId == null) {
      throw new BadRequestException("Trainee ID is required");
    }
    return traineeAssessmentRepository.findByTraineeId(traineeId).stream()
        .map(traineeAssessmentMapper::toResponse)
        .toList();
  }

  @Override
  public List<TraineeAssessmentResponse> getByAssessmentDate(LocalDate assessmentDate) {
    if (assessmentDate == null) {
      throw new BadRequestException("Assessment date is required");
    }
    return traineeAssessmentRepository.findByAssessmentDate(assessmentDate).stream()
        .map(traineeAssessmentMapper::toResponse)
        .toList();
  }

  @Override
  @Transactional
  public TraineeAssessmentResponse createTraineeAssessment(CreateTraineeAssessmentRequest request) {
    if (request == null) {
      throw new BadRequestException("Trainee assessment creation request cannot be null");
    }
    if (request.getAssessmentId() == null) {
      throw new BadRequestException("Assessment ID is required");
    }
    if (request.getEnrollmentId() == null) {
      throw new BadRequestException("Enrollment ID is required");
    }
    if (request.getTraineeId() == null) {
      throw new BadRequestException("Trainee ID is required");
    }
    if (request.getAssessmentDate() == null) {
      throw new BadRequestException("Assessment date is required");
    }

    int attemptNumber = request.getAttemptNumber() != null ? request.getAttemptNumber() : 1;
    if (attemptNumber < 1) {
      throw new BadRequestException("Attempt number must be at least 1", "INVALID_ATTEMPT_NUMBER");
    }

    Assessment assessment = assessmentRepository.findById(request.getAssessmentId())
        .orElseThrow(() -> new ResourceNotFoundException("Assessment", "assessmentId"));

    TrainingEnrollment enrollment = trainingEnrollmentRepository.findById(request.getEnrollmentId())
        .orElseThrow(() -> new ResourceNotFoundException("TrainingEnrollment", "enrollmentId"));

    if (enrollment.getDeletedAt() != null) {
      throw new BadRequestException("Training enrollment is not active", "INACTIVE_ENROLLMENT");
    }

    Trainee trainee = traineeRepository.findById(request.getTraineeId())
        .orElseThrow(() -> new ResourceNotFoundException("Trainee", "traineeId"));

    if (enrollment.getTrainee() != null && !enrollment.getTrainee().getId().equals(trainee.getId())) {
      throw new BadRequestException("Trainee ID does not match enrollment trainee", "TRAINEE_ENROLLMENT_MISMATCH");
    }

    if (enrollment.getTrainingBatch() != null && assessment.getTrainingBatch() != null
        && !enrollment.getTrainingBatch().getId().equals(assessment.getTrainingBatch().getId())) {
      throw new BadRequestException("Enrollment batch does not match assessment batch", "BATCH_ENROLLMENT_MISMATCH");
    }

    if (enrollment.getCourse() != null && assessment.getCourse() != null
        && !enrollment.getCourse().getId().equals(assessment.getCourse().getId())) {
      throw new BadRequestException("Enrollment course does not match assessment course", "COURSE_ENROLLMENT_MISMATCH");
    }

    if (enrollment.getProgram() != null && assessment.getProgram() != null
        && !enrollment.getProgram().getId().equals(assessment.getProgram().getId())) {
      throw new BadRequestException("Enrollment program does not match assessment program", "PROGRAM_ENROLLMENT_MISMATCH");
    }

    if (traineeAssessmentRepository.existsByAssessmentIdAndTraineeIdAndAttemptNumber(
        assessment.getId(), trainee.getId(), attemptNumber)) {
      throw new ConflictException("Trainee assessment attempt already exists", "TRAINEE_ASSESSMENT_ATTEMPT_ALREADY_EXISTS");
    }

    if (request.getEvaluatorUserId() != null) {
      if (!userRepository.existsById(request.getEvaluatorUserId())) {
        throw new ResourceNotFoundException("User", "evaluatorUserId");
      }
    }

    TraineeAssessment entity = traineeAssessmentMapper.toEntity(request, assessment, enrollment, trainee);
    entity.setAttemptNumber(attemptNumber);
    if (request.getEvaluatorName() != null) {
      entity.setEvaluatorName(request.getEvaluatorName().trim());
    }

    LocalDateTime now = LocalDateTime.now();
    entity.setCreatedAt(now);
    entity.setUpdatedAt(now);

    TraineeAssessment saved = traineeAssessmentRepository.save(entity);
    return traineeAssessmentMapper.toResponse(saved);
  }

  @Override
  @Transactional
  public TraineeAssessmentResponse updateTraineeAssessment(Long id, UpdateTraineeAssessmentRequest request) {
    if (id == null) {
      throw new BadRequestException("Trainee assessment ID is required");
    }
    if (request == null) {
      throw new BadRequestException("Trainee assessment update request cannot be null");
    }

    TraineeAssessment entity = traineeAssessmentRepository.findById(id)
        .orElseThrow(() -> new ResourceNotFoundException("TraineeAssessment", "id"));

    if (request.getAppearedFlag() != null) {
      entity.setAppearedFlag(request.getAppearedFlag());
    }

    if (request.getAssessmentDate() != null) {
      entity.setAssessmentDate(request.getAssessmentDate());
    }

    if (request.getEvaluatorUserId() != null) {
      if (!userRepository.existsById(request.getEvaluatorUserId())) {
        throw new ResourceNotFoundException("User", "evaluatorUserId");
      }
      entity.setEvaluatorUserId(request.getEvaluatorUserId());
    }

    if (request.getEvaluatorName() != null) {
      entity.setEvaluatorName(request.getEvaluatorName().trim());
    }

    LocalDateTime now = LocalDateTime.now();
    entity.setUpdatedAt(now);

    TraineeAssessment saved = traineeAssessmentRepository.save(entity);
    return traineeAssessmentMapper.toResponse(saved);
  }

  @Override
  @Transactional
  public void deleteTraineeAssessment(Long id) {
    if (id == null) {
      throw new BadRequestException("Trainee assessment ID is required");
    }
    TraineeAssessment entity = traineeAssessmentRepository.findById(id)
        .orElseThrow(() -> new ResourceNotFoundException("TraineeAssessment", "id"));

    if (assessmentResultRepository.existsByTraineeAssessmentId(id)) {
      throw new ConflictException("Cannot delete trainee assessment because assessment results exist", "TRAINEE_ASSESSMENT_HAS_RESULTS");
    }

    traineeAssessmentRepository.delete(entity);
  }
}
