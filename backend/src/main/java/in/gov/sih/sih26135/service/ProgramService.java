package in.gov.sih.sih26135.service;

import in.gov.sih.sih26135.dto.request.AssignProgramTrainingProviderRequest;
import in.gov.sih.sih26135.dto.request.CreateProgramRequest;
import in.gov.sih.sih26135.dto.request.TerminateEmpanelmentRequest;
import in.gov.sih.sih26135.dto.request.UpdateProgramRequest;
import in.gov.sih.sih26135.dto.response.ProgramResponse;
import in.gov.sih.sih26135.dto.response.ProgramTrainingProviderResponse;
import java.util.List;

public interface ProgramService {

  ProgramResponse getById(Long id);

  ProgramResponse getByCode(String programCode);

  List<ProgramResponse> getAllPrograms();

  List<ProgramResponse> getProgramsBySchemeId(Long schemeId);

  List<ProgramResponse> getProgramsByDepartmentId(Long departmentId);

  ProgramResponse createProgram(CreateProgramRequest request);

  ProgramResponse updateProgram(Long id, UpdateProgramRequest request);

  void deleteProgram(Long id);

  ProgramTrainingProviderResponse assignTrainingProvider(AssignProgramTrainingProviderRequest request);

  ProgramTrainingProviderResponse terminateEmpanelment(Long assignmentId, TerminateEmpanelmentRequest request);

  List<ProgramTrainingProviderResponse> getTrainingProvidersForProgram(Long programId);

  List<ProgramTrainingProviderResponse> getProgramsForTrainingProvider(Long providerId);
}
