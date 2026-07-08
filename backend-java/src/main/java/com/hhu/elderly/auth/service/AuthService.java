package com.hhu.elderly.auth.service;

import com.hhu.elderly.auth.dto.AuthResponse;
import com.hhu.elderly.auth.dto.AuthUserResponse;
import com.hhu.elderly.auth.dto.LoginRequest;
import com.hhu.elderly.auth.dto.PasswordChangeRequest;
import com.hhu.elderly.auth.dto.RegisterRequest;
import com.hhu.elderly.auth.dto.UserProfileUpdateRequest;
import com.hhu.elderly.auth.repository.AuthRepository;
import com.hhu.elderly.auth.security.AuthUserPrincipal;
import com.hhu.elderly.auth.security.JwtTokenProvider;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

@Service
public class AuthService {

    private final AuthRepository authRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;

    public AuthService(
            AuthRepository authRepository,
            PasswordEncoder passwordEncoder,
            JwtTokenProvider jwtTokenProvider
    ) {
        this.authRepository = authRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtTokenProvider = jwtTokenProvider;
    }

    public AuthResponse register(RegisterRequest request) {
        String username = safeTrim(request.username());
        String password = safeTrim(request.password());
        String nickname = safeTrim(request.nickname());
        String phone = safeTrim(request.phone());

        if (username == null || username.length() < 3) {
            throw new IllegalArgumentException("用户名至少需要 3 个字符。");
        }

        if (password == null || password.length() < 6) {
            throw new IllegalArgumentException("密码至少需要 6 个字符。");
        }

        if (authRepository.existsByUsername(username)) {
            throw new IllegalArgumentException("用户名已存在，请更换用户名。");
        }

        String roleCode = normalizeRoleCode(request.roleCode());

        authRepository.createUser(
                username,
                passwordEncoder.encode(password),
                nickname == null || nickname.isBlank() ? username : nickname,
                phone,
                roleCode
        );

        return login(new LoginRequest(username, password));
    }

    public AuthResponse login(LoginRequest request) {
        String username = safeTrim(request.username());
        String password = safeTrim(request.password());

        if (username == null || password == null) {
            throw new IllegalArgumentException("请输入用户名和密码。");
        }

        AuthRepository.UserPasswordRecord record =
                authRepository.findPasswordRecord(username);

        if (record == null) {
            authRepository.saveLoginLog(
                    null,
                    username,
                    null,
                    false,
                    "用户不存在"
            );

            throw new IllegalArgumentException("用户名或密码错误。");
        }

        if (record.status() == null || record.status() != 1) {
            authRepository.saveLoginLog(
                    record.id(),
                    record.username(),
                    record.roleCode(),
                    false,
                    "账号已被禁用"
            );

            throw new IllegalArgumentException("账号已被禁用，请联系管理员。");
        }

        if (!passwordEncoder.matches(password, record.passwordHash())) {
            authRepository.saveLoginLog(
                    record.id(),
                    record.username(),
                    record.roleCode(),
                    false,
                    "密码错误"
            );

            throw new IllegalArgumentException("用户名或密码错误。");
        }

        AuthUserPrincipal principal = authRepository.toPrincipal(record);
        String token = jwtTokenProvider.generateToken(principal);

        authRepository.saveLoginLog(
                record.id(),
                record.username(),
                record.roleCode(),
                true,
                "登录成功"
        );

        return new AuthResponse(
                token,
                authRepository.toUserResponse(record)
        );
    }

    public AuthUserResponse currentUser(AuthUserPrincipal principal) {
        if (principal == null || principal.getId() == null) {
            throw new IllegalArgumentException("当前未登录。");
        }

        return authRepository.findUserResponseById(principal.getId());
    }

    @Transactional
    public AuthResponse updateProfile(
            UserProfileUpdateRequest request,
            AuthUserPrincipal user
    ) {
        if (user == null || user.getId() == null) {
            throw new IllegalArgumentException("当前未登录。");
        }

        String nickname = safeTrim(request.nickname());
        String phone = safeTrim(request.phone());
        String email = safeTrim(request.email());

        authRepository.updateProfile(
                user.getId(),
                nickname,
                phone,
                email
        );

        AuthUserResponse updatedUser =
                authRepository.findUserResponseById(user.getId());

        String newAuthorName = hasText(updatedUser.nickname())
                ? updatedUser.nickname()
                : updatedUser.username();

        authRepository.syncCommunityAuthorName(
                updatedUser.id(),
                newAuthorName
        );

        AuthUserPrincipal newPrincipal = new AuthUserPrincipal(
                updatedUser.id(),
                updatedUser.username(),
                updatedUser.nickname(),
                updatedUser.roleCode(),
                updatedUser.institutionId()
        );

        String token = jwtTokenProvider.generateToken(newPrincipal);

        return new AuthResponse(
                token,
                updatedUser
        );
    }

    @Transactional
    public Map<String, Object> changePassword(
            PasswordChangeRequest request,
            AuthUserPrincipal user
    ) {
        if (user == null || user.getId() == null) {
            throw new IllegalArgumentException("当前未登录。");
        }

        String oldPassword = safeTrim(request.oldPassword());
        String newPassword = safeTrim(request.newPassword());

        if (!hasText(oldPassword)) {
            throw new IllegalArgumentException("请输入原密码。");
        }

        if (!hasText(newPassword) || newPassword.length() < 6) {
            throw new IllegalArgumentException("新密码长度不能少于 6 位。");
        }

        AuthRepository.UserPasswordRecord record =
                authRepository.findPasswordRecord(user.getUsername());

        if (record == null) {
            throw new IllegalArgumentException("当前用户不存在。");
        }

        if (!record.id().equals(user.getId())) {
            throw new IllegalArgumentException("登录状态异常，请重新登录。");
        }

        if (!passwordEncoder.matches(oldPassword, record.passwordHash())) {
            throw new IllegalArgumentException("原密码不正确。");
        }

        authRepository.updatePassword(
                user.getId(),
                passwordEncoder.encode(newPassword)
        );

        return Map.of(
                "success", true,
                "message", "密码修改成功"
        );
    }

    private String normalizeRoleCode(String roleCode) {
        if (roleCode == null || roleCode.isBlank()) {
            return "CUSTOMER";
        }

        return switch (roleCode.trim().toUpperCase()) {
            case "CUSTOMER" -> "CUSTOMER";
            case "INSTITUTION" -> "INSTITUTION";
            default -> "CUSTOMER";
        };
    }

    private String safeTrim(String value) {
        if (value == null) {
            return null;
        }

        return value.trim();
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}