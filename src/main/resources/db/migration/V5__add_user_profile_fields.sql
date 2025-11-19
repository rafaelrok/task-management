-- Add user profile fields
ALTER TABLE users
    ADD COLUMN IF NOT EXISTS avatar_url   VARCHAR(255),
    ADD COLUMN IF NOT EXISTS profession   VARCHAR(120),
    ADD COLUMN IF NOT EXISTS skills       TEXT,
    ADD COLUMN IF NOT EXISTS bio          TEXT,
    ADD COLUMN IF NOT EXISTS website_url  VARCHAR(255),
    ADD COLUMN IF NOT EXISTS github_url   VARCHAR(255),
    ADD COLUMN IF NOT EXISTS linkedin_url VARCHAR(255);

