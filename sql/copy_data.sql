INSERT INTO institutions (
    fid, object_id, name, address, province, city, district,
    poi_type_full, poi_big, poi_mid, poi_small,
    lon_gcj02, lat_gcj02, lon_wgs84, lat_wgs84,
    institution_category, grade_level, total_beds,
    monthly_fee_base, contact_person, contact_phone
)
SELECT 
    fid, object_id, name, address, province, city, district,
    poi_type_full, poi_big, poi_mid, poi_small,
    lon_gcj02, lat_gcj02, lon_wgs84, lat_wgs84,
    institution_category, grade_level, total_beds,
    monthly_fee_base, contact_person, contact_phone
FROM tmp_institutions_import;

UPDATE institutions 
SET geom = ST_SetSRID(ST_MakePoint(lon_wgs84, lat_wgs84), 4326)
WHERE lon_wgs84 IS NOT NULL AND lat_wgs84 IS NOT NULL;

UPDATE institutions 
SET geohash = ST_GeoHash(geom)
WHERE geom IS NOT NULL;

SELECT COUNT(*) FROM institutions;   -- 应该显示总记录数（约271）
SELECT id, name, district, total_beds, ownership_type FROM institutions LIMIT 5;