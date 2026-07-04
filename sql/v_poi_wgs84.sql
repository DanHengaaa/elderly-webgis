DROP VIEW IF EXISTS v_poi_wgs84;

CREATE VIEW v_poi_wgs84 AS
SELECT
    id,
    category,
    poi_code,
    name,
    type_full,
    type_code,
    address,
    province,
    city,
    district,
    adcode,
    poi_big,
    poi_mid,
    poi_small,
    lon_wgs84,
    lat_wgs84,
    geom::geometry(Point, 4326) AS geom
FROM poi
WHERE geom IS NOT NULL
  AND category IN (1, 2, 3);