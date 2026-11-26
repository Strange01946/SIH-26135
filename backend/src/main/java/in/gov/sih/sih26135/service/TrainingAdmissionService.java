package in.gov.sih.sih26135.service;

import in.gov.sih.sih26135.dto.venue.TrainingAdmission;
import in.gov.sih.sih26135.dto.venue.TrainingAdmission.EducationEligibility;
import in.gov.sih.sih26135.exception.BadRequestException;
import in.gov.sih.sih26135.exception.ConflictException;
import in.gov.sih.sih26135.exception.ResourceNotFoundException;
import in.gov.sih.sih26135.security.WorkflowSecurityScopeValidator;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

@Service
public class TrainingAdmissionService {
  private final JdbcTemplate jdbc;
  private final WorkflowSecurityScopeValidator scope;

  public TrainingAdmissionService(JdbcTemplate jdbc, WorkflowSecurityScopeValidator scope) {
    this.jdbc = jdbc;
    this.scope = scope;
  }

  public List<TrainingAdmission.Level> levels() {
    return jdbc.query("""
        SELECT education_level_id,education_name FROM ref_education_level
        WHERE education_code<>'OTHER' ORDER BY sort_order,education_level_id
        """, (rs, row) -> new TrainingAdmission.Level(rs.getLong(1), rs.getString(2)));
  }

  public void validateMinimum(Long minimum) {
    if (minimum != null && rank(minimum) == null) {
      throw new BadRequestException("Choose a ranked education level or Open to all.", "INVALID_MINIMUM_EDUCATION_LEVEL");
    }
  }

  public TrainingAdmission forProvider(long batchId) {
    long providerId = scope.resolveAuthenticatedUser().map(user -> user.getTrainingProviderId())
        .orElseThrow(() -> new AccessDeniedException("A training provider assignment is required."));
    if (!Boolean.TRUE.equals(jdbc.queryForObject("""
        SELECT EXISTS(SELECT 1 FROM training_batches WHERE batch_id=? AND provider_id=? AND deleted_at IS NULL)
        """, Boolean.class, batchId, providerId))) {
      throw new ResourceNotFoundException("This training batch is unavailable.");
    }
    return forBatch(batchId);
  }

  public EducationEligibility eligibility(long traineeId, TrainingAdmission admission) {
    return eligibility(traineeId, admission, false);
  }

  // Enrollment callers hold the batch lock while the trainee's current education is checked.
  public void requireEligible(long batchId, long traineeId) {
    var admission = forBatch(batchId);
    switch (eligibility(traineeId, admission, true)) {
      case EDUCATION_REQUIRED -> throw new ConflictException(
          "Update the trainee profile with a ranked education level. This training requires " + admission.minimumEducationLevel() + ".",
          "TRAINING_EDUCATION_REQUIRED");
      case BELOW_MINIMUM -> throw new ConflictException(
          "This training requires at least " + admission.minimumEducationLevel() + ".", "TRAINING_EDUCATION_BELOW_MINIMUM");
      case ELIGIBLE -> { }
    }
  }

  private TrainingAdmission forBatch(long batchId) {
    return jdbc.query("""
        SELECT plan.minimum_education_level_id,edu.education_name AS minimum_education_level,plan.recommended_prerequisites
        FROM training_batches b
        LEFT JOIN venue_bookings v ON v.booking_id=(
          SELECT MAX(vb.booking_id) FROM venue_bookings vb WHERE vb.batch_id=b.batch_id
            AND vb.booking_status IN ('APPROVED','COMPLETED'))
        LEFT JOIN venue_booking_training_plans plan ON plan.booking_id=v.booking_id
        LEFT JOIN ref_education_level edu ON edu.education_level_id=plan.minimum_education_level_id
        WHERE b.batch_id=?
        """, (rs, row) -> read(rs), batchId)
        .stream().findFirst().orElseThrow(() -> new ResourceNotFoundException("This training batch is unavailable."));
  }

  private EducationEligibility eligibility(long traineeId, TrainingAdmission admission, boolean lock) {
    if (admission.minimumEducationLevelId() == null) return EducationEligibility.ELIGIBLE;
    Long level = jdbc.queryForObject("SELECT education_level_id FROM trainees WHERE trainee_id=?" + (lock ? " FOR UPDATE" : ""),
        Long.class, traineeId);
    Integer current = rank(level);
    if (current == null) return EducationEligibility.EDUCATION_REQUIRED;
    Integer minimum = rank(admission.minimumEducationLevelId());
    return minimum != null && current >= minimum ? EducationEligibility.ELIGIBLE : EducationEligibility.BELOW_MINIMUM;
  }

  private Integer rank(Long id) {
    if (id == null) return null;
    return jdbc.queryForList("""
        SELECT sort_order FROM ref_education_level WHERE education_level_id=? AND education_code<>'OTHER'
        """, Integer.class, id).stream().findFirst().orElse(null);
  }

  public static TrainingAdmission read(ResultSet rs) throws SQLException {
    return new TrainingAdmission(rs.getObject("minimum_education_level_id", Long.class),
        rs.getString("minimum_education_level"), rs.getString("recommended_prerequisites"));
  }
}
