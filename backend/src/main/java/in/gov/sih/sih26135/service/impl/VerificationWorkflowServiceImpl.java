package in.gov.sih.sih26135.service.impl;

import in.gov.sih.sih26135.dto.request.CreateEmploymentVerificationEvidenceRequest;
import in.gov.sih.sih26135.dto.request.CreateEmploymentVerificationRequest;
import in.gov.sih.sih26135.dto.request.CreateEmploymentVerificationRequestRequest;
import in.gov.sih.sih26135.dto.request.InitiateVerificationFromSurveyRequest;
import in.gov.sih.sih26135.dto.request.RecordVerificationVerdictRequest;
import in.gov.sih.sih26135.dto.request.UpdateEmploymentRecordRequest;
import in.gov.sih.sih26135.dto.request.UpdateEmploymentVerificationRequestRequest;
import in.gov.sih.sih26135.dto.response.EmploymentRecordResponse;
import in.gov.sih.sih26135.dto.response.EmploymentVerificationAttemptResponse;
import in.gov.sih.sih26135.dto.response.EmploymentVerificationEvidenceResponse;
import in.gov.sih.sih26135.dto.response.EmploymentVerificationRequestResponse;
import in.gov.sih.sih26135.dto.response.EmploymentVerificationResponse;
import in.gov.sih.sih26135.dto.response.EmploymentVerificationSummaryResponse;
import in.gov.sih.sih26135.dto.response.RecordVerificationVerdictResponse;
import in.gov.sih.sih26135.dto.response.SurveyResponseResponse;
import in.gov.sih.sih26135.entity.RefUserStatus;
import in.gov.sih.sih26135.entity.User;
import in.gov.sih.sih26135.exception.BadRequestException;
import in.gov.sih.sih26135.exception.ConflictException;
import in.gov.sih.sih26135.exception.InvalidCredentialsException;
import in.gov.sih.sih26135.exception.ResourceNotFoundException;
import in.gov.sih.sih26135.repository.RefUserStatusRepository;
import in.gov.sih.sih26135.repository.UserRepository;
import in.gov.sih.sih26135.security.UserPrincipal;
import in.gov.sih.sih26135.service.EmploymentRecordService;
import in.gov.sih.sih26135.service.EmploymentVerificationAttemptService;
import in.gov.sih.sih26135.service.EmploymentVerificationEvidenceService;
import in.gov.sih.sih26135.service.EmploymentVerificationRequestService;
import in.gov.sih.sih26135.service.EmploymentVerificationService;
import in.gov.sih.sih26135.service.FollowupTaskService;
import in.gov.sih.sih26135.service.SurveyResponseService;
import in.gov.sih.sih26135.service.VerificationWorkflowService;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class VerificationWorkflowServiceImpl implements VerificationWorkflowService {

  private final EmploymentRecordService employmentRecordService;
  private final FollowupTaskService followupTaskService;
  private final SurveyResponseService surveyResponseService;
  private final EmploymentVerificationRequestService employmentVerificationRequestService;
  private final EmploymentVerificationAttemptService employmentVerificationAttemptService;
  private final EmploymentVerificationService employmentVerificationService;
  private final EmploymentVerificationEvidenceService employmentVerificationEvidenceService;
  private final UserRepository userRepository;
  private final RefUserStatusRepository refUserStatusRepository;

  @Autowired
  public VerificationWorkflowServiceImpl(
      EmploymentRecordService employmentRecordService,
      FollowupTaskService followupTaskService,
      SurveyResponseService surveyResponseService,
      EmploymentVerificationRequestService employmentVerificationRequestService,
      EmploymentVerificationAttemptService employmentVerificationAttemptService,
      EmploymentVerificationService employmentVerificationService,
      EmploymentVerificationEvidenceService employmentVerificationEvidenceService,
      UserRepository userRepository,
      RefUserStatusRepository refUserStatusRepository) {
    this.employmentRecordService = employmentRecordService;
    this.followupTaskService = followupTaskService;
    this.surveyResponseService = surveyResponseService;
    this.employmentVerificationRequestService = employmentVerificationRequestService;
    this.employmentVerificationAttemptService = employmentVerificationAttemptService;
    this.employmentVerificationService = employmentVerificationService;
    this.employmentVerificationEvidenceService = employmentVerificationEvidenceService;
    this.userRepository = userRepository;
    this.refUserStatusRepository = refUserStatusRepository;
  }

  @Deprecated
  public VerificationWorkflowServiceImpl(
      EmploymentRecordService employmentRecordService,
      FollowupTaskService followupTaskService,
      SurveyResponseService surveyResponseService,
      EmploymentVerificationRequestService employmentVerificationRequestService,
      EmploymentVerificationAttemptService employmentVerificationAttemptService,
      EmploymentVerificationService employmentVerificationService,
      EmploymentVerificationEvidenceService employmentVerificationEvidenceService) {
    this(
        employmentRecordService,
        followupTaskService,
        surveyResponseService,
        employmentVerificationRequestService,
        employmentVerificationAttemptService,
        employmentVerificationService,
        employmentVerificationEvidenceService,
        null,
        null
    );
  }

  @Override
  @Transactional
  public EmploymentVerificationRequestResponse initiateVerificationFromSurvey(
      InitiateVerificationFromSurveyRequest request) {
    if (request == null) {
      throw new BadRequestException("Request body cannot be null", "REQUEST_BODY_NULL");
    }
    if (request.getSurveyResponseId() == null) {
      throw new BadRequestException("Survey response ID is required", "SURVEY_RESPONSE_ID_REQUIRED");
    }
    if (request.getVerificationRequestDetails() == null) {
      throw new BadRequestException("Verification request details are required", "VERIFICATION_DETAILS_REQUIRED");
    }

    SurveyResponseResponse surveyResponse = surveyResponseService.getSurveyResponseById(request.getSurveyResponseId());
    CreateEmploymentVerificationRequestRequest details = request.getVerificationRequestDetails();

    if (details.getTraineeId() == null) {
      details.setTraineeId(surveyResponse.getTraineeId());
    } else if (!details.getTraineeId().equals(surveyResponse.getTraineeId())) {
      throw new BadRequestException(
          "Trainee ID does not match survey response trainee",
          "TRAINEE_SURVEY_MISMATCH"
      );
    }

    if (details.getEmploymentRecordId() == null) {
      throw new BadRequestException("Employment ID is required for verification request", "EMPLOYMENT_ID_REQUIRED");
    }

    EmploymentRecordResponse emp = employmentRecordService.getEmploymentRecordById(details.getEmploymentRecordId());
    if (!emp.traineeId().equals(surveyResponse.getTraineeId())) {
      throw new BadRequestException(
          "Employment record does not belong to survey respondent trainee",
          "TRAINEE_EMPLOYMENT_MISMATCH"
      );
    }

    List<EmploymentVerificationRequestResponse> existingRequests =
        employmentVerificationRequestService.getRequestsByEmploymentId(emp.id());

    boolean hasOpenRequest = existingRequests.stream()
        .anyMatch(r -> Boolean.TRUE.equals(r.isOpenFlag())
            || "REQUESTED".equalsIgnoreCase(r.statusCode())
            || "IN_PROGRESS".equalsIgnoreCase(r.statusCode()));
    if (hasOpenRequest) {
      throw new ConflictException(
          "An open verification request already exists for this employment record",
          "OPEN_REQUEST_EXISTS"
      );
    }

    boolean surveyAlreadyInitiated = existingRequests.stream()
        .anyMatch(r -> surveyResponse.getId().equals(r.surveyResponseId()));
    if (surveyAlreadyInitiated) {
      throw new ConflictException(
          "A verification request has already been initiated for this survey response",
          "OPEN_REQUEST_EXISTS"
      );
    }

    details.setSurveyResponseId(surveyResponse.getId());
    if (details.getFollowupTaskId() == null) {
      details.setFollowupTaskId(surveyResponse.getFollowupTaskId());
    }
    if (details.getPlacementRecordId() == null) {
      details.setPlacementRecordId(emp.placementId());
    }
    if (details.getEmployerId() == null) {
      details.setEmployerId(emp.employerId());
    }

    return employmentVerificationRequestService.createVerificationRequest(details);
  }

  @Override
  @Transactional
  public RecordVerificationVerdictResponse recordVerificationVerdictAndSynchronize(
      RecordVerificationVerdictRequest request) {
    if (request == null) {
      throw new BadRequestException("Request body cannot be null", "REQUEST_BODY_NULL");
    }
    if (request.getVerificationRequest() == null) {
      throw new BadRequestException("Verification request is required", "VERIFICATION_REQUEST_REQUIRED");
    }

    CreateEmploymentVerificationRequest verReq = request.getVerificationRequest();

    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    if (authentication == null || !authentication.isAuthenticated() || "anonymousUser".equals(authentication.getPrincipal())) {
      throw new InvalidCredentialsException("User is not authenticated");
    }

    if (this.userRepository == null) {
      throw new IllegalStateException("UserRepository is required for verifier validation");
    }
    if (this.refUserStatusRepository == null) {
      throw new IllegalStateException("RefUserStatusRepository is required for verifier validation");
    }

    Long authenticatedUserId = resolveAuthenticatedUserId(authentication);

    User verifierUser = userRepository.findById(authenticatedUserId)
        .orElseThrow(() -> new ResourceNotFoundException("User", "id"));

    if (verifierUser.getDeletedAt() != null) {
      throw new InvalidCredentialsException("User account has been deleted");
    }

    if (verifierUser.getUserStatusId() == null) {
      throw new InvalidCredentialsException("User account status reference is missing");
    }

    RefUserStatus status = refUserStatusRepository.findById(verifierUser.getUserStatusId())
        .orElseThrow(() -> new InvalidCredentialsException("User account status record not found"));

    if (status.getStatusCode() == null || !"ACTIVE".equalsIgnoreCase(status.getStatusCode())) {
      throw new InvalidCredentialsException("User account is not active");
    }

    if (verifierUser.getLockedUntil() != null && verifierUser.getLockedUntil().isAfter(LocalDateTime.now())) {
      throw new InvalidCredentialsException("User account is locked");
    }

    // Authoritative verifier identity derived from authenticated security context
    verReq.setVerifiedByUserId(authenticatedUserId);

    EmploymentVerificationResponse verification = employmentVerificationService.createEmploymentVerification(verReq);

    List<EmploymentVerificationEvidenceResponse> evidenceList = new ArrayList<>();
    if (request.getEvidenceRequests() != null && !request.getEvidenceRequests().isEmpty()) {
      for (CreateEmploymentVerificationEvidenceRequest ev : request.getEvidenceRequests()) {
        ev.setEmploymentVerificationId(verification.id());
        evidenceList.add(employmentVerificationEvidenceService.createVerificationEvidence(ev));
      }
    }

    EmploymentVerificationRequestResponse updatedRequest = null;
    if (request.getRequestStatusId() != null && verification.employmentVerificationRequestId() != null) {
      UpdateEmploymentVerificationRequestRequest updateReq = new UpdateEmploymentVerificationRequestRequest();
      updateReq.setStatusId(request.getRequestStatusId());
      updateReq.setCompletedAt(LocalDateTime.now());
      updatedRequest = employmentVerificationRequestService.updateVerificationRequest(
          verification.employmentVerificationRequestId(), updateReq);
    }

    EmploymentRecordResponse updatedEmployment = null;
    if (request.getEmploymentRecordStatusId() != null) {
      EmploymentRecordResponse emp = employmentRecordService.getEmploymentRecordById(verification.employmentRecordId());

      UpdateEmploymentRecordRequest updateEmp = new UpdateEmploymentRecordRequest();
      updateEmp.setEmployerBranchId(emp.employerBranchId());
      updateEmp.setJobRoleId(emp.jobRoleId());
      updateEmp.setEngagementTypeId(emp.engagementTypeId());
      updateEmp.setEmploymentSpellStatusId(emp.employmentSpellStatusId());
      updateEmp.setStartDate(emp.startDate());
      updateEmp.setEndDate(emp.endDate());
      updateEmp.setIsCurrent(emp.isCurrent());
      updateEmp.setStartingSalary(emp.startingSalary());
      updateEmp.setSalaryFrequencyId(emp.salaryFrequencyId());
      updateEmp.setCurrencyCode(emp.currencyCode());
      updateEmp.setWorkLocationId(emp.workLocationId());
      updateEmp.setWorkStateId(emp.workStateId());
      updateEmp.setWorkDistrictId(emp.workDistrictId());
      updateEmp.setEmploymentInfoSourceId(emp.employmentInfoSourceId());
      updateEmp.setRecordVerificationStatusId(request.getEmploymentRecordStatusId());
      updateEmp.setVerifiedAt(LocalDateTime.now());
      updateEmp.setVerifiedByUserId(verification.verifiedByUserId());
      updateEmp.setEmploymentExitReasonId(emp.employmentExitReasonId());
      updateEmp.setExitRemarks(emp.exitRemarks());

      updatedEmployment = employmentRecordService.updateEmploymentRecord(emp.id(), updateEmp);
    }

    return new RecordVerificationVerdictResponse(verification, evidenceList, updatedRequest, updatedEmployment);
  }

  @Override
  public EmploymentVerificationSummaryResponse getEmploymentVerificationSummary(Long employmentId) {
    if (employmentId == null) {
      throw new BadRequestException("Employment ID is required", "EMPLOYMENT_ID_REQUIRED");
    }

    EmploymentRecordResponse emp = employmentRecordService.getEmploymentRecordById(employmentId);
    List<EmploymentVerificationRequestResponse> requests = employmentVerificationRequestService.getRequestsByEmploymentId(employmentId);
    List<EmploymentVerificationResponse> verifications = employmentVerificationService.getVerificationsByEmploymentId(employmentId);

    List<EmploymentVerificationAttemptResponse> attempts = new ArrayList<>();
    for (EmploymentVerificationRequestResponse req : requests) {
      attempts.addAll(employmentVerificationAttemptService.getAttemptsByRequestId(req.id()));
    }

    return new EmploymentVerificationSummaryResponse(emp, requests, attempts, verifications);
  }

  private Long resolveAuthenticatedUserId(Authentication authentication) {
    Object principal = authentication.getPrincipal();
    if (principal instanceof UserPrincipal up) {
      if (up.getId() != null) {
        return up.getId();
      }
      if (up.getUsername() != null && !up.getUsername().isBlank()) {
        return userRepository.findByUsernameOrEmail(up.getUsername(), up.getUsername())
            .map(User::getId)
            .orElseThrow(() -> new ResourceNotFoundException("User", "username"));
      }
    } else if (principal instanceof org.springframework.security.core.userdetails.UserDetails ud) {
      return userRepository.findByUsernameOrEmail(ud.getUsername(), ud.getUsername())
          .map(User::getId)
          .orElseThrow(() -> new ResourceNotFoundException("User", "username"));
    } else if (principal instanceof String str && !"anonymousUser".equals(str)) {
      return userRepository.findByUsernameOrEmail(str, str)
          .map(User::getId)
          .orElseThrow(() -> new ResourceNotFoundException("User", "username"));
    }
    throw new InvalidCredentialsException("Unable to resolve authenticated user identity");
  }
}
