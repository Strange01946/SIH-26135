package in.gov.sih.sih26135.controller;

import jakarta.validation.Valid;
import in.gov.sih.sih26135.dto.request.CreateSystemEventLogRequest;
import in.gov.sih.sih26135.dto.response.SystemEventLogResponse;
import in.gov.sih.sih26135.exception.BadRequestException;
import in.gov.sih.sih26135.response.ApiResponse;
import in.gov.sih.sih26135.response.PagedResponse;
import in.gov.sih.sih26135.response.PaginationUtils;
import in.gov.sih.sih26135.service.SystemEventLogService;
import jakarta.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;
import java.util.Set;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
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
@PreAuthorize("hasAnyAuthority('audit.read', 'system.manage')")
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
   * Retrieves paginated system event log entries across the entire system.
   *
   * @param page zero-based page index (default: 0)
   * @param size page size (default: 20, max: 100)
   * @param sort sort field (default: occurredAt)
   * @param direction sort direction (ASC or DESC, default: DESC)
   * @param httpRequest HTTP servlet request to verify query parameters
   * @return 200 OK with paginated SystemEventLogResponse enveloped in ApiResponse
   */
  @GetMapping(params = {
      "!categoryId", "!severityId", "!eventCode", "!actorUserId",
      "!entityType", "!entityId", "!sourceComponent", "!start", "!end"
  })
  public ResponseEntity<ApiResponse<PagedResponse<SystemEventLogResponse>>> getAllEvents(
      @RequestParam(value = "page", defaultValue = "0") int page,
      @RequestParam(value = "size", defaultValue = "20") int size,
      @RequestParam(value = "sort", defaultValue = "occurredAt") String sort,
      @RequestParam(value = "direction", defaultValue = "DESC") String direction,
      HttpServletRequest httpRequest) {
    validateAllowedQueryParameters(httpRequest, Set.of("page", "size", "sort", "direction"));
    Pageable pageable = PaginationUtils.buildPageable(
        page, size, sort, direction, PaginationUtils.SYSTEM_EVENT_LOG_SORT_FIELDS, "occurredAt", Sort.Direction.DESC
    );
    Page<SystemEventLogResponse> events = systemEventLogService.getAllEvents(pageable);
    return ResponseEntity.ok(ApiResponse.ok(PagedResponse.of(events)));
  }

  /**
   * Retrieves paginated system event log entries for a specific event category.
   *
   * @param categoryId system event category reference identifier
   * @param page zero-based page index (default: 0)
   * @param size page size (default: 20, max: 100)
   * @param sort sort field (default: occurredAt)
   * @param direction sort direction (ASC or DESC, default: DESC)
   * @param httpRequest HTTP servlet request to verify query parameters
   * @return 200 OK with paginated SystemEventLogResponse enveloped in ApiResponse
   */
  @GetMapping(params = {
      "categoryId", "!severityId", "!eventCode", "!actorUserId",
      "!entityType", "!entityId", "!sourceComponent", "!start", "!end"
  })
  public ResponseEntity<ApiResponse<PagedResponse<SystemEventLogResponse>>> getByCategoryId(
      @RequestParam("categoryId") Long categoryId,
      @RequestParam(value = "page", defaultValue = "0") int page,
      @RequestParam(value = "size", defaultValue = "20") int size,
      @RequestParam(value = "sort", defaultValue = "occurredAt") String sort,
      @RequestParam(value = "direction", defaultValue = "DESC") String direction,
      HttpServletRequest httpRequest) {
    validateAllowedQueryParameters(httpRequest, Set.of("categoryId", "page", "size", "sort", "direction"));
    Pageable pageable = PaginationUtils.buildPageable(
        page, size, sort, direction, PaginationUtils.SYSTEM_EVENT_LOG_SORT_FIELDS, "occurredAt", Sort.Direction.DESC
    );
    Page<SystemEventLogResponse> events = systemEventLogService.getEventsByCategoryId(categoryId, pageable);
    return ResponseEntity.ok(ApiResponse.ok(PagedResponse.of(events)));
  }

  /**
   * Retrieves paginated system event log entries for a specific severity level.
   *
   * @param severityId system event severity reference identifier
   * @param page zero-based page index (default: 0)
   * @param size page size (default: 20, max: 100)
   * @param sort sort field (default: occurredAt)
   * @param direction sort direction (ASC or DESC, default: DESC)
   * @param httpRequest HTTP servlet request to verify query parameters
   * @return 200 OK with paginated SystemEventLogResponse enveloped in ApiResponse
   */
  @GetMapping(params = {
      "severityId", "!categoryId", "!eventCode", "!actorUserId",
      "!entityType", "!entityId", "!sourceComponent", "!start", "!end"
  })
  public ResponseEntity<ApiResponse<PagedResponse<SystemEventLogResponse>>> getBySeverityId(
      @RequestParam("severityId") Long severityId,
      @RequestParam(value = "page", defaultValue = "0") int page,
      @RequestParam(value = "size", defaultValue = "20") int size,
      @RequestParam(value = "sort", defaultValue = "occurredAt") String sort,
      @RequestParam(value = "direction", defaultValue = "DESC") String direction,
      HttpServletRequest httpRequest) {
    validateAllowedQueryParameters(httpRequest, Set.of("severityId", "page", "size", "sort", "direction"));
    Pageable pageable = PaginationUtils.buildPageable(
        page, size, sort, direction, PaginationUtils.SYSTEM_EVENT_LOG_SORT_FIELDS, "occurredAt", Sort.Direction.DESC
    );
    Page<SystemEventLogResponse> events = systemEventLogService.getEventsBySeverityId(severityId, pageable);
    return ResponseEntity.ok(ApiResponse.ok(PagedResponse.of(events)));
  }

  /**
   * Retrieves paginated system event log entries matching a specific event code.
   *
   * @param eventCode system event code
   * @param page zero-based page index (default: 0)
   * @param size page size (default: 20, max: 100)
   * @param sort sort field (default: occurredAt)
   * @param direction sort direction (ASC or DESC, default: DESC)
   * @param httpRequest HTTP servlet request to verify query parameters
   * @return 200 OK with paginated SystemEventLogResponse enveloped in ApiResponse
   */
  @GetMapping(params = {
      "eventCode", "!categoryId", "!severityId", "!actorUserId",
      "!entityType", "!entityId", "!sourceComponent", "!start", "!end"
  })
  public ResponseEntity<ApiResponse<PagedResponse<SystemEventLogResponse>>> getByEventCode(
      @RequestParam("eventCode") String eventCode,
      @RequestParam(value = "page", defaultValue = "0") int page,
      @RequestParam(value = "size", defaultValue = "20") int size,
      @RequestParam(value = "sort", defaultValue = "occurredAt") String sort,
      @RequestParam(value = "direction", defaultValue = "DESC") String direction,
      HttpServletRequest httpRequest) {
    validateAllowedQueryParameters(httpRequest, Set.of("eventCode", "page", "size", "sort", "direction"));
    Pageable pageable = PaginationUtils.buildPageable(
        page, size, sort, direction, PaginationUtils.SYSTEM_EVENT_LOG_SORT_FIELDS, "occurredAt", Sort.Direction.DESC
    );
    Page<SystemEventLogResponse> events = systemEventLogService.getEventsByEventCode(eventCode, pageable);
    return ResponseEntity.ok(ApiResponse.ok(PagedResponse.of(events)));
  }

  /**
   * Retrieves paginated system event log entries initiated by a specific actor user.
   *
   * @param actorUserId actor user identifier
   * @param page zero-based page index (default: 0)
   * @param size page size (default: 20, max: 100)
   * @param sort sort field (default: occurredAt)
   * @param direction sort direction (ASC or DESC, default: DESC)
   * @param httpRequest HTTP servlet request to verify query parameters
   * @return 200 OK with paginated SystemEventLogResponse enveloped in ApiResponse
   */
  @GetMapping(params = {
      "actorUserId", "!categoryId", "!severityId", "!eventCode",
      "!entityType", "!entityId", "!sourceComponent", "!start", "!end"
  })
  public ResponseEntity<ApiResponse<PagedResponse<SystemEventLogResponse>>> getByActorUserId(
      @RequestParam("actorUserId") Long actorUserId,
      @RequestParam(value = "page", defaultValue = "0") int page,
      @RequestParam(value = "size", defaultValue = "20") int size,
      @RequestParam(value = "sort", defaultValue = "occurredAt") String sort,
      @RequestParam(value = "direction", defaultValue = "DESC") String direction,
      HttpServletRequest httpRequest) {
    validateAllowedQueryParameters(httpRequest, Set.of("actorUserId", "page", "size", "sort", "direction"));
    Pageable pageable = PaginationUtils.buildPageable(
        page, size, sort, direction, PaginationUtils.SYSTEM_EVENT_LOG_SORT_FIELDS, "occurredAt", Sort.Direction.DESC
    );
    Page<SystemEventLogResponse> events = systemEventLogService.getEventsByActorUserId(actorUserId, pageable);
    return ResponseEntity.ok(ApiResponse.ok(PagedResponse.of(events)));
  }

  /**
   * Retrieves paginated system event log entries for a specific entity type and entity identifier.
   *
   * @param entityType target entity type name
   * @param entityId target entity primary key identifier
   * @param page zero-based page index (default: 0)
   * @param size page size (default: 20, max: 100)
   * @param sort sort field (default: occurredAt)
   * @param direction sort direction (ASC or DESC, default: DESC)
   * @param httpRequest HTTP servlet request to verify query parameters
   * @return 200 OK with paginated SystemEventLogResponse enveloped in ApiResponse
   */
  @GetMapping(params = {
      "entityType", "entityId", "!categoryId", "!severityId",
      "!eventCode", "!actorUserId", "!sourceComponent", "!start", "!end"
  })
  public ResponseEntity<ApiResponse<PagedResponse<SystemEventLogResponse>>> getByEntity(
      @RequestParam("entityType") String entityType,
      @RequestParam("entityId") Long entityId,
      @RequestParam(value = "page", defaultValue = "0") int page,
      @RequestParam(value = "size", defaultValue = "20") int size,
      @RequestParam(value = "sort", defaultValue = "occurredAt") String sort,
      @RequestParam(value = "direction", defaultValue = "DESC") String direction,
      HttpServletRequest httpRequest) {
    validateAllowedQueryParameters(httpRequest, Set.of("entityType", "entityId", "page", "size", "sort", "direction"));
    Pageable pageable = PaginationUtils.buildPageable(
        page, size, sort, direction, PaginationUtils.SYSTEM_EVENT_LOG_SORT_FIELDS, "occurredAt", Sort.Direction.DESC
    );
    Page<SystemEventLogResponse> events = systemEventLogService.getEventsByEntity(entityType, entityId, pageable);
    return ResponseEntity.ok(ApiResponse.ok(PagedResponse.of(events)));
  }

  /**
   * Retrieves paginated system event log entries for a specific entity type.
   *
   * @param entityType target entity type name
   * @param page zero-based page index (default: 0)
   * @param size page size (default: 20, max: 100)
   * @param sort sort field (default: occurredAt)
   * @param direction sort direction (ASC or DESC, default: DESC)
   * @param httpRequest HTTP servlet request to verify query parameters
   * @return 200 OK with paginated SystemEventLogResponse enveloped in ApiResponse
   */
  @GetMapping(params = {
      "entityType", "!entityId", "!categoryId", "!severityId",
      "!eventCode", "!actorUserId", "!sourceComponent", "!start", "!end"
  })
  public ResponseEntity<ApiResponse<PagedResponse<SystemEventLogResponse>>> getByEntityType(
      @RequestParam("entityType") String entityType,
      @RequestParam(value = "page", defaultValue = "0") int page,
      @RequestParam(value = "size", defaultValue = "20") int size,
      @RequestParam(value = "sort", defaultValue = "occurredAt") String sort,
      @RequestParam(value = "direction", defaultValue = "DESC") String direction,
      HttpServletRequest httpRequest) {
    validateAllowedQueryParameters(httpRequest, Set.of("entityType", "page", "size", "sort", "direction"));
    Pageable pageable = PaginationUtils.buildPageable(
        page, size, sort, direction, PaginationUtils.SYSTEM_EVENT_LOG_SORT_FIELDS, "occurredAt", Sort.Direction.DESC
    );
    Page<SystemEventLogResponse> events = systemEventLogService.getEventsByEntityType(entityType, pageable);
    return ResponseEntity.ok(ApiResponse.ok(PagedResponse.of(events)));
  }

  /**
   * Retrieves paginated system event log entries originating from a specific source component.
   *
   * @param sourceComponent source component or service name
   * @param page zero-based page index (default: 0)
   * @param size page size (default: 20, max: 100)
   * @param sort sort field (default: occurredAt)
   * @param direction sort direction (ASC or DESC, default: DESC)
   * @param httpRequest HTTP servlet request to verify query parameters
   * @return 200 OK with paginated SystemEventLogResponse enveloped in ApiResponse
   */
  @GetMapping(params = {
      "sourceComponent", "!categoryId", "!severityId", "!eventCode",
      "!actorUserId", "!entityType", "!entityId", "!start", "!end"
  })
  public ResponseEntity<ApiResponse<PagedResponse<SystemEventLogResponse>>> getBySourceComponent(
      @RequestParam("sourceComponent") String sourceComponent,
      @RequestParam(value = "page", defaultValue = "0") int page,
      @RequestParam(value = "size", defaultValue = "20") int size,
      @RequestParam(value = "sort", defaultValue = "occurredAt") String sort,
      @RequestParam(value = "direction", defaultValue = "DESC") String direction,
      HttpServletRequest httpRequest) {
    validateAllowedQueryParameters(httpRequest, Set.of("sourceComponent", "page", "size", "sort", "direction"));
    Pageable pageable = PaginationUtils.buildPageable(
        page, size, sort, direction, PaginationUtils.SYSTEM_EVENT_LOG_SORT_FIELDS, "occurredAt", Sort.Direction.DESC
    );
    Page<SystemEventLogResponse> events = systemEventLogService.getEventsBySourceComponent(sourceComponent, pageable);
    return ResponseEntity.ok(ApiResponse.ok(PagedResponse.of(events)));
  }

  /**
   * Retrieves paginated system event log entries occurring between two timestamps.
   *
   * @param start start timestamp (ISO-8601 date-time)
   * @param end end timestamp (ISO-8601 date-time)
   * @param page zero-based page index (default: 0)
   * @param size page size (default: 20, max: 100)
   * @param sort sort field (default: occurredAt)
   * @param direction sort direction (ASC or DESC, default: DESC)
   * @param httpRequest HTTP servlet request to verify query parameters
   * @return 200 OK with paginated SystemEventLogResponse enveloped in ApiResponse
   */
  @GetMapping(params = {
      "start", "end", "!categoryId", "!severityId", "!eventCode",
      "!actorUserId", "!entityType", "!entityId", "!sourceComponent"
  })
  public ResponseEntity<ApiResponse<PagedResponse<SystemEventLogResponse>>> getByOccurredAtBetween(
      @RequestParam("start") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime start,
      @RequestParam("end") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime end,
      @RequestParam(value = "page", defaultValue = "0") int page,
      @RequestParam(value = "size", defaultValue = "20") int size,
      @RequestParam(value = "sort", defaultValue = "occurredAt") String sort,
      @RequestParam(value = "direction", defaultValue = "DESC") String direction,
      HttpServletRequest httpRequest) {
    validateAllowedQueryParameters(httpRequest, Set.of("start", "end", "page", "size", "sort", "direction"));
    Pageable pageable = PaginationUtils.buildPageable(
        page, size, sort, direction, PaginationUtils.SYSTEM_EVENT_LOG_SORT_FIELDS, "occurredAt", Sort.Direction.DESC
    );
    Page<SystemEventLogResponse> events = systemEventLogService.getEventsByOccurredAtBetween(start, end, pageable);
    return ResponseEntity.ok(ApiResponse.ok(PagedResponse.of(events)));
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
      @Valid @RequestBody CreateSystemEventLogRequest request,
      HttpServletRequest httpRequest) {
    SystemEventLogResponse response = systemEventLogService.recordSystemEvent(request);
    return ResponseEntity.status(HttpStatus.CREATED)
        .body(ApiResponse.success(
            "System event log recorded successfully",
            response,
            httpRequest.getRequestURI()));
  }

  private void validateAllowedQueryParameters(HttpServletRequest request, Set<String> allowedParams) {
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
