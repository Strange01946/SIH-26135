package in.gov.sih.sih26135.service.impl;

import in.gov.sih.sih26135.dto.request.ChangeBatchStatusRequest;
import in.gov.sih.sih26135.dto.request.CreateTrainingBatchRequest;
import in.gov.sih.sih26135.dto.request.UpdateTrainingBatchRequest;
import in.gov.sih.sih26135.dto.response.TrainingBatchResponse;
import in.gov.sih.sih26135.entity.Course;
import in.gov.sih.sih26135.entity.Program;
import in.gov.sih.sih26135.entity.RefBatchStatus;
import in.gov.sih.sih26135.entity.TrainingBatch;
import in.gov.sih.sih26135.entity.TrainingCenter;
import in.gov.sih.sih26135.entity.TrainingProvider;
import in.gov.sih.sih26135.exception.BadRequestException;
import in.gov.sih.sih26135.exception.ConflictException;
import in.gov.sih.sih26135.exception.ResourceNotFoundException;
import in.gov.sih.sih26135.mapper.TrainingBatchMapper;
import in.gov.sih.sih26135.repository.CourseProgramRepository;
import in.gov.sih.sih26135.repository.CourseRepository;
import in.gov.sih.sih26135.repository.ProgramRepository;
import in.gov.sih.sih26135.repository.RefBatchStatusRepository;
import in.gov.sih.sih26135.repository.TrainingBatchRepository;
import in.gov.sih.sih26135.repository.TrainingCenterRepository;
import in.gov.sih.sih26135.repository.TrainingProviderRepository;
import in.gov.sih.sih26135.service.TrainingBatchService;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class TrainingBatchServiceImpl implements TrainingBatchService {

  private final TrainingBatchRepository trainingBatchRepository;
  private final CourseRepository courseRepository;
  private final TrainingProviderRepository trainingProviderRepository;
  private final TrainingCenterRepository trainingCenterRepository;
  private final ProgramRepository programRepository;
  private final RefBatchStatusRepository refBatchStatusRepository;
  private final CourseProgramRepository courseProgramRepository;
  private final TrainingBatchMapper trainingBatchMapper;

  public TrainingBatchServiceImpl(
      TrainingBatchRepository trainingBatchRepository,
      CourseRepository courseRepository,
      TrainingProviderRepository trainingProviderRepository,
      TrainingCenterRepository trainingCenterRepository,
      ProgramRepository programRepository,
      RefBatchStatusRepository refBatchStatusRepository,
      CourseProgramRepository courseProgramRepository,
      TrainingBatchMapper trainingBatchMapper) {
    this.trainingBatchRepository = trainingBatchRepository;
    this.courseRepository = courseRepository;
    this.trainingProviderRepository = trainingProviderRepository;
    this.trainingCenterRepository = trainingCenterRepository;
    this.programRepository = programRepository;
    this.refBatchStatusRepository = refBatchStatusRepository;
    this.courseProgramRepository = courseProgramRepository;
    this.trainingBatchMapper = trainingBatchMapper;
  }

  @Override
  public TrainingBatchResponse getById(Long id) {
    TrainingBatch batch = trainingBatchRepository.findById(id)
        .orElseThrow(() -> new ResourceNotFoundException("TrainingBatch", "id"));
    return trainingBatchMapper.toResponse(batch);
  }

  @Override
  public TrainingBatchResponse getByCode(String batchCode) {
    TrainingBatch batch = trainingBatchRepository.findByBatchCode(batchCode)
        .orElseThrow(() -> new ResourceNotFoundException("TrainingBatch", "batchCode"));
    return trainingBatchMapper.toResponse(batch);
  }

  @Override
  public List<TrainingBatchResponse> getAllBatches() {
    return trainingBatchRepository.findAll().stream()
        .map(trainingBatchMapper::toResponse)
        .toList();
  }

  @Override
  public List<TrainingBatchResponse> getBatchesByCourseId(Long courseId) {
    return trainingBatchRepository.findByCourseId(courseId).stream()
        .map(trainingBatchMapper::toResponse)
        .toList();
  }

  @Override
  public List<TrainingBatchResponse> getBatchesByProviderId(Long providerId) {
    return trainingBatchRepository.findByTrainingProviderId(providerId).stream()
        .map(trainingBatchMapper::toResponse)
        .toList();
  }

  @Override
  public List<TrainingBatchResponse> getBatchesByCenterId(Long centerId) {
    return trainingBatchRepository.findByTrainingCenterId(centerId).stream()
        .map(trainingBatchMapper::toResponse)
        .toList();
  }

  @Override
  public List<TrainingBatchResponse> getBatchesByProgramId(Long programId) {
    return trainingBatchRepository.findByProgramId(programId).stream()
        .map(trainingBatchMapper::toResponse)
        .toList();
  }

  @Override
  public List<TrainingBatchResponse> getBatchesByStatusId(Long batchStatusId) {
    return trainingBatchRepository.findByBatchStatusId(batchStatusId).stream()
        .map(trainingBatchMapper::toResponse)
        .toList();
  }

  @Override
  @Transactional
  public TrainingBatchResponse createBatch(CreateTrainingBatchRequest request) {
    if (request == null) {
      throw new BadRequestException("Training batch creation request cannot be null");
    }
    if (request.getBatchCode() == null || request.getBatchCode().isBlank()) {
      throw new BadRequestException("Batch code is required");
    }
    if (request.getCourseId() == null) {
      throw new BadRequestException("Course ID is required");
    }
    if (request.getProviderId() == null) {
      throw new BadRequestException("Provider ID is required");
    }
    if (request.getCenterId() == null) {
      throw new BadRequestException("Center ID is required");
    }
    if (request.getProgramId() == null) {
      throw new BadRequestException("Program ID is required");
    }
    if (request.getStartDate() == null) {
      throw new BadRequestException("Start date is required");
    }
    if (request.getCapacity() == null) {
      throw new BadRequestException("Capacity is required");
    }
    if (request.getBatchStatusId() == null) {
      throw new BadRequestException("Batch status ID is required");
    }

    String batchCode = request.getBatchCode().trim();
    if (trainingBatchRepository.existsByBatchCode(batchCode)) {
      throw new ConflictException("Batch code already exists", "BATCH_CODE_ALREADY_EXISTS");
    }

    if (request.getCapacity() <= 0) {
      throw new BadRequestException("Capacity must be greater than 0", "INVALID_BATCH_CAPACITY");
    }

    if (request.getEndDate() != null && request.getEndDate().isBefore(request.getStartDate())) {
      throw new BadRequestException("End date cannot be before start date", "INVALID_DATE_RANGE");
    }

    Course course = courseRepository.findById(request.getCourseId())
        .orElseThrow(() -> new ResourceNotFoundException("Course", "courseId"));

    TrainingProvider provider = trainingProviderRepository.findById(request.getProviderId())
        .orElseThrow(() -> new ResourceNotFoundException("TrainingProvider", "providerId"));

    TrainingCenter center = trainingCenterRepository.findById(request.getCenterId())
        .orElseThrow(() -> new ResourceNotFoundException("TrainingCenter", "centerId"));

    Program program = programRepository.findById(request.getProgramId())
        .orElseThrow(() -> new ResourceNotFoundException("Program", "programId"));

    RefBatchStatus status = refBatchStatusRepository.findById(request.getBatchStatusId())
        .orElseThrow(() -> new ResourceNotFoundException("RefBatchStatus", "batchStatusId"));

    if (center.getTrainingProvider() == null || !provider.getId().equals(center.getTrainingProvider().getId())) {
      throw new BadRequestException(
          "Training center does not belong to the specified training provider",
          "CENTER_PROVIDER_MISMATCH"
      );
    }

    if (!courseProgramRepository.existsByCourseIdAndProgramId(request.getCourseId(), request.getProgramId())) {
      throw new BadRequestException(
          "Course is not associated with the specified program",
          "COURSE_PROGRAM_MISMATCH"
      );
    }

    TrainingBatch batch = trainingBatchMapper.toEntity(request, course, provider, center, program, status);
    batch.setBatchCode(batchCode);

    LocalDateTime now = LocalDateTime.now();
    batch.setCreatedAt(now);
    batch.setUpdatedAt(now);

    TrainingBatch saved = trainingBatchRepository.save(batch);
    return trainingBatchMapper.toResponse(saved);
  }

  @Override
  @Transactional
  public TrainingBatchResponse updateBatch(Long id, UpdateTrainingBatchRequest request) {
    if (request == null) {
      throw new BadRequestException("Training batch update request cannot be null");
    }

    TrainingBatch batch = trainingBatchRepository.findById(id)
        .orElseThrow(() -> new ResourceNotFoundException("TrainingBatch", "id"));

    LocalDate prospectiveStartDate = request.getStartDate() != null ? request.getStartDate() : batch.getStartDate();
    LocalDate prospectiveEndDate = request.getEndDate() != null ? request.getEndDate() : batch.getEndDate();
    if (prospectiveStartDate != null && prospectiveEndDate != null && prospectiveEndDate.isBefore(prospectiveStartDate)) {
      throw new BadRequestException("End date cannot be before start date", "INVALID_DATE_RANGE");
    }

    if (request.getCapacity() != null) {
      if (request.getCapacity() <= 0) {
        throw new BadRequestException("Capacity must be greater than 0", "INVALID_BATCH_CAPACITY");
      }
      batch.setCapacity(request.getCapacity());
    }

    if (request.getStartDate() != null) {
      batch.setStartDate(request.getStartDate());
    }
    if (request.getEndDate() != null) {
      batch.setEndDate(request.getEndDate());
    }

    if (request.getBatchStatusId() != null) {
      RefBatchStatus status = refBatchStatusRepository.findById(request.getBatchStatusId())
          .orElseThrow(() -> new ResourceNotFoundException("RefBatchStatus", "batchStatusId"));
      batch.setBatchStatus(status);
    }

    batch.setUpdatedAt(LocalDateTime.now());
    TrainingBatch saved = trainingBatchRepository.save(batch);
    return trainingBatchMapper.toResponse(saved);
  }

  @Override
  @Transactional
  public TrainingBatchResponse updateBatchStatus(Long id, ChangeBatchStatusRequest request) {
    if (request == null || request.getBatchStatusId() == null) {
      throw new BadRequestException("Batch status ID is required");
    }

    TrainingBatch batch = trainingBatchRepository.findById(id)
        .orElseThrow(() -> new ResourceNotFoundException("TrainingBatch", "id"));

    RefBatchStatus status = refBatchStatusRepository.findById(request.getBatchStatusId())
        .orElseThrow(() -> new ResourceNotFoundException("RefBatchStatus", "batchStatusId"));

    batch.setBatchStatus(status);
    batch.setUpdatedAt(LocalDateTime.now());

    TrainingBatch saved = trainingBatchRepository.save(batch);
    return trainingBatchMapper.toResponse(saved);
  }

  @Override
  @Transactional
  public void deleteBatch(Long id) {
    TrainingBatch batch = trainingBatchRepository.findById(id)
        .orElseThrow(() -> new ResourceNotFoundException("TrainingBatch", "id"));

    LocalDateTime now = LocalDateTime.now();
    batch.setDeletedAt(now);
    batch.setUpdatedAt(now);
    trainingBatchRepository.save(batch);
  }
}
