package com.hhu.elderly.analysis.repository;

import com.hhu.elderly.analysis.dto.MedicalLifelineResponse.NearbyMedicalPoi;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

@Repository
public class MedicalLifelineRepository {

    private final JdbcTemplate jdbcTemplate;

    public MedicalLifelineRepository(JdbcTemplate jdbcTemplate) {
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

    public int countMedicalWithin(Long institutionId, int radiusMeter, boolean keyMedicalOnly) {
        StringBuilder sql = new StringBuilder("""
                SELECT COUNT(*) AS count
                FROM poi p
                CROSS JOIN institutions i
                WHERE i.id = ?
                  AND i.geom IS NOT NULL
                  AND p.geom IS NOT NULL
                  AND p.category = 1
                  AND ST_DWithin(p.geom::geography, i.geom::geography, ?)
                """);

        if (keyMedicalOnly) {
            sql.append("""
                    AND (
                        p.poi_small IN ('三级甲等医院', '综合医院')
                        OR p.type_full ILIKE '%三级甲等医院%'
                        OR p.type_full ILIKE '%综合医院%'
                    )
                    """);
        }

        Long count = jdbcTemplate.queryForObject(
                sql.toString(),
                Long.class,
                institutionId,
                radiusMeter
        );

        return count == null ? 0 : count.intValue();
    }

    public int countDistinctMedicalTypesWithin(Long institutionId, int radiusMeter) {
        Long count = jdbcTemplate.queryForObject("""
                SELECT COUNT(DISTINCT COALESCE(NULLIF(p.poi_small, ''), NULLIF(p.type_full, ''), '其他医疗设施')) AS count
                FROM poi p
                CROSS JOIN institutions i
                WHERE i.id = ?
                  AND i.geom IS NOT NULL
                  AND p.geom IS NOT NULL
                  AND p.category = 1
                  AND ST_DWithin(p.geom::geography, i.geom::geography, ?)
                """, Long.class, institutionId, radiusMeter);

        return count == null ? 0 : count.intValue();
    }

    public List<NearbyMedicalPoi> findNearestKeyMedicalPoi(Long institutionId) {
        return jdbcTemplate.query("""
                SELECT
                    p.id,
                    p.name,
                    p.address,
                    p.district,
                    p.poi_mid,
                    p.poi_small,
                    p.type_full,
                    ST_X(p.geom) AS lon,
                    ST_Y(p.geom) AS lat,
                    ST_Distance(p.geom::geography, i.geom::geography) AS distance_m,
                    TRUE AS key_medical
                FROM poi p
                CROSS JOIN institutions i
                WHERE i.id = ?
                  AND i.geom IS NOT NULL
                  AND p.geom IS NOT NULL
                  AND p.category = 1
                  AND (
                        p.poi_small IN ('三级甲等医院', '综合医院')
                        OR p.type_full ILIKE '%三级甲等医院%'
                        OR p.type_full ILIKE '%综合医院%'
                  )
                ORDER BY ST_Distance(p.geom::geography, i.geom::geography) ASC
                LIMIT 1
                """, this::mapNearbyMedicalPoi, institutionId);
    }

    public List<NearbyMedicalPoi> findNearbyMedicalPois(
            Long institutionId,
            int radiusMeter,
            int limit
    ) {
        return jdbcTemplate.query("""
                SELECT
                    p.id,
                    p.name,
                    p.address,
                    p.district,
                    p.poi_mid,
                    p.poi_small,
                    p.type_full,
                    ST_X(p.geom) AS lon,
                    ST_Y(p.geom) AS lat,
                    ST_Distance(p.geom::geography, i.geom::geography) AS distance_m,
                    CASE
                        WHEN p.poi_small IN ('三级甲等医院', '综合医院')
                          OR p.type_full ILIKE '%三级甲等医院%'
                          OR p.type_full ILIKE '%综合医院%'
                        THEN TRUE
                        ELSE FALSE
                    END AS key_medical
                FROM poi p
                CROSS JOIN institutions i
                WHERE i.id = ?
                  AND i.geom IS NOT NULL
                  AND p.geom IS NOT NULL
                  AND p.category = 1
                  AND ST_DWithin(p.geom::geography, i.geom::geography, ?)
                ORDER BY ST_Distance(p.geom::geography, i.geom::geography) ASC
                LIMIT ?
                """, this::mapNearbyMedicalPoi, institutionId, radiusMeter, limit);
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

    private NearbyMedicalPoi mapNearbyMedicalPoi(ResultSet rs, int rowNum) throws SQLException {
        Double distanceMeter = getDouble(rs, "distance_m");
        Double distanceKm = distanceMeter == null ? null : round(distanceMeter / 1000.0, 3);

        return new NearbyMedicalPoi(
                getLong(rs, "id"),
                rs.getString("name"),
                rs.getString("address"),
                rs.getString("district"),
                rs.getString("poi_mid"),
                rs.getString("poi_small"),
                rs.getString("type_full"),
                getDouble(rs, "lon"),
                getDouble(rs, "lat"),
                distanceKm,
                getBoolean(rs, "key_medical")
        );
    }

    private Long getLong(ResultSet rs, String columnName) throws SQLException {
        Object value = rs.getObject(columnName);
        if (value == null) {
            return null;
        }
        return ((Number) value).longValue();
    }

    private Double getDouble(ResultSet rs, String columnName) throws SQLException {
        Object value = rs.getObject(columnName);
        if (value == null) {
            return null;
        }
        return ((Number) value).doubleValue();
    }

    private Boolean getBoolean(ResultSet rs, String columnName) throws SQLException {
        Object value = rs.getObject(columnName);
        if (value == null) {
            return null;
        }
        return (Boolean) value;
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