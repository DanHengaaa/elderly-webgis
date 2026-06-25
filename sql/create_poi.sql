-- ============================================================
-- POI 统一表（医疗 / 生活服务 / 公园景点）建表与导入脚本
-- 对齐《功能模块设计_完整版》第八章 "POI 表" 及 M2 各子模块
-- 环境：PostgreSQL + PostGIS，数据库 elderly_care
-- 三份源数据字段结构一致，合并为一张 poi 表，用 category 区分
-- ============================================================


-- ============================================================
-- 1. 创建 POI 主表
-- ============================================================
CREATE TABLE poi (
    -- 主键与分类
    id          BIGSERIAL PRIMARY KEY,
    category    SMALLINT NOT NULL,             -- 1-医疗 2-生活服务 3-公园景点 4-负面POI(预留)

    -- 原始追溯
    fid         INT,
    poi_code    VARCHAR(20),                   -- 高德POI唯一编码，去重用

    -- 基本信息
    name        VARCHAR(150) NOT NULL,
    type_full   VARCHAR(255),                  -- 完整分类串
    type_code   VARCHAR(50),                   -- 高德typecode，可能含多值 a|b，故用字符串
    address     VARCHAR(255),
    province    VARCHAR(20) DEFAULT '江苏省',
    city        VARCHAR(20) DEFAULT '南京市',
    district    VARCHAR(50),
    adcode      VARCHAR(20),                   -- 区县编码，按字符串存（保留前导）

    -- POI分级
    poi_big     VARCHAR(50),
    poi_mid     VARCHAR(50),
    poi_small   VARCHAR(100),                  -- M2核心过滤字段，可能含多值 a|b

    -- 空间地理信息
    lon_gcj02   DECIMAL(10,7),
    lat_gcj02   DECIMAL(10,7),
    lon_wgs84   DECIMAL(10,7),
    lat_wgs84   DECIMAL(10,7),
    geom        GEOMETRY(Point, 4326),         -- WGS84，Web展示/拓扑分析
    geom_proj   GEOMETRY(Point, 4548),         -- 投影坐标，米制距离计算（推荐）
    geohash     VARCHAR(20),

    -- 扩展与审计
    extra_data  JSONB,
    created_at  TIMESTAMPTZ DEFAULT NOW()
);

-- 唯一约束（去重）
CREATE UNIQUE INDEX uq_poi_code ON poi (poi_code) WHERE poi_code IS NOT NULL;

-- 空间索引
CREATE INDEX idx_poi_geom      ON poi USING GIST (geom);
CREATE INDEX idx_poi_geom_proj ON poi USING GIST (geom_proj);

-- 业务查询索引
CREATE INDEX idx_poi_category  ON poi (category);
CREATE INDEX idx_poi_small     ON poi (poi_small);
CREATE INDEX idx_poi_big       ON poi (poi_big);
CREATE INDEX idx_poi_district  ON poi (district);
CREATE INDEX idx_poi_geohash   ON poi (geohash);
CREATE INDEX idx_poi_extra     ON poi USING GIN (extra_data);
-- 高频组合查询（M2：某类别+某小类）可建联合索引
CREATE INDEX idx_poi_cat_small ON poi (category, poi_small);


-- ============================================================
-- 2. 临时导入表（字段顺序对应 Excel，中文映射后入库）
--    三份文件结构相同，复用同一张临时表，分三次导入
-- ============================================================
CREATE TEMP TABLE tmp_poi_import (
    fid        INT,
    poi_code   VARCHAR(20),
    name       VARCHAR(150),
    type_full  VARCHAR(255),
    type_code  VARCHAR(50),
    address    VARCHAR(255),
    pcode      VARCHAR(20),
    pname      VARCHAR(20),
    citycode   VARCHAR(10),
    cityname   VARCHAR(20),
    adcode     VARCHAR(20),
    adname     VARCHAR(50),
    big_type   VARCHAR(50),
    mid_type   VARCHAR(50),
    small_type VARCHAR(100),
    lon_gcj02  DECIMAL(10,7),
    lat_gcj02  DECIMAL(10,7),
    lon_wgs84  DECIMAL(10,7),
    lat_wgs84  DECIMAL(10,7)
);
-- 注：公园文件末尾有一个多余空列，导出CSV时删掉，或在COPY时只映射前19列


-- ============================================================
-- 3. 导入数据（以 CSV 为例；先把 xlsx 另存为 UTF-8 CSV）
--    每导入一类，先 TRUNCATE 临时表，再带上对应 category 写入主表
-- ============================================================

-- ---- (1) 医疗设施 category=1 ----
-- \copy tmp_poi_import FROM '医院.csv' WITH (FORMAT csv, HEADER true, ENCODING 'UTF8');
INSERT INTO poi (category, fid, poi_code, name, type_full, type_code, address,
                 province, city, district, adcode, poi_big, poi_mid, poi_small,
                 lon_gcj02, lat_gcj02, lon_wgs84, lat_wgs84)
SELECT 1, fid, poi_code, name, type_full, type_code, address,
       pname, cityname, adname, adcode, big_type, mid_type, small_type,
       lon_gcj02, lat_gcj02, lon_wgs84, lat_wgs84
FROM tmp_poi_import;
-- TRUNCATE tmp_poi_import;

-- ---- (2) 生活服务 category=2 ----
-- \copy tmp_poi_import FROM '生活服务.csv' WITH (FORMAT csv, HEADER true, ENCODING 'UTF8');
-- INSERT INTO poi (category, ...) SELECT 2, ... FROM tmp_poi_import;  -- 同上，首列改为 2
-- TRUNCATE tmp_poi_import;

-- ---- (3) 公园景点 category=3 ----
-- \copy tmp_poi_import FROM '公园景点.csv' WITH (FORMAT csv, HEADER true, ENCODING 'UTF8');
-- INSERT INTO poi (category, ...) SELECT 3, ... FROM tmp_poi_import;  -- 同上，首列改为 3


-- ============================================================
-- 4. 生成空间列（导入完成后统一执行）
-- ============================================================
UPDATE poi
SET geom = ST_SetSRID(ST_MakePoint(lon_wgs84, lat_wgs84), 4326)
WHERE lon_wgs84 IS NOT NULL AND lat_wgs84 IS NOT NULL;

UPDATE poi
SET geom_proj = ST_Transform(geom, 4548)
WHERE geom IS NOT NULL;

UPDATE poi
SET geohash = ST_GeoHash(geom)
WHERE geom IS NOT NULL;


-- ============================================================
-- 5. 校验
-- ============================================================
SELECT category, COUNT(*) AS cnt FROM poi GROUP BY category ORDER BY category;
SELECT id, category, name, poi_small, district FROM poi LIMIT 5;

-- M2 示例：某机构(示例坐标)周边 2km 内的三甲与综合医院数量
-- SELECT COUNT(*) FROM poi
-- WHERE category = 1
--   AND poi_small IN ('三级甲等医院','综合医院')
--   AND ST_DWithin(geom_proj, ST_Transform(ST_SetSRID(ST_MakePoint(118.78,32.04),4326),4548), 2000);
