-- ========== A. 必要：直接关系到模块能否跑通 ==========

-- A1. 机构归属账号（M1 RBAC + 机构SaaS工作台：机构只能改自己的档案）
ALTER TABLE institutions ADD COLUMN owner_user_id BIGINT;
CREATE INDEX idx_institutions_owner ON institutions (owner_user_id);
COMMENT ON COLUMN institutions.owner_user_id IS '机构管理账号ID，关联users(id)；RBAC机构侧据此限制只能维护本机构';
-- 待 users 表建好后再补外键：
-- ALTER TABLE institutions ADD CONSTRAINT fk_inst_owner
--   FOREIGN KEY (owner_user_id) REFERENCES users(id) ON DELETE SET NULL;

-- A2. 价位档位（M4 预算硬过滤；原始 monthly_fee_base 多为 0，无法直接过滤）
ALTER TABLE institutions ADD COLUMN price_tier SMALLINT;
CREATE INDEX idx_institutions_price_tier ON institutions (price_tier);
COMMENT ON COLUMN institutions.price_tier IS '价位档位：1-经济型 2-标准型 3-高端型，供M4预算硬过滤';

-- A3. 评价聚合（M5 列表展示/按评分排序，避免每次 JOIN 聚合 reviews）
ALTER TABLE institutions
    ADD COLUMN rating_avg   DECIMAL(2,1) DEFAULT 0,
    ADD COLUMN rating_count INT          DEFAULT 0;
COMMENT ON COLUMN institutions.rating_avg   IS '综合评分均值，由reviews审核通过后聚合更新';
COMMENT ON COLUMN institutions.rating_count IS '有效评价条数';


-- ========== B. 推荐：提升体验 / 支撑 M6 ==========

-- B1. 展示媒体（机构SaaS：实景图库 / 全景看房）
ALTER TABLE institutions
    ADD COLUMN cover_image_url VARCHAR(255),
    ADD COLUMN images          TEXT[],
    ADD COLUMN has_panorama    BOOLEAN DEFAULT FALSE;
COMMENT ON COLUMN institutions.cover_image_url IS '封面图URL，列表卡片展示';
COMMENT ON COLUMN institutions.images          IS '机构实景图URL数组（机构SaaS端维护）';
COMMENT ON COLUMN institutions.has_panorama    IS '是否支持全景看房';

-- B2. 机构简介（详情页 + M6 RAG 私有知识库向量化语料来源）
ALTER TABLE institutions ADD COLUMN intro TEXT;
COMMENT ON COLUMN institutions.intro IS '机构简介长文本，详情展示并作为RAG知识库语料';


-- ========== C. 可选：性能 / 工程优化 ==========

-- C1. 投影坐标列：让米制距离 / 2SFCA / 最近医院查询又快又准（南京用 EPSG:4548）
--     注意：不要用 GENERATED 列，ST_Transform 非 IMMUTABLE 会报错，用普通列+更新维护
ALTER TABLE institutions ADD COLUMN geom_proj GEOMETRY(Point, 4548);
UPDATE institutions SET geom_proj = ST_Transform(geom, 4548) WHERE geom IS NOT NULL;
CREATE INDEX idx_institutions_geom_proj ON institutions USING GIST (geom_proj);
-- 若不想加列，距离计算时直接用 geom::geography 也能按"米"返回，二选一即可