package in.gov.sih.sih26135.service.impl;

import in.gov.sih.sih26135.dto.response.JoiningStatusResponse;
import in.gov.sih.sih26135.entity.RefJoiningStatus;
import in.gov.sih.sih26135.exception.BadRequestException;
import in.gov.sih.sih26135.exception.ResourceNotFoundException;
import in.gov.sih.sih26135.mapper.JoiningStatusMapper;
import in.gov.sih.sih26135.repository.RefJoiningStatusRepository;
import in.gov.sih.sih26135.service.JoiningStatusReferenceService;
import java.util.List;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class JoiningStatusReferenceServiceImpl implements JoiningStatusReferenceService {

  private final RefJoiningStatusRepository refJoiningStatusRepository;
  private final JoiningStatusMapper joiningStatusMapper;

  public JoiningStatusReferenceServiceImpl(
      RefJoiningStatusRepository refJoiningStatusRepository,
      JoiningStatusMapper joiningStatusMapper) {
    this.refJoiningStatusRepository = refJoiningStatusRepository;
    this.joiningStatusMapper = joiningStatusMapper;
  }

  @Override
  public List<JoiningStatusResponse> getAllJoiningStatuses() {
    return refJoiningStatusRepository.findAll(Sort.by(Sort.Direction.ASC, "sortOrder")).stream()
        .map(joiningStatusMapper::toResponse)
        .toList();
  }

  @Override
  public JoiningStatusResponse getById(Long id) {
    if (id == null) {
      throw new BadRequestException("Joining status ID is required");
    }
    RefJoiningStatus entity = refJoiningStatusRepository.findById(id)
        .orElseThrow(() -> new ResourceNotFoundException("RefJoiningStatus", "id"));
    return joiningStatusMapper.toResponse(entity);
  }

  @Override
  public JoiningStatusResponse getByCode(String statusCode) {
    if (statusCode == null || statusCode.isBlank()) {
      throw new BadRequestException("Status code is required");
    }
    RefJoiningStatus entity = refJoiningStatusRepository.findByStatusCode(statusCode.trim())
        .orElseThrow(() -> new ResourceNotFoundException("RefJoiningStatus", "statusCode"));
    return joiningStatusMapper.toResponse(entity);
  }

  @Override
  public List<JoiningStatusResponse> getByIsJoinedFlag(Boolean isJoinedFlag) {
    if (isJoinedFlag == null) {
      throw new BadRequestException("isJoinedFlag is required");
    }
    return refJoiningStatusRepository.findByIsJoinedFlag(isJoinedFlag).stream()
        .map(joiningStatusMapper::toResponse)
        .toList();
  }
}
