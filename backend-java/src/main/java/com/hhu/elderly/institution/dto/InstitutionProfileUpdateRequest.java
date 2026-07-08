package com.hhu.elderly.institution.dto;

import java.math.BigDecimal;
import java.util.List;

public record InstitutionProfileUpdateRequest(
        String name,
        String address,
        String addressName,
        String province,
        String city,
        String district,
        Double lon,
        Double lat,
        String pointSource,

        Integer institutionCategory,
        String gradeLevel,
        Integer totalBeds,
        Integer availableBeds,
        BigDecimal monthlyFeeBase,
        Integer priceTier,

        String contactPerson,
        String contactPhone,
        String coverImageUrl,
        String images,
        Boolean hasPanorama,
        String intro,

        List<ServiceItem> services,
        List<FacilityItem> facilities
) {
    public record ServiceItem(
            String serviceType,
            String serviceDetail,
            Boolean isAvailable
    ) {
    }

    public record FacilityItem(
            String facilityName,
            String facilityDesc
    ) {
    }
}
