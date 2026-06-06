    CREATE TABLE user_city(
    id VARCHAR(36) PRIMARY KEY NOT NULL,
    user_id VARCHAR(36) NOT NULL,
    active BOOLEAN NOT NULL,
    created_at TIMESTAMP NOT NULL,

    city_id VARCHAR(36) NOT NULL,

    CONSTRAINT fk_city_city
        FOREIGN KEY (city_id)
            REFERENCES cities(id)
)