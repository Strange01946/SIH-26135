package in.gov.sih.sih26135.repository;

import in.gov.sih.sih26135.entity.RefDataQualityIssueStatus;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface RefDataQualityIssueStatusRepository extends
    JpaRepository<RefDataQualityIssueStatus, Long> {

  Optional<RefDataQualityIssueStatus> findByStatusCode(String statusCode);

  boolean existsByStatusCode(String statusCode);

  List<RefDataQualityIssueStatus> findByIsOpenFlagTrue();
}
