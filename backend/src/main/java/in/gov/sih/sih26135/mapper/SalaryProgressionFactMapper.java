package in.gov.sih.sih26135.mapper;

import in.gov.sih.sih26135.dto.response.SalaryProgressionFactResponse;
import in.gov.sih.sih26135.entity.analytics.SalaryProgressionFact;
import org.springframework.stereotype.Component;

@Component
public class SalaryProgressionFactMapper {

  public SalaryProgressionFactResponse toResponse(SalaryProgressionFact entity) {
    if (entity == null) {
      return null;
    }
    return new SalaryProgressionFactResponse(
        entity.getEmploymentId(),
        entity.getTraineeId(),
        entity.getEnrollmentId(),
        entity.getCourseId(),
        entity.getProviderId(),
        entity.getEngagementTypeId(),
        entity.getCurrencyCode(),
        entity.getStartingSalary(),
        entity.getFirstRecordedSalary(),
        entity.getFirstSalaryEffectiveFrom(),
        entity.getFirstObservationMonthOffset(),
        entity.getLatestRecordedSalary(),
        entity.getLatestSalaryEffectiveFrom(),
        entity.getLatestSalaryEffectiveTo(),
        entity.getLatestObservationMonthOffset(),
        entity.getLatestSalaryFrequencyCode(),
        entity.getSalaryChangeAmount(),
        entity.getSalaryChangePct()
    );
  }
}
