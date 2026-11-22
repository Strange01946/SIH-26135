package in.gov.sih.sih26135.service;

import in.gov.sih.sih26135.dto.auth.AuthResponse;
import in.gov.sih.sih26135.dto.registration.ApplicationStatusResponse;
import in.gov.sih.sih26135.dto.registration.OnboardingApplicationResponse;
import in.gov.sih.sih26135.dto.registration.RegisterEmployerRequest;
import in.gov.sih.sih26135.dto.registration.RegisterTraineeRequest;
import in.gov.sih.sih26135.dto.registration.RegisterTrainingProviderRequest;
import in.gov.sih.sih26135.dto.registration.RegistrationReferenceDataResponse;

public interface RegistrationService {

  AuthResponse registerTrainee(RegisterTraineeRequest request);

  OnboardingApplicationResponse registerTrainingProvider(RegisterTrainingProviderRequest request);

  OnboardingApplicationResponse registerEmployer(RegisterEmployerRequest request);

  ApplicationStatusResponse getApplicationStatus(String applicationNumber);

  RegistrationReferenceDataResponse getReferenceData();
}
