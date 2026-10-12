package in.gov.sih.sih26135.service.impl;

import in.gov.sih.sih26135.dto.request.CreateEmploymentRecordRequest;
import in.gov.sih.sih26135.dto.request.CreateSalaryHistoryRequest;
import in.gov.sih.sih26135.dto.request.PlacementToEmploymentTransitionRequest;
import in.gov.sih.sih26135.dto.request.UpdatePlacementRecordRequest;
import in.gov.sih.sih26135.dto.response.EmploymentRecordResponse;
import in.gov.sih.sih26135.dto.response.PlacementRecordResponse;
import in.gov.sih.sih26135.dto.response.PlacementToEmploymentTransitionResponse;
import in.gov.sih.sih26135.dto.response.SalaryHistoryResponse;
import in.gov.sih.sih26135.dto.response.TraineePlacementEmploymentSummaryResponse;
import in.gov.sih.sih26135.entity.RefJoiningStatus;
import in.gov.sih.sih26135.entity.RefPlacementStatus;
import in.gov.sih.sih26135.exception.BadRequestException;
import in.gov.sih.sih26135.exception.ConflictException;
import in.gov.sih.sih26135.exception.ResourceNotFoundException;
import in.gov.sih.sih26135.repository.EmploymentRecordRepository;
import in.gov.sih.sih26135.repository.RefJoiningStatusRepository;
import in.gov.sih.sih26135.repository.RefPlacementStatusRepository;
import in.gov.sih.sih26135.service.EmploymentRecordService;
import in.gov.sih.sih26135.service.PlacementEmploymentWorkflowService;
import in.gov.sih.sih26135.service.PlacementRecordService;
import in.gov.sih.sih26135.service.SalaryHistoryService;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class PlacementEmploymentWorkflowServiceImpl implements PlacementEmploymentWorkflowService {

  private static final Set<String> ALLOWED_PLACEMENT_STATUS_CODES = Set.of("OFFERED", "ACCEPTED", "JOINED");

  private final PlacementRecordService placementRecordService;
  private final EmploymentRecordService employmentRecordService;
  private final SalaryHistoryService salaryHistoryService;
  private final EmploymentRecordRepository employmentRecordRepository;
  private final RefPlacementStatusRepository refPlacementStatusRepository;
  private final RefJoiningStatusRepository refJoiningStatusRepository;

  public PlacementEmploymentWorkflowServiceImpl(
      PlacementRecordService placementRecordService,
      EmploymentRecordService employmentRecordService,
      SalaryHistoryService salaryHistoryService,
      EmploymentRecordRepository employmentRecordRepository,
      RefPlacementStatusRepository refPlacementStatusRepository,
      RefJoiningStatusRepository refJoiningStatusRepository) {
    this.placementRecordService = placementRecordService;
    this.employmentRecordService = employmentRecordService;
    this.salaryHistoryService = salaryHistoryService;
    this.employmentRecordRepository = employmentRecordRepository;
    this.refPlacementStatusRepository = refPlacementStatusRepository;
    this.refJoiningStatusRepository = refJoiningStatusRepository;
  }

  @Override
  @Transactional
  public PlacementToEmploymentTransitionResponse transitionPlacementToEmployment(
      PlacementToEmploymentTransitionRequest request) {
    if (request == null) {
      throw new BadRequestException("Request body cannot be null", "REQUEST_BODY_NULL");
    }
    if (request.getPlacementId() == null) {
      throw new BadRequestException("Placement ID is required", "PLACEMENT_ID_REQUIRED");
    }
    if (request.getEmploymentRecordRequest() == null) {
      throw new BadRequestException("Employment record request is required", "EMPLOYMENT_RECORD_REQUEST_REQUIRED");
    }

    PlacementRecordResponse placement = placementRecordService.getPlacementRecordById(request.getPlacementId());

    if (placement.deletedAt() != null) {
      throw new BadRequestException("Placement record is deleted and cannot be transitioned to employment", "PLACEMENT_RECORD_DELETED");
    }

    if (!ALLOWED_PLACEMENT_STATUS_CODES.contains(placement.placementStatusCode())) {
      throw new BadRequestException(
          "Placement record status '" + placement.placementStatusCode() + "' is not eligible for employment transition",
          "INVALID_PLACEMENT_STATUS"
      );
    }

    if ("NO_SHOW".equalsIgnoreCase(placement.joiningStatusCode())) {
      throw new BadRequestException(
          "Placement record with joining status 'NO_SHOW' cannot be transitioned to employment",
          "INVALID_JOINING_STATUS"
      );
    }

    if (request.getPlacementStatusId() != null) {
      RefPlacementStatus targetStatus = refPlacementStatusRepository.findById(request.getPlacementStatusId())
          .orElseThrow(() -> new ResourceNotFoundException("RefPlacementStatus", "placementStatusId"));
      if (Boolean.TRUE.equals(targetStatus.getIsUnsuccessfulFlag()) || !ALLOWED_PLACEMENT_STATUS_CODES.contains(targetStatus.getStatusCode())) {
        throw new BadRequestException(
            "Target placement status '" + targetStatus.getStatusCode() + "' is not eligible for employment transition",
            "INVALID_PLACEMENT_STATUS"
        );
      }
    }

    if (request.getPlacementJoiningStatusId() != null) {
      RefJoiningStatus targetJoiningStatus = refJoiningStatusRepository.findById(request.getPlacementJoiningStatusId())
          .orElseThrow(() -> new ResourceNotFoundException("RefJoiningStatus", "placementJoiningStatusId"));
      if ("NO_SHOW".equalsIgnoreCase(targetJoiningStatus.getStatusCode())) {
        throw new BadRequestException(
            "Target joining status 'NO_SHOW' is not eligible for employment transition",
            "INVALID_JOINING_STATUS"
        );
      }
    }

    if (employmentRecordRepository.findByPlacementRecordId(placement.id()).isPresent()) {
      throw new ConflictException("Placement record is already linked to another employment record", "DUPLICATE_PLACEMENT_EMPLOYMENT");
    }

    CreateEmploymentRecordRequest empReq = request.getEmploymentRecordRequest();
    if (empReq.getTraineeId() == null) {
      empReq.setTraineeId(placement.traineeId());
    } else if (!empReq.getTraineeId().equals(placement.traineeId())) {
      throw new BadRequestException(
          "Employment record trainee does not match placement record trainee",
          "TRAINEE_PLACEMENT_MISMATCH"
      );
    }

    empReq.setPlacementId(placement.id());

    if (empReq.getEnrollmentId() == null) {
      empReq.setEnrollmentId(placement.enrollmentId());
    }
    if (empReq.getEmployerId() == null) {
      empReq.setEmployerId(placement.employerId());
    }
    if (empReq.getEmployerBranchId() == null) {
      empReq.setEmployerBranchId(placement.employerBranchId());
    }
    if (empReq.getJobRoleId() == null) {
      empReq.setJobRoleId(placement.jobRoleId());
    }
    if (empReq.getEngagementTypeId() == null) {
      empReq.setEngagementTypeId(placement.engagementTypeId());
    }
    if (empReq.getWorkStateId() == null) {
      empReq.setWorkStateId(placement.workStateId());
    }
    if (empReq.getWorkDistrictId() == null) {
      empReq.setWorkDistrictId(placement.workDistrictId());
    }
    if (empReq.getStartingSalary() == null) {
      empReq.setStartingSalary(
          placement.joiningSalary() != null ? placement.joiningSalary() : placement.offeredSalary());
    }
    if (empReq.getSalaryFrequencyId() == null) {
      empReq.setSalaryFrequencyId(placement.salaryFrequencyId());
    }
    if (empReq.getCurrencyCode() == null) {
      empReq.setCurrencyCode(placement.currencyCode());
    }
    if (empReq.getStartDate() == null) {
      empReq.setStartDate(
          placement.actualJoiningDate() != null
              ? placement.actualJoiningDate()
              : placement.expectedJoiningDate());
    }

    if (request.getPlacementJoiningStatusId() != null || request.getPlacementStatusId() != null) {
      UpdatePlacementRecordRequest updatePlacement = new UpdatePlacementRecordRequest();
      updatePlacement.setEmployerBranchId(placement.employerBranchId());
      updatePlacement.setJobRoleId(placement.jobRoleId());
      updatePlacement.setEngagementTypeId(placement.engagementTypeId());
      updatePlacement.setPlacementSourceId(placement.placementSourceId());
      updatePlacement.setPlacementStatusId(
          request.getPlacementStatusId() != null ? request.getPlacementStatusId() : placement.placementStatusId());
      updatePlacement.setJoiningStatusId(
          request.getPlacementJoiningStatusId() != null
              ? request.getPlacementJoiningStatusId()
              : placement.joiningStatusId());
      updatePlacement.setOfferedSalary(placement.offeredSalary());
      updatePlacement.setJoiningSalary(placement.joiningSalary());
      updatePlacement.setSalaryFrequencyId(placement.salaryFrequencyId());
      updatePlacement.setCurrencyCode(placement.currencyCode());
      updatePlacement.setOfferDate(placement.offerDate());
      updatePlacement.setExpectedJoiningDate(placement.expectedJoiningDate());
      updatePlacement.setActualJoiningDate(
          placement.actualJoiningDate() != null ? placement.actualJoiningDate() : LocalDate.now());
      updatePlacement.setNonSelectionReasonId(placement.nonSelectionReasonId());
      updatePlacement.setOutcomeRemarks(placement.outcomeRemarks());
      updatePlacement.setWorkStateId(placement.workStateId());
      updatePlacement.setWorkDistrictId(placement.workDistrictId());
      updatePlacement.setRecordVerificationStatusId(placement.recordVerificationStatusId());

      placement = placementRecordService.updatePlacementRecord(placement.id(), updatePlacement);
    }

    EmploymentRecordResponse employmentRecord = employmentRecordService.createEmploymentRecord(empReq);

    SalaryHistoryResponse initialSalary = null;
    if (request.getInitialSalaryRequest() != null) {
      CreateSalaryHistoryRequest salaryReq = request.getInitialSalaryRequest();
      salaryReq.setEmploymentId(employmentRecord.id());
      salaryReq.setTraineeId(employmentRecord.traineeId());
      if (salaryReq.getEffectiveFrom() == null) {
        salaryReq.setEffectiveFrom(employmentRecord.startDate());
      }
      if (salaryReq.getSalaryAmount() == null) {
        salaryReq.setSalaryAmount(employmentRecord.startingSalary());
      }
      if (salaryReq.getSalaryFrequencyId() == null) {
        salaryReq.setSalaryFrequencyId(employmentRecord.salaryFrequencyId());
      }
      if (salaryReq.getCurrencyCode() == null) {
        salaryReq.setCurrencyCode(employmentRecord.currencyCode());
      }
      initialSalary = salaryHistoryService.createSalaryHistory(salaryReq);
    }

    return new PlacementToEmploymentTransitionResponse(placement, employmentRecord, initialSalary);
  }

  @Override
  public TraineePlacementEmploymentSummaryResponse getTraineePlacementEmploymentSummary(Long traineeId) {
    if (traineeId == null) {
      throw new BadRequestException("Trainee ID is required", "TRAINEE_ID_REQUIRED");
    }

    List<PlacementRecordResponse> placements = placementRecordService.getPlacementRecordsByTrainee(traineeId);
    List<EmploymentRecordResponse> employments = employmentRecordService.getEmploymentRecordsByTrainee(traineeId);

    return new TraineePlacementEmploymentSummaryResponse(traineeId, placements, employments);
  }
}
