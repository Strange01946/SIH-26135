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
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Objects;

@Entity
@Table(name = "certifications")
public class Certification {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "certification_id", nullable = false, updatable = false)
  private Long id;

  @Column(name = "certificate_number", length = 64, nullable = false, unique = true)
  private String certificateNumber;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "trainee_id", nullable = false)
  private Trainee trainee;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "enrollment_id", nullable = false)
  private TrainingEnrollment trainingEnrollment;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "course_id", nullable = false)
  private Course course;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "program_id", nullable = false)
  private Program program;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "assessment_result_id")
  private AssessmentResult assessmentResult;

  @Column(name = "issuing_body", length = 200)
  private String issuingBody;

  @Column(name = "issue_date", nullable = false)
  private LocalDate issueDate;

  @Column(name = "expiry_date")
  private LocalDate expiryDate;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "certificate_status_id", nullable = false)
  private RefCertificateStatus certificateStatus;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "certificate_verification_status_id", nullable = false)
  private RefCertificateVerificationStatus certificateVerificationStatus;

  @Column(name = "verified_at")
  private LocalDateTime verifiedAt;

  @Column(name = "verified_by_user_id")
  private Long verifiedByUserId;

  @Column(name = "created_at", nullable = false, updatable = false)
  private LocalDateTime createdAt;

  @Column(name = "updated_at", nullable = false)
  private LocalDateTime updatedAt;

  @Column(name = "deleted_at")
  private LocalDateTime deletedAt;

  public Certification() {
  }

  public Certification(String certificateNumber, Trainee trainee, TrainingEnrollment trainingEnrollment, Course course,
      Program program, LocalDate issueDate, RefCertificateStatus certificateStatus,
      RefCertificateVerificationStatus certificateVerificationStatus) {
    this.certificateNumber = certificateNumber;
    this.trainee = trainee;
    this.trainingEnrollment = trainingEnrollment;
    this.course = course;
    this.program = program;
    this.issueDate = issueDate;
    this.certificateStatus = certificateStatus;
    this.certificateVerificationStatus = certificateVerificationStatus;
  }

  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
  }

  public String getCertificateNumber() {
    return certificateNumber;
  }

  public void setCertificateNumber(String certificateNumber) {
    this.certificateNumber = certificateNumber;
  }

  public Trainee getTrainee() {
    return trainee;
  }

  public void setTrainee(Trainee trainee) {
    this.trainee = trainee;
  }

  public TrainingEnrollment getTrainingEnrollment() {
    return trainingEnrollment;
  }

  public void setTrainingEnrollment(TrainingEnrollment trainingEnrollment) {
    this.trainingEnrollment = trainingEnrollment;
  }

  public Course getCourse() {
    return course;
  }

  public void setCourse(Course course) {
    this.course = course;
  }

  public Program getProgram() {
    return program;
  }

  public void setProgram(Program program) {
    this.program = program;
  }

  public AssessmentResult getAssessmentResult() {
    return assessmentResult;
  }

  public void setAssessmentResult(AssessmentResult assessmentResult) {
    this.assessmentResult = assessmentResult;
  }

  public String getIssuingBody() {
    return issuingBody;
  }

  public void setIssuingBody(String issuingBody) {
    this.issuingBody = issuingBody;
  }

  public LocalDate getIssueDate() {
    return issueDate;
  }

  public void setIssueDate(LocalDate issueDate) {
    this.issueDate = issueDate;
  }

  public LocalDate getExpiryDate() {
    return expiryDate;
  }

  public void setExpiryDate(LocalDate expiryDate) {
    this.expiryDate = expiryDate;
  }

  public RefCertificateStatus getCertificateStatus() {
    return certificateStatus;
  }

  public void setCertificateStatus(RefCertificateStatus certificateStatus) {
    this.certificateStatus = certificateStatus;
  }

  public RefCertificateVerificationStatus getCertificateVerificationStatus() {
    return certificateVerificationStatus;
  }

  public void setCertificateVerificationStatus(RefCertificateVerificationStatus certificateVerificationStatus) {
    this.certificateVerificationStatus = certificateVerificationStatus;
  }

  public LocalDateTime getVerifiedAt() {
    return verifiedAt;
  }

  public void setVerifiedAt(LocalDateTime verifiedAt) {
    this.verifiedAt = verifiedAt;
  }

  public Long getVerifiedByUserId() {
    return verifiedByUserId;
  }

  public void setVerifiedByUserId(Long verifiedByUserId) {
    this.verifiedByUserId = verifiedByUserId;
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
    if (!(o instanceof Certification other)) {
      return false;
    }
    return id != null && id.equals(other.id);
  }

  @Override
  public int hashCode() {
    return getClass().hashCode();
  }

  @Override
  public String toString() {
    return "Certification{" +
        "id=" + id +
        ", certificateNumber='" + certificateNumber + '\'' +
        ", issueDate=" + issueDate +
        '}';
  }
}
