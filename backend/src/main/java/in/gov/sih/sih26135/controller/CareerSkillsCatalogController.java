package in.gov.sih.sih26135.controller;

import in.gov.sih.sih26135.dto.career.CareerSkills.Option;
import in.gov.sih.sih26135.dto.request.CreateJobRoleRequest;
import in.gov.sih.sih26135.dto.request.CreateSkillCategoryRequest;
import in.gov.sih.sih26135.dto.request.CreateSkillRequest;
import in.gov.sih.sih26135.exception.BadRequestException;
import in.gov.sih.sih26135.response.ApiResponse;
import in.gov.sih.sih26135.service.JobRoleService;
import in.gov.sih.sih26135.service.SkillCategoryService;
import in.gov.sih.sih26135.service.SkillService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/career-skills/catalog")
@PreAuthorize("hasAnyRole('super_admin', 'government_admin') and hasAuthority('assessment.manage')")
@SecurityRequirement(name = "bearerAuth")
public class CareerSkillsCatalogController {
  private final SkillCategoryService categories;
  private final SkillService skills;
  private final JobRoleService roles;
  private final JdbcTemplate jdbc;

  public CareerSkillsCatalogController(SkillCategoryService categories, SkillService skills, JobRoleService roles, JdbcTemplate jdbc) {
    this.categories = categories;
    this.skills = skills;
    this.roles = roles;
    this.jdbc = jdbc;
  }

  public enum Kind { CATEGORY, SKILL, ROLE }
  public record CareerCatalogOptions(List<Option> categories, List<Option> sectors) {}
  public record CareerCatalogEntry(@NotBlank @Size(max = 200) String name, @Positive Long parentId) {}

  @GetMapping
  public ApiResponse<CareerCatalogOptions> careerCatalogOptions() {
    var categoryOptions = jdbc.query("""
        SELECT skill_category_id,category_name FROM skill_categories WHERE deleted_at IS NULL AND lifecycle_status_id=1
        ORDER BY category_name
        """, (rs, row) -> new Option(rs.getLong(1), rs.getString(2)));
    var sectors = jdbc.query("""
        SELECT sector_id,sector_name FROM sectors WHERE deleted_at IS NULL AND lifecycle_status_id=1 ORDER BY sector_name
        """, (rs, row) -> new Option(rs.getLong(1), rs.getString(2)));
    return ApiResponse.ok(new CareerCatalogOptions(categoryOptions, sectors));
  }

  @PostMapping("/{kind}")
  @ResponseStatus(HttpStatus.CREATED)
  public ApiResponse<Option> createCareerCatalogEntry(@PathVariable Kind kind, @Valid @RequestBody CareerCatalogEntry input) {
    String name = input.name().strip();
    if (kind != Kind.ROLE && name.length() > 150) {
      throw new BadRequestException("Enter a name with up to 150 characters.", "INVALID_CAREER_REFERENCE");
    }
    String code = kind.name() + "-" + UUID.randomUUID().toString().substring(0, 23);
    return ApiResponse.ok(switch (kind) {
      case CATEGORY -> {
        var result = categories.createSkillCategory(new CreateSkillCategoryRequest(code, name, 1L));
        yield new Option(result.getId(), result.getCategoryName());
      }
      case SKILL -> {
        requireParent(input.parentId(), "SELECT EXISTS(SELECT 1 FROM skill_categories WHERE skill_category_id=? AND deleted_at IS NULL AND lifecycle_status_id=1)",
            "Choose an active skill category.");
        var result = skills.createSkill(new CreateSkillRequest(code, name, input.parentId(), 1L));
        yield new Option(result.getId(), result.getSkillName());
      }
      case ROLE -> {
        requireParent(input.parentId(), "SELECT EXISTS(SELECT 1 FROM sectors WHERE sector_id=? AND deleted_at IS NULL AND lifecycle_status_id=1)",
            "Choose an active sector.");
        var result = roles.createJobRole(new CreateJobRoleRequest(code, name, input.parentId(), 1L));
        yield new Option(result.getId(), result.getJobRoleName());
      }
    });
  }

  private void requireParent(Long id, String query, String message) {
    if (id == null || !Boolean.TRUE.equals(jdbc.queryForObject(query, Boolean.class, id))) {
      throw new BadRequestException(message, "INVALID_CAREER_REFERENCE");
    }
  }
}
