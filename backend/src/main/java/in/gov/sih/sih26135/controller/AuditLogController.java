package in.gov.sih.sih26135.controller;

import org.springframework.validation.annotation.Validated;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Max;
import jakarta.validation.Valid;
import in.gov.sih.sih26135.dto.request.CreateAuditLogRequest;
import in.gov.sih.sih26135.dto.response.AuditLogResponse;
import in.gov.sih.sih26135.exception.BadRequestException;
import in.gov.sih.sih26135.response.ApiResponse;
import in.gov.sih.sih26135.response.PagedResponse;
import in.gov.sih.sih26135.response.PaginationUtils;
import in.gov.sih.sih26135.service.AuditLogService;
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
 * REST controller for managing Audit Log domain resources.
 *
 * <p>Base Route: /api/v1/audit-logs
 * Consumes: CreateAuditLogRequest
 * Produces: AuditLogResponse enveloped in ApiResponse
 *
 * <p>AuditLog is an append-only audit trail; updates and deletions are strictly prohibited.
 */
@Validated
@RestController
@RequestMapping("/api/v1/audit-logs")
@PreAuthorize("hasAnyAuthority('audit.read', 'system.manage')")
public class AuditLogController {

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
   * Retrieves paginated audit log entries across the entire system.
   *
   * @param page zero-based page index (default: 0)
   * @param size page size (default: 20, max: 100)
   * @param sort sort field (default: occurredAt)
   * @param direction sort direction (ASC or DESC, default: DESC)
   * @param httpRequest HTTP servlet request to verify query parameters
   * @return 200 OK with paginated AuditLogResponse enveloped in ApiResponse
   */
  @GetMapping(params = {
      "!actorUserId", "!auditActionId", "!entityType", "!entityId",
      "!correlationId", "!start", "!end"
  })
  public ResponseEntity<ApiResponse<PagedResponse<AuditLogResponse>>> getAllAuditLogs(
      @RequestParam(value = "page", defaultValue = "0") @Min(0) int page,
      @RequestParam(value = "size", defaultValue = "20") @Min(1) @Max(100) int size,
      @RequestParam(value = "sort", defaultValue = "occurredAt") String sort,
      @RequestParam(value = "direction", defaultValue = "DESC") String direction,
      HttpServletRequest httpRequest) {
    validateAllowedQueryParameters(httpRequest, Set.of("page", "size", "sort", "direction"));
    Pageable pageable = PaginationUtils.buildPageable(
        page, size, sort, direction, PaginationUtils.AUDIT_LOG_SORT_FIELDS, "occurredAt", Sort.Direction.DESC
    );
    Page<AuditLogResponse> logs = auditLogService.getAllAuditLogs(pageable);
    return ResponseEntity.ok(ApiResponse.ok(PagedResponse.of(logs)));
  }

  /**
   * Retrieves paginated audit log entries created by a specific actor user.
   *
   * @param actorUserId actor user identifier
   * @param page zero-based page index (default: 0)
   * @param size page size (default: 20, max: 100)
   * @param sort sort field (default: occurredAt)
   * @param direction sort direction (ASC or DESC, default: DESC)
   * @param httpRequest HTTP servlet request to verify query parameters
   * @return 200 OK with paginated AuditLogResponse enveloped in ApiResponse
   */
  @GetMapping(params = {
      "actorUserId", "!auditActionId", "!entityType", "!entityId",
      "!correlationId", "!start", "!end"
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
   * Retrieves paginated audit log entries for a specific audit action.
   *
   * @param auditActionId audit action reference identifier
   * @param page zero-based page index (default: 0)
   * @param size page size (default: 20, max: 100)
   * @param sort sort field (default: occurredAt)
   * @param direction sort direction (ASC or DESC, default: DESC)
   * @param httpRequest HTTP servlet request to verify query parameters
   * @return 200 OK with paginated AuditLogResponse enveloped in ApiResponse
   */
  @GetMapping(params = {
      "auditActionId", "!actorUserId", "!entityType", "!entityId",
      "!correlationId", "!start", "!end"
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
   * Retrieves paginated audit log entries for a specific entity type and entity identifier.
   *
   * @param entityType target entity type name
   * @param entityId target entity primary key identifier
   * @param page zero-based page index (default: 0)
   * @param size page size (default: 20, max: 100)
   * @param sort sort field (default: occurredAt)
   * @param direction sort direction (ASC or DESC, default: DESC)
   * @param httpRequest HTTP servlet request to verify query parameters
   * @return 200 OK with paginated AuditLogResponse enveloped in ApiResponse
   */
  @GetMapping(params = {
      "entityType", "entityId", "!actorUserId", "!auditActionId",
      "!correlationId", "!start", "!end"
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
   * Retrieves paginated audit log entries for a specific entity type.
   *
   * @param entityType target entity type name
   * @param page zero-based page index (default: 0)
   * @param size page size (default: 20, max: 100)
   * @param sort sort field (default: occurredAt)
   * @param direction sort direction (ASC or DESC, default: DESC)
   * @param httpRequest HTTP servlet request to verify query parameters
   * @return 200 OK with paginated AuditLogResponse enveloped in ApiResponse
   */
  @GetMapping(params = {
      "entityType", "!entityId", "!actorUserId", "!auditActionId",
      "!correlationId", "!start", "!end"
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
   *
   * @param correlationId correlation identifier
   * @param page zero-based page index (default: 0)
   * @param size page size (default: 20, max: 100)
   * @param sort sort field (default: occurredAt)
   * @param direction sort direction (ASC or DESC, default: DESC)
   * @param httpRequest HTTP servlet request to verify query parameters
   * @return 200 OK with paginated AuditLogResponse enveloped in ApiResponse
   */
  @GetMapping(params = {
      "correlationId", "!actorUserId", "!auditActionId", "!entityType",
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
   *
   * @param start start timestamp (ISO-8601 date-time)
   * @param end end timestamp (ISO-8601 date-time)
   * @param page zero-based page index (default: 0)
   * @param size page size (default: 20, max: 100)
   * @param sort sort field (default: occurredAt)
   * @param direction sort direction (ASC or DESC, default: DESC)
   * @param httpRequest HTTP servlet request to verify query parameters
   * @return 200 OK with paginated AuditLogResponse enveloped in ApiResponse
   */
  @GetMapping(params = {
      "start", "end", "!actorUserId", "!auditActionId",
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
    Pageable pageable = PaginationUtils.buildPageable(
        page, size, sort, direction, PaginationUtils.AUDIT_LOG_SORT_FIELDS, "occurredAt", Sort.Direction.DESC
    );
    Page<AuditLogResponse> logs = auditLogService.getAuditLogsByOccurredAtBetween(start, end, pageable);
    return ResponseEntity.ok(ApiResponse.ok(PagedResponse.of(logs)));
  }

  /**
   * Records a new audit log entry.
   *
   * @param request audit log creation payload
   * @param httpRequest HTTP servlet request for URI extraction
   * @return 201 Created with created AuditLogResponse enveloped in ApiResponse
   */
  @PostMapping
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
