package in.gov.sih.sih26135.communication.ratelimit;

import in.gov.sih.sih26135.communication.exception.CommunicationRateLimitException;
import in.gov.sih.sih26135.communication.model.CommunicationChannel;
import in.gov.sih.sih26135.security.InternalAuthenticationToken;
import in.gov.sih.sih26135.security.InternalServicePrincipal;
import in.gov.sih.sih26135.security.TenantScopeType;
import in.gov.sih.sih26135.security.UserPrincipal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

/**
 * Concurrency-safe, database-backed implementation of {@link CommunicationRateLimiter}
 * using MySQL distributed named locks on a single dedicated connection and rolling window event tracking.
 */
@Service
public class CommunicationRateLimiterImpl implements CommunicationRateLimiter {

  private static final Logger log = LoggerFactory.getLogger(CommunicationRateLimiterImpl.class);

  private final JdbcTemplate jdbcTemplate;

  @Autowired
  public CommunicationRateLimiterImpl(JdbcTemplate jdbcTemplate) {
    this.jdbcTemplate = Objects.requireNonNull(jdbcTemplate, "JdbcTemplate is required");
  }

  @Override
  public void checkAndReserveQuota(
      Long traineeId,
      CommunicationChannel channel,
      Long authorizedProviderId,
      String templateCode,
      String parametersHash) {

    if (traineeId == null) {
      throw new IllegalArgumentException("traineeId cannot be null for rate limit evaluation");
    }
    if (channel == null) {
      throw new IllegalArgumentException("channel cannot be null for rate limit evaluation");
    }

    String traineeLimiterKey = "TRAINEE:" + channel.name() + ":" + traineeId;
    String dedupKey = "DEDUP:" + traineeId + ":" + channel.name() + ":" + (templateCode != null ? templateCode : "NONE")
        + ":" + (parametersHash != null ? parametersHash : "EMPTY");
    String lockName = "rl_tr_" + traineeId + "_" + channel.name();

    jdbcTemplate.execute((Connection conn) -> {
      boolean lockAcquired = false;
      try (PreparedStatement lockStmt = conn.prepareStatement("SELECT GET_LOCK(?, 5)")) {
        lockStmt.setString(1, lockName);
        try (ResultSet rs = lockStmt.executeQuery()) {
          if (rs.next() && rs.getInt(1) == 1) {
            lockAcquired = true;
          }
        }
      } catch (Exception ex) {
        log.warn("[RateLimiter] Error acquiring named lock '{}': {}", lockName, ex.getMessage());
      }

      if (!lockAcquired) {
        throw new CommunicationRateLimitException(
            "Concurrent dispatch in progress for trainee " + traineeId + ". Please retry shortly.",
            "CONCURRENT_DISPATCH_TIMEOUT",
            5,
            "LOCK_TIMEOUT"
        );
      }

      try {
        // 1. Rapid Deduplication Check (Window: 5 seconds)
        int dedupCount = 0;
        try (PreparedStatement ps = conn.prepareStatement(
            "SELECT COUNT(*) FROM communication_rate_limit_events WHERE limiter_key = ? AND created_at >= NOW() - INTERVAL ? SECOND")) {
          ps.setString(1, dedupKey);
          ps.setInt(2, DEFAULT_DEDUPLICATION_WINDOW_SECONDS);
          try (ResultSet rs = ps.executeQuery()) {
            if (rs.next()) {
              dedupCount = rs.getInt(1);
            }
          }
        }

        if (dedupCount > 0) {
          throw new CommunicationRateLimitException(
              "Duplicate dispatch request detected within 5-second deduplication window for trainee " + traineeId,
              "DUPLICATE_DISPATCH_REJECTED",
              5,
              "DEDUPLICATION"
          );
        }

        // 2. Trainee Rolling 24-Hour Anti-Spam Quota Check
        int maxTraineeQuota = switch (channel) {
          case SMS, PHONE -> DEFAULT_MAX_SMS_PER_TRAINEE_24H;
          case EMAIL -> DEFAULT_MAX_EMAIL_PER_TRAINEE_24H;
          case WHATSAPP -> DEFAULT_MAX_WHATSAPP_PER_TRAINEE_24H;
        };

        int traineeAttemptsCount = 0;
        try (PreparedStatement ps = conn.prepareStatement(
            "SELECT COUNT(*) FROM communication_rate_limit_events WHERE limiter_key = ? AND created_at >= NOW() - INTERVAL 24 HOUR")) {
          ps.setString(1, traineeLimiterKey);
          try (ResultSet rs = ps.executeQuery()) {
            if (rs.next()) {
              traineeAttemptsCount = rs.getInt(1);
            }
          }
        }

        if (traineeAttemptsCount >= maxTraineeQuota) {
          Long retryAfter = calculateRetryAfterSeconds(conn, traineeLimiterKey, 86400);
          log.warn("[RateLimiter] Trainee {} exceeded {} rate limit: {}/{} in 24h, retryAfter={}",
              traineeId, channel, traineeAttemptsCount, maxTraineeQuota, retryAfter);
          throw new CommunicationRateLimitException(
              "Trainee has exceeded maximum allowed " + channel + " notifications (" + maxTraineeQuota + " per 24 hours)",
              "RATE_LIMIT_EXCEEDED",
              retryAfter != null ? retryAfter : 3600L,
              "TRAINEE_" + channel.name()
          );
        }

        // 3. Training Provider Rolling 60-Second Throughput Quota Check
        if (authorizedProviderId != null && authorizedProviderId > 0) {
          String providerLimiterKey = "PROVIDER:" + authorizedProviderId;
          int providerRequestsCount = 0;
          try (PreparedStatement ps = conn.prepareStatement(
              "SELECT COUNT(*) FROM communication_rate_limit_events WHERE limiter_key = ? AND created_at >= NOW() - INTERVAL 60 SECOND")) {
            ps.setString(1, providerLimiterKey);
            try (ResultSet rs = ps.executeQuery()) {
              if (rs.next()) {
                providerRequestsCount = rs.getInt(1);
              }
            }
          }

          if (providerRequestsCount >= DEFAULT_MAX_PROVIDER_REQUESTS_60S) {
            Long retryAfter = calculateRetryAfterSeconds(conn, providerLimiterKey, 60);
            log.warn("[RateLimiter] Provider {} exceeded burst rate limit: {}/{} in 60s, retryAfter={}",
                authorizedProviderId, providerRequestsCount, DEFAULT_MAX_PROVIDER_REQUESTS_60S, retryAfter);
            throw new CommunicationRateLimitException(
                "Training provider has exceeded maximum throughput rate limit (100 requests per 60 seconds)",
                "PROVIDER_RATE_LIMIT_EXCEEDED",
                retryAfter != null ? retryAfter : 30L,
                "TRAINING_PROVIDER"
            );
          }

          // Record provider throughput request
          try (PreparedStatement ps = conn.prepareStatement(
              "INSERT INTO communication_rate_limit_events (limiter_key, event_type, created_at) VALUES (?, 'PROVIDER_REQUEST', NOW())")) {
            ps.setString(1, providerLimiterKey);
            ps.executeUpdate();
          }
        }

        // 4. Record Trainee Attempt & Deduplication Event
        try (PreparedStatement ps = conn.prepareStatement(
            "INSERT INTO communication_rate_limit_events (limiter_key, event_type, created_at) VALUES (?, 'DISPATCH_ATTEMPT', NOW())")) {
          ps.setString(1, traineeLimiterKey);
          ps.executeUpdate();
        }
        try (PreparedStatement ps = conn.prepareStatement(
            "INSERT INTO communication_rate_limit_events (limiter_key, event_type, created_at) VALUES (?, 'DEDUP', NOW())")) {
          ps.setString(1, dedupKey);
          ps.executeUpdate();
        }

      } finally {
        if (lockAcquired) {
          try (PreparedStatement releaseStmt = conn.prepareStatement("SELECT RELEASE_LOCK(?)")) {
            releaseStmt.setString(1, lockName);
            try (ResultSet rs = releaseStmt.executeQuery()) {
              // released
            }
          } catch (Exception ex) {
            log.warn("[RateLimiter] Error releasing named lock '{}': {}", lockName, ex.getMessage());
          }
        }
      }
      return null;
    });
  }

  @Override
  public Long resolveAuthoritativeProviderId(Long traineeId, Long callerSuppliedProviderId) {
    Authentication auth = SecurityContextHolder.getContext().getAuthentication();

    // 1. Check if caller is an authenticated UserPrincipal scoped to a specific provider
    if (auth != null && auth.getPrincipal() instanceof UserPrincipal principal) {
      if (!isNationwideAdmin(auth)) {
        Long userProviderId = lookupUserProviderId(principal.getId());
        if (userProviderId != null) {
          // If caller supplied a different provider ID, fail-closed
          if (callerSuppliedProviderId != null && !callerSuppliedProviderId.equals(userProviderId)) {
            throw new SecurityException("Caller is not authorized to act on behalf of provider ID: " + callerSuppliedProviderId);
          }
          return userProviderId;
        }
      }
    }

    // 2. Check if caller is an InternalAuthenticationToken with provider scope
    if (auth instanceof InternalAuthenticationToken internalAuth) {
      InternalServicePrincipal principal = (InternalServicePrincipal) internalAuth.getPrincipal();
      if (principal.getTenantScopeType() == TenantScopeType.PROVIDER && principal.getTenantId() != null) {
        return principal.getTenantId();
      }
    }

    // 3. For nationwide callers or internal services: if provider ID supplied, verify it exists
    if (callerSuppliedProviderId != null && callerSuppliedProviderId > 0) {
      if (isNationwideAdmin(auth) || isInternalNationwide(auth)) {
        return callerSuppliedProviderId;
      }
    }

    // 4. Fallback: resolve from trainee's active training enrollment
    if (traineeId != null) {
      try {
        Long enrolledProviderId = jdbcTemplate.queryForObject(
            "SELECT provider_id FROM training_enrollments WHERE trainee_id = ? AND deleted_at IS NULL ORDER BY enrollment_id DESC LIMIT 1",
            Long.class,
            traineeId
        );
        if (enrolledProviderId != null && enrolledProviderId > 0) {
          return enrolledProviderId;
        }
      } catch (DataAccessException ignored) {
        // Trainee might not have an active enrollment record
      }
    }

    return null;
  }

  private Long lookupUserProviderId(Long userId) {
    try {
      return jdbcTemplate.queryForObject(
          "SELECT provider_id FROM users WHERE user_id = ? AND provider_id IS NOT NULL",
          Long.class,
          userId
      );
    } catch (EmptyResultDataAccessException ex) {
      return null;
    }
  }

  private boolean isNationwideAdmin(Authentication auth) {
    if (auth == null) {
      return false;
    }
    return auth.getAuthorities().stream()
        .anyMatch(a -> a.getAuthority().equals("ROLE_super_admin") || a.getAuthority().equals("ROLE_NATIONWIDE_ADMIN"));
  }

  private boolean isInternalNationwide(Authentication auth) {
    if (auth instanceof InternalAuthenticationToken internalAuth) {
      InternalServicePrincipal principal = (InternalServicePrincipal) internalAuth.getPrincipal();
      return principal.isNationwide();
    }
    return false;
  }

  private Long calculateRetryAfterSeconds(Connection conn, String limiterKey, int windowSeconds) {
    try (PreparedStatement ps = conn.prepareStatement(
        "SELECT TIMESTAMPDIFF(SECOND, NOW(), MIN(created_at) + INTERVAL ? SECOND) "
            + "FROM communication_rate_limit_events "
            + "WHERE limiter_key = ? AND created_at >= NOW() - INTERVAL ? SECOND")) {
      ps.setInt(1, windowSeconds);
      ps.setString(2, limiterKey);
      ps.setInt(3, windowSeconds);
      try (ResultSet rs = ps.executeQuery()) {
        if (rs.next()) {
          long rem = rs.getLong(1);
          if (rem > 0) {
            return rem;
          }
        }
      }
    } catch (Exception ex) {
      log.debug("[RateLimiter] Error calculating precise retryAfterSeconds: {}", ex.getMessage());
    }
    return (long) Math.min(windowSeconds, 60);
  }

  public static String hashParameters(Object params) {
    if (params == null) {
      return "EMPTY";
    }
    try {
      MessageDigest md = MessageDigest.getInstance("SHA-256");
      byte[] hash = md.digest(params.toString().getBytes(StandardCharsets.UTF_8));
      StringBuilder sb = new StringBuilder();
      for (int i = 0; i < Math.min(hash.length, 8); i++) {
        sb.append(String.format("%02x", hash[i]));
      }
      return sb.toString();
    } catch (Exception ex) {
      return String.valueOf(params.hashCode());
    }
  }
}
