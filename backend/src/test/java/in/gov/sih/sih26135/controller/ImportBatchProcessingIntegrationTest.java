package in.gov.sih.sih26135.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import in.gov.sih.sih26135.MysqlConnectionTest;
import in.gov.sih.sih26135.config.ImportPipelineProperties;
import in.gov.sih.sih26135.exception.GlobalExceptionHandler;
import in.gov.sih.sih26135.security.jwt.JwtTokenProvider;
import in.gov.sih.sih26135.service.ingestion.ImportFileStagingService;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import javax.sql.DataSource;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.MultipartException;

@SpringBootTest
@AutoConfigureMockMvc
@DisplayName("Phase 28.5 - Bulk Data Ingestion, Quarantine & Chunked Batch Processing Integration Tests")
class ImportBatchProcessingIntegrationTest {

  private static final long STATE_A_ID = 9941L;
  private static final long STATE_B_ID = 9942L;
  private static final long DISTRICT_A1_ID = 9941L;
  private static final long DISTRICT_B1_ID = 9942L;
  private static final long DISTRICT_A2_ID = 9943L;

  private static final long SECTOR_ID = 9941L;
  private static final long COURSE_ID = 9941L;
  private static final long ORG_ID = 9941L;
  private static final long DEPT_ID = 9941L;
  private static final long SCHEME_ID = 9941L;
  private static final long PROGRAM_ID = 9941L;

  private static final long PROVIDER_A_ID = 9941L;
  private static final long PROVIDER_B_ID = 9942L;
  private static final long EMPLOYER_A_ID = 9941L;
  private static final long EMPLOYER_B_ID = 9942L;
  private static final long CENTER_A_ID = 9941L;
  private static final long CENTER_B_ID = 9942L;
  private static final long BATCH_A_ID = 9941L;
  private static final long BATCH_B_ID = 9942L;

  private static final long SEED_TRAINEE_A_ID = 9941L;
  private static final long SEED_TRAINEE_B_ID = 9942L;
  private static final long SEED_TRAINEE_USER_ROW_ID = 9943L;
  private static final long ENROLLMENT_A_ID = 9941L;
  private static final long ENROLLMENT_B_ID = 9942L;
  private static final long SEED_PLACEMENT_A_ID = 9941L;

  private static final long USER_SUPER_ADMIN_ID = 9940L;
  private static final long USER_STATE_A_ID = 9941L;
  private static final long USER_STATE_B_ID = 9942L;
  private static final long USER_DISTRICT_A1_ID = 9943L;
  private static final long USER_DISTRICT_A2_ID = 9944L;
  private static final long USER_PROVIDER_A_ID = 9945L;
  private static final long USER_PROVIDER_B_ID = 9946L;
  private static final long USER_EMPLOYER_A_ID = 9947L;
  private static final long USER_TRAINEE_ID = 9948L;
  private static final long USER_AUDITOR_A_ID = 9949L;
  private static final long USER_UNSCOPED_ID = 99499L;

  @Autowired
  private DataSource dataSource;

  @Autowired
  private MockMvc mockMvc;

  @Autowired
  private ObjectMapper objectMapper;

  @Autowired
  private JwtTokenProvider jwtTokenProvider;

  @Autowired
  private JdbcTemplate jdbcTemplate;

  @Autowired
  private ImportPipelineProperties properties;

  @Autowired
  private ImportFileStagingService fileStagingService;

  @Autowired
  private GlobalExceptionHandler globalExceptionHandler;

  private String tokenSuperAdmin;
  private String tokenStateA;
  private String tokenStateB;
  private String tokenDistrictA1;
  private String tokenDistrictA2;
  private String tokenProviderA;
  private String tokenProviderB;
  private String tokenEmployerA;
  private String tokenTrainee;
  private String tokenAuditorA;
  private String tokenUnscoped;

  private long originalMaxFileSizeBytes;
  private int originalChunkSize;

  @BeforeEach
  void setUp() throws Exception {
    try (var conn = dataSource.getConnection()) {
      String catalog = conn.getCatalog();
      MysqlConnectionTest.assertDedicatedTestCatalog(catalog);
      assertThat(catalog).isEqualToIgnoringCase(MysqlConnectionTest.EXPECTED_TEST_CATALOG);
    }

    originalMaxFileSizeBytes = properties.getMaxFileSizeBytes();
    originalChunkSize = properties.getChunkSize();
    cleanUpTestData();

    // Geographic hierarchy
    jdbcTemplate.update(
        "INSERT INTO states (state_id, state_code, state_name, lifecycle_status_id, created_at, updated_at) "
            + "VALUES (?, 'I1', 'Import State A', 1, NOW(), NOW())", STATE_A_ID);
    jdbcTemplate.update(
        "INSERT INTO states (state_id, state_code, state_name, lifecycle_status_id, created_at, updated_at) "
            + "VALUES (?, 'I2', 'Import State B', 1, NOW(), NOW())", STATE_B_ID);
    jdbcTemplate.update(
        "INSERT INTO districts (district_id, state_id, district_code, district_name, lifecycle_status_id, created_at, updated_at) "
            + "VALUES (?, ?, 'ID1', 'Import District A1', 1, NOW(), NOW())", DISTRICT_A1_ID, STATE_A_ID);
    jdbcTemplate.update(
        "INSERT INTO districts (district_id, state_id, district_code, district_name, lifecycle_status_id, created_at, updated_at) "
            + "VALUES (?, ?, 'ID2', 'Import District B1', 1, NOW(), NOW())", DISTRICT_B1_ID, STATE_B_ID);
    jdbcTemplate.update(
        "INSERT INTO districts (district_id, state_id, district_code, district_name, lifecycle_status_id, created_at, updated_at) "
            + "VALUES (?, ?, 'ID3', 'Import District A2', 1, NOW(), NOW())", DISTRICT_A2_ID, STATE_A_ID);

    // Program & Course hierarchy
    jdbcTemplate.update(
        "INSERT INTO sectors (sector_id, sector_code, sector_name, lifecycle_status_id, created_at, updated_at) "
            + "VALUES (?, 'SEC-IMP-9941', 'Import Sector', 1, NOW(), NOW())", SECTOR_ID);
    jdbcTemplate.update(
        "INSERT INTO courses (course_id, course_code, course_name, sector_id, delivery_mode_id, lifecycle_status_id, created_at, updated_at, deleted_at) "
            + "VALUES (?, 'CRS-IMP-9941', 'Import Course', ?, 1, 1, NOW(), NOW(), NULL)", COURSE_ID, SECTOR_ID);
    jdbcTemplate.update(
        "INSERT INTO organizations (organization_id, organization_code, organization_name, organization_type_id, lifecycle_status_id, created_at, updated_at) "
            + "VALUES (?, 'ORG-IMP-9941', 'Import Org', 1, 1, NOW(), NOW())", ORG_ID);
    jdbcTemplate.update(
        "INSERT INTO departments (department_id, organization_id, department_code, department_name, lifecycle_status_id, created_at, updated_at) "
            + "VALUES (?, ?, 'DEP-IMP-9941', 'Import Dept', 1, NOW(), NOW())", DEPT_ID, ORG_ID);
    jdbcTemplate.update(
        "INSERT INTO schemes (scheme_id, scheme_code, scheme_name, department_id, currency_code, lifecycle_status_id, created_at, updated_at) "
            + "VALUES (?, 'SCH-IMP-9941', 'Import Scheme', ?, 'INR', 1, NOW(), NOW())", SCHEME_ID, DEPT_ID);
    jdbcTemplate.update(
        "INSERT INTO programs (program_id, program_code, program_name, department_id, scheme_id, lifecycle_status_id, created_at, updated_at) "
            + "VALUES (?, 'PRG-IMP-9941', 'Import Program', ?, ?, 1, NOW(), NOW())", PROGRAM_ID, DEPT_ID, SCHEME_ID);

    // Providers & Employers
    jdbcTemplate.update(
        "INSERT INTO training_providers (provider_id, provider_code, provider_name, registration_number, organization_type_id, state_id, district_id, accreditation_status_id, lifecycle_status_id, created_at, updated_at, deleted_at) "
            + "VALUES (?, 'PRV-IMP-9941', 'Import Provider A', 'REG-PRV-9941', 1, ?, ?, 1, 1, NOW(), NOW(), NULL)",
        PROVIDER_A_ID, STATE_A_ID, DISTRICT_A1_ID);
    jdbcTemplate.update(
        "INSERT INTO training_providers (provider_id, provider_code, provider_name, registration_number, organization_type_id, state_id, district_id, accreditation_status_id, lifecycle_status_id, created_at, updated_at, deleted_at) "
            + "VALUES (?, 'PRV-IMP-9942', 'Import Provider B', 'REG-PRV-9942', 1, ?, ?, 1, 1, NOW(), NOW(), NULL)",
        PROVIDER_B_ID, STATE_B_ID, DISTRICT_B1_ID);

    jdbcTemplate.update(
        "INSERT INTO employers (employer_id, employer_code, employer_name, registration_number, organization_type_id, state_id, district_id, record_verification_status_id, lifecycle_status_id, created_at, updated_at, deleted_at) "
            + "VALUES (?, 'EMP-IMP-9941', 'Import Employer A', 'REG-EMP-9941', 1, ?, ?, 1, 1, NOW(), NOW(), NULL)",
        EMPLOYER_A_ID, STATE_A_ID, DISTRICT_A1_ID);
    jdbcTemplate.update(
        "INSERT INTO employers (employer_id, employer_code, employer_name, registration_number, organization_type_id, state_id, district_id, record_verification_status_id, lifecycle_status_id, created_at, updated_at, deleted_at) "
            + "VALUES (?, 'EMP-IMP-9942', 'Import Employer B', 'REG-EMP-9942', 1, ?, ?, 1, 1, NOW(), NOW(), NULL)",
        EMPLOYER_B_ID, STATE_B_ID, DISTRICT_B1_ID);

    // Centers & Batches
    jdbcTemplate.update(
        "INSERT INTO training_centers (center_id, provider_id, center_code, center_name, state_id, district_id, lifecycle_status_id, created_at, updated_at, deleted_at) "
            + "VALUES (?, ?, 'CTR-IMP-9941', 'Import Center A', ?, ?, 1, NOW(), NOW(), NULL)",
        CENTER_A_ID, PROVIDER_A_ID, STATE_A_ID, DISTRICT_A1_ID);
    jdbcTemplate.update(
        "INSERT INTO training_centers (center_id, provider_id, center_code, center_name, state_id, district_id, lifecycle_status_id, created_at, updated_at, deleted_at) "
            + "VALUES (?, ?, 'CTR-IMP-9942', 'Import Center B', ?, ?, 1, NOW(), NOW(), NULL)",
        CENTER_B_ID, PROVIDER_B_ID, STATE_B_ID, DISTRICT_B1_ID);

    jdbcTemplate.update(
        "INSERT INTO training_batches (batch_id, batch_code, course_id, provider_id, center_id, program_id, start_date, capacity, batch_status_id, created_at, updated_at, deleted_at) "
            + "VALUES (?, 'BAT-IMP-9941', ?, ?, ?, ?, '2026-01-01', 30, 1, NOW(), NOW(), NULL)",
        BATCH_A_ID, COURSE_ID, PROVIDER_A_ID, CENTER_A_ID, PROGRAM_ID);
    jdbcTemplate.update(
        "INSERT INTO training_batches (batch_id, batch_code, course_id, provider_id, center_id, program_id, start_date, capacity, batch_status_id, created_at, updated_at, deleted_at) "
            + "VALUES (?, 'BAT-IMP-9942', ?, ?, ?, ?, '2026-01-01', 30, 1, NOW(), NOW(), NULL)",
        BATCH_B_ID, COURSE_ID, PROVIDER_B_ID, CENTER_B_ID, PROGRAM_ID);

    // Users
    insertUser(USER_SUPER_ADMIN_ID, "imp_superadmin", "imp9940@test.gov.in", null, null, null, null);
    insertUser(USER_STATE_A_ID, "imp_state_a", "imp9941@test.gov.in", STATE_A_ID, null, null, null);
    insertUser(USER_STATE_B_ID, "imp_state_b", "imp9942@test.gov.in", STATE_B_ID, null, null, null);
    insertUser(USER_DISTRICT_A1_ID, "imp_dist_a1", "imp9943@test.gov.in", STATE_A_ID, DISTRICT_A1_ID, null, null);
    insertUser(USER_DISTRICT_A2_ID, "imp_dist_a2", "imp9944@test.gov.in", STATE_A_ID, DISTRICT_A2_ID, null, null);
    insertUser(USER_PROVIDER_A_ID, "imp_prov_a", "imp9945@test.gov.in", null, null, PROVIDER_A_ID, null);
    insertUser(USER_PROVIDER_B_ID, "imp_prov_b", "imp9946@test.gov.in", null, null, PROVIDER_B_ID, null);
    insertUser(USER_EMPLOYER_A_ID, "imp_emp_a", "imp9947@test.gov.in", null, null, null, EMPLOYER_A_ID);
    insertUser(USER_TRAINEE_ID, "imp_trainee", "imp9948@test.gov.in", null, null, null, null);
    insertUser(USER_AUDITOR_A_ID, "imp_auditor_a", "imp9949@test.gov.in", STATE_A_ID, null, null, null);
    insertUser(USER_UNSCOPED_ID, "imp_unscoped", "imp99499@test.gov.in", null, null, null, null);

    // Authoritative super_admin role binding for nationwide admin check
    jdbcTemplate.update(
        "INSERT INTO user_roles (user_id, role_id, assigned_at) "
            + "SELECT ?, role_id, NOW() FROM roles WHERE role_code = 'super_admin' LIMIT 1",
        USER_SUPER_ADMIN_ID);

    // Seed trainees, enrollments & placement
    jdbcTemplate.update(
        "INSERT INTO trainees (trainee_id, registration_number, first_name, last_name, phone, email, date_of_birth, gender_id, state_id, district_id, current_employment_status_id, profile_status_id, created_at, updated_at, deleted_at) "
            + "VALUES (?, 'IMP285-SEED-TR-1', 'SeedAsha', 'Kiran', '9840000001', 'seed9941@test.gov.in', '2000-02-15', 1, ?, ?, 1, 1, NOW(), NOW(), NULL)",
        SEED_TRAINEE_A_ID, STATE_A_ID, DISTRICT_A1_ID);
    jdbcTemplate.update(
        "INSERT INTO trainees (trainee_id, registration_number, first_name, last_name, phone, email, date_of_birth, gender_id, state_id, district_id, current_employment_status_id, profile_status_id, created_at, updated_at, deleted_at) "
            + "VALUES (?, 'IMP285-SEED-TR-2', 'SeedBala', 'Rao', '9840000002', 'seed9942@test.gov.in', '2000-03-10', 1, ?, ?, 1, 1, NOW(), NOW(), NULL)",
        SEED_TRAINEE_B_ID, STATE_B_ID, DISTRICT_B1_ID);
    jdbcTemplate.update(
        "INSERT INTO trainees (trainee_id, user_id, registration_number, first_name, last_name, phone, email, date_of_birth, gender_id, state_id, district_id, current_employment_status_id, profile_status_id, created_at, updated_at, deleted_at) "
            + "VALUES (?, ?, 'IMP285-SEED-TR-U', 'TraineeRole', 'User', '9840000003', 'seed9943@test.gov.in', '2001-01-01', 1, ?, ?, 1, 1, NOW(), NOW(), NULL)",
        SEED_TRAINEE_USER_ROW_ID, USER_TRAINEE_ID, STATE_A_ID, DISTRICT_A1_ID);

    jdbcTemplate.update(
        "INSERT INTO training_enrollments (enrollment_id, enrollment_number, trainee_id, program_id, course_id, provider_id, center_id, batch_id, enrollment_date, enrollment_status_id, created_at, updated_at, deleted_at) "
            + "VALUES (?, 'IMP285-ENR-9941', ?, ?, ?, ?, ?, ?, '2026-01-05', 1, NOW(), NOW(), NULL)",
        ENROLLMENT_A_ID, SEED_TRAINEE_A_ID, PROGRAM_ID, COURSE_ID, PROVIDER_A_ID, CENTER_A_ID, BATCH_A_ID);
    jdbcTemplate.update(
        "INSERT INTO training_enrollments (enrollment_id, enrollment_number, trainee_id, program_id, course_id, provider_id, center_id, batch_id, enrollment_date, enrollment_status_id, created_at, updated_at, deleted_at) "
            + "VALUES (?, 'IMP285-ENR-9942', ?, ?, ?, ?, ?, ?, '2026-01-05', 1, NOW(), NOW(), NULL)",
        ENROLLMENT_B_ID, SEED_TRAINEE_B_ID, PROGRAM_ID, COURSE_ID, PROVIDER_B_ID, CENTER_B_ID, BATCH_B_ID);

    jdbcTemplate.update(
        "INSERT INTO placement_records (placement_id, placement_number, trainee_id, enrollment_id, course_id, program_id, provider_id, employer_id, engagement_type_id, placement_source_id, placement_status_id, joining_status_id, offered_salary, work_state_id, work_district_id, record_verification_status_id, created_at, updated_at, deleted_at) "
            + "VALUES (?, 'IMP285-SEED-PLC-1', ?, ?, ?, ?, ?, ?, 1, 1, 1, 1, 22000.00, ?, ?, 1, NOW(), NOW(), NULL)",
        SEED_PLACEMENT_A_ID, SEED_TRAINEE_A_ID, ENROLLMENT_A_ID, COURSE_ID, PROGRAM_ID, PROVIDER_A_ID, EMPLOYER_A_ID, STATE_A_ID, DISTRICT_A1_ID);

    // Tokens
    tokenSuperAdmin = "Bearer " + jwtTokenProvider.generateAccessToken(
        USER_SUPER_ADMIN_ID, "imp_superadmin", List.of("ROLE_super_admin", "system.manage", "audit.read", "trainee.write", "placement.manage"));
    tokenStateA = "Bearer " + jwtTokenProvider.generateAccessToken(
        USER_STATE_A_ID, "imp_state_a", List.of("ROLE_state_admin", "trainee.write", "placement.manage", "audit.read"));
    tokenStateB = "Bearer " + jwtTokenProvider.generateAccessToken(
        USER_STATE_B_ID, "imp_state_b", List.of("ROLE_state_admin", "trainee.write", "placement.manage", "audit.read"));
    tokenDistrictA1 = "Bearer " + jwtTokenProvider.generateAccessToken(
        USER_DISTRICT_A1_ID, "imp_dist_a1", List.of("ROLE_district_officer", "trainee.write", "placement.manage"));
    tokenDistrictA2 = "Bearer " + jwtTokenProvider.generateAccessToken(
        USER_DISTRICT_A2_ID, "imp_dist_a2", List.of("ROLE_district_officer", "trainee.write", "placement.manage"));
    tokenProviderA = "Bearer " + jwtTokenProvider.generateAccessToken(
        USER_PROVIDER_A_ID, "imp_prov_a", List.of("ROLE_training_provider", "trainee.write", "placement.manage", "trainee.read"));
    tokenProviderB = "Bearer " + jwtTokenProvider.generateAccessToken(
        USER_PROVIDER_B_ID, "imp_prov_b", List.of("ROLE_training_provider", "trainee.write", "placement.manage", "trainee.read"));
    tokenEmployerA = "Bearer " + jwtTokenProvider.generateAccessToken(
        USER_EMPLOYER_A_ID, "imp_emp_a", List.of("ROLE_employer", "placement.manage", "trainee.read"));
    tokenTrainee = "Bearer " + jwtTokenProvider.generateAccessToken(
        USER_TRAINEE_ID, "imp_trainee", List.of("ROLE_trainee", "trainee.write", "audit.read"));
    tokenAuditorA = "Bearer " + jwtTokenProvider.generateAccessToken(
        USER_AUDITOR_A_ID, "imp_auditor_a", List.of("ROLE_state_admin", "audit.read"));
    tokenUnscoped = "Bearer " + jwtTokenProvider.generateAccessToken(
        USER_UNSCOPED_ID, "imp_unscoped", List.of("ROLE_state_admin", "system.manage"));
  }

  @AfterEach
  void tearDown() {
    SecurityContextHolder.clearContext();
    properties.setMaxFileSizeBytes(originalMaxFileSizeBytes);
    properties.setChunkSize(originalChunkSize);
    cleanUpTestData();
  }

  private void insertUser(
      Long userId,
      String username,
      String email,
      Long stateId,
      Long districtId,
      Long providerId,
      Long employerId) {
    jdbcTemplate.update(
        "INSERT INTO users (user_id, username, email, password_hash, password_algo, user_status_id, state_id, district_id, training_provider_id, employer_id, created_at, updated_at) "
            + "VALUES (?, ?, ?, 'hash', 'argon2id', 1, ?, ?, ?, ?, NOW(), NOW())",
        userId, username, email, stateId, districtId, providerId, employerId);
  }

  private void cleanUpTestData() {
    List<String> batchCodes = jdbcTemplate.queryForList(
        "SELECT batch_code FROM import_batches WHERE (initiated_by_user_id BETWEEN 9940 AND 99499) OR source_system LIKE 'IMP285_%'",
        String.class);
    for (String code : batchCodes) {
      fileStagingService.deleteStagedFileQuietly(code);
    }

    jdbcTemplate.update(
        "DELETE FROM import_records WHERE import_batch_id IN (SELECT import_batch_id FROM import_batches WHERE (initiated_by_user_id BETWEEN 9940 AND 99499) OR source_system LIKE 'IMP285_%')");
    jdbcTemplate.update(
        "DELETE FROM import_batches WHERE (initiated_by_user_id BETWEEN 9940 AND 99499) OR source_system LIKE 'IMP285_%'");

    jdbcTemplate.update("DELETE FROM placement_records WHERE placement_number LIKE 'IMP285-%' OR placement_id = ?", SEED_PLACEMENT_A_ID);
    jdbcTemplate.update("DELETE FROM training_enrollments WHERE enrollment_id IN (?, ?)", ENROLLMENT_A_ID, ENROLLMENT_B_ID);
    jdbcTemplate.update("DELETE FROM trainees WHERE registration_number LIKE 'IMP285-%' OR trainee_id IN (?, ?, ?)",
        SEED_TRAINEE_A_ID, SEED_TRAINEE_B_ID, SEED_TRAINEE_USER_ROW_ID);

    jdbcTemplate.update(
        "DELETE FROM user_roles WHERE user_id IN (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
        USER_SUPER_ADMIN_ID, USER_STATE_A_ID, USER_STATE_B_ID, USER_DISTRICT_A1_ID, USER_DISTRICT_A2_ID,
        USER_PROVIDER_A_ID, USER_PROVIDER_B_ID, USER_EMPLOYER_A_ID, USER_TRAINEE_ID, USER_AUDITOR_A_ID, USER_UNSCOPED_ID);
    jdbcTemplate.update(
        "DELETE FROM users WHERE user_id IN (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
        USER_SUPER_ADMIN_ID, USER_STATE_A_ID, USER_STATE_B_ID, USER_DISTRICT_A1_ID, USER_DISTRICT_A2_ID,
        USER_PROVIDER_A_ID, USER_PROVIDER_B_ID, USER_EMPLOYER_A_ID, USER_TRAINEE_ID, USER_AUDITOR_A_ID, USER_UNSCOPED_ID);

    jdbcTemplate.update("DELETE FROM training_batches WHERE batch_id IN (?, ?)", BATCH_A_ID, BATCH_B_ID);
    jdbcTemplate.update("DELETE FROM training_centers WHERE center_id IN (?, ?)", CENTER_A_ID, CENTER_B_ID);
    jdbcTemplate.update("DELETE FROM employers WHERE employer_id IN (?, ?)", EMPLOYER_A_ID, EMPLOYER_B_ID);
    jdbcTemplate.update("DELETE FROM training_providers WHERE provider_id IN (?, ?)", PROVIDER_A_ID, PROVIDER_B_ID);
    jdbcTemplate.update("DELETE FROM programs WHERE program_id = ?", PROGRAM_ID);
    jdbcTemplate.update("DELETE FROM schemes WHERE scheme_id = ?", SCHEME_ID);
    jdbcTemplate.update("DELETE FROM departments WHERE department_id = ?", DEPT_ID);
    jdbcTemplate.update("DELETE FROM organizations WHERE organization_id = ?", ORG_ID);
    jdbcTemplate.update("DELETE FROM courses WHERE course_id = ?", COURSE_ID);
    jdbcTemplate.update("DELETE FROM sectors WHERE sector_id = ?", SECTOR_ID);
    jdbcTemplate.update("DELETE FROM districts WHERE district_id IN (?, ?, ?)", DISTRICT_A1_ID, DISTRICT_B1_ID, DISTRICT_A2_ID);
    jdbcTemplate.update("DELETE FROM states WHERE state_id IN (?, ?)", STATE_A_ID, STATE_B_ID);
  }

  private Long stageBatchViaApi(String token, String filename, String contentType, byte[] content, String entityType)
      throws Exception {
    MockMultipartFile file = new MockMultipartFile("file", filename, contentType, content);
    MvcResult res = mockMvc.perform(multipart("/api/v1/import/batches")
            .file(file)
            .param("entityType", entityType)
            .param("sourceSystem", "IMP285_PORTAL")
            .header("Authorization", token))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.success").value(true))
        .andExpect(jsonPath("$.data.importBatchStatusCode").value("PENDING"))
        .andReturn();

    JsonNode root = objectMapper.readTree(res.getResponse().getContentAsString());
    return root.path("data").path("id").asLong();
  }

  @Nested
  @DisplayName("1. Staged Upload Validation, Limits & Authorization")
  class UploadAndStagingTests {

    @Test
    @DisplayName("Stages valid CSV & JSON uploads cleanly with server-generated batchCode and authenticated initiator")
    void stagesValidCsvAndJsonUploads() throws Exception {
      String csv = "registrationNumber,firstName,genderId,stateId,districtId,currentEmploymentStatusId,profileStatusId\n"
          + "IMP285-UPL-1,Asha,1," + STATE_A_ID + "," + DISTRICT_A1_ID + ",1,1\n";
      MockMultipartFile file = new MockMultipartFile(
          "file", "../evil/trainees.csv", "text/csv", csv.getBytes(StandardCharsets.UTF_8));

      MvcResult res = mockMvc.perform(multipart("/api/v1/import/batches")
              .file(file)
              .param("entityType", "Trainee")
              .param("sourceSystem", "IMP285_TEST_SYS")
              .header("Authorization", tokenProviderA))
          .andExpect(status().isCreated())
          .andExpect(jsonPath("$.data.importBatchStatusCode").value("PENDING"))
          .andExpect(jsonPath("$.data.entityType").value("Trainee"))
          .andExpect(jsonPath("$.data.initiatedByUserId").value(USER_PROVIDER_A_ID))
          .andReturn();

      String batchCode = objectMapper.readTree(res.getResponse().getContentAsString())
          .path("data").path("batchCode").asText();
      assertThat(batchCode).startsWith("IMP-");
      assertThat(fileStagingService.hasStagedFile(batchCode)).isTrue();

      // Secondary route /api/v1/import-batches multipart also works
      String json = "[{\"placementNumber\":\"IMP285-UPL-PLC-1\",\"enrollmentId\":" + ENROLLMENT_A_ID
          + ",\"employerId\":" + EMPLOYER_A_ID + ",\"engagementTypeId\":1,\"placementSourceId\":1,\"placementStatusId\":1,\"joiningStatusId\":1,\"recordVerificationStatusId\":1}]";
      MockMultipartFile jsonFile = new MockMultipartFile(
          "file", "placements.json", "application/json", json.getBytes(StandardCharsets.UTF_8));

      mockMvc.perform(multipart("/api/v1/import-batches")
              .file(jsonFile)
              .param("entityType", "PlacementRecord")
              .param("sourceSystem", "IMP285_EMP_PORTAL")
              .header("Authorization", tokenEmployerA))
          .andExpect(status().isCreated())
          .andExpect(jsonPath("$.data.importBatchStatusCode").value("PENDING"))
          .andExpect(jsonPath("$.data.initiatedByUserId").value(USER_EMPLOYER_A_ID));
    }

    @Test
    @DisplayName("Rejects empty, unsupported, missing part, malformed multipart, and size-exceeded uploads")
    void rejectsInvalidUploadsAndMapsUploadSizeExceeded() throws Exception {
      // Missing 'file' part
      mockMvc.perform(multipart("/api/v1/import/batches")
              .file(new MockMultipartFile("wrongPart", "a.csv", "text/csv", "a,b\n1,2\n".getBytes(StandardCharsets.UTF_8)))
              .param("entityType", "Trainee")
              .header("Authorization", tokenSuperAdmin))
          .andExpect(status().isBadRequest())
          .andExpect(jsonPath("$.code").value("MISSING_FILE_PART"));

      // Empty file
      mockMvc.perform(multipart("/api/v1/import/batches")
              .file(new MockMultipartFile("file", "empty.csv", "text/csv", new byte[0]))
              .param("entityType", "Trainee")
              .param("sourceSystem", "IMP285_EMPTY")
              .header("Authorization", tokenSuperAdmin))
          .andExpect(status().isBadRequest())
          .andExpect(jsonPath("$.code").value("EMPTY_IMPORT_FILE"));

      // Unsupported file extension
      mockMvc.perform(multipart("/api/v1/import/batches")
              .file(new MockMultipartFile("file", "bad.exe", "application/octet-stream", "MZ90".getBytes(StandardCharsets.UTF_8)))
              .param("entityType", "Trainee")
              .param("sourceSystem", "IMP285_BADEXT")
              .header("Authorization", tokenSuperAdmin))
          .andExpect(status().isBadRequest())
          .andExpect(jsonPath("$.code").value("UNSUPPORTED_FILE_FORMAT"));

      // Direct POST with multipart media type but no multipart part -> 400 MISSING_FILE_PART
      mockMvc.perform(post("/api/v1/import/batches")
              .contentType(MediaType.MULTIPART_FORM_DATA_VALUE + "; boundary=badboundary")
              .content("not-a-valid-multipart-body".getBytes(StandardCharsets.UTF_8))
              .header("Authorization", tokenSuperAdmin))
          .andExpect(status().isBadRequest())
          .andExpect(jsonPath("$.code").value("MISSING_FILE_PART"));

      // Servlet-level MultipartException -> 400 MALFORMED_MULTIPART_REQUEST
      var multipartExResp = globalExceptionHandler.handleMultipartException(
          new MultipartException("Malformed multipart payload"),
          new MockHttpServletRequest("POST", "/api/v1/import/batches"));
      assertThat(multipartExResp.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
      assertThat(multipartExResp.getBody()).isNotNull();
      assertThat(multipartExResp.getBody().getCode()).isEqualTo("MALFORMED_MULTIPART_REQUEST");

      // Oversized payload against configured import-pipeline max-file-size-bytes
      properties.setMaxFileSizeBytes(64);
      String overLimitCsv = "registrationNumber,firstName,genderId,stateId,districtId,currentEmploymentStatusId,profileStatusId\n"
          + "IMP285-BIG-1,LongNameValue,1,1,1,1,1\n";
      mockMvc.perform(multipart("/api/v1/import/batches")
              .file(new MockMultipartFile("file", "big.csv", "text/csv", overLimitCsv.getBytes(StandardCharsets.UTF_8)))
              .param("entityType", "Trainee")
              .param("sourceSystem", "IMP285_OVER")
              .header("Authorization", tokenSuperAdmin))
          .andExpect(status().isBadRequest())
          .andExpect(jsonPath("$.code").value("FILE_SIZE_LIMIT_EXCEEDED"));

      // GlobalExceptionHandler mapping for servlet-level MaxUploadSizeExceededException -> 413
      MockHttpServletRequest req = new MockHttpServletRequest("POST", "/api/v1/import/batches");
      var handlerResp = globalExceptionHandler.handleMaxUploadSizeExceeded(
          new MaxUploadSizeExceededException(10485760L), req);
      assertThat(handlerResp.getStatusCode()).isEqualTo(HttpStatus.PAYLOAD_TOO_LARGE);
      assertThat(handlerResp.getBody()).isNotNull();
      assertThat(handlerResp.getBody().getCode()).isEqualTo("FILE_SIZE_LIMIT_EXCEEDED");

      // Unauthenticated, Trainee, Auditor-only, Employer on Trainee, Unscoped -> rejected
      MockMultipartFile okCsv = new MockMultipartFile("file", "ok.csv", "text/csv",
          "registrationNumber,firstName,genderId,stateId,districtId,currentEmploymentStatusId,profileStatusId\nIMP285-A1,A,1,1,1,1,1\n".getBytes(StandardCharsets.UTF_8));
      mockMvc.perform(multipart("/api/v1/import/batches").file(okCsv).param("entityType", "Trainee"))
          .andExpect(status().isUnauthorized());
      mockMvc.perform(multipart("/api/v1/import/batches").file(okCsv).param("entityType", "Trainee").header("Authorization", tokenTrainee))
          .andExpect(status().isForbidden());
      mockMvc.perform(multipart("/api/v1/import/batches").file(okCsv).param("entityType", "Trainee").header("Authorization", tokenAuditorA))
          .andExpect(status().isForbidden());
      mockMvc.perform(multipart("/api/v1/import/batches").file(okCsv).param("entityType", "Trainee").header("Authorization", tokenEmployerA))
          .andExpect(status().isForbidden());
      mockMvc.perform(multipart("/api/v1/import/batches").file(okCsv).param("entityType", "Trainee").header("Authorization", tokenUnscoped))
          .andExpect(status().isForbidden());

      // Ensure failed upload attempts created zero batches
      Integer leftCount = jdbcTemplate.queryForObject(
          "SELECT COUNT(*) FROM import_batches WHERE source_system IN ('IMP285_EMPTY', 'IMP285_BADEXT', 'IMP285_OVER')",
          Integer.class);
      assertThat(leftCount).isEqualTo(0);
    }
  }

  @Nested
  @DisplayName("2. Validation, Outcomes (SUCCESS/FAILED/DUPLICATE/SKIPPED) & Quarantine")
  class ValidationAndQuarantineTests {

    @Test
    @DisplayName("Processes mixed CSV classifying SUCCESS, FAILED, DUPLICATE, and SKIPPED cleanly with PII-free error sanitization")
    void processesMixedTraineeCsvEndToEnd() throws Exception {
      String csv = String.join("\n",
          "registrationNumber,firstName,lastName,phone,email,dateOfBirth,genderId,stateId,districtId,pincode,currentEmploymentStatusId,profileStatusId",
          "IMP285-MIX-01,\"Rao, Kavya\",Nair,9841000101,imp_mix01@test.gov.in,2000-05-12,1," + STATE_A_ID + "," + DISTRICT_A1_ID + ",560001,1,1",
          "", // row 2: blank CSV line -> SKIPPED EMPTY_ROW
          "IMP285-MIX-02,MissingCols,OnlyFour,9841000102", // row 3: malformed column count -> FAILED MALFORMED_CSV_ROW
          "IMP285-MIX-03,BadDateUser,Das,9841000103,imp_mix03@test.gov.in,1999/99/99,1," + STATE_A_ID + "," + DISTRICT_A1_ID + ",560001,1,1", // row 4: invalid date -> FAILED
          "IMP285-MIX-04,BadFkUser,Roy,9841000104,imp_mix04@test.gov.in,2000-01-01,1," + STATE_A_ID + ",99999999,560001,1,1", // row 5: missing district FK -> FAILED
          "IMP285-SEED-TR-1,SeedClone,One,9841000105,imp_mix05@test.gov.in,2000-01-01,1," + STATE_A_ID + "," + DISTRICT_A1_ID + ",560001,1,1", // row 6: DB duplicate regNumber -> DUPLICATE
          "IMP285-MIX-01,IntraBatchClone,Dup,9841000106,imp_mix06@test.gov.in,2000-01-01,1," + STATE_A_ID + "," + DISTRICT_A1_ID + ",560001,1,1", // row 7: intra-file duplicate -> DUPLICATE
          "IMP285-MIX-08,ValidSecond,Sen,9841000108,imp_mix08@test.gov.in,2001-08-20,1," + STATE_A_ID + "," + DISTRICT_A1_ID + ",560002,1,1" // row 8: SUCCESS
      );

      Long batchId = stageBatchViaApi(
          tokenStateA, "mixed-trainees.csv", "text/csv", csv.getBytes(StandardCharsets.UTF_8), "Trainee");

      mockMvc.perform(post("/api/v1/import/batches/" + batchId + "/process")
              .header("Authorization", tokenStateA))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.data.status").value("COMPLETED"))
          .andExpect(jsonPath("$.data.isTerminal").value(true))
          .andExpect(jsonPath("$.data.rowCount").value(8))
          .andExpect(jsonPath("$.data.successCount").value(2))
          .andExpect(jsonPath("$.data.failureCount").value(3))
          .andExpect(jsonPath("$.data.duplicateCount").value(2))
          .andExpect(jsonPath("$.data.skippedCount").value(1));

      // Verify DB persistence of valid rows and chronological timestamps
      Integer committedTrainees = jdbcTemplate.queryForObject(
          "SELECT COUNT(*) FROM trainees WHERE registration_number IN ('IMP285-MIX-01', 'IMP285-MIX-08')",
          Integer.class);
      assertThat(committedTrainees).isEqualTo(2);

      Map<String, Object> batchRow = jdbcTemplate.queryForMap(
          "SELECT row_count, success_count, failure_count, started_at, completed_at FROM import_batches WHERE import_batch_id = ?",
          batchId);
      assertThat(((Number) batchRow.get("row_count")).intValue()).isEqualTo(8);
      assertThat(((Number) batchRow.get("success_count")).intValue()).isEqualTo(2);
      assertThat(((Number) batchRow.get("failure_count")).intValue()).isEqualTo(3);
      assertThat(batchRow.get("completed_at")).isNotNull();

      // Verify sanitized quarantine records without raw PII / stack traces
      MvcResult quarantineRes = mockMvc.perform(get("/api/v1/import/batches/" + batchId + "/quarantine")
              .param("includeDuplicates", "true")
              .param("page", "0")
              .param("size", "10")
              .header("Authorization", tokenStateA))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.data.totalElements").value(5))
          .andReturn();

      String qBody = quarantineRes.getResponse().getContentAsString();
      assertThat(qBody).doesNotContain("java.lang.", "SQLException", "SELECT ", "INSERT ", "9841000103", "9841000105");
    }

    @Test
    @DisplayName("Processes PlacementRecord JSON stream including chronology errors, DB duplicates, null skip, and employer tenant persistence")
    void processesPlacementJsonWithValidationAndQuarantine() throws Exception {
      String json = "[\n"
          + "{\"placementNumber\":\"IMP285-PLC-OK1\",\"enrollmentId\":" + ENROLLMENT_A_ID + ",\"employerId\":" + EMPLOYER_A_ID
          + ",\"engagementTypeId\":1,\"placementSourceId\":1,\"placementStatusId\":1,\"joiningStatusId\":1"
          + ",\"offerDate\":\"2026-02-01\",\"actualJoiningDate\":\"2026-02-15\",\"offeredSalary\":24000,\"workStateId\":" + STATE_A_ID + ",\"workDistrictId\":" + DISTRICT_A1_ID + ",\"recordVerificationStatusId\":1},\n"
          + "{\"placementNumber\":\"IMP285-PLC-BADDATE\",\"enrollmentId\":" + ENROLLMENT_A_ID + ",\"employerId\":" + EMPLOYER_A_ID
          + ",\"engagementTypeId\":1,\"placementSourceId\":1,\"placementStatusId\":1,\"joiningStatusId\":1"
          + ",\"offerDate\":\"2026-03-15\",\"actualJoiningDate\":\"2026-03-01\",\"recordVerificationStatusId\":1},\n"
          + "{\"placementNumber\":\"IMP285-SEED-PLC-1\",\"enrollmentId\":" + ENROLLMENT_A_ID + ",\"employerId\":" + EMPLOYER_A_ID
          + ",\"engagementTypeId\":1,\"placementSourceId\":1,\"placementStatusId\":1,\"joiningStatusId\":1,\"recordVerificationStatusId\":1},\n"
          + "null,\n"
          + "42\n"
          + "]";

      Long batchId = stageBatchViaApi(
          tokenEmployerA, "placements.json", "application/json", json.getBytes(StandardCharsets.UTF_8), "PlacementRecord");

      mockMvc.perform(post("/api/v1/import/batches/" + batchId + "/process")
              .header("Authorization", tokenEmployerA))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.data.status").value("COMPLETED"))
          .andExpect(jsonPath("$.data.rowCount").value(5))
          .andExpect(jsonPath("$.data.successCount").value(1))
          .andExpect(jsonPath("$.data.failureCount").value(2))
          .andExpect(jsonPath("$.data.duplicateCount").value(1))
          .andExpect(jsonPath("$.data.skippedCount").value(1));

      mockMvc.perform(get("/api/v1/import/batches/" + batchId + "/quarantine")
              .param("includeDuplicates", "false")
              .header("Authorization", tokenEmployerA))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.data.totalElements").value(2))
          .andExpect(jsonPath("$.data.content[0].errorCode").value("INVALID_JOINING_DATE"))
          .andExpect(jsonPath("$.data.content[1].errorCode").value("MALFORMED_JSON_ROW"));
    }
  }

  @Nested
  @DisplayName("3. Chunked Transactions, Rollback Isolation, Concurrency & Idempotency")
  class ChunkingConcurrencyAndFinalizationTests {

    @Test
    @DisplayName("Processes 215 rows across 3 bounded chunks (100/100/15), quarantining chunk errors without rolling back prior or later valid rows")
    void processesMultiChunkCsvWithIsolatedQuarantine() throws Exception {
      StringBuilder csv = new StringBuilder(
          "registrationNumber,firstName,phone,genderId,stateId,districtId,currentEmploymentStatusId,profileStatusId\n");
      for (int i = 1; i <= 215; i++) {
        String phone = String.format("9750%06d", i);
        if (i == 120 || i == 208) {
          // Invalid district FK -> FAILED in chunks 2 and 3
          csv.append("IMP285-CHK-").append(i).append(",ChunkUser").append(i).append(',').append(phone)
              .append(",1,").append(STATE_A_ID).append(",9999999,1,1\n");
        } else if (i == 150) {
          // Intra-batch duplicate of row 10 -> DUPLICATE in chunk 2
          csv.append("IMP285-CHK-10,DupChunkUser,9750000150,1,").append(STATE_A_ID).append(',').append(DISTRICT_A1_ID).append(",1,1\n");
        } else {
          csv.append("IMP285-CHK-").append(i).append(",ChunkUser").append(i).append(',').append(phone)
              .append(",1,").append(STATE_A_ID).append(',').append(DISTRICT_A1_ID).append(",1,1\n");
        }
      }

      Long batchId = stageBatchViaApi(
          tokenStateA, "chunked-215.csv", "text/csv", csv.toString().getBytes(StandardCharsets.UTF_8), "Trainee");

      mockMvc.perform(post("/api/v1/import/batches/" + batchId + "/process")
              .header("Authorization", tokenStateA))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.data.status").value("COMPLETED"))
          .andExpect(jsonPath("$.data.rowCount").value(215))
          .andExpect(jsonPath("$.data.successCount").value(212))
          .andExpect(jsonPath("$.data.failureCount").value(2))
          .andExpect(jsonPath("$.data.duplicateCount").value(1))
          .andExpect(jsonPath("$.data.skippedCount").value(0));

      Integer persisted = jdbcTemplate.queryForObject(
          "SELECT COUNT(*) FROM trainees WHERE registration_number LIKE 'IMP285-CHK-%'",
          Integer.class);
      assertThat(persisted).isEqualTo(212);
    }

    @Test
    @DisplayName("Prevents concurrent double-execution on same batch and rejects reprocessing terminal batches")
    void enforcesAtomicClaimAndIdempotentTerminalRejection() throws Exception {
      StringBuilder csv = new StringBuilder(
          "registrationNumber,firstName,phone,genderId,stateId,districtId,currentEmploymentStatusId,profileStatusId\n");
      for (int i = 1; i <= 25; i++) {
        csv.append("IMP285-CONC-").append(i).append(",Conc").append(i).append(',')
            .append(String.format("9760%06d", i))
            .append(",1,").append(STATE_A_ID).append(',').append(DISTRICT_A1_ID).append(",1,1\n");
      }

      Long batchId = stageBatchViaApi(
          tokenStateA, "concurrent.csv", "text/csv", csv.toString().getBytes(StandardCharsets.UTF_8), "Trainee");

      ExecutorService pool = Executors.newFixedThreadPool(2);
      CountDownLatch ready = new CountDownLatch(2);
      CountDownLatch go = new CountDownLatch(1);

      Callable<Integer> worker = () -> {
        ready.countDown();
        go.await(5, TimeUnit.SECONDS);
        MvcResult res = mockMvc.perform(post("/api/v1/import/batches/" + batchId + "/process")
                .header("Authorization", tokenStateA))
            .andReturn();
        return res.getResponse().getStatus();
      };

      Future<Integer> f1 = pool.submit(worker);
      Future<Integer> f2 = pool.submit(worker);
      assertThat(ready.await(5, TimeUnit.SECONDS)).isTrue();
      go.countDown();

      List<Integer> statuses = new ArrayList<>();
      statuses.add(f1.get(30, TimeUnit.SECONDS));
      statuses.add(f2.get(30, TimeUnit.SECONDS));
      pool.shutdownNow();

      assertThat(statuses).containsExactlyInAnyOrder(200, 409);

      Integer insertedCount = jdbcTemplate.queryForObject(
          "SELECT COUNT(*) FROM trainees WHERE registration_number LIKE 'IMP285-CONC-%'",
          Integer.class);
      assertThat(insertedCount).isEqualTo(25);

      Integer recCount = jdbcTemplate.queryForObject(
          "SELECT COUNT(*) FROM import_records WHERE import_batch_id = ?",
          Integer.class, batchId);
      assertThat(recCount).isEqualTo(25);

      // Reprocessing terminal COMPLETED batch must return 409 IMPORT_BATCH_TERMINAL
      mockMvc.perform(post("/api/v1/import/batches/" + batchId + "/process")
              .header("Authorization", tokenStateA))
          .andExpect(status().isConflict())
          .andExpect(jsonPath("$.code").value("IMPORT_BATCH_TERMINAL"));
    }

    @Test
    @DisplayName("Preserves committed chunk 1 if chunk 2 suffers fatal stream syntax error, finalizing batch as FAILED")
    void preservesCommittedChunkOnMidStreamSyntaxFailure() throws Exception {
      StringBuilder json = new StringBuilder("[");
      for (int i = 1; i <= 105; i++) {
        if (i > 1) {
          json.append(',');
        }
        if (i == 105) {
          // Corrupted JSON token at record 105 (chunk 2)
          json.append("{\"registrationNumber\":\"IMP285-MIDFAIL-105\", BAD_JSON ");
        } else {
          json.append("{\"registrationNumber\":\"IMP285-MIDFAIL-").append(i)
              .append("\",\"firstName\":\"Mid").append(i)
              .append("\",\"phone\":\"").append(String.format("9770%06d", i))
              .append("\",\"genderId\":1,\"stateId\":").append(STATE_A_ID)
              .append(",\"districtId\":").append(DISTRICT_A1_ID)
              .append(",\"currentEmploymentStatusId\":1,\"profileStatusId\":1}");
        }
      }

      Long batchId = stageBatchViaApi(
          tokenStateA, "midstream-fail.json", "application/json",
          json.toString().getBytes(StandardCharsets.UTF_8), "Trainee");

      mockMvc.perform(post("/api/v1/import/batches/" + batchId + "/process")
              .header("Authorization", tokenStateA))
          .andExpect(status().isBadRequest())
          .andExpect(jsonPath("$.code").value("MALFORMED_JSON_FILE"));

      // First chunk (100 rows) remains safely committed; batch is finalized FAILED
      Integer midCommitted = jdbcTemplate.queryForObject(
          "SELECT COUNT(*) FROM trainees WHERE registration_number LIKE 'IMP285-MIDFAIL-%'",
          Integer.class);
      assertThat(midCommitted).isEqualTo(100);

      mockMvc.perform(get("/api/v1/import/batches/" + batchId + "/progress")
              .header("Authorization", tokenStateA))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.data.status").value("FAILED"))
          .andExpect(jsonPath("$.data.isTerminal").value(true))
          .andExpect(jsonPath("$.data.rowCount").value(100))
          .andExpect(jsonPath("$.data.successCount").value(100));

      // Empty data file (header only) also finalizes FAILED
      String headerOnlyCsv = "registrationNumber,firstName,genderId,stateId,districtId,currentEmploymentStatusId,profileStatusId\n";
      Long emptyDataBatchId = stageBatchViaApi(
          tokenStateA, "header-only.csv", "text/csv", headerOnlyCsv.getBytes(StandardCharsets.UTF_8), "Trainee");

      mockMvc.perform(post("/api/v1/import/batches/" + emptyDataBatchId + "/process")
              .header("Authorization", tokenStateA))
          .andExpect(status().isBadRequest())
          .andExpect(jsonPath("$.code").value("NO_DATA_ROWS"));

      mockMvc.perform(get("/api/v1/import/batches/" + emptyDataBatchId + "/progress")
              .header("Authorization", tokenStateA))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.data.status").value("FAILED"))
          .andExpect(jsonPath("$.data.rowCount").value(0));
    }
  }

  @Nested
  @DisplayName("4. Multi-Tenant Isolation & Paginated Endpoints")
  class TenantIsolationAndPaginationTests {

    @Test
    @DisplayName("Enforces strict tenant isolation across batches, rows, cross-tenant FK refs, and paginated APIs")
    void enforcesTenantIsolationAcrossPersonasAndEndpoints() throws Exception {
      // Provider A stages a Trainee CSV containing 1 valid row in State A and 1 cross-tenant row referencing State B / Provider B
      String provACsv = String.join("\n",
          "registrationNumber,firstName,phone,genderId,stateId,districtId,currentEmploymentStatusId,profileStatusId,providerId",
          "IMP285-TEN-A1,TenantOkA,9780000001,1," + STATE_A_ID + "," + DISTRICT_A1_ID + ",1,1," + PROVIDER_A_ID,
          "IMP285-TEN-X1,CrossProv,9780000002,1," + STATE_A_ID + "," + DISTRICT_A1_ID + ",1,1," + PROVIDER_B_ID,
          "IMP285-TEN-X2,CrossState,9780000003,1," + STATE_B_ID + "," + DISTRICT_B1_ID + ",1,1," + PROVIDER_A_ID
      );

      Long provABatchId = stageBatchViaApi(
          tokenProviderA, "prov-a.csv", "text/csv", provACsv.getBytes(StandardCharsets.UTF_8), "Trainee");

      // Cross-tenant callers CANNOT process or read Provider A's batch
      mockMvc.perform(post("/api/v1/import/batches/" + provABatchId + "/process")
              .header("Authorization", tokenProviderB))
          .andExpect(status().isForbidden());
      mockMvc.perform(post("/api/v1/import/batches/" + provABatchId + "/process")
              .header("Authorization", tokenStateB))
          .andExpect(status().isForbidden());
      mockMvc.perform(post("/api/v1/import/batches/" + provABatchId + "/process")
              .header("Authorization", tokenAuditorA))
          .andExpect(status().isForbidden());

      // Provider A processes own batch -> 1 SUCCESS, 2 FAILED CROSS_TENANT_REFERENCE
      mockMvc.perform(post("/api/v1/import/batches/" + provABatchId + "/process")
              .header("Authorization", tokenProviderA))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.data.status").value("COMPLETED"))
          .andExpect(jsonPath("$.data.rowCount").value(3))
          .andExpect(jsonPath("$.data.successCount").value(1))
          .andExpect(jsonPath("$.data.failureCount").value(2));

      // State B, Provider B, Employer A cannot inspect progress, records, quarantine, or /api/v1/import-records for Provider A's batch
      mockMvc.perform(get("/api/v1/import/batches/" + provABatchId + "/progress").header("Authorization", tokenProviderB))
          .andExpect(status().isForbidden());
      mockMvc.perform(get("/api/v1/import/batches/" + provABatchId + "/records").header("Authorization", tokenStateB))
          .andExpect(status().isForbidden());
      mockMvc.perform(get("/api/v1/import/batches/" + provABatchId + "/quarantine").header("Authorization", tokenEmployerA))
          .andExpect(status().isForbidden());
      mockMvc.perform(get("/api/v1/import-records").param("batchId", provABatchId.toString()).header("Authorization", tokenProviderB))
          .andExpect(status().isForbidden());

      // Nationwide Super Admin CAN inspect progress, records, and quarantine across tenants
      mockMvc.perform(get("/api/v1/import/batches/" + provABatchId + "/progress").header("Authorization", tokenSuperAdmin))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.data.successCount").value(1));

      // District A1 vs District A2 batch isolation
      String distCsv = "registrationNumber,firstName,phone,genderId,stateId,districtId,currentEmploymentStatusId,profileStatusId\n"
          + "IMP285-D1-OK,DistOk,9780000010,1," + STATE_A_ID + "," + DISTRICT_A1_ID + ",1,1\n"
          + "IMP285-D1-CROSS,DistCross,9780000011,1," + STATE_A_ID + "," + DISTRICT_A2_ID + ",1,1\n";
      Long distBatchId = stageBatchViaApi(
          tokenDistrictA1, "dist-a1.csv", "text/csv", distCsv.getBytes(StandardCharsets.UTF_8), "Trainee");
      mockMvc.perform(post("/api/v1/import/batches/" + distBatchId + "/process")
              .header("Authorization", tokenDistrictA1))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.data.successCount").value(1))
          .andExpect(jsonPath("$.data.failureCount").value(1));

      mockMvc.perform(get("/api/v1/import/batches/" + distBatchId + "/progress").header("Authorization", tokenDistrictA2))
          .andExpect(status().isForbidden());

      // Employer Placement cross-tenant employer rejection
      String empCrossJson = "[{\"placementNumber\":\"IMP285-EMP-CROSS\",\"enrollmentId\":" + ENROLLMENT_B_ID
          + ",\"employerId\":" + EMPLOYER_B_ID + ",\"engagementTypeId\":1,\"placementSourceId\":1,\"placementStatusId\":1,\"joiningStatusId\":1,\"recordVerificationStatusId\":1}]";
      Long empBatchId = stageBatchViaApi(
          tokenEmployerA, "emp-cross.json", "application/json",
          empCrossJson.getBytes(StandardCharsets.UTF_8), "PlacementRecord");
      mockMvc.perform(post("/api/v1/import/batches/" + empBatchId + "/process")
              .header("Authorization", tokenEmployerA))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.data.successCount").value(0))
          .andExpect(jsonPath("$.data.failureCount").value(1));

      mockMvc.perform(get("/api/v1/import/batches/" + empBatchId + "/quarantine")
              .header("Authorization", tokenEmployerA))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.data.content[0].errorCode").value("CROSS_TENANT_REFERENCE"));

      // Paginated status/entity queries on /api/v1/import-records respect tenant boundaries & sort guardrails
      mockMvc.perform(get("/api/v1/import-records")
              .param("batchId", provABatchId.toString())
              .param("page", "0")
              .param("size", "2")
              .param("sort", "sourceRowNumber")
              .param("direction", "DESC")
              .header("Authorization", tokenProviderA))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.data.totalElements").value(3))
          .andExpect(jsonPath("$.data.totalPages").value(2))
          .andExpect(jsonPath("$.data.content[0].sourceRowNumber").value(3));

      // State B querying statusId=2 (FAILED) sees 0 of State A's failed rows
      mockMvc.perform(get("/api/v1/import-records")
              .param("statusId", "2")
              .param("page", "0")
              .param("size", "10")
              .header("Authorization", tokenStateB))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.data.content").isEmpty());

      // Invalid parameter / sort field rejected cleanly
      mockMvc.perform(get("/api/v1/import/batches/" + provABatchId + "/records")
              .param("unknownParam", "x")
              .header("Authorization", tokenProviderA))
          .andExpect(status().isBadRequest())
          .andExpect(jsonPath("$.code").value("UNSUPPORTED_PARAMETER"));

      mockMvc.perform(get("/api/v1/import/batches/" + provABatchId + "/records")
              .param("sort", "passwordHash")
              .header("Authorization", tokenProviderA))
          .andExpect(status().isBadRequest())
          .andExpect(jsonPath("$.code").value("INVALID_SORT_FIELD"));
    }

    @Test
    @DisplayName("Processes NDJSON streams cleanly, enforces legacy CRUD permissions/tenancy, and handles overlapping concurrent batches safely")
    void processesNdjsonEnforcesCrudSecurityAndConcurrentBatchOverlap() throws Exception {
      String ndjson = "{\"registrationNumber\":\"IMP285-ND-1\",\"firstName\":\"NdOne\",\"phone\":\"9791000001\",\"genderId\":1,\"stateId\":" + STATE_A_ID + ",\"districtId\":" + DISTRICT_A1_ID + ",\"currentEmploymentStatusId\":1,\"profileStatusId\":1}\n"
          + "{\"registrationNumber\":\"IMP285-ND-2\",\"firstName\":\"NdTwo\",\"phone\":\"9791000002\",\"genderId\":1,\"stateId\":" + STATE_A_ID + ",\"districtId\":" + DISTRICT_A1_ID + ",\"currentEmploymentStatusId\":1,\"profileStatusId\":1}\n";

      Long ndBatchId = stageBatchViaApi(
          tokenStateA, "trainees.ndjson", "application/x-ndjson",
          ndjson.getBytes(StandardCharsets.UTF_8), "Trainee");

      mockMvc.perform(post("/api/v1/import/batches/" + ndBatchId + "/process")
              .header("Authorization", tokenStateA))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.data.status").value("COMPLETED"))
          .andExpect(jsonPath("$.data.successCount").value(2));

      Map<String, Object> ndBatchRow = jdbcTemplate.queryForMap(
          "SELECT started_at, completed_at FROM import_batches WHERE import_batch_id = ?",
          ndBatchId);
      java.time.LocalDateTime startedTs = ndBatchRow.get("started_at") instanceof java.sql.Timestamp ts1
          ? ts1.toLocalDateTime()
          : (java.time.LocalDateTime) ndBatchRow.get("started_at");
      java.time.LocalDateTime completedTs = ndBatchRow.get("completed_at") instanceof java.sql.Timestamp ts2
          ? ts2.toLocalDateTime()
          : (java.time.LocalDateTime) ndBatchRow.get("completed_at");
      assertThat(startedTs).isNotNull();
      assertThat(completedTs).isNotNull();
      assertThat(completedTs).isAfterOrEqualTo(startedTs);

      // Lookup created trainee ID & test paginated entity query scoping
      Long ndTraineeId = jdbcTemplate.queryForObject(
          "SELECT trainee_id FROM trainees WHERE registration_number = 'IMP285-ND-1'",
          Long.class);
      mockMvc.perform(get("/api/v1/import-records")
              .param("entityType", "Trainee")
              .param("entityId", ndTraineeId.toString())
              .header("Authorization", tokenStateA))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.data.totalElements").value(1));

      mockMvc.perform(get("/api/v1/import-records")
              .param("entityType", "Trainee")
              .param("entityId", ndTraineeId.toString())
              .header("Authorization", tokenStateB))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.data.content").isEmpty());

      // Legacy CRUD audit.read write block & cross-tenant read block
      mockMvc.perform(post("/api/v1/import-batches")
              .contentType(MediaType.APPLICATION_JSON)
              .content("{\"batchCode\":\"IMP285-LEG-1\",\"sourceSystem\":\"SYS\",\"entityType\":\"Trainee\"}")
              .header("Authorization", tokenAuditorA))
          .andExpect(status().isForbidden());

      mockMvc.perform(get("/api/v1/import-batches/" + ndBatchId)
              .header("Authorization", tokenStateB))
          .andExpect(status().isForbidden());

      // Concurrent separate batches importing overlapping registration numbers -> no duplicate trainee committed
      String overlapCsv1 = "registrationNumber,firstName,phone,genderId,stateId,districtId,currentEmploymentStatusId,profileStatusId\n"
          + "IMP285-OVLP-1,OverlapUser,9792000001,1," + STATE_A_ID + "," + DISTRICT_A1_ID + ",1,1\n";
      Long ovlpBatch1 = stageBatchViaApi(
          tokenStateA, "ovlp1.csv", "text/csv", overlapCsv1.getBytes(StandardCharsets.UTF_8), "Trainee");
      Long ovlpBatch2 = stageBatchViaApi(
          tokenStateA, "ovlp2.csv", "text/csv", overlapCsv1.getBytes(StandardCharsets.UTF_8), "Trainee");

      ExecutorService pool = Executors.newFixedThreadPool(2);
      CountDownLatch ready = new CountDownLatch(2);
      CountDownLatch go = new CountDownLatch(1);
      Callable<Integer> run1 = () -> {
        ready.countDown();
        go.await(5, TimeUnit.SECONDS);
        return mockMvc.perform(post("/api/v1/import/batches/" + ovlpBatch1 + "/process")
                .header("Authorization", tokenStateA))
            .andReturn().getResponse().getStatus();
      };
      Callable<Integer> run2 = () -> {
        ready.countDown();
        go.await(5, TimeUnit.SECONDS);
        return mockMvc.perform(post("/api/v1/import/batches/" + ovlpBatch2 + "/process")
                .header("Authorization", tokenStateA))
            .andReturn().getResponse().getStatus();
      };
      Future<Integer> f1 = pool.submit(run1);
      Future<Integer> f2 = pool.submit(run2);
      assertThat(ready.await(5, TimeUnit.SECONDS)).isTrue();
      go.countDown();
      assertThat(f1.get(30, TimeUnit.SECONDS)).isEqualTo(200);
      assertThat(f2.get(30, TimeUnit.SECONDS)).isEqualTo(200);
      pool.shutdownNow();

      Integer distinctOverlapTrainees = jdbcTemplate.queryForObject(
          "SELECT COUNT(*) FROM trainees WHERE registration_number = 'IMP285-OVLP-1'",
          Integer.class);
      assertThat(distinctOverlapTrainees).isEqualTo(1);
    }
  }
}
