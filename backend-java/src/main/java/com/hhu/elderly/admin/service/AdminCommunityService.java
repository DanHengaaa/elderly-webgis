package com.hhu.elderly.admin.service;

import com.hhu.elderly.admin.dto.AdminCommunityPostItem;
import com.hhu.elderly.admin.repository.AdminCommunityRepository;
import com.hhu.elderly.auth.security.AuthUserPrincipal;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
public class AdminCommunityService {

    private final AdminCommunityRepository adminCommunityRepository;

    public AdminCommunityService(AdminCommunityRepository adminCommunityRepository) {
        this.adminCommunityRepository = adminCommunityRepository;
    }

    public List<AdminCommunityPostItem> findPosts(Integer status) {
        return adminCommunityRepository.findPostsByStatus(status == null ? 0 : status);
    }

    public Map<String, Object> approvePost(
            Long postId,
            AuthUserPrincipal operator
    ) {
        adminCommunityRepository.updatePostStatus(
                postId,
                1,
                operator.getId(),
                operator.getNickname(),
                "审核通过"
        );

        return Map.of(
                "success", true,
                "message", "帖子已审核通过"
        );
    }

    public Map<String, Object> rejectPost(
            Long postId,
            String reason,
            AuthUserPrincipal operator
    ) {
        adminCommunityRepository.updatePostStatus(
                postId,
                2,
                operator.getId(),
                operator.getNickname(),
                reason == null || reason.isBlank() ? "内容不符合社区规范" : reason
        );

        return Map.of(
                "success", true,
                "message", "帖子已驳回"
        );
    }
}