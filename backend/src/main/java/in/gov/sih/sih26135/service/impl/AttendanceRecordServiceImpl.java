package in.gov.sih.sih26135.service.impl;

import in.gov.sih.sih26135.dto.request.CreateAttendanceRecordRequest;
import in.gov.sih.sih26135.dto.request.UpdateAttendanceRecordRequest;
import in.gov.sih.sih26135.dto.response.AttendanceRecordResponse;
import in.gov.sih.sih26135.entity.AttendanceRecord;
import in.gov.sih.sih26135.entity.RefAttendanceStatus;
import in.gov.sih.sih26135.entity.Trainee;
import in.gov.sih.sih26135.entity.TrainingBatch;
import in.gov.sih.sih26135.entity.TrainingEnrollment;
import in.gov.sih.sih26135.exception.BadRequestException;
import in.gov.sih.sih26135.exception.ConflictException;
import in.gov.sih.sih26135.exception.ResourceNotFoundException;
import in.gov.sih.sih26135.mapper.AttendanceRecordMapper;
import in.gov.sih.sih26135.repository.AttendanceRecordRepository;
import in.gov.sih.sih26135.repository.RefAttendanceStatusRepository;
import in.gov.sih.sih26135.repository.TrainingEnrollmentRepository;
import in.gov.sih.sih26135.repository.UserRepository;
import in.gov.sih.sih26135.service.AttendanceRecordService;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class AttendanceRecordServiceImpl implements AttendanceRecordService {

  private final AttendanceRecordRepository attendanceRecordRepository;
  private final TrainingEnrollmentRepository trainingEnrollmentRepository;
  private final RefAttendanceStatusRepository refAttendanceStatusRepository;
  private final UserRepository userRepository;
  private final AttendanceRecordMapper attendanceRecordMapper;

  public AttendanceRecordServiceImpl(
      AttendanceRecordRepository attendanceRecordRepository,
      TrainingEnrollmentRepository trainingEnrollmentRepository,
      RefAttendanceStatusRepository refAttendanceStatusRepository,
      UserRepository userRepository,
      AttendanceRecordMapper attendanceRecordMapper) {
    this.attendanceRecordRepository = attendanceRecordRepository;
    this.trainingEnrollmentRepository = trainingEnrollmentRepository;
    this.refAttendanceStatusRepository = refAttendanceStatusRepository;
    this.userRepository = userRepository;
    this.attendanceRecordMapper = attendanceRecordMapper;
  }

  @Override
  public AttendanceRecordResponse getById(Long id) {
    if (id == null) {
      throw new BadRequestException("Attendance ID is required");
    }
    AttendanceRecord record = attendanceRecordRepository.findById(id)
        .orElseThrow(() -> new ResourceNotFoundException("AttendanceRecord", "id"));
    return attendanceRecordMapper.toResponse(record);
  }

  @Override
  public List<AttendanceRecordResponse> getByEnrollmentId(Long enrollmentId) {
    if (enrollmentId == null) {
      throw new BadRequestException("Enrollment ID is required");
    }
    return attendanceRecordRepository.findByTrainingEnrollmentId(enrollmentId).stream()
        .map(attendanceRecordMapper::toResponse)
        .toList();
  }

  @Override
  public List<AttendanceRecordResponse> getByTraineeId(Long traineeId) {
    if (traineeId == null) {
      throw new BadRequestException("Trainee ID is required");
    }
    return attendanceRecordRepository.findByTraineeId(traineeId).stream()
        .map(attendanceRecordMapper::toResponse)
        .toList();
  }

  @Override
  public List<AttendanceRecordResponse> getByBatchId(Long batchId) {
    if (batchId == null) {
      throw new BadRequestException("Batch ID is required");
    }
    return attendanceRecordRepository.findByTrainingBatchId(batchId).stream()
        .map(attendanceRecordMapper::toResponse)
        .toList();
  }

  @Override
  public List<AttendanceRecordResponse> getByBatchAndDate(Long batchId, LocalDate sessionDate) {
    if (batchId == null) {
      throw new BadRequestException("Batch ID is required");
    }
    if (sessionDate == null) {
      throw new BadRequestException("Session date is required");
    }
    return attendanceRecordRepository.findByTrainingBatchIdAndSessionDate(batchId, sessionDate).stream()
        .map(attendanceRecordMapper::toResponse)
        .toList();
  }

  @Override
  public List<AttendanceRecordResponse> getBySessionDate(LocalDate sessionDate) {
    if (sessionDate == null) {
      throw new BadRequestException("Session date is required");
    }
    return attendanceRecordRepository.findBySessionDate(sessionDate).stream()
        .map(attendanceRecordMapper::toResponse)
        .toList();
  }

  @Override
  public List<AttendanceRecordResponse> getByAttendanceStatusId(Long attendanceStatusId) {
    if (attendanceStatusId == null) {
      throw new BadRequestException("Attendance status ID is required");
    }
    return attendanceRecordRepository.findByAttendanceStatusId(attendanceStatusId).stream()
        .map(attendanceRecordMapper::toResponse)
        .toList();
  }

  @Override
  @Transactional
  public AttendanceRecordResponse createAttendanceRecord(CreateAttendanceRecordRequest request) {
    if (request == null) {
      throw new BadRequestException("Attendance record creation request cannot be null");
    }
    if (request.getEnrollmentId() == null) {
      throw new BadRequestException("Enrollment ID is required");
    }
    if (request.getSessionDate() == null) {
      throw new BadRequestException("Session date is required");
    }
    if (request.getAttendanceStatusId() == null) {
      throw new BadRequestException("Attendance status ID is required");
    }

    int sessionSequence = request.getSessionSequence() != null ? request.getSessionSequence() : 1;
    if (sessionSequence < 1) {
      throw new BadRequestException("Session sequence must be greater than or equal to 1", "INVALID_SESSION_SEQUENCE");
    }

    if (request.getSessionStartTime() != null && request.getSessionEndTime() != null
        && request.getSessionEndTime().isBefore(request.getSessionStartTime())) {
      throw new BadRequestException("Session end time cannot precede session start time", "INVALID_SESSION_TIMES");
    }

    if (attendanceRecordRepository.existsByTrainingEnrollmentIdAndSessionDateAndSessionSequence(
        request.getEnrollmentId(), request.getSessionDate(), sessionSequence)) {
      throw new ConflictException(
          "Attendance record already exists for this enrollment, date, and sequence",
          "ATTENDANCE_ALREADY_EXISTS"
      );
    }

    TrainingEnrollment enrollment = trainingEnrollmentRepository.findById(request.getEnrollmentId())
        .orElseThrow(() -> new ResourceNotFoundException("TrainingEnrollment", "enrollmentId"));

    Trainee trainee = enrollment.getTrainee();
    if (request.getTraineeId() != null && !request.getTraineeId().equals(trainee.getId())) {
      throw new BadRequestException("Trainee ID does not match enrollment trainee", "TRAINEE_ENROLLMENT_MISMATCH");
    }

    TrainingBatch batch = enrollment.getTrainingBatch();
    if (request.getBatchId() != null && !request.getBatchId().equals(batch.getId())) {
      throw new BadRequestException("Batch ID does not match enrollment batch", "BATCH_ENROLLMENT_MISMATCH");
    }

    RefAttendanceStatus status = refAttendanceStatusRepository.findById(request.getAttendanceStatusId())
        .orElseThrow(() -> new ResourceNotFoundException("RefAttendanceStatus", "attendanceStatusId"));

    if (request.getMarkedByUserId() != null) {
      userRepository.findById(request.getMarkedByUserId())
          .orElseThrow(() -> new ResourceNotFoundException("User", "markedByUserId"));
    }

    AttendanceRecord entity = attendanceRecordMapper.toEntity(request, enrollment, trainee, batch, status);
    entity.setSessionSequence(sessionSequence);
    if (request.getRemarks() != null) {
      entity.setRemarks(request.getRemarks().trim());
    }

    LocalDateTime now = LocalDateTime.now();
    entity.setCreatedAt(now);
    entity.setUpdatedAt(now);

    AttendanceRecord saved = attendanceRecordRepository.save(entity);
    return attendanceRecordMapper.toResponse(saved);
  }

  @Override
  @Transactional
  public AttendanceRecordResponse updateAttendanceRecord(Long id, UpdateAttendanceRecordRequest request) {
    if (id == null) {
      throw new BadRequestException("Attendance ID is required");
    }
    if (request == null) {
      throw new BadRequestException("Attendance record update request cannot be null");
    }

    AttendanceRecord record = attendanceRecordRepository.findById(id)
        .orElseThrow(() -> new ResourceNotFoundException("AttendanceRecord", "id"));

    LocalTime effectiveStartTime = request.getSessionStartTime() != null
        ? request.getSessionStartTime()
        : record.getSessionStartTime();

    LocalTime effectiveEndTime = request.getSessionEndTime() != null
        ? request.getSessionEndTime()
        : record.getSessionEndTime();

    if (effectiveStartTime != null && effectiveEndTime != null
        && effectiveEndTime.isBefore(effectiveStartTime)) {
      throw new BadRequestException("Session end time cannot precede session start time", "INVALID_SESSION_TIMES");
    }

    if (request.getSessionStartTime() != null) {
      record.setSessionStartTime(request.getSessionStartTime());
    }
    if (request.getSessionEndTime() != null) {
      record.setSessionEndTime(request.getSessionEndTime());
    }

    if (request.getAttendanceStatusId() != null) {
      RefAttendanceStatus status = refAttendanceStatusRepository.findById(request.getAttendanceStatusId())
          .orElseThrow(() -> new ResourceNotFoundException("RefAttendanceStatus", "attendanceStatusId"));
      record.setAttendanceStatus(status);
    }

    if (request.getMarkedAt() != null) {
      record.setMarkedAt(request.getMarkedAt());
    }

    if (request.getMarkedByUserId() != null) {
      userRepository.findById(request.getMarkedByUserId())
          .orElseThrow(() -> new ResourceNotFoundException("User", "markedByUserId"));
      record.setMarkedByUserId(request.getMarkedByUserId());
    }

    if (request.getRemarks() != null) {
      record.setRemarks(request.getRemarks().trim());
    }

    LocalDateTime now = LocalDateTime.now();
    record.setUpdatedAt(now);

    AttendanceRecord saved = attendanceRecordRepository.save(record);
    return attendanceRecordMapper.toResponse(saved);
  }

  @Override
  @Transactional
  public void deleteAttendanceRecord(Long id) {
    if (id == null) {
      throw new BadRequestException("Attendance ID is required");
    }
    AttendanceRecord record = attendanceRecordRepository.findById(id)
        .orElseThrow(() -> new ResourceNotFoundException("AttendanceRecord", "id"));

    attendanceRecordRepository.delete(record);
  }
}
