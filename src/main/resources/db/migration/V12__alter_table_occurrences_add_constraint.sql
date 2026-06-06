ALTER TABLE occurrences
    ADD CONSTRAINT fk_occurrences_cities
        FOREIGN KEY (city_id) REFERENCES cities(id);