package in.gov.sih.sih26135.service.impl;

import in.gov.sih.sih26135.dto.response.SectorResponse;
import in.gov.sih.sih26135.entity.Sector;
import in.gov.sih.sih26135.exception.ResourceNotFoundException;
import in.gov.sih.sih26135.mapper.SectorMapper;
import in.gov.sih.sih26135.repository.SectorRepository;
import in.gov.sih.sih26135.service.SectorReferenceService;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class SectorReferenceServiceImpl implements SectorReferenceService {

  private final SectorRepository sectorRepository;
  private final SectorMapper sectorMapper;

  public SectorReferenceServiceImpl(SectorRepository sectorRepository, SectorMapper sectorMapper) {
    this.sectorRepository = sectorRepository;
    this.sectorMapper = sectorMapper;
  }

  @Override
  public List<SectorResponse> getAllSectors() {
    return sectorRepository.findAll().stream()
        .map(sectorMapper::toResponse)
        .toList();
  }

  @Override
  public SectorResponse getById(Long id) {
    Sector sector = sectorRepository.findById(id)
        .orElseThrow(() -> new ResourceNotFoundException("Sector", "id"));
    return sectorMapper.toResponse(sector);
  }

  @Override
  public SectorResponse getByCode(String sectorCode) {
    Sector sector = sectorRepository.findBySectorCode(sectorCode)
        .orElseThrow(() -> new ResourceNotFoundException("Sector", "sectorCode"));
    return sectorMapper.toResponse(sector);
  }
}
