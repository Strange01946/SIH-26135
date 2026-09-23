package in.gov.sih.sih26135.service.impl;

import in.gov.sih.sih26135.dto.response.DistrictOutcomeSummaryResponse;
import in.gov.sih.sih26135.entity.analytics.DistrictOutcomeSummary;
import in.gov.sih.sih26135.exception.BadRequestException;
import in.gov.sih.sih26135.exception.ResourceNotFoundException;
import in.gov.sih.sih26135.mapper.DistrictOutcomeSummaryMapper;
import in.gov.sih.sih26135.repository.analytics.DistrictOutcomeSummaryRepository;
import in.gov.sih.sih26135.service.DistrictOutcomeSummaryService;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class DistrictOutcomeSummaryServiceImpl implements DistrictOutcomeSummaryService {

  private final DistrictOutcomeSummaryRepository repository;
  private final DistrictOutcomeSummaryMapper mapper;

  public DistrictOutcomeSummaryServiceImpl(
      DistrictOutcomeSummaryRepository repository,
      DistrictOutcomeSummaryMapper mapper) {
    this.repository = repository;
    this.mapper = mapper;
  }

  @Override
  public List<DistrictOutcomeSummaryResponse> getAllDistrictOutcomes() {
    return repository.findAll().stream()
        .map(mapper::toResponse)
        .toList();
  }

  @Override
  public List<DistrictOutcomeSummaryResponse> getAllDistrictOutcomesOrderByEnrollmentCountDesc() {
    return repository.findAllByOrderByEnrollmentCountDesc().stream()
        .map(mapper::toResponse)
        .toList();
  }

  @Override
  public DistrictOutcomeSummaryResponse getDistrictOutcomeByDistrictId(Long districtId) {
    if (districtId == null) {
      throw new BadRequestException("District ID is required");
    }
    DistrictOutcomeSummary entity = repository.findById(districtId)
        .orElseThrow(() -> new ResourceNotFoundException("DistrictOutcomeSummary", "districtId"));
    return mapper.toResponse(entity);
  }
}
