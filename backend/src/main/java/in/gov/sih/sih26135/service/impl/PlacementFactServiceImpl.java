package in.gov.sih.sih26135.service.impl;

import in.gov.sih.sih26135.dto.response.PlacementFactResponse;
import in.gov.sih.sih26135.entity.analytics.PlacementFact;
import in.gov.sih.sih26135.exception.BadRequestException;
import in.gov.sih.sih26135.exception.ResourceNotFoundException;
import in.gov.sih.sih26135.mapper.PlacementFactMapper;
import in.gov.sih.sih26135.repository.analytics.PlacementFactRepository;
import in.gov.sih.sih26135.response.PagedResponse;
import in.gov.sih.sih26135.service.PlacementFactService;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class PlacementFactServiceImpl implements PlacementFactService {

  private final PlacementFactRepository repository;
  private final PlacementFactMapper mapper;

  public PlacementFactServiceImpl(
      PlacementFactRepository repository,
      PlacementFactMapper mapper) {
    this.repository = repository;
    this.mapper = mapper;
  }

  @Override
  public PagedResponse<PlacementFactResponse> getAllPlacements(Pageable pageable) {
    Page<PlacementFact> page = repository.findAll(pageable);
    return PagedResponse.from(page, mapper::toResponse);
  }

  @Override
  public List<PlacementFactResponse> getAllPlacements() {
    return repository.findAll().stream()
        .map(mapper::toResponse)
        .toList();
  }

  @Override
  public PlacementFactResponse getPlacementById(Long placementId) {
    if (placementId == null) {
      throw new BadRequestException("Placement ID is required");
    }
    PlacementFact entity = repository.findById(placementId)
        .orElseThrow(() -> new ResourceNotFoundException("PlacementFact", "placementId"));
    return mapper.toResponse(entity);
  }

  @Override
  public PlacementFactResponse getPlacementByPlacementNumber(String placementNumber) {
    if (placementNumber == null || placementNumber.isBlank()) {
      throw new BadRequestException("Placement number is required");
    }
    PlacementFact entity = repository.findByPlacementNumber(placementNumber)
        .orElseThrow(() -> new ResourceNotFoundException("PlacementFact", "placementNumber"));
    return mapper.toResponse(entity);
  }

  @Override
  public List<PlacementFactResponse> getPlacementsByTraineeId(Long traineeId) {
    if (traineeId == null) {
      throw new BadRequestException("Trainee ID is required");
    }
    return repository.findByTraineeId(traineeId).stream()
        .map(mapper::toResponse)
        .toList();
  }

  @Override
  public PagedResponse<PlacementFactResponse> getPlacementsByTraineeId(Long traineeId, Pageable pageable) {
    if (traineeId == null) {
      throw new BadRequestException("Trainee ID is required");
    }
    Page<PlacementFact> page = repository.findByTraineeId(traineeId, pageable);
    return PagedResponse.from(page, mapper::toResponse);
  }

  @Override
  public List<PlacementFactResponse> getPlacementsByEnrollmentId(Long enrollmentId) {
    if (enrollmentId == null) {
      throw new BadRequestException("Enrollment ID is required");
    }
    return repository.findByEnrollmentId(enrollmentId).stream()
        .map(mapper::toResponse)
        .toList();
  }

  @Override
  public PagedResponse<PlacementFactResponse> getPlacementsByEnrollmentId(Long enrollmentId, Pageable pageable) {
    if (enrollmentId == null) {
      throw new BadRequestException("Enrollment ID is required");
    }
    Page<PlacementFact> page = repository.findByEnrollmentId(enrollmentId, pageable);
    return PagedResponse.from(page, mapper::toResponse);
  }

  @Override
  public List<PlacementFactResponse> getPlacementsByEmployerId(Long employerId) {
    if (employerId == null) {
      throw new BadRequestException("Employer ID is required");
    }
    return repository.findByEmployerId(employerId).stream()
        .map(mapper::toResponse)
        .toList();
  }

  @Override
  public PagedResponse<PlacementFactResponse> getPlacementsByEmployerId(Long employerId, Pageable pageable) {
    if (employerId == null) {
      throw new BadRequestException("Employer ID is required");
    }
    Page<PlacementFact> page = repository.findByEmployerId(employerId, pageable);
    return PagedResponse.from(page, mapper::toResponse);
  }

  @Override
  public List<PlacementFactResponse> getPlacementsByCourseId(Long courseId) {
    if (courseId == null) {
      throw new BadRequestException("Course ID is required");
    }
    return repository.findByCourseId(courseId).stream()
        .map(mapper::toResponse)
        .toList();
  }

  @Override
  public PagedResponse<PlacementFactResponse> getPlacementsByCourseId(Long courseId, Pageable pageable) {
    if (courseId == null) {
      throw new BadRequestException("Course ID is required");
    }
    Page<PlacementFact> page = repository.findByCourseId(courseId, pageable);
    return PagedResponse.from(page, mapper::toResponse);
  }

  @Override
  public List<PlacementFactResponse> getPlacementsByProgramId(Long programId) {
    if (programId == null) {
      throw new BadRequestException("Program ID is required");
    }
    return repository.findByProgramId(programId).stream()
        .map(mapper::toResponse)
        .toList();
  }

  @Override
  public PagedResponse<PlacementFactResponse> getPlacementsByProgramId(Long programId, Pageable pageable) {
    if (programId == null) {
      throw new BadRequestException("Program ID is required");
    }
    Page<PlacementFact> page = repository.findByProgramId(programId, pageable);
    return PagedResponse.from(page, mapper::toResponse);
  }

  @Override
  public List<PlacementFactResponse> getPlacementsByProviderId(Long providerId) {
    if (providerId == null) {
      throw new BadRequestException("Provider ID is required");
    }
    return repository.findByProviderId(providerId).stream()
        .map(mapper::toResponse)
        .toList();
  }

  @Override
  public PagedResponse<PlacementFactResponse> getPlacementsByProviderId(Long providerId, Pageable pageable) {
    if (providerId == null) {
      throw new BadRequestException("Provider ID is required");
    }
    Page<PlacementFact> page = repository.findByProviderId(providerId, pageable);
    return PagedResponse.from(page, mapper::toResponse);
  }

  @Override
  public List<PlacementFactResponse> getPlacementsByJobRoleId(Long jobRoleId) {
    if (jobRoleId == null) {
      throw new BadRequestException("Job role ID is required");
    }
    return repository.findByJobRoleId(jobRoleId).stream()
        .map(mapper::toResponse)
        .toList();
  }

  @Override
  public PagedResponse<PlacementFactResponse> getPlacementsByJobRoleId(Long jobRoleId, Pageable pageable) {
    if (jobRoleId == null) {
      throw new BadRequestException("Job role ID is required");
    }
    Page<PlacementFact> page = repository.findByJobRoleId(jobRoleId, pageable);
    return PagedResponse.from(page, mapper::toResponse);
  }

  @Override
  public List<PlacementFactResponse> getPlacementsByTraineeDistrictId(Long traineeDistrictId) {
    if (traineeDistrictId == null) {
      throw new BadRequestException("Trainee district ID is required");
    }
    return repository.findByTraineeDistrictId(traineeDistrictId).stream()
        .map(mapper::toResponse)
        .toList();
  }

  @Override
  public PagedResponse<PlacementFactResponse> getPlacementsByTraineeDistrictId(Long traineeDistrictId, Pageable pageable) {
    if (traineeDistrictId == null) {
      throw new BadRequestException("Trainee district ID is required");
    }
    Page<PlacementFact> page = repository.findByTraineeDistrictId(traineeDistrictId, pageable);
    return PagedResponse.from(page, mapper::toResponse);
  }
}
