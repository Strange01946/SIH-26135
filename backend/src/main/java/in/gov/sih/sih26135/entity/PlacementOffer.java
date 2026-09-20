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
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Objects;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "placement_offers")
public class PlacementOffer {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "placement_offer_id", nullable = false, updatable = false)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "placement_id", nullable = false)
  private PlacementRecord placementRecord;

  @Column(name = "offer_number", length = 32, nullable = false, unique = true)
  private String offerNumber;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "employer_id")
  private Employer employer;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "job_role_id")
  private JobRole jobRole;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "engagement_type_id", nullable = false)
  private RefEngagementType engagementType;

  @Column(name = "offered_salary", precision = 12, scale = 2)
  private BigDecimal offeredSalary;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "salary_frequency_id")
  private RefSalaryFrequency salaryFrequency;

  @JdbcTypeCode(SqlTypes.CHAR)
  @Column(name = "currency_code", length = 3, nullable = false, columnDefinition = "CHAR(3)")
  private String currencyCode = "INR";

  @Column(name = "offer_date", nullable = false)
  private LocalDate offerDate;

  @Column(name = "offer_valid_until")
  private LocalDate offerValidUntil;

  @Column(name = "proposed_joining_date")
  private LocalDate proposedJoiningDate;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "offer_status_id", nullable = false)
  private RefOfferStatus offerStatus;

  @Column(name = "created_by_user_id")
  private Long createdByUserId;

  @Column(name = "created_at", nullable = false, updatable = false)
  private LocalDateTime createdAt;

  @Column(name = "updated_at", nullable = false)
  private LocalDateTime updatedAt;

  public PlacementOffer() {
  }

  public PlacementOffer(PlacementRecord placementRecord, String offerNumber,
      RefEngagementType engagementType, LocalDate offerDate, RefOfferStatus offerStatus) {
    this.placementRecord = placementRecord;
    this.offerNumber = offerNumber;
    this.engagementType = engagementType;
    this.offerDate = offerDate;
    this.offerStatus = offerStatus;
  }

  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
  }

  public PlacementRecord getPlacementRecord() {
    return placementRecord;
  }

  public void setPlacementRecord(PlacementRecord placementRecord) {
    this.placementRecord = placementRecord;
  }

  public String getOfferNumber() {
    return offerNumber;
  }

  public void setOfferNumber(String offerNumber) {
    this.offerNumber = offerNumber;
  }

  public Employer getEmployer() {
    return employer;
  }

  public void setEmployer(Employer employer) {
    this.employer = employer;
  }

  public JobRole getJobRole() {
    return jobRole;
  }

  public void setJobRole(JobRole jobRole) {
    this.jobRole = jobRole;
  }

  public RefEngagementType getEngagementType() {
    return engagementType;
  }

  public void setEngagementType(RefEngagementType engagementType) {
    this.engagementType = engagementType;
  }

  public BigDecimal getOfferedSalary() {
    return offeredSalary;
  }

  public void setOfferedSalary(BigDecimal offeredSalary) {
    this.offeredSalary = offeredSalary;
  }

  public RefSalaryFrequency getSalaryFrequency() {
    return salaryFrequency;
  }

  public void setSalaryFrequency(RefSalaryFrequency salaryFrequency) {
    this.salaryFrequency = salaryFrequency;
  }

  public String getCurrencyCode() {
    return currencyCode;
  }

  public void setCurrencyCode(String currencyCode) {
    this.currencyCode = currencyCode;
  }

  public LocalDate getOfferDate() {
    return offerDate;
  }

  public void setOfferDate(LocalDate offerDate) {
    this.offerDate = offerDate;
  }

  public LocalDate getOfferValidUntil() {
    return offerValidUntil;
  }

  public void setOfferValidUntil(LocalDate offerValidUntil) {
    this.offerValidUntil = offerValidUntil;
  }

  public LocalDate getProposedJoiningDate() {
    return proposedJoiningDate;
  }

  public void setProposedJoiningDate(LocalDate proposedJoiningDate) {
    this.proposedJoiningDate = proposedJoiningDate;
  }

  public RefOfferStatus getOfferStatus() {
    return offerStatus;
  }

  public void setOfferStatus(RefOfferStatus offerStatus) {
    this.offerStatus = offerStatus;
  }

  public Long getCreatedByUserId() {
    return createdByUserId;
  }

  public void setCreatedByUserId(Long createdByUserId) {
    this.createdByUserId = createdByUserId;
  }

  public LocalDateTime getCreatedAt() {
    return createdAt;
  }

  public void setCreatedAt(LocalDateTime createdAt) {
    this.createdAt = createdAt;
  }

  public LocalDateTime getUpdatedAt() {
    return updatedAt;
  }

  public void setUpdatedAt(LocalDateTime updatedAt) {
    this.updatedAt = updatedAt;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof PlacementOffer that)) {
      return false;
    }
    return Objects.equals(offerNumber, that.offerNumber);
  }

  @Override
  public int hashCode() {
    return Objects.hash(offerNumber);
  }

  @Override
  public String toString() {
    return "PlacementOffer{" +
        "id=" + id +
        ", offerNumber='" + offerNumber + '\'' +
        ", currencyCode='" + currencyCode + '\'' +
        ", offerDate=" + offerDate +
        '}';
  }
}
