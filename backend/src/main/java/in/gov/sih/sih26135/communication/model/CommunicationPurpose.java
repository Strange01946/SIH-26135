package in.gov.sih.sih26135.communication.model;

/**
 * Standardized communication purposes for outbound candidate notifications.
 */
public enum CommunicationPurpose {
  SURVEY_INVITE("SURVEY_INVITE", "EMPLOYMENT_FOLLOW_UP"),
  FOLLOWUP_REMINDER("FOLLOWUP_REMINDER", "EMPLOYMENT_FOLLOW_UP"),
  EMPLOYMENT_CHECK("EMPLOYMENT_CHECK", "EMPLOYMENT_FOLLOW_UP"),
  WAGE_CHECK("WAGE_CHECK", "EMPLOYMENT_FOLLOW_UP"),
  RETENTION_CHECK("RETENTION_CHECK", "EMPLOYMENT_FOLLOW_UP"),
  OPERATIONAL("OPERATIONAL", null),
  PLACEMENT_ALERT("PLACEMENT_ALERT", null),
  VERIFICATION_NOTICE("VERIFICATION_NOTICE", null);

  private final String purposeCode;
  private final String requiredPurposeConsentCode;

  CommunicationPurpose(String purposeCode, String requiredPurposeConsentCode) {
    this.purposeCode = purposeCode;
    this.requiredPurposeConsentCode = requiredPurposeConsentCode;
  }

  public String getPurposeCode() {
    return purposeCode;
  }

  public String getRequiredPurposeConsentCode() {
    return requiredPurposeConsentCode;
  }

  public static CommunicationPurpose fromCode(String code) {
    if (code == null || code.isBlank()) {
      return OPERATIONAL;
    }
    for (CommunicationPurpose p : values()) {
      if (p.purposeCode.equalsIgnoreCase(code.trim()) || p.name().equalsIgnoreCase(code.trim())) {
        return p;
      }
    }
    return OPERATIONAL;
  }
}
