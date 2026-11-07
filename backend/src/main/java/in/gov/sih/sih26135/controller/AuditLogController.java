package in.gov.sih.sih26135.controller;

import in.gov.sih.sih26135.dto.request.AuditLogFilterCriteria;
import in.gov.sih.sih26135.dto.request.CreateAuditLogRequest;
import in.gov.sih.sih26135.dto.response.AuditLogResponse;
import in.gov.sih.sih26135.dto.response.AuditTrailTimelineResponse;
import in.gov.sih.sih26135.exception.BadRequestException;
import in.gov.sih.sih26135.response.ApiResponse;
import in.gov.sih.sih26135.response.PagedResponse;
import in.gov.sih.sih26135.response.PaginationUtils;
import in.gov.sih.sih26135.service.AuditLogService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.Set;

/**
 * REST controller for managing Audit Log domain resources and reconstructing entity timelines.
 *
 * <p>Base Route: /api/v1/audit-logs
 *
 * <p>AuditLog is an append-only audit trail; updates and deletions are strictly prohibited.
 */
@Validated
@RestController
@RequestMapping("/api/v1/audit-logs")
@PreAuthorize("hasAnyAuthority('audit.read', 'system.manage')")
public class AuditLogController {

  private static final Set<String> ALLOWED_SEARCH_PARAMETERS = Set.of(
      "actorUserId", "auditActionId", "auditActionCode", "entityType", "entityId",
      "start", "end", "correlationId", "page", "size", "sort", "direction"
  );

  private final AuditLogService auditLogService;

  public AuditLogController(AuditLogService auditLogService) {
    this.auditLogService = auditLogService;
  }

  /**
   * Retrieves an audit log entry by primary key identifier.
   *
   * @param id primary key identifier of the audit log entry
   * @return 200 OK with AuditLogResponse enveloped in ApiResponse
   */
  @GetMapping("/{id}")
  public ResponseEntity<ApiResponse<AuditLogResponse>> getById(@PathVariable @Positive Long id) {
    AuditLogResponse response = auditLogService.getAuditLogById(id);
    return ResponseEntity.ok(ApiResponse.ok(response));
  }

  /**
   * Searches and retrieves paginated audit log entries using multi-criteria filters.
   *
   * @param actorUserId     optional actor user ID filter
   * @param auditActionId   optional audit action ID filter
   * @param auditActionCode optional audit action code filter (e.g. STATUS_CHANGE, CREATE)
   * @param entityType      optional entity type name
   * @param entityId        optional entity ID
   * @param start           optional start timestamp (ISO-8601 date-time)
   * @param end             optional end timestamp (ISO-8601 date-time)
   * @param correlationId   optional correlation ID
   * @param page            zero-based page index (default: 0)
   * @param size            page size (default: 20, max: 100)
   * @param sort            sort field (default: occurredAt)
   * @param direction       sort direction (ASC or DESC, default: DESC)
   * @param httpRequest     HTTP servlet request for parameter validation
   * @return 200 OK with paginated AuditLogResponse enveloped in ApiResponse
   */
  @GetMapping
  public ResponseEntity<ApiResponse<PagedResponse<AuditLogResponse>>> getAllAuditLogs(
      @RequestParam(value = "actorUserId", required = false) @Positive Long actorUserId,
      @RequestParam(value = "auditActionId", required = false) @Positive Long auditActionId,
      @RequestParam(value = "auditActionCode", required = false) String auditActionCode,
      @RequestParam(value = "entityType", required = false) String entityType,
      @RequestParam(value = "entityId", required = false) @Positive Long entityId,
      @RequestParam(value = "start", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime start,
      @RequestParam(value = "end", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime end,
      @RequestParam(value = "correlationId", required = false) String correlationId,
      @RequestParam(value = "page", defaultValue = "0") @Min(0) int page,
      @RequestParam(value = "size", defaultValue = "20") @Min(1) @Max(100) int size,
      @RequestParam(value = "sort", defaultValue = "occurredAt") String sort,
      @RequestParam(value = "direction", defaultValue = "DESC") String direction,
      HttpServletRequest httpRequest) {

    validateAllowedQueryParameters(httpRequest, ALLOWED_SEARCH_PARAMETERS);

    AuditLogFilterCriteria criteria = new AuditLogFilterCriteria(
        actorUserId,
        auditActionCode,
        entityType,
        entityId,
        start,
        end,
        correlationId,
        page,
        size,
        sort,
        direction
    );
    criteria.setAuditActionId(auditActionId);

    Page<AuditLogResponse> logs = auditLogService.searchAuditLogs(criteria);
    return ResponseEntity.ok(ApiResponse.ok(PagedResponse.of(logs)));
  }

  /**
   * Retrieves paginated audit log entries for an actor user.
   */
  @GetMapping(params = {
      "actorUserId", "!auditActionId", "!auditActionCode", "!entityType", "!entityId",
      "!start", "!end", "!correlationId"
  })
  public ResponseEntity<ApiResponse<PagedResponse<AuditLogResponse>>> getByActorUserId(
      @RequestParam("actorUserId") @Positive Long actorUserId,
      @RequestParam(value = "page", defaultValue = "0") @Min(0) int page,
      @RequestParam(value = "size", defaultValue = "20") @Min(1) @Max(100) int size,
      @RequestParam(value = "sort", defaultValue = "occurredAt") String sort,
      @RequestParam(value = "direction", defaultValue = "DESC") String direction,
      HttpServletRequest httpRequest) {
    validateAllowedQueryParameters(httpRequest, Set.of("actorUserId", "page", "size", "sort", "direction"));
    Pageable pageable = PaginationUtils.buildPageable(
        page, size, sort, direction, PaginationUtils.AUDIT_LOG_SORT_FIELDS, "occurredAt", Sort.Direction.DESC
    );
    Page<AuditLogResponse> logs = auditLogService.getAuditLogsByActorUserId(actorUserId, pageable);
    return ResponseEntity.ok(ApiResponse.ok(PagedResponse.of(logs)));
  }

  /**
   * Retrieves paginated audit log entries for an audit action.
   */
  @GetMapping(params = {
      "auditActionId", "!actorUserId", "!auditActionCode", "!entityType", "!entityId",
      "!start", "!end", "!correlationId"
  })
  public ResponseEntity<ApiResponse<PagedResponse<AuditLogResponse>>> getByActionId(
      @RequestParam("auditActionId") @Positive Long auditActionId,
      @RequestParam(value = "page", defaultValue = "0") @Min(0) int page,
      @RequestParam(value = "size", defaultValue = "20") @Min(1) @Max(100) int size,
      @RequestParam(value = "sort", defaultValue = "occurredAt") String sort,
      @RequestParam(value = "direction", defaultValue = "DESC") String direction,
      HttpServletRequest httpRequest) {
    validateAllowedQueryParameters(httpRequest, Set.of("auditActionId", "page", "size", "sort", "direction"));
    Pageable pageable = PaginationUtils.buildPageable(
        page, size, sort, direction, PaginationUtils.AUDIT_LOG_SORT_FIELDS, "occurredAt", Sort.Direction.DESC
    );
    Page<AuditLogResponse> logs = auditLogService.getAuditLogsByActionId(auditActionId, pageable);
    return ResponseEntity.ok(ApiResponse.ok(PagedResponse.of(logs)));
  }

  /**
   * Retrieves paginated audit log entries for a specific entity.
   */
  @GetMapping(params = {
      "entityType", "entityId", "!actorUserId", "!auditActionId", "!auditActionCode",
      "!start", "!end", "!correlationId"
  })
  public ResponseEntity<ApiResponse<PagedResponse<AuditLogResponse>>> getByEntity(
      @RequestParam("entityType") String entityType,
      @RequestParam("entityId") @Positive Long entityId,
      @RequestParam(value = "page", defaultValue = "0") @Min(0) int page,
      @RequestParam(value = "size", defaultValue = "20") @Min(1) @Max(100) int size,
      @RequestParam(value = "sort", defaultValue = "occurredAt") String sort,
      @RequestParam(value = "direction", defaultValue = "DESC") String direction,
      HttpServletRequest httpRequest) {
    validateAllowedQueryParameters(httpRequest, Set.of("entityType", "entityId", "page", "size", "sort", "direction"));
    Pageable pageable = PaginationUtils.buildPageable(
        page, size, sort, direction, PaginationUtils.AUDIT_LOG_SORT_FIELDS, "occurredAt", Sort.Direction.DESC
    );
    Page<AuditLogResponse> logs = auditLogService.getAuditLogsByEntity(entityType, entityId, pageable);
    return ResponseEntity.ok(ApiResponse.ok(PagedResponse.of(logs)));
  }

  /**
   * Retrieves paginated audit log entries for an entity type.
   */
  @GetMapping(params = {
      "entityType", "!entityId", "!actorUserId", "!auditActionId", "!auditActionCode",
      "!start", "!end", "!correlationId"
  })
  public ResponseEntity<ApiResponse<PagedResponse<AuditLogResponse>>> getByEntityType(
      @RequestParam("entityType") String entityType,
      @RequestParam(value = "page", defaultValue = "0") @Min(0) int page,
      @RequestParam(value = "size", defaultValue = "20") @Min(1) @Max(100) int size,
      @RequestParam(value = "sort", defaultValue = "occurredAt") String sort,
      @RequestParam(value = "direction", defaultValue = "DESC") String direction,
      HttpServletRequest httpRequest) {
    validateAllowedQueryParameters(httpRequest, Set.of("entityType", "page", "size", "sort", "direction"));
    Pageable pageable = PaginationUtils.buildPageable(
        page, size, sort, direction, PaginationUtils.AUDIT_LOG_SORT_FIELDS, "occurredAt", Sort.Direction.DESC
    );
    Page<AuditLogResponse> logs = auditLogService.getAuditLogsByEntityType(entityType, pageable);
    return ResponseEntity.ok(ApiResponse.ok(PagedResponse.of(logs)));
  }

  /**
   * Retrieves paginated audit log entries with a specific correlation identifier.
   */
  @GetMapping(params = {
      "correlationId", "!actorUserId", "!auditActionId", "!auditActionCode", "!entityType",
      "!entityId", "!start", "!end"
  })
  public ResponseEntity<ApiResponse<PagedResponse<AuditLogResponse>>> getByCorrelationId(
      @RequestParam("correlationId") String correlationId,
      @RequestParam(value = "page", defaultValue = "0") @Min(0) int page,
      @RequestParam(value = "size", defaultValue = "20") @Min(1) @Max(100) int size,
      @RequestParam(value = "sort", defaultValue = "occurredAt") String sort,
      @RequestParam(value = "direction", defaultValue = "DESC") String direction,
      HttpServletRequest httpRequest) {
    validateAllowedQueryParameters(httpRequest, Set.of("correlationId", "page", "size", "sort", "direction"));
    Pageable pageable = PaginationUtils.buildPageable(
        page, size, sort, direction, PaginationUtils.AUDIT_LOG_SORT_FIELDS, "occurredAt", Sort.Direction.DESC
    );
    Page<AuditLogResponse> logs = auditLogService.getAuditLogsByCorrelationId(correlationId, pageable);
    return ResponseEntity.ok(ApiResponse.ok(PagedResponse.of(logs)));
  }

  /**
   * Retrieves paginated audit log entries occurring between two timestamps.
   */
  @GetMapping(params = {
      "start", "end", "!actorUserId", "!auditActionId", "!auditActionCode",
      "!entityType", "!entityId", "!correlationId"
  })
  public ResponseEntity<ApiResponse<PagedResponse<AuditLogResponse>>> getByOccurredAtBetween(
      @RequestParam("start") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime start,
      @RequestParam("end") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime end,
      @RequestParam(value = "page", defaultValue = "0") @Min(0) int page,
      @RequestParam(value = "size", defaultValue = "20") @Min(1) @Max(100) int size,
      @RequestParam(value = "sort", defaultValue = "occurredAt") String sort,
      @RequestParam(value = "direction", defaultValue = "DESC") String direction,
      HttpServletRequest httpRequest) {
    validateAllowedQueryParameters(httpRequest, Set.of("start", "end", "page", "size", "sort", "direction"));
    if (start != null && end != null && start.isAfter(end)) {
      throw new BadRequestException("Start timestamp cannot be after end timestamp", "INVALID_DATE_RANGE");
    }
    Pageable pageable = PaginationUtils.buildPageable(
        page, size, sort, direction, PaginationUtils.AUDIT_LOG_SORT_FIELDS, "occurredAt", Sort.Direction.DESC
    );
    Page<AuditLogResponse> logs = auditLogService.getAuditLogsByOccurredAtBetween(start, end, pageable);
    return ResponseEntity.ok(ApiResponse.ok(PagedResponse.of(logs)));
  }

  /**
   * Reconstructs an entity's chronological audit trail timeline.
   *
   * @param type target entity type name (e.g., Placement, TrainingEnrollment)
   * @param id   primary key identifier of the target entity
   * @return 200 OK with AuditTrailTimelineResponse enveloped in ApiResponse
   */
  @GetMapping("/entities/{type}/{id}/timeline")
  public ResponseEntity<ApiResponse<AuditTrailTimelineResponse>> getEntityTimeline(
      @PathVariable("type") String type,
      @PathVariable("id") @Positive Long id) {
    AuditTrailTimelineResponse timeline = auditLogService.getEntityTimeline(type, id);
    return ResponseEntity.ok(ApiResponse.ok(timeline));
  }

  /**
   * Records a new audit log entry manually. Restricted strictly to administrative callers.
   *
   * @param request     audit log creation payload
   * @param httpRequest HTTP servlet request for URI extraction
   * @return 201 Created with created AuditLogResponse enveloped in ApiResponse
   */
  @PostMapping
  @PreAuthorize("hasAuthority('system.manage')")
  public ResponseEntity<ApiResponse<AuditLogResponse>> recordAuditLog(
      @Valid @RequestBody CreateAuditLogRequest request,
      HttpServletRequest httpRequest) {
    AuditLogResponse response = auditLogService.recordAuditLog(request);
    return ResponseEntity.status(HttpStatus.CREATED)
        .body(ApiResponse.success(
            "Audit log recorded successfully",
            response,
            httpRequest.getRequestURI()));
  }

  private void validateAllowedQueryParameters(HttpServletRequest httpRequest, Set<String> allowedParams) {
    if (httpRequest == null) {
      return;
    }
    for (String paramName : httpRequest.getParameterMap().keySet()) {
      if (!allowedParams.contains(paramName)) {
        throw new BadRequestException("Unsupported query parameter: " + paramName, "UNSUPPORTED_PARAMETER");
      }
    }
  }
}
