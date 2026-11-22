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

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("SELECT a FROM OnboardingApplication a WHERE a.id = :id")
  Optional<OnboardingApplication> findByIdForUpdate(@Param("id") Long id);

  Optional<OnboardingApplication> findByApplicationNumber(String applicationNumber);

  Optional<OnboardingApplication> findByUserId(Long userId);

  List<OnboardingApplication> findByStatusOrderByCreatedAtDesc(String status);

  List<OnboardingApplication> findAllByOrderByCreatedAtDesc();

  List<OnboardingApplication> findByApplicantType(String applicantType);

  boolean existsByApplicationNumber(String applicationNumber);

  Optional<OnboardingApplication> findByContactEmail(String contactEmail);
}
