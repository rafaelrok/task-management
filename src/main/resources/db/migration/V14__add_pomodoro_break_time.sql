-- Add break time field for pomodoro pause duration
ALTER TABLE tasks
    ADD COLUMN IF NOT EXISTS pomodoro_break_minutes INTEGER DEFAULT 5;

