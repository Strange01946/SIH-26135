package in.gov.sih.sih26135.dto.response;

import java.math.BigDecimal;

/**
 * Standardized response DTO representing follow-up SLA tracking status and compliance.
 *
 * <p>Explicitly distinguishes between supported operational metrics and unconfigured / deferred SLA targets.
 *
 * @param isAvailable whether formal SLA deadline definitions and targets are configured in the schema
 * @param statusMessage explanation of SLA tracking status and configuration availability
 * @param totalSlaEligibleTasks total tasks evaluated for SLA compliance (null if unconfigured)
 * @param completedWithinSlaCount tasks completed within SLA target timeframe (null if unconfigured)
 * @param overdueTaskCount tasks open beyond SLA target timeframe (null if unconfigured)
 * @param slaComplianceRatePct percentage of tasks completed within SLA (null if unconfigured)
 */
public record FollowupSlaResponse(
    Boolean isAvailable,
    String statusMessage,
    Long totalSlaEligibleTasks,
    Long completedWithinSlaCount,
    Long overdueTaskCount,
    BigDecimal slaComplianceRatePct
) {}
