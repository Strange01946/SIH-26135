package in.gov.sih.sih26135.dto.assessor;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public final class BatchAssessment {
  private BatchAssessment() {}

  public record PublishBatchAssessmentRequest(@NotBlank @Size(max = 1200) String brief) {}
  public record ClaimBatchAssessmentRequest(@NotNull LocalDateTime startsAt,
                                            @NotBlank @Size(max = 200) String venue) {}
  public record Publication(long assessmentId, long batchId, String batchCode, String title, String skills,
                            String provider, String trainingVenue, String district, String state,
                            LocalDateTime startsAt, String venue, String brief, String assessor, String status,
                            int candidates, boolean checkedIn, boolean canComplete, LocalDateTime completedAt) {}
  public record Readiness(String batchCode, String batchStatus, int candidates, int sessions,
                          long outstandingMarks, boolean canPublish, String blocker, Publication publication) {}
  public record Candidate(long traineeId, String name, String registrationNumber, Boolean appeared, int present,
                          int trainingSessions, Long resultId, String outcome, BigDecimal theory,
                          BigDecimal practical, BigDecimal viva, BigDecimal total, Long certificateId) {}
  public record Review(Publication publication, List<Candidate> candidates) {}
  public record Appointment(String batchCode, String title, LocalDateTime startsAt, String venue, String brief,
                            String assessor, String status, LocalDateTime completedAt, Long resultId, String outcome,
                            BigDecimal theory, BigDecimal practical, BigDecimal viva, BigDecimal total,
                            Long certificateId) {}
}
