package com.hhu.elderly.community.service;

import com.hhu.elderly.ai.service.LlmClientService;
import com.hhu.elderly.auth.security.AuthUserPrincipal;
import com.hhu.elderly.community.dto.*;
import com.hhu.elderly.community.repository.CommunityRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class CommunityService {

    private final CommunityRepository communityRepository;
    private final LlmClientService llmClientService;

    public CommunityService(
            CommunityRepository communityRepository,
            LlmClientService llmClientService
    ) {
        this.communityRepository = communityRepository;
        this.llmClientService = llmClientService;
    }

    public CommunityPostPageResponse findPosts(
            String type,
            String district,
            String keyword,
            Long institutionId,
            String sort,
            Integer page,
            Integer size
    ) {
        return communityRepository.findPosts(
                type,
                district,
                keyword,
                institutionId,
                sort,
                page,
                size
        );
    }

    public CommunityPostDetail findPostDetail(
            Long id,
            Long currentUserId
    ) {
        return communityRepository.findPostDetail(id, currentUserId);
    }

    public Map<String, Object> createPost(
            CommunityPostCreateRequest request,
            AuthUserPrincipal user
    ) {
        Long id = communityRepository.createPost(
                request,
                user.getId(),
                getDisplayName(user),
                0
        );

        return Map.of(
                "success", true,
                "id", id,
                "status", 0,
                "message", "发布成功，内容已进入管理员审核队列。"
        );
    }

    public Map<String, Object> deletePost(
            Long postId,
            AuthUserPrincipal user
    ) {
        communityRepository.deletePost(postId, user.getId());

        return Map.of(
                "success", true,
                "message", "帖子已删除"
        );
    }

    public List<CommunityCommentResponse> findComments(Long postId) {
        return communityRepository.findComments(postId);
    }

    public Map<String, Object> createComment(
            Long postId,
            CommunityCommentCreateRequest request,
            AuthUserPrincipal user
    ) {
        Long id = communityRepository.createComment(
                postId,
                request,
                user.getId(),
                getDisplayName(user)
        );

        return Map.of(
                "success", true,
                "id", id,
                "message", "评论成功"
        );
    }

    public Map<String, Object> deleteComment(
            Long postId,
            Long commentId,
            AuthUserPrincipal user
    ) {
        communityRepository.deleteComment(
                postId,
                commentId,
                user.getId()
        );

        return Map.of(
                "success", true,
                "message", "评论已删除"
        );
    }

    public Map<String, Object> toggleLike(
            Long postId,
            AuthUserPrincipal user
    ) {
        boolean liked = communityRepository.toggleLike(
                postId,
                user.getId()
        );

        int likeCount = communityRepository.countLikes(postId);

        return Map.of(
                "success", true,
                "liked", liked,
                "likeCount", likeCount,
                "message", liked ? "点赞成功" : "已取消点赞"
        );
    }

    public Map<String, Object> toggleCollect(
            Long postId,
            AuthUserPrincipal user
    ) {
        boolean collected = communityRepository.toggleCollect(
                postId,
                user.getId()
        );

        int collectCount = communityRepository.countCollections(postId);

        return Map.of(
                "success", true,
                "collected", collected,
                "collectCount", collectCount,
                "message", collected ? "收藏成功" : "已取消收藏"
        );
    }

    public List<CommunityInstitutionOption> findInstitutionOptions(String keyword) {
        return communityRepository.findInstitutionOptions(keyword);
    }

    public List<CommunityInstitutionPostBrief> findInstitutionPosts(Long institutionId) {
        return communityRepository.findInstitutionPosts(institutionId, 6);
    }

    public List<CommunityInstitutionPostCount> findPostCountsByInstitutionIds(
            List<Long> institutionIds
    ) {
        return communityRepository.findPostCountsByInstitutionIds(institutionIds);
    }

    public CommunityInstitutionOpinionSummary getInstitutionOpinionSummary(
            Long institutionId,
            String providerId
    ) {
        Map<String, Object> base = communityRepository.findInstitutionCommunityBase(institutionId);

        Long id = ((Number) base.get("institution_id")).longValue();
        String institutionName = String.valueOf(base.get("institution_name"));

        Integer postCount = base.get("post_count") == null
                ? 0
                : ((Number) base.get("post_count")).intValue();

        BigDecimal avgRating = null;
        if (base.get("avg_rating") instanceof BigDecimal value) {
            avgRating = value;
        }

        List<String> hotTags = splitTags(String.valueOf(base.get("tags_text")));

        List<CommunityInstitutionPostBrief> recentPosts =
                communityRepository.findInstitutionPosts(institutionId, 5);

        String summary = buildRuleCommunitySummary(
                institutionName,
                postCount,
                avgRating,
                hotTags
        );

        String aiSummary = llmClientService.generateCommunityOpinionSummary(
                institutionName,
                postCount,
                avgRating,
                recentPosts,
                providerId == null || providerId.isBlank() ? "deepseek" : providerId
        );

        String generatedBy = "系统规则引擎";

        if (aiSummary != null && !aiSummary.isBlank()) {
            summary = aiSummary;
            generatedBy = "DeepSeek";
        }

        return new CommunityInstitutionOpinionSummary(
                id,
                institutionName,
                postCount,
                avgRating,
                hotTags,
                recentPosts,
                summary,
                generatedBy
        );
    }

    public List<CommunityHeatPoint> findCommunityHeatPoints() {
        return communityRepository.findCommunityHeatPoints();
    }

    private String getDisplayName(AuthUserPrincipal user) {
        if (user.getNickname() != null && !user.getNickname().isBlank()) {
            return user.getNickname();
        }

        return user.getUsername();
    }

    private String buildRuleCommunitySummary(
            String institutionName,
            Integer postCount,
            BigDecimal avgRating,
            List<String> hotTags
    ) {
        if (postCount == null || postCount == 0) {
            return "当前该机构暂无社区笔记，建议用户结合机构详情、智能评估结果和实地探访进一步判断。";
        }

        StringBuilder builder = new StringBuilder();

        builder.append("根据社区笔记，")
                .append(institutionName)
                .append("目前共有 ")
                .append(postCount)
                .append(" 条相关内容。");

        if (avgRating != null) {
            builder.append("社区平均评分约为 ")
                    .append(avgRating)
                    .append(" 分。");
        }

        if (hotTags != null && !hotTags.isEmpty()) {
            builder.append("用户常提到的关键词包括：")
                    .append(String.join("、", hotTags.stream().limit(5).toList()))
                    .append("。");
        }

        builder.append("由于社区内容属于用户经验分享，建议结合电话咨询、实地探访和平台空间评估结果综合判断。");

        return builder.toString();
    }

    private List<String> splitTags(String text) {
        if (text == null || text.isBlank() || "null".equalsIgnoreCase(text)) {
            return List.of();
        }

        List<String> result = new ArrayList<>();

        for (String item : text.split(",")) {
            String value = item.trim();

            if (!value.isBlank() && !result.contains(value)) {
                result.add(value);
            }
        }

        return result;
    }
}