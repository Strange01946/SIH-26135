package in.gov.sih.sih26135.service.impl;

import in.gov.sih.sih26135.dto.request.CreateCertificationRequest;
import in.gov.sih.sih26135.dto.request.UpdateCertificationRequest;
import in.gov.sih.sih26135.dto.response.CertificationResponse;
import in.gov.sih.sih26135.entity.Assessment;
import in.gov.sih.sih26135.entity.AssessmentResult;
import in.gov.sih.sih26135.entity.Certification;
import in.gov.sih.sih26135.entity.Course;
import in.gov.sih.sih26135.entity.Organization;
import in.gov.sih.sih26135.entity.Program;
import in.gov.sih.sih26135.entity.RefCertificateStatus;
import in.gov.sih.sih26135.entity.RefCertificateVerificationStatus;
import in.gov.sih.sih26135.entity.RefEnrollmentStatus;
import in.gov.sih.sih26135.entity.Trainee;
import in.gov.sih.sih26135.entity.TraineeAssessment;
import in.gov.sih.sih26135.entity.TrainingEnrollment;
import in.gov.sih.sih26135.entity.TrainingProvider;
import in.gov.sih.sih26135.exception.BadRequestException;
import in.gov.sih.sih26135.exception.ConflictException;
import in.gov.sih.sih26135.exception.ResourceNotFoundException;
import in.gov.sih.sih26135.mapper.CertificationMapper;
import in.gov.sih.sih26135.repository.AssessmentResultRepository;
import in.gov.sih.sih26135.repository.CertificationRepository;
import in.gov.sih.sih26135.repository.CourseRepository;
import in.gov.sih.sih26135.repository.OrganizationRepository;
import in.gov.sih.sih26135.repository.ProgramRepository;
import in.gov.sih.sih26135.repository.RefCertificateStatusRepository;
import in.gov.sih.sih26135.repository.RefCertificateVerificationStatusRepository;
import in.gov.sih.sih26135.repository.RefEnrollmentStatusRepository;
import in.gov.sih.sih26135.repository.TraineeRepository;
import in.gov.sih.sih26135.repository.TrainingEnrollmentRepository;
import in.gov.sih.sih26135.repository.UserRepository;
import in.gov.sih.sih26135.service.CertificationService;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class CertificationServiceImpl implements CertificationService {

  private static final Logger log = LoggerFactory.getLogger(CertificationServiceImpl.class);

  private final CertificationRepository certificationRepository;
  private final TraineeRepository traineeRepository;
  private final TrainingEnrollmentRepository trainingEnrollmentRepository;
  private final CourseRepository courseRepository;
  private final ProgramRepository programRepository;
  private final AssessmentResultRepository assessmentResultRepository;
  private final RefCertificateStatusRepository refCertificateStatusRepository;
  private final RefCertificateVerificationStatusRepository refCertificateVerificationStatusRepository;
  private final UserRepository userRepository;
  private final CertificationMapper certificationMapper;
  private final OrganizationRepository organizationRepository;
  private final RefEnrollmentStatusRepository refEnrollmentStatusRepository;
  private final JdbcTemplate jdbcTemplate;

  public CertificationServiceImpl(
      CertificationRepository certificationRepository,
      TraineeRepository traineeRepository,
      TrainingEnrollmentRepository trainingEnrollmentRepository,
      CourseRepository courseRepository,
      ProgramRepository programRepository,
      AssessmentResultRepository assessmentResultRepository,
      RefCertificateStatusRepository refCertificateStatusRepository,
      RefCertificateVerificationStatusRepository refCertificateVerificationStatusRepository,
      UserRepository userRepository,
      CertificationMapper certificationMapper) {
    this(
        certificationRepository,
        traineeRepository,
        trainingEnrollmentRepository,
        courseRepository,
        programRepository,
        assessmentResultRepository,
        refCertificateStatusRepository,
        refCertificateVerificationStatusRepository,
        userRepository,
        certificationMapper,
        null,
        null,
        null);
  }

  @Autowired
  public CertificationServiceImpl(
      CertificationRepository certificationRepository,
      TraineeRepository traineeRepository,
      TrainingEnrollmentRepository trainingEnrollmentRepository,
      CourseRepository courseRepository,
      ProgramRepository programRepository,
      AssessmentResultRepository assessmentResultRepository,
      RefCertificateStatusRepository refCertificateStatusRepository,
      RefCertificateVerificationStatusRepository refCertificateVerificationStatusRepository,
      UserRepository userRepository,
      CertificationMapper certificationMapper,
      @Autowired(required = false) OrganizationRepository organizationRepository,
      @Autowired(required = false) RefEnrollmentStatusRepository refEnrollmentStatusRepository,
      @Autowired(required = false) JdbcTemplate jdbcTemplate) {
    this.certificationRepository = certificationRepository;
    this.traineeRepository = traineeRepository;
    this.trainingEnrollmentRepository = trainingEnrollmentRepository;
    this.courseRepository = courseRepository;
    this.programRepository = programRepository;
    this.assessmentResultRepository = assessmentResultRepository;
    this.refCertificateStatusRepository = refCertificateStatusRepository;
    this.refCertificateVerificationStatusRepository = refCertificateVerificationStatusRepository;
    this.userRepository = userRepository;
    this.certificationMapper = certificationMapper;
    this.organizationRepository = organizationRepository;
    this.refEnrollmentStatusRepository = refEnrollmentStatusRepository;
    this.jdbcTemplate = jdbcTemplate;
  }

  @Override
  public CertificationResponse getById(Long id) {
    if (id == null) {
      throw new BadRequestException("Certification ID is required");
    }
    Certification cert = certificationRepository.findById(id)
        .orElseThrow(() -> new ResourceNotFoundException("Certification", "id"));
    return certificationMapper.toResponse(cert);
  }

  @Override
  public CertificationResponse getByCertificateNumber(String certificateNumber) {
    if (certificateNumber == null || certificateNumber.isBlank()) {
      throw new BadRequestException("Certificate number is required");
    }
    Certification cert = certificationRepository.findByCertificateNumber(certificateNumber.trim())
        .orElseThrow(() -> new ResourceNotFoundException("Certification", "certificateNumber"));
    return certificationMapper.toResponse(cert);
  }

  @Override
  public List<CertificationResponse> getAllCertifications() {
    return certificationRepository.findAll().stream()
        .map(certificationMapper::toResponse)
        .toList();
  }

  @Override
  public List<CertificationResponse> getByTraineeId(Long traineeId) {
    if (traineeId == null) {
      throw new BadRequestException("Trainee ID is required");
    }
    return certificationRepository.findByTraineeId(traineeId).stream()
        .filter(c -> c.getDeletedAt() == null)
        .map(certificationMapper::toResponse)
        .toList();
  }

  @Override
  public List<CertificationResponse> getByEnrollmentId(Long enrollmentId) {
    if (enrollmentId == null) {
      throw new BadRequestException("Enrollment ID is required");
    }
    return certificationRepository.findByTrainingEnrollmentId(enrollmentId).stream()
        .filter(c -> c.getDeletedAt() == null)
        .map(certificationMapper::toResponse)
        .toList();
  }

  @Override
  public List<CertificationResponse> getByCourseId(Long courseId) {
    if (courseId == null) {
      throw new BadRequestException("Course ID is required");
    }
    return certificationRepository.findByCourseId(courseId).stream()
        .map(certificationMapper::toResponse)
        .toList();
  }

  @Override
  public List<CertificationResponse> getByProgramId(Long programId) {
    if (programId == null) {
      throw new BadRequestException("Program ID is required");
    }
    return certificationRepository.findByProgramId(programId).stream()
        .map(certificationMapper::toResponse)
        .toList();
  }

  @Override
  public List<CertificationResponse> getByAssessmentResultId(Long assessmentResultId) {
    if (assessmentResultId == null) {
      throw new BadRequestException("Assessment result ID is required");
    }
    return certificationRepository.findByAssessmentResultId(assessmentResultId).stream()
        .map(certificationMapper::toResponse)
        .toList();
  }

  @Override
  public List<CertificationResponse> getByCertificateStatusId(Long statusId) {
    if (statusId == null) {
      throw new BadRequestException("Certificate status ID is required");
    }
    return certificationRepository.findByCertificateStatusId(statusId).stream()
        .map(certificationMapper::toResponse)
        .toList();
  }

  @Override
  public List<CertificationResponse> getByCertificateVerificationStatusId(Long verificationStatusId) {
    if (verificationStatusId == null) {
      throw new BadRequestException("Certificate verification status ID is required");
    }
    return certificationRepository.findByCertificateVerificationStatusId(verificationStatusId).stream()
        .map(certificationMapper::toResponse)
        .toList();
  }

  @Override
  public List<CertificationResponse> getByIssueDate(LocalDate issueDate) {
    if (issueDate == null) {
      throw new BadRequestException("Issue date is required");
    }
    return certificationRepository.findByIssueDate(issueDate).stream()
        .map(certificationMapper::toResponse)
        .toList();
  }

  @Override
  @Transactional
  public CertificationResponse createCertification(CreateCertificationRequest request) {
    if (request == null) {
      throw new BadRequestException("Certification creation request cannot be null");
    }
    if (request.getCertificateNumber() == null || request.getCertificateNumber().isBlank()) {
      throw new BadRequestException("Certificate number is required");
    }
    if (request.getIssueDate() == null) {
      throw new BadRequestException("Issue date is required");
    }
    if (request.getCertificateStatusId() == null) {
      throw new BadRequestException("Certificate status ID is required");
    }
    if (request.getCertificateVerificationStatusId() == null) {
      throw new BadRequestException("Certificate verification status ID is required");
    }

    if (request.getExpiryDate() != null && request.getExpiryDate().isBefore(request.getIssueDate())) {
      throw new BadRequestException("Expiry date cannot precede issue date", "INVALID_EXPIRY_DATE");
    }

    String certNum = request.getCertificateNumber().trim();
    if (certificationRepository.existsByCertificateNumber(certNum)) {
      throw new ConflictException("Certificate number already exists", "CERTIFICATE_NUMBER_ALREADY_EXISTS");
    }

    Trainee trainee;
    TrainingEnrollment enrollment;
    Course course;
    Program program;
    AssessmentResult result = null;

    if (request.getAssessmentResultId() != null) {
      result = assessmentResultRepository.findById(request.getAssessmentResultId())
          .orElseThrow(() -> new ResourceNotFoundException("AssessmentResult", "assessmentResultId"));

      if (result.getAssessmentOutcome() == null || !Boolean.TRUE.equals(result.getAssessmentOutcome().getIsPassFlag())) {
        throw new BadRequestException("Certification requires a passing assessment outcome", "ASSESSMENT_OUTCOME_NOT_PASS");
      }

      TraineeAssessment attempt = result.getTraineeAssessment();
      if (attempt == null) {
        throw new BadRequestException("Assessment result has no linked trainee assessment", "TRAINEE_ASSESSMENT_REQUIRED");
      }

      enrollment = attempt.getTrainingEnrollment();
      if (enrollment == null) {
        throw new BadRequestException("Assessment attempt has no linked training enrollment", "ENROLLMENT_REQUIRED");
      }
      if (enrollment.getDeletedAt() != null) {
        throw new BadRequestException("Training enrollment is not active", "INACTIVE_ENROLLMENT");
      }

      trainee = attempt.getTrainee();
      if (trainee == null) {
        trainee = result.getTrainee();
      }
      if (trainee == null) {
        throw new BadRequestException("Assessment attempt has no linked trainee", "TRAINEE_REQUIRED");
      }
      if (trainee.getDeletedAt() != null) {
        throw new BadRequestException("Cannot issue certification for soft-deleted trainee", "TRAINEE_DELETED");
      }

      Assessment assessment = attempt.getAssessment();
      if (assessment == null) {
        throw new BadRequestException("Assessment attempt has no linked assessment", "ASSESSMENT_REQUIRED");
      }

      course = assessment.getCourse();
      if (course == null) {
        throw new BadRequestException("Assessment has no linked course", "COURSE_REQUIRED");
      }

      program = assessment.getProgram();
      if (program == null) {
        throw new BadRequestException("Assessment has no linked program", "PROGRAM_REQUIRED");
      }

      if (result.getTrainee() != null && !result.getTrainee().getId().equals(trainee.getId())) {
        throw new BadRequestException("Assessment result trainee does not match attempt trainee", "TRAINEE_ASSESSMENT_MISMATCH");
      }
      if (enrollment.getTrainee() != null && !enrollment.getTrainee().getId().equals(trainee.getId())) {
        throw new BadRequestException("Enrollment trainee does not match authoritative trainee", "TRAINEE_ENROLLMENT_MISMATCH");
      }
      if (enrollment.getCourse() != null && !enrollment.getCourse().getId().equals(course.getId())) {
        throw new BadRequestException("Enrollment course does not match assessment course", "COURSE_ENROLLMENT_MISMATCH");
      }
      if (enrollment.getProgram() != null && !enrollment.getProgram().getId().equals(program.getId())) {
        throw new BadRequestException("Enrollment program does not match assessment program", "PROGRAM_ENROLLMENT_MISMATCH");
      }
      if (enrollment.getTrainingBatch() != null && assessment.getTrainingBatch() != null
          && !enrollment.getTrainingBatch().getId().equals(assessment.getTrainingBatch().getId())) {
        throw new BadRequestException("Enrollment batch does not match assessment batch", "BATCH_ENROLLMENT_MISMATCH");
      }

      if (request.getTraineeId() != null && !request.getTraineeId().equals(trainee.getId())) {
        throw new BadRequestException("Trainee ID does not match authoritative trainee", "TRAINEE_MISMATCH");
      }
      if (request.getEnrollmentId() != null && !request.getEnrollmentId().equals(enrollment.getId())) {
        throw new BadRequestException("Enrollment ID does not match authoritative enrollment", "ENROLLMENT_MISMATCH");
      }
      if (request.getCourseId() != null && !request.getCourseId().equals(course.getId())) {
        throw new BadRequestException("Course ID does not match authoritative course", "COURSE_MISMATCH");
      }
      if (request.getProgramId() != null && !request.getProgramId().equals(program.getId())) {
        throw new BadRequestException("Program ID does not match authoritative program", "PROGRAM_MISMATCH");
      }

      // Training completion & attendance validation
      if (!checkAttendanceSatisfied(course, enrollment.getId())) {
        throw new BadRequestException("Cannot issue certification: Mandatory attendance requirement not satisfied", "INSUFFICIENT_ATTENDANCE");
      }
      if (enrollment.getTrainingBatch() != null && enrollment.getTrainingBatch().getBatchStatus() != null) {
        Long bStatus = enrollment.getTrainingBatch().getBatchStatus().getId();
        if (bStatus == 5L || bStatus == 6L) {
          throw new BadRequestException("Cannot issue certification for cancelled or suspended training batch", "INVALID_BATCH_STATUS");
        }
      }

      // Authoritative issuer check
      if (request.getIssuingBody() == null || request.getIssuingBody().isBlank()) {
        Optional<String> authIssuerOpt = resolveAuthoritativeIssuer(assessment);
        if (authIssuerOpt.isEmpty()) {
          throw new BadRequestException("Cannot issue certification: No accredited authoritative issuing body established", "AUTHORIZED_ISSUER_REQUIRED");
        }
        request.setIssuingBody(authIssuerOpt.get());
      }

      // Transition active enrollment to completed
      if (enrollment.getEnrollmentStatus() != null && (enrollment.getEnrollmentStatus().getId() == 1L || enrollment.getEnrollmentStatus().getId() == 2L)) {
        if (refEnrollmentStatusRepository != null) {
          RefEnrollmentStatus statusCompleted = refEnrollmentStatusRepository.findById(3L).orElse(null);
          if (statusCompleted != null) {
            enrollment.setEnrollmentStatus(statusCompleted);
            if (enrollment.getActualCompletionDate() == null) {
              enrollment.setActualCompletionDate(LocalDate.now());
            }
            enrollment.setUpdatedAt(LocalDateTime.now());
            trainingEnrollmentRepository.save(enrollment);
          }
        }
      }
    } else {
      if (request.getTraineeId() == null) {
        throw new BadRequestException("Trainee ID is required");
      }
      if (request.getEnrollmentId() == null) {
        throw new BadRequestException("Enrollment ID is required");
      }
      if (request.getCourseId() == null) {
        throw new BadRequestException("Course ID is required");
      }
      if (request.getProgramId() == null) {
        throw new BadRequestException("Program ID is required");
      }

      trainee = traineeRepository.findById(request.getTraineeId())
          .orElseThrow(() -> new ResourceNotFoundException("Trainee", "traineeId"));

      if (trainee.getDeletedAt() != null) {
        throw new BadRequestException("Cannot issue certification for soft-deleted trainee", "TRAINEE_DELETED");
      }

      enrollment = trainingEnrollmentRepository.findById(request.getEnrollmentId())
          .orElseThrow(() -> new ResourceNotFoundException("TrainingEnrollment", "enrollmentId"));

      if (enrollment.getDeletedAt() != null) {
        throw new BadRequestException("Training enrollment is not active", "INACTIVE_ENROLLMENT");
      }

      course = courseRepository.findById(request.getCourseId())
          .orElseThrow(() -> new ResourceNotFoundException("Course", "courseId"));

      program = programRepository.findById(request.getProgramId())
          .orElseThrow(() -> new ResourceNotFoundException("Program", "programId"));

      if (enrollment.getTrainee() != null && !enrollment.getTrainee().getId().equals(trainee.getId())) {
        throw new BadRequestException("Trainee ID does not match enrollment trainee", "TRAINEE_ENROLLMENT_MISMATCH");
      }
      if (enrollment.getCourse() != null && !enrollment.getCourse().getId().equals(course.getId())) {
        throw new BadRequestException("Course ID does not match enrollment course", "COURSE_ENROLLMENT_MISMATCH");
      }
      if (enrollment.getProgram() != null && !enrollment.getProgram().getId().equals(program.getId())) {
        throw new BadRequestException("Program ID does not match enrollment program", "PROGRAM_ENROLLMENT_MISMATCH");
      }
    }

    RefCertificateStatus status = refCertificateStatusRepository.findById(request.getCertificateStatusId())
        .orElseThrow(() -> new ResourceNotFoundException("RefCertificateStatus", "certificateStatusId"));

    RefCertificateVerificationStatus verificationStatus = refCertificateVerificationStatusRepository
        .findById(request.getCertificateVerificationStatusId())
        .orElseThrow(() -> new ResourceNotFoundException("RefCertificateVerificationStatus", "certificateVerificationStatusId"));

    if (("REVOKED".equalsIgnoreCase(status.getStatusCode()) || "EXPIRED".equalsIgnoreCase(status.getStatusCode()))
        && "VERIFIED".equalsIgnoreCase(verificationStatus.getStatusCode())) {
      throw new BadRequestException("Cannot verify a revoked or expired certification", "INVALID_CERTIFICATE_STATUS");
    }

    if (request.getVerifiedByUserId() != null) {
      if (!userRepository.existsById(request.getVerifiedByUserId())) {
        throw new ResourceNotFoundException("User", "verifiedByUserId");
      }
    }

    Certification certification = certificationMapper.toEntity(
        request, trainee, enrollment, course, program, result, status, verificationStatus);
    certification.setCertificateNumber(certNum);
    if (request.getIssuingBody() != null) {
      certification.setIssuingBody(request.getIssuingBody().trim());
    }

    LocalDateTime now = LocalDateTime.now();
    certification.setCreatedAt(now);
    certification.setUpdatedAt(now);

    Certification saved = certificationRepository.save(certification);
    return certificationMapper.toResponse(saved);
  }

  @Override
  @Transactional
  public CertificationResponse updateCertification(Long id, UpdateCertificationRequest request) {
    if (id == null) {
      throw new BadRequestException("Certification ID is required");
    }
    if (request == null) {
      throw new BadRequestException("Certification update request cannot be null");
    }

    Certification cert = certificationRepository.findById(id)
        .orElseThrow(() -> new ResourceNotFoundException("Certification", "id"));

    if (cert.getDeletedAt() != null) {
      throw new BadRequestException("Cannot update a deleted certification", "CERTIFICATION_DELETED");
    }

    LocalDate effectiveExpiry = request.getExpiryDate() != null ? request.getExpiryDate() : cert.getExpiryDate();
    LocalDate issueDate = cert.getIssueDate();

    if (effectiveExpiry != null && issueDate != null && effectiveExpiry.isBefore(issueDate)) {
      throw new BadRequestException("Expiry date cannot precede issue date", "INVALID_EXPIRY_DATE");
    }

    RefCertificateStatus effectiveStatus = cert.getCertificateStatus();
    if (request.getCertificateStatusId() != null) {
      effectiveStatus = refCertificateStatusRepository.findById(request.getCertificateStatusId())
          .orElseThrow(() -> new ResourceNotFoundException("RefCertificateStatus", "certificateStatusId"));
    }

    RefCertificateVerificationStatus effectiveVerificationStatus = cert.getCertificateVerificationStatus();
    if (request.getCertificateVerificationStatusId() != null) {
      effectiveVerificationStatus = refCertificateVerificationStatusRepository
          .findById(request.getCertificateVerificationStatusId())
          .orElseThrow(() -> new ResourceNotFoundException("RefCertificateVerificationStatus", "certificateVerificationStatusId"));
    }

    if (effectiveStatus != null && effectiveVerificationStatus != null
        && ("REVOKED".equalsIgnoreCase(effectiveStatus.getStatusCode()) || "EXPIRED".equalsIgnoreCase(effectiveStatus.getStatusCode()))
        && "VERIFIED".equalsIgnoreCase(effectiveVerificationStatus.getStatusCode())) {
      throw new BadRequestException("Cannot verify a revoked or expired certification", "INVALID_CERTIFICATE_STATUS");
    }

    if (request.getVerifiedByUserId() != null) {
      if (!userRepository.existsById(request.getVerifiedByUserId())) {
        throw new ResourceNotFoundException("User", "verifiedByUserId");
      }
      cert.setVerifiedByUserId(request.getVerifiedByUserId());
    }

    if (request.getExpiryDate() != null) {
      cert.setExpiryDate(request.getExpiryDate());
    }

    if (request.getIssuingBody() != null) {
      cert.setIssuingBody(request.getIssuingBody().trim());
    }

    if (request.getCertificateStatusId() != null) {
      cert.setCertificateStatus(effectiveStatus);
    }

    if (request.getCertificateVerificationStatusId() != null) {
      cert.setCertificateVerificationStatus(effectiveVerificationStatus);
    }

    if (request.getVerifiedAt() != null) {
      cert.setVerifiedAt(request.getVerifiedAt());
    }

    LocalDateTime now = LocalDateTime.now();
    cert.setUpdatedAt(now);

    Certification saved = certificationRepository.save(cert);
    return certificationMapper.toResponse(saved);
  }

  @Override
  @Transactional
  public void deleteCertification(Long id) {
    if (id == null) {
      throw new BadRequestException("Certification ID is required");
    }
    Certification cert = certificationRepository.findById(id)
        .orElseThrow(() -> new ResourceNotFoundException("Certification", "id"));

    if (cert.getDeletedAt() == null) {
      LocalDateTime now = LocalDateTime.now();
      cert.setDeletedAt(now);
      cert.setUpdatedAt(now);
      certificationRepository.save(cert);
    }
  }

  @Override
  @Transactional
  public CertificationResponse issuePendingCertificate(Long assessmentResultId) {
    if (assessmentResultId == null) {
      throw new BadRequestException("Assessment result ID is required");
    }
    AssessmentResult result = assessmentResultRepository.findById(assessmentResultId)
        .orElseThrow(() -> new ResourceNotFoundException("AssessmentResult", "assessmentResultId=" + assessmentResultId));

    if (result.getAssessmentOutcome() == null || !Boolean.TRUE.equals(result.getAssessmentOutcome().getIsPassFlag())) {
      throw new BadRequestException("Candidate did not pass assessment", "ASSESSMENT_OUTCOME_NOT_PASS");
    }

    TraineeAssessment attempt = result.getTraineeAssessment();
    if (attempt == null) {
      throw new BadRequestException("Assessment result has no linked attempt", "TRAINEE_ASSESSMENT_REQUIRED");
    }
    TrainingEnrollment enrollment = attempt.getTrainingEnrollment();
    if (enrollment == null) {
      throw new BadRequestException("Assessment attempt has no linked enrollment", "ENROLLMENT_REQUIRED");
    }
    Assessment assessment = attempt.getAssessment();
    if (assessment == null) {
      throw new BadRequestException("Assessment attempt has no linked assessment", "ASSESSMENT_REQUIRED");
    }

    // Check existing certificate
    Optional<Certification> existingCert = certificationRepository.findFirstByAssessmentResultId(assessmentResultId);
    if (existingCert.isEmpty()) {
      existingCert = certificationRepository.findFirstByTrainingEnrollmentId(enrollment.getId());
    }
    if (existingCert.isPresent()) {
      return certificationMapper.toResponse(existingCert.get());
    }

    Course course = assessment.getCourse();
    // Validate attendance & batch
    boolean batchActive = true;
    if (assessment.getTrainingBatch() != null && assessment.getTrainingBatch().getBatchStatus() != null) {
      Long bStatus = assessment.getTrainingBatch().getBatchStatus().getId();
      if (bStatus == 5L || bStatus == 6L) batchActive = false;
    }
    if (!batchActive) {
      throw new BadRequestException("Training batch is cancelled or suspended", "INVALID_BATCH_STATUS");
    }

    boolean attendanceMet = checkAttendanceSatisfied(course, enrollment.getId());
    if (!attendanceMet) {
      throw new BadRequestException("Cannot issue certification: Mandatory attendance requirement not satisfied", "INSUFFICIENT_ATTENDANCE");
    }

    // Resolve authoritative issuer
    Optional<String> authIssuerOpt = resolveAuthoritativeIssuer(assessment);
    if (authIssuerOpt.isEmpty()) {
      throw new BadRequestException("Cannot issue certification: No accredited authoritative issuing body established", "AUTHORIZED_ISSUER_REQUIRED");
    }

    // Mark enrollment completed if active
    LocalDate today = LocalDate.now();
    LocalDateTime now = LocalDateTime.now();
    RefEnrollmentStatus statusCompleted = refEnrollmentStatusRepository != null
        ? refEnrollmentStatusRepository.findById(3L).orElse(null)
        : null;
    if (enrollment.getEnrollmentStatus() != null && (enrollment.getEnrollmentStatus().getId() == 1L || enrollment.getEnrollmentStatus().getId() == 2L)) {
      if (statusCompleted != null) {
        enrollment.setEnrollmentStatus(statusCompleted);
        if (enrollment.getActualCompletionDate() == null) {
          enrollment.setActualCompletionDate(today);
        }
        enrollment.setUpdatedAt(now);
        trainingEnrollmentRepository.save(enrollment);
      }
    }

    String courseCode = course != null ? course.getCourseCode() : "CRS";
    String certNum = "CERT-" + courseCode + "-" + attempt.getTrainee().getId() + "-" + UUID.randomUUID().toString().replace("-", "").substring(0, 8).toUpperCase(Locale.ROOT);

    RefCertificateStatus statusIssued = refCertificateStatusRepository.findById(1L).orElseThrow();
    RefCertificateVerificationStatus verifStatusPending = refCertificateVerificationStatusRepository.findById(2L).orElseThrow();

    Certification cert = new Certification();
    cert.setCertificateNumber(certNum);
    cert.setTrainee(attempt.getTrainee());
    cert.setTrainingEnrollment(enrollment);
    cert.setCourse(course);
    cert.setProgram(assessment.getProgram());
    cert.setAssessmentResult(result);
    cert.setIssuingBody(authIssuerOpt.get());
    cert.setIssueDate(today);
    cert.setExpiryDate(today.plusYears(5));
    cert.setCertificateStatus(statusIssued);
    cert.setCertificateVerificationStatus(verifStatusPending);
    cert.setCreatedAt(now);
    cert.setUpdatedAt(now);

    Certification saved = certificationRepository.save(cert);
    return certificationMapper.toResponse(saved);
  }

  private boolean checkAttendanceSatisfied(Course course, Long enrollmentId) {
    if (enrollmentId == null) return false;
    Long deliveryModeId = null;
    if (course != null && course.getDeliveryMode() != null) {
      deliveryModeId = course.getDeliveryMode().getId();
    } else if (course != null && course.getId() != null && jdbcTemplate != null) {
      try {
        deliveryModeId = jdbcTemplate.queryForObject("SELECT delivery_mode_id FROM courses WHERE course_id = ?", Long.class, course.getId());
      } catch (Exception ignored) {}
    }

    if (deliveryModeId != null && deliveryModeId == 2L) {
      return true; // Online course: physical attendance not applicable
    }

    if (jdbcTemplate == null) return true;
    Long total = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM attendance_records WHERE enrollment_id = ?", Long.class, enrollmentId);
    if (total == null || total == 0) return false;
    Long attended = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM attendance_records WHERE enrollment_id = ? AND attendance_status_id IN (1, 3, 6)", Long.class, enrollmentId);
    double rate = (attended != null ? attended.doubleValue() : 0.0) / total.doubleValue();
    return rate >= 0.70;
  }

  private Optional<String> resolveAuthoritativeIssuer(Assessment assessment) {
    if (assessment == null) return Optional.empty();

    if (assessment.getTrainingBatch() != null && assessment.getTrainingBatch().getTrainingProvider() != null) {
      TrainingProvider provider = assessment.getTrainingBatch().getTrainingProvider();
      if (provider.getOrganizationId() != null && organizationRepository != null) {
        Optional<Organization> orgOpt = organizationRepository.findById(provider.getOrganizationId());
        if (orgOpt.isPresent()) {
          Organization org = orgOpt.get();
          if (org.getDeletedAt() == null && Long.valueOf(1L).equals(org.getLifecycleStatusId())) {
            Long typeId = org.getOrganizationTypeId();
            if (typeId != null && (typeId == 1L || typeId == 2L || typeId == 3L || typeId == 4L)) {
              if (org.getOrganizationName() != null && !org.getOrganizationName().isBlank()) {
                return Optional.of(org.getOrganizationName().trim());
              }
            }
          }
        }
      }
    }

    if (assessment.getProgram() != null && assessment.getProgram().getId() != null && jdbcTemplate != null) {
      try {
        List<Map<String, Object>> orgRows = jdbcTemplate.queryForList(
            "SELECT o.organization_name, o.organization_type_id, o.lifecycle_status_id " +
            "FROM programs p JOIN departments d ON p.department_id = d.department_id " +
            "JOIN organizations o ON d.organization_id = o.organization_id " +
            "WHERE p.program_id = ? AND p.deleted_at IS NULL AND d.deleted_at IS NULL AND o.deleted_at IS NULL",
            assessment.getProgram().getId()
        );
        if (!orgRows.isEmpty()) {
          Map<String, Object> row = orgRows.get(0);
          Long typeId = ((Number) row.get("organization_type_id")).longValue();
          Long lifecycleId = ((Number) row.get("lifecycle_status_id")).longValue();
          if (lifecycleId == 1L && (typeId == 1L || typeId == 2L || typeId == 3L || typeId == 4L)) {
            String orgName = (String) row.get("organization_name");
            if (orgName != null && !orgName.isBlank()) {
              return Optional.of(orgName.trim());
            }
          }
        }
      } catch (Exception ignored) {}
    }

    if (assessment.getCourse() != null && assessment.getCourse().getSector() != null && jdbcTemplate != null) {
      String sectorName = assessment.getCourse().getSector().getSectorName();
      if (sectorName != null && !sectorName.isBlank()) {
        try {
          List<String> matchingOrgs = jdbcTemplate.queryForList(
              "SELECT organization_name FROM organizations WHERE organization_type_id = 4 AND lifecycle_status_id = 1 AND deleted_at IS NULL " +
              "AND (LOWER(organization_name) LIKE LOWER(?) OR LOWER(organization_code) LIKE LOWER(?)) LIMIT 1",
              String.class, "%" + sectorName.trim() + "%", "%" + sectorName.trim() + "%"
          );
          if (!matchingOrgs.isEmpty() && matchingOrgs.get(0) != null && !matchingOrgs.get(0).isBlank()) {
            return Optional.of(matchingOrgs.get(0).trim());
          }
        } catch (Exception ignored) {}
      }
    }

    return Optional.empty();
  }
}
