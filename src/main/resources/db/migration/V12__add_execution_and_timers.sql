-- Add execution time and timer tracking columns to tasks
ALTER TABLE tasks
    ADD COLUMN IF NOT EXISTS execution_time_minutes INTEGER,
    ADD COLUMN IF NOT EXISTS main_started_at        TIMESTAMP,
    ADD COLUMN IF NOT EXISTS main_elapsed_seconds   BIGINT DEFAULT 0 NOT NULL,
    ADD COLUMN IF NOT EXISTS pomodoro_until         TIMESTAMP;

