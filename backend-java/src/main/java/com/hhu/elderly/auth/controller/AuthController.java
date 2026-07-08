package com.hhu.elderly.auth.controller;

import com.hhu.elderly.auth.dto.AuthResponse;
import com.hhu.elderly.auth.dto.AuthUserResponse;
import com.hhu.elderly.auth.dto.LoginRequest;
import com.hhu.elderly.auth.dto.PasswordChangeRequest;
import com.hhu.elderly.auth.dto.RegisterRequest;
import com.hhu.elderly.auth.dto.UserProfileUpdateRequest;
import com.hhu.elderly.auth.security.AuthUserPrincipal;
import com.hhu.elderly.auth.service.AuthService;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(
            AuthService authService
    ) {
        this.authService = authService;
    }

    @GetMapping("/ping")
    public String ping() {
        return "auth controller ok";
    }

    @PostMapping("/register")
    public AuthResponse register(@RequestBody RegisterRequest request) {
        return authService.register(request);
    }

    @PostMapping("/login")
    public AuthResponse login(@RequestBody LoginRequest request) {
        return authService.login(request);
    }

    @GetMapping("/me")
    public AuthUserResponse me(Authentication authentication) {
        AuthUserPrincipal principal = requireUser(authentication);
        return authService.currentUser(principal);
    }

    /**
     * 修改个人资料。
     *
     * 注意：修改昵称后会重新签发 token，
     * 这样后续发帖、评论时 Authentication 里的 nickname 也会同步为新昵称。
     */
    @PutMapping("/profile")
    public AuthResponse updateProfile(
            @RequestBody UserProfileUpdateRequest request,
            Authentication authentication
    ) {
        AuthUserPrincipal principal = requireUser(authentication);
        return authService.updateProfile(request, principal);
    }

    /**
     * 修改当前登录用户密码。
     */
    @PutMapping("/password")
    public Map<String, Object> changePassword(
            @RequestBody PasswordChangeRequest request,
            Authentication authentication
    ) {
        AuthUserPrincipal principal = requireUser(authentication);
        return authService.changePassword(request, principal);
    }

    @PostMapping("/logout")
    public Map<String, Object> logout() {
        return Map.of(
                "success", true,
                "message", "已退出登录"
        );
    }

    private AuthUserPrincipal requireUser(Authentication authentication) {
        if (authentication == null || !(authentication.getPrincipal() instanceof AuthUserPrincipal principal)) {
            throw new IllegalArgumentException("当前未登录。");
        }

        return principal;
    }
}
