package in.gov.sih.sih26135.config.cache;

import java.util.Collections;
import java.util.Set;
import org.springframework.context.ApplicationEvent;

/**
 * Domain event published when an authoritative workflow state transition commits,
 * instructing the analytics caching layer to invalidate affected dashboard read models.
 */
public class AnalyticsWorkflowInvalidationEvent extends ApplicationEvent {

  private final Set<String> cacheNames;
  private final String sourceWorkflow;

  public AnalyticsWorkflowInvalidationEvent(Object source, String sourceWorkflow, Set<String> cacheNames) {
    super(source);
    this.sourceWorkflow = sourceWorkflow != null ? sourceWorkflow : "UNKNOWN_WORKFLOW";
    this.cacheNames = cacheNames != null ? Collections.unmodifiableSet(cacheNames) : Collections.emptySet();
  }

  public Set<String> getCacheNames() {
    return cacheNames;
  }

  public String getSourceWorkflow() {
    return sourceWorkflow;
  }
}
