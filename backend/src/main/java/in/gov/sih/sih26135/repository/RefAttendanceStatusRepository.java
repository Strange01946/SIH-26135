package in.gov.sih.sih26135.repository;

import in.gov.sih.sih26135.entity.RefAttendanceStatus;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface RefAttendanceStatusRepository extends JpaRepository<RefAttendanceStatus, Long> {

  Optional<RefAttendanceStatus> findByStatusCode(String statusCode);

  boolean existsByStatusCode(String statusCode);

  List<RefAttendanceStatus> findAllByOrderBySortOrderAsc();
}
