package in.gov.sih.sih26135.service.impl;

import in.gov.sih.sih26135.dto.response.EmploymentVerificationFactResponse;
import in.gov.sih.sih26135.entity.analytics.EmploymentVerificationFact;
import in.gov.sih.sih26135.exception.BadRequestException;
import in.gov.sih.sih26135.exception.ResourceNotFoundException;
import in.gov.sih.sih26135.mapper.EmploymentVerificationFactMapper;
import in.gov.sih.sih26135.repository.analytics.EmploymentVerificationFactRepository;
import in.gov.sih.sih26135.response.PagedResponse;
import in.gov.sih.sih26135.service.EmploymentVerificationFactService;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class EmploymentVerificationFactServiceImpl implements EmploymentVerificationFactService {

  private final EmploymentVerificationFactRepository repository;
  private final EmploymentVerificationFactMapper mapper;

  public EmploymentVerificationFactServiceImpl(
      EmploymentVerificationFactRepository repository,
      EmploymentVerificationFactMapper mapper) {
    this.repository = repository;
    this.mapper = mapper;
  }

  @Override
  public PagedResponse<EmploymentVerificationFactResponse> getAllEmploymentVerifications(Pageable pageable) {
    Page<EmploymentVerificationFact> page = repository.findAll(pageable);
    return PagedResponse.from(page, mapper::toResponse);
  }

  @Override
  public List<EmploymentVerificationFactResponse> getAllEmploymentVerifications() {
    return repository.findAll().stream()
        .map(mapper::toResponse)
        .toList();
  }

  @Override
  public EmploymentVerificationFactResponse getEmploymentVerificationById(Long employmentVerificationId) {
    if (employmentVerificationId == null) {
      throw new BadRequestException("Employment verification ID is required");
    }
    EmploymentVerificationFact entity = repository.findById(employmentVerificationId)
        .orElseThrow(() -> new ResourceNotFoundException("EmploymentVerificationFact", "employmentVerificationId"));
    return mapper.toResponse(entity);
  }

  @Override
  public EmploymentVerificationFactResponse getEmploymentVerificationByNumber(String verificationNumber) {
    if (verificationNumber == null || verificationNumber.isBlank()) {
      throw new BadRequestException("Verification number is required");
    }
    EmploymentVerificationFact entity = repository.findByVerificationNumber(verificationNumber)
        .orElseThrow(() -> new ResourceNotFoundException("EmploymentVerificationFact", "verificationNumber"));
    return mapper.toResponse(entity);
  }

  @Override
  public List<EmploymentVerificationFactResponse> getEmploymentVerificationsByTraineeId(Long traineeId) {
    if (traineeId == null) {
      throw new BadRequestException("Trainee ID is required");
    }
    return repository.findByTraineeId(traineeId).stream()
        .map(mapper::toResponse)
        .toList();
  }

  @Override
  public PagedResponse<EmploymentVerificationFactResponse> getEmploymentVerificationsByTraineeId(
      Long traineeId, Pageable pageable) {
    if (traineeId == null) {
      throw new BadRequestException("Trainee ID is required");
    }
    Page<EmploymentVerificationFact> page = repository.findByTraineeId(traineeId, pageable);
    return PagedResponse.from(page, mapper::toResponse);
  }

  @Override
  public List<EmploymentVerificationFactResponse> getEmploymentVerificationsByEmploymentId(Long employmentId) {
    if (employmentId == null) {
      throw new BadRequestException("Employment ID is required");
    }
    return repository.findByEmploymentId(employmentId).stream()
        .map(mapper::toResponse)
        .toList();
  }

  @Override
  public PagedResponse<EmploymentVerificationFactResponse> getEmploymentVerificationsByEmploymentId(
      Long employmentId, Pageable pageable) {
    if (employmentId == null) {
      throw new BadRequestException("Employment ID is required");
    }
    Page<EmploymentVerificationFact> page = repository.findByEmploymentId(employmentId, pageable);
    return PagedResponse.from(page, mapper::toResponse);
  }

  @Override
  public List<EmploymentVerificationFactResponse> getEmploymentVerificationsByPlacementId(Long placementId) {
    if (placementId == null) {
      throw new BadRequestException("Placement ID is required");
    }
    return repository.findByPlacementId(placementId).stream()
        .map(mapper::toResponse)
        .toList();
  }

  @Override
  public PagedResponse<EmploymentVerificationFactResponse> getEmploymentVerificationsByPlacementId(
      Long placementId, Pageable pageable) {
    if (placementId == null) {
      throw new BadRequestException("Placement ID is required");
    }
    Page<EmploymentVerificationFact> page = repository.findByPlacementId(placementId, pageable);
    return PagedResponse.from(page, mapper::toResponse);
  }

  @Override
  public List<EmploymentVerificationFactResponse> getEmploymentVerificationsByEmployerId(Long employerId) {
    if (employerId == null) {
      throw new BadRequestException("Employer ID is required");
    }
    return repository.findByEmployerId(employerId).stream()
        .map(mapper::toResponse)
        .toList();
  }

  @Override
  public PagedResponse<EmploymentVerificationFactResponse> getEmploymentVerificationsByEmployerId(
      Long employerId, Pageable pageable) {
    if (employerId == null) {
      throw new BadRequestException("Employer ID is required");
    }
    Page<EmploymentVerificationFact> page = repository.findByEmployerId(employerId, pageable);
    return PagedResponse.from(page, mapper::toResponse);
  }

  @Override
  public List<EmploymentVerificationFactResponse> getEmploymentVerificationsByStatusId(Long statusId) {
    if (statusId == null) {
      throw new BadRequestException("Status ID is required");
    }
    return repository.findByRecordVerificationStatusId(statusId).stream()
        .map(mapper::toResponse)
        .toList();
  }

  @Override
  public PagedResponse<EmploymentVerificationFactResponse> getEmploymentVerificationsByStatusId(
      Long statusId, Pageable pageable) {
    if (statusId == null) {
      throw new BadRequestException("Status ID is required");
    }
    Page<EmploymentVerificationFact> page = repository.findByRecordVerificationStatusId(statusId, pageable);
    return PagedResponse.from(page, mapper::toResponse);
  }

  @Override
  public List<EmploymentVerificationFactResponse> getEmploymentVerificationsByMethodId(Long methodId) {
    if (methodId == null) {
      throw new BadRequestException("Method ID is required");
    }
    return repository.findByEmploymentVerificationMethodId(methodId).stream()
        .map(mapper::toResponse)
        .toList();
  }

  @Override
  public PagedResponse<EmploymentVerificationFactResponse> getEmploymentVerificationsByMethodId(
      Long methodId, Pageable pageable) {
    if (methodId == null) {
      throw new BadRequestException("Method ID is required");
    }
    Page<EmploymentVerificationFact> page = repository.findByEmploymentVerificationMethodId(methodId, pageable);
    return PagedResponse.from(page, mapper::toResponse);
  }

  @Override
  public List<EmploymentVerificationFactResponse> getCurrentEmploymentVerifications() {
    return repository.findByIsCurrentTrue().stream()
        .map(mapper::toResponse)
        .toList();
  }

  @Override
  public PagedResponse<EmploymentVerificationFactResponse> getCurrentEmploymentVerifications(Pageable pageable) {
    Page<EmploymentVerificationFact> page = repository.findByIsCurrentTrue(pageable);
    return PagedResponse.from(page, mapper::toResponse);
  }

  @Override
  public List<EmploymentVerificationFactResponse> getEmploymentVerificationsByIsVerifiedFlag(Boolean isVerifiedFlag) {
    if (isVerifiedFlag == null) {
      throw new BadRequestException("isVerifiedFlag is required");
    }
    return repository.findByIsVerifiedFlag(isVerifiedFlag).stream()
        .map(mapper::toResponse)
        .toList();
  }

  @Override
  public PagedResponse<EmploymentVerificationFactResponse> getEmploymentVerificationsByIsVerifiedFlag(
      Boolean isVerifiedFlag, Pageable pageable) {
    if (isVerifiedFlag == null) {
      throw new BadRequestException("isVerifiedFlag is required");
    }
    Page<EmploymentVerificationFact> page = repository.findByIsVerifiedFlag(isVerifiedFlag, pageable);
    return PagedResponse.from(page, mapper::toResponse);
  }
}
