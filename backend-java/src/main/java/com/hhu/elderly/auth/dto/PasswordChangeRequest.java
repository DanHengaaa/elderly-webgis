package com.hhu.elderly.auth.dto;

public record PasswordChangeRequest(
        String oldPassword,
        String newPassword,
        String confirmPassword
) {
}
