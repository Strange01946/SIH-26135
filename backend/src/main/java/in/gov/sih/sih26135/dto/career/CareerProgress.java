package in.gov.sih.sih26135.dto.career;

import com.fasterxml.jackson.annotation.JsonInclude;
import in.gov.sih.sih26135.dto.response.EmploymentExitEventResponse;
import in.gov.sih.sih26135.dto.response.TraineeUnemploymentEventResponse;
import in.gov.sih.sih26135.dto.trainee.TraineeSurvey;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public final class CareerProgress {
  private CareerProgress() {}

  public record Candidate(long id, String registrationNumber, String name, String statusCode, String status) {}

  public record Employment(long id, String number, String employer, String role, LocalDate startDate,
                           LocalDate endDate, String status, String verificationStatus, boolean canRecordExit) {}

  @JsonInclude(JsonInclude.Include.NON_NULL)
  public record CandidateHistory(Candidate candidate, List<Employment> employments,
                                 List<EmploymentExitEventResponse> exitEvents,
                                 List<TraineeUnemploymentEventResponse> unemploymentEvents,
                                 TraineeSurvey.CareerUpdate reportedUpdate) {}

  public enum RetentionStatus { NOT_YET_DUE, RETAINED, ENDED_BEFORE_MILESTONE }
  public record RetentionMilestone(int months, LocalDate dueOn, RetentionStatus status) {}
  public record Wage(LocalDate date, BigDecimal amount, String currency, String frequency, String verificationStatus,
                     Integer observationMonth, boolean startingWage) {}
  public record EmploymentRetention(Employment employment, LocalDate asOf, List<RetentionMilestone> milestones,
                                    List<Wage> wages) {}

  public enum ExitType {
    VOLUNTARY("VOLUNTARY", "ENDED", "OTHER"),
    CONTRACT_END("CONTRACT_END", "COMPLETED", "CONTRACT_END"),
    LAY_OFF("INVOLUNTARY", "TERMINATED", "EMPLOYER_TERMINATION");

    private final String nature;
    private final String spellStatus;
    private final String defaultReason;

    ExitType(String nature, String spellStatus, String defaultReason) {
      this.nature = nature;
      this.spellStatus = spellStatus;
      this.defaultReason = defaultReason;
    }

    public String nature() { return nature; }
    public String spellStatus() { return spellStatus; }
    public String defaultReason() { return defaultReason; }
  }

  public record Reference(long id, String name) {}
  public record ExitOptions(List<Reference> unemploymentReasons, List<Reference> exitReasons) {}

  public record EmploymentExitRequest(
      @NotNull @PastOrPresent LocalDate separationDate,
      @NotNull ExitType exitType,
      @Positive Long exitReasonId,
      boolean confirmUnemployment,
      @Positive Long unemploymentReasonId,
      @Size(max = 500) String remarks) {}

  public record ReportUnemploymentRequest(@NotNull @Positive Long unemploymentReasonId,
                                          @Size(max = 500) String remarks) {}
}
