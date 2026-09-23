package in.gov.sih.sih26135.mapper;

import in.gov.sih.sih26135.dto.response.EmploymentExitFactResponse;
import in.gov.sih.sih26135.entity.analytics.EmploymentExitFact;
import org.springframework.stereotype.Component;

@Component
public class EmploymentExitFactMapper {

  public EmploymentExitFactResponse toResponse(EmploymentExitFact entity) {
    if (entity == null) {
      return null;
    }
    return new EmploymentExitFactResponse(
        entity.getEmploymentExitEventId(),
        entity.getEmploymentId(),
        entity.getTraineeId(),
        entity.getEnrollmentId(),
        entity.getSeparationDate(),
        entity.getEmploymentExitReasonId(),
        entity.getExitReasonCode(),
        entity.getSeparationNatureId(),
        entity.getSeparationNatureCode(),
        entity.getIsVoluntaryFlag(),
        entity.getIsInvoluntaryFlag(),
        entity.getEmploymentInfoSourceId(),
        entity.getInfoSourceCode(),
        entity.getRecordVerificationStatusId(),
        entity.getRecordVerificationStatusCode(),
        entity.getTraineeStateId(),
        entity.getTraineeDistrictId()
    );
  }
}
