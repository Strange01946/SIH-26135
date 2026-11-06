package in.gov.sih.sih26135.service;

import in.gov.sih.sih26135.dto.response.PlacementFactResponse;
import in.gov.sih.sih26135.response.PagedResponse;
import java.util.List;
import org.springframework.data.domain.Pageable;

public interface PlacementFactService {

  PagedResponse<PlacementFactResponse> getAllPlacements(Pageable pageable);

  List<PlacementFactResponse> getAllPlacements();

  PlacementFactResponse getPlacementById(Long placementId);

  PlacementFactResponse getPlacementByPlacementNumber(String placementNumber);

  List<PlacementFactResponse> getPlacementsByTraineeId(Long traineeId);

  PagedResponse<PlacementFactResponse> getPlacementsByTraineeId(Long traineeId, Pageable pageable);

  List<PlacementFactResponse> getPlacementsByEnrollmentId(Long enrollmentId);

  PagedResponse<PlacementFactResponse> getPlacementsByEnrollmentId(Long enrollmentId, Pageable pageable);

  List<PlacementFactResponse> getPlacementsByEmployerId(Long employerId);

  PagedResponse<PlacementFactResponse> getPlacementsByEmployerId(Long employerId, Pageable pageable);

  List<PlacementFactResponse> getPlacementsByCourseId(Long courseId);

  PagedResponse<PlacementFactResponse> getPlacementsByCourseId(Long courseId, Pageable pageable);

  List<PlacementFactResponse> getPlacementsByProgramId(Long programId);

  PagedResponse<PlacementFactResponse> getPlacementsByProgramId(Long programId, Pageable pageable);

  List<PlacementFactResponse> getPlacementsByProviderId(Long providerId);

  PagedResponse<PlacementFactResponse> getPlacementsByProviderId(Long providerId, Pageable pageable);

  List<PlacementFactResponse> getPlacementsByJobRoleId(Long jobRoleId);

  PagedResponse<PlacementFactResponse> getPlacementsByJobRoleId(Long jobRoleId, Pageable pageable);

  List<PlacementFactResponse> getPlacementsByTraineeDistrictId(Long traineeDistrictId);

  PagedResponse<PlacementFactResponse> getPlacementsByTraineeDistrictId(Long traineeDistrictId, Pageable pageable);
}
