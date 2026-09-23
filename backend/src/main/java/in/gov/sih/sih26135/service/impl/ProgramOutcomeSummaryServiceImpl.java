package in.gov.sih.sih26135.service.impl;

import in.gov.sih.sih26135.dto.response.ProgramOutcomeSummaryResponse;
import in.gov.sih.sih26135.entity.analytics.ProgramOutcomeSummary;
import in.gov.sih.sih26135.exception.BadRequestException;
import in.gov.sih.sih26135.exception.ResourceNotFoundException;
import in.gov.sih.sih26135.mapper.ProgramOutcomeSummaryMapper;
import in.gov.sih.sih26135.repository.analytics.ProgramOutcomeSummaryRepository;
import in.gov.sih.sih26135.service.ProgramOutcomeSummaryService;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class ProgramOutcomeSummaryServiceImpl implements ProgramOutcomeSummaryService {

  private final ProgramOutcomeSummaryRepository repository;
  private final ProgramOutcomeSummaryMapper mapper;

  public ProgramOutcomeSummaryServiceImpl(
      ProgramOutcomeSummaryRepository repository,
      ProgramOutcomeSummaryMapper mapper) {
    this.repository = repository;
    this.mapper = mapper;
  }

  @Override
  public List<ProgramOutcomeSummaryResponse> getAllProgramOutcomes() {
    return repository.findAll().stream()
        .map(mapper::toResponse)
        .toList();
  }

  @Override
  public List<ProgramOutcomeSummaryResponse> getAllProgramOutcomesOrderByEnrollmentCountDesc() {
    return repository.findAllByOrderByEnrollmentCountDesc().stream()
        .map(mapper::toResponse)
        .toList();
  }

  @Override
  public ProgramOutcomeSummaryResponse getProgramOutcomeByProgramId(Long programId) {
    if (programId == null) {
      throw new BadRequestException("Program ID is required");
    }
    ProgramOutcomeSummary entity = repository.findById(programId)
        .orElseThrow(() -> new ResourceNotFoundException("ProgramOutcomeSummary", "programId"));
    return mapper.toResponse(entity);
  }

  @Override
  public List<ProgramOutcomeSummaryResponse> getProgramOutcomesBySchemeId(Long schemeId) {
    if (schemeId == null) {
      throw new BadRequestException("Scheme ID is required");
    }
    return repository.findBySchemeId(schemeId).stream()
        .map(mapper::toResponse)
        .toList();
  }
}
