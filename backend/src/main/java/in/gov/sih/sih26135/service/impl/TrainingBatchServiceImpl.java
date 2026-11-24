package in.gov.sih.sih26135.service.impl;

import in.gov.sih.sih26135.dto.request.ChangeBatchStatusRequest;
import in.gov.sih.sih26135.dto.response.TrainingBatchResponse;
import in.gov.sih.sih26135.entity.RefBatchStatus;
import in.gov.sih.sih26135.entity.TrainingBatch;
import in.gov.sih.sih26135.entity.TrainingCenter;
import in.gov.sih.sih26135.exception.BadRequestException;
import in.gov.sih.sih26135.exception.ConflictException;
import in.gov.sih.sih26135.exception.ResourceNotFoundException;
import in.gov.sih.sih26135.mapper.TrainingBatchMapper;
import in.gov.sih.sih26135.repository.RefBatchStatusRepository;
import in.gov.sih.sih26135.repository.TrainingBatchRepository;
import in.gov.sih.sih26135.repository.TrainingCenterRepository;
import in.gov.sih.sih26135.response.PaginationUtils;
import in.gov.sih.sih26135.security.WorkflowSecurityScopeValidator;
import in.gov.sih.sih26135.service.TrainingBatchService;
import in.gov.sih.sih26135.service.AuditLogService;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.LinkedHashMap;
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
public class TrainingBatchServiceImpl implements TrainingBatchService {

  private final TrainingBatchRepository trainingBatchRepository;
  private final TrainingCenterRepository trainingCenterRepository;
  private final RefBatchStatusRepository refBatchStatusRepository;
  private final TrainingBatchMapper trainingBatchMapper;
  private final WorkflowSecurityScopeValidator scopeValidator;
  private final JdbcTemplate jdbcTemplate;
  private final AuditLogService auditLogs;

  @Autowired
  public TrainingBatchServiceImpl(
      TrainingBatchRepository trainingBatchRepository,
      TrainingCenterRepository trainingCenterRepository,
      RefBatchStatusRepository refBatchStatusRepository,
      TrainingBatchMapper trainingBatchMapper,
      WorkflowSecurityScopeValidator scopeValidator,
      JdbcTemplate jdbcTemplate,
      AuditLogService auditLogs) {
    this.trainingBatchRepository = trainingBatchRepository;
    this.trainingCenterRepository = trainingCenterRepository;
    this.refBatchStatusRepository = refBatchStatusRepository;
    this.trainingBatchMapper = trainingBatchMapper;
    this.scopeValidator = scopeValidator;
    this.jdbcTemplate = jdbcTemplate;
    this.auditLogs = auditLogs;
  }

  @Override
  public TrainingBatchResponse getById(Long id) {
    TrainingTenantScopeSupport.CallerScope scope = resolveScope();
    TrainingBatch batch = trainingBatchRepository.findById(id)
        .orElseThrow(() -> new ResourceNotFoundException("TrainingBatch", "id"));
    validateBatchScope(scope, batch);
    return trainingBatchMapper.toResponse(batch);
  }

  @Override
  public TrainingBatchResponse getByCode(String batchCode) {
    TrainingTenantScopeSupport.CallerScope scope = resolveScope();
    TrainingBatch batch = trainingBatchRepository.findByBatchCode(batchCode)
        .orElseThrow(() -> new ResourceNotFoundException("TrainingBatch", "batchCode"));
    validateBatchScope(scope, batch);
    return trainingBatchMapper.toResponse(batch);
  }

  @Override
  public List<TrainingBatchResponse> getAllBatches() {
    TrainingTenantScopeSupport.CallerScope scope = resolveScope();
    if (scope == null || scope.nationwideUnrestricted()) {
      return trainingBatchRepository.findAll().stream()
          .map(trainingBatchMapper::toResponse)
          .toList();
    }
    return queryScopedBatchesPage("1=1", List.of(), scope, Pageable.unpaged()).getContent();
  }

  @Override
  public List<TrainingBatchResponse> getBatchesByCourseId(Long courseId) {
    TrainingTenantScopeSupport.CallerScope scope = resolveScope();
    if (scope == null || scope.nationwideUnrestricted()) {
      return trainingBatchRepository.findByCourseId(courseId).stream()
          .map(trainingBatchMapper::toResponse)
          .toList();
    }
    return queryScopedBatchesPage("tb.course_id = ?", List.of(courseId), scope, Pageable.unpaged()).getContent();
  }

  @Override
  public List<TrainingBatchResponse> getBatchesByProviderId(Long providerId) {
    TrainingTenantScopeSupport.CallerScope scope = resolveScope();
    if (scope != null && scope.isProviderScoped() && !scope.trainingProviderId().equals(providerId)) {
      throw new AccessDeniedException("Training provider cannot access another provider's batches");
    }
    if (scope == null || scope.nationwideUnrestricted()) {
      return trainingBatchRepository.findByTrainingProviderId(providerId).stream()
          .map(trainingBatchMapper::toResponse)
          .toList();
    }
    return queryScopedBatchesPage("tb.provider_id = ?", List.of(providerId), scope, Pageable.unpaged()).getContent();
  }

  @Override
  public List<TrainingBatchResponse> getBatchesByCenterId(Long centerId) {
    TrainingTenantScopeSupport.CallerScope scope = resolveScope();
    validateCenterQueryAllowed(scope, centerId);
    if (scope == null || scope.nationwideUnrestricted()) {
      return trainingBatchRepository.findByTrainingCenterId(centerId).stream()
          .map(trainingBatchMapper::toResponse)
          .toList();
    }
    return queryScopedBatchesPage("tb.center_id = ?", List.of(centerId), scope, Pageable.unpaged()).getContent();
  }

  @Override
  public List<TrainingBatchResponse> getBatchesByProgramId(Long programId) {
    TrainingTenantScopeSupport.CallerScope scope = resolveScope();
    if (scope == null || scope.nationwideUnrestricted()) {
      return trainingBatchRepository.findByProgramId(programId).stream()
          .map(trainingBatchMapper::toResponse)
          .toList();
    }
    return queryScopedBatchesPage("tb.program_id = ?", List.of(programId), scope, Pageable.unpaged()).getContent();
  }

  @Override
  public List<TrainingBatchResponse> getBatchesByStatusId(Long batchStatusId) {
    TrainingTenantScopeSupport.CallerScope scope = resolveScope();
    if (scope == null || scope.nationwideUnrestricted()) {
      return trainingBatchRepository.findByBatchStatusId(batchStatusId).stream()
          .map(trainingBatchMapper::toResponse)
          .toList();
    }
    return queryScopedBatchesPage("tb.batch_status_id = ?", List.of(batchStatusId), scope, Pageable.unpaged()).getContent();
  }

  @Override
  public Page<TrainingBatchResponse> getAllBatches(Pageable pageable) {
    TrainingTenantScopeSupport.CallerScope scope = resolveScope();
    if (scope == null || scope.nationwideUnrestricted()) {
      return PaginationUtils.executeSafeJpaPage(pageable, trainingBatchRepository::findByDeletedAtIsNull)
          .map(trainingBatchMapper::toResponse);
    }
    return queryScopedBatchesPage("1=1", List.of(), scope, pageable);
  }

  @Override
  public Page<TrainingBatchResponse> getBatchesByCourseId(Long courseId, Pageable pageable) {
    TrainingTenantScopeSupport.CallerScope scope = resolveScope();
    if (scope == null || scope.nationwideUnrestricted()) {
      return PaginationUtils.executeSafeJpaPage(
              pageable, p -> trainingBatchRepository.findByCourseIdAndDeletedAtIsNull(courseId, p))
          .map(trainingBatchMapper::toResponse);
    }
    return queryScopedBatchesPage("tb.course_id = ?", List.of(courseId), scope, pageable);
  }

  @Override
  public Page<TrainingBatchResponse> getBatchesByProviderId(Long providerId, Pageable pageable) {
    TrainingTenantScopeSupport.CallerScope scope = resolveScope();
    if (scope != null && scope.isProviderScoped() && !scope.trainingProviderId().equals(providerId)) {
      throw new AccessDeniedException("Training provider cannot access another provider's batches");
    }
    if (scope == null || scope.nationwideUnrestricted() || scope.isProviderScoped()) {
      return PaginationUtils.executeSafeJpaPage(
              pageable, p -> trainingBatchRepository.findByTrainingProviderIdAndDeletedAtIsNull(providerId, p))
          .map(trainingBatchMapper::toResponse);
    }
    return queryScopedBatchesPage("tb.provider_id = ?", List.of(providerId), scope, pageable);
  }

  @Override
  public Page<TrainingBatchResponse> getBatchesByCenterId(Long centerId, Pageable pageable) {
    TrainingTenantScopeSupport.CallerScope scope = resolveScope();
    validateCenterQueryAllowed(scope, centerId);
    if (scope == null || scope.nationwideUnrestricted()) {
      return PaginationUtils.executeSafeJpaPage(
              pageable, p -> trainingBatchRepository.findByTrainingCenterIdAndDeletedAtIsNull(centerId, p))
          .map(trainingBatchMapper::toResponse);
    }
    return queryScopedBatchesPage("tb.center_id = ?", List.of(centerId), scope, pageable);
  }

  @Override
  public Page<TrainingBatchResponse> getBatchesByProgramId(Long programId, Pageable pageable) {
    TrainingTenantScopeSupport.CallerScope scope = resolveScope();
    if (scope == null || scope.nationwideUnrestricted()) {
      return PaginationUtils.executeSafeJpaPage(
              pageable, p -> trainingBatchRepository.findByProgramIdAndDeletedAtIsNull(programId, p))
          .map(trainingBatchMapper::toResponse);
    }
    return queryScopedBatchesPage("tb.program_id = ?", List.of(programId), scope, pageable);
  }

  @Override
  public Page<TrainingBatchResponse> getBatchesByStatusId(Long batchStatusId, Pageable pageable) {
    TrainingTenantScopeSupport.CallerScope scope = resolveScope();
    if (scope == null || scope.nationwideUnrestricted()) {
      return PaginationUtils.executeSafeJpaPage(
              pageable, p -> trainingBatchRepository.findByBatchStatusIdAndDeletedAtIsNull(batchStatusId, p))
          .map(trainingBatchMapper::toResponse);
    }
    return queryScopedBatchesPage("tb.batch_status_id = ?", List.of(batchStatusId), scope, pageable);
  }

  @Override
  @Transactional
  public TrainingBatchResponse updateBatchStatus(Long id, ChangeBatchStatusRequest request) {
    TrainingTenantScopeSupport.CallerScope scope = resolveScope();
    if (request == null || request.getBatchStatusId() == null) {
      throw new BadRequestException("Batch status ID is required");
    }

    TrainingBatch batch = batchForChange(id);
    validateBatchScope(scope, batch);

    if (batch.getDeletedAt() != null) {
      throw new BadRequestException("Cannot update status of a deleted training batch", "BATCH_DELETED");
    }

    RefBatchStatus status = refBatchStatusRepository.findById(request.getBatchStatusId())
        .orElseThrow(() -> new ResourceNotFoundException("RefBatchStatus", "batchStatusId"));
    validateBookedBatchTransition(batch, status.getStatusCode());

    Map<String, Object> before = batchState(batch);
    batch.setBatchStatus(status);
    batch.setUpdatedAt(LocalDateTime.now());

    TrainingBatch saved = trainingBatchRepository.save(batch);
    auditBatch("STATUS_CHANGE", "Training batch status changed", id, before, batchState(saved));
    return trainingBatchMapper.toResponse(saved);
  }

  private TrainingBatch batchForChange(Long id) {
    var locked = auditLogs == null ? java.util.Optional.<TrainingBatch>empty() : trainingBatchRepository.findByIdForUpdate(id);
    if (locked.isPresent()) {
      return locked.get();
    }
    return trainingBatchRepository.findById(id)
        .orElseThrow(() -> new ResourceNotFoundException("TrainingBatch", "id"));
  }

  private void validateBookedBatchTransition(TrainingBatch batch, String next) {
    if (!Boolean.TRUE.equals(jdbcTemplate.queryForObject("""
        SELECT EXISTS (SELECT 1 FROM venue_bookings WHERE batch_id=? AND booking_status='APPROVED')
        """, Boolean.class, batch.getId()))) {
      throw new ConflictException("This batch requires an approved venue booking.", "VENUE_APPROVED_BOOKING_REQUIRED");
    }
    String current = batch.getBatchStatus().getStatusCode();
    if (current.equals(next)) return;
    if ("PLANNED".equals(current) && "OPEN".equals(next)) return;
    if ("OPEN".equals(current) && "ONGOING".equals(next)) {
      var now = LocalDateTime.now(java.time.ZoneId.of("Asia/Kolkata"));
      if (Boolean.TRUE.equals(jdbcTemplate.queryForObject("""
          SELECT EXISTS (SELECT 1 FROM venue_booking_sessions s JOIN venue_bookings b USING (booking_id)
            WHERE b.batch_id=? AND s.reserved_at IS NOT NULL AND s.released_at IS NULL AND s.starts_at<=?)
          """, Boolean.class, batch.getId(), now))) {
        return;
      }
      throw new ConflictException("Training can start when the first approved session begins.", "VENUE_BATCH_NOT_STARTED");
    }
    throw new ConflictException("Manage this batch schedule from Venue Bookings.", "VENUE_WORKFLOW_REQUIRED");
  }

  private Map<String, Object> batchState(TrainingBatch batch) {
    Map<String, Object> state = new LinkedHashMap<>();
    state.put("status", batch.getBatchStatus() != null ? batch.getBatchStatus().getStatusCode() : null);
    state.put("capacity", batch.getCapacity());
    state.put("startDate", batch.getStartDate() != null ? batch.getStartDate().toString() : null);
    state.put("endDate", batch.getEndDate() != null ? batch.getEndDate().toString() : null);
    return state;
  }

  private void auditBatch(String action, String summary, Long id, Map<String, ?> before, Map<String, ?> after) {
    if (auditLogs != null) {
      auditLogs.recordStateChange(action, "TrainingBatch", id, summary, before, after, List.of());
    }
  }

  private TrainingTenantScopeSupport.CallerScope resolveScope() {
    if (scopeValidator == null) {
      return null;
    }
    return TrainingTenantScopeSupport.resolveCallerScope(scopeValidator, jdbcTemplate);
  }

  private void validateBatchScope(TrainingTenantScopeSupport.CallerScope scope, TrainingBatch batch) {
    if (scope == null || scope.nationwideUnrestricted() || batch == null) {
      return;
    }
    TrainingCenter center = batch.getTrainingCenter();
    Long batchProv = batch.getTrainingProvider() != null ? batch.getTrainingProvider().getId() : null;
    Long centerProv = center != null && center.getTrainingProvider() != null ? center.getTrainingProvider().getId() : null;
    if (center != null && "SHARED".equals(center.getVenueType())) {
      centerProv = batchProv;
    }
    Long stateId = center != null ? center.getStateId() : null;
    Long districtId = center != null ? center.getDistrictId() : null;
    TrainingTenantScopeSupport.validateBatchOwnership(scope, batchProv, centerProv, stateId, districtId);
  }

  private void validateCenterQueryAllowed(TrainingTenantScopeSupport.CallerScope scope, Long centerId) {
    if (scope == null || scope.nationwideUnrestricted() || centerId == null) {
      return;
    }
    TrainingCenter center = trainingCenterRepository.findById(centerId)
        .orElseThrow(() -> new ResourceNotFoundException("TrainingCenter", "centerId"));
    if (scope.isProviderScoped() && "SHARED".equals(center.getVenueType())) return;
    TrainingTenantScopeSupport.validateCenterOwnership(
        scope,
        center.getTrainingProvider() != null ? center.getTrainingProvider().getId() : null,
        center.getStateId(),
        center.getDistrictId());
  }

  private Page<TrainingBatchResponse> queryScopedBatchesPage(
      String basePredicate,
      List<Object> baseParams,
      TrainingTenantScopeSupport.CallerScope scope,
      Pageable pageable) {
    StringBuilder where = new StringBuilder(
        " FROM training_batches tb " +
            "JOIN training_centers tc ON tb.center_id = tc.center_id AND tc.deleted_at IS NULL " +
            "WHERE tb.deleted_at IS NULL AND ").append(basePredicate);
    List<Object> params = new ArrayList<>(baseParams);
    if (scope.isProviderScoped()) {
      where.append(" AND tb.provider_id = ? AND (tc.provider_id = ? OR tc.venue_type = 'SHARED')");
      params.add(scope.trainingProviderId());
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
    if (total == 0L || (pageable.isPaged() && pageable.getOffset() >= total)) {
      return new PageImpl<>(List.of(), pageable, total);
    }

    String orderBy = buildBatchOrderBy(pageable.isPaged() ? pageable.getSort() : Sort.unsorted());
    List<Object> pageParams = new ArrayList<>(params);
    String limitClause = "";
    if (pageable.isPaged()) {
      limitClause = " LIMIT ? OFFSET ?";
      pageParams.add(pageable.getPageSize());
      pageParams.add(pageable.getOffset());
    }

    List<Long> pageIds = jdbcTemplate.queryForList(
        "SELECT tb.batch_id" + where + orderBy + limitClause,
        Long.class, pageParams.toArray());
    if (pageIds.isEmpty()) {
      return new PageImpl<>(List.of(), pageable, total);
    }

    Map<Long, TrainingBatchResponse> mapped = new HashMap<>();
    for (TrainingBatch b : trainingBatchRepository.findAllById(pageIds)) {
      mapped.put(b.getId(), trainingBatchMapper.toResponse(b));
    }
    List<TrainingBatchResponse> ordered = new ArrayList<>(pageIds.size());
    for (Long bid : pageIds) {
      TrainingBatchResponse resp = mapped.get(bid);
      if (resp != null) {
        ordered.add(resp);
      }
    }
    return new PageImpl<>(ordered, pageable, total);
  }

  private String buildBatchOrderBy(Sort sort) {
    if (sort == null || sort.isUnsorted()) {
      return " ORDER BY tb.batch_id ASC";
    }
    List<String> clauses = new ArrayList<>();
    for (Sort.Order o : sort) {
      String col = switch (o.getProperty()) {
        case "batchCode" -> "tb.batch_code";
        case "startDate" -> "tb.start_date";
        case "endDate" -> "tb.end_date";
        case "capacity" -> "tb.capacity";
        case "createdAt" -> "tb.created_at";
        case "updatedAt" -> "tb.updated_at";
        default -> "tb.batch_id";
      };
      clauses.add(col + (o.isDescending() ? " DESC" : " ASC"));
    }
    return clauses.isEmpty() ? " ORDER BY tb.batch_id ASC" : " ORDER BY " + String.join(", ", clauses);
  }
}
