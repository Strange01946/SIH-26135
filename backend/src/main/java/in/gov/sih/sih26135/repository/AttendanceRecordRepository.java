package in.gov.sih.sih26135.repository;

import in.gov.sih.sih26135.entity.AttendanceRecord;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
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
}
