package in.gov.sih.sih26135.service.impl;

import in.gov.sih.sih26135.dto.request.AssignProgramTrainingProviderRequest;
import in.gov.sih.sih26135.dto.request.CreateProgramRequest;
import in.gov.sih.sih26135.dto.request.TerminateEmpanelmentRequest;
import in.gov.sih.sih26135.dto.request.UpdateProgramRequest;
import in.gov.sih.sih26135.dto.response.ProgramResponse;
import in.gov.sih.sih26135.dto.response.ProgramTrainingProviderResponse;
import in.gov.sih.sih26135.entity.Program;
import in.gov.sih.sih26135.entity.ProgramTrainingProvider;
import in.gov.sih.sih26135.entity.Scheme;
import in.gov.sih.sih26135.entity.TrainingProvider;
import in.gov.sih.sih26135.exception.BadRequestException;
import in.gov.sih.sih26135.exception.ConflictException;
import in.gov.sih.sih26135.exception.ResourceNotFoundException;
import in.gov.sih.sih26135.mapper.ProgramMapper;
import in.gov.sih.sih26135.mapper.ProgramTrainingProviderMapper;
import in.gov.sih.sih26135.repository.ProgramRepository;
import in.gov.sih.sih26135.repository.ProgramTrainingProviderRepository;
import in.gov.sih.sih26135.repository.SchemeRepository;
import in.gov.sih.sih26135.repository.TrainingProviderRepository;
import in.gov.sih.sih26135.service.ProgramService;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class ProgramServiceImpl implements ProgramService {

  private final ProgramRepository programRepository;
  private final SchemeRepository schemeRepository;
  private final TrainingProviderRepository trainingProviderRepository;
  private final ProgramTrainingProviderRepository programTrainingProviderRepository;
  private final ProgramMapper programMapper;
  private final ProgramTrainingProviderMapper programTrainingProviderMapper;

  public ProgramServiceImpl(
      ProgramRepository programRepository,
      SchemeRepository schemeRepository,
      TrainingProviderRepository trainingProviderRepository,
      ProgramTrainingProviderRepository programTrainingProviderRepository,
      ProgramMapper programMapper,
      ProgramTrainingProviderMapper programTrainingProviderMapper) {
    this.programRepository = programRepository;
    this.schemeRepository = schemeRepository;
    this.trainingProviderRepository = trainingProviderRepository;
    this.programTrainingProviderRepository = programTrainingProviderRepository;
    this.programMapper = programMapper;
    this.programTrainingProviderMapper = programTrainingProviderMapper;
  }

  @Override
  public ProgramResponse getById(Long id) {
    Program program = programRepository.findById(id)
        .orElseThrow(() -> new ResourceNotFoundException("Program", "id"));
    return programMapper.toResponse(program);
  }

  @Override
  public ProgramResponse getByCode(String programCode) {
    Program program = programRepository.findByProgramCode(programCode)
        .orElseThrow(() -> new ResourceNotFoundException("Program", "programCode"));
    return programMapper.toResponse(program);
  }

  @Override
  public List<ProgramResponse> getAllPrograms() {
    return programRepository.findAll().stream()
        .map(programMapper::toResponse)
        .toList();
  }

  @Override
  public List<ProgramResponse> getProgramsBySchemeId(Long schemeId) {
    return programRepository.findBySchemeId(schemeId).stream()
        .map(programMapper::toResponse)
        .toList();
  }

  @Override
  public List<ProgramResponse> getProgramsByDepartmentId(Long departmentId) {
    return programRepository.findByDepartmentId(departmentId).stream()
        .map(programMapper::toResponse)
        .toList();
  }

  @Override
  @Transactional
  public ProgramResponse createProgram(CreateProgramRequest request) {
    if (request == null) {
      throw new BadRequestException("Program creation request cannot be null");
    }
    if (request.getProgramCode() == null || request.getProgramCode().isBlank()) {
      throw new BadRequestException("Program code is required");
    }
    if (request.getProgramName() == null || request.getProgramName().isBlank()) {
      throw new BadRequestException("Program name is required");
    }
    if (request.getDepartmentId() == null) {
      throw new BadRequestException("Department ID is required");
    }
    if (request.getSchemeId() == null) {
      throw new BadRequestException("Scheme ID is required", "SCHEME_ID_REQUIRED");
    }
    if (request.getStartDate() == null) {
      throw new BadRequestException("Start date is required");
    }
    if (request.getLifecycleStatusId() == null) {
      throw new BadRequestException("Lifecycle status ID is required");
    }

    String programCode = request.getProgramCode().trim();
    if (programRepository.existsByProgramCode(programCode)) {
      throw new ConflictException("Program code already exists", "PROGRAM_CODE_ALREADY_EXISTS");
    }

    if (request.getBudget() != null && request.getBudget().compareTo(BigDecimal.ZERO) < 0) {
      throw new BadRequestException("Budget must be greater than or equal to 0", "INVALID_BUDGET");
    }

    if (request.getEndDate() != null && request.getEndDate().isBefore(request.getStartDate())) {
      throw new BadRequestException("End date cannot be before start date", "INVALID_DATE_RANGE");
    }

    Scheme scheme = schemeRepository.findById(request.getSchemeId())
        .orElseThrow(() -> new ResourceNotFoundException("Scheme", "schemeId"));

    Program program = programMapper.toEntity(request, scheme);
    program.setProgramCode(programCode);
    program.setProgramName(request.getProgramName().trim());
    if (request.getDescription() != null) {
      program.setDescription(request.getDescription().trim());
    }

    LocalDateTime now = LocalDateTime.now();
    program.setCreatedAt(now);
    program.setUpdatedAt(now);

    Program saved = programRepository.save(program);
    return programMapper.toResponse(saved);
  }

  @Override
  @Transactional
  public ProgramResponse updateProgram(Long id, UpdateProgramRequest request) {
    if (request == null) {
      throw new BadRequestException("Program update request cannot be null");
    }

    Program program = programRepository.findById(id)
        .orElseThrow(() -> new ResourceNotFoundException("Program", "id"));

    LocalDate prospectiveStartDate = request.getStartDate() != null ? request.getStartDate() : program.getStartDate();
    LocalDate prospectiveEndDate = request.getEndDate() != null ? request.getEndDate() : program.getEndDate();
    if (prospectiveStartDate != null && prospectiveEndDate != null && prospectiveEndDate.isBefore(prospectiveStartDate)) {
      throw new BadRequestException("End date cannot be before start date", "INVALID_DATE_RANGE");
    }

    if (request.getBudget() != null && request.getBudget().compareTo(BigDecimal.ZERO) < 0) {
      throw new BadRequestException("Budget must be greater than or equal to 0", "INVALID_BUDGET");
    }

    if (request.getSchemeId() != null) {
      Scheme scheme = schemeRepository.findById(request.getSchemeId())
          .orElseThrow(() -> new ResourceNotFoundException("Scheme", "schemeId"));
      program.setScheme(scheme);
    }

    if (request.getProgramName() != null && !request.getProgramName().isBlank()) {
      program.setProgramName(request.getProgramName().trim());
    }
    if (request.getDescription() != null) {
      program.setDescription(request.getDescription().trim());
    }
    if (request.getDepartmentId() != null) {
      program.setDepartmentId(request.getDepartmentId());
    }
    if (request.getStartDate() != null) {
      program.setStartDate(request.getStartDate());
    }
    if (request.getEndDate() != null) {
      program.setEndDate(request.getEndDate());
    }
    if (request.getBudget() != null) {
      program.setBudget(request.getBudget());
    }
    if (request.getCurrencyCode() != null && !request.getCurrencyCode().isBlank()) {
      program.setCurrencyCode(request.getCurrencyCode().trim());
    }
    if (request.getTargetBeneficiaries() != null) {
      program.setTargetBeneficiaries(request.getTargetBeneficiaries());
    }
    if (request.getLifecycleStatusId() != null) {
      program.setLifecycleStatusId(request.getLifecycleStatusId());
    }

    program.setUpdatedAt(LocalDateTime.now());
    Program saved = programRepository.save(program);
    return programMapper.toResponse(saved);
  }

  @Override
  @Transactional
  public void deleteProgram(Long id) {
    Program program = programRepository.findById(id)
        .orElseThrow(() -> new ResourceNotFoundException("Program", "id"));

    LocalDateTime now = LocalDateTime.now();
    program.setDeletedAt(now);
    program.setUpdatedAt(now);
    programRepository.save(program);
  }

  @Override
  @Transactional
  public ProgramTrainingProviderResponse assignTrainingProvider(AssignProgramTrainingProviderRequest request) {
    if (request == null) {
      throw new BadRequestException("Assignment request cannot be null");
    }
    if (request.getProgramId() == null) {
      throw new BadRequestException("Program ID is required");
    }
    if (request.getProviderId() == null) {
      throw new BadRequestException("Training provider ID is required");
    }
    if (request.getEmpanelledFrom() == null) {
      throw new BadRequestException("Empanelled from date is required");
    }
    if (request.getLifecycleStatusId() == null) {
      throw new BadRequestException("Lifecycle status ID is required");
    }

    if (request.getEmpanelledTo() != null && request.getEmpanelledTo().isBefore(request.getEmpanelledFrom())) {
      throw new BadRequestException("Empanelled to date cannot be before empanelled from date", "INVALID_EMPANELMENT_DATES");
    }

    Program program = programRepository.findById(request.getProgramId())
        .orElseThrow(() -> new ResourceNotFoundException("Program", "programId"));

    TrainingProvider provider = trainingProviderRepository.findById(request.getProviderId())
        .orElseThrow(() -> new ResourceNotFoundException("TrainingProvider", "providerId"));

    if (programTrainingProviderRepository.existsByProgramIdAndTrainingProviderIdAndEmpanelledFrom(
        request.getProgramId(), request.getProviderId(), request.getEmpanelledFrom())) {
      throw new ConflictException("Training provider is already empanelled in this program from this date", "EMPANELMENT_ALREADY_EXISTS");
    }

    ProgramTrainingProvider ptp = programTrainingProviderMapper.toEntity(request, program, provider);
    LocalDateTime now = LocalDateTime.now();
    ptp.setCreatedAt(now);
    ptp.setUpdatedAt(now);

    ProgramTrainingProvider saved = programTrainingProviderRepository.save(ptp);
    return programTrainingProviderMapper.toResponse(saved);
  }

  @Override
  @Transactional
  public ProgramTrainingProviderResponse terminateEmpanelment(Long assignmentId, TerminateEmpanelmentRequest request) {
    if (assignmentId == null) {
      throw new BadRequestException("Assignment ID is required");
    }

    ProgramTrainingProvider assignment = programTrainingProviderRepository.findById(assignmentId)
        .orElseThrow(() -> new ResourceNotFoundException("ProgramTrainingProvider", "assignmentId"));

    LocalDate terminationDate = (request != null && request.getEmpanelledTo() != null)
        ? request.getEmpanelledTo()
        : LocalDate.now();

    if (terminationDate.isBefore(assignment.getEmpanelledFrom())) {
      throw new BadRequestException("Empanelled to date cannot be before empanelled from date", "INVALID_EMPANELMENT_DATES");
    }

    assignment.setEmpanelledTo(terminationDate);
    if (request != null && request.getLifecycleStatusId() != null) {
      assignment.setLifecycleStatusId(request.getLifecycleStatusId());
    }

    assignment.setUpdatedAt(LocalDateTime.now());
    ProgramTrainingProvider saved = programTrainingProviderRepository.save(assignment);
    return programTrainingProviderMapper.toResponse(saved);
  }

  @Override
  public List<ProgramTrainingProviderResponse> getTrainingProvidersForProgram(Long programId) {
    return programTrainingProviderRepository.findByProgramId(programId).stream()
        .map(programTrainingProviderMapper::toResponse)
        .toList();
  }

  @Override
  public List<ProgramTrainingProviderResponse> getProgramsForTrainingProvider(Long providerId) {
    return programTrainingProviderRepository.findByTrainingProviderId(providerId).stream()
        .map(programTrainingProviderMapper::toResponse)
        .toList();
  }
}
