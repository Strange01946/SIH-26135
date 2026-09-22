package in.gov.sih.sih26135.service;

import in.gov.sih.sih26135.dto.response.SectorResponse;
import java.util.List;

public interface SectorReferenceService {

  List<SectorResponse> getAllSectors();

  SectorResponse getById(Long id);

  SectorResponse getByCode(String sectorCode);
}
