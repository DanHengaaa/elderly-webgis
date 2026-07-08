package com.hhu.elderly.auth.dto;

public record UserProfileUpdateRequest(
        String nickname,
        String phone,
        String email
) {
}
