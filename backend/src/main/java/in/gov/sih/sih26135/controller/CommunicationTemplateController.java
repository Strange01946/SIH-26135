package in.gov.sih.sih26135.controller;

import in.gov.sih.sih26135.communication.template.CommunicationTemplateRegistry;
import in.gov.sih.sih26135.communication.template.CommunicationTemplateService;
import in.gov.sih.sih26135.entity.Trainee;
import in.gov.sih.sih26135.exception.ResourceNotFoundException;
import in.gov.sih.sih26135.repository.TraineeRepository;
import in.gov.sih.sih26135.response.ApiResponse;
import in.gov.sih.sih26135.security.WorkflowSecurityScopeValidator;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller for discovering, inspecting, and preview-rendering
 * standardized outbound notification templates.
 */
@Validated
@RestController
@RequestMapping("/api/v1/communication/templates")
@PreAuthorize("hasAnyAuthority('communication.manage', 'survey.manage', 'system.manage')")
public class CommunicationTemplateController {

  private final CommunicationTemplateService templateService;
  private final TraineeRepository traineeRepository;
  private final WorkflowSecurityScopeValidator scope;

  @Autowired
  public CommunicationTemplateController(
      CommunicationTemplateService templateService,
      TraineeRepository traineeRepository,
      WorkflowSecurityScopeValidator scope) {
    this.templateService = templateService;
    this.traineeRepository = traineeRepository;
    this.scope = scope;
  }

  /**
   * Retrieves all registered notification templates and their parameter schemas.
   */
  @GetMapping
  public ResponseEntity<ApiResponse<List<CommunicationTemplateRegistry.TemplateDefinition>>> getAllTemplates() {
    List<CommunicationTemplateRegistry.TemplateDefinition> templates = templateService.getAllTemplates();
    return ResponseEntity.ok(ApiResponse.ok(templates));
  }

  /**
   * Retrieves a specific template definition by code.
   */
  @GetMapping("/{templateCode}")
  public ResponseEntity<ApiResponse<CommunicationTemplateRegistry.TemplateDefinition>> getTemplateByCode(
      @PathVariable @NotBlank String templateCode) {
    CommunicationTemplateRegistry.TemplateDefinition tpl = templateService.getTemplateDefinition(templateCode);
    if (tpl == null) {
      throw new ResourceNotFoundException("CommunicationTemplate", "templateCode");
    }
    return ResponseEntity.ok(ApiResponse.ok(tpl));
  }

  /**
   * Previews a rendered template for a given trainee and parameter map.
   */
  @PostMapping("/{templateCode}/render")
  public ResponseEntity<ApiResponse<CommunicationTemplateRegistry.RenderedTemplate>> renderTemplate(
      @PathVariable @NotBlank String templateCode,
      @RequestParam(name = "traineeId", required = false) @Positive Long traineeId,
      @RequestBody(required = false) Map<String, String> parameters,
      Authentication authentication) {

    Trainee trainee = null;
    if (traineeId != null) {
      if (authentication.getAuthorities().stream().noneMatch(authority ->
          List.of("communication.manage", "survey.manage", "system.manage").contains(authority.getAuthority()))) {
        throw new AccessDeniedException("Only communication staff can preview with a trainee record");
      }
      scope.validateTraineeScope(traineeId);
      trainee = traineeRepository.findById(traineeId)
          .orElseThrow(() -> new ResourceNotFoundException("Trainee", "id"));
    }

    CommunicationTemplateRegistry.RenderedTemplate rendered = templateService.renderTemplate(templateCode, parameters, trainee);
    return ResponseEntity.ok(ApiResponse.ok(rendered));
  }
}
