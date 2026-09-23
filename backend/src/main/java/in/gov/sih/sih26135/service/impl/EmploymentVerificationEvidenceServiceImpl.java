package in.gov.sih.sih26135.service.impl;

import in.gov.sih.sih26135.dto.request.CreateEmploymentVerificationEvidenceRequest;
import in.gov.sih.sih26135.dto.request.UpdateEmploymentVerificationEvidenceRequest;
import in.gov.sih.sih26135.dto.response.EmploymentVerificationEvidenceResponse;
import in.gov.sih.sih26135.entity.EmploymentVerification;
import in.gov.sih.sih26135.entity.EmploymentVerificationAttempt;
import in.gov.sih.sih26135.entity.EmploymentVerificationEvidence;
import in.gov.sih.sih26135.entity.RefEmploymentVerificationEvidenceType;
import in.gov.sih.sih26135.exception.BadRequestException;
import in.gov.sih.sih26135.exception.ConflictException;
import in.gov.sih.sih26135.exception.ResourceNotFoundException;
import in.gov.sih.sih26135.mapper.EmploymentVerificationEvidenceMapper;
import in.gov.sih.sih26135.repository.EmploymentVerificationAttemptRepository;
import in.gov.sih.sih26135.repository.EmploymentVerificationEvidenceRepository;
import in.gov.sih.sih26135.repository.EmploymentVerificationRepository;
import in.gov.sih.sih26135.repository.RefEmploymentVerificationEvidenceTypeRepository;
import in.gov.sih.sih26135.repository.UserRepository;
import in.gov.sih.sih26135.service.EmploymentVerificationEvidenceService;
import java.time.LocalDateTime;
import java.util.List;
import java.util.regex.Pattern;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class EmploymentVerificationEvidenceServiceImpl implements EmploymentVerificationEvidenceService {

  private static final Pattern SHA256_PATTERN = Pattern.compile("^[0-9a-f]{64}$");

  private final EmploymentVerificationEvidenceRepository employmentVerificationEvidenceRepository;
  private final EmploymentVerificationRepository employmentVerificationRepository;
  private final EmploymentVerificationAttemptRepository employmentVerificationAttemptRepository;
  private final RefEmploymentVerificationEvidenceTypeRepository refEmploymentVerificationEvidenceTypeRepository;
  private final UserRepository userRepository;
  private final EmploymentVerificationEvidenceMapper employmentVerificationEvidenceMapper;

  public EmploymentVerificationEvidenceServiceImpl(
      EmploymentVerificationEvidenceRepository employmentVerificationEvidenceRepository,
      EmploymentVerificationRepository employmentVerificationRepository,
      EmploymentVerificationAttemptRepository employmentVerificationAttemptRepository,
      RefEmploymentVerificationEvidenceTypeRepository refEmploymentVerificationEvidenceTypeRepository,
      UserRepository userRepository,
      EmploymentVerificationEvidenceMapper employmentVerificationEvidenceMapper) {
    this.employmentVerificationEvidenceRepository = employmentVerificationEvidenceRepository;
    this.employmentVerificationRepository = employmentVerificationRepository;
    this.employmentVerificationAttemptRepository = employmentVerificationAttemptRepository;
    this.refEmploymentVerificationEvidenceTypeRepository = refEmploymentVerificationEvidenceTypeRepository;
    this.userRepository = userRepository;
    this.employmentVerificationEvidenceMapper = employmentVerificationEvidenceMapper;
  }

  @Override
  @Transactional
  public EmploymentVerificationEvidenceResponse createVerificationEvidence(
      CreateEmploymentVerificationEvidenceRequest request) {
    if (request == null) {
      throw new BadRequestException("Request body cannot be null");
    }
    if (request.getEmploymentVerificationId() == null) {
      throw new BadRequestException("Employment verification ID is required");
    }
    EmploymentVerification verification = employmentVerificationRepository.findById(request.getEmploymentVerificationId())
        .orElseThrow(() -> new ResourceNotFoundException("EmploymentVerification", "employmentVerificationId"));

    if (request.getEmploymentVerificationEvidenceTypeId() == null) {
      throw new BadRequestException("Evidence type ID is required");
    }
    RefEmploymentVerificationEvidenceType evidenceType = refEmploymentVerificationEvidenceTypeRepository
        .findById(request.getEmploymentVerificationEvidenceTypeId())
        .orElseThrow(() -> new ResourceNotFoundException("RefEmploymentVerificationEvidenceType", "employmentVerificationEvidenceTypeId"));

    if (request.getDocumentReferenceCode() == null || request.getDocumentReferenceCode().isBlank()) {
      throw new BadRequestException("Document reference code is required");
    }
    String docRef = request.getDocumentReferenceCode().trim();
    if (employmentVerificationEvidenceRepository.existsByEmploymentVerificationIdAndDocumentReferenceCode(
        verification.getId(), docRef)) {
      throw new ConflictException("Document reference code already exists for this verification", "DUPLICATE_DOCUMENT_REFERENCE");
    }

    String sha256 = null;
    if (request.getContentSha256() != null && !request.getContentSha256().isBlank()) {
      sha256 = request.getContentSha256().trim().toLowerCase();
      if (!SHA256_PATTERN.matcher(sha256).matches()) {
        throw new BadRequestException("Invalid content SHA-256 hash. Must be 64 hexadecimal characters", "INVALID_SHA256");
      }
    }

    EmploymentVerificationAttempt attempt = null;
    if (request.getEmploymentVerificationAttemptId() != null) {
      attempt = employmentVerificationAttemptRepository.findById(request.getEmploymentVerificationAttemptId())
          .orElseThrow(() -> new ResourceNotFoundException("EmploymentVerificationAttempt", "employmentVerificationAttemptId"));
      if (verification.getEmploymentVerificationRequest() != null && attempt.getEmploymentVerificationRequest() != null) {
        if (!verification.getEmploymentVerificationRequest().getId().equals(attempt.getEmploymentVerificationRequest().getId())) {
          throw new BadRequestException("Attempt does not belong to the same verification request as verification", "ATTEMPT_REQUEST_MISMATCH");
        }
      }
    }

    if (request.getUploadedByUserId() != null && !userRepository.existsById(request.getUploadedByUserId())) {
      throw new ResourceNotFoundException("User", "uploadedByUserId");
    }

    LocalDateTime capturedAt = request.getCapturedAt() != null ? request.getCapturedAt() : LocalDateTime.now();

    EmploymentVerificationEvidence entity = new EmploymentVerificationEvidence();
    entity.setEmploymentVerification(verification);
    entity.setEmploymentVerificationAttempt(attempt);
    entity.setEmploymentVerificationEvidenceType(evidenceType);
    entity.setDocumentReferenceCode(docRef);
    entity.setContentSha256(sha256);
    entity.setOriginalFilename(request.getOriginalFilename());
    entity.setCapturedAt(capturedAt);
    entity.setUploadedByUserId(request.getUploadedByUserId());
    entity.setNotes(request.getNotes());

    EmploymentVerificationEvidence saved = employmentVerificationEvidenceRepository.save(entity);
    return employmentVerificationEvidenceMapper.toResponse(saved);
  }

  @Override
  @Transactional
  public EmploymentVerificationEvidenceResponse updateVerificationEvidence(
      Long id, UpdateEmploymentVerificationEvidenceRequest request) {
    if (id == null) {
      throw new BadRequestException("Verification evidence ID is required");
    }
    if (request == null) {
      throw new BadRequestException("Request body cannot be null");
    }

    EmploymentVerificationEvidence entity = employmentVerificationEvidenceRepository.findById(id)
        .orElseThrow(() -> new ResourceNotFoundException("EmploymentVerificationEvidence", "id"));

    if (request.getDocumentReferenceCode() != null && !request.getDocumentReferenceCode().isBlank()) {
      String newCode = request.getDocumentReferenceCode().trim();
      if (!newCode.equals(entity.getDocumentReferenceCode())) {
        if (employmentVerificationEvidenceRepository.existsByEmploymentVerificationIdAndDocumentReferenceCode(
            entity.getEmploymentVerification().getId(), newCode)) {
          throw new ConflictException("Document reference code already exists for this verification", "DUPLICATE_DOCUMENT_REFERENCE");
        }
        entity.setDocumentReferenceCode(newCode);
      }
    }

    if (request.getEmploymentVerificationEvidenceTypeId() != null) {
      RefEmploymentVerificationEvidenceType type = refEmploymentVerificationEvidenceTypeRepository
          .findById(request.getEmploymentVerificationEvidenceTypeId())
          .orElseThrow(() -> new ResourceNotFoundException("RefEmploymentVerificationEvidenceType", "employmentVerificationEvidenceTypeId"));
      entity.setEmploymentVerificationEvidenceType(type);
    }

    if (request.getEmploymentVerificationAttemptId() != null) {
      EmploymentVerificationAttempt attempt = employmentVerificationAttemptRepository
          .findById(request.getEmploymentVerificationAttemptId())
          .orElseThrow(() -> new ResourceNotFoundException("EmploymentVerificationAttempt", "employmentVerificationAttemptId"));
      if (entity.getEmploymentVerification().getEmploymentVerificationRequest() != null
          && attempt.getEmploymentVerificationRequest() != null) {
        if (!entity.getEmploymentVerification().getEmploymentVerificationRequest().getId()
            .equals(attempt.getEmploymentVerificationRequest().getId())) {
          throw new BadRequestException("Attempt does not belong to the same verification request as verification", "ATTEMPT_REQUEST_MISMATCH");
        }
      }
      entity.setEmploymentVerificationAttempt(attempt);
    }

    if (request.getContentSha256() != null) {
      String sha256 = request.getContentSha256().trim().toLowerCase();
      if (!sha256.isBlank()) {
        if (!SHA256_PATTERN.matcher(sha256).matches()) {
          throw new BadRequestException("Invalid content SHA-256 hash. Must be 64 hexadecimal characters", "INVALID_SHA256");
        }
        entity.setContentSha256(sha256);
      } else {
        entity.setContentSha256(null);
      }
    }

    if (request.getOriginalFilename() != null) {
      entity.setOriginalFilename(request.getOriginalFilename());
    }

    if (request.getCapturedAt() != null) {
      entity.setCapturedAt(request.getCapturedAt());
    }

    if (request.getUploadedByUserId() != null) {
      if (!userRepository.existsById(request.getUploadedByUserId())) {
        throw new ResourceNotFoundException("User", "uploadedByUserId");
      }
      entity.setUploadedByUserId(request.getUploadedByUserId());
    }

    if (request.getNotes() != null) {
      entity.setNotes(request.getNotes());
    }

    EmploymentVerificationEvidence saved = employmentVerificationEvidenceRepository.save(entity);
    return employmentVerificationEvidenceMapper.toResponse(saved);
  }

  @Override
  public EmploymentVerificationEvidenceResponse getVerificationEvidenceById(Long id) {
    if (id == null) {
      throw new BadRequestException("Verification evidence ID is required");
    }
    EmploymentVerificationEvidence entity = employmentVerificationEvidenceRepository.findById(id)
        .orElseThrow(() -> new ResourceNotFoundException("EmploymentVerificationEvidence", "id"));
    return employmentVerificationEvidenceMapper.toResponse(entity);
  }

  @Override
  public List<EmploymentVerificationEvidenceResponse> getEvidenceByVerificationId(Long verificationId) {
    if (verificationId == null) {
      throw new BadRequestException("Verification ID is required");
    }
    return employmentVerificationEvidenceRepository.findByEmploymentVerificationId(verificationId).stream()
        .map(employmentVerificationEvidenceMapper::toResponse)
        .toList();
  }

  @Override
  public List<EmploymentVerificationEvidenceResponse> getEvidenceByAttemptId(Long attemptId) {
    if (attemptId == null) {
      throw new BadRequestException("Attempt ID is required");
    }
    return employmentVerificationEvidenceRepository.findByEmploymentVerificationAttemptId(attemptId).stream()
        .map(employmentVerificationEvidenceMapper::toResponse)
        .toList();
  }

  @Override
  public List<EmploymentVerificationEvidenceResponse> getEvidenceByTypeId(Long typeId) {
    if (typeId == null) {
      throw new BadRequestException("Evidence type ID is required");
    }
    return employmentVerificationEvidenceRepository.findByEmploymentVerificationEvidenceTypeId(typeId).stream()
        .map(employmentVerificationEvidenceMapper::toResponse)
        .toList();
  }

  @Override
  @Transactional
  public void deleteVerificationEvidence(Long id) {
    if (id == null) {
      throw new BadRequestException("Verification evidence ID is required");
    }
    EmploymentVerificationEvidence entity = employmentVerificationEvidenceRepository.findById(id)
        .orElseThrow(() -> new ResourceNotFoundException("EmploymentVerificationEvidence", "id"));
    employmentVerificationEvidenceRepository.delete(entity);
  }
}
