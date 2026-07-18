ALTER TABLE plans
    ADD CONSTRAINT uk_plans_user_id UNIQUE (user_id);