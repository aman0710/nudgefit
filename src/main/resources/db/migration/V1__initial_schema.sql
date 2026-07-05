-- V1__initial_schema.sql
-- NudgeFit database schema

-- =====================
-- users
-- =====================
CREATE TABLE users (
    id                   UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    phone_number         VARCHAR(20) NOT NULL UNIQUE,
    name                 VARCHAR(100),
    current_weight_kg    DECIMAL(5,2),
    height_cm            DECIMAL(5,2),
    age                  INTEGER,
    gender               VARCHAR(10),
    activity_level       VARCHAR(20),
    tdee_calories        DECIMAL(7,2),
    daily_calorie_target DECIMAL(7,2),
    fat_goal             VARCHAR(10),
    muscle_goal          VARCHAR(10),
    intensity_level      VARCHAR(20),
    current_body_fat_pct DECIMAL(5,2),
    target_body_fat_pct  DECIMAL(5,2),
    current_muscle_mass_kg DECIMAL(5,2),
    target_muscle_mass_kg  DECIMAL(5,2),
    daily_protein_target_g DECIMAL(5,2),
    daily_carbs_target_g   DECIMAL(5,2),
    daily_fat_target_g     DECIMAL(5,2),
    conversation_state   VARCHAR(30),
    last_message_at      TIMESTAMP,
    timezone             VARCHAR(50) DEFAULT 'Asia/Kolkata',
    created_at           TIMESTAMP DEFAULT NOW(),
    updated_at           TIMESTAMP DEFAULT NOW()
);

-- =====================
-- daily_logs
-- =====================
CREATE TABLE daily_logs (
    id                      UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id                 UUID NOT NULL REFERENCES users(id),
    log_date                DATE NOT NULL,
    total_calories_consumed DECIMAL(7,2) DEFAULT 0,
    total_calories_burned   DECIMAL(7,2) DEFAULT 0,
    net_calories            DECIMAL(7,2) DEFAULT 0,
    target_calories         DECIMAL(7,2),
    target_protein_g        DECIMAL(5,2),
    target_carbs_g          DECIMAL(5,2),
    target_fat_g            DECIMAL(5,2),
    compliance              VARCHAR(20),
    created_at              TIMESTAMP DEFAULT NOW(),
    UNIQUE (user_id, log_date)
);

-- =====================
-- food_entries
-- =====================
CREATE TABLE food_entries (
    id             UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    daily_log_id   UUID NOT NULL REFERENCES daily_logs(id),
    user_id        UUID NOT NULL REFERENCES users(id),
    raw_user_input TEXT,
    parsed_items   JSONB,
    total_calories DECIMAL(7,2),
    protein_g      DECIMAL(5,2),
    carbs_g        DECIMAL(5,2),
    fat_g          DECIMAL(5,2),
    meal_type      VARCHAR(10),
    logged_at      TIMESTAMP DEFAULT NOW()
);

-- =====================
-- workout_entries
-- =====================
CREATE TABLE workout_entries (
    id               UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    daily_log_id     UUID NOT NULL REFERENCES daily_logs(id),
    user_id          UUID NOT NULL REFERENCES users(id),
    raw_user_input   TEXT,
    workout_type     VARCHAR(15),
    duration_minutes INTEGER,
    calories_burned  DECIMAL(7,2),
    details          JSONB,
    logged_at        TIMESTAMP DEFAULT NOW()
);

-- =====================
-- goal_snapshots
-- =====================
CREATE TABLE goal_snapshots (
    id                   UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id              UUID NOT NULL REFERENCES users(id),
    snapshot_date        DATE,
    projected_goal_date  DATE,
    daily_calorie_target DECIMAL(7,2),
    days_remaining       INTEGER,
    days_impact          DECIMAL(5,2),
    reason               TEXT,
    created_at           TIMESTAMP DEFAULT NOW()
);
