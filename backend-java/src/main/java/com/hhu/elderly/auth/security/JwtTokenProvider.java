package com.hhu.elderly.auth.security;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;

@Component
public class JwtTokenProvider {

    @Value("${jwt.secret}")
    private String secret;

    @Value("${jwt.expire-hours:24}")
    private Long expireHours;

    private final ObjectMapper objectMapper = new ObjectMapper();

    public String generateToken(AuthUserPrincipal user) {
        try {
            long now = Instant.now().getEpochSecond();
            long expireAt = now + expireHours * 3600;

            Map<String, Object> header = new LinkedHashMap<>();
            header.put("alg", "HS256");
            header.put("typ", "JWT");

            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put("sub", user.getId());
            payload.put("username", user.getUsername());
            payload.put("nickname", user.getNickname());
            payload.put("roleCode", user.getRoleCode());
            payload.put("institutionId", user.getInstitutionId());
            payload.put("iat", now);
            payload.put("exp", expireAt);

            String headerText = base64UrlEncode(objectMapper.writeValueAsBytes(header));
            String payloadText = base64UrlEncode(objectMapper.writeValueAsBytes(payload));
            String unsignedToken = headerText + "." + payloadText;

            String signature = sign(unsignedToken);

            return unsignedToken + "." + signature;
        } catch (Exception exception) {
            throw new IllegalStateException("JWT 生成失败", exception);
        }
    }

    public AuthUserPrincipal parseToken(String token) {
        try {
            String[] parts = token.split("\\.");

            if (parts.length != 3) {
                throw new IllegalArgumentException("Token 格式错误");
            }

            String unsignedToken = parts[0] + "." + parts[1];
            String expectedSignature = sign(unsignedToken);

            if (!constantTimeEquals(expectedSignature, parts[2])) {
                throw new IllegalArgumentException("Token 签名无效");
            }

            byte[] payloadBytes = base64UrlDecode(parts[1]);

            Map<String, Object> payload = objectMapper.readValue(
                    payloadBytes,
                    new TypeReference<Map<String, Object>>() {
                    }
            );

            long now = Instant.now().getEpochSecond();
            long exp = ((Number) payload.get("exp")).longValue();

            if (now > exp) {
                throw new IllegalArgumentException("Token 已过期");
            }

            Long userId = ((Number) payload.get("sub")).longValue();
            String username = valueToString(payload.get("username"));
            String nickname = valueToString(payload.get("nickname"));
            String roleCode = valueToString(payload.get("roleCode"));

            Long institutionId = null;
            Object rawInstitutionId = payload.get("institutionId");

            if (rawInstitutionId instanceof Number number) {
                institutionId = number.longValue();
            }

            return new AuthUserPrincipal(
                    userId,
                    username,
                    nickname,
                    roleCode,
                    institutionId
            );
        } catch (Exception exception) {
            throw new IllegalArgumentException("Token 解析失败", exception);
        }
    }

    private String sign(String text) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");

        SecretKeySpec key = new SecretKeySpec(
                secret.getBytes(StandardCharsets.UTF_8),
                "HmacSHA256"
        );

        mac.init(key);

        byte[] signatureBytes = mac.doFinal(text.getBytes(StandardCharsets.UTF_8));

        return base64UrlEncode(signatureBytes);
    }

    private String base64UrlEncode(byte[] bytes) {
        return Base64.getUrlEncoder()
                .withoutPadding()
                .encodeToString(bytes);
    }

    private byte[] base64UrlDecode(String text) {
        return Base64.getUrlDecoder().decode(text);
    }

    private String valueToString(Object value) {
        return value == null ? null : String.valueOf(value);
    }

    private boolean constantTimeEquals(String a, String b) {
        if (a == null || b == null) {
            return false;
        }

        byte[] aBytes = a.getBytes(StandardCharsets.UTF_8);
        byte[] bBytes = b.getBytes(StandardCharsets.UTF_8);

        if (aBytes.length != bBytes.length) {
            return false;
        }

        int result = 0;

        for (int i = 0; i < aBytes.length; i++) {
            result |= aBytes[i] ^ bBytes[i];
        }

        return result == 0;
    }
}