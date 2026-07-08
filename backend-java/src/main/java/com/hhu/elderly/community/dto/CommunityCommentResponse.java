package com.hhu.elderly.community.dto;

import java.time.LocalDateTime;

public record CommunityCommentResponse(
        Long id,
        Long postId,
        Long parentId,
        Long authorId,
        String authorName,
        String content,
        Integer likeCount,
        LocalDateTime createdAt
) {
}