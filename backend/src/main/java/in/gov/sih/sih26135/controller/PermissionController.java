package in.gov.sih.sih26135.controller;

import in.gov.sih.sih26135.dto.request.CreatePermissionRequest;
import in.gov.sih.sih26135.dto.response.PermissionResponse;
import in.gov.sih.sih26135.response.ApiResponse;
import in.gov.sih.sih26135.service.PermissionService;
import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller for managing Permission domain catalog resources.
 *
 * <p>Base Route: /api/v1/permissions
 * Consumes: CreatePermissionRequest
 * Produces: PermissionResponse enveloped in ApiResponse
 */
@RestController
@RequestMapping("/api/v1/permissions")
public class PermissionController {

  private final PermissionService permissionService;

  public PermissionController(PermissionService permissionService) {
    this.permissionService = permissionService;
  }

  /**
   * Retrieves a permission by primary key identifier.
   *
   * @param id primary key identifier of the permission
   * @return 200 OK with PermissionResponse enveloped in ApiResponse
   */
  @GetMapping("/{id}")
  public ResponseEntity<ApiResponse<PermissionResponse>> getById(@PathVariable Long id) {
    PermissionResponse response = permissionService.getById(id);
    return ResponseEntity.ok(ApiResponse.ok(response));
  }

  /**
   * Retrieves permissions with optional filtering by code or module code.
   *
   * @param code optional permission code filter
   * @param moduleCode optional module code filter
   * @return 200 OK with list of permissions or single matched permission enveloped in ApiResponse
   */
  @GetMapping
  public ResponseEntity<ApiResponse<?>> getPermissions(
      @RequestParam(value = "code", required = false) String code,
      @RequestParam(value = "moduleCode", required = false) String moduleCode) {
    if (code != null && !code.isBlank()) {
      PermissionResponse response = permissionService.getByCode(code.trim());
      return ResponseEntity.ok(ApiResponse.ok(response));
    }
    if (moduleCode != null && !moduleCode.isBlank()) {
      List<PermissionResponse> permissions = permissionService.getByModuleCode(moduleCode.trim());
      return ResponseEntity.ok(ApiResponse.ok(permissions));
    }
    List<PermissionResponse> permissions = permissionService.getAllPermissions();
    return ResponseEntity.ok(ApiResponse.ok(permissions));
  }

  /**
   * Creates a new permission record.
   *
   * @param request permission creation payload
   * @param httpRequest HTTP servlet request for URI extraction
   * @return 201 Created with created PermissionResponse enveloped in ApiResponse
   */
  @PostMapping
  public ResponseEntity<ApiResponse<PermissionResponse>> createPermission(
      @RequestBody CreatePermissionRequest request,
      HttpServletRequest httpRequest) {
    PermissionResponse response = permissionService.createPermission(request);
    return ResponseEntity.status(HttpStatus.CREATED)
        .body(ApiResponse.success("Permission created successfully", response, httpRequest.getRequestURI()));
  }
}
