package in.gov.sih.sih26135.controller;

import in.gov.sih.sih26135.dto.request.AssignProgramTrainingProviderRequest;
import in.gov.sih.sih26135.dto.request.CreateProgramRequest;
import in.gov.sih.sih26135.dto.request.TerminateEmpanelmentRequest;
import in.gov.sih.sih26135.dto.request.UpdateProgramRequest;
import in.gov.sih.sih26135.dto.response.ProgramResponse;
import in.gov.sih.sih26135.dto.response.ProgramTrainingProviderResponse;
import in.gov.sih.sih26135.response.ApiResponse;
import in.gov.sih.sih26135.service.ProgramService;
import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller for managing Program domain resources and program-training provider empanelments.
 *
 * <p>Base Route: /api/v1/programs
 * Consumes: CreateProgramRequest, UpdateProgramRequest, AssignProgramTrainingProviderRequest, TerminateEmpanelmentRequest
 * Produces: ProgramResponse, ProgramTrainingProviderResponse enveloped in ApiResponse
 */
@RestController
@RequestMapping("/api/v1/programs")
public class ProgramController {

  private final ProgramService programService;

  public ProgramController(ProgramService programService) {
    this.programService = programService;
  }

  /**
   * Retrieves a program by primary key identifier.
   *
   * @param id primary key identifier of the program
   * @return 200 OK with ProgramResponse enveloped in ApiResponse
   */
  @GetMapping("/{id}")
  public ResponseEntity<ApiResponse<ProgramResponse>> getById(@PathVariable Long id) {
    ProgramResponse response = programService.getById(id);
    return ResponseEntity.ok(ApiResponse.ok(response));
  }

  /**
   * Retrieves programs with optional filtering by program code, scheme ID, or department ID.
   *
   * @param code optional program code filter
   * @param schemeId optional scheme identifier filter
   * @param departmentId optional department identifier filter
   * @return 200 OK with list of programs or single matched program enveloped in ApiResponse
   */
  @GetMapping
  public ResponseEntity<ApiResponse<?>> getPrograms(
      @RequestParam(value = "code", required = false) String code,
      @RequestParam(value = "schemeId", required = false) Long schemeId,
      @RequestParam(value = "departmentId", required = false) Long departmentId) {
    if (code != null && !code.isBlank()) {
      ProgramResponse response = programService.getByCode(code.trim());
      return ResponseEntity.ok(ApiResponse.ok(response));
    }
    if (schemeId != null) {
      List<ProgramResponse> programs = programService.getProgramsBySchemeId(schemeId);
      return ResponseEntity.ok(ApiResponse.ok(programs));
    }
    if (departmentId != null) {
      List<ProgramResponse> programs = programService.getProgramsByDepartmentId(departmentId);
      return ResponseEntity.ok(ApiResponse.ok(programs));
    }
    List<ProgramResponse> programs = programService.getAllPrograms();
    return ResponseEntity.ok(ApiResponse.ok(programs));
  }

  /**
   * Creates a new program record.
   *
   * @param request program creation payload
   * @param httpRequest HTTP servlet request for URI extraction
   * @return 201 Created with created ProgramResponse enveloped in ApiResponse
   */
  @PostMapping
  public ResponseEntity<ApiResponse<ProgramResponse>> createProgram(
      @RequestBody CreateProgramRequest request,
      HttpServletRequest httpRequest) {
    ProgramResponse response = programService.createProgram(request);
    return ResponseEntity.status(HttpStatus.CREATED)
        .body(ApiResponse.success("Program created successfully", response, httpRequest.getRequestURI()));
  }

  /**
   * Updates an existing program record.
   *
   * @param id primary key identifier of the program to update
   * @param request program update payload
   * @return 200 OK with updated ProgramResponse enveloped in ApiResponse
   */
  @PutMapping("/{id}")
  public ResponseEntity<ApiResponse<ProgramResponse>> updateProgram(
      @PathVariable Long id,
      @RequestBody UpdateProgramRequest request) {
    ProgramResponse response = programService.updateProgram(id, request);
    return ResponseEntity.ok(ApiResponse.success("Program updated successfully", response));
  }

  /**
   * Soft-deletes a program record.
   *
   * @param id primary key identifier of the program to delete
   * @return 200 OK with success confirmation message
   */
  @DeleteMapping("/{id}")
  public ResponseEntity<ApiResponse<Void>> deleteProgram(@PathVariable Long id) {
    programService.deleteProgram(id);
    return ResponseEntity.ok(ApiResponse.success("Program deleted successfully"));
  }

  /**
   * Retrieves all training providers empanelled under a specific program.
   *
   * @param programId primary key identifier of the program
   * @return 200 OK with list of ProgramTrainingProviderResponse enveloped in ApiResponse
   */
  @GetMapping("/{programId}/training-providers")
  public ResponseEntity<ApiResponse<List<ProgramTrainingProviderResponse>>> getTrainingProvidersForProgram(
      @PathVariable Long programId) {
    List<ProgramTrainingProviderResponse> providers = programService.getTrainingProvidersForProgram(programId);
    return ResponseEntity.ok(ApiResponse.ok(providers));
  }

  /**
   * Empanels / assigns a training provider to a specific program.
   *
   * @param programId primary key identifier of the program
   * @param request assignment payload
   * @param httpRequest HTTP servlet request for URI extraction
   * @return 201 Created with ProgramTrainingProviderResponse enveloped in ApiResponse
   */
  @PostMapping("/{programId}/training-providers")
  public ResponseEntity<ApiResponse<ProgramTrainingProviderResponse>> assignTrainingProvider(
      @PathVariable Long programId,
      @RequestBody AssignProgramTrainingProviderRequest request,
      HttpServletRequest httpRequest) {
    request.setProgramId(programId);
    ProgramTrainingProviderResponse response = programService.assignTrainingProvider(request);
    return ResponseEntity.status(HttpStatus.CREATED)
        .body(ApiResponse.success("Training provider assigned to program successfully", response, httpRequest.getRequestURI()));
  }

  /**
   * Terminates empanelment of a training provider in a program.
   *
   * @param programId primary key identifier of the program
   * @param assignmentId primary key identifier of the empanelment assignment
   * @param request termination details (optional empanelledTo date and status)
   * @return 200 OK with updated ProgramTrainingProviderResponse enveloped in ApiResponse
   */
  @PutMapping("/{programId}/training-providers/{assignmentId}/terminate")
  public ResponseEntity<ApiResponse<ProgramTrainingProviderResponse>> terminateEmpanelment(
      @PathVariable Long programId,
      @PathVariable Long assignmentId,
      @RequestBody(required = false) TerminateEmpanelmentRequest request) {
    ProgramTrainingProviderResponse response = programService.terminateEmpanelment(assignmentId, request);
    return ResponseEntity.ok(ApiResponse.success("Empanelment terminated successfully", response));
  }

  /**
   * Retrieves all programs where a specific training provider is empanelled.
   *
   * @param providerId primary key identifier of the training provider
   * @return 200 OK with list of ProgramTrainingProviderResponse enveloped in ApiResponse
   */
  @GetMapping("/training-providers/{providerId}")
  public ResponseEntity<ApiResponse<List<ProgramTrainingProviderResponse>>> getProgramsForTrainingProvider(
      @PathVariable Long providerId) {
    List<ProgramTrainingProviderResponse> programs = programService.getProgramsForTrainingProvider(providerId);
    return ResponseEntity.ok(ApiResponse.ok(programs));
  }
}
