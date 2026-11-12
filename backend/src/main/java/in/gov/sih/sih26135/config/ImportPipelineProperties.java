package in.gov.sih.sih26135.config;

import java.nio.file.Path;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Configuration properties for Phase 28.5 bulk data ingestion, staged file storage,
 * memory-bounded streaming parsers, and chunked transactional persistence.
 */
@Component
@ConfigurationProperties(prefix = "import-pipeline")
public class ImportPipelineProperties {

  /**
   * Supported runtime topology for bulk import execution and interrupted-batch recovery.
   */
  public enum DeploymentMode {
    SINGLE_INSTANCE,
    MULTI_INSTANCE
  }

  /**
   * Declared deployment topology (defaults to {@link DeploymentMode#SINGLE_INSTANCE}).
   * Multi-instance mode fails closed on manual RUNNING-batch recovery because worker liveness
   * cannot be verified across JVMs without shared lease coordination in the frozen schema.
   */
  private DeploymentMode deploymentMode = DeploymentMode.SINGLE_INSTANCE;

  /**
   * Default transactional persistence chunk size (recommended range: 50-100).
   */
  private int chunkSize = 100;

  /**
   * Maximum permitted uploaded file size in bytes (default: 10 MiB).
   */
  private long maxFileSizeBytes = 10L * 1024L * 1024L;

  /**
   * Maximum permitted source data rows per import batch (default: 10,000 rows).
   */
  private int maxRowsPerBatch = 10000;

  /**
   * Maximum permitted single record byte/character length during streaming parsing (default: 64 KiB).
   */
  private int maxRowBytes = 65536;

  /**
   * Server-controlled staging root directory for uploaded CSV/JSON import files.
   */
  private String stagingDirectory =
      Path.of(System.getProperty("java.io.tmpdir"), "sih26135-import-staging").toString();

  public DeploymentMode getDeploymentMode() {
    return deploymentMode;
  }

  public void setDeploymentMode(DeploymentMode deploymentMode) {
    this.deploymentMode = deploymentMode;
  }

  public int getChunkSize() {
    return chunkSize;
  }

  public void setChunkSize(int chunkSize) {
    if (chunkSize >= 1 && chunkSize <= 1000) {
      this.chunkSize = chunkSize;
    }
  }

  public long getMaxFileSizeBytes() {
    return maxFileSizeBytes;
  }

  public void setMaxFileSizeBytes(long maxFileSizeBytes) {
    if (maxFileSizeBytes > 0) {
      this.maxFileSizeBytes = maxFileSizeBytes;
    }
  }

  public int getMaxRowsPerBatch() {
    return maxRowsPerBatch;
  }

  public void setMaxRowsPerBatch(int maxRowsPerBatch) {
    if (maxRowsPerBatch > 0) {
      this.maxRowsPerBatch = maxRowsPerBatch;
    }
  }

  public int getMaxRowBytes() {
    return maxRowBytes;
  }

  public void setMaxRowBytes(int maxRowBytes) {
    if (maxRowBytes >= 1024) {
      this.maxRowBytes = maxRowBytes;
    }
  }

  public String getStagingDirectory() {
    return stagingDirectory;
  }

  public void setStagingDirectory(String stagingDirectory) {
    if (stagingDirectory != null && !stagingDirectory.isBlank()) {
      this.stagingDirectory = stagingDirectory.trim();
    }
  }
}
