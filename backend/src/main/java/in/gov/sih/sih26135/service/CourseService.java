package in.gov.sih.sih26135.service;

import in.gov.sih.sih26135.dto.request.CreateCourseRequest;
import in.gov.sih.sih26135.dto.request.UpdateCourseRequest;
import in.gov.sih.sih26135.dto.response.CourseResponse;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface CourseService {

  CourseResponse getById(Long id);

  CourseResponse getByCode(String courseCode);

  List<CourseResponse> getAllCourses();

  List<CourseResponse> getCoursesBySectorId(Long sectorId);

  List<CourseResponse> getCoursesByIndustryId(Long industryId);

  List<CourseResponse> getCoursesByDeliveryModeId(Long deliveryModeId);

  Page<CourseResponse> getAllCourses(Pageable pageable);

  Page<CourseResponse> getCoursesBySectorId(Long sectorId, Pageable pageable);

  Page<CourseResponse> getCoursesByIndustryId(Long industryId, Pageable pageable);

  Page<CourseResponse> getCoursesByDeliveryModeId(Long deliveryModeId, Pageable pageable);

  CourseResponse createCourse(CreateCourseRequest request);

  CourseResponse updateCourse(Long id, UpdateCourseRequest request);

  void deleteCourse(Long id);
}
