create TABLE plans(
    id VARCHAR(36) PRIMARY KEY NOT NULL,
    user_id VARCHAR(36) NOT NULL,
    plan_type VARCHAR(20) NOT NULL,
    city_limit INT NOT NULL,
    created_at DATETIME NOT NULL,
    updated_at DATETIME,
    expires_at DATETIME
)