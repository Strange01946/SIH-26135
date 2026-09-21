package in.gov.sih.sih26135.service.impl;

import in.gov.sih.sih26135.dto.request.CreateSchemeRequest;
import in.gov.sih.sih26135.dto.request.UpdateSchemeRequest;
import in.gov.sih.sih26135.dto.response.SchemeResponse;
import in.gov.sih.sih26135.entity.Scheme;
import in.gov.sih.sih26135.exception.BadRequestException;
import in.gov.sih.sih26135.exception.ConflictException;
import in.gov.sih.sih26135.exception.ResourceNotFoundException;
import in.gov.sih.sih26135.mapper.SchemeMapper;
import in.gov.sih.sih26135.repository.SchemeRepository;
import in.gov.sih.sih26135.service.SchemeService;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class SchemeServiceImpl implements SchemeService {

  private final SchemeRepository schemeRepository;
  private final SchemeMapper schemeMapper;

  public SchemeServiceImpl(SchemeRepository schemeRepository, SchemeMapper schemeMapper) {
    this.schemeRepository = schemeRepository;
    this.schemeMapper = schemeMapper;
  }

  @Override
  public SchemeResponse getById(Long id) {
    Scheme scheme = schemeRepository.findById(id)
        .orElseThrow(() -> new ResourceNotFoundException("Scheme", "id"));
    return schemeMapper.toResponse(scheme);
  }

  @Override
  public SchemeResponse getByCode(String schemeCode) {
    Scheme scheme = schemeRepository.findBySchemeCode(schemeCode)
        .orElseThrow(() -> new ResourceNotFoundException("Scheme", "schemeCode"));
    return schemeMapper.toResponse(scheme);
  }

  @Override
  public List<SchemeResponse> getAllSchemes() {
    return schemeRepository.findAll().stream()
        .map(schemeMapper::toResponse)
        .toList();
  }

  @Override
  public List<SchemeResponse> getSchemesByDepartmentId(Long departmentId) {
    return schemeRepository.findByDepartmentId(departmentId).stream()
        .map(schemeMapper::toResponse)
        .toList();
  }

  @Override
  @Transactional
  public SchemeResponse createScheme(CreateSchemeRequest request) {
    if (request == null) {
      throw new BadRequestException("Scheme creation request cannot be null");
    }
    if (request.getSchemeCode() == null || request.getSchemeCode().isBlank()) {
      throw new BadRequestException("Scheme code is required");
    }
    if (request.getSchemeName() == null || request.getSchemeName().isBlank()) {
      throw new BadRequestException("Scheme name is required");
    }
    if (request.getDepartmentId() == null) {
      throw new BadRequestException("Department ID is required");
    }
    if (request.getStartDate() == null) {
      throw new BadRequestException("Start date is required");
    }
    if (request.getLifecycleStatusId() == null) {
      throw new BadRequestException("Lifecycle status ID is required");
    }

    String schemeCode = request.getSchemeCode().trim();
    if (schemeRepository.existsBySchemeCode(schemeCode)) {
      throw new ConflictException("Scheme code already exists", "SCHEME_CODE_ALREADY_EXISTS");
    }

    if (request.getBudget() != null && request.getBudget().compareTo(BigDecimal.ZERO) < 0) {
      throw new BadRequestException("Budget must be greater than or equal to 0", "INVALID_BUDGET");
    }

    if (request.getEndDate() != null && request.getEndDate().isBefore(request.getStartDate())) {
      throw new BadRequestException("End date cannot be before start date", "INVALID_DATE_RANGE");
    }

    Scheme scheme = schemeMapper.toEntity(request);
    scheme.setSchemeCode(schemeCode);
    scheme.setSchemeName(request.getSchemeName().trim());
    if (request.getDescription() != null) {
      scheme.setDescription(request.getDescription().trim());
    }

    LocalDateTime now = LocalDateTime.now();
    scheme.setCreatedAt(now);
    scheme.setUpdatedAt(now);

    Scheme saved = schemeRepository.save(scheme);
    return schemeMapper.toResponse(saved);
  }

  @Override
  @Transactional
  public SchemeResponse updateScheme(Long id, UpdateSchemeRequest request) {
    if (request == null) {
      throw new BadRequestException("Scheme update request cannot be null");
    }

    Scheme scheme = schemeRepository.findById(id)
        .orElseThrow(() -> new ResourceNotFoundException("Scheme", "id"));

    LocalDate prospectiveStartDate = request.getStartDate() != null ? request.getStartDate() : scheme.getStartDate();
    LocalDate prospectiveEndDate = request.getEndDate() != null ? request.getEndDate() : scheme.getEndDate();
    if (prospectiveStartDate != null && prospectiveEndDate != null && prospectiveEndDate.isBefore(prospectiveStartDate)) {
      throw new BadRequestException("End date cannot be before start date", "INVALID_DATE_RANGE");
    }

    if (request.getBudget() != null && request.getBudget().compareTo(BigDecimal.ZERO) < 0) {
      throw new BadRequestException("Budget must be greater than or equal to 0", "INVALID_BUDGET");
    }

    if (request.getSchemeName() != null && !request.getSchemeName().isBlank()) {
      scheme.setSchemeName(request.getSchemeName().trim());
    }
    if (request.getDescription() != null) {
      scheme.setDescription(request.getDescription().trim());
    }
    if (request.getDepartmentId() != null) {
      scheme.setDepartmentId(request.getDepartmentId());
    }
    if (request.getStartDate() != null) {
      scheme.setStartDate(request.getStartDate());
    }
    if (request.getEndDate() != null) {
      scheme.setEndDate(request.getEndDate());
    }
    if (request.getBudget() != null) {
      scheme.setBudget(request.getBudget());
    }
    if (request.getCurrencyCode() != null && !request.getCurrencyCode().isBlank()) {
      scheme.setCurrencyCode(request.getCurrencyCode().trim());
    }
    if (request.getTargetBeneficiaries() != null) {
      scheme.setTargetBeneficiaries(request.getTargetBeneficiaries());
    }
    if (request.getLifecycleStatusId() != null) {
      scheme.setLifecycleStatusId(request.getLifecycleStatusId());
    }

    scheme.setUpdatedAt(LocalDateTime.now());
    Scheme saved = schemeRepository.save(scheme);
    return schemeMapper.toResponse(saved);
  }

  @Override
  @Transactional
  public void deleteScheme(Long id) {
    Scheme scheme = schemeRepository.findById(id)
        .orElseThrow(() -> new ResourceNotFoundException("Scheme", "id"));

    LocalDateTime now = LocalDateTime.now();
    scheme.setDeletedAt(now);
    scheme.setUpdatedAt(now);
    schemeRepository.save(scheme);
  }
}
