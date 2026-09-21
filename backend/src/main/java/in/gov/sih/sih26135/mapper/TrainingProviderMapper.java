package in.gov.sih.sih26135.mapper;

import in.gov.sih.sih26135.dto.request.CreateTrainingProviderRequest;
import in.gov.sih.sih26135.dto.response.TrainingProviderResponse;
import in.gov.sih.sih26135.entity.RefAccreditationStatus;
import in.gov.sih.sih26135.entity.TrainingProvider;
import org.springframework.stereotype.Component;

@Component
public class TrainingProviderMapper {

  public TrainingProviderResponse toResponse(TrainingProvider entity) {
    if (entity == null) {
      return null;
    }

    Long accreditationStatusId = null;
    String accreditationStatusCode = null;
    String accreditationStatusName = null;
    if (entity.getAccreditationStatus() != null) {
      accreditationStatusId = entity.getAccreditationStatus().getId();
      accreditationStatusCode = entity.getAccreditationStatus().getStatusCode();
      accreditationStatusName = entity.getAccreditationStatus().getStatusName();
    }

    return new TrainingProviderResponse(
        entity.getId(),
        entity.getProviderCode(),
        entity.getProviderName(),
        entity.getRegistrationNumber(),
        entity.getOrganizationTypeId(),
        entity.getOrganizationId(),
        entity.getContactPersonName(),
        entity.getContactEmail(),
        entity.getContactPhone(),
        entity.getAddressLine1(),
        entity.getAddressLine2(),
        entity.getPincode(),
        entity.getLocationId(),
        entity.getStateId(),
        entity.getDistrictId(),
        accreditationStatusId,
        accreditationStatusCode,
        accreditationStatusName,
        entity.getRating(),
        entity.getLifecycleStatusId(),
        entity.getCreatedAt(),
        entity.getUpdatedAt(),
        entity.getDeletedAt()
    );
  }

  public TrainingProvider toEntity(CreateTrainingProviderRequest request, RefAccreditationStatus accreditationStatus) {
    if (request == null) {
      return null;
    }

    TrainingProvider provider = new TrainingProvider();
    provider.setProviderCode(request.getProviderCode());
    provider.setProviderName(request.getProviderName());
    provider.setRegistrationNumber(request.getRegistrationNumber());
    provider.setOrganizationTypeId(request.getOrganizationTypeId());
    provider.setOrganizationId(request.getOrganizationId());
    provider.setContactPersonName(request.getContactPersonName());
    provider.setContactEmail(request.getContactEmail());
    provider.setContactPhone(request.getContactPhone());
    provider.setAddressLine1(request.getAddressLine1());
    provider.setAddressLine2(request.getAddressLine2());
    provider.setPincode(request.getPincode());
    provider.setLocationId(request.getLocationId());
    provider.setStateId(request.getStateId());
    provider.setDistrictId(request.getDistrictId());
    provider.setAccreditationStatus(accreditationStatus);
    provider.setRating(request.getRating());
    provider.setLifecycleStatusId(request.getLifecycleStatusId());
    return provider;
  }
}
