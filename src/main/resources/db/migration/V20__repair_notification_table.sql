-- V20: Repair/Ensure notification table exists
-- This migration ensures the notifications table and its columns exist,
-- handling cases where previous migrations might have been skipped or the table dropped.

CREATE TABLE IF NOT EXISTS notifications (
    id BIGSERIAL PRIMARY KEY,
    title VARCHAR(255) NOT NULL,
    message TEXT NOT NULL,
    type VARCHAR(50) NOT NULL,
    task_id BIGINT,
    user_id BIGINT NOT NULL,
    is_read BOOLEAN NOT NULL DEFAULT FALSE,
    is_sticky BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_notifications_user FOREIGN KEY (user_id) REFERENCES users(id)
);

-- Ensure indexes exist (Postgres 9.5+ supports IF NOT EXISTS for indexes)
CREATE INDEX IF NOT EXISTS idx_notifications_user_id ON notifications(user_id);
CREATE INDEX IF NOT EXISTS idx_notifications_created_at ON notifications(created_at);
CREATE INDEX IF NOT EXISTS idx_notifications_is_read ON notifications(is_read);
CREATE INDEX IF NOT EXISTS idx_notifications_sticky ON notifications(user_id, is_read, is_sticky) WHERE is_sticky = TRUE;

-- Ensure columns exist if table already existed but was incomplete
ALTER TABLE notifications ADD COLUMN IF NOT EXISTS is_sticky BOOLEAN NOT NULL DEFAULT FALSE;
