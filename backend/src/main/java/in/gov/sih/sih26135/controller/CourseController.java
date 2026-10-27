package in.gov.sih.sih26135.controller;

import jakarta.validation.Valid;
import in.gov.sih.sih26135.dto.request.CreateCourseRequest;
import in.gov.sih.sih26135.dto.request.UpdateCourseRequest;
import in.gov.sih.sih26135.dto.response.CourseResponse;
import in.gov.sih.sih26135.response.ApiResponse;
import in.gov.sih.sih26135.response.PagedResponse;
import in.gov.sih.sih26135.response.PaginationUtils;
import in.gov.sih.sih26135.service.CourseService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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
import org.springframework.security.access.prepost.PreAuthorize;

/**
 * REST controller for managing Course domain resources.
 *
 * <p>Base Route: /api/v1/courses
 * Consumes: CreateCourseRequest, UpdateCourseRequest
 * Produces: CourseResponse enveloped in ApiResponse
 */
@RestController
@RequestMapping("/api/v1/courses")
@PreAuthorize("hasAuthority('course.manage')")
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
      @RequestParam(value = "deliveryModeId", required = false) Long deliveryModeId,
      @RequestParam(value = "page", defaultValue = "0") int page,
      @RequestParam(value = "size", defaultValue = "20") int size,
      @RequestParam(value = "sort", defaultValue = "id") String sort,
      @RequestParam(value = "direction", defaultValue = "ASC") String direction) {
    if (code != null && !code.isBlank()) {
      CourseResponse response = courseService.getByCode(code.trim());
      return ResponseEntity.ok(ApiResponse.ok(response));
    }
    Pageable pageable = PaginationUtils.buildPageable(
        page, size, sort, direction, PaginationUtils.COURSE_SORT_FIELDS, "id"
    );
    if (sectorId != null) {
      Page<CourseResponse> courses = courseService.getCoursesBySectorId(sectorId, pageable);
      return ResponseEntity.ok(ApiResponse.ok(PagedResponse.of(courses)));
    }
    if (industryId != null) {
      Page<CourseResponse> courses = courseService.getCoursesByIndustryId(industryId, pageable);
      return ResponseEntity.ok(ApiResponse.ok(PagedResponse.of(courses)));
    }
    if (deliveryModeId != null) {
      Page<CourseResponse> courses = courseService.getCoursesByDeliveryModeId(deliveryModeId, pageable);
      return ResponseEntity.ok(ApiResponse.ok(PagedResponse.of(courses)));
    }
    Page<CourseResponse> courses = courseService.getAllCourses(pageable);
    return ResponseEntity.ok(ApiResponse.ok(PagedResponse.of(courses)));
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
      @Valid @RequestBody CreateCourseRequest request,
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
      @Valid @RequestBody UpdateCourseRequest request) {
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
