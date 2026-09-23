package in.gov.sih.sih26135.service.impl;

import in.gov.sih.sih26135.dto.request.CreateEmploymentVerificationAttemptRequest;
import in.gov.sih.sih26135.dto.request.UpdateEmploymentVerificationAttemptRequest;
import in.gov.sih.sih26135.dto.response.EmploymentVerificationAttemptResponse;
import in.gov.sih.sih26135.entity.CommunicationLog;
import in.gov.sih.sih26135.entity.EmploymentVerificationAttempt;
import in.gov.sih.sih26135.entity.EmploymentVerificationRequest;
import in.gov.sih.sih26135.entity.RefEmploymentVerificationAttemptStatus;
import in.gov.sih.sih26135.entity.RefEmploymentVerificationMethod;
import in.gov.sih.sih26135.exception.BadRequestException;
import in.gov.sih.sih26135.exception.ConflictException;
import in.gov.sih.sih26135.exception.ResourceNotFoundException;
import in.gov.sih.sih26135.mapper.EmploymentVerificationAttemptMapper;
import in.gov.sih.sih26135.repository.CommunicationLogRepository;
import in.gov.sih.sih26135.repository.EmploymentVerificationAttemptRepository;
import in.gov.sih.sih26135.repository.EmploymentVerificationEvidenceRepository;
import in.gov.sih.sih26135.repository.EmploymentVerificationRequestRepository;
import in.gov.sih.sih26135.repository.RefEmploymentVerificationAttemptStatusRepository;
import in.gov.sih.sih26135.repository.RefEmploymentVerificationMethodRepository;
import in.gov.sih.sih26135.repository.RefEmploymentVerificationRequestStatusRepository;
import in.gov.sih.sih26135.repository.UserRepository;
import in.gov.sih.sih26135.service.EmploymentVerificationAttemptService;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class EmploymentVerificationAttemptServiceImpl implements EmploymentVerificationAttemptService {

  private final EmploymentVerificationAttemptRepository employmentVerificationAttemptRepository;
  private final EmploymentVerificationRequestRepository employmentVerificationRequestRepository;
  private final RefEmploymentVerificationMethodRepository refEmploymentVerificationMethodRepository;
  private final RefEmploymentVerificationAttemptStatusRepository refEmploymentVerificationAttemptStatusRepository;
  private final RefEmploymentVerificationRequestStatusRepository refEmploymentVerificationRequestStatusRepository;
  private final UserRepository userRepository;
  private final CommunicationLogRepository communicationLogRepository;
  private final EmploymentVerificationEvidenceRepository employmentVerificationEvidenceRepository;
  private final EmploymentVerificationAttemptMapper employmentVerificationAttemptMapper;

  public EmploymentVerificationAttemptServiceImpl(
      EmploymentVerificationAttemptRepository employmentVerificationAttemptRepository,
      EmploymentVerificationRequestRepository employmentVerificationRequestRepository,
      RefEmploymentVerificationMethodRepository refEmploymentVerificationMethodRepository,
      RefEmploymentVerificationAttemptStatusRepository refEmploymentVerificationAttemptStatusRepository,
      RefEmploymentVerificationRequestStatusRepository refEmploymentVerificationRequestStatusRepository,
      UserRepository userRepository,
      CommunicationLogRepository communicationLogRepository,
      EmploymentVerificationEvidenceRepository employmentVerificationEvidenceRepository,
      EmploymentVerificationAttemptMapper employmentVerificationAttemptMapper) {
    this.employmentVerificationAttemptRepository = employmentVerificationAttemptRepository;
    this.employmentVerificationRequestRepository = employmentVerificationRequestRepository;
    this.refEmploymentVerificationMethodRepository = refEmploymentVerificationMethodRepository;
    this.refEmploymentVerificationAttemptStatusRepository = refEmploymentVerificationAttemptStatusRepository;
    this.refEmploymentVerificationRequestStatusRepository = refEmploymentVerificationRequestStatusRepository;
    this.userRepository = userRepository;
    this.communicationLogRepository = communicationLogRepository;
    this.employmentVerificationEvidenceRepository = employmentVerificationEvidenceRepository;
    this.employmentVerificationAttemptMapper = employmentVerificationAttemptMapper;
  }

  @Override
  @Transactional
  public EmploymentVerificationAttemptResponse createVerificationAttempt(
      CreateEmploymentVerificationAttemptRequest request) {
    if (request == null) {
      throw new BadRequestException("Request body cannot be null");
    }
    if (request.getEmploymentVerificationRequestId() == null) {
      throw new BadRequestException("Verification request ID is required");
    }
    EmploymentVerificationRequest verifRequest = employmentVerificationRequestRepository
        .findById(request.getEmploymentVerificationRequestId())
        .orElseThrow(() -> new ResourceNotFoundException("EmploymentVerificationRequest", "employmentVerificationRequestId"));

    List<EmploymentVerificationAttempt> existingAttempts = employmentVerificationAttemptRepository
        .findByEmploymentVerificationRequestId(verifRequest.getId());

    Integer attemptNumber = request.getAttemptNumber();
    if (attemptNumber == null) {
      int maxAttempt = existingAttempts.stream()
          .mapToInt(EmploymentVerificationAttempt::getAttemptNumber)
          .max()
          .orElse(0);
      attemptNumber = maxAttempt + 1;
    } else {
      if (attemptNumber < 1) {
        throw new BadRequestException("Attempt number must be at least 1", "INVALID_ATTEMPT_NUMBER");
      }
      if (employmentVerificationAttemptRepository.existsByEmploymentVerificationRequestIdAndAttemptNumber(
          verifRequest.getId(), attemptNumber)) {
        throw new ConflictException("Attempt number already exists for this verification request", "DUPLICATE_ATTEMPT_NUMBER");
      }
    }

    if (request.getEmploymentVerificationMethodId() == null) {
      throw new BadRequestException("Verification method ID is required");
    }
    RefEmploymentVerificationMethod method = refEmploymentVerificationMethodRepository
        .findById(request.getEmploymentVerificationMethodId())
        .orElseThrow(() -> new ResourceNotFoundException("RefEmploymentVerificationMethod", "employmentVerificationMethodId"));

    if (request.getEmploymentVerificationAttemptStatusId() == null) {
      throw new BadRequestException("Attempt status ID is required");
    }
    RefEmploymentVerificationAttemptStatus status = refEmploymentVerificationAttemptStatusRepository
        .findById(request.getEmploymentVerificationAttemptStatusId())
        .orElseThrow(() -> new ResourceNotFoundException("RefEmploymentVerificationAttemptStatus", "employmentVerificationAttemptStatusId"));

    if (request.getAttemptedByUserId() != null && !userRepository.existsById(request.getAttemptedByUserId())) {
      throw new ResourceNotFoundException("User", "attemptedByUserId");
    }
    if (request.getEmployerRespondentUserId() != null && !userRepository.existsById(request.getEmployerRespondentUserId())) {
      throw new ResourceNotFoundException("User", "employerRespondentUserId");
    }

    CommunicationLog communicationLog = null;
    if (request.getCommunicationLogId() != null) {
      communicationLog = communicationLogRepository.findById(request.getCommunicationLogId())
          .orElseThrow(() -> new ResourceNotFoundException("CommunicationLog", "communicationLogId"));
      if (!communicationLog.getTrainee().getId().equals(verifRequest.getTrainee().getId())) {
        throw new BadRequestException("Communication log does not belong to request trainee", "COMMUNICATION_TRAINEE_MISMATCH");
      }
    }

    LocalDateTime attemptedAt = request.getAttemptedAt() != null ? request.getAttemptedAt() : LocalDateTime.now();
    if (request.getCompletedAt() != null && request.getCompletedAt().isBefore(attemptedAt)) {
      throw new BadRequestException("Completed timestamp cannot be before attempted timestamp", "INVALID_COMPLETED_DATE");
    }

    if (verifRequest.getFirstAttemptAt() == null) {
      verifRequest.setFirstAttemptAt(attemptedAt);
    }
    if ("REQUESTED".equalsIgnoreCase(verifRequest.getEmploymentVerificationRequestStatus().getStatusCode())) {
      refEmploymentVerificationRequestStatusRepository.findByStatusCode("IN_PROGRESS")
          .ifPresent(verifRequest::setEmploymentVerificationRequestStatus);
    }
    employmentVerificationRequestRepository.save(verifRequest);

    EmploymentVerificationAttempt entity = new EmploymentVerificationAttempt();
    entity.setEmploymentVerificationRequest(verifRequest);
    entity.setAttemptNumber(attemptNumber);
    entity.setEmploymentVerificationMethod(method);
    entity.setEmploymentVerificationAttemptStatus(status);
    entity.setAttemptedByUserId(request.getAttemptedByUserId());
    entity.setEmployerRespondentUserId(request.getEmployerRespondentUserId());
    entity.setCommunicationLog(communicationLog);
    entity.setAttemptedAt(attemptedAt);
    entity.setCompletedAt(request.getCompletedAt());
    entity.setNotes(request.getNotes());

    EmploymentVerificationAttempt saved = employmentVerificationAttemptRepository.save(entity);
    return employmentVerificationAttemptMapper.toResponse(saved);
  }

  @Override
  @Transactional
  public EmploymentVerificationAttemptResponse updateVerificationAttempt(
      Long id, UpdateEmploymentVerificationAttemptRequest request) {
    if (id == null) {
      throw new BadRequestException("Verification attempt ID is required");
    }
    if (request == null) {
      throw new BadRequestException("Request body cannot be null");
    }

    EmploymentVerificationAttempt entity = employmentVerificationAttemptRepository.findById(id)
        .orElseThrow(() -> new ResourceNotFoundException("EmploymentVerificationAttempt", "id"));

    if (Boolean.TRUE.equals(entity.getEmploymentVerificationAttemptStatus().getIsTerminalFlag())) {
      throw new ConflictException("Cannot modify an attempt that is in a terminal status", "TERMINAL_ATTEMPT_IMMUTABLE");
    }

    if (request.getEmploymentVerificationAttemptStatusId() != null) {
      RefEmploymentVerificationAttemptStatus newStatus = refEmploymentVerificationAttemptStatusRepository
          .findById(request.getEmploymentVerificationAttemptStatusId())
          .orElseThrow(() -> new ResourceNotFoundException("RefEmploymentVerificationAttemptStatus", "employmentVerificationAttemptStatusId"));
      entity.setEmploymentVerificationAttemptStatus(newStatus);

      if (Boolean.TRUE.equals(newStatus.getIsTerminalFlag()) && entity.getCompletedAt() == null) {
        entity.setCompletedAt(request.getCompletedAt() != null ? request.getCompletedAt() : LocalDateTime.now());
      }
    }

    if (request.getEmployerRespondentUserId() != null) {
      if (!userRepository.existsById(request.getEmployerRespondentUserId())) {
        throw new ResourceNotFoundException("User", "employerRespondentUserId");
      }
      entity.setEmployerRespondentUserId(request.getEmployerRespondentUserId());
    }

    if (request.getCommunicationLogId() != null) {
      CommunicationLog log = communicationLogRepository.findById(request.getCommunicationLogId())
          .orElseThrow(() -> new ResourceNotFoundException("CommunicationLog", "communicationLogId"));
      if (!log.getTrainee().getId().equals(entity.getEmploymentVerificationRequest().getTrainee().getId())) {
        throw new BadRequestException("Communication log does not belong to request trainee", "COMMUNICATION_TRAINEE_MISMATCH");
      }
      entity.setCommunicationLog(log);
    }

    if (request.getCompletedAt() != null) {
      if (request.getCompletedAt().isBefore(entity.getAttemptedAt())) {
        throw new BadRequestException("Completed timestamp cannot be before attempted timestamp", "INVALID_COMPLETED_DATE");
      }
      entity.setCompletedAt(request.getCompletedAt());
    }

    if (request.getNotes() != null) {
      entity.setNotes(request.getNotes());
    }

    EmploymentVerificationAttempt saved = employmentVerificationAttemptRepository.save(entity);
    return employmentVerificationAttemptMapper.toResponse(saved);
  }

  @Override
  public EmploymentVerificationAttemptResponse getVerificationAttemptById(Long id) {
    if (id == null) {
      throw new BadRequestException("Verification attempt ID is required");
    }
    EmploymentVerificationAttempt entity = employmentVerificationAttemptRepository.findById(id)
        .orElseThrow(() -> new ResourceNotFoundException("EmploymentVerificationAttempt", "id"));
    return employmentVerificationAttemptMapper.toResponse(entity);
  }

  @Override
  public List<EmploymentVerificationAttemptResponse> getAttemptsByRequestId(Long requestId) {
    if (requestId == null) {
      throw new BadRequestException("Request ID is required");
    }
    return employmentVerificationAttemptRepository.findByEmploymentVerificationRequestId(requestId).stream()
        .map(employmentVerificationAttemptMapper::toResponse)
        .toList();
  }

  @Override
  public List<EmploymentVerificationAttemptResponse> getAttemptsByMethodId(Long methodId) {
    if (methodId == null) {
      throw new BadRequestException("Method ID is required");
    }
    return employmentVerificationAttemptRepository.findByEmploymentVerificationMethodId(methodId).stream()
        .map(employmentVerificationAttemptMapper::toResponse)
        .toList();
  }

  @Override
  public List<EmploymentVerificationAttemptResponse> getAttemptsByStatusId(Long statusId) {
    if (statusId == null) {
      throw new BadRequestException("Status ID is required");
    }
    return employmentVerificationAttemptRepository.findByEmploymentVerificationAttemptStatusId(statusId).stream()
        .map(employmentVerificationAttemptMapper::toResponse)
        .toList();
  }

  @Override
  @Transactional
  public void deleteVerificationAttempt(Long id) {
    if (id == null) {
      throw new BadRequestException("Verification attempt ID is required");
    }
    EmploymentVerificationAttempt entity = employmentVerificationAttemptRepository.findById(id)
        .orElseThrow(() -> new ResourceNotFoundException("EmploymentVerificationAttempt", "id"));

    if (Boolean.TRUE.equals(entity.getEmploymentVerificationAttemptStatus().getIsTerminalFlag())) {
      throw new ConflictException("Cannot delete a completed or terminal verification attempt", "TERMINAL_ATTEMPT_IMMUTABLE");
    }

    if (!employmentVerificationEvidenceRepository.findByEmploymentVerificationAttemptId(id).isEmpty()) {
      throw new ConflictException("Cannot delete verification attempt with associated evidence", "ATTEMPT_HAS_EVIDENCE");
    }

    employmentVerificationAttemptRepository.delete(entity);
  }
}
