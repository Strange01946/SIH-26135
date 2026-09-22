package in.gov.sih.sih26135.service;

import in.gov.sih.sih26135.dto.request.CreateCourseRequest;
import in.gov.sih.sih26135.dto.request.UpdateCourseRequest;
import in.gov.sih.sih26135.dto.response.CourseResponse;
import java.util.List;

public interface CourseService {

  CourseResponse getById(Long id);

  CourseResponse getByCode(String courseCode);

  List<CourseResponse> getAllCourses();

  List<CourseResponse> getCoursesBySectorId(Long sectorId);

  List<CourseResponse> getCoursesByIndustryId(Long industryId);

  List<CourseResponse> getCoursesByDeliveryModeId(Long deliveryModeId);

  CourseResponse createCourse(CreateCourseRequest request);

  CourseResponse updateCourse(Long id, UpdateCourseRequest request);

  void deleteCourse(Long id);
}
