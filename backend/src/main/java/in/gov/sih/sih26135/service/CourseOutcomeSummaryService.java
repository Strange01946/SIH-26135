package in.gov.sih.sih26135.service;

import in.gov.sih.sih26135.dto.response.CourseOutcomeSummaryResponse;
import java.util.List;

public interface CourseOutcomeSummaryService {

  List<CourseOutcomeSummaryResponse> getAllCourseOutcomes();

  List<CourseOutcomeSummaryResponse> getAllCourseOutcomesOrderByEnrollmentCountDesc();

  CourseOutcomeSummaryResponse getCourseOutcomeByCourseId(Long courseId);
}
