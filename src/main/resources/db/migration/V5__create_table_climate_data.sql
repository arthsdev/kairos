CREATE TABLE climate_data(
    id VARCHAR(36) PRIMARY KEY NOT NULL,
    temperature DOUBLE NOT NULL,
    humidity DOUBLE NOT NULL,
    rain_volume DOUBLE NOT NULL,
    wind_speed DOUBLE NOT NULL,
    risk_level VARCHAR(20),
    collected_at TIMESTAMP NOT NULL,

    city_id VARCHAR(36) NOT NULL,

    CONSTRAINT fk_climate_data_city
    FOREIGN KEY (city_id)
    REFERENCES monitored_cities(id)
)