-- Add role enum to users table
ALTER TABLE users ADD COLUMN IF NOT EXISTS role VARCHAR(20) NOT NULL DEFAULT 'MEMBER';

-- Update existing users to have MEMBER role if they don't have one
UPDATE users SET role = 'MEMBER' WHERE role IS NULL OR role = 'ROLE_USER';
