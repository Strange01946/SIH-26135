package in.gov.sih.sih26135.service.impl;

import in.gov.sih.sih26135.dto.request.CreateEmployerRequest;
import in.gov.sih.sih26135.dto.request.UpdateEmployerRequest;
import in.gov.sih.sih26135.dto.response.EmployerResponse;
import in.gov.sih.sih26135.entity.Employer;
import in.gov.sih.sih26135.entity.Industry;
import in.gov.sih.sih26135.entity.RefCompanySize;
import in.gov.sih.sih26135.entity.RefRecordVerificationStatus;
import in.gov.sih.sih26135.entity.Sector;
import in.gov.sih.sih26135.exception.BadRequestException;
import in.gov.sih.sih26135.exception.ConflictException;
import in.gov.sih.sih26135.exception.ResourceNotFoundException;
import in.gov.sih.sih26135.mapper.EmployerMapper;
import in.gov.sih.sih26135.repository.EmployerRepository;
import in.gov.sih.sih26135.repository.IndustryRepository;
import in.gov.sih.sih26135.repository.RefCompanySizeRepository;
import in.gov.sih.sih26135.repository.RefRecordVerificationStatusRepository;
import in.gov.sih.sih26135.repository.SectorRepository;
import in.gov.sih.sih26135.service.EmployerService;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class EmployerServiceImpl implements EmployerService {

  private final EmployerRepository employerRepository;
  private final IndustryRepository industryRepository;
  private final SectorRepository sectorRepository;
  private final RefCompanySizeRepository refCompanySizeRepository;
  private final RefRecordVerificationStatusRepository refRecordVerificationStatusRepository;
  private final EmployerMapper employerMapper;

  public EmployerServiceImpl(
      EmployerRepository employerRepository,
      IndustryRepository industryRepository,
      SectorRepository sectorRepository,
      RefCompanySizeRepository refCompanySizeRepository,
      RefRecordVerificationStatusRepository refRecordVerificationStatusRepository,
      EmployerMapper employerMapper) {
    this.employerRepository = employerRepository;
    this.industryRepository = industryRepository;
    this.sectorRepository = sectorRepository;
    this.refCompanySizeRepository = refCompanySizeRepository;
    this.refRecordVerificationStatusRepository = refRecordVerificationStatusRepository;
    this.employerMapper = employerMapper;
  }

  @Override
  public EmployerResponse getEmployerById(Long id) {
    if (id == null) {
      throw new BadRequestException("Employer ID is required");
    }
    Employer employer = employerRepository.findById(id)
        .orElseThrow(() -> new ResourceNotFoundException("Employer", "id"));
    return employerMapper.toResponse(employer);
  }

  @Override
  public EmployerResponse getEmployerByCode(String employerCode) {
    if (employerCode == null || employerCode.isBlank()) {
      throw new BadRequestException("Employer code is required");
    }
    Employer employer = employerRepository.findByEmployerCode(employerCode.trim())
        .orElseThrow(() -> new ResourceNotFoundException("Employer", "employerCode"));
    return employerMapper.toResponse(employer);
  }

  @Override
  public List<EmployerResponse> getAllEmployers(boolean includeDeleted) {
    List<Employer> employers = includeDeleted
        ? employerRepository.findAll()
        : employerRepository.findByDeletedAtIsNull();
    return employers.stream()
        .map(employerMapper::toResponse)
        .toList();
  }

  @Override
  public List<EmployerResponse> getEmployersByDistrict(Long districtId) {
    if (districtId == null) {
      throw new BadRequestException("District ID is required");
    }
    return employerRepository.findByDistrictId(districtId).stream()
        .map(employerMapper::toResponse)
        .toList();
  }

  @Override
  public List<EmployerResponse> getEmployersByIndustry(Long industryId) {
    if (industryId == null) {
      throw new BadRequestException("Industry ID is required");
    }
    return employerRepository.findByIndustryId(industryId).stream()
        .map(employerMapper::toResponse)
        .toList();
  }

  @Override
  public List<EmployerResponse> getEmployersBySector(Long sectorId) {
    if (sectorId == null) {
      throw new BadRequestException("Sector ID is required");
    }
    return employerRepository.findBySectorId(sectorId).stream()
        .map(employerMapper::toResponse)
        .toList();
  }

  @Override
  @Transactional
  public EmployerResponse createEmployer(CreateEmployerRequest request) {
    if (request == null) {
      throw new BadRequestException("Employer creation request cannot be null");
    }
    if (request.getEmployerCode() == null || request.getEmployerCode().isBlank()) {
      throw new BadRequestException("Employer code is required");
    }
    if (request.getEmployerName() == null || request.getEmployerName().isBlank()) {
      throw new BadRequestException("Employer name is required");
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
    if (request.getRecordVerificationStatusId() == null) {
      throw new BadRequestException("Record verification status ID is required");
    }

    String employerCode = request.getEmployerCode().trim();
    if (employerRepository.existsByEmployerCode(employerCode)) {
      throw new ConflictException("Employer code already exists", "EMPLOYER_CODE_ALREADY_EXISTS");
    }

    if (request.getRegistrationNumber() != null && !request.getRegistrationNumber().isBlank()) {
      String regNum = request.getRegistrationNumber().trim();
      if (employerRepository.findByRegistrationNumber(regNum).isPresent()) {
        throw new ConflictException("Registration number already exists", "REGISTRATION_NUMBER_ALREADY_EXISTS");
      }
    }

    if (request.getGstin() != null && !request.getGstin().isBlank()) {
      String gstin = request.getGstin().trim();
      if (gstin.length() < 10 || gstin.length() > 15) {
        throw new BadRequestException("GSTIN must be between 10 and 15 characters", "INVALID_GSTIN");
      }
      if (employerRepository.findByGstin(gstin).isPresent()) {
        throw new ConflictException("GSTIN already exists", "GSTIN_ALREADY_EXISTS");
      }
    }

    if (request.getPincode() != null && !request.getPincode().isBlank()) {
      String pincode = request.getPincode().trim();
      if (!pincode.matches("^[1-9][0-9]{5}$")) {
        throw new BadRequestException("PIN code must be a valid 6-digit Indian PIN code", "INVALID_PINCODE");
      }
    }

    Industry industry = null;
    if (request.getIndustryId() != null) {
      industry = industryRepository.findById(request.getIndustryId())
          .orElseThrow(() -> new ResourceNotFoundException("Industry", "industryId"));
    }

    Sector sector = null;
    if (request.getSectorId() != null) {
      sector = sectorRepository.findById(request.getSectorId())
          .orElseThrow(() -> new ResourceNotFoundException("Sector", "sectorId"));
    }

    RefCompanySize companySize = null;
    if (request.getCompanySizeId() != null) {
      companySize = refCompanySizeRepository.findById(request.getCompanySizeId())
          .orElseThrow(() -> new ResourceNotFoundException("RefCompanySize", "companySizeId"));
    }

    RefRecordVerificationStatus recordVerificationStatus = refRecordVerificationStatusRepository
        .findById(request.getRecordVerificationStatusId())
        .orElseThrow(() -> new ResourceNotFoundException("RefRecordVerificationStatus", "recordVerificationStatusId"));

    Employer entity = employerMapper.toEntity(request, industry, sector, companySize, recordVerificationStatus);
    Employer saved = employerRepository.save(entity);
    return employerMapper.toResponse(saved);
  }

  @Override
  @Transactional
  public EmployerResponse updateEmployer(Long id, UpdateEmployerRequest request) {
    if (id == null) {
      throw new BadRequestException("Employer ID is required");
    }
    if (request == null) {
      throw new BadRequestException("Employer update request cannot be null");
    }

    Employer employer = employerRepository.findById(id)
        .orElseThrow(() -> new ResourceNotFoundException("Employer", "id"));

    if (employer.getDeletedAt() != null) {
      throw new BadRequestException("Cannot update a deleted employer", "EMPLOYER_DELETED");
    }

    if (request.getEmployerName() != null && !request.getEmployerName().isBlank()) {
      employer.setEmployerName(request.getEmployerName().trim());
    }

    if (request.getRegistrationNumber() != null) {
      String regNum = request.getRegistrationNumber().trim();
      if (!regNum.isEmpty()) {
        employerRepository.findByRegistrationNumber(regNum)
            .filter(e -> !e.getId().equals(id))
            .ifPresent(e -> {
              throw new ConflictException("Registration number already in use", "REGISTRATION_NUMBER_ALREADY_EXISTS");
            });
        employer.setRegistrationNumber(regNum);
      } else {
        employer.setRegistrationNumber(null);
      }
    }

    if (request.getGstin() != null) {
      String gstin = request.getGstin().trim();
      if (!gstin.isEmpty()) {
        if (gstin.length() < 10 || gstin.length() > 15) {
          throw new BadRequestException("GSTIN must be between 10 and 15 characters", "INVALID_GSTIN");
        }
        employerRepository.findByGstin(gstin)
            .filter(e -> !e.getId().equals(id))
            .ifPresent(e -> {
              throw new ConflictException("GSTIN already in use", "GSTIN_ALREADY_EXISTS");
            });
        employer.setGstin(gstin);
      } else {
        employer.setGstin(null);
      }
    }

    if (request.getPincode() != null) {
      String pincode = request.getPincode().trim();
      if (!pincode.isEmpty()) {
        if (!pincode.matches("^[1-9][0-9]{5}$")) {
          throw new BadRequestException("PIN code must be a valid 6-digit Indian PIN code", "INVALID_PINCODE");
        }
        employer.setPincode(pincode);
      } else {
        employer.setPincode(null);
      }
    }

    if (request.getOrganizationTypeId() != null) {
      employer.setOrganizationTypeId(request.getOrganizationTypeId());
    }
    if (request.getOrganizationId() != null) {
      employer.setOrganizationId(request.getOrganizationId());
    }

    if (request.getIndustryId() != null) {
      Industry industry = industryRepository.findById(request.getIndustryId())
          .orElseThrow(() -> new ResourceNotFoundException("Industry", "industryId"));
      employer.setIndustry(industry);
    }

    if (request.getSectorId() != null) {
      Sector sector = sectorRepository.findById(request.getSectorId())
          .orElseThrow(() -> new ResourceNotFoundException("Sector", "sectorId"));
      employer.setSector(sector);
    }

    if (request.getCompanySizeId() != null) {
      RefCompanySize companySize = refCompanySizeRepository.findById(request.getCompanySizeId())
          .orElseThrow(() -> new ResourceNotFoundException("RefCompanySize", "companySizeId"));
      employer.setCompanySize(companySize);
    }

    if (request.getContactPersonName() != null) {
      employer.setContactPersonName(request.getContactPersonName());
    }
    if (request.getContactEmail() != null) {
      employer.setContactEmail(request.getContactEmail());
    }
    if (request.getContactPhone() != null) {
      employer.setContactPhone(request.getContactPhone());
    }
    if (request.getAddressLine1() != null) {
      employer.setAddressLine1(request.getAddressLine1());
    }
    if (request.getAddressLine2() != null) {
      employer.setAddressLine2(request.getAddressLine2());
    }
    if (request.getLocationId() != null) {
      employer.setLocationId(request.getLocationId());
    }
    if (request.getStateId() != null) {
      employer.setStateId(request.getStateId());
    }
    if (request.getDistrictId() != null) {
      employer.setDistrictId(request.getDistrictId());
    }

    if (request.getRecordVerificationStatusId() != null) {
      RefRecordVerificationStatus verificationStatus = refRecordVerificationStatusRepository
          .findById(request.getRecordVerificationStatusId())
          .orElseThrow(() -> new ResourceNotFoundException("RefRecordVerificationStatus", "recordVerificationStatusId"));
      employer.setRecordVerificationStatus(verificationStatus);
    }

    if (request.getVerifiedAt() != null) {
      employer.setVerifiedAt(request.getVerifiedAt());
    }
    if (request.getVerifiedByUserId() != null) {
      employer.setVerifiedByUserId(request.getVerifiedByUserId());
    }
    if (request.getLifecycleStatusId() != null) {
      employer.setLifecycleStatusId(request.getLifecycleStatusId());
    }

    Employer saved = employerRepository.save(employer);
    return employerMapper.toResponse(saved);
  }

  @Override
  @Transactional
  public void deleteEmployer(Long id) {
    if (id == null) {
      throw new BadRequestException("Employer ID is required");
    }
    Employer employer = employerRepository.findById(id)
        .orElseThrow(() -> new ResourceNotFoundException("Employer", "id"));
    if (employer.getDeletedAt() == null) {
      employer.setDeletedAt(LocalDateTime.now());
      employerRepository.save(employer);
    }
  }
}
