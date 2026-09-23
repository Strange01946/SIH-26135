package in.gov.sih.sih26135.service.impl;

import in.gov.sih.sih26135.dto.response.CourseOutcomeSummaryResponse;
import in.gov.sih.sih26135.entity.analytics.CourseOutcomeSummary;
import in.gov.sih.sih26135.exception.BadRequestException;
import in.gov.sih.sih26135.exception.ResourceNotFoundException;
import in.gov.sih.sih26135.mapper.CourseOutcomeSummaryMapper;
import in.gov.sih.sih26135.repository.analytics.CourseOutcomeSummaryRepository;
import in.gov.sih.sih26135.service.CourseOutcomeSummaryService;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class CourseOutcomeSummaryServiceImpl implements CourseOutcomeSummaryService {

  private final CourseOutcomeSummaryRepository repository;
  private final CourseOutcomeSummaryMapper mapper;

  public CourseOutcomeSummaryServiceImpl(
      CourseOutcomeSummaryRepository repository,
      CourseOutcomeSummaryMapper mapper) {
    this.repository = repository;
    this.mapper = mapper;
  }

  @Override
  public List<CourseOutcomeSummaryResponse> getAllCourseOutcomes() {
    return repository.findAll().stream()
        .map(mapper::toResponse)
        .toList();
  }

  @Override
  public List<CourseOutcomeSummaryResponse> getAllCourseOutcomesOrderByEnrollmentCountDesc() {
    return repository.findAllByOrderByEnrollmentCountDesc().stream()
        .map(mapper::toResponse)
        .toList();
  }

  @Override
  public CourseOutcomeSummaryResponse getCourseOutcomeByCourseId(Long courseId) {
    if (courseId == null) {
      throw new BadRequestException("Course ID is required");
    }
    CourseOutcomeSummary entity = repository.findById(courseId)
        .orElseThrow(() -> new ResourceNotFoundException("CourseOutcomeSummary", "courseId"));
    return mapper.toResponse(entity);
  }
}
