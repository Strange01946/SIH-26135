package in.gov.sih.sih26135.dto.response;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record JobApplicationResponse(
    Long id,
    Long traineeId,
    String traineeRegistrationNumber,
    String traineeFirstName,
    String traineeLastName,
    Long enrollmentId,
    String enrollmentNumber,
    Long jobPostingId,
    String jobPostingCode,
    String jobPostingTitle,
    Long applicationStatusId,
    String applicationStatusCode,
    LocalDate appliedDate,
    Long referredByUserId,
    Long nonSelectionReasonId,
    String nonSelectionReasonCode,
    String remarks,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {}
