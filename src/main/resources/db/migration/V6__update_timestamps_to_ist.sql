-- V6__update_timestamps_to_ist.sql
-- Shift existing UTC timestamps forward to IST and update default value for future inserts

-- 1. Shift existing data by adding 5 hours and 30 minutes
UPDATE users SET 
    created_at = created_at + interval '5 hours 30 minutes',
    updated_at = updated_at + interval '5 hours 30 minutes',
    last_message_at = last_message_at + interval '5 hours 30 minutes'
WHERE created_at IS NOT NULL;

UPDATE daily_logs SET created_at = created_at + interval '5 hours 30 minutes' WHERE created_at IS NOT NULL;
UPDATE food_entries SET logged_at = logged_at + interval '5 hours 30 minutes' WHERE logged_at IS NOT NULL;
UPDATE workout_entries SET logged_at = logged_at + interval '5 hours 30 minutes' WHERE logged_at IS NOT NULL;

-- 2. Update column defaults so new records are automatically in IST
ALTER TABLE users ALTER COLUMN created_at SET DEFAULT timezone('Asia/Kolkata', now());
ALTER TABLE users ALTER COLUMN updated_at SET DEFAULT timezone('Asia/Kolkata', now());

ALTER TABLE daily_logs ALTER COLUMN created_at SET DEFAULT timezone('Asia/Kolkata', now());

ALTER TABLE food_entries ALTER COLUMN logged_at SET DEFAULT timezone('Asia/Kolkata', now());

ALTER TABLE workout_entries ALTER COLUMN logged_at SET DEFAULT timezone('Asia/Kolkata', now());
