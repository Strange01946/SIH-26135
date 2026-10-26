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
import in.gov.sih.sih26135.dto.response.FollowupTaskResponse;
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
import in.gov.sih.sih26135.security.WorkflowSecurityScopeValidator;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
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
  private WorkflowSecurityScopeValidator scopeValidator;

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
    this(
        employmentRecordService,
        followupTaskService,
        surveyResponseService,
        employmentVerificationRequestService,
        employmentVerificationAttemptService,
        employmentVerificationService,
        employmentVerificationEvidenceService,
        userRepository,
        refUserStatusRepository,
        null
    );
  }

  public VerificationWorkflowServiceImpl(
      EmploymentRecordService employmentRecordService,
      FollowupTaskService followupTaskService,
      SurveyResponseService surveyResponseService,
      EmploymentVerificationRequestService employmentVerificationRequestService,
      EmploymentVerificationAttemptService employmentVerificationAttemptService,
      EmploymentVerificationService employmentVerificationService,
      EmploymentVerificationEvidenceService employmentVerificationEvidenceService,
      UserRepository userRepository,
      RefUserStatusRepository refUserStatusRepository,
      WorkflowSecurityScopeValidator scopeValidator) {
    this.employmentRecordService = employmentRecordService;
    this.followupTaskService = followupTaskService;
    this.surveyResponseService = surveyResponseService;
    this.employmentVerificationRequestService = employmentVerificationRequestService;
    this.employmentVerificationAttemptService = employmentVerificationAttemptService;
    this.employmentVerificationService = employmentVerificationService;
    this.employmentVerificationEvidenceService = employmentVerificationEvidenceService;
    this.userRepository = userRepository;
    this.refUserStatusRepository = refUserStatusRepository;
    this.scopeValidator = scopeValidator;
  }

  @Autowired
  public void setScopeValidator(WorkflowSecurityScopeValidator scopeValidator) {
    this.scopeValidator = scopeValidator;
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

    ensureScopeValidator();
    SurveyResponseResponse surveyResponse = surveyResponseService.getSurveyResponseById(request.getSurveyResponseId());
    if (surveyResponse == null) {
      throw new ResourceNotFoundException("SurveyResponse", "surveyResponseId");
    }
    if (surveyResponse.getTraineeId() == null) {
      throw new BadRequestException("Survey response has no associated trainee", "TRAINEE_REQUIRED");
    }
    scopeValidator.validateTraineeScope(surveyResponse.getTraineeId());

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
    if (emp == null) {
      throw new ResourceNotFoundException("EmploymentRecord", "employmentRecordId");
    }
    if (emp.employerId() == null) {
      throw new BadRequestException("Employment record has no associated employer", "EMPLOYER_REQUIRED");
    }
    if (emp.traineeId() == null) {
      throw new BadRequestException("Employment record has no associated trainee", "TRAINEE_REQUIRED");
    }
    scopeValidator.validateEmployerScope(emp.employerId());
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

    if (details.getFollowupTaskId() != null) {
      if (surveyResponse.getFollowupTaskId() == null || !details.getFollowupTaskId().equals(surveyResponse.getFollowupTaskId())) {
        throw new BadRequestException(
            "Followup task ID does not match survey response followup task",
            "FOLLOWUP_TASK_MISMATCH"
        );
      }
    }
    if (details.getPlacementRecordId() != null) {
      if (emp.placementId() == null || !details.getPlacementRecordId().equals(emp.placementId())) {
        throw new BadRequestException(
            "Placement record ID does not match employment record placement",
            "PLACEMENT_EMPLOYMENT_MISMATCH"
        );
      }
    }
    if (details.getEmployerId() != null && !details.getEmployerId().equals(emp.employerId())) {
      throw new BadRequestException(
          "Employer ID does not match employment record employer",
          "EMPLOYER_EMPLOYMENT_MISMATCH"
      );
    }

    if (surveyResponse.getFollowupTaskId() != null) {
      FollowupTaskResponse followupTask = followupTaskService.getFollowupTaskById(surveyResponse.getFollowupTaskId());
      if (followupTask == null) {
        throw new ResourceNotFoundException("FollowupTask", "followupTaskId");
      }
      if (followupTask.getTraineeId() == null) {
        throw new BadRequestException(
            "Followup task does not have an associated trainee",
            "TRAINEE_REQUIRED"
        );
      }
      if (!followupTask.getTraineeId().equals(emp.traineeId())) {
        throw new BadRequestException(
            "Followup task trainee does not match employment record trainee",
            "TRAINEE_TASK_MISMATCH"
        );
      }
      if (followupTask.getEmploymentId() != null && !followupTask.getEmploymentId().equals(emp.id())) {
        throw new BadRequestException(
            "Followup task employment does not match employment record",
            "EMPLOYMENT_TASK_MISMATCH"
        );
      }
      if (followupTask.getPlacementId() != null) {
        if (emp.placementId() == null || !followupTask.getPlacementId().equals(emp.placementId())) {
          throw new BadRequestException(
              "Followup task placement does not match employment record placement",
              "PLACEMENT_TASK_MISMATCH"
          );
        }
      }
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

    ensureScopeValidator();
    Long targetEmploymentId = verReq.getEmploymentRecordId();
    EmploymentVerificationRequestResponse vReq = null;
    if (verReq.getEmploymentVerificationRequestId() != null) {
      vReq = employmentVerificationRequestService
          .getVerificationRequestById(verReq.getEmploymentVerificationRequestId());
      if (vReq == null) {
        throw new ResourceNotFoundException("EmploymentVerificationRequest", "employmentVerificationRequestId");
      }
      if (targetEmploymentId != null && !targetEmploymentId.equals(vReq.employmentRecordId())) {
        throw new BadRequestException(
            "Employment verification request does not belong to target employment record",
            "EMPLOYMENT_VERIFICATION_REQUEST_MISMATCH"
        );
      }
      if (targetEmploymentId == null) {
        targetEmploymentId = vReq.employmentRecordId();
        verReq.setEmploymentRecordId(targetEmploymentId);
      }
    }
    if (targetEmploymentId == null) {
      throw new BadRequestException("Employment record ID is required to record verification verdict", "EMPLOYMENT_ID_REQUIRED");
    }
    EmploymentRecordResponse emp = employmentRecordService.getEmploymentRecordById(targetEmploymentId);
    if (emp == null) {
      throw new ResourceNotFoundException("EmploymentRecord", "employmentRecordId");
    }
    if (emp.employerId() == null) {
      throw new BadRequestException("Employment record has no associated employer", "EMPLOYER_REQUIRED");
    }
    if (emp.traineeId() == null) {
      throw new BadRequestException("Employment record has no associated trainee", "TRAINEE_REQUIRED");
    }
    scopeValidator.validateEmployerScope(emp.employerId());

    if (vReq != null) {
      if (vReq.traineeId() == null) {
        throw new BadRequestException(
            "Employment verification request has no associated trainee",
            "TRAINEE_REQUIRED"
        );
      }
      if (!vReq.traineeId().equals(emp.traineeId())) {
        throw new BadRequestException(
            "Verification request trainee does not match employment record trainee",
            "TRAINEE_EMPLOYMENT_MISMATCH"
        );
      }
      if (vReq.employerId() == null) {
        throw new BadRequestException(
            "Employment verification request has no associated employer",
            "EMPLOYER_REQUIRED"
        );
      }
      if (!vReq.employerId().equals(emp.employerId())) {
        throw new BadRequestException(
            "Verification request employer does not match employment record employer",
            "EMPLOYER_EMPLOYMENT_MISMATCH"
        );
      }
      if (emp.placementId() != null) {
        if (vReq.placementRecordId() == null || !vReq.placementRecordId().equals(emp.placementId())) {
          throw new BadRequestException(
              "Verification request placement does not match employment record placement",
              "PLACEMENT_EMPLOYMENT_MISMATCH"
          );
        }
      } else if (vReq.placementRecordId() != null) {
        throw new BadRequestException(
            "Verification request placement does not match employment record placement",
            "PLACEMENT_EMPLOYMENT_MISMATCH"
        );
      }
    }
    if (verReq.getTraineeId() == null) {
      verReq.setTraineeId(emp.traineeId());
    } else if (!verReq.getTraineeId().equals(emp.traineeId())) {
      throw new BadRequestException(
          "Verification trainee does not match employment record trainee",
          "TRAINEE_EMPLOYMENT_MISMATCH"
      );
    }
    if (verReq.getEmployerId() == null) {
      verReq.setEmployerId(emp.employerId());
    } else if (!verReq.getEmployerId().equals(emp.employerId())) {
      throw new BadRequestException(
          "Verification employer does not match employment record employer",
          "EMPLOYER_EMPLOYMENT_MISMATCH"
      );
    }
    if (verReq.getPlacementRecordId() == null) {
      verReq.setPlacementRecordId(emp.placementId());
    } else if (emp.placementId() == null || !verReq.getPlacementRecordId().equals(emp.placementId())) {
      throw new BadRequestException(
          "Verification placement does not match employment record placement",
          "PLACEMENT_EMPLOYMENT_MISMATCH"
      );
    }
    if (request.getEmploymentRecordStatusId() != null && verReq.getRecordVerificationStatusId() != null
        && !request.getEmploymentRecordStatusId().equals(verReq.getRecordVerificationStatusId())) {
      throw new BadRequestException(
          "Employment record verification status cannot contradict verification verdict status",
          "VERIFICATION_STATUS_CONFLICT"
      );
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
      EmploymentRecordResponse currentEmp = employmentRecordService.getEmploymentRecordById(verification.employmentRecordId());

      UpdateEmploymentRecordRequest updateEmp = new UpdateEmploymentRecordRequest();
      updateEmp.setEmployerBranchId(currentEmp.employerBranchId());
      updateEmp.setJobRoleId(currentEmp.jobRoleId());
      updateEmp.setEngagementTypeId(currentEmp.engagementTypeId());
      updateEmp.setEmploymentSpellStatusId(currentEmp.employmentSpellStatusId());
      updateEmp.setStartDate(currentEmp.startDate());
      updateEmp.setEndDate(currentEmp.endDate());
      updateEmp.setIsCurrent(currentEmp.isCurrent());
      updateEmp.setStartingSalary(currentEmp.startingSalary());
      updateEmp.setSalaryFrequencyId(currentEmp.salaryFrequencyId());
      updateEmp.setCurrencyCode(currentEmp.currencyCode());
      updateEmp.setWorkLocationId(currentEmp.workLocationId());
      updateEmp.setWorkStateId(currentEmp.workStateId());
      updateEmp.setWorkDistrictId(currentEmp.workDistrictId());
      updateEmp.setEmploymentInfoSourceId(currentEmp.employmentInfoSourceId());
      updateEmp.setRecordVerificationStatusId(request.getEmploymentRecordStatusId());
      updateEmp.setVerifiedAt(LocalDateTime.now());
      updateEmp.setVerifiedByUserId(verification.verifiedByUserId());
      updateEmp.setEmploymentExitReasonId(currentEmp.employmentExitReasonId());
      updateEmp.setExitRemarks(currentEmp.exitRemarks());

      updatedEmployment = employmentRecordService.updateEmploymentRecord(currentEmp.id(), updateEmp);
    }

    return new RecordVerificationVerdictResponse(verification, evidenceList, updatedRequest, updatedEmployment);
  }

  @Override
  public EmploymentVerificationSummaryResponse getEmploymentVerificationSummary(Long employmentId) {
    if (employmentId == null) {
      throw new BadRequestException("Employment ID is required", "EMPLOYMENT_ID_REQUIRED");
    }

    ensureScopeValidator();
    EmploymentRecordResponse emp = employmentRecordService.getEmploymentRecordById(employmentId);
    if (emp == null) {
      throw new ResourceNotFoundException("EmploymentRecord", "employmentId");
    }
    if (emp.employerId() == null) {
      throw new BadRequestException("Employment record has no associated employer", "EMPLOYER_REQUIRED");
    }
    scopeValidator.validateEmployerScope(emp.employerId());
    List<EmploymentVerificationRequestResponse> requests = employmentVerificationRequestService.getRequestsByEmploymentId(employmentId);
    List<EmploymentVerificationResponse> verifications = employmentVerificationService.getVerificationsByEmploymentId(employmentId);

    List<EmploymentVerificationAttemptResponse> attempts = new ArrayList<>();
    for (EmploymentVerificationRequestResponse req : requests) {
      attempts.addAll(employmentVerificationAttemptService.getAttemptsByRequestId(req.id()));
    }

    return new EmploymentVerificationSummaryResponse(emp, requests, attempts, verifications);
  }

  private void ensureScopeValidator() {
    if (this.scopeValidator == null) {
      throw new IllegalStateException("WorkflowSecurityScopeValidator is required for scope validation");
    }
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
