-- Create profiles table and migrate existing user profile fields
CREATE TABLE IF NOT EXISTS profiles
(
    id               BIGSERIAL PRIMARY KEY,
    avatar_url       VARCHAR(255),
    profession       VARCHAR(120),
    bio              TEXT,
    website_url      VARCHAR(255),
    github_url       VARCHAR(255),
    linkedin_url     VARCHAR(255),
    location         VARCHAR(120),
    experience_level VARCHAR(50),
    primary_stack    VARCHAR(255),
    availability     VARCHAR(60),
    skills           TEXT
);

-- Add profile_id to users
ALTER TABLE users
    ADD COLUMN IF NOT EXISTS profile_id BIGINT;

-- Create profiles from existing user columns if profile_id is null
INSERT INTO profiles (avatar_url, profession, bio, website_url, github_url, linkedin_url, location, experience_level,
                      primary_stack, availability, skills)
SELECT avatar_url,
       profession,
       bio,
       website_url,
       github_url,
       linkedin_url,
       location,
       experience_level,
       primary_stack,
       availability,
       skills
FROM users u
WHERE u.profile_id IS NULL;

-- Link users to newly created profiles (1:1 mapping by row order)
UPDATE users
SET profile_id = p.id
FROM (SELECT p.id, row_number() OVER () rn
      FROM profiles p) p
         JOIN (SELECT u.id, row_number() OVER () rn FROM users u WHERE u.profile_id IS NULL) u ON u.rn = p.rn
WHERE users.id = u.id
  AND users.profile_id IS NULL;

-- Remove old columns (optional: keep for rollback)
ALTER TABLE users
    DROP COLUMN IF EXISTS avatar_url;
ALTER TABLE users
    DROP COLUMN IF EXISTS profession;
ALTER TABLE users
    DROP COLUMN IF EXISTS bio;
ALTER TABLE users
    DROP COLUMN IF EXISTS website_url;
ALTER TABLE users
    DROP COLUMN IF EXISTS github_url;
ALTER TABLE users
    DROP COLUMN IF EXISTS linkedin_url;
ALTER TABLE users
    DROP COLUMN IF EXISTS location;
ALTER TABLE users
    DROP COLUMN IF EXISTS experience_level;
ALTER TABLE users
    DROP COLUMN IF EXISTS primary_stack;
ALTER TABLE users
    DROP COLUMN IF EXISTS availability;
ALTER TABLE users
    DROP COLUMN IF EXISTS skills;

-- Foreign key constraint
ALTER TABLE users
    ADD CONSTRAINT fk_users_profile FOREIGN KEY (profile_id) REFERENCES profiles (id) ON DELETE SET NULL;

CREATE INDEX IF NOT EXISTS idx_users_profile_id ON users (profile_id);

