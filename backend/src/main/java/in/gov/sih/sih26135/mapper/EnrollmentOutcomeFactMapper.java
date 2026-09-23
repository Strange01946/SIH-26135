package in.gov.sih.sih26135.mapper;

import in.gov.sih.sih26135.dto.response.EnrollmentOutcomeFactResponse;
import in.gov.sih.sih26135.entity.analytics.EnrollmentOutcomeFact;
import org.springframework.stereotype.Component;

@Component
public class EnrollmentOutcomeFactMapper {

  public EnrollmentOutcomeFactResponse toResponse(EnrollmentOutcomeFact entity) {
    if (entity == null) {
      return null;
    }
    return new EnrollmentOutcomeFactResponse(
        entity.getEnrollmentId(),
        entity.getEnrollmentNumber(),
        entity.getTraineeId(),
        entity.getProgramId(),
        entity.getSchemeId(),
        entity.getCourseId(),
        entity.getProviderId(),
        entity.getCenterId(),
        entity.getBatchId(),
        entity.getEnrollmentDate(),
        entity.getStartDate(),
        entity.getActualCompletionDate(),
        entity.getEnrollmentStatusId(),
        entity.getEnrollmentStatusCode(),
        entity.getIsCompletedFlag(),
        entity.getTraineeStateId(),
        entity.getTraineeDistrictId(),
        entity.getIssuedCertificateCount(),
        entity.getHasIssuedCertificateFlag(),
        entity.getPlacementCount(),
        entity.getJoinedPlacementCount(),
        entity.getHasJoinedPlacementFlag(),
        entity.getEmploymentCount(),
        entity.getCurrentEmploymentCount(),
        entity.getWageEmploymentCount(),
        entity.getSelfEmploymentCount(),
        entity.getApprenticeshipCount(),
        entity.getHasEmploymentFlag()
    );
  }
}
