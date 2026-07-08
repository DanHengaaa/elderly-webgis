package com.hhu.elderly.community.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record CommunityInstitutionPostBrief(
        Long id,
        String title,
        String postType,
        String postTypeText,
        String authorName,
        BigDecimal overallRating,
        String contentSummary,
        List<String> tags,
        LocalDateTime createdAt
) {
}