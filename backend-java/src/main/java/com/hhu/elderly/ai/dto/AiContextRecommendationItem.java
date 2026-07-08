package com.hhu.elderly.ai.dto;

import java.math.BigDecimal;
import java.util.List;

public record AiContextRecommendationItem(
        Long id,
        String name,
        String address,
        String district,
        String institutionCategoryText,

        BigDecimal monthlyFeeBase,
        Integer totalBeds,
        Integer availableBeds,

        Double finalScore,
        String matchLevelText,

        Double budgetScore,
        Double medicalScore,
        Double lifeScore,
        Double greenScore,
        Double visitScore,
        Double estimatedVisitMinutes,

        List<String> reasons
) {
}