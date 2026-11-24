package in.gov.sih.sih26135.controller;

import in.gov.sih.sih26135.config.DataQualityEngineProperties;
import in.gov.sih.sih26135.config.SystemEventLoggingProperties;
import in.gov.sih.sih26135.response.ApiResponse;
import in.gov.sih.sih26135.service.SystemEventLoggingService;
import in.gov.sih.sih26135.service.SystemEventLoggingService.ThrottleCounters;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class SystemEventThrottleController {
  private final SystemEventLoggingService logging;
  private final SystemEventLoggingProperties properties;
  private final DataQualityEngineProperties dataQuality;

  public SystemEventThrottleController(SystemEventLoggingService logging,
      SystemEventLoggingProperties properties, DataQualityEngineProperties dataQuality) {
    this.logging = logging;
    this.properties = properties;
    this.dataQuality = dataQuality;
  }

  @GetMapping("/api/v1/system-event-logs/throttle-status")
  @PreAuthorize("hasAnyAuthority('system.manage', 'ROLE_ADMIN', 'ROLE_super_admin')")
  public ApiResponse<ThrottleStatus> getStatus() {
    return ApiResponse.ok(new ThrottleStatus(properties.getDedupWindowSeconds(),
        properties.getRateLimitAlertCooldownSeconds(), dataQuality.getSpikeAlertCooldownSeconds(),
        logging.getThrottleCounters()));
  }

  public record ThrottleStatus(int eventDedupWindowSeconds, int rateLimitCooldownSeconds,
      int dataQualityCooldownSeconds, ThrottleCounters suppressedSinceStartup) {}
}
