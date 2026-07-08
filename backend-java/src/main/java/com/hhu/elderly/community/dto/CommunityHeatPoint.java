package com.hhu.elderly.community.dto;

import java.math.BigDecimal;

public record CommunityHeatPoint(
        Long institutionId,
        String institutionName,
        Double lon,
        Double lat,
        Integer postCount,
        BigDecimal avgRating
) {
}