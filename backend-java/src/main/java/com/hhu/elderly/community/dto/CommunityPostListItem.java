package com.hhu.elderly.community.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record CommunityPostListItem(
        Long id,
        String title,
        String contentSummary,
        String postType,
        String postTypeText,
        String authorName,

        Long institutionId,
        String institutionName,
        String district,
        String careType,
        String careTypeText,

        BigDecimal overallRating,

        Integer likeCount,
        Integer commentCount,
        Integer collectCount,
        Integer viewCount,

        String coverImageUrl,
        List<String> tags,

        LocalDateTime createdAt
) {
}