-- Migration to add GitHub integration fields to users and profiles
-- Date: 2025-11-15

-- Add new fields to profiles table for GitHub data
ALTER TABLE profiles ADD COLUMN IF NOT EXISTS github_login VARCHAR(255);
ALTER TABLE profiles ADD COLUMN IF NOT EXISTS github_name VARCHAR(255);
ALTER TABLE profiles ADD COLUMN IF NOT EXISTS github_company VARCHAR(255);
ALTER TABLE profiles ADD COLUMN IF NOT EXISTS twitter_username VARCHAR(100);
ALTER TABLE profiles ADD COLUMN IF NOT EXISTS github_html_url VARCHAR(500);
ALTER TABLE profiles ADD COLUMN IF NOT EXISTS hireable BOOLEAN DEFAULT false;
ALTER TABLE profiles ADD COLUMN IF NOT EXISTS public_repos INTEGER DEFAULT 0;
ALTER TABLE profiles ADD COLUMN IF NOT EXISTS public_gists INTEGER DEFAULT 0;
ALTER TABLE profiles ADD COLUMN IF NOT EXISTS followers INTEGER DEFAULT 0;
ALTER TABLE profiles ADD COLUMN IF NOT EXISTS following INTEGER DEFAULT 0;
ALTER TABLE profiles ADD COLUMN IF NOT EXISTS github_created_at TIMESTAMP;
ALTER TABLE profiles ADD COLUMN IF NOT EXISTS github_updated_at TIMESTAMP;

-- Add index for GitHub login for faster lookups
CREATE INDEX IF NOT EXISTS idx_profiles_github_login ON profiles(github_login);

-- Add comments to document the fields
COMMENT ON COLUMN profiles.github_login IS 'GitHub username imported from API';
COMMENT ON COLUMN profiles.github_name IS 'Display name from GitHub (may contain emojis and special characters)';
COMMENT ON COLUMN profiles.github_company IS 'Company from GitHub profile';
COMMENT ON COLUMN profiles.twitter_username IS 'Twitter/X username from GitHub';
COMMENT ON COLUMN profiles.github_html_url IS 'Full GitHub profile URL';
COMMENT ON COLUMN profiles.hireable IS 'GitHub hireable status';
COMMENT ON COLUMN profiles.public_repos IS 'Number of public repositories';
COMMENT ON COLUMN profiles.public_gists IS 'Number of public gists';
COMMENT ON COLUMN profiles.followers IS 'Number of followers on GitHub';
COMMENT ON COLUMN profiles.following IS 'Number of users following on GitHub';
COMMENT ON COLUMN profiles.github_created_at IS 'GitHub account creation date';
COMMENT ON COLUMN profiles.github_updated_at IS 'Last GitHub profile update';
