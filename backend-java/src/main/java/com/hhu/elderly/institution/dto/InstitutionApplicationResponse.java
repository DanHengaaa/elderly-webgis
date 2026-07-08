package com.hhu.elderly.institution.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record InstitutionApplicationResponse(
        Long id,
        Long userId,
        Long institutionId,

        String institutionName,
        String contactPerson,
        String contactPhone,

        String province,
        String city,
        String district,
        String address,
        String addressName,
        Double lon,
        Double lat,
        String pointSource,

        Integer institutionCategory,
        String institutionCategoryText,
        String gradeLevel,
        Integer totalBeds,
        Integer availableBeds,
        BigDecimal monthlyFeeBase,
        Integer priceTier,
        String priceTierText,

        String coverImageUrl,
        String images,
        Boolean hasPanorama,
        String intro,
        String description,

        List<InstitutionApplicationRequest.ServiceItem> services,
        List<InstitutionApplicationRequest.FacilityItem> facilities,

        String licenseUrl,
        String recordCertificateUrl,
        String otherMaterialUrl,

        String status,
        String statusText,
        String rejectReason,
        LocalDateTime createdAt,
        LocalDateTime reviewedAt
) {
}
