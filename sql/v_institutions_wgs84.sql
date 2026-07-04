DROP VIEW IF EXISTS v_institutions_wgs84;

CREATE VIEW v_institutions_wgs84 AS
SELECT
    id,
    name,
    address,
    district,
    institution_category,
    grade_level,
    total_beds,
    available_beds,
    monthly_fee_base,
    price_tier,
    rating_avg,
    rating_count,
    cover_image_url,
    status,
    ST_X(geom) AS lon,
    ST_Y(geom) AS lat,
    geom::geometry(Point, 4326) AS geom
FROM institutions
WHERE geom IS NOT NULL
  AND status = 1;