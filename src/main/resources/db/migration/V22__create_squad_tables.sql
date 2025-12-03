-- Create squads table
CREATE TABLE IF NOT EXISTS squads (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    lead_id BIGINT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_squad_lead FOREIGN KEY (lead_id) REFERENCES users(id) ON DELETE CASCADE
);

-- Create squad_members table
CREATE TABLE IF NOT EXISTS squad_members (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    squad_id BIGINT NOT NULL,
    joined_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_squad_member_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_squad_member_squad FOREIGN KEY (squad_id) REFERENCES squads(id) ON DELETE CASCADE,
    CONSTRAINT uk_squad_member UNIQUE (user_id, squad_id)
);

-- Create squad_invites table
CREATE TABLE IF NOT EXISTS squad_invites (
    id BIGSERIAL PRIMARY KEY,
    squad_id BIGINT NOT NULL,
    invited_user_id BIGINT NOT NULL,
    invited_by_id BIGINT NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    responded_at TIMESTAMP,
    CONSTRAINT fk_squad_invite_squad FOREIGN KEY (squad_id) REFERENCES squads(id) ON DELETE CASCADE,
    CONSTRAINT fk_squad_invite_user FOREIGN KEY (invited_user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_squad_invite_by FOREIGN KEY (invited_by_id) REFERENCES users(id) ON DELETE CASCADE
);

-- Create indexes
CREATE INDEX IF NOT EXISTS idx_squad_lead ON squads(lead_id);
CREATE INDEX IF NOT EXISTS idx_squad_member_user ON squad_members(user_id);
CREATE INDEX IF NOT EXISTS idx_squad_member_squad ON squad_members(squad_id);
CREATE INDEX IF NOT EXISTS idx_squad_invite_user ON squad_invites(invited_user_id);
CREATE INDEX IF NOT EXISTS idx_squad_invite_status ON squad_invites(status);
