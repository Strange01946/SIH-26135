package in.gov.sih.sih26135.dto.venue;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.time.LocalDateTime;
import java.util.List;

public final class VenueBookingDto {
  private VenueBookingDto() {}

  public record SessionRequest(@NotNull LocalDateTime startsAt, @NotNull LocalDateTime endsAt) {}

  public record TemporaryVenue(
      @NotBlank @Size(max = 200) String name, @NotBlank @Size(max = 200) String address,
      @Positive long stateId, @Positive long districtId, @Positive int capacity,
      @NotBlank @Size(max = 400) String permissionToUse) {}

  public record NewBooking(
      @Positive Long centerId, @Valid TemporaryVenue temporaryVenue,
      @NotBlank @Size(max = 32) String batchCode,
      @Positive int seats,
      @NotEmpty @Size(max = 366) List<@NotNull @Valid SessionRequest> sessions,
      @Positive Long requirementId, @Size(max = 600) String skillsToTeach, @Size(max = 1500) String trainingNotes,
      @Positive Long minimumEducationLevelId, @Size(max = 1000) String recommendedPrerequisites) {}

  public record Reschedule(@NotEmpty @Size(max = 366) List<@NotNull @Valid SessionRequest> sessions) {}

  public record Review(@NotNull Boolean approved, @Size(max = 500) String reason) {}

  public enum CloseAction { COMPLETE, CANCEL }

  public record Close(@NotNull CloseAction action, @Size(max = 500) String reason) {}

  public record NewSharedVenue(
      @NotBlank @Size(max = 32) String code, @NotBlank @Size(max = 200) String name,
      @NotBlank @Size(max = 200) String address,
      @NotBlank(message = "PIN code is required")
      @Pattern(regexp = "^[1-9][0-9]{5}$", message = "Enter a valid six-digit PIN code") String pincode,
      @Positive long stateId, @Positive long districtId, @Positive int capacity) {}

  public record Blackout(
      @NotNull LocalDateTime startsAt, @NotNull LocalDateTime endsAt, @NotBlank @Size(max = 300) String reason) {}

  public record SharedVenue(long id, String code, String name, String address, long stateId, long districtId, int capacity) {}

  public record BusySlot(LocalDateTime startsAt, LocalDateTime endsAt, Long blackoutId, String reason) {}

  public record Booking(
      long id, long providerId, String providerName, Long centerId, Long batchId,
      Long replacesBookingId, String venueType, String status,
      String batchCode, Long courseId, String courseName, Long programId, String programName,
      int seats, String venueName, String venueAddress, long stateId, long districtId, int venueCapacity,
      String premisesPermission, String decisionNote, LocalDateTime createdAt, LocalDateTime reviewedAt,
      String batchStatus, Long requirementId, String skillsToTeach, String trainingNotes, TrainingAdmission admission) {}

  public record Session(LocalDateTime startsAt, LocalDateTime endsAt, LocalDateTime reservedAt, LocalDateTime releasedAt) {}

  public record Details(Booking booking, List<Session> sessions) {}
}
