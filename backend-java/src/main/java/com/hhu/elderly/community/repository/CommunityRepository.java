package com.hhu.elderly.community.repository;

import com.hhu.elderly.community.dto.CommunityCommentCreateRequest;
import com.hhu.elderly.community.dto.CommunityCommentResponse;
import com.hhu.elderly.community.dto.CommunityHeatPoint;
import com.hhu.elderly.community.dto.CommunityInstitutionOption;
import com.hhu.elderly.community.dto.CommunityInstitutionPostBrief;
import com.hhu.elderly.community.dto.CommunityInstitutionPostCount;
import com.hhu.elderly.community.dto.CommunityPostCreateRequest;
import com.hhu.elderly.community.dto.CommunityPostDetail;
import com.hhu.elderly.community.dto.CommunityPostListItem;
import com.hhu.elderly.community.dto.CommunityPostPageResponse;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

@Repository
public class CommunityRepository {

    private final JdbcTemplate jdbcTemplate;

    public CommunityRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public CommunityPostPageResponse findPosts(
            String type,
            String district,
            String keyword,
            Long institutionId,
            String sort,
            Integer page,
            Integer size
    ) {
        int safePage = page == null || page < 1 ? 1 : page;
        int safeSize = size == null || size < 1 ? 12 : Math.min(size, 50);
        int offset = (safePage - 1) * safeSize;

        StringBuilder where = new StringBuilder(" WHERE p.status = 1 ");
        List<Object> params = new ArrayList<>();

        if (hasText(type) && !"all".equalsIgnoreCase(type)) {
            where.append(" AND p.post_type = ? ");
            params.add(type.trim());
        }

        if (hasText(district) && !"不限".equals(district)) {
            where.append(" AND p.district ILIKE ? ");
            params.add("%" + district.trim() + "%");
        }

        if (institutionId != null) {
            where.append(" AND p.institution_id = ? ");
            params.add(institutionId);
        }

        if (hasText(keyword)) {
            where.append(" AND (p.title ILIKE ? OR p.content ILIKE ? OR i.name ILIKE ?) ");
            String kw = "%" + keyword.trim() + "%";
            params.add(kw);
            params.add(kw);
            params.add(kw);
        }

        String orderBy = switch (sort == null ? "latest" : sort) {
            case "hot" -> """
                    ORDER BY
                        (
                            COALESCE(p.like_count, 0)
                            + COALESCE(p.comment_count, 0) * 2
                            + COALESCE(p.collect_count, 0) * 2
                            + COALESCE(p.view_count, 0) * 0.1
                        ) DESC,
                        p.created_at DESC
                    """;
            case "collect" -> " ORDER BY COALESCE(p.collect_count, 0) DESC, p.created_at DESC ";
            case "rating" -> " ORDER BY p.overall_rating DESC NULLS LAST, p.created_at DESC ";
            default -> " ORDER BY p.created_at DESC ";
        };

        String countSql = """
                SELECT COUNT(*)
                FROM community_posts p
                LEFT JOIN institutions i ON i.id = p.institution_id
                """ + where;

        Long total = jdbcTemplate.queryForObject(
                countSql,
                Long.class,
                params.toArray()
        );

        String listSql = """
                SELECT
                    p.id,
                    p.title,
                    CASE
                        WHEN length(p.content) > 90 THEN substring(p.content from 1 for 90) || '...'
                        ELSE p.content
                    END AS content_summary,
                    p.post_type,
                    p.author_name,
                    p.institution_id,
                    i.name AS institution_name,
                    p.district,
                    p.care_type,
                    p.overall_rating,
                    COALESCE(p.like_count, 0) AS like_count,
                    COALESCE(p.comment_count, 0) AS comment_count,
                    COALESCE(p.collect_count, 0) AS collect_count,
                    COALESCE(p.view_count, 0) AS view_count,
                    (
                        SELECT image_url
                        FROM community_post_images img
                        WHERE img.post_id = p.id
                        ORDER BY img.sort_order ASC, img.id ASC
                        LIMIT 1
                    ) AS cover_image_url,
                    (
                        SELECT string_agg(t.name, ',')
                        FROM community_post_topics pt
                        JOIN community_topics t ON t.id = pt.topic_id
                        WHERE pt.post_id = p.id
                    ) AS tags_text,
                    p.created_at
                FROM community_posts p
                LEFT JOIN institutions i ON i.id = p.institution_id
                """ + where + orderBy + " LIMIT ? OFFSET ? ";

        List<Object> listParams = new ArrayList<>(params);
        listParams.add(safeSize);
        listParams.add(offset);

        List<CommunityPostListItem> items = jdbcTemplate.query(
                listSql,
                (rs, rowNum) -> new CommunityPostListItem(
                        rs.getLong("id"),
                        rs.getString("title"),
                        rs.getString("content_summary"),
                        rs.getString("post_type"),
                        getPostTypeText(rs.getString("post_type")),
                        rs.getString("author_name"),
                        getLong(rs.getObject("institution_id")),
                        rs.getString("institution_name"),
                        rs.getString("district"),
                        rs.getString("care_type"),
                        getCareTypeText(rs.getString("care_type")),
                        rs.getBigDecimal("overall_rating"),
                        rs.getInt("like_count"),
                        rs.getInt("comment_count"),
                        rs.getInt("collect_count"),
                        rs.getInt("view_count"),
                        rs.getString("cover_image_url"),
                        splitTags(rs.getString("tags_text")),
                        toLocalDateTime(rs.getTimestamp("created_at"))
                ),
                listParams.toArray()
        );

        return new CommunityPostPageResponse(
                safePage,
                safeSize,
                total == null ? 0 : total,
                items
        );
    }

    public CommunityPostDetail findPostDetail(
            Long id,
            Long currentUserId
    ) {
        jdbcTemplate.update(
                """
                UPDATE community_posts
                SET view_count = COALESCE(view_count, 0) + 1
                WHERE id = ?
                """,
                id
        );

        String sql = """
                SELECT
                    p.*,
                    i.name AS institution_name,
                    i.address AS institution_address,
                    CASE
                        WHEN ? IS NULL THEN FALSE
                        ELSE EXISTS (
                            SELECT 1
                            FROM community_likes l
                            WHERE l.post_id = p.id
                              AND l.user_id = ?
                        )
                    END AS liked_by_current_user,
                    CASE
                        WHEN ? IS NULL THEN FALSE
                        ELSE EXISTS (
                            SELECT 1
                            FROM community_collections c
                            WHERE c.post_id = p.id
                              AND c.user_id = ?
                        )
                    END AS collected_by_current_user
                FROM community_posts p
                LEFT JOIN institutions i ON i.id = p.institution_id
                WHERE p.id = ?
                  AND p.status = 1
                """;

        return jdbcTemplate.queryForObject(
                sql,
                (rs, rowNum) -> new CommunityPostDetail(
                        rs.getLong("id"),
                        rs.getString("title"),
                        rs.getString("content"),
                        rs.getString("post_type"),
                        getPostTypeText(rs.getString("post_type")),

                        getLong(rs.getObject("author_id")),
                        rs.getString("author_name"),

                        getLong(rs.getObject("institution_id")),
                        rs.getString("institution_name"),
                        rs.getString("institution_address"),
                        rs.getString("district"),
                        rs.getString("care_type"),
                        getCareTypeText(rs.getString("care_type")),

                        rs.getBigDecimal("budget_min"),
                        rs.getBigDecimal("budget_max"),

                        rs.getObject("visit_date", java.time.LocalDate.class),
                        rs.getBoolean("is_field_visit"),

                        rs.getBigDecimal("overall_rating"),
                        rs.getBigDecimal("environment_rating"),
                        rs.getBigDecimal("care_rating"),
                        rs.getBigDecimal("medical_rating"),
                        rs.getBigDecimal("life_rating"),
                        rs.getBigDecimal("visit_rating"),
                        rs.getBigDecimal("price_transparency_rating"),

                        rs.getInt("like_count"),
                        rs.getInt("comment_count"),
                        rs.getInt("collect_count"),
                        rs.getInt("view_count"),

                        rs.getBoolean("liked_by_current_user"),
                        rs.getBoolean("collected_by_current_user"),

                        findImageUrls(id),
                        findTags(id),

                        toLocalDateTime(rs.getTimestamp("created_at"))
                ),
                currentUserId,
                currentUserId,
                currentUserId,
                currentUserId,
                id
        );
    }

    public Long createPost(
            CommunityPostCreateRequest request,
            Long authorId,
            String authorName,
            Integer status
    ) {
        if (!hasText(request.title())) {
            throw new IllegalArgumentException("标题不能为空。");
        }

        if (!hasText(request.content())) {
            throw new IllegalArgumentException("正文内容不能为空。");
        }

        String sql = """
                INSERT INTO community_posts (
                    title,
                    content,
                    post_type,
                    author_id,
                    author_name,
                    institution_id,
                    district,
                    care_type,
                    budget_min,
                    budget_max,
                    visit_date,
                    is_field_visit,
                    overall_rating,
                    environment_rating,
                    care_rating,
                    medical_rating,
                    life_rating,
                    visit_rating,
                    price_transparency_rating,
                    status,
                    like_count,
                    comment_count,
                    collect_count,
                    view_count
                )
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 0, 0, 0, 0)
                RETURNING id
                """;

        Long postId = jdbcTemplate.queryForObject(
                sql,
                Long.class,
                request.title().trim(),
                request.content().trim(),
                hasText(request.postType()) ? request.postType().trim() : "visit",
                authorId,
                authorName,
                request.institutionId(),
                request.district(),
                request.careType(),
                request.budgetMin(),
                request.budgetMax(),
                request.visitDate(),
                request.isFieldVisit() == null ? false : request.isFieldVisit(),
                request.overallRating(),
                request.environmentRating(),
                request.careRating(),
                request.medicalRating(),
                request.lifeRating(),
                request.visitRating(),
                request.priceTransparencyRating(),
                status
        );

        if (postId == null) {
            throw new IllegalStateException("社区帖子创建失败：未返回帖子 ID。");
        }

        saveImages(postId, request.imageUrls());
        saveTags(postId, request.tags());

        return postId;
    }

    public void deletePost(
            Long postId,
            Long userId
    ) {
        int updated = jdbcTemplate.update(
                """
                UPDATE community_posts
                SET status = 3,
                    updated_at = CURRENT_TIMESTAMP
                WHERE id = ?
                  AND author_id = ?
                  AND status IN (0, 1)
                """,
                postId,
                userId
        );

        if (updated <= 0) {
            throw new IllegalArgumentException("只能删除自己发布的帖子，或帖子不存在。");
        }
    }

    public List<CommunityCommentResponse> findComments(Long postId) {
        String sql = """
                SELECT
                    id,
                    post_id,
                    parent_id,
                    author_id,
                    author_name,
                    content,
                    0 AS like_count,
                    created_at
                FROM community_comments
                WHERE post_id = ?
                  AND status = 1
                ORDER BY created_at ASC, id ASC
                """;

        return jdbcTemplate.query(
                sql,
                (rs, rowNum) -> new CommunityCommentResponse(
                        rs.getLong("id"),
                        rs.getLong("post_id"),
                        getLong(rs.getObject("parent_id")),
                        getLong(rs.getObject("author_id")),
                        rs.getString("author_name"),
                        rs.getString("content"),
                        rs.getInt("like_count"),
                        toLocalDateTime(rs.getTimestamp("created_at"))
                ),
                postId
        );
    }

    public Long createComment(
            Long postId,
            CommunityCommentCreateRequest request,
            Long authorId,
            String authorName
    ) {
        if (request == null || !hasText(request.content())) {
            throw new IllegalArgumentException("评论内容不能为空。");
        }

        String sql = """
                INSERT INTO community_comments (
                    post_id,
                    parent_id,
                    author_id,
                    author_name,
                    content,
                    status
                )
                VALUES (?, ?, ?, ?, ?, 1)
                RETURNING id
                """;

        Long id = jdbcTemplate.queryForObject(
                sql,
                Long.class,
                postId,
                request.parentId(),
                authorId,
                authorName,
                request.content().trim()
        );

        jdbcTemplate.update(
                """
                UPDATE community_posts
                SET comment_count = COALESCE(comment_count, 0) + 1
                WHERE id = ?
                """,
                postId
        );

        return id;
    }

    public void deleteComment(
            Long postId,
            Long commentId,
            Long userId
    ) {
        int updated = jdbcTemplate.update(
                """
                UPDATE community_comments
                SET status = 0
                WHERE id = ?
                  AND post_id = ?
                  AND author_id = ?
                  AND status = 1
                """,
                commentId,
                postId,
                userId
        );

        if (updated <= 0) {
            throw new IllegalArgumentException("只能删除自己的评论，或评论不存在。");
        }

        jdbcTemplate.update(
                """
                UPDATE community_posts
                SET comment_count = GREATEST(COALESCE(comment_count, 0) - 1, 0)
                WHERE id = ?
                """,
                postId
        );
    }

    public boolean toggleLike(
            Long postId,
            Long userId
    ) {
        Integer exists = jdbcTemplate.queryForObject(
                """
                SELECT COUNT(*)
                FROM community_likes
                WHERE post_id = ?
                  AND user_id = ?
                """,
                Integer.class,
                postId,
                userId
        );

        if (exists != null && exists > 0) {
            jdbcTemplate.update(
                    """
                    DELETE FROM community_likes
                    WHERE post_id = ?
                      AND user_id = ?
                    """,
                    postId,
                    userId
            );

            jdbcTemplate.update(
                    """
                    UPDATE community_posts
                    SET like_count = GREATEST(COALESCE(like_count, 0) - 1, 0)
                    WHERE id = ?
                    """,
                    postId
            );

            return false;
        }

        jdbcTemplate.update(
                """
                INSERT INTO community_likes (
                    post_id,
                    user_id
                )
                VALUES (?, ?)
                """,
                postId,
                userId
        );

        jdbcTemplate.update(
                """
                UPDATE community_posts
                SET like_count = COALESCE(like_count, 0) + 1
                WHERE id = ?
                """,
                postId
        );

        return true;
    }

    public int countLikes(Long postId) {
        Integer count = jdbcTemplate.queryForObject(
                """
                SELECT COALESCE(like_count, 0)
                FROM community_posts
                WHERE id = ?
                """,
                Integer.class,
                postId
        );

        return count == null ? 0 : count;
    }

    public boolean toggleCollect(
            Long postId,
            Long userId
    ) {
        Integer exists = jdbcTemplate.queryForObject(
                """
                SELECT COUNT(*)
                FROM community_collections
                WHERE post_id = ?
                  AND user_id = ?
                """,
                Integer.class,
                postId,
                userId
        );

        if (exists != null && exists > 0) {
            jdbcTemplate.update(
                    """
                    DELETE FROM community_collections
                    WHERE post_id = ?
                      AND user_id = ?
                    """,
                    postId,
                    userId
            );

            jdbcTemplate.update(
                    """
                    UPDATE community_posts
                    SET collect_count = GREATEST(COALESCE(collect_count, 0) - 1, 0)
                    WHERE id = ?
                    """,
                    postId
            );

            return false;
        }

        jdbcTemplate.update(
                """
                INSERT INTO community_collections (
                    post_id,
                    user_id
                )
                VALUES (?, ?)
                """,
                postId,
                userId
        );

        jdbcTemplate.update(
                """
                UPDATE community_posts
                SET collect_count = COALESCE(collect_count, 0) + 1
                WHERE id = ?
                """,
                postId
        );

        return true;
    }

    public int countCollections(Long postId) {
        Integer count = jdbcTemplate.queryForObject(
                """
                SELECT COALESCE(collect_count, 0)
                FROM community_posts
                WHERE id = ?
                """,
                Integer.class,
                postId
        );

        return count == null ? 0 : count;
    }

    public List<CommunityInstitutionOption> findInstitutionOptions(String keyword) {
        StringBuilder sql = new StringBuilder("""
                SELECT
                    id,
                    name,
                    district,
                    address
                FROM institutions
                WHERE COALESCE(status, 1) = 1
                """);

        List<Object> params = new ArrayList<>();

        if (hasText(keyword)) {
            sql.append(" AND (name ILIKE ? OR district ILIKE ? OR address ILIKE ?) ");
            String kw = "%" + keyword.trim() + "%";
            params.add(kw);
            params.add(kw);
            params.add(kw);
        }

        sql.append(" ORDER BY id ASC LIMIT 100 ");

        return jdbcTemplate.query(
                sql.toString(),
                (rs, rowNum) -> new CommunityInstitutionOption(
                        rs.getLong("id"),
                        rs.getString("name"),
                        rs.getString("district"),
                        rs.getString("address")
                ),
                params.toArray()
        );
    }

    public List<CommunityInstitutionPostBrief> findInstitutionPosts(
            Long institutionId,
            int limit
    ) {
        String sql = """
                SELECT
                    p.id,
                    p.title,
                    p.post_type,
                    p.author_name,
                    p.overall_rating,
                    CASE
                        WHEN length(p.content) > 120 THEN substring(p.content from 1 for 120) || '...'
                        ELSE p.content
                    END AS content_summary,
                    (
                        SELECT string_agg(t.name, ',')
                        FROM community_post_topics pt
                        JOIN community_topics t ON t.id = pt.topic_id
                        WHERE pt.post_id = p.id
                    ) AS tags_text,
                    p.created_at
                FROM community_posts p
                WHERE p.status = 1
                  AND p.institution_id = ?
                ORDER BY p.created_at DESC
                LIMIT ?
                """;

        return jdbcTemplate.query(
                sql,
                (rs, rowNum) -> new CommunityInstitutionPostBrief(
                        rs.getLong("id"),
                        rs.getString("title"),
                        rs.getString("post_type"),
                        getPostTypeText(rs.getString("post_type")),
                        rs.getString("author_name"),
                        rs.getBigDecimal("overall_rating"),
                        rs.getString("content_summary"),
                        splitTags(rs.getString("tags_text")),
                        toLocalDateTime(rs.getTimestamp("created_at"))
                ),
                institutionId,
                limit
        );
    }

    public List<CommunityInstitutionPostCount> findPostCountsByInstitutionIds(
            List<Long> institutionIds
    ) {
        if (institutionIds == null || institutionIds.isEmpty()) {
            return List.of();
        }

        String placeholders = String.join(
                ",",
                institutionIds.stream().map(id -> "?").toList()
        );

        String sql = """
                SELECT
                    institution_id,
                    COUNT(*)::integer AS post_count
                FROM community_posts
                WHERE status = 1
                  AND institution_id IN (
                """ + placeholders + """
                  )
                GROUP BY institution_id
                """;

        return jdbcTemplate.query(
                sql,
                (rs, rowNum) -> new CommunityInstitutionPostCount(
                        rs.getLong("institution_id"),
                        rs.getInt("post_count")
                ),
                institutionIds.toArray()
        );
    }

    public Map<String, Object> findInstitutionCommunityBase(Long institutionId) {
        String sql = """
                SELECT
                    i.id AS institution_id,
                    i.name AS institution_name,
                    COUNT(p.id)::integer AS post_count,
                    ROUND(AVG(p.overall_rating), 1) AS avg_rating,
                    (
                        SELECT string_agg(t.name, ',')
                        FROM community_post_topics pt
                        JOIN community_topics t ON t.id = pt.topic_id
                        JOIN community_posts pp ON pp.id = pt.post_id
                        WHERE pp.institution_id = i.id
                          AND pp.status = 1
                    ) AS tags_text
                FROM institutions i
                LEFT JOIN community_posts p
                  ON p.institution_id = i.id
                 AND p.status = 1
                WHERE i.id = ?
                GROUP BY i.id, i.name
                """;

        return jdbcTemplate.queryForMap(sql, institutionId);
    }

    public List<CommunityHeatPoint> findCommunityHeatPoints() {
        String sql = """
                SELECT
                    i.id AS institution_id,
                    i.name AS institution_name,
                    ST_X(i.geom) AS lon,
                    ST_Y(i.geom) AS lat,
                    COUNT(p.id)::integer AS post_count,
                    ROUND(AVG(p.overall_rating), 1) AS avg_rating
                FROM institutions i
                JOIN community_posts p
                  ON p.institution_id = i.id
                 AND p.status = 1
                WHERE i.geom IS NOT NULL
                GROUP BY i.id, i.name, i.geom
                HAVING COUNT(p.id) > 0
                ORDER BY COUNT(p.id) DESC
                """;

        return jdbcTemplate.query(
                sql,
                (rs, rowNum) -> new CommunityHeatPoint(
                        rs.getLong("institution_id"),
                        rs.getString("institution_name"),
                        getDouble(rs.getObject("lon")),
                        getDouble(rs.getObject("lat")),
                        rs.getInt("post_count"),
                        rs.getBigDecimal("avg_rating")
                )
        );
    }

    private void saveImages(Long postId, List<String> imageUrls) {
        if (imageUrls == null || imageUrls.isEmpty()) {
            return;
        }

        int index = 0;

        for (String rawUrl : imageUrls) {
            if (!hasText(rawUrl)) {
                continue;
            }

            jdbcTemplate.update(
                    """
                    INSERT INTO community_post_images (
                        post_id,
                        image_url,
                        sort_order
                    )
                    VALUES (?, ?, ?)
                    """,
                    postId,
                    rawUrl.trim(),
                    index
            );

            index++;
        }
    }

    private void saveTags(Long postId, List<String> tags) {
        if (tags == null || tags.isEmpty()) {
            return;
        }

        for (String rawTag : tags) {
            if (!hasText(rawTag)) {
                continue;
            }

            String tag = rawTag.trim().replace("#", "");

            if (!hasText(tag)) {
                continue;
            }

            Long topicId = findTopicIdByName(tag);

            if (topicId == null) {
                topicId = createTopic(tag);
            }

            if (topicId == null) {
                continue;
            }

            if (!existsPostTopic(postId, topicId)) {
                jdbcTemplate.update(
                        """
                        INSERT INTO community_post_topics (
                            post_id,
                            topic_id
                        )
                        VALUES (?, ?)
                        """,
                        postId,
                        topicId
                );

                jdbcTemplate.update(
                        """
                        UPDATE community_topics
                        SET post_count = COALESCE(post_count, 0) + 1
                        WHERE id = ?
                        """,
                        topicId
                );
            }
        }
    }

    private Long findTopicIdByName(String tag) {
        List<Long> ids = jdbcTemplate.query(
                """
                SELECT id
                FROM community_topics
                WHERE name = ?
                ORDER BY id ASC
                LIMIT 1
                """,
                (rs, rowNum) -> rs.getLong("id"),
                tag
        );

        return ids.isEmpty() ? null : ids.get(0);
    }

    private Long createTopic(String tag) {
        return jdbcTemplate.queryForObject(
                """
                INSERT INTO community_topics (
                    name,
                    post_count
                )
                VALUES (?, 0)
                RETURNING id
                """,
                Long.class,
                tag
        );
    }

    private boolean existsPostTopic(Long postId, Long topicId) {
        Integer count = jdbcTemplate.queryForObject(
                """
                SELECT COUNT(*)
                FROM community_post_topics
                WHERE post_id = ?
                  AND topic_id = ?
                """,
                Integer.class,
                postId,
                topicId
        );

        return count != null && count > 0;
    }

    private List<String> findImageUrls(Long postId) {
        return jdbcTemplate.query(
                """
                SELECT image_url
                FROM community_post_images
                WHERE post_id = ?
                ORDER BY sort_order ASC, id ASC
                """,
                (rs, rowNum) -> rs.getString("image_url"),
                postId
        );
    }

    private List<String> findTags(Long postId) {
        return jdbcTemplate.query(
                """
                SELECT t.name
                FROM community_post_topics pt
                JOIN community_topics t ON t.id = pt.topic_id
                WHERE pt.post_id = ?
                ORDER BY t.name ASC
                """,
                (rs, rowNum) -> rs.getString("name"),
                postId
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

    private String getCareTypeText(String careType) {
        if (careType == null) {
            return "未标注";
        }

        return switch (careType) {
            case "selfCare" -> "基本自理";
            case "semiCare" -> "半失能";
            case "nursing" -> "失能护理";
            case "dementia" -> "认知症照护";
            default -> "未标注";
        };
    }

    private List<String> splitTags(String tagsText) {
        if (!hasText(tagsText)) {
            return List.of();
        }

        return Arrays.stream(tagsText.split(","))
                .map(String::trim)
                .filter(this::hasText)
                .distinct()
                .toList();
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private Long getLong(Object value) {
        if (value == null) {
            return null;
        }

        return ((Number) value).longValue();
    }

    private Double getDouble(Object value) {
        if (value == null) {
            return null;
        }

        return ((Number) value).doubleValue();
    }

    private java.time.LocalDateTime toLocalDateTime(Timestamp timestamp) {
        if (timestamp == null) {
            return null;
        }

        return timestamp.toLocalDateTime();
    }
}