package in.gov.sih.sih26135.service;

import in.gov.sih.sih26135.dto.request.CreatePermissionRequest;
import in.gov.sih.sih26135.dto.response.PermissionResponse;
import java.util.List;

public interface PermissionService {

  PermissionResponse getById(Long id);

  PermissionResponse getByCode(String permissionCode);

  List<PermissionResponse> getAllPermissions();

  List<PermissionResponse> getByModuleCode(String moduleCode);

  PermissionResponse createPermission(CreatePermissionRequest request);
}
