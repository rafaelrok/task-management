-- Add extra time tracking fields to tasks
ALTER TABLE tasks
    ADD COLUMN IF NOT EXISTS extra_time_minutes      INTEGER DEFAULT 0,
    ADD COLUMN IF NOT EXISTS extension_justification TEXT;

