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
import in.gov.sih.sih26135.exception.BadRequestException;
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

  public VerificationWorkflowServiceImpl(
      EmploymentRecordService employmentRecordService,
      FollowupTaskService followupTaskService,
      SurveyResponseService surveyResponseService,
      EmploymentVerificationRequestService employmentVerificationRequestService,
      EmploymentVerificationAttemptService employmentVerificationAttemptService,
      EmploymentVerificationService employmentVerificationService,
      EmploymentVerificationEvidenceService employmentVerificationEvidenceService) {
    this.employmentRecordService = employmentRecordService;
    this.followupTaskService = followupTaskService;
    this.surveyResponseService = surveyResponseService;
    this.employmentVerificationRequestService = employmentVerificationRequestService;
    this.employmentVerificationAttemptService = employmentVerificationAttemptService;
    this.employmentVerificationService = employmentVerificationService;
    this.employmentVerificationEvidenceService = employmentVerificationEvidenceService;
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
}
