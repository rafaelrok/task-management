-- Remove description length constraint and change to TEXT type for unlimited rich HTML content
-- Rich editor generates HTML with formatting (code blocks, syntax highlighting, etc.)

-- 1) Drop dependent views before altering column type
DROP VIEW IF EXISTS v_tasks_requiring_attention CASCADE;
DROP VIEW IF EXISTS v_active_tasks CASCADE;
DROP VIEW IF EXISTS v_overdue_tasks CASCADE;

-- 2) Drop the old 500 char constraint
ALTER TABLE tasks
    DROP CONSTRAINT IF EXISTS tasks_description_length;

-- 3) Change column type from VARCHAR(8000) to TEXT (unlimited)
ALTER TABLE tasks
    ALTER COLUMN description TYPE TEXT;

COMMENT ON COLUMN tasks.description IS 'Rich HTML content from editor - no length limit';

-- 4) Recreate v_active_tasks view
CREATE OR REPLACE VIEW v_active_tasks AS
SELECT t.id,
       t.title,
       t.description,
       t.status,
       t.priority,
       t.created_at,
       t.updated_at,
       t.due_date,
       t.scheduled_start_at,
       c.name     as category_name,
       u.username as assigned_to,
       u.email    as assigned_to_email,
       CASE
           WHEN t.due_date IS NOT NULL AND t.due_date < CURRENT_TIMESTAMP AND t.status != 'DONE'
               THEN true
           ELSE false
           END    as is_overdue
FROM tasks t
         LEFT JOIN categories c ON t.category_id = c.id
         LEFT JOIN users u ON t.assigned_user_id = u.id
WHERE t.status IN ('TODO', 'IN_PROGRESS', 'IN_PAUSE');

COMMENT ON VIEW v_active_tasks IS 'View showing all active tasks (including paused) with related information';

-- 5) Recreate v_overdue_tasks view
CREATE OR REPLACE VIEW v_overdue_tasks AS
SELECT t.id,
       t.title,
       t.status,
       t.priority,
       t.due_date,
       u.username                                         as assigned_to,
       EXTRACT(DAY FROM (CURRENT_TIMESTAMP - t.due_date)) as days_overdue
FROM tasks t
         LEFT JOIN users u ON t.assigned_user_id = u.id
WHERE t.due_date < CURRENT_TIMESTAMP
  AND t.status NOT IN ('DONE', 'CANCELLED');

COMMENT ON VIEW v_overdue_tasks IS 'View showing all overdue tasks with days overdue calculation';

-- 6) Recreate v_tasks_requiring_attention view
CREATE OR REPLACE VIEW v_tasks_requiring_attention AS
SELECT t.id,
       t.title,
       t.description,
       t.status,
       t.priority,
       t.due_date,
       t.scheduled_start_at,
       c.name     as category_name,
       u.username as assigned_to,
       CASE
           WHEN t.due_date < CURRENT_TIMESTAMP THEN 'OVERDUE'
           WHEN t.due_date < (CURRENT_TIMESTAMP + INTERVAL '24 hours') THEN 'DUE_SOON'
           WHEN t.priority = 'URGENT' THEN 'URGENT'
           ELSE 'NORMAL'
           END    as attention_reason
FROM tasks t
         LEFT JOIN categories c ON t.category_id = c.id
         LEFT JOIN users u ON t.assigned_user_id = u.id
WHERE t.status NOT IN ('DONE', 'CANCELLED')
  AND (
    t.due_date < (CURRENT_TIMESTAMP + INTERVAL '24 hours')
        OR t.priority = 'URGENT'
    );

COMMENT ON VIEW v_tasks_requiring_attention IS 'View showing tasks that need immediate attention (overdue, due soon, or urgent)';

