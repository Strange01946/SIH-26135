package in.gov.sih.sih26135.service;

import in.gov.sih.sih26135.dto.request.ChangeBatchStatusRequest;
import in.gov.sih.sih26135.dto.request.CreateTrainingBatchRequest;
import in.gov.sih.sih26135.dto.request.UpdateTrainingBatchRequest;
import in.gov.sih.sih26135.dto.response.TrainingBatchResponse;
import java.util.List;

public interface TrainingBatchService {

  TrainingBatchResponse getById(Long id);

  TrainingBatchResponse getByCode(String batchCode);

  List<TrainingBatchResponse> getAllBatches();

  List<TrainingBatchResponse> getBatchesByCourseId(Long courseId);

  List<TrainingBatchResponse> getBatchesByProviderId(Long providerId);

  List<TrainingBatchResponse> getBatchesByCenterId(Long centerId);

  List<TrainingBatchResponse> getBatchesByProgramId(Long programId);

  List<TrainingBatchResponse> getBatchesByStatusId(Long batchStatusId);

  TrainingBatchResponse createBatch(CreateTrainingBatchRequest request);

  TrainingBatchResponse updateBatch(Long id, UpdateTrainingBatchRequest request);

  TrainingBatchResponse updateBatchStatus(Long id, ChangeBatchStatusRequest request);

  void deleteBatch(Long id);
}
