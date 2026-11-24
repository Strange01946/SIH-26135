package in.gov.sih.sih26135.service.impl;

import in.gov.sih.sih26135.dto.request.UpdateTrainingCenterRequest;
import in.gov.sih.sih26135.dto.response.TrainingCenterResponse;
import in.gov.sih.sih26135.entity.TrainingCenter;
import in.gov.sih.sih26135.exception.BadRequestException;
import in.gov.sih.sih26135.exception.ConflictException;
import in.gov.sih.sih26135.exception.ResourceNotFoundException;
import in.gov.sih.sih26135.mapper.TrainingCenterMapper;
import in.gov.sih.sih26135.repository.TrainingCenterRepository;
import in.gov.sih.sih26135.response.PaginationUtils;
import in.gov.sih.sih26135.security.WorkflowSecurityScopeValidator;
import in.gov.sih.sih26135.service.ActiveGeography;
import in.gov.sih.sih26135.service.TrainingCenterService;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
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
  private final TrainingCenterMapper trainingCenterMapper;
  private final WorkflowSecurityScopeValidator scopeValidator;
  private final JdbcTemplate jdbcTemplate;

  public TrainingCenterServiceImpl(
      TrainingCenterRepository trainingCenterRepository,
      TrainingCenterMapper trainingCenterMapper) {
    this(trainingCenterRepository, trainingCenterMapper, null, null);
  }

  @Autowired
  public TrainingCenterServiceImpl(
      TrainingCenterRepository trainingCenterRepository,
      TrainingCenterMapper trainingCenterMapper,
      WorkflowSecurityScopeValidator scopeValidator,
      JdbcTemplate jdbcTemplate) {
    this.trainingCenterRepository = trainingCenterRepository;
    this.trainingCenterMapper = trainingCenterMapper;
    this.scopeValidator = scopeValidator;
    this.jdbcTemplate = jdbcTemplate;
  }

  @Override
  public List<LifecycleStatus> getLifecycleStatuses() {
    resolveScope();
    return jdbcTemplate.query("""
        SELECT lifecycle_status_id, status_code, status_name FROM ref_lifecycle_status
        WHERE is_active = 1 ORDER BY sort_order, lifecycle_status_id
        """, (rs, rowNum) -> new LifecycleStatus(rs.getLong(1), rs.getString(2), rs.getString(3)));
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
    TrainingCenter center = trainingCenterRepository.findByCenterCodeAndDeletedAtIsNull(centerCode)
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
  public TrainingCenterResponse updateTrainingCenter(Long id, UpdateTrainingCenterRequest request) {
    TrainingTenantScopeSupport.CallerScope scope = resolveScope();
    if (request == null) {
      throw new BadRequestException("Training center update request cannot be null");
    }

    TrainingCenter center = trainingCenterRepository.findByIdForUpdate(id)
        .orElseThrow(() -> new ResourceNotFoundException("TrainingCenter", "id"));
    Long providerId = center.getTrainingProvider() != null ? center.getTrainingProvider().getId() : null;
    TrainingTenantScopeSupport.validateCenterOwnership(
        scope, providerId, center.getStateId(), center.getDistrictId());
    if (center.getDeletedAt() != null) {
      throw new ResourceNotFoundException("TrainingCenter", "id");
    }
    if ("TEMPORARY".equals(center.getVenueType())) {
      throw new ConflictException("Manage temporary locations through their venue booking.", "VENUE_WORKFLOW_REQUIRED");
    }

    Long stateId = request.getStateId() == null ? center.getStateId() : request.getStateId();
    Long districtId = request.getDistrictId() == null ? center.getDistrictId() : request.getDistrictId();
    boolean geographyChanged = !Objects.equals(stateId, center.getStateId()) || !Objects.equals(districtId, center.getDistrictId());
    if ("SHARED".equals(center.getVenueType())) {
      boolean closing = request.getLifecycleStatusId() != null && request.getLifecycleStatusId() != 1L;
      if (geographyChanged || closing || request.hasOperationalCapacity()) {
        requireNoFutureVenueReservations(id);
      }
      if (request.hasOperationalCapacity() && request.getOperationalCapacity() == null) {
        throw new BadRequestException("Enter the seating capacity of the shared center.", "VENUE_CAPACITY_REQUIRED");
      }
    }
    if (geographyChanged || request.getLocationId() != null) {
      TrainingTenantScopeSupport.validateCenterOwnership(scope, providerId, stateId, districtId);
      validateGeography(stateId, districtId, request.getLocationId());
      if (geographyChanged && hasTrainingHistory(id)) {
        throw new ConflictException("This center has training records. Create a center for the new district to preserve history.",
            "CENTER_HAS_TRAINING_HISTORY");
      }
      center.setStateId(stateId);
      if (!Objects.equals(districtId, center.getDistrictId()) || request.getLocationId() != null) {
        center.setLocationId(request.getLocationId());
      }
      center.setDistrictId(districtId);
    }

    if (request.hasOperationalCapacity()) {
      if (request.getOperationalCapacity() != null && request.getOperationalCapacity() <= 0) {
        throw new BadRequestException("Operational capacity must be greater than 0", "INVALID_OPERATIONAL_CAPACITY");
      }
      center.setOperationalCapacity(request.getOperationalCapacity());
    }

    String pincode = request.hasPincode() ? optionalText(request.getPincode()) : center.getPincode();
    if (pincode == null || pincode.isBlank()) {
      throw new BadRequestException("PIN code is required", "PINCODE_REQUIRED");
    }
    if (!pincode.matches(PINCODE_REGEX)) {
      throw new BadRequestException("Invalid pincode format. Must be 6 digits starting with 1-9", "INVALID_PINCODE");
    }
    if (request.hasPincode()) {
      center.setPincode(pincode);
    }

    if (request.getCenterName() != null && !request.getCenterName().isBlank()) {
      center.setCenterName(request.getCenterName().trim());
    }
    if (request.getAddressLine1() != null) {
      center.setAddressLine1(optionalText(request.getAddressLine1()));
    }
    if (request.getAddressLine2() != null) {
      center.setAddressLine2(optionalText(request.getAddressLine2()));
    }
    if (request.getLifecycleStatusId() != null && !request.getLifecycleStatusId().equals(center.getLifecycleStatusId())) {
      validateLifecycleStatus(request.getLifecycleStatusId());
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
    TrainingCenter center = trainingCenterRepository.findByIdForUpdate(id)
        .orElseThrow(() -> new ResourceNotFoundException("TrainingCenter", "id"));
    TrainingTenantScopeSupport.validateCenterOwnership(
        scope,
        center.getTrainingProvider() != null ? center.getTrainingProvider().getId() : null,
        center.getStateId(),
        center.getDistrictId());
    if (center.getDeletedAt() != null) {
      return;
    }
    if ("TEMPORARY".equals(center.getVenueType())) {
      throw new ConflictException("Manage temporary locations through their venue booking.", "VENUE_WORKFLOW_REQUIRED");
    }
    if ("SHARED".equals(center.getVenueType())) {
      requireNoFutureVenueReservations(id);
    }
    if (hasTrainingHistory(id)) {
      throw new ConflictException("This center has training records. Set its status to Inactive to retain those records.",
          "CENTER_HAS_TRAINING_HISTORY");
    }

    LocalDateTime now = LocalDateTime.now();
    center.setDeletedAt(now);
    center.setUpdatedAt(now);
    trainingCenterRepository.save(center);
  }

  private void validateGeography(Long stateId, Long districtId, Long locationId) {
    ActiveGeography.validate(jdbcTemplate, stateId, districtId);
    if (locationId != null && !Boolean.TRUE.equals(jdbcTemplate.queryForObject("""
        SELECT EXISTS(SELECT 1 FROM locations WHERE location_id = ? AND district_id = ? AND deleted_at IS NULL)
        """, Boolean.class, locationId, districtId))) {
      throw new BadRequestException("The location must belong to the selected district", "INVALID_CENTER_LOCATION");
    }
  }

  private void validateLifecycleStatus(Long statusId) {
    if (!Boolean.TRUE.equals(jdbcTemplate.queryForObject("""
        SELECT EXISTS(SELECT 1 FROM ref_lifecycle_status WHERE lifecycle_status_id = ? AND is_active = 1)
        """, Boolean.class, statusId))) {
      throw new BadRequestException("Select an available center status", "INVALID_CENTER_STATUS");
    }
  }

  private boolean hasTrainingHistory(Long centerId) {
    return !jdbcTemplate.queryForList("SELECT batch_id FROM training_batches WHERE center_id = ? LIMIT 1 FOR SHARE",
        Long.class, centerId).isEmpty()
        || !jdbcTemplate.queryForList("SELECT enrollment_id FROM training_enrollments WHERE center_id = ? LIMIT 1 FOR SHARE",
            Long.class, centerId).isEmpty();
  }

  private void requireNoFutureVenueReservations(Long centerId) {
    if (!jdbcTemplate.queryForList("""
        SELECT s.session_id FROM venue_booking_sessions s JOIN venue_bookings b USING (booking_id)
        WHERE b.center_id=? AND s.reserved_at IS NOT NULL AND s.released_at IS NULL AND s.ends_at>?
        LIMIT 1 FOR UPDATE
        """, Long.class, centerId, LocalDateTime.now(java.time.ZoneId.of("Asia/Kolkata"))).isEmpty()) {
      throw new ConflictException("This center has upcoming reservations. Resolve them before changing capacity, location or availability.",
          "VENUE_HAS_RESERVATIONS");
    }
  }

  private static String optionalText(String value) {
    return value == null || value.isBlank() ? null : value.trim();
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
