-- Create task_cancel_requests table
CREATE TABLE IF NOT EXISTS task_cancel_requests (
    id BIGSERIAL PRIMARY KEY,
    task_id BIGINT NOT NULL,
    requested_by_id BIGINT NOT NULL,
    lead_to_approve_id BIGINT NOT NULL,
    reason TEXT NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    previous_status VARCHAR(20) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    decided_at TIMESTAMP,
    CONSTRAINT fk_cancel_request_task FOREIGN KEY (task_id) REFERENCES tasks(id) ON DELETE CASCADE,
    CONSTRAINT fk_cancel_request_user FOREIGN KEY (requested_by_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_cancel_request_lead FOREIGN KEY (lead_to_approve_id) REFERENCES users(id) ON DELETE CASCADE
);

-- Create indexes
CREATE INDEX IF NOT EXISTS idx_cancel_request_task ON task_cancel_requests(task_id);
CREATE INDEX IF NOT EXISTS idx_cancel_request_lead ON task_cancel_requests(lead_to_approve_id);
CREATE INDEX IF NOT EXISTS idx_cancel_request_status ON task_cancel_requests(status);
