-- Extend user profile with optional developer-centric fields
ALTER TABLE users
    ADD COLUMN IF NOT EXISTS location         VARCHAR(120),
    ADD COLUMN IF NOT EXISTS experience_level VARCHAR(50), -- e.g. JUNIOR, MID, SENIOR
    ADD COLUMN IF NOT EXISTS primary_stack    VARCHAR(255),
    ADD COLUMN IF NOT EXISTS availability     VARCHAR(60); -- e.g. FULL_TIME, PART_TIME, FREELANCE

