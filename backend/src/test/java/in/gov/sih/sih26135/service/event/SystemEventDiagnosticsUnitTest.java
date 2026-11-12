package in.gov.sih.sih26135.service.event;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import in.gov.sih.sih26135.config.AsyncConfig;
import in.gov.sih.sih26135.config.SystemEventLoggingProperties;
import in.gov.sih.sih26135.correlation.CorrelationContextHolder;
import in.gov.sih.sih26135.correlation.CorrelationIdFilter;
import in.gov.sih.sih26135.dto.response.SystemEventLogResponse;
import in.gov.sih.sih26135.entity.RefSystemEventCategory;
import in.gov.sih.sih26135.entity.RefSystemEventSeverity;
import in.gov.sih.sih26135.entity.SystemEventLog;
import in.gov.sih.sih26135.exception.BadRequestException;
import in.gov.sih.sih26135.mapper.SystemEventLogMapper;
import in.gov.sih.sih26135.repository.RefSystemEventCategoryRepository;
import in.gov.sih.sih26135.repository.RefSystemEventSeverityRepository;
import in.gov.sih.sih26135.repository.SystemEventLogRepository;
import in.gov.sih.sih26135.repository.UserRepository;
import in.gov.sih.sih26135.service.SystemEventLoggingService.EventCategory;
import in.gov.sih.sih26135.service.SystemEventLoggingService.EventSeverity;
import in.gov.sih.sih26135.service.impl.SystemEventLoggingServiceImpl;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.slf4j.MDC;
import org.springframework.core.task.TaskDecorator;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.SimpleTransactionStatus;

@DisplayName("Phase 28.6 - SystemEventLoggingService, Sanitization & Async Correlation Unit Tests")
class SystemEventDiagnosticsUnitTest {

  @AfterEach
  void cleanContext() {
    CorrelationContextHolder.clear();
    MDC.clear();
  }

  private SystemEventLoggingServiceImpl createService(
      SystemEventLogRepository eventRepo,
      RefSystemEventCategoryRepository catRepo,
      RefSystemEventSeverityRepository sevRepo,
      UserRepository userRepo) {
    SystemEventLoggingProperties props = new SystemEventLoggingProperties();
    PlatformTransactionManager txManager = mock(PlatformTransactionManager.class);
    when(txManager.getTransaction(any())).thenReturn(new SimpleTransactionStatus());
    return new SystemEventLoggingServiceImpl(
        eventRepo,
        catRepo,
        sevRepo,
        userRepo,
        new SystemEventLogMapper(),
        props,
        txManager);
  }

  @Test
  @DisplayName("Supports all 5 categories, all 4 severities, server metadata, and correlation tagging")
  void supportsAllCategoriesSeveritiesAndMetadata() {
    SystemEventLogRepository eventRepo = mock(SystemEventLogRepository.class);
    RefSystemEventCategoryRepository catRepo = mock(RefSystemEventCategoryRepository.class);
    RefSystemEventSeverityRepository sevRepo = mock(RefSystemEventSeverityRepository.class);
    UserRepository userRepo = mock(UserRepository.class);

    long catId = 1L;
    for (EventCategory cat : EventCategory.values()) {
      RefSystemEventCategory refCat = new RefSystemEventCategory(cat.name(), cat.name(), (int) catId);
      refCat.setId(catId++);
      when(catRepo.findByCategoryCode(cat.name())).thenReturn(Optional.of(refCat));
    }
    long sevId = 1L;
    for (EventSeverity sev : EventSeverity.values()) {
      RefSystemEventSeverity refSev = new RefSystemEventSeverity(sev.name(), sev.name(), (int) sevId);
      refSev.setId(sevId++);
      when(sevRepo.findBySeverityCode(sev.name())).thenReturn(Optional.of(refSev));
    }
    when(userRepo.existsById(42L)).thenReturn(true);
    when(eventRepo.saveAndFlush(any(SystemEventLog.class))).thenAnswer(inv -> {
      SystemEventLog e = inv.getArgument(0);
      e.setId(100L);
      return e;
    });

    SystemEventLoggingServiceImpl service = createService(eventRepo, catRepo, sevRepo, userRepo);
    CorrelationContextHolder.setCorrelationId("corr-unit-001");
    CorrelationContextHolder.setRequestId("req-unit-001");

    for (EventCategory cat : EventCategory.values()) {
      for (EventSeverity sev : EventSeverity.values()) {
        Optional<SystemEventLogResponse> res = service.logEvent(
            cat, sev, "EVT_" + cat.name() + "_" + sev.name(),
            42L, "TestEntity", 77L, "UNIT_COMP", "Operation status detail");
        assertThat(res).isPresent();
        assertThat(res.get().systemEventCategoryCode()).isEqualTo(cat.name());
        assertThat(res.get().systemEventSeverityCode()).isEqualTo(sev.name());
        assertThat(res.get().actorUserId()).isEqualTo(42L);
        assertThat(res.get().occurredAt()).isNotNull();
        assertThat(res.get().createdAt()).isNotNull();
        assertThat(res.get().eventSummary()).startsWith("[cid=corr-unit-001|rid=req-unit-001] ");
      }
    }

    assertThatThrownBy(() -> service.logEvent(null, EventSeverity.INFO, "C", null, null, null, null, "m"))
        .isInstanceOf(BadRequestException.class);
    assertThatThrownBy(() -> service.logEvent(EventCategory.SECURITY, null, "C", null, null, null, null, "m"))
        .isInstanceOf(BadRequestException.class);
  }

  @Test
  @DisplayName("Redacts passwords, JWTs, Aadhaar, PAN, JDBC URLs, CRLF, and stack traces and bounds summary to 500 chars")
  void sanitizesSensitiveDataAndBoundsLength() {
    String dirty = "Auth failed\r\npassword=MySecret123 Bearer abcdefghijk123456 "
        + "eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJ0ZXN0In0.sigval123 "
        + "jdbc:mysql://localhost:3306/SIH26135?password=root "
        + "Aadhaar 2345 6789 0123 PAN ABCDE1234F "
        + "at in.gov.sih.Service.fail(Service.java:42) "
        + "X".repeat(600);

    String sanitized = SystemEventSanitizer.formatWithCorrelation(
        dirty, "FALLBACK", "corr-san-1", "req-san-1");

    assertThat(sanitized).hasSizeLessThanOrEqualTo(500);
    assertThat(sanitized).doesNotContain("\r", "\n", "MySecret123", "abcdefghijk123456",
        "eyJhbGci", "jdbc:mysql", "2345 6789", "ABCDE1234F", "Service.java");
    assertThat(sanitized).startsWith("[cid=corr-san-1|rid=req-san-1] ");
  }

  @Test
  @DisplayName("Isolates persistence failures safely without recursion and prevents duplicate instrumentation in same request")
  void handlesPersistenceFailuresRecursivelySafeAndDeduplicates() {
    SystemEventLogRepository eventRepo = mock(SystemEventLogRepository.class);
    RefSystemEventCategoryRepository catRepo = mock(RefSystemEventCategoryRepository.class);
    RefSystemEventSeverityRepository sevRepo = mock(RefSystemEventSeverityRepository.class);
    UserRepository userRepo = mock(UserRepository.class);

    RefSystemEventCategory batchCat = new RefSystemEventCategory("BATCH_JOB", "Batch", 3);
    batchCat.setId(3L);
    RefSystemEventSeverity errSev = new RefSystemEventSeverity("ERROR", "Error", 3);
    errSev.setId(3L);

    when(catRepo.findByCategoryCode("BATCH_JOB")).thenReturn(Optional.of(batchCat));
    when(sevRepo.findBySeverityCode("ERROR")).thenReturn(Optional.of(errSev));

    SystemEventLoggingServiceImpl service = createService(eventRepo, catRepo, sevRepo, userRepo);
    CorrelationContextHolder.setCorrelationId("corr-dedup-1");
    CorrelationContextHolder.setRequestId("req-dedup-1");

    // 1. Simulate recursive logging triggered during saveAndFlush + DB error
    AtomicInteger attempts = new AtomicInteger();
    when(eventRepo.saveAndFlush(any(SystemEventLog.class))).thenAnswer(inv -> {
      attempts.incrementAndGet();
      // Recursive attempt must be suppressed cleanly without stack overflow or second save
      Optional<SystemEventLogResponse> nested = service.logEvent(
          EventCategory.SECURITY, EventSeverity.CRITICAL, "NESTED_LOOP",
          null, "System", null, "RECURSIVE_TEST", "nested");
      assertThat(nested).isEmpty();
      throw new DataAccessResourceFailureException("Simulated DB down");
    });

    // Persistence failure must NOT throw back and mask caller outcome
    Optional<SystemEventLogResponse> failedAttempt = service.logEvent(
        EventCategory.BATCH_JOB, EventSeverity.ERROR, "IMPORT_BATCH_FAILED",
        null, "ImportBatch", 10L, "IMPORT_WORKFLOW", "Failed batch");
    assertThat(failedAttempt).isEmpty();
    assertThat(attempts.get()).isEqualTo(1);

    // 2. Restore healthy DB -> accidental double instrumentation inside same (corr, req, code, entity) emits once
    when(eventRepo.saveAndFlush(any(SystemEventLog.class))).thenAnswer(inv -> {
      SystemEventLog e = inv.getArgument(0);
      e.setId(555L);
      return e;
    });
    Optional<SystemEventLogResponse> first = service.logEvent(
        EventCategory.BATCH_JOB, EventSeverity.ERROR, "IMPORT_BATCH_FAILED",
        null, "ImportBatch", 10L, "IMPORT_WORKFLOW", "Failed batch");
    Optional<SystemEventLogResponse> second = service.logEvent(
        EventCategory.BATCH_JOB, EventSeverity.ERROR, "IMPORT_BATCH_FAILED",
        null, "ImportBatch", 10L, "IMPORT_WORKFLOW", "Failed batch");
    assertThat(first).isPresent();
    assertThat(second).isEmpty();
    verify(eventRepo, times(2)).saveAndFlush(any(SystemEventLog.class));
  }

  @Test
  @DisplayName("TaskDecorator propagates correlationId with distinct async requestId, creates background IDs when absent, and cleans reused worker thread")
  void propagatesAndCleansAsyncAndBackgroundCorrelation() throws Exception {
    AsyncConfig asyncConfig = new AsyncConfig();
    TaskDecorator decorator = asyncConfig.correlationTaskDecorator();
    var singleWorker = Executors.newSingleThreadExecutor();
    try {
      // Pre-pollute worker thread to verify restoration/cleanup
      singleWorker.submit(() -> {
        MDC.put("staleKey", "staleVal");
        CorrelationContextHolder.setCorrelationId("stale-corr");
      }).get(5, TimeUnit.SECONDS);

      // Case A: Parent thread has HTTP correlation context
      CorrelationContextHolder.setCorrelationId("http-parent-corr-123");
      CorrelationContextHolder.setRequestId("http-parent-req-123");
      MDC.put(CorrelationIdFilter.MDC_CORRELATION_ID_KEY, "http-parent-corr-123");
      MDC.put(CorrelationIdFilter.MDC_REQUEST_ID_KEY, "http-parent-req-123");

      AtomicReference<String> asyncCorr = new AtomicReference<>();
      AtomicReference<String> asyncReq = new AtomicReference<>();
      AtomicReference<String> asyncMdcCorr = new AtomicReference<>();
      AtomicReference<String> asyncMdcReq = new AtomicReference<>();
      CountDownLatch doneA = new CountDownLatch(1);

      Runnable taskA = decorator.decorate(() -> {
        asyncCorr.set(CorrelationContextHolder.getCorrelationId());
        asyncReq.set(CorrelationContextHolder.getRequestId());
        asyncMdcCorr.set(MDC.get(CorrelationIdFilter.MDC_CORRELATION_ID_KEY));
        asyncMdcReq.set(MDC.get(CorrelationIdFilter.MDC_REQUEST_ID_KEY));
        doneA.countDown();
      });

      CorrelationContextHolder.clear();
      MDC.clear();
      singleWorker.submit(taskA).get(5, TimeUnit.SECONDS);
      assertThat(doneA.await(5, TimeUnit.SECONDS)).isTrue();

      assertThat(asyncCorr.get()).isEqualTo("http-parent-corr-123");
      assertThat(asyncMdcCorr.get()).isEqualTo("http-parent-corr-123");
      assertThat(CorrelationIdFilter.isValidCorrelationId(asyncReq.get())).isTrue();
      assertThat(asyncReq.get()).isEqualTo(asyncMdcReq.get());
      assertThat(asyncReq.get()).isNotEqualTo("http-parent-req-123");

      // Case B: Background job without parent correlation gets server-owned UUID context, and leaves worker clean
      AtomicReference<String> bgCorr = new AtomicReference<>();
      AtomicReference<String> bgReq = new AtomicReference<>();
      Runnable taskB = decorator.decorate(() -> {
        bgCorr.set(CorrelationContextHolder.getCorrelationId());
        bgReq.set(CorrelationContextHolder.getRequestId());
      });
      singleWorker.submit(taskB).get(5, TimeUnit.SECONDS);

      assertThat(UUID.fromString(bgCorr.get())).isNotNull();
      assertThat(UUID.fromString(bgReq.get())).isNotNull();
      assertThat(bgCorr.get()).isNotEqualTo("http-parent-corr-123");

      // Verify reused worker thread does not retain background correlation context (or if pre-existing, only restores pre-existing)
      AtomicReference<String> afterHolder = new AtomicReference<>();
      AtomicReference<String> afterMdc = new AtomicReference<>();
      singleWorker.submit(() -> {
        // Note: first task inherited the test-polluted 'stale-corr' restoration; clear once and verify decorator leaves clean
        CorrelationContextHolder.clear();
        MDC.clear();
      }).get(5, TimeUnit.SECONDS);

      singleWorker.submit(decorator.decorate(() -> {})).get(5, TimeUnit.SECONDS);
      singleWorker.submit(() -> {
        afterHolder.set(CorrelationContextHolder.getCorrelationId());
        afterMdc.set(MDC.get(CorrelationIdFilter.MDC_CORRELATION_ID_KEY));
      }).get(5, TimeUnit.SECONDS);
      assertThat(afterHolder.get()).isNull();
      assertThat(afterMdc.get()).isNull();
    } finally {
      singleWorker.shutdownNow();
    }
  }

  @Test
  @DisplayName("Strips/escapes forged correlation markers, applies single canonical prefix, and honors 500-char limit")
  void stripsForgedCorrelationMarkersWithoutDuplicatingCanonicalPrefix() {
    String forgedPrefix = "[cid=forged-c1|rid=forged-r1] Admin action detail " + "Z".repeat(490);
    String formattedLeading = SystemEventSanitizer.formatWithCorrelation(
        forgedPrefix, "FALLBACK", "srv-corr-1", "srv-req-1");
    assertThat(formattedLeading).startsWith("[cid=srv-corr-1|rid=srv-req-1] Admin action detail");
    assertThat(formattedLeading).doesNotContain("[cid=forged-", "|rid=forged-");
    assertThat(formattedLeading).hasSizeLessThanOrEqualTo(500);

    String forgedMiddle = "Note [cid=forged-mid|rid=forged-mid-req] and |rid=tail-forgery] with password=Secret#1";
    String formattedMiddle = SystemEventSanitizer.formatWithCorrelation(
        forgedMiddle, "FALLBACK", "srv-corr-2", "srv-req-2");
    assertThat(formattedMiddle).startsWith("[cid=srv-corr-2|rid=srv-req-2] ");
    assertThat(formattedMiddle).doesNotContain("[cid=forged-", "|rid=forged-", "|rid=tail-forgery", "Secret#1");
    assertThat(formattedMiddle).contains("password=***REDACTED***");

    // Prefix appears only once at position 0
    assertThat(formattedMiddle.indexOf("[cid=")).isZero();
    assertThat(formattedMiddle.indexOf("[cid=", 1)).isEqualTo(-1);
  }
}
