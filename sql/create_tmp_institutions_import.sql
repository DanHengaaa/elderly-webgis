-- 创建临时表，字段名保持中文（方便映射），类型与 Excel 完全对应
CREATE TEMP TABLE tmp_institutions_import (
    fid INT,
    name VARCHAR(100),
    poi_type_full VARCHAR(200),
    address VARCHAR(200),
    province VARCHAR(20),
    city VARCHAR(20),
    district VARCHAR(50),
    poi_big VARCHAR(50),
    poi_mid VARCHAR(50),
    poi_small VARCHAR(50),
    lon_gcj02 DECIMAL(10,7),
    lat_gcj02 DECIMAL(10,7),
    lon_wgs84 DECIMAL(10,7),
    lat_wgs84 DECIMAL(10,7),
    institution_category SMALLINT,
    grade_level VARCHAR(10),
    total_beds INT,
    object_id INT,
    monthly_fee_base DECIMAL(10,2),
    contact_person VARCHAR(20),
    contact_phone VARCHAR(20)
);