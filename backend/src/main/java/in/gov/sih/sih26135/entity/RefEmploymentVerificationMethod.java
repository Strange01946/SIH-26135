package in.gov.sih.sih26135.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.util.Objects;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "ref_employment_verification_method")
public class RefEmploymentVerificationMethod {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "employment_verification_method_id", nullable = false, updatable = false)
  private Long id;

  @Column(name = "method_code", length = 32, nullable = false, unique = true)
  private String methodCode;

  @Column(name = "method_name", length = 150, nullable = false)
  private String methodName;

  @Column(name = "is_employer_side", nullable = false)
  private Boolean isEmployerSide = false;

  @Column(name = "is_trainee_self_reported", nullable = false)
  private Boolean isTraineeSelfReported = false;

  @Column(name = "is_official_flag", nullable = false)
  private Boolean isOfficialFlag = false;

  @JdbcTypeCode(SqlTypes.SMALLINT)
  @Column(name = "sort_order", nullable = false, columnDefinition = "SMALLINT UNSIGNED")
  private Integer sortOrder = 0;

  @Column(name = "created_at", nullable = false, updatable = false)
  private LocalDateTime createdAt;

  @Column(name = "updated_at", nullable = false)
  private LocalDateTime updatedAt;

  public RefEmploymentVerificationMethod() {
  }

  public RefEmploymentVerificationMethod(String methodCode, String methodName, Boolean isEmployerSide,
      Boolean isTraineeSelfReported, Boolean isOfficialFlag, Integer sortOrder) {
    this.methodCode = methodCode;
    this.methodName = methodName;
    this.isEmployerSide = isEmployerSide != null ? isEmployerSide : false;
    this.isTraineeSelfReported = isTraineeSelfReported != null ? isTraineeSelfReported : false;
    this.isOfficialFlag = isOfficialFlag != null ? isOfficialFlag : false;
    this.sortOrder = sortOrder != null ? sortOrder : 0;
  }

  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
  }

  public String getMethodCode() {
    return methodCode;
  }

  public void setMethodCode(String methodCode) {
    this.methodCode = methodCode;
  }

  public String getMethodName() {
    return methodName;
  }

  public void setMethodName(String methodName) {
    this.methodName = methodName;
  }

  public Boolean getIsEmployerSide() {
    return isEmployerSide;
  }

  public void setIsEmployerSide(Boolean employerSide) {
    isEmployerSide = employerSide != null ? employerSide : false;
  }

  public Boolean getIsTraineeSelfReported() {
    return isTraineeSelfReported;
  }

  public void setIsTraineeSelfReported(Boolean traineeSelfReported) {
    isTraineeSelfReported = traineeSelfReported != null ? traineeSelfReported : false;
  }

  public Boolean getIsOfficialFlag() {
    return isOfficialFlag;
  }

  public void setIsOfficialFlag(Boolean officialFlag) {
    isOfficialFlag = officialFlag != null ? officialFlag : false;
  }

  public Integer getSortOrder() {
    return sortOrder;
  }

  public void setSortOrder(Integer sortOrder) {
    this.sortOrder = sortOrder != null ? sortOrder : 0;
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
    if (!(o instanceof RefEmploymentVerificationMethod that)) {
      return false;
    }
    return Objects.equals(methodCode, that.methodCode);
  }

  @Override
  public int hashCode() {
    return Objects.hash(methodCode);
  }

  @Override
  public String toString() {
    return "RefEmploymentVerificationMethod{" +
        "id=" + id +
        ", methodCode='" + methodCode + '\'' +
        ", methodName='" + methodName + '\'' +
        ", sortOrder=" + sortOrder +
        '}';
  }
}
