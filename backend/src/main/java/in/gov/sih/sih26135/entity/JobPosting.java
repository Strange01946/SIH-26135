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
@Table(name = "job_postings")
public class JobPosting {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "job_posting_id", nullable = false, updatable = false)
  private Long id;

  @Column(name = "posting_code", length = 32, nullable = false, unique = true)
  private String postingCode;

  @Column(name = "posting_title", length = 200, nullable = false)
  private String postingTitle;

  @Column(name = "description", columnDefinition = "TEXT")
  private String description;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "employer_id")
  private Employer employer;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "employer_branch_id")
  private EmployerBranch employerBranch;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "job_role_id", nullable = false)
  private JobRole jobRole;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "engagement_type_id", nullable = false)
  private RefEngagementType engagementType;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "qualification_level_id")
  private RefQualificationLevel qualificationLevel;

  @Column(name = "vacancies", nullable = false)
  private Integer vacancies = 1;

  @Column(name = "min_salary", precision = 12, scale = 2)
  private BigDecimal minSalary;

  @Column(name = "max_salary", precision = 12, scale = 2)
  private BigDecimal maxSalary;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "salary_frequency_id")
  private RefSalaryFrequency salaryFrequency;

  @JdbcTypeCode(SqlTypes.CHAR)
  @Column(name = "currency_code", length = 3, nullable = false, columnDefinition = "CHAR(3)")
  private String currencyCode = "INR";

  @Column(name = "state_id", nullable = false)
  private Long stateId;

  @Column(name = "district_id", nullable = false)
  private Long districtId;

  @Column(name = "location_id")
  private Long locationId;

  @Column(name = "posted_date", nullable = false)
  private LocalDate postedDate;

  @Column(name = "closing_date")
  private LocalDate closingDate;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "job_posting_status_id", nullable = false)
  private RefJobPostingStatus jobPostingStatus;

  @Column(name = "created_by_user_id")
  private Long createdByUserId;

  @Column(name = "created_at", nullable = false, updatable = false)
  private LocalDateTime createdAt;

  @Column(name = "updated_at", nullable = false)
  private LocalDateTime updatedAt;

  @Column(name = "deleted_at")
  private LocalDateTime deletedAt;

  public JobPosting() {
  }

  public JobPosting(String postingCode, String postingTitle, JobRole jobRole,
      RefEngagementType engagementType, Long stateId, Long districtId,
      LocalDate postedDate, RefJobPostingStatus jobPostingStatus) {
    this.postingCode = postingCode;
    this.postingTitle = postingTitle;
    this.jobRole = jobRole;
    this.engagementType = engagementType;
    this.stateId = stateId;
    this.districtId = districtId;
    this.postedDate = postedDate;
    this.jobPostingStatus = jobPostingStatus;
  }

  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
  }

  public String getPostingCode() {
    return postingCode;
  }

  public void setPostingCode(String postingCode) {
    this.postingCode = postingCode;
  }

  public String getPostingTitle() {
    return postingTitle;
  }

  public void setPostingTitle(String postingTitle) {
    this.postingTitle = postingTitle;
  }

  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  public Employer getEmployer() {
    return employer;
  }

  public void setEmployer(Employer employer) {
    this.employer = employer;
  }

  public EmployerBranch getEmployerBranch() {
    return employerBranch;
  }

  public void setEmployerBranch(EmployerBranch employerBranch) {
    this.employerBranch = employerBranch;
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

  public RefQualificationLevel getQualificationLevel() {
    return qualificationLevel;
  }

  public void setQualificationLevel(RefQualificationLevel qualificationLevel) {
    this.qualificationLevel = qualificationLevel;
  }

  public Integer getVacancies() {
    return vacancies;
  }

  public void setVacancies(Integer vacancies) {
    this.vacancies = vacancies;
  }

  public BigDecimal getMinSalary() {
    return minSalary;
  }

  public void setMinSalary(BigDecimal minSalary) {
    this.minSalary = minSalary;
  }

  public BigDecimal getMaxSalary() {
    return maxSalary;
  }

  public void setMaxSalary(BigDecimal maxSalary) {
    this.maxSalary = maxSalary;
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

  public Long getStateId() {
    return stateId;
  }

  public void setStateId(Long stateId) {
    this.stateId = stateId;
  }

  public Long getDistrictId() {
    return districtId;
  }

  public void setDistrictId(Long districtId) {
    this.districtId = districtId;
  }

  public Long getLocationId() {
    return locationId;
  }

  public void setLocationId(Long locationId) {
    this.locationId = locationId;
  }

  public LocalDate getPostedDate() {
    return postedDate;
  }

  public void setPostedDate(LocalDate postedDate) {
    this.postedDate = postedDate;
  }

  public LocalDate getClosingDate() {
    return closingDate;
  }

  public void setClosingDate(LocalDate closingDate) {
    this.closingDate = closingDate;
  }

  public RefJobPostingStatus getJobPostingStatus() {
    return jobPostingStatus;
  }

  public void setJobPostingStatus(RefJobPostingStatus jobPostingStatus) {
    this.jobPostingStatus = jobPostingStatus;
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

  public LocalDateTime getDeletedAt() {
    return deletedAt;
  }

  public void setDeletedAt(LocalDateTime deletedAt) {
    this.deletedAt = deletedAt;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof JobPosting that)) {
      return false;
    }
    return Objects.equals(postingCode, that.postingCode);
  }

  @Override
  public int hashCode() {
    return Objects.hash(postingCode);
  }

  @Override
  public String toString() {
    return "JobPosting{" +
        "id=" + id +
        ", postingCode='" + postingCode + '\'' +
        ", postingTitle='" + postingTitle + '\'' +
        ", vacancies=" + vacancies +
        ", postedDate=" + postedDate +
        '}';
  }
}
