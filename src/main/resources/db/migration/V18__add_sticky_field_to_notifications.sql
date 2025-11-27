-- V18: Add sticky field to notifications table
-- Sticky notifications require manual dismissal by the user

ALTER TABLE notifications ADD COLUMN IF NOT EXISTS is_sticky BOOLEAN NOT NULL DEFAULT FALSE;

-- Create index for faster sticky notification queries
CREATE INDEX IF NOT EXISTS idx_notifications_sticky ON notifications(user_id, is_read, is_sticky) WHERE is_sticky = TRUE;

COMMENT ON COLUMN notifications.is_sticky IS 'Indicates if the notification is sticky (requires manual dismissal). Used for critical alerts like PENDING and OVERDUE tasks.';
