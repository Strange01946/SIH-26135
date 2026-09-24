package in.gov.sih.sih26135.controller;

import in.gov.sih.sih26135.dto.request.CreateAuditLogRequest;
import in.gov.sih.sih26135.dto.response.AuditLogResponse;
import in.gov.sih.sih26135.exception.BadRequestException;
import in.gov.sih.sih26135.response.ApiResponse;
import in.gov.sih.sih26135.service.AuditLogService;
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
 * REST controller for managing Audit Log domain resources.
 *
 * <p>Base Route: /api/v1/audit-logs
 * Consumes: CreateAuditLogRequest
 * Produces: AuditLogResponse enveloped in ApiResponse
 *
 * <p>AuditLog is an append-only audit trail; updates and deletions are strictly prohibited.
 */
@RestController
@RequestMapping("/api/v1/audit-logs")
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
  public ResponseEntity<ApiResponse<AuditLogResponse>> getById(@PathVariable Long id) {
    AuditLogResponse response = auditLogService.getAuditLogById(id);
    return ResponseEntity.ok(ApiResponse.ok(response));
  }

  /**
   * Retrieves all audit log entries created by a specific actor user.
   *
   * @param actorUserId actor user identifier
   * @param httpRequest HTTP servlet request to verify query parameters
   * @return 200 OK with list of AuditLogResponse enveloped in ApiResponse
   */
  @GetMapping(params = {
      "actorUserId", "!auditActionId", "!entityType", "!entityId",
      "!correlationId", "!start", "!end"
  })
  public ResponseEntity<ApiResponse<List<AuditLogResponse>>> getByActorUserId(
      @RequestParam("actorUserId") Long actorUserId,
      HttpServletRequest httpRequest) {
    validateOnlyQueryParameter(httpRequest, "actorUserId");
    List<AuditLogResponse> logs = auditLogService.getAuditLogsByActorUserId(actorUserId);
    return ResponseEntity.ok(ApiResponse.ok(logs));
  }

  /**
   * Retrieves all audit log entries for a specific audit action.
   *
   * @param auditActionId audit action reference identifier
   * @param httpRequest HTTP servlet request to verify query parameters
   * @return 200 OK with list of AuditLogResponse enveloped in ApiResponse
   */
  @GetMapping(params = {
      "auditActionId", "!actorUserId", "!entityType", "!entityId",
      "!correlationId", "!start", "!end"
  })
  public ResponseEntity<ApiResponse<List<AuditLogResponse>>> getByActionId(
      @RequestParam("auditActionId") Long auditActionId,
      HttpServletRequest httpRequest) {
    validateOnlyQueryParameter(httpRequest, "auditActionId");
    List<AuditLogResponse> logs = auditLogService.getAuditLogsByActionId(auditActionId);
    return ResponseEntity.ok(ApiResponse.ok(logs));
  }

  /**
   * Retrieves all audit log entries for a specific entity type and entity identifier.
   *
   * @param entityType target entity type name
   * @param entityId target entity primary key identifier
   * @param httpRequest HTTP servlet request to verify query parameters
   * @return 200 OK with list of AuditLogResponse enveloped in ApiResponse
   */
  @GetMapping(params = {
      "entityType", "entityId", "!actorUserId", "!auditActionId",
      "!correlationId", "!start", "!end"
  })
  public ResponseEntity<ApiResponse<List<AuditLogResponse>>> getByEntity(
      @RequestParam("entityType") String entityType,
      @RequestParam("entityId") Long entityId,
      HttpServletRequest httpRequest) {
    validateCompoundQueryParameters(httpRequest, Set.of("entityType", "entityId"));
    List<AuditLogResponse> logs = auditLogService.getAuditLogsByEntity(entityType, entityId);
    return ResponseEntity.ok(ApiResponse.ok(logs));
  }

  /**
   * Retrieves all audit log entries for a specific entity type.
   *
   * @param entityType target entity type name
   * @param httpRequest HTTP servlet request to verify query parameters
   * @return 200 OK with list of AuditLogResponse enveloped in ApiResponse
   */
  @GetMapping(params = {
      "entityType", "!entityId", "!actorUserId", "!auditActionId",
      "!correlationId", "!start", "!end"
  })
  public ResponseEntity<ApiResponse<List<AuditLogResponse>>> getByEntityType(
      @RequestParam("entityType") String entityType,
      HttpServletRequest httpRequest) {
    validateOnlyQueryParameter(httpRequest, "entityType");
    List<AuditLogResponse> logs = auditLogService.getAuditLogsByEntityType(entityType);
    return ResponseEntity.ok(ApiResponse.ok(logs));
  }

  /**
   * Retrieves all audit log entries with a specific correlation identifier.
   *
   * @param correlationId correlation identifier
   * @param httpRequest HTTP servlet request to verify query parameters
   * @return 200 OK with list of AuditLogResponse enveloped in ApiResponse
   */
  @GetMapping(params = {
      "correlationId", "!actorUserId", "!auditActionId", "!entityType",
      "!entityId", "!start", "!end"
  })
  public ResponseEntity<ApiResponse<List<AuditLogResponse>>> getByCorrelationId(
      @RequestParam("correlationId") String correlationId,
      HttpServletRequest httpRequest) {
    validateOnlyQueryParameter(httpRequest, "correlationId");
    List<AuditLogResponse> logs = auditLogService.getAuditLogsByCorrelationId(correlationId);
    return ResponseEntity.ok(ApiResponse.ok(logs));
  }

  /**
   * Retrieves all audit log entries occurring between two timestamps.
   *
   * @param start start timestamp (ISO-8601 date-time)
   * @param end end timestamp (ISO-8601 date-time)
   * @param httpRequest HTTP servlet request to verify query parameters
   * @return 200 OK with list of AuditLogResponse enveloped in ApiResponse
   */
  @GetMapping(params = {
      "start", "end", "!actorUserId", "!auditActionId",
      "!entityType", "!entityId", "!correlationId"
  })
  public ResponseEntity<ApiResponse<List<AuditLogResponse>>> getByOccurredAtBetween(
      @RequestParam("start") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime start,
      @RequestParam("end") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime end,
      HttpServletRequest httpRequest) {
    validateCompoundQueryParameters(httpRequest, Set.of("start", "end"));
    List<AuditLogResponse> logs = auditLogService.getAuditLogsByOccurredAtBetween(start, end);
    return ResponseEntity.ok(ApiResponse.ok(logs));
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
      @RequestBody CreateAuditLogRequest request,
      HttpServletRequest httpRequest) {
    AuditLogResponse response = auditLogService.recordAuditLog(request);
    return ResponseEntity.status(HttpStatus.CREATED)
        .body(ApiResponse.success(
            "Audit log recorded successfully",
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
