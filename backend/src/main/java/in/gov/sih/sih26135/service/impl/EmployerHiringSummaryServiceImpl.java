package in.gov.sih.sih26135.service.impl;

import in.gov.sih.sih26135.dto.response.EmployerHiringSummaryResponse;
import in.gov.sih.sih26135.entity.analytics.EmployerHiringSummary;
import in.gov.sih.sih26135.exception.BadRequestException;
import in.gov.sih.sih26135.exception.ResourceNotFoundException;
import in.gov.sih.sih26135.mapper.EmployerHiringSummaryMapper;
import in.gov.sih.sih26135.repository.analytics.EmployerHiringSummaryRepository;
import in.gov.sih.sih26135.service.EmployerHiringSummaryService;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class EmployerHiringSummaryServiceImpl implements EmployerHiringSummaryService {

  private final EmployerHiringSummaryRepository repository;
  private final EmployerHiringSummaryMapper mapper;

  public EmployerHiringSummaryServiceImpl(
      EmployerHiringSummaryRepository repository,
      EmployerHiringSummaryMapper mapper) {
    this.repository = repository;
    this.mapper = mapper;
  }

  @Override
  public List<EmployerHiringSummaryResponse> getAllEmployerHiringSummaries() {
    return repository.findAll().stream()
        .map(mapper::toResponse)
        .toList();
  }

  @Override
  public List<EmployerHiringSummaryResponse> getAllEmployerHiringSummariesOrderByJoinedCountDesc() {
    return repository.findAllByOrderByJoinedCountDesc().stream()
        .map(mapper::toResponse)
        .toList();
  }

  @Override
  public List<EmployerHiringSummaryResponse> getAllEmployerHiringSummariesOrderByPlacementCountDesc() {
    return repository.findAllByOrderByPlacementCountDesc().stream()
        .map(mapper::toResponse)
        .toList();
  }

  @Override
  public EmployerHiringSummaryResponse getEmployerHiringSummaryByEmployerId(Long employerId) {
    if (employerId == null) {
      throw new BadRequestException("Employer ID is required");
    }
    EmployerHiringSummary entity = repository.findById(employerId)
        .orElseThrow(() -> new ResourceNotFoundException("EmployerHiringSummary", "employerId"));
    return mapper.toResponse(entity);
  }
}
