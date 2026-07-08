package com.hhu.elderly.auth.repository;

import com.hhu.elderly.auth.dto.AuthUserResponse;
import com.hhu.elderly.auth.security.AuthUserPrincipal;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class AuthRepository {

    private final JdbcTemplate jdbcTemplate;

    public AuthRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public boolean existsByUsername(String username) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM sys_users WHERE username = ?",
                Integer.class,
                username
        );

        return count != null && count > 0;
    }

    public void createUser(
            String username,
            String passwordHash,
            String nickname,
            String phone,
            String roleCode
    ) {
        jdbcTemplate.update(
                """
                INSERT INTO sys_users (
                    username,
                    password_hash,
                    nickname,
                    phone,
                    role_code,
                    status
                )
                VALUES (?, ?, ?, ?, ?, 1)
                """,
                username,
                passwordHash,
                nickname,
                phone,
                roleCode
        );
    }

    public UserPasswordRecord findPasswordRecord(String username) {
        String sql = """
                SELECT
                    id,
                    username,
                    password_hash,
                    nickname,
                    phone,
                    email,
                    role_code,
                    institution_id,
                    status
                FROM sys_users
                WHERE username = ?
                """;

        List<UserPasswordRecord> list = jdbcTemplate.query(
                sql,
                (rs, rowNum) -> new UserPasswordRecord(
                        rs.getLong("id"),
                        rs.getString("username"),
                        rs.getString("password_hash"),
                        rs.getString("nickname"),
                        rs.getString("phone"),
                        rs.getString("email"),
                        rs.getString("role_code"),
                        rs.getObject("institution_id") == null
                                ? null
                                : ((Number) rs.getObject("institution_id")).longValue(),
                        rs.getInt("status")
                ),
                username
        );

        return list.isEmpty() ? null : list.get(0);
    }

    public UserPasswordRecord findPasswordRecordById(Long id) {
        String sql = """
                SELECT
                    id,
                    username,
                    password_hash,
                    nickname,
                    phone,
                    email,
                    role_code,
                    institution_id,
                    status
                FROM sys_users
                WHERE id = ?
                """;

        List<UserPasswordRecord> list = jdbcTemplate.query(
                sql,
                (rs, rowNum) -> new UserPasswordRecord(
                        rs.getLong("id"),
                        rs.getString("username"),
                        rs.getString("password_hash"),
                        rs.getString("nickname"),
                        rs.getString("phone"),
                        rs.getString("email"),
                        rs.getString("role_code"),
                        rs.getObject("institution_id") == null
                                ? null
                                : ((Number) rs.getObject("institution_id")).longValue(),
                        rs.getInt("status")
                ),
                id
        );

        return list.isEmpty() ? null : list.get(0);
    }

    public AuthUserResponse findUserResponseById(Long id) {
        String sql = """
                SELECT
                    id,
                    username,
                    nickname,
                    phone,
                    email,
                    role_code,
                    institution_id
                FROM sys_users
                WHERE id = ?
                """;

        return jdbcTemplate.queryForObject(
                sql,
                (rs, rowNum) -> new AuthUserResponse(
                        rs.getLong("id"),
                        rs.getString("username"),
                        rs.getString("nickname"),
                        rs.getString("phone"),
                        rs.getString("email"),
                        rs.getString("role_code"),
                        getRoleName(rs.getString("role_code")),
                        rs.getObject("institution_id") == null
                                ? null
                                : ((Number) rs.getObject("institution_id")).longValue()
                ),
                id
        );
    }

    public void updateProfile(
            Long userId,
            String nickname,
            String phone,
            String email
    ) {
        jdbcTemplate.update(
                """
                UPDATE sys_users
                SET nickname = ?,
                    phone = ?,
                    email = ?,
                    updated_at = CURRENT_TIMESTAMP
                WHERE id = ?
                """,
                nickname,
                phone,
                email,
                userId
        );
    }

    public void updatePassword(
            Long userId,
            String passwordHash
    ) {
        jdbcTemplate.update(
                """
                UPDATE sys_users
                SET password_hash = ?,
                    updated_at = CURRENT_TIMESTAMP
                WHERE id = ?
                """,
                passwordHash,
                userId
        );
    }

    public AuthUserResponse toUserResponse(UserPasswordRecord record) {
        return new AuthUserResponse(
                record.id(),
                record.username(),
                record.nickname(),
                record.phone(),
                record.email(),
                record.roleCode(),
                getRoleName(record.roleCode()),
                record.institutionId()
        );
    }

    public AuthUserPrincipal toPrincipal(UserPasswordRecord record) {
        return new AuthUserPrincipal(
                record.id(),
                record.username(),
                record.nickname(),
                record.roleCode(),
                record.institutionId()
        );
    }

    public void saveLoginLog(
            Long userId,
            String username,
            String roleCode,
            Boolean success,
            String message
    ) {
        jdbcTemplate.update(
                """
                INSERT INTO sys_login_logs (
                    user_id,
                    username,
                    role_code,
                    success,
                    message
                )
                VALUES (?, ?, ?, ?, ?)
                """,
                userId,
                username,
                roleCode,
                success,
                message
        );
    }

    private String getRoleName(String roleCode) {
        if (roleCode == null) {
            return "未知角色";
        }

        return switch (roleCode) {
            case "CUSTOMER" -> "客户";
            case "INSTITUTION" -> "养老机构";
            case "ADMIN" -> "管理员";
            default -> "未知角色";
        };
    }

    public record UserPasswordRecord(
            Long id,
            String username,
            String passwordHash,
            String nickname,
            String phone,
            String email,
            String roleCode,
            Long institutionId,
            Integer status
    ){}
    
    public void syncCommunityAuthorName(
        Long userId,
        String authorName
) {
    jdbcTemplate.update(
            """
            UPDATE community_posts
            SET author_name = ?
            WHERE author_id = ?
            """,
            authorName,
            userId
    );

    jdbcTemplate.update(
            """
            UPDATE community_comments
            SET author_name = ?
            WHERE author_id = ?
            """,
            authorName,
            userId
    );
}
    
    
    
    
    
    
    
    
    
    
    
    
    
    
     {
    }
}
