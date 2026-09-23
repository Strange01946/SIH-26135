package in.gov.sih.sih26135.dto.response;

import java.util.List;

public record RecordVerificationVerdictResponse(
    EmploymentVerificationResponse verification,
    List<EmploymentVerificationEvidenceResponse> evidenceList,
    EmploymentVerificationRequestResponse updatedRequest,
    EmploymentRecordResponse updatedEmployment
) {}
