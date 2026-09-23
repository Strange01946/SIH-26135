package in.gov.sih.sih26135.service.impl;

import in.gov.sih.sih26135.dto.request.CreateDataQualityIssueEventRequest;
import in.gov.sih.sih26135.dto.response.DataQualityIssueEventResponse;
import in.gov.sih.sih26135.entity.DataQualityIssue;
import in.gov.sih.sih26135.entity.DataQualityIssueEvent;
import in.gov.sih.sih26135.entity.RefDataQualityIssueStatus;
import in.gov.sih.sih26135.exception.BadRequestException;
import in.gov.sih.sih26135.exception.ResourceNotFoundException;
import in.gov.sih.sih26135.mapper.DataQualityIssueEventMapper;
import in.gov.sih.sih26135.repository.DataQualityIssueEventRepository;
import in.gov.sih.sih26135.repository.DataQualityIssueRepository;
import in.gov.sih.sih26135.repository.RefDataQualityIssueStatusRepository;
import in.gov.sih.sih26135.repository.UserRepository;
import in.gov.sih.sih26135.service.DataQualityIssueEventService;
import java.time.LocalDateTime;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class DataQualityIssueEventServiceImpl implements DataQualityIssueEventService {

  private static final Logger log = LoggerFactory.getLogger(DataQualityIssueEventServiceImpl.class);

  private final DataQualityIssueEventRepository dataQualityIssueEventRepository;
  private final DataQualityIssueRepository dataQualityIssueRepository;
  private final RefDataQualityIssueStatusRepository refDataQualityIssueStatusRepository;
  private final UserRepository userRepository;
  private final DataQualityIssueEventMapper mapper;

  public DataQualityIssueEventServiceImpl(
      DataQualityIssueEventRepository dataQualityIssueEventRepository,
      DataQualityIssueRepository dataQualityIssueRepository,
      RefDataQualityIssueStatusRepository refDataQualityIssueStatusRepository,
      UserRepository userRepository,
      DataQualityIssueEventMapper mapper) {
    this.dataQualityIssueEventRepository = dataQualityIssueEventRepository;
    this.dataQualityIssueRepository = dataQualityIssueRepository;
    this.refDataQualityIssueStatusRepository = refDataQualityIssueStatusRepository;
    this.userRepository = userRepository;
    this.mapper = mapper;
  }

  @Override
  @Transactional
  public DataQualityIssueEventResponse recordIssueEvent(CreateDataQualityIssueEventRequest request) {
    if (request == null) {
      throw new BadRequestException("Request body cannot be null", "REQUEST_BODY_NULL");
    }
    if (request.getDataQualityIssueId() == null) {
      throw new BadRequestException("Data quality issue ID is required", "DATA_QUALITY_ISSUE_ID_REQUIRED");
    }
    if (request.getDataQualityIssueStatusId() == null) {
      throw new BadRequestException("Data quality issue status ID is required", "STATUS_ID_REQUIRED");
    }

    DataQualityIssue issue = dataQualityIssueRepository.findById(request.getDataQualityIssueId())
        .orElseThrow(() -> new ResourceNotFoundException("DataQualityIssue", "id"));

    RefDataQualityIssueStatus status = refDataQualityIssueStatusRepository.findById(request.getDataQualityIssueStatusId())
        .orElseThrow(() -> new ResourceNotFoundException("RefDataQualityIssueStatus", "id"));

    if (request.getChangedByUserId() != null && !userRepository.existsById(request.getChangedByUserId())) {
      throw new ResourceNotFoundException("User", "id");
    }

    if (request.getRemarks() != null && request.getRemarks().length() > 500) {
      throw new BadRequestException("Remarks must not exceed 500 characters", "REMARKS_TOO_LONG");
    }

    LocalDateTime changedAt = request.getChangedAt() != null ? request.getChangedAt() : LocalDateTime.now();

    DataQualityIssueEvent event = new DataQualityIssueEvent(issue, status, changedAt);
    event.setChangedByUserId(request.getChangedByUserId());
    event.setRemarks(request.getRemarks());

    DataQualityIssueEvent saved = dataQualityIssueEventRepository.save(event);
    log.info("Recorded data quality issue event id={} for issue id={}, status id={}",
        saved.getId(), issue.getId(), status.getId());
    return mapper.toResponse(saved);
  }

  @Override
  public DataQualityIssueEventResponse getIssueEventById(Long id) {
    if (id == null) {
      throw new BadRequestException("Event ID is required");
    }
    DataQualityIssueEvent event = dataQualityIssueEventRepository.findById(id)
        .orElseThrow(() -> new ResourceNotFoundException("DataQualityIssueEvent", "id"));
    return mapper.toResponse(event);
  }

  @Override
  public List<DataQualityIssueEventResponse> getIssueEventsByIssueId(Long issueId) {
    if (issueId == null) {
      throw new BadRequestException("Issue ID is required");
    }
    return dataQualityIssueEventRepository.findByDataQualityIssueId(issueId).stream()
        .map(mapper::toResponse)
        .toList();
  }

  @Override
  public List<DataQualityIssueEventResponse> getIssueEventsByStatusId(Long statusId) {
    if (statusId == null) {
      throw new BadRequestException("Status ID is required");
    }
    return dataQualityIssueEventRepository.findByDataQualityIssueStatusId(statusId).stream()
        .map(mapper::toResponse)
        .toList();
  }

  @Override
  public List<DataQualityIssueEventResponse> getIssueEventsByChangedByUserId(Long changedByUserId) {
    if (changedByUserId == null) {
      throw new BadRequestException("Changed-by user ID is required");
    }
    return dataQualityIssueEventRepository.findByChangedByUserId(changedByUserId).stream()
        .map(mapper::toResponse)
        .toList();
  }
}
