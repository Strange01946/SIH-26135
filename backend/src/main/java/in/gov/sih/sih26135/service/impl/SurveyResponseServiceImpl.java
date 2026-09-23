package in.gov.sih.sih26135.service.impl;

import in.gov.sih.sih26135.dto.request.CreateSurveyResponseRequest;
import in.gov.sih.sih26135.dto.request.UpdateSurveyResponseRequest;
import in.gov.sih.sih26135.dto.response.SurveyResponseResponse;
import in.gov.sih.sih26135.entity.FollowupTask;
import in.gov.sih.sih26135.entity.SurveyResponse;
import in.gov.sih.sih26135.entity.Trainee;
import in.gov.sih.sih26135.entity.TrainingEnrollment;
import in.gov.sih.sih26135.exception.BadRequestException;
import in.gov.sih.sih26135.exception.ConflictException;
import in.gov.sih.sih26135.exception.ResourceNotFoundException;
import in.gov.sih.sih26135.mapper.SurveyResponseMapper;
import in.gov.sih.sih26135.repository.FollowupTaskRepository;
import in.gov.sih.sih26135.repository.SurveyResponseAnswerRepository;
import in.gov.sih.sih26135.repository.SurveyResponseRepository;
import in.gov.sih.sih26135.repository.TraineeRepository;
import in.gov.sih.sih26135.repository.TrainingEnrollmentRepository;
import in.gov.sih.sih26135.service.SurveyResponseService;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class SurveyResponseServiceImpl implements SurveyResponseService {

  private static final Long STATUS_SUBMITTED_ID = 3L;

  private final SurveyResponseRepository surveyResponseRepository;
  private final TraineeRepository traineeRepository;
  private final TrainingEnrollmentRepository trainingEnrollmentRepository;
  private final FollowupTaskRepository followupTaskRepository;
  private final SurveyResponseAnswerRepository surveyResponseAnswerRepository;
  private final SurveyResponseMapper surveyResponseMapper;

  public SurveyResponseServiceImpl(
      SurveyResponseRepository surveyResponseRepository,
      TraineeRepository traineeRepository,
      TrainingEnrollmentRepository trainingEnrollmentRepository,
      FollowupTaskRepository followupTaskRepository,
      SurveyResponseAnswerRepository surveyResponseAnswerRepository,
      SurveyResponseMapper surveyResponseMapper) {
    this.surveyResponseRepository = surveyResponseRepository;
    this.traineeRepository = traineeRepository;
    this.trainingEnrollmentRepository = trainingEnrollmentRepository;
    this.followupTaskRepository = followupTaskRepository;
    this.surveyResponseAnswerRepository = surveyResponseAnswerRepository;
    this.surveyResponseMapper = surveyResponseMapper;
  }

  @Override
  @Transactional
  public SurveyResponseResponse createSurveyResponse(CreateSurveyResponseRequest request) {
    if (request == null) {
      throw new BadRequestException("Survey response creation request cannot be null");
    }
    if (request.getSurveyId() == null) {
      throw new BadRequestException("Survey ID is required");
    }
    if (request.getSurveyTemplateVersionId() == null) {
      throw new BadRequestException("Survey template version ID is required");
    }
    if (request.getTraineeId() == null) {
      throw new BadRequestException("Trainee ID is required");
    }
    if (request.getSurveyResponseStatusId() == null) {
      throw new BadRequestException("Survey response status ID is required");
    }

    int attempt = request.getAttemptNumber() != null ? request.getAttemptNumber() : 1;
    if (attempt < 1) {
      throw new BadRequestException("Attempt number must be at least 1", "INVALID_ATTEMPT_NUMBER");
    }

    if (surveyResponseRepository.existsBySurveyIdAndTraineeIdAndAttemptNumber(request.getSurveyId(), request.getTraineeId(), attempt)) {
      throw new ConflictException("Survey response already exists for this survey, trainee, and attempt number", "DUPLICATE_SURVEY_RESPONSE_ATTEMPT");
    }

    Trainee trainee = traineeRepository.findById(request.getTraineeId())
        .orElseThrow(() -> new ResourceNotFoundException("Trainee", "traineeId"));

    TrainingEnrollment enrollment = null;
    if (request.getEnrollmentId() != null) {
      enrollment = trainingEnrollmentRepository.findById(request.getEnrollmentId())
          .orElseThrow(() -> new ResourceNotFoundException("TrainingEnrollment", "enrollmentId"));
      if (!enrollment.getTrainee().getId().equals(trainee.getId())) {
        throw new BadRequestException("Enrollment does not belong to the specified trainee", "TRAINEE_ENROLLMENT_MISMATCH");
      }
    }

    FollowupTask followupTask = null;
    if (request.getFollowupTaskId() != null) {
      followupTask = followupTaskRepository.findById(request.getFollowupTaskId())
          .orElseThrow(() -> new ResourceNotFoundException("FollowupTask", "followupTaskId"));
      if (!followupTask.getTrainee().getId().equals(trainee.getId())) {
        throw new BadRequestException("Followup task does not belong to the specified trainee", "TRAINEE_TASK_MISMATCH");
      }
      if (followupTask.getSurveyId() != null && !followupTask.getSurveyId().equals(request.getSurveyId())) {
        throw new BadRequestException("Followup task survey ID does not match response survey ID", "SURVEY_MISMATCH");
      }
    }

    LocalDateTime startedAt = request.getStartedAt() != null ? request.getStartedAt() : LocalDateTime.now();
    if (request.getSubmittedAt() != null && request.getSubmittedAt().isBefore(startedAt)) {
      throw new BadRequestException("Submitted timestamp cannot be before started timestamp", "INVALID_TIMESTAMP_ORDER");
    }

    SurveyResponse response = surveyResponseMapper.toEntity(request, trainee, enrollment, followupTask);
    response.setAttemptNumber(attempt);
    response.setStartedAt(startedAt);

    LocalDateTime now = LocalDateTime.now();
    response.setCreatedAt(now);
    response.setUpdatedAt(now);

    SurveyResponse saved = surveyResponseRepository.save(response);
    return surveyResponseMapper.toResponse(saved);
  }

  @Override
  @Transactional
  public SurveyResponseResponse updateSurveyResponse(Long id, UpdateSurveyResponseRequest request) {
    if (id == null) {
      throw new BadRequestException("Survey response ID is required");
    }
    if (request == null) {
      throw new BadRequestException("Update request cannot be null");
    }

    SurveyResponse response = surveyResponseRepository.findById(id)
        .orElseThrow(() -> new ResourceNotFoundException("SurveyResponse", "id"));

    if (STATUS_SUBMITTED_ID.equals(response.getSurveyResponseStatusId())) {
      throw new BadRequestException("Cannot update an already submitted survey response; create a new attempt", "SUBMITTED_RESPONSE_IMMUTABLE");
    }

    Trainee trainee = response.getTrainee();

    if (request.getEnrollmentId() != null) {
      TrainingEnrollment enrollment = trainingEnrollmentRepository.findById(request.getEnrollmentId())
          .orElseThrow(() -> new ResourceNotFoundException("TrainingEnrollment", "enrollmentId"));
      if (!enrollment.getTrainee().getId().equals(trainee.getId())) {
        throw new BadRequestException("Enrollment does not belong to the specified trainee", "TRAINEE_ENROLLMENT_MISMATCH");
      }
      response.setEnrollment(enrollment);
    }

    if (request.getFollowupTaskId() != null) {
      FollowupTask followupTask = followupTaskRepository.findById(request.getFollowupTaskId())
          .orElseThrow(() -> new ResourceNotFoundException("FollowupTask", "followupTaskId"));
      if (!followupTask.getTrainee().getId().equals(trainee.getId())) {
        throw new BadRequestException("Followup task does not belong to the specified trainee", "TRAINEE_TASK_MISMATCH");
      }
      if (followupTask.getSurveyId() != null && !followupTask.getSurveyId().equals(response.getSurveyId())) {
        throw new BadRequestException("Followup task survey ID does not match response survey ID", "SURVEY_MISMATCH");
      }
      response.setFollowupTask(followupTask);
    }

    if (request.getSurveyResponseStatusId() != null) {
      response.setSurveyResponseStatusId(request.getSurveyResponseStatusId());
      if (STATUS_SUBMITTED_ID.equals(request.getSurveyResponseStatusId()) && response.getSubmittedAt() == null) {
        response.setSubmittedAt(request.getSubmittedAt() != null ? request.getSubmittedAt() : LocalDateTime.now());
      }
    }

    if (request.getSubmittedAt() != null) {
      if (request.getSubmittedAt().isBefore(response.getStartedAt())) {
        throw new BadRequestException("Submitted timestamp cannot be before started timestamp", "INVALID_TIMESTAMP_ORDER");
      }
      response.setSubmittedAt(request.getSubmittedAt());
    }

    response.setUpdatedAt(LocalDateTime.now());
    SurveyResponse updated = surveyResponseRepository.save(response);
    return surveyResponseMapper.toResponse(updated);
  }

  @Override
  public SurveyResponseResponse getSurveyResponseById(Long id) {
    if (id == null) {
      throw new BadRequestException("Survey response ID is required");
    }
    SurveyResponse response = surveyResponseRepository.findById(id)
        .orElseThrow(() -> new ResourceNotFoundException("SurveyResponse", "id"));
    return surveyResponseMapper.toResponse(response);
  }

  @Override
  public SurveyResponseResponse getSurveyResponseByAttempt(Long surveyId, Long traineeId, Integer attemptNumber) {
    if (surveyId == null) {
      throw new BadRequestException("Survey ID is required");
    }
    if (traineeId == null) {
      throw new BadRequestException("Trainee ID is required");
    }
    int attempt = attemptNumber != null ? attemptNumber : 1;
    SurveyResponse response = surveyResponseRepository.findBySurveyIdAndTraineeIdAndAttemptNumber(surveyId, traineeId, attempt)
        .orElseThrow(() -> new ResourceNotFoundException("SurveyResponse", "surveyId, traineeId, attemptNumber"));
    return surveyResponseMapper.toResponse(response);
  }

  @Override
  public List<SurveyResponseResponse> getResponsesBySurvey(Long surveyId) {
    if (surveyId == null) {
      throw new BadRequestException("Survey ID is required");
    }
    return surveyResponseRepository.findBySurveyId(surveyId).stream()
        .map(surveyResponseMapper::toResponse)
        .toList();
  }

  @Override
  public List<SurveyResponseResponse> getResponsesByTrainee(Long traineeId) {
    if (traineeId == null) {
      throw new BadRequestException("Trainee ID is required");
    }
    return surveyResponseRepository.findByTraineeId(traineeId).stream()
        .map(surveyResponseMapper::toResponse)
        .toList();
  }

  @Override
  public List<SurveyResponseResponse> getResponsesByFollowupTask(Long followupTaskId) {
    if (followupTaskId == null) {
      throw new BadRequestException("Followup task ID is required");
    }
    return surveyResponseRepository.findByFollowupTaskId(followupTaskId).stream()
        .map(surveyResponseMapper::toResponse)
        .toList();
  }

  @Override
  public List<SurveyResponseResponse> getResponsesByEnrollment(Long enrollmentId) {
    if (enrollmentId == null) {
      throw new BadRequestException("Enrollment ID is required");
    }
    return surveyResponseRepository.findByEnrollmentId(enrollmentId).stream()
        .map(surveyResponseMapper::toResponse)
        .toList();
  }

  @Override
  public List<SurveyResponseResponse> getResponsesByStatus(Long statusId) {
    if (statusId == null) {
      throw new BadRequestException("Survey response status ID is required");
    }
    return surveyResponseRepository.findBySurveyResponseStatusId(statusId).stream()
        .map(surveyResponseMapper::toResponse)
        .toList();
  }

  @Override
  public List<SurveyResponseResponse> getResponsesByTemplateVersion(Long versionId) {
    if (versionId == null) {
      throw new BadRequestException("Template version ID is required");
    }
    return surveyResponseRepository.findBySurveyTemplateVersionId(versionId).stream()
        .map(surveyResponseMapper::toResponse)
        .toList();
  }

  @Override
  @Transactional
  public void deleteSurveyResponse(Long id) {
    if (id == null) {
      throw new BadRequestException("Survey response ID is required");
    }
    SurveyResponse response = surveyResponseRepository.findById(id)
        .orElseThrow(() -> new ResourceNotFoundException("SurveyResponse", "id"));

    if (STATUS_SUBMITTED_ID.equals(response.getSurveyResponseStatusId())) {
      throw new ConflictException("Cannot delete a submitted survey response", "CANNOT_DELETE_SUBMITTED_RESPONSE");
    }

    if (!surveyResponseAnswerRepository.findBySurveyResponseId(id).isEmpty()) {
      throw new ConflictException("Cannot delete survey response with associated answers", "RESPONSE_HAS_ANSWERS");
    }

    surveyResponseRepository.delete(response);
  }
}
