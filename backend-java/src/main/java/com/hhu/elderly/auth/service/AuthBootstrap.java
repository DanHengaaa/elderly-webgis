package com.hhu.elderly.auth.service;

import com.hhu.elderly.auth.repository.AuthRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class AuthBootstrap implements CommandLineRunner {

    private final AuthRepository authRepository;
    private final PasswordEncoder passwordEncoder;

    public AuthBootstrap(
            AuthRepository authRepository,
            PasswordEncoder passwordEncoder
    ) {
        this.authRepository = authRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {
        createUserIfMissing(
                "admin",
                "123456",
                "平台管理员",
                "ADMIN"
        );

        createUserIfMissing(
                "customer",
                "123456",
                "体验客户",
                "CUSTOMER"
        );

        createUserIfMissing(
                "institution",
                "123456",
                "机构用户",
                "INSTITUTION"
        );
    }

    private void createUserIfMissing(
            String username,
            String password,
            String nickname,
            String roleCode
    ) {
        if (authRepository.existsByUsername(username)) {
            return;
        }

        authRepository.createUser(
                username,
                passwordEncoder.encode(password),
                nickname,
                null,
                roleCode
        );
    }
}