package in.gov.sih.sih26135.service.impl;

import in.gov.sih.sih26135.dto.request.CreateEmploymentVerificationRequestRequest;
import in.gov.sih.sih26135.dto.request.UpdateEmploymentVerificationRequestRequest;
import in.gov.sih.sih26135.dto.response.EmploymentVerificationRequestResponse;
import in.gov.sih.sih26135.entity.Employer;
import in.gov.sih.sih26135.entity.EmploymentRecord;
import in.gov.sih.sih26135.entity.EmploymentVerificationRequest;
import in.gov.sih.sih26135.entity.FollowupTask;
import in.gov.sih.sih26135.entity.PlacementRecord;
import in.gov.sih.sih26135.entity.RefEmploymentInfoSource;
import in.gov.sih.sih26135.entity.RefEmploymentVerificationMethod;
import in.gov.sih.sih26135.entity.RefEmploymentVerificationRequestStatus;
import in.gov.sih.sih26135.entity.SurveyResponse;
import in.gov.sih.sih26135.entity.Trainee;
import in.gov.sih.sih26135.exception.BadRequestException;
import in.gov.sih.sih26135.exception.ConflictException;
import in.gov.sih.sih26135.exception.ResourceNotFoundException;
import in.gov.sih.sih26135.mapper.EmploymentVerificationRequestMapper;
import in.gov.sih.sih26135.repository.EmployerRepository;
import in.gov.sih.sih26135.repository.EmploymentRecordRepository;
import in.gov.sih.sih26135.repository.EmploymentVerificationAttemptRepository;
import in.gov.sih.sih26135.repository.EmploymentVerificationRepository;
import in.gov.sih.sih26135.repository.EmploymentVerificationRequestRepository;
import in.gov.sih.sih26135.repository.FollowupTaskRepository;
import in.gov.sih.sih26135.repository.PlacementRecordRepository;
import in.gov.sih.sih26135.repository.RefEmploymentInfoSourceRepository;
import in.gov.sih.sih26135.repository.RefEmploymentVerificationMethodRepository;
import in.gov.sih.sih26135.repository.RefEmploymentVerificationRequestStatusRepository;
import in.gov.sih.sih26135.repository.SurveyResponseRepository;
import in.gov.sih.sih26135.repository.UserRepository;
import in.gov.sih.sih26135.service.EmploymentVerificationRequestService;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class EmploymentVerificationRequestServiceImpl implements EmploymentVerificationRequestService {

  private final EmploymentVerificationRequestRepository employmentVerificationRequestRepository;
  private final EmploymentRecordRepository employmentRecordRepository;
  private final PlacementRecordRepository placementRecordRepository;
  private final EmployerRepository employerRepository;
  private final RefEmploymentVerificationRequestStatusRepository refEmploymentVerificationRequestStatusRepository;
  private final RefEmploymentVerificationMethodRepository refEmploymentVerificationMethodRepository;
  private final RefEmploymentInfoSourceRepository refEmploymentInfoSourceRepository;
  private final UserRepository userRepository;
  private final FollowupTaskRepository followupTaskRepository;
  private final SurveyResponseRepository surveyResponseRepository;
  private final EmploymentVerificationAttemptRepository employmentVerificationAttemptRepository;
  private final EmploymentVerificationRepository employmentVerificationRepository;
  private final EmploymentVerificationRequestMapper employmentVerificationRequestMapper;

  public EmploymentVerificationRequestServiceImpl(
      EmploymentVerificationRequestRepository employmentVerificationRequestRepository,
      EmploymentRecordRepository employmentRecordRepository,
      PlacementRecordRepository placementRecordRepository,
      EmployerRepository employerRepository,
      RefEmploymentVerificationRequestStatusRepository refEmploymentVerificationRequestStatusRepository,
      RefEmploymentVerificationMethodRepository refEmploymentVerificationMethodRepository,
      RefEmploymentInfoSourceRepository refEmploymentInfoSourceRepository,
      UserRepository userRepository,
      FollowupTaskRepository followupTaskRepository,
      SurveyResponseRepository surveyResponseRepository,
      EmploymentVerificationAttemptRepository employmentVerificationAttemptRepository,
      EmploymentVerificationRepository employmentVerificationRepository,
      EmploymentVerificationRequestMapper employmentVerificationRequestMapper) {
    this.employmentVerificationRequestRepository = employmentVerificationRequestRepository;
    this.employmentRecordRepository = employmentRecordRepository;
    this.placementRecordRepository = placementRecordRepository;
    this.employerRepository = employerRepository;
    this.refEmploymentVerificationRequestStatusRepository = refEmploymentVerificationRequestStatusRepository;
    this.refEmploymentVerificationMethodRepository = refEmploymentVerificationMethodRepository;
    this.refEmploymentInfoSourceRepository = refEmploymentInfoSourceRepository;
    this.userRepository = userRepository;
    this.followupTaskRepository = followupTaskRepository;
    this.surveyResponseRepository = surveyResponseRepository;
    this.employmentVerificationAttemptRepository = employmentVerificationAttemptRepository;
    this.employmentVerificationRepository = employmentVerificationRepository;
    this.employmentVerificationRequestMapper = employmentVerificationRequestMapper;
  }

  @Override
  @Transactional
  public EmploymentVerificationRequestResponse createVerificationRequest(
      CreateEmploymentVerificationRequestRequest request) {
    if (request == null) {
      throw new BadRequestException("Request body cannot be null");
    }
    if (request.getEmploymentRecordId() == null) {
      throw new BadRequestException("Employment record ID is required");
    }
    EmploymentRecord employment = employmentRecordRepository.findById(request.getEmploymentRecordId())
        .orElseThrow(() -> new ResourceNotFoundException("EmploymentRecord", "employmentRecordId"));
    if (employment.getDeletedAt() != null) {
      throw new BadRequestException("Cannot create verification request for a deleted employment record", "EMPLOYMENT_RECORD_DELETED");
    }

    if (request.getRequestNumber() == null || request.getRequestNumber().isBlank()) {
      throw new BadRequestException("Request number is required");
    }
    String requestNumber = request.getRequestNumber().trim();
    if (employmentVerificationRequestRepository.existsByRequestNumber(requestNumber)) {
      throw new ConflictException("Request number already exists: " + requestNumber, "DUPLICATE_REQUEST_NUMBER");
    }

    Trainee authoritativeTrainee = employment.getTrainee();
    if (request.getTraineeId() != null && !request.getTraineeId().equals(authoritativeTrainee.getId())) {
      throw new BadRequestException("Supplied trainee ID does not match employment record trainee", "TRAINEE_MISMATCH");
    }

    PlacementRecord authoritativePlacement = employment.getPlacementRecord();
    if (request.getPlacementRecordId() != null) {
      if (authoritativePlacement != null && !request.getPlacementRecordId().equals(authoritativePlacement.getId())) {
        throw new BadRequestException("Supplied placement record ID does not match employment record placement", "PLACEMENT_MISMATCH");
      }
      if (authoritativePlacement == null) {
        PlacementRecord placement = placementRecordRepository.findById(request.getPlacementRecordId())
            .orElseThrow(() -> new ResourceNotFoundException("PlacementRecord", "placementRecordId"));
        if (!placement.getTrainee().getId().equals(authoritativeTrainee.getId())) {
          throw new BadRequestException("Supplied placement record does not belong to trainee", "PLACEMENT_TRAINEE_MISMATCH");
        }
        authoritativePlacement = placement;
      }
    }

    Employer authoritativeEmployer = employment.getEmployer();
    if (request.getEmployerId() != null) {
      if (authoritativeEmployer != null && !request.getEmployerId().equals(authoritativeEmployer.getId())) {
        throw new BadRequestException("Supplied employer ID does not match employment record employer", "EMPLOYER_MISMATCH");
      }
      if (authoritativeEmployer == null) {
        authoritativeEmployer = employerRepository.findById(request.getEmployerId())
            .orElseThrow(() -> new ResourceNotFoundException("Employer", "employerId"));
      }
    }

    List<EmploymentVerificationRequest> existingRequests =
        employmentVerificationRequestRepository.findByEmploymentRecordId(employment.getId());

    Integer cycleNumber = request.getCycleNumber();
    Boolean isReverification = request.getIsReverification();
    if (cycleNumber == null) {
      if (existingRequests.isEmpty()) {
        cycleNumber = 1;
        isReverification = false;
      } else {
        int maxCycle = existingRequests.stream()
            .mapToInt(EmploymentVerificationRequest::getCycleNumber)
            .max()
            .orElse(0);
        cycleNumber = maxCycle + 1;
        isReverification = true;
      }
    } else {
      if (cycleNumber < 1) {
        throw new BadRequestException("Cycle number must be at least 1", "INVALID_CYCLE_NUMBER");
      }
      if (isReverification == null) {
        isReverification = (cycleNumber >= 2);
      } else {
        if (isReverification && cycleNumber < 2) {
          throw new BadRequestException("Reverification requests must have cycle number >= 2", "INVALID_REVERIFICATION_CYCLE");
        }
        if (!isReverification && cycleNumber != 1) {
          throw new BadRequestException("Initial verification request must have cycle number 1", "INVALID_CYCLE_NUMBER");
        }
      }
    }

    if (employmentVerificationRequestRepository.findByEmploymentRecordIdAndCycleNumber(employment.getId(), cycleNumber).isPresent()) {
      throw new ConflictException("Verification request for this employment record and cycle number already exists", "DUPLICATE_CYCLE_NUMBER");
    }

    RefEmploymentVerificationRequestStatus status;
    if (request.getStatusId() != null) {
      status = refEmploymentVerificationRequestStatusRepository.findById(request.getStatusId())
          .orElseThrow(() -> new ResourceNotFoundException("RefEmploymentVerificationRequestStatus", "statusId"));
    } else {
      status = refEmploymentVerificationRequestStatusRepository.findByStatusCode("REQUESTED")
          .orElseThrow(() -> new ResourceNotFoundException("RefEmploymentVerificationRequestStatus", "REQUESTED"));
    }

    if (Boolean.TRUE.equals(status.getIsOpenFlag())) {
      if (employmentVerificationRequestRepository.findByEmploymentRecordIdAndOpenRequestKey(employment.getId(), 1).isPresent()) {
        throw new ConflictException("An open verification request already exists for this employment record", "OPEN_REQUEST_EXISTS");
      }
    }

    RefEmploymentVerificationMethod preferredMethod = null;
    if (request.getPreferredMethodId() != null) {
      preferredMethod = refEmploymentVerificationMethodRepository.findById(request.getPreferredMethodId())
          .orElseThrow(() -> new ResourceNotFoundException("RefEmploymentVerificationMethod", "preferredMethodId"));
    }

    RefEmploymentInfoSource infoSource = null;
    if (request.getEmploymentInfoSourceId() != null) {
      infoSource = refEmploymentInfoSourceRepository.findById(request.getEmploymentInfoSourceId())
          .orElseThrow(() -> new ResourceNotFoundException("RefEmploymentInfoSource", "employmentInfoSourceId"));
    } else {
      infoSource = employment.getEmploymentInfoSource();
    }

    FollowupTask followupTask = null;
    if (request.getFollowupTaskId() != null) {
      followupTask = followupTaskRepository.findById(request.getFollowupTaskId())
          .orElseThrow(() -> new ResourceNotFoundException("FollowupTask", "followupTaskId"));
      if (!followupTask.getTrainee().getId().equals(authoritativeTrainee.getId())) {
        throw new BadRequestException("Followup task does not belong to trainee", "FOLLOWUP_TRAINEE_MISMATCH");
      }
    }

    SurveyResponse surveyResponse = null;
    if (request.getSurveyResponseId() != null) {
      surveyResponse = surveyResponseRepository.findById(request.getSurveyResponseId())
          .orElseThrow(() -> new ResourceNotFoundException("SurveyResponse", "surveyResponseId"));
      if (!surveyResponse.getTrainee().getId().equals(authoritativeTrainee.getId())) {
        throw new BadRequestException("Survey response does not belong to trainee", "SURVEY_TRAINEE_MISMATCH");
      }
    }

    if (request.getRequestedByUserId() != null && !userRepository.existsById(request.getRequestedByUserId())) {
      throw new ResourceNotFoundException("User", "requestedByUserId");
    }
    if (request.getAssignedVerifierUserId() != null && !userRepository.existsById(request.getAssignedVerifierUserId())) {
      throw new ResourceNotFoundException("User", "assignedVerifierUserId");
    }

    LocalDateTime requestedAt = request.getRequestedAt() != null ? request.getRequestedAt() : LocalDateTime.now();
    if (request.getDueAt() != null && request.getDueAt().isBefore(requestedAt)) {
      throw new BadRequestException("Due date cannot be before requested date", "INVALID_DUE_DATE");
    }

    EmploymentVerificationRequest entity = new EmploymentVerificationRequest();
    entity.setRequestNumber(requestNumber);
    entity.setEmploymentRecord(employment);
    entity.setTrainee(authoritativeTrainee);
    entity.setPlacementRecord(authoritativePlacement);
    entity.setEmployer(authoritativeEmployer);
    entity.setCycleNumber(cycleNumber);
    entity.setIsReverification(isReverification);
    entity.setEmploymentVerificationRequestStatus(status);
    entity.setPreferredVerificationMethod(preferredMethod);
    entity.setEmploymentInfoSource(infoSource);
    entity.setRequestedByUserId(request.getRequestedByUserId());
    entity.setAssignedVerifierUserId(request.getAssignedVerifierUserId());
    entity.setFollowupTask(followupTask);
    entity.setSurveyResponse(surveyResponse);
    entity.setRequestedAt(requestedAt);
    entity.setDueAt(request.getDueAt());
    entity.setRemarks(request.getRemarks());

    EmploymentVerificationRequest saved = employmentVerificationRequestRepository.save(entity);
    return employmentVerificationRequestMapper.toResponse(saved);
  }

  @Override
  @Transactional
  public EmploymentVerificationRequestResponse updateVerificationRequest(
      Long id, UpdateEmploymentVerificationRequestRequest request) {
    if (id == null) {
      throw new BadRequestException("Verification request ID is required");
    }
    if (request == null) {
      throw new BadRequestException("Request body cannot be null");
    }

    EmploymentVerificationRequest entity = employmentVerificationRequestRepository.findById(id)
        .orElseThrow(() -> new ResourceNotFoundException("EmploymentVerificationRequest", "id"));

    if (request.getStatusId() != null) {
      RefEmploymentVerificationRequestStatus newStatus = refEmploymentVerificationRequestStatusRepository
          .findById(request.getStatusId())
          .orElseThrow(() -> new ResourceNotFoundException("RefEmploymentVerificationRequestStatus", "statusId"));

      if (Boolean.TRUE.equals(newStatus.getIsOpenFlag())
          && !Boolean.TRUE.equals(entity.getEmploymentVerificationRequestStatus().getIsOpenFlag())) {
        employmentVerificationRequestRepository.findByEmploymentRecordIdAndOpenRequestKey(entity.getEmploymentRecord().getId(), 1)
            .filter(req -> !req.getId().equals(id))
            .ifPresent(req -> {
              throw new ConflictException("Another open verification request already exists for this employment record", "OPEN_REQUEST_EXISTS");
            });
      }

      entity.setEmploymentVerificationRequestStatus(newStatus);
      if (Boolean.TRUE.equals(newStatus.getIsCompletedFlag()) && entity.getCompletedAt() == null) {
        entity.setCompletedAt(request.getCompletedAt() != null ? request.getCompletedAt() : LocalDateTime.now());
      }
    }

    if (request.getPreferredMethodId() != null) {
      RefEmploymentVerificationMethod method = refEmploymentVerificationMethodRepository
          .findById(request.getPreferredMethodId())
          .orElseThrow(() -> new ResourceNotFoundException("RefEmploymentVerificationMethod", "preferredMethodId"));
      entity.setPreferredVerificationMethod(method);
    }

    if (request.getEmploymentInfoSourceId() != null) {
      RefEmploymentInfoSource source = refEmploymentInfoSourceRepository
          .findById(request.getEmploymentInfoSourceId())
          .orElseThrow(() -> new ResourceNotFoundException("RefEmploymentInfoSource", "employmentInfoSourceId"));
      entity.setEmploymentInfoSource(source);
    }

    if (request.getAssignedVerifierUserId() != null) {
      if (!userRepository.existsById(request.getAssignedVerifierUserId())) {
        throw new ResourceNotFoundException("User", "assignedVerifierUserId");
      }
      entity.setAssignedVerifierUserId(request.getAssignedVerifierUserId());
    }

    if (request.getFollowupTaskId() != null) {
      FollowupTask task = followupTaskRepository.findById(request.getFollowupTaskId())
          .orElseThrow(() -> new ResourceNotFoundException("FollowupTask", "followupTaskId"));
      if (!task.getTrainee().getId().equals(entity.getTrainee().getId())) {
        throw new BadRequestException("Followup task does not belong to request trainee", "FOLLOWUP_TRAINEE_MISMATCH");
      }
      entity.setFollowupTask(task);
    }

    if (request.getSurveyResponseId() != null) {
      SurveyResponse response = surveyResponseRepository.findById(request.getSurveyResponseId())
          .orElseThrow(() -> new ResourceNotFoundException("SurveyResponse", "surveyResponseId"));
      if (!response.getTrainee().getId().equals(entity.getTrainee().getId())) {
        throw new BadRequestException("Survey response does not belong to request trainee", "SURVEY_TRAINEE_MISMATCH");
      }
      entity.setSurveyResponse(response);
    }

    if (request.getDueAt() != null) {
      if (request.getDueAt().isBefore(entity.getRequestedAt())) {
        throw new BadRequestException("Due date cannot be before requested date", "INVALID_DUE_DATE");
      }
      entity.setDueAt(request.getDueAt());
    }

    if (request.getCompletedAt() != null) {
      if (request.getCompletedAt().isBefore(entity.getRequestedAt())) {
        throw new BadRequestException("Completed timestamp cannot be before requested date", "INVALID_COMPLETED_DATE");
      }
      entity.setCompletedAt(request.getCompletedAt());
    }

    if (request.getRemarks() != null) {
      entity.setRemarks(request.getRemarks());
    }

    EmploymentVerificationRequest saved = employmentVerificationRequestRepository.save(entity);
    return employmentVerificationRequestMapper.toResponse(saved);
  }

  @Override
  public EmploymentVerificationRequestResponse getVerificationRequestById(Long id) {
    if (id == null) {
      throw new BadRequestException("Verification request ID is required");
    }
    EmploymentVerificationRequest entity = employmentVerificationRequestRepository.findById(id)
        .orElseThrow(() -> new ResourceNotFoundException("EmploymentVerificationRequest", "id"));
    return employmentVerificationRequestMapper.toResponse(entity);
  }

  @Override
  public EmploymentVerificationRequestResponse getVerificationRequestByNumber(String requestNumber) {
    if (requestNumber == null || requestNumber.isBlank()) {
      throw new BadRequestException("Request number is required");
    }
    EmploymentVerificationRequest entity = employmentVerificationRequestRepository.findByRequestNumber(requestNumber.trim())
        .orElseThrow(() -> new ResourceNotFoundException("EmploymentVerificationRequest", "requestNumber"));
    return employmentVerificationRequestMapper.toResponse(entity);
  }

  @Override
  public List<EmploymentVerificationRequestResponse> getRequestsByEmploymentId(Long employmentId) {
    if (employmentId == null) {
      throw new BadRequestException("Employment ID is required");
    }
    return employmentVerificationRequestRepository.findByEmploymentRecordId(employmentId).stream()
        .map(employmentVerificationRequestMapper::toResponse)
        .toList();
  }

  @Override
  public List<EmploymentVerificationRequestResponse> getRequestsByTraineeId(Long traineeId) {
    if (traineeId == null) {
      throw new BadRequestException("Trainee ID is required");
    }
    return employmentVerificationRequestRepository.findByTraineeId(traineeId).stream()
        .map(employmentVerificationRequestMapper::toResponse)
        .toList();
  }

  @Override
  public List<EmploymentVerificationRequestResponse> getRequestsByStatusId(Long statusId) {
    if (statusId == null) {
      throw new BadRequestException("Status ID is required");
    }
    return employmentVerificationRequestRepository.findByEmploymentVerificationRequestStatusId(statusId).stream()
        .map(employmentVerificationRequestMapper::toResponse)
        .toList();
  }

  @Override
  public List<EmploymentVerificationRequestResponse> getRequestsByAssignedVerifierId(Long assignedVerifierUserId) {
    if (assignedVerifierUserId == null) {
      throw new BadRequestException("Assigned verifier user ID is required");
    }
    return employmentVerificationRequestRepository.findByAssignedVerifierUserId(assignedVerifierUserId).stream()
        .map(employmentVerificationRequestMapper::toResponse)
        .toList();
  }

  @Override
  @Transactional
  public void deleteVerificationRequest(Long id) {
    if (id == null) {
      throw new BadRequestException("Verification request ID is required");
    }
    EmploymentVerificationRequest entity = employmentVerificationRequestRepository.findById(id)
        .orElseThrow(() -> new ResourceNotFoundException("EmploymentVerificationRequest", "id"));

    if (!employmentVerificationAttemptRepository.findByEmploymentVerificationRequestId(id).isEmpty()) {
      throw new ConflictException("Cannot delete verification request with existing attempts", "REQUEST_HAS_ATTEMPTS");
    }
    if (employmentVerificationRepository.findByEmploymentVerificationRequestId(id).isPresent()) {
      throw new ConflictException("Cannot delete verification request with existing verification outcome", "REQUEST_HAS_OUTCOME");
    }

    employmentVerificationRequestRepository.delete(entity);
  }
}
