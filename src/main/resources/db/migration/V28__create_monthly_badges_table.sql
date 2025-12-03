-- V28: Create UserMonthlyBadge table for monthly badge tracking

-- Create user_monthly_badges table
CREATE TABLE IF NOT EXISTS user_monthly_badges (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    reference_year INTEGER NOT NULL,
    reference_month INTEGER NOT NULL,
    badge_level VARCHAR(20) NOT NULL DEFAULT 'NONE',
    tasks_completed_in_squads INTEGER NOT NULL DEFAULT 0,
    calculated_at TIMESTAMP,
    is_current_month BOOLEAN DEFAULT false,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    
    -- Unique constraint: one badge per user per month
    CONSTRAINT uk_user_monthly_badge UNIQUE (user_id, reference_year, reference_month)
);

-- Add indexes for common queries
CREATE INDEX IF NOT EXISTS idx_umb_user_id ON user_monthly_badges(user_id);
CREATE INDEX IF NOT EXISTS idx_umb_year_month ON user_monthly_badges(reference_year, reference_month);
CREATE INDEX IF NOT EXISTS idx_umb_badge_level ON user_monthly_badges(badge_level);
CREATE INDEX IF NOT EXISTS idx_umb_current_month ON user_monthly_badges(is_current_month) WHERE is_current_month = true;
CREATE INDEX IF NOT EXISTS idx_umb_user_current ON user_monthly_badges(user_id, is_current_month) WHERE is_current_month = true;

-- Add constraint for valid badge levels
ALTER TABLE user_monthly_badges ADD CONSTRAINT chk_badge_level 
    CHECK (badge_level IN ('NONE', 'BRONZE', 'PRATA', 'OURO', 'DIAMANTE'));

-- Add constraint for valid month range
ALTER TABLE user_monthly_badges ADD CONSTRAINT chk_reference_month 
    CHECK (reference_month >= 1 AND reference_month <= 12);

-- Add comments for documentation
COMMENT ON TABLE user_monthly_badges IS 'Tracks monthly badge progress for users based on squad task completions';
COMMENT ON COLUMN user_monthly_badges.badge_level IS 'Badge level: NONE (0), BRONZE (1-10), PRATA (11-25), OURO (26-45), DIAMANTE (46+)';
COMMENT ON COLUMN user_monthly_badges.tasks_completed_in_squads IS 'Number of squad tasks completed in the reference month';
COMMENT ON COLUMN user_monthly_badges.is_current_month IS 'Flag to identify the current month record for quick access';
COMMENT ON COLUMN user_monthly_badges.calculated_at IS 'Timestamp of last level recalculation';
