package in.gov.sih.sih26135.service;

import in.gov.sih.sih26135.dto.request.CreatePlacementRecordRequest;
import in.gov.sih.sih26135.dto.request.UpdatePlacementRecordRequest;
import in.gov.sih.sih26135.dto.response.PlacementRecordResponse;
import java.util.List;

public interface PlacementRecordService {

  PlacementRecordResponse createPlacementRecord(CreatePlacementRecordRequest request);

  PlacementRecordResponse updatePlacementRecord(Long id, UpdatePlacementRecordRequest request);

  PlacementRecordResponse getPlacementRecordById(Long id);

  PlacementRecordResponse getPlacementRecordByNumber(String placementNumber);

  PlacementRecordResponse getPlacementRecordByJobApplicationId(Long jobApplicationId);

  List<PlacementRecordResponse> getAllPlacementRecords(boolean includeDeleted);

  List<PlacementRecordResponse> getPlacementRecordsByTrainee(Long traineeId);

  List<PlacementRecordResponse> getPlacementRecordsByEnrollment(Long enrollmentId);

  List<PlacementRecordResponse> getPlacementRecordsByEmployer(Long employerId);

  List<PlacementRecordResponse> getPlacementRecordsByJobPosting(Long jobPostingId);

  List<PlacementRecordResponse> getPlacementRecordsByCourse(Long courseId);

  List<PlacementRecordResponse> getPlacementRecordsByStatus(Long placementStatusId);

  void deletePlacementRecord(Long id);
}
