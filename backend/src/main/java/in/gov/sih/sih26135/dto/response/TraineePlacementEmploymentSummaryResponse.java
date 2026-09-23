package in.gov.sih.sih26135.dto.response;

import java.util.List;

public record TraineePlacementEmploymentSummaryResponse(
    Long traineeId,
    List<PlacementRecordResponse> placements,
    List<EmploymentRecordResponse> employmentRecords
) {}
