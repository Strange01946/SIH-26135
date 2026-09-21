package in.gov.sih.sih26135.service.impl;

import in.gov.sih.sih26135.dto.response.AccreditationStatusResponse;
import in.gov.sih.sih26135.entity.RefAccreditationStatus;
import in.gov.sih.sih26135.exception.ResourceNotFoundException;
import in.gov.sih.sih26135.mapper.AccreditationStatusMapper;
import in.gov.sih.sih26135.repository.RefAccreditationStatusRepository;
import in.gov.sih.sih26135.service.AccreditationReferenceService;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class AccreditationReferenceServiceImpl implements AccreditationReferenceService {

  private final RefAccreditationStatusRepository refAccreditationStatusRepository;
  private final AccreditationStatusMapper accreditationStatusMapper;

  public AccreditationReferenceServiceImpl(
      RefAccreditationStatusRepository refAccreditationStatusRepository,
      AccreditationStatusMapper accreditationStatusMapper) {
    this.refAccreditationStatusRepository = refAccreditationStatusRepository;
    this.accreditationStatusMapper = accreditationStatusMapper;
  }

  @Override
  public List<AccreditationStatusResponse> getAllAccreditationStatuses() {
    return refAccreditationStatusRepository.findAll().stream()
        .map(accreditationStatusMapper::toResponse)
        .toList();
  }

  @Override
  public AccreditationStatusResponse getById(Long id) {
    RefAccreditationStatus status = refAccreditationStatusRepository.findById(id)
        .orElseThrow(() -> new ResourceNotFoundException("RefAccreditationStatus", "id"));
    return accreditationStatusMapper.toResponse(status);
  }

  @Override
  public AccreditationStatusResponse getByCode(String statusCode) {
    RefAccreditationStatus status = refAccreditationStatusRepository.findByStatusCode(statusCode)
        .orElseThrow(() -> new ResourceNotFoundException("RefAccreditationStatus", "statusCode"));
    return accreditationStatusMapper.toResponse(status);
  }
}
