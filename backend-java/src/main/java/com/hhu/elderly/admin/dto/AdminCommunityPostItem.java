package com.hhu.elderly.admin.dto;

import java.time.LocalDateTime;

public record AdminCommunityPostItem(
        Long id,
        String title,
        String contentSummary,
        String postType,
        String postTypeText,
        String authorName,
        Integer status,
        String statusText,
        LocalDateTime createdAt
) {
}