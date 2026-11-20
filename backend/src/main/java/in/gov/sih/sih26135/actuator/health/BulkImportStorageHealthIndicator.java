package in.gov.sih.sih26135.actuator.health;

import in.gov.sih.sih26135.config.ImportPipelineProperties;
import java.io.File;
import java.nio.file.Path;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

/**
 * Health indicator monitoring disk space availability for the bulk import staging filesystem.
 *
 * <p>Principles:
 * <ul>
 *   <li>Evaluates usable space on the filesystem hosting the staging directory.</li>
 *   <li>Configurable threshold (default: 1 GiB = 1,073,741,824 bytes).</li>
 *   <li>Strictly zero file creation, modification, or deletion during health evaluations.</li>
 *   <li>Zero public disclosure of absolute filesystem paths.</li>
 *   <li>Low staging space is surfaced as a warning status or within the import health group without bringing the entire application DOWN.</li>
 * </ul>
 */
@Component("bulkImportStorage")
public class BulkImportStorageHealthIndicator implements HealthIndicator {

  private static final Logger log = LoggerFactory.getLogger(BulkImportStorageHealthIndicator.class);

  private final ImportPipelineProperties importPipelineProperties;
  private final long minDiskSpaceBytes;

  @Autowired
  public BulkImportStorageHealthIndicator(
      ImportPipelineProperties importPipelineProperties,
      @Value("${management.health.bulk-import.min-disk-space-bytes:1073741824}") long minDiskSpaceBytes) {
    this.importPipelineProperties = Objects.requireNonNull(importPipelineProperties, "ImportPipelineProperties is required");
    this.minDiskSpaceBytes = minDiskSpaceBytes;
  }

  @Override
  public Health health() {
    try {
      String stagingPathStr = importPipelineProperties.getStagingDirectory();
      File stagingDir = new File(stagingPathStr);

      // If directory doesn't exist yet, inspect its parent or tmpdir
      File targetDir = stagingDir.exists() ? stagingDir : stagingDir.getParentFile();
      if (targetDir == null || !targetDir.exists()) {
        targetDir = new File(System.getProperty("java.io.tmpdir"));
      }

      long usableBytes = targetDir.getUsableSpace();
      long totalBytes = targetDir.getTotalSpace();

      double usableGiB = Math.round((usableBytes / (1024.0 * 1024.0 * 1024.0)) * 100.0) / 100.0;
      double thresholdGiB = Math.round((minDiskSpaceBytes / (1024.0 * 1024.0 * 1024.0)) * 100.0) / 100.0;

      if (usableBytes < minDiskSpaceBytes) {
        log.warn("Bulk import staging filesystem usable space is below threshold: {} GiB < {} GiB", usableGiB, thresholdGiB);
        return Health.status("LOW_DISK_SPACE")
            .withDetail("storageConfigured", true)
            .withDetail("usableSpaceBytes", usableBytes)
            .withDetail("thresholdBytes", minDiskSpaceBytes)
            .withDetail("usableSpaceGiB", usableGiB)
            .withDetail("thresholdGiB", thresholdGiB)
            .withDetail("message", "Usable disk space on bulk import staging filesystem is below the 1 GiB threshold")
            .build();
      }

      return Health.up()
          .withDetail("storageConfigured", true)
          .withDetail("usableSpaceBytes", usableBytes)
          .withDetail("thresholdBytes", minDiskSpaceBytes)
          .withDetail("usableSpaceGiB", usableGiB)
          .withDetail("thresholdGiB", thresholdGiB)
          .build();

    } catch (Exception ex) {
      log.error("Failed to check bulk import storage health: {}", ex.getMessage());
      return Health.down(ex)
          .withDetail("storageConfigured", false)
          .withDetail("error", "Failed to access bulk import staging filesystem")
          .build();
    }
  }

  public long getMinDiskSpaceBytes() {
    return minDiskSpaceBytes;
  }
}
