package in.gov.sih.sih26135.controller;

import in.gov.sih.sih26135.dto.venue.BatchSessionAttendance.AttendanceSessionRoster;
import in.gov.sih.sih26135.dto.venue.BatchSessionAttendance.AttendanceTimetable;
import in.gov.sih.sih26135.dto.venue.BatchSessionAttendance.SaveAttendanceRoster;
import in.gov.sih.sih26135.response.ApiResponse;
import in.gov.sih.sih26135.service.BatchSessionAttendanceService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Validated
@RequestMapping("/api/v1/training-batches/{batchId}/attendance/sessions")
@PreAuthorize("hasRole('training_provider') and hasAuthority('enrollment.manage')")
@SecurityRequirement(name = "bearerAuth")
public class BatchSessionAttendanceController {
  private final BatchSessionAttendanceService attendance;

  public BatchSessionAttendanceController(BatchSessionAttendanceService attendance) {
    this.attendance = attendance;
  }

  @GetMapping
  public ApiResponse<AttendanceTimetable> batchAttendanceTimetable(@PathVariable @Positive long batchId) {
    return ApiResponse.ok(attendance.timetable(batchId));
  }

  @GetMapping("/{sessionId}")
  public ApiResponse<AttendanceSessionRoster> batchAttendanceRoster(
      @PathVariable @Positive long batchId, @PathVariable @Positive long sessionId) {
    return ApiResponse.ok(attendance.roster(batchId, sessionId));
  }

  @PutMapping("/{sessionId}")
  public ApiResponse<AttendanceSessionRoster> saveBatchAttendanceRoster(
      @PathVariable @Positive long batchId, @PathVariable @Positive long sessionId,
      @Valid @RequestBody SaveAttendanceRoster request) {
    return ApiResponse.ok(attendance.save(batchId, sessionId, request));
  }
}
