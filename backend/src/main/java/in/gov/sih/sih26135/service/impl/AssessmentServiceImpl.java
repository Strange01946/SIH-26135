package in.gov.sih.sih26135.service.impl;

import in.gov.sih.sih26135.dto.request.CreateAssessmentRequest;
import in.gov.sih.sih26135.dto.request.UpdateAssessmentRequest;
import in.gov.sih.sih26135.dto.response.AssessmentResponse;
import in.gov.sih.sih26135.entity.Assessment;
import in.gov.sih.sih26135.entity.Course;
import in.gov.sih.sih26135.entity.Program;
import in.gov.sih.sih26135.entity.RefAssessmentType;
import in.gov.sih.sih26135.entity.TrainingBatch;
import in.gov.sih.sih26135.exception.BadRequestException;
import in.gov.sih.sih26135.exception.ConflictException;
import in.gov.sih.sih26135.exception.ResourceNotFoundException;
import in.gov.sih.sih26135.mapper.AssessmentMapper;
import in.gov.sih.sih26135.repository.AssessmentRepository;
import in.gov.sih.sih26135.repository.CourseRepository;
import in.gov.sih.sih26135.repository.ProgramRepository;
import in.gov.sih.sih26135.repository.RefAssessmentTypeRepository;
import in.gov.sih.sih26135.repository.TrainingBatchRepository;
import in.gov.sih.sih26135.repository.UserRepository;
import in.gov.sih.sih26135.service.AssessmentService;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class AssessmentServiceImpl implements AssessmentService {

  private final AssessmentRepository assessmentRepository;
  private final RefAssessmentTypeRepository refAssessmentTypeRepository;
  private final CourseRepository courseRepository;
  private final TrainingBatchRepository trainingBatchRepository;
  private final ProgramRepository programRepository;
  private final UserRepository userRepository;
  private final AssessmentMapper assessmentMapper;

  public AssessmentServiceImpl(
      AssessmentRepository assessmentRepository,
      RefAssessmentTypeRepository refAssessmentTypeRepository,
      CourseRepository courseRepository,
      TrainingBatchRepository trainingBatchRepository,
      ProgramRepository programRepository,
      UserRepository userRepository,
      AssessmentMapper assessmentMapper) {
    this.assessmentRepository = assessmentRepository;
    this.refAssessmentTypeRepository = refAssessmentTypeRepository;
    this.courseRepository = courseRepository;
    this.trainingBatchRepository = trainingBatchRepository;
    this.programRepository = programRepository;
    this.userRepository = userRepository;
    this.assessmentMapper = assessmentMapper;
  }

  @Override
  public AssessmentResponse getById(Long id) {
    if (id == null) {
      throw new BadRequestException("Assessment ID is required");
    }
    Assessment assessment = assessmentRepository.findById(id)
        .orElseThrow(() -> new ResourceNotFoundException("Assessment", "id"));
    return assessmentMapper.toResponse(assessment);
  }

  @Override
  public AssessmentResponse getByCode(String assessmentCode) {
    if (assessmentCode == null || assessmentCode.isBlank()) {
      throw new BadRequestException("Assessment code is required");
    }
    Assessment assessment = assessmentRepository.findByAssessmentCode(assessmentCode.trim())
        .orElseThrow(() -> new ResourceNotFoundException("Assessment", "assessmentCode"));
    return assessmentMapper.toResponse(assessment);
  }

  @Override
  public List<AssessmentResponse> getAllAssessments() {
    return assessmentRepository.findAll().stream()
        .map(assessmentMapper::toResponse)
        .toList();
  }

  @Override
  public List<AssessmentResponse> getByCourseId(Long courseId) {
    if (courseId == null) {
      throw new BadRequestException("Course ID is required");
    }
    return assessmentRepository.findByCourseId(courseId).stream()
        .map(assessmentMapper::toResponse)
        .toList();
  }

  @Override
  public List<AssessmentResponse> getByBatchId(Long batchId) {
    if (batchId == null) {
      throw new BadRequestException("Batch ID is required");
    }
    return assessmentRepository.findByTrainingBatchId(batchId).stream()
        .map(assessmentMapper::toResponse)
        .toList();
  }

  @Override
  public List<AssessmentResponse> getByProgramId(Long programId) {
    if (programId == null) {
      throw new BadRequestException("Program ID is required");
    }
    return assessmentRepository.findByProgramId(programId).stream()
        .map(assessmentMapper::toResponse)
        .toList();
  }

  @Override
  public List<AssessmentResponse> getByAssessmentTypeId(Long assessmentTypeId) {
    if (assessmentTypeId == null) {
      throw new BadRequestException("Assessment type ID is required");
    }
    return assessmentRepository.findByAssessmentTypeId(assessmentTypeId).stream()
        .map(assessmentMapper::toResponse)
        .toList();
  }

  @Override
  public List<AssessmentResponse> getByAssessmentDate(LocalDate assessmentDate) {
    if (assessmentDate == null) {
      throw new BadRequestException("Assessment date is required");
    }
    return assessmentRepository.findByAssessmentDate(assessmentDate).stream()
        .map(assessmentMapper::toResponse)
        .toList();
  }

  @Override
  public List<AssessmentResponse> getByLifecycleStatusId(Long lifecycleStatusId) {
    if (lifecycleStatusId == null) {
      throw new BadRequestException("Lifecycle status ID is required");
    }
    return assessmentRepository.findByLifecycleStatusId(lifecycleStatusId).stream()
        .map(assessmentMapper::toResponse)
        .toList();
  }

  @Override
  @Transactional
  public AssessmentResponse createAssessment(CreateAssessmentRequest request) {
    if (request == null) {
      throw new BadRequestException("Assessment creation request cannot be null");
    }
    if (request.getAssessmentCode() == null || request.getAssessmentCode().isBlank()) {
      throw new BadRequestException("Assessment code is required");
    }
    if (request.getAssessmentName() == null || request.getAssessmentName().isBlank()) {
      throw new BadRequestException("Assessment name is required");
    }
    if (request.getAssessmentTypeId() == null) {
      throw new BadRequestException("Assessment type ID is required");
    }
    if (request.getBatchId() == null) {
      throw new BadRequestException("Batch ID is required");
    }
    if (request.getAssessmentDate() == null) {
      throw new BadRequestException("Assessment date is required");
    }
    if (request.getMaximumScore() == null) {
      throw new BadRequestException("Maximum score is required");
    }
    if (request.getMaximumScore().compareTo(BigDecimal.ZERO) <= 0) {
      throw new BadRequestException("Maximum score must be greater than 0", "INVALID_MAXIMUM_SCORE");
    }
    if (request.getPassScore() != null) {
      if (request.getPassScore().compareTo(BigDecimal.ZERO) < 0) {
        throw new BadRequestException("Pass score cannot be negative", "INVALID_PASS_SCORE");
      }
      if (request.getPassScore().compareTo(request.getMaximumScore()) > 0) {
        throw new BadRequestException("Pass score cannot exceed maximum score", "INVALID_PASS_SCORE");
      }
    }

    String code = request.getAssessmentCode().trim();
    if (assessmentRepository.existsByAssessmentCode(code)) {
      throw new ConflictException("Assessment code already exists", "ASSESSMENT_CODE_ALREADY_EXISTS");
    }

    RefAssessmentType assessmentType = refAssessmentTypeRepository.findById(request.getAssessmentTypeId())
        .orElseThrow(() -> new ResourceNotFoundException("RefAssessmentType", "assessmentTypeId"));

    TrainingBatch batch = trainingBatchRepository.findById(request.getBatchId())
        .orElseThrow(() -> new ResourceNotFoundException("TrainingBatch", "batchId"));

    Course course = batch.getCourse();
    if (course == null) {
      throw new BadRequestException("Training batch does not have an associated course", "BATCH_COURSE_REQUIRED");
    }
    if (request.getCourseId() != null) {
      Course requestedCourse = courseRepository.findById(request.getCourseId())
          .orElseThrow(() -> new ResourceNotFoundException("Course", "courseId"));
      if (!course.getId().equals(requestedCourse.getId())) {
        throw new BadRequestException("Course ID does not match batch course", "COURSE_BATCH_MISMATCH");
      }
    }

    Program program = batch.getProgram();
    if (program == null) {
      throw new BadRequestException("Training batch does not have an associated program", "BATCH_PROGRAM_REQUIRED");
    }
    if (request.getProgramId() != null) {
      Program requestedProgram = programRepository.findById(request.getProgramId())
          .orElseThrow(() -> new ResourceNotFoundException("Program", "programId"));
      if (!program.getId().equals(requestedProgram.getId())) {
        throw new BadRequestException("Program ID does not match batch program", "PROGRAM_BATCH_MISMATCH");
      }
    }

    if (request.getEvaluatorUserId() != null) {
      if (!userRepository.existsById(request.getEvaluatorUserId())) {
        throw new ResourceNotFoundException("User", "evaluatorUserId");
      }
    }

    Assessment assessment = assessmentMapper.toEntity(request, assessmentType, course, batch, program);
    assessment.setAssessmentCode(code);
    assessment.setAssessmentName(request.getAssessmentName().trim());
    if (request.getEvaluatorName() != null) {
      assessment.setEvaluatorName(request.getEvaluatorName().trim());
    }

    LocalDateTime now = LocalDateTime.now();
    assessment.setCreatedAt(now);
    assessment.setUpdatedAt(now);

    Assessment saved = assessmentRepository.save(assessment);
    return assessmentMapper.toResponse(saved);
  }

  @Override
  @Transactional
  public AssessmentResponse updateAssessment(Long id, UpdateAssessmentRequest request) {
    if (id == null) {
      throw new BadRequestException("Assessment ID is required");
    }
    if (request == null) {
      throw new BadRequestException("Assessment update request cannot be null");
    }

    Assessment assessment = assessmentRepository.findById(id)
        .orElseThrow(() -> new ResourceNotFoundException("Assessment", "id"));

    if (request.getAssessmentTypeId() != null) {
      RefAssessmentType type = refAssessmentTypeRepository.findById(request.getAssessmentTypeId())
          .orElseThrow(() -> new ResourceNotFoundException("RefAssessmentType", "assessmentTypeId"));
      assessment.setAssessmentType(type);
    }

    if (request.getAssessmentName() != null && !request.getAssessmentName().isBlank()) {
      assessment.setAssessmentName(request.getAssessmentName().trim());
    }

    if (request.getAssessmentDate() != null) {
      assessment.setAssessmentDate(request.getAssessmentDate());
    }

    BigDecimal effectiveMax = request.getMaximumScore() != null ? request.getMaximumScore() : assessment.getMaximumScore();
    BigDecimal effectivePass = request.getPassScore() != null ? request.getPassScore() : assessment.getPassScore();

    if (request.getMaximumScore() != null) {
      if (request.getMaximumScore().compareTo(BigDecimal.ZERO) <= 0) {
        throw new BadRequestException("Maximum score must be greater than 0", "INVALID_MAXIMUM_SCORE");
      }
      assessment.setMaximumScore(request.getMaximumScore());
    }

    if (request.getPassScore() != null) {
      if (request.getPassScore().compareTo(BigDecimal.ZERO) < 0) {
        throw new BadRequestException("Pass score cannot be negative", "INVALID_PASS_SCORE");
      }
      assessment.setPassScore(request.getPassScore());
    }

    if (effectivePass != null && effectiveMax != null && effectivePass.compareTo(effectiveMax) > 0) {
      throw new BadRequestException("Pass score cannot exceed maximum score", "INVALID_PASS_SCORE");
    }

    if (request.getEvaluatorUserId() != null) {
      if (!userRepository.existsById(request.getEvaluatorUserId())) {
        throw new ResourceNotFoundException("User", "evaluatorUserId");
      }
      assessment.setEvaluatorUserId(request.getEvaluatorUserId());
    }

    if (request.getEvaluatorName() != null) {
      assessment.setEvaluatorName(request.getEvaluatorName().trim());
    }

    if (request.getLifecycleStatusId() != null) {
      assessment.setLifecycleStatusId(request.getLifecycleStatusId());
    }

    LocalDateTime now = LocalDateTime.now();
    assessment.setUpdatedAt(now);

    Assessment saved = assessmentRepository.save(assessment);
    return assessmentMapper.toResponse(saved);
  }

  @Override
  @Transactional
  public void deleteAssessment(Long id) {
    if (id == null) {
      throw new BadRequestException("Assessment ID is required");
    }
    Assessment assessment = assessmentRepository.findById(id)
        .orElseThrow(() -> new ResourceNotFoundException("Assessment", "id"));

    LocalDateTime now = LocalDateTime.now();
    assessment.setDeletedAt(now);
    assessment.setUpdatedAt(now);
    assessmentRepository.save(assessment);
  }
}
