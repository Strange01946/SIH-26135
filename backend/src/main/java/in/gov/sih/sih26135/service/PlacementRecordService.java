package in.gov.sih.sih26135.service;

import in.gov.sih.sih26135.dto.request.CreatePlacementRecordRequest;
import in.gov.sih.sih26135.dto.request.UpdatePlacementRecordRequest;
import in.gov.sih.sih26135.dto.response.PlacementRecordResponse;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface PlacementRecordService {

  PlacementRecordResponse createPlacementRecord(CreatePlacementRecordRequest request);

  PlacementRecordResponse updatePlacementRecord(Long id, UpdatePlacementRecordRequest request);

  PlacementRecordResponse getPlacementRecordById(Long id);

  PlacementRecordResponse getPlacementRecordByNumber(String placementNumber);

  PlacementRecordResponse getPlacementRecordByJobApplicationId(Long jobApplicationId);

  List<PlacementRecordResponse> getAllPlacementRecords(boolean includeDeleted);

  Page<PlacementRecordResponse> getAllPlacementRecords(boolean includeDeleted, Pageable pageable);

  List<PlacementRecordResponse> getPlacementRecordsByTrainee(Long traineeId);

  Page<PlacementRecordResponse> getPlacementRecordsByTrainee(Long traineeId, Pageable pageable);

  List<PlacementRecordResponse> getPlacementRecordsByEnrollment(Long enrollmentId);

  Page<PlacementRecordResponse> getPlacementRecordsByEnrollment(Long enrollmentId, Pageable pageable);

  List<PlacementRecordResponse> getPlacementRecordsByEmployer(Long employerId);

  Page<PlacementRecordResponse> getPlacementRecordsByEmployer(Long employerId, Pageable pageable);

  List<PlacementRecordResponse> getPlacementRecordsByJobPosting(Long jobPostingId);

  Page<PlacementRecordResponse> getPlacementRecordsByJobPosting(Long jobPostingId, Pageable pageable);

  List<PlacementRecordResponse> getPlacementRecordsByCourse(Long courseId);

  Page<PlacementRecordResponse> getPlacementRecordsByCourse(Long courseId, Pageable pageable);

  List<PlacementRecordResponse> getPlacementRecordsByStatus(Long placementStatusId);

  Page<PlacementRecordResponse> getPlacementRecordsByStatus(Long placementStatusId, Pageable pageable);

  void deletePlacementRecord(Long id);
}
