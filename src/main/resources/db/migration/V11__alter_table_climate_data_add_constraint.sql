ALTER TABLE climate_data
    ADD CONSTRAINT fk_climate_data_cities
        FOREIGN KEY (city_id) REFERENCES cities(id);