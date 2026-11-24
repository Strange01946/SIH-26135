package in.gov.sih.sih26135.dto.trainee;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public final class TraineeSurvey {
  private TraineeSurvey() {}

  public record Assignment(long taskId, String name, LocalDate closesOn) {}

  public record Submission(long id, String name, LocalDateTime submittedAt) {}

  public record Overview(boolean followupConsentGranted, List<Assignment> assignments,
                         List<Submission> recentSubmissions) {}

  public record Option(long id, String label) {}

  public record Question(long id, String text, String type, boolean required, List<Option> options) {}

  public record Questionnaire(Assignment assignment, long versionId, List<Question> questions) {}

  public record Answer(
      @NotNull @Positive Long questionId,
      @Size(max = 1000) String text,
      @PositiveOrZero @Digits(integer = 10, fraction = 2) BigDecimal number,
      Boolean yes,
      LocalDate date,
      List<@NotNull @Positive Long> optionIds) {}

  public record SubmitRequest(@NotNull @Positive Long versionId,
                              @NotNull List<@NotNull @Valid Answer> answers) {}
}
