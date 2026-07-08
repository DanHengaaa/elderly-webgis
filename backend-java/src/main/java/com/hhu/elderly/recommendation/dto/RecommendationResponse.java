package com.hhu.elderly.recommendation.dto;

import java.math.BigDecimal;
import java.util.List;

public record RecommendationResponse(
        String message,
        Integer total,
        List<RecommendationItem> items
) {
    public record RecommendationItem(
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
            BigDecimal ratingAvg,
            Integer ratingCount,

            String coverImageUrl,

            Double lon,
            Double lat,

            Double finalScore,
            String matchLevel,
            String matchLevelText,

            Double budgetScore,
            Double bedScore,
            Double ratingScore,
            Double medicalScore,
            Double lifeScore,
            Double greenScore,
            Double visitScore,

            Double estimatedVisitMinutes,

            List<String> reasons
    ) {
    }
}