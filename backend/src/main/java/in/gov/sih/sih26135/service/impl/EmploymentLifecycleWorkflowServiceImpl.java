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
import in.gov.sih.sih26135.entity.TraineeUnemploymentEvent;
import in.gov.sih.sih26135.exception.BadRequestException;
import in.gov.sih.sih26135.exception.ConflictException;
import in.gov.sih.sih26135.exception.ResourceNotFoundException;
import in.gov.sih.sih26135.repository.TraineeUnemploymentEventRepository;
import in.gov.sih.sih26135.service.EmploymentExitEventService;
import in.gov.sih.sih26135.service.EmploymentLifecycleWorkflowService;
import in.gov.sih.sih26135.service.EmploymentRecordService;
import in.gov.sih.sih26135.service.SalaryHistoryService;
import in.gov.sih.sih26135.service.TraineeUnemploymentEventService;
import in.gov.sih.sih26135.security.WorkflowSecurityScopeValidator;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class EmploymentLifecycleWorkflowServiceImpl implements EmploymentLifecycleWorkflowService {

  private final EmploymentRecordService employmentRecordService;
  private final EmploymentExitEventService employmentExitEventService;
  private final TraineeUnemploymentEventService traineeUnemploymentEventService;
  private final SalaryHistoryService salaryHistoryService;
  private final TraineeUnemploymentEventRepository traineeUnemploymentEventRepository;
  private final WorkflowSecurityScopeValidator scopeValidator;

  @Autowired
  public EmploymentLifecycleWorkflowServiceImpl(
      EmploymentRecordService employmentRecordService,
      EmploymentExitEventService employmentExitEventService,
      TraineeUnemploymentEventService traineeUnemploymentEventService,
      SalaryHistoryService salaryHistoryService,
      TraineeUnemploymentEventRepository traineeUnemploymentEventRepository,
      WorkflowSecurityScopeValidator scopeValidator) {
    this.employmentRecordService = employmentRecordService;
    this.employmentExitEventService = employmentExitEventService;
    this.traineeUnemploymentEventService = traineeUnemploymentEventService;
    this.salaryHistoryService = salaryHistoryService;
    this.traineeUnemploymentEventRepository = traineeUnemploymentEventRepository;
    this.scopeValidator = scopeValidator;
  }

  public EmploymentLifecycleWorkflowServiceImpl(
      EmploymentRecordService employmentRecordService,
      EmploymentExitEventService employmentExitEventService,
      TraineeUnemploymentEventService traineeUnemploymentEventService,
      SalaryHistoryService salaryHistoryService,
      TraineeUnemploymentEventRepository traineeUnemploymentEventRepository) {
    this(
        employmentRecordService,
        employmentExitEventService,
        traineeUnemploymentEventService,
        salaryHistoryService,
        traineeUnemploymentEventRepository,
        null
    );
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

    if (request.getUnemploymentEventRequest() != null) {
      CreateTraineeUnemploymentEventRequest unempReq = request.getUnemploymentEventRequest();
      if (unempReq.getTraineeId() != null
          && request.getExitEventRequest().getTraineeId() != null
          && !unempReq.getTraineeId().equals(request.getExitEventRequest().getTraineeId())) {
        throw new BadRequestException(
            "Unemployment event trainee does not match exit event trainee",
            "TRAINEE_EXIT_UNEMPLOYMENT_MISMATCH"
        );
      }
      if (unempReq.getStartDate() != null
          && request.getExitEventRequest().getSeparationDate() != null
          && unempReq.getStartDate().isBefore(request.getExitEventRequest().getSeparationDate())) {
        throw new BadRequestException(
            "Unemployment start date cannot be before separation date",
            "UNEMPLOYMENT_START_BEFORE_SEPARATION"
        );
      }
    }

    ensureScopeValidator();
    if (request.getExitEventRequest().getEmploymentId() == null) {
      throw new BadRequestException("Employment ID is required for exit event", "EMPLOYMENT_ID_REQUIRED");
    }
    EmploymentRecordResponse emp = employmentRecordService.getEmploymentRecordById(
        request.getExitEventRequest().getEmploymentId());
    if (emp == null) {
      throw new ResourceNotFoundException("EmploymentRecord", "employmentId");
    }
    if (emp.employerId() == null) {
      throw new BadRequestException("Employment record has no associated employer", "EMPLOYER_REQUIRED");
    }
    scopeValidator.validateEmployerScope(emp.employerId());

    if (request.getExitEventRequest().getTraineeId() == null) {
      request.getExitEventRequest().setTraineeId(emp.traineeId());
    } else if (!request.getExitEventRequest().getTraineeId().equals(emp.traineeId())) {
      throw new BadRequestException(
          "Exit event trainee does not match employment record trainee",
          "TRAINEE_EMPLOYMENT_MISMATCH"
      );
    }
    if (emp.traineeId() == null) {
      throw new BadRequestException("Employment record has no associated trainee", "TRAINEE_REQUIRED");
    }
    scopeValidator.validateTraineeScope(emp.traineeId());

    if (request.getUnemploymentEventRequest() != null) {
      CreateTraineeUnemploymentEventRequest unempReq = request.getUnemploymentEventRequest();
      if (unempReq.getTraineeId() != null && !unempReq.getTraineeId().equals(emp.traineeId())) {
        throw new BadRequestException(
            "Unemployment event trainee does not match exit event trainee",
            "TRAINEE_EXIT_UNEMPLOYMENT_MISMATCH"
        );
      }
      if (unempReq.getPrecedingEmploymentId() != null && !unempReq.getPrecedingEmploymentId().equals(emp.id())) {
        throw new BadRequestException(
            "Unemployment event preceding employment does not match exit employment",
            "EMPLOYMENT_UNEMPLOYMENT_MISMATCH"
        );
      }
      if (unempReq.getEmploymentExitEventId() != null) {
        throw new BadRequestException(
            "Unemployment event exit event ID must not be pre-set",
            "EXIT_EVENT_ID_PRESET"
        );
      }
      if (unempReq.getEnrollmentId() != null) {
        if (emp.enrollmentId() == null || !unempReq.getEnrollmentId().equals(emp.enrollmentId())) {
          throw new BadRequestException(
              "Unemployment event enrollment does not match employment record enrollment",
              "ENROLLMENT_EMPLOYMENT_MISMATCH"
          );
        }
      }
      if (unempReq.getPlacementId() != null) {
        if (emp.placementId() == null || !unempReq.getPlacementId().equals(emp.placementId())) {
          throw new BadRequestException(
              "Unemployment event placement does not match employment record placement",
              "PLACEMENT_EMPLOYMENT_MISMATCH"
          );
        }
      }
      if (unempReq.getStartDate() != null
          && request.getExitEventRequest().getSeparationDate() != null
          && unempReq.getStartDate().isBefore(request.getExitEventRequest().getSeparationDate())) {
        throw new BadRequestException(
            "Unemployment start date cannot be before separation date",
            "UNEMPLOYMENT_START_BEFORE_SEPARATION"
        );
      }
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
      if (unempReq.getPlacementId() == null) {
        unempReq.setPlacementId(emp.placementId());
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

    ensureScopeValidator();

    TraineeUnemploymentEvent unempEvent = traineeUnemploymentEventRepository.findByIdForUpdate(
        request.getUnemploymentEventId())
        .orElseThrow(() -> new ResourceNotFoundException("TraineeUnemploymentEvent", "unemploymentEventId"));

    if (!Boolean.TRUE.equals(unempEvent.getIsCurrent())
        || unempEvent.getEndDate() != null
        || unempEvent.getSucceedingEmployment() != null) {
      throw new ConflictException(
          "Unemployment event is not currently active and cannot be transitioned to employment",
          "UNEMPLOYMENT_EVENT_NOT_ACTIVE"
      );
    }

    CreateEmploymentRecordRequest newEmpReq = request.getNewEmploymentRequest();
    if (newEmpReq.getTraineeId() == null) {
      newEmpReq.setTraineeId(unempEvent.getTrainee().getId());
    } else if (!newEmpReq.getTraineeId().equals(unempEvent.getTrainee().getId())) {
      throw new BadRequestException(
          "New employment record trainee does not match unemployment event trainee",
          "TRAINEE_UNEMPLOYMENT_MISMATCH"
      );
    }

    ensureScopeValidator();
    if (unempEvent.getTrainee() == null || unempEvent.getTrainee().getId() == null) {
      throw new BadRequestException("Unemployment event has no associated trainee", "TRAINEE_REQUIRED");
    }
    scopeValidator.validateTraineeScope(unempEvent.getTrainee().getId());

    if (newEmpReq.getEmployerId() == null) {
      throw new BadRequestException("Employer ID is required for new employment record", "EMPLOYER_ID_REQUIRED");
    }
    scopeValidator.validateEmployerScope(newEmpReq.getEmployerId());

    if (newEmpReq.getStartDate() != null && newEmpReq.getStartDate().isBefore(unempEvent.getStartDate())) {
      throw new BadRequestException(
          "New employment start date cannot precede unemployment start date",
          "EMPLOYMENT_START_BEFORE_UNEMPLOYMENT"
      );
    }

    if (request.getInitialSalaryRequest() != null) {
      CreateSalaryHistoryRequest salaryReq = request.getInitialSalaryRequest();
      if (salaryReq.getEmploymentId() != null) {
        throw new BadRequestException(
            "Initial salary employment ID must not be pre-set",
            "EMPLOYMENT_ID_PRESET"
        );
      }
      if (salaryReq.getTraineeId() != null && !salaryReq.getTraineeId().equals(unempEvent.getTrainee().getId())) {
        throw new BadRequestException(
            "Initial salary trainee does not match unemployment event trainee",
            "TRAINEE_UNEMPLOYMENT_MISMATCH"
        );
      }
    }

    EmploymentRecordResponse newEmployment = employmentRecordService.createEmploymentRecord(newEmpReq);

    UpdateTraineeUnemploymentEventRequest updateUnemp = new UpdateTraineeUnemploymentEventRequest();
    updateUnemp.setPeriodNumber(unempEvent.getPeriodNumber());
    updateUnemp.setStartDate(unempEvent.getStartDate());
    updateUnemp.setEndDate(newEmployment.startDate());
    updateUnemp.setIsCurrent(false);
    updateUnemp.setLabourStatusId(unempEvent.getLabourStatusId());
    updateUnemp.setUnemploymentReasonId(unempEvent.getUnemploymentReason().getId());
    updateUnemp.setPrecedingEmploymentId(unempEvent.getPrecedingEmployment() != null ? unempEvent.getPrecedingEmployment().getId() : null);
    updateUnemp.setEmploymentExitEventId(unempEvent.getEmploymentExitEvent() != null ? unempEvent.getEmploymentExitEvent().getId() : null);
    updateUnemp.setSucceedingEmploymentId(newEmployment.id());
    updateUnemp.setEnrollmentId(unempEvent.getEnrollment() != null ? unempEvent.getEnrollment().getId() : null);
    updateUnemp.setPlacementId(unempEvent.getPlacementRecord() != null ? unempEvent.getPlacementRecord().getId() : null);
    updateUnemp.setFollowupTaskId(unempEvent.getFollowupTask() != null ? unempEvent.getFollowupTask().getId() : null);
    updateUnemp.setSurveyResponseId(unempEvent.getSurveyResponse() != null ? unempEvent.getSurveyResponse().getId() : null);
    updateUnemp.setEmploymentInfoSourceId(unempEvent.getEmploymentInfoSource().getId());
    updateUnemp.setRecordVerificationStatusId(unempEvent.getRecordVerificationStatus().getId());
    updateUnemp.setVerifiedAt(unempEvent.getVerifiedAt());
    updateUnemp.setVerifiedByUserId(unempEvent.getVerifiedByUserId());
    updateUnemp.setRemarks(unempEvent.getRemarks());

    TraineeUnemploymentEventResponse updatedUnemp = traineeUnemploymentEventService.updateUnemploymentEvent(
        unempEvent.getId(), updateUnemp);

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

    ensureScopeValidator();
    scopeValidator.validateTraineeScope(traineeId);

    List<EmploymentRecordResponse> empRecords = employmentRecordService.getEmploymentRecordsByTrainee(traineeId);
    List<EmploymentExitEventResponse> exitEvents = employmentExitEventService.getExitEventsByTraineeId(traineeId);
    List<TraineeUnemploymentEventResponse> unempEvents = traineeUnemploymentEventService.getUnemploymentEventsByTraineeId(traineeId);

    return new TraineeCareerTimelineResponse(traineeId, empRecords, exitEvents, unempEvents);
  }

  private void ensureScopeValidator() {
    if (this.scopeValidator == null) {
      throw new IllegalStateException("WorkflowSecurityScopeValidator is required for scope validation");
    }
  }
}
