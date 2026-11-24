package in.gov.sih.sih26135.mapper;

import in.gov.sih.sih26135.dto.response.TrainingCenterResponse;
import in.gov.sih.sih26135.entity.TrainingCenter;
import org.springframework.stereotype.Component;

@Component
public class TrainingCenterMapper {

  public TrainingCenterResponse toResponse(TrainingCenter entity) {
    if (entity == null) {
      return null;
    }

    Long providerId = null;
    String providerCode = null;
    String providerName = null;
    if (entity.getTrainingProvider() != null) {
      providerId = entity.getTrainingProvider().getId();
      providerCode = entity.getTrainingProvider().getProviderCode();
      providerName = entity.getTrainingProvider().getProviderName();
    }

    TrainingCenterResponse response = new TrainingCenterResponse(
        entity.getId(),
        providerId,
        providerCode,
        providerName,
        entity.getCenterCode(),
        entity.getCenterName(),
        entity.getAddressLine1(),
        entity.getAddressLine2(),
        entity.getPincode(),
        entity.getLocationId(),
        entity.getStateId(),
        entity.getDistrictId(),
        entity.getOperationalCapacity(),
        entity.getLifecycleStatusId(),
        entity.getCreatedAt(),
        entity.getUpdatedAt(),
        entity.getDeletedAt()
    );
    response.setVenueType(entity.getVenueType());
    return response;
  }
}
