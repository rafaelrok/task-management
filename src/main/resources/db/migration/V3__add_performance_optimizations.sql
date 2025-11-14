-- ============================================================================
-- Additional Composite Indexes for Query Optimization
-- ============================================================================
CREATE INDEX IF NOT EXISTS idx_tasks_status_user ON tasks (status, assigned_user_id)
    WHERE assigned_user_id IS NOT NULL;
CREATE INDEX IF NOT EXISTS idx_tasks_priority_due_date ON tasks (priority, due_date)
    WHERE due_date IS NOT NULL;
CREATE INDEX IF NOT EXISTS idx_tasks_overdue ON tasks (due_date, status)
    WHERE status NOT IN ('DONE', 'CANCELLED');
CREATE INDEX IF NOT EXISTS idx_tasks_category_status ON tasks (category_id, status);
CREATE INDEX IF NOT EXISTS idx_tasks_active ON tasks (id, status, priority, due_date)
    WHERE status IN ('TODO', 'IN_PROGRESS');

-- ============================================================================
-- Create Functions for Business Logic
-- ============================================================================
CREATE OR REPLACE FUNCTION get_user_task_counts(user_id_param BIGINT)
    RETURNS TABLE
            (
                status     VARCHAR,
                task_count BIGINT
            )
AS
$$
BEGIN
    RETURN QUERY
        SELECT t.status,
               COUNT(*)::BIGINT as task_count
        FROM tasks t
        WHERE t.assigned_user_id = user_id_param
        GROUP BY t.status;
END;
$$ LANGUAGE plpgsql STABLE;

COMMENT ON FUNCTION get_user_task_counts IS 'Returns task count by status for a specific user';

CREATE OR REPLACE FUNCTION get_user_overdue_count(user_id_param BIGINT)
    RETURNS BIGINT AS
$$
DECLARE
    overdue_count BIGINT;
BEGIN
    SELECT COUNT(*)
    INTO overdue_count
    FROM tasks
    WHERE assigned_user_id = user_id_param
      AND due_date < CURRENT_TIMESTAMP
      AND status NOT IN ('DONE', 'CANCELLED');

    RETURN overdue_count;
END;
$$ LANGUAGE plpgsql STABLE;

COMMENT ON FUNCTION get_user_overdue_count IS 'Returns count of overdue tasks for a specific user';

CREATE OR REPLACE FUNCTION get_category_completion_rate(category_id_param BIGINT)
    RETURNS NUMERIC AS
$$
DECLARE
    total_tasks     INTEGER;
    completed_tasks INTEGER;
BEGIN
    SELECT COUNT(*)
    INTO total_tasks
    FROM tasks
    WHERE category_id = category_id_param;

    IF total_tasks = 0 THEN
        RETURN 0;
    END IF;

    SELECT COUNT(*)
    INTO completed_tasks
    FROM tasks
    WHERE category_id = category_id_param
      AND status = 'DONE';

    RETURN ROUND((completed_tasks::NUMERIC / total_tasks::NUMERIC) * 100, 2);
END;
$$ LANGUAGE plpgsql STABLE;

COMMENT ON FUNCTION get_category_completion_rate IS 'Returns completion rate percentage for a category';

-- ============================================================================
-- Create Additional Views for Reporting
-- ============================================================================
CREATE OR REPLACE VIEW v_user_task_statistics AS
SELECT u.id                                                 as user_id,
       u.username,
       u.email,
       COUNT(t.id)                                          as total_tasks,
       COUNT(CASE WHEN t.status = 'TODO' THEN 1 END)        as todo_count,
       COUNT(CASE WHEN t.status = 'IN_PROGRESS' THEN 1 END) as in_progress_count,
       COUNT(CASE WHEN t.status = 'DONE' THEN 1 END)        as done_count,
       COUNT(CASE WHEN t.status = 'CANCELLED' THEN 1 END)   as cancelled_count,
       COUNT(CASE
                 WHEN t.due_date < CURRENT_TIMESTAMP AND t.status NOT IN ('DONE', 'CANCELLED')
                     THEN 1 END)                            as overdue_count,
       COUNT(CASE WHEN t.priority = 'URGENT' THEN 1 END)    as urgent_count,
       COUNT(CASE WHEN t.priority = 'HIGH' THEN 1 END)      as high_priority_count
FROM users u
         LEFT JOIN tasks t ON u.id = t.assigned_user_id
GROUP BY u.id, u.username, u.email;

COMMENT ON VIEW v_user_task_statistics IS 'Comprehensive task statistics for each user';

CREATE OR REPLACE VIEW v_category_task_statistics AS
SELECT c.id                                                            as category_id,
       c.name                                                          as category_name,
       c.description,
       COUNT(t.id)                                                     as total_tasks,
       COUNT(CASE WHEN t.status = 'DONE' THEN 1 END)                   as completed_tasks,
       COUNT(CASE WHEN t.status IN ('TODO', 'IN_PROGRESS') THEN 1 END) as active_tasks,
       CASE
           WHEN COUNT(t.id) > 0 THEN
               ROUND((COUNT(CASE WHEN t.status = 'DONE' THEN 1 END)::NUMERIC / COUNT(t.id)::NUMERIC) * 100, 2)
           ELSE 0
           END                                                         as completion_rate,
       MIN(t.created_at)                                               as first_task_date,
       MAX(t.created_at)                                               as last_task_date
FROM categories c
         LEFT JOIN tasks t ON c.id = t.category_id
GROUP BY c.id, c.name, c.description;

COMMENT ON VIEW v_category_task_statistics IS 'Task statistics and metrics for each category';

CREATE OR REPLACE VIEW v_daily_task_statistics AS
SELECT DATE(created_at)                                as task_date,
       COUNT(*)                                        as tasks_created,
       COUNT(CASE WHEN priority = 'URGENT' THEN 1 END) as urgent_tasks,
       COUNT(CASE WHEN priority = 'HIGH' THEN 1 END)   as high_priority_tasks,
       COUNT(DISTINCT assigned_user_id)                as unique_assignees,
       COUNT(DISTINCT category_id)                     as categories_used
FROM tasks
GROUP BY DATE(created_at)
ORDER BY task_date DESC;

COMMENT ON VIEW v_daily_task_statistics IS 'Daily statistics of task creation and distribution';

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
WHERE (t.priority IN ('HIGH', 'URGENT') AND t.status IN ('TODO', 'IN_PROGRESS'))
   OR (t.due_date < CURRENT_TIMESTAMP AND t.status NOT IN ('DONE', 'CANCELLED'))
ORDER BY CASE
             WHEN t.due_date < CURRENT_TIMESTAMP THEN 1
             WHEN t.priority = 'URGENT' THEN 2
             WHEN t.priority = 'HIGH' THEN 3
             ELSE 4
             END,
         t.due_date ASC NULLS LAST;

COMMENT ON VIEW v_tasks_requiring_attention IS 'Tasks that need immediate attention (overdue or high priority)';

-- ============================================================================
-- Adicionar exibição materializada para desempenho (opcional - para grandes conjuntos de dados)
-- ============================================================================
CREATE MATERIALIZED VIEW mv_task_analytics AS
SELECT COUNT(*)                                           as total_tasks,
       COUNT(CASE WHEN status = 'TODO' THEN 1 END)        as todo_tasks,
       COUNT(CASE WHEN status = 'IN_PROGRESS' THEN 1 END) as in_progress_tasks,
       COUNT(CASE WHEN status = 'DONE' THEN 1 END)        as done_tasks,
       COUNT(CASE WHEN status = 'CANCELLED' THEN 1 END)   as cancelled_tasks,
       COUNT(CASE
                 WHEN due_date < CURRENT_TIMESTAMP AND status NOT IN ('DONE', 'CANCELLED')
                     THEN 1 END)                          as overdue_tasks,
       AVG(EXTRACT(DAY FROM updated_at - created_at))     as avg_completion_days,
       COUNT(DISTINCT assigned_user_id)                   as active_users,
       COUNT(DISTINCT category_id)                        as active_categories
FROM tasks;

CREATE UNIQUE INDEX ON mv_task_analytics ((1));

COMMENT ON MATERIALIZED VIEW mv_task_analytics IS 'Cached analytics data for dashboard - refresh periodically';
