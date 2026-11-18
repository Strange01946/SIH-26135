package in.gov.sih.sih26135.communication.queue;

import in.gov.sih.sih26135.communication.dto.CommunicationDispatchCommand;
import in.gov.sih.sih26135.communication.dto.DispatchAttemptDto;
import in.gov.sih.sih26135.communication.dto.DispatchJobDto;
import in.gov.sih.sih26135.communication.dto.NotificationDispatchResponse;
import java.util.List;

/**
 * Service contract for persistent, crash-resilient asynchronous notification queuing,
 * atomic worker claiming, bounded exponential backoff retries, and dead-letter recovery.
 */
public interface DurableDispatchQueueService {

  /**
   * Persists an authorized dispatch request as a PENDING job before asynchronous execution.
   *
   * @param command dispatch command
   * @param idempotencyKey unique idempotency token (caller-supplied or server-generated)
   * @return persisted dispatch job metadata
   */
  DispatchJobDto enqueueJob(CommunicationDispatchCommand command, String idempotencyKey);

  /**
   * Atomically claims and processes an enqueued dispatch job by ID.
   *
   * @param jobId job ID
   * @param workerId unique identifier of the executing worker
   * @return dispatch response if successfully processed, or null if job could not be claimed
   */
  NotificationDispatchResponse claimAndProcessJob(Long jobId, String workerId);

  /**
   * Retrieves a job by ID.
   */
  DispatchJobDto getJobById(Long jobId);

  /**
   * Retrieves all attempt audit records for a job.
   */
  List<DispatchAttemptDto> getAttemptsByJobId(Long jobId);

  /**
   * Retrieves paginated dead-letter jobs.
   */
  List<DispatchJobDto> getDeadLetterJobs(int offset, int limit);

  /**
   * Replays an exhausted or dead-lettered dispatch job after revalidating consent and rate limits.
   *
   * @param jobId job ID
   * @param operatorUsername authenticated operator triggering the replay
   * @return updated job metadata
   */
  DispatchJobDto replayDeadLetterJob(Long jobId, String operatorUsername);

  /**
   * Recovers jobs abandoned in PROCESSING status due to process crashes or network partition.
   *
   * @param staleThresholdMinutes minutes after which a locked job is considered stale
   * @return number of recovered jobs
   */
  int recoverStaleJobs(int staleThresholdMinutes);
}
