package in.gov.sih.sih26135.repository;

import in.gov.sih.sih26135.entity.DataQualityRule;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface DataQualityRuleRepository extends JpaRepository<DataQualityRule, Long> {

  Optional<DataQualityRule> findByRuleCode(String ruleCode);

  boolean existsByRuleCode(String ruleCode);

  List<DataQualityRule> findByTargetEntityType(String targetEntityType);

  List<DataQualityRule> findByDataQualityCategoryId(Long categoryId);

  List<DataQualityRule> findByDataQualitySeverityId(Long severityId);

  List<DataQualityRule> findByLifecycleStatusId(Long lifecycleStatusId);
}
