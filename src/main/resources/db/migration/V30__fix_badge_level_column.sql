-- V30: Fix badge_level column name in user_monthly_badges table

-- PostgreSQL: Rename column from 'level' to 'badge_level'
DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'user_monthly_badges' AND column_name = 'level') THEN
        ALTER TABLE user_monthly_badges RENAME COLUMN level TO badge_level;
    END IF;
END $$;

-- Drop and recreate index with correct name
DROP INDEX IF EXISTS idx_umb_level;

CREATE INDEX IF NOT EXISTS idx_umb_badge_level ON user_monthly_badges (badge_level);

-- Drop and recreate constraint with correct column name
ALTER TABLE user_monthly_badges
DROP CONSTRAINT IF EXISTS chk_badge_level;

ALTER TABLE user_monthly_badges
ADD CONSTRAINT chk_badge_level CHECK (
    badge_level IN (
        'NONE',
        'BRONZE',
        'PRATA',
        'OURO',
        'DIAMANTE'
    )
);