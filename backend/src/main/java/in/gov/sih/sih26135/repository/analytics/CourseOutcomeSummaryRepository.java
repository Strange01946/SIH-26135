package in.gov.sih.sih26135.repository.analytics;

import in.gov.sih.sih26135.entity.analytics.CourseOutcomeSummary;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CourseOutcomeSummaryRepository extends
    JpaRepository<CourseOutcomeSummary, Long> {

  List<CourseOutcomeSummary> findAllByOrderByEnrollmentCountDesc();
}
