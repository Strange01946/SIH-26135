package in.gov.sih.sih26135.repository.analytics;

import in.gov.sih.sih26135.entity.analytics.SkillGapSummary;
import in.gov.sih.sih26135.entity.analytics.SkillGapSummaryId;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SkillGapSummaryRepository extends
    JpaRepository<SkillGapSummary, SkillGapSummaryId> {

  List<SkillGapSummary> findBySkillId(Long skillId);

  List<SkillGapSummary> findBySkillGapSeverityId(Long skillGapSeverityId);

  List<SkillGapSummary> findBySeverityCode(String severityCode);

  List<SkillGapSummary> findAllByOrderByGapCountDesc();
}
