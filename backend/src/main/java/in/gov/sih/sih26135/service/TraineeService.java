package in.gov.sih.sih26135.service;

import in.gov.sih.sih26135.dto.request.CreateTraineeRequest;
import in.gov.sih.sih26135.dto.request.UpdateTraineeRequest;
import in.gov.sih.sih26135.dto.response.TraineeResponse;
import java.util.List;

public interface TraineeService {

  TraineeResponse getById(Long id);

  TraineeResponse getByRegistrationNumber(String registrationNumber);

  TraineeResponse getByUserId(Long userId);

  List<TraineeResponse> getAllTrainees();

  TraineeResponse createTrainee(CreateTraineeRequest request, Long actorUserId);

  TraineeResponse updateTrainee(Long id, UpdateTraineeRequest request, Long actorUserId);

  void deleteTrainee(Long id, Long actorUserId);
}
