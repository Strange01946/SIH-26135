package in.gov.sih.sih26135.dto.venue;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;
import java.util.List;

public final class BatchSessionAttendance {
  private BatchSessionAttendance() {}

  public record AttendanceSession(long id, LocalDateTime startsAt, LocalDateTime endsAt,
                                  boolean started, boolean canMark) {}

  public record AttendanceTimetable(String batchCode, String title, String batchStatus, boolean attendanceLocked,
                                    List<AttendanceSession> sessions) {}

  public record AttendanceRosterRow(long enrollmentId, String name, String registrationNumber, boolean eligible,
                                    String status, String remarks, String version) {}

  public record AttendanceSessionRoster(AttendanceSession session, List<AttendanceRosterRow> rows) {}

  public record SessionAttendanceMark(@Positive long enrollmentId, @NotBlank @Size(max = 32) String status,
                                     @Size(max = 255) String remarks, @Size(max = 36) String expectedVersion) {}

  public record SaveAttendanceRoster(@NotEmpty List<@NotNull @Valid SessionAttendanceMark> marks) {}
}
