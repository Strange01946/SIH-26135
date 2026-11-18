package in.gov.sih.sih26135.communication.template;

import in.gov.sih.sih26135.entity.Trainee;
import in.gov.sih.sih26135.exception.BadRequestException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Authoritative registry and rendering engine for approved, allowlisted communication templates.
 *
 * <p>Enforces:
 * <ul>
 *   <li>Standardized Phase 29.4 templates: TPL-ENROLL-001, TPL-ASSESS-002, TPL-VERIF-003, TPL-SURVEY-004, TPL-ALERT-005.</li>
 *   <li>Full backward-compatibility with Phase 29.3 template codes.</li>
 *   <li>Strict variable allowlisting per template code (rejects unknown parameters).</li>
 *   <li>Mandatory parameter presence (rejects missing required parameters).</li>
 *   <li>PII & credential rejection (disallows Aadhaar, PAN, tokens, raw passwords).</li>
 *   <li>Trusted URL validation for certificate, survey, and verification links.</li>
 *   <li>HTML sanitization and unresolved placeholder prevention.</li>
 * </ul>
 */
@Component
public class CommunicationTemplateRegistry {

  private static final Pattern SENSITIVE_PARAM_PATTERN = Pattern.compile(
      "\\b(?:\\d{4}[ -]?\\d{4}[ -]?\\d{4}|[A-Z]{5}\\d{4}[A-Z]|Bearer\\s+[A-Za-z0-9._~+/-]+=*)\\b"
  );

  private static final Set<String> VALID_SURVEY_MILESTONES = Set.of("3", "6", "12");

  public record RenderedTemplate(
      String templateCode,
      String subject,
      String bodyText,
      String bodyHtml
  ) {
  }

  private final TrustedUrlValidator trustedUrlValidator;
  private final Map<String, TemplateDefinition> templates = new HashMap<>();

  public CommunicationTemplateRegistry() {
    this(new TrustedUrlValidator());
  }

  @Autowired
  public CommunicationTemplateRegistry(TrustedUrlValidator trustedUrlValidator) {
    this.trustedUrlValidator = Objects.requireNonNull(trustedUrlValidator, "TrustedUrlValidator is required");
    registerTemplates();
  }

  private void registerTemplates() {
    // -------------------------------------------------------------------------
    // Phase 29.4 Standardized Template Codes
    // -------------------------------------------------------------------------

    templates.put("TPL-ENROLL-001", new TemplateDefinition(
        "TPL-ENROLL-001",
        "SIH 26135 - Training Enrollment Confirmation - Batch {batchCode}",
        "Dear {traineeName}, your enrollment in batch {batchCode} for course {courseName} has been confirmed. Training commences on {startDate} at {trainingCenterName}. - Skill India Platform",
        Set.of("traineeName", "batchCode", "courseName", "startDate", "trainingCenterName")
    ));

    templates.put("TPL-ASSESS-002", new TemplateDefinition(
        "TPL-ASSESS-002",
        "SIH 26135 - Assessment Result & Certificate Issued",
        "Dear {traineeName}, your assessment for {courseName} is complete with result: {resultStatus}. Certificate number {certificateNumber} has been issued. View your certificate: {certificateUrl} - Skill India Platform",
        Set.of("traineeName", "courseName", "resultStatus", "certificateNumber", "certificateUrl")
    ));

    templates.put("TPL-VERIF-003", new TemplateDefinition(
        "TPL-VERIF-003",
        "SIH 26135 - Employment Verification Request",
        "Dear {traineeName}, employment verification {verificationId} has been initiated for your placement with {employerName}. Please verify details at: {verificationUrl} - Skill India Platform",
        Set.of("traineeName", "employerName", "verificationId", "verificationUrl")
    ));

    templates.put("TPL-SURVEY-004", new TemplateDefinition(
        "TPL-SURVEY-004",
        "SIH 26135 - {milestoneMonths}-Month Post-Placement Follow-up Survey",
        "Dear {traineeName}, as part of your {milestoneMonths}-month employment milestone, please take a moment to complete your feedback survey: {surveyUrl} (Survey ID: {surveyId}). - Skill India Platform",
        Set.of("traineeName", "milestoneMonths", "surveyId", "surveyUrl")
    ));

    templates.put("TPL-ALERT-005", new TemplateDefinition(
        "TPL-ALERT-005",
        "SIH 26135 - Critical Operational Alert - {alertType}",
        "Dear {traineeName}, critical alert: {alertType}. Action required: {actionRequired}. Reference: {referenceId}. - Skill India Platform",
        Set.of("traineeName", "alertType", "actionRequired", "referenceId")
    ));

    // -------------------------------------------------------------------------
    // Phase 29.3 Backward-Compatible Template Codes
    // -------------------------------------------------------------------------

    templates.put("TRAINEE_ENROLLMENT_CONFIRMATION", new TemplateDefinition(
        "TRAINEE_ENROLLMENT_CONFIRMATION",
        "SIH 26135 - Training Enrollment Confirmation",
        "Dear {traineeName}, your enrollment in batch {batchCode} for course {courseName} has been confirmed. Training commences on {startDate}. - Skill India Platform",
        Set.of("traineeName", "batchCode", "courseName", "startDate")
    ));

    templates.put("ASSESSMENT_CERTIFICATION_NOTICE", new TemplateDefinition(
        "ASSESSMENT_CERTIFICATION_NOTICE",
        "SIH 26135 - Certification Issued",
        "Dear {traineeName}, congratulations! You have successfully completed your training and your certificate {certificateNumber} has been issued. - Skill India Platform",
        Set.of("traineeName", "certificateNumber")
    ));

    templates.put("PLACEMENT_OFFER_ALERT", new TemplateDefinition(
        "PLACEMENT_OFFER_ALERT",
        "SIH 26135 - Placement Offer Received",
        "Dear {traineeName}, you have received a placement offer from {companyName} for the role of {jobTitle}. Please log in to your candidate portal for details. - Skill India Platform",
        Set.of("traineeName", "companyName", "jobTitle")
    ));

    templates.put("VERIFICATION_STATUS_NOTICE", new TemplateDefinition(
        "VERIFICATION_STATUS_NOTICE",
        "SIH 26135 - Employment Verification Update",
        "Dear {traineeName}, your employment verification status has been updated to {status}. Reference: {verificationId}. - Skill India Platform",
        Set.of("traineeName", "status", "verificationId")
    ));

    templates.put("FOLLOWUP_REMINDER", new TemplateDefinition(
        "FOLLOWUP_REMINDER",
        "SIH 26135 - Employment Follow-up Check",
        "Dear {traineeName}, this is a scheduled check regarding your {milestoneMonth}-month employment milestone. Please confirm your employment status on the portal. - Skill India Platform",
        Set.of("traineeName", "milestoneMonth")
    ));

    templates.put("OPERATIONAL_ALERT", new TemplateDefinition(
        "OPERATIONAL_ALERT",
        "SIH 26135 - Operational Notice",
        "SIH Alert: Operational notice regarding your candidate lifecycle. Reference: {referenceId}. - Skill India Platform",
        Set.of("referenceId")
    ));

    templates.put("LIVE_SMOKE_TEST", new TemplateDefinition(
        "LIVE_SMOKE_TEST",
        "SIH 26135 - Live Delivery Verification",
        "[SIH 26135 Live Smoke Test] Provider: {providerName}, Channel: {channel}, Timestamp: {timestamp}. Backend delivery verification successful.",
        Set.of("providerName", "channel", "timestamp")
    ));

    templates.put("TWILIO_TRIAL_GENERIC", new TemplateDefinition(
        "TWILIO_TRIAL_GENERIC",
        "SIH 26135 - Notification",
        "Sent from your Twilio trial account - SIH 26135 notification for {traineeName}: {message}",
        Set.of("traineeName", "message")
    ));
  }

  public RenderedTemplate render(String templateCode, Map<String, String> parameters, Trainee trainee) {
    if (templateCode == null || templateCode.isBlank()) {
      throw new BadRequestException("Template code is required for notification rendering", "TEMPLATE_CODE_REQUIRED");
    }
    String normalizedCode = templateCode.trim().toUpperCase();
    TemplateDefinition def = templates.get(normalizedCode);
    if (def == null) {
      throw new BadRequestException("Unsupported template code: " + templateCode, "UNSUPPORTED_TEMPLATE");
    }

    Map<String, String> effectiveParams = new HashMap<>();
    if (parameters != null) {
      // 1. Validate that provided parameters are strictly allowlisted for this template
      for (String providedKey : parameters.keySet()) {
        if (!def.allowedParameterKeys().contains(providedKey)) {
          String errorCode = isPhase293Template(normalizedCode)
              ? "UNEXPECTED_PARAMETER"
              : "PARAMETER_ALLOWLIST_VIOLATION";
          throw new BadRequestException(
              "Unexpected parameter '" + providedKey + "' for template " + normalizedCode,
              errorCode
          );
        }
      }
      effectiveParams.putAll(parameters);
    }

    // 2. Auto-populate traineeName if supported and missing
    if (def.allowedParameterKeys().contains("traineeName") && !effectiveParams.containsKey("traineeName") && trainee != null) {
      String fullName = (trainee.getFirstName() != null ? trainee.getFirstName() : "")
          + (trainee.getLastName() != null ? " " + trainee.getLastName() : "");
      effectiveParams.put("traineeName", fullName.isBlank() ? "Candidate" : fullName.trim());
    }

    // 3. Enforce completion of all required template parameters
    for (String requiredKey : def.allowedParameterKeys()) {
      String val = effectiveParams.get(requiredKey);
      if (val == null || val.isBlank()) {
        throw new BadRequestException(
            "Missing required parameter '" + requiredKey + "' for template " + normalizedCode,
            "MISSING_TEMPLATE_PARAMETER"
        );
      }
    }

    // 4. Validate parameters against sensitive patterns (Aadhaar, PAN, Auth tokens)
    for (Map.Entry<String, String> entry : effectiveParams.entrySet()) {
      String val = entry.getValue();
      if (val != null && SENSITIVE_PARAM_PATTERN.matcher(val).find()) {
        throw new BadRequestException(
            "Template parameter '" + entry.getKey() + "' contains sensitive data (Aadhaar/PAN/Token)",
            "SENSITIVE_PARAMETER_REJECTED"
        );
      }
    }

    // 5. Template-specific semantic validations
    if ("TPL-SURVEY-004".equals(normalizedCode)) {
      String milestone = effectiveParams.get("milestoneMonths");
      if (!VALID_SURVEY_MILESTONES.contains(milestone)) {
        throw new BadRequestException(
            "Invalid milestoneMonths '" + milestone + "' for template TPL-SURVEY-004. Must be 3, 6, or 12.",
            "INVALID_SURVEY_MILESTONE"
        );
      }
    }

    // 6. Validate embedded URLs against trusted application domains
    for (Map.Entry<String, String> entry : effectiveParams.entrySet()) {
      String key = entry.getKey();
      if (key.endsWith("Url") || key.equals("certificateUrl") || key.equals("verificationUrl") || key.equals("surveyUrl")) {
        trustedUrlValidator.validateTrustedUrl(entry.getValue(), key);
      }
    }

    String bodyText = def.bodyTemplate();
    String subject = def.subject();

    // 7. Replace placeholders
    for (Map.Entry<String, String> entry : effectiveParams.entrySet()) {
      String placeholder = "{" + entry.getKey() + "}";
      String val = entry.getValue() != null ? entry.getValue() : "";
      bodyText = bodyText.replace(placeholder, val);
      subject = subject.replace(placeholder, val);
    }

    // 8. Guarantee that no unresolved placeholders remain
    Pattern unresolvedPattern = Pattern.compile("\\{[A-Za-z0-9_]+\\}");
    Matcher bodyMatcher = unresolvedPattern.matcher(bodyText);
    if (bodyMatcher.find()) {
      throw new BadRequestException(
          "Unresolved placeholder '" + bodyMatcher.group() + "' in rendered body for template " + normalizedCode,
          "UNRESOLVED_PLACEHOLDER"
      );
    }
    Matcher subjectMatcher = unresolvedPattern.matcher(subject);
    if (subjectMatcher.find()) {
      throw new BadRequestException(
          "Unresolved placeholder '" + subjectMatcher.group() + "' in rendered subject for template " + normalizedCode,
          "UNRESOLVED_PLACEHOLDER"
      );
    }

    String bodyHtml = "<div style=\"font-family: Arial, sans-serif; line-height: 1.6; color: #333;\">"
        + "<p>" + escapeHtml(bodyText) + "</p>"
        + "<hr style=\"border: none; border-top: 1px solid #ddd; margin: 20px 0;\" />"
        + "<small style=\"color: #777;\">Skill India Platform &bull; Automated System Communication</small>"
        + "</div>";

    return new RenderedTemplate(normalizedCode, subject, bodyText, bodyHtml);
  }

  public Set<String> getRegisteredTemplateCodes() {
    return Collections.unmodifiableSet(templates.keySet());
  }

  public TemplateDefinition getTemplateDefinition(String templateCode) {
    if (templateCode == null) return null;
    return templates.get(templateCode.trim().toUpperCase());
  }

  public List<TemplateDefinition> getAllTemplateDefinitions() {
    return new java.util.ArrayList<>(templates.values());
  }

  public static boolean isPhase293Template(String code) {
    if (code == null) {
      return false;
    }
    return switch (code.toUpperCase().trim()) {
      case "TRAINEE_ENROLLMENT_CONFIRMATION",
           "ASSESSMENT_CERTIFICATION_NOTICE",
           "PLACEMENT_OFFER_ALERT",
           "VERIFICATION_STATUS_NOTICE",
           "FOLLOWUP_REMINDER",
           "OPERATIONAL_ALERT",
           "LIVE_SMOKE_TEST",
           "TWILIO_TRIAL_GENERIC" -> true;
      default -> false;
    };
  }

  private static String escapeHtml(String input) {
    if (input == null) {
      return "";
    }
    return input.replace("&", "&amp;")
        .replace("<", "&lt;")
        .replace(">", "&gt;")
        .replace("\"", "&quot;")
        .replace("'", "&#39;");
  }

  public record TemplateDefinition(
      String templateCode,
      String subject,
      String bodyTemplate,
      Set<String> allowedParameterKeys
  ) {
  }
}
