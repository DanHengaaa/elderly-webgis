package com.hhu.elderly.config;

import com.hhu.elderly.auth.security.JwtAuthenticationFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;

import jakarta.servlet.http.HttpServletRequest;
import java.util.List;

@Configuration
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    public SecurityConfig(
            JwtAuthenticationFilter jwtAuthenticationFilter
    ) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                // 前后端分离项目必须关掉 CSRF，否则 POST、DELETE 很容易直接 403
                .csrf(AbstractHttpConfigurer::disable)

                .cors(cors -> cors.configurationSource(corsConfigurationSource()))

                // JWT 登录不使用 Session
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

                .authorizeHttpRequests(auth -> auth
                        // 登录注册放行
                        .requestMatchers("/api/auth/login").permitAll()
                        .requestMatchers("/api/auth/register").permitAll()
                        .requestMatchers("/api/auth/ping").permitAll()

                        // 上传资源放行
                        .requestMatchers("/uploads/**").permitAll()

                        // 社区 GET 公开浏览
                        .requestMatchers(HttpMethod.GET, "/api/community/**").permitAll()

                        // 社区 POST / DELETE 必须登录
                        .requestMatchers(HttpMethod.POST, "/api/community/**").authenticated()
                        .requestMatchers(HttpMethod.DELETE, "/api/community/**").authenticated()

                        // 机构、地图、分析、推荐相关接口，按你当前课程设计先放行
                        .requestMatchers(HttpMethod.GET, "/api/institutions/**").permitAll()
                        .requestMatchers("/api/analysis/**").permitAll()
                        .requestMatchers("/api/geo/**").permitAll()
                        .requestMatchers("/api/recommendations/**").permitAll()
                        .requestMatchers("/api/ai-assistant/**").permitAll()

                        // 机构端接口
                        .requestMatchers("/api/institution-console/**").hasRole("INSTITUTION")

                        // 管理员接口
                        .requestMatchers("/api/admin/**").hasRole("ADMIN")

                        // 当前用户接口需要登录
                        .requestMatchers("/api/auth/me").authenticated()
                        .requestMatchers("/api/auth/logout").authenticated()

                        // 其他请求先要求登录
                        .anyRequest().authenticated()
                )

                .addFilterBefore(
                        jwtAuthenticationFilter,
                        UsernamePasswordAuthenticationFilter.class
                );

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        return new CorsConfigurationSource() {
            @Override
            public CorsConfiguration getCorsConfiguration(HttpServletRequest request) {
                CorsConfiguration config = new CorsConfiguration();

                config.setAllowedOrigins(List.of(
                        "http://localhost:5173",
                        "http://127.0.0.1:5173"
                ));

                config.setAllowedMethods(List.of(
                        "GET",
                        "POST",
                        "PUT",
                        "DELETE",
                        "OPTIONS"
                ));

                config.setAllowedHeaders(List.of("*"));
                config.setExposedHeaders(List.of("Authorization"));
                config.setAllowCredentials(true);
                config.setMaxAge(3600L);

                return config;
            }
        };
    }
}