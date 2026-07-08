package com.hhu.elderly.auth.dto;

public record RegisterRequest(
        String username,
        String password,
        String nickname,
        String phone,
        String roleCode
) {
}