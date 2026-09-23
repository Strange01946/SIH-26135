package in.gov.sih.sih26135.dto.response;

public record PlacementToEmploymentTransitionResponse(
    PlacementRecordResponse placement,
    EmploymentRecordResponse employmentRecord,
    SalaryHistoryResponse initialSalary
) {}
