package in.gov.sih.sih26135.dto.auth;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;

/**
 * Request payload for user authentication.
 *
 * <p>Accepts either username or email as the identity identifier.
 * Raw password is never exposed in logs or string representations.
 */
@Schema(description = "User authentication credentials payload")
public class LoginRequest {

  @Schema(description = "Username or email address", example = "admin", requiredMode = Schema.RequiredMode.REQUIRED)
  @NotBlank(message = "Username or email is required")
  @Size(max = 255, message = "Username or email cannot exceed 255 characters")
  private String usernameOrEmail;

  @Schema(description = "Raw password (masked in logs)", example = "P@ssw0rd123!", requiredMode = Schema.RequiredMode.REQUIRED)
  @NotBlank(message = "Password is required")
  @Size(max = 128, message = "Password cannot exceed 128 characters")
  private String password;

  public LoginRequest() {
  }

  public LoginRequest(String usernameOrEmail, String password) {
    this.usernameOrEmail = usernameOrEmail;
    this.password = password;
  }

  public String getUsernameOrEmail() {
    return usernameOrEmail;
  }

  public void setUsernameOrEmail(String usernameOrEmail) {
    this.usernameOrEmail = usernameOrEmail;
  }

  public String getPassword() {
    return password;
  }

  public void setPassword(String password) {
    this.password = password;
  }

  @Override
  public String toString() {
    return "LoginRequest{" +
        "usernameOrEmail='" + usernameOrEmail + '\'' +
        '}';
  }
}
