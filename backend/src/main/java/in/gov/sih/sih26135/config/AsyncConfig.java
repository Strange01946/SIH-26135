package in.gov.sih.sih26135.config;

import in.gov.sih.sih26135.correlation.CorrelationContextHolder;
import in.gov.sih.sih26135.correlation.CorrelationIdFilter;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Executor;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.atomic.AtomicLong;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.aop.interceptor.AsyncUncaughtExceptionHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.TaskDecorator;
import org.springframework.scheduling.annotation.AsyncConfigurer;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

/**
 * Asynchronous execution configuration providing bounded, resilient thread pools
 * for background event processing, audit trail persistence, and decoupled operational tasks.
 *
 * <p>Enforces bounded queues and an observable rejection policy ({@link ObservableAuditTaskRejectionHandler})
 * that discards excess audit events without blocking or executing on web business request threads.
 */
@Configuration
@EnableAsync
public class AsyncConfig implements AsyncConfigurer {

  private static final Logger log = LoggerFactory.getLogger(AsyncConfig.class);

  private final ObservableAuditTaskRejectionHandler rejectionHandler = new ObservableAuditTaskRejectionHandler();

  @Bean(name = "auditTaskExecutor")
  public Executor auditTaskExecutor() {
    ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
    executor.setCorePoolSize(4);
    executor.setMaxPoolSize(16);
    executor.setQueueCapacity(500);
    executor.setThreadNamePrefix("audit-exec-");
    executor.setTaskDecorator(correlationTaskDecorator());
    executor.setRejectedExecutionHandler(rejectionHandler);
    executor.setWaitForTasksToCompleteOnShutdown(true);
    executor.setAwaitTerminationSeconds(30);
    executor.initialize();
    return executor;
  }

  @Override
  public Executor getAsyncExecutor() {
    return auditTaskExecutor();
  }

  @Override
  public AsyncUncaughtExceptionHandler getAsyncUncaughtExceptionHandler() {
    return (throwable, method, params) ->
        log.error("Uncaught asynchronous exception in method {}: {}", method.getName(), throwable.getMessage());
  }

  public ObservableAuditTaskRejectionHandler getRejectionHandler() {
    return rejectionHandler;
  }

  /**
   * Bounded, observable rejection handler that safely discards audit persistence tasks under
   * executor saturation without ever executing on the primary business caller thread.
   */
  public static class ObservableAuditTaskRejectionHandler implements RejectedExecutionHandler {

    private static final Logger rejectLog = LoggerFactory.getLogger(ObservableAuditTaskRejectionHandler.class);
    private final AtomicLong rejectedTaskCount = new AtomicLong(0);

    @Override
    public void rejectedExecution(Runnable r, ThreadPoolExecutor executor) {
      long count = rejectedTaskCount.incrementAndGet();
      rejectLog.warn("Audit persistence task rejected due to executor saturation. Total dropped tasks: {}. Queue: {}/{}, Active threads: {}/{}",
          count,
          executor != null && executor.getQueue() != null ? executor.getQueue().size() : -1,
          executor != null && executor.getQueue() != null ? executor.getQueue().size() + executor.getQueue().remainingCapacity() : -1,
          executor != null ? executor.getActiveCount() : -1,
          executor != null ? executor.getMaximumPoolSize() : -1);
      // NOTE: Intentionally do NOT call r.run() on caller thread to maintain complete isolation of primary transactions
    }

    public long getRejectedTaskCount() {
      return rejectedTaskCount.get();
    }

    public void reset() {
      rejectedTaskCount.set(0);
    }
  }

  /**
   * Propagates validated correlation context and SLF4J MDC across asynchronous task boundaries,
   * assigning a distinct async request ID while preserving the parent correlation ID (or generating
   * server-owned IDs for background tasks lacking an originating HTTP context), and restores/clears
   * worker thread context in {@code finally}.
   */
  @Bean
  public TaskDecorator correlationTaskDecorator() {
    return runnable -> {
      Map<String, String> capturedMdc = MDC.getCopyOfContextMap();
      String rawParentCorr = CorrelationContextHolder.getCorrelationId();
      if (rawParentCorr == null && capturedMdc != null) {
        rawParentCorr = capturedMdc.get(CorrelationIdFilter.MDC_CORRELATION_ID_KEY);
      }
      final String parentCorrelationId = CorrelationIdFilter.isValidCorrelationId(rawParentCorr)
          ? rawParentCorr
          : null;

      return () -> {
        Map<String, String> previousMdc = MDC.getCopyOfContextMap();
        String previousContextCorr = CorrelationContextHolder.getCorrelationId();
        String previousContextReq = CorrelationContextHolder.getExplicitRequestId();

        String effectiveCorrelationId = parentCorrelationId != null
            ? parentCorrelationId
            : UUID.randomUUID().toString();
        String distinctAsyncRequestId = UUID.randomUUID().toString();

        try {
          if (capturedMdc != null) {
            MDC.setContextMap(capturedMdc);
          } else {
            MDC.clear();
          }
          MDC.put(CorrelationIdFilter.MDC_CORRELATION_ID_KEY, effectiveCorrelationId);
          MDC.put(CorrelationIdFilter.MDC_REQUEST_ID_KEY, distinctAsyncRequestId);
          CorrelationContextHolder.setCorrelationId(effectiveCorrelationId);
          CorrelationContextHolder.setRequestId(distinctAsyncRequestId);
          runnable.run();
        } finally {
          if (previousContextCorr != null) {
            CorrelationContextHolder.setCorrelationId(previousContextCorr);
          } else {
            CorrelationContextHolder.setCorrelationId(null);
          }
          if (previousContextReq != null) {
            CorrelationContextHolder.setRequestId(previousContextReq);
          } else {
            CorrelationContextHolder.setRequestId(null);
          }
          if (previousMdc != null) {
            MDC.setContextMap(previousMdc);
          } else {
            MDC.clear();
          }
        }
      };
    };
  }
}
