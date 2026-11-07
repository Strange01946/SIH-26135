package in.gov.sih.sih26135.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.aop.interceptor.AsyncUncaughtExceptionHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.AsyncConfigurer;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.atomic.AtomicLong;

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
}
