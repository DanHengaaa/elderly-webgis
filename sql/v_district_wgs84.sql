DROP VIEW IF EXISTS v_district_wgs84;

CREATE VIEW v_district_wgs84 AS
SELECT
    id,
    name,
    gb_code,
    adcode,
    area_km2,
    geom_wgs84::geometry(MultiPolygon, 4326) AS geom
FROM district
WHERE geom_wgs84 IS NOT NULL;