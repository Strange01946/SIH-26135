package in.gov.sih.sih26135.repository;

import in.gov.sih.sih26135.entity.RefAuditAction;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface RefAuditActionRepository extends JpaRepository<RefAuditAction, Long> {

  Optional<RefAuditAction> findByActionCode(String actionCode);

  boolean existsByActionCode(String actionCode);
}
