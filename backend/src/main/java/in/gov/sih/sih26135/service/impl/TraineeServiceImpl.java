package in.gov.sih.sih26135.service.impl;

import in.gov.sih.sih26135.dto.request.CreateTraineeRequest;
import in.gov.sih.sih26135.dto.request.UpdateTraineeRequest;
import in.gov.sih.sih26135.dto.response.TraineeResponse;
import in.gov.sih.sih26135.entity.Trainee;
import in.gov.sih.sih26135.entity.User;
import in.gov.sih.sih26135.exception.BadRequestException;
import in.gov.sih.sih26135.exception.ConflictException;
import in.gov.sih.sih26135.exception.ResourceNotFoundException;
import in.gov.sih.sih26135.mapper.TraineeMapper;
import in.gov.sih.sih26135.repository.TraineeRepository;
import in.gov.sih.sih26135.repository.UserRepository;
import in.gov.sih.sih26135.service.TraineeService;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class TraineeServiceImpl implements TraineeService {

  private static final String PINCODE_REGEX = "^[1-9][0-9]{5}$";

  private final TraineeRepository traineeRepository;
  private final UserRepository userRepository;
  private final TraineeMapper traineeMapper;

  public TraineeServiceImpl(
      TraineeRepository traineeRepository,
      UserRepository userRepository,
      TraineeMapper traineeMapper) {
    this.traineeRepository = traineeRepository;
    this.userRepository = userRepository;
    this.traineeMapper = traineeMapper;
  }

  @Override
  public TraineeResponse getById(Long id) {
    Trainee trainee = traineeRepository.findById(id)
        .orElseThrow(() -> new ResourceNotFoundException("Trainee", "id"));
    return traineeMapper.toResponse(trainee);
  }

  @Override
  public TraineeResponse getByRegistrationNumber(String registrationNumber) {
    Trainee trainee = traineeRepository.findByRegistrationNumber(registrationNumber)
        .orElseThrow(() -> new ResourceNotFoundException("Trainee", "registrationNumber"));
    return traineeMapper.toResponse(trainee);
  }

  @Override
  public TraineeResponse getByUserId(Long userId) {
    Trainee trainee = traineeRepository.findByUserId(userId)
        .orElseThrow(() -> new ResourceNotFoundException("Trainee", "userId"));
    return traineeMapper.toResponse(trainee);
  }

  @Override
  public List<TraineeResponse> getAllTrainees() {
    return traineeRepository.findAll().stream()
        .map(traineeMapper::toResponse)
        .toList();
  }

  @Override
  @Transactional
  public TraineeResponse createTrainee(CreateTraineeRequest request, Long actorUserId) {
    if (request == null) {
      throw new BadRequestException("Trainee creation request cannot be null");
    }
    if (request.getRegistrationNumber() == null || request.getRegistrationNumber().isBlank()) {
      throw new BadRequestException("Registration number is required");
    }
    if (request.getFirstName() == null || request.getFirstName().isBlank()) {
      throw new BadRequestException("First name is required");
    }
    if (request.getGenderId() == null) {
      throw new BadRequestException("Gender ID is required");
    }
    if (request.getStateId() == null) {
      throw new BadRequestException("State ID is required");
    }
    if (request.getDistrictId() == null) {
      throw new BadRequestException("District ID is required");
    }
    if (request.getCurrentEmploymentStatusId() == null) {
      throw new BadRequestException("Current employment status ID is required");
    }
    if (request.getProfileStatusId() == null) {
      throw new BadRequestException("Profile status ID is required");
    }

    String regNumber = request.getRegistrationNumber().trim();
    if (traineeRepository.existsByRegistrationNumber(regNumber)) {
      throw new ConflictException("Registration number already exists", "REGISTRATION_NUMBER_ALREADY_EXISTS");
    }

    User user = null;
    if (request.getUserId() != null) {
      user = userRepository.findById(request.getUserId())
          .orElseThrow(() -> new ResourceNotFoundException("User", "userId"));
      if (traineeRepository.existsByUserId(request.getUserId())) {
        throw new ConflictException("Trainee already linked to this user ID", "USER_ALREADY_LINKED");
      }
    }

    if (request.getPincode() != null && !request.getPincode().isBlank()) {
      String pincode = request.getPincode().trim();
      if (!pincode.matches(PINCODE_REGEX)) {
        throw new BadRequestException("Invalid pincode format. Must be 6 digits starting with 1-9", "INVALID_PINCODE");
      }
      request.setPincode(pincode);
    }

    Trainee trainee = traineeMapper.toEntity(request, user);
    trainee.setRegistrationNumber(regNumber);
    trainee.setFirstName(request.getFirstName().trim());
    if (request.getMiddleName() != null) {
      trainee.setMiddleName(request.getMiddleName().trim());
    }
    if (request.getLastName() != null) {
      trainee.setLastName(request.getLastName().trim());
    }
    if (request.getEmail() != null) {
      trainee.setEmail(request.getEmail().trim());
    }
    if (request.getPhone() != null) {
      trainee.setPhone(request.getPhone().trim());
    }

    LocalDateTime now = LocalDateTime.now();
    trainee.setCreatedAt(now);
    trainee.setUpdatedAt(now);

    Trainee saved = traineeRepository.save(trainee);
    return traineeMapper.toResponse(saved);
  }

  @Override
  @Transactional
  public TraineeResponse updateTrainee(Long id, UpdateTraineeRequest request, Long actorUserId) {
    if (request == null) {
      throw new BadRequestException("Trainee update request cannot be null");
    }

    Trainee trainee = traineeRepository.findById(id)
        .orElseThrow(() -> new ResourceNotFoundException("Trainee", "id"));

    if (request.getFirstName() != null && !request.getFirstName().isBlank()) {
      trainee.setFirstName(request.getFirstName().trim());
    }
    if (request.getMiddleName() != null) {
      trainee.setMiddleName(request.getMiddleName().trim());
    }
    if (request.getLastName() != null) {
      trainee.setLastName(request.getLastName().trim());
    }
    if (request.getDateOfBirth() != null) {
      trainee.setDateOfBirth(request.getDateOfBirth());
    }
    if (request.getGenderId() != null) {
      trainee.setGenderId(request.getGenderId());
    }
    if (request.getEmail() != null) {
      trainee.setEmail(request.getEmail().trim());
    }
    if (request.getPhone() != null) {
      trainee.setPhone(request.getPhone().trim());
    }
    if (request.getAddressLine1() != null) {
      trainee.setAddressLine1(request.getAddressLine1());
    }
    if (request.getAddressLine2() != null) {
      trainee.setAddressLine2(request.getAddressLine2());
    }
    if (request.getPincode() != null && !request.getPincode().isBlank()) {
      String pincode = request.getPincode().trim();
      if (!pincode.matches(PINCODE_REGEX)) {
        throw new BadRequestException("Invalid pincode format. Must be 6 digits starting with 1-9", "INVALID_PINCODE");
      }
      trainee.setPincode(pincode);
    }
    if (request.getLocationId() != null) {
      trainee.setLocationId(request.getLocationId());
    }
    if (request.getStateId() != null) {
      trainee.setStateId(request.getStateId());
    }
    if (request.getDistrictId() != null) {
      trainee.setDistrictId(request.getDistrictId());
    }
    if (request.getBlockId() != null) {
      trainee.setBlockId(request.getBlockId());
    }
    if (request.getEducationLevelId() != null) {
      trainee.setEducationLevelId(request.getEducationLevelId());
    }
    if (request.getCurrentEmploymentStatusId() != null) {
      trainee.setCurrentEmploymentStatusId(request.getCurrentEmploymentStatusId());
    }
    if (request.getProfileStatusId() != null) {
      trainee.setProfileStatusId(request.getProfileStatusId());
    }

    trainee.setUpdatedAt(LocalDateTime.now());
    Trainee saved = traineeRepository.save(trainee);
    return traineeMapper.toResponse(saved);
  }

  @Override
  @Transactional
  public void deleteTrainee(Long id, Long actorUserId) {
    Trainee trainee = traineeRepository.findById(id)
        .orElseThrow(() -> new ResourceNotFoundException("Trainee", "id"));

    LocalDateTime now = LocalDateTime.now();
    trainee.setDeletedAt(now);
    trainee.setUpdatedAt(now);
    traineeRepository.save(trainee);
  }
}
