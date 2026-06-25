CREATE TABLE institution_facilities (
    id BIGSERIAL PRIMARY KEY,
    institution_id BIGINT NOT NULL REFERENCES institutions(id) ON DELETE CASCADE,
    facility_name VARCHAR(50),
    facility_desc TEXT,
    created_at TIMESTAMP DEFAULT NOW()
);
CREATE INDEX idx_facilities_inst ON institution_facilities (institution_id);