package com.hhu.elderly.admin.controller;

import com.hhu.elderly.admin.dto.AdminCommunityPostItem;
import com.hhu.elderly.admin.dto.AdminReviewRequest;
import com.hhu.elderly.admin.service.AdminCommunityService;
import com.hhu.elderly.auth.security.AuthUserPrincipal;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/community")
public class AdminCommunityController {

    private final AdminCommunityService adminCommunityService;

    public AdminCommunityController(AdminCommunityService adminCommunityService) {
        this.adminCommunityService = adminCommunityService;
    }

    @GetMapping("/posts")
    public List<AdminCommunityPostItem> posts(
            @RequestParam(required = false, defaultValue = "0") Integer status
    ) {
        return adminCommunityService.findPosts(status);
    }

    @PostMapping("/posts/{id}/approve")
    public Map<String, Object> approve(
            @PathVariable Long id,
            Authentication authentication
    ) {
        AuthUserPrincipal user = (AuthUserPrincipal) authentication.getPrincipal();
        return adminCommunityService.approvePost(id, user);
    }

    @PostMapping("/posts/{id}/reject")
    public Map<String, Object> reject(
            @PathVariable Long id,
            @RequestBody(required = false) AdminReviewRequest request,
            Authentication authentication
    ) {
        AuthUserPrincipal user = (AuthUserPrincipal) authentication.getPrincipal();

        String reason = request == null ? null : request.reason();

        return adminCommunityService.rejectPost(id, reason, user);
    }
}