-- V27: Expand Squad entity with new fields for enhanced squad management

-- Add new columns to squads table
ALTER TABLE squads ADD COLUMN IF NOT EXISTS squad_type VARCHAR(50) DEFAULT 'FULLSTACK';
ALTER TABLE squads ADD COLUMN IF NOT EXISTS tech_stack VARCHAR(500);
ALTER TABLE squads ADD COLUMN IF NOT EXISTS business_area VARCHAR(200);
ALTER TABLE squads ADD COLUMN IF NOT EXISTS goal TEXT;
ALTER TABLE squads ADD COLUMN IF NOT EXISTS active BOOLEAN DEFAULT true;
ALTER TABLE squads ADD COLUMN IF NOT EXISTS max_members INTEGER;
ALTER TABLE squads ADD COLUMN IF NOT EXISTS deactivated_at TIMESTAMP;

-- Add indexes for common queries
CREATE INDEX IF NOT EXISTS idx_squads_type ON squads(squad_type);
CREATE INDEX IF NOT EXISTS idx_squads_active ON squads(active);
CREATE INDEX IF NOT EXISTS idx_squads_lead_active ON squads(lead_id, active);

-- Add constraint for max_members (optional, between 2 and 50)
-- Note: PostgreSQL-specific, adjust for other databases
-- ALTER TABLE squads ADD CONSTRAINT chk_max_members CHECK (max_members IS NULL OR (max_members >= 2 AND max_members <= 50));

-- Update existing squads to have default type
UPDATE squads SET squad_type = 'FULLSTACK' WHERE squad_type IS NULL;
UPDATE squads SET active = true WHERE active IS NULL;

-- Add comments for documentation
COMMENT ON COLUMN squads.squad_type IS 'Type of squad: BACKEND, FRONTEND, FULLSTACK, DEVOPS, MOBILE, QA, DATA, SECURITY, PLATFORM, OTHER';
COMMENT ON COLUMN squads.tech_stack IS 'Technologies used by the squad (comma-separated or formatted list)';
COMMENT ON COLUMN squads.business_area IS 'Business area or domain the squad focuses on';
COMMENT ON COLUMN squads.goal IS 'Squad objectives and mission statement';
COMMENT ON COLUMN squads.active IS 'Whether the squad is currently active';
COMMENT ON COLUMN squads.max_members IS 'Maximum number of members allowed in the squad';
COMMENT ON COLUMN squads.deactivated_at IS 'Timestamp when the squad was deactivated';
