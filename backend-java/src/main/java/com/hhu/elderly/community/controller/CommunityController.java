package com.hhu.elderly.community.controller;

import com.hhu.elderly.auth.security.AuthUserPrincipal;
import com.hhu.elderly.community.dto.*;
import com.hhu.elderly.community.service.CommunityService;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/community")
public class CommunityController {

    private final CommunityService communityService;

    public CommunityController(CommunityService communityService) {
        this.communityService = communityService;
    }

    @GetMapping("/ping")
    public String ping() {
        return "community controller ok";
    }

    @GetMapping("/posts")
    public CommunityPostPageResponse posts(
            @RequestParam(required = false, defaultValue = "all") String type,
            @RequestParam(required = false) String district,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Long institutionId,
            @RequestParam(required = false, defaultValue = "latest") String sort,
            @RequestParam(required = false, defaultValue = "1") Integer page,
            @RequestParam(required = false, defaultValue = "12") Integer size
    ) {
        return communityService.findPosts(
                type,
                district,
                keyword,
                institutionId,
                sort,
                page,
                size
        );
    }

    @GetMapping("/posts/{id}")
    public CommunityPostDetail postDetail(
            @PathVariable Long id,
            Authentication authentication
    ) {
        Long currentUserId = getCurrentUserId(authentication);
        return communityService.findPostDetail(id, currentUserId);
    }

    @PostMapping("/posts")
    public Map<String, Object> createPost(
            @RequestBody CommunityPostCreateRequest request,
            Authentication authentication
    ) {
        AuthUserPrincipal user = requireUser(authentication);
        return communityService.createPost(request, user);
    }

    @DeleteMapping("/posts/{id}")
    public Map<String, Object> deletePost(
            @PathVariable Long id,
            Authentication authentication
    ) {
        AuthUserPrincipal user = requireUser(authentication);
        return communityService.deletePost(id, user);
    }

    @PostMapping("/posts/{id}/like")
    public Map<String, Object> toggleLike(
            @PathVariable Long id,
            Authentication authentication
    ) {
        AuthUserPrincipal user = requireUser(authentication);
        return communityService.toggleLike(id, user);
    }

    @PostMapping("/posts/{id}/collect")
    public Map<String, Object> toggleCollect(
            @PathVariable Long id,
            Authentication authentication
    ) {
        AuthUserPrincipal user = requireUser(authentication);
        return communityService.toggleCollect(id, user);
    }

    @GetMapping("/posts/{id}/comments")
    public List<CommunityCommentResponse> comments(@PathVariable Long id) {
        return communityService.findComments(id);
    }

    @PostMapping("/posts/{id}/comments")
    public Map<String, Object> createComment(
            @PathVariable Long id,
            @RequestBody CommunityCommentCreateRequest request,
            Authentication authentication
    ) {
        AuthUserPrincipal user = requireUser(authentication);
        return communityService.createComment(id, request, user);
    }

    @DeleteMapping("/posts/{postId}/comments/{commentId}")
    public Map<String, Object> deleteComment(
            @PathVariable Long postId,
            @PathVariable Long commentId,
            Authentication authentication
    ) {
        AuthUserPrincipal user = requireUser(authentication);
        return communityService.deleteComment(postId, commentId, user);
    }

    @GetMapping("/institutions/options")
    public List<CommunityInstitutionOption> institutionOptions(
            @RequestParam(required = false) String keyword
    ) {
        return communityService.findInstitutionOptions(keyword);
    }

    @GetMapping("/institutions/{institutionId}/posts")
    public List<CommunityInstitutionPostBrief> institutionPosts(
            @PathVariable Long institutionId
    ) {
        return communityService.findInstitutionPosts(institutionId);
    }

    @GetMapping("/institutions/counts")
    public List<CommunityInstitutionPostCount> institutionPostCounts(
            @RequestParam String ids
    ) {
        List<Long> institutionIds = Arrays.stream(ids.split(","))
                .map(String::trim)
                .filter(item -> !item.isBlank())
                .map(Long::valueOf)
                .toList();

        return communityService.findPostCountsByInstitutionIds(institutionIds);
    }

    @GetMapping("/institutions/{institutionId}/summary")
    public CommunityInstitutionOpinionSummary institutionOpinionSummary(
            @PathVariable Long institutionId,
            @RequestParam(required = false, defaultValue = "deepseek") String providerId
    ) {
        return communityService.getInstitutionOpinionSummary(institutionId, providerId);
    }

    @GetMapping("/heat")
    public List<CommunityHeatPoint> heatPoints() {
        return communityService.findCommunityHeatPoints();
    }

    private AuthUserPrincipal requireUser(Authentication authentication) {
        if (authentication == null || !(authentication.getPrincipal() instanceof AuthUserPrincipal user)) {
            throw new IllegalArgumentException("当前未登录。");
        }

        return user;
    }

    private Long getCurrentUserId(Authentication authentication) {
        if (authentication == null || !(authentication.getPrincipal() instanceof AuthUserPrincipal user)) {
            return null;
        }

        return user.getId();
    }
}