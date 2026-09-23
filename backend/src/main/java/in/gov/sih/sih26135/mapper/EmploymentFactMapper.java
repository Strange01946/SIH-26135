package in.gov.sih.sih26135.mapper;

import in.gov.sih.sih26135.dto.response.EmploymentFactResponse;
import in.gov.sih.sih26135.entity.analytics.EmploymentFact;
import org.springframework.stereotype.Component;

@Component
public class EmploymentFactMapper {

  public EmploymentFactResponse toResponse(EmploymentFact entity) {
    if (entity == null) {
      return null;
    }
    return new EmploymentFactResponse(
        entity.getEmploymentId(),
        entity.getEmploymentNumber(),
        entity.getTraineeId(),
        entity.getEnrollmentId(),
        entity.getPlacementId(),
        entity.getEmployerId(),
        entity.getJobRoleId(),
        entity.getEngagementTypeId(),
        entity.getEngagementTypeCode(),
        entity.getIsWageEmployment(),
        entity.getIsSelfEmployment(),
        entity.getIsApprenticeship(),
        entity.getIsInternship(),
        entity.getEmploymentSpellStatusId(),
        entity.getEmploymentSpellStatusCode(),
        entity.getIsActiveFlag(),
        entity.getStartDate(),
        entity.getEndDate(),
        entity.getIsCurrent(),
        entity.getDurationDays(),
        entity.getStartingSalary(),
        entity.getSalaryFrequencyId(),
        entity.getSalaryFrequencyCode(),
        entity.getCurrencyCode(),
        entity.getWorkStateId(),
        entity.getWorkDistrictId(),
        entity.getEmploymentInfoSourceId(),
        entity.getEmploymentInfoSourceCode(),
        entity.getIsSelfReportedFlag(),
        entity.getRecordVerificationStatusId(),
        entity.getRecordVerificationStatusCode(),
        entity.getIsVerifiedFlag(),
        entity.getEmploymentExitReasonId(),
        entity.getTraineeStateId(),
        entity.getTraineeDistrictId()
    );
  }
}
