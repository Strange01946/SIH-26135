package in.gov.sih.sih26135.dto.registration;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class ProvisionSuperAdminRequest {

  @NotBlank(message = "Username is required")
  @Size(min = 3, max = 80, message = "Username must be between 3 and 80 characters")
  private String username;

  @NotBlank(message = "Email is required")
  @Email(message = "Email must be a valid email address")
  private String email;

  @NotBlank(message = "Password is required")
  @Size(min = 8, max = 100, message = "Password must be at least 8 characters")
  private String password;

  private String fullName;

  public ProvisionSuperAdminRequest() {
  }

  public String getUsername() {
    return username;
  }

  public void setUsername(String username) {
    this.username = username;
  }

  public String getEmail() {
    return email;
  }

  public void setEmail(String email) {
    this.email = email;
  }

  public String getPassword() {
    return password;
  }

  public void setPassword(String password) {
    this.password = password;
  }

  public String getFullName() {
    return fullName;
  }

  public void setFullName(String fullName) {
    this.fullName = fullName;
  }
}
