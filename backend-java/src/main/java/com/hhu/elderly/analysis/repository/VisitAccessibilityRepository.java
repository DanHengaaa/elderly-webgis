package com.hhu.elderly.analysis.repository;

import com.hhu.elderly.analysis.dto.VisitAccessibilityResponse.IsochroneRing;
import com.hhu.elderly.analysis.dto.VisitAccessibilityResponse.ReachableInstitution;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

@Repository
public class VisitAccessibilityRepository {

    private final JdbcTemplate jdbcTemplate;

    public VisitAccessibilityRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public Long findNearestRoadNode(Double lon, Double lat) {
        return jdbcTemplate.queryForObject("""
                SELECT id
                FROM road_network_vertices_pgr
                ORDER BY the_geom <-> ST_Transform(
                    ST_SetSRID(ST_MakePoint(?, ?), 4326),
                    2364
                )
                LIMIT 1
                """, Long.class, lon, lat);
    }

    public IsochroneRing buildIsochroneRing(Long startNode, int seconds, int minutes) {
        String polygonGeoJson = jdbcTemplate.queryForObject("""
                WITH reached AS (
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
                        ?,
                        ?,
                        true
                    )
                ),
                reached_points AS (
                    SELECT v.the_geom AS geom
                    FROM reached r
                    JOIN road_network_vertices_pgr v
                      ON v.id = r.node
                    WHERE r.agg_cost <= ?
                ),
                ring_geom AS (
                    SELECT
                        CASE
                            WHEN COUNT(*) >= 3 THEN
                                ST_Transform(
                                    ST_Buffer(
                                        ST_ConvexHull(ST_Collect(geom)),
                                        250
                                    ),
                                    4326
                                )
                            ELSE NULL::geometry
                        END AS geom
                    FROM reached_points
                )
                SELECT ST_AsGeoJSON(geom)
                FROM ring_geom
                """, String.class, startNode, seconds, seconds);

        return new IsochroneRing(
                minutes,
                (double) seconds,
                getRingLevel(minutes),
                getRingLevelText(minutes),
                polygonGeoJson
        );
    }

    public List<ReachableInstitution> findReachableInstitutions(Long startNode, int maxSeconds) {
        return jdbcTemplate.query("""
                WITH reached AS (
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
                        ?,
                        ?,
                        true
                    )
                ),
                candidate AS (
                    SELECT
                        i.id,
                        i.name,
                        i.address,
                        i.district,
                        ST_X(i.geom) AS lon,
                        ST_Y(i.geom) AS lat,
                        best.estimated_s,
                        ROW_NUMBER() OVER (
                            PARTITION BY i.id
                            ORDER BY best.estimated_s ASC
                        ) AS rn
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
                      AND i.status = 1
                )
                SELECT
                    id,
                    name,
                    address,
                    district,
                    lon,
                    lat,
                    estimated_s
                FROM candidate
                WHERE rn = 1
                  AND estimated_s <= ?
                ORDER BY estimated_s ASC
                """, this::mapReachableInstitution, startNode, maxSeconds, maxSeconds);
    }

    private ReachableInstitution mapReachableInstitution(ResultSet rs, int rowNum) throws SQLException {
        Double estimatedSeconds = getDouble(rs, "estimated_s");
        Double estimatedMinutes = estimatedSeconds == null ? null : round(estimatedSeconds / 60.0, 1);

        return new ReachableInstitution(
                getLong(rs, "id"),
                rs.getString("name"),
                rs.getString("address"),
                rs.getString("district"),
                getDouble(rs, "lon"),
                getDouble(rs, "lat"),
                estimatedMinutes,
                getReachabilityLevel(estimatedMinutes),
                getReachabilityLevelText(estimatedMinutes)
        );
    }

    private String getRingLevel(int minutes) {
        if (minutes <= 15) {
            return "excellent";
        }

        if (minutes <= 30) {
            return "good";
        }

        return "medium";
    }

    private String getRingLevelText(int minutes) {
        if (minutes <= 15) {
            return "15 分钟探视圈";
        }

        if (minutes <= 30) {
            return "30 分钟探视圈";
        }

        return "60 分钟探视圈";
    }

    private String getReachabilityLevel(Double minutes) {
        if (minutes == null) {
            return "unknown";
        }

        if (minutes <= 15) {
            return "excellent";
        }

        if (minutes <= 30) {
            return "good";
        }

        if (minutes <= 60) {
            return "medium";
        }

        return "weak";
    }

    private String getReachabilityLevelText(Double minutes) {
        if (minutes == null) {
            return "暂无可达性";
        }

        if (minutes <= 15) {
            return "探视可达性优";
        }

        if (minutes <= 30) {
            return "探视可达性良";
        }

        if (minutes <= 60) {
            return "探视可达性中";
        }

        return "探视可达性弱";
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

    private Double round(Double value, int scale) {
        if (value == null) {
            return null;
        }

        double factor = Math.pow(10, scale);
        return Math.round(value * factor) / factor;
    }
}