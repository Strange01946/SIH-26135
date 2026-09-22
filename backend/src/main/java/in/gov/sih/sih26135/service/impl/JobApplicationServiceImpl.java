package in.gov.sih.sih26135.service.impl;

import in.gov.sih.sih26135.dto.request.CreateJobApplicationRequest;
import in.gov.sih.sih26135.dto.request.UpdateJobApplicationRequest;
import in.gov.sih.sih26135.dto.response.JobApplicationResponse;
import in.gov.sih.sih26135.entity.JobApplication;
import in.gov.sih.sih26135.entity.JobPosting;
import in.gov.sih.sih26135.entity.RefApplicationStatus;
import in.gov.sih.sih26135.entity.RefNonSelectionReason;
import in.gov.sih.sih26135.entity.Trainee;
import in.gov.sih.sih26135.entity.TrainingEnrollment;
import in.gov.sih.sih26135.exception.BadRequestException;
import in.gov.sih.sih26135.exception.ConflictException;
import in.gov.sih.sih26135.exception.ResourceNotFoundException;
import in.gov.sih.sih26135.mapper.JobApplicationMapper;
import in.gov.sih.sih26135.repository.JobApplicationRepository;
import in.gov.sih.sih26135.repository.JobPostingRepository;
import in.gov.sih.sih26135.repository.PlacementRecordRepository;
import in.gov.sih.sih26135.repository.RefApplicationStatusRepository;
import in.gov.sih.sih26135.repository.RefNonSelectionReasonRepository;
import in.gov.sih.sih26135.repository.TraineeRepository;
import in.gov.sih.sih26135.repository.TrainingEnrollmentRepository;
import in.gov.sih.sih26135.service.JobApplicationService;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class JobApplicationServiceImpl implements JobApplicationService {

  private final JobApplicationRepository jobApplicationRepository;
  private final PlacementRecordRepository placementRecordRepository;
  private final TraineeRepository traineeRepository;
  private final TrainingEnrollmentRepository trainingEnrollmentRepository;
  private final JobPostingRepository jobPostingRepository;
  private final RefApplicationStatusRepository refApplicationStatusRepository;
  private final RefNonSelectionReasonRepository refNonSelectionReasonRepository;
  private final JobApplicationMapper jobApplicationMapper;

  public JobApplicationServiceImpl(
      JobApplicationRepository jobApplicationRepository,
      PlacementRecordRepository placementRecordRepository,
      TraineeRepository traineeRepository,
      TrainingEnrollmentRepository trainingEnrollmentRepository,
      JobPostingRepository jobPostingRepository,
      RefApplicationStatusRepository refApplicationStatusRepository,
      RefNonSelectionReasonRepository refNonSelectionReasonRepository,
      JobApplicationMapper jobApplicationMapper) {
    this.jobApplicationRepository = jobApplicationRepository;
    this.placementRecordRepository = placementRecordRepository;
    this.traineeRepository = traineeRepository;
    this.trainingEnrollmentRepository = trainingEnrollmentRepository;
    this.jobPostingRepository = jobPostingRepository;
    this.refApplicationStatusRepository = refApplicationStatusRepository;
    this.refNonSelectionReasonRepository = refNonSelectionReasonRepository;
    this.jobApplicationMapper = jobApplicationMapper;
  }

  @Override
  public JobApplicationResponse getJobApplicationById(Long id) {
    if (id == null) {
      throw new BadRequestException("Job application ID is required");
    }
    JobApplication application = jobApplicationRepository.findById(id)
        .orElseThrow(() -> new ResourceNotFoundException("JobApplication", "id"));
    return jobApplicationMapper.toResponse(application);
  }

  @Override
  public JobApplicationResponse getJobApplicationByTraineeAndPosting(Long traineeId, Long jobPostingId) {
    if (traineeId == null) {
      throw new BadRequestException("Trainee ID is required");
    }
    if (jobPostingId == null) {
      throw new BadRequestException("Job posting ID is required");
    }
    JobApplication application = jobApplicationRepository.findByTraineeIdAndJobPostingId(traineeId, jobPostingId)
        .orElseThrow(() -> new ResourceNotFoundException("JobApplication", "traineeId and jobPostingId"));
    return jobApplicationMapper.toResponse(application);
  }

  @Override
  public List<JobApplicationResponse> getJobApplicationsByTrainee(Long traineeId) {
    if (traineeId == null) {
      throw new BadRequestException("Trainee ID is required");
    }
    return jobApplicationRepository.findByTraineeId(traineeId).stream()
        .map(jobApplicationMapper::toResponse)
        .toList();
  }

  @Override
  public List<JobApplicationResponse> getJobApplicationsByPosting(Long jobPostingId) {
    if (jobPostingId == null) {
      throw new BadRequestException("Job posting ID is required");
    }
    return jobApplicationRepository.findByJobPostingId(jobPostingId).stream()
        .map(jobApplicationMapper::toResponse)
        .toList();
  }

  @Override
  public List<JobApplicationResponse> getJobApplicationsByStatus(Long applicationStatusId) {
    if (applicationStatusId == null) {
      throw new BadRequestException("Application status ID is required");
    }
    return jobApplicationRepository.findByApplicationStatusId(applicationStatusId).stream()
        .map(jobApplicationMapper::toResponse)
        .toList();
  }

  @Override
  @Transactional
  public JobApplicationResponse createJobApplication(CreateJobApplicationRequest request) {
    if (request == null) {
      throw new BadRequestException("Job application creation request cannot be null");
    }
    if (request.getTraineeId() == null) {
      throw new BadRequestException("Trainee ID is required");
    }
    if (request.getJobPostingId() == null) {
      throw new BadRequestException("Job posting ID is required");
    }
    if (request.getApplicationStatusId() == null) {
      throw new BadRequestException("Application status ID is required");
    }
    if (request.getAppliedDate() == null) {
      throw new BadRequestException("Applied date is required");
    }

    if (jobApplicationRepository.existsByTraineeIdAndJobPostingId(request.getTraineeId(), request.getJobPostingId())) {
      throw new ConflictException("Trainee has already applied for this job posting", "DUPLICATE_JOB_APPLICATION");
    }

    Trainee trainee = traineeRepository.findById(request.getTraineeId())
        .orElseThrow(() -> new ResourceNotFoundException("Trainee", "traineeId"));

    JobPosting jobPosting = jobPostingRepository.findById(request.getJobPostingId())
        .orElseThrow(() -> new ResourceNotFoundException("JobPosting", "jobPostingId"));

    if (jobPosting.getDeletedAt() != null) {
      throw new BadRequestException("Cannot apply to a deleted job posting", "JOB_POSTING_DELETED");
    }

    TrainingEnrollment enrollment = null;
    if (request.getEnrollmentId() != null) {
      enrollment = trainingEnrollmentRepository.findById(request.getEnrollmentId())
          .orElseThrow(() -> new ResourceNotFoundException("TrainingEnrollment", "enrollmentId"));

      if (enrollment.getDeletedAt() != null) {
        throw new BadRequestException("Cannot associate deleted enrollment with job application", "INACTIVE_ENROLLMENT");
      }
      if (!enrollment.getTrainee().getId().equals(trainee.getId())) {
        throw new BadRequestException("Enrollment does not belong to the applying trainee", "TRAINEE_ENROLLMENT_MISMATCH");
      }
    }

    RefApplicationStatus applicationStatus = refApplicationStatusRepository.findById(request.getApplicationStatusId())
        .orElseThrow(() -> new ResourceNotFoundException("RefApplicationStatus", "applicationStatusId"));

    RefNonSelectionReason nonSelectionReason = null;
    if (request.getNonSelectionReasonId() != null) {
      nonSelectionReason = refNonSelectionReasonRepository.findById(request.getNonSelectionReasonId())
          .orElseThrow(() -> new ResourceNotFoundException("RefNonSelectionReason", "nonSelectionReasonId"));
    }

    JobApplication entity = jobApplicationMapper.toEntity(
        request, trainee, enrollment, jobPosting, applicationStatus, nonSelectionReason);
    JobApplication saved = jobApplicationRepository.save(entity);
    return jobApplicationMapper.toResponse(saved);
  }

  @Override
  @Transactional
  public JobApplicationResponse updateJobApplication(Long id, UpdateJobApplicationRequest request) {
    if (id == null) {
      throw new BadRequestException("Job application ID is required");
    }
    if (request == null) {
      throw new BadRequestException("Job application update request cannot be null");
    }

    JobApplication application = jobApplicationRepository.findById(id)
        .orElseThrow(() -> new ResourceNotFoundException("JobApplication", "id"));

    if (request.getApplicationStatusId() != null) {
      RefApplicationStatus status = refApplicationStatusRepository.findById(request.getApplicationStatusId())
          .orElseThrow(() -> new ResourceNotFoundException("RefApplicationStatus", "applicationStatusId"));
      application.setApplicationStatus(status);
    }

    if (request.getNonSelectionReasonId() != null) {
      RefNonSelectionReason reason = refNonSelectionReasonRepository.findById(request.getNonSelectionReasonId())
          .orElseThrow(() -> new ResourceNotFoundException("RefNonSelectionReason", "nonSelectionReasonId"));
      application.setNonSelectionReason(reason);
    }

    if (request.getRemarks() != null) {
      application.setRemarks(request.getRemarks());
    }

    JobApplication saved = jobApplicationRepository.save(application);
    return jobApplicationMapper.toResponse(saved);
  }

  @Override
  @Transactional
  public void deleteJobApplication(Long id) {
    if (id == null) {
      throw new BadRequestException("Job application ID is required");
    }
    JobApplication application = jobApplicationRepository.findById(id)
        .orElseThrow(() -> new ResourceNotFoundException("JobApplication", "id"));

    if (placementRecordRepository.findByJobApplicationId(id).isPresent()) {
      throw new ConflictException("Cannot delete job application referenced by a placement record", "JOB_APPLICATION_HAS_PLACEMENT");
    }

    jobApplicationRepository.delete(application);
  }
}
