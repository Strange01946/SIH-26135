package in.gov.sih.sih26135.service;

import in.gov.sih.sih26135.dto.response.ProgramOutcomeSummaryResponse;
import java.util.List;

public interface ProgramOutcomeSummaryService {

  List<ProgramOutcomeSummaryResponse> getAllProgramOutcomes();

  List<ProgramOutcomeSummaryResponse> getAllProgramOutcomesOrderByEnrollmentCountDesc();

  ProgramOutcomeSummaryResponse getProgramOutcomeByProgramId(Long programId);

  List<ProgramOutcomeSummaryResponse> getProgramOutcomesBySchemeId(Long schemeId);
}
