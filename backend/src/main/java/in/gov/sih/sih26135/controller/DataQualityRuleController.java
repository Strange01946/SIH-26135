package in.gov.sih.sih26135.controller;

import in.gov.sih.sih26135.dto.request.CreateDataQualityRuleRequest;
import in.gov.sih.sih26135.dto.request.UpdateDataQualityRuleRequest;
import in.gov.sih.sih26135.dto.response.DataQualityRuleResponse;
import in.gov.sih.sih26135.exception.BadRequestException;
import in.gov.sih.sih26135.response.ApiResponse;
import in.gov.sih.sih26135.service.DataQualityRuleService;
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
 * REST controller for managing Data Quality Rule domain resources.
 *
 * <p>Base Route: /api/v1/data-quality-rules
 * Consumes: CreateDataQualityRuleRequest, UpdateDataQualityRuleRequest
 * Produces: DataQualityRuleResponse enveloped in ApiResponse
 */
@RestController
@RequestMapping("/api/v1/data-quality-rules")
@PreAuthorize("hasAnyAuthority('audit.read', 'system.manage')")
public class DataQualityRuleController {

  private final DataQualityRuleService dataQualityRuleService;

  public DataQualityRuleController(DataQualityRuleService dataQualityRuleService) {
    this.dataQualityRuleService = dataQualityRuleService;
  }

  /**
   * Retrieves a data quality rule by primary key identifier.
   *
   * @param id primary key identifier of the rule
   * @return 200 OK with DataQualityRuleResponse enveloped in ApiResponse
   */
  @GetMapping("/{id}")
  public ResponseEntity<ApiResponse<DataQualityRuleResponse>> getById(@PathVariable Long id) {
    DataQualityRuleResponse response = dataQualityRuleService.getRuleById(id);
    return ResponseEntity.ok(ApiResponse.ok(response));
  }

  /**
   * Retrieves a single data quality rule by unique rule code.
   *
   * @param ruleCode unique rule code
   * @param httpRequest HTTP servlet request to verify query parameters
   * @return 200 OK with DataQualityRuleResponse enveloped in ApiResponse
   */
  @GetMapping(params = {
      "ruleCode", "!targetEntityType", "!categoryId", "!severityId", "!lifecycleStatusId"
  })
  public ResponseEntity<ApiResponse<DataQualityRuleResponse>> getByRuleCode(
      @RequestParam("ruleCode") String ruleCode,
      HttpServletRequest httpRequest) {
    validateOnlyQueryParameter(httpRequest, "ruleCode");
    DataQualityRuleResponse response = dataQualityRuleService.getRuleByCode(ruleCode);
    return ResponseEntity.ok(ApiResponse.ok(response));
  }

  /**
   * Retrieves all data quality rules targeting a specific entity type.
   *
   * @param targetEntityType target entity type name
   * @param httpRequest HTTP servlet request to verify query parameters
   * @return 200 OK with list of DataQualityRuleResponse enveloped in ApiResponse
   */
  @GetMapping(params = {
      "targetEntityType", "!ruleCode", "!categoryId", "!severityId", "!lifecycleStatusId"
  })
  public ResponseEntity<ApiResponse<List<DataQualityRuleResponse>>> getByTargetEntityType(
      @RequestParam("targetEntityType") String targetEntityType,
      HttpServletRequest httpRequest) {
    validateOnlyQueryParameter(httpRequest, "targetEntityType");
    List<DataQualityRuleResponse> rules = dataQualityRuleService.getRulesByTargetEntityType(targetEntityType);
    return ResponseEntity.ok(ApiResponse.ok(rules));
  }

  /**
   * Retrieves all data quality rules for a specific category.
   *
   * @param categoryId data quality category reference identifier
   * @param httpRequest HTTP servlet request to verify query parameters
   * @return 200 OK with list of DataQualityRuleResponse enveloped in ApiResponse
   */
  @GetMapping(params = {
      "categoryId", "!ruleCode", "!targetEntityType", "!severityId", "!lifecycleStatusId"
  })
  public ResponseEntity<ApiResponse<List<DataQualityRuleResponse>>> getByCategoryId(
      @RequestParam("categoryId") Long categoryId,
      HttpServletRequest httpRequest) {
    validateOnlyQueryParameter(httpRequest, "categoryId");
    List<DataQualityRuleResponse> rules = dataQualityRuleService.getRulesByCategoryId(categoryId);
    return ResponseEntity.ok(ApiResponse.ok(rules));
  }

  /**
   * Retrieves all data quality rules for a specific severity level.
   *
   * @param severityId data quality severity reference identifier
   * @param httpRequest HTTP servlet request to verify query parameters
   * @return 200 OK with list of DataQualityRuleResponse enveloped in ApiResponse
   */
  @GetMapping(params = {
      "severityId", "!ruleCode", "!targetEntityType", "!categoryId", "!lifecycleStatusId"
  })
  public ResponseEntity<ApiResponse<List<DataQualityRuleResponse>>> getBySeverityId(
      @RequestParam("severityId") Long severityId,
      HttpServletRequest httpRequest) {
    validateOnlyQueryParameter(httpRequest, "severityId");
    List<DataQualityRuleResponse> rules = dataQualityRuleService.getRulesBySeverityId(severityId);
    return ResponseEntity.ok(ApiResponse.ok(rules));
  }

  /**
   * Retrieves all data quality rules with a specific lifecycle status.
   *
   * @param lifecycleStatusId lifecycle status identifier
   * @param httpRequest HTTP servlet request to verify query parameters
   * @return 200 OK with list of DataQualityRuleResponse enveloped in ApiResponse
   */
  @GetMapping(params = {
      "lifecycleStatusId", "!ruleCode", "!targetEntityType", "!categoryId", "!severityId"
  })
  public ResponseEntity<ApiResponse<List<DataQualityRuleResponse>>> getByLifecycleStatusId(
      @RequestParam("lifecycleStatusId") Long lifecycleStatusId,
      HttpServletRequest httpRequest) {
    validateOnlyQueryParameter(httpRequest, "lifecycleStatusId");
    List<DataQualityRuleResponse> rules = dataQualityRuleService.getRulesByLifecycleStatusId(lifecycleStatusId);
    return ResponseEntity.ok(ApiResponse.ok(rules));
  }

  /**
   * Creates a new data quality rule.
   *
   * @param request creation payload
   * @param httpRequest HTTP servlet request for URI extraction
   * @return 201 Created with created DataQualityRuleResponse enveloped in ApiResponse
   */
  @PostMapping
  public ResponseEntity<ApiResponse<DataQualityRuleResponse>> createRule(
      @RequestBody CreateDataQualityRuleRequest request,
      HttpServletRequest httpRequest) {
    DataQualityRuleResponse response = dataQualityRuleService.createRule(request);
    return ResponseEntity.status(HttpStatus.CREATED)
        .body(ApiResponse.success(
            "Data quality rule created successfully",
            response,
            httpRequest.getRequestURI()));
  }

  /**
   * Updates an existing data quality rule.
   *
   * @param id primary key identifier of the rule to update
   * @param request update payload
   * @return 200 OK with updated DataQualityRuleResponse enveloped in ApiResponse
   */
  @PutMapping("/{id}")
  public ResponseEntity<ApiResponse<DataQualityRuleResponse>> updateRule(
      @PathVariable Long id,
      @RequestBody UpdateDataQualityRuleRequest request) {
    DataQualityRuleResponse response = dataQualityRuleService.updateRule(id, request);
    return ResponseEntity.ok(ApiResponse.success(
        "Data quality rule updated successfully",
        response));
  }

  /**
   * Deletes a data quality rule.
   *
   * @param id primary key identifier of the rule to delete
   * @return 200 OK with success confirmation message
   */
  @DeleteMapping("/{id}")
  public ResponseEntity<ApiResponse<Void>> deleteRule(@PathVariable Long id) {
    dataQualityRuleService.deleteRule(id);
    return ResponseEntity.ok(ApiResponse.success("Data quality rule deleted successfully"));
  }

  private void validateOnlyQueryParameter(HttpServletRequest request, String allowedParam) {
    for (String paramName : request.getParameterMap().keySet()) {
      if (!paramName.equals(allowedParam)) {
        throw new BadRequestException(
            "Unsupported query parameter: " + paramName,
            "UNSUPPORTED_PARAMETER"
        );
      }
    }
  }
}
