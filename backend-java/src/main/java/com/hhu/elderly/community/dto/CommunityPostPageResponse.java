package com.hhu.elderly.community.dto;

import java.util.List;

public record CommunityPostPageResponse(
        Integer page,
        Integer size,
        Long total,
        List<CommunityPostListItem> items
) {
}