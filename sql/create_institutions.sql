-- 创建养老机构主表（修正 TINYINT → SMALLINT）
CREATE TABLE institutions (
    -- 主键与基础标识
    id BIGSERIAL PRIMARY KEY,
    fid INT,
    object_id INT,
    
    -- 基本信息
    name VARCHAR(100) NOT NULL,
    address VARCHAR(200),
    province VARCHAR(20) DEFAULT '江苏省',
    city VARCHAR(20) DEFAULT '南京市',
    district VARCHAR(50),
    
    -- POI分类信息
    poi_type_full VARCHAR(200),
    poi_big VARCHAR(50),
    poi_mid VARCHAR(50),
    poi_small VARCHAR(50),
    
    -- 空间地理信息
    lon_gcj02 DECIMAL(10, 7),
    lat_gcj02 DECIMAL(10, 7),
    lon_wgs84 DECIMAL(10, 7),
    lat_wgs84 DECIMAL(10, 7),
    geom GEOMETRY(Point, 4326),
    geohash VARCHAR(20),
    
    -- 核心运营与性质字段（TINYINT → SMALLINT）
    institution_category SMALLINT,              -- ✅ 修改处
    ownership_type SMALLINT NOT NULL DEFAULT 3, -- ✅ 修改处
    grade_level VARCHAR(10),
    total_beds INT,
    monthly_fee_base DECIMAL(10, 2),
    
    -- 联系方式
    contact_person VARCHAR(20),
    contact_phone VARCHAR(20),
    
    -- 动态运营与系统状态（TINYINT → SMALLINT）
    available_beds INT,
    status SMALLINT DEFAULT 1,                  -- ✅ 修改处
    
    -- 万能扩展字段
    extra_data JSONB,
    
    -- 审计追踪
    created_at TIMESTAMPTZ DEFAULT NOW(),
    updated_at TIMESTAMPTZ DEFAULT NOW()
);

-- 创建空间索引
CREATE INDEX idx_institutions_geom ON institutions USING GIST (geom);

-- 创建业务查询索引
CREATE INDEX idx_institutions_district ON institutions (district);
CREATE INDEX idx_institutions_ownership ON institutions (ownership_type);
CREATE INDEX idx_institutions_grade ON institutions (grade_level);
CREATE INDEX idx_institutions_status ON institutions (status);
CREATE INDEX idx_institutions_total_beds ON institutions (total_beds);

-- 创建JSONB索引
CREATE INDEX idx_institutions_extra_data ON institutions USING GIN (extra_data);