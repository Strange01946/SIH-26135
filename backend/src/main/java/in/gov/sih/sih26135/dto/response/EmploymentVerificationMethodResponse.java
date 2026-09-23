package in.gov.sih.sih26135.dto.response;

import java.time.LocalDateTime;

public record EmploymentVerificationMethodResponse(
    Long id,
    String methodCode,
    String methodName,
    Boolean isEmployerSide,
    Boolean isTraineeSelfReported,
    Boolean isOfficialFlag,
    Integer sortOrder,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {}
