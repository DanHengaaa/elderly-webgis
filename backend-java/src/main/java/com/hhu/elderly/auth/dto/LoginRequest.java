package com.hhu.elderly.auth.dto;

public record LoginRequest(
        String username,
        String password
) {
}