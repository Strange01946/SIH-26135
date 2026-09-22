package in.gov.sih.sih26135.service.impl;

import in.gov.sih.sih26135.dto.response.SalaryFrequencyResponse;
import in.gov.sih.sih26135.entity.RefSalaryFrequency;
import in.gov.sih.sih26135.exception.BadRequestException;
import in.gov.sih.sih26135.exception.ResourceNotFoundException;
import in.gov.sih.sih26135.mapper.SalaryFrequencyMapper;
import in.gov.sih.sih26135.repository.RefSalaryFrequencyRepository;
import in.gov.sih.sih26135.service.SalaryFrequencyReferenceService;
import java.util.List;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class SalaryFrequencyReferenceServiceImpl implements SalaryFrequencyReferenceService {

  private final RefSalaryFrequencyRepository refSalaryFrequencyRepository;
  private final SalaryFrequencyMapper salaryFrequencyMapper;

  public SalaryFrequencyReferenceServiceImpl(
      RefSalaryFrequencyRepository refSalaryFrequencyRepository,
      SalaryFrequencyMapper salaryFrequencyMapper) {
    this.refSalaryFrequencyRepository = refSalaryFrequencyRepository;
    this.salaryFrequencyMapper = salaryFrequencyMapper;
  }

  @Override
  public List<SalaryFrequencyResponse> getAllSalaryFrequencies() {
    return refSalaryFrequencyRepository.findAll(Sort.by(Sort.Direction.ASC, "sortOrder")).stream()
        .map(salaryFrequencyMapper::toResponse)
        .toList();
  }

  @Override
  public SalaryFrequencyResponse getById(Long id) {
    if (id == null) {
      throw new BadRequestException("Salary frequency ID is required");
    }
    RefSalaryFrequency entity = refSalaryFrequencyRepository.findById(id)
        .orElseThrow(() -> new ResourceNotFoundException("RefSalaryFrequency", "id"));
    return salaryFrequencyMapper.toResponse(entity);
  }

  @Override
  public SalaryFrequencyResponse getByCode(String frequencyCode) {
    if (frequencyCode == null || frequencyCode.isBlank()) {
      throw new BadRequestException("Frequency code is required");
    }
    RefSalaryFrequency entity = refSalaryFrequencyRepository.findByFrequencyCode(frequencyCode.trim())
        .orElseThrow(() -> new ResourceNotFoundException("RefSalaryFrequency", "frequencyCode"));
    return salaryFrequencyMapper.toResponse(entity);
  }
}
