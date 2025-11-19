-- Extend tasks.description to 8000 and add pomodoro scheduling fields safely
-- Drop dependent views before altering column type, then recreate them

-- 1) Drop dependent views if exist
DROP VIEW IF EXISTS v_tasks_requiring_attention CASCADE;
DROP VIEW IF EXISTS v_active_tasks CASCADE;
DROP VIEW IF EXISTS v_overdue_tasks CASCADE;

-- 2) Alter column type to larger VARCHAR
ALTER TABLE tasks
    ALTER COLUMN description TYPE VARCHAR(8000);

-- 3) Add new columns if not exist
ALTER TABLE tasks
    ADD COLUMN IF NOT EXISTS scheduled_start_at TIMESTAMP NULL;
ALTER TABLE tasks
    ADD COLUMN IF NOT EXISTS pomodoro_minutes INTEGER NULL;

-- 4) Recreate views with potential new fields
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

CREATE OR REPLACE VIEW v_overdue_tasks AS
SELECT t.id,
       t.title,
       t.status,
       t.priority,
       t.due_date,
       u.username                                       as assigned_to,
       c.name                                           as category_name,
       EXTRACT(DAY FROM CURRENT_TIMESTAMP - t.due_date) as days_overdue
FROM tasks t
         LEFT JOIN users u ON t.assigned_user_id = u.id
         LEFT JOIN categories c ON t.category_id = c.id
WHERE t.due_date < CURRENT_TIMESTAMP
  AND t.status NOT IN ('DONE', 'CANCELLED');

COMMENT ON VIEW v_overdue_tasks IS 'View showing overdue tasks with days overdue calculation';

CREATE OR REPLACE VIEW v_tasks_requiring_attention AS
SELECT t.id,
       t.title,
       t.description,
       t.status,
       t.priority,
       t.due_date,
       u.username as assigned_to,
       u.email    as assigned_to_email,
       c.name     as category_name,
       CASE
           WHEN t.due_date < CURRENT_TIMESTAMP AND t.status NOT IN ('DONE', 'CANCELLED') THEN 'OVERDUE'
           WHEN t.priority = 'URGENT' THEN 'URGENT'
           WHEN t.priority = 'HIGH' THEN 'HIGH_PRIORITY'
           ELSE 'NORMAL'
           END    as attention_reason,
       CASE
           WHEN t.due_date < CURRENT_TIMESTAMP THEN EXTRACT(DAY FROM CURRENT_TIMESTAMP - t.due_date)
           ELSE 0
           END    as days_overdue
FROM tasks t
         LEFT JOIN users u ON t.assigned_user_id = u.id
         LEFT JOIN categories c ON t.category_id = c.id
WHERE (t.priority IN ('HIGH', 'URGENT') AND t.status IN ('TODO', 'IN_PROGRESS', 'IN_PAUSE'))
   OR (t.due_date < CURRENT_TIMESTAMP AND t.status NOT IN ('DONE', 'CANCELLED'))
ORDER BY CASE
             WHEN t.due_date < CURRENT_TIMESTAMP THEN 1
             WHEN t.priority = 'URGENT' THEN 2
             WHEN t.priority = 'HIGH' THEN 3
             ELSE 4
             END,
         t.due_date ASC NULLS LAST;

COMMENT ON VIEW v_tasks_requiring_attention IS 'Tasks that need immediate attention (overdue or high priority incl. paused)';
