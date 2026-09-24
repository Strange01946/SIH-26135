package in.gov.sih.sih26135.controller;

import in.gov.sih.sih26135.dto.request.CreateSystemEventLogRequest;
import in.gov.sih.sih26135.dto.response.SystemEventLogResponse;
import in.gov.sih.sih26135.exception.BadRequestException;
import in.gov.sih.sih26135.response.ApiResponse;
import in.gov.sih.sih26135.service.SystemEventLogService;
import jakarta.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller for managing System Event Log domain resources.
 *
 * <p>Base Route: /api/v1/system-event-logs
 * Consumes: CreateSystemEventLogRequest
 * Produces: SystemEventLogResponse enveloped in ApiResponse
 *
 * <p>System event logs are append-only operational records; updates and deletions are strictly prohibited.
 */
@RestController
@RequestMapping("/api/v1/system-event-logs")
public class SystemEventLogController {

  private final SystemEventLogService systemEventLogService;

  public SystemEventLogController(SystemEventLogService systemEventLogService) {
    this.systemEventLogService = systemEventLogService;
  }

  /**
   * Retrieves a system event log entry by primary key identifier.
   *
   * @param id primary key identifier of the system event log entry
   * @return 200 OK with SystemEventLogResponse enveloped in ApiResponse
   */
  @GetMapping("/{id}")
  public ResponseEntity<ApiResponse<SystemEventLogResponse>> getById(@PathVariable Long id) {
    SystemEventLogResponse response = systemEventLogService.getEventById(id);
    return ResponseEntity.ok(ApiResponse.ok(response));
  }

  /**
   * Retrieves all system event log entries for a specific event category.
   *
   * @param categoryId system event category reference identifier
   * @param httpRequest HTTP servlet request to verify query parameters
   * @return 200 OK with list of SystemEventLogResponse enveloped in ApiResponse
   */
  @GetMapping(params = {
      "categoryId", "!severityId", "!eventCode", "!actorUserId",
      "!entityType", "!entityId", "!sourceComponent", "!start", "!end"
  })
  public ResponseEntity<ApiResponse<List<SystemEventLogResponse>>> getByCategoryId(
      @RequestParam("categoryId") Long categoryId,
      HttpServletRequest httpRequest) {
    validateOnlyQueryParameter(httpRequest, "categoryId");
    List<SystemEventLogResponse> events = systemEventLogService.getEventsByCategoryId(categoryId);
    return ResponseEntity.ok(ApiResponse.ok(events));
  }

  /**
   * Retrieves all system event log entries for a specific severity level.
   *
   * @param severityId system event severity reference identifier
   * @param httpRequest HTTP servlet request to verify query parameters
   * @return 200 OK with list of SystemEventLogResponse enveloped in ApiResponse
   */
  @GetMapping(params = {
      "severityId", "!categoryId", "!eventCode", "!actorUserId",
      "!entityType", "!entityId", "!sourceComponent", "!start", "!end"
  })
  public ResponseEntity<ApiResponse<List<SystemEventLogResponse>>> getBySeverityId(
      @RequestParam("severityId") Long severityId,
      HttpServletRequest httpRequest) {
    validateOnlyQueryParameter(httpRequest, "severityId");
    List<SystemEventLogResponse> events = systemEventLogService.getEventsBySeverityId(severityId);
    return ResponseEntity.ok(ApiResponse.ok(events));
  }

  /**
   * Retrieves all system event log entries matching a specific event code.
   *
   * @param eventCode system event code
   * @param httpRequest HTTP servlet request to verify query parameters
   * @return 200 OK with list of SystemEventLogResponse enveloped in ApiResponse
   */
  @GetMapping(params = {
      "eventCode", "!categoryId", "!severityId", "!actorUserId",
      "!entityType", "!entityId", "!sourceComponent", "!start", "!end"
  })
  public ResponseEntity<ApiResponse<List<SystemEventLogResponse>>> getByEventCode(
      @RequestParam("eventCode") String eventCode,
      HttpServletRequest httpRequest) {
    validateOnlyQueryParameter(httpRequest, "eventCode");
    List<SystemEventLogResponse> events = systemEventLogService.getEventsByEventCode(eventCode);
    return ResponseEntity.ok(ApiResponse.ok(events));
  }

  /**
   * Retrieves all system event log entries initiated by a specific actor user.
   *
   * @param actorUserId actor user identifier
   * @param httpRequest HTTP servlet request to verify query parameters
   * @return 200 OK with list of SystemEventLogResponse enveloped in ApiResponse
   */
  @GetMapping(params = {
      "actorUserId", "!categoryId", "!severityId", "!eventCode",
      "!entityType", "!entityId", "!sourceComponent", "!start", "!end"
  })
  public ResponseEntity<ApiResponse<List<SystemEventLogResponse>>> getByActorUserId(
      @RequestParam("actorUserId") Long actorUserId,
      HttpServletRequest httpRequest) {
    validateOnlyQueryParameter(httpRequest, "actorUserId");
    List<SystemEventLogResponse> events = systemEventLogService.getEventsByActorUserId(actorUserId);
    return ResponseEntity.ok(ApiResponse.ok(events));
  }

  /**
   * Retrieves all system event log entries for a specific entity type and entity identifier.
   *
   * @param entityType target entity type name
   * @param entityId target entity primary key identifier
   * @param httpRequest HTTP servlet request to verify query parameters
   * @return 200 OK with list of SystemEventLogResponse enveloped in ApiResponse
   */
  @GetMapping(params = {
      "entityType", "entityId", "!categoryId", "!severityId",
      "!eventCode", "!actorUserId", "!sourceComponent", "!start", "!end"
  })
  public ResponseEntity<ApiResponse<List<SystemEventLogResponse>>> getByEntity(
      @RequestParam("entityType") String entityType,
      @RequestParam("entityId") Long entityId,
      HttpServletRequest httpRequest) {
    validateCompoundQueryParameters(httpRequest, Set.of("entityType", "entityId"));
    List<SystemEventLogResponse> events = systemEventLogService.getEventsByEntity(entityType, entityId);
    return ResponseEntity.ok(ApiResponse.ok(events));
  }

  /**
   * Retrieves all system event log entries for a specific entity type.
   *
   * @param entityType target entity type name
   * @param httpRequest HTTP servlet request to verify query parameters
   * @return 200 OK with list of SystemEventLogResponse enveloped in ApiResponse
   */
  @GetMapping(params = {
      "entityType", "!entityId", "!categoryId", "!severityId",
      "!eventCode", "!actorUserId", "!sourceComponent", "!start", "!end"
  })
  public ResponseEntity<ApiResponse<List<SystemEventLogResponse>>> getByEntityType(
      @RequestParam("entityType") String entityType,
      HttpServletRequest httpRequest) {
    validateOnlyQueryParameter(httpRequest, "entityType");
    List<SystemEventLogResponse> events = systemEventLogService.getEventsByEntityType(entityType);
    return ResponseEntity.ok(ApiResponse.ok(events));
  }

  /**
   * Retrieves all system event log entries originating from a specific source component.
   *
   * @param sourceComponent source component or service name
   * @param httpRequest HTTP servlet request to verify query parameters
   * @return 200 OK with list of SystemEventLogResponse enveloped in ApiResponse
   */
  @GetMapping(params = {
      "sourceComponent", "!categoryId", "!severityId", "!eventCode",
      "!actorUserId", "!entityType", "!entityId", "!start", "!end"
  })
  public ResponseEntity<ApiResponse<List<SystemEventLogResponse>>> getBySourceComponent(
      @RequestParam("sourceComponent") String sourceComponent,
      HttpServletRequest httpRequest) {
    validateOnlyQueryParameter(httpRequest, "sourceComponent");
    List<SystemEventLogResponse> events = systemEventLogService.getEventsBySourceComponent(sourceComponent);
    return ResponseEntity.ok(ApiResponse.ok(events));
  }

  /**
   * Retrieves all system event log entries occurring between two timestamps.
   *
   * @param start start timestamp (ISO-8601 date-time)
   * @param end end timestamp (ISO-8601 date-time)
   * @param httpRequest HTTP servlet request to verify query parameters
   * @return 200 OK with list of SystemEventLogResponse enveloped in ApiResponse
   */
  @GetMapping(params = {
      "start", "end", "!categoryId", "!severityId", "!eventCode",
      "!actorUserId", "!entityType", "!entityId", "!sourceComponent"
  })
  public ResponseEntity<ApiResponse<List<SystemEventLogResponse>>> getByOccurredAtBetween(
      @RequestParam("start") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime start,
      @RequestParam("end") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime end,
      HttpServletRequest httpRequest) {
    validateCompoundQueryParameters(httpRequest, Set.of("start", "end"));
    List<SystemEventLogResponse> events = systemEventLogService.getEventsByOccurredAtBetween(start, end);
    return ResponseEntity.ok(ApiResponse.ok(events));
  }

  /**
   * Records a new system event log entry.
   *
   * @param request event log creation payload
   * @param httpRequest HTTP servlet request for URI extraction
   * @return 201 Created with created SystemEventLogResponse enveloped in ApiResponse
   */
  @PostMapping
  public ResponseEntity<ApiResponse<SystemEventLogResponse>> recordSystemEvent(
      @RequestBody CreateSystemEventLogRequest request,
      HttpServletRequest httpRequest) {
    SystemEventLogResponse response = systemEventLogService.recordSystemEvent(request);
    return ResponseEntity.status(HttpStatus.CREATED)
        .body(ApiResponse.success(
            "System event log recorded successfully",
            response,
            httpRequest.getRequestURI()));
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
