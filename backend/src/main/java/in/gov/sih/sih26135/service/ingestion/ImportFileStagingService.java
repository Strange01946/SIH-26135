package in.gov.sih.sih26135.service.ingestion;

import in.gov.sih.sih26135.config.ImportPipelineProperties;
import in.gov.sih.sih26135.exception.BadRequestException;
import in.gov.sih.sih26135.repository.ImportBatchRepository;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.UUID;
import java.util.regex.Pattern;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

/**
 * Durably stages uploaded bulk import files to a server-controlled staging directory using
 * bounded streaming I/O (8 KiB buffers, zero full-file byte buffering in JVM heap) and strict
 * canonical path-traversal guards.
 */
@Component
public class ImportFileStagingService {

  public enum FileFormat {
    CSV("csv"),
    JSON("json");

    private final String extension;

    FileFormat(String extension) {
      this.extension = extension;
    }

    public String getExtension() {
      return extension;
    }
  }

  public record StagedFileResult(String batchCode, FileFormat format, Path stagedPath, long byteCount) {}

  private static final Pattern SAFE_BATCH_CODE_PATTERN = Pattern.compile("^[A-Za-z0-9_-]{8,32}$");
  private static final DateTimeFormatter DATE_PREFIX_FMT = DateTimeFormatter.BASIC_ISO_DATE;
  private static final int IO_BUFFER_SIZE = 8192;

  private final ImportPipelineProperties properties;
  private final ImportBatchRepository importBatchRepository;

  public ImportFileStagingService(
      ImportPipelineProperties properties,
      ImportBatchRepository importBatchRepository) {
    this.properties = Objects.requireNonNull(properties, "properties is required");
    this.importBatchRepository = Objects.requireNonNull(importBatchRepository, "importBatchRepository is required");
  }

  public String generateUniqueBatchCode() {
    String datePrefix = LocalDate.now().format(DATE_PREFIX_FMT);
    for (int attempt = 0; attempt < 8; attempt++) {
      String suffix = UUID.randomUUID().toString().replace("-", "").substring(0, 16).toUpperCase(Locale.ROOT);
      String candidate = "IMP-" + datePrefix + "-" + suffix; // 29 chars <= 32
      if (!importBatchRepository.existsByBatchCode(candidate)) {
        return candidate;
      }
    }
    throw new IllegalStateException("Unable to allocate unique import batch code");
  }

  public FileFormat detectAndValidateFormat(MultipartFile file) {
    if (file == null || file.isEmpty()) {
      throw new BadRequestException("Uploaded import file cannot be empty", "EMPTY_IMPORT_FILE");
    }
    if (file.getSize() > properties.getMaxFileSizeBytes()) {
      throw new BadRequestException(
          "Uploaded file exceeds maximum allowed size",
          "FILE_SIZE_LIMIT_EXCEEDED"
      );
    }

    String originalFilename = file.getOriginalFilename();
    String ext = extractSanitizedExtension(originalFilename);
    String contentType = file.getContentType() != null
        ? file.getContentType().trim().toLowerCase(Locale.ROOT)
        : "";

    if (!ext.isEmpty()) {
      if ("csv".equals(ext)) {
        return FileFormat.CSV;
      }
      if ("json".equals(ext) || "ndjson".equals(ext)) {
        return FileFormat.JSON;
      }
      throw new BadRequestException(
          "Unsupported file format; only CSV and JSON/NDJSON uploads are supported",
          "UNSUPPORTED_FILE_FORMAT"
      );
    }

    if (contentType.contains("csv") || "text/plain".equals(contentType)) {
      return FileFormat.CSV;
    }
    if (contentType.contains("json") || contentType.contains("ndjson")) {
      return FileFormat.JSON;
    }
    throw new BadRequestException(
        "Unsupported or undetectable file format; provide a .csv or .json/.ndjson upload",
        "UNSUPPORTED_FILE_FORMAT"
    );
  }

  /**
   * Streams the uploaded multipart content into a temporary staging file with bounded chunks,
   * enforces max byte limits during read, rejects whitespace-only payloads, and atomically
   * commits to the final staging file keyed strictly by server-generated {@code batchCode}.
   */
  public StagedFileResult stageFile(MultipartFile file, String batchCode, FileFormat format) {
    validateSafeBatchCode(batchCode);
    Path stagingRoot = ensureStagingRoot();
    Path tempFile = resolveSafeChild(stagingRoot, batchCode + "." + format.getExtension() + ".part");
    Path finalFile = resolveSafeChild(stagingRoot, batchCode + "." + format.getExtension());

    long totalBytes = 0;
    boolean sawNonWhitespace = false;
    byte[] buffer = new byte[IO_BUFFER_SIZE];

    try (InputStream rawIn = new BufferedInputStream(file.getInputStream(), IO_BUFFER_SIZE);
         OutputStream out = new BufferedOutputStream(Files.newOutputStream(tempFile), IO_BUFFER_SIZE)) {

      int read;
      while ((read = rawIn.read(buffer)) != -1) {
        totalBytes += read;
        if (totalBytes > properties.getMaxFileSizeBytes()) {
          throw new BadRequestException(
              "Uploaded file exceeds maximum allowed size",
              "FILE_SIZE_LIMIT_EXCEEDED"
          );
        }
        if (!sawNonWhitespace) {
          for (int i = 0; i < read; i++) {
            int b = buffer[i] & 0xFF;
            if (b != ' ' && b != '\t' && b != '\r' && b != '\n') {
              sawNonWhitespace = true;
              break;
            }
          }
        }
        out.write(buffer, 0, read);
      }
      out.flush();
    } catch (BadRequestException e) {
      deletePathQuietly(tempFile);
      throw e;
    } catch (IOException e) {
      deletePathQuietly(tempFile);
      throw new IllegalStateException("Failed to stage uploaded import file durably", e);
    }

    if (totalBytes == 0 || !sawNonWhitespace) {
      deletePathQuietly(tempFile);
      throw new BadRequestException("Uploaded import file cannot be empty", "EMPTY_IMPORT_FILE");
    }

    try {
      try {
        Files.move(tempFile, finalFile, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
      } catch (AtomicMoveNotSupportedException e) {
        Files.move(tempFile, finalFile, StandardCopyOption.REPLACE_EXISTING);
      }
      if (!Files.exists(finalFile) || Files.size(finalFile) != totalBytes) {
        throw new IllegalStateException("Staged file integrity verification failed");
      }
      return new StagedFileResult(batchCode, format, finalFile, totalBytes);
    } catch (IOException e) {
      deletePathQuietly(tempFile);
      deletePathQuietly(finalFile);
      throw new IllegalStateException("Failed to commit staged import file", e);
    }
  }

  public StreamingImportParser openParser(String batchCode, String entityType) {
    validateSafeBatchCode(batchCode);
    Path stagingRoot = ensureStagingRoot();

    for (FileFormat fmt : List.of(FileFormat.CSV, FileFormat.JSON)) {
      Path candidate = resolveSafeChild(stagingRoot, batchCode + "." + fmt.getExtension());
      if (Files.isRegularFile(candidate) && Files.isReadable(candidate)) {
        try {
          InputStream is = new BufferedInputStream(Files.newInputStream(candidate), IO_BUFFER_SIZE);
          return fmt == FileFormat.CSV
              ? new StreamingCsvRecordParser(is, entityType, properties.getMaxRowBytes())
              : new StreamingJsonRecordParser(is, entityType, properties.getMaxRowBytes());
        } catch (IOException e) {
          throw new StreamingImportParser.ImportStreamParseException(
              "STAGED_FILE_NOT_FOUND",
              "Failed to open staged import file stream",
              e
          );
        }
      }
    }
    throw new StreamingImportParser.ImportStreamParseException(
        "STAGED_FILE_NOT_FOUND",
        "Staged import file is unavailable for batch processing"
    );
  }

  public boolean hasStagedFile(String batchCode) {
    if (batchCode == null || !SAFE_BATCH_CODE_PATTERN.matcher(batchCode).matches()) {
      return false;
    }
    Path stagingRoot = Path.of(properties.getStagingDirectory()).toAbsolutePath().normalize();
    for (FileFormat fmt : FileFormat.values()) {
      Path candidate = stagingRoot.resolve(batchCode + "." + fmt.getExtension()).normalize();
      if (candidate.startsWith(stagingRoot) && Files.isRegularFile(candidate)) {
        return true;
      }
    }
    return false;
  }

  public void deleteStagedFileQuietly(String batchCode) {
    if (batchCode == null || !SAFE_BATCH_CODE_PATTERN.matcher(batchCode).matches()) {
      return;
    }
    Path stagingRoot = Path.of(properties.getStagingDirectory()).toAbsolutePath().normalize();
    for (FileFormat fmt : FileFormat.values()) {
      deletePathQuietly(stagingRoot.resolve(batchCode + "." + fmt.getExtension()).normalize());
      deletePathQuietly(stagingRoot.resolve(batchCode + "." + fmt.getExtension() + ".part").normalize());
    }
  }

  public void deleteStagedFileQuietly(Path path) {
    deletePathQuietly(path);
  }

  private void validateSafeBatchCode(String batchCode) {
    if (batchCode == null || !SAFE_BATCH_CODE_PATTERN.matcher(batchCode).matches()) {
      throw new BadRequestException("Invalid batch code format", "INVALID_BATCH_CODE");
    }
  }

  private Path ensureStagingRoot() {
    Path root = Path.of(properties.getStagingDirectory()).toAbsolutePath().normalize();
    try {
      Files.createDirectories(root);
      return root;
    } catch (IOException e) {
      throw new IllegalStateException("Unable to initialize import staging directory", e);
    }
  }

  private Path resolveSafeChild(Path stagingRoot, String safeFileName) {
    Path resolved = stagingRoot.resolve(safeFileName).normalize();
    if (!resolved.startsWith(stagingRoot) || !resolved.getParent().equals(stagingRoot)) {
      throw new IllegalStateException("Resolved staging path escaped staging root");
    }
    return resolved;
  }

  private String extractSanitizedExtension(String filename) {
    if (filename == null || filename.isBlank()) {
      return "";
    }
    // Strip directory prefixes safely to prevent path traversal tricks
    String baseName = filename.replace('\\', '/');
    int lastSlash = baseName.lastIndexOf('/');
    if (lastSlash >= 0) {
      baseName = baseName.substring(lastSlash + 1);
    }
    int dotIdx = baseName.lastIndexOf('.');
    if (dotIdx < 0 || dotIdx == baseName.length() - 1) {
      return "";
    }
    return baseName.substring(dotIdx + 1).trim().toLowerCase(Locale.ROOT);
  }

  private void deletePathQuietly(Path path) {
    if (path != null) {
      try {
        Files.deleteIfExists(path);
      } catch (IOException ignored) {
      }
    }
  }
}
