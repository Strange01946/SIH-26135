package in.gov.sih.sih26135.mapper;

import in.gov.sih.sih26135.dto.response.SalaryFrequencyResponse;
import in.gov.sih.sih26135.entity.RefSalaryFrequency;
import org.springframework.stereotype.Component;

@Component
public class SalaryFrequencyMapper {

  public SalaryFrequencyResponse toResponse(RefSalaryFrequency entity) {
    if (entity == null) {
      return null;
    }
    return new SalaryFrequencyResponse(
        entity.getId(),
        entity.getFrequencyCode(),
        entity.getFrequencyName(),
        entity.getSortOrder(),
        entity.getCreatedAt(),
        entity.getUpdatedAt()
    );
  }
}
