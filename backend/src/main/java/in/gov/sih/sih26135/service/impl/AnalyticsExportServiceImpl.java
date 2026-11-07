package in.gov.sih.sih26135.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import in.gov.sih.sih26135.config.AnalyticsExportProperties;
import in.gov.sih.sih26135.dto.request.AnalyticsFilterCriteria;
import in.gov.sih.sih26135.dto.response.export.DsdpExportRow;
import in.gov.sih.sih26135.dto.response.export.ProviderScorecardExportRow;
import in.gov.sih.sih26135.dto.response.export.QprExportRow;
import in.gov.sih.sih26135.exception.BadRequestException;
import in.gov.sih.sih26135.exception.ResourceNotFoundException;
import in.gov.sih.sih26135.repository.analytics.AnalyticsExportRepository;
import in.gov.sih.sih26135.repository.analytics.AnalyticsExportRepository.DistrictMetadata;
import in.gov.sih.sih26135.service.AnalyticsExportService;
import in.gov.sih.sih26135.service.export.ExportRateLimiter;
import in.gov.sih.sih26135.service.export.SafeCsvStreamWriter;
import in.gov.sih.sih26135.service.export.StructuredJsonStreamWriter;
import in.gov.sih.sih26135.service.privacy.AnalyticsPrivacySuppressionService;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

/**
 * Production implementation of {@link AnalyticsExportService}.
 * Enforces preflight validation, rate limits, concurrency control, bounded chunked iteration,
 * and Phase 27.14 statistical disclosure controls.
 */
@Service
public class AnalyticsExportServiceImpl implements AnalyticsExportService {

  private static final Logger log = LoggerFactory.getLogger(AnalyticsExportServiceImpl.class);

  private static final int CHUNK_SIZE = 1000;
  private static final Pattern FY_PATTERN = Pattern.compile("^FY\\d{4}-\\d{2}$");

  private final AnalyticsExportRepository repository;
  private final ExportRateLimiter rateLimiter;
  private final AnalyticsExportProperties properties;
  private final AnalyticsPrivacySuppressionService privacyService;
  private final ObjectMapper objectMapper;

  public AnalyticsExportServiceImpl(
      AnalyticsExportRepository repository,
      ExportRateLimiter rateLimiter,
      AnalyticsExportProperties properties,
      AnalyticsPrivacySuppressionService privacyService,
      ObjectMapper objectMapper) {
    this.repository = repository;
    this.rateLimiter = rateLimiter;
    this.properties = properties;
    this.privacyService = privacyService;
    this.objectMapper = objectMapper;
  }

  // =========================================================================
  // 1. DSDP Exports
  // =========================================================================

  @Override
  public StreamingResponseBody exportDsdpCsv(Long districtId, AnalyticsFilterCriteria criteria, Long userId) {
    rateLimiter.checkAndAcquire(userId);
    boolean preflightSucceeded = false;
    try {
      DistrictMetadata meta = resolveDistrictMetadata(districtId);
      int totalRows = checkRowLimits(repository.countDsdpRows(districtId, criteria));
      preflightSucceeded = true;

      return outputStream -> {
        long start = System.currentTimeMillis();
        int written = 0;
        try (SafeCsvStreamWriter csvWriter = new SafeCsvStreamWriter(outputStream)) {
          csvWriter.writeHeader(List.of(
              "District ID", "District Code", "District Name",
              "State Code", "State Name",
              "Course Code", "Course Name", "Sector Name",
              "Enrollments", "Trainees", "Completions", "Certifications",
              "Placements", "Employed",
              "Completion Rate (%)", "Certification Rate (%)",
              "Placement Rate (%)", "Employment Rate (%)",
              "Avg Starting Wage"
          ));

          int offset = 0;
          while (offset < totalRows) {
            int batchLimit = Math.min(CHUNK_SIZE, totalRows - offset);
            List<DsdpExportRow> rawChunk = repository.fetchDsdpRows(districtId, meta, criteria, batchLimit, offset);
            if (rawChunk.isEmpty()) {
              break;
            }
            List<DsdpExportRow> safeChunk = privacyService.applyPrivacyGuardrailsToDsdp(rawChunk);
            for (DsdpExportRow row : safeChunk) {
              csvWriter.writeRow(List.of(
                  row.districtId() != null ? row.districtId() : "",
                  row.districtCode() != null ? row.districtCode() : "",
                  row.districtName() != null ? row.districtName() : "",
                  row.stateCode() != null ? row.stateCode() : "",
                  row.stateName() != null ? row.stateName() : "",
                  row.courseCode() != null ? row.courseCode() : "",
                  row.courseName() != null ? row.courseName() : "",
                  row.sectorName() != null ? row.sectorName() : "",
                  row.enrollmentCount() != null ? row.enrollmentCount() : "",
                  row.traineeCount() != null ? row.traineeCount() : "",
                  row.completedCount() != null ? row.completedCount() : "",
                  row.certifiedCount() != null ? row.certifiedCount() : "",
                  row.joinedPlacementCount() != null ? row.joinedPlacementCount() : "",
                  row.employedCount() != null ? row.employedCount() : "",
                  row.completionRatePct() != null ? row.completionRatePct() : "",
                  row.certificationRatePct() != null ? row.certificationRatePct() : "",
                  row.placementRatePct() != null ? row.placementRatePct() : "",
                  row.employmentRatePct() != null ? row.employmentRatePct() : "",
                  row.avgStartingWage() != null ? row.avgStartingWage() : ""
              ));
              written++;
            }
            offset += rawChunk.size();
            if (rawChunk.size() < batchLimit) {
              break;
            }
          }
          logAudit("SUCCESS", userId, "DSDP", "CSV", written, System.currentTimeMillis() - start);
        } catch (Exception e) {
          log.error("MIS export streaming failed for DSDP CSV: userId={}", userId, e);
          logAudit("FAILED", userId, "DSDP", "CSV", written, System.currentTimeMillis() - start);
          throw e;
        } finally {
          rateLimiter.release(userId);
        }
      };
    } finally {
      if (!preflightSucceeded) {
        rateLimiter.release(userId);
      }
    }
  }

  @Override
  public StreamingResponseBody exportDsdpJson(Long districtId, AnalyticsFilterCriteria criteria, Long userId) {
    rateLimiter.checkAndAcquire(userId);
    boolean preflightSucceeded = false;
    try {
      DistrictMetadata meta = resolveDistrictMetadata(districtId);
      int totalRows = checkRowLimits(repository.countDsdpRows(districtId, criteria));
      preflightSucceeded = true;

      return outputStream -> {
        long start = System.currentTimeMillis();
        int written = 0;
        try (StructuredJsonStreamWriter jsonWriter = new StructuredJsonStreamWriter(outputStream, objectMapper)) {
          Map<String, Object> reportingPeriod = buildReportingPeriodMap(criteria);
          Map<String, Object> scope = new LinkedHashMap<>();
          scope.put("level", "DISTRICT");
          scope.put("districtId", meta.districtId());
          scope.put("districtCode", meta.districtCode());
          scope.put("districtName", meta.districtName());
          scope.put("stateId", meta.stateId());
          scope.put("stateCode", meta.stateCode());
          scope.put("stateName", meta.stateName());

          jsonWriter.writeRootStart("DSDP", reportingPeriod, scope, Instant.now().toString());
          jsonWriter.startRows();

          int offset = 0;
          while (offset < totalRows) {
            int batchLimit = Math.min(CHUNK_SIZE, totalRows - offset);
            List<DsdpExportRow> rawChunk = repository.fetchDsdpRows(districtId, meta, criteria, batchLimit, offset);
            if (rawChunk.isEmpty()) {
              break;
            }
            List<DsdpExportRow> safeChunk = privacyService.applyPrivacyGuardrailsToDsdp(rawChunk);
            for (DsdpExportRow row : safeChunk) {
              jsonWriter.writeRow(row);
              written++;
            }
            offset += rawChunk.size();
            if (rawChunk.size() < batchLimit) {
              break;
            }
          }

          jsonWriter.endRows();
          jsonWriter.writeMetadata(written, AnalyticsPrivacySuppressionService.MIN_ANONYMITY_THRESHOLD);
          jsonWriter.writeRootEnd();
          logAudit("SUCCESS", userId, "DSDP", "JSON", written, System.currentTimeMillis() - start);
        } catch (Exception e) {
          log.error("MIS export streaming failed for DSDP JSON: userId={}", userId, e);
          logAudit("FAILED", userId, "DSDP", "JSON", written, System.currentTimeMillis() - start);
          throw e;
        } finally {
          rateLimiter.release(userId);
        }
      };
    } finally {
      if (!preflightSucceeded) {
        rateLimiter.release(userId);
      }
    }
  }

  // =========================================================================
  // 2. QPR Exports
  // =========================================================================

  @Override
  public StreamingResponseBody exportQprCsv(String fiscalYear, String quarter, AnalyticsFilterCriteria criteria, Long userId) {
    rateLimiter.checkAndAcquire(userId);
    boolean preflightSucceeded = false;
    try {
      DateRange range = resolveQuarterDates(fiscalYear, quarter);
      int totalRows = checkRowLimits(repository.countQprRows());
      preflightSucceeded = true;

      return outputStream -> {
        long start = System.currentTimeMillis();
        int written = 0;
        try (SafeCsvStreamWriter csvWriter = new SafeCsvStreamWriter(outputStream)) {
          csvWriter.writeHeader(List.of(
              "State ID", "State Code", "State Name",
              "Fiscal Year", "Quarter",
              "Enrollments", "Trainees", "Completions", "Certifications",
              "Placements", "Employed",
              "Completion Rate (%)", "Certification Rate (%)",
              "Placement Rate (%)", "Employment Rate (%)",
              "Rank"
          ));

          // State QPR table is bounded (~36 states/UTs); fetch all and apply complementary suppression
          List<QprExportRow> rawRows = repository.fetchQprRows(
              range.startDate(), range.endDate(), range.fiscalYear(), range.quarter(),
              criteria, totalRows, 0
          );
          List<QprExportRow> safeRows = privacyService.applyPrivacyGuardrailsToQpr(rawRows);

          for (QprExportRow row : safeRows) {
            csvWriter.writeRow(List.of(
                row.stateId() != null ? row.stateId() : "",
                row.stateCode() != null ? row.stateCode() : "",
                row.stateName() != null ? row.stateName() : "",
                row.fiscalYear() != null ? row.fiscalYear() : "",
                row.quarter() != null ? row.quarter() : "",
                row.enrollmentCount() != null ? row.enrollmentCount() : "",
                row.traineeCount() != null ? row.traineeCount() : "",
                row.completedCount() != null ? row.completedCount() : "",
                row.certifiedCount() != null ? row.certifiedCount() : "",
                row.joinedPlacementCount() != null ? row.joinedPlacementCount() : "",
                row.employedCount() != null ? row.employedCount() : "",
                row.completionRatePct() != null ? row.completionRatePct() : "",
                row.certificationRatePct() != null ? row.certificationRatePct() : "",
                row.placementRatePct() != null ? row.placementRatePct() : "",
                row.employmentRatePct() != null ? row.employmentRatePct() : "",
                row.rank() != null ? row.rank() : ""
            ));
            written++;
          }
          logAudit("SUCCESS", userId, "QPR", "CSV", written, System.currentTimeMillis() - start);
        } catch (Exception e) {
          log.error("MIS export streaming failed for QPR CSV: userId={}", userId, e);
          logAudit("FAILED", userId, "QPR", "CSV", written, System.currentTimeMillis() - start);
          throw e;
        } finally {
          rateLimiter.release(userId);
        }
      };
    } finally {
      if (!preflightSucceeded) {
        rateLimiter.release(userId);
      }
    }
  }

  @Override
  public StreamingResponseBody exportQprJson(String fiscalYear, String quarter, AnalyticsFilterCriteria criteria, Long userId) {
    rateLimiter.checkAndAcquire(userId);
    boolean preflightSucceeded = false;
    try {
      DateRange range = resolveQuarterDates(fiscalYear, quarter);
      int totalRows = checkRowLimits(repository.countQprRows());
      preflightSucceeded = true;

      return outputStream -> {
        long start = System.currentTimeMillis();
        int written = 0;
        try (StructuredJsonStreamWriter jsonWriter = new StructuredJsonStreamWriter(outputStream, objectMapper)) {
          Map<String, Object> reportingPeriod = new LinkedHashMap<>();
          reportingPeriod.put("fiscalYear", range.fiscalYear());
          reportingPeriod.put("quarter", range.quarter());
          reportingPeriod.put("startDate", range.startDate().toString());
          reportingPeriod.put("endDate", range.endDate().toString());

          Map<String, Object> scope = Map.of("level", "NATIONAL");

          jsonWriter.writeRootStart("QPR", reportingPeriod, scope, Instant.now().toString());

          // 1. Fetch raw rows and apply privacy guardrails including complementary suppression
          List<QprExportRow> rawRows = repository.fetchQprRows(
              range.startDate(), range.endDate(), range.fiscalYear(), range.quarter(),
              criteria, totalRows, 0
          );
          List<QprExportRow> safeRows = privacyService.applyPrivacyGuardrailsToQpr(rawRows);

          // 2. Fetch National Summary and apply privacy controls with partition anti-reconstruction
          Map<String, Object> rawSummary = repository.fetchQprNationalSummary(range.startDate(), range.endDate(), criteria);
          Map<String, Object> safeSummary = sanitizeQprSummary(rawSummary, safeRows);
          jsonWriter.writeSummary(safeSummary);

          jsonWriter.startRows();
          for (QprExportRow row : safeRows) {
            jsonWriter.writeRow(row);
            written++;
          }

          jsonWriter.endRows();
          jsonWriter.writeMetadata(written, AnalyticsPrivacySuppressionService.MIN_ANONYMITY_THRESHOLD);
          jsonWriter.writeRootEnd();
          logAudit("SUCCESS", userId, "QPR", "JSON", written, System.currentTimeMillis() - start);
        } catch (Exception e) {
          log.error("MIS export streaming failed for QPR JSON: userId={}", userId, e);
          logAudit("FAILED", userId, "QPR", "JSON", written, System.currentTimeMillis() - start);
          throw e;
        } finally {
          rateLimiter.release(userId);
        }
      };
    } finally {
      if (!preflightSucceeded) {
        rateLimiter.release(userId);
      }
    }
  }

  // =========================================================================
  // 3. Provider Scorecard Exports
  // =========================================================================

  @Override
  public StreamingResponseBody exportProviderScorecardCsv(Long providerId, Long stateId, AnalyticsFilterCriteria criteria, Long userId) {
    rateLimiter.checkAndAcquire(userId);
    boolean preflightSucceeded = false;
    try {
      int totalRows = checkRowLimits(repository.countProviderScorecardRows(stateId, providerId));
      preflightSucceeded = true;

      return outputStream -> {
        long start = System.currentTimeMillis();
        int written = 0;
        try (SafeCsvStreamWriter csvWriter = new SafeCsvStreamWriter(outputStream)) {
          csvWriter.writeHeader(List.of(
              "Provider ID", "Provider Code", "Provider Name",
              "State", "District",
              "Active Batch Capacity",
              "Enrollments", "Trainees", "Completions", "Certifications",
              "Placements", "Employed",
              "Completion Rate (%)", "Certification Rate (%)",
              "Placement Rate (%)", "Employment Rate (%)",
              "Rank"
          ));

          int offset = 0;
          while (offset < totalRows) {
            int batchLimit = Math.min(CHUNK_SIZE, totalRows - offset);
            List<ProviderScorecardExportRow> rawChunk = repository.fetchProviderScorecardRows(
                stateId, providerId, criteria, batchLimit, offset
            );
            if (rawChunk.isEmpty()) {
              break;
            }
            List<ProviderScorecardExportRow> safeChunk = privacyService.applyPrivacyGuardrailsToProviderScorecards(rawChunk);
            for (ProviderScorecardExportRow row : safeChunk) {
              csvWriter.writeRow(List.of(
                  row.providerId() != null ? row.providerId() : "",
                  row.providerCode() != null ? row.providerCode() : "",
                  row.providerName() != null ? row.providerName() : "",
                  row.stateName() != null ? row.stateName() : "",
                  row.districtName() != null ? row.districtName() : "",
                  row.activeBatchCapacity() != null ? row.activeBatchCapacity() : "",
                  row.enrollmentCount() != null ? row.enrollmentCount() : "",
                  row.traineeCount() != null ? row.traineeCount() : "",
                  row.completedCount() != null ? row.completedCount() : "",
                  row.certifiedCount() != null ? row.certifiedCount() : "",
                  row.joinedPlacementCount() != null ? row.joinedPlacementCount() : "",
                  row.employedCount() != null ? row.employedCount() : "",
                  row.completionRatePct() != null ? row.completionRatePct() : "",
                  row.certificationRatePct() != null ? row.certificationRatePct() : "",
                  row.placementRatePct() != null ? row.placementRatePct() : "",
                  row.employmentRatePct() != null ? row.employmentRatePct() : "",
                  row.rank() != null ? row.rank() : ""
              ));
              written++;
            }
            offset += rawChunk.size();
            if (rawChunk.size() < batchLimit) {
              break;
            }
          }
          logAudit("SUCCESS", userId, "PROVIDER_SCORECARD", "CSV", written, System.currentTimeMillis() - start);
        } catch (Exception e) {
          log.error("MIS export streaming failed for PROVIDER_SCORECARD CSV: userId={}", userId, e);
          logAudit("FAILED", userId, "PROVIDER_SCORECARD", "CSV", written, System.currentTimeMillis() - start);
          throw e;
        } finally {
          rateLimiter.release(userId);
        }
      };
    } finally {
      if (!preflightSucceeded) {
        rateLimiter.release(userId);
      }
    }
  }

  @Override
  public StreamingResponseBody exportProviderScorecardJson(Long providerId, Long stateId, AnalyticsFilterCriteria criteria, Long userId) {
    rateLimiter.checkAndAcquire(userId);
    boolean preflightSucceeded = false;
    try {
      int totalRows = checkRowLimits(repository.countProviderScorecardRows(stateId, providerId));
      preflightSucceeded = true;

      return outputStream -> {
        long start = System.currentTimeMillis();
        int written = 0;
        try (StructuredJsonStreamWriter jsonWriter = new StructuredJsonStreamWriter(outputStream, objectMapper)) {
          Map<String, Object> reportingPeriod = buildReportingPeriodMap(criteria);
          Map<String, Object> scope = new LinkedHashMap<>();
          if (providerId != null) {
            scope.put("level", "PROVIDER");
            scope.put("providerId", providerId);
          } else if (stateId != null) {
            scope.put("level", "STATE");
            scope.put("stateId", stateId);
          } else {
            scope.put("level", "NATIONAL");
          }

          jsonWriter.writeRootStart("PROVIDER_SCORECARD", reportingPeriod, scope, Instant.now().toString());
          jsonWriter.startRows();

          int offset = 0;
          while (offset < totalRows) {
            int batchLimit = Math.min(CHUNK_SIZE, totalRows - offset);
            List<ProviderScorecardExportRow> rawChunk = repository.fetchProviderScorecardRows(
                stateId, providerId, criteria, batchLimit, offset
            );
            if (rawChunk.isEmpty()) {
              break;
            }
            List<ProviderScorecardExportRow> safeChunk = privacyService.applyPrivacyGuardrailsToProviderScorecards(rawChunk);
            for (ProviderScorecardExportRow row : safeChunk) {
              jsonWriter.writeRow(row);
              written++;
            }
            offset += rawChunk.size();
            if (rawChunk.size() < batchLimit) {
              break;
            }
          }

          jsonWriter.endRows();
          jsonWriter.writeMetadata(written, AnalyticsPrivacySuppressionService.MIN_ANONYMITY_THRESHOLD);
          jsonWriter.writeRootEnd();
          logAudit("SUCCESS", userId, "PROVIDER_SCORECARD", "JSON", written, System.currentTimeMillis() - start);
        } catch (Exception e) {
          log.error("MIS export streaming failed for PROVIDER_SCORECARD JSON: userId={}", userId, e);
          logAudit("FAILED", userId, "PROVIDER_SCORECARD", "JSON", written, System.currentTimeMillis() - start);
          throw e;
        } finally {
          rateLimiter.release(userId);
        }
      };
    } finally {
      if (!preflightSucceeded) {
        rateLimiter.release(userId);
      }
    }
  }

  // =========================================================================
  // Helper Methods
  // =========================================================================

  private DistrictMetadata resolveDistrictMetadata(Long districtId) {
    if (districtId == null) {
      throw new BadRequestException("districtId parameter is required", "MISSING_REQUIRED_PARAMETER");
    }
    return repository.findDistrictMetadata(districtId)
        .orElseThrow(() -> new ResourceNotFoundException("District not found with ID: " + districtId));
  }

  private int checkRowLimits(int count) {
    if (count > properties.getRowLimitMax()) {
      log.warn("Export rejected: row count {} exceeds max limit {}", count, properties.getRowLimitMax());
      throw new BadRequestException(
          "Requested export exceeds maximum allowed row limit of " + properties.getRowLimitMax()
              + " rows. Current matching rows: " + count + ". Please apply narrower filters.",
          "EXPORT_ROW_LIMIT_EXCEEDED"
      );
    }
    return count;
  }

  public record DateRange(LocalDate startDate, LocalDate endDate, String fiscalYear, String quarter) {}

  private DateRange resolveQuarterDates(String fiscalYear, String quarter) {
    String effectiveFy = (fiscalYear != null && !fiscalYear.isBlank()) ? fiscalYear.trim().toUpperCase() : "FY2026-27";
    if (!FY_PATTERN.matcher(effectiveFy).matches()) {
      throw new BadRequestException("Fiscal year must match format 'FYyyyy-yy' (e.g. 'FY2026-27')", "INVALID_FISCAL_YEAR");
    }

    int startYear = Integer.parseInt(effectiveFy.substring(2, 6));
    int endYearSuffix = Integer.parseInt(effectiveFy.substring(7, 9));
    int expectedSuffix = (startYear + 1) % 100;
    if (endYearSuffix != expectedSuffix) {
      throw new BadRequestException(
          "Invalid fiscal year sequence. '" + effectiveFy + "' must end with '-"
              + String.format("%02d", expectedSuffix) + "'",
          "INVALID_FISCAL_YEAR"
      );
    }

    String effectiveQuarter = (quarter != null && !quarter.isBlank()) ? quarter.trim().toUpperCase() : "Q1";
    LocalDate start;
    LocalDate end;

    switch (effectiveQuarter) {
      case "Q1" -> {
        start = LocalDate.of(startYear, 4, 1);
        end = LocalDate.of(startYear, 6, 30);
      }
      case "Q2" -> {
        start = LocalDate.of(startYear, 7, 1);
        end = LocalDate.of(startYear, 9, 30);
      }
      case "Q3" -> {
        start = LocalDate.of(startYear, 10, 1);
        end = LocalDate.of(startYear, 12, 31);
      }
      case "Q4" -> {
        start = LocalDate.of(startYear + 1, 1, 1);
        end = LocalDate.of(startYear + 1, 3, 31);
      }
      case "ALL" -> {
        start = LocalDate.of(startYear, 4, 1);
        end = LocalDate.of(startYear + 1, 3, 31);
      }
      default -> throw new BadRequestException(
          "Invalid quarter '" + quarter + "'. Expected Q1, Q2, Q3, Q4, or ALL",
          "INVALID_QUARTER"
      );
    }

    return new DateRange(start, end, effectiveFy, effectiveQuarter);
  }

  private Map<String, Object> buildReportingPeriodMap(AnalyticsFilterCriteria criteria) {
    Map<String, Object> map = new LinkedHashMap<>();
    if (criteria != null) {
      if (criteria.getFiscalYear() != null) {
        map.put("fiscalYear", criteria.getFiscalYear());
      }
      LocalDate s = criteria.resolveEffectiveStartDate();
      LocalDate e = criteria.resolveEffectiveEndDate();
      if (s != null) {
        map.put("startDate", s.toString());
      }
      if (e != null) {
        map.put("endDate", e.toString());
      }
    }
    return map;
  }

  private Map<String, Object> sanitizeQprSummary(Map<String, Object> raw, List<QprExportRow> safeRows) {
    Map<String, Object> safe = new LinkedHashMap<>();
    if (raw == null || raw.isEmpty()) {
      return safe;
    }
    Number trainees = (Number) raw.get("totalTrainees");
    long traineeCount = trainees != null ? trainees.longValue() : 0L;

    if (traineeCount > 0 && traineeCount < AnalyticsPrivacySuppressionService.MIN_ANONYMITY_THRESHOLD) {
      safe.put("totalEnrollments", null);
      safe.put("totalTrainees", null);
      safe.put("totalCompleted", null);
      safe.put("totalCertified", null);
      safe.put("totalJoinedPlacement", null);
      safe.put("totalEmployed", null);
      safe.put("nationalCompletionRatePct", null);
      safe.put("nationalCertificationRatePct", null);
      safe.put("nationalPlacementRatePct", null);
      safe.put("nationalEmploymentRatePct", null);
      return safe;
    }

    Long compCount = sanitizeCount((Number) raw.get("totalCompleted"));
    BigDecimal compRate = (compCount == null) ? null : (BigDecimal) raw.get("nationalCompletionRatePct");

    Long certCount = sanitizeCount((Number) raw.get("totalCertified"));
    BigDecimal certRate = (certCount == null) ? null : (BigDecimal) raw.get("nationalCertificationRatePct");

    Long joinCount = sanitizeCount((Number) raw.get("totalJoinedPlacement"));
    BigDecimal joinRate = (joinCount == null) ? null : (BigDecimal) raw.get("nationalPlacementRatePct");

    Long empCount = sanitizeCount((Number) raw.get("totalEmployed"));
    BigDecimal empRate = (empCount == null) ? null : (BigDecimal) raw.get("nationalEmploymentRatePct");

    // Partition anti-reconstruction: Withhold national total if exactly 1 state row in safeRows is suppressed
    // (which would allow an attacker to isolate and reconstruct that single cell: Total - sum(known states))
    if (safeRows != null && !safeRows.isEmpty()) {
      if (countSuppressedCells(safeRows, QprExportRow::completedCount) == 1) {
        compCount = null;
        compRate = null;
      }
      if (countSuppressedCells(safeRows, QprExportRow::certifiedCount) == 1) {
        certCount = null;
        certRate = null;
      }
      if (countSuppressedCells(safeRows, QprExportRow::joinedPlacementCount) == 1) {
        joinCount = null;
        joinRate = null;
      }
      if (countSuppressedCells(safeRows, QprExportRow::employedCount) == 1) {
        empCount = null;
        empRate = null;
      }
      if (countSuppressedCells(safeRows, QprExportRow::traineeCount) == 1) {
        safe.put("totalEnrollments", null);
        safe.put("totalTrainees", null);
        safe.put("totalCompleted", null);
        safe.put("totalCertified", null);
        safe.put("totalJoinedPlacement", null);
        safe.put("totalEmployed", null);
        safe.put("nationalCompletionRatePct", null);
        safe.put("nationalCertificationRatePct", null);
        safe.put("nationalPlacementRatePct", null);
        safe.put("nationalEmploymentRatePct", null);
        return safe;
      }
    }

    safe.put("totalEnrollments", raw.get("totalEnrollments"));
    safe.put("totalTrainees", raw.get("totalTrainees"));
    safe.put("totalCompleted", compCount);
    safe.put("totalCertified", certCount);
    safe.put("totalJoinedPlacement", joinCount);
    safe.put("totalEmployed", empCount);
    safe.put("nationalCompletionRatePct", compRate);
    safe.put("nationalCertificationRatePct", certRate);
    safe.put("nationalPlacementRatePct", joinRate);
    safe.put("nationalEmploymentRatePct", empRate);
    return safe;
  }

  private int countSuppressedCells(List<QprExportRow> rows, java.util.function.Function<QprExportRow, Long> getter) {
    int suppressed = 0;
    for (QprExportRow row : rows) {
      if (getter.apply(row) == null) {
        suppressed++;
      }
    }
    return suppressed;
  }

  private Long sanitizeCount(Number n) {
    if (n == null) {
      return null;
    }
    long val = n.longValue();
    if (val > 0 && val < AnalyticsPrivacySuppressionService.MIN_ANONYMITY_THRESHOLD) {
      return null;
    }
    return val;
  }

  private void logAudit(String status, Long userId, String reportType, String format, int rows, long durationMs) {
    log.info("MIS_EXPORT_AUDIT status={}, principalId={}, reportType={}, format={}, rows={}, durationMs={}",
        status, userId != null ? userId : 0L, reportType, format, rows, durationMs);
  }
}
