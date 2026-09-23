package in.gov.sih.sih26135.service.impl;

import in.gov.sih.sih26135.dto.request.CreateFollowupTaskRequest;
import in.gov.sih.sih26135.dto.request.UpdateFollowupTaskRequest;
import in.gov.sih.sih26135.dto.response.FollowupTaskResponse;
import in.gov.sih.sih26135.entity.EmploymentRecord;
import in.gov.sih.sih26135.entity.FollowupCampaign;
import in.gov.sih.sih26135.entity.FollowupTask;
import in.gov.sih.sih26135.entity.PlacementRecord;
import in.gov.sih.sih26135.entity.RefCommunicationChannel;
import in.gov.sih.sih26135.entity.Trainee;
import in.gov.sih.sih26135.entity.TrainingEnrollment;
import in.gov.sih.sih26135.exception.BadRequestException;
import in.gov.sih.sih26135.exception.ConflictException;
import in.gov.sih.sih26135.exception.ResourceNotFoundException;
import in.gov.sih.sih26135.mapper.FollowupTaskMapper;
import in.gov.sih.sih26135.repository.CommunicationLogRepository;
import in.gov.sih.sih26135.repository.EmploymentRecordRepository;
import in.gov.sih.sih26135.repository.FollowupCampaignRepository;
import in.gov.sih.sih26135.repository.FollowupTaskRepository;
import in.gov.sih.sih26135.repository.PlacementRecordRepository;
import in.gov.sih.sih26135.repository.RefCommunicationChannelRepository;
import in.gov.sih.sih26135.repository.SurveyResponseRepository;
import in.gov.sih.sih26135.repository.TraineeRepository;
import in.gov.sih.sih26135.repository.TrainingEnrollmentRepository;
import in.gov.sih.sih26135.repository.UserRepository;
import in.gov.sih.sih26135.service.FollowupTaskService;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class FollowupTaskServiceImpl implements FollowupTaskService {

  private final FollowupTaskRepository followupTaskRepository;
  private final FollowupCampaignRepository followupCampaignRepository;
  private final TraineeRepository traineeRepository;
  private final TrainingEnrollmentRepository trainingEnrollmentRepository;
  private final PlacementRecordRepository placementRecordRepository;
  private final EmploymentRecordRepository employmentRecordRepository;
  private final UserRepository userRepository;
  private final RefCommunicationChannelRepository refCommunicationChannelRepository;
  private final SurveyResponseRepository surveyResponseRepository;
  private final CommunicationLogRepository communicationLogRepository;
  private final FollowupTaskMapper followupTaskMapper;

  public FollowupTaskServiceImpl(
      FollowupTaskRepository followupTaskRepository,
      FollowupCampaignRepository followupCampaignRepository,
      TraineeRepository traineeRepository,
      TrainingEnrollmentRepository trainingEnrollmentRepository,
      PlacementRecordRepository placementRecordRepository,
      EmploymentRecordRepository employmentRecordRepository,
      UserRepository userRepository,
      RefCommunicationChannelRepository refCommunicationChannelRepository,
      SurveyResponseRepository surveyResponseRepository,
      CommunicationLogRepository communicationLogRepository,
      FollowupTaskMapper followupTaskMapper) {
    this.followupTaskRepository = followupTaskRepository;
    this.followupCampaignRepository = followupCampaignRepository;
    this.traineeRepository = traineeRepository;
    this.trainingEnrollmentRepository = trainingEnrollmentRepository;
    this.placementRecordRepository = placementRecordRepository;
    this.employmentRecordRepository = employmentRecordRepository;
    this.userRepository = userRepository;
    this.refCommunicationChannelRepository = refCommunicationChannelRepository;
    this.surveyResponseRepository = surveyResponseRepository;
    this.communicationLogRepository = communicationLogRepository;
    this.followupTaskMapper = followupTaskMapper;
  }

  @Override
  @Transactional
  public FollowupTaskResponse createFollowupTask(CreateFollowupTaskRequest request) {
    if (request == null) {
      throw new BadRequestException("Followup task creation request cannot be null");
    }
    if (request.getFollowupCampaignId() == null) {
      throw new BadRequestException("Followup campaign ID is required");
    }
    if (request.getTraineeId() == null) {
      throw new BadRequestException("Trainee ID is required");
    }
    if (request.getScheduledDate() == null) {
      throw new BadRequestException("Scheduled date is required");
    }
    if (request.getFollowupStatusId() == null) {
      throw new BadRequestException("Followup status ID is required");
    }

    if (followupTaskRepository.existsByFollowupCampaignIdAndTraineeId(request.getFollowupCampaignId(), request.getTraineeId())) {
      throw new ConflictException("Followup task already exists for this campaign and trainee", "DUPLICATE_CAMPAIGN_TRAINEE_TASK");
    }

    if (request.getNextFollowupDate() != null && request.getNextFollowupDate().isBefore(request.getScheduledDate())) {
      throw new BadRequestException("Next followup date cannot be before scheduled date", "INVALID_DATE_RANGE");
    }

    FollowupCampaign campaign = followupCampaignRepository.findById(request.getFollowupCampaignId())
        .orElseThrow(() -> new ResourceNotFoundException("FollowupCampaign", "followupCampaignId"));

    if (campaign.getDeletedAt() != null) {
      throw new BadRequestException("Cannot create followup task for a soft-deleted campaign", "CAMPAIGN_DELETED");
    }

    Trainee trainee = traineeRepository.findById(request.getTraineeId())
        .orElseThrow(() -> new ResourceNotFoundException("Trainee", "traineeId"));

    TrainingEnrollment enrollment = null;
    if (request.getEnrollmentId() != null) {
      enrollment = trainingEnrollmentRepository.findById(request.getEnrollmentId())
          .orElseThrow(() -> new ResourceNotFoundException("TrainingEnrollment", "enrollmentId"));
      if (!enrollment.getTrainee().getId().equals(trainee.getId())) {
        throw new BadRequestException("Enrollment does not belong to the specified trainee", "TRAINEE_ENROLLMENT_MISMATCH");
      }
      if (campaign.getProgram() != null && enrollment.getProgram() != null
          && !campaign.getProgram().getId().equals(enrollment.getProgram().getId())) {
        throw new BadRequestException("Enrollment program does not match campaign program", "PROGRAM_MISMATCH");
      }
      if (campaign.getCourse() != null && enrollment.getCourse() != null
          && !campaign.getCourse().getId().equals(enrollment.getCourse().getId())) {
        throw new BadRequestException("Enrollment course does not match campaign course", "COURSE_MISMATCH");
      }
    }

    PlacementRecord placement = null;
    if (request.getPlacementId() != null) {
      placement = placementRecordRepository.findById(request.getPlacementId())
          .orElseThrow(() -> new ResourceNotFoundException("PlacementRecord", "placementId"));
      if (!placement.getTrainee().getId().equals(trainee.getId())) {
        throw new BadRequestException("Placement record does not belong to the specified trainee", "TRAINEE_PLACEMENT_MISMATCH");
      }
      if (enrollment != null && placement.getEnrollment() != null
          && !placement.getEnrollment().getId().equals(enrollment.getId())) {
        throw new BadRequestException("Placement record does not match the specified enrollment", "PLACEMENT_ENROLLMENT_MISMATCH");
      }
    }

    EmploymentRecord employment = null;
    if (request.getEmploymentId() != null) {
      employment = employmentRecordRepository.findById(request.getEmploymentId())
          .orElseThrow(() -> new ResourceNotFoundException("EmploymentRecord", "employmentId"));
      if (!employment.getTrainee().getId().equals(trainee.getId())) {
        throw new BadRequestException("Employment record does not belong to the specified trainee", "TRAINEE_EMPLOYMENT_MISMATCH");
      }
      if (placement != null && employment.getPlacementRecord() != null
          && !employment.getPlacementRecord().getId().equals(placement.getId())) {
        throw new BadRequestException("Employment record does not match the specified placement record", "EMPLOYMENT_PLACEMENT_MISMATCH");
      }
    }

    if (request.getAssignedUserId() != null) {
      if (!userRepository.existsById(request.getAssignedUserId())) {
        throw new ResourceNotFoundException("User", "assignedUserId");
      }
    }

    RefCommunicationChannel lastChannel = null;
    if (request.getLastChannelId() != null) {
      lastChannel = refCommunicationChannelRepository.findById(request.getLastChannelId())
          .orElseThrow(() -> new ResourceNotFoundException("RefCommunicationChannel", "lastChannelId"));
    }

    Long surveyId = request.getSurveyId() != null ? request.getSurveyId() : campaign.getSurveyId();

    FollowupTask task = followupTaskMapper.toEntity(request, campaign, trainee, enrollment, placement, employment, lastChannel);
    task.setSurveyId(surveyId);
    if (request.getRemarks() != null) {
      task.setRemarks(request.getRemarks().trim());
    }

    LocalDateTime now = LocalDateTime.now();
    task.setCreatedAt(now);
    task.setUpdatedAt(now);

    FollowupTask saved = followupTaskRepository.save(task);
    return followupTaskMapper.toResponse(saved);
  }

  @Override
  @Transactional
  public FollowupTaskResponse updateFollowupTask(Long id, UpdateFollowupTaskRequest request) {
    if (id == null) {
      throw new BadRequestException("Followup task ID is required");
    }
    if (request == null) {
      throw new BadRequestException("Update request cannot be null");
    }

    FollowupTask task = followupTaskRepository.findById(id)
        .orElseThrow(() -> new ResourceNotFoundException("FollowupTask", "id"));

    FollowupCampaign campaign = task.getFollowupCampaign();
    if (campaign == null || campaign.getId() == null) {
      throw new BadRequestException("Followup task does not have an associated campaign", "CAMPAIGN_REQUIRED");
    }
    campaign = followupCampaignRepository.findById(campaign.getId())
        .orElseThrow(() -> new ResourceNotFoundException("FollowupCampaign", "campaignId"));

    if (campaign.getDeletedAt() != null) {
      throw new BadRequestException("Cannot update followup task for a soft-deleted campaign", "CAMPAIGN_DELETED");
    }

    LocalDate effectiveScheduled = request.getScheduledDate() != null
        ? request.getScheduledDate() : task.getScheduledDate();
    LocalDate effectiveNext = request.getNextFollowupDate() != null
        ? request.getNextFollowupDate() : task.getNextFollowupDate();

    if (effectiveNext != null && effectiveNext.isBefore(effectiveScheduled)) {
      throw new BadRequestException("Next followup date cannot be before scheduled date", "INVALID_DATE_RANGE");
    }

    if (request.getScheduledDate() != null) {
      task.setScheduledDate(request.getScheduledDate());
    }
    if (request.getNextFollowupDate() != null) {
      task.setNextFollowupDate(request.getNextFollowupDate());
    }

    Trainee trainee = task.getTrainee();

    if (request.getEnrollmentId() != null) {
      TrainingEnrollment enrollment = trainingEnrollmentRepository.findById(request.getEnrollmentId())
          .orElseThrow(() -> new ResourceNotFoundException("TrainingEnrollment", "enrollmentId"));
      if (!enrollment.getTrainee().getId().equals(trainee.getId())) {
        throw new BadRequestException("Enrollment does not belong to the specified trainee", "TRAINEE_ENROLLMENT_MISMATCH");
      }
      if (campaign.getProgram() != null
          && (enrollment.getProgram() == null || !campaign.getProgram().getId().equals(enrollment.getProgram().getId()))) {
        throw new BadRequestException("Enrollment program does not match campaign program", "PROGRAM_MISMATCH");
      }
      if (campaign.getCourse() != null
          && (enrollment.getCourse() == null || !campaign.getCourse().getId().equals(enrollment.getCourse().getId()))) {
        throw new BadRequestException("Enrollment course does not match campaign course", "COURSE_MISMATCH");
      }
      task.setEnrollment(enrollment);
    }

    if (request.getPlacementId() != null) {
      PlacementRecord placement = placementRecordRepository.findById(request.getPlacementId())
          .orElseThrow(() -> new ResourceNotFoundException("PlacementRecord", "placementId"));
      if (!placement.getTrainee().getId().equals(trainee.getId())) {
        throw new BadRequestException("Placement record does not belong to the specified trainee", "TRAINEE_PLACEMENT_MISMATCH");
      }
      TrainingEnrollment effectiveEnrollment = task.getEnrollment();
      if (effectiveEnrollment != null && placement.getEnrollment() != null
          && !placement.getEnrollment().getId().equals(effectiveEnrollment.getId())) {
        throw new BadRequestException("Placement record does not match the specified enrollment", "PLACEMENT_ENROLLMENT_MISMATCH");
      }
      task.setPlacementRecord(placement);
    } else if (request.getEnrollmentId() != null && task.getPlacementRecord() != null) {
      if (task.getPlacementRecord().getEnrollment() != null
          && !task.getPlacementRecord().getEnrollment().getId().equals(task.getEnrollment().getId())) {
        throw new BadRequestException("Placement record does not match the specified enrollment", "PLACEMENT_ENROLLMENT_MISMATCH");
      }
    }

    if (request.getEmploymentId() != null) {
      EmploymentRecord employment = employmentRecordRepository.findById(request.getEmploymentId())
          .orElseThrow(() -> new ResourceNotFoundException("EmploymentRecord", "employmentId"));
      if (!employment.getTrainee().getId().equals(trainee.getId())) {
        throw new BadRequestException("Employment record does not belong to the specified trainee", "TRAINEE_EMPLOYMENT_MISMATCH");
      }
      PlacementRecord effectivePlacement = task.getPlacementRecord();
      if (effectivePlacement != null && employment.getPlacementRecord() != null
          && !employment.getPlacementRecord().getId().equals(effectivePlacement.getId())) {
        throw new BadRequestException("Employment record does not match the specified placement record", "EMPLOYMENT_PLACEMENT_MISMATCH");
      }
      task.setEmploymentRecord(employment);
    } else if (request.getPlacementId() != null && task.getEmploymentRecord() != null) {
      if (task.getEmploymentRecord().getPlacementRecord() != null
          && !task.getEmploymentRecord().getPlacementRecord().getId().equals(task.getPlacementRecord().getId())) {
        throw new BadRequestException("Employment record does not match the specified placement record", "EMPLOYMENT_PLACEMENT_MISMATCH");
      }
    }

    if (request.getSurveyId() != null) {
      task.setSurveyId(request.getSurveyId());
    }

    if (request.getFollowupStatusId() != null) {
      task.setFollowupStatusId(request.getFollowupStatusId());
    }

    if (request.getFollowupOutcomeId() != null) {
      task.setFollowupOutcomeId(request.getFollowupOutcomeId());
    }

    if (request.getNonResponseReasonId() != null) {
      task.setNonResponseReasonId(request.getNonResponseReasonId());
    }

    if (request.getAssignedUserId() != null) {
      if (!userRepository.existsById(request.getAssignedUserId())) {
        throw new ResourceNotFoundException("User", "assignedUserId");
      }
      task.setAssignedUserId(request.getAssignedUserId());
    }

    if (request.getLastChannelId() != null) {
      RefCommunicationChannel lastChannel = refCommunicationChannelRepository.findById(request.getLastChannelId())
          .orElseThrow(() -> new ResourceNotFoundException("RefCommunicationChannel", "lastChannelId"));
      task.setLastChannel(lastChannel);
    }

    if (request.getRemarks() != null) {
      task.setRemarks(request.getRemarks().trim());
    }

    if (request.getCompletedAt() != null) {
      task.setCompletedAt(request.getCompletedAt());
    }

    task.setUpdatedAt(LocalDateTime.now());
    FollowupTask updated = followupTaskRepository.save(task);
    return followupTaskMapper.toResponse(updated);
  }

  @Override
  public FollowupTaskResponse getFollowupTaskById(Long id) {
    if (id == null) {
      throw new BadRequestException("Followup task ID is required");
    }
    FollowupTask task = followupTaskRepository.findById(id)
        .orElseThrow(() -> new ResourceNotFoundException("FollowupTask", "id"));
    return followupTaskMapper.toResponse(task);
  }

  @Override
  public FollowupTaskResponse getFollowupTaskByCampaignAndTrainee(Long campaignId, Long traineeId) {
    if (campaignId == null) {
      throw new BadRequestException("Campaign ID is required");
    }
    if (traineeId == null) {
      throw new BadRequestException("Trainee ID is required");
    }
    FollowupTask task = followupTaskRepository.findByFollowupCampaignIdAndTraineeId(campaignId, traineeId)
        .orElseThrow(() -> new ResourceNotFoundException("FollowupTask", "campaignId, traineeId"));
    return followupTaskMapper.toResponse(task);
  }

  @Override
  public List<FollowupTaskResponse> getFollowupTasksByCampaign(Long campaignId) {
    if (campaignId == null) {
      throw new BadRequestException("Campaign ID is required");
    }
    return followupTaskRepository.findByFollowupCampaignId(campaignId).stream()
        .map(followupTaskMapper::toResponse)
        .toList();
  }

  @Override
  public List<FollowupTaskResponse> getFollowupTasksByTrainee(Long traineeId) {
    if (traineeId == null) {
      throw new BadRequestException("Trainee ID is required");
    }
    return followupTaskRepository.findByTraineeId(traineeId).stream()
        .map(followupTaskMapper::toResponse)
        .toList();
  }

  @Override
  public List<FollowupTaskResponse> getFollowupTasksByEnrollment(Long enrollmentId) {
    if (enrollmentId == null) {
      throw new BadRequestException("Enrollment ID is required");
    }
    return followupTaskRepository.findByEnrollmentId(enrollmentId).stream()
        .map(followupTaskMapper::toResponse)
        .toList();
  }

  @Override
  public List<FollowupTaskResponse> getFollowupTasksByPlacement(Long placementId) {
    if (placementId == null) {
      throw new BadRequestException("Placement ID is required");
    }
    return followupTaskRepository.findByPlacementRecordId(placementId).stream()
        .map(followupTaskMapper::toResponse)
        .toList();
  }

  @Override
  public List<FollowupTaskResponse> getFollowupTasksByEmployment(Long employmentId) {
    if (employmentId == null) {
      throw new BadRequestException("Employment ID is required");
    }
    return followupTaskRepository.findByEmploymentRecordId(employmentId).stream()
        .map(followupTaskMapper::toResponse)
        .toList();
  }

  @Override
  public List<FollowupTaskResponse> getFollowupTasksBySurvey(Long surveyId) {
    if (surveyId == null) {
      throw new BadRequestException("Survey ID is required");
    }
    return followupTaskRepository.findBySurveyId(surveyId).stream()
        .map(followupTaskMapper::toResponse)
        .toList();
  }

  @Override
  public List<FollowupTaskResponse> getFollowupTasksByStatus(Long statusId) {
    if (statusId == null) {
      throw new BadRequestException("Followup status ID is required");
    }
    return followupTaskRepository.findByFollowupStatusId(statusId).stream()
        .map(followupTaskMapper::toResponse)
        .toList();
  }

  @Override
  public List<FollowupTaskResponse> getFollowupTasksByOutcome(Long outcomeId) {
    if (outcomeId == null) {
      throw new BadRequestException("Followup outcome ID is required");
    }
    return followupTaskRepository.findByFollowupOutcomeId(outcomeId).stream()
        .map(followupTaskMapper::toResponse)
        .toList();
  }

  @Override
  public List<FollowupTaskResponse> getFollowupTasksByAssignedUser(Long assignedUserId) {
    if (assignedUserId == null) {
      throw new BadRequestException("Assigned user ID is required");
    }
    return followupTaskRepository.findByAssignedUserId(assignedUserId).stream()
        .map(followupTaskMapper::toResponse)
        .toList();
  }

  @Override
  public List<FollowupTaskResponse> getFollowupTasksByScheduledDate(LocalDate scheduledDate) {
    if (scheduledDate == null) {
      throw new BadRequestException("Scheduled date is required");
    }
    return followupTaskRepository.findByScheduledDate(scheduledDate).stream()
        .map(followupTaskMapper::toResponse)
        .toList();
  }

  @Override
  @Transactional
  public void deleteFollowupTask(Long id) {
    if (id == null) {
      throw new BadRequestException("Followup task ID is required");
    }
    FollowupTask task = followupTaskRepository.findById(id)
        .orElseThrow(() -> new ResourceNotFoundException("FollowupTask", "id"));

    if (!surveyResponseRepository.findByFollowupTaskId(id).isEmpty()) {
      throw new ConflictException("Cannot delete followup task with associated survey responses", "TASK_HAS_RESPONSES");
    }

    if (!communicationLogRepository.findByFollowupTaskId(id).isEmpty()) {
      throw new ConflictException("Cannot delete followup task with associated communication logs", "TASK_HAS_COMMUNICATIONS");
    }

    followupTaskRepository.delete(task);
  }
}
