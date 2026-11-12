package in.gov.sih.sih26135.service.ingestion;

import in.gov.sih.sih26135.entity.ImportBatch;
import in.gov.sih.sih26135.entity.User;
import in.gov.sih.sih26135.exception.BadRequestException;
import in.gov.sih.sih26135.repository.UserRepository;
import in.gov.sih.sih26135.security.WorkflowSecurityScopeValidator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

/**
 * Enforces fail-closed role, permission, and multi-tenant isolation boundaries for bulk import
 * uploads, batch processing, progress monitoring, and record/quarantine retrieval.
 */
@Component
public class ImportSecurityScopeValidator {

  private final WorkflowSecurityScopeValidator scopeValidator;
  private final UserRepository userRepository;
  private final JdbcTemplate jdbcTemplate;

  public ImportSecurityScopeValidator(
      WorkflowSecurityScopeValidator scopeValidator,
      UserRepository userRepository,
      JdbcTemplate jdbcTemplate) {
    this.scopeValidator = Objects.requireNonNull(scopeValidator, "scopeValidator is required");
    this.userRepository = Objects.requireNonNull(userRepository, "userRepository is required");
    this.jdbcTemplate = Objects.requireNonNull(jdbcTemplate, "jdbcTemplate is required");
  }

  public record CallerScope(
      User caller,
      boolean nationwideAdmin,
      Long stateId,
      Long districtId,
      Long trainingProviderId,
      Long employerId,
      Long providerStateId,
      Set<String> authorities) {

    public boolean isStateOrDistrictScoped() {
      return !nationwideAdmin && stateId != null;
    }

    public boolean isProviderScoped() {
      return !nationwideAdmin && trainingProviderId != null;
    }

    public boolean isEmployerScoped() {
      return !nationwideAdmin && employerId != null;
    }
  }

  public String normalizeImportEntityType(String rawEntityType) {
    if (rawEntityType == null || rawEntityType.isBlank()) {
      throw new BadRequestException("entityType is required", "ENTITY_TYPE_REQUIRED");
    }
    String key = rawEntityType.trim()
        .replace("_", "")
        .replace("-", "")
        .replace(" ", "")
        .toLowerCase(Locale.ROOT);
    return switch (key) {
      case "trainee", "trainees" -> ImportContractSchema.ENTITY_TRAINEE;
      case "placement", "placements", "placementrecord", "placementrecords" ->
          ImportContractSchema.ENTITY_PLACEMENT_RECORD;
      default -> throw new BadRequestException(
          "Unsupported import entityType; allowed values: Trainee, PlacementRecord",
          "UNSUPPORTED_IMPORT_ENTITY_TYPE");
    };
  }

  public CallerScope resolveAndValidateGenericWriteCaller() {
    Authentication auth = SecurityContextHolder.getContext().getAuthentication();
    if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getPrincipal())) {
      throw new AccessDeniedException("Authenticated user identity is required for bulk import operations");
    }
    Set<String> authorities = auth.getAuthorities() != null
        ? auth.getAuthorities().stream().map(GrantedAuthority::getAuthority).collect(Collectors.toSet())
        : Set.of();
    boolean hasAnyWrite = authorities.contains("system.manage")
        || authorities.contains("trainee.write")
        || authorities.contains("placement.manage");
    if (!hasAnyWrite) {
      throw new AccessDeniedException("Caller lacks required permission to modify bulk import resources");
    }
    return resolveAuthenticatedScope(false, null);
  }

  public void validateCallerCanWriteBatch(CallerScope callerScope, ImportBatch batch) {
    validateCallerCanAccessBatch(callerScope, batch);
    boolean hasManage = callerScope.authorities().contains("system.manage");
    String normalizedBatchEntity = normalizeEntityTypeLenient(batch.getEntityType());
    boolean hasDomainWrite = (ImportContractSchema.ENTITY_TRAINEE.equals(normalizedBatchEntity)
        && callerScope.authorities().contains("trainee.write"))
        || (ImportContractSchema.ENTITY_PLACEMENT_RECORD.equals(normalizedBatchEntity)
        && callerScope.authorities().contains("placement.manage"));
    if (!hasManage && !hasDomainWrite) {
      throw new AccessDeniedException("Caller lacks write permission for import batch entityType=" + normalizedBatchEntity);
    }
    if (callerScope.isProviderScoped()) {
      verifyActiveProviderExists(callerScope.trainingProviderId());
    }
    if (callerScope.isEmployerScoped()) {
      verifyActiveEmployerExists(callerScope.employerId());
    }
  }

  public void appendBatchTenantWhereClause(CallerScope scope, StringBuilder sql, List<Object> params) {
    if (scope.nationwideAdmin()) {
      return;
    }
    sql.append(" AND u.user_id IS NOT NULL");
    if (scope.isEmployerScoped()) {
      sql.append(" AND LOWER(TRIM(ib.entity_type)) NOT IN ('trainee', 'trainees') AND u.employer_id = ?");
      params.add(scope.employerId());
      return;
    }
    if (scope.isProviderScoped()) {
      sql.append(" AND u.training_provider_id = ?");
      params.add(scope.trainingProviderId());
      return;
    }
    if (scope.isStateOrDistrictScoped()) {
      if (scope.districtId() != null) {
        sql.append(" AND u.state_id = ? AND u.district_id = ?");
        params.add(scope.stateId());
        params.add(scope.districtId());
      } else {
        sql.append(" AND COALESCE(u.state_id, tp.state_id, emp.state_id) = ?");
        params.add(scope.stateId());
      }
      return;
    }
    sql.append(" AND 1 = 0");
  }

  public CallerScope resolveAndValidateUploadOrProcessCaller(String normalizedEntityType) {
    CallerScope scope = resolveAuthenticatedScope(true, normalizedEntityType);
    if (scope.isEmployerScoped() && ImportContractSchema.ENTITY_TRAINEE.equals(normalizedEntityType)) {
      throw new AccessDeniedException("Employers are not authorized to bulk-import trainee records");
    }
    if (scope.isProviderScoped()) {
      verifyActiveProviderExists(scope.trainingProviderId());
    }
    if (scope.isEmployerScoped()) {
      verifyActiveEmployerExists(scope.employerId());
    }
    return scope;
  }

  public CallerScope resolveAndValidateReadCaller(String entityTypeHint) {
    String normalizedHint = null;
    if (entityTypeHint != null && !entityTypeHint.isBlank()) {
      normalizedHint = normalizeEntityTypeLenient(entityTypeHint);
    }
    return resolveAuthenticatedScope(false, normalizedHint);
  }

  public void validateCallerCanAccessBatch(CallerScope callerScope, ImportBatch batch) {
    if (batch == null) {
      throw new AccessDeniedException("Import batch is unavailable for scope verification");
    }
    String normalizedBatchEntity = normalizeEntityTypeLenient(batch.getEntityType());
    if (callerScope.isEmployerScoped() && ImportContractSchema.ENTITY_TRAINEE.equals(normalizedBatchEntity)) {
      throw new AccessDeniedException("Employers are not authorized to access trainee import batches");
    }
    if (callerScope.nationwideAdmin()) {
      return;
    }

    Long initiatorId = batch.getInitiatedByUserId();
    if (initiatorId == null) {
      throw new AccessDeniedException("Batch lacks verified initiator provenance for scoped access");
    }

    User initiator = userRepository.findById(initiatorId)
        .orElseThrow(() -> new AccessDeniedException("Batch initiator record could not be verified"));

    if (callerScope.isEmployerScoped()) {
      if (initiator.getEmployerId() == null || !initiator.getEmployerId().equals(callerScope.employerId())) {
        throw new AccessDeniedException("Employer caller is not authorized to access another employer's import batch");
      }
      return;
    }

    if (callerScope.isProviderScoped()) {
      if (initiator.getTrainingProviderId() == null
          || !initiator.getTrainingProviderId().equals(callerScope.trainingProviderId())) {
        throw new AccessDeniedException("Training provider caller is not authorized to access another provider's import batch");
      }
      return;
    }

    if (callerScope.isStateOrDistrictScoped()) {
      if (callerScope.districtId() != null) {
        if (!Objects.equals(initiator.getStateId(), callerScope.stateId())
            || !Objects.equals(initiator.getDistrictId(), callerScope.districtId())) {
          throw new AccessDeniedException("District caller is not authorized to access import batch outside district scope");
        }
        return;
      }

      Long initiatorStateId = resolveUserEffectiveStateId(initiator);
      if (initiatorStateId == null || !initiatorStateId.equals(callerScope.stateId())) {
        throw new AccessDeniedException("State caller is not authorized to access import batch outside state scope");
      }
      return;
    }

    throw new AccessDeniedException("Caller lacks valid tenant scope to access import batch");
  }

  public boolean canCallerViewBatchSilently(CallerScope callerScope, ImportBatch batch) {
    try {
      validateCallerCanAccessBatch(callerScope, batch);
      return true;
    } catch (AccessDeniedException ex) {
      return false;
    }
  }

  private CallerScope resolveAuthenticatedScope(boolean requireWriteAuthority, String normalizedEntityType) {
    Authentication auth = SecurityContextHolder.getContext().getAuthentication();
    if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getPrincipal())) {
      throw new AccessDeniedException("Authenticated user identity is required for bulk import operations");
    }

    Set<String> authorities = auth.getAuthorities() != null
        ? auth.getAuthorities().stream().map(GrantedAuthority::getAuthority).collect(Collectors.toSet())
        : Set.of();

    if (requireWriteAuthority) {
      boolean hasManage = authorities.contains("system.manage");
      boolean hasDomainWrite = (ImportContractSchema.ENTITY_TRAINEE.equals(normalizedEntityType)
          && authorities.contains("trainee.write"))
          || (ImportContractSchema.ENTITY_PLACEMENT_RECORD.equals(normalizedEntityType)
          && authorities.contains("placement.manage"));
      if (!hasManage && !hasDomainWrite) {
        throw new AccessDeniedException("Caller lacks required permission to perform bulk import operations");
      }
    } else {
      boolean hasRead = authorities.contains("system.manage")
          || authorities.contains("audit.read")
          || authorities.contains("trainee.read")
          || authorities.contains("trainee.write")
          || authorities.contains("placement.manage");
      if (!hasRead) {
        throw new AccessDeniedException("Caller lacks required permission to view import batches");
      }
    }

    Long callerUserId = scopeValidator.resolveAuthenticatedUserId();
    if (callerUserId == null) {
      throw new AccessDeniedException("Authenticated user identity could not be resolved");
    }
    if (scopeValidator.isTraineeCaller()) {
      throw new AccessDeniedException("Trainees are not authorized to access bulk import operations");
    }

    User caller = scopeValidator.resolveAuthenticatedUser()
        .orElseThrow(() -> new AccessDeniedException("Authoritative caller user record not found"));

    if (caller.getDeletedAt() != null || (caller.getUserStatusId() != null && caller.getUserStatusId() != 1)) {
      throw new AccessDeniedException("Caller user account is deleted or inactive");
    }

    if (scopeValidator.isNationwideAdmin()) {
      return new CallerScope(caller, true, null, null, null, null, null, authorities);
    }

    return resolveNonNationwideScope(caller, authorities);
  }

  private CallerScope resolveNonNationwideScope(User caller, Set<String> authorities) {
    boolean roleProvider = authorities.contains("ROLE_training_provider");
    boolean roleEmployer = authorities.contains("ROLE_employer");
    boolean roleStateAdmin = authorities.contains("ROLE_state_admin");
    boolean roleOfficer = authorities.contains("ROLE_district_officer")
        || authorities.contains("ROLE_government_officer");
    boolean roleNational = authorities.contains("ROLE_super_admin")
        || authorities.contains("ROLE_government_admin");

    int personaRoleCount = (roleProvider ? 1 : 0)
        + (roleEmployer ? 1 : 0)
        + ((roleStateAdmin || roleOfficer) ? 1 : 0);
    if (personaRoleCount > 1) {
      throw new AccessDeniedException("Caller holds conflicting persona roles");
    }

    boolean hasStateId = caller.getStateId() != null;
    boolean hasDistrictId = caller.getDistrictId() != null;
    boolean hasProviderId = caller.getTrainingProviderId() != null;
    boolean hasEmployerId = caller.getEmployerId() != null;

    if (roleNational && (hasProviderId || hasEmployerId)) {
      throw new AccessDeniedException("Caller has conflicting administrative role and organizational tenancy");
    }
    if ((roleStateAdmin || roleOfficer) && (hasProviderId || hasEmployerId)) {
      throw new AccessDeniedException("Administrative caller has conflicting provider or employer tenancy scope");
    }
    if ((roleStateAdmin || roleOfficer) && !hasStateId) {
      throw new AccessDeniedException("Administrative caller lacks required state scope");
    }
    if (roleProvider && !hasProviderId) {
      throw new AccessDeniedException("Training provider caller lacks required trainingProviderId scope");
    }
    if (roleEmployer && !hasEmployerId) {
      throw new AccessDeniedException("Employer caller lacks required employerId scope");
    }

    int tenantRootCount = (hasStateId ? 1 : 0) + (hasProviderId ? 1 : 0) + (hasEmployerId ? 1 : 0);
    if (tenantRootCount == 0) {
      throw new AccessDeniedException("Caller lacks authorized tenant scope");
    }
    if (tenantRootCount > 1) {
      throw new AccessDeniedException("Caller has ambiguous or conflicting tenant scope");
    }
    if (hasStateId && (roleProvider || roleEmployer)) {
      throw new AccessDeniedException("Caller has conflicting persona role and state tenancy");
    }
    if ((hasProviderId || hasEmployerId) && hasDistrictId) {
      throw new AccessDeniedException("Caller has conflicting organizational and geographic tenancy");
    }

    Long providerStateId = hasProviderId ? lookupProviderStateId(caller.getTrainingProviderId()) : null;
    return new CallerScope(
        caller,
        false,
        caller.getStateId(),
        caller.getDistrictId(),
        caller.getTrainingProviderId(),
        caller.getEmployerId(),
        providerStateId,
        authorities
    );
  }

  private Long resolveUserEffectiveStateId(User user) {
    if (user.getStateId() != null) {
      return user.getStateId();
    }
    if (user.getTrainingProviderId() != null) {
      return lookupProviderStateId(user.getTrainingProviderId());
    }
    if (user.getEmployerId() != null) {
      return lookupEmployerStateId(user.getEmployerId());
    }
    return null;
  }

  private Long lookupProviderStateId(Long providerId) {
    try {
      return jdbcTemplate.queryForObject(
          "SELECT state_id FROM training_providers WHERE provider_id = ? AND deleted_at IS NULL",
          Long.class, providerId);
    } catch (EmptyResultDataAccessException e) {
      return null;
    }
  }

  private Long lookupEmployerStateId(Long employerId) {
    try {
      return jdbcTemplate.queryForObject(
          "SELECT state_id FROM employers WHERE employer_id = ? AND deleted_at IS NULL",
          Long.class, employerId);
    } catch (EmptyResultDataAccessException e) {
      return null;
    }
  }

  private void verifyActiveProviderExists(Long providerId) {
    if (providerId == null || lookupProviderStateId(providerId) == null) {
      throw new AccessDeniedException("Training provider affiliation could not be verified as active");
    }
  }

  private void verifyActiveEmployerExists(Long employerId) {
    if (employerId == null || lookupEmployerStateId(employerId) == null) {
      throw new AccessDeniedException("Employer affiliation could not be verified as active");
    }
  }

  public String normalizeEntityTypeLenient(String entityType) {
    if (entityType == null) {
      return "";
    }
    String key = entityType.trim().replace("_", "").replace("-", "").replace(" ", "").toLowerCase(Locale.ROOT);
    if ("trainee".equals(key) || "trainees".equals(key)) {
      return ImportContractSchema.ENTITY_TRAINEE;
    }
    if (List.of("placement", "placements", "placementrecord", "placementrecords").contains(key)) {
      return ImportContractSchema.ENTITY_PLACEMENT_RECORD;
    }
    return entityType.trim();
  }
}
