package in.gov.sih.sih26135.pagination;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import in.gov.sih.sih26135.MysqlConnectionTest;
import in.gov.sih.sih26135.entity.Course;
import in.gov.sih.sih26135.entity.Employer;
import in.gov.sih.sih26135.entity.Trainee;
import in.gov.sih.sih26135.entity.TrainingBatch;
import in.gov.sih.sih26135.entity.TrainingCenter;
import in.gov.sih.sih26135.entity.TrainingEnrollment;
import in.gov.sih.sih26135.entity.TrainingProvider;
import in.gov.sih.sih26135.repository.CourseRepository;
import in.gov.sih.sih26135.repository.EmployerRepository;
import in.gov.sih.sih26135.repository.TraineeRepository;
import in.gov.sih.sih26135.repository.TrainingBatchRepository;
import in.gov.sih.sih26135.repository.TrainingCenterRepository;
import in.gov.sih.sih26135.repository.TrainingEnrollmentRepository;
import in.gov.sih.sih26135.repository.TrainingProviderRepository;
import in.gov.sih.sih26135.response.PaginationUtils;
import in.gov.sih.sih26135.security.jwt.JwtTokenProvider;
import java.sql.Connection;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import javax.sql.DataSource;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

/**
 * Integration and regression test suite for Phase 26.3 Core Domain Resource Pagination.
 * Covers all seven resources: Trainee, Employer, Course, TrainingCenter, TrainingProvider,
 * TrainingBatch, TrainingEnrollment.
 * Verifies pagination defaults, bounds, sorting allowlists, soft-delete filtering across content
 * and count queries, single-record lookup unpaginated contracts, and security boundaries.
 * Uses collision-guarded, foreign-key-safe fixtures to ensure 100% deterministic assertion execution.
 */
@SpringBootTest
@AutoConfigureMockMvc
@DisplayName("Phase 26.3 - Core Domain Resource Pagination Integration Tests")
class CoreDomainResourcePaginationTest {

  @Autowired
  private DataSource dataSource;

  @Autowired
  private MockMvc mockMvc;

  @Autowired
  private ObjectMapper objectMapper;

  @Autowired
  private JwtTokenProvider jwtTokenProvider;

  @Autowired
  private JdbcTemplate jdbc;

  @Autowired
  private TraineeRepository traineeRepository;

  @Autowired
  private EmployerRepository employerRepository;

  @Autowired
  private CourseRepository courseRepository;

  @Autowired
  private TrainingCenterRepository trainingCenterRepository;

  @Autowired
  private TrainingProviderRepository trainingProviderRepository;

  @Autowired
  private TrainingBatchRepository trainingBatchRepository;

  @Autowired
  private TrainingEnrollmentRepository trainingEnrollmentRepository;

  private String fullAdminToken;
  private String guestToken;
  private boolean fixtureCleanupRequired = false;

  @BeforeEach
  void setUp() throws Exception {
    try (Connection conn = dataSource.getConnection()) {
      String catalog = conn.getCatalog();
      MysqlConnectionTest.assertDedicatedTestCatalog(catalog);
      assertThat(catalog).isEqualToIgnoringCase(MysqlConnectionTest.EXPECTED_TEST_CATALOG);
    }

    // 1. Collision guard: MUST run before any operation that could modify or delete records.
    // If a collision exists, abort safely without modifying or deleting the colliding record.
    checkNoCollisions();

    // 2. Mark cleanup as required before insertion begins so any partial insertion is cleaned up.
    fixtureCleanupRequired = true;
    try {
      insertTestFixtures();
    } catch (Exception e) {
      cleanTestFixtures();
      fixtureCleanupRequired = false;
      throw e;
    }

    fullAdminToken = "Bearer " + jwtTokenProvider.generateAccessToken(
        1L,
        "admin",
        List.of(
            "trainee.read",
            "placement.manage",
            "course.manage",
            "center.manage",
            "provider.manage",
            "enrollment.manage"
        )
    );

    guestToken = "Bearer " + jwtTokenProvider.generateAccessToken(
        999L,
        "guest",
        List.of("ROLE_GUEST")
    );
  }

  @AfterEach
  void tearDown() {
    if (fixtureCleanupRequired) {
      try {
        cleanTestFixtures();
      } finally {
        fixtureCleanupRequired = false;
      }
    }
  }

  private void checkNoCollisions() {
    // Dedicated collision guard for training_providers registration numbers
    Integer providerRegCollisions = jdbc.queryForObject(
        "SELECT count(*) FROM training_providers WHERE registration_number IN ('REG-PRV-9971', 'REG-PRV-9972')",
        Integer.class);
    if (providerRegCollisions != null && providerRegCollisions > 0) {
      throw new IllegalStateException("Test fixture collision detected: training provider record already exists with registration number 'REG-PRV-9971' or 'REG-PRV-9972'. Aborting safely without modifying that record.");
    }

    Integer collisions = jdbc.queryForObject(
        "SELECT ("
            + "(SELECT count(*) FROM states WHERE state_id = 9971 OR state_code = 'PX') + "
            + "(SELECT count(*) FROM districts WHERE district_id = 9971 OR district_code = 'PD71') + "
            + "(SELECT count(*) FROM sectors WHERE sector_id = 9971 OR sector_code = 'SEC-PAG-9971') + "
            + "(SELECT count(*) FROM courses WHERE course_id IN (9971, 9972) OR course_code IN ('CRS-PAG-9971', 'CRS-PAG-9972')) + "
            + "(SELECT count(*) FROM employers WHERE employer_id IN (9971, 9972) OR employer_code IN ('EMP-PAG-9971', 'EMP-PAG-9972')) + "
            + "(SELECT count(*) FROM trainees WHERE trainee_id IN (9971, 9972) OR registration_number IN ('REG-PAG-9971', 'REG-PAG-9972')) + "
            + "(SELECT count(*) FROM training_providers WHERE provider_id IN (9971, 9972) OR provider_code IN ('PRV-PAG-9971', 'PRV-PAG-9972') OR registration_number IN ('REG-PRV-9971', 'REG-PRV-9972')) + "
            + "(SELECT count(*) FROM training_centers WHERE center_id IN (9971, 9972) OR center_code IN ('CTR-PAG-9971', 'CTR-PAG-9972')) + "
            + "(SELECT count(*) FROM organizations WHERE organization_id = 9971 OR organization_code = 'ORG-PAG-9971') + "
            + "(SELECT count(*) FROM departments WHERE department_id = 9971 OR department_code = 'DEP-9971') + "
            + "(SELECT count(*) FROM schemes WHERE scheme_id = 9971 OR scheme_code = 'SCH-PAG-9971') + "
            + "(SELECT count(*) FROM programs WHERE program_id = 9971 OR program_code = 'PRG-PAG-9971') + "
            + "(SELECT count(*) FROM training_batches WHERE batch_id IN (9971, 9972) OR batch_code IN ('BAT-PAG-9971', 'BAT-PAG-9972')) + "
            + "(SELECT count(*) FROM training_enrollments WHERE enrollment_id IN (9971, 9972) OR enrollment_number IN ('ENR-PAG-9971', 'ENR-PAG-9972'))"
            + ")", Integer.class);
    if (collisions != null && collisions > 0) {
      throw new IllegalStateException("Test fixture collision detected: one or more records exist with fixture IDs, codes, or registration numbers. Aborting to protect existing data.");
    }
  }

  private void insertTestFixtures() {
    // 1. states
    jdbc.update("INSERT INTO states (state_id, state_code, state_name, lifecycle_status_id, created_at, updated_at) "
        + "VALUES (9971, 'PX', 'Pagination Test State 9971', 1, NOW(), NOW())");

    // 2. districts
    jdbc.update("INSERT INTO districts (district_id, state_id, district_code, district_name, lifecycle_status_id, created_at, updated_at) "
        + "VALUES (9971, 9971, 'PD71', 'Pagination Test District 9971', 1, NOW(), NOW())");

    // 3. sectors
    jdbc.update("INSERT INTO sectors (sector_id, sector_code, sector_name, lifecycle_status_id, created_at, updated_at) "
        + "VALUES (9971, 'SEC-PAG-9971', 'Pagination Test Sector 9971', 1, NOW(), NOW())");

    // 4. courses (active 9971, soft-deleted 9972)
    jdbc.update("INSERT INTO courses (course_id, course_code, course_name, sector_id, delivery_mode_id, lifecycle_status_id, created_at, updated_at, deleted_at) "
        + "VALUES (9971, 'CRS-PAG-9971', 'Course Active 9971', 9971, 1, 1, NOW(), NOW(), NULL)");
    jdbc.update("INSERT INTO courses (course_id, course_code, course_name, sector_id, delivery_mode_id, lifecycle_status_id, created_at, updated_at, deleted_at) "
        + "VALUES (9972, 'CRS-PAG-9972', 'Course Deleted 9972', 9971, 1, 1, NOW(), NOW(), NOW())");

    // 5. employers (active 9971, soft-deleted 9972)
    jdbc.update("INSERT INTO employers (employer_id, employer_code, employer_name, organization_type_id, state_id, district_id, record_verification_status_id, lifecycle_status_id, created_at, updated_at, deleted_at) "
        + "VALUES (9971, 'EMP-PAG-9971', 'Employer Active 9971', 1, 9971, 9971, 1, 1, NOW(), NOW(), NULL)");
    jdbc.update("INSERT INTO employers (employer_id, employer_code, employer_name, organization_type_id, state_id, district_id, record_verification_status_id, lifecycle_status_id, created_at, updated_at, deleted_at) "
        + "VALUES (9972, 'EMP-PAG-9972', 'Employer Deleted 9972', 1, 9971, 9971, 1, 1, NOW(), NOW(), NOW())");

    // 6. trainees (active 9971, soft-deleted 9972)
    jdbc.update("INSERT INTO trainees (trainee_id, registration_number, first_name, last_name, gender_id, state_id, district_id, current_employment_status_id, profile_status_id, created_at, updated_at, deleted_at) "
        + "VALUES (9971, 'REG-PAG-9971', 'TraineeActive', 'Tester', 1, 9971, 9971, 1, 1, NOW(), NOW(), NULL)");
    jdbc.update("INSERT INTO trainees (trainee_id, registration_number, first_name, last_name, gender_id, state_id, district_id, current_employment_status_id, profile_status_id, created_at, updated_at, deleted_at) "
        + "VALUES (9972, 'REG-PAG-9972', 'TraineeDeleted', 'Tester', 1, 9971, 9971, 1, 1, NOW(), NOW(), NOW())");

    // 7. training_providers (active 9971, soft-deleted 9972)
    jdbc.update("INSERT INTO training_providers (provider_id, provider_code, provider_name, registration_number, organization_type_id, state_id, district_id, accreditation_status_id, lifecycle_status_id, created_at, updated_at, deleted_at) "
        + "VALUES (9971, 'PRV-PAG-9971', 'Provider Active 9971', 'REG-PRV-9971', 1, 9971, 9971, 1, 1, NOW(), NOW(), NULL)");
    jdbc.update("INSERT INTO training_providers (provider_id, provider_code, provider_name, registration_number, organization_type_id, state_id, district_id, accreditation_status_id, lifecycle_status_id, created_at, updated_at, deleted_at) "
        + "VALUES (9972, 'PRV-PAG-9972', 'Provider Deleted 9972', 'REG-PRV-9972', 1, 9971, 9971, 1, 1, NOW(), NOW(), NOW())");

    // 8. training_centers (active 9971, soft-deleted 9972)
    jdbc.update("INSERT INTO training_centers (center_id, provider_id, center_code, center_name, state_id, district_id, lifecycle_status_id, created_at, updated_at, deleted_at) "
        + "VALUES (9971, 9971, 'CTR-PAG-9971', 'Center Active 9971', 9971, 9971, 1, NOW(), NOW(), NULL)");
    jdbc.update("INSERT INTO training_centers (center_id, provider_id, center_code, center_name, state_id, district_id, lifecycle_status_id, created_at, updated_at, deleted_at) "
        + "VALUES (9972, 9971, 'CTR-PAG-9972', 'Center Deleted 9972', 9971, 9971, 1, NOW(), NOW(), NOW())");

    // 9. organizations
    jdbc.update("INSERT INTO organizations (organization_id, organization_code, organization_name, organization_type_id, lifecycle_status_id, created_at, updated_at) "
        + "VALUES (9971, 'ORG-PAG-9971', 'Test Organization 9971', 1, 1, NOW(), NOW())");

    // 10. departments
    jdbc.update("INSERT INTO departments (department_id, organization_id, department_code, department_name, lifecycle_status_id, created_at, updated_at) "
        + "VALUES (9971, 9971, 'DEP-9971', 'Test Department 9971', 1, NOW(), NOW())");

    // 11. schemes
    jdbc.update("INSERT INTO schemes (scheme_id, scheme_code, scheme_name, department_id, currency_code, lifecycle_status_id, created_at, updated_at) "
        + "VALUES (9971, 'SCH-PAG-9971', 'Pagination Test Scheme 9971', 9971, 'INR', 1, NOW(), NOW())");

    // 12. programs
    jdbc.update("INSERT INTO programs (program_id, program_code, program_name, department_id, scheme_id, lifecycle_status_id, created_at, updated_at) "
        + "VALUES (9971, 'PRG-PAG-9971', 'Pagination Test Program 9971', 9971, 9971, 1, NOW(), NOW())");

    // 13. training_batches (active 9971, soft-deleted 9972)
    jdbc.update("INSERT INTO training_batches (batch_id, batch_code, course_id, provider_id, center_id, program_id, start_date, capacity, batch_status_id, created_at, updated_at, deleted_at) "
        + "VALUES (9971, 'BAT-PAG-9971', 9971, 9971, 9971, 9971, '2026-01-01', 30, 1, NOW(), NOW(), NULL)");
    jdbc.update("INSERT INTO training_batches (batch_id, batch_code, course_id, provider_id, center_id, program_id, start_date, capacity, batch_status_id, created_at, updated_at, deleted_at) "
        + "VALUES (9972, 'BAT-PAG-9972', 9971, 9971, 9971, 9971, '2026-01-01', 30, 1, NOW(), NOW(), NOW())");

    // 14. training_enrollments (active 9971, soft-deleted 9972)
    jdbc.update("INSERT INTO training_enrollments (enrollment_id, enrollment_number, trainee_id, program_id, course_id, provider_id, center_id, batch_id, enrollment_date, enrollment_status_id, created_at, updated_at, deleted_at) "
        + "VALUES (9971, 'ENR-PAG-9971', 9971, 9971, 9971, 9971, 9971, 9971, '2026-01-01', 1, NOW(), NOW(), NULL)");
    jdbc.update("INSERT INTO training_enrollments (enrollment_id, enrollment_number, trainee_id, program_id, course_id, provider_id, center_id, batch_id, enrollment_date, enrollment_status_id, created_at, updated_at, deleted_at) "
        + "VALUES (9972, 'ENR-PAG-9972', 9972, 9971, 9971, 9971, 9971, 9972, '2026-01-01', 1, NOW(), NOW(), NOW())");
  }

  private void cleanTestFixtures() {
    jdbc.update("DELETE FROM training_enrollments WHERE enrollment_id IN (9971, 9972) AND enrollment_number IN ('ENR-PAG-9971', 'ENR-PAG-9972')");
    jdbc.update("DELETE FROM training_batches WHERE batch_id IN (9971, 9972) AND batch_code IN ('BAT-PAG-9971', 'BAT-PAG-9972')");
    jdbc.update("DELETE FROM programs WHERE program_id = 9971 AND program_code = 'PRG-PAG-9971'");
    jdbc.update("DELETE FROM schemes WHERE scheme_id = 9971 AND scheme_code = 'SCH-PAG-9971'");
    jdbc.update("DELETE FROM departments WHERE department_id = 9971 AND department_code = 'DEP-9971'");
    jdbc.update("DELETE FROM organizations WHERE organization_id = 9971 AND organization_code = 'ORG-PAG-9971'");
    jdbc.update("DELETE FROM training_centers WHERE center_id IN (9971, 9972) AND center_code IN ('CTR-PAG-9971', 'CTR-PAG-9972')");
    jdbc.update("DELETE FROM training_providers WHERE provider_id IN (9971, 9972) AND provider_code IN ('PRV-PAG-9971', 'PRV-PAG-9972') AND registration_number IN ('REG-PRV-9971', 'REG-PRV-9972')");
    jdbc.update("DELETE FROM trainees WHERE trainee_id IN (9971, 9972) AND registration_number IN ('REG-PAG-9971', 'REG-PAG-9972')");
    jdbc.update("DELETE FROM employers WHERE employer_id IN (9971, 9972) AND employer_code IN ('EMP-PAG-9971', 'EMP-PAG-9972')");
    jdbc.update("DELETE FROM courses WHERE course_id IN (9971, 9972) AND course_code IN ('CRS-PAG-9971', 'CRS-PAG-9972')");
    jdbc.update("DELETE FROM sectors WHERE sector_id = 9971 AND sector_code = 'SEC-PAG-9971'");
    jdbc.update("DELETE FROM districts WHERE district_id = 9971 AND district_code = 'PD71'");
    jdbc.update("DELETE FROM states WHERE state_id = 9971 AND state_code = 'PX'");
  }

  // =========================================================================
  // 1. Default Pagination Behavior across all 7 resources
  // =========================================================================

  @Test
  @DisplayName("1.1 Trainee listing returns ApiResponse<PagedResponse<TraineeResponse>> with default page=0, size=20")
  void testTraineeDefaultPagination() throws Exception {
    assertEndpointReturnsValidPage("/api/v1/trainees");
  }

  @Test
  @DisplayName("1.2 Employer listing returns ApiResponse<PagedResponse<EmployerResponse>> with default page=0, size=20")
  void testEmployerDefaultPagination() throws Exception {
    assertEndpointReturnsValidPage("/api/v1/employers");
  }

  @Test
  @DisplayName("1.3 Course listing returns ApiResponse<PagedResponse<CourseResponse>> with default page=0, size=20")
  void testCourseDefaultPagination() throws Exception {
    assertEndpointReturnsValidPage("/api/v1/courses");
  }

  @Test
  @DisplayName("1.4 TrainingCenter listing returns ApiResponse<PagedResponse<TrainingCenterResponse>> with default page=0, size=20")
  void testTrainingCenterDefaultPagination() throws Exception {
    assertEndpointReturnsValidPage("/api/v1/training-centers");
  }

  @Test
  @DisplayName("1.5 TrainingProvider listing returns ApiResponse<PagedResponse<TrainingProviderResponse>> with default page=0, size=20")
  void testTrainingProviderDefaultPagination() throws Exception {
    assertEndpointReturnsValidPage("/api/v1/training-providers");
  }

  @Test
  @DisplayName("1.6 TrainingBatch listing returns ApiResponse<PagedResponse<TrainingBatchResponse>> with default page=0, size=20")
  void testTrainingBatchDefaultPagination() throws Exception {
    assertEndpointReturnsValidPage("/api/v1/training-batches");
  }

  @Test
  @DisplayName("1.7 TrainingEnrollment listing returns ApiResponse<PagedResponse<TrainingEnrollmentResponse>> with default page=0, size=20")
  void testTrainingEnrollmentDefaultPagination() throws Exception {
    assertEndpointReturnsValidPage("/api/v1/training-enrollments");
  }

  private void assertEndpointReturnsValidPage(String endpoint) throws Exception {
    MvcResult result = mockMvc.perform(get(endpoint)
            .header("Authorization", fullAdminToken)
            .contentType(MediaType.APPLICATION_JSON))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.success").value(true))
        .andExpect(jsonPath("$.data.page").value(0))
        .andExpect(jsonPath("$.data.size").value(20))
        .andExpect(jsonPath("$.data.totalElements").isNumber())
        .andExpect(jsonPath("$.data.totalPages").isNumber())
        .andExpect(jsonPath("$.data.first").isBoolean())
        .andExpect(jsonPath("$.data.last").isBoolean())
        .andExpect(jsonPath("$.data.hasNext").isBoolean())
        .andExpect(jsonPath("$.data.hasPrevious").isBoolean())
        .andExpect(jsonPath("$.data.content").isArray())
        .andReturn();

    JsonNode data = objectMapper.readTree(result.getResponse().getContentAsString()).get("data");
    assertThat(data.get("page").asInt()).isEqualTo(0);
    assertThat(data.get("size").asInt()).isEqualTo(20);
    assertThat(data.get("totalElements").asLong()).isGreaterThanOrEqualTo(1);
    assertThat(data.get("content").size()).isGreaterThanOrEqualTo(1);
  }

  // =========================================================================
  // 2. Custom Page Number and Page Size Bounds & Enforcement
  // =========================================================================

  @Test
  @DisplayName("2.1 Custom page and size parameters are respected")
  void testCustomPageAndSize() throws Exception {
    mockMvc.perform(get("/api/v1/courses?page=0&size=5")
            .header("Authorization", fullAdminToken))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.page").value(0))
        .andExpect(jsonPath("$.data.size").value(5));
  }

  @Test
  @DisplayName("2.2 Maximum permitted page size (100) is enforced; size=101 returns HTTP 400 INVALID_PAGE_SIZE")
  void testMaxPageSizeEnforced() throws Exception {
    mockMvc.perform(get("/api/v1/trainees?size=101")
            .header("Authorization", fullAdminToken))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("INVALID_PAGE_SIZE"))
        .andExpect(jsonPath("$.message").value("Page size must not exceed 100"));
  }

  @Test
  @DisplayName("2.3 Negative page index (page=-1) returns HTTP 400 INVALID_PAGE")
  void testNegativePageIndexRejected() throws Exception {
    mockMvc.perform(get("/api/v1/employers?page=-1")
            .header("Authorization", fullAdminToken))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("INVALID_PAGE"))
        .andExpect(jsonPath("$.message").value("Page index must not be less than zero"));
  }

  @Test
  @DisplayName("2.4 Zero page size (size=0) returns HTTP 400 INVALID_PAGE_SIZE")
  void testZeroPageSizeRejected() throws Exception {
    mockMvc.perform(get("/api/v1/courses?size=0")
            .header("Authorization", fullAdminToken))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("INVALID_PAGE_SIZE"))
        .andExpect(jsonPath("$.message").value("Page size must be at least 1"));
  }

  // =========================================================================
  // 3. Sorting Allowlists, Direction Handling & Stable Tie-Breaking
  // =========================================================================

  @Test
  @DisplayName("3.1 Sorting by approved field with ASC and DESC direction is supported")
  void testApprovedSortingAndDirection() throws Exception {
    mockMvc.perform(get("/api/v1/courses?sort=courseCode&direction=DESC")
            .header("Authorization", fullAdminToken))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.success").value(true));

    mockMvc.perform(get("/api/v1/courses?sort=name&direction=ASC")
            .header("Authorization", fullAdminToken))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.success").value(true));
  }

  @Test
  @DisplayName("3.2 Unsupported sort field returns HTTP 400 INVALID_SORT_FIELD")
  void testUnsupportedSortFieldRejected() throws Exception {
    mockMvc.perform(get("/api/v1/trainees?sort=maliciousProperty")
            .header("Authorization", fullAdminToken))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("INVALID_SORT_FIELD"))
        .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("Unsupported sort field 'maliciousProperty'")));
  }

  @Test
  @DisplayName("3.3 Invalid sort direction returns HTTP 400 INVALID_SORT_DIRECTION")
  void testInvalidSortDirectionRejected() throws Exception {
    mockMvc.perform(get("/api/v1/employers?direction=DIAGONAL")
            .header("Authorization", fullAdminToken))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("INVALID_SORT_DIRECTION"))
        .andExpect(jsonPath("$.message").value("Invalid sort direction 'DIAGONAL'. Must be ASC or DESC"));
  }

  @Test
  @DisplayName("3.4 Stable sorting enforces tie-breaking by id ASC on non-id sort fields without duplicating id")
  void testStableSortingTieBreaker() {
    Pageable pageable = PaginationUtils.buildPageable(
        0, 10, "courseName", "DESC", PaginationUtils.COURSE_SORT_FIELDS, "id"
    );
    Sort sort = pageable.getSort();
    assertThat(sort.isSorted()).isTrue();

    // Verify primary sort order
    Sort.Order primaryOrder = sort.getOrderFor("courseName");
    assertThat(primaryOrder).isNotNull();
    assertThat(primaryOrder.getDirection()).isEqualTo(Sort.Direction.DESC);

    // Verify secondary tie-breaker on id ASC
    Sort.Order tieBreaker = sort.getOrderFor("id");
    assertThat(tieBreaker).isNotNull();
    assertThat(tieBreaker.getDirection()).isEqualTo(Sort.Direction.ASC);

    // Verify that sorting directly on "id" does not duplicate the tie-breaker
    Pageable idPageable = PaginationUtils.buildPageable(
        0, 10, "id", "DESC", PaginationUtils.COURSE_SORT_FIELDS, "id"
    );
    assertThat(idPageable.getSort().stream().count()).isEqualTo(1);
    assertThat(idPageable.getSort().getOrderFor("id").getDirection()).isEqualTo(Sort.Direction.DESC);
  }

  // =========================================================================
  // 4. Filtering Combined with Pagination
  // =========================================================================

  @Test
  @DisplayName("4.1 Filter endpoints on Employer combine safely with pagination")
  void testEmployerFilterPagination() throws Exception {
    mockMvc.perform(get("/api/v1/employers?districtId=9971&page=0&size=10&sort=id&direction=ASC")
            .header("Authorization", fullAdminToken))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.page").value(0))
        .andExpect(jsonPath("$.data.size").value(10))
        .andExpect(jsonPath("$.data.content[0].id").value(9971));
  }

  @Test
  @DisplayName("4.2 Filter endpoints on TrainingEnrollment combine safely with pagination")
  void testTrainingEnrollmentFilterPagination() throws Exception {
    mockMvc.perform(get("/api/v1/training-enrollments?batchId=9971&page=0&size=5")
            .header("Authorization", fullAdminToken))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.page").value(0))
        .andExpect(jsonPath("$.data.size").value(5))
        .andExpect(jsonPath("$.data.content[0].id").value(9971));
  }

  // =========================================================================
  // 5. Strict Soft-Delete Filtering Verification across Content & Count Queries
  // =========================================================================

  @Test
  @DisplayName("5.1 Course soft-deleted records are strictly excluded from content, totalElements, and totalPages")
  void testStrictSoftDeletePreservation() {
    Pageable pageable = PageRequest.of(0, 50, Sort.by("id"));

    // Query active records before mutation
    Page<Course> activeBefore = courseRepository.findByDeletedAtIsNull(pageable);
    long totalBefore = activeBefore.getTotalElements();
    assertThat(totalBefore)
        .as("Precondition: active courses count must be at least 1")
        .isGreaterThanOrEqualTo(1);

    List<Long> idsBefore = activeBefore.getContent().stream().map(Course::getId).toList();
    assertThat(idsBefore)
        .as("Active Course 9971 must be present, and soft-deleted Course 9972 must be absent")
        .contains(9971L)
        .doesNotContain(9972L);

    // Soft delete the active course fixture
    Course sample = courseRepository.findById(9971L)
        .orElseThrow(() -> new IllegalStateException("Fixture Course 9971 not found"));
    sample.setDeletedAt(LocalDateTime.now());
    courseRepository.saveAndFlush(sample);

    // Query active records again
    Page<Course> activeAfter = courseRepository.findByDeletedAtIsNull(pageable);
    long totalAfter = activeAfter.getTotalElements();

    // Assert that totalElements decreased by exactly 1
    assertThat(totalAfter)
        .as("Soft-deleted course must not be counted in totalElements")
        .isEqualTo(totalBefore - 1);

    // Assert that neither 9971 nor 9972 appears in active paginated content
    List<Long> idsAfter = activeAfter.getContent().stream().map(Course::getId).toList();
    assertThat(idsAfter)
        .as("Soft-deleted course must not appear in paginated content")
        .doesNotContain(9971L)
        .doesNotContain(9972L);

    // Assert findAll includes both 9971 and 9972
    Page<Course> allCourses = courseRepository.findAll(pageable);
    List<Long> allIds = allCourses.getContent().stream().map(Course::getId).toList();
    assertThat(allIds).contains(9971L, 9972L);
  }

  @Test
  @DisplayName("5.2 Repository count query matches content count on paginated queries")
  void testRepositoryDataAndCountQueryConsistency() {
    Pageable pageable = PageRequest.of(0, 10, Sort.by("id"));

    Page<Trainee> traineePage = traineeRepository.findByDeletedAtIsNull(pageable);
    assertThat(traineePage.getTotalElements())
        .as("Total elements must be >= 1 and >= content size")
        .isGreaterThanOrEqualTo(1)
        .isGreaterThanOrEqualTo(traineePage.getContent().size());

    Page<Employer> employerPage = employerRepository.findByDeletedAtIsNull(pageable);
    assertThat(employerPage.getTotalElements())
        .as("Employer total elements must be >= 1 and >= content size")
        .isGreaterThanOrEqualTo(1)
        .isGreaterThanOrEqualTo(employerPage.getContent().size());

    Page<TrainingBatch> batchPage = trainingBatchRepository.findByDeletedAtIsNull(pageable);
    assertThat(batchPage.getTotalElements())
        .as("TrainingBatch total elements must be >= 1 and >= content size")
        .isGreaterThanOrEqualTo(1)
        .isGreaterThanOrEqualTo(batchPage.getContent().size());

    Page<TrainingCenter> centerPage = trainingCenterRepository.findByDeletedAtIsNull(pageable);
    assertThat(centerPage.getTotalElements())
        .as("TrainingCenter total elements must be >= 1 and >= content size")
        .isGreaterThanOrEqualTo(1)
        .isGreaterThanOrEqualTo(centerPage.getContent().size());

    Page<TrainingProvider> providerPage = trainingProviderRepository.findByDeletedAtIsNull(pageable);
    assertThat(providerPage.getTotalElements())
        .as("TrainingProvider total elements must be >= 1 and >= content size")
        .isGreaterThanOrEqualTo(1)
        .isGreaterThanOrEqualTo(providerPage.getContent().size());

    Page<TrainingEnrollment> enrollmentPage = trainingEnrollmentRepository.findByDeletedAtIsNull(pageable);
    assertThat(enrollmentPage.getTotalElements())
        .as("TrainingEnrollment total elements must be >= 1 and >= content size")
        .isGreaterThanOrEqualTo(1)
        .isGreaterThanOrEqualTo(enrollmentPage.getContent().size());
  }

  @Test
  @DisplayName("5.3 Employer includeDeleted behavior: soft-deleted employers included when includeDeleted=true, excluded otherwise")
  void testEmployerIncludeDeletedBehavior() throws Exception {
    // 1. With includeDeleted=true: both active (9971) and soft-deleted (9972) employers MUST be present
    MvcResult resultWithDeleted = mockMvc.perform(get("/api/v1/employers?includeDeleted=true")
            .header("Authorization", fullAdminToken))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.content").isArray())
        .andReturn();
    JsonNode contentWithDeleted = objectMapper.readTree(resultWithDeleted.getResponse().getContentAsString())
        .get("data").get("content");
    List<Long> idsWithDeleted = new ArrayList<>();
    for (JsonNode node : contentWithDeleted) {
      idsWithDeleted.add(node.get("id").asLong());
    }
    assertThat(idsWithDeleted)
        .as("Both active and soft-deleted employers must appear when includeDeleted=true")
        .contains(9971L, 9972L);

    // 2. With includeDeleted=false: active (9971) MUST be present, soft-deleted (9972) MUST NOT be present
    MvcResult resultWithoutDeleted = mockMvc.perform(get("/api/v1/employers?includeDeleted=false")
            .header("Authorization", fullAdminToken))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.content").isArray())
        .andReturn();
    JsonNode contentWithoutDeleted = objectMapper.readTree(resultWithoutDeleted.getResponse().getContentAsString())
        .get("data").get("content");
    List<Long> idsWithoutDeleted = new ArrayList<>();
    for (JsonNode node : contentWithoutDeleted) {
      idsWithoutDeleted.add(node.get("id").asLong());
    }
    assertThat(idsWithoutDeleted)
        .as("Active employer must appear when includeDeleted=false")
        .contains(9971L);
    assertThat(idsWithoutDeleted)
        .as("Soft-deleted employer must NOT appear when includeDeleted=false")
        .doesNotContain(9972L);

    // 3. Normal /api/v1/employers (omitting includeDeleted): active (9971) MUST be present, soft-deleted (9972) MUST NOT be present
    MvcResult resultNormal = mockMvc.perform(get("/api/v1/employers")
            .header("Authorization", fullAdminToken))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.content").isArray())
        .andReturn();
    JsonNode contentNormal = objectMapper.readTree(resultNormal.getResponse().getContentAsString())
        .get("data").get("content");
    List<Long> idsNormal = new ArrayList<>();
    for (JsonNode node : contentNormal) {
      idsNormal.add(node.get("id").asLong());
    }
    assertThat(idsNormal)
        .as("Active employer must appear in default listing")
        .contains(9971L);
    assertThat(idsNormal)
        .as("Soft-deleted employer must NOT appear in default listing")
        .doesNotContain(9972L);
  }

  @Test
  @DisplayName("5.4 Strict soft-delete filtering and dynamic mutation verification across all seven domain repositories")
  void testStrictSoftDeleteAcrossAllSevenRepositories() {
    Pageable pageable = PageRequest.of(0, 50, Sort.by("id"));

    // 1. Trainee: verify initial exclusion, mutate active fixture, verify count decrement & exclusion
    Page<Trainee> traineeBefore = traineeRepository.findByDeletedAtIsNull(pageable);
    long traineeTotalBefore = traineeBefore.getTotalElements();
    assertThat(traineeBefore.getContent().stream().map(Trainee::getId).toList())
        .as("Precondition: Trainee 9971 active, 9972 deleted")
        .contains(9971L).doesNotContain(9972L);
    Trainee trainee = traineeRepository.findById(9971L).orElseThrow();
    trainee.setDeletedAt(LocalDateTime.now());
    traineeRepository.saveAndFlush(trainee);
    Page<Trainee> traineeAfter = traineeRepository.findByDeletedAtIsNull(pageable);
    assertThat(traineeAfter.getTotalElements()).isEqualTo(traineeTotalBefore - 1);
    assertThat(traineeAfter.getContent().stream().map(Trainee::getId).toList())
        .doesNotContain(9971L, 9972L);

    // 2. Employer
    Page<Employer> employerBefore = employerRepository.findByDeletedAtIsNull(pageable);
    long employerTotalBefore = employerBefore.getTotalElements();
    assertThat(employerBefore.getContent().stream().map(Employer::getId).toList())
        .as("Precondition: Employer 9971 active, 9972 deleted")
        .contains(9971L).doesNotContain(9972L);
    Employer employer = employerRepository.findById(9971L).orElseThrow();
    employer.setDeletedAt(LocalDateTime.now());
    employerRepository.saveAndFlush(employer);
    Page<Employer> employerAfter = employerRepository.findByDeletedAtIsNull(pageable);
    assertThat(employerAfter.getTotalElements()).isEqualTo(employerTotalBefore - 1);
    assertThat(employerAfter.getContent().stream().map(Employer::getId).toList())
        .doesNotContain(9971L, 9972L);

    // 3. Course
    Page<Course> courseBefore = courseRepository.findByDeletedAtIsNull(pageable);
    long courseTotalBefore = courseBefore.getTotalElements();
    assertThat(courseBefore.getContent().stream().map(Course::getId).toList())
        .as("Precondition: Course 9971 active, 9972 deleted")
        .contains(9971L).doesNotContain(9972L);
    Course course = courseRepository.findById(9971L).orElseThrow();
    course.setDeletedAt(LocalDateTime.now());
    courseRepository.saveAndFlush(course);
    Page<Course> courseAfter = courseRepository.findByDeletedAtIsNull(pageable);
    assertThat(courseAfter.getTotalElements()).isEqualTo(courseTotalBefore - 1);
    assertThat(courseAfter.getContent().stream().map(Course::getId).toList())
        .doesNotContain(9971L, 9972L);

    // 4. TrainingCenter
    Page<TrainingCenter> centerBefore = trainingCenterRepository.findByDeletedAtIsNull(pageable);
    long centerTotalBefore = centerBefore.getTotalElements();
    assertThat(centerBefore.getContent().stream().map(TrainingCenter::getId).toList())
        .as("Precondition: Center 9971 active, 9972 deleted")
        .contains(9971L).doesNotContain(9972L);
    TrainingCenter center = trainingCenterRepository.findById(9971L).orElseThrow();
    center.setDeletedAt(LocalDateTime.now());
    trainingCenterRepository.saveAndFlush(center);
    Page<TrainingCenter> centerAfter = trainingCenterRepository.findByDeletedAtIsNull(pageable);
    assertThat(centerAfter.getTotalElements()).isEqualTo(centerTotalBefore - 1);
    assertThat(centerAfter.getContent().stream().map(TrainingCenter::getId).toList())
        .doesNotContain(9971L, 9972L);

    // 5. TrainingProvider
    Page<TrainingProvider> providerBefore = trainingProviderRepository.findByDeletedAtIsNull(pageable);
    long providerTotalBefore = providerBefore.getTotalElements();
    assertThat(providerBefore.getContent().stream().map(TrainingProvider::getId).toList())
        .as("Precondition: Provider 9971 active, 9972 deleted")
        .contains(9971L).doesNotContain(9972L);
    TrainingProvider provider = trainingProviderRepository.findById(9971L).orElseThrow();
    provider.setDeletedAt(LocalDateTime.now());
    trainingProviderRepository.saveAndFlush(provider);
    Page<TrainingProvider> providerAfter = trainingProviderRepository.findByDeletedAtIsNull(pageable);
    assertThat(providerAfter.getTotalElements()).isEqualTo(providerTotalBefore - 1);
    assertThat(providerAfter.getContent().stream().map(TrainingProvider::getId).toList())
        .doesNotContain(9971L, 9972L);

    // 6. TrainingBatch
    Page<TrainingBatch> batchBefore = trainingBatchRepository.findByDeletedAtIsNull(pageable);
    long batchTotalBefore = batchBefore.getTotalElements();
    assertThat(batchBefore.getContent().stream().map(TrainingBatch::getId).toList())
        .as("Precondition: Batch 9971 active, 9972 deleted")
        .contains(9971L).doesNotContain(9972L);
    TrainingBatch batch = trainingBatchRepository.findById(9971L).orElseThrow();
    batch.setDeletedAt(LocalDateTime.now());
    trainingBatchRepository.saveAndFlush(batch);
    Page<TrainingBatch> batchAfter = trainingBatchRepository.findByDeletedAtIsNull(pageable);
    assertThat(batchAfter.getTotalElements()).isEqualTo(batchTotalBefore - 1);
    assertThat(batchAfter.getContent().stream().map(TrainingBatch::getId).toList())
        .doesNotContain(9971L, 9972L);

    // 7. TrainingEnrollment
    Page<TrainingEnrollment> enrollmentBefore = trainingEnrollmentRepository.findByDeletedAtIsNull(pageable);
    long enrollmentTotalBefore = enrollmentBefore.getTotalElements();
    assertThat(enrollmentBefore.getContent().stream().map(TrainingEnrollment::getId).toList())
        .as("Precondition: Enrollment 9971 active, 9972 deleted")
        .contains(9971L).doesNotContain(9972L);
    TrainingEnrollment enrollment = trainingEnrollmentRepository.findById(9971L).orElseThrow();
    enrollment.setDeletedAt(LocalDateTime.now());
    trainingEnrollmentRepository.saveAndFlush(enrollment);
    Page<TrainingEnrollment> enrollmentAfter = trainingEnrollmentRepository.findByDeletedAtIsNull(pageable);
    assertThat(enrollmentAfter.getTotalElements()).isEqualTo(enrollmentTotalBefore - 1);
    assertThat(enrollmentAfter.getContent().stream().map(TrainingEnrollment::getId).toList())
        .doesNotContain(9971L, 9972L);
  }

  // =========================================================================
  // 6. Security and Authorization Preservation
  // =========================================================================

  @Test
  @DisplayName("6.1 Unauthenticated requests to paginated endpoints return HTTP 401 Unauthorized")
  void testUnauthenticatedPaginatedRequestRejected() throws Exception {
    mockMvc.perform(get("/api/v1/trainees"))
        .andExpect(status().isUnauthorized());

    mockMvc.perform(get("/api/v1/employers"))
        .andExpect(status().isUnauthorized());
  }

  @Test
  @DisplayName("6.2 Authenticated requests with insufficient authorities return HTTP 403 Forbidden")
  void testInsufficientAuthorityPaginatedRequestForbidden() throws Exception {
    mockMvc.perform(get("/api/v1/trainees")
            .header("Authorization", guestToken))
        .andExpect(status().isForbidden());

    mockMvc.perform(get("/api/v1/employers")
            .header("Authorization", guestToken))
        .andExpect(status().isForbidden());
  }

  // =========================================================================
  // 7. Single-Record Lookups Preservation (Unpaginated Contract)
  // =========================================================================

  @Test
  @DisplayName("7.1 Existing single-record lookup endpoints return unpaginated ApiResponse<T>")
  void testSingleRecordLookupsRemainUnpaginated() throws Exception {
    // 1. Course lookup by ID (9971) and code (CRS-PAG-9971)
    mockMvc.perform(get("/api/v1/courses/9971")
            .header("Authorization", fullAdminToken))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.success").value(true))
        .andExpect(jsonPath("$.data.id").value(9971))
        .andExpect(jsonPath("$.data.courseCode").value("CRS-PAG-9971"))
        .andExpect(jsonPath("$.data.content").doesNotExist())
        .andExpect(jsonPath("$.data.totalElements").doesNotExist());

    mockMvc.perform(get("/api/v1/courses?code=CRS-PAG-9971")
            .header("Authorization", fullAdminToken))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.success").value(true))
        .andExpect(jsonPath("$.data.id").value(9971))
        .andExpect(jsonPath("$.data.content").doesNotExist());

    // 2. Employer lookup by ID (9971) and code (EMP-PAG-9971)
    mockMvc.perform(get("/api/v1/employers/9971")
            .header("Authorization", fullAdminToken))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.success").value(true))
        .andExpect(jsonPath("$.data.id").value(9971))
        .andExpect(jsonPath("$.data.employerCode").value("EMP-PAG-9971"))
        .andExpect(jsonPath("$.data.content").doesNotExist())
        .andExpect(jsonPath("$.data.totalElements").doesNotExist());

    mockMvc.perform(get("/api/v1/employers?code=EMP-PAG-9971")
            .header("Authorization", fullAdminToken))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.success").value(true))
        .andExpect(jsonPath("$.data.id").value(9971))
        .andExpect(jsonPath("$.data.content").doesNotExist());

    // 3. Trainee lookup by ID (9971) and registrationNumber (REG-PAG-9971)
    mockMvc.perform(get("/api/v1/trainees/9971")
            .header("Authorization", fullAdminToken))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.success").value(true))
        .andExpect(jsonPath("$.data.id").value(9971))
        .andExpect(jsonPath("$.data.registrationNumber").value("REG-PAG-9971"))
        .andExpect(jsonPath("$.data.content").doesNotExist())
        .andExpect(jsonPath("$.data.totalElements").doesNotExist());

    mockMvc.perform(get("/api/v1/trainees?registrationNumber=REG-PAG-9971")
            .header("Authorization", fullAdminToken))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.success").value(true))
        .andExpect(jsonPath("$.data.id").value(9971))
        .andExpect(jsonPath("$.data.content").doesNotExist());

    // 4. TrainingBatch lookup by ID (9971) and code (BAT-PAG-9971)
    mockMvc.perform(get("/api/v1/training-batches/9971")
            .header("Authorization", fullAdminToken))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.success").value(true))
        .andExpect(jsonPath("$.data.id").value(9971))
        .andExpect(jsonPath("$.data.batchCode").value("BAT-PAG-9971"))
        .andExpect(jsonPath("$.data.content").doesNotExist())
        .andExpect(jsonPath("$.data.totalElements").doesNotExist());

    mockMvc.perform(get("/api/v1/training-batches?code=BAT-PAG-9971")
            .header("Authorization", fullAdminToken))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.success").value(true))
        .andExpect(jsonPath("$.data.id").value(9971))
        .andExpect(jsonPath("$.data.content").doesNotExist());

    // 5. TrainingCenter lookup by ID (9971) and code (CTR-PAG-9971)
    mockMvc.perform(get("/api/v1/training-centers/9971")
            .header("Authorization", fullAdminToken))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.success").value(true))
        .andExpect(jsonPath("$.data.id").value(9971))
        .andExpect(jsonPath("$.data.centerCode").value("CTR-PAG-9971"))
        .andExpect(jsonPath("$.data.content").doesNotExist())
        .andExpect(jsonPath("$.data.totalElements").doesNotExist());

    mockMvc.perform(get("/api/v1/training-centers?code=CTR-PAG-9971")
            .header("Authorization", fullAdminToken))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.success").value(true))
        .andExpect(jsonPath("$.data.id").value(9971))
        .andExpect(jsonPath("$.data.content").doesNotExist());

    // 6. TrainingProvider lookup by ID (9971) and code (PRV-PAG-9971)
    mockMvc.perform(get("/api/v1/training-providers/9971")
            .header("Authorization", fullAdminToken))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.success").value(true))
        .andExpect(jsonPath("$.data.id").value(9971))
        .andExpect(jsonPath("$.data.providerCode").value("PRV-PAG-9971"))
        .andExpect(jsonPath("$.data.content").doesNotExist())
        .andExpect(jsonPath("$.data.totalElements").doesNotExist());

    mockMvc.perform(get("/api/v1/training-providers?code=PRV-PAG-9971")
            .header("Authorization", fullAdminToken))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.success").value(true))
        .andExpect(jsonPath("$.data.id").value(9971))
        .andExpect(jsonPath("$.data.content").doesNotExist());

    // 7. TrainingEnrollment lookup by ID (9971) and enrollmentNumber (ENR-PAG-9971)
    mockMvc.perform(get("/api/v1/training-enrollments/9971")
            .header("Authorization", fullAdminToken))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.success").value(true))
        .andExpect(jsonPath("$.data.id").value(9971))
        .andExpect(jsonPath("$.data.enrollmentNumber").value("ENR-PAG-9971"))
        .andExpect(jsonPath("$.data.content").doesNotExist())
        .andExpect(jsonPath("$.data.totalElements").doesNotExist());

    mockMvc.perform(get("/api/v1/training-enrollments?enrollmentNumber=ENR-PAG-9971")
            .header("Authorization", fullAdminToken))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.success").value(true))
        .andExpect(jsonPath("$.data.id").value(9971))
        .andExpect(jsonPath("$.data.content").doesNotExist());
  }

  // =========================================================================
  // 8. Empty Results, Out-of-Range Pages & Metadata Correctness
  // =========================================================================

  @Test
  @DisplayName("8.1 Request for page beyond last available page returns HTTP 200 with empty content and accurate metadata")
  void testOutOfRangePageReturnsEmptyContent() throws Exception {
    mockMvc.perform(get("/api/v1/courses?page=9999&size=10")
            .header("Authorization", fullAdminToken))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.success").value(true))
        .andExpect(jsonPath("$.data.content").isArray())
        .andExpect(jsonPath("$.data.content").isEmpty())
        .andExpect(jsonPath("$.data.page").value(9999))
        .andExpect(jsonPath("$.data.size").value(10))
        .andExpect(jsonPath("$.data.first").value(false))
        .andExpect(jsonPath("$.data.last").value(true))
        .andExpect(jsonPath("$.data.hasNext").value(false))
        .andExpect(jsonPath("$.data.hasPrevious").value(true));
  }

  @Test
  @DisplayName("8.2 Empty filter result returns HTTP 200 with empty content, zero totals, and valid boundary metadata")
  void testEmptyFilterResultMetadata() throws Exception {
    mockMvc.perform(get("/api/v1/courses?sectorId=9999999")
            .header("Authorization", fullAdminToken))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.success").value(true))
        .andExpect(jsonPath("$.data.content").isArray())
        .andExpect(jsonPath("$.data.content").isEmpty())
        .andExpect(jsonPath("$.data.totalElements").value(0))
        .andExpect(jsonPath("$.data.totalPages").value(0))
        .andExpect(jsonPath("$.data.first").value(true))
        .andExpect(jsonPath("$.data.last").value(true))
        .andExpect(jsonPath("$.data.hasNext").value(false))
        .andExpect(jsonPath("$.data.hasPrevious").value(false));
  }

  @Test
  @DisplayName("8.3 Pagination metadata fields are complete and strictly validated")
  void testPaginationMetadataCorrectness() throws Exception {
    MvcResult result = mockMvc.perform(get("/api/v1/courses?page=0&size=2")
            .header("Authorization", fullAdminToken))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.page").value(0))
        .andExpect(jsonPath("$.data.size").value(2))
        .andExpect(jsonPath("$.data.first").value(true))
        .andExpect(jsonPath("$.data.hasPrevious").value(false))
        .andReturn();

    JsonNode data = objectMapper.readTree(result.getResponse().getContentAsString()).get("data");
    long totalElements = data.get("totalElements").asLong();
    int totalPages = data.get("totalPages").asInt();
    boolean last = data.get("last").asBoolean();
    boolean hasNext = data.get("hasNext").asBoolean();

    int expectedPages = (int) Math.ceil((double) totalElements / 2.0);
    assertThat(totalPages).isEqualTo(expectedPages);
    if (totalElements > 2) {
      assertThat(last).isFalse();
      assertThat(hasNext).isTrue();
    } else {
      assertThat(last).isTrue();
      assertThat(hasNext).isFalse();
    }
  }
}
