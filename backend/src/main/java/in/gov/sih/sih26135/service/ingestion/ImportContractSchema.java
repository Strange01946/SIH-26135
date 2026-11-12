package in.gov.sih.sih26135.service.ingestion;

import static java.util.Map.entry;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Authoritative field normalization and contract allowlist definitions for Phase 28.5 bulk imports
 * targeting {@code Trainee} and {@code PlacementRecord}.
 */
public final class ImportContractSchema {

  public static final String ENTITY_TRAINEE = "Trainee";
  public static final String ENTITY_PLACEMENT_RECORD = "PlacementRecord";

  private static final Map<String, String> TRAINEE_FIELDS = Map.ofEntries(
      entry("userid", "userId"),
      entry("registrationnumber", "registrationNumber"),
      entry("firstname", "firstName"),
      entry("middlename", "middleName"),
      entry("lastname", "lastName"),
      entry("dateofbirth", "dateOfBirth"),
      entry("genderid", "genderId"),
      entry("email", "email"),
      entry("phone", "phone"),
      entry("addressline1", "addressLine1"),
      entry("addressline2", "addressLine2"),
      entry("pincode", "pincode"),
      entry("locationid", "locationId"),
      entry("stateid", "stateId"),
      entry("districtid", "districtId"),
      entry("blockid", "blockId"),
      entry("educationlevelid", "educationLevelId"),
      entry("currentemploymentstatusid", "currentEmploymentStatusId"),
      entry("profilestatusid", "profileStatusId"),
      // Optional provider/center/batch context fields validated against caller tenancy
      entry("providerid", "providerId"),
      entry("centerid", "centerId"),
      entry("batchid", "batchId")
  );

  private static final List<String> TRAINEE_REQUIRED_FIELDS = List.of(
      "registrationNumber",
      "firstName",
      "genderId",
      "stateId",
      "districtId",
      "currentEmploymentStatusId",
      "profileStatusId"
  );

  private static final Map<String, String> PLACEMENT_FIELDS = Map.ofEntries(
      entry("placementnumber", "placementNumber"),
      entry("traineeid", "traineeId"),
      entry("enrollmentid", "enrollmentId"),
      entry("courseid", "courseId"),
      entry("programid", "programId"),
      entry("providerid", "providerId"),
      entry("employerid", "employerId"),
      entry("employerbranchid", "employerBranchId"),
      entry("jobpostingid", "jobPostingId"),
      entry("jobapplicationid", "jobApplicationId"),
      entry("jobroleid", "jobRoleId"),
      entry("engagementtypeid", "engagementTypeId"),
      entry("placementsourceid", "placementSourceId"),
      entry("placementstatusid", "placementStatusId"),
      entry("joiningstatusid", "joiningStatusId"),
      entry("offeredsalary", "offeredSalary"),
      entry("joiningsalary", "joiningSalary"),
      entry("salaryfrequencyid", "salaryFrequencyId"),
      entry("currencycode", "currencyCode"),
      entry("offerdate", "offerDate"),
      entry("expectedjoiningdate", "expectedJoiningDate"),
      entry("actualjoiningdate", "actualJoiningDate"),
      entry("nonselectionreasonid", "nonSelectionReasonId"),
      entry("outcomeremarks", "outcomeRemarks"),
      entry("workstateid", "workStateId"),
      entry("workdistrictid", "workDistrictId"),
      entry("recordverificationstatusid", "recordVerificationStatusId")
  );

  private static final List<String> PLACEMENT_REQUIRED_FIELDS = List.of(
      "placementNumber",
      "enrollmentId",
      "placementSourceId",
      "placementStatusId",
      "joiningStatusId",
      "recordVerificationStatusId"
  );

  private ImportContractSchema() {
  }

  public static String normalizeFieldName(String entityType, String rawFieldName) {
    if (rawFieldName == null || rawFieldName.isBlank()) {
      return null;
    }
    String compact = rawFieldName.trim()
        .replace("_", "")
        .replace("-", "")
        .replace(" ", "")
        .toLowerCase(Locale.ROOT);
    Map<String, String> allowed = getAllowedFieldsMap(entityType);
    return allowed != null ? allowed.get(compact) : null;
  }

  public static boolean isFieldSupported(String entityType, String rawFieldName) {
    return normalizeFieldName(entityType, rawFieldName) != null;
  }

  public static List<String> getMissingRequiredHeaders(String entityType, Set<String> canonicalHeaders) {
    List<String> required = getRequiredFields(entityType);
    return required.stream()
        .filter(req -> !canonicalHeaders.contains(req))
        .toList();
  }

  public static List<String> getRequiredFields(String entityType) {
    if (ENTITY_TRAINEE.equals(entityType)) {
      return TRAINEE_REQUIRED_FIELDS;
    }
    if (ENTITY_PLACEMENT_RECORD.equals(entityType)) {
      return PLACEMENT_REQUIRED_FIELDS;
    }
    return List.of();
  }

  private static Map<String, String> getAllowedFieldsMap(String entityType) {
    if (ENTITY_TRAINEE.equals(entityType)) {
      return TRAINEE_FIELDS;
    }
    if (ENTITY_PLACEMENT_RECORD.equals(entityType)) {
      return PLACEMENT_FIELDS;
    }
    return null;
  }
}
