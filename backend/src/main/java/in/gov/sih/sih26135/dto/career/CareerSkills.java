package in.gov.sih.sih26135.dto.career;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public final class CareerSkills {
  private CareerSkills() {}

  @Schema(name = "CareerSkillsOption")
  public record Option(long id, String name) {}
  @Schema(name = "CareerSkillsReference")
  public record Reference(List<Option> roles, List<Option> skills, List<Option> levels) {}
  public record SkillInput(@NotBlank @Size(max = 150) String name, @Positive Long levelId) {}
  @Schema(name = "SaveCareerSkills")
  public record Save(@NotNull @PositiveOrZero Long revision, @Positive Long desiredRoleId,
                     @Size(max = 200) String desiredJobTitle,
                     @NotNull @Size(max = 50) List<@NotNull @Valid SkillInput> skills) {}
  @JsonInclude(JsonInclude.Include.NON_NULL)
  @Schema(name = "CareerProfileSkill")
  public record Skill(String name, Long skillId, Long levelId, String level, Integer rank) {}
  @Schema(name = "CareerSkillEvidence")
  public record Evidence(long skillId, String skill, String level, int rank, LocalDateTime observedAt, String source) {}
  @JsonInclude(JsonInclude.Include.NON_NULL)
  @Schema(name = "CareerSkillRequirement")
  public record Requirement(long skillId, String skill, String requiredLevel, int requiredRank,
                            String currentLevel, Integer currentRank, String source, boolean needsTraining) {}
  @JsonInclude(JsonInclude.Include.NON_NULL)
  @Schema(name = "CareerSkillsProfile")
  public record Profile(long revision, Long desiredRoleId, String desiredJobTitle, List<Skill> skills,
                        List<Evidence> evidence, List<Requirement> requirements) {}
  public record Observe(@NotNull @Positive Long skillId, @NotNull @Positive Long levelId) {}
  public record RoleRequirement(@NotNull @Positive Long skillId, @NotNull @Positive Long levelId) {}
  public enum Choice { INTERESTED, DISMISSED, UNDECIDED }
  public record Interest(@NotNull Choice choice) {}
  public record InterestCount(long interested) {}
  @JsonInclude(JsonInclude.Include.NON_NULL)
  @Schema(name = "CareerTrainingSuggestion")
  public record Training(long batchId, String code, String topic, String skillsToTeach, String provider, String venue,
                         String district, String state, LocalDate startsOn, LocalDate endsOn, LocalDateTime firstSession,
                         String status, long seatsAvailable, Choice choice, List<String> matchedSkills, boolean enrolled,
                         boolean relatedToJobGoal) {}
}
