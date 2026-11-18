package in.gov.sih.sih26135.exception;

import org.springframework.http.HttpStatus;

/**
 * Exception thrown when authentication fails because the user account is locked
 * due to reaching the maximum configured failed login attempts or active security hold.
 *
 * <p>Maps to HTTP 423 Locked with error code {@code ACCOUNT_LOCKED}.
 */
public class AccountLockedException extends BaseException {

  public AccountLockedException(String message) {
    super(message, HttpStatus.LOCKED, "ACCOUNT_LOCKED");
  }

  public AccountLockedException() {
    this("User account is locked");
  }
}
