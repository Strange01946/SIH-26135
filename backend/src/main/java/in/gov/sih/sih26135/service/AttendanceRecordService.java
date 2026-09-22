package in.gov.sih.sih26135.service;

import in.gov.sih.sih26135.dto.request.CreateAttendanceRecordRequest;
import in.gov.sih.sih26135.dto.request.UpdateAttendanceRecordRequest;
import in.gov.sih.sih26135.dto.response.AttendanceRecordResponse;
import java.time.LocalDate;
import java.util.List;

public interface AttendanceRecordService {

  AttendanceRecordResponse getById(Long id);

  List<AttendanceRecordResponse> getByEnrollmentId(Long enrollmentId);

  List<AttendanceRecordResponse> getByTraineeId(Long traineeId);

  List<AttendanceRecordResponse> getByBatchId(Long batchId);

  List<AttendanceRecordResponse> getByBatchAndDate(Long batchId, LocalDate sessionDate);

  List<AttendanceRecordResponse> getBySessionDate(LocalDate sessionDate);

  List<AttendanceRecordResponse> getByAttendanceStatusId(Long attendanceStatusId);

  AttendanceRecordResponse createAttendanceRecord(CreateAttendanceRecordRequest request);

  AttendanceRecordResponse updateAttendanceRecord(Long id, UpdateAttendanceRecordRequest request);

  void deleteAttendanceRecord(Long id);
}
