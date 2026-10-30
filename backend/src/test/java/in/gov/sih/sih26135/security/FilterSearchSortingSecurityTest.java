package in.gov.sih.sih26135.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import in.gov.sih.sih26135.MysqlConnectionTest;
import in.gov.sih.sih26135.exception.BadRequestException;
import in.gov.sih.sih26135.response.PaginationUtils;
import in.gov.sih.sih26135.response.SearchSanitizationUtils;
import in.gov.sih.sih26135.security.jwt.JwtTokenProvider;
import java.lang.reflect.Method;
import java.sql.Connection;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import javax.sql.DataSource;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Phase 26.11 — Filter, Search & Sorting Security Hardening Test Suite.
 *
 * Demonstrates:
 * 1. Entity-specific sort field allowlists across all paginated domain endpoints.
 * 2. Rejection of sensitive sort fields (password, hash, token, secret, vault refs, soft-delete)
 *    with fail-closed non-echoing messages ("Invalid sort field provided").
 * 3. Preservation of standard unsupported field messages for safe domain terms.
 * 4. Rejection of SQL injection payloads and parameter tampering in sort and direction parameters.
 * 5. Search sanitization utility behavior for SQL wildcard escaping (%, _, \), length limits, and pattern generation.
 * 6. Safe parameterized handling of literal wildcard characters (%, _) at the API/query boundary.
 * 7. ReDoS immunity and linear execution verification across all static regex patterns.
 */
@SpringBootTest
@AutoConfigureMockMvc
@DisplayName("Phase 26.11 — Filter, Search & Sorting Security Hardening Integration Tests")
class FilterSearchSortingSecurityTest {

  @Autowired
  private DataSource dataSource;

  @Autowired
  private MockMvc mockMvc;

  @Autowired
  private JwtTokenProvider jwtTokenProvider;

  @Autowired
  private ApplicationContext applicationContext;

  private String adminToken;

  @BeforeEach
  void setUp() throws Exception {
    try (Connection conn = dataSource.getConnection()) {
      String catalog = conn.getCatalog();
      MysqlConnectionTest.assertDedicatedTestCatalog(catalog);
      assertThat(catalog).isEqualToIgnoringCase(MysqlConnectionTest.EXPECTED_TEST_CATALOG);
    }

    adminToken = "Bearer " + jwtTokenProvider.generateAccessToken(
        1L,
        "admin",
        List.of(
            "trainee.read", "trainee.manage",
            "employer.read", "employer.manage",
            "course.read", "course.manage",
            "center.read", "center.manage",
            "provider.read", "provider.manage",
            "batch.read", "batch.manage",
            "enrollment.read", "enrollment.manage",
            "employment.read", "employment.manage",
            "placement.read", "placement.manage",
            "verification.read", "verification.manage",
            "survey.read", "survey.manage",
            "task.read", "task.manage",
            "communication.read", "communication.manage",
            "audit.read", "system.event.read", "system.event.manage"
        )
    );
  }

  // =========================================================================
  // Section 1: Entity-Specific Sort Field Allowlists
  // =========================================================================

  @Nested
  @DisplayName("1. Entity-Specific Sort Field Allowlists")
  class SortFieldAllowlists {

    @Test
    @DisplayName("1.1 Trainee endpoint accepts allowlisted sort fields")
    void testTraineeAllowlistedSort() throws Exception {
      mockMvc.perform(get("/api/v1/trainees?sort=firstName&direction=ASC")
              .header("Authorization", adminToken))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.success").value(true));

      mockMvc.perform(get("/api/v1/trainees?sort=registration_number&direction=DESC")
              .header("Authorization", adminToken))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    @DisplayName("1.2 Employer endpoint accepts allowlisted sort fields")
    void testEmployerAllowlistedSort() throws Exception {
      mockMvc.perform(get("/api/v1/employers?sort=employerName&direction=ASC")
              .header("Authorization", adminToken))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    @DisplayName("1.3 Course endpoint accepts allowlisted sort fields")
    void testCourseAllowlistedSort() throws Exception {
      mockMvc.perform(get("/api/v1/courses?sort=courseCode&direction=DESC")
              .header("Authorization", adminToken))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    @DisplayName("1.4 Training center endpoint accepts allowlisted sort fields")
    void testTrainingCenterAllowlistedSort() throws Exception {
      mockMvc.perform(get("/api/v1/training-centers?sort=centerName&direction=ASC")
              .header("Authorization", adminToken))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    @DisplayName("1.5 Training provider endpoint accepts allowlisted sort fields")
    void testTrainingProviderAllowlistedSort() throws Exception {
      mockMvc.perform(get("/api/v1/training-providers?sort=providerName&direction=ASC")
              .header("Authorization", adminToken))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    @DisplayName("1.6 Training batch endpoint accepts allowlisted sort fields")
    void testTrainingBatchAllowlistedSort() throws Exception {
      mockMvc.perform(get("/api/v1/training-batches?sort=startDate&direction=DESC")
              .header("Authorization", adminToken))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    @DisplayName("1.7 Training enrollment endpoint accepts allowlisted sort fields")
    void testTrainingEnrollmentAllowlistedSort() throws Exception {
      mockMvc.perform(get("/api/v1/training-enrollments?sort=enrollmentDate&direction=ASC")
              .header("Authorization", adminToken))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    @DisplayName("1.8 Audit log endpoint accepts allowlisted sort fields")
    void testAuditLogAllowlistedSort() throws Exception {
      mockMvc.perform(get("/api/v1/audit-logs?sort=occurredAt&direction=DESC")
              .header("Authorization", adminToken))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    @DisplayName("1.9 System event log endpoint accepts allowlisted sort fields")
    void testSystemEventLogAllowlistedSort() throws Exception {
      mockMvc.perform(get("/api/v1/system-event-logs?sort=occurredAt&direction=DESC")
              .header("Authorization", adminToken))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.success").value(true));
    }
  }

  // =========================================================================
  // Section 2: Sensitive Sort Field Rejection & Anti-Echo Protection
  // =========================================================================

  @Nested
  @DisplayName("2. Sensitive Sort Field Rejection Without Information Leakage")
  class SensitiveSortFieldRejection {

    @Test
    @DisplayName("2.1 passwordHash sort field is rejected with generic message and no echo")
    void testPasswordHashSortRejectedWithoutEcho() throws Exception {
      mockMvc.perform(get("/api/v1/trainees?sort=passwordHash")
              .header("Authorization", adminToken))
          .andExpect(status().isBadRequest())
          .andExpect(jsonPath("$.code").value("INVALID_SORT_FIELD"))
          .andExpect(jsonPath("$.message").value("Invalid sort field provided"))
          .andExpect(jsonPath("$.message", Matchers.not(Matchers.containsStringIgnoringCase("password"))))
          .andExpect(jsonPath("$.message", Matchers.not(Matchers.containsStringIgnoringCase("hash"))));
    }

    @Test
    @DisplayName("2.2 password_hash snake_case sort field is rejected with generic message")
    void testPasswordHashSnakeCaseRejected() throws Exception {
      mockMvc.perform(get("/api/v1/employers?sort=password_hash")
              .header("Authorization", adminToken))
          .andExpect(status().isBadRequest())
          .andExpect(jsonPath("$.code").value("INVALID_SORT_FIELD"))
          .andExpect(jsonPath("$.message").value("Invalid sort field provided"))
          .andExpect(jsonPath("$.message", Matchers.not(Matchers.containsStringIgnoringCase("password"))));
    }

    @Test
    @DisplayName("2.3 Nested traversal sort user.passwordHash is rejected without echo")
    void testNestedPropertySortRejected() throws Exception {
      mockMvc.perform(get("/api/v1/trainees?sort=user.passwordHash")
              .header("Authorization", adminToken))
          .andExpect(status().isBadRequest())
          .andExpect(jsonPath("$.code").value("INVALID_SORT_FIELD"))
          .andExpect(jsonPath("$.message").value("Invalid sort field provided"))
          .andExpect(jsonPath("$.message", Matchers.not(Matchers.containsStringIgnoringCase("password"))));
    }

    @Test
    @DisplayName("2.4 token and secret sort fields are rejected without echo")
    void testTokenAndSecretSortRejected() throws Exception {
      mockMvc.perform(get("/api/v1/courses?sort=token")
              .header("Authorization", adminToken))
          .andExpect(status().isBadRequest())
          .andExpect(jsonPath("$.code").value("INVALID_SORT_FIELD"))
          .andExpect(jsonPath("$.message").value("Invalid sort field provided"))
          .andExpect(jsonPath("$.message", Matchers.not(Matchers.containsStringIgnoringCase("token"))));

      mockMvc.perform(get("/api/v1/training-centers?sort=secret")
              .header("Authorization", adminToken))
          .andExpect(status().isBadRequest())
          .andExpect(jsonPath("$.code").value("INVALID_SORT_FIELD"))
          .andExpect(jsonPath("$.message").value("Invalid sort field provided"))
          .andExpect(jsonPath("$.message", Matchers.not(Matchers.containsStringIgnoringCase("secret"))));
    }

    @Test
    @DisplayName("2.5 Vault and financial references (aadhaarVaultRef, bankAccount) are rejected without echo")
    void testVaultAndFinancialSortRejected() throws Exception {
      mockMvc.perform(get("/api/v1/trainees?sort=aadhaarVaultRef")
              .header("Authorization", adminToken))
          .andExpect(status().isBadRequest())
          .andExpect(jsonPath("$.code").value("INVALID_SORT_FIELD"))
          .andExpect(jsonPath("$.message").value("Invalid sort field provided"))
          .andExpect(jsonPath("$.message", Matchers.not(Matchers.containsStringIgnoringCase("aadhaar"))))
          .andExpect(jsonPath("$.message", Matchers.not(Matchers.containsStringIgnoringCase("vault"))));

      mockMvc.perform(get("/api/v1/trainees?sort=bankAccount")
              .header("Authorization", adminToken))
          .andExpect(status().isBadRequest())
          .andExpect(jsonPath("$.code").value("INVALID_SORT_FIELD"))
          .andExpect(jsonPath("$.message").value("Invalid sort field provided"))
          .andExpect(jsonPath("$.message", Matchers.not(Matchers.containsStringIgnoringCase("bank"))));
    }

    @Test
    @DisplayName("2.6 Soft-delete fields (deletedAt, isDeleted) are rejected without echo")
    void testSoftDeleteSortRejected() throws Exception {
      mockMvc.perform(get("/api/v1/trainees?sort=deletedAt")
              .header("Authorization", adminToken))
          .andExpect(status().isBadRequest())
          .andExpect(jsonPath("$.code").value("INVALID_SORT_FIELD"))
          .andExpect(jsonPath("$.message").value("Invalid sort field provided"))
          .andExpect(jsonPath("$.message", Matchers.not(Matchers.containsStringIgnoringCase("deleted"))));

      mockMvc.perform(get("/api/v1/employers?sort=isDeleted")
              .header("Authorization", adminToken))
          .andExpect(status().isBadRequest())
          .andExpect(jsonPath("$.code").value("INVALID_SORT_FIELD"))
          .andExpect(jsonPath("$.message").value("Invalid sort field provided"));
    }

    @Test
    @DisplayName("2.7 Security counters (failedLoginCount, lockedUntil) are rejected without echo")
    void testSecurityCountersSortRejected() throws Exception {
      mockMvc.perform(get("/api/v1/trainees?sort=failedLoginCount")
              .header("Authorization", adminToken))
          .andExpect(status().isBadRequest())
          .andExpect(jsonPath("$.code").value("INVALID_SORT_FIELD"))
          .andExpect(jsonPath("$.message").value("Invalid sort field provided"));

      mockMvc.perform(get("/api/v1/trainees?sort=lockedUntil")
              .header("Authorization", adminToken))
          .andExpect(status().isBadRequest())
          .andExpect(jsonPath("$.code").value("INVALID_SORT_FIELD"))
          .andExpect(jsonPath("$.message").value("Invalid sort field provided"));
    }
  }

  // =========================================================================
  // Section 3: Safe Unsupported Field Messages & Backward Compatibility
  // =========================================================================

  @Nested
  @DisplayName("3. Unsupported Sort Field Backward Compatibility")
  class UnsupportedSortFieldPreservation {

    @Test
    @DisplayName("3.1 Safe unsupported field name preserves standard error message")
    void testSafeUnsupportedFieldPreservesMessage() throws Exception {
      mockMvc.perform(get("/api/v1/trainees?sort=maliciousProperty")
              .header("Authorization", adminToken))
          .andExpect(status().isBadRequest())
          .andExpect(jsonPath("$.code").value("INVALID_SORT_FIELD"))
          .andExpect(jsonPath("$.message").value(Matchers.containsString("Unsupported sort field 'maliciousProperty'")))
          .andExpect(jsonPath("$.message").value(Matchers.containsString("Supported fields:")));
    }

    @Test
    @DisplayName("3.2 Non-sensitive unknown field preserves supported fields allowlist in message")
    void testNonSensitiveUnknownFieldPreserved() throws Exception {
      mockMvc.perform(get("/api/v1/courses?sort=nonExistentProperty")
              .header("Authorization", adminToken))
          .andExpect(status().isBadRequest())
          .andExpect(jsonPath("$.code").value("INVALID_SORT_FIELD"))
          .andExpect(jsonPath("$.message").value(Matchers.containsString("Unsupported sort field 'nonExistentProperty'")));
    }
  }

  // =========================================================================
  // Section 4: Injection Attacks, Tampering & Parameter Boundaries
  // =========================================================================

  @Nested
  @DisplayName("4. Injection Attacks & Parameter Boundary Hardening")
  class InjectionAndBoundaryHardening {

    @Test
    @DisplayName("4.1 SQL injection in sort parameter is rejected fail-closed without SQL echoing")
    void testSqlInjectionInSortRejected() throws Exception {
      mockMvc.perform(get("/api/v1/trainees?sort=id; DROP TABLE users;--")
              .header("Authorization", adminToken))
          .andExpect(status().isBadRequest())
          .andExpect(jsonPath("$.code").value("INVALID_SORT_FIELD"))
          .andExpect(jsonPath("$.message").value("Invalid sort field provided"))
          .andExpect(jsonPath("$.message", Matchers.not(Matchers.containsStringIgnoringCase("DROP TABLE"))));
    }

    @Test
    @DisplayName("4.2 Comma-separated sort attempting sensitive traversal is rejected")
    void testCommaSeparatedSensitiveSortRejected() throws Exception {
      mockMvc.perform(get("/api/v1/trainees?sort=id,passwordHash")
              .header("Authorization", adminToken))
          .andExpect(status().isBadRequest())
          .andExpect(jsonPath("$.code").value("INVALID_SORT_FIELD"))
          .andExpect(jsonPath("$.message").value("Invalid sort field provided"))
          .andExpect(jsonPath("$.message", Matchers.not(Matchers.containsStringIgnoringCase("password"))));
    }

    @Test
    @DisplayName("4.3 Excessively long sort parameter (> 64 chars) is rejected")
    void testExcessivelyLongSortParameterRejected() throws Exception {
      String longSort = "a".repeat(65);
      mockMvc.perform(get("/api/v1/trainees?sort=" + longSort)
              .header("Authorization", adminToken))
          .andExpect(status().isBadRequest())
          .andExpect(jsonPath("$.code").value("INVALID_SORT_FIELD"))
          .andExpect(jsonPath("$.message").value("Invalid sort field provided"));
    }

    @Test
    @DisplayName("4.4 Valid sort direction DIAGONAL preserves approved message")
    void testInvalidSortDirectionDiagonalPreserved() throws Exception {
      mockMvc.perform(get("/api/v1/employers?direction=DIAGONAL")
              .header("Authorization", adminToken))
          .andExpect(status().isBadRequest())
          .andExpect(jsonPath("$.code").value("INVALID_SORT_DIRECTION"))
          .andExpect(jsonPath("$.message").value("Invalid sort direction 'DIAGONAL'. Must be ASC or DESC"));
    }

    @Test
    @DisplayName("4.5 Sensitive sort direction is rejected without echoing sensitive term")
    void testSensitiveSortDirectionRejectedWithoutEcho() throws Exception {
      mockMvc.perform(get("/api/v1/employers?direction=passwordHash")
              .header("Authorization", adminToken))
          .andExpect(status().isBadRequest())
          .andExpect(jsonPath("$.code").value("INVALID_SORT_DIRECTION"))
          .andExpect(jsonPath("$.message").value("Invalid sort direction provided"))
          .andExpect(jsonPath("$.message", Matchers.not(Matchers.containsStringIgnoringCase("password"))));
    }

    @Test
    @DisplayName("4.6 SQL injection in sort direction is rejected without echoing payload")
    void testSqlInjectionInDirectionRejected() throws Exception {
      mockMvc.perform(get("/api/v1/employers?direction=ASC; DROP TABLE users;--")
              .header("Authorization", adminToken))
          .andExpect(status().isBadRequest())
          .andExpect(jsonPath("$.code").value("INVALID_SORT_DIRECTION"))
          .andExpect(jsonPath("$.message").value("Invalid sort direction provided"))
          .andExpect(jsonPath("$.message", Matchers.not(Matchers.containsStringIgnoringCase("DROP TABLE"))));
    }
  }

  // =========================================================================
  // Section 5: Search & SQL Wildcard Sanitization Utility
  // =========================================================================

  @Nested
  @DisplayName("5. Search & SQL Wildcard Sanitization Utility Unit Tests")
  class SearchSanitizationUtilityTests {

    @Test
    @DisplayName("5.1 SQL wildcards %, _, and \\ are escaped correctly")
    void testEscapeSqlWildcards() {
      assertThat(SearchSanitizationUtils.escapeSqlWildcards(null)).isNull();
      assertThat(SearchSanitizationUtils.escapeSqlWildcards("")).isEmpty();
      assertThat(SearchSanitizationUtils.escapeSqlWildcards("simple")).isEqualTo("simple");
      assertThat(SearchSanitizationUtils.escapeSqlWildcards("100%")).isEqualTo("100\\%");
      assertThat(SearchSanitizationUtils.escapeSqlWildcards("first_name")).isEqualTo("first\\_name");
      assertThat(SearchSanitizationUtils.escapeSqlWildcards("path\\to\\dir")).isEqualTo("path\\\\to\\\\dir");
      assertThat(SearchSanitizationUtils.escapeSqlWildcards("100%_complete\\done"))
          .isEqualTo("100\\%\\_complete\\\\done");
    }

    @Test
    @DisplayName("5.2 validateAndEscapeSearchTerm enforces max length of 100 characters")
    void testValidateAndEscapeSearchTermBounds() {
      assertThat(SearchSanitizationUtils.validateAndEscapeSearchTerm(null)).isNull();
      assertThat(SearchSanitizationUtils.validateAndEscapeSearchTerm("   ")).isNull();
      assertThat(SearchSanitizationUtils.validateAndEscapeSearchTerm("valid search")).isEqualTo("valid search");
      assertThat(SearchSanitizationUtils.validateAndEscapeSearchTerm("search%")).isEqualTo("search\\%");

      String exactly100 = "a".repeat(100);
      assertThat(SearchSanitizationUtils.validateAndEscapeSearchTerm(exactly100)).isEqualTo(exactly100);

      String length101 = "a".repeat(101);
      assertThatThrownBy(() -> SearchSanitizationUtils.validateAndEscapeSearchTerm(length101))
          .isInstanceOf(BadRequestException.class)
          .hasMessage("Search term must not exceed 100 characters");
    }

    @Test
    @DisplayName("5.3 Pattern builders (contains, startsWith, endsWith) format properly")
    void testPatternBuilders() {
      assertThat(SearchSanitizationUtils.toContainsPattern("java")).isEqualTo("%java%");
      assertThat(SearchSanitizationUtils.toContainsPattern("100%")).isEqualTo("%100\\%%");
      assertThat(SearchSanitizationUtils.toStartsWithPattern("prefix_")).isEqualTo("prefix\\_%");
      assertThat(SearchSanitizationUtils.toEndsWithPattern("_suffix")).isEqualTo("%\\_suffix");

      assertThat(SearchSanitizationUtils.toContainsPattern(null)).isNull();
      assertThat(SearchSanitizationUtils.toContainsPattern("   ")).isNull();
    }

    @Test
    @DisplayName("5.4 containsSqlWildcards correctly identifies presence of wildcards")
    void testContainsSqlWildcards() {
      assertThat(SearchSanitizationUtils.containsSqlWildcards(null)).isFalse();
      assertThat(SearchSanitizationUtils.containsSqlWildcards("plainText123")).isFalse();
      assertThat(SearchSanitizationUtils.containsSqlWildcards("50%")).isTrue();
      assertThat(SearchSanitizationUtils.containsSqlWildcards("user_id")).isTrue();
      assertThat(SearchSanitizationUtils.containsSqlWildcards("c:\\test")).isTrue();
    }

    @Test
    @DisplayName("5.5 isSafeLiteralSearchTerm accurately classifies safe literal terms")
    void testIsSafeLiteralSearchTerm() {
      assertThat(SearchSanitizationUtils.isSafeLiteralSearchTerm(null)).isFalse();
      assertThat(SearchSanitizationUtils.isSafeLiteralSearchTerm("")).isFalse();
      assertThat(SearchSanitizationUtils.isSafeLiteralSearchTerm("cleanTerm")).isTrue();
      assertThat(SearchSanitizationUtils.isSafeLiteralSearchTerm("termWith%")).isFalse();
      assertThat(SearchSanitizationUtils.isSafeLiteralSearchTerm("termWith_")).isFalse();
      assertThat(SearchSanitizationUtils.isSafeLiteralSearchTerm("a".repeat(101))).isFalse();
    }
  }

  // =========================================================================
  // Section 6: Literal Query Parameter Behavior with % and _ at API Boundary
  // =========================================================================

  @Nested
  @DisplayName("6. Literal Query Parameter Behavior at Boundary")
  class LiteralQueryParameterBoundaryTests {

    @Test
    @DisplayName("6.1 Query parameter with % is treated literally and does not cause wildcard expansion")
    void testQueryParameterWithPercentTreatedLiterally() throws Exception {
      // Querying courses by code with a percent sign.
      // Parameterized query must treat '%' literally and not match all courses.
      mockMvc.perform(get("/api/v1/courses?code=COURSE%25")
              .header("Authorization", adminToken))
          .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("6.2 Query parameter with _ is treated literally and does not match single character wildcards")
    void testQueryParameterWithUnderscoreTreatedLiterally() throws Exception {
      // Querying employers by code with an underscore.
      mockMvc.perform(get("/api/v1/employers?code=EMP_NONEXISTENT_999")
              .header("Authorization", adminToken))
          .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("6.3 Query parameter with backslash is treated literally without escape corruption")
    void testQueryParameterWithBackslashTreatedLiterally() throws Exception {
      // Querying training centers by code with a backslash.
      mockMvc.perform(get("/api/v1/training-centers?code=CENTER\\TEST\\123")
              .header("Authorization", adminToken))
          .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("6.4 Exact equality matching on %, _, and \\ across domains does not leak data or cause wildcard expansion")
    void testExactEqualityMatchingAcrossDomains() throws Exception {
      // Training provider by code with %
      mockMvc.perform(get("/api/v1/training-providers?code=PROV%25")
              .header("Authorization", adminToken))
          .andExpect(status().isNotFound());

      // Training batch by code with _
      mockMvc.perform(get("/api/v1/training-batches?code=BATCH_TEST_NONE")
              .header("Authorization", adminToken))
          .andExpect(status().isNotFound());
    }

    private static final Pattern SQL_WILDCARD_PATTERN = Pattern.compile(
        "\\b(LIKE|ILIKE)\\b",
        Pattern.CASE_INSENSITIVE
    );

    private void collectRepositoryInterfaces(Class<?> type, Set<Class<?>> collected) {
      if (type == null) {
        return;
      }
      for (Class<?> iface : type.getInterfaces()) {
        if (iface.getName().startsWith("in.gov.sih.sih26135.repository")) {
          if (collected.add(iface)) {
            collectRepositoryInterfaces(iface, collected);
          }
        } else {
          collectRepositoryInterfaces(iface, collected);
        }
      }
      if (type.getSuperclass() != null) {
        collectRepositoryInterfaces(type.getSuperclass(), collected);
      }
    }

    @Test
    @DisplayName("6.5 Empirical audit: 0 repositories use LIKE/wildcard queries, 100% use parameterized equality")
    void testEmpiricalAuditZeroLikeOrWildcardQueries() {
      Map<String, JpaRepository> repos = applicationContext.getBeansOfType(JpaRepository.class);
      assertThat(repos).isNotEmpty();

      // 1. Traverse the complete repository interface hierarchy, avoiding duplicate inspection
      Set<Class<?>> repositoryInterfaces = new LinkedHashSet<>();
      for (JpaRepository<?, ?> repo : repos.values()) {
        collectRepositoryInterfaces(repo.getClass(), repositoryInterfaces);
      }

      // 2. Verify complete repository coverage across domain & analytics modules
      assertThat(repositoryInterfaces)
          .as("Expected all 137 SIH-26135 Spring Data repository interfaces to be discovered across hierarchy")
          .hasSize(137);

      // 3. Inspect all query methods, derived naming keywords, and @Query annotations
      Set<Method> inspectedMethods = new HashSet<>();
      int verifiedMethodCount = 0;
      int annotatedQueryCount = 0;

      for (Class<?> iface : repositoryInterfaces) {
        for (Method method : iface.getDeclaredMethods()) {
          if (!inspectedMethods.add(method)) {
            continue; // Deduplicate methods visible across interface inheritance
          }
          verifiedMethodCount++;
          String methodName = method.getName();

          // Ensure method naming adheres strictly to equality and does not use wildcard keywords
          assertThat(methodName)
              .as("Repository method %s.%s must not use Spring Data wildcard keywords", iface.getSimpleName(), methodName)
              .doesNotContain("Like")
              .doesNotContain("Containing")
              .doesNotContain("StartingWith")
              .doesNotContain("EndingWith");

          // Inspect Spring Data @Query annotations (JPQL and Native Queries)
          Query queryAnnotation = method.getAnnotation(Query.class);
          if (queryAnnotation != null) {
            annotatedQueryCount++;
            String queryValue = queryAnnotation.value();
            assertThat(SQL_WILDCARD_PATTERN.matcher(queryValue).find())
                .as("Query in %s.%s must not contain SQL LIKE/ILIKE wildcards (found: '%s')",
                    iface.getSimpleName(), methodName, queryValue)
                .isFalse();
          }

          // Also check @NativeQuery annotation if present on classpath
          try {
            Class<? extends java.lang.annotation.Annotation> nqCls =
                (Class<? extends java.lang.annotation.Annotation>) Class.forName("org.springframework.data.jpa.repository.NativeQuery");
            java.lang.annotation.Annotation nq = method.getAnnotation(nqCls);
            if (nq != null) {
              annotatedQueryCount++;
              String nqValue = (String) nqCls.getMethod("value").invoke(nq);
              assertThat(SQL_WILDCARD_PATTERN.matcher(nqValue).find())
                  .as("NativeQuery in %s.%s must not contain SQL LIKE/ILIKE wildcards (found: '%s')",
                      iface.getSimpleName(), methodName, nqValue)
                  .isFalse();
            }
          } catch (Exception ignored) {
          }
        }
      }

      // 4. Verify that non-trivial method coverage exists across the 137 repositories
      assertThat(verifiedMethodCount)
          .as("Expected substantial query method coverage across all 137 repository interfaces")
          .isGreaterThanOrEqualTo(200);
      assertThat(annotatedQueryCount)
          .as("Expected exact count of annotated queries in repository layer")
          .isEqualTo(2);
    }
  }

  // =========================================================================
  // Section 7: ReDoS Immunity & Static Regex Performance Verification
  // =========================================================================

  @Nested
  @DisplayName("7. ReDoS Immunity & Linear Execution Verification")
  class ReDoSImmunityVerification {

    @Test
    @DisplayName("7.1 PaginationUtils.isSensitiveSortField executes linearly without catastrophic backtracking")
    void testSensitiveSortFieldCheckPerformance() {
      // Test with long adversarial input
      String adversarial = "a".repeat(1000) + "password" + "b".repeat(1000);
      long start = System.nanoTime();
      boolean sensitive = PaginationUtils.isSensitiveSortField(adversarial);
      long durationMs = (System.nanoTime() - start) / 1_000_000;

      assertThat(sensitive).isTrue();
      assertThat(durationMs).isLessThan(50); // Must complete in well under 50ms
    }

    @Test
    @DisplayName("7.2 PaginationUtils.buildPageable enforces stable tie-breaker id ASC")
    void testStableTieBreakerEnforced() {
      Pageable pageable = PaginationUtils.buildPageable(
          0, 20, "courseName", "DESC",
          PaginationUtils.COURSE_SORT_FIELDS, "id"
      );

      Sort sort = pageable.getSort();
      Sort.Order firstOrder = sort.getOrderFor("courseName");
      Sort.Order tieBreakerOrder = sort.getOrderFor("id");

      assertThat(firstOrder).isNotNull();
      assertThat(firstOrder.getDirection()).isEqualTo(Sort.Direction.DESC);
      assertThat(tieBreakerOrder).isNotNull();
      assertThat(tieBreakerOrder.getDirection()).isEqualTo(Sort.Direction.ASC);
    }
  }
}
