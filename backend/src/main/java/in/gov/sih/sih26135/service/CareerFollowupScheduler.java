package in.gov.sih.sih26135.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@EnableScheduling
@ConditionalOnProperty(prefix = "career.followup", name = "automatic-enabled", havingValue = "true", matchIfMissing = true)
class CareerFollowupScheduler {
  private static final Logger log = LoggerFactory.getLogger(CareerFollowupScheduler.class);
  private static final int PAGE_SIZE = 200;
  private final CareerFollowupService followups;

  CareerFollowupScheduler(CareerFollowupService followups) {
    this.followups = followups;
  }

  @Scheduled(initialDelayString = "${career.followup.initial-delay-ms:15000}",
      fixedDelayString = "${career.followup.scan-interval-ms:900000}")
  void assignDueSurveys() {
    var today = CareerFollowupService.today();
    long afterId = 0;
    int assigned = 0;
    while (true) {
      var candidates = followups.dueTrainees(today, afterId, PAGE_SIZE);
      for (long traineeId : candidates) {
        try {
          if (followups.assignIfDue(traineeId, today)) {
            assigned++;
          }
        } catch (RuntimeException failure) {
          log.warn("Could not schedule a due employment follow-up for trainee {}", traineeId, failure);
        }
        afterId = traineeId;
      }
      if (candidates.size() < PAGE_SIZE) {
        break;
      }
    }
    log.debug("Scheduled {} automatic employment follow-ups", assigned);
  }
}
