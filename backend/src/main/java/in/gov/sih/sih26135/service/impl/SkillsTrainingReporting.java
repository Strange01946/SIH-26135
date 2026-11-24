package in.gov.sih.sih26135.service.impl;

import in.gov.sih.sih26135.exception.ConflictException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.Normalizer;
import java.util.HexFormat;
import java.util.Locale;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/** Creates the foreign keys required by the existing batch, enrolment and outcome schemas. */
@Component
class SkillsTrainingReporting {
  private static final String REFERENCE_CODE = "SKILLS-REPORTING";
  private final JdbcTemplate jdbc;

  SkillsTrainingReporting(JdbcTemplate jdbc) {
    this.jdbc = jdbc;
  }

  static void requireEditable(String code) {
    if (REFERENCE_CODE.equals(code)) {
      throw new ConflictException("The skills reporting reference is maintained by the system.", "SKILLS_REPORTING_READ_ONLY");
    }
  }

  record References(long courseId, long programId) {}
  private record ReportingDefaults(long programId, long sectorId, long deliveryModeId) {}
  private record CourseReference(long id, String skills) {}

  References forApprovedSkills(String skills) {
    if (skills == null || skills.isBlank()) {
      throw new ConflictException("The booking is missing its training skills.", "TRAINING_SKILLS_REQUIRED");
    }
    String description = Normalizer.normalize(skills, Normalizer.Form.NFC).replaceAll("(?U)\\s+", " ").strip();
    String key = canonical(description);
    String code = courseCode(key);
    ReportingDefaults defaults = jdbc.query("""
        SELECT p.program_id,s.sector_id,m.delivery_mode_id
        FROM programs p JOIN schemes sc ON sc.scheme_id=p.scheme_id
        JOIN sectors s ON s.sector_code='SKILLS-UNCLASSIFIED'
        JOIN ref_delivery_mode m ON m.mode_code='OFFLINE'
        WHERE p.program_code=? AND sc.scheme_code=? AND p.deleted_at IS NULL AND sc.deleted_at IS NULL
          AND s.deleted_at IS NULL AND p.lifecycle_status_id=1 AND sc.lifecycle_status_id=1 AND s.lifecycle_status_id=1
        """, (rs, row) -> new ReportingDefaults(rs.getLong(1), rs.getLong(2), rs.getLong(3)),
        REFERENCE_CODE, REFERENCE_CODE).stream().findFirst()
        .orElseThrow(() -> new ConflictException("Training reporting references are unavailable.", "SKILLS_REPORTING_UNAVAILABLE"));
    String name = description.substring(0, description.offsetByCodePoints(0, Math.min(200, description.codePointCount(0, description.length()))));
    jdbc.update("""
        INSERT INTO courses(course_code,course_name,description,sector_id,delivery_mode_id,lifecycle_status_id)
        VALUES (?,?,?,?,?,1) ON DUPLICATE KEY UPDATE course_id=courses.course_id
        """, code, name, description, defaults.sectorId(), defaults.deliveryModeId());
    CourseReference course = jdbc.query("""
        SELECT course_id,description FROM courses WHERE course_code=? AND sector_id=? AND deleted_at IS NULL FOR SHARE
        """, (rs, row) -> new CourseReference(rs.getLong(1), rs.getString(2)), code, defaults.sectorId()).stream()
        .findFirst().orElseThrow(() -> new ConflictException("Training reporting references are unavailable.", "SKILLS_REPORTING_UNAVAILABLE"));
    if (course.skills() == null || !canonical(course.skills()).equals(key)) {
      throw new ConflictException("Training reporting references are unavailable.", "SKILLS_REPORTING_UNAVAILABLE");
    }
    jdbc.update("""
        INSERT INTO course_programs(course_id,program_id) VALUES (?,?)
        ON DUPLICATE KEY UPDATE course_id=course_programs.course_id
        """, course.id(), defaults.programId());
    return new References(course.id(), defaults.programId());
  }

  private static String canonical(String value) {
    return Normalizer.normalize(value, Normalizer.Form.NFC).replaceAll("(?U)\\s+", " ").strip().toLowerCase(Locale.ROOT);
  }

  private static String courseCode(String key) {
    try {
      byte[] hash = MessageDigest.getInstance("SHA-256").digest(key.getBytes(StandardCharsets.UTF_8));
      return "SKL-" + HexFormat.of().formatHex(hash, 0, 14);
    } catch (NoSuchAlgorithmException ex) {
      throw new IllegalStateException("SHA-256 is unavailable", ex);
    }
  }
}
