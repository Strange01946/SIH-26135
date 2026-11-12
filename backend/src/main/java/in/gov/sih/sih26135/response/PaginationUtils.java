package in.gov.sih.sih26135.response;

import static java.util.Map.entry;

import in.gov.sih.sih26135.exception.BadRequestException;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

/**
 * Utility for parsing, validating, and building Spring Data {@link Pageable} instances.
 * Enforces pagination limits, parameter bounds, sorting direction, and sortable field allowlists.
 */
public final class PaginationUtils {

  public static final int DEFAULT_PAGE = 0;
  public static final int DEFAULT_SIZE = 20;
  public static final int MIN_PAGE = 0;
  public static final int MIN_SIZE = 1;
  public static final int MAX_SIZE = 100;
  public static final String DEFAULT_DIRECTION = "ASC";

  // =========================================================================
  // Core Domain Entity Sort Field Allowlists (Phase 26.3)
  // =========================================================================

  public static final Map<String, String> TRAINEE_SORT_FIELDS = Map.ofEntries(
      entry("id", "id"),
      entry("registrationnumber", "registrationNumber"),
      entry("registrationNumber", "registrationNumber"),
      entry("registration_number", "registrationNumber"),
      entry("firstname", "firstName"),
      entry("firstName", "firstName"),
      entry("first_name", "firstName"),
      entry("lastname", "lastName"),
      entry("lastName", "lastName"),
      entry("last_name", "lastName"),
      entry("dateofbirth", "dateOfBirth"),
      entry("dateOfBirth", "dateOfBirth"),
      entry("date_of_birth", "dateOfBirth"),
      entry("email", "email"),
      entry("createdat", "createdAt"),
      entry("createdAt", "createdAt"),
      entry("created_at", "createdAt"),
      entry("updatedat", "updatedAt"),
      entry("updatedAt", "updatedAt"),
      entry("updated_at", "updatedAt")
  );

  public static final Map<String, String> EMPLOYER_SORT_FIELDS = Map.ofEntries(
      entry("id", "id"),
      entry("code", "employerCode"),
      entry("employercode", "employerCode"),
      entry("employerCode", "employerCode"),
      entry("employer_code", "employerCode"),
      entry("name", "employerName"),
      entry("employername", "employerName"),
      entry("employerName", "employerName"),
      entry("employer_name", "employerName"),
      entry("registrationnumber", "registrationNumber"),
      entry("registrationNumber", "registrationNumber"),
      entry("registration_number", "registrationNumber"),
      entry("gstin", "gstin"),
      entry("createdat", "createdAt"),
      entry("createdAt", "createdAt"),
      entry("created_at", "createdAt"),
      entry("updatedat", "updatedAt"),
      entry("updatedAt", "updatedAt"),
      entry("updated_at", "updatedAt")
  );

  public static final Map<String, String> COURSE_SORT_FIELDS = Map.ofEntries(
      entry("id", "id"),
      entry("code", "courseCode"),
      entry("coursecode", "courseCode"),
      entry("courseCode", "courseCode"),
      entry("course_code", "courseCode"),
      entry("name", "courseName"),
      entry("coursename", "courseName"),
      entry("courseName", "courseName"),
      entry("course_name", "courseName"),
      entry("durationhours", "durationHours"),
      entry("durationHours", "durationHours"),
      entry("duration_hours", "durationHours"),
      entry("durationdays", "durationDays"),
      entry("durationDays", "durationDays"),
      entry("duration_days", "durationDays"),
      entry("createdat", "createdAt"),
      entry("createdAt", "createdAt"),
      entry("created_at", "createdAt"),
      entry("updatedat", "updatedAt"),
      entry("updatedAt", "updatedAt"),
      entry("updated_at", "updatedAt")
  );

  public static final Map<String, String> TRAINING_CENTER_SORT_FIELDS = Map.ofEntries(
      entry("id", "id"),
      entry("code", "centerCode"),
      entry("centercode", "centerCode"),
      entry("centerCode", "centerCode"),
      entry("center_code", "centerCode"),
      entry("name", "centerName"),
      entry("centername", "centerName"),
      entry("centerName", "centerName"),
      entry("center_name", "centerName"),
      entry("operationalcapacity", "operationalCapacity"),
      entry("operationalCapacity", "operationalCapacity"),
      entry("operational_capacity", "operationalCapacity"),
      entry("createdat", "createdAt"),
      entry("createdAt", "createdAt"),
      entry("created_at", "createdAt"),
      entry("updatedat", "updatedAt"),
      entry("updatedAt", "updatedAt"),
      entry("updated_at", "updatedAt")
  );

  public static final Map<String, String> TRAINING_PROVIDER_SORT_FIELDS = Map.ofEntries(
      entry("id", "id"),
      entry("code", "providerCode"),
      entry("providercode", "providerCode"),
      entry("providerCode", "providerCode"),
      entry("provider_code", "providerCode"),
      entry("name", "providerName"),
      entry("providername", "providerName"),
      entry("providerName", "providerName"),
      entry("provider_name", "providerName"),
      entry("registrationnumber", "registrationNumber"),
      entry("registrationNumber", "registrationNumber"),
      entry("registration_number", "registrationNumber"),
      entry("rating", "rating"),
      entry("createdat", "createdAt"),
      entry("createdAt", "createdAt"),
      entry("created_at", "createdAt"),
      entry("updatedat", "updatedAt"),
      entry("updatedAt", "updatedAt"),
      entry("updated_at", "updatedAt")
  );

  public static final Map<String, String> TRAINING_BATCH_SORT_FIELDS = Map.ofEntries(
      entry("id", "id"),
      entry("code", "batchCode"),
      entry("batchcode", "batchCode"),
      entry("batchCode", "batchCode"),
      entry("batch_code", "batchCode"),
      entry("startdate", "startDate"),
      entry("startDate", "startDate"),
      entry("start_date", "startDate"),
      entry("enddate", "endDate"),
      entry("endDate", "endDate"),
      entry("end_date", "endDate"),
      entry("capacity", "capacity"),
      entry("createdat", "createdAt"),
      entry("createdAt", "createdAt"),
      entry("created_at", "createdAt"),
      entry("updatedat", "updatedAt"),
      entry("updatedAt", "updatedAt"),
      entry("updated_at", "updatedAt")
  );

  public static final Map<String, String> TRAINING_ENROLLMENT_SORT_FIELDS = Map.ofEntries(
      entry("id", "id"),
      entry("enrollmentnumber", "enrollmentNumber"),
      entry("enrollmentNumber", "enrollmentNumber"),
      entry("enrollment_number", "enrollmentNumber"),
      entry("enrollmentdate", "enrollmentDate"),
      entry("enrollmentDate", "enrollmentDate"),
      entry("enrollment_date", "enrollmentDate"),
      entry("startdate", "startDate"),
      entry("startDate", "startDate"),
      entry("start_date", "startDate"),
      entry("expectedcompletiondate", "expectedCompletionDate"),
      entry("expectedCompletionDate", "expectedCompletionDate"),
      entry("expected_completion_date", "expectedCompletionDate"),
      entry("actualcompletiondate", "actualCompletionDate"),
      entry("actualCompletionDate", "actualCompletionDate"),
      entry("actual_completion_date", "actualCompletionDate"),
      entry("createdat", "createdAt"),
      entry("createdAt", "createdAt"),
      entry("created_at", "createdAt"),
      entry("updatedat", "updatedAt"),
      entry("updatedAt", "updatedAt"),
      entry("updated_at", "updatedAt")
  );

  // =========================================================================
  // Operational, Log & Event Entity Sort Field Allowlists (Phase 26.4)
  // =========================================================================

  public static final Map<String, String> EMPLOYMENT_RECORD_SORT_FIELDS = Map.ofEntries(
      entry("id", "id"),
      entry("employmentnumber", "employmentNumber"),
      entry("employmentNumber", "employmentNumber"),
      entry("employment_number", "employmentNumber"),
      entry("number", "employmentNumber"),
      entry("startdate", "startDate"),
      entry("startDate", "startDate"),
      entry("start_date", "startDate"),
      entry("enddate", "endDate"),
      entry("endDate", "endDate"),
      entry("end_date", "endDate"),
      entry("iscurrent", "isCurrent"),
      entry("isCurrent", "isCurrent"),
      entry("is_current", "isCurrent"),
      entry("startingsalary", "startingSalary"),
      entry("startingSalary", "startingSalary"),
      entry("starting_salary", "startingSalary"),
      entry("salary", "startingSalary"),
      entry("createdat", "createdAt"),
      entry("createdAt", "createdAt"),
      entry("created_at", "createdAt"),
      entry("updatedat", "updatedAt"),
      entry("updatedAt", "updatedAt"),
      entry("updated_at", "updatedAt")
  );

  public static final Map<String, String> PLACEMENT_RECORD_SORT_FIELDS = Map.ofEntries(
      entry("id", "id"),
      entry("placementnumber", "placementNumber"),
      entry("placementNumber", "placementNumber"),
      entry("placement_number", "placementNumber"),
      entry("number", "placementNumber"),
      entry("offerdate", "offerDate"),
      entry("offerDate", "offerDate"),
      entry("offer_date", "offerDate"),
      entry("expectedjoiningdate", "expectedJoiningDate"),
      entry("expectedJoiningDate", "expectedJoiningDate"),
      entry("expected_joining_date", "expectedJoiningDate"),
      entry("actualjoiningdate", "actualJoiningDate"),
      entry("actualJoiningDate", "actualJoiningDate"),
      entry("actual_joining_date", "actualJoiningDate"),
      entry("offeredsalary", "offeredSalary"),
      entry("offeredSalary", "offeredSalary"),
      entry("offered_salary", "offeredSalary"),
      entry("joiningsalary", "joiningSalary"),
      entry("joiningSalary", "joiningSalary"),
      entry("joining_salary", "joiningSalary"),
      entry("salary", "joiningSalary"),
      entry("verifiedat", "verifiedAt"),
      entry("verifiedAt", "verifiedAt"),
      entry("verified_at", "verifiedAt"),
      entry("createdat", "createdAt"),
      entry("createdAt", "createdAt"),
      entry("created_at", "createdAt"),
      entry("updatedat", "updatedAt"),
      entry("updatedAt", "updatedAt"),
      entry("updated_at", "updatedAt")
  );

  public static final Map<String, String> EMPLOYMENT_VERIFICATION_REQUEST_SORT_FIELDS = Map.ofEntries(
      entry("id", "id"),
      entry("requestnumber", "requestNumber"),
      entry("requestNumber", "requestNumber"),
      entry("request_number", "requestNumber"),
      entry("number", "requestNumber"),
      entry("cyclenumber", "cycleNumber"),
      entry("cycleNumber", "cycleNumber"),
      entry("cycle_number", "cycleNumber"),
      entry("cycle", "cycleNumber"),
      entry("isreverification", "isReverification"),
      entry("isReverification", "isReverification"),
      entry("is_reverification", "isReverification"),
      entry("requestedat", "requestedAt"),
      entry("requestedAt", "requestedAt"),
      entry("requested_at", "requestedAt"),
      entry("dueat", "dueAt"),
      entry("dueAt", "dueAt"),
      entry("due_at", "dueAt"),
      entry("firstattemptat", "firstAttemptAt"),
      entry("firstAttemptAt", "firstAttemptAt"),
      entry("first_attempt_at", "firstAttemptAt"),
      entry("completedat", "completedAt"),
      entry("completedAt", "completedAt"),
      entry("completed_at", "completedAt"),
      entry("createdat", "createdAt"),
      entry("createdAt", "createdAt"),
      entry("created_at", "createdAt"),
      entry("updatedat", "updatedAt"),
      entry("updatedAt", "updatedAt"),
      entry("updated_at", "updatedAt")
  );

  public static final Map<String, String> EMPLOYMENT_VERIFICATION_ATTEMPT_SORT_FIELDS = Map.ofEntries(
      entry("id", "id"),
      entry("attemptnumber", "attemptNumber"),
      entry("attemptNumber", "attemptNumber"),
      entry("attempt_number", "attemptNumber"),
      entry("attempt", "attemptNumber"),
      entry("attemptedat", "attemptedAt"),
      entry("attemptedAt", "attemptedAt"),
      entry("attempted_at", "attemptedAt"),
      entry("completedat", "completedAt"),
      entry("completedAt", "completedAt"),
      entry("completed_at", "completedAt"),
      entry("createdat", "createdAt"),
      entry("createdAt", "createdAt"),
      entry("created_at", "createdAt"),
      entry("updatedat", "updatedAt"),
      entry("updatedAt", "updatedAt"),
      entry("updated_at", "updatedAt")
  );

  public static final Map<String, String> SURVEY_RESPONSE_SORT_FIELDS = Map.ofEntries(
      entry("id", "id"),
      entry("surveyid", "surveyId"),
      entry("surveyId", "surveyId"),
      entry("survey_id", "surveyId"),
      entry("surveytemplateversionid", "surveyTemplateVersionId"),
      entry("surveyTemplateVersionId", "surveyTemplateVersionId"),
      entry("survey_template_version_id", "surveyTemplateVersionId"),
      entry("attemptnumber", "attemptNumber"),
      entry("attemptNumber", "attemptNumber"),
      entry("attempt_number", "attemptNumber"),
      entry("attempt", "attemptNumber"),
      entry("surveyresponsestatusid", "surveyResponseStatusId"),
      entry("surveyResponseStatusId", "surveyResponseStatusId"),
      entry("survey_response_status_id", "surveyResponseStatusId"),
      entry("startedat", "startedAt"),
      entry("startedAt", "startedAt"),
      entry("started_at", "startedAt"),
      entry("submittedat", "submittedAt"),
      entry("submittedAt", "submittedAt"),
      entry("submitted_at", "submittedAt"),
      entry("createdat", "createdAt"),
      entry("createdAt", "createdAt"),
      entry("created_at", "createdAt"),
      entry("updatedat", "updatedAt"),
      entry("updatedAt", "updatedAt"),
      entry("updated_at", "updatedAt")
  );

  public static final Map<String, String> FOLLOWUP_TASK_SORT_FIELDS = Map.ofEntries(
      entry("id", "id"),
      entry("scheduleddate", "scheduledDate"),
      entry("scheduledDate", "scheduledDate"),
      entry("scheduled_date", "scheduledDate"),
      entry("nextfollowupdate", "nextFollowupDate"),
      entry("nextFollowupDate", "nextFollowupDate"),
      entry("next_followup_date", "nextFollowupDate"),
      entry("followupstatusid", "followupStatusId"),
      entry("followupStatusId", "followupStatusId"),
      entry("followup_status_id", "followupStatusId"),
      entry("followupoutcomeid", "followupOutcomeId"),
      entry("followupOutcomeId", "followupOutcomeId"),
      entry("followup_outcome_id", "followupOutcomeId"),
      entry("assigneduserid", "assignedUserId"),
      entry("assignedUserId", "assignedUserId"),
      entry("assigned_user_id", "assignedUserId"),
      entry("completedat", "completedAt"),
      entry("completedAt", "completedAt"),
      entry("completed_at", "completedAt"),
      entry("createdat", "createdAt"),
      entry("createdAt", "createdAt"),
      entry("created_at", "createdAt"),
      entry("updatedat", "updatedAt"),
      entry("updatedAt", "updatedAt"),
      entry("updated_at", "updatedAt")
  );

  public static final Map<String, String> COMMUNICATION_LOG_SORT_FIELDS = Map.ofEntries(
      entry("id", "id"),
      entry("providermessageid", "providerMessageId"),
      entry("providerMessageId", "providerMessageId"),
      entry("provider_message_id", "providerMessageId"),
      entry("messagetemplatecode", "messageTemplateCode"),
      entry("messageTemplateCode", "messageTemplateCode"),
      entry("message_template_code", "messageTemplateCode"),
      entry("queuedat", "queuedAt"),
      entry("queuedAt", "queuedAt"),
      entry("queued_at", "queuedAt"),
      entry("sentat", "sentAt"),
      entry("sentAt", "sentAt"),
      entry("sent_at", "sentAt"),
      entry("deliveredat", "deliveredAt"),
      entry("deliveredAt", "deliveredAt"),
      entry("delivered_at", "deliveredAt"),
      entry("readat", "readAt"),
      entry("readAt", "readAt"),
      entry("read_at", "readAt"),
      entry("failedat", "failedAt"),
      entry("failedAt", "failedAt"),
      entry("failed_at", "failedAt"),
      entry("createdat", "createdAt"),
      entry("createdAt", "createdAt"),
      entry("created_at", "createdAt"),
      entry("updatedat", "updatedAt"),
      entry("updatedAt", "updatedAt"),
      entry("updated_at", "updatedAt")
  );

  public static final Map<String, String> AUDIT_LOG_SORT_FIELDS = Map.ofEntries(
      entry("id", "id"),
      entry("actoruserid", "actorUserId"),
      entry("actorUserId", "actorUserId"),
      entry("actor_user_id", "actorUserId"),
      entry("entitytype", "entityType"),
      entry("entityType", "entityType"),
      entry("entity_type", "entityType"),
      entry("entityid", "entityId"),
      entry("entityId", "entityId"),
      entry("entity_id", "entityId"),
      entry("correlationid", "correlationId"),
      entry("correlationId", "correlationId"),
      entry("correlation_id", "correlationId"),
      entry("occurredat", "occurredAt"),
      entry("occurredAt", "occurredAt"),
      entry("occurred_at", "occurredAt"),
      entry("createdat", "createdAt"),
      entry("createdAt", "createdAt"),
      entry("created_at", "createdAt")
  );

  public static final Map<String, String> SYSTEM_EVENT_LOG_SORT_FIELDS = Map.ofEntries(
      entry("id", "id"),
      entry("eventcode", "eventCode"),
      entry("eventCode", "eventCode"),
      entry("event_code", "eventCode"),
      entry("code", "eventCode"),
      entry("actoruserid", "actorUserId"),
      entry("actorUserId", "actorUserId"),
      entry("actor_user_id", "actorUserId"),
      entry("entitytype", "entityType"),
      entry("entityType", "entityType"),
      entry("entity_type", "entityType"),
      entry("entityid", "entityId"),
      entry("entityId", "entityId"),
      entry("entity_id", "entityId"),
      entry("sourcecomponent", "sourceComponent"),
      entry("sourceComponent", "sourceComponent"),
      entry("source_component", "sourceComponent"),
      entry("occurredat", "occurredAt"),
      entry("occurredAt", "occurredAt"),
      entry("occurred_at", "occurredAt"),
      entry("createdat", "createdAt"),
      entry("createdAt", "createdAt"),
      entry("created_at", "createdAt")
  );

  // =========================================================================
  // Analytical Fact Entity Sort Field Allowlists (Phase 27.12)
  // =========================================================================

  public static final Map<String, String> EMPLOYMENT_EXIT_FACT_SORT_FIELDS = Map.ofEntries(
      entry("id", "employmentExitEventId"),
      entry("employmentexiteventid", "employmentExitEventId"),
      entry("employmentExitEventId", "employmentExitEventId"),
      entry("employment_exit_event_id", "employmentExitEventId"),
      entry("traineeid", "traineeId"),
      entry("traineeId", "traineeId"),
      entry("trainee_id", "traineeId"),
      entry("employmentid", "employmentId"),
      entry("employmentId", "employmentId"),
      entry("employment_id", "employmentId"),
      entry("exitreason", "exitReason"),
      entry("exitReason", "exitReason"),
      entry("exit_reason", "exitReason"),
      entry("exitdate", "exitDate"),
      entry("exitDate", "exitDate"),
      entry("exit_date", "exitDate"),
      entry("unemploymentdurationdays", "unemploymentDurationDays"),
      entry("unemploymentDurationDays", "unemploymentDurationDays"),
      entry("unemployment_duration_days", "unemploymentDurationDays"),
      entry("settlementstatus", "settlementStatus"),
      entry("settlementStatus", "settlementStatus"),
      entry("settlement_status", "settlementStatus")
  );

  public static final Map<String, String> EMPLOYMENT_FACT_SORT_FIELDS = Map.ofEntries(
      entry("id", "employmentId"),
      entry("employmentid", "employmentId"),
      entry("employmentId", "employmentId"),
      entry("employment_id", "employmentId"),
      entry("traineeid", "traineeId"),
      entry("traineeId", "traineeId"),
      entry("trainee_id", "traineeId"),
      entry("employerid", "employerId"),
      entry("employerId", "employerId"),
      entry("employer_id", "employerId"),
      entry("jobrole", "jobRole"),
      entry("jobRole", "jobRole"),
      entry("job_role", "jobRole"),
      entry("employmenttype", "employmentType"),
      entry("employmentType", "employmentType"),
      entry("employment_type", "employmentType"),
      entry("employmentstatus", "employmentStatus"),
      entry("employmentStatus", "employmentStatus"),
      entry("employment_status", "employmentStatus"),
      entry("monthlyinr", "monthlyInr"),
      entry("monthlyInr", "monthlyInr"),
      entry("monthly_inr", "monthlyInr"),
      entry("startdate", "startDate"),
      entry("startDate", "startDate"),
      entry("start_date", "startDate"),
      entry("enddate", "endDate"),
      entry("endDate", "endDate"),
      entry("end_date", "endDate"),
      entry("verificationstatus", "verificationStatus"),
      entry("verificationStatus", "verificationStatus"),
      entry("verification_status", "verificationStatus"),
      entry("retentionriskscore", "retentionRiskScore"),
      entry("retentionRiskScore", "retentionRiskScore"),
      entry("retention_risk_score", "retentionRiskScore")
  );

  public static final Map<String, String> EMPLOYMENT_RETENTION_FACT_SORT_FIELDS = Map.ofEntries(
      entry("id", "employmentId"),
      entry("employmentid", "employmentId"),
      entry("employmentId", "employmentId"),
      entry("employment_id", "employmentId"),
      entry("traineeid", "traineeId"),
      entry("traineeId", "traineeId"),
      entry("trainee_id", "traineeId"),
      entry("employerid", "employerId"),
      entry("employerId", "employerId"),
      entry("employer_id", "employerId"),
      entry("startdate", "startDate"),
      entry("startDate", "startDate"),
      entry("start_date", "startDate"),
      entry("retentionmonths", "retentionMonths"),
      entry("retentionMonths", "retentionMonths"),
      entry("retention_months", "retentionMonths"),
      entry("currentstatus", "currentStatus"),
      entry("currentStatus", "currentStatus"),
      entry("current_status", "currentStatus"),
      entry("attritionriskfactor", "attritionRiskFactor"),
      entry("attritionRiskFactor", "attritionRiskFactor"),
      entry("attrition_risk_factor", "attritionRiskFactor")
  );

  public static final Map<String, String> EMPLOYMENT_VERIFICATION_FACT_SORT_FIELDS = Map.ofEntries(
      entry("id", "employmentVerificationId"),
      entry("employmentverificationid", "employmentVerificationId"),
      entry("employmentVerificationId", "employmentVerificationId"),
      entry("employment_verification_id", "employmentVerificationId"),
      entry("employmentid", "employmentId"),
      entry("employmentId", "employmentId"),
      entry("employment_id", "employmentId"),
      entry("traineeid", "traineeId"),
      entry("traineeId", "traineeId"),
      entry("trainee_id", "traineeId"),
      entry("employerid", "employerId"),
      entry("employerId", "employerId"),
      entry("employer_id", "employerId"),
      entry("verificationmethod", "verificationMethod"),
      entry("verificationMethod", "verificationMethod"),
      entry("verification_method", "verificationMethod"),
      entry("verificationstatus", "verificationStatus"),
      entry("verificationStatus", "verificationStatus"),
      entry("verification_status", "verificationStatus"),
      entry("verifiedat", "verifiedAt"),
      entry("verifiedAt", "verifiedAt"),
      entry("verified_at", "verifiedAt"),
      entry("confidencescore", "confidenceScore"),
      entry("confidenceScore", "confidenceScore"),
      entry("confidence_score", "confidenceScore"),
      entry("iscurrent", "isCurrent"),
      entry("isCurrent", "isCurrent"),
      entry("is_current", "isCurrent")
  );

  public static final Map<String, String> ENROLLMENT_OUTCOME_FACT_SORT_FIELDS = Map.ofEntries(
      entry("id", "enrollmentId"),
      entry("enrollmentid", "enrollmentId"),
      entry("enrollmentId", "enrollmentId"),
      entry("enrollment_id", "enrollmentId"),
      entry("traineeid", "traineeId"),
      entry("traineeId", "traineeId"),
      entry("trainee_id", "traineeId"),
      entry("courseid", "courseId"),
      entry("courseId", "courseId"),
      entry("course_id", "courseId"),
      entry("centerid", "centerId"),
      entry("centerId", "centerId"),
      entry("center_id", "centerId"),
      entry("schemeid", "schemeId"),
      entry("schemeId", "schemeId"),
      entry("scheme_id", "schemeId"),
      entry("enrollmentstatus", "enrollmentStatus"),
      entry("enrollmentStatus", "enrollmentStatus"),
      entry("enrollment_status", "enrollmentStatus"),
      entry("completionstatus", "completionStatus"),
      entry("completionStatus", "completionStatus"),
      entry("completion_status", "completionStatus"),
      entry("certificationstatus", "certificationStatus"),
      entry("certificationStatus", "certificationStatus"),
      entry("certification_status", "certificationStatus"),
      entry("placementstatus", "placementStatus"),
      entry("placementStatus", "placementStatus"),
      entry("placement_status", "placementStatus"),
      entry("attendancepercentage", "attendancePercentage"),
      entry("attendancePercentage", "attendancePercentage"),
      entry("attendance_percentage", "attendancePercentage"),
      entry("completiondate", "completionDate"),
      entry("completionDate", "completionDate"),
      entry("completion_date", "completionDate"),
      entry("certificationdate", "certificationDate"),
      entry("certificationDate", "certificationDate"),
      entry("certification_date", "certificationDate"),
      entry("firstplacementdate", "firstPlacementDate"),
      entry("firstPlacementDate", "firstPlacementDate"),
      entry("first_placement_date", "firstPlacementDate")
  );

  public static final Map<String, String> FOLLOWUP_FACT_SORT_FIELDS = Map.ofEntries(
      entry("id", "followupTaskId"),
      entry("followuptaskid", "followupTaskId"),
      entry("followupTaskId", "followupTaskId"),
      entry("followup_task_id", "followupTaskId"),
      entry("traineeid", "traineeId"),
      entry("traineeId", "traineeId"),
      entry("trainee_id", "traineeId"),
      entry("followupwave", "followupWave"),
      entry("followupWave", "followupWave"),
      entry("followup_wave", "followupWave"),
      entry("scheduleddate", "scheduledDate"),
      entry("scheduledDate", "scheduledDate"),
      entry("scheduled_date", "scheduledDate"),
      entry("completeddate", "completedDate"),
      entry("completedDate", "completedDate"),
      entry("completed_date", "completedDate"),
      entry("taskstatus", "taskStatus"),
      entry("taskStatus", "taskStatus"),
      entry("task_status", "taskStatus"),
      entry("contactchannel", "contactChannel"),
      entry("contactChannel", "contactChannel"),
      entry("contact_channel", "contactChannel"),
      entry("responsestatus", "responseStatus"),
      entry("responseStatus", "responseStatus"),
      entry("response_status", "responseStatus"),
      entry("slameta", "slaMet"),
      entry("slaMet", "slaMet"),
      entry("sla_met", "slaMet"),
      entry("slabreachdays", "slaBreachDays"),
      entry("slaBreachDays", "slaBreachDays"),
      entry("sla_breach_days", "slaBreachDays")
  );

  public static final Map<String, String> PLACEMENT_FACT_SORT_FIELDS = Map.ofEntries(
      entry("id", "placementId"),
      entry("placementid", "placementId"),
      entry("placementId", "placementId"),
      entry("placement_id", "placementId"),
      entry("traineeid", "traineeId"),
      entry("traineeId", "traineeId"),
      entry("trainee_id", "traineeId"),
      entry("employerid", "employerId"),
      entry("employerId", "employerId"),
      entry("employer_id", "employerId"),
      entry("jobrole", "jobRole"),
      entry("jobRole", "jobRole"),
      entry("job_role", "jobRole"),
      entry("monthlyinr", "monthlyInr"),
      entry("monthlyInr", "monthlyInr"),
      entry("monthly_inr", "monthlyInr"),
      entry("placementdate", "placementDate"),
      entry("placementDate", "placementDate"),
      entry("placement_date", "placementDate"),
      entry("placementtype", "placementType"),
      entry("placementType", "placementType"),
      entry("placement_type", "placementType"),
      entry("placementstatus", "placementStatus"),
      entry("placementStatus", "placementStatus"),
      entry("placement_status", "placementStatus"),
      entry("statecode", "stateCode"),
      entry("stateCode", "stateCode"),
      entry("state_code", "stateCode"),
      entry("districtcode", "districtCode"),
      entry("districtCode", "districtCode"),
      entry("district_code", "districtCode")
  );

  public static final Map<String, String> SALARY_PROGRESSION_FACT_SORT_FIELDS = Map.ofEntries(
      entry("id", "employmentId"),
      entry("employmentid", "employmentId"),
      entry("employmentId", "employmentId"),
      entry("employment_id", "employmentId"),
      entry("traineeid", "traineeId"),
      entry("traineeId", "traineeId"),
      entry("trainee_id", "traineeId"),
      entry("employerid", "employerId"),
      entry("employerId", "employerId"),
      entry("employer_id", "employerId"),
      entry("initialsalaryinr", "initialSalaryInr"),
      entry("initialSalaryInr", "initialSalaryInr"),
      entry("initial_salary_inr", "initialSalaryInr"),
      entry("currentsalaryinr", "currentSalaryInr"),
      entry("currentSalaryInr", "currentSalaryInr"),
      entry("current_salary_inr", "currentSalaryInr"),
      entry("salaryhikepercentage", "salaryHikePercentage"),
      entry("salaryHikePercentage", "salaryHikePercentage"),
      entry("salary_hike_percentage", "salaryHikePercentage"),
      entry("salarytier", "salaryTier"),
      entry("salaryTier", "salaryTier"),
      entry("salary_tier", "salaryTier"),
      entry("recordedat", "recordedAt"),
      entry("recordedAt", "recordedAt"),
      entry("recorded_at", "recordedAt"),
      entry("experiencemonths", "experienceMonths"),
      entry("experienceMonths", "experienceMonths"),
      entry("experience_months", "experienceMonths")
  );

  public static final Map<String, String> SKILL_GAP_FACT_SORT_FIELDS = Map.ofEntries(
      entry("id", "skillGapId"),
      entry("skillgapid", "skillGapId"),
      entry("skillGapId", "skillGapId"),
      entry("skill_gap_id", "skillGapId"),
      entry("traineeid", "traineeId"),
      entry("traineeId", "traineeId"),
      entry("trainee_id", "traineeId"),
      entry("courseid", "courseId"),
      entry("courseId", "courseId"),
      entry("course_id", "courseId"),
      entry("skillname", "skillName"),
      entry("skillName", "skillName"),
      entry("skill_name", "skillName"),
      entry("requiredproficiency", "requiredProficiency"),
      entry("requiredProficiency", "requiredProficiency"),
      entry("required_proficiency", "requiredProficiency"),
      entry("assessedproficiency", "assessedProficiency"),
      entry("assessedProficiency", "assessedProficiency"),
      entry("assessed_proficiency", "assessedProficiency"),
      entry("gapseverity", "gapSeverity"),
      entry("gapSeverity", "gapSeverity"),
      entry("gap_severity", "gapSeverity"),
      entry("remediationrecommended", "remediationRecommended"),
      entry("remediationRecommended", "remediationRecommended"),
      entry("remediation_recommended", "remediationRecommended"),
      entry("assessmentdate", "assessmentDate"),
      entry("assessmentDate", "assessmentDate"),
      entry("assessment_date", "assessmentDate")
  );

  public static final Map<String, String> SURVEY_RESPONSE_FACT_SORT_FIELDS = Map.ofEntries(
      entry("id", "surveyResponseId"),
      entry("surveyresponseid", "surveyResponseId"),
      entry("surveyResponseId", "surveyResponseId"),
      entry("survey_response_id", "surveyResponseId"),
      entry("traineeid", "traineeId"),
      entry("traineeId", "traineeId"),
      entry("trainee_id", "traineeId"),
      entry("surveytype", "surveyType"),
      entry("surveyType", "surveyType"),
      entry("survey_type", "surveyType"),
      entry("responsedate", "responseDate"),
      entry("responseDate", "responseDate"),
      entry("response_date", "responseDate"),
      entry("overallsatisfactionscore", "overallSatisfactionScore"),
      entry("overallSatisfactionScore", "overallSatisfactionScore"),
      entry("overall_satisfaction_score", "overallSatisfactionScore"),
      entry("npscategory", "npsCategory"),
      entry("npsCategory", "npsCategory"),
      entry("nps_category", "npsCategory")
  );

  public static final Map<String, String> UNEMPLOYMENT_FACT_SORT_FIELDS = Map.ofEntries(
      entry("id", "unemploymentEventId"),
      entry("unemploymenteventid", "unemploymentEventId"),
      entry("unemploymentEventId", "unemploymentEventId"),
      entry("unemployment_event_id", "unemploymentEventId"),
      entry("traineeid", "traineeId"),
      entry("traineeId", "traineeId"),
      entry("trainee_id", "traineeId"),
      entry("exiteventid", "exitEventId"),
      entry("exitEventId", "exitEventId"),
      entry("exit_event_id", "exitEventId"),
      entry("unemploymentstartdate", "unemploymentStartDate"),
      entry("unemploymentStartDate", "unemploymentStartDate"),
      entry("unemployment_start_date", "unemploymentStartDate"),
      entry("unemploymentenddate", "unemploymentEndDate"),
      entry("unemploymentEndDate", "unemploymentEndDate"),
      entry("unemployment_end_date", "unemploymentEndDate"),
      entry("durationdays", "durationDays"),
      entry("durationDays", "durationDays"),
      entry("duration_days", "durationDays"),
      entry("resolutiontype", "resolutionType"),
      entry("resolutionType", "resolutionType"),
      entry("resolution_type", "resolutionType"),
      entry("allowanceclaimed", "allowanceClaimed"),
      entry("allowanceClaimed", "allowanceClaimed"),
      entry("allowance_claimed", "allowanceClaimed")
  );

  public static final Map<String, String> IMPORT_RECORD_SORT_FIELDS = Map.ofEntries(
      entry("id", "id"),
      entry("sourcerownumber", "sourceRowNumber"),
      entry("sourceRowNumber", "sourceRowNumber"),
      entry("source_row_number", "sourceRowNumber"),
      entry("entitytype", "entityType"),
      entry("entityType", "entityType"),
      entry("entity_type", "entityType"),
      entry("entityid", "entityId"),
      entry("entityId", "entityId"),
      entry("entity_id", "entityId"),
      entry("errorcode", "errorCode"),
      entry("errorCode", "errorCode"),
      entry("error_code", "errorCode"),
      entry("createdat", "createdAt"),
      entry("createdAt", "createdAt"),
      entry("created_at", "createdAt"),
      entry("updatedat", "updatedAt"),
      entry("updatedAt", "updatedAt"),
      entry("updated_at", "updatedAt")
  );

  public static final int MAX_SORT_FIELD_LENGTH = 64;
  public static final int MAX_SORT_DIRECTION_LENGTH = 16;

  private static final Pattern SAFE_SORT_PROPERTY_PATTERN = Pattern.compile("^[a-zA-Z0-9_-]{1,64}$");
  private static final Pattern SAFE_SORT_DIRECTION_PATTERN = Pattern.compile("^[a-zA-Z]{1,16}$");

  private static final Set<String> SENSITIVE_SORT_TERMS = Set.of(
      "password", "passwordhash", "passwordalgo", "passwd", "pwd",
      "secret", "token", "accesstoken", "refreshtoken", "authtoken",
      "apikey", "credential", "credentials",
      "failedlogincount", "lockeduntil", "mustchangepassword",
      "aadhaarvaultref", "aadhaar", "panvaultref",
      "bankaccount", "bankaccountnumber", "ifsc", "accountnumber",
      "deletedat", "isdeleted", "softdeleted", "deletedby",
      "hash", "salt", "privatekey", "secretkey", "otp"
  );

  /**
   * Checks whether the given sort field name contains sensitive terms or internal attributes.
   *
   * @param sort the sort property to check
   * @return true if sensitive, false otherwise
   */
  public static boolean isSensitiveSortField(String sort) {
    if (sort == null || sort.isBlank()) {
      return false;
    }
    String normalized = sort.toLowerCase().replaceAll("[^a-z0-9]", "");
    for (String term : SENSITIVE_SORT_TERMS) {
      if (normalized.contains(term)) {
        return true;
      }
    }
    return false;
  }

  private PaginationUtils() {
    // Utility class
  }

  /**
   * Validates pagination parameters and builds a Pageable instance with stable sorting.
   *
   * @param page zero-based page index
   * @param size number of records per page
   * @param sort field to sort on (optional, uses default if null/blank)
   * @param direction sorting direction ("ASC" or "DESC", case-insensitive)
   * @param allowedSortFields map of allowed lowercase sort query keys to entity property names
   * @param defaultSortProperty default property to sort by when sort parameter is omitted
   * @return validated {@link Pageable}
   * @throws BadRequestException if any parameter is out of bounds or unsupported
   */
  public static Pageable buildPageable(
      int page,
      int size,
      String sort,
      String direction,
      Map<String, String> allowedSortFields,
      String defaultSortProperty) {
    return buildPageable(
        page, size, sort, direction, allowedSortFields, defaultSortProperty,
        Sort.Direction.ASC, Sort.Direction.ASC
    );
  }

  /**
   * Validates pagination parameters and builds a Pageable instance with configurable default direction and tie-breaker.
   *
   * @param page zero-based page index
   * @param size number of records per page
   * @param sort field to sort on (optional, uses default if null/blank)
   * @param direction sorting direction ("ASC" or "DESC", case-insensitive)
   * @param allowedSortFields map of allowed lowercase sort query keys to entity property names
   * @param defaultSortProperty default property to sort by when sort parameter is omitted
   * @param defaultDirection default sort direction when direction parameter is omitted
   * @return validated {@link Pageable}
   * @throws BadRequestException if any parameter is out of bounds or unsupported
   */
  public static Pageable buildPageable(
      int page,
      int size,
      String sort,
      String direction,
      Map<String, String> allowedSortFields,
      String defaultSortProperty,
      Sort.Direction defaultDirection) {
    Sort.Direction tieBreakerDir = (defaultDirection == Sort.Direction.DESC) ? Sort.Direction.DESC : Sort.Direction.ASC;
    return buildPageable(
        page, size, sort, direction, allowedSortFields, defaultSortProperty,
        defaultDirection, tieBreakerDir
    );
  }

  /**
   * Validates pagination parameters and builds a Pageable instance with explicit default direction and tie-breaker.
   *
   * @param page zero-based page index
   * @param size number of records per page
   * @param sort field to sort on (optional, uses default if null/blank)
   * @param direction sorting direction ("ASC" or "DESC", case-insensitive)
   * @param allowedSortFields map of allowed lowercase sort query keys to entity property names
   * @param defaultSortProperty default property to sort by when sort parameter is omitted
   * @param defaultDirection default sort direction when direction parameter is omitted
   * @param tieBreakerDirection direction for secondary id tie-breaker
   * @return validated {@link Pageable}
   * @throws BadRequestException if any parameter is out of bounds or unsupported
   */
  public static Pageable buildPageable(
      int page,
      int size,
      String sort,
      String direction,
      Map<String, String> allowedSortFields,
      String defaultSortProperty,
      Sort.Direction defaultDirection,
      Sort.Direction tieBreakerDirection) {
    return buildPageable(
        page,
        size,
        sort,
        direction,
        allowedSortFields,
        defaultSortProperty,
        "id",
        defaultDirection,
        tieBreakerDirection
    );
  }

  /**
   * Validates pagination parameters and builds a Pageable instance with explicit default direction,
   * custom primary key identifier property, and tie-breaker.
   *
   * @param page zero-based page index
   * @param size number of records per page
   * @param sort field to sort on (optional, uses default if null/blank)
   * @param direction sorting direction ("ASC" or "DESC", case-insensitive)
   * @param allowedSortFields map of allowed lowercase sort query keys to entity property names
   * @param defaultSortProperty default property to sort by when sort parameter is omitted
   * @param idProperty entity's authoritative unique identifier property for deterministic tie-breaking
   * @param defaultDirection default sort direction when direction parameter is omitted
   * @param tieBreakerDirection direction for secondary id tie-breaker
   * @return validated {@link Pageable}
   * @throws BadRequestException if any parameter is out of bounds or unsupported
   */
  public static Pageable buildPageable(
      int page,
      int size,
      String sort,
      String direction,
      Map<String, String> allowedSortFields,
      String defaultSortProperty,
      String idProperty,
      Sort.Direction defaultDirection,
      Sort.Direction tieBreakerDirection) {

    if (page < MIN_PAGE) {
      throw new BadRequestException("Page index must not be less than zero", "INVALID_PAGE");
    }

    if (size < MIN_SIZE) {
      throw new BadRequestException("Page size must be at least 1", "INVALID_PAGE_SIZE");
    }

    if (size > MAX_SIZE) {
      throw new BadRequestException("Page size must not exceed " + MAX_SIZE, "INVALID_PAGE_SIZE");
    }

    Sort.Direction sortDirection = parseDirection(direction, defaultDirection);

    String resolvedProperty = defaultSortProperty;
    if (sort != null && !sort.isBlank()) {
      String trimmed = sort.trim();
      if (trimmed.length() > MAX_SORT_FIELD_LENGTH
          || !SAFE_SORT_PROPERTY_PATTERN.matcher(trimmed).matches()
          || isSensitiveSortField(trimmed)) {
        throw new BadRequestException("Invalid sort field provided", "INVALID_SORT_FIELD");
      }
      String normalizedKey = trimmed.toLowerCase();
      if (allowedSortFields.containsKey(trimmed)) {
        resolvedProperty = allowedSortFields.get(trimmed);
      } else if (allowedSortFields.containsKey(normalizedKey)) {
        resolvedProperty = allowedSortFields.get(normalizedKey);
      } else {
        throw new BadRequestException(
            "Unsupported sort field '" + sort + "'. Supported fields: " + getCleanAllowedFields(allowedSortFields),
            "INVALID_SORT_FIELD"
        );
      }
    }

    Sort primarySort = Sort.by(sortDirection, resolvedProperty);
    // Add stable tie-breaker on authoritative primary key if not already sorting by it
    String effectiveIdProp = (idProperty != null && !idProperty.isBlank()) ? idProperty : "id";
    Sort.Direction effectiveTieBreakerDir = tieBreakerDirection != null ? tieBreakerDirection : Sort.Direction.ASC;
    Sort finalSort = effectiveIdProp.equals(resolvedProperty)
        ? primarySort
        : primarySort.and(Sort.by(effectiveTieBreakerDir, effectiveIdProp));

    return PageRequest.of(page, size, finalSort);
  }

  private static Sort.Direction parseDirection(String direction, Sort.Direction defaultDirection) {
    if (direction == null || direction.isBlank()) {
      return defaultDirection != null ? defaultDirection : Sort.Direction.ASC;
    }
    String normalized = direction.trim().toUpperCase();
    if ("ASC".equals(normalized)) {
      return Sort.Direction.ASC;
    }
    if ("DESC".equals(normalized)) {
      return Sort.Direction.DESC;
    }
    String trimmed = direction.trim();
    if (trimmed.length() > MAX_SORT_DIRECTION_LENGTH
        || !SAFE_SORT_DIRECTION_PATTERN.matcher(trimmed).matches()
        || isSensitiveSortField(trimmed)) {
      throw new BadRequestException("Invalid sort direction provided", "INVALID_SORT_DIRECTION");
    }
    throw new BadRequestException(
        "Invalid sort direction '" + direction + "'. Must be ASC or DESC",
        "INVALID_SORT_DIRECTION"
    );
  }

  private static Set<String> getCleanAllowedFields(Map<String, String> allowedSortFields) {
    return Collections.unmodifiableSet(allowedSortFields.keySet());
  }
}
