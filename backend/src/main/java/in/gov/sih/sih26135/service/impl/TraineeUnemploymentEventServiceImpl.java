package in.gov.sih.sih26135.service.impl;

import in.gov.sih.sih26135.dto.request.CreateTraineeUnemploymentEventRequest;
import in.gov.sih.sih26135.dto.request.UpdateTraineeUnemploymentEventRequest;
import in.gov.sih.sih26135.dto.response.TraineeUnemploymentEventResponse;
import in.gov.sih.sih26135.entity.EmploymentExitEvent;
import in.gov.sih.sih26135.entity.EmploymentRecord;
import in.gov.sih.sih26135.entity.FollowupTask;
import in.gov.sih.sih26135.entity.PlacementRecord;
import in.gov.sih.sih26135.entity.RefEmploymentInfoSource;
import in.gov.sih.sih26135.entity.RefRecordVerificationStatus;
import in.gov.sih.sih26135.entity.RefUnemploymentReason;
import in.gov.sih.sih26135.entity.SurveyResponse;
import in.gov.sih.sih26135.entity.Trainee;
import in.gov.sih.sih26135.entity.TraineeUnemploymentEvent;
import in.gov.sih.sih26135.entity.TrainingEnrollment;
import in.gov.sih.sih26135.exception.BadRequestException;
import in.gov.sih.sih26135.exception.ConflictException;
import in.gov.sih.sih26135.exception.ResourceNotFoundException;
import in.gov.sih.sih26135.mapper.TraineeUnemploymentEventMapper;
import in.gov.sih.sih26135.repository.EmploymentExitEventRepository;
import in.gov.sih.sih26135.repository.EmploymentRecordRepository;
import in.gov.sih.sih26135.repository.FollowupTaskRepository;
import in.gov.sih.sih26135.repository.PlacementRecordRepository;
import in.gov.sih.sih26135.repository.RefEmploymentInfoSourceRepository;
import in.gov.sih.sih26135.repository.RefRecordVerificationStatusRepository;
import in.gov.sih.sih26135.repository.RefUnemploymentReasonRepository;
import in.gov.sih.sih26135.repository.SurveyResponseRepository;
import in.gov.sih.sih26135.repository.TraineeRepository;
import in.gov.sih.sih26135.repository.TraineeUnemploymentEventRepository;
import in.gov.sih.sih26135.repository.TrainingEnrollmentRepository;
import in.gov.sih.sih26135.repository.UserRepository;
import in.gov.sih.sih26135.service.TraineeUnemploymentEventService;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class TraineeUnemploymentEventServiceImpl implements TraineeUnemploymentEventService {

  private final TraineeUnemploymentEventRepository traineeUnemploymentEventRepository;
  private final TraineeRepository traineeRepository;
  private final RefUnemploymentReasonRepository refUnemploymentReasonRepository;
  private final EmploymentRecordRepository employmentRecordRepository;
  private final EmploymentExitEventRepository employmentExitEventRepository;
  private final TrainingEnrollmentRepository trainingEnrollmentRepository;
  private final PlacementRecordRepository placementRecordRepository;
  private final FollowupTaskRepository followupTaskRepository;
  private final SurveyResponseRepository surveyResponseRepository;
  private final RefEmploymentInfoSourceRepository refEmploymentInfoSourceRepository;
  private final RefRecordVerificationStatusRepository refRecordVerificationStatusRepository;
  private final UserRepository userRepository;
  private final TraineeUnemploymentEventMapper mapper;

  public TraineeUnemploymentEventServiceImpl(
      TraineeUnemploymentEventRepository traineeUnemploymentEventRepository,
      TraineeRepository traineeRepository,
      RefUnemploymentReasonRepository refUnemploymentReasonRepository,
      EmploymentRecordRepository employmentRecordRepository,
      EmploymentExitEventRepository employmentExitEventRepository,
      TrainingEnrollmentRepository trainingEnrollmentRepository,
      PlacementRecordRepository placementRecordRepository,
      FollowupTaskRepository followupTaskRepository,
      SurveyResponseRepository surveyResponseRepository,
      RefEmploymentInfoSourceRepository refEmploymentInfoSourceRepository,
      RefRecordVerificationStatusRepository refRecordVerificationStatusRepository,
      UserRepository userRepository,
      TraineeUnemploymentEventMapper mapper) {
    this.traineeUnemploymentEventRepository = traineeUnemploymentEventRepository;
    this.traineeRepository = traineeRepository;
    this.refUnemploymentReasonRepository = refUnemploymentReasonRepository;
    this.employmentRecordRepository = employmentRecordRepository;
    this.employmentExitEventRepository = employmentExitEventRepository;
    this.trainingEnrollmentRepository = trainingEnrollmentRepository;
    this.placementRecordRepository = placementRecordRepository;
    this.followupTaskRepository = followupTaskRepository;
    this.surveyResponseRepository = surveyResponseRepository;
    this.refEmploymentInfoSourceRepository = refEmploymentInfoSourceRepository;
    this.refRecordVerificationStatusRepository = refRecordVerificationStatusRepository;
    this.userRepository = userRepository;
    this.mapper = mapper;
  }

  @Override
  @Transactional
  public TraineeUnemploymentEventResponse createUnemploymentEvent(CreateTraineeUnemploymentEventRequest request) {
    if (request == null) {
      throw new BadRequestException("Request body cannot be null", "REQUEST_BODY_NULL");
    }
    if (request.getTraineeId() == null) {
      throw new BadRequestException("Trainee ID is required", "TRAINEE_ID_REQUIRED");
    }

    Trainee trainee = traineeRepository.findById(request.getTraineeId())
        .orElseThrow(() -> new ResourceNotFoundException("Trainee", "traineeId"));
    if (trainee.getDeletedAt() != null) {
      throw new BadRequestException("Cannot create unemployment event for soft-deleted trainee", "TRAINEE_DELETED");
    }

    if (request.getStartDate() == null) {
      throw new BadRequestException("Start date is required", "START_DATE_REQUIRED");
    }

    // Unique start date per trainee
    if (traineeUnemploymentEventRepository.findByTraineeIdAndStartDate(trainee.getId(), request.getStartDate()).isPresent()) {
      throw new ConflictException(
          "An unemployment event already exists for trainee on start date",
          "UNEMPLOYMENT_START_DATE_EXISTS"
      );
    }

    // Period number computation and validation
    Integer periodNumber = request.getPeriodNumber();
    if (periodNumber == null) {
      List<TraineeUnemploymentEvent> existingPeriods = traineeUnemploymentEventRepository.findByTraineeId(trainee.getId());
      periodNumber = existingPeriods.stream()
          .mapToInt(TraineeUnemploymentEvent::getPeriodNumber)
          .max()
          .orElse(0) + 1;
    } else {
      if (periodNumber < 1) {
        throw new BadRequestException("Period number must be greater than or equal to 1", "PERIOD_NUMBER_INVALID");
      }
      if (traineeUnemploymentEventRepository.existsByTraineeIdAndPeriodNumber(trainee.getId(), periodNumber)) {
        throw new ConflictException("Period number already exists for trainee", "UNEMPLOYMENT_PERIOD_EXISTS");
      }
    }

    // Current status and date rules
    boolean isCurrent = request.getIsCurrent() == null || Boolean.TRUE.equals(request.getIsCurrent());
    LocalDate endDate = request.getEndDate();

    if (isCurrent && endDate != null) {
      throw new BadRequestException("Current unemployment period cannot have an end date", "CURRENT_PERIOD_CANNOT_HAVE_END_DATE");
    }
    if (endDate != null && endDate.isBefore(request.getStartDate())) {
      throw new BadRequestException("End date cannot be before start date", "END_DATE_BEFORE_START_DATE");
    }

    // Current period management and date safety check (Clarification 1)
    if (isCurrent) {
      Optional<TraineeUnemploymentEvent> existingCurrentOpt =
          traineeUnemploymentEventRepository.findByTraineeIdAndCurrentPeriodKey(trainee.getId(), 1);
      if (existingCurrentOpt.isPresent()) {
        TraineeUnemploymentEvent existingCurrent = existingCurrentOpt.get();
        if (request.getStartDate().isBefore(existingCurrent.getStartDate())) {
          throw new BadRequestException(
              "New unemployment period cannot start before the existing current period",
              "UNEMPLOYMENT_PERIOD_START_BEFORE_CURRENT"
          );
        }
        existingCurrent.setIsCurrent(false);
        if (existingCurrent.getEndDate() == null) {
          existingCurrent.setEndDate(request.getStartDate());
        }
        traineeUnemploymentEventRepository.save(existingCurrent);
      }
    }

    // Preceding & succeeding employment
    EmploymentRecord precedingEmployment = null;
    EmploymentRecord succeedingEmployment = null;

    if (request.getPrecedingEmploymentId() != null) {
      precedingEmployment = employmentRecordRepository.findById(request.getPrecedingEmploymentId())
          .orElseThrow(() -> new ResourceNotFoundException("EmploymentRecord", "precedingEmploymentId"));
      if (precedingEmployment.getTrainee() == null || !precedingEmployment.getTrainee().getId().equals(trainee.getId())) {
        throw new BadRequestException(
            "Preceding employment does not belong to the trainee",
            "PRECEDING_EMPLOYMENT_TRAINEE_MISMATCH"
        );
      }
    }

    if (request.getSucceedingEmploymentId() != null) {
      succeedingEmployment = employmentRecordRepository.findById(request.getSucceedingEmploymentId())
          .orElseThrow(() -> new ResourceNotFoundException("EmploymentRecord", "succeedingEmploymentId"));
      if (succeedingEmployment.getTrainee() == null || !succeedingEmployment.getTrainee().getId().equals(trainee.getId())) {
        throw new BadRequestException(
            "Succeeding employment does not belong to the trainee",
            "SUCCEEDING_EMPLOYMENT_TRAINEE_MISMATCH"
        );
      }
    }

    // Distinct jobs check
    if (precedingEmployment != null && succeedingEmployment != null
        && precedingEmployment.getId().equals(succeedingEmployment.getId())) {
      throw new BadRequestException(
          "Preceding and succeeding employment cannot be the same",
          "SAME_PRECEDING_AND_SUCCEEDING_EMPLOYMENT"
      );
    }

    // Employment Exit Event linkage
    EmploymentExitEvent exitEvent = null;
    if (request.getEmploymentExitEventId() != null) {
      exitEvent = employmentExitEventRepository.findById(request.getEmploymentExitEventId())
          .orElseThrow(() -> new ResourceNotFoundException("EmploymentExitEvent", "employmentExitEventId"));
      if (exitEvent.getTrainee() == null || !exitEvent.getTrainee().getId().equals(trainee.getId())) {
        throw new BadRequestException(
            "Exit event does not belong to the trainee",
            "EXIT_EVENT_TRAINEE_MISMATCH"
        );
      }
      if (traineeUnemploymentEventRepository.existsByEmploymentExitEventId(exitEvent.getId())) {
        throw new ConflictException(
            "Unemployment event already exists for this exit event",
            "UNEMPLOYMENT_EXIT_EVENT_EXISTS"
        );
      }
      if (request.getStartDate().isBefore(exitEvent.getSeparationDate())) {
        throw new BadRequestException(
            "Unemployment start date cannot precede exit event separation date",
            "UNEMPLOYMENT_START_BEFORE_EXIT_DATE"
        );
      }
      if (precedingEmployment != null) {
        if (exitEvent.getEmploymentRecord() == null || !exitEvent.getEmploymentRecord().getId().equals(precedingEmployment.getId())) {
          throw new BadRequestException(
              "Preceding employment does not match exit event employment record",
              "EXIT_EVENT_PRECEDING_EMPLOYMENT_MISMATCH"
          );
        }
      } else {
        precedingEmployment = exitEvent.getEmploymentRecord();
      }
    }

    // Enrollment linkage
    TrainingEnrollment enrollment = null;
    if (request.getEnrollmentId() != null) {
      enrollment = trainingEnrollmentRepository.findById(request.getEnrollmentId())
          .orElseThrow(() -> new ResourceNotFoundException("TrainingEnrollment", "enrollmentId"));
      if (enrollment.getTrainee() == null || !enrollment.getTrainee().getId().equals(trainee.getId())) {
        throw new BadRequestException("Enrollment does not belong to the trainee", "ENROLLMENT_TRAINEE_MISMATCH");
      }
    }

    // Placement linkage
    PlacementRecord placementRecord = null;
    if (request.getPlacementId() != null) {
      placementRecord = placementRecordRepository.findById(request.getPlacementId())
          .orElseThrow(() -> new ResourceNotFoundException("PlacementRecord", "placementId"));
      if (placementRecord.getTrainee() == null || !placementRecord.getTrainee().getId().equals(trainee.getId())) {
        throw new BadRequestException("Placement record does not belong to the trainee", "PLACEMENT_TRAINEE_MISMATCH");
      }
    }

    // FollowupTask linkage
    FollowupTask followupTask = null;
    if (request.getFollowupTaskId() != null) {
      followupTask = followupTaskRepository.findById(request.getFollowupTaskId())
          .orElseThrow(() -> new ResourceNotFoundException("FollowupTask", "followupTaskId"));
      if (followupTask.getTrainee() == null || !followupTask.getTrainee().getId().equals(trainee.getId())) {
        throw new BadRequestException("Followup task does not belong to the trainee", "FOLLOWUP_TASK_TRAINEE_MISMATCH");
      }
    }

    // SurveyResponse linkage
    SurveyResponse surveyResponse = null;
    if (request.getSurveyResponseId() != null) {
      surveyResponse = surveyResponseRepository.findById(request.getSurveyResponseId())
          .orElseThrow(() -> new ResourceNotFoundException("SurveyResponse", "surveyResponseId"));
      if (surveyResponse.getTrainee() == null || !surveyResponse.getTrainee().getId().equals(trainee.getId())) {
        throw new BadRequestException("Survey response does not belong to the trainee", "SURVEY_RESPONSE_TRAINEE_MISMATCH");
      }
    }

    // Reference validations
    if (request.getLabourStatusId() == null) {
      throw new BadRequestException("Labour status ID is required", "LABOUR_STATUS_REQUIRED");
    }

    if (request.getUnemploymentReasonId() == null) {
      throw new BadRequestException("Unemployment reason ID is required", "UNEMPLOYMENT_REASON_REQUIRED");
    }
    RefUnemploymentReason unemploymentReason = refUnemploymentReasonRepository.findById(request.getUnemploymentReasonId())
        .orElseThrow(() -> new ResourceNotFoundException("RefUnemploymentReason", "unemploymentReasonId"));

    if (request.getEmploymentInfoSourceId() == null) {
      throw new BadRequestException("Employment info source ID is required", "INFO_SOURCE_REQUIRED");
    }
    RefEmploymentInfoSource infoSource = refEmploymentInfoSourceRepository.findById(request.getEmploymentInfoSourceId())
        .orElseThrow(() -> new ResourceNotFoundException("RefEmploymentInfoSource", "employmentInfoSourceId"));

    if (request.getRecordVerificationStatusId() == null) {
      throw new BadRequestException("Record verification status ID is required", "VERIFICATION_STATUS_REQUIRED");
    }
    RefRecordVerificationStatus verificationStatus = refRecordVerificationStatusRepository.findById(request.getRecordVerificationStatusId())
        .orElseThrow(() -> new ResourceNotFoundException("RefRecordVerificationStatus", "recordVerificationStatusId"));

    if (request.getVerifiedByUserId() != null) {
      if (!userRepository.existsById(request.getVerifiedByUserId())) {
        throw new ResourceNotFoundException("User", "verifiedByUserId");
      }
    }

    TraineeUnemploymentEvent event = new TraineeUnemploymentEvent(
        trainee,
        periodNumber,
        request.getStartDate(),
        request.getLabourStatusId(),
        unemploymentReason,
        infoSource,
        verificationStatus
    );
    event.setEndDate(endDate);
    event.setIsCurrent(isCurrent);
    event.setPrecedingEmployment(precedingEmployment);
    event.setEmploymentExitEvent(exitEvent);
    event.setSucceedingEmployment(succeedingEmployment);
    event.setEnrollment(enrollment);
    event.setPlacementRecord(placementRecord);
    event.setFollowupTask(followupTask);
    event.setSurveyResponse(surveyResponse);
    event.setVerifiedAt(request.getVerifiedAt());
    event.setVerifiedByUserId(request.getVerifiedByUserId());
    event.setRemarks(request.getRemarks());

    TraineeUnemploymentEvent saved = traineeUnemploymentEventRepository.save(event);
    return mapper.toResponse(saved);
  }

  @Override
  public TraineeUnemploymentEventResponse getUnemploymentEventById(Long id) {
    if (id == null) {
      throw new BadRequestException("Unemployment event ID is required");
    }
    TraineeUnemploymentEvent entity = traineeUnemploymentEventRepository.findById(id)
        .orElseThrow(() -> new ResourceNotFoundException("TraineeUnemploymentEvent", "id"));
    return mapper.toResponse(entity);
  }

  @Override
  public TraineeUnemploymentEventResponse getUnemploymentEventByTraineeIdAndPeriodNumber(Long traineeId, Integer periodNumber) {
    if (traineeId == null) {
      throw new BadRequestException("Trainee ID is required");
    }
    if (periodNumber == null) {
      throw new BadRequestException("Period number is required");
    }
    TraineeUnemploymentEvent entity = traineeUnemploymentEventRepository.findByTraineeIdAndPeriodNumber(traineeId, periodNumber)
        .orElseThrow(() -> new ResourceNotFoundException("TraineeUnemploymentEvent", "periodNumber"));
    return mapper.toResponse(entity);
  }

  @Override
  public List<TraineeUnemploymentEventResponse> getUnemploymentEventsByTraineeId(Long traineeId) {
    if (traineeId == null) {
      throw new BadRequestException("Trainee ID is required");
    }
    return traineeUnemploymentEventRepository.findByTraineeId(traineeId).stream()
        .map(mapper::toResponse)
        .toList();
  }

  @Override
  public List<TraineeUnemploymentEventResponse> getCurrentUnemploymentEventsByTraineeId(Long traineeId) {
    if (traineeId == null) {
      throw new BadRequestException("Trainee ID is required");
    }
    return traineeUnemploymentEventRepository.findByTraineeIdAndIsCurrentTrue(traineeId).stream()
        .map(mapper::toResponse)
        .toList();
  }

  @Override
  public Optional<TraineeUnemploymentEventResponse> getCurrentPeriodByTraineeId(Long traineeId) {
    if (traineeId == null) {
      throw new BadRequestException("Trainee ID is required");
    }
    return traineeUnemploymentEventRepository.findByTraineeIdAndCurrentPeriodKey(traineeId, 1)
        .map(mapper::toResponse);
  }

  @Override
  public Optional<TraineeUnemploymentEventResponse> getUnemploymentEventByExitEventId(Long exitEventId) {
    if (exitEventId == null) {
      throw new BadRequestException("Exit event ID is required");
    }
    return traineeUnemploymentEventRepository.findByEmploymentExitEventId(exitEventId)
        .map(mapper::toResponse);
  }

  @Override
  public List<TraineeUnemploymentEventResponse> getUnemploymentEventsByStartDate(LocalDate startDate) {
    if (startDate == null) {
      throw new BadRequestException("Start date is required");
    }
    return traineeUnemploymentEventRepository.findByStartDate(startDate).stream()
        .map(mapper::toResponse)
        .toList();
  }

  @Override
  public List<TraineeUnemploymentEventResponse> getUnemploymentEventsByReasonId(Long reasonId) {
    if (reasonId == null) {
      throw new BadRequestException("Reason ID is required");
    }
    return traineeUnemploymentEventRepository.findByUnemploymentReasonId(reasonId).stream()
        .map(mapper::toResponse)
        .toList();
  }

  @Override
  public List<TraineeUnemploymentEventResponse> getUnemploymentEventsByLabourStatusId(Long labourStatusId) {
    if (labourStatusId == null) {
      throw new BadRequestException("Labour status ID is required");
    }
    return traineeUnemploymentEventRepository.findByLabourStatusId(labourStatusId).stream()
        .map(mapper::toResponse)
        .toList();
  }

  @Override
  public List<TraineeUnemploymentEventResponse> getUnemploymentEventsByPrecedingEmploymentId(Long employmentId) {
    if (employmentId == null) {
      throw new BadRequestException("Employment ID is required");
    }
    return traineeUnemploymentEventRepository.findByPrecedingEmploymentId(employmentId).stream()
        .map(mapper::toResponse)
        .toList();
  }

  @Override
  public List<TraineeUnemploymentEventResponse> getUnemploymentEventsBySucceedingEmploymentId(Long employmentId) {
    if (employmentId == null) {
      throw new BadRequestException("Employment ID is required");
    }
    return traineeUnemploymentEventRepository.findBySucceedingEmploymentId(employmentId).stream()
        .map(mapper::toResponse)
        .toList();
  }

  @Override
  public List<TraineeUnemploymentEventResponse> getUnemploymentEventsByEnrollmentId(Long enrollmentId) {
    if (enrollmentId == null) {
      throw new BadRequestException("Enrollment ID is required");
    }
    return traineeUnemploymentEventRepository.findByEnrollmentId(enrollmentId).stream()
        .map(mapper::toResponse)
        .toList();
  }

  @Override
  public List<TraineeUnemploymentEventResponse> getUnemploymentEventsByPlacementId(Long placementId) {
    if (placementId == null) {
      throw new BadRequestException("Placement ID is required");
    }
    return traineeUnemploymentEventRepository.findByPlacementRecordId(placementId).stream()
        .map(mapper::toResponse)
        .toList();
  }

  @Override
  public List<TraineeUnemploymentEventResponse> getUnemploymentEventsByFollowupTaskId(Long followupTaskId) {
    if (followupTaskId == null) {
      throw new BadRequestException("Followup task ID is required");
    }
    return traineeUnemploymentEventRepository.findByFollowupTaskId(followupTaskId).stream()
        .map(mapper::toResponse)
        .toList();
  }

  @Override
  public List<TraineeUnemploymentEventResponse> getUnemploymentEventsBySurveyResponseId(Long surveyResponseId) {
    if (surveyResponseId == null) {
      throw new BadRequestException("Survey response ID is required");
    }
    return traineeUnemploymentEventRepository.findBySurveyResponseId(surveyResponseId).stream()
        .map(mapper::toResponse)
        .toList();
  }

  @Override
  public List<TraineeUnemploymentEventResponse> getUnemploymentEventsByVerificationStatusId(Long statusId) {
    if (statusId == null) {
      throw new BadRequestException("Verification status ID is required");
    }
    return traineeUnemploymentEventRepository.findByRecordVerificationStatusId(statusId).stream()
        .map(mapper::toResponse)
        .toList();
  }

  @Override
  @Transactional
  public TraineeUnemploymentEventResponse updateUnemploymentEvent(Long id, UpdateTraineeUnemploymentEventRequest request) {
    if (id == null) {
      throw new BadRequestException("Unemployment event ID is required", "ID_REQUIRED");
    }
    if (request == null) {
      throw new BadRequestException("Request body cannot be null", "REQUEST_BODY_NULL");
    }

    TraineeUnemploymentEvent event = traineeUnemploymentEventRepository.findById(id)
        .orElseThrow(() -> new ResourceNotFoundException("TraineeUnemploymentEvent", "id"));

    Trainee trainee = event.getTrainee();

    // Preserve period number unless explicitly supported
    if (request.getPeriodNumber() != null && !request.getPeriodNumber().equals(event.getPeriodNumber())) {
      if (request.getPeriodNumber() < 1) {
        throw new BadRequestException("Period number must be greater than or equal to 1", "PERIOD_NUMBER_INVALID");
      }
      Optional<TraineeUnemploymentEvent> conflict =
          traineeUnemploymentEventRepository.findByTraineeIdAndPeriodNumber(trainee.getId(), request.getPeriodNumber());
      if (conflict.isPresent() && !conflict.get().getId().equals(id)) {
        throw new ConflictException("Period number already exists for trainee", "UNEMPLOYMENT_PERIOD_EXISTS");
      }
      event.setPeriodNumber(request.getPeriodNumber());
    }

    // Revalidate changed start date (excluding current record)
    LocalDate effectiveStartDate = request.getStartDate() != null ? request.getStartDate() : event.getStartDate();
    if (request.getStartDate() != null && !request.getStartDate().equals(event.getStartDate())) {
      Optional<TraineeUnemploymentEvent> conflict =
          traineeUnemploymentEventRepository.findByTraineeIdAndStartDate(trainee.getId(), request.getStartDate());
      if (conflict.isPresent() && !conflict.get().getId().equals(id)) {
        throw new ConflictException(
            "An unemployment event already exists for trainee on start date",
            "UNEMPLOYMENT_START_DATE_EXISTS"
        );
      }
      event.setStartDate(request.getStartDate());
    }

    // Revalidate changed end date and current period invariant
    Boolean effectiveIsCurrent = request.getIsCurrent() != null ? request.getIsCurrent() : event.getIsCurrent();
    LocalDate effectiveEndDate = request.getEndDate() != null ? request.getEndDate() : event.getEndDate();

    if (Boolean.TRUE.equals(effectiveIsCurrent) && effectiveEndDate != null) {
      throw new BadRequestException("Current unemployment period cannot have an end date", "CURRENT_PERIOD_CANNOT_HAVE_END_DATE");
    }
    if (effectiveEndDate != null && effectiveEndDate.isBefore(effectiveStartDate)) {
      throw new BadRequestException("End date cannot be before start date", "END_DATE_BEFORE_START_DATE");
    }

    if (request.getEndDate() != null) {
      event.setEndDate(request.getEndDate());
    }
    if (request.getIsCurrent() != null) {
      event.setIsCurrent(request.getIsCurrent());
    }

    // Current period management and date safety check (Clarification 1)
    if (Boolean.TRUE.equals(effectiveIsCurrent)) {
      Optional<TraineeUnemploymentEvent> existingCurrentOpt =
          traineeUnemploymentEventRepository.findByTraineeIdAndCurrentPeriodKey(trainee.getId(), 1);
      if (existingCurrentOpt.isPresent() && !existingCurrentOpt.get().getId().equals(id)) {
        TraineeUnemploymentEvent existingCurrent = existingCurrentOpt.get();
        if (effectiveStartDate.isBefore(existingCurrent.getStartDate())) {
          throw new BadRequestException(
              "New unemployment period cannot start before the existing current period",
              "UNEMPLOYMENT_PERIOD_START_BEFORE_CURRENT"
          );
        }
        existingCurrent.setIsCurrent(false);
        if (existingCurrent.getEndDate() == null) {
          existingCurrent.setEndDate(effectiveStartDate);
        }
        traineeUnemploymentEventRepository.save(existingCurrent);
      }
    }

    // Exit event check and update
    if (request.getEmploymentExitEventId() != null) {
      EmploymentExitEvent exitEvent = employmentExitEventRepository.findById(request.getEmploymentExitEventId())
          .orElseThrow(() -> new ResourceNotFoundException("EmploymentExitEvent", "employmentExitEventId"));
      if (exitEvent.getTrainee() == null || !exitEvent.getTrainee().getId().equals(trainee.getId())) {
        throw new BadRequestException("Exit event does not belong to the trainee", "EXIT_EVENT_TRAINEE_MISMATCH");
      }
      Optional<TraineeUnemploymentEvent> conflict =
          traineeUnemploymentEventRepository.findByEmploymentExitEventId(exitEvent.getId());
      if (conflict.isPresent() && !conflict.get().getId().equals(id)) {
        throw new ConflictException("Unemployment event already exists for this exit event", "UNEMPLOYMENT_EXIT_EVENT_EXISTS");
      }
      if (effectiveStartDate.isBefore(exitEvent.getSeparationDate())) {
        throw new BadRequestException(
            "Unemployment start date cannot precede exit event separation date",
            "UNEMPLOYMENT_START_BEFORE_EXIT_DATE"
        );
      }
      event.setEmploymentExitEvent(exitEvent);
    }

    // Preceding and succeeding employment updates
    if (request.getPrecedingEmploymentId() != null) {
      EmploymentRecord prec = employmentRecordRepository.findById(request.getPrecedingEmploymentId())
          .orElseThrow(() -> new ResourceNotFoundException("EmploymentRecord", "precedingEmploymentId"));
      if (prec.getTrainee() == null || !prec.getTrainee().getId().equals(trainee.getId())) {
        throw new BadRequestException("Preceding employment does not belong to the trainee", "PRECEDING_EMPLOYMENT_TRAINEE_MISMATCH");
      }
      event.setPrecedingEmployment(prec);
    }

    if (request.getSucceedingEmploymentId() != null) {
      EmploymentRecord succ = employmentRecordRepository.findById(request.getSucceedingEmploymentId())
          .orElseThrow(() -> new ResourceNotFoundException("EmploymentRecord", "succeedingEmploymentId"));
      if (succ.getTrainee() == null || !succ.getTrainee().getId().equals(trainee.getId())) {
        throw new BadRequestException("Succeeding employment does not belong to the trainee", "SUCCEEDING_EMPLOYMENT_TRAINEE_MISMATCH");
      }
      event.setSucceedingEmployment(succ);
    }

    // Distinct jobs check
    Long precId = event.getPrecedingEmployment() != null ? event.getPrecedingEmployment().getId() : null;
    Long succId = event.getSucceedingEmployment() != null ? event.getSucceedingEmployment().getId() : null;
    if (precId != null && succId != null && precId.equals(succId)) {
      throw new BadRequestException(
          "Preceding and succeeding employment cannot be the same",
          "SAME_PRECEDING_AND_SUCCEEDING_EMPLOYMENT"
      );
    }

    // Enrollment, placement, followup, survey
    if (request.getEnrollmentId() != null) {
      TrainingEnrollment enrollment = trainingEnrollmentRepository.findById(request.getEnrollmentId())
          .orElseThrow(() -> new ResourceNotFoundException("TrainingEnrollment", "enrollmentId"));
      if (enrollment.getTrainee() == null || !enrollment.getTrainee().getId().equals(trainee.getId())) {
        throw new BadRequestException("Enrollment does not belong to the trainee", "ENROLLMENT_TRAINEE_MISMATCH");
      }
      event.setEnrollment(enrollment);
    }

    if (request.getPlacementId() != null) {
      PlacementRecord placement = placementRecordRepository.findById(request.getPlacementId())
          .orElseThrow(() -> new ResourceNotFoundException("PlacementRecord", "placementId"));
      if (placement.getTrainee() == null || !placement.getTrainee().getId().equals(trainee.getId())) {
        throw new BadRequestException("Placement record does not belong to the trainee", "PLACEMENT_TRAINEE_MISMATCH");
      }
      event.setPlacementRecord(placement);
    }

    if (request.getFollowupTaskId() != null) {
      FollowupTask followupTask = followupTaskRepository.findById(request.getFollowupTaskId())
          .orElseThrow(() -> new ResourceNotFoundException("FollowupTask", "followupTaskId"));
      if (followupTask.getTrainee() == null || !followupTask.getTrainee().getId().equals(trainee.getId())) {
        throw new BadRequestException("Followup task does not belong to the trainee", "FOLLOWUP_TASK_TRAINEE_MISMATCH");
      }
      event.setFollowupTask(followupTask);
    }

    if (request.getSurveyResponseId() != null) {
      SurveyResponse surveyResponse = surveyResponseRepository.findById(request.getSurveyResponseId())
          .orElseThrow(() -> new ResourceNotFoundException("SurveyResponse", "surveyResponseId"));
      if (surveyResponse.getTrainee() == null || !surveyResponse.getTrainee().getId().equals(trainee.getId())) {
        throw new BadRequestException("Survey response does not belong to the trainee", "SURVEY_RESPONSE_TRAINEE_MISMATCH");
      }
      event.setSurveyResponse(surveyResponse);
    }

    // References
    if (request.getLabourStatusId() != null) {
      event.setLabourStatusId(request.getLabourStatusId());
    }

    if (request.getUnemploymentReasonId() != null) {
      RefUnemploymentReason reason = refUnemploymentReasonRepository.findById(request.getUnemploymentReasonId())
          .orElseThrow(() -> new ResourceNotFoundException("RefUnemploymentReason", "unemploymentReasonId"));
      event.setUnemploymentReason(reason);
    }

    if (request.getEmploymentInfoSourceId() != null) {
      RefEmploymentInfoSource source = refEmploymentInfoSourceRepository.findById(request.getEmploymentInfoSourceId())
          .orElseThrow(() -> new ResourceNotFoundException("RefEmploymentInfoSource", "employmentInfoSourceId"));
      event.setEmploymentInfoSource(source);
    }

    if (request.getRecordVerificationStatusId() != null) {
      RefRecordVerificationStatus status = refRecordVerificationStatusRepository.findById(request.getRecordVerificationStatusId())
          .orElseThrow(() -> new ResourceNotFoundException("RefRecordVerificationStatus", "recordVerificationStatusId"));
      event.setRecordVerificationStatus(status);
    }

    if (request.getVerifiedByUserId() != null) {
      if (!userRepository.existsById(request.getVerifiedByUserId())) {
        throw new ResourceNotFoundException("User", "verifiedByUserId");
      }
      event.setVerifiedByUserId(request.getVerifiedByUserId());
    }

    if (request.getVerifiedAt() != null) {
      event.setVerifiedAt(request.getVerifiedAt());
    }

    if (request.getRemarks() != null) {
      event.setRemarks(request.getRemarks());
    }

    TraineeUnemploymentEvent saved = traineeUnemploymentEventRepository.save(event);
    return mapper.toResponse(saved);
  }

  @Override
  @Transactional
  public void deleteUnemploymentEvent(Long id) {
    if (id == null) {
      throw new BadRequestException("Unemployment event ID is required", "ID_REQUIRED");
    }
    TraineeUnemploymentEvent event = traineeUnemploymentEventRepository.findById(id)
        .orElseThrow(() -> new ResourceNotFoundException("TraineeUnemploymentEvent", "id"));

    // Leaf entity; physical delete permitted
    traineeUnemploymentEventRepository.delete(event);
  }
}
