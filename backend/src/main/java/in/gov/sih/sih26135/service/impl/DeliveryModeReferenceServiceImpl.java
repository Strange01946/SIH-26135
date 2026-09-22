package in.gov.sih.sih26135.service.impl;

import in.gov.sih.sih26135.dto.response.DeliveryModeResponse;
import in.gov.sih.sih26135.entity.RefDeliveryMode;
import in.gov.sih.sih26135.exception.ResourceNotFoundException;
import in.gov.sih.sih26135.mapper.DeliveryModeMapper;
import in.gov.sih.sih26135.repository.RefDeliveryModeRepository;
import in.gov.sih.sih26135.service.DeliveryModeReferenceService;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class DeliveryModeReferenceServiceImpl implements DeliveryModeReferenceService {

  private final RefDeliveryModeRepository refDeliveryModeRepository;
  private final DeliveryModeMapper deliveryModeMapper;

  public DeliveryModeReferenceServiceImpl(
      RefDeliveryModeRepository refDeliveryModeRepository,
      DeliveryModeMapper deliveryModeMapper) {
    this.refDeliveryModeRepository = refDeliveryModeRepository;
    this.deliveryModeMapper = deliveryModeMapper;
  }

  @Override
  public List<DeliveryModeResponse> getAllDeliveryModes() {
    return refDeliveryModeRepository.findAll().stream()
        .map(deliveryModeMapper::toResponse)
        .toList();
  }

  @Override
  public DeliveryModeResponse getById(Long id) {
    RefDeliveryMode mode = refDeliveryModeRepository.findById(id)
        .orElseThrow(() -> new ResourceNotFoundException("RefDeliveryMode", "id"));
    return deliveryModeMapper.toResponse(mode);
  }

  @Override
  public DeliveryModeResponse getByCode(String modeCode) {
    RefDeliveryMode mode = refDeliveryModeRepository.findByModeCode(modeCode)
        .orElseThrow(() -> new ResourceNotFoundException("RefDeliveryMode", "modeCode"));
    return deliveryModeMapper.toResponse(mode);
  }
}
