package com.hhu.elderly.admin.repository;

import com.hhu.elderly.admin.dto.AdminCommunityPostItem;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public class AdminCommunityRepository {

    private final JdbcTemplate jdbcTemplate;

    public AdminCommunityRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<AdminCommunityPostItem> findPostsByStatus(Integer status) {
        String sql = """
                SELECT
                    id,
                    title,
                    CASE
                        WHEN length(content) > 100 THEN substring(content from 1 for 100) || '...'
                        ELSE content
                    END AS content_summary,
                    post_type,
                    author_name,
                    status,
                    created_at
                FROM community_posts
                WHERE status = ?
                ORDER BY created_at DESC
                """;

        return jdbcTemplate.query(
                sql,
                (rs, rowNum) -> new AdminCommunityPostItem(
                        rs.getLong("id"),
                        rs.getString("title"),
                        rs.getString("content_summary"),
                        rs.getString("post_type"),
                        getPostTypeText(rs.getString("post_type")),
                        rs.getString("author_name"),
                        rs.getInt("status"),
                        getStatusText(rs.getInt("status")),
                        toLocalDateTime(rs.getTimestamp("created_at"))
                ),
                status
        );
    }

    public void updatePostStatus(
            Long postId,
            Integer newStatus,
            Long operatorId,
            String operatorName,
            String reason
    ) {
        Integer oldStatus = jdbcTemplate.queryForObject(
                "SELECT status FROM community_posts WHERE id = ?",
                Integer.class,
                postId
        );

        jdbcTemplate.update(
                """
                UPDATE community_posts
                SET status = ?,
                    review_reason = ?,
                    reviewed_by = ?,
                    reviewed_at = CURRENT_TIMESTAMP,
                    updated_at = CURRENT_TIMESTAMP
                WHERE id = ?
                """,
                newStatus,
                reason,
                operatorId,
                postId
        );

        jdbcTemplate.update(
                """
                INSERT INTO audit_logs (
                    target_type,
                    target_id,
                    action,
                    old_status,
                    new_status,
                    operator_id,
                    operator_name,
                    reason
                )
                VALUES ('community_post', ?, ?, ?, ?, ?, ?, ?)
                """,
                postId,
                newStatus == 1 ? "approve" : "reject",
                String.valueOf(oldStatus),
                String.valueOf(newStatus),
                operatorId,
                operatorName,
                reason
        );
    }

    private String getPostTypeText(String type) {
        if (type == null) {
            return "社区笔记";
        }

        return switch (type) {
            case "visit" -> "探院笔记";
            case "care" -> "照护经验";
            case "review" -> "机构评价";
            case "guide" -> "养老避坑";
            case "route" -> "探视路线";
            case "question" -> "问题求助";
            default -> "社区笔记";
        };
    }

    private String getStatusText(Integer status) {
        if (status == null) {
            return "未知";
        }

        return switch (status) {
            case 0 -> "待审核";
            case 1 -> "已通过";
            case 2 -> "已驳回";
            case 3 -> "已下架";
            default -> "未知";
        };
    }

    private LocalDateTime toLocalDateTime(Timestamp timestamp) {
        return timestamp == null ? null : timestamp.toLocalDateTime();
    }
}