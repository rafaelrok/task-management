-- ============================================================================
-- Create Users Table
-- ============================================================================
CREATE TABLE IF NOT EXISTS users (
    id BIGSERIAL PRIMARY KEY,
    username VARCHAR(255) NOT NULL UNIQUE,
    email VARCHAR(255) NOT NULL UNIQUE,
    full_name VARCHAR(255),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT users_username_not_empty CHECK (username <> ''),
    CONSTRAINT users_email_not_empty CHECK (email <> ''),
    CONSTRAINT users_email_format CHECK (email ~* '^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\.[A-Za-z]{2,}$')
);

CREATE INDEX idx_users_username ON users(username);
CREATE INDEX idx_users_email ON users(email);

COMMENT ON TABLE users IS 'Stores user information for task assignment';
COMMENT ON COLUMN users.id IS 'Unique identifier for user';
COMMENT ON COLUMN users.username IS 'Unique username for the user';
COMMENT ON COLUMN users.email IS 'Unique email address for the user';
COMMENT ON COLUMN users.full_name IS 'Full name of the user';

-- ============================================================================
-- Create Categories Table
-- ============================================================================
CREATE TABLE IF NOT EXISTS categories (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL UNIQUE,
    description VARCHAR(255),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT categories_name_not_empty CHECK (name <> '')
);

CREATE INDEX idx_categories_name ON categories(name);

COMMENT ON TABLE categories IS 'Stores task categories for organization';
COMMENT ON COLUMN categories.id IS 'Unique identifier for category';
COMMENT ON COLUMN categories.name IS 'Unique name of the category';
COMMENT ON COLUMN categories.description IS 'Description of the category';

-- ============================================================================
-- Create Tasks Table
-- ============================================================================
CREATE TABLE IF NOT EXISTS tasks (
    id BIGSERIAL PRIMARY KEY,
    title VARCHAR(100) NOT NULL,
    description VARCHAR(500),
    status VARCHAR(20) NOT NULL DEFAULT 'TODO',
    priority VARCHAR(20) NOT NULL DEFAULT 'MEDIUM',
    category_id BIGINT,
    assigned_user_id BIGINT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    due_date TIMESTAMP,
    
    CONSTRAINT fk_tasks_category FOREIGN KEY (category_id)
        REFERENCES categories(id) ON DELETE SET NULL ON UPDATE CASCADE,
    CONSTRAINT fk_tasks_user FOREIGN KEY (assigned_user_id) 
        REFERENCES users(id) ON DELETE SET NULL ON UPDATE CASCADE,
    
    CONSTRAINT tasks_title_not_empty CHECK (title <> ''),
    CONSTRAINT tasks_title_length CHECK (LENGTH(title) BETWEEN 3 AND 100),
    CONSTRAINT tasks_description_length CHECK (description IS NULL OR LENGTH(description) <= 500),
    CONSTRAINT tasks_status_valid CHECK (status IN ('TODO', 'IN_PROGRESS', 'DONE', 'CANCELLED')),
    CONSTRAINT tasks_priority_valid CHECK (priority IN ('LOW', 'MEDIUM', 'HIGH', 'URGENT')),
    CONSTRAINT tasks_due_date_valid CHECK (due_date IS NULL OR due_date > created_at)
);

CREATE INDEX idx_tasks_status ON tasks(status);
CREATE INDEX idx_tasks_priority ON tasks(priority);
CREATE INDEX idx_tasks_category_id ON tasks(category_id);
CREATE INDEX idx_tasks_assigned_user_id ON tasks(assigned_user_id);
CREATE INDEX idx_tasks_created_at ON tasks(created_at);
CREATE INDEX idx_tasks_due_date ON tasks(due_date);
CREATE INDEX idx_tasks_status_priority ON tasks(status, priority);

COMMENT ON TABLE tasks IS 'Stores all tasks in the system';
COMMENT ON COLUMN tasks.id IS 'Unique identifier for task';
COMMENT ON COLUMN tasks.title IS 'Title of the task (3-100 characters)';
COMMENT ON COLUMN tasks.description IS 'Detailed description of the task (max 500 characters)';
COMMENT ON COLUMN tasks.status IS 'Current status: TODO, IN_PROGRESS, DONE, CANCELLED';
COMMENT ON COLUMN tasks.priority IS 'Task priority: LOW, MEDIUM, HIGH, URGENT';
COMMENT ON COLUMN tasks.category_id IS 'Reference to category table';
COMMENT ON COLUMN tasks.assigned_user_id IS 'Reference to user assigned to this task';
COMMENT ON COLUMN tasks.created_at IS 'Timestamp when task was created';
COMMENT ON COLUMN tasks.updated_at IS 'Timestamp when task was last updated';
COMMENT ON COLUMN tasks.due_date IS 'Due date for task completion';

-- ============================================================================
-- Create Update Timestamp Triggers
-- ============================================================================
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER update_users_updated_at
    BEFORE UPDATE ON users
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_categories_updated_at
    BEFORE UPDATE ON categories
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_tasks_updated_at
    BEFORE UPDATE ON tasks
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();

-- ============================================================================
-- Create Views for Common Queries
-- ============================================================================
CREATE OR REPLACE VIEW v_active_tasks AS
SELECT 
    t.id,
    t.title,
    t.description,
    t.status,
    t.priority,
    t.created_at,
    t.updated_at,
    t.due_date,
    c.name as category_name,
    u.username as assigned_to,
    u.email as assigned_to_email,
    CASE 
        WHEN t.due_date IS NOT NULL AND t.due_date < CURRENT_TIMESTAMP AND t.status != 'DONE' 
        THEN true 
        ELSE false 
    END as is_overdue
FROM tasks t
LEFT JOIN categories c ON t.category_id = c.id
LEFT JOIN users u ON t.assigned_user_id = u.id
WHERE t.status IN ('TODO', 'IN_PROGRESS');

COMMENT ON VIEW v_active_tasks IS 'View showing all active (not completed or cancelled) tasks with related information';

CREATE OR REPLACE VIEW v_overdue_tasks AS
SELECT 
    t.id,
    t.title,
    t.status,
    t.priority,
    t.due_date,
    u.username as assigned_to,
    c.name as category_name,
    EXTRACT(DAY FROM CURRENT_TIMESTAMP - t.due_date) as days_overdue
FROM tasks t
LEFT JOIN users u ON t.assigned_user_id = u.id
LEFT JOIN categories c ON t.category_id = c.id
WHERE t.due_date < CURRENT_TIMESTAMP 
  AND t.status NOT IN ('DONE', 'CANCELLED');

COMMENT ON VIEW v_overdue_tasks IS 'View showing overdue tasks with days overdue calculation';