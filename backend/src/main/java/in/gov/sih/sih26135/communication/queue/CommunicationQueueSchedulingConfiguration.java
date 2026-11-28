package in.gov.sih.sih26135.communication.queue;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

@Configuration(proxyBeanMethods = false)
@EnableScheduling
@ConditionalOnProperty(prefix = "communication.queue", name = "worker-enabled", havingValue = "true", matchIfMissing = true)
public class CommunicationQueueSchedulingConfiguration {
  private final DurableDispatchQueueServiceImpl queue;

  public CommunicationQueueSchedulingConfiguration(DurableDispatchQueueServiceImpl queue) {
    this.queue = queue;
  }

  @Scheduled(fixedDelay = 5000)
  void processPendingQueue() {
    queue.processPendingQueueScheduler();
  }
}
