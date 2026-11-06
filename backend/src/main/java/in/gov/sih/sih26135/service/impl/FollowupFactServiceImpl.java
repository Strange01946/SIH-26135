package in.gov.sih.sih26135.service.impl;

import in.gov.sih.sih26135.dto.response.FollowupFactResponse;
import in.gov.sih.sih26135.entity.analytics.FollowupFact;
import in.gov.sih.sih26135.exception.BadRequestException;
import in.gov.sih.sih26135.exception.ResourceNotFoundException;
import in.gov.sih.sih26135.mapper.FollowupFactMapper;
import in.gov.sih.sih26135.repository.analytics.FollowupFactRepository;
import in.gov.sih.sih26135.response.PagedResponse;
import in.gov.sih.sih26135.service.FollowupFactService;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class FollowupFactServiceImpl implements FollowupFactService {

  private final FollowupFactRepository repository;
  private final FollowupFactMapper mapper;

  public FollowupFactServiceImpl(
      FollowupFactRepository repository,
      FollowupFactMapper mapper) {
    this.repository = repository;
    this.mapper = mapper;
  }

  @Override
  public PagedResponse<FollowupFactResponse> getAllFollowupFacts(Pageable pageable) {
    Page<FollowupFact> page = repository.findAll(pageable);
    return PagedResponse.from(page, mapper::toResponse);
  }

  @Override
  public List<FollowupFactResponse> getAllFollowupFacts() {
    return repository.findAll().stream()
        .map(mapper::toResponse)
        .toList();
  }

  @Override
  public FollowupFactResponse getFollowupFactById(Long followupTaskId) {
    if (followupTaskId == null) {
      throw new BadRequestException("Followup task ID is required");
    }
    FollowupFact entity = repository.findById(followupTaskId)
        .orElseThrow(() -> new ResourceNotFoundException("FollowupFact", "followupTaskId"));
    return mapper.toResponse(entity);
  }

  @Override
  public List<FollowupFactResponse> getFollowupFactsByTraineeId(Long traineeId) {
    if (traineeId == null) {
      throw new BadRequestException("Trainee ID is required");
    }
    return repository.findByTraineeId(traineeId).stream()
        .map(mapper::toResponse)
        .toList();
  }

  @Override
  public PagedResponse<FollowupFactResponse> getFollowupFactsByTraineeId(Long traineeId, Pageable pageable) {
    if (traineeId == null) {
      throw new BadRequestException("Trainee ID is required");
    }
    Page<FollowupFact> page = repository.findByTraineeId(traineeId, pageable);
    return PagedResponse.from(page, mapper::toResponse);
  }

  @Override
  public List<FollowupFactResponse> getFollowupFactsByCampaignId(Long followupCampaignId) {
    if (followupCampaignId == null) {
      throw new BadRequestException("Followup campaign ID is required");
    }
    return repository.findByFollowupCampaignId(followupCampaignId).stream()
        .map(mapper::toResponse)
        .toList();
  }

  @Override
  public PagedResponse<FollowupFactResponse> getFollowupFactsByCampaignId(Long followupCampaignId, Pageable pageable) {
    if (followupCampaignId == null) {
      throw new BadRequestException("Followup campaign ID is required");
    }
    Page<FollowupFact> page = repository.findByFollowupCampaignId(followupCampaignId, pageable);
    return PagedResponse.from(page, mapper::toResponse);
  }

  @Override
  public List<FollowupFactResponse> getFollowupFactsByTypeId(Long followupTypeId) {
    if (followupTypeId == null) {
      throw new BadRequestException("Followup type ID is required");
    }
    return repository.findByFollowupTypeId(followupTypeId).stream()
        .map(mapper::toResponse)
        .toList();
  }

  @Override
  public PagedResponse<FollowupFactResponse> getFollowupFactsByTypeId(Long followupTypeId, Pageable pageable) {
    if (followupTypeId == null) {
      throw new BadRequestException("Followup type ID is required");
    }
    Page<FollowupFact> page = repository.findByFollowupTypeId(followupTypeId, pageable);
    return PagedResponse.from(page, mapper::toResponse);
  }

  @Override
  public List<FollowupFactResponse> getFollowupFactsByTypeCode(String followupTypeCode) {
    if (followupTypeCode == null || followupTypeCode.isBlank()) {
      throw new BadRequestException("Followup type code is required");
    }
    return repository.findByFollowupTypeCode(followupTypeCode.trim()).stream()
        .map(mapper::toResponse)
        .toList();
  }

  @Override
  public PagedResponse<FollowupFactResponse> getFollowupFactsByTypeCode(String followupTypeCode, Pageable pageable) {
    if (followupTypeCode == null || followupTypeCode.isBlank()) {
      throw new BadRequestException("Followup type code is required");
    }
    Page<FollowupFact> page = repository.findByFollowupTypeCode(followupTypeCode.trim(), pageable);
    return PagedResponse.from(page, mapper::toResponse);
  }

  @Override
  public List<FollowupFactResponse> getFollowupFactsByStatusId(Long followupStatusId) {
    if (followupStatusId == null) {
      throw new BadRequestException("Followup status ID is required");
    }
    return repository.findByFollowupStatusId(followupStatusId).stream()
        .map(mapper::toResponse)
        .toList();
  }

  @Override
  public PagedResponse<FollowupFactResponse> getFollowupFactsByStatusId(Long followupStatusId, Pageable pageable) {
    if (followupStatusId == null) {
      throw new BadRequestException("Followup status ID is required");
    }
    Page<FollowupFact> page = repository.findByFollowupStatusId(followupStatusId, pageable);
    return PagedResponse.from(page, mapper::toResponse);
  }

  @Override
  public List<FollowupFactResponse> getFollowupFactsByOutcomeId(Long followupOutcomeId) {
    if (followupOutcomeId == null) {
      throw new BadRequestException("Followup outcome ID is required");
    }
    return repository.findByFollowupOutcomeId(followupOutcomeId).stream()
        .map(mapper::toResponse)
        .toList();
  }

  @Override
  public PagedResponse<FollowupFactResponse> getFollowupFactsByOutcomeId(Long followupOutcomeId, Pageable pageable) {
    if (followupOutcomeId == null) {
      throw new BadRequestException("Followup outcome ID is required");
    }
    Page<FollowupFact> page = repository.findByFollowupOutcomeId(followupOutcomeId, pageable);
    return PagedResponse.from(page, mapper::toResponse);
  }

  @Override
  public List<FollowupFactResponse> getFollowupFactsByIsOpenFlag(Boolean isOpenFlag) {
    if (isOpenFlag == null) {
      throw new BadRequestException("isOpenFlag is required");
    }
    return repository.findByIsOpenFlag(isOpenFlag).stream()
        .map(mapper::toResponse)
        .toList();
  }

  @Override
  public PagedResponse<FollowupFactResponse> getFollowupFactsByIsOpenFlag(Boolean isOpenFlag, Pageable pageable) {
    if (isOpenFlag == null) {
      throw new BadRequestException("isOpenFlag is required");
    }
    Page<FollowupFact> page = repository.findByIsOpenFlag(isOpenFlag, pageable);
    return PagedResponse.from(page, mapper::toResponse);
  }
}
