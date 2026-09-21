package in.gov.sih.sih26135.mapper;

import in.gov.sih.sih26135.dto.request.CreateTraineeRequest;
import in.gov.sih.sih26135.dto.response.TraineeResponse;
import in.gov.sih.sih26135.entity.Trainee;
import in.gov.sih.sih26135.entity.User;
import org.springframework.stereotype.Component;

@Component
public class TraineeMapper {

  public TraineeResponse toResponse(Trainee entity) {
    if (entity == null) {
      return null;
    }

    return new TraineeResponse(
        entity.getId(),
        entity.getUser() != null ? entity.getUser().getId() : null,
        entity.getRegistrationNumber(),
        entity.getFirstName(),
        entity.getMiddleName(),
        entity.getLastName(),
        entity.getDateOfBirth(),
        entity.getGenderId(),
        entity.getEmail(),
        entity.getPhone(),
        entity.getAddressLine1(),
        entity.getAddressLine2(),
        entity.getPincode(),
        entity.getLocationId(),
        entity.getStateId(),
        entity.getDistrictId(),
        entity.getBlockId(),
        entity.getEducationLevelId(),
        entity.getCurrentEmploymentStatusId(),
        entity.getProfileStatusId(),
        entity.getCreatedAt(),
        entity.getUpdatedAt(),
        entity.getDeletedAt()
    );
  }

  public Trainee toEntity(CreateTraineeRequest request, User user) {
    if (request == null) {
      return null;
    }

    Trainee trainee = new Trainee();
    trainee.setUser(user);
    trainee.setRegistrationNumber(request.getRegistrationNumber());
    trainee.setFirstName(request.getFirstName());
    trainee.setMiddleName(request.getMiddleName());
    trainee.setLastName(request.getLastName());
    trainee.setDateOfBirth(request.getDateOfBirth());
    trainee.setGenderId(request.getGenderId());
    trainee.setEmail(request.getEmail());
    trainee.setPhone(request.getPhone());
    trainee.setAddressLine1(request.getAddressLine1());
    trainee.setAddressLine2(request.getAddressLine2());
    trainee.setPincode(request.getPincode());
    trainee.setLocationId(request.getLocationId());
    trainee.setStateId(request.getStateId());
    trainee.setDistrictId(request.getDistrictId());
    trainee.setBlockId(request.getBlockId());
    trainee.setEducationLevelId(request.getEducationLevelId());
    trainee.setCurrentEmploymentStatusId(request.getCurrentEmploymentStatusId());
    trainee.setProfileStatusId(request.getProfileStatusId());
    return trainee;
  }
}
