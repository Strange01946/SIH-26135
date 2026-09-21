package in.gov.sih.sih26135.mapper;

import in.gov.sih.sih26135.dto.request.AssignProgramTrainingProviderRequest;
import in.gov.sih.sih26135.dto.response.ProgramTrainingProviderResponse;
import in.gov.sih.sih26135.entity.Program;
import in.gov.sih.sih26135.entity.ProgramTrainingProvider;
import in.gov.sih.sih26135.entity.TrainingProvider;
import org.springframework.stereotype.Component;

@Component
public class ProgramTrainingProviderMapper {

  public ProgramTrainingProviderResponse toResponse(ProgramTrainingProvider entity) {
    if (entity == null) {
      return null;
    }

    Long programId = null;
    String programCode = null;
    String programName = null;
    if (entity.getProgram() != null) {
      programId = entity.getProgram().getId();
      programCode = entity.getProgram().getProgramCode();
      programName = entity.getProgram().getProgramName();
    }

    Long providerId = null;
    String providerCode = null;
    String providerName = null;
    if (entity.getTrainingProvider() != null) {
      providerId = entity.getTrainingProvider().getId();
      providerCode = entity.getTrainingProvider().getProviderCode();
      providerName = entity.getTrainingProvider().getProviderName();
    }

    return new ProgramTrainingProviderResponse(
        entity.getId(),
        programId,
        programCode,
        programName,
        providerId,
        providerCode,
        providerName,
        entity.getEmpanelledFrom(),
        entity.getEmpanelledTo(),
        entity.getLifecycleStatusId(),
        entity.getCreatedAt(),
        entity.getUpdatedAt()
    );
  }

  public ProgramTrainingProvider toEntity(
      AssignProgramTrainingProviderRequest request,
      Program program,
      TrainingProvider trainingProvider) {
    if (request == null) {
      return null;
    }

    ProgramTrainingProvider ptp = new ProgramTrainingProvider();
    ptp.setProgram(program);
    ptp.setTrainingProvider(trainingProvider);
    ptp.setEmpanelledFrom(request.getEmpanelledFrom());
    ptp.setEmpanelledTo(request.getEmpanelledTo());
    ptp.setLifecycleStatusId(request.getLifecycleStatusId());
    return ptp;
  }
}
