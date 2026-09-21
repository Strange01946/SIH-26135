package in.gov.sih.sih26135.service.impl;

import in.gov.sih.sih26135.dto.request.CreateTrainingProviderRequest;
import in.gov.sih.sih26135.dto.request.UpdateTrainingProviderRequest;
import in.gov.sih.sih26135.dto.response.TrainingProviderResponse;
import in.gov.sih.sih26135.entity.RefAccreditationStatus;
import in.gov.sih.sih26135.entity.TrainingProvider;
import in.gov.sih.sih26135.exception.BadRequestException;
import in.gov.sih.sih26135.exception.ConflictException;
import in.gov.sih.sih26135.exception.ResourceNotFoundException;
import in.gov.sih.sih26135.mapper.TrainingProviderMapper;
import in.gov.sih.sih26135.repository.RefAccreditationStatusRepository;
import in.gov.sih.sih26135.repository.TrainingProviderRepository;
import in.gov.sih.sih26135.service.TrainingProviderService;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class TrainingProviderServiceImpl implements TrainingProviderService {

  private static final String PINCODE_REGEX = "^[1-9][0-9]{5}$";
  private static final BigDecimal MAX_RATING = new BigDecimal("5.00");

  private final TrainingProviderRepository trainingProviderRepository;
  private final RefAccreditationStatusRepository refAccreditationStatusRepository;
  private final TrainingProviderMapper trainingProviderMapper;

  public TrainingProviderServiceImpl(
      TrainingProviderRepository trainingProviderRepository,
      RefAccreditationStatusRepository refAccreditationStatusRepository,
      TrainingProviderMapper trainingProviderMapper) {
    this.trainingProviderRepository = trainingProviderRepository;
    this.refAccreditationStatusRepository = refAccreditationStatusRepository;
    this.trainingProviderMapper = trainingProviderMapper;
  }

  @Override
  public TrainingProviderResponse getById(Long id) {
    TrainingProvider provider = trainingProviderRepository.findById(id)
        .orElseThrow(() -> new ResourceNotFoundException("TrainingProvider", "id"));
    return trainingProviderMapper.toResponse(provider);
  }

  @Override
  public TrainingProviderResponse getByCode(String providerCode) {
    TrainingProvider provider = trainingProviderRepository.findByProviderCode(providerCode)
        .orElseThrow(() -> new ResourceNotFoundException("TrainingProvider", "providerCode"));
    return trainingProviderMapper.toResponse(provider);
  }

  @Override
  public TrainingProviderResponse getByRegistrationNumber(String registrationNumber) {
    TrainingProvider provider = trainingProviderRepository.findByRegistrationNumber(registrationNumber)
        .orElseThrow(() -> new ResourceNotFoundException("TrainingProvider", "registrationNumber"));
    return trainingProviderMapper.toResponse(provider);
  }

  @Override
  public List<TrainingProviderResponse> getAllTrainingProviders() {
    return trainingProviderRepository.findAll().stream()
        .map(trainingProviderMapper::toResponse)
        .toList();
  }

  @Override
  public List<TrainingProviderResponse> getTrainingProvidersByStateId(Long stateId) {
    return trainingProviderRepository.findByStateId(stateId).stream()
        .map(trainingProviderMapper::toResponse)
        .toList();
  }

  @Override
  public List<TrainingProviderResponse> getTrainingProvidersByDistrictId(Long districtId) {
    return trainingProviderRepository.findByDistrictId(districtId).stream()
        .map(trainingProviderMapper::toResponse)
        .toList();
  }

  @Override
  @Transactional
  public TrainingProviderResponse createTrainingProvider(CreateTrainingProviderRequest request) {
    if (request == null) {
      throw new BadRequestException("Training provider creation request cannot be null");
    }
    if (request.getProviderCode() == null || request.getProviderCode().isBlank()) {
      throw new BadRequestException("Provider code is required");
    }
    if (request.getProviderName() == null || request.getProviderName().isBlank()) {
      throw new BadRequestException("Provider name is required");
    }
    if (request.getRegistrationNumber() == null || request.getRegistrationNumber().isBlank()) {
      throw new BadRequestException("Registration number is required");
    }
    if (request.getOrganizationTypeId() == null) {
      throw new BadRequestException("Organization type ID is required");
    }
    if (request.getStateId() == null) {
      throw new BadRequestException("State ID is required");
    }
    if (request.getDistrictId() == null) {
      throw new BadRequestException("District ID is required");
    }
    if (request.getAccreditationStatusId() == null) {
      throw new BadRequestException("Accreditation status ID is required");
    }
    if (request.getLifecycleStatusId() == null) {
      throw new BadRequestException("Lifecycle status ID is required");
    }

    String providerCode = request.getProviderCode().trim();
    if (trainingProviderRepository.existsByProviderCode(providerCode)) {
      throw new ConflictException("Provider code already exists", "PROVIDER_CODE_ALREADY_EXISTS");
    }

    String registrationNumber = request.getRegistrationNumber().trim();
    if (trainingProviderRepository.existsByRegistrationNumber(registrationNumber)) {
      throw new ConflictException("Registration number already exists", "REGISTRATION_NUMBER_ALREADY_EXISTS");
    }

    if (request.getRating() != null) {
      if (request.getRating().compareTo(BigDecimal.ZERO) < 0 || request.getRating().compareTo(MAX_RATING) > 0) {
        throw new BadRequestException("Rating must be between 0.00 and 5.00", "INVALID_RATING");
      }
    }

    if (request.getPincode() != null && !request.getPincode().isBlank()) {
      String pincode = request.getPincode().trim();
      if (!pincode.matches(PINCODE_REGEX)) {
        throw new BadRequestException("Invalid pincode format. Must be 6 digits starting with 1-9", "INVALID_PINCODE");
      }
      request.setPincode(pincode);
    }

    RefAccreditationStatus status = refAccreditationStatusRepository.findById(request.getAccreditationStatusId())
        .orElseThrow(() -> new ResourceNotFoundException("RefAccreditationStatus", "accreditationStatusId"));

    TrainingProvider provider = trainingProviderMapper.toEntity(request, status);
    provider.setProviderCode(providerCode);
    provider.setProviderName(request.getProviderName().trim());
    provider.setRegistrationNumber(registrationNumber);
    if (request.getContactPersonName() != null) {
      provider.setContactPersonName(request.getContactPersonName().trim());
    }
    if (request.getContactEmail() != null) {
      provider.setContactEmail(request.getContactEmail().trim());
    }
    if (request.getContactPhone() != null) {
      provider.setContactPhone(request.getContactPhone().trim());
    }
    if (request.getAddressLine1() != null) {
      provider.setAddressLine1(request.getAddressLine1().trim());
    }
    if (request.getAddressLine2() != null) {
      provider.setAddressLine2(request.getAddressLine2().trim());
    }

    LocalDateTime now = LocalDateTime.now();
    provider.setCreatedAt(now);
    provider.setUpdatedAt(now);

    TrainingProvider saved = trainingProviderRepository.save(provider);
    return trainingProviderMapper.toResponse(saved);
  }

  @Override
  @Transactional
  public TrainingProviderResponse updateTrainingProvider(Long id, UpdateTrainingProviderRequest request) {
    if (request == null) {
      throw new BadRequestException("Training provider update request cannot be null");
    }

    TrainingProvider provider = trainingProviderRepository.findById(id)
        .orElseThrow(() -> new ResourceNotFoundException("TrainingProvider", "id"));

    if (request.getRating() != null) {
      if (request.getRating().compareTo(BigDecimal.ZERO) < 0 || request.getRating().compareTo(MAX_RATING) > 0) {
        throw new BadRequestException("Rating must be between 0.00 and 5.00", "INVALID_RATING");
      }
      provider.setRating(request.getRating());
    }

    if (request.getPincode() != null && !request.getPincode().isBlank()) {
      String pincode = request.getPincode().trim();
      if (!pincode.matches(PINCODE_REGEX)) {
        throw new BadRequestException("Invalid pincode format. Must be 6 digits starting with 1-9", "INVALID_PINCODE");
      }
      provider.setPincode(pincode);
    }

    if (request.getAccreditationStatusId() != null) {
      RefAccreditationStatus status = refAccreditationStatusRepository.findById(request.getAccreditationStatusId())
          .orElseThrow(() -> new ResourceNotFoundException("RefAccreditationStatus", "accreditationStatusId"));
      provider.setAccreditationStatus(status);
    }

    if (request.getProviderName() != null && !request.getProviderName().isBlank()) {
      provider.setProviderName(request.getProviderName().trim());
    }
    if (request.getOrganizationTypeId() != null) {
      provider.setOrganizationTypeId(request.getOrganizationTypeId());
    }
    if (request.getOrganizationId() != null) {
      provider.setOrganizationId(request.getOrganizationId());
    }
    if (request.getContactPersonName() != null) {
      provider.setContactPersonName(request.getContactPersonName().trim());
    }
    if (request.getContactEmail() != null) {
      provider.setContactEmail(request.getContactEmail().trim());
    }
    if (request.getContactPhone() != null) {
      provider.setContactPhone(request.getContactPhone().trim());
    }
    if (request.getAddressLine1() != null) {
      provider.setAddressLine1(request.getAddressLine1().trim());
    }
    if (request.getAddressLine2() != null) {
      provider.setAddressLine2(request.getAddressLine2().trim());
    }
    if (request.getLocationId() != null) {
      provider.setLocationId(request.getLocationId());
    }
    if (request.getStateId() != null) {
      provider.setStateId(request.getStateId());
    }
    if (request.getDistrictId() != null) {
      provider.setDistrictId(request.getDistrictId());
    }
    if (request.getLifecycleStatusId() != null) {
      provider.setLifecycleStatusId(request.getLifecycleStatusId());
    }

    provider.setUpdatedAt(LocalDateTime.now());
    TrainingProvider saved = trainingProviderRepository.save(provider);
    return trainingProviderMapper.toResponse(saved);
  }

  @Override
  @Transactional
  public void deleteTrainingProvider(Long id) {
    TrainingProvider provider = trainingProviderRepository.findById(id)
        .orElseThrow(() -> new ResourceNotFoundException("TrainingProvider", "id"));

    LocalDateTime now = LocalDateTime.now();
    provider.setDeletedAt(now);
    provider.setUpdatedAt(now);
    trainingProviderRepository.save(provider);
  }
}
