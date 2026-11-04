package in.gov.sih.sih26135.dto.response;

import java.math.BigDecimal;

/**
 * Standardized response DTO representing salary observation distribution across
 * mandated economic brackets:
 * <ul>
 *   <li>Below ₹15,000 ({@code < 15,000})</li>
 *   <li>₹15,000–₹25,000 ({@code >= 15,000 AND <= 25,000})</li>
 *   <li>₹25,000–₹50,000 ({@code > 25,000 AND <= 50,000})</li>
 *   <li>Above ₹50,000 ({@code > 50,000})</li>
 * </ul>
 */
public record SalaryDistributionResponse(
    String bracketCode,
    String bracketName,
    Long count,
    BigDecimal percentage
) {}
