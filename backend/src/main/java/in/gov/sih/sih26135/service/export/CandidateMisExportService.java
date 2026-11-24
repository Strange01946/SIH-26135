package in.gov.sih.sih26135.service.export;

import com.fasterxml.jackson.databind.ObjectMapper;
import in.gov.sih.sih26135.config.AnalyticsExportProperties;
import in.gov.sih.sih26135.dto.request.AnalyticsFilterCriteria;
import in.gov.sih.sih26135.exception.BadRequestException;
import in.gov.sih.sih26135.repository.analytics.util.AnalyticsQueryFilterBuilder;
import in.gov.sih.sih26135.service.privacy.AnalyticsPrivacySuppressionService;
import java.io.IOException;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.ConnectionCallback;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

@Service
public class CandidateMisExportService {
  public record Column(String key, String label) {}

  private static final List<Column> COLUMNS = List.of(
      new Column("financialYear", "Financial year"), new Column("scheme", "Scheme"),
      new Column("sector", "Sector"), new Column("state", "State"),
      new Column("district", "District"), new Column("gender", "Gender"),
      new Column("outcomeStage", "Training outcome"));
  private static final String RELEASE = """
      WITH latest AS (
        SELECT v.enrollment_id, v.trainee_id, v.enrollment_date, v.scheme_id, v.course_id,
               v.trainee_state_id, v.trainee_district_id,
               ROW_NUMBER() OVER (PARTITION BY v.trainee_id ORDER BY v.enrollment_date DESC, v.enrollment_id DESC) rn,
               CASE WHEN v.has_joined_placement_flag = 1 THEN 'Placed'
                    WHEN v.has_issued_certificate_flag = 1 THEN 'Certified'
                    WHEN v.is_completed_flag = 1 THEN 'Completed'
                    WHEN v.enrollment_status_code IN ('ENROLLED', 'IN_PROGRESS') THEN 'In training'
                    ELSE 'Other outcome' END AS outcomeStage
        FROM vw_enrollment_outcome_fact v WHERE v.enrollment_date <= CURDATE()
      ), scoped AS (
        SELECT v.trainee_id, v.scheme_id, c.sector_id, v.trainee_state_id, v.trainee_district_id,
               YEAR(DATE_SUB(v.enrollment_date, INTERVAL 3 MONTH)) AS fiscal_start,
               t.gender_id, v.outcomeStage
        FROM latest v
        JOIN trainees t ON t.trainee_id = v.trainee_id
        JOIN courses c ON c.course_id = v.course_id
        JOIN trainee_consents consent ON consent.trainee_consent_id = (
          SELECT tc.trainee_consent_id FROM trainee_consents tc
          JOIN ref_consent_type ct USING (consent_type_id)
          WHERE tc.trainee_id = v.trainee_id AND ct.consent_code = 'ANALYTICS'
          ORDER BY tc.updated_at DESC, tc.trainee_consent_id DESC LIMIT 1
        )
        JOIN ref_consent_status cs ON cs.consent_status_id = consent.consent_status_id
        WHERE v.rn = 1 AND c.deleted_at IS NULL AND cs.status_code = 'GRANTED'
          AND (consent.granted_at IS NULL OR consent.granted_at <= NOW())
          AND (consent.revoked_at IS NULL OR consent.revoked_at > NOW())
          AND (consent.expires_at IS NULL OR consent.expires_at > NOW())
      """;
  private static final String RELEASE_SUFFIX = """
      ), counted AS (
        SELECT scoped.*, COUNT(*) OVER (PARTITION BY fiscal_start, scheme_id, sector_id,
            trainee_state_id, trainee_district_id, gender_id, outcomeStage) AS group_size
        FROM scoped
      ), released AS (
        SELECT * FROM counted WHERE group_size >= ?
      )
      """;
  private static final String EXPORT_ROWS = """
      SELECT CONCAT('FY', r.fiscal_start, '-', LPAD(MOD(r.fiscal_start + 1, 100), 2, '0')) AS financialYear,
             sc.scheme_name AS scheme, se.sector_name AS sector, st.state_name AS state,
             d.district_name AS district, g.gender_name AS gender, r.outcomeStage, COUNT(*) OVER () AS released_rows
      FROM released r JOIN schemes sc USING (scheme_id) JOIN sectors se USING (sector_id)
      LEFT JOIN states st ON st.state_id = r.trainee_state_id
      LEFT JOIN districts d ON d.district_id = r.trainee_district_id
      LEFT JOIN ref_gender g ON g.gender_id = r.gender_id
      ORDER BY r.fiscal_start DESC, r.scheme_id, r.sector_id, r.trainee_state_id,
               r.trainee_district_id, r.gender_id, r.outcomeStage
      """;

  private static final Logger log = LoggerFactory.getLogger(CandidateMisExportService.class);
  private static final long K = AnalyticsPrivacySuppressionService.MIN_ANONYMITY_THRESHOLD;
  private final JdbcTemplate jdbc;
  private final AnalyticsExportProperties properties;
  private final ExportRateLimiter limits;
  private final ObjectMapper mapper;

  public CandidateMisExportService(JdbcTemplate jdbc, AnalyticsExportProperties properties,
      ExportRateLimiter limits, ObjectMapper mapper) {
    this.jdbc = jdbc;
    this.properties = properties;
    this.limits = limits;
    this.mapper = mapper;
  }

  public List<Column> columns() {
    return COLUMNS;
  }

  public StreamingResponseBody prepare(String format, List<String> requestedColumns,
      AnalyticsFilterCriteria criteria, long userId) {
    if (!"csv".equals(format) && !"json".equals(format)) {
      throw new BadRequestException("Choose CSV or JSON", "INVALID_EXPORT_FORMAT");
    }
    if (criteria.getStartDate() != null || criteria.getEndDate() != null || criteria.getCalendarYear() != null
        || criteria.getCourseId() != null || criteria.getProgramId() != null) {
      throw new BadRequestException("This export supports whole financial years and the listed cohort filters",
          "UNSUPPORTED_EXPORT_FILTER");
    }
    var projection = selectColumns(requestedColumns);
    var filter = AnalyticsQueryFilterBuilder.buildEnrollmentFactPredicates(criteria, "v");
    var parameters = new ArrayList<>(filter.getParameters());
    parameters.add(K);
    String released = RELEASE + filter.getSqlFragment() + RELEASE_SUFFIX;
    limits.checkAndAcquire(userId);
    boolean prepared = false;
    try {
      checkRows(jdbc.queryForObject(released + "SELECT COUNT(*) FROM released", Long.class, parameters.toArray()));
      prepared = true;
      return output -> write(output, released + EXPORT_ROWS, parameters, projection, format, userId, criteria.getFiscalYear());
    } finally {
      if (!prepared) {
        limits.release(userId);
      }
    }
  }

  private static List<Column> selectColumns(List<String> requested) {
    if (requested == null) {
      return COLUMNS;
    }
    var unique = new java.util.LinkedHashSet<>(requested);
    if (unique.isEmpty() || unique.size() != requested.size()) {
      throw new BadRequestException("Select one or more distinct export columns", "INVALID_EXPORT_COLUMNS");
    }
    return requested.stream().map(key -> COLUMNS.stream().filter(column -> column.key().equals(key)).findFirst()
        .orElseThrow(() -> new BadRequestException("An export column is not supported", "INVALID_EXPORT_COLUMNS"))).toList();
  }

  private void checkRows(long count) {
    if (count > properties.getRowLimitMax()) {
      throw new BadRequestException("The export exceeds " + properties.getRowLimitMax()
          + " released rows. Select a narrower cohort.", "EXPORT_ROW_LIMIT_EXCEEDED");
    }
  }

  private void write(OutputStream output, String sql, List<Object> parameters, List<Column> projection,
      String format, long userId, String fiscalYear) throws IOException {
    try {
      Integer count = jdbc.execute((ConnectionCallback<Integer>) con -> {
        try (PreparedStatement statement = con.prepareStatement(sql, ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY)) {
          statement.setFetchSize(Integer.MIN_VALUE);
          for (var i = 0; i < parameters.size(); i++) {
            statement.setObject(i + 1, parameters.get(i));
          }
          try (ResultSet rows = statement.executeQuery()) {
            boolean hasRow = rows.next();
            if (hasRow) {
              checkRows(rows.getLong("released_rows"));
            }
            return "csv".equals(format) ? csv(output, projection, rows, hasRow)
                : json(output, projection, rows, hasRow, fiscalYear);
          } catch (IOException writeFailure) {
            throw new UncheckedIOException(writeFailure);
          }
        }
      });
      log.info("MIS candidate outcome export completed: userId={}, format={}, rows={}", userId, format, count);
    } catch (UncheckedIOException interrupted) {
      log.info("MIS candidate outcome stream ended before completion: userId={}, format={}", userId, format);
      throw interrupted.getCause();
    } finally {
      limits.release(userId);
    }
  }

  private int csv(OutputStream output, List<Column> projection, ResultSet rows, boolean hasRow) throws IOException, SQLException {
    try (var writer = new SafeCsvStreamWriter(output)) {
      writer.writeHeader(projection.stream().map(Column::label).toList());
      int count = 0;
      while (hasRow) {
        List<Object> values = new ArrayList<>(projection.size());
        for (Column column : projection) {
          values.add(rows.getString(column.key()));
        }
        writer.writeRow(values);
        count++;
        hasRow = rows.next();
      }
      return count;
    }
  }

  private int json(OutputStream output, List<Column> projection, ResultSet rows, boolean hasRow, String fiscalYear)
      throws IOException, SQLException {
    try (var writer = new StructuredJsonStreamWriter(output, mapper)) {
      writer.writeRootStart("CANDIDATE_OUTCOMES", fiscalYear == null ? Map.of() : Map.of("fiscalYear", fiscalYear),
          Map.of("level", "NATIONWIDE", "basis", "LATEST_ENROLLMENT", "identifiersIncluded", false), Instant.now().toString());
      writer.startRows();
      while (hasRow) {
        var projected = new LinkedHashMap<String, Object>();
        for (Column column : projection) {
          projected.put(column.key(), rows.getString(column.key()));
        }
        writer.writeRow(projected);
        hasRow = rows.next();
      }
      writer.endRows();
      writer.writeMetadata(writer.getRowCount(), K);
      writer.writeRootEnd();
      return writer.getRowCount();
    }
  }
}
