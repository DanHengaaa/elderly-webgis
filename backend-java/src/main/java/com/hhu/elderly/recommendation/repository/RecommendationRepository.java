package com.hhu.elderly.recommendation.repository;

import com.hhu.elderly.recommendation.dto.RecommendationRequest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Repository
public class RecommendationRepository {

    private final JdbcTemplate jdbcTemplate;

    public RecommendationRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * 查询推荐候选机构。
     *
     * 优化重点：
     * 1. 不再在推荐接口中实时计算医疗、生活、公园 POI 空间指标；
     * 2. 改为读取 institution_spatial_index 缓存表；
     * 3. 预算和床位不作为硬过滤条件，而是在 Service 层参与评分；
     * 4. 区县和等级使用 ILIKE 模糊匹配，避免字段不完全一致导致结果为 0。
     */
    public List<CandidateInstitution> findCandidates(
            RecommendationRequest request,
            int limit
    ) {
        StringBuilder sql = new StringBuilder("""
                SELECT
                    i.id,
                    i.name,
                    i.address,
                    i.district,
                    i.institution_category,
                    i.grade_level,
                    i.total_beds,
                    i.available_beds,
                    i.monthly_fee_base,
                    i.price_tier,
                    i.rating_avg,
                    i.rating_count,
                    i.cover_image_url,
                    ST_X(i.geom) AS lon,
                    ST_Y(i.geom) AS lat,

                    s.nearest_key_medical_km,
                    COALESCE(s.key_medical_count_3km, 0) AS key_medical_count_3km,
                    COALESCE(s.medical_type_count_5km, 0) AS medical_type_count_5km,

                    s.nearest_life_km,
                    COALESCE(s.life_count_600m, 0) AS life_count_600m,
                    COALESCE(s.life_type_count_600m, 0) AS life_type_count_600m,

                    s.nearest_park_km,
                    COALESCE(s.park_count_3km, 0) AS park_count_3km,
                    COALESCE(s.park_type_count_3km, 0) AS park_type_count_3km

                FROM institutions i
                LEFT JOIN institution_spatial_index s
                  ON s.institution_id = i.id

                WHERE i.geom IS NOT NULL
                  AND COALESCE(i.status, 1) = 1
                """);

        List<Object> params = new ArrayList<>();

        /*
         * 区县筛选：使用模糊匹配。
         * 例如前端传“鼓楼区”，数据库里可能是“南京市鼓楼区”。
         */
        if (hasText(request.preferredDistrict()) && !"不限".equals(request.preferredDistrict())) {
            sql.append(" AND i.district ILIKE ? ");
            params.add("%" + request.preferredDistrict().trim() + "%");
        }

        /*
         * 机构性质：
         * 1 公办公营
         * 2 公办民营
         * 3 民营
         */
        if (request.preferredCategory() != null) {
            sql.append(" AND i.institution_category = ? ");
            params.add(request.preferredCategory());
        }

        /*
         * 等级筛选：使用模糊匹配。
         * 例如数据库可能存“五级养老机构”“三级”“三级机构”等。
         */
        if (hasText(request.gradeLevel()) && !"不限".equals(request.gradeLevel())) {
            sql.append(" AND i.grade_level ILIKE ? ");
            params.add("%" + request.gradeLevel().trim() + "%");
        }

        /*
         * 重要：
         * 不在候选阶段硬过滤 available_beds。
         * 因为很多原始数据中 available_beds 可能为空或为 0。
         * 床位情况已经在 RecommendationService.calculateBedScore() 中参与评分。
         */

        /*
         * 重要：
         * 不在候选阶段硬过滤 monthly_fee_base。
         * 因为很多机构价格字段可能为空，或者用户预算较窄时会直接筛成 0 家。
         * 预算匹配已经在 RecommendationService.calculateBudgetScore() 中参与评分。
         */

        sql.append("""
                ORDER BY
                    i.rating_avg DESC NULLS LAST,
                    i.available_beds DESC NULLS LAST,
                    i.id ASC
                LIMIT ?
                """);

        params.add(limit);

        return jdbcTemplate.query(
                sql.toString(),
                this::mapCandidateInstitution,
                params.toArray()
        );
    }

    /**
     * 根据探视起点计算养老机构的路网可达时间。
     *
     * 注意：
     * 这个方法仍然会调用 pgRouting。
     * 因此只有当前端传入 startLon / startLat 时，Service 层才应该调用它。
     */
    public Map<Long, Double> findVisitMinutesByStart(
            Double startLon,
            Double startLat,
            int maxSeconds
    ) {
        String sql = """
                WITH start_node AS (
                    SELECT id
                    FROM road_network_vertices_pgr
                    ORDER BY the_geom <-> ST_Transform(
                        ST_SetSRID(ST_MakePoint(?, ?), 4326),
                        2364
                    )
                    LIMIT 1
                ),
                reached AS (
                    SELECT node, agg_cost
                    FROM pgr_drivingDistance(
                        'SELECT
                            gid::bigint AS id,
                            source::bigint AS source,
                            target::bigint AS target,
                            cost_s::double precision AS cost,
                            reverse_cost_s::double precision AS reverse_cost
                         FROM road_network
                         WHERE source IS NOT NULL
                           AND target IS NOT NULL
                           AND cost_s IS NOT NULL
                           AND reverse_cost_s IS NOT NULL',
                        (SELECT id FROM start_node),
                        ?,
                        true
                    )
                ),
                matched AS (
                    SELECT
                        i.id,
                        best.estimated_s
                    FROM institutions i
                    JOIN LATERAL (
                        SELECT
                            r.agg_cost
                            + ST_Distance(
                                i.geom::geography,
                                ST_Transform(v.the_geom, 4326)::geography
                              ) / 80.0 AS estimated_s
                        FROM reached r
                        JOIN road_network_vertices_pgr v
                          ON v.id = r.node
                        WHERE ST_DWithin(
                            i.geom::geography,
                            ST_Transform(v.the_geom, 4326)::geography,
                            1000
                        )
                        ORDER BY ST_Distance(
                            i.geom::geography,
                            ST_Transform(v.the_geom, 4326)::geography
                        ) ASC
                        LIMIT 1
                    ) best ON TRUE
                    WHERE i.geom IS NOT NULL
                      AND COALESCE(i.status, 1) = 1
                )
                SELECT
                    id,
                    estimated_s / 60.0 AS estimated_minutes
                FROM matched
                WHERE estimated_s <= ?
                """;

        Map<Long, Double> result = new LinkedHashMap<>();

        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                sql,
                startLon,
                startLat,
                maxSeconds,
                maxSeconds
        );

        for (Map<String, Object> row : rows) {
            Long institutionId = toLong(row.get("id"));
            Double minutes = toDouble(row.get("estimated_minutes"));

            if (institutionId != null && minutes != null) {
                result.put(institutionId, round(minutes, 1));
            }
        }

        return result;
    }

    private CandidateInstitution mapCandidateInstitution(ResultSet rs, int rowNum) throws SQLException {
        return new CandidateInstitution(
                getLong(rs, "id"),
                rs.getString("name"),
                rs.getString("address"),
                rs.getString("district"),

                getInteger(rs, "institution_category"),
                rs.getString("grade_level"),
                getInteger(rs, "total_beds"),
                getInteger(rs, "available_beds"),

                rs.getBigDecimal("monthly_fee_base"),
                getInteger(rs, "price_tier"),
                rs.getBigDecimal("rating_avg"),
                getInteger(rs, "rating_count"),

                rs.getString("cover_image_url"),

                getDouble(rs, "lon"),
                getDouble(rs, "lat"),

                getDouble(rs, "nearest_key_medical_km"),
                getInteger(rs, "key_medical_count_3km"),
                getInteger(rs, "medical_type_count_5km"),

                getDouble(rs, "nearest_life_km"),
                getInteger(rs, "life_count_600m"),
                getInteger(rs, "life_type_count_600m"),

                getDouble(rs, "nearest_park_km"),
                getInteger(rs, "park_count_3km"),
                getInteger(rs, "park_type_count_3km")
        );
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private Long getLong(ResultSet rs, String columnName) throws SQLException {
        Object value = rs.getObject(columnName);

        if (value == null) {
            return null;
        }

        return ((Number) value).longValue();
    }

    private Integer getInteger(ResultSet rs, String columnName) throws SQLException {
        Object value = rs.getObject(columnName);

        if (value == null) {
            return null;
        }

        return ((Number) value).intValue();
    }

    private Double getDouble(ResultSet rs, String columnName) throws SQLException {
        Object value = rs.getObject(columnName);

        if (value == null) {
            return null;
        }

        return ((Number) value).doubleValue();
    }

    private Long toLong(Object value) {
        if (value == null) {
            return null;
        }

        return ((Number) value).longValue();
    }

    private Double toDouble(Object value) {
        if (value == null) {
            return null;
        }

        return ((Number) value).doubleValue();
    }

    private Double round(Double value, int scale) {
        if (value == null) {
            return null;
        }

        double factor = Math.pow(10, scale);
        return Math.round(value * factor) / factor;
    }

    /**
     * 推荐候选机构对象。
     *
     * 这里把机构基础属性和空间画像指标一起带到 Service 层，
     * Service 层负责根据用户需求计算推荐分。
     */
    public record CandidateInstitution(
            Long id,
            String name,
            String address,
            String district,

            Integer institutionCategory,
            String gradeLevel,
            Integer totalBeds,
            Integer availableBeds,

            BigDecimal monthlyFeeBase,
            Integer priceTier,
            BigDecimal ratingAvg,
            Integer ratingCount,

            String coverImageUrl,

            Double lon,
            Double lat,

            Double nearestKeyMedicalKm,
            Integer keyMedicalCount3km,
            Integer medicalTypeCount5km,

            Double nearestLifeKm,
            Integer lifeCount600m,
            Integer lifeTypeCount600m,

            Double nearestParkKm,
            Integer parkCount3km,
            Integer parkTypeCount3km
    ) {
    }
}