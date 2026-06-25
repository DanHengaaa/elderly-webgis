-- ============================================================
-- 路网入库 + pgRouting 拓扑构建脚本
-- 源数据：Nanjing_road.shp
-- 坐标系：Xian 1980 / 3-degree Gauss-Kruger zone 40 = EPSG:2364（由 .prj 确认）
-- 编码：UTF-8（由 .cpg 确认）
-- 环境：PostgreSQL + PostGIS + pgRouting，库 elderly_care
-- 用法：第1步在命令行跑 shp2pgsql；其余在 psql/pgAdmin 里顺序执行
-- ============================================================


-- ---- 0. 启用 pgRouting 扩展（postgis 已装）----
CREATE EXTENSION IF NOT EXISTS pgrouting;


-- ---- 1. 【命令行，非SQL】导入 shp 到临时表 road_import ----
--    注意 -s 2364（西安80 zone40）、-W UTF-8（编码）
-- shp2pgsql -s 2364 -W UTF-8 -g geom -I Nanjing_road.shp road_import | psql -U postgres -d elderly_care


-- ---- 2. 建正式路网表 ----
DROP TABLE IF EXISTS road_network CASCADE;
CREATE TABLE road_network (
    gid            BIGSERIAL PRIMARY KEY,
    name           VARCHAR(100),
    road_class_no  SMALLINT,
    road_class     VARCHAR(50),
    roadname       VARCHAR(100),
    speed_kmh      DOUBLE PRECISION,
    length_m       DOUBLE PRECISION,
    cost_s         DOUBLE PRECISION,        -- 正向耗时(秒)，pgRouting cost
    reverse_cost_s DOUBLE PRECISION,        -- 反向耗时，无单行道信息默认=正向
    source         BIGINT,                  -- 拓扑生成
    target         BIGINT,                  -- 拓扑生成
    geom           GEOMETRY(LineString, 2364),
    geom_wgs84     GEOMETRY(LineString, 4326)
);


-- ---- 3. 从临时表整理写入 ----
--    ST_LineMerge + ST_Dump：把多段线打散成单段 LineString，pgRouting 才能正确建拓扑
INSERT INTO road_network (name, road_class_no, road_class, roadname,
                          speed_kmh, length_m, cost_s, reverse_cost_s, geom)
SELECT name,
       no_class::SMALLINT,
       class, roadname,
       speed, length_m, time_s, time_s,
       (ST_Dump(ST_LineMerge(geom))).geom
FROM road_import;


-- ---- 4. 生成 WGS84 几何（展示 / 与 POI 互操作）----
--    注意：西安80 -> WGS84 存在基准面转换误差（数米级），城市尺度等时圈可接受
UPDATE road_network SET geom_wgs84 = ST_Transform(geom, 4326);


-- ---- 5. 索引 ----
CREATE INDEX idx_road_geom       ON road_network USING GIST (geom);
CREATE INDEX idx_road_geom_wgs84 ON road_network USING GIST (geom_wgs84);
CREATE INDEX idx_road_source     ON road_network (source);
CREATE INDEX idx_road_target     ON road_network (target);
CREATE INDEX idx_road_class      ON road_network (road_class_no);


-- ---- 6. 构建拓扑（容差单位=米，因 geom 是投影坐标；1 米合理）----
SELECT pgr_createTopology('road_network', 1.0, 'geom', 'gid');
--    完成后自动生成节点表 road_network_vertices_pgr


-- ---- 7. 校验 ----
SELECT COUNT(*) AS 边数            FROM road_network;
SELECT COUNT(*) AS 节点数          FROM road_network_vertices_pgr;
SELECT COUNT(*) AS 未连通边        FROM road_network WHERE source IS NULL OR target IS NULL;
-- 清理临时表
-- DROP TABLE road_import;


-- ============================================================
-- 8. 等时圈用法示例（探视可达性 / 30分钟可达）
-- ============================================================
-- (1) 找某机构(示例WGS84坐标)最近的路网节点；POI转到2364再做最近邻
-- SELECT id FROM road_network_vertices_pgr
-- ORDER BY the_geom <-> ST_Transform(ST_SetSRID(ST_MakePoint(118.78, 32.04), 4326), 2364)
-- LIMIT 1;

-- (2) 从该节点出发、1800秒(30分钟)可达的所有节点
-- SELECT * FROM pgr_drivingDistance(
--   'SELECT gid AS id, source, target, cost_s AS cost, reverse_cost_s AS reverse_cost FROM road_network',
--   <上一步的节点id>, 1800);

-- (3) 把可达节点做凹包/凸包即得等时圈多边形，再用 ST_Intersects 筛圈内的养老机构
