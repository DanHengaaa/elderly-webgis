-- ============================================================
-- 南京市区县面数据 入库脚本（M3 四维评估的空间统计单元底座）
-- 源数据：xian.shp
-- 坐标系：CGCS2000 地理坐标 = EPSG:4490（由 .prj 确认，经纬度）
-- 编码：UTF-8（由 .cpg 确认）
-- 环境：PostgreSQL + PostGIS，库 elderly_care
-- ============================================================


-- ---- 1.【CMD，非SQL；务必用 CMD 不要用 PowerShell，否则中文乱码】----
--    路径含空格，cd 时整体加引号
-- cd /d "D:\Elderly\data\Administrative Division"
-- set PGCLIENTENCODING=UTF8
-- set PGPASSWORD=你的密码
-- "E:\Program Files\PostgreSQL\18\bin\shp2pgsql.exe" -s 4490 -W UTF-8 -g geom -I xian.shp district_import | "E:\Program Files\PostgreSQL\18\bin\psql.exe" -U postgres -d elderly_care


-- ---- 2. 建正式表 ----
DROP TABLE IF EXISTS district CASCADE;
CREATE TABLE district (
    id         BIGSERIAL PRIMARY KEY,
    name       VARCHAR(50) NOT NULL,
    gb_code    VARCHAR(20),
    adcode     VARCHAR(10),               -- 6位行政区划代码，与 poi.adcode 对接
    area_km2   DOUBLE PRECISION,
    geom       GEOMETRY(MultiPolygon, 4490),
    geom_wgs84 GEOMETRY(MultiPolygon, 4326)
);


-- ---- 3. 从临时表整理写入 ----
--    ST_MakeValid 修复自相交等无效面；ST_CollectionExtract(...,3) 只保留面部分；ST_Multi 统一为 MultiPolygon
INSERT INTO district (name, gb_code, adcode, geom)
SELECT trim(name),
       trim(gb),
       right(trim(gb), 6),
       ST_Multi(ST_CollectionExtract(ST_MakeValid(geom), 3))
FROM district_import;


-- ---- 4. 生成 WGS84 几何 + 面积 ----
UPDATE district SET geom_wgs84 = ST_Transform(geom, 4326);
UPDATE district SET area_km2  = ROUND((ST_Area(geom_wgs84::geography) / 1e6)::numeric, 2);


-- ---- 5. 索引 ----
CREATE INDEX idx_district_geom       ON district USING GIST (geom);
CREATE INDEX idx_district_geom_wgs84 ON district USING GIST (geom_wgs84);
CREATE INDEX idx_district_adcode     ON district (adcode);


-- ============================================================
-- 6. 数据质量核对（重点！原始数据范围异常、记录数=14≠11，务必检查）
-- ============================================================
-- 按面积降序+中心点，肉眼找异常面（中心点应都在 118~119.3E / 31~32.6N 附近）
SELECT id, name, adcode, area_km2,
       ST_AsText(ST_Centroid(geom_wgs84)) AS center
FROM district
ORDER BY area_km2 DESC;

-- 找出中心点明显偏离南京范围的可疑要素（落在此框外的要警惕）
SELECT id, name, adcode, area_km2
FROM district
WHERE NOT ST_Within(ST_Centroid(geom_wgs84),
                    ST_MakeEnvelope(118.2, 31.1, 119.3, 32.7, 4326));

-- 查重复区县名/代码
SELECT name, COUNT(*) FROM district GROUP BY name HAVING COUNT(*) > 1;

-- 确认是否11个区
SELECT COUNT(*) AS 区县数 FROM district;
-- 如发现坏要素，定位后删除：DELETE FROM district WHERE id = <异常id>;


-- ============================================================
-- 7. 用法示例
-- ============================================================
-- (1) 空间连接：把每个机构/POI 落到所属区县（推荐，最稳）
-- SELECT i.id, i.name, d.name AS district
-- FROM institutions i JOIN district d ON ST_Contains(d.geom_wgs84, i.geom);

-- (2) 属性连接：若 adcode 已对齐，也可按 adcode join
-- SELECT p.* FROM poi p JOIN district d ON p.adcode = d.adcode;

-- (3) 统计每区机构数（M3 供需侧雏形）
-- SELECT d.name, COUNT(i.id) AS inst_cnt, d.area_km2
-- FROM district d LEFT JOIN institutions i ON ST_Contains(d.geom_wgs84, i.geom)
-- GROUP BY d.id, d.name, d.area_km2 ORDER BY inst_cnt DESC;
