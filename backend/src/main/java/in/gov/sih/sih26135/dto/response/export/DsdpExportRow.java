package in.gov.sih.sih26135.dto.response.export;

import java.math.BigDecimal;

/**
 * Represents a single tabular/JSON row in the District Skill Development Plan (DSDP) MIS export.
 * Reflects course-level outcomes, trainee volumes, and employment metrics within a specific district.
 */
public record DsdpExportRow(
    Long districtId,
    String districtCode,
    String districtName,
    Long stateId,
    String stateCode,
    String stateName,
    Long courseId,
    String courseCode,
    String courseName,
    String sectorName,
    Long enrollmentCount,
    Long traineeCount,
    Long completedCount,
    Long certifiedCount,
    Long joinedPlacementCount,
    Long employedCount,
    BigDecimal completionRatePct,
    BigDecimal certificationRatePct,
    BigDecimal placementRatePct,
    BigDecimal employmentRatePct,
    BigDecimal avgStartingWage
) {}
