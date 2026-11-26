package in.gov.sih.sih26135.dto.venue;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

public final class TrainingRequirementDto {
  private TrainingRequirementDto() {}

  public record Publish(
      @NotBlank @Size(max = 150) String title,
      @NotBlank @Size(max = 600) String skillsToTeach,
      @Size(max = 1500) String description,
      @Positive long stateId, @Positive long districtId, @NotNull @Positive Long centerId, @Positive int seats,
      @NotNull LocalDate earliestDate, @NotNull LocalDate latestDate,
      @NotNull LocalTime dailyStartTime, @NotNull LocalTime dailyEndTime,
      @Positive Long minimumEducationLevelId, @Size(max = 1000) String recommendedPrerequisites) {}

  public record Apply(
      @NotNull LocalDate startDate, @NotNull LocalDate endDate,
      @NotNull LocalTime dailyStartTime, @NotNull LocalTime dailyEndTime,
      @Size(max = 1500) String deliveryNotes) {}

  public record SelectProvider(@Positive long applicationId) {}

  public record SetCentre(@NotNull @Positive Long centerId) {}

  public record Requirement(
      long id, String title, String skillsToTeach, String description,
      long stateId, String stateName, long districtId, String districtName, int seats,
      Long centerId, String centerName, String centerAddress,
      LocalDate earliestDate, LocalDate latestDate, LocalTime dailyStartTime, LocalTime dailyEndTime,
      String status, boolean acceptingApplications, Long selectedProviderId, String selectedProviderName,
      LocalDateTime publishedAt, LocalDateTime selectedAt, Long bookingId, String bookingStatus, TrainingAdmission admission) {}

  public record Application(
      long id, long requirementId, String requirementTitle,
      long providerId, String providerName, String status,
      LocalDate startDate, LocalDate endDate, LocalTime dailyStartTime, LocalTime dailyEndTime,
      String deliveryNotes, LocalDateTime submittedAt) {}

  public record RequirementDetails(Requirement requirement, List<Application> applications, Application myApplication) {}
}
