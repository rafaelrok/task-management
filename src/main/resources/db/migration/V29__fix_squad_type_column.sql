-- V29: Fix squad_type column name (rename from 'type' to 'squad_type')

-- PostgreSQL: Rename column if 'type' exists
DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'squads' AND column_name = 'type') THEN
        ALTER TABLE squads RENAME COLUMN type TO squad_type;
    END IF;
END $$;

-- Recreate index with correct name
DROP INDEX IF EXISTS idx_squads_type;

CREATE INDEX IF NOT EXISTS idx_squads_squad_type ON squads (squad_type);

-- Update default values
UPDATE squads SET squad_type = 'FULLSTACK' WHERE squad_type IS NULL;