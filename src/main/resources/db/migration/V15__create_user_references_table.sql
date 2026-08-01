CREATE TABLE user_references(
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    keycloak_user_id VARCHAR(255) NOT NULL UNIQUE,
    created_at TIMESTAMP NOT NULL
);