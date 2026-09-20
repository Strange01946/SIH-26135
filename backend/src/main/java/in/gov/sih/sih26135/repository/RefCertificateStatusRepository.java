package in.gov.sih.sih26135.repository;

import in.gov.sih.sih26135.entity.RefCertificateStatus;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface RefCertificateStatusRepository extends JpaRepository<RefCertificateStatus, Long> {

  Optional<RefCertificateStatus> findByStatusCode(String statusCode);

  boolean existsByStatusCode(String statusCode);

  List<RefCertificateStatus> findAllByOrderBySortOrderAsc();
}
