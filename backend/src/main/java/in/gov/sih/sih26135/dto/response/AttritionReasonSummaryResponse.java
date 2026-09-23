package in.gov.sih.sih26135.dto.response;

public record AttritionReasonSummaryResponse(
    Long employmentExitReasonId,
    String exitReasonCode,
    Long separationNatureId,
    String separationNatureCode,
    Boolean isVoluntaryFlag,
    Boolean isInvoluntaryFlag,
    Long exitCount,
    Long traineeCount
) {}
