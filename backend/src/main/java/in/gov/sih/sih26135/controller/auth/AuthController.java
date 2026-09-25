package in.gov.sih.sih26135.controller.auth;

import in.gov.sih.sih26135.dto.auth.AuthResponse;
import in.gov.sih.sih26135.dto.auth.AuthenticatedUserResponse;
import in.gov.sih.sih26135.dto.auth.LoginRequest;
import in.gov.sih.sih26135.dto.auth.RefreshTokenRequest;
import in.gov.sih.sih26135.exception.BadRequestException;
import in.gov.sih.sih26135.response.ApiResponse;
import in.gov.sih.sih26135.security.UserPrincipal;
import in.gov.sih.sih26135.service.auth.AuthenticationService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller for authentication endpoints: user login, token refresh, and authenticated user profile.
 *
 * <p>Base Route: /api/v1/auth
 * <ul>
 *   <li>POST /login — Public credential verification and JWT pair generation</li>
 *   <li>POST /refresh — Public token renewal using a valid refresh token</li>
 *   <li>GET /me — Authenticated self-profile information of the current principal</li>
 * </ul>
 */
@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

  private final AuthenticationService authenticationService;

  public AuthController(AuthenticationService authenticationService) {
    this.authenticationService = authenticationService;
  }

  /**
   * Authenticates user with username or email and raw password.
   *
   * @param request login payload containing username/email and password
   * @param httpRequest HTTP servlet request
   * @return 200 OK with AuthResponse enveloped in ApiResponse
   */
  @PostMapping("/login")
  public ResponseEntity<ApiResponse<AuthResponse>> login(
      @RequestBody LoginRequest request,
      HttpServletRequest httpRequest) {
    if (request == null) {
      throw new BadRequestException("Login request cannot be null");
    }
    if (request.getUsernameOrEmail() == null || request.getUsernameOrEmail().isBlank()) {
      throw new BadRequestException("Username or email is required");
    }
    if (request.getPassword() == null || request.getPassword().isBlank()) {
      throw new BadRequestException("Password is required");
    }
    AuthResponse response = authenticationService.login(request);
    return ResponseEntity.ok(ApiResponse.success("Authentication successful", response, httpRequest.getRequestURI()));
  }

  /**
   * Renews access token using a valid refresh token with token rotation.
   *
   * @param request refresh payload containing valid refresh token
   * @param httpRequest HTTP servlet request
   * @return 200 OK with new AuthResponse enveloped in ApiResponse
   */
  @PostMapping("/refresh")
  public ResponseEntity<ApiResponse<AuthResponse>> refresh(
      @RequestBody RefreshTokenRequest request,
      HttpServletRequest httpRequest) {
    if (request == null) {
      throw new BadRequestException("Refresh token request cannot be null");
    }
    if (request.getRefreshToken() == null || request.getRefreshToken().isBlank()) {
      throw new BadRequestException("Refresh token is required");
    }
    AuthResponse response = authenticationService.refresh(request);
    return ResponseEntity.ok(ApiResponse.success("Token refreshed successfully", response, httpRequest.getRequestURI()));
  }

  /**
   * Retrieves profile and granted authorities for the currently authenticated principal.
   *
   * @param principal authenticated user principal injected from SecurityContext
   * @param httpRequest HTTP servlet request
   * @return 200 OK with AuthenticatedUserResponse enveloped in ApiResponse
   */
  @GetMapping("/me")
  public ResponseEntity<ApiResponse<AuthenticatedUserResponse>> me(
      @AuthenticationPrincipal UserPrincipal principal,
      HttpServletRequest httpRequest) {
    AuthenticatedUserResponse response = authenticationService.getCurrentUser(principal);
    return ResponseEntity.ok(ApiResponse.success("Authenticated user profile retrieved successfully", response, httpRequest.getRequestURI()));
  }
}
