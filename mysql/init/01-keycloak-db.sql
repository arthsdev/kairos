CREATE DATABASE IF NOT EXISTS kairos_keycloak CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
GRANT ALL PRIVILEGES ON kairos_keycloak.* TO 'kairos_user'@'%';
FLUSH PRIVILEGES;