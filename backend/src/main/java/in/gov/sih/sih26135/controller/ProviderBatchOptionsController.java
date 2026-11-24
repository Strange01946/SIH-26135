package in.gov.sih.sih26135.controller;

import in.gov.sih.sih26135.response.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.constraints.Positive;
import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Validated
@RequestMapping("/api/v1/provider/courses")
@PreAuthorize("hasAuthority('course.manage')")
@SecurityRequirement(name = "bearerAuth")
public class ProviderBatchOptionsController {
  private final JdbcTemplate jdbc;

  public ProviderBatchOptionsController(JdbcTemplate jdbc) {
    this.jdbc = jdbc;
  }

  @GetMapping("/{courseId}/programs")
  public ApiResponse<List<BatchProgramOption>> getBatchProgramOptions(@PathVariable @Positive long courseId) {
    return ApiResponse.ok(jdbc.query("""
        SELECT p.program_id, p.program_code, p.program_name
        FROM course_programs cp
        JOIN programs p ON p.program_id = cp.program_id
        JOIN ref_lifecycle_status status ON status.lifecycle_status_id = p.lifecycle_status_id
        WHERE cp.course_id = ? AND p.deleted_at IS NULL AND status.status_code = 'ACTIVE'
        ORDER BY p.program_name, p.program_id
        """, (rs, row) -> new BatchProgramOption(rs.getLong(1), rs.getString(2), rs.getString(3)), courseId));
  }

  public record BatchProgramOption(long id, String code, String name) {}
}
