package in.gov.sih.sih26135.dto.response;

public record UnemploymentToEmploymentTransitionResponse(
    TraineeUnemploymentEventResponse updatedUnemploymentEvent,
    EmploymentRecordResponse newEmploymentRecord,
    SalaryHistoryResponse initialSalary
) {}
