package in.gov.sih.sih26135.dto.response;

import java.util.List;

public record TraineeCareerTimelineResponse(
    Long traineeId,
    List<EmploymentRecordResponse> employmentRecords,
    List<EmploymentExitEventResponse> exitEvents,
    List<TraineeUnemploymentEventResponse> unemploymentEvents
) {}
