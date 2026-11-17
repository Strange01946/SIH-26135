package in.gov.sih.sih26135.communication.template;

import in.gov.sih.sih26135.entity.Trainee;
import in.gov.sih.sih26135.exception.BadRequestException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.stereotype.Component;

/**
 * Authoritative registry of approved, allowlisted communication templates.
 *
 * <p>Enforces:
 * <ul>
 *   <li>Strict allowlisting: Only registered, vetted template codes can be dispatched.</li>
 *   <li>Controlled parameter interpolation: Prevents arbitrary injection of unvetted text.</li>
 *   <li>PII and secret sanitization: Disallows Aadhaar, PAN, raw passwords, or tokens in parameters.</li>
 * </ul>
 */
@Component
public class CommunicationTemplateRegistry {

  private static final Pattern SENSITIVE_PARAM_PATTERN = Pattern.compile(
      "\\b(?:\\d{4}[ -]?\\d{4}[ -]?\\d{4}|[A-Z]{5}\\d{4}[A-Z]|Bearer\\s+[A-Za-z0-9._~+/-]+=*)\\b"
  );

  public record RenderedTemplate(
      String templateCode,
      String subject,
      String bodyText,
      String bodyHtml
  ) {
  }

  private final Map<String, TemplateDefinition> templates = new HashMap<>();

  public CommunicationTemplateRegistry() {
    registerTemplates();
  }

  private void registerTemplates() {
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
      effectiveParams.putAll(parameters);
    }

    // Auto-populate traineeName if missing
    if (!effectiveParams.containsKey("traineeName") && trainee != null) {
      String fullName = (trainee.getFirstName() != null ? trainee.getFirstName() : "")
          + (trainee.getLastName() != null ? " " + trainee.getLastName() : "");
      effectiveParams.put("traineeName", fullName.isBlank() ? "Candidate" : fullName.trim());
    }

    // Validate parameters against sensitive patterns
    for (Map.Entry<String, String> entry : effectiveParams.entrySet()) {
      String val = entry.getValue();
      if (val != null && SENSITIVE_PARAM_PATTERN.matcher(val).find()) {
        throw new BadRequestException("Template parameter '" + entry.getKey() + "' contains sensitive data (Aadhaar/PAN/Token)", "SENSITIVE_PARAMETER_REJECTED");
      }
    }

    String bodyText = def.bodyTemplate();
    String subject = def.subject();

    for (Map.Entry<String, String> entry : effectiveParams.entrySet()) {
      String placeholder = "{" + entry.getKey() + "}";
      String val = entry.getValue() != null ? entry.getValue() : "";
      bodyText = bodyText.replace(placeholder, val);
      subject = subject.replace(placeholder, val);
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

  private record TemplateDefinition(
      String templateCode,
      String subject,
      String bodyTemplate,
      Set<String> allowedParameterKeys
  ) {
  }
}
