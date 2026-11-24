package in.gov.sih.sih26135.service;

import in.gov.sih.sih26135.exception.BadRequestException;
import org.springframework.jdbc.core.JdbcTemplate;

/** Shared reference scope for selectable states and districts. */
public final class ActiveGeography {
  public static final String STATES = """
      FROM states s
      JOIN ref_lifecycle_status ss ON ss.lifecycle_status_id = s.lifecycle_status_id AND ss.status_code = 'ACTIVE'
      WHERE s.deleted_at IS NULL
      """;

  public static final String DISTRICTS = """
      FROM districts d JOIN states s ON s.state_id = d.state_id
      JOIN ref_lifecycle_status ds ON ds.lifecycle_status_id = d.lifecycle_status_id AND ds.status_code = 'ACTIVE'
      JOIN ref_lifecycle_status ss ON ss.lifecycle_status_id = s.lifecycle_status_id AND ss.status_code = 'ACTIVE'
      WHERE d.deleted_at IS NULL AND s.deleted_at IS NULL
      """;

  private ActiveGeography() {}

  public static void validate(JdbcTemplate jdbc, Long stateId, Long districtId) {
    if (districtId != null) {
      if (stateId == null || !Boolean.TRUE.equals(jdbc.queryForObject(
          "SELECT EXISTS(SELECT 1 " + DISTRICTS + " AND d.district_id = ? AND s.state_id = ?)",
          Boolean.class, districtId, stateId))) {
        throw new BadRequestException("Select an active district within the selected state or union territory.",
            "INVALID_STATE_DISTRICT");
      }
    } else if (stateId != null && !Boolean.TRUE.equals(jdbc.queryForObject(
        "SELECT EXISTS(SELECT 1 " + STATES + " AND s.state_id = ?)", Boolean.class, stateId))) {
      throw new BadRequestException("Select an active state or union territory.", "INVALID_STATE");
    }
  }
}
