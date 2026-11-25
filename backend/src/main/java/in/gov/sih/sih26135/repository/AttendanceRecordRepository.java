package in.gov.sih.sih26135.repository;

import in.gov.sih.sih26135.entity.AttendanceRecord;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface AttendanceRecordRepository extends JpaRepository<AttendanceRecord, Long> {

  Optional<AttendanceRecord> findByTrainingEnrollmentIdAndSessionDateAndSessionSequence(
      Long enrollmentId, LocalDate sessionDate, Integer sessionSequence);

  boolean existsByTrainingEnrollmentIdAndSessionDateAndSessionSequence(
      Long enrollmentId, LocalDate sessionDate, Integer sessionSequence);

  List<AttendanceRecord> findByTrainingEnrollmentId(Long enrollmentId);

  List<AttendanceRecord> findByTraineeId(Long traineeId);

  List<AttendanceRecord> findByTrainingBatchId(Long batchId);

  List<AttendanceRecord> findByTrainingBatchIdAndSessionDate(Long batchId, LocalDate sessionDate);

  List<AttendanceRecord> findBySessionDate(LocalDate sessionDate);

  List<AttendanceRecord> findByAttendanceStatusId(Long attendanceStatusId);

  @Query("SELECT a.trainingBatch.id FROM AttendanceRecord a WHERE a.id=:id")
  Optional<Long> findBatchIdForRecord(@Param("id") Long id);

  @Query(value = """
      SELECT COUNT(*) FROM venue_booking_sessions s JOIN venue_bookings b USING (booking_id)
      WHERE b.batch_id=:batch AND s.reserved_at IS NOT NULL AND s.released_at IS NULL
        AND s.starts_at>=:dayStart AND s.starts_at<:dayEnd AND s.starts_at<=:now
      """, nativeQuery = true)
  long countStartedVenueSessions(@Param("batch") Long batchId, @Param("dayStart") LocalDateTime dayStart,
      @Param("dayEnd") LocalDateTime dayEnd, @Param("now") LocalDateTime now);
}
