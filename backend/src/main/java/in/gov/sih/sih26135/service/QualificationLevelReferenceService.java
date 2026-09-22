package in.gov.sih.sih26135.service;

import in.gov.sih.sih26135.dto.response.QualificationLevelResponse;
import java.util.List;

public interface QualificationLevelReferenceService {

  List<QualificationLevelResponse> getAllQualificationLevels();

  QualificationLevelResponse getById(Long id);

  QualificationLevelResponse getByCode(String levelCode);
}
