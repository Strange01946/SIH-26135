package in.gov.sih.sih26135.config.cache;

import in.gov.sih.sih26135.dto.request.AnalyticsFilterCriteria;
import java.lang.reflect.Method;
import java.util.Objects;
import org.springframework.cache.interceptor.KeyGenerator;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

/**
 * Deterministic, collision-free Spring Cache key generator for analytical queries.
 *
 * <p>Generates canonical cache keys incorporating:
 * <ul>
 *   <li>The analytical method identifier.</li>
 *   <li>Authorized entity or territorial scope identifiers (e.g. {@code stateId}, {@code districtId}).</li>
 *   <li>Complete multi-dimensional filter criteria (temporal, organizational, spatial, demographic).</li>
 *   <li>Pagination parameters (page, size, sort field, sort direction) where applicable.</li>
 * </ul>
 *
 * <p>Guarantees that different filter combinations, scopes, and pagination offsets never collide,
 * and normalizes equivalent empty or default queries to identical canonical keys.
 */
@Component("analyticsKeyGenerator")
public class AnalyticsCacheKeyGenerator implements KeyGenerator {

  @Override
  public Object generate(Object target, Method method, Object... params) {
    StringBuilder sb = new StringBuilder(64);
    sb.append(method.getName());

    if (params == null || params.length == 0) {
      return sb.toString();
    }

    for (Object param : params) {
      if (param instanceof java.util.function.Supplier<?>) {
        // Supplier is the execution callback on cache miss; omit from key to prevent unstable lambda identities
        continue;
      }
      sb.append(':');
      if (param == null) {
        sb.append("null");
      } else if (param instanceof AnalyticsFilterCriteria criteria) {
        sb.append(serializeCriteria(criteria));
      } else if (param instanceof Pageable pageable) {
        sb.append(serializePageable(pageable));
      } else {
        sb.append(param.getClass().getSimpleName()).append('=').append(param);
      }
    }

    return sb.toString();
  }

  /**
   * Deterministically serializes {@link AnalyticsFilterCriteria} into a canonical string key.
   */
  public static String serializeCriteria(AnalyticsFilterCriteria criteria) {
    if (criteria == null || criteria.isEmpty()) {
      return "criteria=ALL";
    }
    StringBuilder sb = new StringBuilder(64);
    sb.append("criteria[");
    boolean first = true;

    if (criteria.getFiscalYear() != null && !criteria.getFiscalYear().isBlank()) {
      first = appendField(sb, "fy", criteria.getFiscalYear().trim(), first);
    }
    if (criteria.getCalendarYear() != null) {
      first = appendField(sb, "cy", criteria.getCalendarYear(), first);
    }
    if (criteria.getStartDate() != null) {
      first = appendField(sb, "sd", criteria.getStartDate(), first);
    }
    if (criteria.getEndDate() != null) {
      first = appendField(sb, "ed", criteria.getEndDate(), first);
    }
    if (criteria.getSchemeId() != null) {
      first = appendField(sb, "sch", criteria.getSchemeId(), first);
    }
    if (criteria.getProgramId() != null) {
      first = appendField(sb, "prog", criteria.getProgramId(), first);
    }
    if (criteria.getSectorId() != null) {
      first = appendField(sb, "sec", criteria.getSectorId(), first);
    }
    if (criteria.getCourseId() != null) {
      first = appendField(sb, "crs", criteria.getCourseId(), first);
    }
    if (criteria.getStateId() != null) {
      first = appendField(sb, "st", criteria.getStateId(), first);
    }
    if (criteria.getDistrictId() != null) {
      first = appendField(sb, "dist", criteria.getDistrictId(), first);
    }
    String genderCode = criteria.resolveGenderCode();
    if (genderCode != null) {
      first = appendField(sb, "gen", genderCode, first);
    }
    if (criteria.getSocialCategory() != null && !criteria.getSocialCategory().isBlank()) {
      first = appendField(sb, "soc", criteria.getSocialCategory().trim(), first);
    }
    if (criteria.getMinority() != null && !criteria.getMinority().isBlank()) {
      first = appendField(sb, "min", criteria.getMinority().trim(), first);
    }
    if (criteria.getPwd() != null && !criteria.getPwd().isBlank()) {
      appendField(sb, "pwd", criteria.getPwd().trim(), first);
    }

    sb.append(']');
    return sb.toString();
  }

  private static boolean appendField(StringBuilder sb, String key, Object value, boolean first) {
    if (!first) {
      sb.append(';');
    }
    sb.append(key).append('=').append(value);
    return false;
  }

  private static String serializePageable(Pageable pageable) {
    if (pageable == null || pageable.isUnpaged()) {
      return "pageable=UNPAGED";
    }
    return "p=" + pageable.getPageNumber() + ";s=" + pageable.getPageSize() + ";sort=" + pageable.getSort();
  }
}
