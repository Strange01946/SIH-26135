package in.gov.sih.sih26135.dto.trainee;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public final class TrainingRegistration {
  private TrainingRegistration() {}

  public record RegistrationBatch(long id, String code, String title, String skills, String provider, String venue,
                      long districtId, String district, String state, LocalDate startsOn, LocalDate endsOn,
                      LocalDateTime firstSession, String status, boolean governmentLed, long seatsAvailable) {}

  public record RegistrationBatchPage(List<RegistrationBatch> batches, boolean hasNext) {}

  public record TrainingTimetableSession(LocalDateTime startsAt, LocalDateTime endsAt) {}

  public record TrainingBatchDetails(RegistrationBatch batch, String address, List<TrainingTimetableSession> sessions, boolean registrationOpen,
                        boolean consentGranted, String enrollmentNumber, String enrollmentStatus) {}

  public record TrainingRosterEntry(long enrollmentId, String enrollmentNumber, String name, String registrationNumber,
                            String status, String email, String phone) {}

  public record TrainingRosterPage(List<TrainingRosterEntry> entries, boolean hasNext) {}
}
