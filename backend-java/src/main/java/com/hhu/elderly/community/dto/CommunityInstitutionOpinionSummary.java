package com.hhu.elderly.community.dto;

import java.math.BigDecimal;
import java.util.List;

public record CommunityInstitutionOpinionSummary(
        Long institutionId,
        String institutionName,
        Integer postCount,
        BigDecimal avgRating,
        List<String> hotTags,
        List<CommunityInstitutionPostBrief> recentPosts,
        String summary,
        String generatedBy
) {
}