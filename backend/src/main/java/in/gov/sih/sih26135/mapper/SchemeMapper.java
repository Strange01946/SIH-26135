package in.gov.sih.sih26135.mapper;

import in.gov.sih.sih26135.dto.request.CreateSchemeRequest;
import in.gov.sih.sih26135.dto.response.SchemeResponse;
import in.gov.sih.sih26135.entity.Scheme;
import org.springframework.stereotype.Component;

@Component
public class SchemeMapper {

  public SchemeResponse toResponse(Scheme entity) {
    if (entity == null) {
      return null;
    }

    return new SchemeResponse(
        entity.getId(),
        entity.getSchemeCode(),
        entity.getSchemeName(),
        entity.getDescription(),
        entity.getDepartmentId(),
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

  public Scheme toEntity(CreateSchemeRequest request) {
    if (request == null) {
      return null;
    }

    Scheme scheme = new Scheme();
    scheme.setSchemeCode(request.getSchemeCode());
    scheme.setSchemeName(request.getSchemeName());
    scheme.setDescription(request.getDescription());
    scheme.setDepartmentId(request.getDepartmentId());
    scheme.setStartDate(request.getStartDate());
    scheme.setEndDate(request.getEndDate());
    scheme.setBudget(request.getBudget());
    if (request.getCurrencyCode() != null && !request.getCurrencyCode().isBlank()) {
      scheme.setCurrencyCode(request.getCurrencyCode());
    }
    scheme.setTargetBeneficiaries(request.getTargetBeneficiaries());
    scheme.setLifecycleStatusId(request.getLifecycleStatusId());
    return scheme;
  }
}
