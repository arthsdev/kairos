CREATE TABLE occurrences (
    id VARCHAR(36) PRIMARY KEY NOT NULL,
    title VARCHAR(100) NOT NULL,
    description VARCHAR(500),
    category VARCHAR(20) NOT NULL,
    severity VARCHAR(20) NOT NULL,
    status VARCHAR(20) NOT NULL,
    latitude DOUBLE NOT NULL,
    longitude DOUBLE NOT NULL,
    image_url VARCHAR(512),
    user_id VARCHAR(36) NOT NULL,
    created_at DATETIME NOT NULL
)