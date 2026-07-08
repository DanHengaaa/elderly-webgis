package com.hhu.elderly.community.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public record CommunityPostDetail(
        Long id,
        String title,
        String content,
        String postType,
        String postTypeText,

        Long authorId,
        String authorName,

        Long institutionId,
        String institutionName,
        String institutionAddress,
        String district,
        String careType,
        String careTypeText,

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

        Integer likeCount,
        Integer commentCount,
        Integer collectCount,
        Integer viewCount,

        Boolean likedByCurrentUser,
        Boolean collectedByCurrentUser,

        List<String> imageUrls,
        List<String> tags,

        LocalDateTime createdAt
) {
}