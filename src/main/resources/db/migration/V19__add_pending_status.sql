-- Add PENDING status to the task status constraint
-- PENDING is used when execution time is completed but due date hasn't passed yet

ALTER TABLE tasks
    DROP CONSTRAINT IF EXISTS tasks_status_valid;

ALTER TABLE tasks
    ADD CONSTRAINT tasks_status_valid CHECK (status IN
        ('TODO', 'IN_PROGRESS', 'IN_PAUSE', 'PENDING', 'DONE', 'CANCELLED', 'OVERDUE'));
