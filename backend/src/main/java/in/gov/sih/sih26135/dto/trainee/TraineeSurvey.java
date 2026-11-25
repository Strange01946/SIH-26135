package in.gov.sih.sih26135.dto.trainee;

import com.fasterxml.jackson.annotation.JsonInclude;
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

  @JsonInclude(JsonInclude.Include.NON_NULL)
  public record Assignment(long taskId, String name, LocalDate closesOn) {}

  @JsonInclude(JsonInclude.Include.NON_NULL)
  public record Submission(long id, String name, LocalDateTime submittedAt) {}

  public record Overview(boolean followupConsentGranted, List<Assignment> assignments,
                         List<Submission> recentSubmissions, boolean careerUpdateAvailable) {}

  public record CareerUpdateQuestionnaire(String name, long versionId, List<Question> questions,
                                          List<Answer> initialAnswers) {}

  @JsonInclude(JsonInclude.Include.NON_NULL)
  public record CareerUpdate(long submissionId, LocalDateTime submittedAt, long statusId, String statusCode,
                             String status, String organization, String role, LocalDate since,
                             BigDecimal monthlyIncome, String location, String skills, String barriers) {}

  @JsonInclude(JsonInclude.Include.NON_NULL)
  public record RecordedAnswer(String question, String value) {}

  public record SubmissionDetail(Submission submission, List<RecordedAnswer> answers) {}

  @JsonInclude(JsonInclude.Include.NON_NULL)
  public record Campaign(long id, String name, String survey, LocalDate closesOn) {}

  public record StaffAssignment(long id, String name, LocalDate scheduledOn, String status) {}

  public record StaffOverview(boolean followupConsentGranted, List<Campaign> campaigns,
                              List<StaffAssignment> assignments, List<Submission> recentSubmissions) {}

  public record ScheduleRequest(@NotNull @Positive Long campaignId, @NotNull LocalDate scheduledOn) {}

  public record Option(long id, String label) {}

  public record Question(long id, String text, String type, boolean required, List<Option> options) {}

  public record Questionnaire(Assignment assignment, long versionId, List<Question> questions) {}

  @JsonInclude(JsonInclude.Include.NON_NULL)
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
