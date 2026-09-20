package in.gov.sih.sih26135.repository;

import in.gov.sih.sih26135.entity.DataQualityIssue;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface DataQualityIssueRepository extends JpaRepository<DataQualityIssue, Long> {

  List<DataQualityIssue> findByDataQualityRuleId(Long ruleId);

  List<DataQualityIssue> findByDataQualityCategoryId(Long categoryId);

  List<DataQualityIssue> findByDataQualitySeverityId(Long severityId);

  List<DataQualityIssue> findByDataQualityIssueStatusId(Long statusId);

  List<DataQualityIssue> findByDataQualityDetectionSourceId(Long sourceId);

  List<DataQualityIssue> findByEntityType(String entityType);

  List<DataQualityIssue> findByEntityTypeAndEntityId(String entityType, Long entityId);

  List<DataQualityIssue> findByAssignedUserId(Long assignedUserId);

  List<DataQualityIssue> findByResolvedByUserId(Long resolvedByUserId);
}
