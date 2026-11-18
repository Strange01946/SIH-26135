package in.gov.sih.sih26135.communication.template;

import in.gov.sih.sih26135.entity.Trainee;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Service contract for notification template management, validation, and rendering.
 */
public interface CommunicationTemplateService {

  /**
   * Renders a communication template for a trainee recipient after validating allowlisted parameters,
   * sensitive pattern absence, and trusted application URLs.
   *
   * @param templateCode standardized or backward-compatible template code
   * @param parameters template interpolation parameters
   * @param trainee trainee entity
   * @return rendered template containing subject, plain text body, and sanitized HTML body
   */
  CommunicationTemplateRegistry.RenderedTemplate renderTemplate(
      String templateCode,
      Map<String, String> parameters,
      Trainee trainee
  );

  /**
   * Returns all registered template codes.
   */
  Set<String> getRegisteredTemplateCodes();

  /**
   * Retrieves definition schema for a specific template.
   */
  CommunicationTemplateRegistry.TemplateDefinition getTemplateDefinition(String templateCode);

  /**
   * Retrieves all registered template definitions.
   */
  List<CommunicationTemplateRegistry.TemplateDefinition> getAllTemplates();
}
