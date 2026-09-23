package in.gov.sih.sih26135.service.impl;

import in.gov.sih.sih26135.dto.request.CreateEmploymentExitEventRequest;
import in.gov.sih.sih26135.dto.request.UpdateEmploymentExitEventRequest;
import in.gov.sih.sih26135.dto.response.EmploymentExitEventResponse;
import in.gov.sih.sih26135.entity.EmploymentExitEvent;
import in.gov.sih.sih26135.entity.EmploymentRecord;
import in.gov.sih.sih26135.entity.RefEmploymentExitReason;
import in.gov.sih.sih26135.entity.RefEmploymentInfoSource;
import in.gov.sih.sih26135.entity.RefRecordVerificationStatus;
import in.gov.sih.sih26135.entity.RefSeparationNature;
import in.gov.sih.sih26135.entity.Trainee;
import in.gov.sih.sih26135.entity.TrainingEnrollment;
import in.gov.sih.sih26135.exception.BadRequestException;
import in.gov.sih.sih26135.exception.ConflictException;
import in.gov.sih.sih26135.exception.ResourceNotFoundException;
import in.gov.sih.sih26135.mapper.EmploymentExitEventMapper;
import in.gov.sih.sih26135.repository.EmploymentExitEventRepository;
import in.gov.sih.sih26135.repository.EmploymentRecordRepository;
import in.gov.sih.sih26135.repository.RefEmploymentExitReasonRepository;
import in.gov.sih.sih26135.repository.RefEmploymentInfoSourceRepository;
import in.gov.sih.sih26135.repository.RefRecordVerificationStatusRepository;
import in.gov.sih.sih26135.repository.RefSeparationNatureRepository;
import in.gov.sih.sih26135.repository.TraineeRepository;
import in.gov.sih.sih26135.repository.TraineeUnemploymentEventRepository;
import in.gov.sih.sih26135.repository.TrainingEnrollmentRepository;
import in.gov.sih.sih26135.repository.UserRepository;
import in.gov.sih.sih26135.service.EmploymentExitEventService;
import java.time.LocalDate;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class EmploymentExitEventServiceImpl implements EmploymentExitEventService {

  private final EmploymentExitEventRepository employmentExitEventRepository;
  private final EmploymentRecordRepository employmentRecordRepository;
  private final TraineeRepository traineeRepository;
  private final TrainingEnrollmentRepository trainingEnrollmentRepository;
  private final RefEmploymentExitReasonRepository refEmploymentExitReasonRepository;
  private final RefSeparationNatureRepository refSeparationNatureRepository;
  private final RefEmploymentInfoSourceRepository refEmploymentInfoSourceRepository;
  private final RefRecordVerificationStatusRepository refRecordVerificationStatusRepository;
  private final UserRepository userRepository;
  private final TraineeUnemploymentEventRepository traineeUnemploymentEventRepository;
  private final EmploymentExitEventMapper mapper;

  public EmploymentExitEventServiceImpl(
      EmploymentExitEventRepository employmentExitEventRepository,
      EmploymentRecordRepository employmentRecordRepository,
      TraineeRepository traineeRepository,
      TrainingEnrollmentRepository trainingEnrollmentRepository,
      RefEmploymentExitReasonRepository refEmploymentExitReasonRepository,
      RefSeparationNatureRepository refSeparationNatureRepository,
      RefEmploymentInfoSourceRepository refEmploymentInfoSourceRepository,
      RefRecordVerificationStatusRepository refRecordVerificationStatusRepository,
      UserRepository userRepository,
      TraineeUnemploymentEventRepository traineeUnemploymentEventRepository,
      EmploymentExitEventMapper mapper) {
    this.employmentExitEventRepository = employmentExitEventRepository;
    this.employmentRecordRepository = employmentRecordRepository;
    this.traineeRepository = traineeRepository;
    this.trainingEnrollmentRepository = trainingEnrollmentRepository;
    this.refEmploymentExitReasonRepository = refEmploymentExitReasonRepository;
    this.refSeparationNatureRepository = refSeparationNatureRepository;
    this.refEmploymentInfoSourceRepository = refEmploymentInfoSourceRepository;
    this.refRecordVerificationStatusRepository = refRecordVerificationStatusRepository;
    this.userRepository = userRepository;
    this.traineeUnemploymentEventRepository = traineeUnemploymentEventRepository;
    this.mapper = mapper;
  }

  @Override
  @Transactional
  public EmploymentExitEventResponse createExitEvent(CreateEmploymentExitEventRequest request) {
    if (request == null) {
      throw new BadRequestException("Request body cannot be null", "REQUEST_BODY_NULL");
    }
    if (request.getEmploymentId() == null) {
      throw new BadRequestException("Employment ID is required", "EMPLOYMENT_ID_REQUIRED");
    }

    if (employmentExitEventRepository.existsByEmploymentRecordId(request.getEmploymentId())) {
      throw new ConflictException(
          "An exit event already exists for employment record " + request.getEmploymentId(),
          "EXIT_EVENT_ALREADY_EXISTS"
      );
    }

    EmploymentRecord employmentRecord = employmentRecordRepository.findById(request.getEmploymentId())
        .orElseThrow(() -> new ResourceNotFoundException("EmploymentRecord", "employmentId"));

    if (employmentRecord.getDeletedAt() != null) {
      throw new BadRequestException(
          "Cannot record exit event for soft-deleted employment record",
          "EMPLOYMENT_RECORD_DELETED"
      );
    }

    Trainee trainee = employmentRecord.getTrainee();
    if (trainee == null) {
      throw new BadRequestException("Employment record has no associated trainee", "EMPLOYMENT_TRAINEE_MISSING");
    }
    if (trainee.getDeletedAt() != null) {
      throw new BadRequestException("Cannot record exit event for soft-deleted trainee", "TRAINEE_DELETED");
    }
    if (request.getTraineeId() != null && !trainee.getId().equals(request.getTraineeId())) {
      throw new BadRequestException("Trainee ID does not match employment record trainee", "EXIT_EVENT_TRAINEE_MISMATCH");
    }

    if (request.getSeparationDate() == null) {
      throw new BadRequestException("Separation date is required", "SEPARATION_DATE_REQUIRED");
    }
    if (employmentRecord.getStartDate() != null && request.getSeparationDate().isBefore(employmentRecord.getStartDate())) {
      throw new BadRequestException(
          "Separation date cannot be before employment start date",
          "SEPARATION_DATE_BEFORE_START_DATE"
      );
    }

    TrainingEnrollment enrollment = null;
    if (request.getEnrollmentId() != null) {
      enrollment = trainingEnrollmentRepository.findById(request.getEnrollmentId())
          .orElseThrow(() -> new ResourceNotFoundException("TrainingEnrollment", "enrollmentId"));
      if (enrollment.getTrainee() == null || !enrollment.getTrainee().getId().equals(trainee.getId())) {
        throw new BadRequestException(
            "Enrollment does not belong to the employment trainee",
            "EXIT_EVENT_ENROLLMENT_TRAINEE_MISMATCH"
        );
      }
      if (employmentRecord.getEnrollment() != null && !employmentRecord.getEnrollment().getId().equals(enrollment.getId())) {
        throw new BadRequestException(
            "Enrollment does not match employment record enrollment",
            "EXIT_EVENT_ENROLLMENT_MISMATCH"
        );
      }
    } else if (employmentRecord.getEnrollment() != null) {
      enrollment = employmentRecord.getEnrollment();
    }

    if (request.getEmploymentExitReasonId() == null) {
      throw new BadRequestException("Employment exit reason ID is required", "EXIT_REASON_REQUIRED");
    }
    RefEmploymentExitReason exitReason = refEmploymentExitReasonRepository.findById(request.getEmploymentExitReasonId())
        .orElseThrow(() -> new ResourceNotFoundException("RefEmploymentExitReason", "employmentExitReasonId"));

    if (request.getSeparationNatureId() == null) {
      throw new BadRequestException("Separation nature ID is required", "SEPARATION_NATURE_REQUIRED");
    }
    RefSeparationNature separationNature = refSeparationNatureRepository.findById(request.getSeparationNatureId())
        .orElseThrow(() -> new ResourceNotFoundException("RefSeparationNature", "separationNatureId"));

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

    EmploymentExitEvent exitEvent = new EmploymentExitEvent(
        employmentRecord,
        trainee,
        request.getSeparationDate(),
        exitReason,
        separationNature,
        infoSource,
        verificationStatus
    );
    exitEvent.setEnrollment(enrollment);
    exitEvent.setVerifiedAt(request.getVerifiedAt());
    exitEvent.setVerifiedByUserId(request.getVerifiedByUserId());
    exitEvent.setRemarks(request.getRemarks());

    EmploymentExitEvent saved = employmentExitEventRepository.save(exitEvent);

    // Spell synchronization on create
    employmentRecord.setEndDate(request.getSeparationDate());
    employmentRecord.setIsCurrent(false);
    employmentRecord.setEmploymentExitReason(exitReason);
    if (request.getRemarks() != null && !request.getRemarks().isBlank()
        && (employmentRecord.getExitRemarks() == null || employmentRecord.getExitRemarks().isBlank())) {
      employmentRecord.setExitRemarks(request.getRemarks());
    }
    employmentRecordRepository.save(employmentRecord);

    return mapper.toResponse(saved);
  }

  @Override
  public EmploymentExitEventResponse getExitEventById(Long id) {
    if (id == null) {
      throw new BadRequestException("Employment exit event ID is required");
    }
    EmploymentExitEvent entity = employmentExitEventRepository.findById(id)
        .orElseThrow(() -> new ResourceNotFoundException("EmploymentExitEvent", "id"));
    return mapper.toResponse(entity);
  }

  @Override
  public EmploymentExitEventResponse getExitEventByEmploymentId(Long employmentId) {
    if (employmentId == null) {
      throw new BadRequestException("Employment ID is required");
    }
    EmploymentExitEvent entity = employmentExitEventRepository.findByEmploymentRecordId(employmentId)
        .orElseThrow(() -> new ResourceNotFoundException("EmploymentExitEvent", "employmentId"));
    return mapper.toResponse(entity);
  }

  @Override
  public List<EmploymentExitEventResponse> getExitEventsByTraineeId(Long traineeId) {
    if (traineeId == null) {
      throw new BadRequestException("Trainee ID is required");
    }
    return employmentExitEventRepository.findByTraineeId(traineeId).stream()
        .map(mapper::toResponse)
        .toList();
  }

  @Override
  public List<EmploymentExitEventResponse> getExitEventsByEnrollmentId(Long enrollmentId) {
    if (enrollmentId == null) {
      throw new BadRequestException("Enrollment ID is required");
    }
    return employmentExitEventRepository.findByEnrollmentId(enrollmentId).stream()
        .map(mapper::toResponse)
        .toList();
  }

  @Override
  public List<EmploymentExitEventResponse> getExitEventsBySeparationDate(LocalDate separationDate) {
    if (separationDate == null) {
      throw new BadRequestException("Separation date is required");
    }
    return employmentExitEventRepository.findBySeparationDate(separationDate).stream()
        .map(mapper::toResponse)
        .toList();
  }

  @Override
  public List<EmploymentExitEventResponse> getExitEventsByExitReasonId(Long exitReasonId) {
    if (exitReasonId == null) {
      throw new BadRequestException("Exit reason ID is required");
    }
    return employmentExitEventRepository.findByEmploymentExitReasonId(exitReasonId).stream()
        .map(mapper::toResponse)
        .toList();
  }

  @Override
  public List<EmploymentExitEventResponse> getExitEventsBySeparationNatureId(Long separationNatureId) {
    if (separationNatureId == null) {
      throw new BadRequestException("Separation nature ID is required");
    }
    return employmentExitEventRepository.findBySeparationNatureId(separationNatureId).stream()
        .map(mapper::toResponse)
        .toList();
  }

  @Override
  public List<EmploymentExitEventResponse> getExitEventsByVerificationStatusId(Long statusId) {
    if (statusId == null) {
      throw new BadRequestException("Verification status ID is required");
    }
    return employmentExitEventRepository.findByRecordVerificationStatusId(statusId).stream()
        .map(mapper::toResponse)
        .toList();
  }

  @Override
  @Transactional
  public EmploymentExitEventResponse updateExitEvent(Long id, UpdateEmploymentExitEventRequest request) {
    if (id == null) {
      throw new BadRequestException("Employment exit event ID is required", "ID_REQUIRED");
    }
    if (request == null) {
      throw new BadRequestException("Request body cannot be null", "REQUEST_BODY_NULL");
    }

    EmploymentExitEvent exitEvent = employmentExitEventRepository.findById(id)
        .orElseThrow(() -> new ResourceNotFoundException("EmploymentExitEvent", "id"));

    EmploymentRecord employmentRecord = exitEvent.getEmploymentRecord();
    if (employmentRecord == null) {
      throw new BadRequestException("Exit event has no associated employment record", "EMPLOYMENT_RECORD_MISSING");
    }

    if (request.getSeparationDate() != null) {
      if (employmentRecord.getStartDate() != null && request.getSeparationDate().isBefore(employmentRecord.getStartDate())) {
        throw new BadRequestException(
            "Separation date cannot be before employment start date",
            "SEPARATION_DATE_BEFORE_START_DATE"
        );
      }
      exitEvent.setSeparationDate(request.getSeparationDate());
    }

    if (request.getEnrollmentId() != null) {
      TrainingEnrollment enrollment = trainingEnrollmentRepository.findById(request.getEnrollmentId())
          .orElseThrow(() -> new ResourceNotFoundException("TrainingEnrollment", "enrollmentId"));
      if (enrollment.getTrainee() == null || !enrollment.getTrainee().getId().equals(exitEvent.getTrainee().getId())) {
        throw new BadRequestException(
            "Enrollment does not belong to the employment trainee",
            "EXIT_EVENT_ENROLLMENT_TRAINEE_MISMATCH"
        );
      }
      if (employmentRecord.getEnrollment() != null && !employmentRecord.getEnrollment().getId().equals(enrollment.getId())) {
        throw new BadRequestException(
            "Enrollment does not match employment record enrollment",
            "EXIT_EVENT_ENROLLMENT_MISMATCH"
        );
      }
      exitEvent.setEnrollment(enrollment);
    }

    if (request.getEmploymentExitReasonId() != null) {
      RefEmploymentExitReason exitReason = refEmploymentExitReasonRepository.findById(request.getEmploymentExitReasonId())
          .orElseThrow(() -> new ResourceNotFoundException("RefEmploymentExitReason", "employmentExitReasonId"));
      exitEvent.setEmploymentExitReason(exitReason);
    }

    if (request.getSeparationNatureId() != null) {
      RefSeparationNature separationNature = refSeparationNatureRepository.findById(request.getSeparationNatureId())
          .orElseThrow(() -> new ResourceNotFoundException("RefSeparationNature", "separationNatureId"));
      exitEvent.setSeparationNature(separationNature);
    }

    if (request.getEmploymentInfoSourceId() != null) {
      RefEmploymentInfoSource infoSource = refEmploymentInfoSourceRepository.findById(request.getEmploymentInfoSourceId())
          .orElseThrow(() -> new ResourceNotFoundException("RefEmploymentInfoSource", "employmentInfoSourceId"));
      exitEvent.setEmploymentInfoSource(infoSource);
    }

    if (request.getRecordVerificationStatusId() != null) {
      RefRecordVerificationStatus verificationStatus = refRecordVerificationStatusRepository.findById(request.getRecordVerificationStatusId())
          .orElseThrow(() -> new ResourceNotFoundException("RefRecordVerificationStatus", "recordVerificationStatusId"));
      exitEvent.setRecordVerificationStatus(verificationStatus);
    }

    if (request.getVerifiedByUserId() != null) {
      if (!userRepository.existsById(request.getVerifiedByUserId())) {
        throw new ResourceNotFoundException("User", "verifiedByUserId");
      }
      exitEvent.setVerifiedByUserId(request.getVerifiedByUserId());
    }

    if (request.getVerifiedAt() != null) {
      exitEvent.setVerifiedAt(request.getVerifiedAt());
    }

    if (request.getRemarks() != null) {
      exitEvent.setRemarks(request.getRemarks());
    }

    EmploymentExitEvent saved = employmentExitEventRepository.save(exitEvent);

    // Spell synchronization on update
    employmentRecord.setEndDate(exitEvent.getSeparationDate());
    employmentRecord.setIsCurrent(false);
    employmentRecord.setEmploymentExitReason(exitEvent.getEmploymentExitReason());
    if (exitEvent.getRemarks() != null && !exitEvent.getRemarks().isBlank()
        && (employmentRecord.getExitRemarks() == null || employmentRecord.getExitRemarks().isBlank())) {
      employmentRecord.setExitRemarks(exitEvent.getRemarks());
    }
    employmentRecordRepository.save(employmentRecord);

    return mapper.toResponse(saved);
  }

  @Override
  @Transactional
  public void deleteExitEvent(Long id) {
    if (id == null) {
      throw new BadRequestException("Employment exit event ID is required", "ID_REQUIRED");
    }
    EmploymentExitEvent exitEvent = employmentExitEventRepository.findById(id)
        .orElseThrow(() -> new ResourceNotFoundException("EmploymentExitEvent", "id"));

    if (traineeUnemploymentEventRepository.existsByEmploymentExitEventId(id)) {
      throw new ConflictException(
          "Cannot delete exit event linked to an unemployment event",
          "EXIT_EVENT_LINKED_TO_UNEMPLOYMENT"
      );
    }

    // Physically delete only the exit event.
    // NEVER automatically restore EmploymentRecord.isCurrent.
    // NEVER rewrite employment history during exit-event deletion.
    employmentExitEventRepository.delete(exitEvent);
  }
}
