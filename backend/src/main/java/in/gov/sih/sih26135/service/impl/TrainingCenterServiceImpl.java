package in.gov.sih.sih26135.service.impl;

import in.gov.sih.sih26135.dto.request.CreateTrainingCenterRequest;
import in.gov.sih.sih26135.dto.request.UpdateTrainingCenterRequest;
import in.gov.sih.sih26135.dto.response.TrainingCenterResponse;
import in.gov.sih.sih26135.entity.TrainingCenter;
import in.gov.sih.sih26135.entity.TrainingProvider;
import in.gov.sih.sih26135.exception.BadRequestException;
import in.gov.sih.sih26135.exception.ConflictException;
import in.gov.sih.sih26135.exception.ResourceNotFoundException;
import in.gov.sih.sih26135.mapper.TrainingCenterMapper;
import in.gov.sih.sih26135.repository.TrainingCenterRepository;
import in.gov.sih.sih26135.repository.TrainingProviderRepository;
import in.gov.sih.sih26135.service.TrainingCenterService;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class TrainingCenterServiceImpl implements TrainingCenterService {

  private static final String PINCODE_REGEX = "^[1-9][0-9]{5}$";

  private final TrainingCenterRepository trainingCenterRepository;
  private final TrainingProviderRepository trainingProviderRepository;
  private final TrainingCenterMapper trainingCenterMapper;

  public TrainingCenterServiceImpl(
      TrainingCenterRepository trainingCenterRepository,
      TrainingProviderRepository trainingProviderRepository,
      TrainingCenterMapper trainingCenterMapper) {
    this.trainingCenterRepository = trainingCenterRepository;
    this.trainingProviderRepository = trainingProviderRepository;
    this.trainingCenterMapper = trainingCenterMapper;
  }

  @Override
  public TrainingCenterResponse getById(Long id) {
    TrainingCenter center = trainingCenterRepository.findById(id)
        .orElseThrow(() -> new ResourceNotFoundException("TrainingCenter", "id"));
    return trainingCenterMapper.toResponse(center);
  }

  @Override
  public TrainingCenterResponse getByCode(String centerCode) {
    TrainingCenter center = trainingCenterRepository.findByCenterCode(centerCode)
        .orElseThrow(() -> new ResourceNotFoundException("TrainingCenter", "centerCode"));
    return trainingCenterMapper.toResponse(center);
  }

  @Override
  public List<TrainingCenterResponse> getAllTrainingCenters() {
    return trainingCenterRepository.findAll().stream()
        .map(trainingCenterMapper::toResponse)
        .toList();
  }

  @Override
  public List<TrainingCenterResponse> getTrainingCentersByProviderId(Long providerId) {
    return trainingCenterRepository.findByTrainingProviderId(providerId).stream()
        .map(trainingCenterMapper::toResponse)
        .toList();
  }

  @Override
  public List<TrainingCenterResponse> getTrainingCentersByDistrictId(Long districtId) {
    return trainingCenterRepository.findByDistrictId(districtId).stream()
        .map(trainingCenterMapper::toResponse)
        .toList();
  }

  @Override
  public List<TrainingCenterResponse> getTrainingCentersByStateId(Long stateId) {
    return trainingCenterRepository.findByStateId(stateId).stream()
        .map(trainingCenterMapper::toResponse)
        .toList();
  }

  @Override
  @Transactional
  public TrainingCenterResponse createTrainingCenter(CreateTrainingCenterRequest request) {
    if (request == null) {
      throw new BadRequestException("Training center creation request cannot be null");
    }
    if (request.getCenterCode() == null || request.getCenterCode().isBlank()) {
      throw new BadRequestException("Center code is required");
    }
    if (request.getCenterName() == null || request.getCenterName().isBlank()) {
      throw new BadRequestException("Center name is required");
    }
    if (request.getProviderId() == null) {
      throw new BadRequestException("Provider ID is required");
    }
    if (request.getStateId() == null) {
      throw new BadRequestException("State ID is required");
    }
    if (request.getDistrictId() == null) {
      throw new BadRequestException("District ID is required");
    }
    if (request.getLifecycleStatusId() == null) {
      throw new BadRequestException("Lifecycle status ID is required");
    }

    String centerCode = request.getCenterCode().trim();
    if (trainingCenterRepository.existsByCenterCode(centerCode)) {
      throw new ConflictException("Center code already exists", "CENTER_CODE_ALREADY_EXISTS");
    }

    if (request.getOperationalCapacity() != null && request.getOperationalCapacity() <= 0) {
      throw new BadRequestException("Operational capacity must be greater than 0", "INVALID_OPERATIONAL_CAPACITY");
    }

    if (request.getPincode() != null && !request.getPincode().isBlank()) {
      String pincode = request.getPincode().trim();
      if (!pincode.matches(PINCODE_REGEX)) {
        throw new BadRequestException("Invalid pincode format. Must be 6 digits starting with 1-9", "INVALID_PINCODE");
      }
      request.setPincode(pincode);
    }

    TrainingProvider provider = trainingProviderRepository.findById(request.getProviderId())
        .orElseThrow(() -> new ResourceNotFoundException("TrainingProvider", "providerId"));

    TrainingCenter center = trainingCenterMapper.toEntity(request, provider);
    center.setCenterCode(centerCode);
    center.setCenterName(request.getCenterName().trim());
    if (request.getAddressLine1() != null) {
      center.setAddressLine1(request.getAddressLine1().trim());
    }
    if (request.getAddressLine2() != null) {
      center.setAddressLine2(request.getAddressLine2().trim());
    }

    LocalDateTime now = LocalDateTime.now();
    center.setCreatedAt(now);
    center.setUpdatedAt(now);

    TrainingCenter saved = trainingCenterRepository.save(center);
    return trainingCenterMapper.toResponse(saved);
  }

  @Override
  @Transactional
  public TrainingCenterResponse updateTrainingCenter(Long id, UpdateTrainingCenterRequest request) {
    if (request == null) {
      throw new BadRequestException("Training center update request cannot be null");
    }

    TrainingCenter center = trainingCenterRepository.findById(id)
        .orElseThrow(() -> new ResourceNotFoundException("TrainingCenter", "id"));

    if (request.getOperationalCapacity() != null) {
      if (request.getOperationalCapacity() <= 0) {
        throw new BadRequestException("Operational capacity must be greater than 0", "INVALID_OPERATIONAL_CAPACITY");
      }
      center.setOperationalCapacity(request.getOperationalCapacity());
    }

    if (request.getPincode() != null && !request.getPincode().isBlank()) {
      String pincode = request.getPincode().trim();
      if (!pincode.matches(PINCODE_REGEX)) {
        throw new BadRequestException("Invalid pincode format. Must be 6 digits starting with 1-9", "INVALID_PINCODE");
      }
      center.setPincode(pincode);
    }

    if (request.getCenterName() != null && !request.getCenterName().isBlank()) {
      center.setCenterName(request.getCenterName().trim());
    }
    if (request.getAddressLine1() != null) {
      center.setAddressLine1(request.getAddressLine1().trim());
    }
    if (request.getAddressLine2() != null) {
      center.setAddressLine2(request.getAddressLine2().trim());
    }
    if (request.getLocationId() != null) {
      center.setLocationId(request.getLocationId());
    }
    if (request.getStateId() != null) {
      center.setStateId(request.getStateId());
    }
    if (request.getDistrictId() != null) {
      center.setDistrictId(request.getDistrictId());
    }
    if (request.getLifecycleStatusId() != null) {
      center.setLifecycleStatusId(request.getLifecycleStatusId());
    }

    center.setUpdatedAt(LocalDateTime.now());
    TrainingCenter saved = trainingCenterRepository.save(center);
    return trainingCenterMapper.toResponse(saved);
  }

  @Override
  @Transactional
  public void deleteTrainingCenter(Long id) {
    TrainingCenter center = trainingCenterRepository.findById(id)
        .orElseThrow(() -> new ResourceNotFoundException("TrainingCenter", "id"));

    LocalDateTime now = LocalDateTime.now();
    center.setDeletedAt(now);
    center.setUpdatedAt(now);
    trainingCenterRepository.save(center);
  }
}
