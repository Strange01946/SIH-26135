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
import in.gov.sih.sih26135.security.WorkflowSecurityScopeValidator;
import in.gov.sih.sih26135.service.TraineeService;
import in.gov.sih.sih26135.service.AuditLogService;
import java.time.LocalDateTime;
import java.util.List;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class TraineeServiceImpl implements TraineeService {

  private static final String PINCODE_REGEX = "^[1-9][0-9]{5}$";

  private final TraineeRepository traineeRepository;
  private final UserRepository userRepository;
  private final TraineeMapper traineeMapper;
  private final WorkflowSecurityScopeValidator securityScopeValidator;
  private final AuditLogService auditLogs;

  public TraineeServiceImpl(
      TraineeRepository traineeRepository,
      UserRepository userRepository,
      TraineeMapper traineeMapper) {
    this(traineeRepository, userRepository, traineeMapper, null, null);
  }

  public TraineeServiceImpl(
      TraineeRepository traineeRepository,
      UserRepository userRepository,
      TraineeMapper traineeMapper,
      WorkflowSecurityScopeValidator securityScopeValidator) {
    this(traineeRepository, userRepository, traineeMapper, securityScopeValidator, null);
  }

  @Autowired
  public TraineeServiceImpl(
      TraineeRepository traineeRepository,
      UserRepository userRepository,
      TraineeMapper traineeMapper,
      WorkflowSecurityScopeValidator securityScopeValidator,
      AuditLogService auditLogs) {
    this.traineeRepository = traineeRepository;
    this.userRepository = userRepository;
    this.traineeMapper = traineeMapper;
    this.securityScopeValidator = securityScopeValidator;
    this.auditLogs = auditLogs;
  }

  @Override
  public TraineeResponse getById(Long id) {
    if (securityScopeValidator != null) {
      securityScopeValidator.validateTraineeScope(id);
    }
    Trainee trainee = traineeRepository.findById(id)
        .orElseThrow(() -> new ResourceNotFoundException("Trainee", "id"));
    return traineeMapper.toResponse(trainee);
  }

  @Override
  public TraineeResponse getByRegistrationNumber(String registrationNumber) {
    Trainee trainee = traineeRepository.findByRegistrationNumber(registrationNumber)
        .orElseThrow(() -> new ResourceNotFoundException("Trainee", "registrationNumber"));
    if (securityScopeValidator != null) {
      securityScopeValidator.validateTraineeScope(trainee.getId());
    }
    return traineeMapper.toResponse(trainee);
  }

  @Override
  public TraineeResponse getByUserId(Long userId) {
    Trainee trainee = traineeRepository.findByUserId(userId)
        .orElseThrow(() -> new ResourceNotFoundException("Trainee", "userId"));
    if (securityScopeValidator != null) {
      securityScopeValidator.validateTraineeScope(trainee.getId());
    }
    return traineeMapper.toResponse(trainee);
  }

  @Override
  public List<TraineeResponse> getAllTrainees() {
    return traineeRepository.findAll().stream()
        .map(traineeMapper::toResponse)
        .toList();
  }

  @Override
  public Page<TraineeResponse> getAllTrainees(Pageable pageable) {
    return traineeRepository.findByDeletedAtIsNull(pageable)
        .map(traineeMapper::toResponse);
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
      throw new ConflictException("Trainee registration number already exists", "DUPLICATE_TRAINEE_REGISTRATION");
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
    auditTrainee("CREATE", "Candidate profile created", saved.getId(), null, candidateState(saved), List.of());
    return traineeMapper.toResponse(saved);
  }

  @Override
  @Transactional
  public TraineeResponse updateTrainee(Long id, UpdateTraineeRequest request, Long actorUserId) {
    if (request == null) {
      throw new BadRequestException("Trainee update request cannot be null");
    }
    if (securityScopeValidator != null) {
      securityScopeValidator.validateTraineeScope(id);
    }

    Trainee trainee = traineeForChange(id);
    Map<String, Object> before = candidateState(trainee);
    Map<String, Object> privateBefore = privateFields(trainee);

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
    Map<String, Object> privateAfter = privateFields(saved);
    List<String> redacted = privateBefore.keySet().stream()
        .filter(field -> !Objects.equals(privateBefore.get(field), privateAfter.get(field))).toList();
    Map<String, Object> after = candidateState(saved);
    boolean transitioned = !Objects.equals(before.get("profileStatusId"), after.get("profileStatusId"))
        || !Objects.equals(before.get("currentEmploymentStatusId"), after.get("currentEmploymentStatusId"));
    auditTrainee(transitioned ? "STATUS_CHANGE" : "UPDATE", "Candidate profile updated", id, before, after, redacted);
    return traineeMapper.toResponse(saved);
  }

  @Override
  @Transactional
  public void deleteTrainee(Long id, Long actorUserId) {
    if (securityScopeValidator != null) {
      securityScopeValidator.validateTraineeScope(id);
    }
    Trainee trainee = traineeForChange(id);
    boolean wasActive = trainee.getDeletedAt() == null;

    LocalDateTime now = LocalDateTime.now();
    trainee.setDeletedAt(now);
    trainee.setUpdatedAt(now);
    traineeRepository.save(trainee);
    if (wasActive) {
      auditTrainee("SOFT_DELETE", "Candidate profile removed", id, candidateState(trainee), null, List.of());
    }
  }

  private Trainee traineeForChange(Long id) {
    var trainee = auditLogs == null ? traineeRepository.findById(id) : traineeRepository.findLockedById(id);
    return trainee.orElseThrow(() -> new ResourceNotFoundException("Trainee", "id"));
  }

  private Map<String, Object> candidateState(Trainee trainee) {
    Map<String, Object> state = new LinkedHashMap<>();
    state.put("profileStatusId", trainee.getProfileStatusId());
    state.put("currentEmploymentStatusId", trainee.getCurrentEmploymentStatusId());
    state.put("educationLevelId", trainee.getEducationLevelId());
    return state;
  }

  private Map<String, Object> privateFields(Trainee trainee) {
    Map<String, Object> fields = new LinkedHashMap<>();
    fields.put("firstName", trainee.getFirstName());
    fields.put("middleName", trainee.getMiddleName());
    fields.put("lastName", trainee.getLastName());
    fields.put("dateOfBirth", trainee.getDateOfBirth());
    fields.put("genderId", trainee.getGenderId());
    fields.put("email", trainee.getEmail());
    fields.put("phone", trainee.getPhone());
    fields.put("addressLine1", trainee.getAddressLine1());
    fields.put("addressLine2", trainee.getAddressLine2());
    fields.put("pincode", trainee.getPincode());
    fields.put("locationId", trainee.getLocationId());
    fields.put("stateId", trainee.getStateId());
    fields.put("districtId", trainee.getDistrictId());
    fields.put("blockId", trainee.getBlockId());
    return fields;
  }

  private void auditTrainee(String action, String summary, Long id, Map<String, ?> before, Map<String, ?> after,
      List<String> redactedFields) {
    if (auditLogs != null) {
      auditLogs.recordStateChange(action, "Trainee", id, summary, before, after, redactedFields);
    }
  }
}
