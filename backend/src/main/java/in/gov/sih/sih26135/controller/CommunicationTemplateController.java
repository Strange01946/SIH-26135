package in.gov.sih.sih26135.controller;

import in.gov.sih.sih26135.communication.template.CommunicationTemplateRegistry;
import in.gov.sih.sih26135.communication.template.CommunicationTemplateService;
import in.gov.sih.sih26135.entity.Trainee;
import in.gov.sih.sih26135.exception.ResourceNotFoundException;
import in.gov.sih.sih26135.repository.TraineeRepository;
import in.gov.sih.sih26135.response.ApiResponse;
import jakarta.validation.constraints.NotBlank;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
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
@RestController
@RequestMapping("/api/v1/communication/templates")
@PreAuthorize("hasAnyAuthority('communication.manage', 'survey.manage', 'system.manage')")
public class CommunicationTemplateController {

  private final CommunicationTemplateService templateService;
  private final TraineeRepository traineeRepository;

  @Autowired
  public CommunicationTemplateController(
      CommunicationTemplateService templateService,
      TraineeRepository traineeRepository) {
    this.templateService = templateService;
    this.traineeRepository = traineeRepository;
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
      @RequestParam(name = "traineeId", required = false) Long traineeId,
      @RequestBody(required = false) Map<String, String> parameters) {

    Trainee trainee = null;
    if (traineeId != null) {
      trainee = traineeRepository.findById(traineeId)
          .orElseThrow(() -> new ResourceNotFoundException("Trainee", "id"));
    }

    CommunicationTemplateRegistry.RenderedTemplate rendered = templateService.renderTemplate(templateCode, parameters, trainee);
    return ResponseEntity.ok(ApiResponse.ok(rendered));
  }
}
