-- Add AUTH_PENDING to task status enum
-- Note: PostgreSQL doesn't support ALTER TYPE ADD VALUE in a transaction
-- This migration should be run separately if using PostgreSQL
-- For H2 or other databases, this will work fine

-- For PostgreSQL, you might need to run this outside a transaction:
-- ALTER TYPE task_status ADD VALUE IF NOT EXISTS 'AUTH_PENDING';

-- For H2 and similar databases, we'll handle this at the application level
-- The enum is defined in the Java code, so no SQL changes needed for H2
