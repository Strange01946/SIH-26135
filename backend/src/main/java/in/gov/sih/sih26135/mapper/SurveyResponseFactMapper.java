package in.gov.sih.sih26135.mapper;

import in.gov.sih.sih26135.dto.response.SurveyResponseFactResponse;
import in.gov.sih.sih26135.entity.analytics.SurveyResponseFact;
import org.springframework.stereotype.Component;

@Component
public class SurveyResponseFactMapper {

  public SurveyResponseFactResponse toResponse(SurveyResponseFact entity) {
    if (entity == null) {
      return null;
    }
    return new SurveyResponseFactResponse(
        entity.getSurveyResponseId(),
        entity.getSurveyId(),
        entity.getSurveyPurposeId(),
        entity.getSurveyPurposeCode(),
        entity.getProgramId(),
        entity.getCourseId(),
        entity.getBatchId(),
        entity.getFollowupOffsetMonths(),
        entity.getTraineeId(),
        entity.getEnrollmentId(),
        entity.getFollowupTaskId(),
        entity.getAttemptNumber(),
        entity.getSurveyResponseStatusId(),
        entity.getResponseStatusCode(),
        entity.getIsSubmittedFlag(),
        entity.getStartedAt(),
        entity.getSubmittedAt(),
        entity.getTraineeStateId(),
        entity.getTraineeDistrictId()
    );
  }
}
