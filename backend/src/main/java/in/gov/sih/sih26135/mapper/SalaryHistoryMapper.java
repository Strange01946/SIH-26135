package in.gov.sih.sih26135.mapper;

import in.gov.sih.sih26135.dto.request.CreateSalaryHistoryRequest;
import in.gov.sih.sih26135.dto.response.SalaryHistoryResponse;
import in.gov.sih.sih26135.entity.EmploymentRecord;
import in.gov.sih.sih26135.entity.RefEmploymentInfoSource;
import in.gov.sih.sih26135.entity.RefRecordVerificationStatus;
import in.gov.sih.sih26135.entity.RefSalaryFrequency;
import in.gov.sih.sih26135.entity.SalaryHistory;
import in.gov.sih.sih26135.entity.Trainee;
import org.springframework.stereotype.Component;

@Component
public class SalaryHistoryMapper {

  public SalaryHistoryResponse toResponse(SalaryHistory entity) {
    if (entity == null) {
      return null;
    }

    Long employmentId = null;
    String employmentNumber = null;
    if (entity.getEmploymentRecord() != null) {
      employmentId = entity.getEmploymentRecord().getId();
      employmentNumber = entity.getEmploymentRecord().getEmploymentNumber();
    }

    Long traineeId = null;
    String traineeReg = null;
    String traineeFirst = null;
    String traineeLast = null;
    if (entity.getTrainee() != null) {
      traineeId = entity.getTrainee().getId();
      traineeReg = entity.getTrainee().getRegistrationNumber();
      traineeFirst = entity.getTrainee().getFirstName();
      traineeLast = entity.getTrainee().getLastName();
    }

    Long salaryFrequencyId = null;
    String salaryFrequencyCode = null;
    String salaryFrequencyName = null;
    if (entity.getSalaryFrequency() != null) {
      salaryFrequencyId = entity.getSalaryFrequency().getId();
      salaryFrequencyCode = entity.getSalaryFrequency().getFrequencyCode();
      salaryFrequencyName = entity.getSalaryFrequency().getFrequencyName();
    }

    Long infoSourceId = null;
    String infoSourceCode = null;
    String infoSourceName = null;
    if (entity.getEmploymentInfoSource() != null) {
      infoSourceId = entity.getEmploymentInfoSource().getId();
      infoSourceCode = entity.getEmploymentInfoSource().getSourceCode();
      infoSourceName = entity.getEmploymentInfoSource().getSourceName();
    }

    Long recordVerificationStatusId = null;
    String recordVerificationStatusCode = null;
    if (entity.getRecordVerificationStatus() != null) {
      recordVerificationStatusId = entity.getRecordVerificationStatus().getId();
      recordVerificationStatusCode = entity.getRecordVerificationStatus().getStatusCode();
    }

    return new SalaryHistoryResponse(
        entity.getId(),
        employmentId,
        employmentNumber,
        traineeId,
        traineeReg,
        traineeFirst,
        traineeLast,
        entity.getSalaryAmount(),
        salaryFrequencyId,
        salaryFrequencyCode,
        salaryFrequencyName,
        entity.getCurrencyCode(),
        entity.getEffectiveFrom(),
        entity.getEffectiveTo(),
        entity.getObservationMonthOffset(),
        infoSourceId,
        infoSourceCode,
        infoSourceName,
        recordVerificationStatusId,
        recordVerificationStatusCode,
        entity.getVerifiedAt(),
        entity.getVerifiedByUserId(),
        entity.getCreatedAt(),
        entity.getUpdatedAt()
    );
  }

  public SalaryHistory toEntity(
      CreateSalaryHistoryRequest request,
      EmploymentRecord employmentRecord,
      Trainee trainee,
      RefSalaryFrequency salaryFrequency,
      RefEmploymentInfoSource employmentInfoSource,
      RefRecordVerificationStatus recordVerificationStatus) {
    if (request == null) {
      return null;
    }

    SalaryHistory entity = new SalaryHistory();
    entity.setEmploymentRecord(employmentRecord);
    entity.setTrainee(trainee);
    entity.setSalaryAmount(request.getSalaryAmount());
    entity.setSalaryFrequency(salaryFrequency);
    entity.setCurrencyCode(request.getCurrencyCode() != null ? request.getCurrencyCode() : "INR");
    entity.setEffectiveFrom(request.getEffectiveFrom());
    entity.setEffectiveTo(request.getEffectiveTo());
    entity.setObservationMonthOffset(request.getObservationMonthOffset());
    entity.setEmploymentInfoSource(employmentInfoSource);
    entity.setRecordVerificationStatus(recordVerificationStatus);
    entity.setVerifiedAt(request.getVerifiedAt());
    entity.setVerifiedByUserId(request.getVerifiedByUserId());
    return entity;
  }
}
