package in.gov.sih.sih26135.dto.response;

import java.util.List;

public record EmploymentVerificationSummaryResponse(
    EmploymentRecordResponse employmentRecord,
    List<EmploymentVerificationRequestResponse> requests,
    List<EmploymentVerificationAttemptResponse> attempts,
    List<EmploymentVerificationResponse> verifications
) {}
