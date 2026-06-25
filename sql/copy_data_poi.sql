-- 进入库：psql -U postgres -d elderly_care

-- 建临时表（create_poi.sql 第2节已有，这里复述）
CREATE TEMP TABLE tmp_poi_import (
    fid INT, poi_code VARCHAR(20), name VARCHAR(150), type_full VARCHAR(255),
    type_code VARCHAR(50), address VARCHAR(255), pcode VARCHAR(20), pname VARCHAR(20),
    citycode VARCHAR(10), cityname VARCHAR(20), adcode VARCHAR(20), adname VARCHAR(50),
    big_type VARCHAR(50), mid_type VARCHAR(50), small_type VARCHAR(100),
    lon_gcj02 DECIMAL(10,7), lat_gcj02 DECIMAL(10,7),
    lon_wgs84 DECIMAL(10,7), lat_wgs84 DECIMAL(10,7)
);

-- 导入CSV（路径改成你的；HEADER true 跳过表头行）
\copy tmp_poi_import FROM 'D:/Elderly/data/hospital_data.csv' WITH (FORMAT csv, HEADER true, ENCODING 'UTF8');
INSERT INTO poi (category, fid, poi_code, name, type_full, type_code, address,
                 province, city, district, adcode, poi_big, poi_mid, poi_small,
                 lon_gcj02, lat_gcj02, lon_wgs84, lat_wgs84)
SELECT DISTINCT ON (poi_code)
       1, fid, poi_code, name, type_full, type_code, address,
       pname, cityname, adname, adcode, big_type, mid_type, small_type,
       lon_gcj02, lat_gcj02, lon_wgs84, lat_wgs84
FROM tmp_poi_import
ORDER BY poi_code
ON CONFLICT (poi_code) WHERE poi_code IS NOT NULL DO NOTHING;

-- 生活服务 category=2
TRUNCATE tmp_poi_import;
\copy tmp_poi_import FROM 'D:/Elderly/data/life_service.csv' WITH (FORMAT csv, HEADER true, ENCODING 'UTF8');
INSERT INTO poi (category, fid, poi_code, name, type_full, type_code, address,
                 province, city, district, adcode, poi_big, poi_mid, poi_small,
                 lon_gcj02, lat_gcj02, lon_wgs84, lat_wgs84)
SELECT DISTINCT ON (poi_code)
       2, fid, poi_code, name, type_full, type_code, address,
       pname, cityname, adname, adcode, big_type, mid_type, small_type,
       lon_gcj02, lat_gcj02, lon_wgs84, lat_wgs84
FROM tmp_poi_import
ORDER BY poi_code
ON CONFLICT (poi_code) WHERE poi_code IS NOT NULL DO NOTHING;

-- 公园景点 category=3
TRUNCATE tmp_poi_import;
\copy tmp_poi_import FROM 'D:/Elderly/data/park_data.csv' WITH (FORMAT csv, HEADER true, ENCODING 'UTF8');
INSERT INTO poi (category, fid, poi_code, name, type_full, type_code, address,
                 province, city, district, adcode, poi_big, poi_mid, poi_small,
                 lon_gcj02, lat_gcj02, lon_wgs84, lat_wgs84)
SELECT DISTINCT ON (poi_code)
       3, fid, poi_code, name, type_full, type_code, address,
       pname, cityname, adname, adcode, big_type, mid_type, small_type,
       lon_gcj02, lat_gcj02, lon_wgs84, lat_wgs84
FROM tmp_poi_import
ORDER BY poi_code
ON CONFLICT (poi_code) WHERE poi_code IS NOT NULL DO NOTHING;
