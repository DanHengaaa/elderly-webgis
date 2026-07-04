package com.hhu.elderly.institution.dto;

import java.math.BigDecimal;
import java.util.List;

public record InstitutionDetailResponse(
        Long id,
        String fid,
        Long objectId,

        String name,
        String address,
        String province,
        String city,
        String district,

        String poiTypeFull,
        String poiBig,
        String poiMid,
        String poiSmall,

        Double lonGcj02,
        Double latGcj02,
        Double lonWgs84,
        Double latWgs84,
        Double lon,
        Double lat,

        String geomWkt,
        String geomProjWkt,

        Integer institutionCategory,
        String institutionCategoryText,

        String gradeLevel,

        Integer totalBeds,
        Integer availableBeds,

        BigDecimal monthlyFeeBase,
        Integer priceTier,
        String priceTierText,

        String contactPerson,
        String contactPhone,

        Integer status,
        String statusText,

        Long ownerUserId,

        BigDecimal ratingAvg,
        Integer ratingCount,

        String coverImageUrl,
        String images,
        Boolean hasPanorama,
        String intro,

        String extraData,

        String createdAt,
        String updatedAt,

        List<ServiceItem> services,
        List<FacilityItem> facilities,
        List<ReviewItem> reviews
) {
    public record ServiceItem(
            Long id,
            String serviceType,
            String serviceDetail,
            Boolean isAvailable
    ) {
    }

    public record FacilityItem(
            Long id,
            String facilityName,
            String facilityDesc
    ) {
    }

    public record ReviewItem(
            Long id,
            Long userId,
            BigDecimal ratingAvg,
            BigDecimal ratingMedical,
            BigDecimal ratingHardware,
            BigDecimal ratingFood,
            BigDecimal ratingService,
            String content,
            String images,
            Boolean isApproved,
            String createdAt
    ) {
    }
}