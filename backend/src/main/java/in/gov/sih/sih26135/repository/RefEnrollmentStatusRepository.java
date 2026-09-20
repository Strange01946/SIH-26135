package in.gov.sih.sih26135.repository;

import in.gov.sih.sih26135.entity.RefEnrollmentStatus;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface RefEnrollmentStatusRepository extends JpaRepository<RefEnrollmentStatus, Long> {

  Optional<RefEnrollmentStatus> findByStatusCode(String statusCode);

  boolean existsByStatusCode(String statusCode);

  List<RefEnrollmentStatus> findAllByOrderBySortOrderAsc();
}
