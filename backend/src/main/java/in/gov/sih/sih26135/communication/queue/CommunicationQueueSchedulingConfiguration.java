package in.gov.sih.sih26135.communication.queue;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

@Configuration(proxyBeanMethods = false)
@EnableScheduling
@ConditionalOnProperty(prefix = "communication.queue", name = "worker-enabled", havingValue = "true", matchIfMissing = true)
public class CommunicationQueueSchedulingConfiguration {
}
