package in.gov.sih.sih26135.service;

import in.gov.sih.sih26135.dto.response.PlacementFactResponse;
import java.util.List;

public interface PlacementFactService {

  List<PlacementFactResponse> getAllPlacements();

  PlacementFactResponse getPlacementById(Long placementId);

  PlacementFactResponse getPlacementByPlacementNumber(String placementNumber);

  List<PlacementFactResponse> getPlacementsByTraineeId(Long traineeId);

  List<PlacementFactResponse> getPlacementsByEnrollmentId(Long enrollmentId);

  List<PlacementFactResponse> getPlacementsByEmployerId(Long employerId);

  List<PlacementFactResponse> getPlacementsByCourseId(Long courseId);

  List<PlacementFactResponse> getPlacementsByProgramId(Long programId);

  List<PlacementFactResponse> getPlacementsByProviderId(Long providerId);

  List<PlacementFactResponse> getPlacementsByJobRoleId(Long jobRoleId);

  List<PlacementFactResponse> getPlacementsByTraineeDistrictId(Long traineeDistrictId);
}
