package com.hhu.elderly.community.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record CommunityPostCreateRequest(
        String title,
        String content,
        String postType,

        Long institutionId,
        String district,
        String careType,

        BigDecimal budgetMin,
        BigDecimal budgetMax,

        LocalDate visitDate,
        Boolean isFieldVisit,

        BigDecimal overallRating,
        BigDecimal environmentRating,
        BigDecimal careRating,
        BigDecimal medicalRating,
        BigDecimal lifeRating,
        BigDecimal visitRating,
        BigDecimal priceTransparencyRating,

        List<String> imageUrls,
        List<String> tags
) {
}