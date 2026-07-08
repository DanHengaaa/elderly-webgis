package com.hhu.elderly.community.dto;

public record CommunityCommentCreateRequest(
        Long parentId,
        String content
) {
}