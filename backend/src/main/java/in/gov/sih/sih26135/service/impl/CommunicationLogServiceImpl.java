package in.gov.sih.sih26135.service.impl;

import in.gov.sih.sih26135.dto.request.CreateCommunicationLogRequest;
import in.gov.sih.sih26135.dto.request.UpdateCommunicationLogRequest;
import in.gov.sih.sih26135.dto.response.CommunicationLogResponse;
import in.gov.sih.sih26135.entity.CommunicationLog;
import in.gov.sih.sih26135.entity.FollowupTask;
import in.gov.sih.sih26135.entity.RefCommunicationChannel;
import in.gov.sih.sih26135.entity.RefCommunicationDirection;
import in.gov.sih.sih26135.entity.RefCommunicationPurpose;
import in.gov.sih.sih26135.entity.RefCommunicationStatus;
import in.gov.sih.sih26135.entity.RefConsentType;
import in.gov.sih.sih26135.entity.SurveyResponse;
import in.gov.sih.sih26135.entity.Trainee;
import in.gov.sih.sih26135.exception.BadRequestException;
import in.gov.sih.sih26135.exception.ConflictException;
import in.gov.sih.sih26135.exception.ResourceNotFoundException;
import in.gov.sih.sih26135.mapper.CommunicationLogMapper;
import in.gov.sih.sih26135.repository.CommunicationLogRepository;
import in.gov.sih.sih26135.repository.FollowupTaskRepository;
import in.gov.sih.sih26135.repository.RefCommunicationChannelRepository;
import in.gov.sih.sih26135.repository.RefCommunicationDirectionRepository;
import in.gov.sih.sih26135.repository.RefCommunicationPurposeRepository;
import in.gov.sih.sih26135.repository.RefCommunicationStatusRepository;
import in.gov.sih.sih26135.repository.RefConsentTypeRepository;
import in.gov.sih.sih26135.repository.SurveyResponseRepository;
import in.gov.sih.sih26135.repository.TraineeRepository;
import in.gov.sih.sih26135.repository.UserRepository;
import in.gov.sih.sih26135.service.CommunicationLogService;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class CommunicationLogServiceImpl implements CommunicationLogService {

  private final CommunicationLogRepository communicationLogRepository;
  private final TraineeRepository traineeRepository;
  private final FollowupTaskRepository followupTaskRepository;
  private final SurveyResponseRepository surveyResponseRepository;
  private final RefCommunicationChannelRepository refCommunicationChannelRepository;
  private final RefCommunicationDirectionRepository refCommunicationDirectionRepository;
  private final RefCommunicationPurposeRepository refCommunicationPurposeRepository;
  private final RefCommunicationStatusRepository refCommunicationStatusRepository;
  private final RefConsentTypeRepository refConsentTypeRepository;
  private final UserRepository userRepository;
  private final CommunicationLogMapper communicationLogMapper;

  public CommunicationLogServiceImpl(
      CommunicationLogRepository communicationLogRepository,
      TraineeRepository traineeRepository,
      FollowupTaskRepository followupTaskRepository,
      SurveyResponseRepository surveyResponseRepository,
      RefCommunicationChannelRepository refCommunicationChannelRepository,
      RefCommunicationDirectionRepository refCommunicationDirectionRepository,
      RefCommunicationPurposeRepository refCommunicationPurposeRepository,
      RefCommunicationStatusRepository refCommunicationStatusRepository,
      RefConsentTypeRepository refConsentTypeRepository,
      UserRepository userRepository,
      CommunicationLogMapper communicationLogMapper) {
    this.communicationLogRepository = communicationLogRepository;
    this.traineeRepository = traineeRepository;
    this.followupTaskRepository = followupTaskRepository;
    this.surveyResponseRepository = surveyResponseRepository;
    this.refCommunicationChannelRepository = refCommunicationChannelRepository;
    this.refCommunicationDirectionRepository = refCommunicationDirectionRepository;
    this.refCommunicationPurposeRepository = refCommunicationPurposeRepository;
    this.refCommunicationStatusRepository = refCommunicationStatusRepository;
    this.refConsentTypeRepository = refConsentTypeRepository;
    this.userRepository = userRepository;
    this.communicationLogMapper = communicationLogMapper;
  }

  @Override
  @Transactional
  public CommunicationLogResponse logCommunication(CreateCommunicationLogRequest request) {
    if (request == null) {
      throw new BadRequestException("Communication log creation request cannot be null");
    }
    if (request.getTraineeId() == null) {
      throw new BadRequestException("Trainee ID is required");
    }
    if (request.getCommunicationChannelId() == null) {
      throw new BadRequestException("Communication channel ID is required");
    }
    if (request.getCommunicationDirectionId() == null) {
      throw new BadRequestException("Communication direction ID is required");
    }
    if (request.getCommunicationPurposeId() == null) {
      throw new BadRequestException("Communication purpose ID is required");
    }
    if (request.getCommunicationStatusId() == null) {
      throw new BadRequestException("Communication status ID is required");
    }

    String providerMsgId = request.getProviderMessageId() != null && !request.getProviderMessageId().isBlank()
        ? request.getProviderMessageId().trim() : null;
    if (providerMsgId != null && communicationLogRepository.existsByProviderMessageId(providerMsgId)) {
      throw new ConflictException("Communication log already exists for this provider message ID", "DUPLICATE_PROVIDER_MESSAGE_ID");
    }

    validateTimestampSequence(
        request.getQueuedAt(),
        request.getSentAt(),
        request.getDeliveredAt(),
        request.getReadAt(),
        request.getFailedAt()
    );

    Trainee trainee = traineeRepository.findById(request.getTraineeId())
        .orElseThrow(() -> new ResourceNotFoundException("Trainee", "traineeId"));

    FollowupTask followupTask = null;
    if (request.getFollowupTaskId() != null) {
      followupTask = followupTaskRepository.findById(request.getFollowupTaskId())
          .orElseThrow(() -> new ResourceNotFoundException("FollowupTask", "followupTaskId"));
      if (!followupTask.getTrainee().getId().equals(trainee.getId())) {
        throw new BadRequestException("Followup task does not belong to the specified trainee", "TRAINEE_TASK_MISMATCH");
      }
    }

    SurveyResponse surveyResponse = null;
    if (request.getSurveyResponseId() != null) {
      surveyResponse = surveyResponseRepository.findById(request.getSurveyResponseId())
          .orElseThrow(() -> new ResourceNotFoundException("SurveyResponse", "surveyResponseId"));
      if (!surveyResponse.getTrainee().getId().equals(trainee.getId())) {
        throw new BadRequestException("Survey response does not belong to the specified trainee", "TRAINEE_RESPONSE_MISMATCH");
      }
    }

    RefCommunicationChannel channel = refCommunicationChannelRepository.findById(request.getCommunicationChannelId())
        .orElseThrow(() -> new ResourceNotFoundException("RefCommunicationChannel", "communicationChannelId"));

    RefCommunicationDirection direction = refCommunicationDirectionRepository.findById(request.getCommunicationDirectionId())
        .orElseThrow(() -> new ResourceNotFoundException("RefCommunicationDirection", "communicationDirectionId"));

    RefCommunicationPurpose purpose = refCommunicationPurposeRepository.findById(request.getCommunicationPurposeId())
        .orElseThrow(() -> new ResourceNotFoundException("RefCommunicationPurpose", "communicationPurposeId"));

    RefCommunicationStatus status = refCommunicationStatusRepository.findById(request.getCommunicationStatusId())
        .orElseThrow(() -> new ResourceNotFoundException("RefCommunicationStatus", "communicationStatusId"));

    RefConsentType consentType = null;
    if (request.getConsentTypeId() != null) {
      consentType = refConsentTypeRepository.findById(request.getConsentTypeId())
          .orElseThrow(() -> new ResourceNotFoundException("RefConsentType", "consentTypeId"));
    }

    if (request.getInitiatedByUserId() != null) {
      if (!userRepository.existsById(request.getInitiatedByUserId())) {
        throw new ResourceNotFoundException("User", "initiatedByUserId");
      }
    }

    CommunicationLog entity = communicationLogMapper.toEntity(
        request, trainee, followupTask, surveyResponse, channel, direction, purpose, status, consentType
    );
    entity.setProviderMessageId(providerMsgId);
    if (request.getMessageTemplateCode() != null) {
      entity.setMessageTemplateCode(request.getMessageTemplateCode().trim());
    }
    if (request.getFailureReason() != null) {
      entity.setFailureReason(request.getFailureReason().trim());
    }

    LocalDateTime now = LocalDateTime.now();
    entity.setCreatedAt(now);
    entity.setUpdatedAt(now);

    CommunicationLog saved = communicationLogRepository.save(entity);
    return communicationLogMapper.toResponse(saved);
  }

  @Override
  @Transactional
  public CommunicationLogResponse updateCommunicationLog(Long id, UpdateCommunicationLogRequest request) {
    if (id == null) {
      throw new BadRequestException("Communication log ID is required");
    }
    if (request == null) {
      throw new BadRequestException("Update request cannot be null");
    }

    CommunicationLog log = communicationLogRepository.findById(id)
        .orElseThrow(() -> new ResourceNotFoundException("CommunicationLog", "id"));

    LocalDateTime effectiveQueued = request.getQueuedAt() != null ? request.getQueuedAt() : log.getQueuedAt();
    LocalDateTime effectiveSent = request.getSentAt() != null ? request.getSentAt() : log.getSentAt();
    LocalDateTime effectiveDelivered = request.getDeliveredAt() != null ? request.getDeliveredAt() : log.getDeliveredAt();
    LocalDateTime effectiveRead = request.getReadAt() != null ? request.getReadAt() : log.getReadAt();
    LocalDateTime effectiveFailed = request.getFailedAt() != null ? request.getFailedAt() : log.getFailedAt();

    validateTimestampSequence(effectiveQueued, effectiveSent, effectiveDelivered, effectiveRead, effectiveFailed);

    if (request.getCommunicationStatusId() != null) {
      RefCommunicationStatus status = refCommunicationStatusRepository.findById(request.getCommunicationStatusId())
          .orElseThrow(() -> new ResourceNotFoundException("RefCommunicationStatus", "communicationStatusId"));
      log.setCommunicationStatus(status);
    }

    if (request.getFailureReason() != null) {
      log.setFailureReason(request.getFailureReason().trim());
    }
    if (request.getQueuedAt() != null) {
      log.setQueuedAt(request.getQueuedAt());
    }
    if (request.getSentAt() != null) {
      log.setSentAt(request.getSentAt());
    }
    if (request.getDeliveredAt() != null) {
      log.setDeliveredAt(request.getDeliveredAt());
    }
    if (request.getReadAt() != null) {
      log.setReadAt(request.getReadAt());
    }
    if (request.getFailedAt() != null) {
      log.setFailedAt(request.getFailedAt());
    }

    log.setUpdatedAt(LocalDateTime.now());
    CommunicationLog updated = communicationLogRepository.save(log);
    return communicationLogMapper.toResponse(updated);
  }

  @Override
  public CommunicationLogResponse getCommunicationLogById(Long id) {
    if (id == null) {
      throw new BadRequestException("Communication log ID is required");
    }
    CommunicationLog log = communicationLogRepository.findById(id)
        .orElseThrow(() -> new ResourceNotFoundException("CommunicationLog", "id"));
    return communicationLogMapper.toResponse(log);
  }

  @Override
  public CommunicationLogResponse getCommunicationLogByProviderMessageId(String providerMessageId) {
    if (providerMessageId == null || providerMessageId.isBlank()) {
      throw new BadRequestException("Provider message ID is required");
    }
    CommunicationLog log = communicationLogRepository.findByProviderMessageId(providerMessageId.trim())
        .orElseThrow(() -> new ResourceNotFoundException("CommunicationLog", "providerMessageId"));
    return communicationLogMapper.toResponse(log);
  }

  @Override
  public List<CommunicationLogResponse> getCommunicationLogsByTrainee(Long traineeId) {
    if (traineeId == null) {
      throw new BadRequestException("Trainee ID is required");
    }
    return communicationLogRepository.findByTraineeId(traineeId).stream()
        .map(communicationLogMapper::toResponse)
        .toList();
  }

  @Override
  public List<CommunicationLogResponse> getCommunicationLogsByFollowupTask(Long followupTaskId) {
    if (followupTaskId == null) {
      throw new BadRequestException("Followup task ID is required");
    }
    return communicationLogRepository.findByFollowupTaskId(followupTaskId).stream()
        .map(communicationLogMapper::toResponse)
        .toList();
  }

  @Override
  public List<CommunicationLogResponse> getCommunicationLogsBySurvey(Long surveyId) {
    if (surveyId == null) {
      throw new BadRequestException("Survey ID is required");
    }
    return communicationLogRepository.findBySurveyId(surveyId).stream()
        .map(communicationLogMapper::toResponse)
        .toList();
  }

  @Override
  public List<CommunicationLogResponse> getCommunicationLogsBySurveyResponse(Long surveyResponseId) {
    if (surveyResponseId == null) {
      throw new BadRequestException("Survey response ID is required");
    }
    return communicationLogRepository.findBySurveyResponseId(surveyResponseId).stream()
        .map(communicationLogMapper::toResponse)
        .toList();
  }

  @Override
  public List<CommunicationLogResponse> getCommunicationLogsByChannel(Long channelId) {
    if (channelId == null) {
      throw new BadRequestException("Channel ID is required");
    }
    return communicationLogRepository.findByCommunicationChannelId(channelId).stream()
        .map(communicationLogMapper::toResponse)
        .toList();
  }

  @Override
  public List<CommunicationLogResponse> getCommunicationLogsByStatus(Long statusId) {
    if (statusId == null) {
      throw new BadRequestException("Status ID is required");
    }
    return communicationLogRepository.findByCommunicationStatusId(statusId).stream()
        .map(communicationLogMapper::toResponse)
        .toList();
  }

  @Override
  public List<CommunicationLogResponse> getCommunicationLogsByPurpose(Long purposeId) {
    if (purposeId == null) {
      throw new BadRequestException("Purpose ID is required");
    }
    return communicationLogRepository.findByCommunicationPurposeId(purposeId).stream()
        .map(communicationLogMapper::toResponse)
        .toList();
  }

  @Override
  public List<CommunicationLogResponse> getCommunicationLogsByDirection(Long directionId) {
    if (directionId == null) {
      throw new BadRequestException("Direction ID is required");
    }
    return communicationLogRepository.findByCommunicationDirectionId(directionId).stream()
        .map(communicationLogMapper::toResponse)
        .toList();
  }

  @Override
  @Transactional
  public void deleteCommunicationLog(Long id) {
    if (id == null) {
      throw new BadRequestException("Communication log ID is required");
    }
    CommunicationLog log = communicationLogRepository.findById(id)
        .orElseThrow(() -> new ResourceNotFoundException("CommunicationLog", "id"));

    if (log.getSentAt() != null || log.getDeliveredAt() != null || log.getReadAt() != null) {
      throw new ConflictException("Cannot delete communication log that has already been dispatched or delivered", "DISPATCHED_COMMUNICATION_IMMUTABLE");
    }

    communicationLogRepository.delete(log);
  }

  private void validateTimestampSequence(
      LocalDateTime queuedAt,
      LocalDateTime sentAt,
      LocalDateTime deliveredAt,
      LocalDateTime readAt,
      LocalDateTime failedAt) {
    if (sentAt != null && queuedAt != null && sentAt.isBefore(queuedAt)) {
      throw new BadRequestException("Sent timestamp cannot be before queued timestamp", "INVALID_TIMESTAMP_SEQUENCE");
    }
    if (deliveredAt != null && sentAt != null && deliveredAt.isBefore(sentAt)) {
      throw new BadRequestException("Delivered timestamp cannot be before sent timestamp", "INVALID_TIMESTAMP_SEQUENCE");
    }
    if (readAt != null && deliveredAt != null && readAt.isBefore(deliveredAt)) {
      throw new BadRequestException("Read timestamp cannot be before delivered timestamp", "INVALID_TIMESTAMP_SEQUENCE");
    }
    if (failedAt != null && queuedAt != null && failedAt.isBefore(queuedAt)) {
      throw new BadRequestException("Failed timestamp cannot be before queued timestamp", "INVALID_TIMESTAMP_SEQUENCE");
    }
  }
}
