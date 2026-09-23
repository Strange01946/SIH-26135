package in.gov.sih.sih26135.dto.response;

public record EmploymentExitAndUnemploymentResponse(
    EmploymentExitEventResponse exitEvent,
    TraineeUnemploymentEventResponse unemploymentEvent
) {}
