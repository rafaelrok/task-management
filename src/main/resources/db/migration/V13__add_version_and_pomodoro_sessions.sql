-- Add optimistic locking column to tasks
ALTER TABLE tasks
    ADD COLUMN IF NOT EXISTS version BIGINT DEFAULT 0 NOT NULL;

-- Create pomodoro_sessions table to store history of pomodoro pauses
CREATE TABLE IF NOT EXISTS pomodoro_sessions
(
    id               BIGSERIAL PRIMARY KEY,
    task_id          BIGINT    NOT NULL REFERENCES tasks (id) ON DELETE CASCADE,
    started_at       TIMESTAMP NOT NULL,
    ended_at         TIMESTAMP NULL,
    duration_seconds BIGINT    NULL,
    created_at       TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Index for quick lookup of active sessions (ended_at IS NULL)
CREATE INDEX IF NOT EXISTS idx_pomodoro_active ON pomodoro_sessions (task_id) WHERE ended_at IS NULL;

