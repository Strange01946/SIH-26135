package in.gov.sih.sih26135.communication.config;

import jakarta.annotation.PostConstruct;
import java.util.Arrays;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.EnvironmentAware;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

/**
 * Externalized configuration properties for Phase 29.3 Communication Infrastructure,
 * covering Twilio SMS, Twilio WhatsApp, SendGrid Email, and Gmail SMTP delivery.
 *
 * <p>Enforces trusted delivery mode routing (DEMO vs LIVE) and includes a fail-closed
 * production guard that prohibits DEMO or MOCK execution in production environments.
 */
@Component
@ConfigurationProperties(prefix = "communication")
public class CommunicationProperties implements EnvironmentAware {

  public enum ProviderMode {
    DEMO,
    MOCK,
    LIVE
  }

  public enum EmailProviderPreference {
    AUTO,
    SENDGRID,
    SMTP
  }

  private ProviderMode mode = ProviderMode.MOCK;
  private boolean liveRecipientOverrideEnabled = true;
  private EmailProviderPreference emailProviderType = EmailProviderPreference.AUTO;
  private Environment environment;

  private TwilioConfig twilio = new TwilioConfig();
  private SendGridConfig sendgrid = new SendGridConfig();
  private SmtpConfig smtp = new SmtpConfig();

  @Override
  public void setEnvironment(Environment environment) {
    this.environment = environment;
  }

  @PostConstruct
  public void validateConfiguration() {
    validateProductionSafety(this.environment);
  }

  /**
   * Validates that DEMO or MOCK modes are never executed in production environments.
   *
   * @param env Spring environment abstraction
   * @throws IllegalStateException if DEMO/MOCK mode is enabled against production
   */
  public void validateProductionSafety(Environment env) {
    if (!isDemoMode()) {
      return;
    }
    if (env == null) {
      return;
    }

    boolean isProdProfile = env.getActiveProfiles() != null && Arrays.stream(env.getActiveProfiles())
        .anyMatch(p -> p.equalsIgnoreCase("prod") || p.equalsIgnoreCase("production"));

    String appEnv = env.getProperty("APP_ENVIRONMENT", env.getProperty("app.environment", ""));
    boolean isProdEnv = "prod".equalsIgnoreCase(appEnv) || "production".equalsIgnoreCase(appEnv);

    String dbUrl = env.getProperty("spring.datasource.url", "");
    String dbName = env.getProperty("DB_NAME", "");
    boolean isProdCatalog = (dbUrl.contains("/SIH26135") && !dbUrl.contains("/SIH26135_TEST"))
        || ("SIH26135".equalsIgnoreCase(dbName) && !dbUrl.contains("SIH26135_TEST"));

    if (isProdProfile || isProdEnv || isProdCatalog) {
      throw new IllegalStateException(
          "DEMO/MOCK communication mode is strictly prohibited in production environment "
              + "(catalog: SIH26135, detected: prodProfile=" + isProdProfile + ", prodEnv=" + isProdEnv + ", prodCatalog=" + isProdCatalog + "). "
              + "Set communication.mode=LIVE for production operations or target test catalog SIH26135_TEST."
      );
    }
  }

  public boolean isLiveMode() {
    return mode == ProviderMode.LIVE;
  }

  public boolean isDemoMode() {
    return mode == ProviderMode.DEMO || mode == ProviderMode.MOCK;
  }

  public String getEffectiveModeName() {
    return isLiveMode() ? "LIVE" : "DEMO";
  }

  public ProviderMode getMode() {
    return mode;
  }

  public void setMode(ProviderMode mode) {
    this.mode = mode != null ? mode : ProviderMode.MOCK;
  }

  public boolean isLiveRecipientOverrideEnabled() {
    return liveRecipientOverrideEnabled;
  }

  public void setLiveRecipientOverrideEnabled(boolean liveRecipientOverrideEnabled) {
    this.liveRecipientOverrideEnabled = liveRecipientOverrideEnabled;
  }

  public EmailProviderPreference getEmailProviderType() {
    return emailProviderType;
  }

  public void setEmailProviderType(EmailProviderPreference emailProviderType) {
    this.emailProviderType = emailProviderType != null ? emailProviderType : EmailProviderPreference.AUTO;
  }

  public TwilioConfig getTwilio() {
    return twilio;
  }

  public void setTwilio(TwilioConfig twilio) {
    this.twilio = twilio != null ? twilio : new TwilioConfig();
  }

  public SendGridConfig getSendgrid() {
    return sendgrid;
  }

  public SendGridConfig getSendGrid() {
    return sendgrid;
  }

  public void setSendgrid(SendGridConfig sendgrid) {
    this.sendgrid = sendgrid != null ? sendgrid : new SendGridConfig();
  }

  public SmtpConfig getSmtp() {
    return smtp;
  }

  public void setSmtp(SmtpConfig smtp) {
    this.smtp = smtp != null ? smtp : new SmtpConfig();
  }

  // --- Sub-configurations ---

  public static class TwilioConfig {
    private String accountSid = "";
    private String authToken = "";
    private String smsFrom = "";
    private String whatsappFrom = "whatsapp:+17372508034";
    private String liveTestPhone = "";
    private String liveTestWhatsapp = "";
    private String whatsappContentSid = "";
    private String whatsappContentVariables = "";
    private int connectTimeoutMs = 5000;
    private int readTimeoutMs = 10000;

    public boolean isConfigured() {
      return accountSid != null && !accountSid.isBlank()
          && authToken != null && !authToken.isBlank()
          && smsFrom != null && !smsFrom.isBlank();
    }

    public String getAccountSid() {
      return accountSid;
    }

    public void setAccountSid(String accountSid) {
      this.accountSid = accountSid != null ? accountSid.trim() : "";
    }

    public String getAuthToken() {
      return authToken;
    }

    public void setAuthToken(String authToken) {
      this.authToken = authToken != null ? authToken.trim() : "";
    }

    public String getSmsFrom() {
      return smsFrom;
    }

    public void setSmsFrom(String smsFrom) {
      this.smsFrom = smsFrom != null ? smsFrom.trim() : "";
    }

    public String getWhatsappFrom() {
      return whatsappFrom;
    }

    public void setWhatsappFrom(String whatsappFrom) {
      this.whatsappFrom = whatsappFrom != null ? whatsappFrom.trim() : "whatsapp:+14155238886";
    }

    public String getLiveTestPhone() {
      return liveTestPhone;
    }

    public void setLiveTestPhone(String liveTestPhone) {
      this.liveTestPhone = liveTestPhone != null ? liveTestPhone.trim() : "";
    }

    public String getLiveTestWhatsapp() {
      return liveTestWhatsapp;
    }

    public void setLiveTestWhatsapp(String liveTestWhatsapp) {
      this.liveTestWhatsapp = liveTestWhatsapp != null ? liveTestWhatsapp.trim() : "";
    }

    public String getWhatsappContentSid() {
      return whatsappContentSid;
    }

    public void setWhatsappContentSid(String whatsappContentSid) {
      this.whatsappContentSid = whatsappContentSid != null ? whatsappContentSid.trim() : "";
    }

    public String getWhatsappContentVariables() {
      return whatsappContentVariables;
    }

    public void setWhatsappContentVariables(String whatsappContentVariables) {
      this.whatsappContentVariables = whatsappContentVariables != null ? whatsappContentVariables.trim() : "";
    }

    public int getConnectTimeoutMs() {
      return connectTimeoutMs;
    }

    public void setConnectTimeoutMs(int connectTimeoutMs) {
      this.connectTimeoutMs = connectTimeoutMs > 0 ? connectTimeoutMs : 5000;
    }

    public int getReadTimeoutMs() {
      return readTimeoutMs;
    }

    public void setReadTimeoutMs(int readTimeoutMs) {
      this.readTimeoutMs = readTimeoutMs > 0 ? readTimeoutMs : 10000;
    }
  }

  public static class SendGridConfig {
    private String apiKey = "";
    private String fromEmail = "";
    private String liveTestEmail = "";
    private int connectTimeoutMs = 5000;
    private int readTimeoutMs = 10000;

    public boolean isConfigured() {
      return apiKey != null && !apiKey.isBlank()
          && fromEmail != null && !fromEmail.isBlank();
    }

    public String getApiKey() {
      return apiKey;
    }

    public void setApiKey(String apiKey) {
      this.apiKey = apiKey != null ? apiKey.trim() : "";
    }

    public String getFromEmail() {
      return fromEmail;
    }

    public void setFromEmail(String fromEmail) {
      this.fromEmail = fromEmail != null ? fromEmail.trim() : "";
    }

    public String getLiveTestEmail() {
      return liveTestEmail;
    }

    public void setLiveTestEmail(String liveTestEmail) {
      this.liveTestEmail = liveTestEmail != null ? liveTestEmail.trim() : "";
    }

    public int getConnectTimeoutMs() {
      return connectTimeoutMs;
    }

    public void setConnectTimeoutMs(int connectTimeoutMs) {
      this.connectTimeoutMs = connectTimeoutMs > 0 ? connectTimeoutMs : 5000;
    }

    public int getReadTimeoutMs() {
      return readTimeoutMs;
    }

    public void setReadTimeoutMs(int readTimeoutMs) {
      this.readTimeoutMs = readTimeoutMs > 0 ? readTimeoutMs : 10000;
    }
  }

  public static class SmtpConfig {
    private String host = "smtp.gmail.com";
    private int port = 587;
    private String username = "";
    private String password = "";
    private String fromEmail = "";
    private String liveTestEmail = "";
    private boolean auth = true;
    private boolean starttls = true;
    private int connectionTimeoutMs = 5000;
    private int timeoutMs = 10000;
    private int writeTimeoutMs = 5000;

    public boolean isConfigured() {
      return host != null && !host.isBlank()
          && username != null && !username.isBlank()
          && password != null && !password.isBlank()
          && fromEmail != null && !fromEmail.isBlank();
    }

    public String getHost() {
      return host;
    }

    public void setHost(String host) {
      this.host = host != null ? host.trim() : "smtp.gmail.com";
    }

    public int getPort() {
      return port;
    }

    public void setPort(int port) {
      this.port = port > 0 ? port : 587;
    }

    public String getUsername() {
      return username;
    }

    public void setUsername(String username) {
      this.username = username != null ? username.trim() : "";
    }

    public String getPassword() {
      return password;
    }

    public void setPassword(String password) {
      this.password = password != null ? password.trim() : "";
    }

    public String getFromEmail() {
      return fromEmail;
    }

    public void setFromEmail(String fromEmail) {
      this.fromEmail = fromEmail != null ? fromEmail.trim() : "";
    }

    public String getLiveTestEmail() {
      return liveTestEmail;
    }

    public void setLiveTestEmail(String liveTestEmail) {
      this.liveTestEmail = liveTestEmail != null ? liveTestEmail.trim() : "";
    }

    public boolean isAuth() {
      return auth;
    }

    public void setAuth(boolean auth) {
      this.auth = auth;
    }

    public boolean isStarttls() {
      return starttls;
    }

    public void setStarttls(boolean starttls) {
      this.starttls = starttls;
    }

    public int getConnectionTimeoutMs() {
      return connectionTimeoutMs;
    }

    public void setConnectionTimeoutMs(int connectionTimeoutMs) {
      this.connectionTimeoutMs = connectionTimeoutMs > 0 ? connectionTimeoutMs : 5000;
    }

    public int getTimeoutMs() {
      return timeoutMs;
    }

    public void setTimeoutMs(int timeoutMs) {
      this.timeoutMs = timeoutMs > 0 ? timeoutMs : 10000;
    }

    public int getWriteTimeoutMs() {
      return writeTimeoutMs;
    }

    public void setWriteTimeoutMs(int writeTimeoutMs) {
      this.writeTimeoutMs = writeTimeoutMs > 0 ? writeTimeoutMs : 5000;
    }
  }
}
