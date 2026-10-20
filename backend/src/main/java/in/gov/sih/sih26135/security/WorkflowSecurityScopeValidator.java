package in.gov.sih.sih26135.security;

import in.gov.sih.sih26135.entity.Employer;
import in.gov.sih.sih26135.entity.TrainingProvider;
import in.gov.sih.sih26135.entity.Trainee;
import in.gov.sih.sih26135.entity.User;
import in.gov.sih.sih26135.exception.BadRequestException;
import in.gov.sih.sih26135.repository.EmployerRepository;
import in.gov.sih.sih26135.repository.OrganizationRepository;
import in.gov.sih.sih26135.repository.TraineeRepository;
import in.gov.sih.sih26135.repository.TrainingProviderRepository;
import in.gov.sih.sih26135.repository.UserRepository;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

/**
 * Validates authoritative organization, provider, employer, and trainee scope
 * boundaries for authenticated principals during workflow operations.
 *
 * <p>Enforces:
 * <ul>
 *   <li>Trainee isolation: Authenticated trainees are strictly confined to their own records.</li>
 *   <li>Employer isolation: Authenticated employers are strictly confined to their own placement and employment records.</li>
 *   <li>Provider isolation: Authenticated training providers are strictly confined to their own batches and enrollments.</li>
 *   <li>Organization isolation: Authenticated organization users are strictly confined to records within their organization.</li>
 *   <li>System administrator bypass: Authorized Phase 24 system administrator roles (super_admin, government_admin)
 *       operate with cross-tenant administrative authority. Non-catalog variants (e.g. government_officer) are not recognized.</li>
 * </ul>
 */
@Component
public class WorkflowSecurityScopeValidator {

  private static final Logger log = LoggerFactory.getLogger(WorkflowSecurityScopeValidator.class);

  private static final Set<String> SYSTEM_ADMIN_ROLES = Set.of(
      "ROLE_super_admin",
      "ROLE_government_admin"
  );

  private final UserRepository userRepository;
  private final TraineeRepository traineeRepository;
  private final EmployerRepository employerRepository;
  private final TrainingProviderRepository trainingProviderRepository;
  private final OrganizationRepository organizationRepository;

  @Autowired
  public WorkflowSecurityScopeValidator(
      UserRepository userRepository,
      TraineeRepository traineeRepository,
      EmployerRepository employerRepository,
      TrainingProviderRepository trainingProviderRepository,
      OrganizationRepository organizationRepository) {
    this.userRepository = Objects.requireNonNull(userRepository, "UserRepository is required");
    this.traineeRepository = Objects.requireNonNull(traineeRepository, "TraineeRepository is required");
    this.employerRepository = Objects.requireNonNull(employerRepository, "EmployerRepository is required");
    this.trainingProviderRepository = Objects.requireNonNull(trainingProviderRepository, "TrainingProviderRepository is required");
    this.organizationRepository = Objects.requireNonNull(organizationRepository, "OrganizationRepository is required");
  }

  /**
   * Resolves the authenticated user ID from the active Spring Security context.
   *
   * @return authenticated user ID, or {@code null} if no authenticated user is present
   */
  public Long resolveAuthenticatedUserId() {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    if (authentication == null || !authentication.isAuthenticated() || "anonymousUser".equals(authentication.getPrincipal())) {
      return null;
    }
    Object principal = authentication.getPrincipal();
    if (principal instanceof UserPrincipal up && up.getId() != null) {
      return up.getId();
    }
    if (principal instanceof UserPrincipal up && up.getUsername() != null && !up.getUsername().isBlank()) {
      return userRepository.findByUsernameOrEmail(up.getUsername(), up.getUsername())
          .map(User::getId)
          .orElse(null);
    }
    if (principal instanceof org.springframework.security.core.userdetails.UserDetails ud) {
      return userRepository.findByUsernameOrEmail(ud.getUsername(), ud.getUsername())
          .map(User::getId)
          .orElse(null);
    }
    if (principal instanceof String str && !"anonymousUser".equals(str)) {
      return userRepository.findByUsernameOrEmail(str, str)
          .map(User::getId)
          .orElse(null);
    }
    return null;
  }

  /**
   * Checks whether the authenticated caller holds platform-wide administrative authority.
   *
   * @return true if caller is an authorized Phase 24 system administrator (super_admin, government_admin)
   */
  public boolean isAdministrativeCaller() {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    if (authentication == null || authentication.getAuthorities() == null) {
      return false;
    }
    return authentication.getAuthorities().stream()
        .map(GrantedAuthority::getAuthority)
        .anyMatch(SYSTEM_ADMIN_ROLES::contains);
  }

  /**
   * Resolves the authoritative User entity for the currently authenticated caller.
   *
   * @return Optional containing the User entity, or empty if not authenticated or not found
   */
  public Optional<User> resolveAuthenticatedUser() {
    Long userId = resolveAuthenticatedUserId();
    if (userId == null) {
      return Optional.empty();
    }
    return userRepository.findById(userId);
  }

  /**
   * Resolves the authoritative Trainee entity associated with the currently authenticated caller.
   *
   * @return Optional containing the Trainee entity, or empty if not a trainee user
   */
  public Optional<Trainee> resolveAuthenticatedTrainee() {
    Long userId = resolveAuthenticatedUserId();
    if (userId == null) {
      return Optional.empty();
    }
    return traineeRepository.findByUserId(userId);
  }

  /**
   * Validates that the target employer ID matches the caller's assigned employer scope.
   * Target employer must exist and have a valid organization affiliation.
   * Administrative callers bypass cross-tenant single-employer assignment matching,
   * but do not bypass required target ID, target existence, or organization affiliation.
   *
   * @param targetEmployerId the employer ID of the resource being accessed or mutated
   * @throws BadRequestException if target is null, non-existent, unassigned to org, or scope mismatched
   */
  public void validateEmployerScope(Long targetEmployerId) {
    if (targetEmployerId == null) {
      throw new BadRequestException(
          "Target employer ID is required for scope validation",
          "EMPLOYER_ID_REQUIRED"
      );
    }

    // Administrative callers bypass cross-tenant assignment matching, but NOT target existence or affiliation
    if (isAdministrativeCaller()) {
      Employer targetEmployer = employerRepository.findById(targetEmployerId)
          .orElseThrow(() -> new BadRequestException(
              "Target employer record not found for scope validation",
              "EMPLOYER_NOT_FOUND"
          ));
      Long targetOrgId = targetEmployer.getOrganizationId();
      if (targetOrgId == null) {
        log.warn("Organization scope violation: target employer {} is missing organization affiliation", targetEmployerId);
        throw new BadRequestException(
            "Target employer is missing required organization affiliation",
            "ORGANIZATION_ASSIGNMENT_REQUIRED"
        );
      }
      if (!organizationRepository.existsById(targetOrgId)) {
        log.warn("Organization scope violation: target employer {} has invalid organization affiliation {}", targetEmployerId, targetOrgId);
        throw new BadRequestException(
            "Target employer organization affiliation record not found",
            "ORGANIZATION_NOT_FOUND"
        );
      }
      return;
    }

    // Non-administrative caller:
    // 1. Resolve caller user and verify employer assignment
    User authUser = resolveAuthenticatedUser()
        .orElseThrow(() -> new BadRequestException(
            "Authenticated user cannot be resolved for scope validation",
            "AUTHENTICATED_USER_UNRESOLVED"
        ));
    Long userEmployerId = authUser.getEmployerId();
    if (userEmployerId == null) {
      log.warn("Employer scope violation: authenticated user {} has no employer assignment", authUser.getId());
      throw new BadRequestException(
          "Authenticated user is not assigned to an employer",
          "EMPLOYER_ASSIGNMENT_REQUIRED"
      );
    }
    // 2. Tenancy boundary check: Caller cannot access other employers (zero data disclosure)
    if (!userEmployerId.equals(targetEmployerId)) {
      log.warn("Employer scope violation: authenticated user employer={} attempted to access target employer={}",
          userEmployerId, targetEmployerId);
      throw new BadRequestException(
          "Authenticated employer is not authorized to act on records of another employer",
          "EMPLOYER_MISMATCH"
      );
    }

    // 3. Verify target employer exists and has valid organization affiliation
    Employer targetEmployer = employerRepository.findById(targetEmployerId)
        .orElseThrow(() -> new BadRequestException(
            "Target employer record not found for scope validation",
            "EMPLOYER_NOT_FOUND"
        ));
    Long targetOrgId = targetEmployer.getOrganizationId();
    if (targetOrgId == null) {
      log.warn("Organization scope violation: target employer {} is missing organization affiliation", targetEmployerId);
      throw new BadRequestException(
          "Target employer is missing required organization affiliation",
          "ORGANIZATION_ASSIGNMENT_REQUIRED"
      );
    }

    // 4. Resolve caller organization and validate organization scope - FAIL CLOSED
    Long userOrgId = authUser.getOrganizationId();
    if (userOrgId == null && userEmployerId != null) {
      userOrgId = employerRepository.findById(userEmployerId)
          .map(Employer::getOrganizationId)
          .orElse(null);
    }
    if (userOrgId == null) {
      log.warn("Organization scope violation: authenticated user {} (employer {}) has no organization affiliation",
          authUser.getId(), userEmployerId);
      throw new BadRequestException(
          "Authenticated user is not assigned to an organization",
          "ORGANIZATION_ASSIGNMENT_REQUIRED"
      );
    }

    // 5. Validate organization scope match
    validateOrganizationScope(targetOrgId);
  }

  /**
   * Validates that the target training provider ID matches the caller's assigned provider scope.
   * Target provider must exist and have a valid organization affiliation.
   * Administrative callers bypass cross-tenant single-provider assignment matching,
   * but do not bypass required target ID, target existence, or organization affiliation.
   *
   * @param targetProviderId the provider ID of the resource being accessed or mutated
   * @throws BadRequestException if target is null, non-existent, unassigned to org, or scope mismatched
   */
  public void validateProviderScope(Long targetProviderId) {
    if (targetProviderId == null) {
      throw new BadRequestException(
          "Target training provider ID is required for scope validation",
          "PROVIDER_ID_REQUIRED"
      );
    }

    // Administrative callers bypass cross-tenant assignment matching, but NOT target existence or affiliation
    if (isAdministrativeCaller()) {
      TrainingProvider targetProvider = trainingProviderRepository.findById(targetProviderId)
          .orElseThrow(() -> new BadRequestException(
              "Target training provider record not found for scope validation",
              "PROVIDER_NOT_FOUND"
          ));
      Long targetOrgId = targetProvider.getOrganizationId();
      if (targetOrgId == null) {
        log.warn("Organization scope violation: target provider {} is missing organization affiliation", targetProviderId);
        throw new BadRequestException(
            "Target training provider is missing required organization affiliation",
            "ORGANIZATION_ASSIGNMENT_REQUIRED"
        );
      }
      if (!organizationRepository.existsById(targetOrgId)) {
        log.warn("Organization scope violation: target provider {} has invalid organization affiliation {}", targetProviderId, targetOrgId);
        throw new BadRequestException(
            "Target training provider organization affiliation record not found",
            "ORGANIZATION_NOT_FOUND"
        );
      }
      return;
    }

    // Non-administrative caller:
    // 1. Resolve caller user and verify provider assignment
    User authUser = resolveAuthenticatedUser()
        .orElseThrow(() -> new BadRequestException(
            "Authenticated user cannot be resolved for scope validation",
            "AUTHENTICATED_USER_UNRESOLVED"
        ));
    Long userProviderId = authUser.getTrainingProviderId();
    if (userProviderId == null) {
      log.warn("Training provider scope violation: authenticated user {} has no training provider assignment", authUser.getId());
      throw new BadRequestException(
          "Authenticated user is not assigned to a training provider",
          "PROVIDER_ASSIGNMENT_REQUIRED"
      );
    }
    // 2. Tenancy boundary check: Caller cannot access other providers (zero data disclosure)
    if (!userProviderId.equals(targetProviderId)) {
      log.warn("Training provider scope violation: authenticated user provider={} attempted to access target provider={}",
          userProviderId, targetProviderId);
      throw new BadRequestException(
          "Authenticated training provider is not authorized to act on records of another training provider",
          "PROVIDER_MISMATCH"
      );
    }

    // 3. Verify target provider exists and has valid organization affiliation
    TrainingProvider targetProvider = trainingProviderRepository.findById(targetProviderId)
        .orElseThrow(() -> new BadRequestException(
            "Target training provider record not found for scope validation",
            "PROVIDER_NOT_FOUND"
        ));
    Long targetOrgId = targetProvider.getOrganizationId();
    if (targetOrgId == null) {
      log.warn("Organization scope violation: target provider {} is missing organization affiliation", targetProviderId);
      throw new BadRequestException(
          "Target training provider is missing required organization affiliation",
          "ORGANIZATION_ASSIGNMENT_REQUIRED"
      );
    }

    // 4. Resolve caller organization and validate organization scope - FAIL CLOSED
    Long userOrgId = authUser.getOrganizationId();
    if (userOrgId == null && userProviderId != null) {
      userOrgId = trainingProviderRepository.findById(userProviderId)
          .map(TrainingProvider::getOrganizationId)
          .orElse(null);
    }
    if (userOrgId == null) {
      log.warn("Organization scope violation: authenticated user {} (provider {}) has no organization affiliation",
          authUser.getId(), userProviderId);
      throw new BadRequestException(
          "Authenticated user is not assigned to an organization",
          "ORGANIZATION_ASSIGNMENT_REQUIRED"
      );
    }

    // 5. Validate organization scope match
    validateOrganizationScope(targetOrgId);
  }

  /**
   * Validates that the target trainee ID matches the caller's self-service trainee scope.
   * Target trainee must exist in the repository.
   * Administrative callers bypass self-service 1:1 identity matching,
   * but do not bypass required target ID or target existence.
   *
   * @param targetTraineeId the trainee ID of the resource being accessed or mutated
   * @throws BadRequestException if target is null, non-existent, or caller does not match
   */
  public void validateTraineeScope(Long targetTraineeId) {
    if (targetTraineeId == null) {
      throw new BadRequestException(
          "Target trainee ID is required for scope validation",
          "TRAINEE_ID_REQUIRED"
      );
    }

    // Administrative callers bypass self-service 1:1 identity matching, but NOT target existence
    if (isAdministrativeCaller()) {
      if (!traineeRepository.existsById(targetTraineeId)) {
        throw new BadRequestException(
            "Target trainee record not found for scope validation",
            "TRAINEE_NOT_FOUND"
        );
      }
      return;
    }

    // Non-administrative caller:
    // 1. Resolve caller user and trainee assignment
    resolveAuthenticatedUser()
        .orElseThrow(() -> new BadRequestException(
            "Authenticated user cannot be resolved for scope validation",
            "AUTHENTICATED_USER_UNRESOLVED"
        ));
    Trainee authTrainee = resolveAuthenticatedTrainee()
        .orElseThrow(() -> new BadRequestException(
            "Authenticated user is not assigned to a trainee profile",
            "TRAINEE_ASSIGNMENT_REQUIRED"
        ));
    // 2. Tenancy boundary check: Caller cannot access other trainees (zero data disclosure)
    if (!authTrainee.getId().equals(targetTraineeId)) {
      log.warn("Trainee self-service scope violation: authenticated trainee={} attempted to access target trainee={}",
          authTrainee.getId(), targetTraineeId);
      throw new BadRequestException(
          "Authenticated trainee is not authorized to act on records of another trainee",
          "TRAINEE_MISMATCH"
      );
    }

    // 3. Verify target trainee exists
    if (!traineeRepository.existsById(targetTraineeId)) {
      throw new BadRequestException(
          "Target trainee record not found for scope validation",
          "TRAINEE_NOT_FOUND"
      );
    }
  }

  /**
   * Validates that the target organization ID matches the caller's assigned organization scope.
   * Administrative callers bypass single-organization assignment matching,
   * but do not bypass required target organization ID.
   *
   * @param targetOrganizationId the organization ID of the resource being accessed or mutated
   * @throws BadRequestException if target is null, or caller does not match
   */
  public void validateOrganizationScope(Long targetOrganizationId) {
    if (targetOrganizationId == null) {
      throw new BadRequestException(
          "Target organization ID is required for scope validation",
          "ORGANIZATION_ID_REQUIRED"
      );
    }

    // Administrative callers bypass cross-tenant organization scope matching, but NOT target existence
    if (isAdministrativeCaller()) {
      if (!organizationRepository.existsById(targetOrganizationId)) {
        log.warn("Organization scope violation: target organization {} not found", targetOrganizationId);
        throw new BadRequestException(
            "Target organization record not found for scope validation",
            "ORGANIZATION_NOT_FOUND"
        );
      }
      return;
    }

    User authUser = resolveAuthenticatedUser()
        .orElseThrow(() -> new BadRequestException(
            "Authenticated user cannot be resolved for scope validation",
            "AUTHENTICATED_USER_UNRESOLVED"
        ));
    Long userOrgId = authUser.getOrganizationId();
    if (userOrgId == null && authUser.getEmployerId() != null) {
      userOrgId = employerRepository.findById(authUser.getEmployerId())
          .map(Employer::getOrganizationId)
          .orElse(null);
    }
    if (userOrgId == null && authUser.getTrainingProviderId() != null) {
      userOrgId = trainingProviderRepository.findById(authUser.getTrainingProviderId())
          .map(TrainingProvider::getOrganizationId)
          .orElse(null);
    }
    if (userOrgId == null) {
      log.warn("Organization scope violation: authenticated user {} has no organization assignment", authUser.getId());
      throw new BadRequestException(
          "Authenticated user is not assigned to an organization",
          "ORGANIZATION_ASSIGNMENT_REQUIRED"
      );
    }
    if (!userOrgId.equals(targetOrganizationId)) {
      log.warn("Organization scope violation: authenticated user org={} attempted to access target org={}",
          userOrgId, targetOrganizationId);
      throw new BadRequestException(
          "Authenticated organization is not authorized to act on records of another organization",
          "ORGANIZATION_MISMATCH"
      );
    }

    if (!organizationRepository.existsById(targetOrganizationId)) {
      log.warn("Organization scope violation: target organization {} not found", targetOrganizationId);
      throw new BadRequestException(
          "Target organization record not found for scope validation",
          "ORGANIZATION_NOT_FOUND"
      );
    }
  }
}
