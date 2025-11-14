-- ============================================================================
-- Insert Sample Users
-- ============================================================================
INSERT INTO users (username, email, full_name)
VALUES ('admin', 'admin@taskmanagement.com', 'System Administrator'),
       ('john.doe', 'john.doe@example.com', 'John Doe'),
       ('jane.smith', 'jane.smith@example.com', 'Jane Smith'),
       ('bob.johnson', 'bob.johnson@example.com', 'Bob Johnson'),
       ('alice.williams', 'alice.williams@example.com', 'Alice Williams')
ON CONFLICT (username) DO NOTHING;

-- ============================================================================
-- Insert Sample Categories
-- ============================================================================
INSERT INTO categories (name, description)
VALUES ('Development', 'Software development and programming tasks'),
       ('Testing', 'Quality assurance and testing activities'),
       ('Documentation', 'Documentation and knowledge base tasks'),
       ('Meeting', 'Meetings and discussions'),
       ('Bug Fix', 'Bug fixes and issues resolution'),
       ('Feature', 'New feature implementation'),
       ('Research', 'Research and investigation tasks'),
       ('DevOps', 'DevOps and infrastructure tasks'),
       ('Design', 'UI/UX and design tasks'),
       ('Personal', 'Personal tasks and reminders')
ON CONFLICT (name) DO NOTHING;

-- ============================================================================
-- Insert Sample Tasks
-- ============================================================================
INSERT INTO tasks (title, description, status, priority, category_id, assigned_user_id, due_date)
VALUES ('Implement User Authentication',
        'Add JWT-based authentication to the API endpoints',
        'IN_PROGRESS',
        'HIGH',
        (SELECT id FROM categories WHERE name = 'Development'),
        (SELECT id FROM users WHERE username = 'john.doe'),
        CURRENT_TIMESTAMP + INTERVAL '7 days'),

       ('Create REST API Documentation',
        'Document all REST endpoints using Swagger/OpenAPI',
        'TODO',
        'MEDIUM',
        (SELECT id FROM categories WHERE name = 'Documentation'),
        (SELECT id FROM users WHERE username = 'jane.smith'),
        CURRENT_TIMESTAMP + INTERVAL '5 days'),

       ('Fix Database Connection Pool',
        'Investigate and fix the connection pool timeout issues',
        'IN_PROGRESS',
        'URGENT',
        (SELECT id FROM categories WHERE name = 'Bug Fix'),
        (SELECT id FROM users WHERE username = 'bob.johnson'),
        CURRENT_TIMESTAMP + INTERVAL '2 days'),

       ('Write Integration Tests',
        'Create integration tests for task management endpoints',
        'TODO',
        'HIGH',
        (SELECT id FROM categories WHERE name = 'Testing'),
        (SELECT id FROM users WHERE username = 'alice.williams'),
        CURRENT_TIMESTAMP + INTERVAL '10 days'),

       ('Performance Testing',
        'Conduct load testing and optimize database queries',
        'TODO',
        'MEDIUM',
        (SELECT id FROM categories WHERE name = 'Testing'),
        (SELECT id FROM users WHERE username = 'bob.johnson'),
        CURRENT_TIMESTAMP + INTERVAL '14 days'),

       ('Setup CI/CD Pipeline',
        'Configure GitHub Actions for automated testing and deployment',
        'IN_PROGRESS',
        'HIGH',
        (SELECT id FROM categories WHERE name = 'DevOps'),
        (SELECT id FROM users WHERE username = 'john.doe'),
        CURRENT_TIMESTAMP + INTERVAL '4 days'),

       ('Docker Configuration',
        'Create Docker containers for application and database',
        'TODO',
        'MEDIUM',
        (SELECT id FROM categories WHERE name = 'DevOps'),
        (SELECT id FROM users WHERE username = 'admin'),
        CURRENT_TIMESTAMP + INTERVAL '8 days'),

       ('Add Email Notifications',
        'Implement email notifications for task assignments and updates',
        'TODO',
        'LOW',
        (SELECT id FROM categories WHERE name = 'Feature'),
        (SELECT id FROM users WHERE username = 'jane.smith'),
        CURRENT_TIMESTAMP + INTERVAL '20 days'),

       ('Implement Task Comments',
        'Add commenting functionality to tasks',
        'TODO',
        'MEDIUM',
        (SELECT id FROM categories WHERE name = 'Feature'),
        (SELECT id FROM users WHERE username = 'john.doe'),
        CURRENT_TIMESTAMP + INTERVAL '15 days'),

       ('UI Mockups for Dashboard',
        'Create wireframes and mockups for the dashboard interface',
        'DONE',
        'HIGH',
        (SELECT id FROM categories WHERE name = 'Design'),
        (SELECT id FROM users WHERE username = 'alice.williams'),
        CURRENT_TIMESTAMP + INTERVAL '30 days'),

       ('Research Caching Solutions',
        'Evaluate Redis and Hazelcast for application caching',
        'TODO',
        'MEDIUM',
        (SELECT id FROM categories WHERE name = 'Research'),
        (SELECT id FROM users WHERE username = 'bob.johnson'),
        CURRENT_TIMESTAMP + INTERVAL '12 days'),

       ('Weekly Team Standup',
        'Regular team sync meeting to discuss progress and blockers',
        'TODO',
        'LOW',
        (SELECT id FROM categories WHERE name = 'Meeting'),
        (SELECT id FROM users WHERE username = 'admin'),
        CURRENT_TIMESTAMP + INTERVAL '1 day'),

       ('Migrate to MongoDB',
        'This task was cancelled as we decided to stick with PostgreSQL',
        'CANCELLED',
        'LOW',
        (SELECT id FROM categories WHERE name = 'Development'),
        (SELECT id FROM users WHERE username = 'admin'),
        NULL),

       ('Update Dependencies',
        'Update all project dependencies to latest versions',
        'TODO',
        'MEDIUM',
        (SELECT id FROM categories WHERE name = 'Development'),
        (SELECT id FROM users WHERE username = 'john.doe'),
        CURRENT_TIMESTAMP + INTERVAL '1 hour');