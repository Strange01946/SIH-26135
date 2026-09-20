package in.gov.sih.sih26135.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.util.Objects;

@Entity
@Table(name = "data_quality_issue_events")
public class DataQualityIssueEvent {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "data_quality_issue_event_id", nullable = false, updatable = false)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "data_quality_issue_id", nullable = false)
  private DataQualityIssue dataQualityIssue;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "data_quality_issue_status_id", nullable = false)
  private RefDataQualityIssueStatus dataQualityIssueStatus;

  @Column(name = "changed_by_user_id")
  private Long changedByUserId;

  @Column(name = "changed_at", nullable = false)
  private LocalDateTime changedAt;

  @Column(name = "remarks", length = 500)
  private String remarks;

  @Column(name = "created_at", nullable = false, updatable = false)
  private LocalDateTime createdAt;

  public DataQualityIssueEvent() {
  }

  public DataQualityIssueEvent(DataQualityIssue dataQualityIssue,
      RefDataQualityIssueStatus dataQualityIssueStatus, LocalDateTime changedAt) {
    this.dataQualityIssue = dataQualityIssue;
    this.dataQualityIssueStatus = dataQualityIssueStatus;
    this.changedAt = changedAt;
  }

  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
  }

  public DataQualityIssue getDataQualityIssue() {
    return dataQualityIssue;
  }

  public void setDataQualityIssue(DataQualityIssue dataQualityIssue) {
    this.dataQualityIssue = dataQualityIssue;
  }

  public RefDataQualityIssueStatus getDataQualityIssueStatus() {
    return dataQualityIssueStatus;
  }

  public void setDataQualityIssueStatus(RefDataQualityIssueStatus dataQualityIssueStatus) {
    this.dataQualityIssueStatus = dataQualityIssueStatus;
  }

  public Long getChangedByUserId() {
    return changedByUserId;
  }

  public void setChangedByUserId(Long changedByUserId) {
    this.changedByUserId = changedByUserId;
  }

  public LocalDateTime getChangedAt() {
    return changedAt;
  }

  public void setChangedAt(LocalDateTime changedAt) {
    this.changedAt = changedAt;
  }

  public String getRemarks() {
    return remarks;
  }

  public void setRemarks(String remarks) {
    this.remarks = remarks;
  }

  public LocalDateTime getCreatedAt() {
    return createdAt;
  }

  public void setCreatedAt(LocalDateTime createdAt) {
    this.createdAt = createdAt;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof DataQualityIssueEvent that)) {
      return false;
    }
    return Objects.equals(id, that.id);
  }

  @Override
  public int hashCode() {
    return Objects.hash(id);
  }

  @Override
  public String toString() {
    return "DataQualityIssueEvent{" +
        "id=" + id +
        ", changedByUserId=" + changedByUserId +
        ", changedAt=" + changedAt +
        '}';
  }
}
