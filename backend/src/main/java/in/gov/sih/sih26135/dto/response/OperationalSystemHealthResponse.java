package in.gov.sih.sih26135.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Map;

/**
 * Phase 28.7 response DTO for {@code GET /api/v1/operational/system-health}.
 */
public record OperationalSystemHealthResponse(
    Long totalIncidents,
    Long criticalEventCount,
    Long errorEventCount,
    Long warningEventCount,
    Long infoEventCount,
    Map<String, Long> incidentsBySeverity,
    Map<String, Long> incidentsByCategory,
    Long correlatedIncidentsCount,
    BigDecimal criticalEventsPerHour,
    BigDecimal errorEventsPerHour,
    String velocityUnit,
    Long auditMutationVolume,
    Long distinctEntitiesModified,
    Map<String, Long> auditMutationsByAction,
    Long auditNonMutationCount,
    String suppressionDisclosure,
    LocalDateTime windowStart,
    LocalDateTime windowEndExclusive,
    BigDecimal windowDurationHours,
    LocalDateTime evaluatedAt
) {

  public OperationalSystemHealthResponse {
    incidentsBySeverity = incidentsBySeverity != null
        ? Collections.unmodifiableMap(incidentsBySeverity)
        : Map.of();
    incidentsByCategory = incidentsByCategory != null
        ? Collections.unmodifiableMap(incidentsByCategory)
        : Map.of();
    auditMutationsByAction = auditMutationsByAction != null
        ? Collections.unmodifiableMap(auditMutationsByAction)
        : Map.of();
  }
}
