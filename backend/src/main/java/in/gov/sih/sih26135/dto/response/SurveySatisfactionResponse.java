package in.gov.sih.sih26135.dto.response;

import java.math.BigDecimal;

/**
 * Standardized response DTO representing survey satisfaction feedback analytics.
 *
 * <p>Explicitly indicates whether standardized rating scales and questions are configured.
 *
 * @param isAvailable whether standardized satisfaction rating questions are configured
 * @param statusMessage explanation of satisfaction rating availability
 * @param averageRating average numeric rating across submitted responses (null if unconfigured)
 * @param totalRatingsCount total submitted responses with numeric ratings (null if unconfigured)
 */
public record SurveySatisfactionResponse(
    Boolean isAvailable,
    String statusMessage,
    BigDecimal averageRating,
    Long totalRatingsCount
) {}
