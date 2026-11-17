package in.gov.sih.sih26135.communication.model;

/**
 * Standardized communication channels supported by the SIH 26135 communication infrastructure.
 */
public enum CommunicationChannel {
  SMS("SMS", "COMMUNICATION_SMS"),
  WHATSAPP("WHATSAPP", "COMMUNICATION_WHATSAPP"),
  EMAIL("EMAIL", "COMMUNICATION_EMAIL"),
  PHONE("PHONE", "COMMUNICATION_PHONE");

  private final String channelCode;
  private final String requiredConsentCode;

  CommunicationChannel(String channelCode, String requiredConsentCode) {
    this.channelCode = channelCode;
    this.requiredConsentCode = requiredConsentCode;
  }

  public String getChannelCode() {
    return channelCode;
  }

  public String getRequiredConsentCode() {
    return requiredConsentCode;
  }

  public static CommunicationChannel fromCode(String code) {
    if (code == null || code.isBlank()) {
      return null;
    }
    for (CommunicationChannel channel : values()) {
      if (channel.channelCode.equalsIgnoreCase(code.trim()) || channel.name().equalsIgnoreCase(code.trim())) {
        return channel;
      }
    }
    return null;
  }
}
