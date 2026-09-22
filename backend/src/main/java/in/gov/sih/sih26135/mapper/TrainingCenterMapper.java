package in.gov.sih.sih26135.mapper;

import in.gov.sih.sih26135.dto.request.CreateTrainingCenterRequest;
import in.gov.sih.sih26135.dto.response.TrainingCenterResponse;
import in.gov.sih.sih26135.entity.TrainingCenter;
import in.gov.sih.sih26135.entity.TrainingProvider;
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

    return new TrainingCenterResponse(
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
  }

  public TrainingCenter toEntity(CreateTrainingCenterRequest request, TrainingProvider provider) {
    if (request == null) {
      return null;
    }

    TrainingCenter center = new TrainingCenter();
    center.setTrainingProvider(provider);
    center.setCenterCode(request.getCenterCode());
    center.setCenterName(request.getCenterName());
    center.setAddressLine1(request.getAddressLine1());
    center.setAddressLine2(request.getAddressLine2());
    center.setPincode(request.getPincode());
    center.setLocationId(request.getLocationId());
    center.setStateId(request.getStateId());
    center.setDistrictId(request.getDistrictId());
    center.setOperationalCapacity(request.getOperationalCapacity());
    center.setLifecycleStatusId(request.getLifecycleStatusId());
    return center;
  }
}
