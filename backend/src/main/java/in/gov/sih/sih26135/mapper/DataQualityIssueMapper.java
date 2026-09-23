package in.gov.sih.sih26135.mapper;

import in.gov.sih.sih26135.dto.response.DataQualityIssueResponse;
import in.gov.sih.sih26135.entity.DataQualityIssue;
import org.springframework.stereotype.Component;

@Component
public class DataQualityIssueMapper {

  public DataQualityIssueResponse toResponse(DataQualityIssue entity) {
    if (entity == null) {
      return null;
    }

    Long ruleId = null;
    String ruleCode = null;
    String ruleName = null;
    if (entity.getDataQualityRule() != null) {
      ruleId = entity.getDataQualityRule().getId();
      ruleCode = entity.getDataQualityRule().getRuleCode();
      ruleName = entity.getDataQualityRule().getRuleName();
    }

    Long categoryId = null;
    String categoryCode = null;
    String categoryName = null;
    if (entity.getDataQualityCategory() != null) {
      categoryId = entity.getDataQualityCategory().getId();
      categoryCode = entity.getDataQualityCategory().getCategoryCode();
      categoryName = entity.getDataQualityCategory().getCategoryName();
    }

    Long severityId = null;
    String severityCode = null;
    String severityName = null;
    if (entity.getDataQualitySeverity() != null) {
      severityId = entity.getDataQualitySeverity().getId();
      severityCode = entity.getDataQualitySeverity().getSeverityCode();
      severityName = entity.getDataQualitySeverity().getSeverityName();
    }

    Long statusId = null;
    String statusCode = null;
    String statusName = null;
    Boolean isOpenFlag = null;
    Boolean isTerminalFlag = null;
    if (entity.getDataQualityIssueStatus() != null) {
      statusId = entity.getDataQualityIssueStatus().getId();
      statusCode = entity.getDataQualityIssueStatus().getStatusCode();
      statusName = entity.getDataQualityIssueStatus().getStatusName();
      isOpenFlag = entity.getDataQualityIssueStatus().getIsOpenFlag();
      isTerminalFlag = entity.getDataQualityIssueStatus().getIsTerminalFlag();
    }

    Long sourceId = null;
    String sourceCode = null;
    String sourceName = null;
    if (entity.getDataQualityDetectionSource() != null) {
      sourceId = entity.getDataQualityDetectionSource().getId();
      sourceCode = entity.getDataQualityDetectionSource().getSourceCode();
      sourceName = entity.getDataQualityDetectionSource().getSourceName();
    }

    return new DataQualityIssueResponse(
        entity.getId(),
        ruleId,
        ruleCode,
        ruleName,
        categoryId,
        categoryCode,
        categoryName,
        severityId,
        severityCode,
        severityName,
        statusId,
        statusCode,
        statusName,
        isOpenFlag,
        isTerminalFlag,
        sourceId,
        sourceCode,
        sourceName,
        entity.getEntityType(),
        entity.getEntityId(),
        entity.getIssueSummary(),
        entity.getDetectedAt(),
        entity.getAssignedUserId(),
        entity.getResolvedAt(),
        entity.getResolvedByUserId(),
        entity.getResolutionNotes(),
        entity.getCreatedAt(),
        entity.getUpdatedAt()
    );
  }
}
