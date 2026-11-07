package in.gov.sih.sih26135.dto.response;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;

/**
 * Standardized response DTO representing system-wide data quality health metrics and cleanliness scorecards.
 */
public record DataQualityHealthScorecardResponse(
    Long evaluatedRecordCount,
    Long totalActiveIssues,
    Long criticalIssueCount,
    Long highIssueCount,
    Long mediumIssueCount,
    Long lowIssueCount,
    BigDecimal cleanlinessScore,
    LocalDateTime evaluatedAt
) {

  public DataQualityHealthScorecardResponse {
    cleanlinessScore = cleanlinessScore != null
        ? cleanlinessScore.setScale(2, RoundingMode.HALF_UP)
        : BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
  }
}
