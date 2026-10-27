package in.gov.sih.sih26135.service;

import in.gov.sih.sih26135.dto.request.ChangeBatchStatusRequest;
import in.gov.sih.sih26135.dto.request.CreateTrainingBatchRequest;
import in.gov.sih.sih26135.dto.request.UpdateTrainingBatchRequest;
import in.gov.sih.sih26135.dto.response.TrainingBatchResponse;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface TrainingBatchService {

  TrainingBatchResponse getById(Long id);

  TrainingBatchResponse getByCode(String batchCode);

  List<TrainingBatchResponse> getAllBatches();

  List<TrainingBatchResponse> getBatchesByCourseId(Long courseId);

  List<TrainingBatchResponse> getBatchesByProviderId(Long providerId);

  List<TrainingBatchResponse> getBatchesByCenterId(Long centerId);

  List<TrainingBatchResponse> getBatchesByProgramId(Long programId);

  List<TrainingBatchResponse> getBatchesByStatusId(Long batchStatusId);

  Page<TrainingBatchResponse> getAllBatches(Pageable pageable);

  Page<TrainingBatchResponse> getBatchesByCourseId(Long courseId, Pageable pageable);

  Page<TrainingBatchResponse> getBatchesByProviderId(Long providerId, Pageable pageable);

  Page<TrainingBatchResponse> getBatchesByCenterId(Long centerId, Pageable pageable);

  Page<TrainingBatchResponse> getBatchesByProgramId(Long programId, Pageable pageable);

  Page<TrainingBatchResponse> getBatchesByStatusId(Long batchStatusId, Pageable pageable);

  TrainingBatchResponse createBatch(CreateTrainingBatchRequest request);

  TrainingBatchResponse updateBatch(Long id, UpdateTrainingBatchRequest request);

  TrainingBatchResponse updateBatchStatus(Long id, ChangeBatchStatusRequest request);

  void deleteBatch(Long id);
}
