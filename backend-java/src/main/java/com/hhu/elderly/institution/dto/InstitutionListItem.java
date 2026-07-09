package com.hhu.elderly.institution.dto;

import java.math.BigDecimal;

public record InstitutionListItem(
        Long id,
        String name,
        String address,
        String district,

        Integer institutionCategory,
        String institutionCategoryText,

        String gradeLevel,
        Integer totalBeds,
        Integer availableBeds,

        BigDecimal monthlyFeeBase,
        Integer priceTier,
        String priceTierText,

        String serviceTypes,
        String serviceTypeText,

        BigDecimal ratingAvg,
        Integer ratingCount,

        String coverImageUrl,

        Double lon,
        Double lat
) {
}