package in.gov.sih.sih26135.service;

import in.gov.sih.sih26135.dto.request.AnalyticsFilterCriteria;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

/**
 * Service interface for Government MIS Report Generation and Streaming Data Exports.
 *
 * <p>Supports:
 * <ul>
 *   <li>District Skill Development Plan (DSDP) in CSV and JSON formats.</li>
 *   <li>Ministry Quarterly Progress Report (QPR) in CSV and JSON formats.</li>
 *   <li>Training Provider Performance Scorecard in CSV and JSON formats.</li>
 * </ul>
 */
public interface AnalyticsExportService {

  /**
   * Generates a streaming CSV export of the District Skill Development Plan (DSDP).
   */
  StreamingResponseBody exportDsdpCsv(Long districtId, AnalyticsFilterCriteria criteria, Long userId);

  /**
   * Generates a streaming JSON export of the District Skill Development Plan (DSDP).
   */
  StreamingResponseBody exportDsdpJson(Long districtId, AnalyticsFilterCriteria criteria, Long userId);

  /**
   * Generates a streaming CSV export of the Ministry Quarterly Progress Report (QPR).
   */
  StreamingResponseBody exportQprCsv(String fiscalYear, String quarter, AnalyticsFilterCriteria criteria, Long userId);

  /**
   * Generates a streaming JSON export of the Ministry Quarterly Progress Report (QPR).
   */
  StreamingResponseBody exportQprJson(String fiscalYear, String quarter, AnalyticsFilterCriteria criteria, Long userId);

  /**
   * Generates a streaming CSV export of the Training Provider Performance Scorecard.
   */
  StreamingResponseBody exportProviderScorecardCsv(Long providerId, Long stateId, AnalyticsFilterCriteria criteria, Long userId);

  /**
   * Generates a streaming JSON export of the Training Provider Performance Scorecard.
   */
  StreamingResponseBody exportProviderScorecardJson(Long providerId, Long stateId, AnalyticsFilterCriteria criteria, Long userId);
}
