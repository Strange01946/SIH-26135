package in.gov.sih.sih26135.service.ingestion;

import in.gov.sih.sih26135.dto.request.CreateTraineeRequest;
import in.gov.sih.sih26135.entity.Trainee;
import in.gov.sih.sih26135.entity.User;
import in.gov.sih.sih26135.mapper.TraineeMapper;
import in.gov.sih.sih26135.repository.TraineeRepository;
import in.gov.sih.sih26135.repository.UserRepository;
import in.gov.sih.sih26135.service.ingestion.ImportRowFieldParser.RowValidationFailureException;
import in.gov.sih.sih26135.service.ingestion.ImportSecurityScopeValidator.CallerScope;
import in.gov.sih.sih26135.service.ingestion.RowOutcomeResult.ChunkSeenKeys;
import jakarta.validation.Validator;
import java.sql.Date;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * Validates, deduplicates, enforces tenant ownership, and persists single {@link Trainee} import rows.
 */
@Component
public class TraineeImportRowProcessor {

  private final TraineeRepository traineeRepository;
  private final UserRepository userRepository;
  private final TraineeMapper traineeMapper;
  private final JdbcTemplate jdbcTemplate;
  private final Validator validator;

  public TraineeImportRowProcessor(
      TraineeRepository traineeRepository,
      UserRepository userRepository,
      TraineeMapper traineeMapper,
      JdbcTemplate jdbcTemplate,
      Validator validator) {
    this.traineeRepository = Objects.requireNonNull(traineeRepository);
    this.userRepository = Objects.requireNonNull(userRepository);
    this.traineeMapper = Objects.requireNonNull(traineeMapper);
    this.jdbcTemplate = Objects.requireNonNull(jdbcTemplate);
    this.validator = Objects.requireNonNull(validator);
  }

  public RowOutcomeResult validateAndPersist(
      ParsedImportRow row,
      CallerScope callerScope,
      ChunkSeenKeys seenKeys) {
    Map<String, String> fields = row.fields();
    try {
      CreateTraineeRequest dto = parseAndValidateDto(fields);
      validateReferenceForeignKeys(dto);
      Long providerId = ImportRowFieldParser.optionalPositiveLong(fields, "providerId");
      Long centerId = ImportRowFieldParser.optionalPositiveLong(fields, "centerId");
      Long batchId = ImportRowFieldParser.optionalPositiveLong(fields, "batchId");

      validateTenantOwnership(callerScope, dto, providerId, centerId, batchId);

      RowOutcomeResult duplicateResult = checkDuplicateTrainee(dto, seenKeys);
      if (duplicateResult != null) {
        return duplicateResult;
      }

      User linkedUser = null;
      if (dto.getUserId() != null) {
        linkedUser = userRepository.findById(dto.getUserId())
            .filter(u -> u.getDeletedAt() == null)
            .orElseThrow(() -> new RowValidationFailureException("FOREIGN_KEY_NOT_FOUND",
                "Referenced userId does not exist or has been deleted"));
      }

      Trainee entity = traineeMapper.toEntity(dto, linkedUser);
      LocalDateTime now = LocalDateTime.now();
      entity.setCreatedAt(now);
      entity.setUpdatedAt(now);
      Trainee saved = traineeRepository.save(entity);

      recordIntraChunkSeenKeys(dto, seenKeys);
      return RowOutcomeResult.success(ImportContractSchema.ENTITY_TRAINEE, saved.getId());
    } catch (RowValidationFailureException ex) {
      return RowOutcomeResult.failed(ImportContractSchema.ENTITY_TRAINEE, ex.getErrorCode(), ex.getMessage());
    }
  }

  private CreateTraineeRequest parseAndValidateDto(Map<String, String> fields) {
    String registrationNumber = ImportRowFieldParser.requireNonBlank(fields, "registrationNumber", 32);
    String firstName = ImportRowFieldParser.requireNonBlank(fields, "firstName", 80);
    String middleName = ImportRowFieldParser.optionalTrimmed(fields, "middleName", 80);
    String lastName = ImportRowFieldParser.optionalTrimmed(fields, "lastName", 80);
    LocalDate dateOfBirth = ImportRowFieldParser.optionalDate(fields, "dateOfBirth");
    Long genderId = ImportRowFieldParser.requirePositiveLong(fields, "genderId");
    String email = ImportRowFieldParser.optionalTrimmed(fields, "email", 255);
    String phone = ImportRowFieldParser.optionalTrimmed(fields, "phone", 15);
    String addressLine1 = ImportRowFieldParser.optionalTrimmed(fields, "addressLine1", 200);
    String addressLine2 = ImportRowFieldParser.optionalTrimmed(fields, "addressLine2", 200);
    String pincode = ImportRowFieldParser.optionalTrimmed(fields, "pincode", 6);
    Long locationId = ImportRowFieldParser.optionalPositiveLong(fields, "locationId");
    Long stateId = ImportRowFieldParser.requirePositiveLong(fields, "stateId");
    Long districtId = ImportRowFieldParser.requirePositiveLong(fields, "districtId");
    Long blockId = ImportRowFieldParser.optionalPositiveLong(fields, "blockId");
    Long educationLevelId = ImportRowFieldParser.optionalPositiveLong(fields, "educationLevelId");
    Long currentEmploymentStatusId = ImportRowFieldParser.requirePositiveLong(fields, "currentEmploymentStatusId");
    Long profileStatusId = ImportRowFieldParser.requirePositiveLong(fields, "profileStatusId");
    Long userId = ImportRowFieldParser.optionalPositiveLong(fields, "userId");

    CreateTraineeRequest dto = new CreateTraineeRequest(
        userId, registrationNumber, firstName, middleName, lastName, dateOfBirth,
        genderId, email, phone, addressLine1, addressLine2, pincode,
        locationId, stateId, districtId, blockId, educationLevelId,
        currentEmploymentStatusId, profileStatusId
    );
    ImportRowFieldParser.runBeanValidation(validator, dto);
    return dto;
  }

  private void validateReferenceForeignKeys(CreateTraineeRequest dto) {
    if (!ImportRowFieldParser.existsActiveById(jdbcTemplate, "states", "state_id", dto.getStateId(), true)) {
      throw new RowValidationFailureException("FOREIGN_KEY_NOT_FOUND", "Referenced stateId does not exist");
    }
    Long districtStateId = ImportRowFieldParser.queryOptionalLong(
        jdbcTemplate,
        "SELECT state_id FROM districts WHERE district_id = ? AND deleted_at IS NULL",
        dto.getDistrictId());
    if (districtStateId == null) {
      throw new RowValidationFailureException("FOREIGN_KEY_NOT_FOUND", "Referenced districtId does not exist");
    }
    if (!districtStateId.equals(dto.getStateId())) {
      throw new RowValidationFailureException("GEOGRAPHIC_MISMATCH",
          "Referenced districtId does not belong to specified stateId");
    }

    if (dto.getBlockId() != null) {
      Long blockDistrictId = ImportRowFieldParser.queryOptionalLong(
          jdbcTemplate,
          "SELECT district_id FROM blocks WHERE block_id = ? AND deleted_at IS NULL",
          dto.getBlockId());
      if (blockDistrictId == null) {
        throw new RowValidationFailureException("FOREIGN_KEY_NOT_FOUND", "Referenced blockId does not exist");
      }
      if (!blockDistrictId.equals(dto.getDistrictId())) {
        throw new RowValidationFailureException("GEOGRAPHIC_MISMATCH",
            "Referenced blockId does not belong to specified districtId");
      }
    }

    if (dto.getLocationId() != null) {
      Long locDistrictId = ImportRowFieldParser.queryOptionalLong(
          jdbcTemplate,
          "SELECT district_id FROM locations WHERE location_id = ? AND deleted_at IS NULL",
          dto.getLocationId());
      if (locDistrictId == null) {
        throw new RowValidationFailureException("FOREIGN_KEY_NOT_FOUND", "Referenced locationId does not exist");
      }
      if (!locDistrictId.equals(dto.getDistrictId())) {
        throw new RowValidationFailureException("GEOGRAPHIC_MISMATCH",
            "Referenced locationId does not belong to specified districtId");
      }
    }

    if (!ImportRowFieldParser.existsActiveById(jdbcTemplate, "ref_gender", "gender_id", dto.getGenderId(), false)) {
      throw new RowValidationFailureException("FOREIGN_KEY_NOT_FOUND", "Referenced genderId does not exist");
    }
    if (!ImportRowFieldParser.existsActiveById(
        jdbcTemplate, "ref_employment_status", "employment_status_id", dto.getCurrentEmploymentStatusId(), false)) {
      throw new RowValidationFailureException("FOREIGN_KEY_NOT_FOUND",
          "Referenced currentEmploymentStatusId does not exist");
    }
    if (!ImportRowFieldParser.existsActiveById(
        jdbcTemplate, "ref_profile_status", "profile_status_id", dto.getProfileStatusId(), false)) {
      throw new RowValidationFailureException("FOREIGN_KEY_NOT_FOUND", "Referenced profileStatusId does not exist");
    }
    if (dto.getEducationLevelId() != null
        && !ImportRowFieldParser.existsActiveById(
            jdbcTemplate, "ref_education_level", "education_level_id", dto.getEducationLevelId(), false)) {
      throw new RowValidationFailureException("FOREIGN_KEY_NOT_FOUND", "Referenced educationLevelId does not exist");
    }
    if (dto.getUserId() != null
        && !ImportRowFieldParser.existsActiveById(jdbcTemplate, "users", "user_id", dto.getUserId(), true)) {
      throw new RowValidationFailureException("FOREIGN_KEY_NOT_FOUND", "Referenced userId does not exist");
    }
  }

  private void validateTenantOwnership(
      CallerScope scope,
      CreateTraineeRequest dto,
      Long providerId,
      Long centerId,
      Long batchId) {
    Long providerStateId = null;
    if (providerId != null) {
      providerStateId = ImportRowFieldParser.queryOptionalLong(
          jdbcTemplate,
          "SELECT state_id FROM training_providers WHERE provider_id = ? AND deleted_at IS NULL",
          providerId);
      if (providerStateId == null) {
        throw new RowValidationFailureException("FOREIGN_KEY_NOT_FOUND", "Referenced providerId does not exist");
      }
    }

    Long centerProviderId = null;
    Long centerStateId = null;
    Long centerDistrictId = null;
    if (centerId != null) {
      List<Map<String, Object>> centerRows = jdbcTemplate.queryForList(
          "SELECT provider_id, state_id, district_id FROM training_centers WHERE center_id = ? AND deleted_at IS NULL",
          centerId);
      if (centerRows.isEmpty()) {
        throw new RowValidationFailureException("FOREIGN_KEY_NOT_FOUND", "Referenced centerId does not exist");
      }
      Map<String, Object> cRow = centerRows.get(0);
      centerProviderId = ((Number) cRow.get("provider_id")).longValue();
      centerStateId = ((Number) cRow.get("state_id")).longValue();
      centerDistrictId = ((Number) cRow.get("district_id")).longValue();
      if (providerId != null && !centerProviderId.equals(providerId)) {
        throw new RowValidationFailureException("GEOGRAPHIC_MISMATCH",
            "Referenced centerId does not belong to specified providerId");
      }
    }

    Long batchProviderId = null;
    Long batchCenterId = null;
    if (batchId != null) {
      List<Map<String, Object>> batchRows = jdbcTemplate.queryForList(
          "SELECT provider_id, center_id FROM training_batches WHERE batch_id = ? AND deleted_at IS NULL",
          batchId);
      if (batchRows.isEmpty()) {
        throw new RowValidationFailureException("FOREIGN_KEY_NOT_FOUND", "Referenced batchId does not exist");
      }
      Map<String, Object> bRow = batchRows.get(0);
      batchProviderId = ((Number) bRow.get("provider_id")).longValue();
      batchCenterId = ((Number) bRow.get("center_id")).longValue();
      if (providerId != null && !batchProviderId.equals(providerId)) {
        throw new RowValidationFailureException("GEOGRAPHIC_MISMATCH",
            "Referenced batchId does not belong to specified providerId");
      }
      if (centerId != null && !batchCenterId.equals(centerId)) {
        throw new RowValidationFailureException("GEOGRAPHIC_MISMATCH",
            "Referenced batchId does not belong to specified centerId");
      }
      if (centerId == null) {
        List<Map<String, Object>> bCenterRows = jdbcTemplate.queryForList(
            "SELECT state_id, district_id FROM training_centers WHERE center_id = ? AND deleted_at IS NULL",
            batchCenterId);
        if (!bCenterRows.isEmpty()) {
          centerStateId = ((Number) bCenterRows.get(0).get("state_id")).longValue();
          centerDistrictId = ((Number) bCenterRows.get(0).get("district_id")).longValue();
        }
      }
    }

    if (scope.nationwideAdmin()) {
      return;
    }

    if (scope.isProviderScoped()) {
      Long callerProviderId = scope.trainingProviderId();
      if (providerId != null && !providerId.equals(callerProviderId)) {
        throw new RowValidationFailureException("CROSS_TENANT_REFERENCE",
            "Trainee row references a training provider outside caller tenant scope");
      }
      if (centerProviderId != null && !centerProviderId.equals(callerProviderId)) {
        throw new RowValidationFailureException("CROSS_TENANT_REFERENCE",
            "Trainee row references a training center outside caller provider scope");
      }
      if (batchProviderId != null && !batchProviderId.equals(callerProviderId)) {
        throw new RowValidationFailureException("CROSS_TENANT_REFERENCE",
            "Trainee row references a training batch outside caller provider scope");
      }
      if (scope.providerStateId() != null && !dto.getStateId().equals(scope.providerStateId())) {
        throw new RowValidationFailureException("CROSS_TENANT_REFERENCE",
            "Trainee row stateId does not match caller training provider state");
      }
      return;
    }

    if (scope.isStateOrDistrictScoped()) {
      if (!dto.getStateId().equals(scope.stateId())) {
        throw new RowValidationFailureException("CROSS_TENANT_REFERENCE",
            "Trainee row stateId is outside caller state scope");
      }
      if (scope.districtId() != null && !dto.getDistrictId().equals(scope.districtId())) {
        throw new RowValidationFailureException("CROSS_TENANT_REFERENCE",
            "Trainee row districtId is outside caller district scope");
      }
      if (providerStateId != null && !providerStateId.equals(scope.stateId())) {
        throw new RowValidationFailureException("CROSS_TENANT_REFERENCE",
            "Referenced provider belongs to a state outside caller scope");
      }
      if (centerStateId != null && !centerStateId.equals(scope.stateId())) {
        throw new RowValidationFailureException("CROSS_TENANT_REFERENCE",
            "Referenced center belongs to a state outside caller scope");
      }
      if (scope.districtId() != null && centerDistrictId != null && !centerDistrictId.equals(scope.districtId())) {
        throw new RowValidationFailureException("CROSS_TENANT_REFERENCE",
            "Referenced center/batch belongs to a district outside caller district scope");
      }
      return;
    }

    throw new RowValidationFailureException("CROSS_TENANT_REFERENCE",
        "Caller is not authorized to persist trainee record in target tenant");
  }

  private RowOutcomeResult checkDuplicateTrainee(CreateTraineeRequest dto, ChunkSeenKeys seenKeys) {
    String normalizedReg = dto.getRegistrationNumber().toLowerCase(Locale.ROOT);
    if (seenKeys.traineeRegNumbers.contains(normalizedReg)
        || traineeRepository.existsByRegistrationNumber(dto.getRegistrationNumber())) {
      return RowOutcomeResult.duplicate(
          ImportContractSchema.ENTITY_TRAINEE,
          "DUPLICATE_TRAINEE_REGISTRATION",
          "Trainee registration number already exists in database or earlier in batch");
    }

    if (dto.getUserId() != null) {
      if (seenKeys.traineeUserIds.contains(dto.getUserId())
          || traineeRepository.existsByUserId(dto.getUserId())) {
        return RowOutcomeResult.duplicate(
            ImportContractSchema.ENTITY_TRAINEE,
            "DUPLICATE_TRAINEE_USER",
            "Trainee userId is already linked to an existing trainee record");
      }
    }

    List<String> identitySignals = buildIdentitySignals(dto);
    for (String sig : identitySignals) {
      if (seenKeys.traineeIdentitySignals.contains(sig)) {
        return RowOutcomeResult.duplicate(
            ImportContractSchema.ENTITY_TRAINEE,
            "DUPLICATE_TRAINEE_CANDIDATE",
            "Trainee matches an earlier record in this import by deterministic identity signals");
      }
    }

    if (existsMatchingTraineeByIdentitySignals(dto)) {
      return RowOutcomeResult.duplicate(
          ImportContractSchema.ENTITY_TRAINEE,
          "DUPLICATE_TRAINEE_CANDIDATE",
          "Trainee matches an existing active trainee by deterministic identity signals");
    }

    return null;
  }

  private boolean existsMatchingTraineeByIdentitySignals(CreateTraineeRequest dto) {
    String phone = dto.getPhone();
    String email = dto.getEmail() != null ? dto.getEmail().toLowerCase(Locale.ROOT) : null;
    LocalDate dob = dto.getDateOfBirth();
    String firstName = dto.getFirstName();
    String lastName = dto.getLastName() != null ? dto.getLastName() : "";

    if ((phone == null || phone.isBlank()) && (email == null || email.isBlank())) {
      return false;
    }

    List<String> predicates = new ArrayList<>();
    List<Object> params = new ArrayList<>();
    if (phone != null && !phone.isBlank()) {
      if (dob != null) {
        predicates.add("(t.phone = ? AND t.date_of_birth = ?)");
        params.add(phone);
        params.add(Date.valueOf(dob));
      }
      if (firstName != null && !firstName.isBlank()) {
        predicates.add("(t.phone = ? AND LOWER(t.first_name) = ? AND LOWER(COALESCE(t.last_name, '')) = ?)");
        params.add(phone);
        params.add(firstName.toLowerCase(Locale.ROOT));
        params.add(lastName.toLowerCase(Locale.ROOT));
      }
    }
    if (email != null && !email.isBlank()) {
      if (dob != null) {
        predicates.add("(LOWER(t.email) = ? AND t.date_of_birth = ?)");
        params.add(email);
        params.add(Date.valueOf(dob));
      }
      if (firstName != null && !firstName.isBlank()) {
        predicates.add("(LOWER(t.email) = ? AND LOWER(t.first_name) = ? AND LOWER(COALESCE(t.last_name, '')) = ?)");
        params.add(email);
        params.add(firstName.toLowerCase(Locale.ROOT));
        params.add(lastName.toLowerCase(Locale.ROOT));
      }
    }
    if (phone != null && !phone.isBlank() && email != null && !email.isBlank()) {
      predicates.add("(t.phone = ? AND LOWER(t.email) = ?)");
      params.add(phone);
      params.add(email);
    }

    if (predicates.isEmpty()) {
      return false;
    }

    String sql = "SELECT COUNT(*) FROM trainees t WHERE t.deleted_at IS NULL AND ("
        + String.join(" OR ", predicates) + ")";
    Integer count = jdbcTemplate.queryForObject(sql, Integer.class, params.toArray());
    return count != null && count > 0;
  }

  private void recordIntraChunkSeenKeys(CreateTraineeRequest dto, ChunkSeenKeys seenKeys) {
    seenKeys.traineeRegNumbers.add(dto.getRegistrationNumber().toLowerCase(Locale.ROOT));
    if (dto.getUserId() != null) {
      seenKeys.traineeUserIds.add(dto.getUserId());
    }
    seenKeys.traineeIdentitySignals.addAll(buildIdentitySignals(dto));
  }

  private List<String> buildIdentitySignals(CreateTraineeRequest dto) {
    List<String> signals = new ArrayList<>();
    String phone = dto.getPhone();
    String email = dto.getEmail() != null ? dto.getEmail().toLowerCase(Locale.ROOT) : null;
    LocalDate dob = dto.getDateOfBirth();
    String fullName = (dto.getFirstName() != null ? dto.getFirstName().toLowerCase(Locale.ROOT) : "")
        + "|" + (dto.getLastName() != null ? dto.getLastName().toLowerCase(Locale.ROOT) : "");

    if (phone != null && !phone.isBlank()) {
      if (dob != null) {
        signals.add("P_DOB:" + phone + "|" + dob);
      }
      signals.add("P_NAME:" + phone + "|" + fullName);
    }
    if (email != null && !email.isBlank()) {
      if (dob != null) {
        signals.add("E_DOB:" + email + "|" + dob);
      }
      signals.add("E_NAME:" + email + "|" + fullName);
    }
    if (phone != null && !phone.isBlank() && email != null && !email.isBlank()) {
      signals.add("PE:" + phone + "|" + email);
    }
    return signals;
  }
}
