package in.gov.sih.sih26135.dto.response;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * Standardized response record for Regional Disparity Analytics.
 * Evaluates performance spread, lowest-performing districts (Q06 ASC priority),
 * underserved districts without training centers, and explicit source data availability documentation.
 */
public record RegionalDisparityResponse(
    List<DistrictPerformanceResponse> lowestPerformingDistricts,
    List<DistrictPerformanceResponse> topPerformingDistricts,
    BigDecimal employmentRateGapPct,
    BigDecimal placementRateGapPct,
    Long districtsWithZeroEnrollment,
    Long districtsWithoutTrainingCenters,
    Map<String, String> dataAvailabilityNotes
) {}
