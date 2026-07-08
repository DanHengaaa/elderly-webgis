package com.hhu.elderly.auth.dto;

public record AuthResponse(
        String token,
        AuthUserResponse user
) {
}