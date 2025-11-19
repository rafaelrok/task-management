-- Expand task status constraint to include IN_PAUSE and OVERDUE
ALTER TABLE tasks
    DROP CONSTRAINT IF EXISTS tasks_status_valid;
ALTER TABLE tasks
    ADD CONSTRAINT tasks_status_valid CHECK (status IN
                                             ('TODO', 'IN_PROGRESS', 'IN_PAUSE', 'DONE', 'CANCELLED', 'OVERDUE'));

