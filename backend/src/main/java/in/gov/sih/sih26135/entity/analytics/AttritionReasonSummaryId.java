package in.gov.sih.sih26135.entity.analytics;

import java.io.Serializable;
import java.util.Objects;

public class AttritionReasonSummaryId implements Serializable {

  private Long employmentExitReasonId;
  private Long separationNatureId;

  public AttritionReasonSummaryId() {
  }

  public AttritionReasonSummaryId(Long employmentExitReasonId, Long separationNatureId) {
    this.employmentExitReasonId = employmentExitReasonId;
    this.separationNatureId = separationNatureId;
  }

  public Long getEmploymentExitReasonId() {
    return employmentExitReasonId;
  }

  public void setEmploymentExitReasonId(Long employmentExitReasonId) {
    this.employmentExitReasonId = employmentExitReasonId;
  }

  public Long getSeparationNatureId() {
    return separationNatureId;
  }

  public void setSeparationNatureId(Long separationNatureId) {
    this.separationNatureId = separationNatureId;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof AttritionReasonSummaryId that)) {
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
