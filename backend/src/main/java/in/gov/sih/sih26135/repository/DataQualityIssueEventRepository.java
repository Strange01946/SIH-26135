package in.gov.sih.sih26135.repository;

import in.gov.sih.sih26135.entity.DataQualityIssueEvent;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface DataQualityIssueEventRepository extends
    JpaRepository<DataQualityIssueEvent, Long> {

  List<DataQualityIssueEvent> findByDataQualityIssueId(Long issueId);

  List<DataQualityIssueEvent> findByDataQualityIssueStatusId(Long statusId);

  List<DataQualityIssueEvent> findByChangedByUserId(Long changedByUserId);
}
