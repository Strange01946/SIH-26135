package in.gov.sih.sih26135.repository;

import in.gov.sih.sih26135.entity.OnboardingApplication;
import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface OnboardingApplicationRepository extends JpaRepository<OnboardingApplication, Long> {

  // Use the organisation's location. A login account can have a different or older location.
  String IN_JURISDICTION = """
      SELECT a.* FROM onboarding_applications a
      LEFT JOIN training_providers p ON a.applicant_type = 'TRAINING_PROVIDER'
        AND p.provider_id = a.entity_id AND p.deleted_at IS NULL
      LEFT JOIN employers e ON a.applicant_type = 'EMPLOYER'
        AND e.employer_id = a.entity_id AND e.deleted_at IS NULL
      WHERE COALESCE(p.state_id, e.state_id) = :stateId
        AND (:districtId IS NULL OR COALESCE(p.district_id, e.district_id) = :districtId)
      """;

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("SELECT a FROM OnboardingApplication a WHERE a.id = :id")
  Optional<OnboardingApplication> findByIdForUpdate(@Param("id") Long id);

  Optional<OnboardingApplication> findByApplicationNumber(String applicationNumber);

  Optional<OnboardingApplication> findByUserId(Long userId);

  List<OnboardingApplication> findByStatusOrderByCreatedAtDesc(String status);

  List<OnboardingApplication> findAllByOrderByCreatedAtDesc();

  @Query(value = IN_JURISDICTION + " AND (:status IS NULL OR a.status = :status) ORDER BY a.created_at DESC",
      nativeQuery = true)
  List<OnboardingApplication> findInJurisdiction(@Param("stateId") Long stateId,
      @Param("districtId") Long districtId, @Param("status") String status);

  @Query(value = IN_JURISDICTION + " AND a.application_id = :id", nativeQuery = true)
  Optional<OnboardingApplication> findByIdInJurisdiction(@Param("id") Long id,
      @Param("stateId") Long stateId, @Param("districtId") Long districtId);

  // Lock the application and the matched applicant so that its location stays fixed during the decision.
  @Query(value = IN_JURISDICTION + " AND a.application_id = :id FOR UPDATE", nativeQuery = true)
  Optional<OnboardingApplication> findByIdInJurisdictionForUpdate(@Param("id") Long id,
      @Param("stateId") Long stateId, @Param("districtId") Long districtId);

  List<OnboardingApplication> findByApplicantType(String applicantType);

  boolean existsByApplicationNumber(String applicationNumber);

  Optional<OnboardingApplication> findByContactEmail(String contactEmail);
}
