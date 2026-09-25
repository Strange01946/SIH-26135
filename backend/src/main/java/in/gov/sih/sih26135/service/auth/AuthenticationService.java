package in.gov.sih.sih26135.service.auth;

import in.gov.sih.sih26135.dto.auth.AuthResponse;
import in.gov.sih.sih26135.dto.auth.AuthenticatedUserResponse;
import in.gov.sih.sih26135.dto.auth.LoginRequest;
import in.gov.sih.sih26135.dto.auth.RefreshTokenRequest;
import in.gov.sih.sih26135.security.UserPrincipal;

/**
 * Service interface for user authentication, token renewal, and current user profile retrieval.
 */
public interface AuthenticationService {

  /**
   * Authenticates a user using credentials and returns JWT access and refresh tokens.
   *
   * @param request login request with username/email and password
   * @return AuthResponse containing access token, refresh token, and identity metadata
   */
  AuthResponse login(LoginRequest request);

  /**
   * Validates a refresh token and issues a new access token and rotated refresh token.
   *
   * @param request refresh request with refresh token
   * @return AuthResponse containing renewed access token and rotated refresh token
   */
  AuthResponse refresh(RefreshTokenRequest request);

  /**
   * Retrieves the profile data and effective authorities for the currently authenticated user.
   *
   * @param principal authenticated user principal from security context
   * @return AuthenticatedUserResponse containing user details and authorities
   */
  AuthenticatedUserResponse getCurrentUser(UserPrincipal principal);
}
