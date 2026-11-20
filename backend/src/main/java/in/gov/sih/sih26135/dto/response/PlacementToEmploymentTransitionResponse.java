package in.gov.sih.sih26135.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Response payload after successful placement-to-employment transition")
public record PlacementToEmploymentTransitionResponse(
    @Schema(description = "Updated placement record")
    PlacementRecordResponse placement,
    @Schema(description = "Newly created active employment record")
    EmploymentRecordResponse employmentRecord,
    @Schema(description = "Initial salary compensation record")
    SalaryHistoryResponse initialSalary
) {}
