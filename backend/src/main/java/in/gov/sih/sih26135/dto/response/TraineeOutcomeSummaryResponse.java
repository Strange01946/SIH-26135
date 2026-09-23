package in.gov.sih.sih26135.dto.response;

import java.math.BigDecimal;

public record TraineeOutcomeSummaryResponse(
    Long traineeId,
    Long stateId,
    Long districtId,
    Long currentEmploymentStatusId,
    String snapshotEmploymentStatusCode,
    Boolean snapshotIsEmployedFlag,
    Long enrollmentCount,
    BigDecimal completedEnrollmentCount,
    BigDecimal certifiedEnrollmentCount,
    BigDecimal placedEnrollmentCount,
    BigDecimal employedEnrollmentCount,
    Long employmentSpellCount,
    BigDecimal currentEmploymentCount,
    BigDecimal currentWageEmploymentCount,
    BigDecimal currentSelfEmploymentCount,
    Long unemploymentPeriodCount,
    BigDecimal currentUnemploymentCount,
    Long currentSkillGapCount,
    BigDecimal completionRatePct
) {}
