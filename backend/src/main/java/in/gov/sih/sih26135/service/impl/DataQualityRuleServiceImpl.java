package in.gov.sih.sih26135.service.impl;

import in.gov.sih.sih26135.dto.request.CreateDataQualityRuleRequest;
import in.gov.sih.sih26135.dto.request.UpdateDataQualityRuleRequest;
import in.gov.sih.sih26135.dto.response.DataQualityRuleResponse;
import in.gov.sih.sih26135.entity.DataQualityRule;
import in.gov.sih.sih26135.entity.RefDataQualityCategory;
import in.gov.sih.sih26135.entity.RefDataQualitySeverity;
import in.gov.sih.sih26135.exception.BadRequestException;
import in.gov.sih.sih26135.exception.ConflictException;
import in.gov.sih.sih26135.exception.ResourceNotFoundException;
import in.gov.sih.sih26135.mapper.DataQualityRuleMapper;
import in.gov.sih.sih26135.repository.DataQualityIssueRepository;
import in.gov.sih.sih26135.repository.DataQualityRuleRepository;
import in.gov.sih.sih26135.repository.RefDataQualityCategoryRepository;
import in.gov.sih.sih26135.repository.RefDataQualitySeverityRepository;
import in.gov.sih.sih26135.service.DataQualityRuleService;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class DataQualityRuleServiceImpl implements DataQualityRuleService {

  private final DataQualityRuleRepository dataQualityRuleRepository;
  private final RefDataQualityCategoryRepository refDataQualityCategoryRepository;
  private final RefDataQualitySeverityRepository refDataQualitySeverityRepository;
  private final DataQualityIssueRepository dataQualityIssueRepository;
  private final DataQualityRuleMapper mapper;

  public DataQualityRuleServiceImpl(
      DataQualityRuleRepository dataQualityRuleRepository,
      RefDataQualityCategoryRepository refDataQualityCategoryRepository,
      RefDataQualitySeverityRepository refDataQualitySeverityRepository,
      DataQualityIssueRepository dataQualityIssueRepository,
      DataQualityRuleMapper mapper) {
    this.dataQualityRuleRepository = dataQualityRuleRepository;
    this.refDataQualityCategoryRepository = refDataQualityCategoryRepository;
    this.refDataQualitySeverityRepository = refDataQualitySeverityRepository;
    this.dataQualityIssueRepository = dataQualityIssueRepository;
    this.mapper = mapper;
  }

  @Override
  @Transactional
  public DataQualityRuleResponse createRule(CreateDataQualityRuleRequest request) {
    if (request == null) {
      throw new BadRequestException("Request body cannot be null", "REQUEST_BODY_NULL");
    }
    if (request.getRuleCode() == null || request.getRuleCode().isBlank()) {
      throw new BadRequestException("Rule code is required", "RULE_CODE_REQUIRED");
    }
    String ruleCode = request.getRuleCode().trim();
    if (dataQualityRuleRepository.existsByRuleCode(ruleCode)) {
      throw new ConflictException("Rule code already exists: " + ruleCode, "RULE_CODE_EXISTS");
    }

    if (request.getRuleName() == null || request.getRuleName().isBlank()) {
      throw new BadRequestException("Rule name is required", "RULE_NAME_REQUIRED");
    }
    if (request.getTargetEntityType() == null || request.getTargetEntityType().isBlank()) {
      throw new BadRequestException("Target entity type is required", "TARGET_ENTITY_TYPE_REQUIRED");
    }

    if (request.getDataQualityCategoryId() == null) {
      throw new BadRequestException("Data quality category ID is required", "CATEGORY_ID_REQUIRED");
    }
    RefDataQualityCategory category = refDataQualityCategoryRepository.findById(request.getDataQualityCategoryId())
        .orElseThrow(() -> new ResourceNotFoundException("RefDataQualityCategory", "dataQualityCategoryId"));

    if (request.getDataQualitySeverityId() == null) {
      throw new BadRequestException("Data quality severity ID is required", "SEVERITY_ID_REQUIRED");
    }
    RefDataQualitySeverity severity = refDataQualitySeverityRepository.findById(request.getDataQualitySeverityId())
        .orElseThrow(() -> new ResourceNotFoundException("RefDataQualitySeverity", "dataQualitySeverityId"));

    if (request.getLifecycleStatusId() == null) {
      throw new BadRequestException("Lifecycle status ID is required", "LIFECYCLE_STATUS_REQUIRED");
    }

    DataQualityRule rule = new DataQualityRule(
        ruleCode,
        request.getRuleName().trim(),
        request.getTargetEntityType().trim(),
        category,
        severity,
        request.getLifecycleStatusId()
    );
    rule.setDescription(request.getDescription());

    DataQualityRule saved = dataQualityRuleRepository.save(rule);
    return mapper.toResponse(saved);
  }

  @Override
  @Transactional
  public DataQualityRuleResponse updateRule(Long id, UpdateDataQualityRuleRequest request) {
    if (id == null) {
      throw new BadRequestException("Rule ID is required", "ID_REQUIRED");
    }
    if (request == null) {
      throw new BadRequestException("Request body cannot be null", "REQUEST_BODY_NULL");
    }

    DataQualityRule rule = dataQualityRuleRepository.findById(id)
        .orElseThrow(() -> new ResourceNotFoundException("DataQualityRule", "id"));

    if (request.getRuleName() != null && !request.getRuleName().isBlank()) {
      rule.setRuleName(request.getRuleName().trim());
    }
    if (request.getDescription() != null) {
      rule.setDescription(request.getDescription());
    }
    if (request.getTargetEntityType() != null && !request.getTargetEntityType().isBlank()) {
      rule.setTargetEntityType(request.getTargetEntityType().trim());
    }

    if (request.getDataQualityCategoryId() != null) {
      RefDataQualityCategory category = refDataQualityCategoryRepository.findById(request.getDataQualityCategoryId())
          .orElseThrow(() -> new ResourceNotFoundException("RefDataQualityCategory", "dataQualityCategoryId"));
      rule.setDataQualityCategory(category);
    }

    if (request.getDataQualitySeverityId() != null) {
      RefDataQualitySeverity severity = refDataQualitySeverityRepository.findById(request.getDataQualitySeverityId())
          .orElseThrow(() -> new ResourceNotFoundException("RefDataQualitySeverity", "dataQualitySeverityId"));
      rule.setDataQualitySeverity(severity);
    }

    if (request.getLifecycleStatusId() != null) {
      rule.setLifecycleStatusId(request.getLifecycleStatusId());
    }

    DataQualityRule saved = dataQualityRuleRepository.save(rule);
    return mapper.toResponse(saved);
  }

  @Override
  public DataQualityRuleResponse getRuleById(Long id) {
    if (id == null) {
      throw new BadRequestException("Rule ID is required");
    }
    DataQualityRule entity = dataQualityRuleRepository.findById(id)
        .orElseThrow(() -> new ResourceNotFoundException("DataQualityRule", "id"));
    return mapper.toResponse(entity);
  }

  @Override
  public DataQualityRuleResponse getRuleByCode(String ruleCode) {
    if (ruleCode == null || ruleCode.isBlank()) {
      throw new BadRequestException("Rule code is required");
    }
    DataQualityRule entity = dataQualityRuleRepository.findByRuleCode(ruleCode.trim())
        .orElseThrow(() -> new ResourceNotFoundException("DataQualityRule", "ruleCode"));
    return mapper.toResponse(entity);
  }

  @Override
  public List<DataQualityRuleResponse> getRulesByTargetEntityType(String targetEntityType) {
    if (targetEntityType == null || targetEntityType.isBlank()) {
      throw new BadRequestException("Target entity type is required");
    }
    return dataQualityRuleRepository.findByTargetEntityType(targetEntityType.trim()).stream()
        .map(mapper::toResponse)
        .toList();
  }

  @Override
  public List<DataQualityRuleResponse> getRulesByCategoryId(Long categoryId) {
    if (categoryId == null) {
      throw new BadRequestException("Category ID is required");
    }
    return dataQualityRuleRepository.findByDataQualityCategoryId(categoryId).stream()
        .map(mapper::toResponse)
        .toList();
  }

  @Override
  public List<DataQualityRuleResponse> getRulesBySeverityId(Long severityId) {
    if (severityId == null) {
      throw new BadRequestException("Severity ID is required");
    }
    return dataQualityRuleRepository.findByDataQualitySeverityId(severityId).stream()
        .map(mapper::toResponse)
        .toList();
  }

  @Override
  public List<DataQualityRuleResponse> getRulesByLifecycleStatusId(Long lifecycleStatusId) {
    if (lifecycleStatusId == null) {
      throw new BadRequestException("Lifecycle status ID is required");
    }
    return dataQualityRuleRepository.findByLifecycleStatusId(lifecycleStatusId).stream()
        .map(mapper::toResponse)
        .toList();
  }

  @Override
  @Transactional
  public void deleteRule(Long id) {
    if (id == null) {
      throw new BadRequestException("Rule ID is required", "ID_REQUIRED");
    }
    DataQualityRule rule = dataQualityRuleRepository.findById(id)
        .orElseThrow(() -> new ResourceNotFoundException("DataQualityRule", "id"));

    if (!dataQualityIssueRepository.findByDataQualityRuleId(id).isEmpty()) {
      throw new ConflictException("Cannot delete data quality rule with existing issues", "RULE_HAS_ISSUES");
    }

    dataQualityRuleRepository.delete(rule);
  }
}
