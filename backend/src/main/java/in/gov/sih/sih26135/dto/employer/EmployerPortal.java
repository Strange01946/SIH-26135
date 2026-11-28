package in.gov.sih.sih26135.dto.employer;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public final class EmployerPortal {
  private EmployerPortal() {}

  public record Reference(long id, String label) {}

  public record References(String employerName, List<Reference> jobRoles, List<Reference> qualifications,
                           List<Reference> districts, List<Reference> engagementTypes, List<Reference> payFrequencies,
                           List<Reference> skillLevels, List<Reference> rejectionReasons,
                           List<Reference> exitReasons, List<Reference> separationNatures) {}

  public record SkillTag(@NotNull @Positive Long skillId, @NotNull @Positive Long levelId) {}

  public record VacancyRequest(
      @NotBlank @Size(max = 200) String title,
      @Size(max = 8000) String description,
      @NotNull @Positive Long jobRoleId,
      @NotNull @Positive Long engagementTypeId,
      @NotNull @Positive Long qualificationId,
      @NotNull @Positive Long districtId,
      @NotNull @Positive @Max(65535) Integer vacancies,
      @NotNull @PositiveOrZero @Digits(integer = 10, fraction = 2) BigDecimal minimumPay,
      @NotNull @PositiveOrZero @Digits(integer = 10, fraction = 2) BigDecimal maximumPay,
      @NotNull @Positive Long payFrequencyId,
      LocalDate closingDate,
      @NotEmpty @Size(max = 20) List<@NotNull @Valid SkillTag> skills) {}

  public record VacancySkill(long id, String name, String level) {}

  public record Vacancy(long id, String code, String title, String jobRole, String qualification,
                        String district, int vacancies, BigDecimal minimumPay, BigDecimal maximumPay,
                        String payFrequency, String currency, String status, LocalDate closesOn,
                        List<VacancySkill> skills) {}

  public record Placement(long id, String number, String candidateName, String course, Long jobRoleId,
                          String jobRole, String placementStatus, String joiningStatus, LocalDate offeredOn,
                          LocalDate joinedOn, BigDecimal offeredPay, Long payFrequencyId, String payFrequency,
                          String currency, Long employmentId, boolean canConfirm) {}

  public record ConfirmPlacementRequest(
      @NotNull @Positive Long jobRoleId,
      @NotNull @PastOrPresent LocalDate joiningDate,
      @NotNull @PositiveOrZero @Digits(integer = 10, fraction = 2) BigDecimal offeredPay,
      @NotNull @Positive Long payFrequencyId) {}

  public record PostedJobHire(@NotBlank @Size(max = 80) String registrationNumber,
                             @Size(max = 80) String enrollmentNumber,
                             @NotNull @PastOrPresent LocalDate joiningDate,
                             @NotNull @PositiveOrZero @Digits(integer = 10, fraction = 2) BigDecimal pay,
                             @NotNull @Positive Long payFrequencyId) {}

  public enum Decision { ATTEST, DISPUTE, REPORT_EXIT }

  public record VerdictRequest(@NotNull Decision decision, @Size(max = 500) String notes,
                               @Positive Long rejectionReasonId, @PastOrPresent LocalDate exitDate,
                               @Positive Long exitReasonId, @Positive Long separationNatureId) {}

  public record Verification(long id, String number, String candidateName, String employmentNumber,
                             String jobRole, LocalDate startedOn, LocalDate endedOn, BigDecimal startingPay,
                             String payFrequency, String currency, LocalDateTime requestedAt, LocalDateTime dueAt,
                             String requestStatus, String employmentVerificationStatus, String verdict,
                             boolean canRespond, boolean canReportExit, Long evidenceId, String evidenceName) {}
}
