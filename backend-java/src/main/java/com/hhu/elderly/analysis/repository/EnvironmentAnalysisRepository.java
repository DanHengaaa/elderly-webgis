package com.hhu.elderly.analysis.repository;

import com.hhu.elderly.analysis.dto.EnvironmentAnalysisResponse.EnvironmentPoi;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

@Repository
public class EnvironmentAnalysisRepository {

    private final JdbcTemplate jdbcTemplate;

    public EnvironmentAnalysisRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<BasicInstitution> findInstitutionById(Long institutionId) {
        return jdbcTemplate.query("""
                SELECT
                    i.id,
                    i.name,
                    i.address,
                    i.district,
                    ST_X(i.geom) AS lon,
                    ST_Y(i.geom) AS lat
                FROM institutions i
                WHERE i.id = ?
                  AND i.geom IS NOT NULL
                """, this::mapBasicInstitution, institutionId);
    }

    public int countPoiWithin(Long institutionId, int category, int radiusMeter) {
        Long count = jdbcTemplate.queryForObject("""
                SELECT COUNT(*) AS count
                FROM poi p
                CROSS JOIN institutions i
                WHERE i.id = ?
                  AND i.geom IS NOT NULL
                  AND p.geom IS NOT NULL
                  AND p.category = ?
                  AND ST_DWithin(p.geom::geography, i.geom::geography, ?)
                """, Long.class, institutionId, category, radiusMeter);

        return count == null ? 0 : count.intValue();
    }

    public int countDistinctPoiTypesWithin(Long institutionId, int category, int radiusMeter) {
        Long count = jdbcTemplate.queryForObject("""
                SELECT COUNT(DISTINCT COALESCE(NULLIF(p.poi_small, ''), NULLIF(p.poi_mid, ''), NULLIF(p.type_full, ''), '其他')) AS count
                FROM poi p
                CROSS JOIN institutions i
                WHERE i.id = ?
                  AND i.geom IS NOT NULL
                  AND p.geom IS NOT NULL
                  AND p.category = ?
                  AND ST_DWithin(p.geom::geography, i.geom::geography, ?)
                """, Long.class, institutionId, category, radiusMeter);

        return count == null ? 0 : count.intValue();
    }

    public List<EnvironmentPoi> findNearestPoi(Long institutionId, int category) {
        return jdbcTemplate.query("""
                SELECT
                    p.id,
                    p.category,
                    p.name,
                    p.address,
                    p.district,
                    p.poi_mid,
                    p.poi_small,
                    p.type_full,
                    ST_X(p.geom) AS lon,
                    ST_Y(p.geom) AS lat,
                    ST_Distance(p.geom::geography, i.geom::geography) AS distance_m
                FROM poi p
                CROSS JOIN institutions i
                WHERE i.id = ?
                  AND i.geom IS NOT NULL
                  AND p.geom IS NOT NULL
                  AND p.category = ?
                ORDER BY ST_Distance(p.geom::geography, i.geom::geography) ASC
                LIMIT 1
                """, this::mapEnvironmentPoi, institutionId, category);
    }

    public List<EnvironmentPoi> findNearbyPois(
            Long institutionId,
            int category,
            int radiusMeter,
            int limit
    ) {
        return jdbcTemplate.query("""
                SELECT
                    p.id,
                    p.category,
                    p.name,
                    p.address,
                    p.district,
                    p.poi_mid,
                    p.poi_small,
                    p.type_full,
                    ST_X(p.geom) AS lon,
                    ST_Y(p.geom) AS lat,
                    ST_Distance(p.geom::geography, i.geom::geography) AS distance_m
                FROM poi p
                CROSS JOIN institutions i
                WHERE i.id = ?
                  AND i.geom IS NOT NULL
                  AND p.geom IS NOT NULL
                  AND p.category = ?
                  AND ST_DWithin(p.geom::geography, i.geom::geography, ?)
                ORDER BY ST_Distance(p.geom::geography, i.geom::geography) ASC
                LIMIT ?
                """, this::mapEnvironmentPoi, institutionId, category, radiusMeter, limit);
    }

    private BasicInstitution mapBasicInstitution(ResultSet rs, int rowNum) throws SQLException {
        return new BasicInstitution(
                getLong(rs, "id"),
                rs.getString("name"),
                rs.getString("address"),
                rs.getString("district"),
                getDouble(rs, "lon"),
                getDouble(rs, "lat")
        );
    }

    private EnvironmentPoi mapEnvironmentPoi(ResultSet rs, int rowNum) throws SQLException {
        Integer category = getInteger(rs, "category");
        Double distanceMeter = getDouble(rs, "distance_m");
        Double distanceKm = distanceMeter == null ? null : round(distanceMeter / 1000.0, 3);

        return new EnvironmentPoi(
                getLong(rs, "id"),
                category,
                getCategoryText(category),
                rs.getString("name"),
                rs.getString("address"),
                rs.getString("district"),
                rs.getString("poi_mid"),
                rs.getString("poi_small"),
                rs.getString("type_full"),
                getDouble(rs, "lon"),
                getDouble(rs, "lat"),
                distanceKm
        );
    }

    private String getCategoryText(Integer category) {
        if (category == null) {
            return "未知 POI";
        }

        return switch (category) {
            case 2 -> "生活服务";
            case 3 -> "公园景点";
            default -> "其他 POI";
        };
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

    private Double round(Double value, int scale) {
        if (value == null) {
            return null;
        }

        double factor = Math.pow(10, scale);
        return Math.round(value * factor) / factor;
    }

    public record BasicInstitution(
            Long id,
            String name,
            String address,
            String district,
            Double lon,
            Double lat
    ) {
    }
}