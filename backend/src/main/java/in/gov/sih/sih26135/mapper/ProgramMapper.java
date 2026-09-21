package in.gov.sih.sih26135.mapper;

import in.gov.sih.sih26135.dto.request.CreateProgramRequest;
import in.gov.sih.sih26135.dto.response.ProgramResponse;
import in.gov.sih.sih26135.entity.Program;
import in.gov.sih.sih26135.entity.Scheme;
import org.springframework.stereotype.Component;

@Component
public class ProgramMapper {

  public ProgramResponse toResponse(Program entity) {
    if (entity == null) {
      return null;
    }

    Long schemeId = null;
    String schemeCode = null;
    String schemeName = null;
    if (entity.getScheme() != null) {
      schemeId = entity.getScheme().getId();
      schemeCode = entity.getScheme().getSchemeCode();
      schemeName = entity.getScheme().getSchemeName();
    }

    return new ProgramResponse(
        entity.getId(),
        entity.getProgramCode(),
        entity.getProgramName(),
        entity.getDescription(),
        entity.getDepartmentId(),
        schemeId,
        schemeCode,
        schemeName,
        entity.getStartDate(),
        entity.getEndDate(),
        entity.getBudget(),
        entity.getCurrencyCode(),
        entity.getTargetBeneficiaries(),
        entity.getLifecycleStatusId(),
        entity.getCreatedAt(),
        entity.getUpdatedAt(),
        entity.getDeletedAt()
    );
  }

  public Program toEntity(CreateProgramRequest request, Scheme scheme) {
    if (request == null) {
      return null;
    }

    Program program = new Program();
    program.setProgramCode(request.getProgramCode());
    program.setProgramName(request.getProgramName());
    program.setDescription(request.getDescription());
    program.setDepartmentId(request.getDepartmentId());
    program.setScheme(scheme);
    program.setStartDate(request.getStartDate());
    program.setEndDate(request.getEndDate());
    program.setBudget(request.getBudget());
    if (request.getCurrencyCode() != null && !request.getCurrencyCode().isBlank()) {
      program.setCurrencyCode(request.getCurrencyCode());
    }
    program.setTargetBeneficiaries(request.getTargetBeneficiaries());
    program.setLifecycleStatusId(request.getLifecycleStatusId());
    return program;
  }
}
