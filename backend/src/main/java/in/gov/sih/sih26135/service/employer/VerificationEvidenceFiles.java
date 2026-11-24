package in.gov.sih.sih26135.service.employer;

import in.gov.sih.sih26135.exception.BadRequestException;
import in.gov.sih.sih26135.exception.ResourceNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Locale;
import java.util.UUID;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;

@Component
public class VerificationEvidenceFiles {
  private static final int MAX_BYTES = 5 * 1024 * 1024;
  private final Path directory;

  public VerificationEvidenceFiles(@Value("${SIH_VERIFICATION_EVIDENCE_ROOT:${user.home}/.sih26135/verification-evidence}") String directory) {
    this.directory = Path.of(directory).toAbsolutePath().normalize();
  }

  public StoredFile save(MultipartFile upload) {
    if (upload.isEmpty() || upload.getSize() > MAX_BYTES) {
      throw new BadRequestException("Choose a PDF or JPEG file up to 5 MB.", "INVALID_EVIDENCE_SIZE");
    }
    byte[] bytes;
    try (var stream = upload.getInputStream()) {
      bytes = stream.readNBytes(MAX_BYTES + 1);
    } catch (IOException ex) {
      throw new BadRequestException("The evidence file could not be read.", "EVIDENCE_READ_FAILED");
    }
    if (bytes.length > MAX_BYTES) {
      throw new BadRequestException("The evidence file exceeds 5 MB.", "INVALID_EVIDENCE_SIZE");
    }
    String extension = extension(bytes);
    String original = upload.getOriginalFilename();
    String expectedSuffix = extension.equals("pdf") ? "\\.pdf$" : "\\.jpe?g$";
    if (original == null || !original.toLowerCase(Locale.ROOT).matches(".*" + expectedSuffix)) {
      throw new BadRequestException("The evidence extension must match its PDF or JPEG content.", "INVALID_EVIDENCE_TYPE");
    }
    String filename = original.replace('\\', '/');
    filename = filename.substring(filename.lastIndexOf('/') + 1).replaceAll("[\\p{Cntrl}]", "");
    if (filename.length() > 200) {
      throw new BadRequestException("The evidence filename must be 200 characters or fewer.", "INVALID_EVIDENCE_NAME");
    }
    String reference = "EMP-" + UUID.randomUUID() + "." + extension;
    Path target = directory.resolve(reference);
    if (!TransactionSynchronizationManager.isSynchronizationActive()) {
      throw new IllegalStateException("Evidence storage requires a transaction");
    }
    try {
      Files.createDirectories(directory);
      Files.createFile(target);
    } catch (IOException ex) {
      throw new IllegalStateException("Verification evidence could not be stored", ex);
    }
    TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
      @Override
      public void afterCompletion(int status) {
        if (status == STATUS_ROLLED_BACK) {
          try {
            Files.deleteIfExists(target);
          } catch (IOException ex) {
            LoggerFactory.getLogger(VerificationEvidenceFiles.class).warn("Could not remove rolled-back verification evidence");
          }
        }
      }
    });
    try {
      Files.write(target, bytes, StandardOpenOption.WRITE);
    } catch (IOException ex) {
      throw new IllegalStateException("Verification evidence could not be stored", ex);
    }
    return new StoredFile(reference, filename, sha256(bytes));
  }

  public Resource read(String reference) {
    if (reference == null || !reference.matches("EMP-[0-9a-f-]{36}\\.(pdf|jpg)")) {
      throw new ResourceNotFoundException("Evidence file is unavailable");
    }
    Path target = directory.resolve(reference);
    if (!Files.isRegularFile(target, java.nio.file.LinkOption.NOFOLLOW_LINKS)) {
      throw new ResourceNotFoundException("Evidence file is unavailable");
    }
    return new FileSystemResource(target);
  }

  public MediaType mediaType(String reference) {
    return reference.endsWith(".pdf") ? MediaType.APPLICATION_PDF : MediaType.IMAGE_JPEG;
  }

  private static String extension(byte[] bytes) {
    if (bytes.length > 5 && bytes[0] == '%' && bytes[1] == 'P' && bytes[2] == 'D'
        && bytes[3] == 'F' && bytes[4] == '-') {
      return "pdf";
    }
    if (bytes.length > 4 && (bytes[0] & 0xff) == 0xff && (bytes[1] & 0xff) == 0xd8 && (bytes[2] & 0xff) == 0xff
        && (bytes[bytes.length - 2] & 0xff) == 0xff && (bytes[bytes.length - 1] & 0xff) == 0xd9) {
      return "jpg";
    }
    throw new BadRequestException("Evidence must be a valid PDF or JPEG file.", "INVALID_EVIDENCE_TYPE");
  }

  private static String sha256(byte[] bytes) {
    try {
      return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
    } catch (NoSuchAlgorithmException ex) {
      throw new IllegalStateException("SHA-256 is unavailable", ex);
    }
  }

  public record StoredFile(String reference, String filename, String sha256) {}
}
