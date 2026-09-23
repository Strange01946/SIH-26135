package in.gov.sih.sih26135.service.impl;

import in.gov.sih.sih26135.dto.request.CreateSkillGapAssessmentRequest;
import in.gov.sih.sih26135.dto.request.UpdateSkillGapAssessmentRequest;
import in.gov.sih.sih26135.dto.response.SkillGapAssessmentResponse;
import in.gov.sih.sih26135.entity.AssessmentResult;
import in.gov.sih.sih26135.entity.Certification;
import in.gov.sih.sih26135.entity.Course;
import in.gov.sih.sih26135.entity.EmploymentRecord;
import in.gov.sih.sih26135.entity.EmploymentVerification;
import in.gov.sih.sih26135.entity.FollowupTask;
import in.gov.sih.sih26135.entity.JobPosting;
import in.gov.sih.sih26135.entity.JobRole;
import in.gov.sih.sih26135.entity.PlacementRecord;
import in.gov.sih.sih26135.entity.RefSkillGapAssessmentStatus;
import in.gov.sih.sih26135.entity.RefSkillGapSource;
import in.gov.sih.sih26135.entity.SkillGap;
import in.gov.sih.sih26135.entity.SkillGapAssessment;
import in.gov.sih.sih26135.entity.SurveyResponse;
import in.gov.sih.sih26135.entity.Trainee;
import in.gov.sih.sih26135.entity.TraineeAssessment;
import in.gov.sih.sih26135.entity.TrainingBatch;
import in.gov.sih.sih26135.entity.TrainingEnrollment;
import in.gov.sih.sih26135.exception.BadRequestException;
import in.gov.sih.sih26135.exception.ConflictException;
import in.gov.sih.sih26135.exception.ResourceNotFoundException;
import in.gov.sih.sih26135.mapper.SkillGapAssessmentMapper;
import in.gov.sih.sih26135.repository.AssessmentResultRepository;
import in.gov.sih.sih26135.repository.CertificationRepository;
import in.gov.sih.sih26135.repository.CourseRepository;
import in.gov.sih.sih26135.repository.EmploymentRecordRepository;
import in.gov.sih.sih26135.repository.EmploymentVerificationRepository;
import in.gov.sih.sih26135.repository.FollowupTaskRepository;
import in.gov.sih.sih26135.repository.JobPostingRepository;
import in.gov.sih.sih26135.repository.JobRoleRepository;
import in.gov.sih.sih26135.repository.PlacementRecordRepository;
import in.gov.sih.sih26135.repository.RefSkillGapAssessmentStatusRepository;
import in.gov.sih.sih26135.repository.RefSkillGapSourceRepository;
import in.gov.sih.sih26135.repository.SkillGapAssessmentRepository;
import in.gov.sih.sih26135.repository.SkillGapRepository;
import in.gov.sih.sih26135.repository.SurveyResponseRepository;
import in.gov.sih.sih26135.repository.TraineeAssessmentRepository;
import in.gov.sih.sih26135.repository.TraineeRepository;
import in.gov.sih.sih26135.repository.TrainingBatchRepository;
import in.gov.sih.sih26135.repository.TrainingEnrollmentRepository;
import in.gov.sih.sih26135.repository.UserRepository;
import in.gov.sih.sih26135.service.SkillGapAssessmentService;
import java.time.LocalDate;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class SkillGapAssessmentServiceImpl implements SkillGapAssessmentService {

  private final SkillGapAssessmentRepository assessmentRepository;
  private final TraineeRepository traineeRepository;
  private final TrainingEnrollmentRepository enrollmentRepository;
  private final CourseRepository courseRepository;
  private final TrainingBatchRepository batchRepository;
  private final JobRoleRepository jobRoleRepository;
  private final JobPostingRepository jobPostingRepository;
  private final EmploymentRecordRepository employmentRecordRepository;
  private final PlacementRecordRepository placementRecordRepository;
  private final TraineeAssessmentRepository traineeAssessmentRepository;
  private final AssessmentResultRepository assessmentResultRepository;
  private final CertificationRepository certificationRepository;
  private final SurveyResponseRepository surveyResponseRepository;
  private final FollowupTaskRepository followupTaskRepository;
  private final EmploymentVerificationRepository employmentVerificationRepository;
  private final RefSkillGapSourceRepository skillGapSourceRepository;
  private final RefSkillGapAssessmentStatusRepository skillGapAssessmentStatusRepository;
  private final UserRepository userRepository;
  private final SkillGapRepository skillGapRepository;
  private final SkillGapAssessmentMapper mapper;

  public SkillGapAssessmentServiceImpl(
      SkillGapAssessmentRepository assessmentRepository,
      TraineeRepository traineeRepository,
      TrainingEnrollmentRepository enrollmentRepository,
      CourseRepository courseRepository,
      TrainingBatchRepository batchRepository,
      JobRoleRepository jobRoleRepository,
      JobPostingRepository jobPostingRepository,
      EmploymentRecordRepository employmentRecordRepository,
      PlacementRecordRepository placementRecordRepository,
      TraineeAssessmentRepository traineeAssessmentRepository,
      AssessmentResultRepository assessmentResultRepository,
      CertificationRepository certificationRepository,
      SurveyResponseRepository surveyResponseRepository,
      FollowupTaskRepository followupTaskRepository,
      EmploymentVerificationRepository employmentVerificationRepository,
      RefSkillGapSourceRepository skillGapSourceRepository,
      RefSkillGapAssessmentStatusRepository skillGapAssessmentStatusRepository,
      UserRepository userRepository,
      SkillGapRepository skillGapRepository,
      SkillGapAssessmentMapper mapper) {
    this.assessmentRepository = assessmentRepository;
    this.traineeRepository = traineeRepository;
    this.enrollmentRepository = enrollmentRepository;
    this.courseRepository = courseRepository;
    this.batchRepository = batchRepository;
    this.jobRoleRepository = jobRoleRepository;
    this.jobPostingRepository = jobPostingRepository;
    this.employmentRecordRepository = employmentRecordRepository;
    this.placementRecordRepository = placementRecordRepository;
    this.traineeAssessmentRepository = traineeAssessmentRepository;
    this.assessmentResultRepository = assessmentResultRepository;
    this.certificationRepository = certificationRepository;
    this.surveyResponseRepository = surveyResponseRepository;
    this.followupTaskRepository = followupTaskRepository;
    this.employmentVerificationRepository = employmentVerificationRepository;
    this.skillGapSourceRepository = skillGapSourceRepository;
    this.skillGapAssessmentStatusRepository = skillGapAssessmentStatusRepository;
    this.userRepository = userRepository;
    this.skillGapRepository = skillGapRepository;
    this.mapper = mapper;
  }

  @Override
  @Transactional
  public SkillGapAssessmentResponse createAssessment(CreateSkillGapAssessmentRequest request) {
    if (request == null) {
      throw new BadRequestException("Request cannot be null");
    }

    if (request.getAssessmentNumber() == null || request.getAssessmentNumber().isBlank()) {
      throw new BadRequestException("Assessment number is required");
    }
    String assessmentNumber = request.getAssessmentNumber().trim();
    if (assessmentNumber.length() > 32) {
      throw new BadRequestException("Assessment number cannot exceed 32 characters");
    }
    if (assessmentRepository.existsByAssessmentNumber(assessmentNumber)) {
      throw new ConflictException("Assessment number already exists: " + assessmentNumber, "ASSESSMENT_NUMBER_ALREADY_EXISTS");
    }

    if (request.getTraineeId() == null) {
      throw new BadRequestException("Trainee ID is required");
    }
    Trainee trainee = traineeRepository.findById(request.getTraineeId())
        .orElseThrow(() -> new ResourceNotFoundException("Trainee", "id"));
    if (trainee.getDeletedAt() != null) {
      throw new BadRequestException("Trainee is soft-deleted", "TRAINEE_SOFT_DELETED");
    }

    if (request.getSkillGapSourceId() == null) {
      throw new BadRequestException("Skill gap source ID is required");
    }
    RefSkillGapSource source = skillGapSourceRepository.findById(request.getSkillGapSourceId())
        .orElseThrow(() -> new ResourceNotFoundException("RefSkillGapSource", "id"));

    if (request.getSkillGapAssessmentStatusId() == null) {
      throw new BadRequestException("Skill gap assessment status ID is required");
    }
    RefSkillGapAssessmentStatus assessmentStatus = skillGapAssessmentStatusRepository.findById(request.getSkillGapAssessmentStatusId())
        .orElseThrow(() -> new ResourceNotFoundException("RefSkillGapAssessmentStatus", "id"));

    if (request.getAssessedOn() == null) {
      throw new BadRequestException("Assessed on date is required");
    }

    if (request.getAssessedByUserId() != null && !userRepository.existsById(request.getAssessedByUserId())) {
      throw new ResourceNotFoundException("User", "assessedByUserId");
    }

    SkillGapAssessment assessment = new SkillGapAssessment(
        assessmentNumber,
        trainee,
        source,
        assessmentStatus,
        request.getAssessedOn()
    );
    assessment.setAssessedByUserId(request.getAssessedByUserId());
    assessment.setNotes(request.getNotes() != null ? request.getNotes().trim() : null);

    validateAndSetContextRelations(
        assessment,
        trainee,
        request.getEnrollmentId(),
        request.getCourseId(),
        request.getBatchId(),
        request.getJobRoleId(),
        request.getJobPostingId(),
        request.getEmploymentRecordId(),
        request.getPlacementRecordId(),
        request.getTraineeAssessmentId(),
        request.getAssessmentResultId(),
        request.getCertificationId(),
        request.getSurveyResponseId(),
        request.getFollowupTaskId(),
        request.getEmploymentVerificationId()
    );

    SkillGapAssessment saved = assessmentRepository.save(assessment);
    return mapper.toResponse(saved);
  }

  @Override
  @Transactional
  public SkillGapAssessmentResponse updateAssessment(Long id, UpdateSkillGapAssessmentRequest request) {
    if (id == null) {
      throw new BadRequestException("Assessment ID is required");
    }
    if (request == null) {
      throw new BadRequestException("Request cannot be null");
    }

    SkillGapAssessment assessment = assessmentRepository.findById(id)
        .orElseThrow(() -> new ResourceNotFoundException("SkillGapAssessment", "id"));

    if (request.getSkillGapSourceId() != null) {
      RefSkillGapSource source = skillGapSourceRepository.findById(request.getSkillGapSourceId())
          .orElseThrow(() -> new ResourceNotFoundException("RefSkillGapSource", "id"));
      assessment.setSkillGapSource(source);
    }

    if (request.getSkillGapAssessmentStatusId() != null) {
      RefSkillGapAssessmentStatus status = skillGapAssessmentStatusRepository.findById(request.getSkillGapAssessmentStatusId())
          .orElseThrow(() -> new ResourceNotFoundException("RefSkillGapAssessmentStatus", "id"));
      assessment.setSkillGapAssessmentStatus(status);
    }

    if (request.getAssessedOn() != null) {
      assessment.setAssessedOn(request.getAssessedOn());
    }

    if (request.getAssessedByUserId() != null) {
      if (!userRepository.existsById(request.getAssessedByUserId())) {
        throw new ResourceNotFoundException("User", "assessedByUserId");
      }
      assessment.setAssessedByUserId(request.getAssessedByUserId());
    }

    if (request.getNotes() != null) {
      assessment.setNotes(request.getNotes().trim());
    }

    // Update context relations if specified
    validateAndSetContextRelations(
        assessment,
        assessment.getTrainee(),
        request.getEnrollmentId() != null ? request.getEnrollmentId() : (assessment.getEnrollment() != null ? assessment.getEnrollment().getId() : null),
        request.getCourseId() != null ? request.getCourseId() : (assessment.getCourse() != null ? assessment.getCourse().getId() : null),
        request.getBatchId() != null ? request.getBatchId() : (assessment.getBatch() != null ? assessment.getBatch().getId() : null),
        request.getJobRoleId() != null ? request.getJobRoleId() : (assessment.getJobRole() != null ? assessment.getJobRole().getId() : null),
        request.getJobPostingId() != null ? request.getJobPostingId() : (assessment.getJobPosting() != null ? assessment.getJobPosting().getId() : null),
        request.getEmploymentRecordId() != null ? request.getEmploymentRecordId() : (assessment.getEmploymentRecord() != null ? assessment.getEmploymentRecord().getId() : null),
        request.getPlacementRecordId() != null ? request.getPlacementRecordId() : (assessment.getPlacementRecord() != null ? assessment.getPlacementRecord().getId() : null),
        request.getTraineeAssessmentId() != null ? request.getTraineeAssessmentId() : (assessment.getTraineeAssessment() != null ? assessment.getTraineeAssessment().getId() : null),
        request.getAssessmentResultId() != null ? request.getAssessmentResultId() : (assessment.getAssessmentResult() != null ? assessment.getAssessmentResult().getId() : null),
        request.getCertificationId() != null ? request.getCertificationId() : (assessment.getCertification() != null ? assessment.getCertification().getId() : null),
        request.getSurveyResponseId() != null ? request.getSurveyResponseId() : (assessment.getSurveyResponse() != null ? assessment.getSurveyResponse().getId() : null),
        request.getFollowupTaskId() != null ? request.getFollowupTaskId() : (assessment.getFollowupTask() != null ? assessment.getFollowupTask().getId() : null),
        request.getEmploymentVerificationId() != null ? request.getEmploymentVerificationId() : (assessment.getEmploymentVerification() != null ? assessment.getEmploymentVerification().getId() : null)
    );

    SkillGapAssessment updated = assessmentRepository.save(assessment);
    return mapper.toResponse(updated);
  }

  private void validateAndSetContextRelations(
      SkillGapAssessment assessment,
      Trainee trainee,
      Long enrollmentId,
      Long courseId,
      Long batchId,
      Long jobRoleId,
      Long jobPostingId,
      Long employmentRecordId,
      Long placementRecordId,
      Long traineeAssessmentId,
      Long assessmentResultId,
      Long certificationId,
      Long surveyResponseId,
      Long followupTaskId,
      Long employmentVerificationId) {

    TrainingEnrollment enrollment = null;
    if (enrollmentId != null) {
      enrollment = enrollmentRepository.findById(enrollmentId)
          .orElseThrow(() -> new ResourceNotFoundException("TrainingEnrollment", "id"));
      if (!enrollment.getTrainee().getId().equals(trainee.getId())) {
        throw new BadRequestException("Enrollment does not belong to the assessed trainee", "ASSESSMENT_ENROLLMENT_TRAINEE_MISMATCH");
      }
    }
    assessment.setEnrollment(enrollment);

    Course course = null;
    if (courseId != null) {
      course = courseRepository.findById(courseId)
          .orElseThrow(() -> new ResourceNotFoundException("Course", "id"));
      if (course.getDeletedAt() != null) {
        throw new BadRequestException("Course is soft-deleted", "COURSE_SOFT_DELETED");
      }
      if (enrollment != null && enrollment.getCourse() != null && !enrollment.getCourse().getId().equals(course.getId())) {
        throw new BadRequestException("Course does not match enrollment course", "ASSESSMENT_COURSE_ENROLLMENT_MISMATCH");
      }
    }
    assessment.setCourse(course);

    TrainingBatch batch = null;
    if (batchId != null) {
      batch = batchRepository.findById(batchId)
          .orElseThrow(() -> new ResourceNotFoundException("TrainingBatch", "id"));
      if (enrollment != null && enrollment.getTrainingBatch() != null && !enrollment.getTrainingBatch().getId().equals(batch.getId())) {
        throw new BadRequestException("Batch does not match enrollment batch", "ASSESSMENT_BATCH_ENROLLMENT_MISMATCH");
      }
      if (course != null && batch.getCourse() != null && !batch.getCourse().getId().equals(course.getId())) {
        throw new BadRequestException("Batch course does not match course", "ASSESSMENT_BATCH_COURSE_MISMATCH");
      }
    }
    assessment.setBatch(batch);

    JobRole jobRole = null;
    if (jobRoleId != null) {
      jobRole = jobRoleRepository.findById(jobRoleId)
          .orElseThrow(() -> new ResourceNotFoundException("JobRole", "id"));
    }
    assessment.setJobRole(jobRole);

    JobPosting jobPosting = null;
    if (jobPostingId != null) {
      jobPosting = jobPostingRepository.findById(jobPostingId)
          .orElseThrow(() -> new ResourceNotFoundException("JobPosting", "id"));
      if (jobPosting.getDeletedAt() != null) {
        throw new BadRequestException("Job posting is soft-deleted", "JOB_POSTING_SOFT_DELETED");
      }
      if (jobRole != null && jobPosting.getJobRole() != null && !jobPosting.getJobRole().getId().equals(jobRole.getId())) {
        throw new BadRequestException("Job posting role does not match job role", "ASSESSMENT_POSTING_JOB_ROLE_MISMATCH");
      }
    }
    assessment.setJobPosting(jobPosting);

    EmploymentRecord employmentRecord = null;
    if (employmentRecordId != null) {
      employmentRecord = employmentRecordRepository.findById(employmentRecordId)
          .orElseThrow(() -> new ResourceNotFoundException("EmploymentRecord", "id"));
      if (employmentRecord.getDeletedAt() != null) {
        throw new BadRequestException("Employment record is soft-deleted", "EMPLOYMENT_RECORD_SOFT_DELETED");
      }
      if (!employmentRecord.getTrainee().getId().equals(trainee.getId())) {
        throw new BadRequestException("Employment record does not belong to the assessed trainee", "ASSESSMENT_EMPLOYMENT_TRAINEE_MISMATCH");
      }
    }
    assessment.setEmploymentRecord(employmentRecord);

    PlacementRecord placementRecord = null;
    if (placementRecordId != null) {
      placementRecord = placementRecordRepository.findById(placementRecordId)
          .orElseThrow(() -> new ResourceNotFoundException("PlacementRecord", "id"));
      if (placementRecord.getDeletedAt() != null) {
        throw new BadRequestException("Placement record is soft-deleted", "PLACEMENT_RECORD_SOFT_DELETED");
      }
      if (!placementRecord.getTrainee().getId().equals(trainee.getId())) {
        throw new BadRequestException("Placement record does not belong to the assessed trainee", "ASSESSMENT_PLACEMENT_TRAINEE_MISMATCH");
      }
      if (employmentRecord != null && employmentRecord.getPlacementRecord() != null
          && !employmentRecord.getPlacementRecord().getId().equals(placementRecord.getId())) {
        throw new BadRequestException("Placement record does not match employment record placement", "ASSESSMENT_PLACEMENT_EMPLOYMENT_MISMATCH");
      }
    }
    assessment.setPlacementRecord(placementRecord);

    TraineeAssessment traineeAssessment = null;
    if (traineeAssessmentId != null) {
      traineeAssessment = traineeAssessmentRepository.findById(traineeAssessmentId)
          .orElseThrow(() -> new ResourceNotFoundException("TraineeAssessment", "id"));
      if (!traineeAssessment.getTrainee().getId().equals(trainee.getId())) {
        throw new BadRequestException("Trainee assessment does not belong to the assessed trainee", "ASSESSMENT_TRAINEE_ASSESSMENT_TRAINEE_MISMATCH");
      }
    }
    assessment.setTraineeAssessment(traineeAssessment);

    AssessmentResult assessmentResult = null;
    if (assessmentResultId != null) {
      assessmentResult = assessmentResultRepository.findById(assessmentResultId)
          .orElseThrow(() -> new ResourceNotFoundException("AssessmentResult", "id"));
      if (!assessmentResult.getTrainee().getId().equals(trainee.getId())) {
        throw new BadRequestException("Assessment result does not belong to the assessed trainee", "ASSESSMENT_RESULT_TRAINEE_MISMATCH");
      }
    }
    assessment.setAssessmentResult(assessmentResult);

    Certification certification = null;
    if (certificationId != null) {
      certification = certificationRepository.findById(certificationId)
          .orElseThrow(() -> new ResourceNotFoundException("Certification", "id"));
      if (!certification.getTrainee().getId().equals(trainee.getId())) {
        throw new BadRequestException("Certification does not belong to the assessed trainee", "ASSESSMENT_CERTIFICATION_TRAINEE_MISMATCH");
      }
    }
    assessment.setCertification(certification);

    SurveyResponse surveyResponse = null;
    if (surveyResponseId != null) {
      surveyResponse = surveyResponseRepository.findById(surveyResponseId)
          .orElseThrow(() -> new ResourceNotFoundException("SurveyResponse", "id"));
      if (!surveyResponse.getTrainee().getId().equals(trainee.getId())) {
        throw new BadRequestException("Survey response does not belong to the assessed trainee", "ASSESSMENT_SURVEY_TRAINEE_MISMATCH");
      }
    }
    assessment.setSurveyResponse(surveyResponse);

    FollowupTask followupTask = null;
    if (followupTaskId != null) {
      followupTask = followupTaskRepository.findById(followupTaskId)
          .orElseThrow(() -> new ResourceNotFoundException("FollowupTask", "id"));
      if (!followupTask.getTrainee().getId().equals(trainee.getId())) {
        throw new BadRequestException("Followup task does not belong to the assessed trainee", "ASSESSMENT_FOLLOWUP_TRAINEE_MISMATCH");
      }
    }
    assessment.setFollowupTask(followupTask);

    EmploymentVerification employmentVerification = null;
    if (employmentVerificationId != null) {
      employmentVerification = employmentVerificationRepository.findById(employmentVerificationId)
          .orElseThrow(() -> new ResourceNotFoundException("EmploymentVerification", "id"));
      if (!employmentVerification.getTrainee().getId().equals(trainee.getId())) {
        throw new BadRequestException("Employment verification does not belong to the assessed trainee", "ASSESSMENT_VERIFICATION_TRAINEE_MISMATCH");
      }
    }
    assessment.setEmploymentVerification(employmentVerification);
  }

  @Override
  public SkillGapAssessmentResponse getAssessmentById(Long id) {
    if (id == null) {
      throw new BadRequestException("Assessment ID is required");
    }
    SkillGapAssessment entity = assessmentRepository.findById(id)
        .orElseThrow(() -> new ResourceNotFoundException("SkillGapAssessment", "id"));
    return mapper.toResponse(entity);
  }

  @Override
  public SkillGapAssessmentResponse getAssessmentByNumber(String assessmentNumber) {
    if (assessmentNumber == null || assessmentNumber.isBlank()) {
      throw new BadRequestException("Assessment number is required");
    }
    SkillGapAssessment entity = assessmentRepository.findByAssessmentNumber(assessmentNumber.trim())
        .orElseThrow(() -> new ResourceNotFoundException("SkillGapAssessment", "assessmentNumber"));
    return mapper.toResponse(entity);
  }

  @Override
  public List<SkillGapAssessmentResponse> getAssessmentsByTrainee(Long traineeId) {
    if (traineeId == null) {
      throw new BadRequestException("Trainee ID is required");
    }
    return assessmentRepository.findByTraineeId(traineeId).stream()
        .map(mapper::toResponse)
        .toList();
  }

  @Override
  public List<SkillGapAssessmentResponse> getAssessmentsByEnrollment(Long enrollmentId) {
    if (enrollmentId == null) {
      throw new BadRequestException("Enrollment ID is required");
    }
    return assessmentRepository.findByEnrollmentId(enrollmentId).stream()
        .map(mapper::toResponse)
        .toList();
  }

  @Override
  public List<SkillGapAssessmentResponse> getAssessmentsByCourse(Long courseId) {
    if (courseId == null) {
      throw new BadRequestException("Course ID is required");
    }
    return assessmentRepository.findByCourseId(courseId).stream()
        .map(mapper::toResponse)
        .toList();
  }

  @Override
  public List<SkillGapAssessmentResponse> getAssessmentsByBatch(Long batchId) {
    if (batchId == null) {
      throw new BadRequestException("Batch ID is required");
    }
    return assessmentRepository.findByBatchId(batchId).stream()
        .map(mapper::toResponse)
        .toList();
  }

  @Override
  public List<SkillGapAssessmentResponse> getAssessmentsByJobRole(Long jobRoleId) {
    if (jobRoleId == null) {
      throw new BadRequestException("Job role ID is required");
    }
    return assessmentRepository.findByJobRoleId(jobRoleId).stream()
        .map(mapper::toResponse)
        .toList();
  }

  @Override
  public List<SkillGapAssessmentResponse> getAssessmentsByJobPosting(Long jobPostingId) {
    if (jobPostingId == null) {
      throw new BadRequestException("Job posting ID is required");
    }
    return assessmentRepository.findByJobPostingId(jobPostingId).stream()
        .map(mapper::toResponse)
        .toList();
  }

  @Override
  public List<SkillGapAssessmentResponse> getAssessmentsByEmploymentRecord(Long employmentId) {
    if (employmentId == null) {
      throw new BadRequestException("Employment record ID is required");
    }
    return assessmentRepository.findByEmploymentRecordId(employmentId).stream()
        .map(mapper::toResponse)
        .toList();
  }

  @Override
  public List<SkillGapAssessmentResponse> getAssessmentsByPlacementRecord(Long placementId) {
    if (placementId == null) {
      throw new BadRequestException("Placement record ID is required");
    }
    return assessmentRepository.findByPlacementRecordId(placementId).stream()
        .map(mapper::toResponse)
        .toList();
  }

  @Override
  public List<SkillGapAssessmentResponse> getAssessmentsByTraineeAssessment(Long traineeAssessmentId) {
    if (traineeAssessmentId == null) {
      throw new BadRequestException("Trainee assessment ID is required");
    }
    return assessmentRepository.findByTraineeAssessmentId(traineeAssessmentId).stream()
        .map(mapper::toResponse)
        .toList();
  }

  @Override
  public List<SkillGapAssessmentResponse> getAssessmentsByAssessmentResult(Long assessmentResultId) {
    if (assessmentResultId == null) {
      throw new BadRequestException("Assessment result ID is required");
    }
    return assessmentRepository.findByAssessmentResultId(assessmentResultId).stream()
        .map(mapper::toResponse)
        .toList();
  }

  @Override
  public List<SkillGapAssessmentResponse> getAssessmentsByCertification(Long certificationId) {
    if (certificationId == null) {
      throw new BadRequestException("Certification ID is required");
    }
    return assessmentRepository.findByCertificationId(certificationId).stream()
        .map(mapper::toResponse)
        .toList();
  }

  @Override
  public List<SkillGapAssessmentResponse> getAssessmentsBySurveyResponse(Long surveyResponseId) {
    if (surveyResponseId == null) {
      throw new BadRequestException("Survey response ID is required");
    }
    return assessmentRepository.findBySurveyResponseId(surveyResponseId).stream()
        .map(mapper::toResponse)
        .toList();
  }

  @Override
  public List<SkillGapAssessmentResponse> getAssessmentsByFollowupTask(Long followupTaskId) {
    if (followupTaskId == null) {
      throw new BadRequestException("Followup task ID is required");
    }
    return assessmentRepository.findByFollowupTaskId(followupTaskId).stream()
        .map(mapper::toResponse)
        .toList();
  }

  @Override
  public List<SkillGapAssessmentResponse> getAssessmentsByEmploymentVerification(Long employmentVerificationId) {
    if (employmentVerificationId == null) {
      throw new BadRequestException("Employment verification ID is required");
    }
    return assessmentRepository.findByEmploymentVerificationId(employmentVerificationId).stream()
        .map(mapper::toResponse)
        .toList();
  }

  @Override
  public List<SkillGapAssessmentResponse> getAssessmentsBySource(Long sourceId) {
    if (sourceId == null) {
      throw new BadRequestException("Source ID is required");
    }
    return assessmentRepository.findBySkillGapSourceId(sourceId).stream()
        .map(mapper::toResponse)
        .toList();
  }

  @Override
  public List<SkillGapAssessmentResponse> getAssessmentsByStatus(Long statusId) {
    if (statusId == null) {
      throw new BadRequestException("Status ID is required");
    }
    return assessmentRepository.findBySkillGapAssessmentStatusId(statusId).stream()
        .map(mapper::toResponse)
        .toList();
  }

  @Override
  public List<SkillGapAssessmentResponse> getAssessmentsByDate(LocalDate assessedOn) {
    if (assessedOn == null) {
      throw new BadRequestException("Assessed on date is required");
    }
    return assessmentRepository.findByAssessedOn(assessedOn).stream()
        .map(mapper::toResponse)
        .toList();
  }

  @Override
  public List<SkillGapAssessmentResponse> getAssessmentsByTraineeAndDate(Long traineeId, LocalDate assessedOn) {
    if (traineeId == null) {
      throw new BadRequestException("Trainee ID is required");
    }
    if (assessedOn == null) {
      throw new BadRequestException("Assessed on date is required");
    }
    return assessmentRepository.findByTraineeIdAndAssessedOn(traineeId, assessedOn).stream()
        .map(mapper::toResponse)
        .toList();
  }

  @Override
  @Transactional
  public void deleteAssessment(Long id) {
    if (id == null) {
      throw new BadRequestException("Assessment ID is required");
    }
    SkillGapAssessment assessment = assessmentRepository.findById(id)
        .orElseThrow(() -> new ResourceNotFoundException("SkillGapAssessment", "id"));

    List<SkillGap> existingGaps = skillGapRepository.findBySkillGapAssessmentId(id);
    if (!existingGaps.isEmpty()) {
      throw new ConflictException("Cannot delete assessment with existing skill gaps", "ASSESSMENT_HAS_SKILL_GAPS");
    }

    assessmentRepository.delete(assessment);
  }
}
