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
import in.gov.sih.sih26135.response.PaginationUtils;
import in.gov.sih.sih26135.security.WorkflowSecurityScopeValidator;
import in.gov.sih.sih26135.service.TrainingCenterService;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class TrainingCenterServiceImpl implements TrainingCenterService {

  private static final String PINCODE_REGEX = "^[1-9][0-9]{5}$";

  private final TrainingCenterRepository trainingCenterRepository;
  private final TrainingProviderRepository trainingProviderRepository;
  private final TrainingCenterMapper trainingCenterMapper;
  private final WorkflowSecurityScopeValidator scopeValidator;
  private final JdbcTemplate jdbcTemplate;

  public TrainingCenterServiceImpl(
      TrainingCenterRepository trainingCenterRepository,
      TrainingProviderRepository trainingProviderRepository,
      TrainingCenterMapper trainingCenterMapper) {
    this(trainingCenterRepository, trainingProviderRepository, trainingCenterMapper, null, null);
  }

  @Autowired
  public TrainingCenterServiceImpl(
      TrainingCenterRepository trainingCenterRepository,
      TrainingProviderRepository trainingProviderRepository,
      TrainingCenterMapper trainingCenterMapper,
      WorkflowSecurityScopeValidator scopeValidator,
      JdbcTemplate jdbcTemplate) {
    this.trainingCenterRepository = trainingCenterRepository;
    this.trainingProviderRepository = trainingProviderRepository;
    this.trainingCenterMapper = trainingCenterMapper;
    this.scopeValidator = scopeValidator;
    this.jdbcTemplate = jdbcTemplate;
  }

  @Override
  public TrainingCenterResponse getById(Long id) {
    TrainingTenantScopeSupport.CallerScope scope = resolveScope();
    TrainingCenter center = trainingCenterRepository.findById(id)
        .orElseThrow(() -> new ResourceNotFoundException("TrainingCenter", "id"));
    TrainingTenantScopeSupport.validateCenterOwnership(
        scope,
        center.getTrainingProvider() != null ? center.getTrainingProvider().getId() : null,
        center.getStateId(),
        center.getDistrictId());
    return trainingCenterMapper.toResponse(center);
  }

  @Override
  public TrainingCenterResponse getByCode(String centerCode) {
    TrainingTenantScopeSupport.CallerScope scope = resolveScope();
    TrainingCenter center = trainingCenterRepository.findByCenterCode(centerCode)
        .orElseThrow(() -> new ResourceNotFoundException("TrainingCenter", "centerCode"));
    TrainingTenantScopeSupport.validateCenterOwnership(
        scope,
        center.getTrainingProvider() != null ? center.getTrainingProvider().getId() : null,
        center.getStateId(),
        center.getDistrictId());
    return trainingCenterMapper.toResponse(center);
  }

  @Override
  public List<TrainingCenterResponse> getAllTrainingCenters() {
    TrainingTenantScopeSupport.CallerScope scope = resolveScope();
    if (scope != null && scope.isProviderScoped()) {
      return trainingCenterRepository.findByTrainingProviderId(scope.trainingProviderId()).stream()
          .map(trainingCenterMapper::toResponse)
          .toList();
    }
    if (scope != null && scope.isStateScoped()) {
      return (scope.districtId() != null
          ? trainingCenterRepository.findByDistrictId(scope.districtId())
          : trainingCenterRepository.findByStateId(scope.stateId()))
          .stream().map(trainingCenterMapper::toResponse).toList();
    }
    return trainingCenterRepository.findAll().stream()
        .map(trainingCenterMapper::toResponse)
        .toList();
  }

  @Override
  public List<TrainingCenterResponse> getTrainingCentersByProviderId(Long providerId) {
    TrainingTenantScopeSupport.CallerScope scope = resolveScope();
    validateProviderQueryAllowed(scope, providerId);
    return trainingCenterRepository.findByTrainingProviderId(providerId).stream()
        .filter(c -> inStateDistrictScope(scope, c))
        .map(trainingCenterMapper::toResponse)
        .toList();
  }

  @Override
  public List<TrainingCenterResponse> getTrainingCentersByDistrictId(Long districtId) {
    TrainingTenantScopeSupport.CallerScope scope = resolveScope();
    validateDistrictQueryAllowed(scope, districtId);
    if (scope != null && scope.isProviderScoped()) {
      return trainingCenterRepository.findByTrainingProviderId(scope.trainingProviderId()).stream()
          .filter(c -> districtId != null && districtId.equals(c.getDistrictId()))
          .map(trainingCenterMapper::toResponse)
          .toList();
    }
    return trainingCenterRepository.findByDistrictId(districtId).stream()
        .map(trainingCenterMapper::toResponse)
        .toList();
  }

  @Override
  public List<TrainingCenterResponse> getTrainingCentersByStateId(Long stateId) {
    TrainingTenantScopeSupport.CallerScope scope = resolveScope();
    validateStateQueryAllowed(scope, stateId);
    if (scope != null && scope.isProviderScoped()) {
      return trainingCenterRepository.findByTrainingProviderId(scope.trainingProviderId()).stream()
          .filter(c -> stateId != null && stateId.equals(c.getStateId()))
          .map(trainingCenterMapper::toResponse)
          .toList();
    }
    if (scope != null && scope.isStateScoped() && scope.districtId() != null) {
      return trainingCenterRepository.findByDistrictId(scope.districtId()).stream()
          .map(trainingCenterMapper::toResponse)
          .toList();
    }
    return trainingCenterRepository.findByStateId(stateId).stream()
        .map(trainingCenterMapper::toResponse)
        .toList();
  }

  @Override
  public Page<TrainingCenterResponse> getAllTrainingCenters(Pageable pageable) {
    TrainingTenantScopeSupport.CallerScope scope = resolveScope();
    if (scope == null || scope.nationwideUnrestricted()) {
      return PaginationUtils.executeSafeJpaPage(pageable, trainingCenterRepository::findByDeletedAtIsNull)
          .map(trainingCenterMapper::toResponse);
    }
    if (scope.isProviderScoped()) {
      return PaginationUtils.executeSafeJpaPage(
              pageable, p -> trainingCenterRepository.findByTrainingProviderIdAndDeletedAtIsNull(
                  scope.trainingProviderId(), p))
          .map(trainingCenterMapper::toResponse);
    }
    if (scope.districtId() != null) {
      return PaginationUtils.executeSafeJpaPage(
              pageable, p -> trainingCenterRepository.findByDistrictIdAndDeletedAtIsNull(
                  scope.districtId(), p))
          .map(trainingCenterMapper::toResponse);
    }
    return PaginationUtils.executeSafeJpaPage(
            pageable, p -> trainingCenterRepository.findByStateIdAndDeletedAtIsNull(scope.stateId(), p))
        .map(trainingCenterMapper::toResponse);
  }

  @Override
  public Page<TrainingCenterResponse> getTrainingCentersByProviderId(Long providerId, Pageable pageable) {
    TrainingTenantScopeSupport.CallerScope scope = resolveScope();
    validateProviderQueryAllowed(scope, providerId);
    if (scope == null || scope.nationwideUnrestricted() || scope.isProviderScoped()) {
      return PaginationUtils.executeSafeJpaPage(
              pageable, p -> trainingCenterRepository.findByTrainingProviderIdAndDeletedAtIsNull(providerId, p))
          .map(trainingCenterMapper::toResponse);
    }
    List<Object> params = new ArrayList<>();
    params.add(providerId);
    return queryScopedCentersPage("tc.provider_id = ?", params, scope, pageable);
  }

  @Override
  public Page<TrainingCenterResponse> getTrainingCentersByDistrictId(Long districtId, Pageable pageable) {
    TrainingTenantScopeSupport.CallerScope scope = resolveScope();
    validateDistrictQueryAllowed(scope, districtId);
    if (scope == null || scope.nationwideUnrestricted() || (scope.isStateScoped() && (scope.districtId() == null || scope.districtId().equals(districtId)))) {
      return PaginationUtils.executeSafeJpaPage(
              pageable, p -> trainingCenterRepository.findByDistrictIdAndDeletedAtIsNull(districtId, p))
          .map(trainingCenterMapper::toResponse);
    }
    List<Object> params = new ArrayList<>();
    params.add(districtId);
    return queryScopedCentersPage("tc.district_id = ?", params, scope, pageable);
  }

  @Override
  public Page<TrainingCenterResponse> getTrainingCentersByStateId(Long stateId, Pageable pageable) {
    TrainingTenantScopeSupport.CallerScope scope = resolveScope();
    validateStateQueryAllowed(scope, stateId);
    if (scope == null || scope.nationwideUnrestricted() || (scope.isStateScoped() && scope.districtId() == null)) {
      return PaginationUtils.executeSafeJpaPage(
              pageable, p -> trainingCenterRepository.findByStateIdAndDeletedAtIsNull(stateId, p))
          .map(trainingCenterMapper::toResponse);
    }
    List<Object> params = new ArrayList<>();
    params.add(stateId);
    return queryScopedCentersPage("tc.state_id = ?", params, scope, pageable);
  }

  @Override
  @Transactional
  public TrainingCenterResponse createTrainingCenter(CreateTrainingCenterRequest request) {
    TrainingTenantScopeSupport.CallerScope scope = resolveScope();
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
    TrainingTenantScopeSupport.validateCenterOwnership(
        scope, request.getProviderId(), request.getStateId(), request.getDistrictId());

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
    TrainingTenantScopeSupport.CallerScope scope = resolveScope();
    if (request == null) {
      throw new BadRequestException("Training center update request cannot be null");
    }

    TrainingCenter center = trainingCenterRepository.findById(id)
        .orElseThrow(() -> new ResourceNotFoundException("TrainingCenter", "id"));
    TrainingTenantScopeSupport.validateCenterOwnership(
        scope,
        center.getTrainingProvider() != null ? center.getTrainingProvider().getId() : null,
        center.getStateId(),
        center.getDistrictId());

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
    TrainingTenantScopeSupport.CallerScope scope = resolveScope();
    TrainingCenter center = trainingCenterRepository.findById(id)
        .orElseThrow(() -> new ResourceNotFoundException("TrainingCenter", "id"));
    TrainingTenantScopeSupport.validateCenterOwnership(
        scope,
        center.getTrainingProvider() != null ? center.getTrainingProvider().getId() : null,
        center.getStateId(),
        center.getDistrictId());

    LocalDateTime now = LocalDateTime.now();
    center.setDeletedAt(now);
    center.setUpdatedAt(now);
    trainingCenterRepository.save(center);
  }

  private TrainingTenantScopeSupport.CallerScope resolveScope() {
    if (scopeValidator == null) {
      return null;
    }
    return TrainingTenantScopeSupport.resolveCallerScope(scopeValidator, jdbcTemplate);
  }

  private void validateProviderQueryAllowed(TrainingTenantScopeSupport.CallerScope scope, Long providerId) {
    if (scope == null || scope.nationwideUnrestricted()) {
      return;
    }
    if (scope.isProviderScoped() && !scope.trainingProviderId().equals(providerId)) {
      throw new AccessDeniedException("Training provider cannot access another provider's centers");
    }
  }

  private void validateStateQueryAllowed(TrainingTenantScopeSupport.CallerScope scope, Long stateId) {
    if (scope == null || scope.nationwideUnrestricted()) {
      return;
    }
    if (scope.isStateScoped() && !scope.stateId().equals(stateId)) {
      throw new AccessDeniedException("Caller cannot access training centers outside assigned state");
    }
  }

  private void validateDistrictQueryAllowed(TrainingTenantScopeSupport.CallerScope scope, Long districtId) {
    if (scope == null || scope.nationwideUnrestricted()) {
      return;
    }
    if (scope.isStateScoped()) {
      if (scope.districtId() != null && !scope.districtId().equals(districtId)) {
        throw new AccessDeniedException("District caller cannot access training centers outside assigned district");
      }
      Long parentState = scopeValidator.findDistrictStateId(districtId);
      if (parentState != null && !scope.stateId().equals(parentState)) {
        throw new AccessDeniedException("Requested district belongs to another state");
      }
    }
  }

  private boolean inStateDistrictScope(TrainingTenantScopeSupport.CallerScope scope, TrainingCenter center) {
    if (scope == null || scope.nationwideUnrestricted() || !scope.isStateScoped()) {
      return true;
    }
    if (!scope.stateId().equals(center.getStateId())) {
      return false;
    }
    return scope.districtId() == null || scope.districtId().equals(center.getDistrictId());
  }

  private Page<TrainingCenterResponse> queryScopedCentersPage(
      String basePredicate,
      List<Object> baseParams,
      TrainingTenantScopeSupport.CallerScope scope,
      Pageable pageable) {
    StringBuilder where = new StringBuilder(" FROM training_centers tc WHERE tc.deleted_at IS NULL AND ")
        .append(basePredicate);
    List<Object> params = new ArrayList<>(baseParams);
    if (scope.isProviderScoped()) {
      where.append(" AND tc.provider_id = ?");
      params.add(scope.trainingProviderId());
    } else if (scope.isStateScoped()) {
      where.append(" AND tc.state_id = ?");
      params.add(scope.stateId());
      if (scope.districtId() != null) {
        where.append(" AND tc.district_id = ?");
        params.add(scope.districtId());
      }
    }

    Long totalObj = jdbcTemplate.queryForObject("SELECT COUNT(*)" + where, Long.class, params.toArray());
    long total = totalObj != null ? totalObj : 0L;
    if (total == 0L || pageable.getOffset() >= total) {
      return new PageImpl<>(List.of(), pageable, total);
    }

    String orderBy = buildCenterOrderBy(pageable.getSort());
    List<Object> pageParams = new ArrayList<>(params);
    pageParams.add(pageable.getPageSize());
    pageParams.add(pageable.getOffset());

    List<Long> pageIds = jdbcTemplate.queryForList(
        "SELECT tc.center_id" + where + orderBy + " LIMIT ? OFFSET ?",
        Long.class, pageParams.toArray());
    if (pageIds.isEmpty()) {
      return new PageImpl<>(List.of(), pageable, total);
    }

    Map<Long, TrainingCenterResponse> mapped = new HashMap<>();
    for (TrainingCenter c : trainingCenterRepository.findAllById(pageIds)) {
      mapped.put(c.getId(), trainingCenterMapper.toResponse(c));
    }
    List<TrainingCenterResponse> ordered = new ArrayList<>(pageIds.size());
    for (Long cid : pageIds) {
      TrainingCenterResponse resp = mapped.get(cid);
      if (resp != null) {
        ordered.add(resp);
      }
    }
    return new PageImpl<>(ordered, pageable, total);
  }

  private String buildCenterOrderBy(Sort sort) {
    if (sort == null || sort.isUnsorted()) {
      return " ORDER BY tc.center_id ASC";
    }
    List<String> clauses = new ArrayList<>();
    for (Sort.Order o : sort) {
      String col = switch (o.getProperty()) {
        case "centerCode" -> "tc.center_code";
        case "centerName" -> "tc.center_name";
        case "operationalCapacity" -> "tc.operational_capacity";
        case "createdAt" -> "tc.created_at";
        case "updatedAt" -> "tc.updated_at";
        default -> "tc.center_id";
      };
      clauses.add(col + (o.isDescending() ? " DESC" : " ASC"));
    }
    return clauses.isEmpty() ? " ORDER BY tc.center_id ASC" : " ORDER BY " + String.join(", ", clauses);
  }
}
