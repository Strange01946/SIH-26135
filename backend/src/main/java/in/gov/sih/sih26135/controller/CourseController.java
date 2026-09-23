package in.gov.sih.sih26135.controller;

import in.gov.sih.sih26135.dto.request.CreateCourseRequest;
import in.gov.sih.sih26135.dto.request.UpdateCourseRequest;
import in.gov.sih.sih26135.dto.response.CourseResponse;
import in.gov.sih.sih26135.response.ApiResponse;
import in.gov.sih.sih26135.service.CourseService;
import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller for managing Course domain resources.
 *
 * <p>Base Route: /api/v1/courses
 * Consumes: CreateCourseRequest, UpdateCourseRequest
 * Produces: CourseResponse enveloped in ApiResponse
 */
@RestController
@RequestMapping("/api/v1/courses")
public class CourseController {

  private final CourseService courseService;

  public CourseController(CourseService courseService) {
    this.courseService = courseService;
  }

  /**
   * Retrieves a course by primary key identifier.
   *
   * @param id primary key identifier of the course
   * @return 200 OK with CourseResponse enveloped in ApiResponse
   */
  @GetMapping("/{id}")
  public ResponseEntity<ApiResponse<CourseResponse>> getById(@PathVariable Long id) {
    CourseResponse response = courseService.getById(id);
    return ResponseEntity.ok(ApiResponse.ok(response));
  }

  /**
   * Retrieves courses with optional filtering by code, sector ID, industry ID, or delivery mode ID.
   *
   * @param code optional course code filter
   * @param sectorId optional sector identifier filter
   * @param industryId optional industry identifier filter
   * @param deliveryModeId optional delivery mode identifier filter
   * @return 200 OK with list of courses or single matched course enveloped in ApiResponse
   */
  @GetMapping
  public ResponseEntity<ApiResponse<?>> getCourses(
      @RequestParam(value = "code", required = false) String code,
      @RequestParam(value = "sectorId", required = false) Long sectorId,
      @RequestParam(value = "industryId", required = false) Long industryId,
      @RequestParam(value = "deliveryModeId", required = false) Long deliveryModeId) {
    if (code != null && !code.isBlank()) {
      CourseResponse response = courseService.getByCode(code.trim());
      return ResponseEntity.ok(ApiResponse.ok(response));
    }
    if (sectorId != null) {
      List<CourseResponse> courses = courseService.getCoursesBySectorId(sectorId);
      return ResponseEntity.ok(ApiResponse.ok(courses));
    }
    if (industryId != null) {
      List<CourseResponse> courses = courseService.getCoursesByIndustryId(industryId);
      return ResponseEntity.ok(ApiResponse.ok(courses));
    }
    if (deliveryModeId != null) {
      List<CourseResponse> courses = courseService.getCoursesByDeliveryModeId(deliveryModeId);
      return ResponseEntity.ok(ApiResponse.ok(courses));
    }
    List<CourseResponse> courses = courseService.getAllCourses();
    return ResponseEntity.ok(ApiResponse.ok(courses));
  }

  /**
   * Creates a new course record.
   *
   * @param request course creation payload
   * @param httpRequest HTTP servlet request for URI extraction
   * @return 201 Created with created CourseResponse enveloped in ApiResponse
   */
  @PostMapping
  public ResponseEntity<ApiResponse<CourseResponse>> createCourse(
      @RequestBody CreateCourseRequest request,
      HttpServletRequest httpRequest) {
    CourseResponse response = courseService.createCourse(request);
    return ResponseEntity.status(HttpStatus.CREATED)
        .body(ApiResponse.success("Course created successfully", response, httpRequest.getRequestURI()));
  }

  /**
   * Updates an existing course record.
   *
   * @param id primary key identifier of the course to update
   * @param request course update payload
   * @return 200 OK with updated CourseResponse enveloped in ApiResponse
   */
  @PutMapping("/{id}")
  public ResponseEntity<ApiResponse<CourseResponse>> updateCourse(
      @PathVariable Long id,
      @RequestBody UpdateCourseRequest request) {
    CourseResponse response = courseService.updateCourse(id, request);
    return ResponseEntity.ok(ApiResponse.success("Course updated successfully", response));
  }

  /**
   * Soft-deletes a course record.
   *
   * @param id primary key identifier of the course to delete
   * @return 200 OK with success confirmation message
   */
  @DeleteMapping("/{id}")
  public ResponseEntity<ApiResponse<Void>> deleteCourse(@PathVariable Long id) {
    courseService.deleteCourse(id);
    return ResponseEntity.ok(ApiResponse.success("Course deleted successfully"));
  }
}
