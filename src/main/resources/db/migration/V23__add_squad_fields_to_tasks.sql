-- Add squad and gamification fields to tasks table
ALTER TABLE tasks ADD COLUMN IF NOT EXISTS squad_id BIGINT;
ALTER TABLE tasks ADD COLUMN IF NOT EXISTS created_by_id BIGINT;
ALTER TABLE tasks ADD COLUMN IF NOT EXISTS completed_at TIMESTAMP;

-- Add foreign keys
ALTER TABLE tasks ADD CONSTRAINT fk_task_squad FOREIGN KEY (squad_id) REFERENCES squads(id) ON DELETE SET NULL;
ALTER TABLE tasks ADD CONSTRAINT fk_task_created_by FOREIGN KEY (created_by_id) REFERENCES users(id) ON DELETE SET NULL;

-- Create indexes
CREATE INDEX IF NOT EXISTS idx_task_squad ON tasks(squad_id);
CREATE INDEX IF NOT EXISTS idx_task_created_by ON tasks(created_by_id);
