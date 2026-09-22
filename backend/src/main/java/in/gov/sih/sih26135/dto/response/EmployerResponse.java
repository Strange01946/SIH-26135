package in.gov.sih.sih26135.dto.response;

import java.time.LocalDateTime;

public record EmployerResponse(
    Long id,
    String employerCode,
    String employerName,
    String registrationNumber,
    String gstin,
    Long organizationTypeId,
    Long organizationId,
    Long industryId,
    String industryName,
    Long sectorId,
    String sectorName,
    Long companySizeId,
    String companySizeName,
    String contactPersonName,
    String contactEmail,
    String contactPhone,
    String addressLine1,
    String addressLine2,
    String pincode,
    Long locationId,
    Long stateId,
    Long districtId,
    Long recordVerificationStatusId,
    String recordVerificationStatusCode,
    LocalDateTime verifiedAt,
    Long verifiedByUserId,
    Long lifecycleStatusId,
    LocalDateTime createdAt,
    LocalDateTime updatedAt,
    LocalDateTime deletedAt
) {}
