CREATE EXTENSION IF NOT EXISTS postgis;
CREATE EXTENSION IF NOT EXISTS postgis_topology;   -- 可选，一般用不到

SELECT PostGIS_Version();

-- 查看当前数据库编码
SELECT datname, encoding, datcollate, datctype FROM pg_database WHERE datname = 'elderly_care';

