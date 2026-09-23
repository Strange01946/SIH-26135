package in.gov.sih.sih26135.service.impl;

import in.gov.sih.sih26135.dto.request.CreateEmploymentVerificationRequest;
import in.gov.sih.sih26135.dto.request.UpdateEmploymentVerificationRequest;
import in.gov.sih.sih26135.dto.response.EmploymentVerificationResponse;
import in.gov.sih.sih26135.entity.Employer;
import in.gov.sih.sih26135.entity.EmploymentRecord;
import in.gov.sih.sih26135.entity.EmploymentVerification;
import in.gov.sih.sih26135.entity.EmploymentVerificationRequest;
import in.gov.sih.sih26135.entity.PlacementRecord;
import in.gov.sih.sih26135.entity.RefEmploymentInfoSource;
import in.gov.sih.sih26135.entity.RefEmploymentVerificationMethod;
import in.gov.sih.sih26135.entity.RefEmploymentVerificationRejectionReason;
import in.gov.sih.sih26135.entity.RefRecordVerificationStatus;
import in.gov.sih.sih26135.entity.Trainee;
import in.gov.sih.sih26135.exception.BadRequestException;
import in.gov.sih.sih26135.exception.ConflictException;
import in.gov.sih.sih26135.exception.ResourceNotFoundException;
import in.gov.sih.sih26135.mapper.EmploymentVerificationMapper;
import in.gov.sih.sih26135.repository.EmployerRepository;
import in.gov.sih.sih26135.repository.EmploymentRecordRepository;
import in.gov.sih.sih26135.repository.EmploymentVerificationEvidenceRepository;
import in.gov.sih.sih26135.repository.EmploymentVerificationRepository;
import in.gov.sih.sih26135.repository.EmploymentVerificationRequestRepository;
import in.gov.sih.sih26135.repository.PlacementRecordRepository;
import in.gov.sih.sih26135.repository.RefEmploymentInfoSourceRepository;
import in.gov.sih.sih26135.repository.RefEmploymentVerificationMethodRepository;
import in.gov.sih.sih26135.repository.RefEmploymentVerificationRejectionReasonRepository;
import in.gov.sih.sih26135.repository.RefEmploymentVerificationRequestStatusRepository;
import in.gov.sih.sih26135.repository.RefRecordVerificationStatusRepository;
import in.gov.sih.sih26135.repository.UserRepository;
import in.gov.sih.sih26135.service.EmploymentVerificationService;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class EmploymentVerificationServiceImpl implements EmploymentVerificationService {

  private final EmploymentVerificationRepository employmentVerificationRepository;
  private final EmploymentVerificationRequestRepository employmentVerificationRequestRepository;
  private final EmploymentRecordRepository employmentRecordRepository;
  private final PlacementRecordRepository placementRecordRepository;
  private final EmployerRepository employerRepository;
  private final RefRecordVerificationStatusRepository refRecordVerificationStatusRepository;
  private final RefEmploymentVerificationMethodRepository refEmploymentVerificationMethodRepository;
  private final RefEmploymentInfoSourceRepository refEmploymentInfoSourceRepository;
  private final RefEmploymentVerificationRejectionReasonRepository refEmploymentVerificationRejectionReasonRepository;
  private final RefEmploymentVerificationRequestStatusRepository refEmploymentVerificationRequestStatusRepository;
  private final UserRepository userRepository;
  private final EmploymentVerificationEvidenceRepository employmentVerificationEvidenceRepository;
  private final EmploymentVerificationMapper employmentVerificationMapper;

  public EmploymentVerificationServiceImpl(
      EmploymentVerificationRepository employmentVerificationRepository,
      EmploymentVerificationRequestRepository employmentVerificationRequestRepository,
      EmploymentRecordRepository employmentRecordRepository,
      PlacementRecordRepository placementRecordRepository,
      EmployerRepository employerRepository,
      RefRecordVerificationStatusRepository refRecordVerificationStatusRepository,
      RefEmploymentVerificationMethodRepository refEmploymentVerificationMethodRepository,
      RefEmploymentInfoSourceRepository refEmploymentInfoSourceRepository,
      RefEmploymentVerificationRejectionReasonRepository refEmploymentVerificationRejectionReasonRepository,
      RefEmploymentVerificationRequestStatusRepository refEmploymentVerificationRequestStatusRepository,
      UserRepository userRepository,
      EmploymentVerificationEvidenceRepository employmentVerificationEvidenceRepository,
      EmploymentVerificationMapper employmentVerificationMapper) {
    this.employmentVerificationRepository = employmentVerificationRepository;
    this.employmentVerificationRequestRepository = employmentVerificationRequestRepository;
    this.employmentRecordRepository = employmentRecordRepository;
    this.placementRecordRepository = placementRecordRepository;
    this.employerRepository = employerRepository;
    this.refRecordVerificationStatusRepository = refRecordVerificationStatusRepository;
    this.refEmploymentVerificationMethodRepository = refEmploymentVerificationMethodRepository;
    this.refEmploymentInfoSourceRepository = refEmploymentInfoSourceRepository;
    this.refEmploymentVerificationRejectionReasonRepository = refEmploymentVerificationRejectionReasonRepository;
    this.refEmploymentVerificationRequestStatusRepository = refEmploymentVerificationRequestStatusRepository;
    this.userRepository = userRepository;
    this.employmentVerificationEvidenceRepository = employmentVerificationEvidenceRepository;
    this.employmentVerificationMapper = employmentVerificationMapper;
  }

  @Override
  @Transactional
  public EmploymentVerificationResponse createEmploymentVerification(CreateEmploymentVerificationRequest request) {
    if (request == null) {
      throw new BadRequestException("Request body cannot be null");
    }
    if (request.getEmploymentRecordId() == null) {
      throw new BadRequestException("Employment record ID is required");
    }
    EmploymentRecord employment = employmentRecordRepository.findById(request.getEmploymentRecordId())
        .orElseThrow(() -> new ResourceNotFoundException("EmploymentRecord", "employmentRecordId"));
    if (employment.getDeletedAt() != null) {
      throw new BadRequestException("Cannot verify a deleted employment record", "EMPLOYMENT_RECORD_DELETED");
    }

    if (request.getVerificationNumber() == null || request.getVerificationNumber().isBlank()) {
      throw new BadRequestException("Verification number is required");
    }
    String verificationNumber = request.getVerificationNumber().trim();
    if (employmentVerificationRepository.existsByVerificationNumber(verificationNumber)) {
      throw new ConflictException("Verification number already exists: " + verificationNumber, "DUPLICATE_VERIFICATION_NUMBER");
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

    EmploymentVerificationRequest verifRequest = null;
    if (request.getEmploymentVerificationRequestId() != null) {
      verifRequest = employmentVerificationRequestRepository.findById(request.getEmploymentVerificationRequestId())
          .orElseThrow(() -> new ResourceNotFoundException("EmploymentVerificationRequest", "employmentVerificationRequestId"));
      if (!verifRequest.getEmploymentRecord().getId().equals(employment.getId())) {
        throw new BadRequestException("Verification request does not belong to the same employment record", "REQUEST_EMPLOYMENT_MISMATCH");
      }
      if (employmentVerificationRepository.existsByEmploymentVerificationRequestId(verifRequest.getId())) {
        throw new ConflictException("Verification outcome already exists for this verification request", "DUPLICATE_REQUEST_OUTCOME");
      }
    }

    List<EmploymentVerification> existingVerifications =
        employmentVerificationRepository.findByEmploymentRecordId(employment.getId());

    Integer cycleNumber = request.getCycleNumber();
    Boolean isReverification = request.getIsReverification();
    if (verifRequest != null) {
      if (cycleNumber != null && !cycleNumber.equals(verifRequest.getCycleNumber())) {
        throw new BadRequestException(
            "Supplied cycle number does not match linked verification request cycle number",
            "VERIFICATION_CYCLE_REQUEST_MISMATCH");
      }
      cycleNumber = verifRequest.getCycleNumber();

      if (isReverification != null && !isReverification.equals(verifRequest.getIsReverification())) {
        throw new BadRequestException(
            "Supplied isReverification does not match linked verification request isReverification",
            "VERIFICATION_REVERIFICATION_REQUEST_MISMATCH");
      }
      isReverification = verifRequest.getIsReverification();
    } else {
      if (cycleNumber == null) {
        if (existingVerifications.isEmpty()) {
          cycleNumber = 1;
          isReverification = false;
        } else {
          int maxCycle = existingVerifications.stream()
              .mapToInt(EmploymentVerification::getCycleNumber)
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
            throw new BadRequestException("Reverification must have cycle number >= 2", "INVALID_REVERIFICATION_CYCLE");
          }
          if (!isReverification && cycleNumber != 1) {
            throw new BadRequestException("Initial verification must have cycle number 1", "INVALID_CYCLE_NUMBER");
          }
        }
      }
    }

    if (employmentVerificationRepository.findByEmploymentRecordIdAndCycleNumber(employment.getId(), cycleNumber).isPresent()) {
      throw new ConflictException("Verification for this employment record and cycle number already exists", "DUPLICATE_CYCLE_NUMBER");
    }

    if (request.getRecordVerificationStatusId() == null) {
      throw new BadRequestException("Record verification status ID is required");
    }
    RefRecordVerificationStatus recordStatus = refRecordVerificationStatusRepository
        .findById(request.getRecordVerificationStatusId())
        .orElseThrow(() -> new ResourceNotFoundException("RefRecordVerificationStatus", "recordVerificationStatusId"));

    if (request.getEmploymentVerificationMethodId() == null) {
      throw new BadRequestException("Verification method ID is required");
    }
    RefEmploymentVerificationMethod method = refEmploymentVerificationMethodRepository
        .findById(request.getEmploymentVerificationMethodId())
        .orElseThrow(() -> new ResourceNotFoundException("RefEmploymentVerificationMethod", "employmentVerificationMethodId"));

    RefEmploymentInfoSource infoSource = null;
    if (request.getEmploymentInfoSourceId() != null) {
      infoSource = refEmploymentInfoSourceRepository.findById(request.getEmploymentInfoSourceId())
          .orElseThrow(() -> new ResourceNotFoundException("RefEmploymentInfoSource", "employmentInfoSourceId"));
    } else if (verifRequest != null && verifRequest.getEmploymentInfoSource() != null) {
      infoSource = verifRequest.getEmploymentInfoSource();
    } else {
      infoSource = employment.getEmploymentInfoSource();
    }

    RefEmploymentVerificationRejectionReason rejectionReason = null;
    if (request.getEmploymentVerificationRejectionReasonId() != null) {
      rejectionReason = refEmploymentVerificationRejectionReasonRepository
          .findById(request.getEmploymentVerificationRejectionReasonId())
          .orElseThrow(() -> new ResourceNotFoundException("RefEmploymentVerificationRejectionReason", "employmentVerificationRejectionReasonId"));
    }

    if ("REJECTED".equalsIgnoreCase(recordStatus.getStatusCode()) && rejectionReason == null) {
      throw new BadRequestException("Rejection reason is required when status is REJECTED", "REJECTION_REASON_REQUIRED");
    }

    String rejectionNotes = request.getRejectionNotes();
    if (rejectionNotes != null && !rejectionNotes.isBlank() && rejectionReason == null) {
      throw new BadRequestException("Rejection notes are only allowed when rejection reason is specified", "REJECTION_NOTES_NOT_ALLOWED");
    }

    if (request.getVerifiedByUserId() != null && !userRepository.existsById(request.getVerifiedByUserId())) {
      throw new ResourceNotFoundException("User", "verifiedByUserId");
    }
    if (request.getEmployerRespondentUserId() != null && !userRepository.existsById(request.getEmployerRespondentUserId())) {
      throw new ResourceNotFoundException("User", "employerRespondentUserId");
    }

    LocalDateTime requestedAt = request.getRequestedAt() != null
        ? request.getRequestedAt()
        : (verifRequest != null ? verifRequest.getRequestedAt() : LocalDateTime.now());

    LocalDateTime outcomeRecordedAt = request.getOutcomeRecordedAt() != null
        ? request.getOutcomeRecordedAt()
        : LocalDateTime.now();

    if (outcomeRecordedAt.isBefore(requestedAt)) {
      throw new BadRequestException("Outcome recorded timestamp cannot be before requested timestamp", "INVALID_OUTCOME_DATE");
    }
    if (request.getVerifiedAt() != null && request.getVerifiedAt().isBefore(requestedAt)) {
      throw new BadRequestException("Verified timestamp cannot be before requested timestamp", "INVALID_VERIFIED_DATE");
    }

    boolean isCurrent = request.getIsCurrent() == null || Boolean.TRUE.equals(request.getIsCurrent());

    if (isCurrent) {
      List<EmploymentVerification> currentVerifications =
          employmentVerificationRepository.findByEmploymentRecordIdAndIsCurrentTrue(employment.getId());
      for (EmploymentVerification cv : currentVerifications) {
        cv.setIsCurrent(false);
        employmentVerificationRepository.save(cv);
      }

      employment.setRecordVerificationStatus(recordStatus);
      if ("VERIFIED".equalsIgnoreCase(recordStatus.getStatusCode())) {
        employment.setVerifiedAt(request.getVerifiedAt() != null ? request.getVerifiedAt() : outcomeRecordedAt);
        employment.setVerifiedByUserId(request.getVerifiedByUserId());
      }
      employmentRecordRepository.save(employment);
    }

    if (verifRequest != null) {
      refEmploymentVerificationRequestStatusRepository.findByStatusCode("COMPLETED")
          .ifPresent(verifRequest::setEmploymentVerificationRequestStatus);
      if (verifRequest.getCompletedAt() == null) {
        verifRequest.setCompletedAt(outcomeRecordedAt);
      }
      employmentVerificationRequestRepository.save(verifRequest);
    }

    EmploymentVerification entity = new EmploymentVerification();
    entity.setVerificationNumber(verificationNumber);
    entity.setEmploymentVerificationRequest(verifRequest);
    entity.setEmploymentRecord(employment);
    entity.setTrainee(authoritativeTrainee);
    entity.setPlacementRecord(authoritativePlacement);
    entity.setEmployer(authoritativeEmployer);
    entity.setCycleNumber(cycleNumber);
    entity.setIsReverification(isReverification);
    entity.setIsCurrent(isCurrent);
    entity.setRecordVerificationStatus(recordStatus);
    entity.setEmploymentVerificationMethod(method);
    entity.setEmploymentInfoSource(infoSource);
    entity.setEmploymentVerificationRejectionReason(rejectionReason);
    entity.setVerifiedByUserId(request.getVerifiedByUserId());
    entity.setEmployerRespondentUserId(request.getEmployerRespondentUserId());
    entity.setTraineeAttestedFlag(Boolean.TRUE.equals(request.getTraineeAttestedFlag()));
    entity.setEmployerConfirmedFlag(Boolean.TRUE.equals(request.getEmployerConfirmedFlag()));
    entity.setRequestedAt(requestedAt);
    entity.setVerifiedAt(request.getVerifiedAt());
    entity.setOutcomeRecordedAt(outcomeRecordedAt);
    entity.setNotes(request.getNotes());
    entity.setRejectionNotes(rejectionNotes);

    EmploymentVerification saved = employmentVerificationRepository.save(entity);
    return employmentVerificationMapper.toResponse(saved);
  }

  @Override
  @Transactional
  public EmploymentVerificationResponse updateEmploymentVerification(
      Long id, UpdateEmploymentVerificationRequest request) {
    if (id == null) {
      throw new BadRequestException("Employment verification ID is required");
    }
    if (request == null) {
      throw new BadRequestException("Request body cannot be null");
    }

    EmploymentVerification entity = employmentVerificationRepository.findById(id)
        .orElseThrow(() -> new ResourceNotFoundException("EmploymentVerification", "id"));

    if (request.getRecordVerificationStatusId() != null) {
      RefRecordVerificationStatus newStatus = refRecordVerificationStatusRepository
          .findById(request.getRecordVerificationStatusId())
          .orElseThrow(() -> new ResourceNotFoundException("RefRecordVerificationStatus", "recordVerificationStatusId"));
      entity.setRecordVerificationStatus(newStatus);
    }

    if (request.getEmploymentVerificationMethodId() != null) {
      RefEmploymentVerificationMethod method = refEmploymentVerificationMethodRepository
          .findById(request.getEmploymentVerificationMethodId())
          .orElseThrow(() -> new ResourceNotFoundException("RefEmploymentVerificationMethod", "employmentVerificationMethodId"));
      entity.setEmploymentVerificationMethod(method);
    }

    if (request.getEmploymentInfoSourceId() != null) {
      RefEmploymentInfoSource source = refEmploymentInfoSourceRepository
          .findById(request.getEmploymentInfoSourceId())
          .orElseThrow(() -> new ResourceNotFoundException("RefEmploymentInfoSource", "employmentInfoSourceId"));
      entity.setEmploymentInfoSource(source);
    }

    if (request.getEmploymentVerificationRejectionReasonId() != null) {
      RefEmploymentVerificationRejectionReason reason = refEmploymentVerificationRejectionReasonRepository
          .findById(request.getEmploymentVerificationRejectionReasonId())
          .orElseThrow(() -> new ResourceNotFoundException("RefEmploymentVerificationRejectionReason", "employmentVerificationRejectionReasonId"));
      entity.setEmploymentVerificationRejectionReason(reason);
    }

    if (request.getRejectionNotes() != null) {
      entity.setRejectionNotes(request.getRejectionNotes());
    }

    if ("REJECTED".equalsIgnoreCase(entity.getRecordVerificationStatus().getStatusCode())
        && entity.getEmploymentVerificationRejectionReason() == null) {
      throw new BadRequestException("Rejection reason is required when status is REJECTED", "REJECTION_REASON_REQUIRED");
    }

    if (entity.getRejectionNotes() != null && !entity.getRejectionNotes().isBlank()
        && entity.getEmploymentVerificationRejectionReason() == null) {
      throw new BadRequestException("Rejection notes are only allowed when rejection reason is specified", "REJECTION_NOTES_NOT_ALLOWED");
    }

    if (request.getVerifiedByUserId() != null) {
      if (!userRepository.existsById(request.getVerifiedByUserId())) {
        throw new ResourceNotFoundException("User", "verifiedByUserId");
      }
      entity.setVerifiedByUserId(request.getVerifiedByUserId());
    }

    if (request.getEmployerRespondentUserId() != null) {
      if (!userRepository.existsById(request.getEmployerRespondentUserId())) {
        throw new ResourceNotFoundException("User", "employerRespondentUserId");
      }
      entity.setEmployerRespondentUserId(request.getEmployerRespondentUserId());
    }

    if (request.getTraineeAttestedFlag() != null) {
      entity.setTraineeAttestedFlag(request.getTraineeAttestedFlag());
    }

    if (request.getEmployerConfirmedFlag() != null) {
      entity.setEmployerConfirmedFlag(request.getEmployerConfirmedFlag());
    }

    if (request.getVerifiedAt() != null) {
      if (request.getVerifiedAt().isBefore(entity.getRequestedAt())) {
        throw new BadRequestException("Verified timestamp cannot be before requested timestamp", "INVALID_VERIFIED_DATE");
      }
      entity.setVerifiedAt(request.getVerifiedAt());
    }

    if (request.getNotes() != null) {
      entity.setNotes(request.getNotes());
    }

    if (request.getIsCurrent() != null) {
      if (Boolean.TRUE.equals(request.getIsCurrent())) {
        List<EmploymentVerification> currentVerifications =
            employmentVerificationRepository.findByEmploymentRecordIdAndIsCurrentTrue(entity.getEmploymentRecord().getId());
        for (EmploymentVerification cv : currentVerifications) {
          if (!cv.getId().equals(id)) {
            cv.setIsCurrent(false);
            employmentVerificationRepository.save(cv);
          }
        }
        entity.setIsCurrent(true);

        EmploymentRecord emp = entity.getEmploymentRecord();
        emp.setRecordVerificationStatus(entity.getRecordVerificationStatus());
        if ("VERIFIED".equalsIgnoreCase(entity.getRecordVerificationStatus().getStatusCode())) {
          emp.setVerifiedAt(entity.getVerifiedAt() != null ? entity.getVerifiedAt() : entity.getOutcomeRecordedAt());
          emp.setVerifiedByUserId(entity.getVerifiedByUserId());
        }
        employmentRecordRepository.save(emp);
      } else {
        entity.setIsCurrent(false);
      }
    } else if (Boolean.TRUE.equals(entity.getIsCurrent())) {
      EmploymentRecord emp = entity.getEmploymentRecord();
      emp.setRecordVerificationStatus(entity.getRecordVerificationStatus());
      if ("VERIFIED".equalsIgnoreCase(entity.getRecordVerificationStatus().getStatusCode())) {
        emp.setVerifiedAt(entity.getVerifiedAt() != null ? entity.getVerifiedAt() : entity.getOutcomeRecordedAt());
        emp.setVerifiedByUserId(entity.getVerifiedByUserId());
      }
      employmentRecordRepository.save(emp);
    }

    EmploymentVerification saved = employmentVerificationRepository.save(entity);
    return employmentVerificationMapper.toResponse(saved);
  }

  @Override
  public EmploymentVerificationResponse getEmploymentVerificationById(Long id) {
    if (id == null) {
      throw new BadRequestException("Employment verification ID is required");
    }
    EmploymentVerification entity = employmentVerificationRepository.findById(id)
        .orElseThrow(() -> new ResourceNotFoundException("EmploymentVerification", "id"));
    return employmentVerificationMapper.toResponse(entity);
  }

  @Override
  public EmploymentVerificationResponse getEmploymentVerificationByNumber(String verificationNumber) {
    if (verificationNumber == null || verificationNumber.isBlank()) {
      throw new BadRequestException("Verification number is required");
    }
    EmploymentVerification entity = employmentVerificationRepository.findByVerificationNumber(verificationNumber.trim())
        .orElseThrow(() -> new ResourceNotFoundException("EmploymentVerification", "verificationNumber"));
    return employmentVerificationMapper.toResponse(entity);
  }

  @Override
  public EmploymentVerificationResponse getCurrentVerificationByEmploymentId(Long employmentId) {
    if (employmentId == null) {
      throw new BadRequestException("Employment ID is required");
    }
    List<EmploymentVerification> currentList =
        employmentVerificationRepository.findByEmploymentRecordIdAndIsCurrentTrue(employmentId);
    if (currentList.isEmpty()) {
      throw new ResourceNotFoundException("EmploymentVerification", "employmentId (current)");
    }
    return employmentVerificationMapper.toResponse(currentList.get(0));
  }

  @Override
  public List<EmploymentVerificationResponse> getVerificationsByEmploymentId(Long employmentId) {
    if (employmentId == null) {
      throw new BadRequestException("Employment ID is required");
    }
    return employmentVerificationRepository.findByEmploymentRecordId(employmentId).stream()
        .map(employmentVerificationMapper::toResponse)
        .toList();
  }

  @Override
  public List<EmploymentVerificationResponse> getVerificationsByTraineeId(Long traineeId) {
    if (traineeId == null) {
      throw new BadRequestException("Trainee ID is required");
    }
    return employmentVerificationRepository.findByTraineeId(traineeId).stream()
        .map(employmentVerificationMapper::toResponse)
        .toList();
  }

  @Override
  public List<EmploymentVerificationResponse> getVerificationsByStatusId(Long statusId) {
    if (statusId == null) {
      throw new BadRequestException("Status ID is required");
    }
    return employmentVerificationRepository.findByRecordVerificationStatusId(statusId).stream()
        .map(employmentVerificationMapper::toResponse)
        .toList();
  }

  @Override
  @Transactional
  public void deleteEmploymentVerification(Long id) {
    if (id == null) {
      throw new BadRequestException("Employment verification ID is required");
    }
    EmploymentVerification entity = employmentVerificationRepository.findById(id)
        .orElseThrow(() -> new ResourceNotFoundException("EmploymentVerification", "id"));

    if (!employmentVerificationEvidenceRepository.findByEmploymentVerificationId(id).isEmpty()) {
      throw new ConflictException("Cannot delete employment verification with associated evidence records", "VERIFICATION_HAS_EVIDENCE");
    }

    if (Boolean.TRUE.equals(entity.getIsCurrent())) {
      throw new ConflictException("Cannot delete the current active verification for an employment record", "CURRENT_VERIFICATION_RESTRICTED");
    }

    employmentVerificationRepository.delete(entity);
  }
}
