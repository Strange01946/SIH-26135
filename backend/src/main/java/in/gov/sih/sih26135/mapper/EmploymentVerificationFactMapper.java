package in.gov.sih.sih26135.mapper;

import in.gov.sih.sih26135.dto.response.EmploymentVerificationFactResponse;
import in.gov.sih.sih26135.entity.analytics.EmploymentVerificationFact;
import org.springframework.stereotype.Component;

@Component
public class EmploymentVerificationFactMapper {

  public EmploymentVerificationFactResponse toResponse(EmploymentVerificationFact entity) {
    if (entity == null) {
      return null;
    }
    return new EmploymentVerificationFactResponse(
        entity.getEmploymentVerificationId(),
        entity.getVerificationNumber(),
        entity.getEmploymentId(),
        entity.getTraineeId(),
        entity.getPlacementId(),
        entity.getEmployerId(),
        entity.getCycleNumber(),
        entity.getIsReverification(),
        entity.getIsCurrent(),
        entity.getRecordVerificationStatusId(),
        entity.getRecordVerificationStatusCode(),
        entity.getIsVerifiedFlag(),
        entity.getEmploymentVerificationMethodId(),
        entity.getVerificationMethodCode(),
        entity.getIsTraineeSelfReported(),
        entity.getIsOfficialFlag(),
        entity.getEmploymentInfoSourceId(),
        entity.getRequestedAt(),
        entity.getVerifiedAt(),
        entity.getTraineeStateId(),
        entity.getTraineeDistrictId()
    );
  }
}
