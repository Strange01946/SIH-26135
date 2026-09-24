package in.gov.sih.sih26135.controller;

import in.gov.sih.sih26135.dto.request.CreateTraineeUnemploymentEventRequest;
import in.gov.sih.sih26135.dto.request.UpdateTraineeUnemploymentEventRequest;
import in.gov.sih.sih26135.dto.response.TraineeUnemploymentEventResponse;
import in.gov.sih.sih26135.exception.BadRequestException;
import in.gov.sih.sih26135.exception.ResourceNotFoundException;
import in.gov.sih.sih26135.response.ApiResponse;
import in.gov.sih.sih26135.service.TraineeUnemploymentEventService;
import jakarta.servlet.http.HttpServletRequest;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller for managing Trainee Unemployment Event domain resources.
 *
 * <p>Base Route: /api/v1/trainee-unemployment-events
 * Consumes: CreateTraineeUnemploymentEventRequest, UpdateTraineeUnemploymentEventRequest
 * Produces: TraineeUnemploymentEventResponse enveloped in ApiResponse
 */
@RestController
@RequestMapping("/api/v1/trainee-unemployment-events")
public class TraineeUnemploymentEventController {

  private final TraineeUnemploymentEventService traineeUnemploymentEventService;

  public TraineeUnemploymentEventController(TraineeUnemploymentEventService traineeUnemploymentEventService) {
    this.traineeUnemploymentEventService = traineeUnemploymentEventService;
  }

  /**
   * Retrieves a trainee unemployment event by primary key identifier.
   *
   * @param id primary key identifier of the unemployment event
   * @return 200 OK with TraineeUnemploymentEventResponse enveloped in ApiResponse
   */
  @GetMapping("/{id}")
  public ResponseEntity<ApiResponse<TraineeUnemploymentEventResponse>> getById(@PathVariable Long id) {
    TraineeUnemploymentEventResponse response = traineeUnemploymentEventService.getUnemploymentEventById(id);
    return ResponseEntity.ok(ApiResponse.ok(response));
  }

  /**
   * Retrieves a single trainee unemployment event by trainee identifier and period number.
   *
   * @param traineeId trainee identifier
   * @param periodNumber sequence number of the unemployment period
   * @param httpRequest HTTP servlet request to verify query parameters
   * @return 200 OK with TraineeUnemploymentEventResponse enveloped in ApiResponse
   */
  @GetMapping(params = {
      "traineeId", "periodNumber", "!isCurrent", "!currentPeriod", "!exitEventId",
      "!startDate", "!reasonId", "!labourStatusId", "!precedingEmploymentId",
      "!succeedingEmploymentId", "!enrollmentId", "!placementId", "!followupTaskId",
      "!surveyResponseId", "!statusId"
  })
  public ResponseEntity<ApiResponse<TraineeUnemploymentEventResponse>> getByTraineeIdAndPeriodNumber(
      @RequestParam("traineeId") Long traineeId,
      @RequestParam("periodNumber") Integer periodNumber,
      HttpServletRequest httpRequest) {
    validateCompoundQueryParameters(httpRequest, Set.of("traineeId", "periodNumber"));
    TraineeUnemploymentEventResponse response =
        traineeUnemploymentEventService.getUnemploymentEventByTraineeIdAndPeriodNumber(traineeId, periodNumber);
    return ResponseEntity.ok(ApiResponse.ok(response));
  }

  /**
   * Retrieves current active unemployment events for a specific trainee.
   *
   * @param traineeId trainee identifier
   * @param isCurrent boolean indicating current status filter (must be true)
   * @param httpRequest HTTP servlet request to verify query parameters
   * @return 200 OK with list of TraineeUnemploymentEventResponse enveloped in ApiResponse
   */
  @GetMapping(params = {
      "traineeId", "isCurrent", "!periodNumber", "!currentPeriod", "!exitEventId",
      "!startDate", "!reasonId", "!labourStatusId", "!precedingEmploymentId",
      "!succeedingEmploymentId", "!enrollmentId", "!placementId", "!followupTaskId",
      "!surveyResponseId", "!statusId"
  })
  public ResponseEntity<ApiResponse<List<TraineeUnemploymentEventResponse>>> getCurrentByTraineeId(
      @RequestParam("traineeId") Long traineeId,
      @RequestParam("isCurrent") boolean isCurrent,
      HttpServletRequest httpRequest) {
    validateCompoundQueryParameters(httpRequest, Set.of("traineeId", "isCurrent"));
    if (!isCurrent) {
      throw new BadRequestException(
          "Only isCurrent=true is supported for current unemployment events filter",
          "INVALID_FILTER_PARAMETER");
    }
    List<TraineeUnemploymentEventResponse> events =
        traineeUnemploymentEventService.getCurrentUnemploymentEventsByTraineeId(traineeId);
    return ResponseEntity.ok(ApiResponse.ok(events));
  }

  /**
   * Retrieves the current single unemployment period for a specific trainee.
   *
   * @param traineeId trainee identifier
   * @param currentPeriod boolean indicating current period lookup (must be true)
   * @param httpRequest HTTP servlet request to verify query parameters
   * @return 200 OK with TraineeUnemploymentEventResponse enveloped in ApiResponse
   */
  @GetMapping(params = {
      "traineeId", "currentPeriod", "!periodNumber", "!isCurrent", "!exitEventId",
      "!startDate", "!reasonId", "!labourStatusId", "!precedingEmploymentId",
      "!succeedingEmploymentId", "!enrollmentId", "!placementId", "!followupTaskId",
      "!surveyResponseId", "!statusId"
  })
  public ResponseEntity<ApiResponse<TraineeUnemploymentEventResponse>> getCurrentPeriodByTraineeId(
      @RequestParam("traineeId") Long traineeId,
      @RequestParam("currentPeriod") boolean currentPeriod,
      HttpServletRequest httpRequest) {
    validateCompoundQueryParameters(httpRequest, Set.of("traineeId", "currentPeriod"));
    if (!currentPeriod) {
      throw new BadRequestException(
          "Only currentPeriod=true is supported for current period filter",
          "INVALID_FILTER_PARAMETER");
    }
    TraineeUnemploymentEventResponse response =
        traineeUnemploymentEventService.getCurrentPeriodByTraineeId(traineeId)
            .orElseThrow(() -> new ResourceNotFoundException("TraineeUnemploymentEvent", "traineeId"));
    return ResponseEntity.ok(ApiResponse.ok(response));
  }

  /**
   * Retrieves all unemployment events for a specific trainee.
   *
   * @param traineeId trainee identifier
   * @param httpRequest HTTP servlet request to verify query parameters
   * @return 200 OK with list of TraineeUnemploymentEventResponse enveloped in ApiResponse
   */
  @GetMapping(params = {
      "traineeId", "!periodNumber", "!isCurrent", "!currentPeriod", "!exitEventId",
      "!startDate", "!reasonId", "!labourStatusId", "!precedingEmploymentId",
      "!succeedingEmploymentId", "!enrollmentId", "!placementId", "!followupTaskId",
      "!surveyResponseId", "!statusId"
  })
  public ResponseEntity<ApiResponse<List<TraineeUnemploymentEventResponse>>> getByTraineeId(
      @RequestParam("traineeId") Long traineeId,
      HttpServletRequest httpRequest) {
    validateOnlyQueryParameter(httpRequest, "traineeId");
    List<TraineeUnemploymentEventResponse> events =
        traineeUnemploymentEventService.getUnemploymentEventsByTraineeId(traineeId);
    return ResponseEntity.ok(ApiResponse.ok(events));
  }

  /**
   * Retrieves the unemployment event linked to a specific employment exit event.
   *
   * @param exitEventId employment exit event identifier
   * @param httpRequest HTTP servlet request to verify query parameters
   * @return 200 OK with TraineeUnemploymentEventResponse enveloped in ApiResponse
   */
  @GetMapping(params = {
      "exitEventId", "!traineeId", "!periodNumber", "!isCurrent", "!currentPeriod",
      "!startDate", "!reasonId", "!labourStatusId", "!precedingEmploymentId",
      "!succeedingEmploymentId", "!enrollmentId", "!placementId", "!followupTaskId",
      "!surveyResponseId", "!statusId"
  })
  public ResponseEntity<ApiResponse<TraineeUnemploymentEventResponse>> getByExitEventId(
      @RequestParam("exitEventId") Long exitEventId,
      HttpServletRequest httpRequest) {
    validateOnlyQueryParameter(httpRequest, "exitEventId");
    TraineeUnemploymentEventResponse response =
        traineeUnemploymentEventService.getUnemploymentEventByExitEventId(exitEventId)
            .orElseThrow(() -> new ResourceNotFoundException("TraineeUnemploymentEvent", "exitEventId"));
    return ResponseEntity.ok(ApiResponse.ok(response));
  }

  /**
   * Retrieves all unemployment events starting on a specific date.
   *
   * @param startDate period start date
   * @param httpRequest HTTP servlet request to verify query parameters
   * @return 200 OK with list of TraineeUnemploymentEventResponse enveloped in ApiResponse
   */
  @GetMapping(params = {
      "startDate", "!traineeId", "!periodNumber", "!isCurrent", "!currentPeriod",
      "!exitEventId", "!reasonId", "!labourStatusId", "!precedingEmploymentId",
      "!succeedingEmploymentId", "!enrollmentId", "!placementId", "!followupTaskId",
      "!surveyResponseId", "!statusId"
  })
  public ResponseEntity<ApiResponse<List<TraineeUnemploymentEventResponse>>> getByStartDate(
      @RequestParam("startDate") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
      HttpServletRequest httpRequest) {
    validateOnlyQueryParameter(httpRequest, "startDate");
    List<TraineeUnemploymentEventResponse> events =
        traineeUnemploymentEventService.getUnemploymentEventsByStartDate(startDate);
    return ResponseEntity.ok(ApiResponse.ok(events));
  }

  /**
   * Retrieves all unemployment events for a specific unemployment reason.
   *
   * @param reasonId unemployment reason reference identifier
   * @param httpRequest HTTP servlet request to verify query parameters
   * @return 200 OK with list of TraineeUnemploymentEventResponse enveloped in ApiResponse
   */
  @GetMapping(params = {
      "reasonId", "!traineeId", "!periodNumber", "!isCurrent", "!currentPeriod",
      "!exitEventId", "!startDate", "!labourStatusId", "!precedingEmploymentId",
      "!succeedingEmploymentId", "!enrollmentId", "!placementId", "!followupTaskId",
      "!surveyResponseId", "!statusId"
  })
  public ResponseEntity<ApiResponse<List<TraineeUnemploymentEventResponse>>> getByReasonId(
      @RequestParam("reasonId") Long reasonId,
      HttpServletRequest httpRequest) {
    validateOnlyQueryParameter(httpRequest, "reasonId");
    List<TraineeUnemploymentEventResponse> events =
        traineeUnemploymentEventService.getUnemploymentEventsByReasonId(reasonId);
    return ResponseEntity.ok(ApiResponse.ok(events));
  }

  /**
   * Retrieves all unemployment events for a specific labour status.
   *
   * @param labourStatusId labour status identifier
   * @param httpRequest HTTP servlet request to verify query parameters
   * @return 200 OK with list of TraineeUnemploymentEventResponse enveloped in ApiResponse
   */
  @GetMapping(params = {
      "labourStatusId", "!traineeId", "!periodNumber", "!isCurrent", "!currentPeriod",
      "!exitEventId", "!startDate", "!reasonId", "!precedingEmploymentId",
      "!succeedingEmploymentId", "!enrollmentId", "!placementId", "!followupTaskId",
      "!surveyResponseId", "!statusId"
  })
  public ResponseEntity<ApiResponse<List<TraineeUnemploymentEventResponse>>> getByLabourStatusId(
      @RequestParam("labourStatusId") Long labourStatusId,
      HttpServletRequest httpRequest) {
    validateOnlyQueryParameter(httpRequest, "labourStatusId");
    List<TraineeUnemploymentEventResponse> events =
        traineeUnemploymentEventService.getUnemploymentEventsByLabourStatusId(labourStatusId);
    return ResponseEntity.ok(ApiResponse.ok(events));
  }

  /**
   * Retrieves all unemployment events linked to a specific preceding employment record.
   *
   * @param precedingEmploymentId preceding employment record identifier
   * @param httpRequest HTTP servlet request to verify query parameters
   * @return 200 OK with list of TraineeUnemploymentEventResponse enveloped in ApiResponse
   */
  @GetMapping(params = {
      "precedingEmploymentId", "!traineeId", "!periodNumber", "!isCurrent", "!currentPeriod",
      "!exitEventId", "!startDate", "!reasonId", "!labourStatusId",
      "!succeedingEmploymentId", "!enrollmentId", "!placementId", "!followupTaskId",
      "!surveyResponseId", "!statusId"
  })
  public ResponseEntity<ApiResponse<List<TraineeUnemploymentEventResponse>>> getByPrecedingEmploymentId(
      @RequestParam("precedingEmploymentId") Long precedingEmploymentId,
      HttpServletRequest httpRequest) {
    validateOnlyQueryParameter(httpRequest, "precedingEmploymentId");
    List<TraineeUnemploymentEventResponse> events =
        traineeUnemploymentEventService.getUnemploymentEventsByPrecedingEmploymentId(precedingEmploymentId);
    return ResponseEntity.ok(ApiResponse.ok(events));
  }

  /**
   * Retrieves all unemployment events linked to a specific succeeding employment record.
   *
   * @param succeedingEmploymentId succeeding employment record identifier
   * @param httpRequest HTTP servlet request to verify query parameters
   * @return 200 OK with list of TraineeUnemploymentEventResponse enveloped in ApiResponse
   */
  @GetMapping(params = {
      "succeedingEmploymentId", "!traineeId", "!periodNumber", "!isCurrent", "!currentPeriod",
      "!exitEventId", "!startDate", "!reasonId", "!labourStatusId",
      "!precedingEmploymentId", "!enrollmentId", "!placementId", "!followupTaskId",
      "!surveyResponseId", "!statusId"
  })
  public ResponseEntity<ApiResponse<List<TraineeUnemploymentEventResponse>>> getBySucceedingEmploymentId(
      @RequestParam("succeedingEmploymentId") Long succeedingEmploymentId,
      HttpServletRequest httpRequest) {
    validateOnlyQueryParameter(httpRequest, "succeedingEmploymentId");
    List<TraineeUnemploymentEventResponse> events =
        traineeUnemploymentEventService.getUnemploymentEventsBySucceedingEmploymentId(succeedingEmploymentId);
    return ResponseEntity.ok(ApiResponse.ok(events));
  }

  /**
   * Retrieves all unemployment events linked to a specific training enrollment.
   *
   * @param enrollmentId training enrollment identifier
   * @param httpRequest HTTP servlet request to verify query parameters
   * @return 200 OK with list of TraineeUnemploymentEventResponse enveloped in ApiResponse
   */
  @GetMapping(params = {
      "enrollmentId", "!traineeId", "!periodNumber", "!isCurrent", "!currentPeriod",
      "!exitEventId", "!startDate", "!reasonId", "!labourStatusId",
      "!precedingEmploymentId", "!succeedingEmploymentId", "!placementId", "!followupTaskId",
      "!surveyResponseId", "!statusId"
  })
  public ResponseEntity<ApiResponse<List<TraineeUnemploymentEventResponse>>> getByEnrollmentId(
      @RequestParam("enrollmentId") Long enrollmentId,
      HttpServletRequest httpRequest) {
    validateOnlyQueryParameter(httpRequest, "enrollmentId");
    List<TraineeUnemploymentEventResponse> events =
        traineeUnemploymentEventService.getUnemploymentEventsByEnrollmentId(enrollmentId);
    return ResponseEntity.ok(ApiResponse.ok(events));
  }

  /**
   * Retrieves all unemployment events linked to a specific placement record.
   *
   * @param placementId placement record identifier
   * @param httpRequest HTTP servlet request to verify query parameters
   * @return 200 OK with list of TraineeUnemploymentEventResponse enveloped in ApiResponse
   */
  @GetMapping(params = {
      "placementId", "!traineeId", "!periodNumber", "!isCurrent", "!currentPeriod",
      "!exitEventId", "!startDate", "!reasonId", "!labourStatusId",
      "!precedingEmploymentId", "!succeedingEmploymentId", "!enrollmentId", "!followupTaskId",
      "!surveyResponseId", "!statusId"
  })
  public ResponseEntity<ApiResponse<List<TraineeUnemploymentEventResponse>>> getByPlacementId(
      @RequestParam("placementId") Long placementId,
      HttpServletRequest httpRequest) {
    validateOnlyQueryParameter(httpRequest, "placementId");
    List<TraineeUnemploymentEventResponse> events =
        traineeUnemploymentEventService.getUnemploymentEventsByPlacementId(placementId);
    return ResponseEntity.ok(ApiResponse.ok(events));
  }

  /**
   * Retrieves all unemployment events linked to a specific follow-up task.
   *
   * @param followupTaskId follow-up task identifier
   * @param httpRequest HTTP servlet request to verify query parameters
   * @return 200 OK with list of TraineeUnemploymentEventResponse enveloped in ApiResponse
   */
  @GetMapping(params = {
      "followupTaskId", "!traineeId", "!periodNumber", "!isCurrent", "!currentPeriod",
      "!exitEventId", "!startDate", "!reasonId", "!labourStatusId",
      "!precedingEmploymentId", "!succeedingEmploymentId", "!enrollmentId", "!placementId",
      "!surveyResponseId", "!statusId"
  })
  public ResponseEntity<ApiResponse<List<TraineeUnemploymentEventResponse>>> getByFollowupTaskId(
      @RequestParam("followupTaskId") Long followupTaskId,
      HttpServletRequest httpRequest) {
    validateOnlyQueryParameter(httpRequest, "followupTaskId");
    List<TraineeUnemploymentEventResponse> events =
        traineeUnemploymentEventService.getUnemploymentEventsByFollowupTaskId(followupTaskId);
    return ResponseEntity.ok(ApiResponse.ok(events));
  }

  /**
   * Retrieves all unemployment events linked to a specific survey response.
   *
   * @param surveyResponseId survey response identifier
   * @param httpRequest HTTP servlet request to verify query parameters
   * @return 200 OK with list of TraineeUnemploymentEventResponse enveloped in ApiResponse
   */
  @GetMapping(params = {
      "surveyResponseId", "!traineeId", "!periodNumber", "!isCurrent", "!currentPeriod",
      "!exitEventId", "!startDate", "!reasonId", "!labourStatusId",
      "!precedingEmploymentId", "!succeedingEmploymentId", "!enrollmentId", "!placementId",
      "!followupTaskId", "!statusId"
  })
  public ResponseEntity<ApiResponse<List<TraineeUnemploymentEventResponse>>> getBySurveyResponseId(
      @RequestParam("surveyResponseId") Long surveyResponseId,
      HttpServletRequest httpRequest) {
    validateOnlyQueryParameter(httpRequest, "surveyResponseId");
    List<TraineeUnemploymentEventResponse> events =
        traineeUnemploymentEventService.getUnemploymentEventsBySurveyResponseId(surveyResponseId);
    return ResponseEntity.ok(ApiResponse.ok(events));
  }

  /**
   * Retrieves all unemployment events with a specific record verification status.
   *
   * @param statusId record verification status reference identifier
   * @param httpRequest HTTP servlet request to verify query parameters
   * @return 200 OK with list of TraineeUnemploymentEventResponse enveloped in ApiResponse
   */
  @GetMapping(params = {
      "statusId", "!traineeId", "!periodNumber", "!isCurrent", "!currentPeriod",
      "!exitEventId", "!startDate", "!reasonId", "!labourStatusId",
      "!precedingEmploymentId", "!succeedingEmploymentId", "!enrollmentId", "!placementId",
      "!followupTaskId", "!surveyResponseId"
  })
  public ResponseEntity<ApiResponse<List<TraineeUnemploymentEventResponse>>> getByVerificationStatusId(
      @RequestParam("statusId") Long statusId,
      HttpServletRequest httpRequest) {
    validateOnlyQueryParameter(httpRequest, "statusId");
    List<TraineeUnemploymentEventResponse> events =
        traineeUnemploymentEventService.getUnemploymentEventsByVerificationStatusId(statusId);
    return ResponseEntity.ok(ApiResponse.ok(events));
  }

  /**
   * Creates a new trainee unemployment event.
   *
   * @param request creation payload
   * @param httpRequest HTTP servlet request for URI extraction
   * @return 201 Created with created TraineeUnemploymentEventResponse enveloped in ApiResponse
   */
  @PostMapping
  public ResponseEntity<ApiResponse<TraineeUnemploymentEventResponse>> createUnemploymentEvent(
      @RequestBody CreateTraineeUnemploymentEventRequest request,
      HttpServletRequest httpRequest) {
    TraineeUnemploymentEventResponse response =
        traineeUnemploymentEventService.createUnemploymentEvent(request);
    return ResponseEntity.status(HttpStatus.CREATED)
        .body(ApiResponse.success(
            "Trainee unemployment event created successfully",
            response,
            httpRequest.getRequestURI()));
  }

  /**
   * Updates an existing trainee unemployment event.
   *
   * @param id primary key identifier of the unemployment event to update
   * @param request update payload
   * @return 200 OK with updated TraineeUnemploymentEventResponse enveloped in ApiResponse
   */
  @PutMapping("/{id}")
  public ResponseEntity<ApiResponse<TraineeUnemploymentEventResponse>> updateUnemploymentEvent(
      @PathVariable Long id,
      @RequestBody UpdateTraineeUnemploymentEventRequest request) {
    TraineeUnemploymentEventResponse response =
        traineeUnemploymentEventService.updateUnemploymentEvent(id, request);
    return ResponseEntity.ok(ApiResponse.success(
        "Trainee unemployment event updated successfully",
        response));
  }

  /**
   * Deletes a trainee unemployment event.
   *
   * @param id primary key identifier of the unemployment event to delete
   * @return 200 OK with success confirmation message
   */
  @DeleteMapping("/{id}")
  public ResponseEntity<ApiResponse<Void>> deleteUnemploymentEvent(@PathVariable Long id) {
    traineeUnemploymentEventService.deleteUnemploymentEvent(id);
    return ResponseEntity.ok(ApiResponse.success("Trainee unemployment event deleted successfully"));
  }

  private void validateOnlyQueryParameter(HttpServletRequest request, String allowedParam) {
    for (String paramName : request.getParameterMap().keySet()) {
      if (!paramName.equals(allowedParam)) {
        throw new BadRequestException(
            "Unsupported query parameter: " + paramName,
            "UNSUPPORTED_PARAMETER"
        );
      }
    }
  }

  private void validateCompoundQueryParameters(HttpServletRequest request, Set<String> allowedParams) {
    for (String paramName : request.getParameterMap().keySet()) {
      if (!allowedParams.contains(paramName)) {
        throw new BadRequestException(
            "Unsupported query parameter: " + paramName,
            "UNSUPPORTED_PARAMETER"
        );
      }
    }
  }
}
