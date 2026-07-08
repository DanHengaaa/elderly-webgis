package com.hhu.elderly.auth.dto;

public record AuthUserResponse(
        Long id,
        String username,
        String nickname,
        String phone,
        String email,
        String roleCode,
        String roleName,
        Long institutionId
) {
}