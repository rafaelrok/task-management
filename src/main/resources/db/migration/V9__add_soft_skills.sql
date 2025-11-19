-- Add soft skills column to profiles
ALTER TABLE profiles ADD COLUMN IF NOT EXISTS soft_skills VARCHAR(1000);

COMMENT ON COLUMN profiles.soft_skills IS 'Soft skills armazenadas como lista separada por vírgulas';
