package in.gov.sih.sih26135.service.ingestion;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import in.gov.sih.sih26135.config.ImportPipelineProperties;
import in.gov.sih.sih26135.config.ImportPipelineProperties.DeploymentMode;
import in.gov.sih.sih26135.entity.ImportBatch;
import in.gov.sih.sih26135.entity.RefImportBatchStatus;
import in.gov.sih.sih26135.entity.RefImportRecordStatus;
import in.gov.sih.sih26135.entity.User;
import in.gov.sih.sih26135.exception.BadRequestException;
import in.gov.sih.sih26135.exception.ConflictException;
import in.gov.sih.sih26135.mapper.ImportBatchMapper;
import in.gov.sih.sih26135.mapper.ImportRecordMapper;
import in.gov.sih.sih26135.repository.ImportBatchRepository;
import in.gov.sih.sih26135.repository.ImportRecordRepository;
import in.gov.sih.sih26135.repository.RefImportBatchStatusRepository;
import in.gov.sih.sih26135.repository.RefImportRecordStatusRepository;
import in.gov.sih.sih26135.service.impl.ImportBatchProcessingWorkflowImpl;
import in.gov.sih.sih26135.service.ingestion.ImportErrorSanitizer.DbFailureClassification;
import in.gov.sih.sih26135.service.ingestion.ImportFileStagingService.FileFormat;
import in.gov.sih.sih26135.service.ingestion.ImportFileStagingService.StagedFileResult;
import in.gov.sih.sih26135.service.ingestion.ImportSecurityScopeValidator.CallerScope;
import in.gov.sih.sih26135.service.ingestion.ParsedImportRow.RowKind;
import in.gov.sih.sih26135.service.ingestion.StreamingImportParser.ImportStreamParseException;
import jakarta.persistence.EntityManager;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.SimpleTransactionStatus;
import org.springframework.web.multipart.MultipartFile;

@DisplayName("Phase 28.5 - Streaming CSV/JSON Parsers, Staging, and Sanitization Unit Tests")
class ImportPipelineStreamingAndValidationUnitTest {

  private static final int DEFAULT_MAX_ROW_BYTES = 65536;

  @Nested
  @DisplayName("Streaming CSV Parser Tests")
  class CsvParserTests {

    @Test
    @DisplayName("Parses RFC 4180 CSV with UTF-8 BOM, quoted commas, newlines, escaped quotes, blank rows, and malformed rows")
    void parsesComplexRfc4180FeaturesAndPreservesRowNumbers() throws Exception {
      String isolatedCsv = "\uFEFFregistration_number,first_name,gender_id,state_id,district_id,current_employment_status_id,profile_status_id,address_line1\r\n"
          + "REG-001,\"Kumar, \"\"Asha\"\"\",1,1,1,1,1,\"House 12\nSector 4\"\r\n"
          + "\r\n"
          + "REG-002,OnlyTwoCols\r\n"
          + "REG-003,Bad\"QuoteInUnquoted,1,1,1,1,1,Addr\r\n"
          + "REG-004,ValidName,1,1,1,1,1,CleanAddr\r\n";

      List<ParsedImportRow> rows = new ArrayList<>();
      try (StreamingCsvRecordParser parser = new StreamingCsvRecordParser(
          new ByteArrayInputStream(isolatedCsv.getBytes(StandardCharsets.UTF_8)),
          ImportContractSchema.ENTITY_TRAINEE,
          DEFAULT_MAX_ROW_BYTES)) {
        ParsedImportRow row;
        while ((row = parser.nextRow()) != null) {
          rows.add(row);
        }
      }

      assertThat(rows).hasSize(5);

      ParsedImportRow r1 = rows.get(0);
      assertThat(r1.sourceRowNumber()).isEqualTo(1);
      assertThat(r1.rowKind()).isEqualTo(RowKind.DATA);
      assertThat(r1.fields().get("firstName")).isEqualTo("Kumar, \"Asha\"");
      assertThat(r1.fields().get("addressLine1")).isEqualTo("House 12\nSector 4");

      ParsedImportRow r2 = rows.get(1);
      assertThat(r2.sourceRowNumber()).isEqualTo(2);
      assertThat(r2.rowKind()).isEqualTo(RowKind.SKIPPED);
      assertThat(r2.errorCode()).isEqualTo("EMPTY_ROW");

      ParsedImportRow r3 = rows.get(2);
      assertThat(r3.sourceRowNumber()).isEqualTo(3);
      assertThat(r3.rowKind()).isEqualTo(RowKind.MALFORMED);
      assertThat(r3.errorCode()).isEqualTo("MALFORMED_CSV_ROW");

      ParsedImportRow r4 = rows.get(3);
      assertThat(r4.sourceRowNumber()).isEqualTo(4);
      assertThat(r4.rowKind()).isEqualTo(RowKind.MALFORMED);
      assertThat(r4.errorCode()).isEqualTo("MALFORMED_CSV_ROW");

      ParsedImportRow r5 = rows.get(4);
      assertThat(r5.sourceRowNumber()).isEqualTo(5);
      assertThat(r5.rowKind()).isEqualTo(RowKind.DATA);
      assertThat(r5.fields().get("registrationNumber")).isEqualTo("REG-004");
      assertThat(r5.fields().get("firstName")).isEqualTo("ValidName");
    }

    @Test
    @DisplayName("Streams 2,500+ CSV rows in bounded pull fashion")
    void streamsLargeCsvIncrementally() throws Exception {
      StringBuilder sb = new StringBuilder(
          "registrationNumber,firstName,genderId,stateId,districtId,currentEmploymentStatusId,profileStatusId\n");
      for (int i = 1; i <= 2500; i++) {
        sb.append("REG-BULK-").append(i).append(",Trainee").append(i).append(",1,1,1,1,1\n");
      }

      int count = 0;
      String lastReg = null;
      try (StreamingCsvRecordParser parser = new StreamingCsvRecordParser(
          new ByteArrayInputStream(sb.toString().getBytes(StandardCharsets.UTF_8)),
          ImportContractSchema.ENTITY_TRAINEE,
          DEFAULT_MAX_ROW_BYTES)) {
        ParsedImportRow row;
        while ((row = parser.nextRow()) != null) {
          assertThat(row.rowKind()).isEqualTo(RowKind.DATA);
          count++;
          assertThat(row.sourceRowNumber()).isEqualTo(count);
          lastReg = row.fields().get("registrationNumber");
        }
      }

      assertThat(count).isEqualTo(2500);
      assertThat(lastReg).isEqualTo("REG-BULK-2500");
    }

    @Test
    @DisplayName("Rejects missing required headers, duplicate headers, unsupported headers, and oversized CSV rows")
    void rejectsInvalidHeadersAndOversizedRows() {
      String missingReqHeader = "registrationNumber,firstName\nREG-1,Asha\n";
      assertThatThrownBy(() -> new StreamingCsvRecordParser(
          new ByteArrayInputStream(missingReqHeader.getBytes(StandardCharsets.UTF_8)),
          ImportContractSchema.ENTITY_TRAINEE,
          DEFAULT_MAX_ROW_BYTES))
          .isInstanceOf(ImportStreamParseException.class)
          .satisfies(ex -> assertThat(((ImportStreamParseException) ex).getErrorCode()).isEqualTo("INVALID_CSV_HEADER"));

      String unsupportedHeader = "registrationNumber,firstName,genderId,stateId,districtId,currentEmploymentStatusId,profileStatusId,hackField\n";
      assertThatThrownBy(() -> new StreamingCsvRecordParser(
          new ByteArrayInputStream(unsupportedHeader.getBytes(StandardCharsets.UTF_8)),
          ImportContractSchema.ENTITY_TRAINEE,
          DEFAULT_MAX_ROW_BYTES))
          .isInstanceOf(ImportStreamParseException.class)
          .satisfies(ex -> assertThat(((ImportStreamParseException) ex).getErrorCode()).isEqualTo("INVALID_CSV_HEADER"));

      String duplicateHeader = "registrationNumber,registration_number,firstName,genderId,stateId,districtId,currentEmploymentStatusId,profileStatusId\n";
      assertThatThrownBy(() -> new StreamingCsvRecordParser(
          new ByteArrayInputStream(duplicateHeader.getBytes(StandardCharsets.UTF_8)),
          ImportContractSchema.ENTITY_TRAINEE,
          DEFAULT_MAX_ROW_BYTES))
          .isInstanceOf(ImportStreamParseException.class)
          .satisfies(ex -> assertThat(((ImportStreamParseException) ex).getErrorCode()).isEqualTo("INVALID_CSV_HEADER"));

      String bigRowCsv = "registrationNumber,firstName,genderId,stateId,districtId,currentEmploymentStatusId,profileStatusId,addressLine1\n"
          + "REG-1,Asha,1,1,1,1,1," + "X".repeat(1500) + "\n";
      assertThatThrownBy(() -> {
        try (StreamingCsvRecordParser parser = new StreamingCsvRecordParser(
            new ByteArrayInputStream(bigRowCsv.getBytes(StandardCharsets.UTF_8)),
            ImportContractSchema.ENTITY_TRAINEE,
            1024)) {
          parser.nextRow();
        }
      })
          .isInstanceOf(ImportStreamParseException.class)
          .satisfies(ex -> assertThat(((ImportStreamParseException) ex).getErrorCode()).isEqualTo("ROW_TOO_LARGE"));
    }
  }

  @Nested
  @DisplayName("Streaming JSON & NDJSON Parser Tests")
  class JsonParserTests {

    @Test
    @DisplayName("Streams JSON array handling null (SKIPPED), scalar (MALFORMED), unsupported field, and 2,500 records")
    void parsesJsonArrayStreamAndLargePayload() throws Exception {
      String mixedArray = """
          [
            {"registrationNumber":"REG-J1","firstName":"Meera","genderId":1,"stateId":1,"districtId":1,"currentEmploymentStatusId":1,"profileStatusId":1},
            null,
            "not-an-object",
            {"registrationNumber":"REG-J2","unknownField":"bad","firstName":"Ravi","genderId":1,"stateId":1,"districtId":1,"currentEmploymentStatusId":1,"profileStatusId":1}
          ]
          """;

      List<ParsedImportRow> parsed = new ArrayList<>();
      try (StreamingJsonRecordParser parser = new StreamingJsonRecordParser(
          new ByteArrayInputStream(mixedArray.getBytes(StandardCharsets.UTF_8)),
          ImportContractSchema.ENTITY_TRAINEE,
          DEFAULT_MAX_ROW_BYTES)) {
        ParsedImportRow row;
        while ((row = parser.nextRow()) != null) {
          parsed.add(row);
        }
      }

      assertThat(parsed).hasSize(4);
      assertThat(parsed.get(0).rowKind()).isEqualTo(RowKind.DATA);
      assertThat(parsed.get(0).fields().get("firstName")).isEqualTo("Meera");
      assertThat(parsed.get(1).rowKind()).isEqualTo(RowKind.SKIPPED);
      assertThat(parsed.get(1).errorCode()).isEqualTo("EMPTY_ROW");
      assertThat(parsed.get(2).rowKind()).isEqualTo(RowKind.MALFORMED);
      assertThat(parsed.get(2).errorCode()).isEqualTo("MALFORMED_JSON_ROW");
      assertThat(parsed.get(3).rowKind()).isEqualTo(RowKind.MALFORMED);
      assertThat(parsed.get(3).errorCode()).isEqualTo("UNSUPPORTED_FIELD");

      StringBuilder largeJson = new StringBuilder("[");
      for (int i = 1; i <= 2500; i++) {
        if (i > 1) {
          largeJson.append(',');
        }
        largeJson.append("{\"placementNumber\":\"PLC-J-").append(i)
            .append("\",\"enrollmentId\":").append(i)
            .append(",\"placementSourceId\":1,\"placementStatusId\":1,\"joiningStatusId\":1,\"recordVerificationStatusId\":1}");
      }
      largeJson.append(']');

      int largeCount = 0;
      try (StreamingJsonRecordParser parser = new StreamingJsonRecordParser(
          new ByteArrayInputStream(largeJson.toString().getBytes(StandardCharsets.UTF_8)),
          ImportContractSchema.ENTITY_PLACEMENT_RECORD,
          DEFAULT_MAX_ROW_BYTES)) {
        ParsedImportRow row;
        while ((row = parser.nextRow()) != null) {
          assertThat(row.rowKind()).isEqualTo(RowKind.DATA);
          largeCount++;
        }
      }
      assertThat(largeCount).isEqualTo(2500);
    }

    @Test
    @DisplayName("Streams NDJSON and aborts cleanly on mid-stream corrupted JSON")
    void parsesNdjsonAndRejectsMidStreamCorruption() throws Exception {
      String ndjson = "{\"registrationNumber\":\"N-1\",\"firstName\":\"A\",\"genderId\":1,\"stateId\":1,\"districtId\":1,\"currentEmploymentStatusId\":1,\"profileStatusId\":1}\n"
          + "{\"registrationNumber\":\"N-2\",\"firstName\":\"B\",\"genderId\":1,\"stateId\":1,\"districtId\":1,\"currentEmploymentStatusId\":1,\"profileStatusId\":1}\n";

      List<ParsedImportRow> okRows = new ArrayList<>();
      try (StreamingJsonRecordParser parser = new StreamingJsonRecordParser(
          new ByteArrayInputStream(ndjson.getBytes(StandardCharsets.UTF_8)),
          ImportContractSchema.ENTITY_TRAINEE,
          DEFAULT_MAX_ROW_BYTES)) {
        ParsedImportRow row;
        while ((row = parser.nextRow()) != null) {
          okRows.add(row);
        }
      }
      assertThat(okRows).hasSize(2);
      assertThat(okRows.get(1).fields().get("registrationNumber")).isEqualTo("N-2");

      String corruptedJson = "[{\"registrationNumber\":\"N-1\"}, {\"registrationNumber\": ";
      try (StreamingJsonRecordParser parser = new StreamingJsonRecordParser(
          new ByteArrayInputStream(corruptedJson.getBytes(StandardCharsets.UTF_8)),
          ImportContractSchema.ENTITY_TRAINEE,
          DEFAULT_MAX_ROW_BYTES)) {
        ParsedImportRow first = parser.nextRow();
        assertThat(first.rowKind()).isEqualTo(RowKind.DATA);
        assertThatThrownBy(parser::nextRow)
            .isInstanceOf(ImportStreamParseException.class)
            .satisfies(ex -> assertThat(((ImportStreamParseException) ex).getErrorCode()).isEqualTo("MALFORMED_JSON_FILE"));
      }
    }
  }

  @Nested
  @DisplayName("File Staging & Error Sanitization Tests")
  class StagingAndSanitizationTests {

    @Test
    @DisplayName("Stages files durably, rejects empty/unsupported/oversized uploads and traversal batch codes")
    void enforcesStagingGuardrails(@TempDir Path tempDir) {
      ImportPipelineProperties props = new ImportPipelineProperties();
      props.setStagingDirectory(tempDir.toString());
      props.setMaxFileSizeBytes(128);

      ImportBatchRepository batchRepo = mock(ImportBatchRepository.class);
      when(batchRepo.existsByBatchCode(anyString())).thenReturn(false);

      ImportFileStagingService staging = new ImportFileStagingService(props, batchRepo);

      MockMultipartFile emptyFile = new MockMultipartFile("file", "trainees.csv", "text/csv", new byte[0]);
      assertThatThrownBy(() -> staging.detectAndValidateFormat(emptyFile))
          .isInstanceOf(BadRequestException.class)
          .satisfies(ex -> assertThat(((BadRequestException) ex).getErrorCode()).isEqualTo("EMPTY_IMPORT_FILE"));

      MockMultipartFile exeFile = new MockMultipartFile("file", "payload.exe", "application/octet-stream", "MZ".getBytes(StandardCharsets.UTF_8));
      assertThatThrownBy(() -> staging.detectAndValidateFormat(exeFile))
          .isInstanceOf(BadRequestException.class)
          .satisfies(ex -> assertThat(((BadRequestException) ex).getErrorCode()).isEqualTo("UNSUPPORTED_FILE_FORMAT"));

      MockMultipartFile hugeFile = new MockMultipartFile("file", "big.csv", "text/csv", "A".repeat(256).getBytes(StandardCharsets.UTF_8));
      assertThatThrownBy(() -> staging.detectAndValidateFormat(hugeFile))
          .isInstanceOf(BadRequestException.class)
          .satisfies(ex -> assertThat(((BadRequestException) ex).getErrorCode()).isEqualTo("FILE_SIZE_LIMIT_EXCEEDED"));

      assertThatThrownBy(() -> staging.stageFile(
          new MockMultipartFile("file", "ok.csv", "text/csv", "a,b\n1,2\n".getBytes(StandardCharsets.UTF_8)),
          "../escape",
          FileFormat.CSV))
          .isInstanceOf(BadRequestException.class)
          .satisfies(ex -> assertThat(((BadRequestException) ex).getErrorCode()).isEqualTo("INVALID_BATCH_CODE"));

      String batchCode = staging.generateUniqueBatchCode();
      MockMultipartFile validFile = new MockMultipartFile(
          "file", "../../ignored/path/trainees.csv", "text/csv",
          "registrationNumber,firstName\nR1,Asha\n".getBytes(StandardCharsets.UTF_8));
      assertThat(staging.detectAndValidateFormat(validFile)).isEqualTo(FileFormat.CSV);

      StagedFileResult result = staging.stageFile(validFile, batchCode, FileFormat.CSV);
      assertThat(result.stagedPath().getParent()).isEqualTo(tempDir.toAbsolutePath().normalize());
      assertThat(staging.hasStagedFile(batchCode)).isTrue();
      staging.deleteStagedFileQuietly(batchCode);
      assertThat(staging.hasStagedFile(batchCode)).isFalse();
    }

    @Test
    @DisplayName("Sanitizes error messages, bounds lengths, and classifies DB exceptions without SQL/PII leakage")
    void sanitizesErrorsAndClassifiesDbFailures() {
      String longMsg = "Line1\r\nLine2\t\t" + "Z".repeat(600);
      String sanitized = ImportErrorSanitizer.sanitizeErrorMessage(longMsg, "fallback");
      assertThat(sanitized).doesNotContain("\r", "\n", "\t");
      assertThat(sanitized.length()).isEqualTo(ImportErrorSanitizer.MAX_ERROR_MESSAGE_LENGTH);

      String code = ImportErrorSanitizer.sanitizeErrorCode("BAD/CODE WITH SPACES AND LONG SUFFIX_" + "X".repeat(80), "ERR");
      assertThat(code.length()).isEqualTo(ImportErrorSanitizer.MAX_ERROR_CODE_LENGTH);
      assertThat(code).matches("^[A-Za-z0-9_.-]+$");

      DbFailureClassification dupReg = ImportErrorSanitizer.classifyDatabaseFailure(
          new RuntimeException("Duplicate entry 'SECRET-REG-999' for key 'trainees.uk_trainees_registration_number'"));
      assertThat(dupReg.duplicate()).isTrue();
      assertThat(dupReg.errorCode()).isEqualTo("DUPLICATE_TRAINEE_REGISTRATION");
      assertThat(dupReg.errorMessage()).doesNotContain("SECRET-REG-999", "INSERT", "SELECT");

      DbFailureClassification fkErr = ImportErrorSanitizer.classifyDatabaseFailure(
          new RuntimeException("Cannot add or update a child row: a foreign key constraint fails (`SIH26135_TEST`.`trainees`...)"));
      assertThat(fkErr.duplicate()).isFalse();
      assertThat(fkErr.errorCode()).isEqualTo("FOREIGN_KEY_NOT_FOUND");
      assertThat(fkErr.errorMessage()).doesNotContain("SIH26135_TEST", "child row");
    }

    @Test
    @DisplayName("Deletes staged temp/final files when mid-stream IO fails or DB metadata persistence throws")
    void rollsBackStagedFilesOnIoOrPersistenceFailure(@TempDir Path tempDir) throws Exception {
      ImportPipelineProperties props = new ImportPipelineProperties();
      props.setStagingDirectory(tempDir.toString());

      ImportBatchRepository batchRepo = mock(ImportBatchRepository.class);
      when(batchRepo.existsByBatchCode(anyString())).thenReturn(false);
      ImportFileStagingService staging = new ImportFileStagingService(props, batchRepo);

      // 1. Mid-stream IOException during staging deletes .part file
      MultipartFile brokenStreamFile = mock(MultipartFile.class);
      when(brokenStreamFile.getInputStream()).thenReturn(new InputStream() {
        private int count = 0;

        @Override
        public int read() throws IOException {
          if (count++ >= 10) {
            throw new IOException("Simulated broken stream");
          }
          return 'a';
        }
      });

      assertThatThrownBy(() -> staging.stageFile(brokenStreamFile, "IMP-FAILTEST-001", FileFormat.CSV))
          .isInstanceOf(IllegalStateException.class);
      try (var stream = Files.list(tempDir)) {
        assertThat(stream.count()).isZero();
      }

      // 2. DB failure during stageUpload cleans up committed staging file
      ImportSecurityScopeValidator scopeValidator = mock(ImportSecurityScopeValidator.class);
      User caller = new User();
      caller.setId(99L);
      CallerScope scope = new CallerScope(caller, true, null, null, null, null, null, Set.of("ROLE_super_admin"));
      when(scopeValidator.normalizeImportEntityType("Trainee")).thenReturn("Trainee");
      when(scopeValidator.resolveAndValidateUploadOrProcessCaller("Trainee")).thenReturn(scope);

      RefImportBatchStatusRepository statusRepo = mock(RefImportBatchStatusRepository.class);
      RefImportBatchStatus pending = new RefImportBatchStatus();
      pending.setId(1L);
      pending.setStatusCode("PENDING");
      when(statusRepo.findByStatusCode("PENDING")).thenReturn(Optional.of(pending));
      when(batchRepo.saveAndFlush(org.mockito.ArgumentMatchers.any()))
          .thenThrow(new DataIntegrityViolationException("Simulated DB save failure"));

      PlatformTransactionManager txManager = mock(PlatformTransactionManager.class);
      when(txManager.getTransaction(org.mockito.ArgumentMatchers.any())).thenReturn(new SimpleTransactionStatus());

      ImportBatchProcessingWorkflowImpl workflow = new ImportBatchProcessingWorkflowImpl(
          props,
          staging,
          scopeValidator,
          mock(TraineeImportRowProcessor.class),
          mock(PlacementImportRowProcessor.class),
          batchRepo,
          mock(ImportRecordRepository.class),
          statusRepo,
          mock(RefImportRecordStatusRepository.class),
          mock(ImportBatchMapper.class),
          mock(ImportRecordMapper.class),
          mock(JdbcTemplate.class),
          txManager
      );

      MockMultipartFile validCsv = new MockMultipartFile(
          "file", "trainees.csv", "text/csv",
          "registrationNumber,firstName,genderId,stateId,districtId,currentEmploymentStatusId,profileStatusId\nR1,Asha,1,1,1,1,1\n"
              .getBytes(StandardCharsets.UTF_8));

      assertThatThrownBy(() -> workflow.stageUpload(validCsv, "Trainee", "UNIT_TEST"))
          .isInstanceOf(DataIntegrityViolationException.class);

      try (var stream = Files.list(tempDir)) {
        assertThat(stream.count()).isZero();
      }
    }

    @Test
    @DisplayName("Blocks recovery while a worker thread is deterministically active on the target batch")
    void blocksRecoveryWhileActiveWorkerExecutesDeterministically(@TempDir Path tempDir) throws Exception {
      ImportPipelineProperties props = new ImportPipelineProperties();
      props.setStagingDirectory(tempDir.toString());

      ImportBatchRepository batchRepo = mock(ImportBatchRepository.class);
      when(batchRepo.existsByBatchCode(anyString())).thenReturn(false);
      ImportFileStagingService staging = new ImportFileStagingService(props, batchRepo);
      MockMultipartFile validCsv = new MockMultipartFile(
          "file", "trainees.csv", "text/csv",
          "registrationNumber,firstName,genderId,stateId,districtId,currentEmploymentStatusId,profileStatusId\nR1,Asha,1,1,1,1,1\n"
              .getBytes(StandardCharsets.UTF_8));
      String batchCode = "IMP-20261112-ACTIVEWORKER01";
      staging.stageFile(validCsv, batchCode, FileFormat.CSV);

      RefImportBatchStatus pending = new RefImportBatchStatus();
      pending.setId(1L);
      pending.setStatusCode("PENDING");
      pending.setIsTerminalFlag(false);
      RefImportBatchStatus running = new RefImportBatchStatus();
      running.setId(2L);
      running.setStatusCode("RUNNING");
      running.setIsTerminalFlag(false);
      RefImportBatchStatus completed = new RefImportBatchStatus();
      completed.setId(3L);
      completed.setStatusCode("COMPLETED");
      completed.setIsTerminalFlag(true);
      RefImportBatchStatus failed = new RefImportBatchStatus();
      failed.setId(4L);
      failed.setStatusCode("FAILED");
      failed.setIsTerminalFlag(true);

      ImportBatch batch = new ImportBatch(batchCode, "UNIT", "Trainee", pending);
      batch.setId(77L);
      batch.setInitiatedByUserId(99L);

      when(batchRepo.findById(77L)).thenReturn(Optional.of(batch));
      when(batchRepo.saveAndFlush(org.mockito.ArgumentMatchers.any())).thenAnswer(inv -> inv.getArgument(0));

      RefImportBatchStatusRepository statusRepo = mock(RefImportBatchStatusRepository.class);
      when(statusRepo.findByStatusCode("PENDING")).thenReturn(Optional.of(pending));
      when(statusRepo.findByStatusCode("RUNNING")).thenReturn(Optional.of(running));
      when(statusRepo.findByStatusCode("COMPLETED")).thenReturn(Optional.of(completed));
      when(statusRepo.findByStatusCode("FAILED")).thenReturn(Optional.of(failed));

      RefImportRecordStatusRepository recStatusRepo = mock(RefImportRecordStatusRepository.class);
      for (long i = 1; i <= 4; i++) {
        RefImportRecordStatus st = new RefImportRecordStatus();
        st.setId(i);
        String code = switch ((int) i) {
          case 1 -> "SUCCESS";
          case 2 -> "FAILED";
          case 3 -> "SKIPPED";
          default -> "DUPLICATE";
        };
        st.setStatusCode(code);
        when(recStatusRepo.findByStatusCode(code)).thenReturn(Optional.of(st));
      }

      ImportSecurityScopeValidator scopeValidator = mock(ImportSecurityScopeValidator.class);
      User caller = new User();
      caller.setId(99L);
      CallerScope scope = new CallerScope(caller, true, null, null, null, null, null, Set.of("ROLE_super_admin", "system.manage"));
      when(scopeValidator.normalizeImportEntityType("Trainee")).thenReturn("Trainee");
      when(scopeValidator.resolveAndValidateGenericWriteCaller()).thenReturn(scope);
      when(scopeValidator.resolveAndValidateUploadOrProcessCaller("Trainee")).thenReturn(scope);

      CountDownLatch workerInsideChunk = new CountDownLatch(1);
      CountDownLatch allowWorkerFinish = new CountDownLatch(1);
      TraineeImportRowProcessor traineeProcessor = mock(TraineeImportRowProcessor.class);
      when(traineeProcessor.validateAndPersist(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any()))
          .thenAnswer(inv -> {
            workerInsideChunk.countDown();
            allowWorkerFinish.await(5, TimeUnit.SECONDS);
            return RowOutcomeResult.success("Trainee", 500L);
          });

      PlatformTransactionManager txManager = mock(PlatformTransactionManager.class);
      when(txManager.getTransaction(org.mockito.ArgumentMatchers.any())).thenReturn(new SimpleTransactionStatus());

      JdbcTemplate jdbcMock = mock(JdbcTemplate.class);
      when(jdbcMock.queryForList(org.mockito.ArgumentMatchers.contains("GROUP BY rirs.status_code"), org.mockito.ArgumentMatchers.eq(77L)))
          .thenReturn(List.of(java.util.Map.of("status_code", "SUCCESS", "cnt", 1L)));

      ImportBatchProcessingWorkflowImpl workflow = new ImportBatchProcessingWorkflowImpl(
          props,
          staging,
          scopeValidator,
          traineeProcessor,
          mock(PlacementImportRowProcessor.class),
          batchRepo,
          mock(ImportRecordRepository.class),
          statusRepo,
          recStatusRepo,
          mock(ImportBatchMapper.class),
          mock(ImportRecordMapper.class),
          jdbcMock,
          txManager
      );
      ReflectionTestUtils.setField(workflow, "entityManager", mock(EntityManager.class));

      var pool = Executors.newSingleThreadExecutor();
      Future<?> workerFuture = pool.submit(() -> workflow.processBatch(77L));
      assertThat(workerInsideChunk.await(5, TimeUnit.SECONDS)).isTrue();

      assertThatThrownBy(() -> workflow.recoverInterruptedBatch(77L, true))
          .isInstanceOf(ConflictException.class)
          .satisfies(ex -> assertThat(((ConflictException) ex).getErrorCode()).isEqualTo("IMPORT_BATCH_ALREADY_RUNNING"));

      allowWorkerFinish.countDown();
      workerFuture.get(5, TimeUnit.SECONDS);
      pool.shutdownNow();
    }

    @Test
    @DisplayName("Fails closed for RUNNING-batch recovery in MULTI_INSTANCE mode even when worker-termination flag is true")
    void blocksRecoveryInMultiInstanceModeEvenWithConfirmation(@TempDir Path tempDir) throws Exception {
      ImportPipelineProperties props = new ImportPipelineProperties();
      props.setStagingDirectory(tempDir.toString());
      props.setDeploymentMode(DeploymentMode.MULTI_INSTANCE);

      ImportBatchRepository batchRepo = mock(ImportBatchRepository.class);
      ImportFileStagingService staging = new ImportFileStagingService(props, batchRepo);

      RefImportBatchStatus running = new RefImportBatchStatus();
      running.setId(2L);
      running.setStatusCode("RUNNING");
      running.setIsTerminalFlag(false);

      ImportBatch batch = new ImportBatch("IMP-20261112-MULTIINST01", "UNIT", "Trainee", running);
      batch.setId(88L);
      batch.setInitiatedByUserId(99L);
      when(batchRepo.findById(88L)).thenReturn(Optional.of(batch));

      ImportSecurityScopeValidator scopeValidator = mock(ImportSecurityScopeValidator.class);
      User caller = new User();
      caller.setId(99L);
      CallerScope scope = new CallerScope(caller, true, null, null, null, null, null, Set.of("ROLE_super_admin", "system.manage"));
      when(scopeValidator.normalizeImportEntityType("Trainee")).thenReturn("Trainee");
      when(scopeValidator.resolveAndValidateGenericWriteCaller()).thenReturn(scope);
      when(scopeValidator.resolveAndValidateUploadOrProcessCaller("Trainee")).thenReturn(scope);

      PlatformTransactionManager txManager = mock(PlatformTransactionManager.class);
      when(txManager.getTransaction(org.mockito.ArgumentMatchers.any())).thenReturn(new SimpleTransactionStatus());

      ImportBatchProcessingWorkflowImpl workflow = new ImportBatchProcessingWorkflowImpl(
          props,
          staging,
          scopeValidator,
          mock(TraineeImportRowProcessor.class),
          mock(PlacementImportRowProcessor.class),
          batchRepo,
          mock(ImportRecordRepository.class),
          mock(RefImportBatchStatusRepository.class),
          mock(RefImportRecordStatusRepository.class),
          mock(ImportBatchMapper.class),
          mock(ImportRecordMapper.class),
          mock(JdbcTemplate.class),
          txManager
      );
      ReflectionTestUtils.setField(workflow, "entityManager", mock(EntityManager.class));

      assertThatThrownBy(() -> workflow.recoverInterruptedBatch(88L, true))
          .isInstanceOf(ConflictException.class)
          .satisfies(ex -> assertThat(((ConflictException) ex).getErrorCode()).isEqualTo("MULTI_INSTANCE_RECOVERY_UNSUPPORTED"));

      verify(batchRepo, never()).saveAndFlush(org.mockito.ArgumentMatchers.any());
    }
  }
}
