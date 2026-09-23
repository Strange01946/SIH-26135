package in.gov.sih.sih26135.mapper;

import in.gov.sih.sih26135.dto.response.UnemploymentFactResponse;
import in.gov.sih.sih26135.entity.analytics.UnemploymentFact;
import org.springframework.stereotype.Component;

@Component
public class UnemploymentFactMapper {

  public UnemploymentFactResponse toResponse(UnemploymentFact entity) {
    if (entity == null) {
      return null;
    }
    return new UnemploymentFactResponse(
        entity.getUnemploymentEventId(),
        entity.getTraineeId(),
        entity.getPeriodNumber(),
        entity.getStartDate(),
        entity.getEndDate(),
        entity.getIsCurrent(),
        entity.getDurationDays(),
        entity.getLabourStatusId(),
        entity.getLabourStatusCode(),
        entity.getUnemploymentReasonId(),
        entity.getUnemploymentReasonCode(),
        entity.getPrecedingEmploymentId(),
        entity.getEmploymentExitEventId(),
        entity.getSucceedingEmploymentId(),
        entity.getReemployedFlag(),
        entity.getNeverPrecededByEmploymentFlag(),
        entity.getEnrollmentId(),
        entity.getPlacementId(),
        entity.getFollowupTaskId(),
        entity.getSurveyResponseId(),
        entity.getEmploymentInfoSourceId(),
        entity.getInfoSourceCode(),
        entity.getIsSelfReportedFlag(),
        entity.getRecordVerificationStatusId(),
        entity.getRecordVerificationStatusCode(),
        entity.getIsVerifiedFlag(),
        entity.getTraineeStateId(),
        entity.getTraineeDistrictId()
    );
  }
}
