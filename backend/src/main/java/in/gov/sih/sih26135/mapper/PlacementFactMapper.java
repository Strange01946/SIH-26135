package in.gov.sih.sih26135.mapper;

import in.gov.sih.sih26135.dto.response.PlacementFactResponse;
import in.gov.sih.sih26135.entity.analytics.PlacementFact;
import org.springframework.stereotype.Component;

@Component
public class PlacementFactMapper {

  public PlacementFactResponse toResponse(PlacementFact entity) {
    if (entity == null) {
      return null;
    }
    return new PlacementFactResponse(
        entity.getPlacementId(),
        entity.getPlacementNumber(),
        entity.getTraineeId(),
        entity.getEnrollmentId(),
        entity.getCourseId(),
        entity.getProgramId(),
        entity.getProviderId(),
        entity.getEmployerId(),
        entity.getJobRoleId(),
        entity.getEngagementTypeId(),
        entity.getEngagementTypeCode(),
        entity.getIsWageEmployment(),
        entity.getIsSelfEmployment(),
        entity.getIsApprenticeship(),
        entity.getIsInternship(),
        entity.getPlacementSourceId(),
        entity.getPlacementSourceCode(),
        entity.getPlacementStatusId(),
        entity.getPlacementStatusCode(),
        entity.getPlacementStatusJoinedFlag(),
        entity.getIsUnsuccessfulFlag(),
        entity.getJoiningStatusId(),
        entity.getJoiningStatusCode(),
        entity.getIsJoinedFlag(),
        entity.getJoinedFlag(),
        entity.getOfferedSalary(),
        entity.getJoiningSalary(),
        entity.getSalaryFrequencyId(),
        entity.getCurrencyCode(),
        entity.getOfferDate(),
        entity.getActualJoiningDate(),
        entity.getWorkStateId(),
        entity.getWorkDistrictId(),
        entity.getRecordVerificationStatusId(),
        entity.getRecordVerificationStatusCode(),
        entity.getIsVerifiedFlag(),
        entity.getTraineeStateId(),
        entity.getTraineeDistrictId()
    );
  }
}
