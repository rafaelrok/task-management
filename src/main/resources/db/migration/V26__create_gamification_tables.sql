-- Create gamification tables

-- User scores table
CREATE TABLE IF NOT EXISTS user_scores (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL UNIQUE,
    total_points INTEGER NOT NULL DEFAULT 0,
    total_tasks_completed_in_squads INTEGER NOT NULL DEFAULT 0,
    total_tasks_completed_early INTEGER NOT NULL DEFAULT 0,
    current_level INTEGER NOT NULL DEFAULT 1,
    CONSTRAINT fk_user_score_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

-- Badges table
CREATE TABLE IF NOT EXISTS badges (
    id BIGSERIAL PRIMARY KEY,
    code VARCHAR(50) NOT NULL UNIQUE,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    icon_class VARCHAR(100)
);

-- User badges table (many-to-many)
CREATE TABLE IF NOT EXISTS user_badges (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    badge_id BIGINT NOT NULL,
    awarded_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_user_badge_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_user_badge_badge FOREIGN KEY (badge_id) REFERENCES badges(id) ON DELETE CASCADE,
    CONSTRAINT uk_user_badge UNIQUE (user_id, badge_id)
);

-- Create indexes
CREATE INDEX IF NOT EXISTS idx_user_score_user ON user_scores(user_id);
CREATE INDEX IF NOT EXISTS idx_user_score_points ON user_scores(total_points DESC);
CREATE INDEX IF NOT EXISTS idx_user_badge_user ON user_badges(user_id);
CREATE INDEX IF NOT EXISTS idx_user_badge_badge ON user_badges(badge_id);

-- Insert initial badges
INSERT INTO badges (code, name, description, icon_class) VALUES
    ('EARLY_FINISHER_5', 'Early Finisher 5', 'Concluiu 5 tarefas de squad antes do prazo', 'fa-solid fa-medal'),
    ('EARLY_FINISHER_20', 'Early Finisher 20', 'Concluiu 20 tarefas de squad antes do prazo', 'fa-solid fa-trophy'),
    ('SQUAD_COMMITED_10', 'Squad Committed 10', 'Concluiu 10 tarefas de squad', 'fa-solid fa-star'),
    ('SQUAD_MULTI', 'Squad Multi', 'Participa de mais de 1 squad', 'fa-solid fa-users')
ON CONFLICT (code) DO NOTHING;
