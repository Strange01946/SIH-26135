package in.gov.sih.sih26135.dto.auth;

/**
 * Request payload for user authentication.
 *
 * <p>Accepts either username or email as the identity identifier.
 * Raw password is never exposed in logs or string representations.
 */
public class LoginRequest {

  private String usernameOrEmail;
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
