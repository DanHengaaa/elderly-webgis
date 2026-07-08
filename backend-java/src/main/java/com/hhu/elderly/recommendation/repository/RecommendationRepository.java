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
     * 修复点：
     * 1. 优先读取 institution_spatial_index 缓存表；
     * 2. 如果新入驻机构尚未生成缓存，则用 poi 表实时计算医疗、生活、环境指标兜底；
     * 3. 因此新审核通过的机构不会再出现医疗/生活/环境评分为 0 的问题。
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

                    COALESCE(s.nearest_key_medical_km, live.nearest_key_medical_km) AS nearest_key_medical_km,
                    COALESCE(s.key_medical_count_3km, live.key_medical_count_3km, 0) AS key_medical_count_3km,
                    COALESCE(s.medical_type_count_5km, live.medical_type_count_5km, 0) AS medical_type_count_5km,

                    COALESCE(s.nearest_life_km, live.nearest_life_km) AS nearest_life_km,
                    COALESCE(s.life_count_600m, live.life_count_600m, 0) AS life_count_600m,
                    COALESCE(s.life_type_count_600m, live.life_type_count_600m, 0) AS life_type_count_600m,

                    COALESCE(s.nearest_park_km, live.nearest_park_km) AS nearest_park_km,
                    COALESCE(s.park_count_3km, live.park_count_3km, 0) AS park_count_3km,
                    COALESCE(s.park_type_count_3km, live.park_type_count_3km, 0) AS park_type_count_3km

                FROM institutions i
                LEFT JOIN institution_spatial_index s
                  ON s.institution_id = i.id
                LEFT JOIN LATERAL (
                    SELECT
                        (
                            SELECT ROUND((ST_Distance(p.geom::geography, i.geom::geography) / 1000.0)::numeric, 3)::double precision
                            FROM poi p
                            WHERE p.geom IS NOT NULL
                              AND p.category = 1
                              AND (
                                    p.poi_small IN ('三级甲等医院', '综合医院')
                                    OR p.type_full ILIKE '%三级甲等医院%'
                                    OR p.type_full ILIKE '%综合医院%'
                              )
                            ORDER BY ST_Distance(p.geom::geography, i.geom::geography) ASC
                            LIMIT 1
                        ) AS nearest_key_medical_km,
                        (
                            SELECT COUNT(*)::integer
                            FROM poi p
                            WHERE p.geom IS NOT NULL
                              AND p.category = 1
                              AND ST_DWithin(p.geom::geography, i.geom::geography, 3000)
                              AND (
                                    p.poi_small IN ('三级甲等医院', '综合医院')
                                    OR p.type_full ILIKE '%三级甲等医院%'
                                    OR p.type_full ILIKE '%综合医院%'
                              )
                        ) AS key_medical_count_3km,
                        (
                            SELECT COUNT(DISTINCT COALESCE(NULLIF(p.poi_small, ''), NULLIF(p.type_full, ''), '其他医疗设施'))::integer
                            FROM poi p
                            WHERE p.geom IS NOT NULL
                              AND p.category = 1
                              AND ST_DWithin(p.geom::geography, i.geom::geography, 5000)
                        ) AS medical_type_count_5km,
                        (
                            SELECT ROUND((ST_Distance(p.geom::geography, i.geom::geography) / 1000.0)::numeric, 3)::double precision
                            FROM poi p
                            WHERE p.geom IS NOT NULL
                              AND p.category = 2
                            ORDER BY ST_Distance(p.geom::geography, i.geom::geography) ASC
                            LIMIT 1
                        ) AS nearest_life_km,
                        (
                            SELECT COUNT(*)::integer
                            FROM poi p
                            WHERE p.geom IS NOT NULL
                              AND p.category = 2
                              AND ST_DWithin(p.geom::geography, i.geom::geography, 600)
                        ) AS life_count_600m,
                        (
                            SELECT COUNT(DISTINCT COALESCE(NULLIF(p.poi_small, ''), NULLIF(p.poi_mid, ''), NULLIF(p.type_full, ''), '其他生活服务'))::integer
                            FROM poi p
                            WHERE p.geom IS NOT NULL
                              AND p.category = 2
                              AND ST_DWithin(p.geom::geography, i.geom::geography, 600)
                        ) AS life_type_count_600m,
                        (
                            SELECT ROUND((ST_Distance(p.geom::geography, i.geom::geography) / 1000.0)::numeric, 3)::double precision
                            FROM poi p
                            WHERE p.geom IS NOT NULL
                              AND p.category = 3
                            ORDER BY ST_Distance(p.geom::geography, i.geom::geography) ASC
                            LIMIT 1
                        ) AS nearest_park_km,
                        (
                            SELECT COUNT(*)::integer
                            FROM poi p
                            WHERE p.geom IS NOT NULL
                              AND p.category = 3
                              AND ST_DWithin(p.geom::geography, i.geom::geography, 3000)
                        ) AS park_count_3km,
                        (
                            SELECT COUNT(DISTINCT COALESCE(NULLIF(p.poi_small, ''), NULLIF(p.poi_mid, ''), NULLIF(p.type_full, ''), '其他公园景点'))::integer
                            FROM poi p
                            WHERE p.geom IS NOT NULL
                              AND p.category = 3
                              AND ST_DWithin(p.geom::geography, i.geom::geography, 3000)
                        ) AS park_type_count_3km
                ) live ON TRUE
                WHERE i.geom IS NOT NULL
                  AND COALESCE(i.status, 1) = 1
                """);

        List<Object> params = new ArrayList<>();

        if (hasText(request.preferredDistrict()) && !"不限".equals(request.preferredDistrict())) {
            sql.append(" AND i.district ILIKE ? ");
            params.add("%" + request.preferredDistrict().trim() + "%");
        }

        if (request.preferredCategory() != null) {
            sql.append(" AND i.institution_category = ? ");
            params.add(request.preferredCategory());
        }

        if (hasText(request.gradeLevel()) && !"不限".equals(request.gradeLevel())) {
            sql.append(" AND i.grade_level ILIKE ? ");
            params.add("%" + request.gradeLevel().trim() + "%");
        }

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
        return value == null ? null : ((Number) value).longValue();
    }

    private Integer getInteger(ResultSet rs, String columnName) throws SQLException {
        Object value = rs.getObject(columnName);
        return value == null ? null : ((Number) value).intValue();
    }

    private Double getDouble(ResultSet rs, String columnName) throws SQLException {
        Object value = rs.getObject(columnName);
        return value == null ? null : ((Number) value).doubleValue();
    }

    private Long toLong(Object value) {
        return value == null ? null : ((Number) value).longValue();
    }

    private Double toDouble(Object value) {
        return value == null ? null : ((Number) value).doubleValue();
    }

    private Double round(Double value, int scale) {
        if (value == null) {
            return null;
        }
        double factor = Math.pow(10, scale);
        return Math.round(value * factor) / factor;
    }

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
