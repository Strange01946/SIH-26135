package in.gov.sih.sih26135.controller;

import in.gov.sih.sih26135.dto.request.CreateAttendanceRecordRequest;
import in.gov.sih.sih26135.dto.request.UpdateAttendanceRecordRequest;
import in.gov.sih.sih26135.dto.response.AttendanceRecordResponse;
import in.gov.sih.sih26135.response.ApiResponse;
import in.gov.sih.sih26135.service.AttendanceRecordService;
import jakarta.servlet.http.HttpServletRequest;
import java.time.LocalDate;
import java.util.List;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.security.access.prepost.PreAuthorize;

/**
 * REST controller for managing Attendance Record domain resources.
 *
 * <p>Base Route: /api/v1/attendance-records
 * Consumes: CreateAttendanceRecordRequest, UpdateAttendanceRecordRequest
 * Produces: AttendanceRecordResponse enveloped in ApiResponse
 */
@RestController
@RequestMapping("/api/v1/attendance-records")
@PreAuthorize("hasAuthority('enrollment.manage')")
public class AttendanceRecordController {

  private final AttendanceRecordService attendanceRecordService;

  public AttendanceRecordController(AttendanceRecordService attendanceRecordService) {
    this.attendanceRecordService = attendanceRecordService;
  }

  /**
   * Retrieves an attendance record by primary key identifier.
   *
   * @param id primary key identifier of the attendance record
   * @return 200 OK with AttendanceRecordResponse enveloped in ApiResponse
   */
  @GetMapping("/{id}")
  public ResponseEntity<ApiResponse<AttendanceRecordResponse>> getById(@PathVariable Long id) {
    AttendanceRecordResponse response = attendanceRecordService.getById(id);
    return ResponseEntity.ok(ApiResponse.ok(response));
  }

  /**
   * Retrieves all attendance records for a specific enrollment.
   *
   * @param enrollmentId training enrollment identifier
   * @return 200 OK with list of AttendanceRecordResponse enveloped in ApiResponse
   */
  @GetMapping(params = "enrollmentId")
  public ResponseEntity<ApiResponse<List<AttendanceRecordResponse>>> getByEnrollmentId(
      @RequestParam("enrollmentId") Long enrollmentId) {
    List<AttendanceRecordResponse> records = attendanceRecordService.getByEnrollmentId(enrollmentId);
    return ResponseEntity.ok(ApiResponse.ok(records));
  }

  /**
   * Retrieves all attendance records for a specific trainee.
   *
   * @param traineeId trainee identifier
   * @return 200 OK with list of AttendanceRecordResponse enveloped in ApiResponse
   */
  @GetMapping(params = "traineeId")
  public ResponseEntity<ApiResponse<List<AttendanceRecordResponse>>> getByTraineeId(
      @RequestParam("traineeId") Long traineeId) {
    List<AttendanceRecordResponse> records = attendanceRecordService.getByTraineeId(traineeId);
    return ResponseEntity.ok(ApiResponse.ok(records));
  }

  /**
   * Retrieves attendance records for a specific batch on a specific session date.
   *
   * @param batchId training batch identifier
   * @param sessionDate session date (ISO-8601 YYYY-MM-DD)
   * @return 200 OK with list of AttendanceRecordResponse enveloped in ApiResponse
   */
  @GetMapping(params = {"batchId", "sessionDate"})
  public ResponseEntity<ApiResponse<List<AttendanceRecordResponse>>> getByBatchAndDate(
      @RequestParam("batchId") Long batchId,
      @RequestParam("sessionDate") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate sessionDate) {
    List<AttendanceRecordResponse> records = attendanceRecordService.getByBatchAndDate(batchId, sessionDate);
    return ResponseEntity.ok(ApiResponse.ok(records));
  }

  /**
   * Retrieves all attendance records for a specific batch.
   *
   * @param batchId training batch identifier
   * @return 200 OK with list of AttendanceRecordResponse enveloped in ApiResponse
   */
  @GetMapping(params = "batchId")
  public ResponseEntity<ApiResponse<List<AttendanceRecordResponse>>> getByBatchId(
      @RequestParam("batchId") Long batchId) {
    List<AttendanceRecordResponse> records = attendanceRecordService.getByBatchId(batchId);
    return ResponseEntity.ok(ApiResponse.ok(records));
  }

  /**
   * Retrieves all attendance records for a specific session date across all batches.
   *
   * @param sessionDate session date (ISO-8601 YYYY-MM-DD)
   * @return 200 OK with list of AttendanceRecordResponse enveloped in ApiResponse
   */
  @GetMapping(params = "sessionDate")
  public ResponseEntity<ApiResponse<List<AttendanceRecordResponse>>> getBySessionDate(
      @RequestParam("sessionDate") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate sessionDate) {
    List<AttendanceRecordResponse> records = attendanceRecordService.getBySessionDate(sessionDate);
    return ResponseEntity.ok(ApiResponse.ok(records));
  }

  /**
   * Retrieves all attendance records with a specific attendance status.
   *
   * @param attendanceStatusId attendance status identifier
   * @return 200 OK with list of AttendanceRecordResponse enveloped in ApiResponse
   */
  @GetMapping(params = "attendanceStatusId")
  public ResponseEntity<ApiResponse<List<AttendanceRecordResponse>>> getByAttendanceStatusId(
      @RequestParam("attendanceStatusId") Long attendanceStatusId) {
    List<AttendanceRecordResponse> records = attendanceRecordService.getByAttendanceStatusId(attendanceStatusId);
    return ResponseEntity.ok(ApiResponse.ok(records));
  }

  /**
   * Creates a new attendance record.
   *
   * @param request attendance record creation payload
   * @param httpRequest HTTP servlet request for URI extraction
   * @return 201 Created with created AttendanceRecordResponse enveloped in ApiResponse
   */
  @PostMapping
  public ResponseEntity<ApiResponse<AttendanceRecordResponse>> createAttendanceRecord(
      @RequestBody CreateAttendanceRecordRequest request,
      HttpServletRequest httpRequest) {
    AttendanceRecordResponse response = attendanceRecordService.createAttendanceRecord(request);
    return ResponseEntity.status(HttpStatus.CREATED)
        .body(ApiResponse.success("Attendance record created successfully", response, httpRequest.getRequestURI()));
  }

  /**
   * Updates an existing attendance record.
   *
   * @param id primary key identifier of the attendance record to update
   * @param request attendance record update payload
   * @return 200 OK with updated AttendanceRecordResponse enveloped in ApiResponse
   */
  @PutMapping("/{id}")
  public ResponseEntity<ApiResponse<AttendanceRecordResponse>> updateAttendanceRecord(
      @PathVariable Long id,
      @RequestBody UpdateAttendanceRecordRequest request) {
    AttendanceRecordResponse response = attendanceRecordService.updateAttendanceRecord(id, request);
    return ResponseEntity.ok(ApiResponse.success("Attendance record updated successfully", response));
  }

  /**
   * Deletes an attendance record.
   *
   * @param id primary key identifier of the attendance record to delete
   * @return 200 OK with success confirmation message
   */
  @DeleteMapping("/{id}")
  public ResponseEntity<ApiResponse<Void>> deleteAttendanceRecord(@PathVariable Long id) {
    attendanceRecordService.deleteAttendanceRecord(id);
    return ResponseEntity.ok(ApiResponse.success("Attendance record deleted successfully"));
  }
}
