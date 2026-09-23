package in.gov.sih.sih26135.dto.response;

import java.time.LocalDate;

public record EmploymentRetentionFactResponse(
    Long employmentId,
    Long traineeId,
    Long enrollmentId,
    Long engagementTypeId,
    LocalDate startDate,
    LocalDate endDate,
    Boolean isCurrent,
    Integer retained6mFlag,
    Integer retained12mFlag,
    Integer retained24mFlag,
    Integer retained36mFlag
) {}
