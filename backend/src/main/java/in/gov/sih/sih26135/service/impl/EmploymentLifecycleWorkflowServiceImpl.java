package in.gov.sih.sih26135.service.impl;

import in.gov.sih.sih26135.dto.request.CreateEmploymentRecordRequest;
import in.gov.sih.sih26135.dto.request.CreateSalaryHistoryRequest;
import in.gov.sih.sih26135.dto.request.CreateTraineeUnemploymentEventRequest;
import in.gov.sih.sih26135.dto.request.EmploymentExitAndUnemploymentRequest;
import in.gov.sih.sih26135.dto.request.UnemploymentToEmploymentTransitionRequest;
import in.gov.sih.sih26135.dto.request.UpdateTraineeUnemploymentEventRequest;
import in.gov.sih.sih26135.dto.response.EmploymentExitAndUnemploymentResponse;
import in.gov.sih.sih26135.dto.response.EmploymentExitEventResponse;
import in.gov.sih.sih26135.dto.response.EmploymentRecordResponse;
import in.gov.sih.sih26135.dto.response.SalaryHistoryResponse;
import in.gov.sih.sih26135.dto.response.TraineeCareerTimelineResponse;
import in.gov.sih.sih26135.dto.response.TraineeUnemploymentEventResponse;
import in.gov.sih.sih26135.dto.response.UnemploymentToEmploymentTransitionResponse;
import in.gov.sih.sih26135.exception.BadRequestException;
import in.gov.sih.sih26135.service.EmploymentExitEventService;
import in.gov.sih.sih26135.service.EmploymentLifecycleWorkflowService;
import in.gov.sih.sih26135.service.EmploymentRecordService;
import in.gov.sih.sih26135.service.SalaryHistoryService;
import in.gov.sih.sih26135.service.TraineeUnemploymentEventService;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class EmploymentLifecycleWorkflowServiceImpl implements EmploymentLifecycleWorkflowService {

  private final EmploymentRecordService employmentRecordService;
  private final EmploymentExitEventService employmentExitEventService;
  private final TraineeUnemploymentEventService traineeUnemploymentEventService;
  private final SalaryHistoryService salaryHistoryService;

  public EmploymentLifecycleWorkflowServiceImpl(
      EmploymentRecordService employmentRecordService,
      EmploymentExitEventService employmentExitEventService,
      TraineeUnemploymentEventService traineeUnemploymentEventService,
      SalaryHistoryService salaryHistoryService) {
    this.employmentRecordService = employmentRecordService;
    this.employmentExitEventService = employmentExitEventService;
    this.traineeUnemploymentEventService = traineeUnemploymentEventService;
    this.salaryHistoryService = salaryHistoryService;
  }

  @Override
  @Transactional
  public EmploymentExitAndUnemploymentResponse recordExitAndInitiateUnemployment(
      EmploymentExitAndUnemploymentRequest request) {
    if (request == null) {
      throw new BadRequestException("Request body cannot be null", "REQUEST_BODY_NULL");
    }
    if (request.getExitEventRequest() == null) {
      throw new BadRequestException("Exit event request is required", "EXIT_EVENT_REQUEST_REQUIRED");
    }

    EmploymentExitEventResponse exitEvent = employmentExitEventService.createExitEvent(request.getExitEventRequest());

    TraineeUnemploymentEventResponse unemploymentEvent = null;
    if (request.getUnemploymentEventRequest() != null) {
      CreateTraineeUnemploymentEventRequest unempReq = request.getUnemploymentEventRequest();

      if (unempReq.getTraineeId() == null) {
        unempReq.setTraineeId(exitEvent.traineeId());
      } else if (!unempReq.getTraineeId().equals(exitEvent.traineeId())) {
        throw new BadRequestException(
            "Unemployment event trainee does not match exit event trainee",
            "TRAINEE_EXIT_UNEMPLOYMENT_MISMATCH"
        );
      }

      unempReq.setPrecedingEmploymentId(exitEvent.employmentId());
      unempReq.setEmploymentExitEventId(exitEvent.id());

      if (unempReq.getEnrollmentId() == null) {
        unempReq.setEnrollmentId(exitEvent.enrollmentId());
      }

      if (unempReq.getStartDate() == null) {
        unempReq.setStartDate(exitEvent.separationDate());
      } else if (unempReq.getStartDate().isBefore(exitEvent.separationDate())) {
        throw new BadRequestException(
            "Unemployment start date cannot be before separation date",
            "UNEMPLOYMENT_START_BEFORE_SEPARATION"
        );
      }

      unemploymentEvent = traineeUnemploymentEventService.createUnemploymentEvent(unempReq);
    }

    return new EmploymentExitAndUnemploymentResponse(exitEvent, unemploymentEvent);
  }

  @Override
  @Transactional
  public UnemploymentToEmploymentTransitionResponse transitionUnemploymentToEmployment(
      UnemploymentToEmploymentTransitionRequest request) {
    if (request == null) {
      throw new BadRequestException("Request body cannot be null", "REQUEST_BODY_NULL");
    }
    if (request.getUnemploymentEventId() == null) {
      throw new BadRequestException("Unemployment event ID is required", "UNEMPLOYMENT_EVENT_ID_REQUIRED");
    }
    if (request.getNewEmploymentRequest() == null) {
      throw new BadRequestException("New employment request is required", "NEW_EMPLOYMENT_REQUEST_REQUIRED");
    }

    TraineeUnemploymentEventResponse unempEvent = traineeUnemploymentEventService.getUnemploymentEventById(
        request.getUnemploymentEventId());

    CreateEmploymentRecordRequest newEmpReq = request.getNewEmploymentRequest();
    if (newEmpReq.getTraineeId() == null) {
      newEmpReq.setTraineeId(unempEvent.traineeId());
    } else if (!newEmpReq.getTraineeId().equals(unempEvent.traineeId())) {
      throw new BadRequestException(
          "New employment record trainee does not match unemployment event trainee",
          "TRAINEE_UNEMPLOYMENT_MISMATCH"
      );
    }

    if (newEmpReq.getStartDate() != null && newEmpReq.getStartDate().isBefore(unempEvent.startDate())) {
      throw new BadRequestException(
          "New employment start date cannot precede unemployment start date",
          "EMPLOYMENT_START_BEFORE_UNEMPLOYMENT"
      );
    }

    EmploymentRecordResponse newEmployment = employmentRecordService.createEmploymentRecord(newEmpReq);

    UpdateTraineeUnemploymentEventRequest updateUnemp = new UpdateTraineeUnemploymentEventRequest();
    updateUnemp.setPeriodNumber(unempEvent.periodNumber());
    updateUnemp.setStartDate(unempEvent.startDate());
    updateUnemp.setEndDate(newEmployment.startDate());
    updateUnemp.setIsCurrent(false);
    updateUnemp.setLabourStatusId(unempEvent.labourStatusId());
    updateUnemp.setUnemploymentReasonId(unempEvent.unemploymentReasonId());
    updateUnemp.setPrecedingEmploymentId(unempEvent.precedingEmploymentId());
    updateUnemp.setEmploymentExitEventId(unempEvent.employmentExitEventId());
    updateUnemp.setSucceedingEmploymentId(newEmployment.id());
    updateUnemp.setEnrollmentId(unempEvent.enrollmentId());
    updateUnemp.setPlacementId(unempEvent.placementId());
    updateUnemp.setFollowupTaskId(unempEvent.followupTaskId());
    updateUnemp.setSurveyResponseId(unempEvent.surveyResponseId());
    updateUnemp.setEmploymentInfoSourceId(unempEvent.employmentInfoSourceId());
    updateUnemp.setRecordVerificationStatusId(unempEvent.recordVerificationStatusId());
    updateUnemp.setVerifiedAt(unempEvent.verifiedAt());
    updateUnemp.setVerifiedByUserId(unempEvent.verifiedByUserId());
    updateUnemp.setRemarks(unempEvent.remarks());

    TraineeUnemploymentEventResponse updatedUnemp = traineeUnemploymentEventService.updateUnemploymentEvent(
        unempEvent.id(), updateUnemp);

    SalaryHistoryResponse initialSalary = null;
    if (request.getInitialSalaryRequest() != null) {
      CreateSalaryHistoryRequest salaryReq = request.getInitialSalaryRequest();
      salaryReq.setEmploymentId(newEmployment.id());
      salaryReq.setTraineeId(newEmployment.traineeId());
      if (salaryReq.getEffectiveFrom() == null) {
        salaryReq.setEffectiveFrom(newEmployment.startDate());
      }
      if (salaryReq.getSalaryAmount() == null) {
        salaryReq.setSalaryAmount(newEmployment.startingSalary());
      }
      if (salaryReq.getSalaryFrequencyId() == null) {
        salaryReq.setSalaryFrequencyId(newEmployment.salaryFrequencyId());
      }
      if (salaryReq.getCurrencyCode() == null) {
        salaryReq.setCurrencyCode(newEmployment.currencyCode());
      }
      initialSalary = salaryHistoryService.createSalaryHistory(salaryReq);
    }

    return new UnemploymentToEmploymentTransitionResponse(updatedUnemp, newEmployment, initialSalary);
  }

  @Override
  public TraineeCareerTimelineResponse getTraineeCareerTimeline(Long traineeId) {
    if (traineeId == null) {
      throw new BadRequestException("Trainee ID is required", "TRAINEE_ID_REQUIRED");
    }

    List<EmploymentRecordResponse> empRecords = employmentRecordService.getEmploymentRecordsByTrainee(traineeId);
    List<EmploymentExitEventResponse> exitEvents = employmentExitEventService.getExitEventsByTraineeId(traineeId);
    List<TraineeUnemploymentEventResponse> unempEvents = traineeUnemploymentEventService.getUnemploymentEventsByTraineeId(traineeId);

    return new TraineeCareerTimelineResponse(traineeId, empRecords, exitEvents, unempEvents);
  }
}
