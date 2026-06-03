CREATE TABLE monitored_cities(
    id VARCHAR(36) PRIMARY KEY NOT NULL,
    name VARCHAR(100) NOT NULL,
    state VARCHAR(100) NOT NULL,
    latitude DOUBLE NOT NULL,
    longitude DOUBLE NOT NULL,
    active boolean NOT NULL,
    requested_by VARCHAR(100) NOT NULL,
    created_at DATETIME NOT NULL
    )