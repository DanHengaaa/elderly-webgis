CREATE TABLE institution_reviews (
    id BIGSERIAL PRIMARY KEY,
    institution_id BIGINT NOT NULL REFERENCES institutions(id) ON DELETE CASCADE,
    user_id BIGINT,
    rating_avg DECIMAL(2,1),
    rating_medical DECIMAL(2,1),
    rating_hardware DECIMAL(2,1),
    rating_food DECIMAL(2,1),
    rating_service DECIMAL(2,1),
    content TEXT,
    images TEXT[],
    is_approved BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT NOW()
);
CREATE INDEX idx_reviews_inst ON institution_reviews (institution_id);