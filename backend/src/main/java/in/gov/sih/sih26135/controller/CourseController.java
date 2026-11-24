package in.gov.sih.sih26135.controller;

import org.springframework.validation.annotation.Validated;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Max;
import in.gov.sih.sih26135.dto.response.CourseResponse;
import in.gov.sih.sih26135.response.ApiResponse;
import in.gov.sih.sih26135.response.PagedResponse;
import in.gov.sih.sih26135.response.PaginationUtils;
import in.gov.sih.sih26135.service.CourseService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.security.access.prepost.PreAuthorize;

/**
 * Read access to training identifiers used by historical records and reporting.
 *
 * <p>Base Route: /api/v1/courses
 * Produces: CourseResponse enveloped in ApiResponse
 */
@Validated
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
  public ResponseEntity<ApiResponse<CourseResponse>> getById(@PathVariable @Positive Long id) {
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
      @RequestParam(value = "sectorId", required = false) @Positive Long sectorId,
      @RequestParam(value = "industryId", required = false) @Positive Long industryId,
      @RequestParam(value = "deliveryModeId", required = false) @Positive Long deliveryModeId,
      @RequestParam(value = "page", defaultValue = "0") @Min(0) int page,
      @RequestParam(value = "size", defaultValue = "20") @Min(1) @Max(100) int size,
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

}
