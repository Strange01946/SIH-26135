package in.gov.sih.sih26135.entity.analytics;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;
import java.util.Objects;
import org.hibernate.annotations.Immutable;

@Entity
@Table(name = "vw_attrition_reason_summary")
@IdClass(AttritionReasonSummaryId.class)
@Immutable
public class AttritionReasonSummary {

  @Id
  @Column(name = "employment_exit_reason_id", nullable = false)
  private Long employmentExitReasonId;

  @Column(name = "exit_reason_code", length = 32, nullable = false)
  private String exitReasonCode;

  @Id
  @Column(name = "separation_nature_id", nullable = false)
  private Long separationNatureId;

  @Column(name = "separation_nature_code", length = 32, nullable = false)
  private String separationNatureCode;

  @Column(name = "is_voluntary_flag", nullable = false)
  private Boolean isVoluntaryFlag;

  @Column(name = "is_involuntary_flag", nullable = false)
  private Boolean isInvoluntaryFlag;

  @Column(name = "exit_count")
  private Long exitCount;

  @Column(name = "trainee_count")
  private Long traineeCount;

  public AttritionReasonSummary() {
  }

  public Long getEmploymentExitReasonId() {
    return employmentExitReasonId;
  }

  public void setEmploymentExitReasonId(Long employmentExitReasonId) {
    this.employmentExitReasonId = employmentExitReasonId;
  }

  public String getExitReasonCode() {
    return exitReasonCode;
  }

  public void setExitReasonCode(String exitReasonCode) {
    this.exitReasonCode = exitReasonCode;
  }

  public Long getSeparationNatureId() {
    return separationNatureId;
  }

  public void setSeparationNatureId(Long separationNatureId) {
    this.separationNatureId = separationNatureId;
  }

  public String getSeparationNatureCode() {
    return separationNatureCode;
  }

  public void setSeparationNatureCode(String separationNatureCode) {
    this.separationNatureCode = separationNatureCode;
  }

  public Boolean getIsVoluntaryFlag() {
    return isVoluntaryFlag;
  }

  public void setIsVoluntaryFlag(Boolean isVoluntaryFlag) {
    this.isVoluntaryFlag = isVoluntaryFlag;
  }

  public Boolean getIsInvoluntaryFlag() {
    return isInvoluntaryFlag;
  }

  public void setIsInvoluntaryFlag(Boolean isInvoluntaryFlag) {
    this.isInvoluntaryFlag = isInvoluntaryFlag;
  }

  public Long getExitCount() {
    return exitCount;
  }

  public void setExitCount(Long exitCount) {
    this.exitCount = exitCount;
  }

  public Long getTraineeCount() {
    return traineeCount;
  }

  public void setTraineeCount(Long traineeCount) {
    this.traineeCount = traineeCount;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof AttritionReasonSummary that)) {
      return false;
    }
    return Objects.equals(employmentExitReasonId, that.employmentExitReasonId)
        && Objects.equals(separationNatureId, that.separationNatureId);
  }

  @Override
  public int hashCode() {
    return Objects.hash(employmentExitReasonId, separationNatureId);
  }
}
