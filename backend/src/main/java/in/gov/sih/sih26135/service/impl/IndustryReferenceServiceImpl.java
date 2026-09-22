package in.gov.sih.sih26135.service.impl;

import in.gov.sih.sih26135.dto.response.IndustryResponse;
import in.gov.sih.sih26135.entity.Industry;
import in.gov.sih.sih26135.exception.ResourceNotFoundException;
import in.gov.sih.sih26135.mapper.IndustryMapper;
import in.gov.sih.sih26135.repository.IndustryRepository;
import in.gov.sih.sih26135.service.IndustryReferenceService;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class IndustryReferenceServiceImpl implements IndustryReferenceService {

  private final IndustryRepository industryRepository;
  private final IndustryMapper industryMapper;

  public IndustryReferenceServiceImpl(IndustryRepository industryRepository, IndustryMapper industryMapper) {
    this.industryRepository = industryRepository;
    this.industryMapper = industryMapper;
  }

  @Override
  public List<IndustryResponse> getAllIndustries() {
    return industryRepository.findAll().stream()
        .map(industryMapper::toResponse)
        .toList();
  }

  @Override
  public IndustryResponse getById(Long id) {
    Industry industry = industryRepository.findById(id)
        .orElseThrow(() -> new ResourceNotFoundException("Industry", "id"));
    return industryMapper.toResponse(industry);
  }

  @Override
  public IndustryResponse getByCode(String industryCode) {
    Industry industry = industryRepository.findByIndustryCode(industryCode)
        .orElseThrow(() -> new ResourceNotFoundException("Industry", "industryCode"));
    return industryMapper.toResponse(industry);
  }

  @Override
  public List<IndustryResponse> getIndustriesBySectorId(Long sectorId) {
    return industryRepository.findBySectorId(sectorId).stream()
        .map(industryMapper::toResponse)
        .toList();
  }
}
