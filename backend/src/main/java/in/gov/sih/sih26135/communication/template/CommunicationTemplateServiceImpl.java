package in.gov.sih.sih26135.communication.template;

import in.gov.sih.sih26135.entity.Trainee;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * Production implementation of {@link CommunicationTemplateService} backed by {@link CommunicationTemplateRegistry}.
 */
@Service
public class CommunicationTemplateServiceImpl implements CommunicationTemplateService {

  private final CommunicationTemplateRegistry templateRegistry;

  @Autowired
  public CommunicationTemplateServiceImpl(CommunicationTemplateRegistry templateRegistry) {
    this.templateRegistry = Objects.requireNonNull(templateRegistry, "CommunicationTemplateRegistry is required");
  }

  @Override
  public CommunicationTemplateRegistry.RenderedTemplate renderTemplate(
      String templateCode,
      Map<String, String> parameters,
      Trainee trainee) {
    return templateRegistry.render(templateCode, parameters, trainee);
  }

  @Override
  public Set<String> getRegisteredTemplateCodes() {
    return templateRegistry.getRegisteredTemplateCodes();
  }

  @Override
  public CommunicationTemplateRegistry.TemplateDefinition getTemplateDefinition(String templateCode) {
    return templateRegistry.getTemplateDefinition(templateCode);
  }

  @Override
  public List<CommunicationTemplateRegistry.TemplateDefinition> getAllTemplates() {
    return templateRegistry.getAllTemplateDefinitions();
  }
}
