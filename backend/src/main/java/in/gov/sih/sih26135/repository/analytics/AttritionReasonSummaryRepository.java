package in.gov.sih.sih26135.repository.analytics;

import in.gov.sih.sih26135.entity.analytics.AttritionReasonSummary;
import in.gov.sih.sih26135.entity.analytics.AttritionReasonSummaryId;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AttritionReasonSummaryRepository extends
    JpaRepository<AttritionReasonSummary, AttritionReasonSummaryId> {

  List<AttritionReasonSummary> findByEmploymentExitReasonId(Long employmentExitReasonId);

  List<AttritionReasonSummary> findBySeparationNatureId(Long separationNatureId);

  List<AttritionReasonSummary> findByIsVoluntaryFlag(Boolean isVoluntaryFlag);

  List<AttritionReasonSummary> findByIsInvoluntaryFlag(Boolean isInvoluntaryFlag);

  List<AttritionReasonSummary> findAllByOrderByExitCountDesc();
}
