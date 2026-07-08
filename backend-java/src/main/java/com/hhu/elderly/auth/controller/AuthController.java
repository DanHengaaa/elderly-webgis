package com.hhu.elderly.auth.controller;

import com.hhu.elderly.auth.dto.AuthResponse;
import com.hhu.elderly.auth.dto.AuthUserResponse;
import com.hhu.elderly.auth.dto.LoginRequest;
import com.hhu.elderly.auth.dto.RegisterRequest;
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
        if (authentication == null || authentication.getPrincipal() == null) {
            throw new IllegalArgumentException("当前未登录。");
        }

        AuthUserPrincipal principal = (AuthUserPrincipal) authentication.getPrincipal();

        return authService.currentUser(principal);
    }

    @PostMapping("/logout")
    public Map<String, Object> logout() {
        return Map.of(
                "success", true,
                "message", "已退出登录"
        );
    }
}