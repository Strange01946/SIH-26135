package in.gov.sih.sih26135.service.impl;

import in.gov.sih.sih26135.dto.response.AttendanceStatusResponse;
import in.gov.sih.sih26135.entity.RefAttendanceStatus;
import in.gov.sih.sih26135.exception.BadRequestException;
import in.gov.sih.sih26135.exception.ResourceNotFoundException;
import in.gov.sih.sih26135.mapper.AttendanceStatusMapper;
import in.gov.sih.sih26135.repository.RefAttendanceStatusRepository;
import in.gov.sih.sih26135.service.AttendanceStatusReferenceService;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class AttendanceStatusReferenceServiceImpl implements AttendanceStatusReferenceService {

  private final RefAttendanceStatusRepository refAttendanceStatusRepository;
  private final AttendanceStatusMapper attendanceStatusMapper;

  public AttendanceStatusReferenceServiceImpl(
      RefAttendanceStatusRepository refAttendanceStatusRepository,
      AttendanceStatusMapper attendanceStatusMapper) {
    this.refAttendanceStatusRepository = refAttendanceStatusRepository;
    this.attendanceStatusMapper = attendanceStatusMapper;
  }

  @Override
  public List<AttendanceStatusResponse> getAllAttendanceStatuses() {
    return refAttendanceStatusRepository.findAllByOrderBySortOrderAsc().stream()
        .map(attendanceStatusMapper::toResponse)
        .toList();
  }

  @Override
  public AttendanceStatusResponse getById(Long id) {
    if (id == null) {
      throw new BadRequestException("Attendance status ID is required");
    }
    RefAttendanceStatus status = refAttendanceStatusRepository.findById(id)
        .orElseThrow(() -> new ResourceNotFoundException("RefAttendanceStatus", "id"));
    return attendanceStatusMapper.toResponse(status);
  }

  @Override
  public AttendanceStatusResponse getByCode(String statusCode) {
    if (statusCode == null || statusCode.isBlank()) {
      throw new BadRequestException("Status code is required");
    }
    RefAttendanceStatus status = refAttendanceStatusRepository.findByStatusCode(statusCode.trim())
        .orElseThrow(() -> new ResourceNotFoundException("RefAttendanceStatus", "statusCode"));
    return attendanceStatusMapper.toResponse(status);
  }
}
