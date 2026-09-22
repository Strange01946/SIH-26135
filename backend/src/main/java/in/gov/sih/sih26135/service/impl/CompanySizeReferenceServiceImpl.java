package in.gov.sih.sih26135.service.impl;

import in.gov.sih.sih26135.dto.response.CompanySizeResponse;
import in.gov.sih.sih26135.entity.RefCompanySize;
import in.gov.sih.sih26135.exception.BadRequestException;
import in.gov.sih.sih26135.exception.ResourceNotFoundException;
import in.gov.sih.sih26135.mapper.CompanySizeMapper;
import in.gov.sih.sih26135.repository.RefCompanySizeRepository;
import in.gov.sih.sih26135.service.CompanySizeReferenceService;
import java.util.List;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class CompanySizeReferenceServiceImpl implements CompanySizeReferenceService {

  private final RefCompanySizeRepository refCompanySizeRepository;
  private final CompanySizeMapper companySizeMapper;

  public CompanySizeReferenceServiceImpl(
      RefCompanySizeRepository refCompanySizeRepository,
      CompanySizeMapper companySizeMapper) {
    this.refCompanySizeRepository = refCompanySizeRepository;
    this.companySizeMapper = companySizeMapper;
  }

  @Override
  public List<CompanySizeResponse> getAllCompanySizes() {
    return refCompanySizeRepository.findAll(Sort.by(Sort.Direction.ASC, "sortOrder")).stream()
        .map(companySizeMapper::toResponse)
        .toList();
  }

  @Override
  public CompanySizeResponse getById(Long id) {
    if (id == null) {
      throw new BadRequestException("Company size ID is required");
    }
    RefCompanySize entity = refCompanySizeRepository.findById(id)
        .orElseThrow(() -> new ResourceNotFoundException("RefCompanySize", "id"));
    return companySizeMapper.toResponse(entity);
  }

  @Override
  public CompanySizeResponse getByCode(String sizeCode) {
    if (sizeCode == null || sizeCode.isBlank()) {
      throw new BadRequestException("Size code is required");
    }
    RefCompanySize entity = refCompanySizeRepository.findBySizeCode(sizeCode.trim())
        .orElseThrow(() -> new ResourceNotFoundException("RefCompanySize", "sizeCode"));
    return companySizeMapper.toResponse(entity);
  }
}
