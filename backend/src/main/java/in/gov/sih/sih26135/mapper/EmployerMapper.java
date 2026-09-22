package in.gov.sih.sih26135.mapper;

import in.gov.sih.sih26135.dto.request.CreateEmployerRequest;
import in.gov.sih.sih26135.dto.response.EmployerResponse;
import in.gov.sih.sih26135.entity.Employer;
import in.gov.sih.sih26135.entity.Industry;
import in.gov.sih.sih26135.entity.RefCompanySize;
import in.gov.sih.sih26135.entity.RefRecordVerificationStatus;
import in.gov.sih.sih26135.entity.Sector;
import org.springframework.stereotype.Component;

@Component
public class EmployerMapper {

  public EmployerResponse toResponse(Employer entity) {
    if (entity == null) {
      return null;
    }

    Long industryId = null;
    String industryName = null;
    if (entity.getIndustry() != null) {
      industryId = entity.getIndustry().getId();
      industryName = entity.getIndustry().getIndustryName();
    }

    Long sectorId = null;
    String sectorName = null;
    if (entity.getSector() != null) {
      sectorId = entity.getSector().getId();
      sectorName = entity.getSector().getSectorName();
    }

    Long companySizeId = null;
    String companySizeName = null;
    if (entity.getCompanySize() != null) {
      companySizeId = entity.getCompanySize().getId();
      companySizeName = entity.getCompanySize().getSizeName();
    }

    Long recordVerificationStatusId = null;
    String recordVerificationStatusCode = null;
    if (entity.getRecordVerificationStatus() != null) {
      recordVerificationStatusId = entity.getRecordVerificationStatus().getId();
      recordVerificationStatusCode = entity.getRecordVerificationStatus().getStatusCode();
    }

    return new EmployerResponse(
        entity.getId(),
        entity.getEmployerCode(),
        entity.getEmployerName(),
        entity.getRegistrationNumber(),
        entity.getGstin(),
        entity.getOrganizationTypeId(),
        entity.getOrganizationId(),
        industryId,
        industryName,
        sectorId,
        sectorName,
        companySizeId,
        companySizeName,
        entity.getContactPersonName(),
        entity.getContactEmail(),
        entity.getContactPhone(),
        entity.getAddressLine1(),
        entity.getAddressLine2(),
        entity.getPincode(),
        entity.getLocationId(),
        entity.getStateId(),
        entity.getDistrictId(),
        recordVerificationStatusId,
        recordVerificationStatusCode,
        entity.getVerifiedAt(),
        entity.getVerifiedByUserId(),
        entity.getLifecycleStatusId(),
        entity.getCreatedAt(),
        entity.getUpdatedAt(),
        entity.getDeletedAt()
    );
  }

  public Employer toEntity(
      CreateEmployerRequest request,
      Industry industry,
      Sector sector,
      RefCompanySize companySize,
      RefRecordVerificationStatus recordVerificationStatus) {
    if (request == null) {
      return null;
    }

    Employer entity = new Employer();
    entity.setEmployerCode(request.getEmployerCode());
    entity.setEmployerName(request.getEmployerName());
    entity.setRegistrationNumber(request.getRegistrationNumber());
    entity.setGstin(request.getGstin());
    entity.setOrganizationTypeId(request.getOrganizationTypeId());
    entity.setOrganizationId(request.getOrganizationId());
    entity.setIndustry(industry);
    entity.setSector(sector);
    entity.setCompanySize(companySize);
    entity.setContactPersonName(request.getContactPersonName());
    entity.setContactEmail(request.getContactEmail());
    entity.setContactPhone(request.getContactPhone());
    entity.setAddressLine1(request.getAddressLine1());
    entity.setAddressLine2(request.getAddressLine2());
    entity.setPincode(request.getPincode());
    entity.setLocationId(request.getLocationId());
    entity.setStateId(request.getStateId());
    entity.setDistrictId(request.getDistrictId());
    entity.setRecordVerificationStatus(recordVerificationStatus);
    entity.setVerifiedAt(request.getVerifiedAt());
    entity.setVerifiedByUserId(request.getVerifiedByUserId());
    entity.setLifecycleStatusId(request.getLifecycleStatusId() != null ? request.getLifecycleStatusId() : 1L);
    return entity;
  }
}
